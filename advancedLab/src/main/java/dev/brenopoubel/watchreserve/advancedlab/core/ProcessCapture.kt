package dev.brenopoubel.watchreserve.advancedlab.core

import java.io.IOException
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
        val scanner = OutputStatusScanner()
        val reader = Executors.newSingleThreadExecutor { task -> Thread(task, "diagnostic-output").apply { isDaemon = true } }
        val draining = reader.submit<Long> {
            var received = 0L
            process.inputStream.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val size = input.read(buffer)
                    if (size < 0) break
                    scanner.accept(buffer, size)
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
            return CaptureResult(if (exited) process.exitValue() else null, !exited || scanner.timedOut, bytes > maxBytes,
                bytes, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), scanner.failure)
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

/** Examina também a saída descartada e conserva só uma cauda para marcadores entre blocos. */
internal class OutputStatusScanner {
    private var tail = ""
    var timedOut = false
        private set
    var failure: String? = null
        private set

    fun accept(bytes: ByteArray, size: Int) {
        // Os marcadores são ASCII; a conversão de um byte por caractere preserva suas fronteiras.
        val window = tail + String(bytes, 0, size, Charsets.ISO_8859_1)
        if (dumpTimeout.containsMatchIn(window)) timedOut = true
        if (permissionDenied.containsMatchIn(window)) failure = "permission_denied"
        else if (failure == null && unavailable.containsMatchIn(window)) failure = "service_unavailable"
        tail = window.takeLast(1024)
    }

    companion object {
        private val dumpTimeout = Regex("(?im)^[ \\t]*\\*\\*\\* SERVICE [^\\r\\n]*\\bDUMP TIMEOUT\\b")
        private val permissionDenied = Regex("(?im)^[ \\t]*(permission denial:|permission denied|.*requires.*permission.*denied)")
        private val unavailable = Regex("(?im)^[ \\t]*(can't find service:|error dumping service info:)")
    }
}
