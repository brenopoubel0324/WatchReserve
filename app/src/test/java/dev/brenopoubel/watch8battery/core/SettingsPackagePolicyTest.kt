package dev.brenopoubel.watch8battery.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPackagePolicyTest {
    @Test fun `aceita pacote sistema de bateria diferente do pacote de ajustes`() {
        val allowed = SettingsPackagePolicy.allowedPackages(listOf(
            SettingsProvider("system.settings", true),
            SettingsProvider("vendor.battery", true),
        ))
        assertEquals(setOf("system.settings", "vendor.battery"), allowed)
    }

    @Test fun `handler instalado pelo usuario nao entra na allowlist`() {
        val allowed = SettingsPackagePolicy.allowedPackages(listOf(
            SettingsProvider("system.settings", true),
            SettingsProvider("thirdparty.battery", false),
        ))
        assertEquals(setOf("system.settings"), allowed)
    }

    @Test fun `sem handler de bateria preserva somente configuracoes gerais`() {
        assertEquals(setOf("system.settings"), SettingsPackagePolicy.allowedPackages(listOf(
            SettingsProvider("system.settings", true),
        )))
    }

    @Test fun `sem provedor comprovado nenhum pacote e aceito`() {
        assertTrue(SettingsPackagePolicy.allowedPackages(emptyList()).isEmpty())
    }
}
