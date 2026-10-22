package dev.brenopoubel.watchreserve.advancedlab.service

import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.PowerManager
import android.os.SystemClock
import androidx.annotation.Keep
import dev.brenopoubel.watchreserve.advancedlab.IReadOnlyService
import dev.brenopoubel.watchreserve.advancedlab.core.ProcessCapture
import dev.brenopoubel.watchreserve.advancedlab.core.ReadQuery
import dev.brenopoubel.watchreserve.advancedlab.core.ReadSession
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Instanciado pelo UserService da ponte; não é um serviço Android permanente. */
@Keep
class ReadOnlyService @Keep constructor(private val context: Context) : IReadOnlyService.Stub() {
    private val ownerUid = context.applicationInfo.uid

    private fun checkCaller() {
        check(Process.myUid() == 2000) { "Este laboratório exige ponte shell, sem root." }
        check(Binder.getCallingUid() == ownerUid) { "Cliente não autorizado." }
    }

    override fun identity(): String {
        checkCaller()
        return JSONObject().put("uid", Process.myUid()).put("ownerUid", ownerUid)
            .put("pid", Process.myPid()).put("sdk", Build.VERSION.SDK_INT)
            .put("powerSave", context.getSystemService(PowerManager::class.java).isPowerSaveMode).toString()
    }

    @Synchronized
    override fun collect(queryIds: Array<String>, destination: ParcelFileDescriptor, sessionId: String, deadlineElapsed: Long): String {
        checkCaller()
        require(UUID.fromString(sessionId).toString() == sessionId) { "Identificação de coleta inválida." }
        val queries = ReadQuery.resolve(queryIds)
        val startedAt = System.currentTimeMillis()
        val startedElapsed = SystemClock.elapsedRealtime()
        require(deadlineElapsed > startedElapsed && deadlineElapsed - startedElapsed <= ReadSession.MAX_DURATION_MILLIS) {
            "Prazo da coleta inválido ou vencido."
        }
        val powerSaveAtStart = context.getSystemService(PowerManager::class.java).isPowerSaveMode
        val results = JSONArray()
        return destination.use { descriptor ->
            ZipOutputStream(ParcelFileDescriptor.AutoCloseOutputStream(descriptor)).use { zip ->
                for (query in queries) {
                    zip.putNextEntry(ZipEntry("${query.name.lowercase()}.txt"))
                    val remaining = deadlineElapsed - SystemClock.elapsedRealtime()
                    val result = if (remaining > 0) {
                        val captured = ProcessCapture(timeoutMillis = minOf(10_000L, remaining)).run(query.arguments, zip)
                        JSONObject().put("query", query.name).put("status", captured.status)
                            .put("exitCode", captured.exitCode ?: JSONObject.NULL).put("bytes", captured.bytes)
                            .put("durationMillis", captured.durationMillis)
                    } else JSONObject().put("query", query.name).put("status", "not_run_deadline")
                        .put("exitCode", JSONObject.NULL).put("bytes", 0).put("durationMillis", 0)
                    zip.closeEntry()
                    results.put(result)
                }
                val metadata = JSONObject(identity()).put("schema", 2).put("queries", results)
                    .put("sessionId", sessionId).put("deadlineElapsed", deadlineElapsed)
                    .put("deadlineExceeded", SystemClock.elapsedRealtime() >= deadlineElapsed)
                    .put("model", Build.MODEL).put("firmware", Build.VERSION.INCREMENTAL)
                    .put("startedAtEpochMillis", startedAt).put("finishedAtEpochMillis", System.currentTimeMillis())
                    .put("elapsedMillis", SystemClock.elapsedRealtime() - startedElapsed)
                    .put("powerSaveAtStart", powerSaveAtStart)
                metadata.put("powerSaveAtEnd", metadata.getBoolean("powerSave"))
                zip.putNextEntry(ZipEntry("summary.json"))
                zip.write(metadata.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                metadata.toString()
            }
        }
    }

    override fun destroy() {
        check(Binder.getCallingUid() == ownerUid || Binder.getCallingUid() == 2000) { "Cliente não autorizado." }
        kotlin.system.exitProcess(0)
    }
}
