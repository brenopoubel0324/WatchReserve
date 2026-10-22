package dev.brenopoubel.watchreserve.advancedlab.core

import org.junit.Assert.*
import org.junit.Test

class ReadQueryTest {
    @Test fun refusesUnrecognizedInputAndDuplicateQueriesBeforeRunning() {
        for (ids in listOf(emptyArray(), arrayOf("POWER", "POWER"), arrayOf("POWER", "POWER; reboot"))) {
            assertThrows(IllegalArgumentException::class.java) { ReadQuery.resolve(ids) }
        }
    }

    @Test fun batteryQueryNeverConsumesOrResetsHistory() {
        val queries = ReadQuery.resolve(ReadQuery.entries.map { it.name }.toTypedArray())
        assertTrue(queries.single { it == ReadQuery.BATTERY }.arguments.contains("--charged"))
        assertTrue(queries.flatMap { it.arguments }.none { it in setOf("--checkin", "--reset", "--enable", "put", "set", "disable-user", "force-idle") })
    }
}
