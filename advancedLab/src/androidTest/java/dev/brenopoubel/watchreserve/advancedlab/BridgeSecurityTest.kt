package dev.brenopoubel.watchreserve.advancedlab

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BridgeSecurityTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun ordinaryClientCannotLaunchGrantScreen() {
        assertThrows(SecurityException::class.java) {
            context.startActivity(Intent().setComponent(ComponentName(BRIDGE, "$BRIDGE.PermissionActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("uid", android.os.Process.myUid()))
        }
    }

    @Test fun ordinaryClientCannotDeliverPrivilegedBinder() {
        assertThrows(SecurityException::class.java) {
            context.contentResolver.call(Uri.parse("content://$BRIDGE.shizuku"), "sendBinder", null, Bundle())
        }
    }

    companion object {
        private const val BRIDGE = "dev.brenopoubel.watchreserve.bridge"
        @JvmStatic @BeforeClass fun guard() {
            assertEquals("WatchReserve_Wear5_Small_Round", InstrumentationRegistry.getArguments().getString("reservedAvd"))
            assertTrue(Build.HARDWARE in setOf("ranchu", "goldfish"))
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            assertTrue(context.resources.configuration.isScreenRound)
            assumeTrue("Caso aplicável somente à ponte Wear.",
                runCatching { context.packageManager.getPackageInfo(BRIDGE, 0) }.isSuccess)
        }
    }
}
