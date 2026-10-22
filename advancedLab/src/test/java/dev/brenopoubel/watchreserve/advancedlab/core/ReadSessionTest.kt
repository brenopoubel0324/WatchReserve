package dev.brenopoubel.watchreserve.advancedlab.core

import org.junit.Assert.*
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties

class ReadSessionTest {
    private val bridge = BridgeIdentity(BridgeTrust.WATCHRESERVE,
        "db38593b18806c7403cc7c2950848d2ad6296c3d7010d35f58d80c37b8e1f228", 2, "0.2-wear-readonly")
    private val environment = ReadEnvironment("fixture-id", "fixture-watch", "fixture-firmware", 7,
        10120, 2, "0.2-readonly-sessions", "caed7e1e5f17eead9720810079e0072264358d3219d72f67a0ea19466ec48d69", bridge)
    private val prepared = ReadSession("11111111-1111-4111-8111-111111111111", ReadPhase.PREPARED,
        environment, quick = false, createdAt = 1000)

    @Test fun recipeRoundTripPreservesContextAndAccents() {
        val recipe = prepared.copy(detail = "Já está pronta.\nConexão e versão: ação manual.")
        assertEquals(recipe, ReadSession.decode(recipe.encode()))
    }

    @Test fun everyCompatibilityBoundaryInvalidatesPreparedRecipe() {
        environment.requireCompatible(environment)
        val changes = listOf(environment.copy(deviceId = "other"), environment.copy(model = "other"),
            environment.copy(fingerprint = "updated"), environment.copy(bootCount = 8),
            environment.copy(clientUid = 10121), environment.copy(clientVersion = 3),
            environment.copy(clientVersionName = "next"), environment.copy(clientCertificate = "00"),
            environment.copy(bridge = bridge.copy(versionCode = 3)),
            environment.copy(bridge = bridge.copy(certificate = "00")),
            environment.copy(bridge = bridge.copy(packageName = "other")))
        changes.forEach { changed ->
            assertThrows(IllegalArgumentException::class.java) { environment.requireCompatible(changed) }
        }
    }

    @Test fun preparedCannotSkipExecutionOrInventCompletion() {
        for (phase in listOf(ReadPhase.RUNNING, ReadPhase.COMPLETED, ReadPhase.INTERRUPTED)) {
            assertThrows(IllegalArgumentException::class.java) { prepared.transition(phase, 2000) }
        }
        assertThrows(IllegalArgumentException::class.java) { prepared.transition(ReadPhase.CONNECTING, 2000) }
    }

    @Test fun localDelayConnectionAndReadHaveExplicitFiniteOwner() {
        val connected = prepared.transition(ReadPhase.CONNECTING, 2000, "process-one", 181000)
        val delayed = connected.transition(ReadPhase.DELAYED, 3000)
        val running = delayed.transition(ReadPhase.RUNNING, 4000)
        val complete = running.transition(ReadPhase.COMPLETED, 5000, reason = "UID 2000 · concluído")
        assertEquals("process-one", complete.ownerToken)
        assertEquals(181000, complete.deadlineElapsed)
        assertEquals(complete, ReadSession.decode(complete.encode()))
        assertThrows(IllegalArgumentException::class.java) { complete.transition(ReadPhase.CONNECTING, 6000) }
    }

    @Test fun interruptionIsTerminalAndNeverBecomesAutomaticRetry() {
        val connecting = prepared.transition(ReadPhase.CONNECTING, 2000, "old-process", 181000)
        val interrupted = connecting.transition(ReadPhase.INTERRUPTED, 3000, reason = "Processo encerrado")
        assertTrue(interrupted.phase.terminal)
        assertThrows(IllegalArgumentException::class.java) { interrupted.transition(ReadPhase.CONNECTING, 4000) }
    }

    @Test fun cancelBeforeCommandsKeepsPreparedIdentity() {
        val cancelled = prepared.transition(ReadPhase.CANCELLED, 2000, reason = "Cancelado")
        assertEquals(prepared.id, cancelled.id)
        assertEquals(prepared.environment, cancelled.environment)
        assertEquals(0, cancelled.deadlineElapsed)
    }

    @Test fun parserRejectsIncompleteUnknownAndUnboundedRecords() {
        assertThrows(IllegalArgumentException::class.java) { ReadSession.decode("x".repeat(16385)) }
        assertThrows(IllegalArgumentException::class.java) { ReadSession.decode("") }
        val invalid = listOf("schema" to "2", "quick" to "yes", "phase" to "ROOT",
            "id" to "../../other", "bootCount" to "-1", "clientUid" to "0",
            "clientCertificate" to "unknown", "bridgeCertificate" to "00", "createdAt" to "0",
            "phase" to "RUNNING")
        invalid.forEach { (key, value) ->
            assertThrows(IllegalArgumentException::class.java) { ReadSession.decode(change(prepared, key, value)) }
        }
        assertThrows(IllegalArgumentException::class.java) { ReadSession.decode(change(prepared, "deviceId", null)) }
    }

    private fun change(recipe: ReadSession, key: String, value: String?): String {
        val properties = Properties().apply { load(StringReader(recipe.encode())) }
        if (value == null) properties.remove(key) else properties.setProperty(key, value)
        return StringWriter().also { properties.store(it, null) }.toString()
    }
}
