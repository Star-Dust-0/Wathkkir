package com.example.ui.screens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.example.utils.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.example.ui.oneui.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.L10n
import com.example.ui.i18n.Lang
import com.example.ui.i18n.S
import com.example.ui.oneui.OneUIDialog
import com.example.ui.oneui.OneUIDivider

@Composable
fun LanguagePickerDialog(selected: Lang, onSelect: (Lang) -> Unit, onDismiss: () -> Unit) {
    var pending by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(selected) }
    OneUIDialog(title = L10n.t(S.Language), onDismiss = onDismiss, onConfirm = { onSelect(pending); onDismiss() }, confirmText = L10n.t(S.Apply)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Lang.entries.forEachIndexed { index, lang ->
                if (index > 0) OneUIDivider(4.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { pending = lang }
                        .padding(horizontal = 8.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        lang.nativeName,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 19.sp,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (lang == pending) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
