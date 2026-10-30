package com.example.ui.screens
import com.example.ui.i18n.S
import com.example.ui.i18n.L10n

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIDivider
import com.example.ui.oneui.OneUIIconBadge
import com.example.ui.oneui.OneUIRow
import com.example.ui.oneui.OneUIScreen

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val uri = LocalUriHandler.current
    OneUIScreen(title = L10n.t(S.About), onBack = onBack) {
        item(key = "hero") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppIconBadge()
                Spacer(modifier = Modifier.height(12.dp))
                Text("وَذكِّر", color = cs.onBackground, style = MaterialTheme.typography.headlineMedium)
                Text(
                    L10n.t(S.Tagline),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item(key = "info") {
            OneUICard {
                OneUIRow(
                    title = L10n.t(S.Version),
                    leading = { OneUIIconBadge(Icons.Outlined.Info) },
                    trailing = { Text(com.example.BuildConfig.VERSION_NAME, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
                )
                OneUIDivider(70.dp)
                OneUIRow(
                    title = L10n.t(S.Developer),
                    leading = { OneUIIconBadge(Icons.Outlined.Person) },
                    trailing = { Text(L10n.t(S.DevName), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
                )
                OneUIDivider(70.dp)
                OneUIRow(
                    title = L10n.t(S.HisnTab),
                    summary = "المحتوى من مشروع HisnElmoslem (مرخّص بـ GPL v3)",
                    leading = { OneUIIconBadge(Icons.AutoMirrored.Filled.MenuBook) }
                )
            }
        }
        item(key = "credit") {
            OneUICard {
                OneUIRow(
                    title = L10n.t(S.InstinctCredit),
                    summary = "brought to you by Instinct",
                    leading = { OneUIIconBadge(Icons.Outlined.Star) },
                    trailing = { ChevronIcon() },
                    onClick = { uri.openUri("https://instinct.com") }
                )
            }
        }
        item(key = "dua") {
            OneUICard {
                OneUIRow(
                    title = L10n.t(S.Dua),
                    leading = { OneUIIconBadge(Icons.Outlined.Favorite) }
                )
            }
        }
    }
}

@Composable
fun AppIconBadge() {
    AndroidView(
        factory = { ctx -> android.widget.ImageView(ctx).apply { setImageResource(R.drawable.app_icon) } },
        modifier = Modifier.size(84.dp)
    )
}
