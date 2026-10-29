package com.example.prayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Brightness2
import androidx.compose.material.icons.outlined.Brightness3
import androidx.compose.material.icons.outlined.Brightness5
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fajr.calculation.PrayerAdjustments
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIDivider
import com.example.ui.oneui.OneUIIconBadge
import com.example.ui.oneui.OneUIRow
import com.example.ui.oneui.OneUIScreen
import com.example.ui.oneui.OneUISwitch

/** Optional prayer-screen rows (gap item 4): Imsak, Duha, Midnight, Qiyam - all off by default. */
@Composable
fun OptionalTimesScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var tick by remember { mutableIntStateOf(0) }

    OneUIScreen(title = L10n.t(S.OptTimesTitle), onBack = onBack) {
        item(key = "explainer") {
            Text(
                L10n.t(S.OptTimesSub),
                color = cs.onSurfaceVariant, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        item(key = "rows") {
            OneUICard {
                OptRow("imsak", L10n.t(S.Imsak), Icons.Outlined.Brightness2, tick) { tick++ }
                OneUIDivider(70.dp)
                OptRow("duha", L10n.t(S.Duha), Icons.Outlined.Brightness5, tick) { tick++ }
                OneUIDivider(70.dp)
                OptRow("midnight", L10n.t(S.Midnight), Icons.Outlined.Bedtime, tick) { tick++ }
                OneUIDivider(70.dp)
                OptRow("qiyam", L10n.t(S.Qiyam), Icons.Outlined.Brightness3, tick) { tick++ }
            }
        }
        item(key = "pad") { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun OptRow(key: String, title: String, icon: ImageVector, tick: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    val on = remember(key, tick) { PrayerAdjustments.INSTANCE.optionalRow(context, key) }
    OneUIRow(
        title = title,
        leading = { OneUIIconBadge(icon) },
        trailing = {
            OneUISwitch(on, {
                PrayerAdjustments.INSTANCE.setOptionalRow(context, key, it)
                onChanged()
            })
        }
    )
}
