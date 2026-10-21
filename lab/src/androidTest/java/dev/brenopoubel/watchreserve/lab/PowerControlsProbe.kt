package dev.brenopoubel.watchreserve.lab

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.util.AtomicFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Prova de capacidade no AVD reservado; não é o mecanismo de ajustes do app publicado. */
@RunWith(AndroidJUnit4::class)
class PowerControlsProbe {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver
    private val power = context.getSystemService(PowerManager::class.java)
    private val location = context.getSystemService(LocationManager::class.java)
    private val journal = AtomicFile(File(context.noBackupFilesDir, "power-session.json"))
    private val report = AtomicFile(File(context.noBackupFilesDir, "power-report.json"))
    private val keys = listOf(
        "battery_saver_constants", "low_power", "low_power_sticky",
        "low_power_sticky_auto_disable_enabled", "low_power_sticky_auto_disable_level",
    )

    @Test fun evaluateAndRestoreControls() {
        requireReservedEmulator()
        instrumentation.uiAutomation.dropShellPermissionIdentity()
        val runId = checkNotNull(InstrumentationRegistry.getArguments().getString("runId"))
        if (InstrumentationRegistry.getArguments().getString("recoverOnly") == "true") {
            check(journal.baseFile.exists()) { "Não há sessão pendente para recuperar." }
            restore(JSONObject(journal.readFully().toString(Charsets.UTF_8)))
            writeAtomic(report, JSONObject().put("runId", runId).put("recoveryOnly", true)
                .put("settingsRestored", true).put("runtimeStateRestored", true)
                .put("batterySimulationReset", true).put("permissionRevoked", !hasPermission()).put("passed", true))
            return
        }
        check(!journal.baseFile.exists()) { "Sessão pendente: execute a recuperação antes de outra prova." }
        check(!hasPermission()) { "O laboratório deve iniciar sem a permissão avançada." }
        check(!batterySimulated()) { "Já existe simulação de bateria; não será substituída." }
        check(!power.isPowerSaveMode) { "A fixture exige economia inicialmente desligada." }
        val previousSettings = JSONObject()
        keys.forEach { previousSettings.put(it, Settings.Global.getString(resolver, it) ?: JSONObject.NULL) }
        val session = JSONObject()
            .put("schema", 2)
            .put("package", context.packageName)
            .put("settings", previousSettings)
            .put("powerSaveBefore", power.isPowerSaveMode)
            .put("locationModeBefore", power.locationPowerSaveMode)
            .put("secureLocationMode", Settings.Secure.getString(resolver, "location_mode") ?: JSONObject.NULL)
            .put("locationEnabledBefore", location.isLocationEnabled)
        val nativeSaverSupported = nativeSaverSupported()
        val results = JSONObject().put("runId", runId).put("sdk", Build.VERSION.SDK_INT)
            .put("ordinaryAppUid", Process.myUid() >= 10_000)
            .put("samsungValidated", false).put("autonomyMeasured", false)
            .put("platformBatterySaverSupported", nativeSaverSupported)
        writeAtomic(journal, session)
        var failure: Throwable? = null
        try {
            expectWriteDenied()
            results.put("writeDeniedBeforeGrant", true)
            setPermission(true)
            results.put("permissionGranted", hasPermission())
            shell("dumpsys battery unplug")
            shell("dumpsys battery set level 60")
            assertTrue(batterySimulated())

            assertTrue(Settings.Global.putInt(resolver, "low_power", 1))
            results.put("lowPowerWriteAccepted", true)
                .put("lowPowerReadback", Settings.Global.getString(resolver, "low_power"))
            if (nativeSaverSupported) awaitState("Economia não observada pela API") { power.isPowerSaveMode }
            else assertFalse("Mecanismo AOSP declarado desativado, mas API está ativa.", power.isPowerSaveMode)
            results.put("powerSaveObserved", power.isPowerSaveMode)
                .put("powerSaveControlValidated", power.isPowerSaveMode)
            val modeBefore = power.locationPowerSaveMode
            val modeApplied = if (modeBefore == PowerManager.LOCATION_MODE_FOREGROUND_ONLY)
                PowerManager.LOCATION_MODE_THROTTLE_REQUESTS_WHEN_SCREEN_OFF
            else PowerManager.LOCATION_MODE_FOREGROUND_ONLY
            val originalConstants = nullableString(previousSettings, "battery_saver_constants")
            val constants = withLocationMode(originalConstants, modeApplied)
            assertTrue(Settings.Global.putString(resolver, "battery_saver_constants", constants))
            if (nativeSaverSupported) awaitState("Política de localização não observada pela API") {
                power.locationPowerSaveMode == modeApplied
            } else assertEquals(PowerManager.LOCATION_MODE_NO_CHANGE, power.locationPowerSaveMode)
            results.put("locationModeBeforeProbe", modeBefore)
                .put("locationModeApplied", modeApplied)
                .put("locationModeObserved", power.locationPowerSaveMode)
                .put("policyControlValidated", nativeSaverSupported && power.locationPowerSaveMode == modeApplied)

            val locationApplied = !session.getBoolean("locationEnabledBefore")
            assertTrue(Settings.Secure.putInt(resolver, "location_mode", if (locationApplied) 3 else 0))
            awaitState("Localização global não observada pelo serviço") { location.isLocationEnabled == locationApplied }
            results.put("locationEnabledBefore", session.getBoolean("locationEnabledBefore"))
                .put("locationEnabledApplied", locationApplied)
                .put("locationEnabledObserved", location.isLocationEnabled)
                .put("globalLocationControlValidated", true)

            if (InstrumentationRegistry.getArguments().getString("interruptAfterApply") == "true") {
                // Injeção de morte real do processo: demonstra recuperação do arquivo persistido.
                writeAtomic(report, results.put("pendingRecovery", true).put("passed", false))
                Process.killProcess(Process.myPid())
                error("A interrupção de laboratório não encerrou o processo.")
            }

            setPermission(false)
            expectWriteDenied()
            results.put("writeDeniedAfterRevoke", true)
        } catch (error: Throwable) {
            failure = error
            results.put("failure", error.javaClass.simpleName)
        } finally {
            try {
                restore(session)
                results.put("settingsRestored", true).put("runtimeStateRestored", true)
                    .put("batterySimulationReset", true).put("permissionRevoked", !hasPermission())
            } catch (restoreFailure: Throwable) {
                results.put("restoreFailure", restoreFailure.javaClass.simpleName)
                if (failure == null) failure = restoreFailure else failure.addSuppressed(restoreFailure)
            }
            results.put("passed", failure == null)
            writeAtomic(report, results)
        }
        failure?.let { throw it }
    }

