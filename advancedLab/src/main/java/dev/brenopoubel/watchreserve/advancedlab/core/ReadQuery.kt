package dev.brenopoubel.watchreserve.advancedlab.core

/** Consultas fixas: nenhum argumento de comando vem de intents, relatórios ou texto do usuário. */
enum class ReadQuery(val arguments: List<String>) {
    POWER(listOf("dumpsys", "-t", "10", "power")),
    IDLE(listOf("dumpsys", "-t", "10", "deviceidle")),
    BATTERY(listOf("dumpsys", "-t", "10", "batterystats", "--charged")),
    ALARMS(listOf("dumpsys", "-t", "10", "alarm")),
    JOBS(listOf("dumpsys", "-t", "10", "jobscheduler")),
    PROCESSES(listOf("dumpsys", "-t", "10", "activity", "processes")),
    SERVICES(listOf("dumpsys", "-t", "10", "activity", "services")),
    PACKAGES(listOf("cmd", "package", "list", "packages", "-U")),
    BLUETOOTH(listOf("dumpsys", "-t", "10", "bluetooth_manager")),
    WIFI(listOf("cmd", "wifi", "status")),
    SENSORS(listOf("dumpsys", "-t", "10", "sensorservice")),
    WIFI_SCAN_POLICY(listOf("settings", "get", "global", "wifi_scan_always_enabled")),
    BLE_SCAN_POLICY(listOf("settings", "get", "global", "ble_scan_always_enabled"));

    companion object {
        fun resolve(ids: Array<String>): List<ReadQuery> {
            require(ids.isNotEmpty() && ids.size <= entries.size) { "Conjunto de consultas inválido." }
            require(ids.toSet().size == ids.size) { "Consultas repetidas." }
            return ids.map { id -> entries.singleOrNull { it.name == id }
                ?: throw IllegalArgumentException("Consulta não permitida.") }
        }
    }
}
