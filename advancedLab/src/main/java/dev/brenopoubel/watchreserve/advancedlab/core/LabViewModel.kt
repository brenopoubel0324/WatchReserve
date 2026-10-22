package dev.brenopoubel.watchreserve.advancedlab.core

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.brenopoubel.watchreserve.advancedlab.BuildConfig
import dev.brenopoubel.watchreserve.advancedlab.IReadOnlyService
import dev.brenopoubel.watchreserve.advancedlab.service.ReadOnlyService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LabState(val status: String = "Verificando acesso…", val canAuthorize: Boolean = false,
    val canCollect: Boolean = false, val busy: Boolean = false, val collecting: Boolean = false, val message: String = "",
    val report: File? = null, val sessionPrepared: Boolean = false, val recordError: Boolean = false,
    val sessionStatus: String = "")

class LabViewModel @JvmOverloads constructor(application: Application,
    private val durationLimitMillis: Long = COLLECTION_LIMIT_MILLIS) : AndroidViewModel(application) {
    private val reports = ReportStore(application)
    private val sessions = ReadSessionStore(application)
    var state by mutableStateOf(LabState(report = reports.latest()))
        private set
    private val main = Handler(Looper.getMainLooper())
    private var operation: Job? = null
    private class Session(val args: Shizuku.UserServiceArgs) {
        var connection: ServiceConnection? = null
        @Volatile var recipe: ReadSession? = null
        var timedOut = false
        var bridgeLost = false
    }
    private var activeSession: Session? = null
    private var cleanupBlocked = false
    private val received = Shizuku.OnBinderReceivedListener { main.post { refresh() } }
    private val died = Shizuku.OnBinderDeadListener { main.post {
        activeSession?.bridgeLost = true
        cancel(); cleanupBlocked = false; refresh()
    } }
    private val permission = Shizuku.OnRequestPermissionResultListener { request, result ->
        if (request == PERMISSION_REQUEST) main.post {
            refresh()
            state = state.copy(message = if (result == PackageManager.PERMISSION_GRANTED)
                "" else "Acesso recusado. Nenhuma coleta iniciada.")
        }
    }

    init {
        require(durationLimitMillis in 1..COLLECTION_LIMIT_MILLIS)
        require(durationLimitMillis == COLLECTION_LIMIT_MILLIS || BuildConfig.DEBUG &&
            android.os.Build.HARDWARE in setOf("ranchu", "goldfish")) { "Prazo de ensaio reservado ao emulador." }
        try {
            sessions.reconcile()?.let { recipe ->
                state = state.copy(sessionPrepared = recipe.phase == ReadPhase.PREPARED,
                    sessionStatus = phaseLabel(recipe.phase), message = recipe.detail)
            }
        } catch (_: Exception) {
            state = state.copy(recordError = true, message = "Registro de coleta indisponível. Descarte-o antes de preparar outra sessão.")
        }
        Shizuku.addBinderReceivedListenerSticky(received)
        Shizuku.addBinderDeadListener(died)
        Shizuku.addRequestPermissionResultListener(permission)
        refresh()
    }

    @Suppress("DEPRECATION")
    private fun environment(): ReadEnvironment {
        val app = getApplication<Application>()
        val info = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signatures = info.signingInfo?.apkContentsSigners.orEmpty()
        require(signatures.size == 1) { "Assinatura do laboratório não reconhecida." }
        val certificate = MessageDigest.getInstance("SHA-256").digest(signatures.single().toByteArray())
            .joinToString("") { "%02x".format(it) }
        val deviceId = Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val boot = Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -1)
        check(deviceId.isNotBlank() && boot >= 0) { "Não foi possível identificar este dispositivo e reinício." }
        return ReadEnvironment(deviceId, android.os.Build.MODEL, android.os.Build.FINGERPRINT, boot,
            Process.myUid(), info.longVersionCode, info.versionName.orEmpty(), certificate, trustedManager())
    }

    @Suppress("DEPRECATION")
    private fun trustedManager(): BridgeIdentity {
        val manager = getApplication<Application>().packageManager
        val candidates = BridgeTrust.packages.mapNotNull { name ->
            try {
                val info = manager.getPackageInfo(name, PackageManager.GET_SIGNING_CERTIFICATES)
                val signatures = info.signingInfo?.apkContentsSigners.orEmpty()
                require(signatures.size == 1) { "Assinatura da ponte não reconhecida." }
                val hash = MessageDigest.getInstance("SHA-256").digest(signatures.single().toByteArray())
                    .joinToString("") { "%02x".format(it) }
                BridgeIdentity(name, hash, info.longVersionCode, info.versionName.orEmpty())
            } catch (_: PackageManager.NameNotFoundException) { null }
        }
        require(candidates.isNotEmpty()) { "Instale e inicie uma ponte qualificada neste relógio." }
        return BridgeTrust.validate(candidates)
    }

    fun refresh() {
        if (state.busy) return
        if (cleanupBlocked || state.recordError) {
            state = state.copy(canCollect = false, canAuthorize = false)
            return
        }
        try {
            val manager = trustedManager()
            val label = when (manager.packageName) {
                BridgeTrust.WATCHRESERVE -> "WatchReserve Bridge"
                BridgeTrust.SHEVERY -> "Shevery"
                else -> "Shizuku"
            }
            if (!Shizuku.pingBinder()) {
                state = state.copy(status = "$label instalado; ponte não iniciada.", canAuthorize = false, canCollect = false)
            } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                state = state.copy(status = "$label pronto. Autorize o acesso.", canAuthorize = true, canCollect = false)
            } else {
                require(Shizuku.getUid() == 2000) { "Este laboratório aceita somente ponte shell, sem root." }
                require(Shizuku.getVersion() >= 13) { "Protocolo da ponte incompatível." }
                state = state.copy(status = "$label autorizado.", canAuthorize = false, canCollect = true)
            }
        } catch (failure: RuntimeException) {
            state = state.copy(status = failure.message ?: "Acesso indisponível.", canAuthorize = false, canCollect = false)
        }
    }

    fun prepareSession() {
        refresh()
        if (!state.canCollect || state.busy || state.sessionPrepared) return
        state = state.copy(busy = true)
        operation = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    sessions.save(ReadSession(UUID.randomUUID().toString(), ReadPhase.PREPARED,
                        environment(), quick = false, createdAt = System.currentTimeMillis()))
                }
                state = state.copy(sessionPrepared = true, sessionStatus = phaseLabel(ReadPhase.PREPARED),
                    message = "Sessão pronta. Ative a economia desejada e volte para executar a coleta.")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                state = state.copy(recordError = true, message = "Não foi possível preparar a sessão. Nenhuma coleta iniciada.")
            } finally { state = state.copy(busy = false); refresh() }
        }
    }

    fun discardSession() {
        if (state.busy) return
        try {
            sessions.discard()
            state = state.copy(sessionPrepared = false, recordError = false, sessionStatus = "",
                message = "Registro de leitura descartado. Nenhuma configuração foi alterada.")
            refresh()
        } catch (_: Exception) { state = state.copy(message = "Não foi possível descartar o registro. Encerre a coleta primeiro.") }
    }

    fun authorize() {
        refresh()
        if (!state.canAuthorize) return
        try { trustedManager(); Shizuku.requestPermission(PERMISSION_REQUEST) }
        catch (_: RuntimeException) { state = state.copy(message = "Falha ao solicitar acesso. Atualize o estado e tente novamente.") }
    }

    private suspend fun bind(session: Session): IReadOnlyService = suspendCancellableCoroutine { continuation ->
        val current = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                if (continuation.isActive) continuation.resume(IReadOnlyService.Stub.asInterface(binder))
                else runCatching { Shizuku.unbindUserService(session.args, this, true) }
            }
            override fun onServiceDisconnected(name: ComponentName) {
                if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Serviço desconectado."))
            }
        }
        session.connection = current
        continuation.invokeOnCancellation { runCatching { Shizuku.unbindUserService(session.args, current, true) } }
        try { Shizuku.bindUserService(session.args, current) }
        catch (failure: RuntimeException) { if (continuation.isActive) continuation.resumeWithException(failure) }
    }

    fun collect(quick: Boolean = false, prepared: Boolean = false) {
        refresh()
        if (!state.canCollect || state.busy || state.sessionPrepared != prepared) return
        state = state.copy(busy = true, collecting = true, message = "Coletando dados privados…")
        // Uma resposta tardia de uma coleta cancelada só pode encerrar a própria sessão.
        val session = Session(Shizuku.UserServiceArgs(ComponentName(getApplication(), ReadOnlyService::class.java))
            .daemon(false).processNameSuffix("diagnostic").tag("readonly-${UUID.randomUUID()}")
            .debuggable(BuildConfig.DEBUG).version(BuildConfig.VERSION_CODE))
        activeSession = session
        val deadlineElapsed = SystemClock.elapsedRealtime() + durationLimitMillis
        val watchdog = Runnable {
            if (activeSession === session) { session.timedOut = true; cancel() }
        }
        main.postDelayed(watchdog, durationLimitMillis)
        operation = viewModelScope.launch {
            var pending: File? = null
            var outcome = ReadPhase.FAILED
            try {
                withContext(Dispatchers.IO) {
                    val current = environment()
                    val recipe = if (prepared) checkNotNull(sessions.load()) { "Sessão preparada ausente." }
                    else ReadSession(UUID.randomUUID().toString(), ReadPhase.PREPARED, current, quick,
                        System.currentTimeMillis()).also { sessions.save(it) }
                    session.recipe = recipe
                    require(recipe.phase == ReadPhase.PREPARED) { "A sessão não está pronta para executar." }
                    try { recipe.environment.requireCompatible(current) }
                    catch (failure: IllegalArgumentException) {
                        sessions.update(recipe.id) { it.transition(ReadPhase.INVALID, System.currentTimeMillis(), reason = failure.message.orEmpty()) }
                        outcome = ReadPhase.INVALID
                        throw failure
                    }
                    sessions.update(recipe.id) {
                        it.transition(ReadPhase.CONNECTING, System.currentTimeMillis(),
                            token = ReadSessionStore.processToken, deadline = deadlineElapsed)
                    }
                }
                state = state.copy(sessionPrepared = false)
                checkNotNull(session.recipe).environment.requireCompatible(withContext(Dispatchers.IO) { environment() })
                state = state.copy(message = "Conectando ao helper local…", sessionStatus = phaseLabel(ReadPhase.CONNECTING))
                val service = withTimeout(10_000) { bind(session) }
                withContext(Dispatchers.IO) {
                    val identity = JSONObject(service.identity())
                    check(identity.getInt("uid") == 2000 && identity.getInt("ownerUid") == Process.myUid()) {
                        "Identidade do serviço divergente."
                    }
                }
                if (prepared) {
                    sessions.update(checkNotNull(session.recipe).id) { it.transition(ReadPhase.DELAYED, System.currentTimeMillis()) }
                    state = state.copy(message = "Coleta em 5 segundos. Mantenha este painel aberto.", sessionStatus = phaseLabel(ReadPhase.DELAYED))
                    delay(5_000)
                }
                state = state.copy(message = "Coletando dados privados…", sessionStatus = phaseLabel(ReadPhase.RUNNING))
                val result = withContext(Dispatchers.IO) {
                    val identity = JSONObject(service.identity())
                    check(identity.getInt("uid") == 2000 && identity.getInt("ownerUid") == Process.myUid()) {
                        "Identidade do serviço divergente."
                    }
                    val recipe = checkNotNull(session.recipe)
                    recipe.environment.requireCompatible(environment())
                    sessions.update(recipe.id) { it.transition(ReadPhase.RUNNING, System.currentTimeMillis()) }
                    val file = reports.pending(recipe.id).also { pending = it }
                    val queries = if (recipe.quick) listOf(ReadQuery.POWER, ReadQuery.PROCESSES) else ReadQuery.entries
                    val summary = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_CREATE or
                        ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_WRITE_ONLY).use {
                        JSONObject(service.collect(queries.map { query -> query.name }.toTypedArray(), it, recipe.id, deadlineElapsed))
                    }
                    file to summary
                }
                val finalized = reports.complete(result.first)
                pending = null
                outcome = if (result.second.getBoolean("deadlineExceeded")) ReadPhase.TIMED_OUT else ReadPhase.COMPLETED
                val queries = result.second.getJSONArray("queries")
                val partial = (0 until queries.length()).count { queries.getJSONObject(it).getString("status") != "ok" }
                state = state.copy(report = finalized,
                    message = "UID 2000 · ${queries.length()} consultas. $partial parciais ou com falha.")
            } catch (_: TimeoutCancellationException) {
                outcome = ReadPhase.TIMED_OUT
                state = state.copy(message = "Tempo limite ao conectar o serviço da ponte.")
            } catch (cancelled: CancellationException) {
                outcome = when {
                    session.timedOut -> ReadPhase.TIMED_OUT
                    session.bridgeLost -> ReadPhase.FAILED
                    else -> ReadPhase.CANCELLED
                }
                state = state.copy(message = if (session.timedOut) "Prazo total de coleta esgotado. Relatório incompleto descartado."
                    else if (session.bridgeLost) "A ponte foi encerrada. Coleta interrompida."
                    else "Coleta cancelada. Relatório incompleto descartado.")
                throw cancelled
            } catch (failure: Exception) {
                runCatching { File(getApplication<Application>().noBackupFilesDir, "last-error.txt")
                    .writeText(failure.stackTraceToString().take(16_384)) }
                state = state.copy(message = if (outcome == ReadPhase.INVALID) failure.message.orEmpty()
                    else "Coleta não concluída (${failure.javaClass.simpleName}). Confira o acesso.")
            } finally {
                withContext(NonCancellable) {
                    main.removeCallbacks(watchdog)
                    val cleanup = runCatching {
                        if (Shizuku.pingBinder()) session.connection?.let { Shizuku.unbindUserService(session.args, it, true) }
                    }
                    if (cleanup.isFailure) outcome = ReadPhase.FAILED
                    val record = withContext(Dispatchers.IO) {
                        runCatching {
                            session.recipe?.let { recipe -> sessions.update(recipe.id) {
                                if (it.phase.terminal) it else it.transition(outcome, System.currentTimeMillis(), reason =
                                    if (cleanup.isFailure) "Falha ao encerrar helper; encerre a ponte." else state.message)
                            } } ?: sessions.load()
                        }
                    }
                    if (activeSession === session) activeSession = null
                    pending?.delete()
                    state = state.copy(busy = false, collecting = false,
                        sessionPrepared = record.getOrNull()?.phase == ReadPhase.PREPARED,
                        sessionStatus = phaseLabel(record.getOrNull()?.phase ?: outcome), recordError = record.isFailure)
                    refresh()
                    if (cleanup.isFailure) {
                        cleanupBlocked = true
                        state = state.copy(message = "Falha ao encerrar o serviço da coleta. Encerre a ponte antes de continuar.", canCollect = false)
                    }
                    if (record.isFailure) state = state.copy(message = "Não foi possível concluir o registro da coleta. Descarte-o antes de continuar.")
                }
            }
        }
    }

    fun cancel() {
        operation?.cancel()
        activeSession?.let { session -> runCatching { session.connection?.let { Shizuku.unbindUserService(session.args, it, true) } } }
    }

    fun export(destination: Uri) {
        val report = state.report ?: return
        if (state.busy) return
        viewModelScope.launch {
            state = state.copy(busy = true)
            state = try {
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    checkNotNull(resolver.openOutputStream(destination, "wt")).use { output -> report.inputStream().use { it.copyTo(output) } }
                }
                state.copy(busy = false, message = "Relatório exportado por sua solicitação.")
            } catch (cancelled: CancellationException) {
                state = state.copy(busy = false)
                throw cancelled
            } catch (_: Exception) { state.copy(busy = false, message = "Falha na exportação. Relatório mantido no laboratório.") }
        }
    }

    fun exportUnavailable() { state = state.copy(message = "Exportação indisponível neste relógio. Relatório mantido no laboratório.") }

    override fun onCleared() {
        Shizuku.removeBinderReceivedListener(received)
        Shizuku.removeBinderDeadListener(died)
        Shizuku.removeRequestPermissionResultListener(permission)
        cancel()
    }

    companion object {
        private const val PERMISSION_REQUEST = 71
        const val COLLECTION_LIMIT_MILLIS = ReadSession.MAX_DURATION_MILLIS
        private fun phaseLabel(phase: ReadPhase) = when (phase) {
            ReadPhase.PREPARED -> "Sessão preparada"
            ReadPhase.DELAYED -> "Aguardando início local"
            ReadPhase.CONNECTING -> "Conectando ao helper"
            ReadPhase.RUNNING -> "Coleta em andamento"
            ReadPhase.COMPLETED -> "Coleta concluída"
            ReadPhase.CANCELLED -> "Coleta cancelada"
            ReadPhase.TIMED_OUT -> "Coleta excedeu o prazo"
            ReadPhase.FAILED -> "Coleta não concluída"
            ReadPhase.INTERRUPTED -> "Coleta interrompida"
            ReadPhase.INVALID -> "Sessão incompatível"
        }
    }
}
