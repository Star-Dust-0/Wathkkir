package com.wathakker.wear
import androidx.compose.runtime.mutableIntStateOf

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as rotateScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.BasicSwipeToDismissBox
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.SwipeToDismissValue
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.remote.interactions.RemoteActivityHelper
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

val Gold: Color get() = WTheme.cur.main
val Ink = Color(0xFF0B0B0C)
val Pill = Color(0xFF1B1B1D)
val SoftGrey = Color(0xFF9D9DA2)
fun tx(size: Float, color: Color = Color.White, bold: Boolean = false, align: TextAlign = TextAlign.Center) =
    TextStyle(fontFamily = WFonts.cur.family, fontSize = (size * WFonts.scale).sp, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, textAlign = align)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocationSync.refresh(this)
        Settings.load(this)
        Adhan.schedule(this)
        if (intent.hasExtra("pat")) starPattern = intent.getIntExtra("pat", 1)
        if (intent.hasExtra("adhan_test")) Adhan.testIn(this, intent.getIntExtra("adhan_test", 15))
        if (intent.hasExtra("lang")) Loc.save(this, intent.getIntExtra("lang", 0))
        if (intent.hasExtra("theme")) Settings.saveTheme(this, intent.getIntExtra("theme", 0))
        val prefs = getSharedPreferences("wear_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("perm_asked", false) && !intent.hasExtra("page")) {
            prefs.edit().putBoolean("perm_asked", true).apply()
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { }
                .launch(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.POST_NOTIFICATIONS))
        }
        val page = intent.getIntExtra("page", 0)
        val sub = intent.getIntExtra("sub", 0)
        setContent { WearApp(page, sub) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalWearFoundationApi::class)
