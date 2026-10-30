package com.wathakker.wear

import android.content.ComponentName
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

    // Custom typefaces are not allowed inside tiles (platform restriction), so the font choice reaches
    // the tile as size scale + bold weight only; the typeface stays the system font.
    private fun text(t: String, size: Float, color: Int, bold: Boolean) =
        LayoutElementBuilders.Text.Builder()
            .setText(t)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(sp(size * WFonts.scale)).setColor(argb(color))
                    .setWeight(if (bold || WFonts.bold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL)
                    .build()
            ).build()

    /** Platform-time expression, evaluated by the tile host without provider minute ticks. */
    private fun nativeCounter(s: PrayerState): LayoutElementBuilders.Text {
        val D = androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant.platformTimeWithSecondsPrecision()
            .durationUntil(androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant.withSecondsPrecision(java.time.Instant.ofEpochMilli(s.next.at))).toIntSeconds()
        val seconds = androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32.onCondition(D.lt(0)).use(0).elseUse(D)
        val two = androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32.IntFormatter.Builder().setMinIntegerDigits(2).setGroupingUsed(false).build()
        val value = androidx.wear.protolayout.expression.DynamicBuilders.DynamicString.constant(T("المتبقي ", "Left "))
            .concat(seconds.div(3600).format()).concat(androidx.wear.protolayout.expression.DynamicBuilders.DynamicString.constant(":"))
            .concat(seconds.div(60).rem(60).format(two))
        val fallback = T("المتبقي ", "Left ") + Prayer.remainingText(s.next.at-s.now)
        return LayoutElementBuilders.Text.Builder()
            .setText(androidx.wear.protolayout.TypeBuilders.StringProp.Builder(fallback).setDynamicValue(value).build())
            .setLayoutConstraintsForDynamicText(androidx.wear.protolayout.TypeBuilders.StringLayoutConstraint.Builder(T("المتبقي ", "Left ")+"00:00").build())
            .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setSize(sp(12f*WFonts.scale)).setColor(argb(tc.main)).setWeight(if(WFonts.bold)LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL).build()).build()
    }

    fun layout(s: PrayerState): LayoutElementBuilders.LayoutElement {
        val sweep = 270f
        val track = LayoutElementBuilders.ArcLine.Builder().setLength(degrees(sweep)).setThickness(dp(13f)).setColor(argb(tc.dark)).build()
        val fill = LayoutElementBuilders.ArcLine.Builder().setLength(degrees(sweep * s.progress)).setThickness(dp(13f)).setColor(argb(tc.main)).build()
        fun arc(line: LayoutElementBuilders.ArcLine) = LayoutElementBuilders.Arc.Builder()
            .setAnchorAngle(degrees(-135f)).setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START).addContent(line).build()

        fun gap(h: Float) = LayoutElementBuilders.Spacer.Builder().setHeight(dp(h)).build()
        val center = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(text(Prayer.time12(s.next.at), 24f, 0xFFFFFFFF.toInt(), false))
            .addContent(gap(2f))
            .addContent(text(s.next.name, 19f, 0xFFFFFFFF.toInt(), true))
            .addContent(gap(5f))
            .addContent(nativeCounter(s))
            .addContent(gap(7f))
            .addContent(text(Prayer.hijriDay(s.now), 26f, 0xFFFFFFFF.toInt(), true))
            .addContent(text(Prayer.hijriMonth(s.now), 12f, 0xFF9D9DA2.toInt(), false))
            .build()

        val click = ModifiersBuilders.Clickable.Builder()
            .setId("open")
            .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder().setPackageName(packageName).setClassName(MainActivity::class.java.name).build()
            ).build()).build()

        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand()).setHeight(expand())
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setPadding(ModifiersBuilders.Padding.Builder().setAll(dp(6f)).build()).setClickable(click).build())
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(click).build())
            .addContent(arc(track)).addContent(arc(fill)).addContent(center)
            .build()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        WFonts.load(this)
        val s = Prayer.compute(this)
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(60_000)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(s)))
            .build()
        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(ResourceBuilders.Resources.Builder().setVersion("1").build())
}
