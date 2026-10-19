package dev.brenopoubel.watch8battery.core

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast

data class CandidateInspection(val component: ComponentName?, val settingsVersion: Long?, val error: String?)

@SuppressLint("UseKtx") // A confirmação síncrona de commit() é necessária para liberar o destino.
class RouteStore(context: Context) {
    private val preferences = context.getSharedPreferences("validated_destination", Context.MODE_PRIVATE)

    fun read(): RouteProof? {
        val component = preferences.getString("component", null) ?: return null
        return RouteProof(
            component,
            preferences.getString("fingerprint", "").orEmpty(),
            preferences.getLong("version", -1),
            preferences.getBoolean("returned", false),
            preferences.getBoolean("confirmed", false),
        )
    }

    fun save(proof: RouteProof): Boolean = preferences.edit()
        .putString("component", proof.component)
        .putString("fingerprint", proof.fingerprint)
        .putLong("version", proof.settingsVersion)
        .putBoolean("returned", proof.testReturned)
        .putBoolean("confirmed", proof.visuallyConfirmed)
        .commit()

    fun clear(): Boolean = preferences.edit().clear().commit()
}

object ShortcutRouter {
    /** Descobre os provedores do sistema sem presumir que bateria e ajustes usam o mesmo pacote. */
    @Suppress("DEPRECATION")
    fun inspect(context: Context, flattenedComponent: String): CandidateInspection {
        val component = ComponentName.unflattenFromString(flattenedComponent.trim())
            ?: return CandidateInspection(null, null, "Use pacote/classe, obtido por ADB no seu relógio.")
        val manager = context.packageManager
        return try {
            // queryIntentActivities lista provedores reais; resolveActivity pode devolver o seletor.
            val providers = listOf(Settings.ACTION_SETTINGS, Settings.ACTION_BATTERY_SAVER_SETTINGS)
                .flatMap { action -> manager.queryIntentActivities(Intent(action), PackageManager.MATCH_DEFAULT_ONLY) }
                .map { resolved ->
                    val info = resolved.activityInfo
                    SettingsProvider(
                        info.packageName,
                        info.applicationInfo.flags and
                            (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                    )
                }
            val allowed = SettingsPackagePolicy.allowedPackages(providers)
            if (component.packageName !in allowed) {
                return CandidateInspection(null, null, "O destino deve pertencer a Configurações ou Bateria do sistema.")
            }
            val activity = manager.getActivityInfo(component, 0)
            val componentState = manager.getComponentEnabledSetting(component)
            val packageState = manager.getApplicationEnabledSetting(component.packageName)
            val disabledStates = setOf(
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED,
            )
            val error = when {
                !activity.exported -> "Essa tela não aceita abertura por outros apps."
                !activity.enabled || !activity.applicationInfo.enabled || componentState in disabledStates ||
                    packageState in disabledStates -> "Essa tela está desativada."
                activity.permission != null && context.checkSelfPermission(activity.permission) !=
                    PackageManager.PERMISSION_GRANTED -> "Essa tela exige uma permissão indisponível ao app."
                else -> null
            }
            if (error != null) CandidateInspection(null, null, error) else {
                val version = manager.getPackageInfo(component.packageName, 0).longVersionCode
                CandidateInspection(component, version, null)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            CandidateInspection(null, null, "Componente não encontrado neste firmware.")
        } catch (_: SecurityException) {
            CandidateInspection(null, null, "O sistema bloqueou a consulta desse componente.")
        }
    }

    fun validatedDestination(context: Context): Pair<ComponentName?, String?> {
        val store = RouteStore(context)
        val route = store.read() ?: return null to null
        val inspection = inspect(context, route.component)
        if (inspection.error != null || !route.isUsable(Build.FINGERPRINT, inspection.settingsVersion ?: -1)) {
            // Não transforma uma candidata ainda sem prova em um destino autorizado.
            if (route.visuallyConfirmed) {
                store.clear()
                return null to "Destino invalidado. Valide novamente após a atualização."
            }
            return null to null
        }
        return inspection.component to null
    }

    fun openWatchOnly(activity: Activity): Boolean {
        val (destination, invalidation) = validatedDestination(activity)
        if (destination != null) {
            try {
                activity.startActivity(Intent().setComponent(destination))
                return true
            } catch (_: ActivityNotFoundException) {
                RouteStore(activity).clear()
            } catch (_: SecurityException) {
                RouteStore(activity).clear()
            }
        }
        return openSettings(
            activity,
            Settings.ACTION_BATTERY_SAVER_SETTINGS,
            invalidation ?: "Só relógio ainda sem destino validado. Procure a opção nas configurações de bateria.",
        )
    }

    fun openSettings(activity: Activity, action: String, guidance: String): Boolean {
        for (candidate in listOf(action, Settings.ACTION_SETTINGS).distinct()) {
            try {
                activity.startActivity(Intent(candidate))
                Toast.makeText(activity, guidance, Toast.LENGTH_LONG).show()
                return true
            } catch (_: ActivityNotFoundException) {
                // O próximo destino é a alternativa pública documentada.
            } catch (_: SecurityException) {
                // Firmware pode bloquear inclusive uma ação pública de configurações.
            }
        }
        Toast.makeText(activity, "Configurações indisponíveis. Abra Ajustes → Bateria → Só relógio.", Toast.LENGTH_LONG).show()
        return false
    }
}
