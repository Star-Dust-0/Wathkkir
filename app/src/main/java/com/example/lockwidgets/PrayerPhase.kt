package com.example.lockwidgets

/** Shared, clock-injected countdown state. Fixed iqama delays are unchanged. */
class PrayerPhase private constructor(
    @JvmField val targetIndex: Int,
    @JvmField val previousIndex: Int,
    @JvmField val start: Long,
    @JvmField val target: Long,
    @JvmField val iqama: Boolean,
) {
    fun label(names: Array<String>, code: String): String =
        if (iqama) iqamaFor(code, names[targetIndex]) else names[targetIndex]

    companion object {
        private val MINUTES = intArrayOf(25, 0, 20, 20, 10, 20)

        @JvmStatic
        fun iqamaMinutes(prayer: Int): Int = MINUTES[prayer]

        @JvmStatic
        fun window(today: LongArray, yesterday: LongArray?, now: Long, friday: Boolean): LongArray? {
            for (i in 5 downTo 0) {
                val end = today[i] + MINUTES[i] * 60000L
                if (MINUTES[i] > 0 && !(friday && i == 2) && today[i] <= now && now < end)
                    return longArrayOf(i.toLong(), today[i], end)
            }
            if (yesterday != null && yesterday[5] <= now && now < yesterday[5] + MINUTES[5] * 60000L)
                return longArrayOf(5, yesterday[5], yesterday[5] + MINUTES[5] * 60000L)
            return null
        }

        @JvmStatic
        fun resolve(today: LongArray, yesterday: LongArray, tomorrow: LongArray, now: Long, friday: Boolean, includeSunrise: Boolean): PrayerPhase {
            val w = window(today, yesterday, now, friday)
            if (w != null) return PrayerPhase(w[0].toInt(), w[0].toInt(), w[1], w[2], true)
            var previous = 5
            var start = yesterday[5]
            for (i in 0 until 6) {
                if (i == 1 && !includeSunrise) continue
                if (today[i] > now) return PrayerPhase(i, previous, start, today[i], false)
                previous = i
                start = today[i]
            }
            return PrayerPhase(0, 5, today[5], tomorrow[0], false)
        }

        @JvmStatic
        fun iqamaLabel(code: String): String {
            if (code.startsWith("ar")) return "الإقامة"
            if (code.startsWith("fr")) return "Iqama"
            if (code.startsWith("de")) return "Iqama"
            if (code.startsWith("tr")) return "Kamet"
            if (code.startsWith("ur")) return "اقامت"
            if (code.startsWith("id")) return "Iqamah"
            if (code.startsWith("ru")) return "Икама"
            return "Iqama"
        }

        /** "Iqama Dhuhr" in other languages; Arabic needs the preposition: الإقامة للظهر (ل + الظهر = للظهر). */
        @JvmStatic
        fun iqamaFor(code: String, name: String?): String {
            if (code.startsWith("ar")) {
                val n = name ?: ""
                return "الإقامة " + (if (n.startsWith("ال")) "لل" + n.substring(2) else "ل$n")
            }
            return iqamaLabel(code) + " " + name
        }
    }
}
