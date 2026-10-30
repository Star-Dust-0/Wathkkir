package com.wathakker.wear

import androidx.compose.ui.graphics.toArgb
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.renderer.TileRenderer
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.rendering.ComplicationDrawable

private class PreviewTile(c: Context) : PrayerTileService() { init { attachBaseContext(c) } }
private class PreviewComp(c: Context, val k: Int, spoof: String? = null, style: String? = null) : PrayerComplicationService() {
    init { attachBaseContext(c) }
    override val kind get() = k
    private val sp = spoof
    override val selfName get() = sp ?: super.selfName
    private val st = style
    override val cdStyle get() = if (st == "stopwatch") androidx.wear.watchface.complications.data.TimeDifferenceStyle.STOPWATCH
        else if (st == "dual") androidx.wear.watchface.complications.data.TimeDifferenceStyle.SHORT_DUAL_UNIT else super.cdStyle
}

/** Debug preview: draws the real tile layout with the Tiles renderer, and the real complication data with the platform complication renderer. */
class PreviewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra("mode") ?: "tile"
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(root)
        // iqama_demo: pretend "now" is 3 minutes after the upcoming adhan so the iqama window is active.
        val demo = intent.getBooleanExtra("iqama_demo", false)
        val now0 = if (demo) (Prayer.nextAdhan(this)?.at ?: System.currentTimeMillis()) + 3 * 60000L else System.currentTimeMillis()
        var s = Prayer.compute(this, now0)
        var iq = Prayer.iqamaNow(this, now0)
        // countdown_sec: synthetic state whose next prayer is exactly that many seconds away, so the
        // rendered countdown can be checked against a known remaining value at exact instants.
        val cds = intent.getIntExtra("countdown_sec", -1)
        if (cds >= 0) {
            val now = System.currentTimeMillis()
            val prev = Slot(0, Prayer.names[0], now - 3600_000L)
            val next = Slot(2, Prayer.names[2], now + cds * 1000L)
            s = PrayerState(now, listOf(prev, next), prev, next, next)
            iq = null
        }
        if (mode == "tile") {
            val layout = LayoutElementBuilders.Layout.Builder().setRoot(PreviewTile(this).layout(s, iq)).build()
            val res = ResourceBuilders.Resources.Builder().setVersion("1").build()
            // Note: the bundled TileRenderer mirrors the arc under an RTL system locale while the real
            // watch (sysui) does not; a per-renderer LTR context does NOT override that (verified on
            // emulator), so arc-accurate tile captures are taken under an en-US system locale instead.
            val r = TileRenderer(this, ContextCompat.getMainExecutor(this)) { }
            r.inflateAsync(layout, res, root)
        } else {
            val k = intent.getIntExtra("kind", 0)
            // Simulated host lifecycle, for emulator validation of the alarm behavior:
            // request_id >= 0 runs the REAL onComplicationRequest path (arms boundary, and the
            // minute pulse only for ranged), spoofing the given provider class so PendingIntent
            // identity matches production; boundary_sec arms a boundary alarm that many seconds
            // from now for rollover testing.
            val reqId = intent.getIntExtra("request_id", -1)
            if (reqId >= 0) {
                val rt = if (intent.getStringExtra("req_type") == "ranged") ComplicationType.RANGED_VALUE else ComplicationType.SHORT_TEXT
                val cls = intent.getStringExtra("spoof_cls") ?: "com.wathakker.wear.PrayerComplicationService"
                kotlinx.coroutines.runBlocking {
                    PreviewComp(this@PreviewActivity, k, cls).onComplicationRequest(ComplicationRequest(reqId, rt, false))
                }
            }
            val bsec = intent.getIntExtra("boundary_sec", -1)
            if (bsec >= 0) PreviewComp(this, k).scheduleBoundary(System.currentTimeMillis() + bsec * 1000L)
            // multi=1: all four lifecycle requests from ONE launch (a force-stop between launches
            // would wipe earlier arms and make coexistence/pulse-death verdicts spurious).
            if (intent.getBooleanExtra("multi", false)) {
                kotlinx.coroutines.runBlocking {
                    PreviewComp(this@PreviewActivity, 0, "com.wathakker.wear.PrayerComplicationService").onComplicationRequest(ComplicationRequest(101, ComplicationType.RANGED_VALUE, false))
                    PreviewComp(this@PreviewActivity, 0, "com.wathakker.wear.PrayerComplicationService").onComplicationRequest(ComplicationRequest(102, ComplicationType.RANGED_VALUE, false))
                    PreviewComp(this@PreviewActivity, 8, "com.wathakker.wear.UpcomingCountdownComplication").onComplicationRequest(ComplicationRequest(201, ComplicationType.RANGED_VALUE, false))
                    PreviewComp(this@PreviewActivity, 1, "com.wathakker.wear.DateGregComplication").onComplicationRequest(ComplicationRequest(301, ComplicationType.SHORT_TEXT, false))
                }
            }
            // cross=70,130: arm both crossover boundaries from ONE launch (same force-stop reason).
            intent.getStringExtra("cross")?.let { spec ->
                for (part in spec.split(",")) part.toLongOrNull()?.let { sec ->
                    PreviewComp(this, k).scheduleBoundary(System.currentTimeMillis() + sec * 1000L)
                }
            }
            // eval_probe: deterministic instant sweep around exact-minute remainders and expiry,
            // evaluated through the platform getTextAt at controlled instants (no sleeps).
            if (intent.getBooleanExtra("eval_probe", false)) {
                val t0 = System.currentTimeMillis() / 60000 * 60000 + 30 * 60000L // aligned, +30min
                val text = PreviewComp(this@PreviewActivity, k, style = intent.getStringExtra("cd_style")).nativeCountdown(t0)
                for (off in longArrayOf(-180000, -120001, -120000, -119999, -61000, -60000, -59999, -1000, 0, 1, 1000)) {
                    val at = java.time.Instant.ofEpochMilli(t0 + off)
                    Log.i("WathakkerCd", "probe R=" + (-off) + "ms text=[" + text.getTextAt(resources, at) +
                        "] nextChange=+" + (text.getNextChangeTime(at).toEpochMilli() - (t0 + off)) + "ms")
                }
            }
            val type = if (k == 3) ComplicationType.LONG_TEXT else if (mode == "short" || k > 0) ComplicationType.SHORT_TEXT else ComplicationType.RANGED_VALUE
            val cdStyleExtra = intent.getStringExtra("cd_style")
            val data = if (mode == "prev") PreviewComp(this, k, style = cdStyleExtra).getPreviewData(type)!! else PreviewComp(this, k, style = cdStyleExtra).build(type, s, iq = iq)!!
            val cd = ComplicationDrawable(this)
            cd.activeStyle.rangedValuePrimaryColor = WTheme.cur.main.toArgb()
            cd.activeStyle.rangedValueSecondaryColor = 0xFF3A3420.toInt()
            cd.activeStyle.textColor = Color.WHITE
            cd.activeStyle.titleColor = WTheme.cur.main.toArgb()
            cd.setComplicationData(data, true)
            // Exact string the host will show, evaluated through the platform's ComplicationText.
            val ct = when (data) {
                is ShortTextComplicationData -> data.text
                is RangedValueComplicationData -> data.text
                else -> null
            }
            Log.i("WathakkerCd", "cd_eval mode=$mode kind=$k cds=$cds text=[" + (ct?.getTextAt(resources, java.time.Instant.now()) ?: "null") + "]")
            root.addView(object : View(this) {
                override fun onDraw(canvas: Canvas) {
                    val sz = (width * 0.40f).toInt(); val cx = width / 2; val cy = height / 2
                    cd.bounds = Rect(cx - sz / 2, cy - sz / 2, cx + sz / 2, cy + sz / 2)
                    cd.currentTime = java.time.Instant.now()
                    cd.draw(canvas)
                    postInvalidateDelayed(1000)
                }
            }, ViewGroup.LayoutParams(-1, -1))
        }
    }
}
