package com.example.ui.i18n

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/** Supported UI languages. Azkar/Hisn content stays Arabic; this translates UI chrome only. */
enum class Lang(val code: String, val nativeName: String, val rtl: Boolean) {
    AR("ar", "العربية", true),
    EN("en", "English", false),
    FR("fr", "Français", false),
    DE("de", "Deutsch", false),
    ES("es", "Español", false),
    IT("it", "Italiano", false),
    TR("tr", "Türkçe", false),
    UR("ur", "اردو", true),
    ID("id", "Indonesia", false),
    RU("ru", "Русский", false),
    PT("pt", "Português", false);

    companion object {
        fun fromCode(code: String?): Lang = entries.firstOrNull { it.code == code } ?: AR
    }
}

/** String keys for every translatable UI literal. */
enum class S {
    PrayerTab, AzkarTab, HisnTab, FajrTab, TasbeehTab, Favorites, Reminders, Stats,
    Tags, Settings, About, More, Menu, AddDhikr, SearchAzkar, All,
    NoAzkar, NoAzkarUnderTag, NoResultsQuery, Options, EditDhikrTags, ReminderTimes, Delete, NoActiveReminders,
    Today, ThisWeek, DayStreak, CurrentReminder, NextReminder, NoActiveAzkar, Shortcuts, TasbeehCounter,
    HisnSubtitle, FavSubtitle, ReadDone, TotalSuffix, NoReadsYet, DhikrIdFmt, Periods, Week,
    Month, KeepGoing, TotalReads, SinceStart, RecentReads, RemindersSubtitle, StatsSubtitle, TagsSubtitle,
    FajrAlarm, ByLocation, SettingsSubtitle, AboutSubtitle, DeleteTag, DeleteTagMsg, AddTag, NoTags,
    Edit, NewTag, EditTag, TagName, Color, TasbeehaFmt, TapToCount, ResetCounter,
    ActiveOfTotalFmt, NoReminders, NoFavorites, Light, Dark, SystemAuto, LockWidgets, LockWidgetsToast,
    OpenEditor, Close, LockWidgetsDesc, LockWidgetsSteps, LockWidgetsHint, ChangeLocation, LocationSubtitle, FajrAlarmSubtitle,
    LockScreenSection, LockWidgetsRowSub, Notifications, NotifSound, Default, Vibration, Appearance, AppFont,
    Language, NewDhikr, EditDhikr, TitleLabel, DhikrText, Cancel, Add, AddReminderTime,
    RepeatReminder, ReminderTime, Save, Apply, ChaptersFmt, SearchHisn, NoFavChapters, NoResults,
    ReadTodaySuffix, AddToMyAzkar, AddedToMyAzkar, Done, Tagline, Version, Developer, DevName,
    HisnCredit, InstinctCredit, Dua, Font, FontLight, FontBold, FontSystem, FontCairo,
    FontNaskh, FontAmiri, FontRuqaa, PrayerFajr, PrayerShuruq, PrayerDhuhr, PrayerJumuah, PrayerAsr,
    PrayerMaghrib, PrayerIsha, PrayerTimes, NextPrayer, AmMarker, PmMarker, PickCountryCity, SearchCityHint,
    NoSearchResults, FajrPrayer, FajrNow, Snooze, StopAlarm, ChooseSnooze, MinutesShortFmt, ColorWhite,
    Auto, ColorBlack, ColorPurple, BgGlass, BgWallBlur, ColorGray, ColorBlue, ColorGreen,
    ColorAmber, ColorBlueDark, ColorGreenDark, WidgetSettings, BarColor, BgColor, WidgetPattern, NoPattern,
    PickRingtone, LocationMethod, CurrentLocation, AutoUpdateFajr, PickCitySub, CurrentCity, ChangeCity, NoLocPermission,
    GpsOff, CalcMethod, SelectedCalcMethod, Change, UpdatingTime, UpdateFajrNow, AlarmToneSection, SelectedTone,
    SystemTones, AudioFile, ExtraSettings, VibrateDuringAlarm, NextFajrAt, AlarmOnDesc, AlarmOffDesc, PickCalcMethod,
    NotifReminder, NotifReadDone, NotifRemindLater, NotifOpenApp, NotifChannelAzkar, NotifChannelFajr, TodayAt, TomorrowAt,
    DefaultAlarmTone, LocCalcTitle, LocCalcSubtitle, QiblaTitle, QiblaNorthFmt, QiblaSetLocation, QiblaNoSensor, QiblaCalibrate,
    QiblaAligned, QiblaCityFmt, DirN, DirNE, DirE, DirSE, DirS, DirSW,
    DirW, DirNW, CalendarTab, CalEvents, CalNoEvents, CalMonthEvents, CalReminderOn, CalReminderOff,
    WdSun, WdMon, WdTue, WdWed, WdThu, WdFri, WdSat, AutoDnd,
    AutoDndSub, OptTimesTitle, OptTimesSub, Imsak, Duha, Midnight, Qiyam, LocSection,
    CalcSection, AsrJuristic, AsrShafii, AsrHanafi, HighLatRule, HighLatNone, HighLatMiddle, HighLatSeventh,
    HighLatAngle, AdjSection, AdjNote, AdjHijri, AdjMinShort, AdjDayShort, AlertTiming, AtAdhanTime,
    MinBeforeFmt, AdhanReminder, IqamaReminder, IqamaReminderSub, PrayerNowFmt, PrayerApproachingFmt, ShuruqNow, ShuruqApproaching,
    BgFrosted, NeedsFilesPerm, ColorYellow, ColorTurquoise, WidgetAccent, PermLocTitle, PermLocBody, PermFilesTitle,
    PermFilesBody, PermAllow, PermNotNow, PermOpenSettings, IqamaNowFmt, SharedAlertSettings, HijriConvTitle, HijriConvSub,
    GregToHijri, HijriToGreg, DayLabel, MonthLabel, YearLabel, Back, AtAdhanRow, AtAdhanRowSub,
    BeforeAdhanRow, EditCityName, ResetCityNameAuto, CustomLocationNameHint, BeforeAdhanHelper, WidgetTapTitle, WidgetTapOpenApp, WidgetTapNothing,
    WidgetTapRefresh
}

