package dev.brenopoubel.watchreserve.advancedlab.ui

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.brenopoubel.watchreserve.advancedlab.core.LabViewModel
import kotlinx.coroutines.launch

class LabActivity : ComponentActivity() {
    private lateinit var model: LabViewModel
    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {
        if (it != null) model.export(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[LabViewModel::class.java]
        setContent {
            MaterialTheme {
                val state = model.state
                val scroll = rememberScrollState()
                val focus = remember { FocusRequester() }
                val scope = rememberCoroutineScope()
                LaunchedEffect(state.collecting) {
                    if (state.collecting) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                LaunchedEffect(Unit) { focus.requestFocus() }
                LaunchedEffect(state.status, state.message) { scroll.animateScrollTo(0) }
                Column(Modifier.fillMaxSize().background(Color.Black)
                    .onRotaryScrollEvent { event -> scope.launch { scroll.scrollBy(event.verticalScrollPixels) }; true }
                    .focusRequester(focus).focusable().verticalScroll(scroll)
                    .padding(horizontal = 26.dp, vertical = 38.dp), Arrangement.spacedBy(10.dp)) {
                    Text("WatchReserve", fontSize = 16.sp)
                    Text("Somente leitura", fontSize = 12.sp, color = Color(0xFF80D4FF))
                    if (state.sessionPrepared) Button(onClick = { model.collect(prepared = true) },
                        enabled = state.canCollect && !state.busy, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Executar sessão", fontSize = 14.sp) })
                    if (state.collecting) Button(onClick = { model.cancel() }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Cancelar coleta", fontSize = 14.sp) })
                    if ((state.sessionPrepared || state.recordError) && !state.collecting) Button(onClick = { model.discardSession() },
                        enabled = !state.busy, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Descartar registro", fontSize = 14.sp) })
                    Text(state.status, fontSize = 13.sp)
                    if (state.message.isNotBlank()) Text(state.message,
                        Modifier.semantics { liveRegion = LiveRegionMode.Polite }, fontSize = 13.sp)
                    if (state.canAuthorize) Button(onClick = { model.authorize() }, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(), label = { Text("Autorizar acesso", fontSize = 14.sp) })
                    if (state.sessionStatus.isNotBlank()) Text(state.sessionStatus, fontSize = 13.sp)
                    if (!state.sessionPrepared && !state.collecting) {
                        Button(onClick = { model.collect(quick = true) }, enabled = state.canCollect && !state.busy,
                            modifier = Modifier.fillMaxWidth(), label = { Text("Leitura rápida", fontSize = 14.sp) })
                        Button(onClick = { model.collect() }, enabled = state.canCollect && !state.busy,
                            modifier = Modifier.fillMaxWidth(), label = { Text("Diagnóstico completo", fontSize = 14.sp) })
                        Button(onClick = { model.prepareSession() }, enabled = state.canCollect && !state.busy,
                            modifier = Modifier.fillMaxWidth(), label = { Text("Preparar sessão offline", fontSize = 14.sp) })
                    }
                    Text("Após conectar, a coleta aguarda 5 segundos. O prazo total é de 3 minutos. Mantenha este painel aberto; a tela fica acesa durante a coleta.", fontSize = 12.sp)
                    Text("Relatórios privados com alarmes, apps e rede. Exportação só por sua escolha.", fontSize = 12.sp)
                    Button(onClick = { try { export.launch("watchreserve-diagnostico.zip") }
                        catch (_: ActivityNotFoundException) { model.exportUnavailable() } },
                        enabled = state.report != null && !state.busy,
                        modifier = Modifier.fillMaxWidth(), label = { Text("Exportar relatório", fontSize = 14.sp) })
                    Button(onClick = { model.refresh() }, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(), label = { Text("Atualizar estado", fontSize = 14.sp) })
                    Text("Os controles de economia ainda não estão habilitados neste laboratório.", fontSize = 13.sp)
                }
            }
        }
    }

    override fun onResume() { super.onResume(); if (::model.isInitialized) model.refresh() }
}