@Composable
fun WearApp(startPage: Int, startSub: Int = 0) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val state = remember(now / 20000) { Prayer.compute(ctx, now) }
    val appDir = if (langTick >= 0 && Loc.en) LayoutDirection.Ltr else LayoutDirection.Rtl
    CompositionLocalProvider(LocalLayoutDirection provides appDir) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            val w = maxWidth.value
            val pager = rememberPagerState(initialPage = startPage) { 4 }
            val act = ctx as? android.app.Activity
            var settingsSub by remember { mutableIntStateOf(startSub) }
            val settingsListState = rememberScalingLazyListState(initialCenterItemIndex = 0, initialCenterItemScrollOffset = 0)
            // Fresh entry to the settings page opens at the top (19:44); returning from a
            // sub-screen restores the exact position (21:30) because the state survives above
            // the AnimatedContent that swaps sub-screens.
            var prevPagerPage by remember { mutableIntStateOf(pager.currentPage) }
            LaunchedEffect(pager.currentPage) {
                if (pager.currentPage == 3 && prevPagerPage != 3) settingsListState.scrollToItem(0, 0)
                prevPagerPage = pager.currentPage
            }
            // One swipe-to-dismiss level for the whole navigation hierarchy (like SwipeDismissableNavHost):
            // on a settings sub-screen the edge swipe dismisses it back to the settings root with the root
            // visible underneath; on the first pager page it exits the app. On other pages the pager itself
            // consumes right swipes to page back one page at a time.
            val subOpen = pager.currentPage == 3 && settingsSub != 0
            val dismissState = rememberSwipeToDismissBoxState()
            // True while the sub-screen is closing via the edge swipe-back gesture: the box has already
            // animated that visually, so the content swap back to the root must be instant (no double).
            var returnViaSwipe by remember { mutableStateOf(false) }
            LaunchedEffect(dismissState.currentValue) {
                if (dismissState.currentValue == SwipeToDismissValue.Dismissed) {
                    dismissState.snapTo(SwipeToDismissValue.Default)
                    if (subOpen) { returnViaSwipe = true; settingsSub = 0 } else act?.finish()
                }
            }
            // Constant content (root-cause fix for his 19:44 report): keying the box content per
            // sub-screen made the box dispose and recreate it, so the enter transition never ran.
            // Now the box only owns the swipe-back gesture (background = settings root revealed under
            // the finger) and the forward/return transitions run in the AnimatedContent below.
            BasicSwipeToDismissBox(
                state = dismissState,
                modifier = Modifier.fillMaxSize().edgeSwipeToDismiss(dismissState),
                userSwipeEnabled = subOpen || pager.currentPage == 0
            ) { isBackground ->
                if (isBackground) {
                    if (subOpen) SettingsContent(0, w, {}, settingsListState) else Box(Modifier.fillMaxSize().background(Color.Black))
                } else {
                    // The pager direction is fixed to LTR: the Wear back gesture is always a right swipe (it does not
                    // mirror in RTL), so the next page must always come from a left swipe, in every language.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        // While a settings sub-screen is open, the pager must not consume horizontal drags:
                        // a right swipe then only dismisses the sub-screen back to the settings root.
                        HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = settingsSub == 0) { page ->
                            CompositionLocalProvider(LocalLayoutDirection provides appDir) {
                                when (page) {
                                    0 -> MainPage(state, now, w)
                                    1 -> ListPage(state, w)
                                    2 -> QiblaPage(w)
                                    else -> {
                                        // One UI Watch navigation: the sub-screen slides in from the right
                                        // while the root slides out left; returning mirrors it. The
                                        // swipe-back gesture animates itself, so that path stays instant.
                                        AnimatedContent(
                                            targetState = settingsSub,
                                            transitionSpec = {
                                                when {
                                                    returnViaSwipe && targetState == 0 -> EnterTransition.None togetherWith ExitTransition.None
                                                    targetState != 0 -> (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                                                    else -> (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                                                }
                                            },
                                            label = "settingsSub"
                                        ) { sub ->
                                            SettingsPage(w, sub, { settingsSub = it }, settingsListState)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            LaunchedEffect(settingsSub) { returnViaSwipe = false }
            // Dots mirror the physical page order (page 0 at the left, advancing moves the pill right),
            // matching the fixed LTR pager direction in every language.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = (w * 0.045f).dp),
                    horizontalArrangement = Arrangement.spacedBy((w * 0.018f).dp)
                ) {
                    // The active pill flows with the swipe (One UI style) instead of jumping page to page.
                    val pos = pager.currentPage + pager.currentPageOffsetFraction
                    for (i in 0 until 4) {
                        val dist = abs(pos - i).coerceIn(0f, 1f)
                        Box(
                            Modifier.width(((0.022f + (0.07f - 0.022f) * (1f - dist)) * w).dp).height((w * 0.022f).dp)
                                .clip(CircleShape).background(lerp(Color(0xFF5A5A5E), Color.White, 1f - dist))
                        )
                    }
                }
            }
        }
    }
}

/** Pattern chosen for the main page background: 1 = Rub el Hizb stars touching tip to tip, 2 = 12-fold interlaced rosette. */
var starPattern by androidx.compose.runtime.mutableIntStateOf(1)

/** Rub el Hizb (eight-pointed Islamic star, two overlaid squares) as a 16-vertex outline. */
private fun starPath(cx: Float, cy: Float, r: Float): Path {
    val p = Path(); val inner = r * 0.7654f
    for (i in 0 until 16) {
        val a = -Math.PI / 2 + i * Math.PI / 8
        val rr = if (i % 2 == 0) r else inner
        val x = cx + cos(a).toFloat() * rr; val y = cy + sin(a).toFloat() * rr
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close(); return p
}

private fun diamondPath(cx: Float, cy: Float, r: Float): Path {
    val p = Path(); p.moveTo(cx, cy - r); p.lineTo(cx + r, cy); p.lineTo(cx, cy + r); p.lineTo(cx - r, cy); p.close(); return p
}

/** Outline of an n-pointed star: outer radius ro, inner radius ri, first tip at the top. */
private fun starOutline(cx: Float, cy: Float, n: Int, ro: Float, ri: Float): Path {
    val p = Path()
    for (i in 0 until 2 * n) {
        val a = -Math.PI / 2 + i * Math.PI / n
        val rr = if (i % 2 == 0) ro else ri
        val x = cx + cos(a).toFloat() * rr; val y = cy + sin(a).toFloat() * rr
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close(); return p
}

/** Regular square-grid Islamic star pattern that emerges from the bottom-left corner and fades out. */
@Composable
fun StarField(modifier: Modifier, pat: Int = starPattern, boost: Float = 1f) {
    Canvas(modifier) {
        val r = size.minDimension * 0.085f
        val step = r * 2f
        val maxD = hypot(size.width, size.height) * 0.95f
        val sw = size.minDimension * 0.003f
        if (pat >= 8) {
            val ro = size.minDimension * 0.095f
            val hex = pat == 9
            val dx = if (hex) ro * 1.732f else ro * 2f
            val dy = if (hex) ro * 1.5f else ro * 2f
            val c0 = (size.width / dx).toInt() + 3; val r0 = (size.height / dy).toInt() + 3
            for (row in 0..r0) for (col in 0..c0) {
                val cx = col * dx + (if (hex && row % 2 == 1) dx / 2 else if (!hex && row % 2 == 1 && pat == 11) dx / 2 else 0f)
                val cy = size.height - row * dy
                val al = (0.30f * boost * (1f - hypot(cx, size.height - cy) / maxD).coerceIn(0f, 1f)).coerceAtMost(1f)
                if (al < 0.01f) continue
                val col1 = Color.White.copy(alpha = al)
                when (pat) {
                    8 -> { drawPath(starOutline(cx, cy, 8, ro, ro * 0.7654f), col1, style = Stroke(width = sw)); drawPath(diamondPath(cx + dx / 2, cy - dy / 2, ro * 0.30f), col1, style = Stroke(width = sw)) }
                    9 -> { drawPath(starOutline(cx, cy, 6, ro * 0.98f, ro * 0.577f), col1, style = Stroke(width = sw)); drawCircle(col1, ro * 0.16f, Offset(cx, cy), style = Stroke(width = sw)) }
                    10 -> { drawPath(starOutline(cx, cy, 8, ro, ro * 0.541f), col1, style = Stroke(width = sw)); drawPath(starOutline(cx, cy, 8, ro * 0.5f, ro * 0.27f), col1.copy(alpha = al * 0.7f), style = Stroke(width = sw)) }
                    else -> { drawPath(starOutline(cx, cy, 12, ro, ro * 0.5176f), col1, style = Stroke(width = sw)); drawCircle(col1, ro * 0.22f, Offset(cx, cy), style = Stroke(width = sw)); drawCircle(col1.copy(alpha = al * 0.6f), ro * 0.62f, Offset(cx, cy), style = Stroke(width = sw)) }
                }
            }
            return@Canvas
        }
        val cols = (size.width / step).toInt() + 2; val rows = (size.height / step).toInt() + 2
        for (row in 0..rows) for (col in 0..cols) {
            val cx = col * step; val cy = size.height - row * step
            val a = (1f - hypot(cx, size.height - cy) / maxD).coerceIn(0f, 1f)
            if (a > 0.03f) {
                val al = (0.34f * boost * a).coerceAtMost(1f)
                drawPath(starPath(cx, cy, r), Color.White.copy(alpha = al), style = Stroke(width = sw))
            }
        }
    }
}

@Composable
fun MainPage(s: PrayerState, now: Long, w: Float) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        StarField(Modifier.fillMaxSize())
        Text1(Prayer.weekday(now), tx(w * 0.062f, SoftGrey), Modifier.align(Alignment.TopStart).padding(start = (w * 0.24f).dp, top = (w * 0.12f).dp))
        Text1(Prayer.gregDate(now), tx(w * 0.062f, SoftGrey), Modifier.align(Alignment.TopEnd).padding(end = (w * 0.24f).dp, top = (w * 0.12f).dp))
        Text1(Prayer.hijri(now), tx(w * 0.055f, SoftGrey), Modifier.align(Alignment.TopCenter).padding(top = (w * 0.205f).dp))
        Column(Modifier.align(Alignment.Center).padding(top = (w * 0.13f).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text1(s.next.name, tx(w * 0.15f, Color.White, true))
            Text1(Prayer.time12(s.next.at), tx(w * 0.095f, Color.White))
            Spacer(Modifier.height((w * 0.035f).dp))
            Text1(T("متبقي ", "Time left ") + Prayer.remainingText(s.next.at - now), tx(w * 0.062f, Gold))
        }
    }
}

@Composable
fun Text1(t: String, style: TextStyle, modifier: Modifier = Modifier, maxLines: Int = 1) {
    androidx.compose.foundation.text.BasicText(t, modifier, style = style, maxLines = maxLines)
}

@Composable
fun ListPage(s: PrayerState, w: Float) {
    val nextIdx = s.today.indexOfFirst { it.at == s.next.at }.let { if (it < 0) 0 else it }
    // item 0 is the title, prayers are items 1..6
    val listState = androidx.wear.compose.foundation.lazy.rememberScalingLazyListState(initialCenterItemIndex = nextIdx + 1)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        androidx.wear.compose.foundation.lazy.ScalingLazyColumn(Modifier.fillMaxSize(), state = listState) {
            item { Text1(T("مواقيت الصلاة", "Prayer times"), tx(w * 0.07f, Color(0xFFB9B9BE)), Modifier.padding(top = (w * 0.07f).dp, bottom = (w * 0.05f).dp)) }
            items(s.today.size) { i ->
                val slot = s.today[i]
                val isNext = slot.at == s.next.at
                val past = slot.at <= s.now
                Row(
                    Modifier.fillMaxWidth().height((w * 0.19f).dp).clip(RoundedCornerShape(50)).then(if (isNext) Modifier.background(WTheme.cur.brush()) else Modifier.background(Pill))
                        .padding(horizontal = (w * 0.065f).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val c = if (isNext) Ink else if (past) SoftGrey else Color.White
                    Text1(slot.name, tx(w * 0.07f, c, isNext))
                    Spacer(Modifier.weight(1f).widthIn(min = (w * 0.07f).dp))
                    Text1(Prayer.time12(slot.at), tx(w * 0.066f, c, isNext))
                }
            }
        }
    }
}

@Composable
fun QiblaPage(w: Float) {
    val ctx = LocalContext.current
    var heading by remember { mutableFloatStateOf(0f) }
    val bearing = remember { Prayer.qiblaBearing(ctx) }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val r = FloatArray(9); SensorManager.getRotationMatrixFromVector(r, e.values)
                val o = FloatArray(3); SensorManager.getOrientation(r, o)
                heading = ((Math.toDegrees(o[0].toDouble()) + 360.0) % 360.0).toFloat()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(l) }
    }
    val needle = bearing - heading
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Text1(T("القبلة", "Qibla"), tx(w * 0.065f, Color(0xFFB9B9BE)), Modifier.align(Alignment.TopCenter).padding(top = (w * 0.085f).dp))
        Box(Modifier.align(Alignment.Center).padding(bottom = (w * 0.07f).dp).size((w * 0.54f).dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                drawCircle(Color(0xFF2B2B2E), radius = size.minDimension / 2 - 2f, style = Stroke(width = size.minDimension * 0.008f))
                rotateScope(needle, c) {
                    val len = size.minDimension * 0.46f; val half = size.minDimension * 0.055f
                    val tip = Path().apply { moveTo(c.x, c.y - len); lineTo(c.x + half, c.y); lineTo(c.x - half, c.y); close() }
                    drawPath(tip, Gold)
                    // Kaaba above the arrow tip: black cube with a gold band and door
                    val k = size.minDimension * 0.034f; val ky = c.y - len - k * 1.15f
                    val cube = Path().apply { moveTo(c.x - k, ky - k * 0.9f); lineTo(c.x + k, ky - k * 0.9f); lineTo(c.x + k, ky + k * 0.9f); lineTo(c.x - k, ky + k * 0.9f); close() }
                    drawPath(cube, Color(0xFF141414)); drawPath(cube, Gold, style = Stroke(width = k * 0.14f))
                    drawRect(Gold, Offset(c.x - k, ky - k * 0.45f), Size(k * 2f, k * 0.32f))
                    drawRect(Gold, Offset(c.x - k * 0.55f, ky - k * 0.45f + k * 0.55f), Size(k * 0.3f, k * 0.62f))
                    val tail = Path().apply { moveTo(c.x, c.y + len * 0.62f); lineTo(c.x + half, c.y); lineTo(c.x - half, c.y); close() }
                    drawPath(tail, Color(0xFF3C3C40))
                }
                drawCircle(Color.Black, radius = size.minDimension * 0.045f)
                drawCircle(Gold, radius = size.minDimension * 0.045f, style = Stroke(width = size.minDimension * 0.016f))
            }
            val ls = tx(w * 0.062f, Color(0xFFD0D0D4), true)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(Modifier.fillMaxSize().rotate(-heading)) {
                    Text1("N", tx(w * 0.07f, Gold, true), Modifier.align(Alignment.TopCenter).padding(top = (w * 0.035f).dp).rotate(heading))
                    Text1("E", ls, Modifier.align(Alignment.CenterEnd).padding(end = (w * 0.04f).dp).rotate(heading))
                    Text1("W", ls, Modifier.align(Alignment.CenterStart).padding(start = (w * 0.04f).dp).rotate(heading))
                    Text1("S", ls, Modifier.align(Alignment.BottomCenter).padding(bottom = (w * 0.035f).dp).rotate(heading))
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = (w * 0.105f).dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((w * 0.025f).dp)) {
            Text1("${bearing.roundToInt()}°", tx(w * 0.072f, Color.White))
            Text1(Prayer.directionName(bearing), tx(w * 0.044f, SoftGrey))
        }
    }
}

@Composable
fun CirclesPage(s: PrayerState, now: Long, w: Float, visible: Boolean, bg: Color = Color.Black) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            anim.snapTo(0f)
            anim.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
        } else anim.snapTo(0f)
    }
    val ids = listOf(0, 2, 3, 4, 5)  // clockwise from the top: Fajr, Dhuhr, Asr, Maghrib, Isha
    val slots = ids.map { id -> if (id == 0 && s.next.at == s.nextFajrTomorrow.at) s.nextFajrTomorrow else s.today[id] }
    val d = w * 0.295f
    val rad = w * 0.285f
    Box(Modifier.fillMaxSize().background(bg)) {
        slots.forEachIndexed { i, slot ->
            val ang = Math.toRadians(-90.0 + i * 72.0)
            val tx0 = (cos(ang) * rad).toFloat(); val ty0 = (sin(ang) * rad - w * 0.012f).toFloat()
            val isNext = slot.at == s.next.at
            val past = slot.at <= now && !isNext
            Box(
                Modifier.align(Alignment.Center)
                    .offset { IntOffset((tx0 * anim.value * density).roundToInt(), (ty0 * anim.value * density).roundToInt()) }
                    .size(d.dp)
                    .graphicsLayer { scaleX = anim.value.coerceAtLeast(0.001f); scaleY = anim.value.coerceAtLeast(0.001f); alpha = anim.value.coerceIn(0f, 1f) },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = size.minDimension * 0.045f
                    if (isNext) {
                        drawCircle(WTheme.cur.brush())
                        drawArc(Ink, -90f, 360f * s.progress, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(width = sw * 0.6f, cap = StrokeCap.Round))
                    } else if (past) {
                        drawCircle(Color(0xFF1D1D1F))
                        drawCircle(Gold.copy(alpha = 0.35f), radius = size.minDimension / 2 - sw / 2, style = Stroke(width = sw))
                    } else {
                        drawCircle(Color(0xFF2A2A2D))
                    }
                }
                val c = if (isNext) Ink else if (past) Color(0xFFC9C9CE) else Color.White
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text1(slot.name, tx(w * 0.05f, c, true))
                    Text1(if (past) Prayer.time24(slot.at) else Prayer.hhmm(slot.at - now), tx(w * 0.078f, c))
                    Text1(if (past) T("مضت", "Passed") else T("متبقية", "Left"), tx(w * 0.042f, if (isNext) Ink else SoftGrey))
                }
            }
        }
    }
}

private val density: Float get() = android.content.res.Resources.getSystem().displayMetrics.density

fun openOnPhone(ctx: Context, report: (String) -> Unit) {
    try {
        val helper = RemoteActivityHelper(ctx, Executors.newSingleThreadExecutor())
        val i = Intent(Intent.ACTION_VIEW).addCategory(Intent.CATEGORY_BROWSABLE).setData(Uri.parse("wathakker://open"))
        val f = helper.startRemoteActivity(i)
        f.addListener({
            try { f.get(); report(T("تم الفتح على الهاتف", "Opened on phone")) } catch (e: Exception) { report(T("الهاتف غير متصل", "Phone not connected")) }
        }, ContextCompat.getMainExecutor(ctx))
    } catch (e: Exception) {
        report(T("الهاتف غير متصل", "Phone not connected"))
    }
}
