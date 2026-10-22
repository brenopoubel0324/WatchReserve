package dev.brenopoubel.watchreserve.advancedlab.core

import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ProcessCaptureTest {
    private fun fixture(mode: String): List<String> {
        val executable = File(System.getProperty("java.home"), "bin/java" + if (System.getProperty("os.name").orEmpty().startsWith("Windows")) ".exe" else "")
        return listOf(executable.path, "-cp", System.getProperty("java.class.path"), CaptureFixture::class.java.name, mode)
    }

    @Test fun drainsLargeMixedOutputWithoutDeadlockAndCapsStoredBytes() {
        val output = ByteArrayOutputStream()
        val result = ProcessCapture(timeoutMillis = 5_000, maxBytes = 1024).run(fixture("large"), output)
        assertEquals("truncated", result.status)
        assertEquals(1024, output.size())
        assertEquals(262_144L, result.bytes)
        assertFalse(result.timedOut)
    }

    @Test fun killsCommandOnTimeoutWithoutReportingSuccessfulEmptyOutput() {
        val result = ProcessCapture(timeoutMillis = 200).run(fixture("hang"), ByteArrayOutputStream())
        assertEquals("timeout", result.status)
        assertNull(result.exitCode)
        assertTrue(result.durationMillis < 4_000)
    }

    @Test fun preservesNonzeroExitStatus() {
        val result = ProcessCapture().run(fixture("fail"), ByteArrayOutputStream())
        assertEquals(7, result.exitCode)
        assertEquals("command_failed", result.status)
    }

    @Test fun recognizesPermissionFailureEvenWhenCommandReturnsZero() {
        val result = ProcessCapture().run(fixture("denied"), ByteArrayOutputStream())
        assertEquals(0, result.exitCode)
        assertEquals("permission_denied", result.status)
    }

    @Test fun recognizesMissingServiceWithoutTreatingItAsZeroActivity() {
        val result = ProcessCapture().run(fixture("missing"), ByteArrayOutputStream())
        assertEquals("service_unavailable", result.status)
    }
}

object CaptureFixture {
    @JvmStatic fun main(args: Array<String>) {
        when (args.single()) {
            "large" -> repeat(16) { System.out.write(ByteArray(8192) { 65 }); System.err.write(ByteArray(8192) { 66 }) }
            "hang" -> Thread.sleep(60_000)
            "fail" -> kotlin.system.exitProcess(7)
            "denied" -> println("Permission Denial: can't dump power without android.permission.DUMP")
            "missing" -> println("Can't find service: absent")
        }
    }
}
