package dev.brenopoubel.watchreserve.advancedlab.core

import org.junit.Assert.*
import org.junit.Test

class BridgeTrustTest {
    private val shevery = BridgeIdentity(BridgeTrust.SHEVERY,
        "f1673c7e4ecdbaecc134dae9ffd7f8ce0815d58deb4d590d8770f7ed8cd61670", 44, "14.4.0")

    @Test fun acceptsQualifiedSheveryIdentity() { assertEquals(shevery, BridgeTrust.validate(listOf(shevery))) }
    @Test fun rejectsUnknownCertificateEvenWithExpectedPackageAndVersion() {
        assertThrows(IllegalArgumentException::class.java) { BridgeTrust.validate(listOf(shevery.copy(certificate = "00"))) }
    }
    @Test fun rejectsAmbiguousManagersAndUnqualifiedUpdate() {
        for (identities in listOf(emptyList(), listOf(shevery, shevery), listOf(shevery.copy(versionCode = 45)))) {
            assertThrows(IllegalArgumentException::class.java) { BridgeTrust.validate(identities) }
        }
    }
    @Test fun originalShizukuRequiresExactBuildAsWellAsCertificate() {
        val original = BridgeIdentity(BridgeTrust.SHIZUKU,
            "268b5590e868fb08bae7e0ac413564cd1ff88f5ccff74af9dbd0dc918e30db30", 1086, "13.6.0.r1086.2650830c")
        assertEquals(original, BridgeTrust.validate(listOf(original)))
        for (candidate in listOf(original.copy(versionCode = 1087), original.copy(versionName = "13.6.0-other"))) {
            assertThrows(IllegalArgumentException::class.java) { BridgeTrust.validate(listOf(candidate)) }
        }
    }
}
