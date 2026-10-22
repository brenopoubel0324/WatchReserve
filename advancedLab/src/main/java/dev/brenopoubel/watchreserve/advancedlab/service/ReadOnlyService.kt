package dev.brenopoubel.watchreserve.advancedlab.service

import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.PowerManager
import androidx.annotation.Keep
import dev.brenopoubel.watchreserve.advancedlab.IReadOnlyService
import dev.brenopoubel.watchreserve.advancedlab.core.ProcessCapture
import dev.brenopoubel.watchreserve.advancedlab.core.ReadQuery
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
    override fun collect(queryIds: Array<String>, destination: ParcelFileDescriptor): String {
        checkCaller()
        val queries = ReadQuery.resolve(queryIds)
        val results = JSONArray()
        destination.use { descriptor ->
            ZipOutputStream(ParcelFileDescriptor.AutoCloseOutputStream(descriptor)).use { zip ->
                for (query in queries) {
                    zip.putNextEntry(ZipEntry("${query.name.lowercase()}.txt"))
                    val result = try {
                        val captured = ProcessCapture().run(query.arguments, zip)
                        JSONObject().put("query", query.name).put("status", captured.status)
                            .put("exitCode", captured.exitCode ?: JSONObject.NULL).put("bytes", captured.bytes)
                            .put("durationMillis", captured.durationMillis)
                    } catch (failure: java.io.IOException) {
                        // Falha do arquivo aborta a coleta em vez de produzir um ZIP falsamente completo.
                        throw failure
                    }
                    zip.closeEntry()
                    results.put(result)
                }
                val metadata = JSONObject(identity()).put("schema", 1).put("queries", results)
                zip.putNextEntry(ZipEntry("summary.json"))
                zip.write(metadata.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return JSONObject(identity()).put("queries", results).toString()
    }

    override fun destroy() {
        check(Binder.getCallingUid() == ownerUid || Binder.getCallingUid() == 2000) { "Cliente não autorizado." }
        kotlin.system.exitProcess(0)
    }
}
