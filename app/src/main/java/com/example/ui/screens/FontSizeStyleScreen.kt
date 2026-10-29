package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.example.ui.oneui.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.FontScaleSettings
import com.example.ui.i18n.L10n
import com.example.ui.i18n.Lang
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIRow
import com.example.ui.oneui.OneUIScreen
import com.example.ui.theme.FontChoice
import kotlin.math.roundToInt

/** One UI style stepped A..A font-size slider, shown inline in Settings > Appearance. */
@Composable
fun FontSizeSliderRow(current: Float, onScale: (Float) -> Unit) {
    var index by remember(current) { mutableIntStateOf(FontScaleSettings.nearest(current)) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        StepSlider(index = index, count = FontScaleSettings.STEPS.size) { i, commit ->
            index = i
            if (commit) onScale(FontScaleSettings.STEPS[i])
        }
    }
}

@Composable
private fun StepSlider(index: Int, count: Int, onChange: (Int, Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val view = LocalView.current
    val dark = cs.background == Color.Black || cs.background.red < 0.2f
    val thumbFill = if (dark) Color.Black else Color.White
    val dotColor = if (dark) Color(0xFF58585C) else Color(0xFFC8C8CC)
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    var widthPx by remember { mutableFloatStateOf(1f) }
    val thumbR = with(LocalDensity.current) { 14.dp.toPx() }
    val anim = remember { Animatable(index.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    var last by remember { mutableIntStateOf(index) }
    LaunchedEffect(index) { anim.animateTo(index.toFloat(), spring(dampingRatio = 0.7f, stiffness = 900f)) }
    fun pick(x: Float, commit: Boolean) {
        val usable = (widthPx - 2 * thumbR).coerceAtLeast(1f)
        var f = ((x - thumbR) / usable).coerceIn(0f, 1f)
        if (rtl) f = 1f - f
        val i = (f * (count - 1)).roundToInt()
        if (i != last) { last = i; com.example.utils.AppHaptics.tick(view) }
        onChange(i, commit)
    }
    Row(
        Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(36.dp)).background(if (dark) Color(0xFF2A2A2E) else Color(0xFFF0F0F3)).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("A", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
        Spacer(Modifier.width(14.dp))
        Box(
            Modifier.weight(1f).height(56.dp)
                .onSizeChangedPx { widthPx = it }
                .pointerInput(count, rtl) {
                    detectTapGestures { pick(it.x, true) }
                }
                .pointerInput(count, rtl) {
                    detectDragGestures(
                        onDragStart = { dragging = true; pick(it.x, false) },
                        onDragEnd = { dragging = false; onChange(last, true) },
                        onDragCancel = { dragging = false; onChange(last, true) }
                    ) { change, _ -> change.consume(); pick(change.position.x, false) }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val usable = (widthPx - 2 * thumbR).coerceAtLeast(1f)
            // track
            Box(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(if (dark) Color(0xFF3A3A3E) else Color(0xFFE2E2E6)))
            // dots
            for (i in 0 until count) {
                val f = i / (count - 1f)
                val x = thumbR + usable * f
                val isDef = kotlin.math.abs(FontScaleSettings.STEPS[i] - FontScaleSettings.DEFAULT) < 0.01f
                val dsz = if (isDef) 10.dp else 8.dp
                Box(Modifier.offset { IntOffset((x - dsz.toPx() / 2f).roundToInt(), 0) }.size(dsz).clip(CircleShape).background(if (isDef) cs.primary else dotColor))
            }
            // thumb
            val fx = anim.value / (count - 1f)
            val tx = thumbR + usable * fx
            Box(
                Modifier.offset { IntOffset((tx - thumbR).roundToInt(), 0) }.size(28.dp)
                    .clip(CircleShape).background(thumbFill).border(2.5.dp, cs.primary, CircleShape)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text("A", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
    }
}

private fun Modifier.onSizeChangedPx(f: (Float) -> Unit): Modifier =
    this.then(Modifier.onSizeChanged { f(it.width.toFloat()) })

