package dev.brenopoubel.watch8battery.core

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreFailureTest {
    private val candidate = RouteProof("settings/example.Screen", "firmware-a", 100L, true, false)
    private val sample = BatterySample(80, 10_000L, 1_000_000L, 1, false)

    @Test fun `falha ao confirmar nao autoriza destino pelo cache`() {
        val preferences = FailingPreferences()
        val store = RouteStore(preferences)
        assertTrue(store.save(candidate))

        preferences.failures = 1
        assertFalse(store.save(candidate.copy(visuallyConfirmed = true)))
        assertEquals(candidate, store.read())
        assertFalse(store.read()!!.isUsable("firmware-a", 100L))
    }

    @Test fun `falha ao apagar preserva a prova anterior`() {
        val preferences = FailingPreferences()
        val store = RouteStore(preferences)
        val confirmed = candidate.copy(visuallyConfirmed = true)
        assertTrue(store.save(confirmed))

        preferences.failures = 1
        assertFalse(store.clear())
        assertEquals(confirmed, store.read())
    }

    @Test fun `falha ao iniciar nao deixa medicao ativa`() {
        val preferences = FailingPreferences()
        val store = MeasurementStore(preferences)
        assertTrue(store.complete("Resultado anterior"))

        preferences.failures = 1
        assertFalse(store.start(sample))
        assertNull(store.active())
        assertEquals("Resultado anterior", store.lastResult())
    }

    @Test fun `falha ao finalizar mantem medicao e resultado anteriores`() {
        val preferences = FailingPreferences()
        val store = MeasurementStore(preferences)
        assertTrue(store.complete("Resultado anterior"))
        assertTrue(store.start(sample))

        preferences.failures = 1
        assertFalse(store.complete("Resultado novo"))
        assertEquals(sample, store.active())
        assertEquals("Resultado anterior", store.lastResult())
    }

    @Test fun `falha ao limpar nao perde medicao ativa`() {
        val preferences = FailingPreferences()
        val store = MeasurementStore(preferences)
        assertTrue(store.start(sample))

        preferences.failures = 1
        assertFalse(store.clear())
        assertEquals(sample, store.active())
    }

    @Test fun `falha tambem no rollback preserva cache e permite nova tentativa`() {
        val preferences = FailingPreferences()
        val store = RouteStore(preferences)
        assertTrue(store.save(candidate))

        preferences.failures = 2
        assertFalse(store.save(candidate.copy(visuallyConfirmed = true)))
        assertEquals(candidate, store.read())
        assertTrue(store.save(candidate.copy(visuallyConfirmed = true)))
        assertTrue(store.read()!!.isUsable("firmware-a", 100L))
    }
}

/** Reproduz o contrato Android: commit() altera o cache mesmo quando retorna false. */
private class FailingPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any>()
    var failures = 0

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun contains(key: String): Boolean = values.containsKey(key)
    override fun getString(key: String, defValue: String?): String? = (values[key] ?: defValue) as String?
    override fun getInt(key: String, defValue: Int): Int = (values[key] ?: defValue) as Int
    override fun getLong(key: String, defValue: Long): Long = (values[key] ?: defValue) as Long
    override fun getFloat(key: String, defValue: Float): Float = (values[key] ?: defValue) as Float
    override fun getBoolean(key: String, defValue: Boolean): Boolean = (values[key] ?: defValue) as Boolean

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        ((values[key] ?: defValues) as Set<String>?)?.toMutableSet()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun edit(): SharedPreferences.Editor = Editor()

    private inner class Editor : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String, value: String?) = apply { changes[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?) = apply { changes[key] = values?.toSet() }
        override fun putInt(key: String, value: Int) = apply { changes[key] = value }
        override fun putLong(key: String, value: Long) = apply { changes[key] = value }
        override fun putFloat(key: String, value: Float) = apply { changes[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value }
        override fun remove(key: String) = apply { changes[key] = null }
        override fun clear() = apply { clearRequested = true }
        override fun apply(): Unit = error("O store deve confirmar a gravação de forma síncrona.")

        override fun commit(): Boolean {
            if (clearRequested) values.clear()
            for ((key, value) in changes) {
                if (value == null) values.remove(key) else values[key] = value
            }
            if (failures > 0) {
                failures -= 1
                return false
            }
            return true
        }
    }
}
