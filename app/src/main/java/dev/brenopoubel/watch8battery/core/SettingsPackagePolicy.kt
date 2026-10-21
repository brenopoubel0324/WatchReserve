package dev.brenopoubel.watch8battery.core

/** Provedor descoberto para configurações gerais, economia Android ou bateria Samsung. */
data class SettingsProvider(val packageName: String, val systemApp: Boolean)

object SettingsPackagePolicy {
    fun allowedPackages(providers: Iterable<SettingsProvider>): Set<String> = providers
        .filter { it.systemApp && it.packageName.isNotBlank() }
        .map { it.packageName }
        .toSet()
}
