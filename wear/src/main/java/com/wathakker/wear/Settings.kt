package com.wathakker.wear

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Theme colors: 0 gold (default), 1 turquoise (app), 2 One UI blue, 3 One UI green. Gradient values come from the user's One UI screenshots. */
class ThemeDef(val name: String, val c1: Long, val c2: Long) {
    val start get() = Color(c1)
    val end get() = Color(c2)
    val main get() = Color(androidx.core.graphics.ColorUtils.blendARGB(c1.toInt(), c2.toInt(), 0.5f))
    fun brush() = Brush.horizontalGradient(listOf(start, end))
}

object WTheme {
    val defs = listOf(
        ThemeDef("ذهبي", 0xFFD6B64A, 0xFFD6B64A),
        ThemeDef("فيروزي", 0xFF44CCC0, 0xFF169498),
        ThemeDef("أزرق", 0xFF3E9FFE, 0xFF3580F8),
        ThemeDef("أخضر", 0xFF69D043, 0xFF07C364)
    )
    var idx by mutableIntStateOf(0)
    val cur get() = defs[idx.coerceIn(0, defs.size - 1)]
}

object Settings {
    private const val P = "wear_prefs"
    /** Names of the background patterns shown in the Appearance list, with their internal ids. */
    val patterns = listOf(1 to "ربع الحزب", 8 to "نجوم ومعينات", 9 to "شبكة سداسية", 10 to "نجمة ثمانية متقاطعة", 11 to "أرابيسك دائري")

    fun load(c: Context) {
        val p = c.getSharedPreferences(P, Context.MODE_PRIVATE)
        Loc.refresh(c)
        starPattern = p.getInt("pat", 1).let { v -> if (patterns.any { it.first == v }) v else 1 }
        WTheme.idx = p.getInt("theme", 0)
        WFonts.load(c)
    }
    fun savePattern(c: Context, v: Int) { starPattern = v; c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putInt("pat", v).apply() }
    fun saveTheme(c: Context, v: Int) { WTheme.idx = v; c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putInt("theme", v).apply() }
    fun themeIdx(c: Context) = c.getSharedPreferences(P, Context.MODE_PRIVATE).getInt("theme", 0)
    /** 0 = sync from the phone, 1 = automatic (GPS on the watch). */
    fun locMode(c: Context) = c.getSharedPreferences(P, Context.MODE_PRIVATE).getInt("locmode", 0)
    fun saveLocMode(c: Context, v: Int) { c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putInt("locmode", v).apply(); try { requestAllComplications(c) } catch (_: Throwable) {} }
    fun saveGps(c: Context, lat: Double, lon: Double) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
            .putLong("gps_lat", java.lang.Double.doubleToLongBits(lat)).putLong("gps_lon", java.lang.Double.doubleToLongBits(lon)).apply()
        try { Adhan.schedule(c) } catch (_: Throwable) {}
        try { requestAllComplications(c) } catch (_: Throwable) {}
    }
}

/** Watch UI language: 0 = sync (phone language; Arabic until the phone sends one), 1 = Arabic, 2 = English. */
object Loc {
    @Volatile var en = false
    fun refresh(c: Context) {
        val m = c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).getInt("lang", 0)
        en = when (m) { 1 -> false; 2 -> true; else -> c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).getString("phone_lang", "ar") == "en" }
    }
    fun mode(c: Context) = c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).getInt("lang", 0)
    fun save(c: Context, v: Int) { c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).edit().putInt("lang", v).apply(); refresh(c); langTick++; try { requestAllComplications(c) } catch (_: Throwable) {} }
}
var langTick by mutableIntStateOf(0)
fun T(ar: String, en: String) = if (Loc.en) en else ar
