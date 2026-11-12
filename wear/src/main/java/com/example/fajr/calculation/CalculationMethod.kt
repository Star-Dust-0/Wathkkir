package com.example.fajr.calculation

/** Prayer-time calculation methods (angles and isha rules per authority). */
enum class CalculationMethod(
    val id: Int,
    val titleAr: String,
    val fajrAngle: Double,
    val ishaAngle: Double?,
    val ishaMinutesAfterMaghrib: Int?,
    val titleEn: String,
) {
    MUSLIM_WORLD_LEAGUE(0, "رابطة العالم الإسلامي (18°)", 18.0, 17.0, null, "Muslim World League (18°)"),
    EGYPTIAN(1, "الهيئة المصرية العامة للمساحة (19.5°)", 19.5, 17.5, null, "Egyptian General Authority of Survey (19.5°)"),
    UMM_AL_QURA(2, "أم القرى - مكة المكرمة (18.5°)", 18.5, null, 90, "Umm al-Qura, Makkah (18.5°)"),
    ISNA(3, "الجمعية الإسلامية لشمال أمريكا (15°)", 15.0, 15.0, null, "Islamic Society of North America (15°)"),
    KARACHI(4, "جامعة العلوم الإسلامية بكراتشي (18°)", 18.0, 18.0, null, "Univ. of Islamic Sciences, Karachi (18°)"),
    DUBAI(5, "دبي (18.2°)", 18.2, 18.2, null, "Dubai (18.2°)"),
    KUWAIT(6, "الكويت (18°)", 18.0, 17.5, null, "Kuwait (18°)"),
    QATAR(7, "قطر (18°)", 18.0, null, 90, "Qatar (18°)");

    fun title(lang: String): String = if (lang == "ar") titleAr else titleEn

    companion object INSTANCE {
        fun fromId(id: Int): CalculationMethod = values().firstOrNull { it.id == id } ?: MUSLIM_WORLD_LEAGUE
    }
}
