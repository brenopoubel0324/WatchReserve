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
import kotlinx.coroutines.TimeoutCancellationException
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
    val report: File? = null)

class LabViewModel(application: Application) : AndroidViewModel(application) {
    private val reports = ReportStore(application)
    var state by mutableStateOf(LabState(report = reports.latest()))
        private set
    private val main = Handler(Looper.getMainLooper())
    private var operation: Job? = null
    private class Session(val args: Shizuku.UserServiceArgs) { var connection: ServiceConnection? = null }
    private var activeSession: Session? = null
    private var cleanupBlocked = false
    private val received = Shizuku.OnBinderReceivedListener { main.post { refresh() } }
    private val died = Shizuku.OnBinderDeadListener { main.post { cancel(); cleanupBlocked = false; refresh() } }
    private val permission = Shizuku.OnRequestPermissionResultListener { request, result ->
        if (request == PERMISSION_REQUEST) main.post {
            refresh()
            state = state.copy(message = if (result == PackageManager.PERMISSION_GRANTED)
                "" else "Acesso recusado. Nenhuma coleta iniciada.")
        }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(received)
        Shizuku.addBinderDeadListener(died)
        Shizuku.addRequestPermissionResultListener(permission)
        refresh()
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
        if (cleanupBlocked) {
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

    fun collect(quick: Boolean = false) {
        refresh()
        if (!state.canCollect || state.busy) return
        state = state.copy(busy = true, collecting = true, message = "Coletando dados privados…")
        // Uma resposta tardia de uma coleta cancelada só pode encerrar a própria sessão.
        val session = Session(Shizuku.UserServiceArgs(ComponentName(getApplication(), ReadOnlyService::class.java))
            .daemon(false).processNameSuffix("diagnostic").tag("readonly-${UUID.randomUUID()}")
            .debuggable(BuildConfig.DEBUG).version(BuildConfig.VERSION_CODE))
        activeSession = session
        operation = viewModelScope.launch {
            var pending: File? = null
            try {
                trustedManager()
                val service = withTimeout(10_000) { bind(session) }
                val result = withContext(Dispatchers.IO) {
                    val identity = JSONObject(service.identity())
                    check(identity.getInt("uid") == 2000 && identity.getInt("ownerUid") == Process.myUid()) {
                        "Identidade do serviço divergente."
                    }
                    val file = reports.pending().also { pending = it }
                    val queries = if (quick) listOf(ReadQuery.POWER, ReadQuery.PROCESSES) else ReadQuery.entries
                    val summary = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_CREATE or
                        ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_WRITE_ONLY).use {
                        JSONObject(service.collect(queries.map { query -> query.name }.toTypedArray(), it))
                    }
                    reports.complete(file) to summary
                }
                pending = null
                val queries = result.second.getJSONArray("queries")
                val partial = (0 until queries.length()).count { queries.getJSONObject(it).getString("status") != "ok" }
                state = state.copy(report = result.first,
                    message = "UID 2000 · ${queries.length()} consultas. $partial parciais ou com falha.")
            } catch (_: TimeoutCancellationException) {
                state = state.copy(message = "Tempo limite ao conectar o serviço da ponte.")
            } catch (cancelled: CancellationException) {
                state = state.copy(message = "Coleta cancelada. Relatório incompleto descartado.")
                throw cancelled
            } catch (failure: Exception) {
                runCatching { File(getApplication<Application>().noBackupFilesDir, "last-error.txt")
                    .writeText(failure.stackTraceToString().take(16_384)) }
                state = state.copy(message = "Coleta não concluída (${failure.javaClass.simpleName}). Confira o acesso.")
            } finally {
                val cleanup = runCatching { session.connection?.let { Shizuku.unbindUserService(session.args, it, true) } }
                if (activeSession === session) activeSession = null
                pending?.delete()
                state = state.copy(busy = false, collecting = false)
                refresh()
                if (cleanup.isFailure) {
                    cleanupBlocked = true
                    state = state.copy(message = "Falha ao encerrar o serviço da coleta. Encerre a ponte antes de continuar.", canCollect = false)
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

    companion object { private const val PERMISSION_REQUEST = 71 }
}
