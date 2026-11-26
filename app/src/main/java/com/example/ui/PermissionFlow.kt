package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import com.example.ui.oneui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.ui.oneui.OneUIDialog

/**
 * First-launch permission explainer: location. It is asked once with a formal explanation;
 * declining is final (never asked again automatically) and the app keeps working with its
 * fallback (manual city).
 * The all-files/wallpaper step was removed in 3.6 together with the MANAGE_EXTERNAL_STORAGE
 * permission: the real-wallpaper read it unlocked is blocked by the system on Android 14+
 * regardless, so a cosmetic effect cannot justify a Play-restricted permission. Widget
 * backgrounds fall back to the permissionless wallpaper-colour mesh (the Frosted look).
 */
object PermissionFlow {
    private const val PREFS = "app_prefs"
    private const val KEY_DONE = "perm_flow_done"

    fun hasLocation(c: Context) =
        c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @Composable
    fun Host() {
        val ctx = LocalContext.current
        val sp = remember { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
        // stage persisted in prefs so an activity recreation (e.g. after the system location prompt) resumes at the right step
        // 1 = location pending, 3 = finished (never asked again)
        fun initial(): Int {
            if (sp.getInt("perm_stage", if (sp.getBoolean(KEY_DONE, false)) 3 else 1) >= 3) return 0
            return if (!hasLocation(ctx)) 1 else 0
        }
        var step by remember { mutableIntStateOf(initial().also { android.util.Log.i("PermFlow", "initial step=$it") }) }
        fun finish() { sp.edit().putInt("perm_stage", 3).putBoolean(KEY_DONE, true).apply(); step = 0 }
        val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { finish() }
        val dir = if (L10n.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(LocalLayoutDirection provides dir) {
            if (step == 1) {
                OneUIDialog(
                    title = L10n.t(S.PermLocTitle),
                    onDismiss = { finish() },
                    onConfirm = {
                        try { locLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) } catch (_: Exception) { finish() }
                    },
                    confirmText = L10n.t(S.PermAllow),
                    dismissText = L10n.t(S.PermNotNow)
                ) { Body(L10n.t(S.PermLocBody)) }
            }
        }
    }

    @Composable
    private fun Body(text: String) {
        Column {
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp, textAlign = TextAlign.Start)
        }
    }
}
