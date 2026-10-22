package dev.brenopoubel.watchreserve.advancedlab

import android.app.Application
import android.os.Build
import android.os.SystemClock
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.brenopoubel.watchreserve.advancedlab.core.*
import dev.brenopoubel.watchreserve.advancedlab.ui.LabActivity
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PreparedSessionTest {
    @get:Rule val compose = createAndroidComposeRule<LabActivity>()
    private val model get() = ViewModelProvider(compose.activity)[LabViewModel::class.java]
    private val store get() = ReadSessionStore(compose.activity)

    @Before fun startWithoutPendingReadRecipe() {
        compose.waitUntil(10_000) { model.state.canCollect }
        compose.activityRule.scenario.onActivity { model.discardSession() }
    }

    @Test fun preparationPersistsWithoutStartingHelperOrReplacingReport() {
        val report = model.state.report
        prepare()
        val recipe = checkNotNull(store.load())
        assertEquals(ReadPhase.PREPARED, recipe.phase)
        assertEquals(Build.FINGERPRINT, recipe.environment.fingerprint)
        assertEquals(BuildConfig.VERSION_CODE.toLong(), recipe.environment.clientVersion)
        assertEquals(report, model.state.report)
        compose.activityRule.scenario.recreate()
        assertEquals(recipe, store.load())
        assertTrue(model.state.sessionPrepared)
        compose.onNodeWithText("Executar sessão").performScrollTo()
        screenshot("session-prepared")
        assertNoHelper()
    }

    @Test fun cancelDuringLocalDelayRecordsCancellationWithoutCommands() {
        prepare()
        compose.activityRule.scenario.onActivity { model.collect(prepared = true) }
        compose.waitUntil(5_000) { store.load()?.phase == ReadPhase.DELAYED }
        compose.activityRule.scenario.onActivity { model.cancel() }
        compose.waitUntil(5_000) { !model.state.busy }
        assertEquals(ReadPhase.CANCELLED, store.load()?.phase)
        assertFalse(model.state.recordError)
        assertNoHelper()
    }

    @Test fun changedBootRejectsRecipeBeforeStartingHelper() {
        prepare()
        val recipe = checkNotNull(store.load())
        store.save(recipe.copy(environment = recipe.environment.copy(bootCount = recipe.environment.bootCount + 1)))
        compose.activityRule.scenario.onActivity { model.collect(prepared = true) }
        compose.waitUntil(5_000) { !model.state.busy }
        assertEquals(ReadPhase.INVALID, store.load()?.phase)
        assertTrue(model.state.message.contains("ambiente mudou"))
        assertNoHelper()
    }

    @Test fun totalBudgetInterruptsDelayedSessionAndRecordsTimeout() {
        val owner = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        lateinit var shortBudget: LabViewModel
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LabViewModel(compose.activity.application as Application, 250) as T
            }
            shortBudget = ViewModelProvider(owner, factory)[LabViewModel::class.java]
            shortBudget.prepareSession()
        }
        try {
            compose.waitUntil(5_000) { shortBudget.state.sessionPrepared }
            instrumentation.runOnMainSync { shortBudget.collect(prepared = true) }
            compose.waitUntil(5_000) { !shortBudget.state.busy }
            assertEquals(ReadPhase.TIMED_OUT, store.load()?.phase)
            assertFalse(shortBudget.state.recordError)
            assertNoHelper()
        } finally { instrumentation.runOnMainSync { owner.viewModelStore.clear() } }
    }

    private fun prepare() {
        compose.activityRule.scenario.onActivity { model.prepareSession() }
        compose.waitUntil(5_000) { !model.state.busy && model.state.sessionPrepared }
    }

    private fun assertNoHelper() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        repeat(30) {
            val output = automation.executeShellCommand("ps -A -o NAME").use {
                android.os.ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().use { reader -> reader.readText() }
            }
            if (!output.contains("${BuildConfig.APPLICATION_ID}:diagnostic")) return
            SystemClock.sleep(100)
        }
        fail("Helper de leitura permaneceu em execução.")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val directory = File(compose.activity.filesDir, "review-screenshots").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @After fun closeReadFixture() {
        compose.activityRule.scenario.onActivity { model.cancel() }
        compose.waitUntil(5_000) { !model.state.busy }
        compose.activityRule.scenario.onActivity { model.discardSession() }
    }

    companion object {
        @JvmStatic @BeforeClass fun guard() {
            assertEquals("WatchReserve_Wear5_Small_Round", InstrumentationRegistry.getArguments().getString("reservedAvd"))
            assertTrue(Build.HARDWARE in setOf("ranchu", "goldfish"))
            assertTrue(InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.isScreenRound)
        }
    }
}