object L10n {
    private const val PREFS = "app_prefs"
    private const val KEY = "language"

    private val _lang = mutableStateOf(Lang.AR)
    val lang: Lang get() = _lang.value
    val isRtl: Boolean get() = _lang.value.rtl

    fun init(context: Context) {
        _lang.value = Lang.fromCode(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "ar"))
    }

    fun set(context: Context, lang: Lang) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, lang.code).apply()
        _lang.value = lang
    }

    fun t(key: S): String {
        val l = _lang.value
        if (l == Lang.AR) return AR[key] ?: key.name
        return table(l)[key] ?: EN[key] ?: AR[key] ?: key.name
    }

    // Count helpers with correct per-language grammar. Arabic keeps its full plural system.
    fun azkarCount(n: Int): String = when (_lang.value) {
        Lang.AR -> when { n == 0 -> "لا أذكار"; n == 1 -> "ذكر واحد"; n == 2 -> "ذكران"; n in 3..10 -> "$n أذكار"; else -> "$n ذكرًا" }
        Lang.EN -> if (n == 1) "1 dhikr" else "$n azkar"
        Lang.FR -> if (n == 1) "1 dhikr" else "$n azkar"
        Lang.DE -> if (n == 1) "1 Dhikr" else "$n Dhikr"
        Lang.ES -> if (n == 1) "1 dhikr" else "$n azkar"
        Lang.IT -> if (n == 1) "1 dhikr" else "$n azkar"
        Lang.TR -> "$n zikir"
        Lang.UR -> if (n == 1) "1 ذکر" else "$n اذکار"
        Lang.ID -> "$n dzikir"
        Lang.RU -> when { n % 10 == 1 && n % 100 != 11 -> "$n зикр"; n % 10 in 2..4 && n % 100 !in 12..14 -> "$n зикра"; else -> "$n зикров" }
        Lang.PT -> if (n == 1) "1 dhikr" else "$n azkar"
    }

    fun readsCount(n: Int): String = when (_lang.value) {
        Lang.AR -> when { n == 0 -> "لا قراءات"; n == 1 -> "قراءة واحدة"; n == 2 -> "قراءتان"; n in 3..10 -> "$n قراءات"; else -> "$n قراءة" }
        Lang.EN -> if (n == 1) "1 read" else "$n reads"
        Lang.FR -> if (n == 1) "1 lecture" else "$n lectures"
        Lang.DE -> if (n == 1) "1 Lesung" else "$n Lesungen"
        Lang.ES -> if (n == 1) "1 lectura" else "$n lecturas"
        Lang.IT -> if (n == 1) "1 lettura" else "$n letture"
        Lang.TR -> if (n == 1) "1 okuma" else "$n okuma"
        Lang.UR -> if (n == 1) "1 قراءت" else "$n قراءتیں"
        Lang.ID -> if (n == 1) "1 bacaan" else "$n bacaan"
        Lang.RU -> when { n % 10 == 1 && n % 100 != 11 -> "$n чтение"; n % 10 in 2..4 && n % 100 !in 12..14 -> "$n чтения"; else -> "$n чтений" }
        Lang.PT -> if (n == 1) "1 leitura" else "$n leituras"
    }

    fun minutesCount(n: Int): String = when (_lang.value) {
        Lang.AR -> if (n == 1) "دقيقة واحدة" else if (n == 2) "دقيقتان" else if (n in 3..10) "$n دقائق" else "$n دقيقة"
        Lang.EN, Lang.FR -> if (n == 1) "1 minute" else "$n minutes"
        Lang.DE -> if (n == 1) "1 Minute" else "$n Minuten"
        Lang.ES, Lang.PT -> if (n == 1) "1 minuto" else "$n minutos"
        Lang.IT -> if (n == 1) "1 minuto" else "$n minuti"
        Lang.TR -> "$n dakika"
        Lang.UR -> "$n منٹ"
        Lang.ID -> "$n menit"
        Lang.RU -> { val a = n % 10; val b = n % 100; if (a == 1 && b != 11) "$n минута" else if (a in 2..4 && b !in 12..14) "$n минуты" else "$n минут" }
    }

    fun daysCount(n: Int): String = when (_lang.value) {
        Lang.AR -> when { n == 0 -> "لا أيام"; n == 1 -> "يوم واحد"; n == 2 -> "يومان"; n in 3..10 -> "$n أيام"; else -> "$n يومًا" }
        Lang.EN -> if (n == 1) "1 day" else "$n days"
        Lang.FR -> if (n == 1) "1 jour" else "$n jours"
        Lang.DE -> if (n == 1) "1 Tag" else "$n Tage"
        Lang.ES -> if (n == 1) "1 día" else "$n días"
        Lang.IT -> if (n == 1) "1 giorno" else "$n giorni"
        Lang.TR -> "$n gün"
        Lang.UR -> if (n == 1) "1 دن" else "$n دن"
        Lang.ID -> "$n hari"
        Lang.RU -> when { n % 10 == 1 && n % 100 != 11 -> "$n день"; n % 10 in 2..4 && n % 100 !in 12..14 -> "$n дня"; else -> "$n дней" }
        Lang.PT -> if (n == 1) "1 dia" else "$n dias"
    }

    fun remindersCount(n: Int): String = when (_lang.value) {
        Lang.AR -> if (n in 3..10) "$n تنبيهات مفعلة" else "$n تنبيهًا مفعلة"
        Lang.EN -> if (n == 1) "1 reminder active" else "$n reminders active"
        Lang.FR -> if (n == 1) "1 rappel actif" else "$n rappels actifs"
        Lang.DE -> if (n == 1) "1 Erinnerung aktiv" else "$n Erinnerungen aktiv"
        Lang.ES -> if (n == 1) "1 recordatorio activo" else "$n recordatorios activos"
        Lang.IT -> if (n == 1) "1 promemoria attivo" else "$n promemoria attivi"
        Lang.TR -> "$n hatırlatıcı etkin"
        Lang.UR -> if (n == 1) "1 یاد دہانی فعال" else "$n یاد دہانیاں فعال"
        Lang.ID -> "$n pengingat aktif"
        Lang.RU -> when { n % 10 == 1 && n % 100 != 11 -> "$n напоминание активно"; n % 10 in 2..4 && n % 100 !in 12..14 -> "$n напоминания активны"; else -> "$n напоминаний активны" }
        Lang.PT -> if (n == 1) "1 lembrete ativo" else "$n lembretes ativos"
    }

    fun tagsCount(n: Int): String = when (_lang.value) {
        Lang.AR -> if (n in 3..10) "$n تصنيفات" else "$n تصنيف"
        else -> "$n " + t(S.Tags).lowercase()
    }
}
