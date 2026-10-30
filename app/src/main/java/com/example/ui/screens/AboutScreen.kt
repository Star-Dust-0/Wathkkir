package com.example.ui.screens

import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import com.example.ui.oneui.*

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    OneUIScreen(title = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.About), onBack = onBack) {
        item(key = "hero") {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AppIconBadge()
                Spacer(Modifier.height(12.dp))
                Text("وذكّر", style = MaterialTheme.typography.headlineMedium, color = cs.onBackground)
                Text(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Tagline), style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            }
        }
        item(key = "info") {
            OneUICard {
                OneUIRow(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Version), leading = { OneUIIconBadge(Icons.Outlined.Info) },
                    trailing = { Text(androidx.compose.ui.platform.LocalContext.current.let { c -> c.packageManager.getPackageInfo(c.packageName, 0).versionName ?: "" }, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) })
                OneUIDivider(inset = 70.dp)
                OneUIRow(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Developer), leading = { OneUIIconBadge(Icons.Outlined.Person) },
                    trailing = { Text(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.DevName), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) })
                OneUIDivider(inset = 70.dp)
                OneUIRow(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.HisnTab), summary = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.HisnCredit),
                    leading = { OneUIIconBadge(Icons.AutoMirrored.Filled.MenuBook) })
            }
        }
        item(key = "credit") {
            OneUICard {
                val uri = androidx.compose.ui.platform.LocalUriHandler.current
                OneUIRow(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.InstinctCredit), summary = "brought to you by Instinct",
                    onClick = { uri.openUri("https://instinct.com") },
                    leading = { OneUIIconBadge(Icons.Default.Star) }, trailing = { ChevronIcon() })
            }
        }
        item(key = "dua") {
            OneUICard {
                OneUIRow(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Dua), leading = { OneUIIconBadge(Icons.Default.Favorite) })
            }
        }
    }
}

@Composable
fun AppIconBadge() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val bmp = androidx.compose.runtime.remember {
        val d = androidx.core.content.ContextCompat.getDrawable(ctx, com.example.R.drawable.app_icon)
        val px = (84 * ctx.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        if (d == null) null else {
            val b = android.graphics.Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888)
            val c = android.graphics.Canvas(b)
            d.setBounds(0, 0, px, px); d.draw(c)
            b.asImageBitmap()
        }
    }
    if (bmp != null) androidx.compose.foundation.Image(
        bitmap = bmp,
        contentDescription = null,
        modifier = Modifier.size(84.dp).androidx_clip22()
    )
}

private fun Modifier.androidx_clip22(): Modifier =
    this.then(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp)))
