package dev.brenopoubel.watch8battery.core

/** Provedor descoberto para ACTION_SETTINGS ou ACTION_BATTERY_SAVER_SETTINGS. */
data class SettingsProvider(val packageName: String, val systemApp: Boolean)

object SettingsPackagePolicy {
    fun allowedPackages(providers: Iterable<SettingsProvider>): Set<String> = providers
        .filter { it.systemApp && it.packageName.isNotBlank() }
        .map { it.packageName }
        .toSet()
}
