package dev.brenopoubel.watch8battery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch

@Composable
internal fun WearScreen(
    title: String,
    notice: String = "",
    noticeEvent: Long = 0,
    content: @Composable ColumnScope.() -> Unit,
) {
    MaterialTheme {
        key(title) {
            val scrollState = rememberScrollState()
            val scope = rememberCoroutineScope()
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            LaunchedEffect(notice, noticeEvent) {
                if (notice.isNotBlank()) scrollState.animateScrollTo(0)
            }
            Column(
                modifier = Modifier.fillMaxSize().background(Color.Black)
                    .onRotaryScrollEvent { event ->
                        scope.launch { scrollState.scrollBy(event.verticalScrollPixels) }
                        true
                    }
                    .focusRequester(focusRequester).focusable()
                    .verticalScroll(scrollState).padding(horizontal = 26.dp, vertical = 38.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (notice.isNotBlank()) {
                    Note(notice, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                content()
            }
        }
    }
}

@Composable
internal fun Action(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(), label = { Text(label) })
}

@Composable
internal fun Note(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 13.sp, color = Color(0xFFD5D9E0))
}
