package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.widget.WidgetUpdater
import com.example.lockwidgets.HomeWidgets

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        com.example.utils.WearSync.push(this)
        com.example.utils.NotifPrefs.refreshChannels(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // A widget tap that targets a specific tab starts the activity fresh, without saved state.
        val state = if (HomeWidgets.handleIntent(intent)) null else savedInstanceState
        super.onCreate(state)
        ThemeSettings.init(this)
        com.example.utils.Digits.init(this)
        WidgetUpdater.INSTANCE.updateAll(this)
        com.example.prayer.alerts.PrayerAlertScheduler.scheduleAll(this)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val appFont by mainViewModel.appFont.collectAsState()
            val dark = ThemeSettings.isDark()
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
            var onb by remember { mutableStateOf(com.example.ui.Onboarding.isPending(this@MainActivity)) }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33 && !onb) {
                    try {
                        launcher.launch("android.permission.POST_NOTIFICATIONS")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            MyApplicationTheme(fontChoice = appFont, darkTheme = dark, dynamicColor = false) {
                MainScreen(mainViewModel)
                if (onb) com.example.ui.Onboarding.Host { onb = false } else com.example.ui.PermissionFlow.Host()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        if (HomeWidgets.handleIntent(intent)) {
            setIntent(intent)
            recreate()
        }
    }

    private fun handleIntent(intent: Intent?) {
        HomeWidgets.handleIntent(intent)
        com.example.ui.ShortcutNav.fromIntent(intent)
        com.example.ui.ShortcutNav.publish(this)
        val dhikrId = intent?.getIntExtra("DHIKR_ID", -1) ?: -1
        if (dhikrId != -1) mainViewModel.onDhikrSelectedFromNotification(dhikrId)
    }
}
