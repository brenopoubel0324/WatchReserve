package dev.brenopoubel.watch8battery.core

data class BatterySample(
    val percent: Int,
    val elapsedMillis: Long,
    val wallMillis: Long,
    val bootCount: Int?,
    val charging: Boolean,
)

data class MeasurementResult(
    val durationMillis: Long,
    val percentagePoints: Int,
    val pointsPerHour: Double?,
    val invalidReason: String?,
)

object BatteryMeasurement {
    /** Sem observação contínua, uma recarga ocorrida entre amostras pode passar despercebida. */
    fun integrityFailure(start: BatterySample, end: BatterySample): String? = when {
        start.percent !in 0..100 || end.percent !in 0..100 -> "Leitura da bateria indisponível."
        start.charging || end.charging -> "Carga detectada. Inicie outra medição fora do carregador."
        start.bootCount != null && end.bootCount != null && start.bootCount != end.bootCount ->
            "Reinício detectado. Inicie outra medição."
        end.elapsedMillis < start.elapsedMillis -> "Reinício detectado. Inicie outra medição."
        kotlin.math.abs((end.wallMillis - start.wallMillis) -
            (end.elapsedMillis - start.elapsedMillis)) > 300_000L ->
            "Relógio do sistema alterado ou aparelho reiniciado. Inicie outra medição."
        end.percent > start.percent -> "A bateria aumentou. Possível recarga; inicie outra medição."
        else -> null
    }

    fun finish(start: BatterySample, end: BatterySample): MeasurementResult {
        val duration = end.elapsedMillis - start.elapsedMillis
        val drop = start.percent - end.percent
        val reason = integrityFailure(start, end) ?: if (duration <= 0L) {
            "Tempo de medição insuficiente."
        } else null
        return MeasurementResult(
            durationMillis = duration.coerceAtLeast(0),
            percentagePoints = drop,
            pointsPerHour = if (reason == null) drop * 3_600_000.0 / duration else null,
            invalidReason = reason,
        )
    }
}
