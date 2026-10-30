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

    // Custom typefaces are not allowed inside tiles (platform restriction): the typeface stays the
    // system font. The tile also ignores the in-app font-SIZE setting on purpose (his 19:43-19:45
    // review): a tile is fixed real estate, and scaling its text pushed the Hijri date off screen.
    // Same for the SYSTEM font scale: his watch rendered the tile content ~1.35x after a system
    // font change, which overflowed the layout. Dividing by the system fontScale keeps the tile's
    // designed geometry identical on every watch (glanceable fixed layout, like a watch face).
    private val fontScale: Float get() = resources.configuration.fontScale
    private fun text(t: String, size: Float, color: Int, bold: Boolean) =
        LayoutElementBuilders.Text.Builder()
            .setText(t)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(sp(size / fontScale)).setColor(argb(color))
                    .setWeight(if (bold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL)
                    .build()
            ).build()

    fun layout(s: PrayerState, iq: Prayer.Iqama? = null): LayoutElementBuilders.LayoutElement {
        val sweep = 270f
        // During an iqama window the arc and center switch to the iqama countdown (owner feature, wear 1.1.0).
        val arcProgress = if (iq != null) ((s.now - iq.prayer.at).toFloat() / maxOf(1L, iq.at - iq.prayer.at).toFloat()).coerceIn(0.02f, 1f) else s.progress
        val track = LayoutElementBuilders.ArcLine.Builder().setLength(degrees(sweep)).setThickness(dp(16f)).setColor(argb(tc.dark)).build()
        val fill = LayoutElementBuilders.ArcLine.Builder().setLength(degrees(sweep * arcProgress)).setThickness(dp(16f)).setColor(argb(tc.main)).build()
        fun arc(line: LayoutElementBuilders.ArcLine) = LayoutElementBuilders.Arc.Builder()
            .setAnchorAngle(degrees(-135f)).setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START).addContent(line).build()

        fun gap(h: Float) = LayoutElementBuilders.Spacer.Builder().setHeight(dp(h)).build()
        // Layout measured against the reference gold widget ON THE REAL WATCH (Samsung font renders
        // ~1.4x taller than the emulator, so constants are scaled ~1.16x (0.776 * 1.30 * 1.15, i.e. +49.5% over installed build) to land on the reference
        // spacing on the actual device; the 454px emulator shows it proportionally smaller).
        val center = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(ModifiersBuilders.Modifiers.Builder()
                .setPadding(ModifiersBuilders.Padding.Builder().setTop(dp(16.5f)).build()).build())
            .addContent(text(Prayer.time12(iq?.at ?: s.next.at), 25.5f, 0xFFFFFFFF.toInt(), false))
            .addContent(gap(5f))
            .addContent(text(if (iq != null) T("إقامة ", "Iqama ") + iq.prayer.name else s.next.name, 19.5f, 0xFFFFFFFF.toInt(), false))
            .addContent(gap(9.7f))
            .addContent(text(T("المتبقي ", "Left ") + Prayer.remainingText((iq?.at ?: s.next.at) - s.now), 14.5f, tc.main, true))
            .addContent(gap(8f))
            .addContent(text(Prayer.hijriDay(s.now), 30f, 0xFFFFFFFF.toInt(), true))
            .addContent(gap(3.3f))
            .addContent(text(Prayer.hijriMonth(s.now), 14.5f, 0xFF9D9DA2.toInt(), false))
            .build()

        val click = ModifiersBuilders.Clickable.Builder()
            .setId("open")
            .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder().setPackageName(packageName).setClassName(MainActivity::class.java.name).build()
            ).build()).build()

        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand()).setHeight(expand())
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(click).build())
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
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(s, iq)))
            .build()
        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(ResourceBuilders.Resources.Builder().setVersion("1").build())
}
