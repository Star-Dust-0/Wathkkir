package com.example.prayer

import android.app.Activity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.example.R
import com.example.ThemeSettings
import com.example.ui.oneui.FrostSpec
import com.example.ui.oneui.blurSupported
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Time-of-day sky background for the prayer screen (v3.2.0).
 * Six bundled static skies, one per prayer period, crossfaded at the boundaries.
 * Static art: zero per-frame cost, no battery impact; haze blur is reused from
 * the existing oneUIGlass infrastructure.
 */
enum class DayPeriod(val drawableRes: Int, val darkScene: Boolean) {
    FAJR(R.drawable.sky_fajr, true),
    SHURUQ(R.drawable.sky_shuruq, false),
    DHUHR(R.drawable.sky_dhuhr, false),
    ASR(R.drawable.sky_asr, false),
    MAGHRIB(R.drawable.sky_maghrib, true),
    ISHA(R.drawable.sky_isha, true);

    companion object {
        /** All times are epoch millis for the same day (from the existing `today` object). */
        fun forTime(now: Long, fajr: Long, sunrise: Long, dhuhr: Long, asr: Long, maghrib: Long, isha: Long): DayPeriod = when {
            now < fajr -> ISHA
            now < sunrise -> FAJR
            now < dhuhr -> SHURUQ
            now < asr -> DHUHR
            now < maghrib -> ASR
            now < isha -> MAGHRIB
            else -> ISHA
        }
    }
}

/** Per-scene content colors so text stays legible over bright and dark skies. */
class SceneColors(
    val textPrimary: Color,
    val textSecondary: Color,
    val chip: Color,
    val divider: Color,
    val accent: Color,
    // Current-prayer row (build 50): scene-aware so the green stays readable on light
    // skies - #62D484 on a bright day sky fails contrast badly (his report).
    val nextName: Color,
    val nextTime: Color,
    val countdown: Color,
    val glassBase: Color,
    val glassAlpha: Float,
    val glassBorder: Color,
    val track: Color
)

fun sceneColors(darkScene: Boolean): SceneColors =
    if (darkScene) SceneColors(
        textPrimary = Color.White,
        textSecondary = Color(0xB8FFFFFF),
        chip = Color(0x24FFFFFF),
        divider = Color(0x24FFFFFF),
        accent = Color(0xFF3E91FF),
        nextName = Color(0xFF62D484),
        nextTime = Color(0xFF3E91FF),
        countdown = Color(0xFF59C8DC),
        glassBase = Color(0xFF17171B),
        glassAlpha = 0.46f,
        glassBorder = Color(0x29FFFFFF),
        track = Color(0xFF38383E)
    ) else SceneColors(
        textPrimary = Color(0xFF111114),
        textSecondary = Color(0xA81E1E22),
        chip = Color(0x6BFFFFFF),
        divider = Color(0x1A000000),
        accent = Color(0xFF0381FE),
        // Dark green / deeper blue on light skies for the current-prayer row.
        nextName = Color(0xFF1E7E34),
        nextTime = Color(0xFF0266C8),
        // Deeper than OneUITimeCyan: #59C8DC fails contrast on bright frosted glass.
        countdown = Color(0xFF1E8FA8),
        glassBase = Color.White,
        glassAlpha = 0.55f,
        glassBorder = Color(0x73FFFFFF),
        track = Color(0xFFC4C4CD)
    )

/**
 * Frosted card over the sky: same recipe as oneUIGlass (28dp blur, 0.04 noise) but
 * against the prayer screen's own sky HazeState and with a lower tint alpha so the
 * sky reads through. Falls back to a near-opaque surface on API < 31.
 */
fun Modifier.skyGlass(shape: Shape, haze: HazeState?, colors: SceneColors): Modifier =
    // Build 50+ (his call): NO border on the sky glass - the light hairline read as
    // an annoying white edge over bright skies. Frosted fill only.
    if (haze != null && blurSupported) {
        this.clip(shape)
            .hazeEffect(haze, HazeStyle(colors.glassBase, HazeTint(colors.glassBase.copy(alpha = colors.glassAlpha)), 28.dp, 0.04f))
    } else {
        this.clip(shape).background(colors.glassBase.copy(alpha = 0.96f))
    }

/**
 * The sky itself, crossfaded when the period flips at a prayer-time boundary.
 * Feeds both the card haze and the shell haze (bottom bar, header pills, drawer),
 * so every frosted surface blurs the sky behind it.
 */
@Composable
fun TimeOfDayBackground(period: DayPeriod, haze: HazeState, shellHaze: HazeState? = null) {
    Crossfade(targetState = period, animationSpec = tween(1400), label = "sky") { p ->
        Image(
            painter = painterResource(p.drawableRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(haze)
                .then(if (shellHaze != null) Modifier.hazeSource(shellHaze) else Modifier),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * The prayer screen's current frost material, shared with the shell (bottom bar,
 * drawer) while the prayer tab is visible. Null off the prayer screen: those
 * surfaces keep their default look everywhere else. Set/cleared by PrayerTimesContent.
 */
object PrayerSkyGlass {
    private val _spec = androidx.compose.runtime.mutableStateOf<FrostSpec?>(null)
    val spec: androidx.compose.runtime.State<FrostSpec?> get() = _spec
    fun set(spec: FrostSpec?) { _spec.value = spec }
}

/**
 * Status-bar icon contrast follows the sky (light icons on dark scenes).
 * Restores the theme default when leaving the prayer screen.
 */
@Composable
fun SkyStatusBarEffect(darkScene: Boolean) {
    val view = LocalView.current
    val themeDark = ThemeSettings.isDark()
    DisposableEffect(darkScene, themeDark) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        controller?.isAppearanceLightStatusBars = !darkScene
        onDispose { controller?.isAppearanceLightStatusBars = !themeDark }
    }
}
