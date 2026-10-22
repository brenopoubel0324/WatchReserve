package dev.brenopoubel.watchreserve.advancedlab.core

import java.io.IOException
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class CaptureResult(val exitCode: Int?, val timedOut: Boolean, val truncated: Boolean,
    val bytes: Long, val durationMillis: Long, val failure: String? = null) {
    val status: String get() = when {
        timedOut -> "timeout"
        failure != null -> failure
        exitCode != 0 -> "command_failed"
        truncated -> "truncated"
        else -> "ok"
    }
}

/** Drena stdout/stderr juntos enquanto aguarda; limita arquivo sem bloquear o processo produtor. */
class ProcessCapture(private val timeoutMillis: Long = 10_000, private val maxBytes: Long = 1_048_576) {
    fun run(arguments: List<String>, output: OutputStream): CaptureResult {
        require(timeoutMillis > 0 && maxBytes > 0)
        val start = System.nanoTime()
        val process = ProcessBuilder(arguments).redirectErrorStream(true).start()
        val prefix = ByteArrayOutputStream()
        val reader = Executors.newSingleThreadExecutor { task -> Thread(task, "diagnostic-output").apply { isDaemon = true } }
        val draining = reader.submit<Long> {
            var received = 0L
            process.inputStream.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val size = input.read(buffer)
                    if (size < 0) break
                    val prefixRemaining = 8192 - prefix.size()
                    if (prefixRemaining > 0) prefix.write(buffer, 0, minOf(size, prefixRemaining))
                    val remaining = (maxBytes - received).coerceAtLeast(0)
                    if (remaining > 0) output.write(buffer, 0, minOf(size.toLong(), remaining).toInt())
                    received += size
                }
            }
            received
        }
        try {
            val exited = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
            if (!exited) {
                process.destroyForcibly()
                process.waitFor(1, TimeUnit.SECONDS)
            }
            val bytes = try { draining.get(1, TimeUnit.SECONDS) }
            catch (failure: Exception) { throw IOException("Saída incompleta da consulta.", failure) }
            val text = prefix.toString("UTF-8")
            val failure = when {
                Regex("(?im)^\\s*(permission denial:|permission denied|.*requires.*permission.*denied)").containsMatchIn(text) -> "permission_denied"
                Regex("(?im)^\\s*(can't find service:|error dumping service info:)").containsMatchIn(text) -> "service_unavailable"
                else -> null
            }
            return CaptureResult(if (exited) process.exitValue() else null, !exited, bytes > maxBytes,
                bytes, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), failure)
        } finally {
            process.destroyForcibly()
            process.inputStream.close()
            process.errorStream.close()
            process.outputStream.close()
            draining.cancel(true)
            reader.shutdownNow()
        }
    }
}
