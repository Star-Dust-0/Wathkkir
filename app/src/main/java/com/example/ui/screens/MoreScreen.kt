package com.example.ui.screens
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIDivider
import com.example.ui.oneui.OneUIIconBadge
import com.example.ui.oneui.OneUIRow
import com.example.ui.oneui.OneUIScreen

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    OneUIScreen(title = L10n.t(S.More)) {
        item(key = "g1") {
            OneUICard {
                MoreRow(L10n.t(S.Favorites), L10n.t(S.FavSubtitle), Icons.Outlined.Star) { onNavigate("favorites") }
                OneUIDivider(70.dp)
                MoreRow(L10n.t(S.Reminders), L10n.t(S.RemindersSubtitle), Icons.Outlined.Notifications) { onNavigate("reminders") }
                OneUIDivider(70.dp)
                MoreRow(L10n.t(S.Stats), L10n.t(S.StatsSubtitle), Icons.Outlined.BarChart) { onNavigate("statistics") }
                OneUIDivider(70.dp)
                MoreRow(L10n.t(S.Tags), L10n.t(S.TagsSubtitle), Icons.AutoMirrored.Filled.Label) { onNavigate("tags") }
            }
        }
        item(key = "g2") {
            OneUICard {
                MoreRow(L10n.t(S.FajrAlarm), L10n.t(S.ByLocation), Icons.Outlined.Alarm) { onNavigate("fajr_settings") }
                OneUIDivider(70.dp)
                MoreRow(L10n.t(S.Settings), L10n.t(S.SettingsSubtitle), Icons.Outlined.Settings) { onNavigate("settings") }
                OneUIDivider(70.dp)
                MoreRow(L10n.t(S.About), L10n.t(S.AboutSubtitle), Icons.Outlined.Info) { onNavigate("about") }
            }
        }
    }
}

@Composable
private fun MoreRow(title: String, summary: String, icon: ImageVector, onClick: () -> Unit) {
    OneUIRow(
        title = title,
        summary = summary,
        leading = { OneUIIconBadge(icon) },
        onClick = onClick
    )
}
