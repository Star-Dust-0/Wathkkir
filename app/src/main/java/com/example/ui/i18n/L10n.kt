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
    PrayerTab, AzkarTab, HisnTab, FajrTab, TasbeehTab, Favorites, Reminders, Stats, Tags, Settings, About, More, Menu,
    AddDhikr, SearchAzkar, All, NoAzkar, NoAzkarUnderTag, NoResultsQuery, Options, EditDhikrTags, ReminderTimes, Delete,
    NoActiveReminders, Today, ThisWeek, DayStreak, CurrentReminder, NextReminder, NoActiveAzkar, Shortcuts,
    TasbeehCounter, HisnSubtitle, FavSubtitle, ReadDone,
    TotalSuffix, NoReadsYet, DhikrIdFmt, Periods, Week, Month, KeepGoing, TotalReads, SinceStart, RecentReads,
    RemindersSubtitle, StatsSubtitle, TagsSubtitle, FajrAlarm, ByLocation, SettingsSubtitle, AboutSubtitle,
    DeleteTag, DeleteTagMsg, AddTag, NoTags, Edit, NewTag, EditTag, TagName, Color,
    TasbeehaFmt, TapToCount, ResetCounter, ActiveOfTotalFmt, NoReminders, NoFavorites,
    Light, Dark, SystemAuto, LockWidgets, LockWidgetsToast, OpenEditor, Close, LockWidgetsDesc, LockWidgetsSteps,
    LockWidgetsHint, ChangeLocation, LocationSubtitle, FajrAlarmSubtitle, LockScreenSection, LockWidgetsRowSub,
    Notifications, NotifSound, Default, Vibration, Appearance, AppFont, Language,
    NewDhikr, EditDhikr, TitleLabel, DhikrText, Cancel, Add, AddReminderTime, RepeatReminder, ReminderTime, Save, Apply,
    ChaptersFmt, SearchHisn, NoFavChapters, NoResults, ReadTodaySuffix, AddToMyAzkar, AddedToMyAzkar, Done,
    Tagline, Version, Developer, DevName, HisnCredit, InstinctCredit, Dua,
    Font, FontLight, FontBold, FontSystem, FontCairo, FontNaskh, FontAmiri, EditCityName, ResetCityNameAuto, FontRuqaa,
    Back, LocCalcTitle, LocCalcSubtitle, LocSection, CalcSection, AsrJuristic, AsrShafii, AsrHanafi, HighLatRule, HighLatNone, HighLatMiddle, HighLatSeventh, HighLatAngle, AdjSection, AdjNote, AdjHijri, AdjMinShort, AdjDayShort,
    WidgetTapTitle, WidgetTapOpenApp, WidgetTapNothing, WidgetTapRefresh, PrayerFajr, PrayerShuruq, PrayerDhuhr, PrayerJumuah, PrayerAsr, PrayerMaghrib, PrayerIsha, PrayerTimes, NextPrayer, AmMarker, PmMarker, PickCountryCity, SearchCityHint, NoSearchResults, FajrPrayer, FajrNow, Snooze, StopAlarm, ChooseSnooze, MinutesShortFmt, ColorWhite, Auto, ColorBlack, BgGlass, BgWallBlur, ColorGray, ColorBlue, ColorGreen, ColorPurple, ColorAmber, ColorBlueDark, ColorYellow, ColorTurquoise, WidgetAccent, ColorGreenDark, WidgetSettings, BarColor, BgColor, WidgetPattern, NoPattern, PickRingtone, LocationMethod, CurrentLocation, AutoUpdateFajr, PickCitySub, CurrentCity, CustomLocationNameHint, ChangeCity, NoLocPermission, GpsOff, CalcMethod, SelectedCalcMethod, Change, UpdatingTime, UpdateFajrNow, AlarmToneSection, SelectedTone, SystemTones, AudioFile, ExtraSettings, VibrateDuringAlarm, NextFajrAt, AlarmOnDesc, AlarmOffDesc, PickCalcMethod, NotifReminder, NotifReadDone, NotifRemindLater, NotifOpenApp, NotifChannelAzkar, NotifChannelFajr, TodayAt, TomorrowAt, DefaultAlarmTone, QiblaTitle, QiblaNorthFmt, QiblaSetLocation, QiblaNoSensor, QiblaCalibrate, QiblaAligned, QiblaCityFmt, DirN, DirNE, DirE, DirSE, DirS, DirSW, DirW, DirNW, CalendarTab, CalEvents, CalNoEvents, CalMonthEvents, CalReminderOn, CalReminderOff, WdSun, WdMon, WdTue, WdWed, WdThu, WdFri, WdSat, AutoDnd, AutoDndSub, OptTimesTitle, OptTimesSub, Imsak, Duha, Midnight, Qiyam, HijriConvTitle, HijriConvSub, GregToHijri, HijriToGreg, DayLabel, MonthLabel, YearLabel, AlertTiming, AtAdhanTime, AtAdhanRow, AtAdhanRowSub, BeforeAdhanRow, BeforeAdhanHelper, MinBeforeFmt, AdhanReminder, IqamaReminder, IqamaReminderSub, PrayerNowFmt, PrayerApproachingFmt, ShuruqNow, ShuruqApproaching, BgFrosted, NeedsFilesPerm, PermLocTitle, PermLocBody, PermFilesTitle, PermFilesBody, PermAllow, PermNotNow, PermOpenSettings, IqamaNowFmt, SharedAlertSettings, FajrAlarmOn, AlarmEnabledSub, AlarmDisabledSub, AlarmVolume, SnoozeDuration, AlertMode, AlertModeNotif, AlertModeAlarm,
    AdhanAlertType, AlertTypeNotification, AlertTypeFullScreen
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

    fun tasbeehCount(n: Int): String = when (_lang.value) {
        Lang.AR -> when { n == 2 -> "تسبيحتان"; n in 3..10 -> "$n تسبيحات"; else -> "$n تسبيحة" }
        Lang.EN -> "$n tasbeeh"
        Lang.FR -> "$n tasbeeh"
        Lang.DE -> "$n Tasbeeh"
        Lang.ES -> "$n tasbeeh"
        Lang.IT -> "$n tasbeeh"
        Lang.TR -> "$n tesbih"
        Lang.UR -> "$n تسبیح"
        Lang.ID -> "$n tasbih"
        Lang.RU -> "$n тасбих"
        Lang.PT -> "$n tasbeeh"
    }
    fun minutesCount(n: Int): String = when (_lang.value) {
        Lang.AR -> when { n == 1 -> "دقيقة واحدة"; n == 2 -> "دقيقتان"; n in 3..10 -> "$n دقائق"; else -> "$n دقيقة" }
        Lang.RU -> when { n % 10 == 1 && n % 100 != 11 -> "$n минута"; n % 10 in 2..4 && n % 100 !in 12..14 -> "$n минуты"; else -> "$n минут" }
        Lang.EN -> if (n == 1) "1 minute" else "$n minutes"
        Lang.FR -> if (n == 1) "1 minute" else "$n minutes"
        Lang.DE -> if (n == 1) "1 Minute" else "$n Minuten"
        Lang.ES -> if (n == 1) "1 minuto" else "$n minutos"
        Lang.IT -> if (n == 1) "1 minuto" else "$n minuti"
        Lang.TR -> "$n dakika"
        Lang.UR -> "$n منٹ"
        Lang.ID -> "$n menit"
        Lang.PT -> if (n == 1) "1 minuto" else "$n minutos"
    }
}
