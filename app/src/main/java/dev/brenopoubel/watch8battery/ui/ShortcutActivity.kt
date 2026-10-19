package dev.brenopoubel.watch8battery.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.brenopoubel.watch8battery.complication.WatchOnlyComplicationService
import dev.brenopoubel.watch8battery.core.ShortcutRouter

/** Entrada comum de Home, tile e complicação. Não interpreta parâmetros externos. */
class ShortcutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WatchOnlyComplicationService.refresh(this)
        if (ShortcutRouter.openWatchOnly(this)) {
            finish()
        } else {
            setContent {
                WearScreen("Só relógio") {
                    Note("Não foi possível abrir as configurações. No relógio, procure Ajustes → Bateria → Só relógio.")
                    Action("Central de economia") {
                        startActivity(Intent(this@ShortcutActivity, PanelActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}
