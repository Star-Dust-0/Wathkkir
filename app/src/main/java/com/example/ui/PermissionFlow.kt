package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * First-launch permission explainer: location, then files access (real wallpaper blur).
 * Each step is asked once with a formal explanation; declining is final (never asked again
 * automatically) and the app keeps working with its fallbacks (manual city, Frosted colours).
 */
object PermissionFlow {
    private const val PREFS = "app_prefs"
    private const val KEY_DONE = "perm_flow_done"

    fun hasLocation(c: Context) =
        c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasFiles(c: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else c.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    /** Opens the system "All files access" page for this app (the only way to grant it). */
    fun requestFilesIfNeeded(a: Activity) {
        if (hasFiles(a)) return
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                a.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + a.packageName)))
            } else {
                a.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 77)
            }
        } catch (e: Exception) {
            try { a.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) } catch (_: Exception) {}
        }
    }

    @Composable
    fun Host() {
        val ctx = LocalContext.current
        val sp = remember { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
        // stage persisted in prefs so an activity recreation (e.g. after the system location prompt) resumes at the right step
        // 1 = location pending, 2 = files pending, 3 = finished (never asked again)
        fun initial(): Int {
            val st = sp.getInt("perm_stage", if (sp.getBoolean(KEY_DONE, false)) 3 else 1)
            return when {
                st >= 3 -> 0
                st == 1 -> if (!hasLocation(ctx)) 1 else if (!hasFiles(ctx)) 2 else 0
                else -> if (!hasFiles(ctx)) 2 else 0
            }
        }
        var step by remember { mutableIntStateOf(initial().also { android.util.Log.i("PermFlow", "initial step=$it") }) }
        fun finish() { sp.edit().putInt("perm_stage", 3).putBoolean(KEY_DONE, true).apply(); step = 0 }
        fun toFiles() { android.util.Log.i("PermFlow", "toFiles hasFiles=${hasFiles(ctx)}"); if (hasFiles(ctx)) finish() else { sp.edit().putInt("perm_stage", 2).apply(); step = 2 } }
        val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { toFiles() }
        val dir = if (L10n.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(LocalLayoutDirection provides dir) {
            when (step) {
                1 -> OneUIDialog(
                    title = L10n.t(S.PermLocTitle),
                    onDismiss = { toFiles() },
                    onConfirm = {
                        try { locLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) } catch (_: Exception) { toFiles() }
                    },
                    confirmText = L10n.t(S.PermAllow),
                    dismissText = L10n.t(S.PermNotNow)
                ) { Body(L10n.t(S.PermLocBody)) }
                2 -> OneUIDialog(
                    title = L10n.t(S.PermFilesTitle),
                    onDismiss = { finish() },
                    onConfirm = {
                        val act = ctx as? Activity
                        finish()
                        if (act != null) requestFilesIfNeeded(act)
                    },
                    confirmText = L10n.t(S.PermOpenSettings),
                    dismissText = L10n.t(S.PermNotNow)
                ) { Body(L10n.t(S.PermFilesBody)) }
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
