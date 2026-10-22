package dev.brenopoubel.watchreserve.advancedlab

import android.os.Build
import android.os.Process
import android.os.SystemClock
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.brenopoubel.watchreserve.advancedlab.core.LabViewModel
import dev.brenopoubel.watchreserve.advancedlab.core.ReadQuery
import dev.brenopoubel.watchreserve.advancedlab.service.ReadOnlyService
import dev.brenopoubel.watchreserve.advancedlab.ui.LabActivity
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipFile

/** Prova de integração: requer ponte previamente autorizada; não altera controles do sistema. */
@RunWith(AndroidJUnit4::class)
class BridgeEmulatorTest {
    @get:Rule val compose = createAndroidComposeRule<LabActivity>()
    private val model: LabViewModel get() = ViewModelProvider(compose.activity)[LabViewModel::class.java]

    @Test fun quickReadUsesShellAndProducesPrivateArchive() {
        compose.waitUntil(10_000) { model.state.canCollect }
        compose.onNodeWithText("Somente leitura").assertIsDisplayed()
        screenshot("home")
        val previous = model.state.report
        compose.onNodeWithText("Leitura rápida").performScrollTo().performClick()
        compose.waitUntil(20_000) { !model.state.busy }
        assertNotNull(model.state.message, model.state.report)
        assertNotEquals(model.state.message, previous, model.state.report)
        val report = checkNotNull(model.state.report)
        ZipFile(report).use { zip ->
            val summary = JSONObject(zip.getInputStream(zip.getEntry("summary.json")).bufferedReader().use { it.readText() })
            assertEquals(2000, summary.getInt("uid"))
            assertEquals(Process.myUid(), summary.getInt("ownerUid"))
            assertEquals(2, summary.getJSONArray("queries").length())
            assertNotNull(zip.getEntry("power.txt"))
            assertNotNull(zip.getEntry("processes.txt"))
        }
        assertTrue(report.canonicalPath.startsWith(compose.activity.noBackupFilesDir.canonicalPath + File.separator))
        compose.onNodeWithText("Exportar relatório").performScrollTo().assertIsDisplayed()
        assertHelperStopped()
        screenshot("report")
    }

    @Test fun cancellationWhileBindingLeavesNoHelperOrPendingReport() {
        compose.waitUntil(10_000) { model.state.canCollect }
        compose.activityRule.scenario.onActivity { activity ->
            ViewModelProvider(activity)[LabViewModel::class.java].apply { collect(); cancel() }
        }
        compose.waitUntil(10_000) { !model.state.busy }
        assertTrue(File(compose.activity.noBackupFilesDir, "diagnostics").listFiles().orEmpty().none { it.extension == "pending" })
        assertHelperStopped()
    }

    @Test fun fullReadReportsEveryQueryAndStopsHelper() {
        compose.waitUntil(10_000) { model.state.canCollect }
        val previous = model.state.report
        compose.onNodeWithText("Diagnóstico completo").performScrollTo().performClick()
        compose.waitUntil(160_000) { !model.state.busy }
        assertNotNull(model.state.message, model.state.report)
        assertNotEquals(model.state.message, previous, model.state.report)
        ZipFile(checkNotNull(model.state.report)).use { zip ->
            val summary = JSONObject(zip.getInputStream(zip.getEntry("summary.json")).bufferedReader().use { it.readText() })
            assertEquals(2000, summary.getInt("uid"))
            assertEquals(ReadQuery.entries.size, summary.getJSONArray("queries").length())
            ReadQuery.entries.forEach { assertNotNull(zip.getEntry("${it.name.lowercase()}.txt")) }
        }
        assertHelperStopped()
    }

    @Test fun ordinaryClientCannotImpersonateShellService() {
        assertNotEquals(2000, Process.myUid())
        assertThrows(IllegalStateException::class.java) { ReadOnlyService(compose.activity).identity() }
    }

    @Test fun recreationPreservesUiAndDoesNotStartCollection() {
        compose.waitUntil(10_000) { model.state.canCollect }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Somente leitura").assertIsDisplayed()
        assertFalse(model.state.busy)
        assertHelperStopped()
    }

    private fun assertHelperStopped() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        repeat(20) {
            val running = automation.executeShellCommand("ps -A -o NAME").use { descriptor ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
            }
            if (!running.contains("${BuildConfig.APPLICATION_ID}:diagnostic")) return
            SystemClock.sleep(100)
        }
        fail("O processo de leitura permaneceu ativo depois da operação.")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val directory = File(compose.activity.filesDir, "review-screenshots").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    companion object {
        @JvmStatic @BeforeClass fun emulatorOnly() {
            assertEquals("Informe o AVD reservado pelo roteiro de teste.", "WatchReserve_Wear5_Small_Round",
                InstrumentationRegistry.getArguments().getString("reservedAvd"))
            assertTrue("Instrumentação reservada ao AVD.", Build.HARDWARE in setOf("ranchu", "goldfish"))
            assertTrue(InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.isScreenRound)
        }
    }
}
