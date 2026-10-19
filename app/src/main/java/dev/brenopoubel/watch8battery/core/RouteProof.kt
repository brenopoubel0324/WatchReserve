package dev.brenopoubel.watch8battery.core

/** Prova local: um destino só permanece válido no ambiente em que foi testado. */
data class RouteProof(
    val component: String,
    val fingerprint: String,
    val settingsVersion: Long,
    val testReturned: Boolean = false,
    val visuallyConfirmed: Boolean = false,
) {
    fun isUsable(currentFingerprint: String, currentSettingsVersion: Long): Boolean =
        component.isNotBlank() && fingerprint.isNotBlank() &&
            fingerprint == currentFingerprint && settingsVersion == currentSettingsVersion &&
            testReturned && visuallyConfirmed
}
