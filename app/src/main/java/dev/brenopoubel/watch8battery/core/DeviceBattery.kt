package dev.brenopoubel.watch8battery.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.SystemClock
import android.provider.Settings

object DeviceBattery {
    fun read(context: Context): BatterySample {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val bootCount = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
        } catch (_: Settings.SettingNotFoundException) {
            null
        } catch (_: SecurityException) {
            null
        }
        return BatterySample(
            percent = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else -1,
            elapsedMillis = SystemClock.elapsedRealtime(),
            wallMillis = System.currentTimeMillis(),
            bootCount = bootCount,
            charging = plugged != 0 || status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
        )
    }
}

/** Dados somente locais; não registra sensores nem agenda trabalho em segundo plano. */
class MeasurementStore internal constructor(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("measurement", Context.MODE_PRIVATE))

    fun active(): BatterySample? {
        if (!preferences.getBoolean("active", false)) return null
        return BatterySample(
            percent = preferences.getInt("percent", -1),
            elapsedMillis = preferences.getLong("elapsed", -1),
            wallMillis = preferences.getLong("wall", -1),
            bootCount = preferences.getInt("boot", -1).takeIf { it >= 0 },
            charging = false,
        )
    }

    fun start(sample: BatterySample): Boolean {
        if (active() != null || sample.charging || sample.percent !in 0..100) return false
        return preferences.commitPreservingMemory {
            putBoolean("active", true)
            putInt("percent", sample.percent)
            putLong("elapsed", sample.elapsedMillis)
            putLong("wall", sample.wallMillis)
            putInt("boot", sample.bootCount ?: -1)
        }
    }

    fun complete(summary: String): Boolean = preferences.commitPreservingMemory {
        remove("active").remove("percent").remove("elapsed").remove("wall").remove("boot")
        putString("last_result", summary)
    }

    fun lastResult(): String? = preferences.getString("last_result", null)

    fun clear(): Boolean = preferences.commitPreservingMemory { clear() }
}