    private fun requireReservedEmulator() {
        check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Prova exclusiva de emulador." }
        check(context.packageName == "dev.brenopoubel.watchreserve.lab") { "Pacote divergente." }
        check(Process.myUid() == context.applicationInfo.uid && Process.myUid() >= 10_000) {
            "A prova deve escrever com o UID comum do APK."
        }
        check(context.resources.configuration.isScreenRound &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)) { "AVD Wear circular obrigatório." }
        check(InstrumentationRegistry.getArguments().getString("reservedAvd") == "WatchReserve_Wear5_Small_Round") {
            "Execute pelo roteiro reservado, com o argumento de laboratório."
        }
    }

    private fun hasPermission() = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
        PackageManager.PERMISSION_GRANTED

    private fun setPermission(grant: Boolean) {
        shell("pm ${if (grant) "grant" else "revoke"} ${context.packageName} android.permission.WRITE_SECURE_SETTINGS")
        assertEquals(grant, hasPermission())
    }

    private fun expectWriteDenied() {
        assertFalse(hasPermission())
        var denied = false
        try {
            val current = Settings.Global.getString(resolver, "battery_saver_constants")
            Settings.Global.putString(resolver, "battery_saver_constants", current)
        } catch (_: SecurityException) { denied = true }
        assertTrue("Escrita sem permissão não foi bloqueada.", denied)
    }

    private fun restore(session: JSONObject) {
        check(session.getInt("schema") == 2 && session.getString("package") == context.packageName)
        check(session.has("secureLocationMode") && session.has("locationEnabledBefore"))
        val previous = session.getJSONObject("settings")
        check(keys.all { previous.has(it) }) { "Sessão incompleta; recuperação manual necessária." }
        val errors = mutableListOf<Throwable>()
        try {
            try { setPermission(true) } catch (error: Throwable) { errors.add(error) }
            // Desativa primeiro para não reaplicar efeitos da política durante a limpeza.
            try {
                check(Settings.Global.putInt(resolver, "low_power", 0))
                awaitState("Falha ao desativar economia para restaurar") { !power.isPowerSaveMode }
            } catch (error: Throwable) { errors.add(error) }
            for (key in keys.filter { it != "low_power" }) {
                try { check(Settings.Global.putString(resolver, key, nullableString(previous, key))) }
                catch (error: Throwable) { errors.add(error) }
            }
            try { check(Settings.Global.putString(resolver, "low_power", nullableString(previous, "low_power"))) }
            catch (error: Throwable) { errors.add(error) }
            try { check(Settings.Secure.putString(resolver, "location_mode", nullableString(session, "secureLocationMode"))) }
            catch (error: Throwable) { errors.add(error) }
            awaitState("Estado efetivo não restaurado") {
                power.isPowerSaveMode == session.getBoolean("powerSaveBefore") &&
                    power.locationPowerSaveMode == session.getInt("locationModeBefore") &&
                    location.isLocationEnabled == session.getBoolean("locationEnabledBefore")
            }
        } catch (error: Throwable) { errors.add(error) }
        finally {
            try { shell("dumpsys battery reset"); check(!batterySimulated()) }
            catch (error: Throwable) { errors.add(error) }
            try { setPermission(false) } catch (error: Throwable) { errors.add(error) }
        }
        if (Settings.Secure.getString(resolver, "location_mode") != nullableString(session, "secureLocationMode")) {
            errors.add(IllegalStateException("Localização não restaurada."))
        }
        for (key in keys) {
            if (Settings.Global.getString(resolver, key) != nullableString(previous, key)) {
                errors.add(IllegalStateException("Valor não restaurado: $key"))
            }
        }
        try {
            awaitState("Estado mudou após remover a simulação") {
                power.isPowerSaveMode == session.getBoolean("powerSaveBefore") &&
                    power.locationPowerSaveMode == session.getInt("locationModeBefore") &&
                    location.isLocationEnabled == session.getBoolean("locationEnabledBefore")
            }
        } catch (error: Throwable) { errors.add(error) }
        if (errors.isNotEmpty()) {
            throw IllegalStateException("Restauração incompleta; sessão preservada.").also { error ->
                errors.forEach(error::addSuppressed)
            }
        }
        journal.delete()
        check(!journal.baseFile.exists()) { "Não foi possível excluir a sessão restaurada." }
    }

    private fun withLocationMode(raw: String?, mode: Int): String {
        val fields = raw.orEmpty().split(',').filter { it.isNotBlank() }
        check(fields.all { '=' in it }) { "Política original inválida; não sobrescrever." }
        return (fields.filterNot { it.substringBefore('=').trim() == "location_mode" } + "location_mode=$mode")
            .joinToString(",")
    }

    @SuppressLint("DiscouragedApi") // Recurso interno consultado apenas para caracterizar a imagem de laboratório.
    private fun nativeSaverSupported(): Boolean {
        val id = context.resources.getIdentifier("config_batterySaverSupported", "bool", "android")
        check(id != 0) { "Capacidade AOSP desconhecida; não inferir compatibilidade." }
        return context.resources.getBoolean(id)
    }

    private fun nullableString(json: JSONObject, key: String): String? =
        if (json.isNull(key)) null else json.getString(key)

    private fun batterySimulated(): Boolean = shell("dumpsys battery")
        .contains("UPDATES STOPPED", ignoreCase = true)

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .bufferedReader().use { it.readText() }

    private fun awaitState(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 5_000
        while (!condition() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50)
        check(condition()) { message }
    }

    private fun writeAtomic(file: AtomicFile, json: JSONObject) {
        val content = json.toString(2).toByteArray(Charsets.UTF_8)
        val stream = file.startWrite()
        try { stream.write(content); stream.fd.sync(); file.finishWrite(stream) }
        catch (error: Throwable) { file.failWrite(stream); throw error }
        check(file.readFully().contentEquals(content)) { "Registro não confirmado; não prosseguir." }
    }
}
