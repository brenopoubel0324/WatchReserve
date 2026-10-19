package dev.brenopoubel.watch8battery.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.brenopoubel.watch8battery.complication.WatchOnlyComplicationService
import dev.brenopoubel.watch8battery.core.BatteryMeasurement
import dev.brenopoubel.watch8battery.core.BatterySample
import dev.brenopoubel.watch8battery.core.DeviceBattery
import dev.brenopoubel.watch8battery.core.MeasurementStore
import dev.brenopoubel.watch8battery.core.RouteProof
import dev.brenopoubel.watch8battery.core.RouteStore
import dev.brenopoubel.watch8battery.core.ShortcutRouter
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class PanelActivity : ComponentActivity() {
    private enum class Page { HOME, DESTINATION, MEASUREMENT }

    private lateinit var routeStore: RouteStore
    private lateinit var measurementStore: MeasurementStore
    private var page by mutableStateOf(Page.HOME)
    private var messageText by mutableStateOf("")
    private var messageEvent by mutableLongStateOf(0L)
    private var message: String
        get() = messageText
        set(value) {
            messageText = value
            messageEvent += 1
        }
    private var componentText by mutableStateOf("")
    private var route by mutableStateOf<RouteProof?>(null)
    private var battery by mutableStateOf<BatterySample?>(null)
    private var measurementStart by mutableStateOf<BatterySample?>(null)
    private var lastMeasurement by mutableStateOf<String?>(null)
    private var pendingTest: String? = null

    private val testDestination = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val expected = pendingTest
        pendingTest = null
        val stored = routeStore.read()
        val inspection = stored?.let { ShortcutRouter.inspect(this, it.component) }
        if (expected != null && stored != null && inspection != null && stored.component == expected &&
            stored.fingerprint == Build.FINGERPRINT && stored.settingsVersion == inspection.settingsVersion &&
            inspection.error == null
        ) {
            if (routeStore.save(stored.copy(testReturned = true, visuallyConfirmed = false))) {
                route = routeStore.read()
                message = "O teste retornou. Confirme somente se viu a tela correta de Só relógio."
            } else message = "Falha ao registrar o teste. Tente novamente."
        } else message = "Teste invalidado. Salve e teste novamente o componente atual."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        routeStore = RouteStore(this)
        measurementStore = MeasurementStore(this)
        WatchOnlyComplicationService.refresh(this)
        pendingTest = savedInstanceState?.getString("pending_test")
        page = savedInstanceState?.getString("page")?.let { name -> Page.entries.find { it.name == name } } ?: Page.HOME
        route = routeStore.read()
        componentText = savedInstanceState?.getString("component") ?: route?.component.orEmpty()
        setContent {
            BackHandler(enabled = page != Page.HOME) { page = Page.HOME; message = "" }
            WearScreen(
                when (page) {
                    Page.HOME -> "Economia"
                    Page.DESTINATION -> "Destino avançado"
                    Page.MEASUREMENT -> "Medição manual"
                },
                notice = message,
                noticeEvent = messageEvent,
            ) {
                when (page) {
                    Page.HOME -> {
                        val current = battery
                        Note(if (current != null && current.percent in 0..100) "Bateria: ${current.percent}%" else "Bateria indisponível")
                        Note(if (route?.visuallyConfirmed == true) "Atalho validado neste firmware." else "Só relógio: destino direto ainda não validado.")
                        Action("Só relógio") { startActivity(Intent(this@PanelActivity, ShortcutActivity::class.java)) }
                        Action("Economia do sistema") { openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS, "Abra Economia de energia nas configurações de bateria.") }
                        if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                            Action("Redes móveis (LTE)") { openSettings(Settings.ACTION_NETWORK_OPERATOR_SETTINGS, "Desligue o LTE quando não precisar dele. Procure Conexões → Redes móveis.") }
                        }
                        Action("Conexões") { openSettings(Settings.ACTION_WIRELESS_SETTINGS, "Revise as conexões ativas de acordo com o uso.") }
                        Action("Tela e brilho") { openSettings(Settings.ACTION_DISPLAY_SETTINGS, "Ajuste brilho, AOD, gestos e tempo de tela nas configurações de exibição.") }
                        Action("Localização") { openSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Confira se você precisa da localização durante este período.") }
                        Action("Bluetooth") { openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Ajuste o Bluetooth conforme o uso; considere também o consumo de Wi-Fi e LTE.") }
                        Action("Wi-Fi") { openSettings(Settings.ACTION_WIFI_SETTINGS, "Desligue o Wi-Fi quando não precisar da conexão.") }
                        Action("Saúde") { openHealth() }
                        Action("Medir consumo") { page = Page.MEASUREMENT; message = "" }
                        Action("Configurar Só relógio") { page = Page.DESTINATION; message = "" }
                        Note("Ajustes manuais, sem monitoramento contínuo. Você confirma as alterações nas telas do sistema.")
                        Action("Atualizar bateria") { refresh() }
                    }
                    Page.DESTINATION -> {
                        Note("Avançado: obtenha o componente da tela por ADB. Só atividades exportadas de Configurações ou Bateria do sistema são aceitas. Nenhum nome Samsung é presumido.")
                        Note("Componente (pacote/classe)")
                        BasicTextField(
                            value = componentText,
                            onValueChange = { componentText = it.take(500) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .border(1.dp, Color.Gray, RoundedCornerShape(10.dp)).padding(10.dp)
                                .semantics { contentDescription = "Componente da tela de Só relógio" },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            singleLine = true,
                        )
                        Action("Salvar candidata") { saveCandidate() }
                        val matchesSaved = route != null && componentText.trim() == route?.component
                        if (route != null && !matchesSaved) Note("Há alterações não salvas no componente.")
                        Action("Testar abertura", enabled = matchesSaved) { launchCandidate() }
                        if (route?.testReturned == true && route?.visuallyConfirmed != true) {
                            Note("Você viu a tela de Só relógio? Apenas abrir bateria ou ajustes gerais não comprova acesso direto. Preserve a confirmação nativa de ativação.")
                            Action("Confirmar tela correta", enabled = matchesSaved) { confirmCandidate() }
                        }
                        if (route?.visuallyConfirmed == true) Note("Confirmada. A prova será invalidada se o firmware ou o pacote do destino mudar.")
                        Action("Apagar destino") {
                            if (routeStore.clear()) { route = null; componentText = ""; message = "Destino removido." }
                            else message = "Não foi possível remover o destino."
                        }
                        Action("Voltar") { page = Page.HOME; message = "" }
                    }
                    Page.MEASUREMENT -> {
                        Note("Leitura sob demanda, sem sensores ou serviço ativo. Faça janelas de 24 h, sem depuração e com uso semelhante. Anote as condições fora do app.")
                        measurementStart?.let {
                            Note("Início: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it.wallMillis))}; bateria ${it.percent}%.")
                            Action("Finalizar medição") { finishMeasurement() }
                        } ?: Action("Iniciar medição") { startMeasurement() }
                        lastMeasurement?.let { Note(it) }
                        Note("Uma recarga entre leituras pode não ser detectada. Descarte manualmente se carregou, reiniciou ou mudou condições de uso. Os resultados ficam apenas neste relógio.")
                        Action("Apagar medições locais") {
                            if (measurementStore.clear()) { message = "Medições removidas."; refresh() }
                            else message = "Não foi possível remover as medições."
                        }
                        Action("Voltar") { page = Page.HOME; message = "" }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::routeStore.isInitialized) refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pending_test", pendingTest)
        outState.putString("page", page.name)
        outState.putString("component", componentText)
        super.onSaveInstanceState(outState)
    }

    private fun refresh() {
        val (_, invalidation) = ShortcutRouter.validatedDestination(this)
        if (invalidation != null) message = invalidation
        route = routeStore.read()
        val sample = DeviceBattery.read(this)
        battery = sample
        val active = measurementStore.active()
        if (active != null) {
            BatteryMeasurement.integrityFailure(active, sample)?.let { reason ->
                if (measurementStore.complete("Medição descartada: $reason")) message = reason
                else message = "Falha ao registrar a invalidação da medição."
            }
        }
        measurementStart = measurementStore.active()
        lastMeasurement = measurementStore.lastResult()
    }

    private fun saveCandidate() {
        val inspection = ShortcutRouter.inspect(this, componentText)
        if (inspection.error != null) { message = inspection.error; return }
        val candidate = RouteProof(inspection.component!!.flattenToString(), Build.FINGERPRINT, inspection.settingsVersion!!)
        if (routeStore.save(candidate)) {
            route = candidate
            componentText = candidate.component
            message = "Candidata salva. Teste a abertura pelo app antes de confirmar."
        } else message = "Falha ao salvar a candidata."
    }

    private fun launchCandidate() {
        val stored = routeStore.read() ?: return
        val inspection = ShortcutRouter.inspect(this, stored.component)
        if (inspection.error != null || stored.fingerprint != Build.FINGERPRINT || stored.settingsVersion != inspection.settingsVersion) {
            message = inspection.error ?: "Ambiente alterado. Salve novamente antes do teste."
            return
        }
        if (!routeStore.save(stored.copy(testReturned = false, visuallyConfirmed = false))) {
            message = "Falha ao preparar o teste."; return
        }
        route = routeStore.read()
        pendingTest = stored.component
        try {
            testDestination.launch(Intent().setComponent(inspection.component))
        } catch (_: ActivityNotFoundException) {
            pendingTest = null
            message = "O firmware não permitiu abrir essa atividade."
        } catch (_: SecurityException) {
            pendingTest = null
            message = "A abertura pelo UID do app foi bloqueada. O destino não será usado."
        }
    }

    private fun confirmCandidate() {
        val stored = routeStore.read() ?: return
        val inspection = ShortcutRouter.inspect(this, stored.component)
        if (!stored.testReturned || inspection.error != null || stored.fingerprint != Build.FINGERPRINT || stored.settingsVersion != inspection.settingsVersion) {
            message = "Prova incompleta ou ambiente alterado. Salve e teste novamente."; return
        }
        if (routeStore.save(stored.copy(visuallyConfirmed = true))) {
            route = routeStore.read()
            message = "Destino confirmado apenas para este firmware. Compare o número de ações com o caminho nativo."
        } else message = "Falha ao salvar a confirmação."
    }

    private fun openSettings(action: String, guidance: String) {
        if (!ShortcutRouter.openSettings(this, action, guidance)) message = guidance
    }

    private fun openHealth() {
        for (healthPackage in listOf("com.samsung.android.wear.shealth", "com.sec.android.app.shealth")) {
            val intent = packageManager.getLaunchIntentForPackage(healthPackage) ?: continue
            try { startActivity(intent); return }
            catch (_: ActivityNotFoundException) { /* Tenta o próximo pacote Samsung declarado. */ }
            catch (_: SecurityException) { /* Mantém a alternativa pública se o firmware bloquear. */ }
        }
        openSettings(Settings.ACTION_SETTINGS, "Abra Samsung Health e revise monitoramento de saúde e frequência de medições.")
    }

    private fun startMeasurement() {
        val sample = DeviceBattery.read(this)
        message = when {
            sample.charging -> "Retire o relógio do carregador antes de iniciar."
            sample.percent !in 0..100 -> "Bateria indisponível."
            measurementStore.start(sample) -> "Medição iniciada. Desligue a depuração para comparar consumo."
            else -> "Já existe uma medição ou não foi possível salvá-la."
        }
        refresh()
    }

    private fun finishMeasurement() {
        val start = measurementStore.active() ?: return
        val end = DeviceBattery.read(this)
        val result = BatteryMeasurement.finish(start, end)
        val summary = result.invalidReason?.let { "Medição descartada: $it" } ?: String.format(
            Locale.forLanguageTag("pt-BR"),
            "%s: %d%% → %d%% em %.2f h; %.2f pontos percentuais/h. Resultado descritivo; repita antes de concluir economia.",
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(end.wallMillis)),
            start.percent, end.percent, result.durationMillis / 3_600_000.0, result.pointsPerHour,
        )
        message = if (measurementStore.complete(summary)) "Medição finalizada." else "Falha ao salvar. A medição continua ativa."
        refresh()
    }
}
