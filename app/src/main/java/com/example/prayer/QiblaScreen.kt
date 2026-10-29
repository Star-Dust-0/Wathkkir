package com.example.prayer

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fajr.data.FajrPreferences
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.ui.oneui.OneUIScreen
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/** Qibla compass. Own implementation of a classic tick-ring dial:
 *  fine ticks, bold ticks + degree labels every 30 deg, cardinal words inside,
 *  fixed top arrow, center heading readout, Kaaba-styled Qibla marker.
 *  Smoothness: sensor events only update a raw target value (no Compose state);
 *  a frame-synced loop applies time-normalized circular interpolation at display
 *  rate. displayDeg is read only in the Canvas draw phase (redraw, no
 *  recomposition); text reads a rounded Int state that changes at 1 deg steps.
 *  Kaaba: 21.422487 N, 39.826206 E. */
private const val KAABA_LAT = 21.422487
private const val KAABA_LON = 39.826206

private fun qiblaBearing(lat: Double, lon: Double): Double {
    val phi1 = Math.toRadians(lat)
    val phi2 = Math.toRadians(KAABA_LAT)
    val dLambda = Math.toRadians(KAABA_LON - lon)
    val y = sin(dLambda)
    val x = cos(phi1) * tan(phi2) - sin(phi1) * cos(dLambda)
    return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
}

private fun circularDelta(from: Float, to: Float): Float = ((to - from + 540f) % 360f) - 180f

private fun directionKey(deg: Int): S = when (((deg + 22) / 45) % 8) {
    0 -> S.DirN
    1 -> S.DirNE
    2 -> S.DirE
    3 -> S.DirSE
    4 -> S.DirS
    5 -> S.DirSW
    6 -> S.DirW
    else -> S.DirNW
}

/** Latest target angle written by the sensor callback, read by the frame loop.
 *  No Compose state: sensor events must not trigger recomposition. */
private object SensorTargetHolder {
    @Volatile var value: Float = Float.NaN
}

