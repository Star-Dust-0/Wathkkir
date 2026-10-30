package com.example.prayer;

import android.icu.util.IslamicCalendar;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import kotlin.Metadata;
import kotlin.Triple;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;

/* compiled from: HijriDate.kt */
@Metadata(d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\bÇ\u0002\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J \u0010\u0007\u001a\u0014\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\n\u001a\u00020\u000bJ\u000e\u0010\f\u001a\u00020\u00062\u0006\u0010\r\u001a\u00020\tJ\u000e\u0010\u000e\u001a\u00020\u00062\u0006\u0010\n\u001a\u00020\u000bR\u0014\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006\u000f"}, d2 = {"Lcom/example/prayer/HijriDate;", "", "<init>", "()V", "monthNames", "", "", "hijriYmd", "Lkotlin/Triple;", "", "cal", "Ljava/util/Calendar;", "monthName", "month", "format", "app"}, k = 1, mv = {2, 2, 0}, xi = 48)
/* loaded from: classes10.dex */
public final class HijriDate {
    public static final HijriDate INSTANCE = new HijriDate();
    private static final List<String> monthNames = java.util.Arrays.asList(new String[]{"محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى", "جمادى الثانية", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"});
    private static final List<String> monthNamesEn = java.util.Arrays.asList(new String[]{"Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhul-Qa'dah", "Dhul-Hijjah"});
    public static final int $stable = 8;

    private HijriDate() {
    }

    public final Triple<Integer, Integer, Integer> hijriYmd(Calendar cal) {
        Intrinsics.checkNotNullParameter(cal, "cal");
        IslamicCalendar ic = new IslamicCalendar();
        ic.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
        ic.setTime(new Date(cal.getTimeInMillis()));
        return new Triple<>(Integer.valueOf(ic.get(1)), Integer.valueOf(ic.get(2) + 1), Integer.valueOf(ic.get(5)));
    }

    public final String monthName(int month) {
        return monthNames.get(RangesKt.coerceIn(month - 1, 0, 11));
    }

    public final String monthName(int month, String lang) {
        return "ar".equals(lang) ? monthNames.get(RangesKt.coerceIn(month - 1, 0, 11)) : monthNamesEn.get(RangesKt.coerceIn(month - 1, 0, 11));
    }

    public final String format(Calendar cal, String lang) {
        Intrinsics.checkNotNullParameter(cal, "cal");
        Triple<Integer, Integer, Integer> hijriYmd = hijriYmd(cal);
        int y = hijriYmd.component1().intValue();
        int m = hijriYmd.component2().intValue();
        int d = hijriYmd.component3().intValue();
        return d + " " + ((Object) monthName(m, lang)) + " " + y + ("ar".equals(lang) ? " هـ" : " AH");
    }

    public final String format(Calendar cal) {
        Intrinsics.checkNotNullParameter(cal, "cal");
        Triple<Integer, Integer, Integer> hijriYmd = hijriYmd(cal);
        int y = hijriYmd.component1().intValue();
        int m = hijriYmd.component2().intValue();
        int d = hijriYmd.component3().intValue();
        return d + " " + ((Object) monthNames.get(RangesKt.coerceIn(m - 1, 0, 11))) + " " + y + " هـ";
    }
}
