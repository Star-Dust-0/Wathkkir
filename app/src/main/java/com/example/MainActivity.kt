package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.lockwidgets.HomeWidgets
import com.example.prayer.alerts.PrayerAlertScheduler
import com.example.ui.Onboarding
import com.example.ui.PermissionFlow
import com.example.ui.ShortcutNav
import com.example.ui.screens.MainScreen
import com.example.ui.theme.FontChoice
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.Digits
import com.example.ui.i18n.L10n
import com.example.utils.NotifPrefs
import com.example.widget.WidgetUpdater

/**
 * Single activity. Rebuilt from the original 3.4.2 MainActivity: handles the wathakker://open?go= deep link,
 * the launcher shortcuts, the widget "open prayer" action and the dhikr notification extra, then shows
 * onboarding, the permission flow, or the main screen.
 */
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private fun handle(intent: Intent?) {
        HomeWidgets.handleIntent(intent)
        ShortcutNav.fromIntent(intent)
        try { ShortcutNav.publish(applicationContext) } catch (_: Throwable) {}
        val id = intent?.getIntExtra("DHIKR_ID", -1) ?: -1
        if (id != -1) viewModel.selectDhikrFromNotification(id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(if (HomeWidgets.handleIntent(intent)) null else savedInstanceState)
        L10n.init(this)
        ThemeSettings.setMode(getSharedPreferences("app_prefs", MODE_PRIVATE).getString("theme_mode", "auto") ?: "auto")
        Digits.init(this)
        WidgetUpdater.INSTANCE.updateAll(this)
        PrayerAlertScheduler.INSTANCE.scheduleAll(this)
        handle(intent)
        enableEdgeToEdge()
        setContent { Root() }
    }

    @Composable
    private fun Root() {
        val fontId by viewModel.appFont.collectAsState()
        MyApplicationTheme(font = FontChoice.fromId(fontId)) {
            var onboarding by remember { mutableStateOf(Onboarding.isPending(this)) }
            if (onboarding) {
                Onboarding.Host { onboarding = false }
            } else {
                MainScreen(viewModel)
                PermissionFlow.Host()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
        if (HomeWidgets.handleIntent(intent)) { setIntent(intent); recreate() }
    }

    override fun onResume() {
        super.onResume()
        NotifPrefs.refreshChannels(this)
    }
}
