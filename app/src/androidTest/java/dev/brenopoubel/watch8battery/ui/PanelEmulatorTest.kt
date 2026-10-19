package dev.brenopoubel.watch8battery.ui

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Executar somente no AVD reservado: estas verificações não validam firmware Samsung. */
@RunWith(AndroidJUnit4::class)
class PanelEmulatorTest {
    @get:Rule val compose = createAndroidComposeRule<PanelActivity>()

    @Test fun panelStartsWithVisibleShortcutOnRoundScreen() {
        assertTrue(compose.activity.resources.configuration.isScreenRound)
        compose.onNodeWithText("Economia").assertIsDisplayed()
        compose.onNodeWithText("Só relógio").assertIsDisplayed()
        compose.onNodeWithText("Atalho não validado.").assertExists()
        screenshot("home")
    }

    @Test fun backReturnsFromMeasurementAndPageSurvivesRecreation() {
        compose.onNodeWithText("Medir consumo").performScrollTo().performClick()
        compose.onNodeWithText("Medição manual").assertIsDisplayed()
        compose.onNodeWithText("Iniciar medição").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Medição manual").assertIsDisplayed()
        screenshot("measurement")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithText("Economia").assertIsDisplayed()
        compose.onNodeWithText("Só relógio").assertIsDisplayed()
    }

    @Test fun invalidCandidateShowsFeedbackAndCannotBeTested() {
        compose.onNodeWithText("Configurar Só relógio").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Componente da tela de Só relógio")
            .performScrollTo().performTextInput("invalido")
        compose.activityRule.scenario.onActivity { activity ->
            activity.getSystemService(InputMethodManager::class.java)
                .hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        compose.waitForIdle()
        repeat(2) {
            compose.onNodeWithText("Salvar candidata").performScrollTo().performClick()
            compose.onNodeWithText("Use pacote/classe, obtido por ADB no seu relógio.").assertIsDisplayed()
        }
        compose.onNodeWithText("Testar abertura").assertIsNotEnabled()
        screenshot("destination-error")
    }

    @Test fun rotaryInputScrollsTheFocusedPanel() {
        val now = SystemClock.uptimeMillis()
        val properties = arrayOf(MotionEvent.PointerProperties().apply { id = 0 })
        val coordinates = arrayOf(MotionEvent.PointerCoords().apply {
            setAxisValue(MotionEvent.AXIS_SCROLL, -12f)
        })
        val event = MotionEvent.obtain(
            now, now, MotionEvent.ACTION_SCROLL, 1, properties, coordinates,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_ROTARY_ENCODER, 0,
        )
        try {
            assertTrue(InstrumentationRegistry.getInstrumentation().uiAutomation.injectInputEvent(event, true))
        } finally {
            event.recycle()
        }
        compose.waitForIdle()
        compose.onNodeWithText("Economia").assertIsNotDisplayed()
        screenshot("rotary-scroll")
    }

    private fun screenshot(name: String) {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir, "audit-screenshots").apply { mkdirs() }
        // Sincroniza o desenho por PixelCopy antes de capturar a tela completa do Android,
        // incluindo sua máscara circular. A superfície Compose isolada não inclui essa máscara.
        compose.onRoot().captureToImage().asAndroidBitmap().recycle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        SystemClock.sleep(100)
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        bitmap.recycle()
    }

    companion object {
        @BeforeClass @JvmStatic fun requireEmulator() {
            check(Build.HARDWARE in setOf("ranchu", "goldfish")) {
                "Estes testes são exclusivos de emulador; não executar em relógio pessoal."
            }
        }
    }
}
