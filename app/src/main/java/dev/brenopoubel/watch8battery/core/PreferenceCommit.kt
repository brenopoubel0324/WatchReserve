package dev.brenopoubel.watch8battery.core

import android.annotation.SuppressLint
import android.content.SharedPreferences

/** Mantém a memória anterior quando commit() falha depois de aplicar a edição ao cache. */
@SuppressLint("UseKtx") // O resultado síncrono da persistência faz parte do contrato dos stores.
internal fun SharedPreferences.commitPreservingMemory(change: SharedPreferences.Editor.() -> Unit): Boolean =
    synchronized(this) {
        val previous = all
        if (edit().apply(change).commit()) return@synchronized true

        val rollback = edit().clear()
        for ((key, value) in previous) {
            when (value) {
                is String -> rollback.putString(key, value)
                is Int -> rollback.putInt(key, value)
                is Long -> rollback.putLong(key, value)
                is Float -> rollback.putFloat(key, value)
                is Boolean -> rollback.putBoolean(key, value)
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    rollback.putStringSet(key, value as Set<String>)
                }
                null -> rollback.remove(key)
            }
        }
        // Mesmo se esta gravação falhar, o cache volta ao estado anterior. O disco não é
        // considerado restaurado: o chamador continua recebendo falha e deve informá-la.
        rollback.commit()
        false
    }
