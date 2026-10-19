package dev.brenopoubel.watch8battery.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteProofTest {
    private val proof = RouteProof("settings/example.Screen", "firmware-a", 100L, true, true)

    @Test fun `confirmacao exige retorno do teste e confirmacao visual`() {
        assertFalse(proof.copy(testReturned = false).isUsable("firmware-a", 100L))
        assertFalse(proof.copy(visuallyConfirmed = false).isUsable("firmware-a", 100L))
        assertTrue(proof.isUsable("firmware-a", 100L))
    }

    @Test fun `atualizacao do firmware invalida mesmo componente`() {
        assertFalse(proof.isUsable("firmware-b", 100L))
    }

    @Test fun `atualizacao das configuracoes invalida mesmo firmware`() {
        assertFalse(proof.isUsable("firmware-a", 101L))
    }

    @Test fun `identidade vazia nunca autoriza destino`() {
        assertFalse(proof.copy(component = "").isUsable("firmware-a", 100L))
        assertFalse(proof.copy(fingerprint = "").isUsable("", 100L))
    }
}
