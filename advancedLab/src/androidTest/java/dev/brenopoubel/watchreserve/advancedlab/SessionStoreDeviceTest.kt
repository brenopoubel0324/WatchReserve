package dev.brenopoubel.watchreserve.advancedlab

import android.content.ContextWrapper
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.brenopoubel.watchreserve.advancedlab.core.*
import org.junit.Assert.*
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SessionStoreDeviceTest {
    private val target = InstrumentationRegistry.getInstrumentation().targetContext
    private val directory = File(target.noBackupFilesDir, "store-fixture-${UUID.randomUUID()}").apply { check(mkdirs()) }
    private val context = object : ContextWrapper(target) { override fun getNoBackupFilesDir() = directory }
    private val store = ReadSessionStore(context)
    private val recipe = ReadSession(UUID.randomUUID().toString(), ReadPhase.PREPARED,
        ReadEnvironment("fixture", "fixture-watch", "fixture-firmware", 1, 10120, 2, "0.2-readonly-sessions",
            "caed7e1e5f17eead9720810079e0072264358d3219d72f67a0ea19466ec48d69",
            BridgeIdentity(BridgeTrust.WATCHRESERVE, "db38593b18806c7403cc7c2950848d2ad6296c3d7010d35f58d80c37b8e1f228",
                2, "0.2-wear-readonly")), false, System.currentTimeMillis())

    @Test fun newStoreReadsSamePreparedRecipeWithoutStartingAnything() {
        store.save(recipe)
        assertEquals(recipe, ReadSessionStore(context).reconcile())
    }

    @Test fun previousProcessIsRecordedAsInterruptedAndNeverRetried() {
        val active = recipe.transition(ReadPhase.CONNECTING, System.currentTimeMillis(), "old-process", 180000)
        store.save(active)
        val reconciled = checkNotNull(ReadSessionStore(context).reconcile())
        assertEquals(ReadPhase.INTERRUPTED, reconciled.phase)
        assertEquals(active.id, reconciled.id)
        assertEquals(active.environment, reconciled.environment)
        assertEquals(reconciled, ReadSessionStore(context).reconcile())
    }

    @Test fun concurrentControllerCannotReconcileOrDiscardActiveSession() {
        val active = recipe.transition(ReadPhase.CONNECTING, System.currentTimeMillis(), ReadSessionStore.processToken, 180000)
        store.save(active)
        assertThrows(IllegalStateException::class.java) { ReadSessionStore(context).reconcile() }
        assertThrows(IllegalStateException::class.java) { store.discard() }
        assertThrows(IllegalArgumentException::class.java) { store.save(recipe.copy(id = UUID.randomUUID().toString())) }
        assertEquals(active, store.load())
    }

    @Test fun atomicBackupWinsOverTornReplacement() {
        store.save(recipe)
        val file = File(directory, "read-session.properties")
        assertTrue(file.renameTo(File(directory, "read-session.properties.bak")))
        file.writeText("torn replacement")
        assertEquals(recipe, ReadSessionStore(context).load())
    }

    @Test fun malformedReadRecordFailsClosedUntilExplicitDiscard() {
        File(directory, "read-session.properties").writeText("schema=999")
        assertThrows(IllegalArgumentException::class.java) { store.load() }
        store.discard()
        assertNull(store.load())
    }

    @After fun removeFixture() { check(directory.deleteRecursively()) }

    companion object {
        @JvmStatic @BeforeClass fun guard() {
            assertEquals("WatchReserve_Wear5_Small_Round", InstrumentationRegistry.getArguments().getString("reservedAvd"))
            assertTrue(Build.HARDWARE in setOf("ranchu", "goldfish"))
            assertTrue(InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.isScreenRound)
        }
    }
}
