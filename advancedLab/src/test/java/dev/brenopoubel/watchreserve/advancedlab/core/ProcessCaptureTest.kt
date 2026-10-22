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

    @Test fun recognizesDumpsysTimeoutEvenAfterLargeOutputAndSuccessfulExit() {
        val result = ProcessCapture().run(fixture("dump-timeout"), ByteArrayOutputStream())
        assertEquals(0, result.exitCode)
        assertEquals("timeout", result.status)
        assertTrue(result.timedOut)
        assertFalse(result.truncated)
    }

    @Test fun recognizesLatePermissionDenialBeyondStoredOutputLimit() {
        val result = ProcessCapture(maxBytes = 1024).run(fixture("late-denied"), ByteArrayOutputStream())
        assertTrue(result.truncated)
        assertEquals("permission_denied", result.status)
    }

    @Test fun keepsSuccessfulOutputContainingOrdinaryTimeoutConfigurationValid() {
        val result = ProcessCapture().run(fixture("timeout-config"), ByteArrayOutputStream())
        assertEquals("ok", result.status)
        assertFalse(result.timedOut)
    }

    @Test fun detectsTimeoutAcrossEveryPossibleReadBoundary() {
        val marker = "\n*** SERVICE 'power' DUMP TIMEOUT (10000ms) EXPIRED ***\n".toByteArray()
        for (split in 1 until marker.size) {
            val scanner = OutputStatusScanner()
            val first = marker.copyOfRange(0, split)
            val second = marker.copyOfRange(split, marker.size)
            scanner.accept(first, first.size)
            scanner.accept(second, second.size)
            assertTrue("Marcador dividido no byte $split.", scanner.timedOut)
        }
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
            "dump-timeout" -> {
                repeat(3) { System.out.write(ByteArray(8192) { 65 }) }
                print("\n*** SERVICE 'power' DUMP TIME")
                System.out.flush()
                Thread.sleep(50)
                println("OUT (10000ms) EXPIRED ***")
            }
            "late-denied" -> {
                repeat(3) { System.out.write(ByteArray(8192) { 65 }) }
                println("\nPermission Denial: can't dump power without android.permission.DUMP")
            }
            "timeout-config" -> println("mTimeout=10000\nlastTimeoutMillis=0")
        }
    }
}
