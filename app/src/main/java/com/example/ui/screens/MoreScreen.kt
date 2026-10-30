package com.example.ui.screens

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
    OneUIScreen(title = "المزيد") {
        item(key = "g1") {
            OneUICard {
                MoreRow("المفضلة", "الأذكار التي تحبها", Icons.Outlined.Star) { onNavigate("favorites") }
                OneUIDivider(70.dp)
                MoreRow("التنبيهات", "جميع أوقات التذكير", Icons.Outlined.Notifications) { onNavigate("reminders") }
                OneUIDivider(70.dp)
                MoreRow("الإحصائيات", "السجل والأيام المتتالية", Icons.Outlined.BarChart) { onNavigate("statistics") }
                OneUIDivider(70.dp)
                MoreRow("التصنيفات", "تنظيم الأذكار بالألوان", Icons.AutoMirrored.Filled.Label) { onNavigate("tags") }
            }
        }
        item(key = "g2") {
            OneUICard {
                MoreRow("منبه صلاة الفجر", "حسب موقعك الجغرافي", Icons.Outlined.Alarm) { onNavigate("fajr_settings") }
                OneUIDivider(70.dp)
                MoreRow("الإعدادات", "الاهتزاز والخط والنغمة", Icons.Outlined.Settings) { onNavigate("settings") }
                OneUIDivider(70.dp)
                MoreRow("حول التطبيق", "الإصدار والمطوّر", Icons.Outlined.Info) { onNavigate("about") }
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
