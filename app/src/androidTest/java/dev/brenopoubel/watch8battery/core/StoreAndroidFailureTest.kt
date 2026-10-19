package dev.brenopoubel.watch8battery.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreAndroidFailureTest {
    @Test fun failedDiskWriteDoesNotConfirmDestinationInRealSharedPreferences() = withPreferences { preferences, directory ->
        val store = RouteStore(preferences)
        val candidate = RouteProof("settings/example.Screen", Build.FINGERPRINT, 1L, true, false)
        assertTrue(store.save(candidate))
        assertTrue(directory.setWritable(false, true))
        assertFalse(store.save(candidate.copy(visuallyConfirmed = true)))
        assertEquals(candidate, store.read())
    }

    @Test fun failedDiskWriteDoesNotFinishActiveMeasurement() = withPreferences { preferences, directory ->
        val store = MeasurementStore(preferences)
        val sample = BatterySample(80, 10_000L, 1_000_000L, 1, false)
        assertTrue(store.start(sample))
        assertTrue(directory.setWritable(false, true))
        assertFalse(store.complete("Resultado de teste"))
        assertEquals(sample, store.active())
    }

    private fun withPreferences(block: (SharedPreferences, File) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "audit-fixture-${UUID.randomUUID()}"
        val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val directory = File(context.applicationInfo.dataDir, "shared_prefs")
        try {
            block(preferences, directory)
        } finally {
            check(directory.setWritable(true, true)) { "Falha ao restaurar permissões da fixture de teste." }
            context.deleteSharedPreferences(name)
        }
    }

    companion object {
        @BeforeClass @JvmStatic fun requireEmulator() {
            check(Build.HARDWARE in setOf("ranchu", "goldfish")) {
                "A injeção de falhas é exclusiva de emulador; não executar em aparelho pessoal."
            }
        }
    }
}
