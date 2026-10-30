package com.wathakker.wear

import android.content.ComponentName
import androidx.wear.protolayout.expression.DynamicBuilders
import androidx.wear.protolayout.TypeBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.compose.ui.graphics.toArgb
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** The gold arc watch widget: arc progress between the previous and the next prayer, next prayer in the middle. */
open class PrayerTileService : TileService() {

    private class TC(val main: Int, val dark: Int)
    private val tc: TC get() {
        val d = WTheme.defs[Settings.themeIdx(this).coerceIn(0, WTheme.defs.size - 1)]
        val m = d.main.toArgb()
        return TC(m, androidx.core.graphics.ColorUtils.blendARGB(0xFF000000.toInt(), m, 0.22f))
    }

    // Custom typefaces are not allowed inside tiles (platform restriction): the typeface stays the
    // system font. The tile also ignores the in-app font-SIZE setting on purpose (his 19:43-19:45
    // review): a tile is fixed real estate, and scaling its text pushed the Hijri date off screen.
    // Same for the SYSTEM font scale: his watch rendered the tile content ~1.35x after a system
    // font change, which overflowed the layout. Dividing by the system fontScale keeps the tile's
    // designed geometry identical on every watch (glanceable fixed layout, like a watch face).
    private val fontScale: Float get() = resources.configuration.fontScale
    private fun text(t: String, size: Float, color: Int, bold: Boolean, dynamic: DynamicBuilders.DynamicString? = null) =
        LayoutElementBuilders.Text.Builder()
            .setText(TypeBuilders.StringProp.Builder(t).apply { if(dynamic != null) setDynamicValue(dynamic) }.build())
            .apply { if(dynamic != null) setLayoutConstraintsForDynamicText(TypeBuilders.StringLayoutConstraint.Builder(T("المتبقي 00:00:00", "Left 00:00:00")).build()) }
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(sp(size / fontScale)).setColor(argb(color))
                    .setWeight(if (bold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL)
                    .build()
            ).build()

    fun layout(s: PrayerState, iq: Prayer.Iqama? = null, native: Boolean = false): LayoutElementBuilders.LayoutElement {
        val sweep = 270f
        // During an iqama window the arc and center switch to the iqama countdown (owner feature, wear 1.1.0).
        val arcProgress = if (iq != null) ((s.now - iq.prayer.at).toFloat() / maxOf(1L, iq.at - iq.prayer.at).toFloat()).coerceIn(0.02f, 1f) else s.progress
        // One Arc with sequential contents: elapsed fill first from the anchor, then the remaining track.
        // (Two sibling Arcs get concatenated by some renderers, which drew the fill after the track's end.)
        val fillLen = sweep * arcProgress
        val begin = iq?.prayer?.at ?: s.prev.at
        val target = iq?.at ?: s.next.at
        val now = DynamicBuilders.DynamicInstant.platformTimeWithSecondsPrecision()
        val elapsed = DynamicBuilders.DynamicInstant.withSecondsPrecision(java.time.Instant.ofEpochMilli(begin)).durationUntil(now).toIntSeconds().asFloat()
        val raw = elapsed.div(maxOf(1f,(target-begin)/1000f)).times(sweep)
        val low = DynamicBuilders.DynamicFloat.onCondition(raw.lt(sweep*0.02f)).use(sweep*0.02f).elseUse(raw)
        val progress = DynamicBuilders.DynamicFloat.onCondition(low.gt(sweep)).use(sweep).elseUse(low)
        val fill = LayoutElementBuilders.ArcLine.Builder()
            .setLength(DimensionBuilders.DegreesProp.Builder(fillLen).apply { if(native) setDynamicValue(progress) }.build())
            .apply { if(native) setLayoutConstraintsForDynamicLength(DimensionBuilders.AngularLayoutConstraint.Builder(sweep).setAngularAlignment(LayoutElementBuilders.ANGULAR_ALIGNMENT_START).build()) }
            .setThickness(dp(13f)).setColor(argb(tc.main)).build()
        // A fixed background arc and a start-aligned dynamic overlay avoid reserving
        // two full dynamic lengths sequentially in the same Arc.
        val track = LayoutElementBuilders.ArcLine.Builder().setLength(degrees(sweep)).setThickness(dp(13f)).setColor(argb(tc.dark)).build()
        fun arc(line: LayoutElementBuilders.ArcLine) = LayoutElementBuilders.Arc.Builder()
            .setAnchorAngle(degrees(-135f)).setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START).addContent(line).build()
        val seconds0=now.durationUntil(DynamicBuilders.DynamicInstant.withSecondsPrecision(java.time.Instant.ofEpochMilli(target))).toIntSeconds()
        val seconds=DynamicBuilders.DynamicInt32.onCondition(seconds0.lt(0)).use(0).elseUse(seconds0)
        val two=DynamicBuilders.DynamicInt32.IntFormatter.Builder().setMinIntegerDigits(2).setGroupingUsed(false).build()
        val numeric=seconds.div(3600).format().concat(DynamicBuilders.DynamicString.constant(":"))
            .concat(seconds.div(60).rem(60).format(two)).concat(DynamicBuilders.DynamicString.constant(":"))
            .concat(seconds.rem(60).format(two))
        val remaining=DynamicBuilders.DynamicString.constant(T("المتبقي ","Left ")).concat(numeric)

        fun gap(h: Float) = LayoutElementBuilders.Spacer.Builder().setHeight(dp(h)).build()
        // Recovered Oct26 tile geometry; native ring/text behavior remains separate.
        val center = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(ModifiersBuilders.Modifiers.Builder()
                .setPadding(ModifiersBuilders.Padding.Builder().setTop(dp(18f)).build()).build())
            .addContent(text(Prayer.time12(iq?.at ?: s.next.at), 25.5f, 0xFFFFFFFF.toInt(), false))
            .addContent(gap(2.5f))
            .addContent(text(if (iq != null) T("إقامة ", "Iqama ") + iq.prayer.name else s.next.name, 19.5f, 0xFFFFFFFF.toInt(), false))
            .addContent(gap(4.5f))
            .addContent(text(T("المتبقي ", "Left ") + Prayer.remainingText((iq?.at ?: s.next.at) - s.now), 14.5f, tc.main, true, if(native) remaining else null))
            .addContent(gap(4f))
            .addContent(text(Prayer.hijriDay(s.now), 30f, 0xFFFFFFFF.toInt(), true))
            .addContent(gap(2f))
            .addContent(text(Prayer.hijriMonth(s.now), 14.5f, 0xFF9D9DA2.toInt(), false))
            .build()

        val click = ModifiersBuilders.Clickable.Builder()
            .setId("open")
            .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder().setPackageName(packageName).setClassName(MainActivity::class.java.name).build()
            ).build()).build()

        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand()).setHeight(expand())
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setPadding(ModifiersBuilders.Padding.Builder().setAll(dp(6f)).build()).setClickable(click).build())
            .addContent(arc(track)).addContent(arc(fill)).addContent(center)
            .build()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        WFonts.load(this)
        val s = Prayer.compute(this)
        val iq = Prayer.iqamaNow(this)
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(60_000)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(s, iq, requestParams.deviceConfiguration.rendererSchemaVersion.let { it.major > 1 || (it.major == 1 && it.minor >= 200) })))
            .build()
        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(ResourceBuilders.Resources.Builder().setVersion("1").build())
}
