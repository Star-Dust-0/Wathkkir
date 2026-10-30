package com.example.ui.screens
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.oneui.OneUIDialog
import com.example.ui.oneui.OneUIDivider
import com.example.ThemeSettings
import com.example.ui.theme.FontChoice
import com.example.ui.theme.fontFamilyFor

@Composable
fun FontPickerDialog(selected: FontChoice, onSelect: (FontChoice) -> Unit, onDismiss: () -> Unit) {
    var pending by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(selected) }
    OneUIDialog(title = "الخط", onDismiss = onDismiss, onConfirm = { onSelect(pending); onDismiss() }, confirmText = L10n.t(S.Apply)) {
        FontChoice.entries.forEachIndexed { index, choice ->
            if (index > 0) OneUIDivider(4.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { pending = choice }
                    .padding(horizontal = 8.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    choice.titleAr,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = fontFamilyFor(choice))
                )
                if (choice == pending) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}


@Composable
fun ThemePickerDialog(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(
        ThemeSettings.AUTO to "تلقائي (النظام)",
        ThemeSettings.LIGHT to "فاتح",
        ThemeSettings.DARK to "داكن"
    )
    var pending by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(selected) }
    OneUIDialog(title = "المظهر", onDismiss = onDismiss, onConfirm = { onSelect(pending); onDismiss() }, confirmText = L10n.t(S.Apply)) {
        options.forEachIndexed { index, (key, label) ->
            if (index > 0) OneUIDivider(4.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { pending = key }
                    .padding(horizontal = 8.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (key == pending) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}


@Composable
fun FontSizeDialog(selected: Float, onSelect: (Float) -> Unit, onDismiss: () -> Unit) {
    val ar = L10n.lang == com.example.ui.i18n.Lang.AR
    var pending by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(selected) }
    OneUIDialog(title = if (ar) "حجم الخط" else "Font size", onDismiss = onDismiss, onConfirm = { onSelect(pending); onDismiss() }, confirmText = L10n.t(S.Apply)) {
        com.example.FontScaleSettings.STEPS.forEachIndexed { index, step ->
            if (index > 0) OneUIDivider(4.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { pending = step }
                    .padding(horizontal = 8.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    com.example.FontScaleSettings.label(step, ar),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (19f * step).sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (kotlin.math.abs(step - pending) < 0.01f) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
