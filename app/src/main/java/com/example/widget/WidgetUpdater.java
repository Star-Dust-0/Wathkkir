package com.example.widget;

import com.example.utils.Str;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.util.SizeF;
import android.widget.RemoteViews;
import com.example.MainActivity;
import com.example.fajr.calculation.CalculationMethod;
import com.example.fajr.calculation.PrayerAdjustments;
import com.example.fajr.calculation.PrayerTimesCalculator;
import com.example.fajr.data.FajrPreferences;
import com.example.lockwidgets.HomeWidgets;
import com.example.R;

import com.example.ui.i18n.L10n;



import com.example.utils.Str;





import com.example.ui.i18n.S;




import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class WidgetUpdater {
    public static final int $stable = 8;
    public static final WidgetUpdater INSTANCE = new WidgetUpdater();
    private static final Map<String, Integer> iqamaOffsets = Str.X(new java.util.AbstractMap.SimpleEntry("الفجر", 25), new java.util.AbstractMap.SimpleEntry("الظهر", 20), new java.util.AbstractMap.SimpleEntry("العصر", 20), new java.util.AbstractMap.SimpleEntry("المغرب", 10), new java.util.AbstractMap.SimpleEntry("العشاء", 20));
    private static final Map<Integer, Map<String, Integer>> barVariants = Str.X(new java.util.AbstractMap.SimpleEntry(Integer.valueOf(R.layout.widget_hero), Str.X(new java.util.AbstractMap.SimpleEntry("amber", Integer.valueOf(R.layout.widget_hero_amber)), new java.util.AbstractMap.SimpleEntry("blue", Integer.valueOf(R.layout.widget_hero_blue)), new java.util.AbstractMap.SimpleEntry("green", Integer.valueOf(R.layout.widget_hero_green)))), new java.util.AbstractMap.SimpleEntry(Integer.valueOf(R.layout.widget_hero_compact), Str.X(new java.util.AbstractMap.SimpleEntry("amber", Integer.valueOf(R.layout.widget_hero_compact_amber)), new java.util.AbstractMap.SimpleEntry("blue", Integer.valueOf(R.layout.widget_hero_compact_blue)), new java.util.AbstractMap.SimpleEntry("green", Integer.valueOf(R.layout.widget_hero_compact_green)))), new java.util.AbstractMap.SimpleEntry(Integer.valueOf(R.layout.widget_iqama), Str.X(new java.util.AbstractMap.SimpleEntry("white", Integer.valueOf(R.layout.widget_iqama_white)), new java.util.AbstractMap.SimpleEntry("blue", Integer.valueOf(R.layout.widget_iqama_blue)), new java.util.AbstractMap.SimpleEntry("green", Integer.valueOf(R.layout.widget_iqama_green)))), new java.util.AbstractMap.SimpleEntry(Integer.valueOf(R.layout.widget_iqama_small), Str.X(new java.util.AbstractMap.SimpleEntry("white", Integer.valueOf(R.layout.widget_iqama_small_white)), new java.util.AbstractMap.SimpleEntry("blue", Integer.valueOf(R.layout.widget_iqama_small_blue)), new java.util.AbstractMap.SimpleEntry("green", Integer.valueOf(R.layout.widget_iqama_small_green)))), new java.util.AbstractMap.SimpleEntry(Integer.valueOf(R.layout.widget_prayer_bar), Str.X(new java.util.AbstractMap.SimpleEntry("amber", Integer.valueOf(R.layout.widget_prayer_bar_amber)), new java.util.AbstractMap.SimpleEntry("blue", Integer.valueOf(R.layout.widget_prayer_bar_blue)), new java.util.AbstractMap.SimpleEntry("green", Integer.valueOf(R.layout.widget_prayer_bar_green)))), new java.util.AbstractMap.SimpleEntry(Integer.valueOf(R.layout.widget_wide_bar), Str.X(new java.util.AbstractMap.SimpleEntry("amber", Integer.valueOf(R.layout.widget_wide_bar_amber)), new java.util.AbstractMap.SimpleEntry("blue", Integer.valueOf(R.layout.widget_wide_bar_blue)), new java.util.AbstractMap.SimpleEntry("green", Integer.valueOf(R.layout.widget_wide_bar_green)))));

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static final class IqamaState {
        private final long adhanMillis;
        private final long iqamaMillis;
        private final long nextIqamaMillis;
        private final String prayerName;
        private final int progressPct;

        public IqamaState(String str, long j, long j2, int i, long j3) {
            str.getClass();
            this.prayerName = str;
            this.adhanMillis = j;
            this.iqamaMillis = j2;
            this.progressPct = i;
            this.nextIqamaMillis = j3;
        }

        public static IqamaState copy$default(IqamaState iqamaState, String str, long j, long j2, int i, long j3, int i2, Object obj) {
            if ((i2 & 1) != 0) {
                str = iqamaState.prayerName;
            }
            if ((i2 & 2) != 0) {
                j = iqamaState.adhanMillis;
            }
            if ((i2 & 4) != 0) {
                j2 = iqamaState.iqamaMillis;
            }
            if ((i2 & 8) != 0) {
                i = iqamaState.progressPct;
            }
            if ((i2 & 16) != 0) {
                j3 = iqamaState.nextIqamaMillis;
            }
            int i3 = i;
            long j4 = j2;
            return iqamaState.copy(str, j, j4, i3, j3);
        }

        public final IqamaState copy(String str, long j, long j2, int i, long j3) {
            str.getClass();
            return new IqamaState(str, j, j2, i, j3);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof IqamaState)) {
                return false;
            }
            IqamaState iqamaState = (IqamaState) obj;
            return java.util.Objects.equals(this.prayerName, iqamaState.prayerName) && this.adhanMillis == iqamaState.adhanMillis && this.iqamaMillis == iqamaState.iqamaMillis && this.progressPct == iqamaState.progressPct && this.nextIqamaMillis == iqamaState.nextIqamaMillis;
        }

        public final long getAdhanMillis() {
            return this.adhanMillis;
        }

        public final long getIqamaMillis() {
            return this.iqamaMillis;
        }

        public final long getNextIqamaMillis() {
            return this.nextIqamaMillis;
        }

        public final String getPrayerName() {
            return this.prayerName;
        }

        public final int getProgressPct() {
            return this.progressPct;
        }

        public int hashCode() {
            return Long.hashCode(this.nextIqamaMillis) + ((((Long.hashCode(this.iqamaMillis) + ((Long.hashCode(this.adhanMillis) + (this.prayerName.hashCode() * 31)) * 31)) * 31) + this.progressPct) * 31);
        }

        public String toString() {
            return "IqamaState(prayerName=" + this.prayerName + ", adhanMillis=" + this.adhanMillis + ", iqamaMillis=" + this.iqamaMillis + ", progressPct=" + this.progressPct + ", nextIqamaMillis=" + this.nextIqamaMillis + ")";
        }
    }

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static final class PrayerState {
        private final String city;
        private final List<String> names;
        private final int nextIndex;
        private final long nextMillis;
        private final String nextName;
        private final long prevMillis;
        private final String prevName;
        private final List<Long> times;

        public PrayerState(List<String> list, List<Long> list2, int i, String str, long j, String str2, long j2, String str3) {
            list.getClass();
            list2.getClass();
            str.getClass();
            str2.getClass();
            str3.getClass();
            this.names = list;
            this.times = list2;
            this.nextIndex = i;
            this.nextName = str;
            this.nextMillis = j;
            this.prevName = str2;
            this.prevMillis = j2;
            this.city = str3;
        }

        public static PrayerState copy$default(PrayerState prayerState, List list, List list2, int i, String str, long j, String str2, long j2, String str3, int i2, Object obj) {
            if ((i2 & 1) != 0) {
                list = prayerState.names;
            }
            if ((i2 & 2) != 0) {
                list2 = prayerState.times;
            }
            if ((i2 & 4) != 0) {
                i = prayerState.nextIndex;
            }
            if ((i2 & 8) != 0) {
                str = prayerState.nextName;
            }
            if ((i2 & 16) != 0) {
                j = prayerState.nextMillis;
            }
            if ((i2 & 32) != 0) {
                str2 = prayerState.prevName;
            }
            if ((i2 & 64) != 0) {
                j2 = prayerState.prevMillis;
            }
            if ((i2 & 128) != 0) {
                str3 = prayerState.city;
            }
            String str4 = str2;
            long j3 = j;
            int i3 = i;
            String str5 = str;
            return prayerState.copy(list, list2, i3, str5, j3, str4, j2, str3);
        }

        public final List<String> component1() {
            return this.names;
        }

        public final List<Long> component2() {
            return this.times;
        }

        public final PrayerState copy(List<String> list, List<Long> list2, int i, String str, long j, String str2, long j2, String str3) {
            list.getClass();
            list2.getClass();
            str.getClass();
            str2.getClass();
            str3.getClass();
            return new PrayerState(list, list2, i, str, j, str2, j2, str3);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PrayerState)) {
                return false;
            }
            PrayerState prayerState = (PrayerState) obj;
            return java.util.Objects.equals(this.names, prayerState.names) && java.util.Objects.equals(this.times, prayerState.times) && this.nextIndex == prayerState.nextIndex && java.util.Objects.equals(this.nextName, prayerState.nextName) && this.nextMillis == prayerState.nextMillis && java.util.Objects.equals(this.prevName, prayerState.prevName) && this.prevMillis == prayerState.prevMillis && java.util.Objects.equals(this.city, prayerState.city);
        }

        public final String getCity() {
            return this.city;
        }

        public final List<String> getNames() {
            return this.names;
        }

        public final int getNextIndex() {
            return this.nextIndex;
        }

        public final long getNextMillis() {
            return this.nextMillis;
        }

        public final String getNextName() {
            return this.nextName;
        }

        public final long getPrevMillis() {
            return this.prevMillis;
        }

        public final String getPrevName() {
            return this.prevName;
        }

        public final List<Long> getTimes() {
            return this.times;
        }

        public int hashCode() {
            return this.city.hashCode() + ((Long.hashCode(this.prevMillis) + Str.hash((Long.hashCode(this.nextMillis) + Str.hash((((this.times.hashCode() + (this.names.hashCode() * 31)) * 31) + this.nextIndex) * 31, 31, this.nextName)) * 31, 31, this.prevName)) * 31);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("PrayerState(names=");
            sb.append(this.names);
            sb.append(", times=");
            sb.append(this.times);
            sb.append(", nextIndex=");
            sb.append(this.nextIndex);
            sb.append(", nextName=");
            sb.append(this.nextName);
            sb.append(", nextMillis=");
            sb.append(this.nextMillis);
            sb.append(", prevName=");
            sb.append(this.prevName);
            sb.append(", prevMillis=");
            sb.append(this.prevMillis);
            sb.append(", city=");
            return Str.O(sb, this.city, ")");
        }
    }

    private WidgetUpdater() {
    }

    private final void applyToAll(Context context, AppWidgetManager appWidgetManager, Class<?> cls, RemoteViews remoteViews) {
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, cls));
        appWidgetIds.getClass();
        if (appWidgetIds.length != 0) {
            appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
        }
    }

    private final int barLayout(Context context, int i) {
        Map<String, Integer> map;
        Integer num;
        String widgetBarColor = new FajrPreferences(context).getWidgetBarColor();
        return (java.util.Objects.equals(widgetBarColor, "default") || (map = barVariants.get(Integer.valueOf(i))) == null || (num = map.get(widgetBarColor)) == null) ? i : num.intValue();
    }

    private static final String compactRemaining(long j, String str) {
        String str2;
        String str3;
        String str4;
        StringBuilder sb;
        int i = (int) (j / 60000);
        if (i < 0) {
            i = 0;
        }
        int i2 = i / 60;
        int i3 = i % 60;
        if (str.startsWith("ar")) {
            if (i2 <= 0) {
                sb = new StringBuilder();
            } else {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(i2);
                sb2.append(" س ");
                sb = sb2;
            }
            sb.append(i3);
            sb.append(" د");
            return sb.toString();
        }
        if (str.startsWith("tr")) {
            str3 = "sa";
            str2 = "dk";
        } else if (str.startsWith("id")) {
            str3 = "jam";
            str2 = "mnt";
        } else if (str.startsWith("ru")) {
            str3 = "ч";
            str2 = "мин";
        } else if (str.startsWith("ur")) {
            str3 = "گھنٹے";
            str2 = "منٹ";
        } else {
            str2 = str.startsWith("en") ? "m" : "min";
            str3 = "h";
        }
        StringBuilder sb3 = new StringBuilder();
        if (i2 <= 0) {
            str4 = "";
        } else {
            str4 = i2 + " " + str3 + " ";
        }
        sb3.append(str4);
        sb3.append(i3);
        sb3.append(" ");
        sb3.append(str2);
        return sb3.toString();
    }

    private final IqamaState computeIqamaState(Context context, PrayerState prayerState, long j) {
        Object obj;
        Object obj2;
        Object obj3;
        W w = null;
        Integer num;
        Integer num2 = 1;
        List asList = Arrays.asList("الفجر", "الظهر", "العصر", "المغرب", "العشاء");
        ArrayList arrayList = new ArrayList();
        int size = prayerState.getNames().size();
        boolean z = Calendar.getInstance().get(7) == 6;
        int i = 0;
        while (i < size) {
            String str = prayerState.getNames().get(i);
            if (!asList.contains(str) || (z && "الظهر".equals(str))) {
                num = num2;
            } else {
                num = num2;
                arrayList.add(new W(str, prayerState.getTimes().get(i).longValue(), computeIqamaState$iqamaOf(str, prayerState.getTimes().get(i).longValue())));
            }
            i++;
            num2 = num;
        }
        Integer num3 = num2;
        FajrPreferences fajrPreferences = new FajrPreferences(context);
        CalculationMethod fromId = CalculationMethod.INSTANCE.fromId(fajrPreferences.getCalculationMethodId());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        int i2 = PrayerTimesCalculator.$stable;
        Object clone = calendar.clone();
        clone.getClass();
        Calendar calendar2 = (Calendar) clone;
        calendar2.add(6, -1);
        PrayerAdjustments prayerAdjustments = PrayerAdjustments.INSTANCE;
        PrayerTimesCalculator.DayPrayerTimes calculateAll = prayerAdjustments.calculateAll(context, calendar2, fajrPreferences.getLatitude(), fajrPreferences.getLongitude(), fromId);
        W w2 = new W("العشاء", calculateAll.getIsha(), computeIqamaState$iqamaOf("العشاء", calculateAll.getIsha()));
        Object clone2 = calendar.clone();
        clone2.getClass();
        Calendar calendar3 = (Calendar) clone2;
        calendar3.add(6, 1);
        PrayerTimesCalculator.DayPrayerTimes calculateAll2 = prayerAdjustments.calculateAll(context, calendar3, fajrPreferences.getLatitude(), fajrPreferences.getLongitude(), fromId);
        List t0 = Str.sorted(Str.plus(Str.plus(Str.single(w2), arrayList), Str.single(new W("الفجر", calculateAll2.getFajr(), computeIqamaState$iqamaOf("الفجر", calculateAll2.getFajr())))), new Comparator() { // from class: com.example.widget.WidgetUpdater.1
            @Override // java.util.Comparator
            public final int compare(Object obj4, Object obj5) {
                return Long.compare(((W) obj4).getAdhan(), ((W) obj5).getAdhan());
            }
        });
        ListIterator listIterator = t0.listIterator(t0.size());
        while (true) {
            obj = null;
            if (!listIterator.hasPrevious()) {
                obj2 = null;
                break;
            }
            obj2 = listIterator.previous();
            if ((((W) obj2).getAdhan() <= j ? num3 : null) != null) {
                break;
            }
        }
        W w3 = (W) obj2;
        if (w3 != null) {
            if (j <= w3.getIqama()) {
                Iterator it = t0.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    Object next = it.next();
                    if ((((W) next).getAdhan() > w3.getAdhan() ? num3 : null) != null) {
                        obj = next;
                        break;
                    }
                }
                w = (W) obj;
            } else {
                Iterator it2 = t0.iterator();
                while (true) {
                    if (!it2.hasNext()) {
                        obj3 = null;
                        break;
                    }
                    obj3 = it2.next();
                    if ((((W) obj3).getAdhan() > j ? num3 : null) != null) {
                        break;
                    }
                }
                w3 = (W) obj3;
                if (w3 != null) {
                    Iterator it3 = t0.iterator();
                    while (true) {
                        if (!it3.hasNext()) {
                            break;
                        }
                        Object next2 = it3.next();
                        if ((((W) next2).getAdhan() > w3.getAdhan() ? num3 : null) != null) {
                            obj = next2;
                            break;
                        }
                    }
                    w = (W) obj;
                }
            }
            if (w3 == null) {
                return null;
            }
            long iqama = w3.getIqama() - w3.getAdhan();
            if (iqama < 1) {
                iqama = 1;
            }
            return new IqamaState(w3.getName(), w3.getAdhan(), w3.getIqama(), com.example.utils.Str.coerce((int) (((j - w3.getAdhan()) * 100) / iqama), 0, 100), w != null ? w.getIqama() : w3.getIqama());
        }
        return null;
    }

    private static final long computeIqamaState$iqamaOf(String str, long j) {
        return ((iqamaOffsets.get(str) != null ? iqamaOffsets.get(str).intValue() : 15) * 60000) + j;
    }

    private final PrayerState computeState(Context context) {
        String str;
        List list;
        int i;
        long longValue;
        String str2;
        int i2;
        int i3;
        long longValue2;
        String str3;
        FajrPreferences fajrPreferences = new FajrPreferences(context);
        CalculationMethod fromId = CalculationMethod.INSTANCE.fromId(fajrPreferences.getCalculationMethodId());
        long currentTimeMillis = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeMillis);
        int i4 = PrayerTimesCalculator.$stable;
        PrayerTimesCalculator.DayPrayerTimes calculateAll = PrayerAdjustments.INSTANCE.calculateAll(context, calendar, fajrPreferences.getLatitude(), fajrPreferences.getLongitude(), fromId);
        List asList = Arrays.asList(Long.valueOf(calculateAll.getFajr()), Long.valueOf(calculateAll.getSunrise()), Long.valueOf(calculateAll.getDhuhr()), Long.valueOf(calculateAll.getAsr()), Long.valueOf(calculateAll.getMaghrib()), Long.valueOf(calculateAll.getIsha()));
        L10n.INSTANCE.init(context);
        String str4 = L10n.INSTANCE.getLang().getCode();
        List asList2 = Arrays.asList(L10n.INSTANCE.t(S.PrayerFajr), L10n.INSTANCE.t(S.PrayerShuruq), L10n.INSTANCE.t(S.PrayerDhuhr), L10n.INSTANCE.t(S.PrayerAsr), L10n.INSTANCE.t(S.PrayerMaghrib), L10n.INSTANCE.t(S.PrayerIsha));
        if (calendar.get(7) == 6) {
            asList2.set(2, L10n.INSTANCE.t(S.PrayerJumuah));
        }
        Iterator it = asList.iterator();
        int i5 = 0;
        while (true) {
            if (!it.hasNext()) {
                i5 = -1;
                break;
            }
            if (((Number) it.next()).longValue() > currentTimeMillis) {
                break;
            }
            i5++;
        }
        if (i5 == -1) {
            Object clone = calendar.clone();
            clone.getClass();
            Calendar calendar2 = (Calendar) clone;
            calendar2.add(6, 1);
            int i6 = PrayerTimesCalculator.$stable;
            i = -1;
            list = asList2;
            str = str4;
            longValue = PrayerAdjustments.INSTANCE.calculateAll(context, calendar2, fajrPreferences.getLatitude(), fajrPreferences.getLongitude(), fromId).getFajr();
            str2 = (String) list.get(0);
            i2 = 0;
        } else {
            str = str4;
            list = asList2;
            i = -1;
            longValue = ((Number) asList.get(i5)).longValue();
            str2 = (String) list.get(i5);
            i2 = i5;
        }
        long j = longValue;
        String str5 = str2;
        ListIterator listIterator = asList.listIterator(asList.size());
        while (true) {
            if (!listIterator.hasPrevious()) {
                i3 = i;
                break;
            }
            if (((Number) listIterator.previous()).longValue() <= currentTimeMillis) {
                i3 = listIterator.nextIndex();
                break;
            }
        }
        if (i3 == i) {
            Object clone2 = calendar.clone();
            clone2.getClass();
            Calendar calendar3 = (Calendar) clone2;
            calendar3.add(6, i);
            longValue2 = PrayerAdjustments.INSTANCE.calculateAll(context, calendar3, fajrPreferences.getLatitude(), fajrPreferences.getLongitude(), fromId).getIsha();
            str3 = (String) list.get(5);
        } else {
            longValue2 = ((Number) asList.get(i3)).longValue();
            str3 = (String) list.get(i3);
        }
        return new PrayerState(list, asList, i2, str5, j, str3, longValue2, fajrPreferences.getCityNameLocalized(str));
    }

    private static final String faceRemaining(String str, String str2, String str3) {
        if (str3.startsWith("ar")) {
            return "باقي على صلاة " + str + " " + str2;
        }
        if (str3.startsWith("ur")) {
            return "باقی " + str + " " + str2;
        }
        if (str3.startsWith("fr")) {
            return Str.L(str, " dans ", str2);
        }
        if (str3.startsWith("de")) {
            return Str.L(str, " in ", str2);
        }
        if (str3.startsWith("es")) {
            return Str.L(str, " en ", str2);
        }
        if (str3.startsWith("it")) {
            return Str.L(str, " tra ", str2);
        }
        if (!str3.startsWith("tr")) {
            return str3.startsWith("id") ? Str.L(str, " dalam ", str2) : str3.startsWith("ru") ? Str.L(str, " через ", str2) : str3.startsWith("pt") ? Str.L(str, " em ", str2) : Str.L(str, " in ", str2);
        }
        return str + ": " + str2 + " kaldı";
    }

    private final String formatRemaining(long j) {
        int i = (int) (j / 60000);
        if (i < 0) {
            i = 0;
        }
        int i2 = i / 60;
        int i3 = i % 60;
        if (i2 <= 0) {
            return formatRemaining$minutesText(i);
        }
        String str = "ساعتان";
        if (i3 == 0) {
            return i2 != 1 ? i2 != 2 ? i2 < 11 ? Str.cat(i2, " ساعات") : Str.cat(i2, " ساعة") : "ساعتان" : "ساعة واحدة";
        }
        if (i2 == 1) {
            str = "ساعة";
        } else if (i2 != 2) {
            str = i2 < 11 ? Str.cat(i2, " ساعات") : Str.cat(i2, " ساعة");
        }
        return str + " و " + formatRemaining$minutesText(i3);
    }

    private static final String formatRemaining$minutesText(int i) {
        if (i == 1) {
            return "دقيقة واحدة";
        }
        if (i == 2) {
            return "دقيقتان";
        }
        return Str.cat(i, (3 > i || i >= 11) ? " دقيقة" : " دقائق");
    }

    private final String formatTime(long j) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        return new SimpleDateFormat("h:mm", Locale.US).format(calendar.getTime()) + " " + L10n.INSTANCE.t(calendar.get(9) == 0 ? S.AmMarker : S.PmMarker);
    }

    private final String formatTimeShort(long j) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("h:mm", Locale.US);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        String format = simpleDateFormat.format(calendar.getTime());
        format.getClass();
        return format;
    }

    private final PendingIntent launchIntent(Context context) {
        Intent intent = new Intent(context, (Class<?>) MainActivity.class);
        intent.setFlags(335544320);
        PendingIntent activity = PendingIntent.getActivity(context, 0, intent, 201326592);
        activity.getClass();
        return activity;
    }

    private static final RemoteViews pushFaceWidget$build(Context context, PrayerState prayerState, String str, int i, int i2) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), i2);
        remoteViews.setTextViewText(R.id.facewidget_prayer_name, prayerState.getNextName());
        WidgetUpdater widgetUpdater = INSTANCE;
        remoteViews.setTextViewText(R.id.facewidget_prayer_time, widgetUpdater.formatTime(prayerState.getNextMillis()));
        remoteViews.setChronometerCountDown(R.id.facewidget_remaining, true);
        remoteViews.setChronometer(R.id.facewidget_remaining,
                SystemClock.elapsedRealtime() + prayerState.getNextMillis() - System.currentTimeMillis(), "%s", true);
        remoteViews.setProgressBar(R.id.facewidget_progress, 100, i, false);
        remoteViews.setOnClickPendingIntent(R.id.facewidget_root, widgetUpdater.launchIntent(context));
        return remoteViews;
    }

    private final void updateClockCountdown(Context context, AppWidgetManager appWidgetManager, PrayerState prayerState, long j) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_clock);
        remoteViews.setTextViewText(R.id.widget_clock_prev_name, prayerState.getPrevName());
        remoteViews.setTextViewText(R.id.widget_clock_prev_time, formatTimeShort(prayerState.getPrevMillis()));
        remoteViews.setTextViewText(R.id.widget_clock_next_name, prayerState.getNextName());
        remoteViews.setTextViewText(R.id.widget_clock_next_time, formatTimeShort(prayerState.getNextMillis()));
        if (Build.VERSION.SDK_INT >= 24) {
            remoteViews.setChronometerCountDown(R.id.widget_clock_countdown, true);
        }
        remoteViews.setChronometer(R.id.widget_clock_countdown, SystemClock.elapsedRealtime() + (prayerState.getNextMillis() - j), "%s", true);
        remoteViews.setOnClickPendingIntent(R.id.widget_clock_countdown, launchIntent(context));
        applyToAll(context, appWidgetManager, ClockWidgetProvider.class, remoteViews);
    }

    private final void updateDayPrayer(Context context, AppWidgetManager appWidgetManager, PrayerState prayerState, Calendar calendar) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_day_prayer);
        remoteViews.setTextViewText(R.id.widget_day_name, new SimpleDateFormat("EEEE", new Locale("ar", "EG")).format(calendar.getTime()));
        remoteViews.setTextViewText(R.id.widget_day_prayer, prayerState.getNextName() + " " + formatTime(prayerState.getNextMillis()));
        remoteViews.setOnClickPendingIntent(R.id.widget_day_name, launchIntent(context));
        applyToAll(context, appWidgetManager, DayPrayerWidgetProvider.class, remoteViews);
    }

    private final void updateGregorian(Context context, AppWidgetManager appWidgetManager, Calendar calendar) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_gregorian);
        remoteViews.setTextViewText(R.id.widget_greg_day, String.valueOf(calendar.get(5)));
        remoteViews.setTextViewText(R.id.widget_greg_month, new SimpleDateFormat("MMMM", new Locale("ar", "EG")).format(calendar.getTime()));
        remoteViews.setTextViewText(R.id.widget_greg_year, String.valueOf(calendar.get(1)));
        remoteViews.setOnClickPendingIntent(R.id.widget_greg_day, launchIntent(context));
        applyToAll(context, appWidgetManager, GregorianWidgetProvider.class, remoteViews);
    }

    private final void updateHero(Context context, AppWidgetManager appWidgetManager, PrayerState prayerState, long j) {
        long nextMillis = prayerState.getNextMillis() - prayerState.getPrevMillis();
        if (nextMillis < 1) {
            nextMillis = 1;
        }
        int r = com.example.utils.Str.coerce((int) (((j - prayerState.getPrevMillis()) * 100) / nextMillis), 0, 100);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), barLayout(context, R.layout.widget_hero));
        remoteViews.setTextViewText(R.id.widget_hero_prev_name, prayerState.getPrevName());
        remoteViews.setTextViewText(R.id.widget_hero_next_name, prayerState.getNextName());
        remoteViews.setTextViewText(R.id.widget_hero_prev_time, formatTimeShort(prayerState.getPrevMillis()));
        remoteViews.setTextViewText(R.id.widget_hero_next_time, formatTimeShort(prayerState.getNextMillis()));
        remoteViews.setTextViewText(R.id.widget_hero_remaining, faceRemaining(prayerState.getNextName(), compactRemaining(prayerState.getNextMillis() - j), L10n.INSTANCE.getLang().getCode()));
        remoteViews.setProgressBar(R.id.widget_hero_progress, 100, r, false);
        String a = com.example.prayer.HijriDate.INSTANCE.format(calendar, "ar");
        if (a.endsWith(" هـ")) {
            a = a.substring(0, a.length() - 3);
        }
        remoteViews.setTextViewText(R.id.widget_hero_hijri, a);
        remoteViews.setTextViewText(R.id.widget_hero_greg, new SimpleDateFormat("d MMMM yyyy", new Locale("ar", "EG")).format(calendar.getTime()));
        remoteViews.setOnClickPendingIntent(R.id.widget_hero_root, launchIntent(context));
        RemoteViews remoteViews2 = new RemoteViews(context.getPackageName(), barLayout(context, R.layout.widget_hero_compact));
        remoteViews2.setTextViewText(R.id.widget_hero_compact_name, prayerState.getNextName() + " " + formatTime(prayerState.getNextMillis()));
        StringBuilder sb = new StringBuilder("باقي ");
        sb.append(compactRemaining(prayerState.getNextMillis() - j));
        remoteViews2.setTextViewText(R.id.widget_hero_compact_remaining, sb.toString());
        remoteViews2.setProgressBar(R.id.widget_hero_compact_progress, 100, r, false);
        remoteViews2.setOnClickPendingIntent(R.id.widget_hero_compact_root, launchIntent(context));
        if (Build.VERSION.SDK_INT >= 31) {
            remoteViews = new RemoteViews((java.util.Map<SizeF, RemoteViews>) Str.X(new java.util.AbstractMap.SimpleEntry(new SizeF(250.0f, 110.0f), remoteViews), new java.util.AbstractMap.SimpleEntry(new SizeF(250.0f, 40.0f), remoteViews2)));
        }
        applyToAll(context, appWidgetManager, HeroWidgetProvider.class, remoteViews);
    }

    private final void updateHijri(Context context, AppWidgetManager appWidgetManager, Calendar calendar) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_hijri);
        kotlin.Triple c = com.example.prayer.HijriDate.INSTANCE.hijriYmd(calendar);
        int intValue = ((Integer) c.getFirst()).intValue();
        int intValue2 = ((Integer) c.getSecond()).intValue();
        remoteViews.setTextViewText(R.id.widget_hijri_day, String.valueOf(((Integer) c.getThird()).intValue()));
        remoteViews.setTextViewText(R.id.widget_hijri_month, com.example.prayer.HijriDate.INSTANCE.monthName(intValue2, "ar"));
        remoteViews.setTextViewText(R.id.widget_hijri_year, intValue + " هـ");
        remoteViews.setOnClickPendingIntent(R.id.widget_hijri_day, launchIntent(context));
        applyToAll(context, appWidgetManager, HijriWidgetProvider.class, remoteViews);
    }

    private final void updateIqama(Context context, AppWidgetManager appWidgetManager, PrayerState prayerState, long j) {
        IqamaState computeIqamaState = computeIqamaState(context, prayerState, j);
        if (computeIqamaState == null) {
            return;
        }
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), barLayout(context, R.layout.widget_iqama));
        remoteViews.setTextViewText(R.id.widget_iqama_countdown, HomeWidgets.mmss(computeIqamaState.getIqamaMillis() - j));
        remoteViews.setTextViewText(R.id.widget_iqama_adhan, computeIqamaState.getPrayerName() + " " + formatTimeShort(computeIqamaState.getAdhanMillis()));
        StringBuilder sb = new StringBuilder("الإقامة ");
        sb.append(formatTimeShort(computeIqamaState.getIqamaMillis()));
        remoteViews.setTextViewText(R.id.widget_iqama_time, sb.toString());
        remoteViews.setTextViewText(R.id.widget_iqama_next, "الإقامة " + formatTimeShort(computeIqamaState.getNextIqamaMillis()));
        remoteViews.setProgressBar(R.id.widget_iqama_progress, 100, computeIqamaState.getProgressPct(), false);
        remoteViews.setOnClickPendingIntent(R.id.widget_iqama_countdown, launchIntent(context));
        applyToAll(context, appWidgetManager, IqamaWidgetProvider.class, remoteViews);
    }

    private final void updateIqamaSmall(Context context, AppWidgetManager appWidgetManager, PrayerState prayerState, long j) {
        IqamaState computeIqamaState = computeIqamaState(context, prayerState, j);
        if (computeIqamaState == null) {
            return;
        }
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), barLayout(context, R.layout.widget_iqama_small));
        remoteViews.setTextViewText(R.id.widget_iqama_small_time, formatTimeShort(computeIqamaState.getIqamaMillis()));
        remoteViews.setProgressBar(R.id.widget_iqama_small_progress, 100, computeIqamaState.getProgressPct(), false);
        remoteViews.setOnClickPendingIntent(R.id.widget_iqama_small_time, launchIntent(context));
        applyToAll(context, appWidgetManager, IqamaSmallWidgetProvider.class, remoteViews);
    }

    private final void updateNextPrayer(Context context, AppWidgetManager appWidgetManager, PrayerState prayerState, long j) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_next_prayer);
        remoteViews.setTextViewText(R.id.widget_prayer_name, prayerState.getNextName());
        remoteViews.setTextViewText(R.id.widget_prayer_time, formatTime(prayerState.getNextMillis()));
        if (Build.VERSION.SDK_INT >= 24) {
            remoteViews.setChronometerCountDown(R.id.widget_countdown, true);
        }
        remoteViews.setChronometer(R.id.widget_countdown, SystemClock.elapsedRealtime() + (prayerState.getNextMillis() - j), "باقي %s", true);
        remoteViews.setOnClickPendingIntent(R.id.widget_prayer_name, launchIntent(context));
        remoteViews.setOnClickPendingIntent(R.id.widget_prayer_time, launchIntent(context));
        applyToAll(context, appWidgetManager, NextPrayerWidgetProvider.class, remoteViews);
    }

    private final void updatePill(Context context, AppWidgetManager appWidgetManager, PrayerState prayerState, long j) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_pill);
        remoteViews.setTextViewText(R.id.widget_pill_text, "أذان " + prayerState.getNextName() + " بعد " + formatRemaining(prayerState.getNextMillis() - j));
        remoteViews.setOnClickPendingIntent(R.id.widget_pill_text, launchIntent(context));
        applyToAll(context, appWidgetManager, PillWidgetProvider.class, remoteViews);
    }

    private final void updatePrayerBar(Context context, AppWidgetManager appWidgetManager, PrayerState prayerState, long j) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), barLayout(context, R.layout.widget_prayer_bar));
        remoteViews.setTextViewText(R.id.widget_bar_name, prayerState.getNextName());
        remoteViews.setTextViewText(R.id.widget_bar_time, formatTime(prayerState.getNextMillis()));
        long nextMillis = prayerState.getNextMillis() - prayerState.getPrevMillis();
        if (nextMillis < 1) {
            nextMillis = 1;
        }
        remoteViews.setProgressBar(R.id.widget_bar_progress, 100, com.example.utils.Str.coerce((int) (((j - prayerState.getPrevMillis()) * 100) / nextMillis), 0, 100), false);
        remoteViews.setOnClickPendingIntent(R.id.widget_bar_name, launchIntent(context));
        remoteViews.setOnClickPendingIntent(R.id.widget_bar_time, launchIntent(context));
        applyToAll(context, appWidgetManager, PrayerBarWidgetProvider.class, remoteViews);
    }

    private final void updateSchedule(Context context, AppWidgetManager appWidgetManager, PrayerState prayerState) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_schedule);
        int i = 0;
        List asList = Arrays.asList(Integer.valueOf(R.id.widget_name_0), Integer.valueOf(R.id.widget_name_1), Integer.valueOf(R.id.widget_name_2), Integer.valueOf(R.id.widget_name_3), Integer.valueOf(R.id.widget_name_4), Integer.valueOf(R.id.widget_name_5));
        List asList2 = Arrays.asList(Integer.valueOf(R.id.widget_time_0), Integer.valueOf(R.id.widget_time_1), Integer.valueOf(R.id.widget_time_2), Integer.valueOf(R.id.widget_time_3), Integer.valueOf(R.id.widget_time_4), Integer.valueOf(R.id.widget_time_5));
        while (i < 6) {
            remoteViews.setTextViewText(((Number) asList.get(i)).intValue(), prayerState.getNames().get(i));
            remoteViews.setTextViewText(((Number) asList2.get(i)).intValue(), formatTimeShort(prayerState.getTimes().get(i).longValue()));
            int i2 = -12676609;
            int i3 = i == prayerState.getNextIndex() ? -12676609 : -1;
            if (i != prayerState.getNextIndex()) {
                i2 = -5855578;
            }
            remoteViews.setTextColor(((Number) asList2.get(i)).intValue(), i3);
            remoteViews.setTextColor(((Number) asList.get(i)).intValue(), i2);
            i++;
        }
        remoteViews.setOnClickPendingIntent(R.id.widget_name_0, launchIntent(context));
        applyToAll(context, appWidgetManager, ScheduleWidgetProvider.class, remoteViews);
    }

    private final void updateWideBar(Context context, AppWidgetManager appWidgetManager, PrayerState prayerState, Calendar calendar, long j) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), barLayout(context, R.layout.widget_wide_bar));
        remoteViews.setTextViewText(R.id.widget_wide_day, new SimpleDateFormat("EEEE", new Locale("ar", "EG")).format(calendar.getTime()));
        remoteViews.setTextViewText(R.id.widget_wide_city, prayerState.getCity());
        remoteViews.setTextViewText(R.id.widget_wide_next_name, prayerState.getNextName());
        remoteViews.setTextViewText(R.id.widget_wide_prev_name, prayerState.getPrevName());
        remoteViews.setTextViewText(R.id.widget_wide_next_time, formatTimeShort(prayerState.getNextMillis()));
        remoteViews.setTextViewText(R.id.widget_wide_prev_time, formatTimeShort(prayerState.getPrevMillis()));
        remoteViews.setTextViewText(R.id.widget_wide_remaining, "باقي على " + prayerState.getNextName() + " " + formatRemaining(prayerState.getNextMillis() - j));
        long nextMillis = prayerState.getNextMillis() - prayerState.getPrevMillis();
        if (nextMillis < 1) {
            nextMillis = 1;
        }
        remoteViews.setProgressBar(R.id.widget_wide_progress, 100, com.example.utils.Str.coerce((int) (((j - prayerState.getPrevMillis()) * 100) / nextMillis), 0, 100), false);
        String a = com.example.prayer.HijriDate.INSTANCE.format(calendar, "ar");
        if (a.endsWith(" هـ")) {
            a = a.substring(0, a.length() - 3);
        }
        remoteViews.setTextViewText(R.id.widget_wide_hijri, a);
        remoteViews.setTextViewText(R.id.widget_wide_greg, new SimpleDateFormat("d MMMM yyyy", new Locale("ar", "EG")).format(calendar.getTime()));
        remoteViews.setOnClickPendingIntent(R.id.widget_wide_remaining, launchIntent(context));
        applyToAll(context, appWidgetManager, WideBarWidgetProvider.class, remoteViews);
    }

    public final void pushFaceWidget(Context context, String str) {
        context.getClass();
        str.getClass();
        if (str.equals(FaceWidgetReceiver.PAGE_ID_PRAYER)) {
            PrayerState computeState = computeState(context);
            long currentTimeMillis = System.currentTimeMillis();
            long nextMillis = computeState.getNextMillis() - computeState.getPrevMillis();
            if (nextMillis < 1) {
                nextMillis = 1;
            }
            int r = com.example.utils.Str.coerce((int) (((currentTimeMillis - computeState.getPrevMillis()) * 100) / nextMillis), 0, 100);
            String faceRemaining = faceRemaining(computeState.getNextName(), compactRemaining(computeState.getNextMillis() - currentTimeMillis), L10n.INSTANCE.getLang().getCode());
            Intent intent = new Intent(FaceWidgetReceiver.ACTION_RESPONSE);
            intent.setPackage("com.android.systemui");
            intent.putExtra("package", context.getPackageName());
            intent.putExtra("pageId", str);
            intent.putExtra("show", true);
            intent.putExtra("origin", pushFaceWidget$build(context, computeState, faceRemaining, r, R.layout.facewidget_prayer));
            intent.putExtra("aod", pushFaceWidget$build(context, computeState, faceRemaining, r, R.layout.facewidget_prayer_aod));
            context.sendBroadcast(intent);
        }
    }

    public final void updateAll(Context context) {
        HomeWidgets.updateAll(context);
        try {
            pushFaceWidget(context, FaceWidgetReceiver.PAGE_ID_PRAYER);
        } catch (Exception unused) {
        }
    }

    public final void updateFace(Context context) {
        try {
            pushFaceWidget(context, FaceWidgetReceiver.PAGE_ID_PRAYER);
        } catch (Exception e) {
            Log.w("WathakkerHome", "face widget push failed", e);
        }
    }

    private final String compactRemaining(long j) {
        return compactRemaining(j, L10n.INSTANCE.getLang().getCode());
    }
}
