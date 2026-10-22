package dev.brenopoubel.watchreserve.advancedlab.core

data class BridgeIdentity(val packageName: String, val certificate: String, val versionCode: Long, val versionName: String)

object BridgeTrust {
    const val SHEVERY = "com.hamondev.shevery"
    const val SHIZUKU = "moe.shizuku.privileged.api"
    val packages = listOf(SHEVERY, SHIZUKU)
    private val certificates = mapOf(
        SHEVERY to "f1673c7e4ecdbaecc134dae9ffd7f8ce0815d58deb4d590d8770f7ed8cd61670",
        SHIZUKU to "268b5590e868fb08bae7e0ac413564cd1ff88f5ccff74af9dbd0dc918e30db30",
    )
    fun validate(candidates: List<BridgeIdentity>): BridgeIdentity {
        require(candidates.size == 1) { "Use apenas uma ponte conhecida neste laboratório." }
        val candidate = candidates.single()
        require(certificates[candidate.packageName] == candidate.certificate.lowercase()) { "Certificado da ponte não reconhecido." }
        require((candidate.packageName == SHEVERY && candidate.versionCode == 44L && candidate.versionName == "14.4.0") ||
            (candidate.packageName == SHIZUKU && candidate.versionCode == 1086L &&
                candidate.versionName == "13.6.0.r1086.2650830c")) {
            "Versão da ponte ainda não qualificada."
        }
        return candidate
    }
}
