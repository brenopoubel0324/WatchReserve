package dev.brenopoubel.watchreserve.advancedlab.core

import java.io.StringReader
import java.io.StringWriter
import java.util.Properties
import java.util.UUID

data class ReadEnvironment(val deviceId: String, val model: String, val fingerprint: String,
    val bootCount: Int, val clientUid: Int, val clientVersion: Long, val clientVersionName: String,
    val clientCertificate: String, val bridge: BridgeIdentity) {
    fun requireCompatible(current: ReadEnvironment) {
        require(this == current) { "O ambiente mudou. Descarte a sessão e prepare outra." }
    }
}

enum class ReadPhase {
    PREPARED, DELAYED, CONNECTING, RUNNING, COMPLETED, CANCELLED, TIMED_OUT, FAILED, INTERRUPTED, INVALID;
    val active get() = this in setOf(DELAYED, CONNECTING, RUNNING)
    val terminal get() = this != PREPARED && !active
}

/** Receita exclusivamente de leitura; não é um diário de restauração de controles. */
data class ReadSession(val id: String, val phase: ReadPhase, val environment: ReadEnvironment,
    val quick: Boolean, val createdAt: Long, val changedAt: Long = createdAt,
    val ownerToken: String = "", val deadlineElapsed: Long = 0, val detail: String = "") {
    fun transition(next: ReadPhase, now: Long, token: String = ownerToken,
        deadline: Long = deadlineElapsed, reason: String = ""): ReadSession {
        val allowed = when (phase) {
            ReadPhase.PREPARED -> setOf(ReadPhase.CONNECTING, ReadPhase.CANCELLED, ReadPhase.INVALID)
            ReadPhase.DELAYED -> setOf(ReadPhase.RUNNING, ReadPhase.CANCELLED, ReadPhase.TIMED_OUT,
                ReadPhase.FAILED, ReadPhase.INTERRUPTED, ReadPhase.INVALID)
            ReadPhase.CONNECTING -> setOf(ReadPhase.DELAYED, ReadPhase.RUNNING, ReadPhase.CANCELLED, ReadPhase.TIMED_OUT,
                ReadPhase.FAILED, ReadPhase.INTERRUPTED, ReadPhase.INVALID)
            ReadPhase.RUNNING -> setOf(ReadPhase.COMPLETED, ReadPhase.CANCELLED, ReadPhase.TIMED_OUT,
                ReadPhase.FAILED, ReadPhase.INTERRUPTED)
            else -> emptySet()
        }
        require(next in allowed) { "Transição de coleta inválida: $phase → $next." }
        require(!next.active || token.isNotBlank() && deadline > 0) { "Coleta sem responsável ou prazo." }
        return copy(phase = next, changedAt = now, ownerToken = token, deadlineElapsed = deadline, detail = reason)
    }

    fun encode(): String = Properties().apply {
        setProperty("schema", "1")
        setProperty("id", id); setProperty("phase", phase.name); setProperty("quick", quick.toString())
        setProperty("createdAt", createdAt.toString()); setProperty("changedAt", changedAt.toString())
        setProperty("ownerToken", ownerToken); setProperty("deadlineElapsed", deadlineElapsed.toString())
        setProperty("detail", detail)
        environment.let {
            setProperty("deviceId", it.deviceId); setProperty("model", it.model); setProperty("fingerprint", it.fingerprint)
            setProperty("bootCount", it.bootCount.toString()); setProperty("clientUid", it.clientUid.toString())
            setProperty("clientVersion", it.clientVersion.toString()); setProperty("clientVersionName", it.clientVersionName)
            setProperty("clientCertificate", it.clientCertificate)
            setProperty("bridgePackage", it.bridge.packageName); setProperty("bridgeCertificate", it.bridge.certificate)
            setProperty("bridgeVersion", it.bridge.versionCode.toString()); setProperty("bridgeVersionName", it.bridge.versionName)
        }
    }.let { properties -> StringWriter().also { properties.store(it, null) }.toString() }

    companion object {
        const val MAX_DURATION_MILLIS = 180_000L
        fun decode(text: String): ReadSession {
            require(text.length <= 16_384) { "Registro de coleta excedeu o limite." }
            val properties = Properties().apply { load(StringReader(text)) }
            fun field(key: String) = requireNotNull(properties.getProperty(key)) { "Campo de coleta ausente: $key." }
            require(field("schema") == "1") { "Formato de coleta não reconhecido." }
            val id = field("id")
            require(UUID.fromString(id).toString() == id) { "Identificador de coleta inválido." }
            val environment = ReadEnvironment(field("deviceId"), field("model"), field("fingerprint"),
                field("bootCount").toInt(), field("clientUid").toInt(), field("clientVersion").toLong(),
                field("clientVersionName"), field("clientCertificate"),
                BridgeIdentity(field("bridgePackage"), field("bridgeCertificate"), field("bridgeVersion").toLong(), field("bridgeVersionName")))
            require(environment.deviceId.isNotBlank() && environment.fingerprint.isNotBlank() && environment.model.isNotBlank())
            require(environment.bootCount >= 0 && environment.clientUid > 0 && environment.clientVersion > 0)
            require(environment.clientCertificate.matches(Regex("[a-f0-9]{64}")))
            BridgeTrust.validate(listOf(environment.bridge))
            return ReadSession(id, ReadPhase.valueOf(field("phase")), environment,
                field("quick").toBooleanStrict(), field("createdAt").toLong(), field("changedAt").toLong(),
                field("ownerToken"), field("deadlineElapsed").toLong(), field("detail")).also {
                require(it.createdAt > 0 && it.changedAt > 0 && it.deadlineElapsed >= 0)
                require(!it.phase.active || it.ownerToken.isNotBlank() && it.deadlineElapsed > 0)
            }
        }
    }
}
