package com.wathakker.wear
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin

/** RENDER ONLY (temporary): fake analog face behind a dark scrim and the real CirclesPage. */
class RenderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scrim = intent.getIntExtra("scrim", 32) / 100f
        val circles = intent.getBooleanExtra("circles", true)
        setContent {
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            val st = remember { Prayer.compute(this@RenderActivity, now) }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
                    val w = maxWidth.value
                    Canvas(Modifier.fillMaxSize()) {
                        val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension / 2
                        for (i in 0 until 60) {
                            val a = Math.toRadians(i * 6.0 - 90); val big = i % 5 == 0
                            val r0 = r * (if (big) 0.86f else 0.93f); val r1 = r * 0.99f
                            drawLine(if (big) Color(0xFFF3D62C) else Color(0xFF6A6A6A), Offset(c.x + cos(a).toFloat() * r0, c.y + sin(a).toFloat() * r0), Offset(c.x + cos(a).toFloat() * r1, c.y + sin(a).toFloat() * r1), strokeWidth = if (big) 7f else 3f, cap = StrokeCap.Butt)
                        }
                        val h = Math.toRadians(-60.0); val m = Math.toRadians(40.0)
                        drawLine(Color(0xFFDDDDDD), c, Offset(c.x + cos(h).toFloat() * r * 0.5f, c.y + sin(h).toFloat() * r * 0.5f), strokeWidth = 16f, cap = StrokeCap.Round)
                        drawLine(Color(0xFFDDDDDD), c, Offset(c.x + cos(m).toFloat() * r * 0.78f, c.y + sin(m).toFloat() * r * 0.78f), strokeWidth = 12f, cap = StrokeCap.Round)
                    }
                    if (circles) CirclesPage(st, now, w, true, Color.Black.copy(alpha = scrim))
                }
            }
        }
    }
}
