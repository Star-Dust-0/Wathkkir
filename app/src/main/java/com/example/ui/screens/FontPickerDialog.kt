package com.example.ui.screens

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
import com.example.ui.theme.FontChoice
import com.example.ui.theme.fontFamilyFor

/** OneUI font picker sheet: every option rendered in its own font as a live preview. */
@Composable
fun FontPickerDialog(
    selected: FontChoice,
    onSelect: (FontChoice) -> Unit,
    onDismiss: () -> Unit
) {
    OneUIDialog(
        title = "الخط",
        onDismiss = onDismiss,
        onConfirm = null,
        dismissText = "إغلاق"
    ) {
        FontChoice.entries.forEachIndexed { index, choice ->
            if (index > 0) OneUIDivider(inset = 4.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        onSelect(choice)
                        onDismiss()
                    }
                    .padding(horizontal = 8.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    choice.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = fontFamilyFor(choice)
                    ),
                    fontSize = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (choice == selected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
