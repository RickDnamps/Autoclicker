package com.rickdnamps.autoclicker

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/** Helpers to check the accessibility service and open the right system screens. */
object SystemSetup {

    /** Android 13+ blocks accessibility for sideloaded apps until "restricted settings" are allowed. */
    val hasRestrictedSettings: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private fun serviceComponent(context: Context) =
        ComponentName(context, AutoClickService::class.java)

    fun isServiceEnabled(context: Context): Boolean {
        if (AutoClickService.instance != null) return true
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val component = serviceComponent(context)
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it) == component
        }
    }

    /** Opens AutoClicker's own accessibility page, or the accessibility list as a fallback. */
    fun openServiceSettings(context: Context) {
        val component = serviceComponent(context).flattenToString()
        val details = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS")
            .putExtra(Intent.EXTRA_COMPONENT_NAME, component)
        // Highlights AutoClicker in the list on most devices.
        val list = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .putExtra(":settings:fragment_args_key", component)
            .putExtra(":settings:show_fragment_args", Bundle().apply {
                putString(":settings:fragment_args_key", component)
            })
        if (!tryStart(context, details)) tryStart(context, list)
    }

    fun openAppInfo(context: Context) {
        tryStart(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
        )
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}

/** Pads this view so its content stays clear of the system bars and the keyboard. */
fun View.applySystemBarsPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.updatePadding(
            left = bars.left,
            top = bars.top,
            right = bars.right,
            bottom = maxOf(bars.bottom, ime.bottom),
        )
        insets
    }
}
