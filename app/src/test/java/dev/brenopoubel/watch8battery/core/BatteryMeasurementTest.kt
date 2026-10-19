package dev.brenopoubel.watch8battery.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryMeasurementTest {
    private val start = BatterySample(80, 10_000_000L, 1_000_000_000L, 4, false)
    private val twoHoursLater = start.copy(percent = 74, elapsedMillis = start.elapsedMillis + 7_200_000L,
        wallMillis = start.wallMillis + 7_200_000L)

    @Test fun `calcula pontos percentuais por hora sem confundir porcentagem relativa`() {
        val result = BatteryMeasurement.finish(start, twoHoursLater)
        assertEquals(6, result.percentagePoints)
        assertEquals(3.0, result.pointsPerHour!!, 0.0001)
        assertNull(result.invalidReason)
    }

    @Test fun `carga detectada invalida e nao produz taxa`() {
        val result = BatteryMeasurement.finish(start, twoHoursLater.copy(charging = true))
        assertNotNull(result.invalidReason)
        assertNull(result.pointsPerHour)
    }

    @Test fun `boot count detecta reinicio mesmo uptime final maior`() {
        val result = BatteryMeasurement.finish(start, twoHoursLater.copy(bootCount = 5))
        assertNotNull(result.invalidReason)
        assertNull(result.pointsPerHour)
    }

    @Test fun `uptime menor detecta reinicio sem boot count`() {
        val result = BatteryMeasurement.finish(start.copy(bootCount = null), twoHoursLater.copy(bootCount = null,
            elapsedMillis = 50_000L))
        assertNotNull(result.invalidReason)
        assertNull(result.pointsPerHour)
    }

    @Test fun `mudanca de relogio torna comparacao inconclusiva`() {
        val result = BatteryMeasurement.finish(start, twoHoursLater.copy(wallMillis = twoHoursLater.wallMillis + 3_600_000L))
        assertNotNull(result.invalidReason)
        assertNull(result.pointsPerHour)
    }

    @Test fun `aumento da bateria rejeita possivel carga entre leituras`() {
        assertNotNull(BatteryMeasurement.finish(start, twoHoursLater.copy(percent = 85)).invalidReason)
    }

    @Test fun `tempo zero nao divide por zero`() {
        val result = BatteryMeasurement.finish(start, start)
        assertNull(result.pointsPerHour)
        assertNotNull(result.invalidReason)
    }

    @Test fun `bateria constante e medicao valida nao provam economia`() {
        val result = BatteryMeasurement.finish(start, twoHoursLater.copy(percent = 80))
        assertEquals(0.0, result.pointsPerHour!!, 0.0001)
        assertNull(result.invalidReason)
    }

    @Test fun `ausencia de boot count aceita janela com relogios coerentes`() {
        assertNull(BatteryMeasurement.finish(start.copy(bootCount = null), twoHoursLater.copy(bootCount = null)).invalidReason)
    }
}
