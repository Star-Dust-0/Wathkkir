package com.example.ui.screens
import com.example.ui.i18n.S
import com.example.ui.i18n.L10n

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.oneui.OneUIButton
import com.example.ui.oneui.OneUIChip
import com.example.ThemeSettings
import com.example.ui.oneui.FrostSpec
import com.example.ui.oneui.oneUIFrosted
import com.example.ui.oneui.oneUIBorderColor
import com.example.ui.oneui.OneUIScreen
import com.example.ui.theme.OneUIActiveGreen
import com.example.ui.viewmodel.MainViewModel

@Composable
fun TasbeehScreen(viewModel: MainViewModel, onMenu: (() -> Unit)? = null) {
    val count by viewModel.tasbeehCount.collectAsStateWithLifecycle()
    TasbeehContent(
        count = count,
        onMenu = onMenu,
        onTap = viewModel::incrementTasbeeh,
        onReset = viewModel::resetTasbeeh
    )
}

@Composable
fun TasbeehContent(count: Int, onMenu: (() -> Unit)?, onTap: () -> Unit, onReset: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val targets = listOf(33, 99, 100)
    var target by remember { mutableIntStateOf(33) }
    var progress = (count % target).toFloat() / target
    if (count > 0 && count % target == 0) progress = 1f
    val finalProgress = progress
    val track = cs.surfaceVariant

    OneUIScreen(title = L10n.t(S.TasbeehTab), subtitle = L10n.tasbeehCount(count), onMenu = onMenu) {
        item(key = "ring") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(268.dp)
                        .oneUIFrosted(CircleShape, FrostSpec(cs.surface, if (ThemeSettings.isDark()) 0.50f else 0.62f, oneUIBorderColor()), 24)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (com.example.utils.AppHaptics.enabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTap()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        val stroke = 12.dp.toPx()
                        val inset = stroke / 2f
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        drawArc(
                            color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = OneUIActiveGreen, startAngle = -90f, sweepAngle = finalProgress * -360f, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            count.toString(),
                            color = cs.onSurface,
                            fontSize = 72.sp,
                            lineHeight = 80.sp,
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(L10n.t(S.TapToCount), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item(key = "targets") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                targets.forEach { t ->
                    OneUIChip(label = t.toString(), selected = t == target, onClick = { target = t })
                }
            }
        }
        item(key = "reset") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                OneUIButton(
                    text = L10n.t(S.ResetCounter),
                    onClick = onReset,
                    filled = false,
                    icon = Icons.Outlined.Refresh,
                    destructive = true
                )
            }
        }
    }
}