@Composable
fun QiblaScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val prefs = remember { FajrPreferences(context) }
    val hasLocation = prefs.latitude != 0.0 || prefs.longitude != 0.0
    val qibla = remember { qiblaBearing(prefs.latitude, prefs.longitude).toFloat() }
    // declination depends on location, not the sensor stream: compute once
    val declination = remember {
        GeomagneticField(
            prefs.latitude.toFloat(), prefs.longitude.toFloat(), 0f,
            System.currentTimeMillis()
        ).declination
    }

    var displayDeg by remember { mutableFloatStateOf(0f) }  // draw-phase only
    var headingInt by remember { mutableIntStateOf(0) }     // center text, 1-deg steps
    var aligned by remember { mutableStateOf(false) }       // boolean: changes rarely
    var accuracy by remember { mutableIntStateOf(-1) }
    var hasSensor by remember { mutableStateOf(true) }
    var buzzed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rv = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val rotMatrix = FloatArray(9)
        val orient = FloatArray(3)
        var grav: FloatArray? = null
        var geom: FloatArray? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                var have = false
                if (e.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotMatrix, e.values)
                    have = true
                } else if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    grav = e.values.clone()
                } else if (e.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    geom = e.values.clone()
                }
                val g = grav
                val m = geom
                if (!have && g != null && m != null) {
                    have = SensorManager.getRotationMatrix(rotMatrix, null, g, m)
                }
                if (!have) return
                SensorManager.getOrientation(rotMatrix, orient)
                val deg = (Math.toDegrees(orient[0].toDouble()).toFloat() + declination + 360f) % 360f
                val t = SensorTargetHolder.value
                SensorTargetHolder.value = if (t.isNaN()) {
                    deg
                } else {
                    // light sensor-domain circular low-pass before the frame loop
                    (t + 0.35f * circularDelta(t, deg) + 360f) % 360f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, acc: Int) {
                accuracy = acc
            }
        }
        SensorTargetHolder.value = Float.NaN
        var registered = false
        if (rv != null) {
            registered = sm.registerListener(listener, rv, SensorManager.SENSOR_DELAY_GAME)
        }
        if (!registered && accel != null && mag != null) {
            val a = sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
            val b = sm.registerListener(listener, mag, SensorManager.SENSOR_DELAY_GAME)
            registered = a && b
        }
        if (!registered) hasSensor = false
        onDispose { sm.unregisterListener(listener) }
    }

    // Frame-synced smoothing: display-rate interpolation, wrap-safe at 0/360.
    LaunchedEffect(Unit) {
        var lastNs = 0L
        while (true) {
            withFrameNanos { ns ->
                val target = SensorTargetHolder.value
                if (!target.isNaN()) {
                    if (lastNs == 0L) displayDeg = target // first fix: snap, no sweep
                    val dt = if (lastNs == 0L) 0.016f
                        else ((ns - lastNs) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
                    lastNs = ns
                    val d = circularDelta(displayDeg, target)
                    if (abs(d) < 0.02f) {
                        if (displayDeg != target) displayDeg = target
                    } else {
                        val k = 1f - exp(-dt / 0.09f) // tau = 90ms
                        displayDeg = (displayDeg + k * d + 360f) % 360f
                    }
                    val ri = displayDeg.roundToInt().mod(360)
                    if (ri != headingInt) headingInt = ri
                    val al = abs(circularDelta(displayDeg, qibla)) < 3f && accuracy > 1
                    if (al != aligned) aligned = al
                } else {
                    lastNs = ns
                }
            }
        }
    }

    LaunchedEffect(aligned) {
        if (aligned && !buzzed) {
            try {
                val v = context.getSystemService(Vibrator::class.java)
                v?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (_: Throwable) {}
            buzzed = true
        } else if (!aligned) {
            buzzed = false
        }
    }

    val northColor = Color(0xFFE8496B)
    val qiblaColor = Color(0xFFF0A835)
    val alignedTint = Color(0xFF22C55E)
    val degPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    val cardPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    OneUIScreen(title = L10n.t(S.QiblaTitle), onBack = onBack) {
        item(key = "dial") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 28.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    val c = center
                    val az = displayDeg // draw-phase read: redraw only
                    val al = aligned
                    val ringColor = if (al) alignedTint else cs.primary
                    degPaint.textSize = 11.dp.toPx()
                    cardPaint.textSize = 19.dp.toPx()

                    // degree labels every 30, inside the canvas edge
                    var ld = 0
                    while (ld < 360) {
                        val a = Math.toRadians((ld - az - 90f).toDouble())
                        val x = c.x + (r * 0.965f * cos(a)).toFloat()
                        val y = c.y + (r * 0.965f * sin(a)).toFloat() + degPaint.textSize / 3f
                        degPaint.color = android.graphics.Color.argb(
                            255, (cs.onSurfaceVariant.red * 255).toInt(),
                            (cs.onSurfaceVariant.green * 255).toInt(),
                            (cs.onSurfaceVariant.blue * 255).toInt()
                        )
                        drawContext.canvas.nativeCanvas.drawText(com.example.utils.Digits.ui(ld.toString()), x, y, degPaint)
                        ld += 30
                    }
                    // tick ring: fine every 3 deg, bold every 30
                    var deg = 0
                    while (deg < 360) {
                        val a = Math.toRadians((deg - az - 90f).toDouble())
                        val major = deg % 30 == 0
                        val inner = if (major) r * 0.76f else r * 0.82f
                        drawLine(
                            if (major) cs.onSurface else cs.outline,
                            Offset(c.x + (inner * cos(a)).toFloat(), c.y + (inner * sin(a)).toFloat()),
                            Offset(c.x + (r * 0.87f * cos(a)).toFloat(), c.y + (r * 0.87f * sin(a)).toFloat()),
                            strokeWidth = if (major) 2.6.dp.toPx() else 1.2.dp.toPx()
                        )
                        deg += 3
                    }
                    // cardinal words inside the ring (N colored)
                    val cards = arrayOf(
                        0 to L10n.t(S.DirN), 90 to L10n.t(S.DirE),
                        180 to L10n.t(S.DirS), 270 to L10n.t(S.DirW)
                    )
                    for ((cd, label) in cards) {
                        val a = Math.toRadians((cd - az - 90f).toDouble())
                        val x = c.x + (r * 0.56f * cos(a)).toFloat()
                        val y = c.y + (r * 0.56f * sin(a)).toFloat() + cardPaint.textSize / 3f
                        val col = if (cd == 0) northColor else cs.onSurface
                        cardPaint.color = android.graphics.Color.argb(
                            255, (col.red * 255).toInt(), (col.green * 255).toInt(), (col.blue * 255).toInt()
                        )
                        drawContext.canvas.nativeCanvas.drawText(label, x, y, cardPaint)
                    }
                    // Qibla marker on the ring (Kaaba-styled square with band)
                    val qa = Math.toRadians((qibla - az - 90f).toDouble())
                    val qx = c.x + (r * 0.68f * cos(qa)).toFloat()
                    val qy = c.y + (r * 0.68f * sin(qa)).toFloat()
                    val ks = 9.dp.toPx()
                    val qCol = if (al) alignedTint else qiblaColor
                    drawLine(qCol.copy(alpha = 0.45f), c, Offset(qx, qy), strokeWidth = 2.dp.toPx())
                    drawRect(qCol, topLeft = Offset(qx - ks / 2f, qy - ks / 2f), size = Size(ks, ks))
                    drawRect(
                        Color.Black.copy(alpha = 0.3f),
                        topLeft = Offset(qx - ks / 2f, qy - ks * 0.2f),
                        size = Size(ks, ks * 0.32f)
                    )
                    // fixed top arrow indicator pointing at the ring
                    val aw = 10.dp.toPx()
                    val ah = 13.dp.toPx()
                    val tipY = c.y - r * 0.76f
                    val arrow = Path().apply {
                        moveTo(c.x, tipY)
                        lineTo(c.x - aw, tipY - ah)
                        lineTo(c.x + aw, tipY - ah)
                        close()
                    }
                    drawPath(arrow, ringColor)
                    drawLine(
                        ringColor,
                        Offset(c.x, tipY),
                        Offset(c.x, tipY + 8.dp.toPx()),
                        strokeWidth = 3.5.dp.toPx()
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        headingInt.toString() + "°",
                        color = cs.onSurface, fontSize = 52.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        L10n.t(directionKey(headingInt)),
                        color = cs.onSurfaceVariant, fontSize = 19.sp, fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        L10n.t(S.QiblaNorthFmt).format(qibla.roundToInt()),
                        color = qiblaColor, fontSize = 13.sp
                    )
                }
            }
        }
        item(key = "status") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    !hasLocation -> Text(
                        L10n.t(S.QiblaSetLocation), color = Color(0xFFF59E0B),
                        fontSize = 15.sp, fontWeight = FontWeight.Medium
                    )
                    !hasSensor -> Text(
                        L10n.t(S.QiblaNoSensor), color = Color(0xFFF59E0B),
                        fontSize = 15.sp, fontWeight = FontWeight.Medium
                    )
                    accuracy in 0..1 -> Text(
                        L10n.t(S.QiblaCalibrate), color = Color(0xFFF59E0B),
                        fontSize = 15.sp, fontWeight = FontWeight.Medium
                    )
                    aligned -> Text(
                        L10n.t(S.QiblaAligned), color = Color(0xFF22C55E),
                        fontSize = 17.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    L10n.t(S.QiblaCityFmt).format(prefs.cityName),
                    color = cs.onSurfaceVariant, fontSize = 12.sp
                )
            }
        }
    }
}
