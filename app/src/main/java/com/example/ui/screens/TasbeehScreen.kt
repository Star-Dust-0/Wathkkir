package com.example.ui.screens

import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.oneui.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun TasbeehScreen(viewModel: MainViewModel, onMenu: (() -> Unit)? = null) {
    val count by viewModel.tasbeehCount.collectAsStateWithLifecycle()
    TasbeehContent(count, onMenu, viewModel::incrementTasbeeh, viewModel::resetTasbeeh)
}

@Composable
fun TasbeehContent(count: Int, onBack: (() -> Unit)?, onTap: () -> Unit, onReset: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val targets = listOf(33, 99, 100)
    var target by remember { mutableIntStateOf(33) }
    val progress = ((count % target).toFloat() / target).let { if (count > 0 && count % target == 0) 1f else it }
    OneUIScreen(title = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.TasbeehTab), subtitle = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.TasbeehaFmt).format(count), onMenu = onBack, actions = { com.example.ui.oneui.OneUIIconAction(androidx.compose.material.icons.Icons.Default.Settings, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Settings), { com.example.ui.oneui.HeaderTools.nav?.navigate("settings") }) }) {
        item(key = "ring") {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(268.dp)
                        .clip(CircleShape)
                        .background(cs.surface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (com.example.utils.AppHaptics.enabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTap()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val track = cs.surfaceVariant
                    val arc = cs.primary
                    Canvas(Modifier.fillMaxSize().padding(14.dp)) {
                        val stroke = 12.dp.toPx()
                        val inset = stroke / 2
                        val sz = Size(size.width - stroke, size.height - stroke)
                        drawArc(track, 0f, 360f, false, Offset(inset, inset), sz, style = Stroke(stroke, cap = StrokeCap.Round))
                        // RTL feel: progress runs counter-clockwise, starting at the top
                        drawArc(arc, -90f, -360f * progress, false, Offset(inset, inset), sz, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(count.toString(), style = MaterialTheme.typography.displayLarge, fontSize = 72.sp, lineHeight = 80.sp, color = cs.onSurface)
                        Text(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.TapToCount), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                }
            }
        }
        item(key = "targets") {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                targets.forEach { t -> OneUIChip("$t", target == t, { target = t }) }
            }
        }
        item(key = "reset") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                OneUIButton(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.ResetCounter), onReset, icon = Icons.Default.Refresh, destructive = true)
            }
        }
    }
}
