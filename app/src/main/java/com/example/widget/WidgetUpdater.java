package com.example.widget;

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
import com.example.ui.i18n.S;
import com.example.prayer.HijriDate;
import kotlin.Pair;
import kotlin.Triple;
import kotlin.collections.MapsKt;
import kotlin.collections.CollectionsKt;
import kotlin.ranges.RangesKt;
import java.util.Objects;
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
    private static final Map<String, Integer> iqamaOffsets = MapsKt.mapOf(new Pair<>("الفجر", 25), new Pair<>("الظهر", 20), new Pair<>("العصر", 20), new Pair<>("المغرب", 10), new Pair<>("العشاء", 20));
    private static final Map<Integer, Map<String, Integer>> barVariants = MapsKt.mapOf(new Pair<>(Integer.valueOf(R.layout.widget_hero), MapsKt.mapOf(new Pair<>("amber", Integer.valueOf(R.layout.widget_hero_amber)), new Pair<>("blue", Integer.valueOf(R.layout.widget_hero_blue)), new Pair<>("green", Integer.valueOf(R.layout.widget_hero_green)))), new Pair<>(Integer.valueOf(R.layout.widget_hero_compact), MapsKt.mapOf(new Pair<>("amber", Integer.valueOf(R.layout.widget_hero_compact_amber)), new Pair<>("blue", Integer.valueOf(R.layout.widget_hero_compact_blue)), new Pair<>("green", Integer.valueOf(R.layout.widget_hero_compact_green)))), new Pair<>(Integer.valueOf(R.layout.widget_iqama), MapsKt.mapOf(new Pair<>("white", Integer.valueOf(R.layout.widget_iqama_white)), new Pair<>("blue", Integer.valueOf(R.layout.widget_iqama_blue)), new Pair<>("green", Integer.valueOf(R.layout.widget_iqama_green)))), new Pair<>(Integer.valueOf(R.layout.widget_iqama_small), MapsKt.mapOf(new Pair<>("white", Integer.valueOf(R.layout.widget_iqama_small_white)), new Pair<>("blue", Integer.valueOf(R.layout.widget_iqama_small_blue)), new Pair<>("green", Integer.valueOf(R.layout.widget_iqama_small_green)))), new Pair<>(Integer.valueOf(R.layout.widget_prayer_bar), MapsKt.mapOf(new Pair<>("amber", Integer.valueOf(R.layout.widget_prayer_bar_amber)), new Pair<>("blue", Integer.valueOf(R.layout.widget_prayer_bar_blue)), new Pair<>("green", Integer.valueOf(R.layout.widget_prayer_bar_green)))), new Pair<>(Integer.valueOf(R.layout.widget_wide_bar), MapsKt.mapOf(new Pair<>("amber", Integer.valueOf(R.layout.widget_wide_bar_amber)), new Pair<>("blue", Integer.valueOf(R.layout.widget_wide_bar_blue)), new Pair<>("green", Integer.valueOf(R.layout.widget_wide_bar_green)))));

    public final void updateAll(Context context) {
        HomeWidgets.updateAll(context);
        try {
            pushFaceWidget(context, FaceWidgetReceiver.PAGE_ID_PRAYER);
        } catch (Exception e) {
        }
    }

    /** Face-widget push only - used by the minute pulse, which already updates home widgets itself. */
    public final void updateFace(Context context) {
        try {
            pushFaceWidget(context, FaceWidgetReceiver.PAGE_ID_PRAYER);
        } catch (Exception e) {
            android.util.Log.w("WathakkerHome", "face widget push failed", e);
        }
    }

    private WidgetUpdater() {
    }

    private final String formatTime(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        com.example.ui.i18n.L10n lx0 = com.example.ui.i18n.L10n.INSTANCE;
        String ampm = cal.get(9) == 0 ? lx0.t(com.example.ui.i18n.S.AmMarker) : lx0.t(com.example.ui.i18n.S.PmMarker);
        return new SimpleDateFormat("h:mm", Locale.US).format(cal.getTime()) + " " + ampm;
    }

    private final String formatTimeShort(long millis) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("h:mm", Locale.US);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        String format = simpleDateFormat.format(calendar.getTime());
        
        return format;
    }

    /* JADX INFO: Access modifiers changed from: private */
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
            return Objects.equals(this.prayerName, iqamaState.prayerName) && this.adhanMillis == iqamaState.adhanMillis && this.iqamaMillis == iqamaState.iqamaMillis && this.progressPct == iqamaState.progressPct && this.nextIqamaMillis == iqamaState.nextIqamaMillis;
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
            return Objects.equals(this.names, prayerState.names) && Objects.equals(this.times, prayerState.times) && this.nextIndex == prayerState.nextIndex && Objects.equals(this.nextName, prayerState.nextName) && this.nextMillis == prayerState.nextMillis && Objects.equals(this.prevName, prayerState.prevName) && this.prevMillis == prayerState.prevMillis && Objects.equals(this.city, prayerState.city);
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

        public int hashCode() { return Objects.hash(names,times,nextIndex,nextName,nextMillis,prevName,prevMillis,city); }

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
            return sb.append(this.city).append(")").toString();
        }
    }

    private static class W {
      final String name; final long adhan, iqama;
      W(String n,long a,long i){name=n;adhan=a;iqama=i;}
      String getName(){return name;} long getAdhan(){return adhan;} long getIqama(){return iqama;}
    }
    private static List<W> sortedEvents(W first, List<W> middle, W last, Comparator<W> comparator) {
      List<W> all = new ArrayList<>(); all.add(first);all.addAll(middle);all.add(last);all.sort(comparator);return all;
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
        return (Objects.equals(widgetBarColor, "default") || (map = barVariants.get(Integer.valueOf(i))) == null || (num = map.get(widgetBarColor)) == null) ? i : num.intValue();
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
        Object objPrevious;
        Object next;
        W w = null;
        Integer num;
        Integer num2 = 1;
        List listAsList = Arrays.asList("الفجر", "الظهر", "العصر", "المغرب", "العشاء");
        ArrayList arrayList = new ArrayList();
        int size = prayerState.getNames().size();
        boolean z = Calendar.getInstance().get(7) == 6;
        int i = 0;
        while (i < size) {
            String str = prayerState.getNames().get(i);
            if (!listAsList.contains(str) || (z && "الظهر".equals(str))) {
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
        CalculationMethod calculationMethodFromId = CalculationMethod.INSTANCE.fromId(fajrPreferences.getCalculationMethodId());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        int i2 = PrayerTimesCalculator.$stable;
        Object objClone = calendar.clone();
        objClone.getClass();
        Calendar calendar2 = (Calendar) objClone;
        calendar2.add(6, -1);
        PrayerAdjustments prayerAdjustments = PrayerAdjustments.INSTANCE;
        PrayerTimesCalculator.DayPrayerTimes dayPrayerTimesCalculateAll = prayerAdjustments.calculateAll(context, calendar2, fajrPreferences.getLatitude(), fajrPreferences.getLongitude(), calculationMethodFromId);
        W w2 = new W("العشاء", dayPrayerTimesCalculateAll.getIsha(), computeIqamaState$iqamaOf("العشاء", dayPrayerTimesCalculateAll.getIsha()));
        Object objClone2 = calendar.clone();
        objClone2.getClass();
        Calendar calendar3 = (Calendar) objClone2;
        calendar3.add(6, 1);
        PrayerTimesCalculator.DayPrayerTimes dayPrayerTimesCalculateAll2 = prayerAdjustments.calculateAll(context, calendar3, fajrPreferences.getLatitude(), fajrPreferences.getLongitude(), calculationMethodFromId);
        List listT0 = sortedEvents(w2, arrayList, new W("الفجر", dayPrayerTimesCalculateAll2.getFajr(), computeIqamaState$iqamaOf("الفجر", dayPrayerTimesCalculateAll2.getFajr())), new Comparator() { // from class: com.example.widget.WidgetUpdater.1
            @Override // java.util.Comparator
            public final int compare(Object obj2, Object obj3) {
                return Long.compare(((W) obj2).getAdhan(), ((W) obj3).getAdhan());
            }
        });
        ListIterator listIterator = listT0.listIterator(listT0.size());
        while (true) {
            obj = null;
            if (!listIterator.hasPrevious()) {
                objPrevious = null;
                break;
            }
            objPrevious = listIterator.previous();
            if ((((W) objPrevious).getAdhan() <= j ? num3 : null) != null) {
                break;
            }
        }
        W w3 = (W) objPrevious;
        if (w3 != null) {
            if (j <= w3.getIqama()) {
                Iterator it = listT0.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    Object next2 = it.next();
                    if ((((W) next2).getAdhan() > w3.getAdhan() ? num3 : null) != null) {
                        obj = next2;
                        break;
                    }
                }
                w = (W) obj;
            } else {
                Iterator it2 = listT0.iterator();
                while (true) {
                    if (!it2.hasNext()) {
                        next = null;
                        break;
                    }
                    next = it2.next();
                    if ((((W) next).getAdhan() > j ? num3 : null) != null) {
                        break;
                    }
                }
                w3 = (W) next;
                if (w3 != null) {
                    Iterator it3 = listT0.iterator();
                    while (true) {
                        if (!it3.hasNext()) {
                            break;
                        }
                        Object next3 = it3.next();
                        if ((((W) next3).getAdhan() > w3.getAdhan() ? num3 : null) != null) {
                            obj = next3;
                            break;
                        }
                    }
                    w = (W) obj;
                }
            }
            long iqama = w3.getIqama() - w3.getAdhan();
            if (iqama < 1) {
                iqama = 1;
            }
            return new IqamaState(w3.getName(), w3.getAdhan(), w3.getIqama(), RangesKt.coerceIn((int) (((j - w3.getAdhan()) * 100) / iqama), 0, 100), w != null ? w.getIqama() : w3.getIqama());
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
        long jLongValue;
        String str2;
        int i2;
        int iNextIndex;
        long jLongValue2;
        String str3;
        FajrPreferences fajrPreferences = new FajrPreferences(context);
        CalculationMethod calculationMethodFromId = CalculationMethod.INSTANCE.fromId(fajrPreferences.getCalculationMethodId());
        long jCurrentTimeMillis = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(jCurrentTimeMillis);
        int i3 = PrayerTimesCalculator.$stable;
        PrayerTimesCalculator.DayPrayerTimes dayPrayerTimesCalculateAll = PrayerAdjustments.INSTANCE.calculateAll(context, calendar, fajrPreferences.getLatitude(), fajrPreferences.getLongitude(), calculationMethodFromId);
        List listAsList = Arrays.asList(Long.valueOf(dayPrayerTimesCalculateAll.getFajr()), Long.valueOf(dayPrayerTimesCalculateAll.getSunrise()), Long.valueOf(dayPrayerTimesCalculateAll.getDhuhr()), Long.valueOf(dayPrayerTimesCalculateAll.getAsr()), Long.valueOf(dayPrayerTimesCalculateAll.getMaghrib()), Long.valueOf(dayPrayerTimesCalculateAll.getIsha()));
        L10n.INSTANCE.init(context);
        String str4 = L10n.INSTANCE.getLang().getCode();
        List listAsList2 = Arrays.asList(L10n.INSTANCE.t(S.PrayerFajr), L10n.INSTANCE.t(S.PrayerShuruq), L10n.INSTANCE.t(S.PrayerDhuhr), L10n.INSTANCE.t(S.PrayerAsr), L10n.INSTANCE.t(S.PrayerMaghrib), L10n.INSTANCE.t(S.PrayerIsha));
        if (calendar.get(7) == 6) {
            listAsList2.set(2, L10n.INSTANCE.t(S.PrayerJumuah));
        }
        Iterator it = listAsList.iterator();
        int i4 = 0;
        while (true) {
            if (!it.hasNext()) {
                i4 = -1;
                break;
            }
            if (((Number) it.next()).longValue() > jCurrentTimeMillis) {
                break;
            }
            i4++;
        }
        if (i4 == -1) {
            Object objClone = calendar.clone();
            objClone.getClass();
            Calendar calendar2 = (Calendar) objClone;
            calendar2.add(6, 1);
            int i5 = PrayerTimesCalculator.$stable;
            i = -1;
            list = listAsList2;
            str = str4;
            jLongValue = PrayerAdjustments.INSTANCE.calculateAll(context, calendar2, fajrPreferences.getLatitude(), fajrPreferences.getLongitude(), calculationMethodFromId).getFajr();
            str2 = (String) list.get(0);
            i2 = 0;
        } else {
            str = str4;
            list = listAsList2;
            i = -1;
            jLongValue = ((Number) listAsList.get(i4)).longValue();
            str2 = (String) list.get(i4);
            i2 = i4;
        }
        long j = jLongValue;
        String str5 = str2;
        ListIterator listIterator = listAsList.listIterator(listAsList.size());
        while (true) {
            if (!listIterator.hasPrevious()) {
                iNextIndex = i;
                break;
            }
            if (((Number) listIterator.previous()).longValue() <= jCurrentTimeMillis) {
                iNextIndex = listIterator.nextIndex();
                break;
            }
        }
        if (iNextIndex == i) {
            Object objClone2 = calendar.clone();
            objClone2.getClass();
            Calendar calendar3 = (Calendar) objClone2;
            calendar3.add(6, i);
            jLongValue2 = PrayerAdjustments.INSTANCE.calculateAll(context, calendar3, fajrPreferences.getLatitude(), fajrPreferences.getLongitude(), calculationMethodFromId).getIsha();
            str3 = (String) list.get(5);
        } else {
            jLongValue2 = ((Number) listAsList.get(iNextIndex)).longValue();
            str3 = (String) list.get(iNextIndex);
        }
        return new PrayerState(list, listAsList, i2, str5, j, str3, jLongValue2, fajrPreferences.getCityNameLocalized(str));
    }

    private static final String faceRemaining(String str, String str2, String str3) {
        if (str3.startsWith("ar")) {
            return "باقي على صلاة " + str + " " + str2;
        }
        if (str3.startsWith("ur")) {
            return "باقی " + str + " " + str2;
        }
        if (str3.startsWith("fr")) {
            return (str + " dans " + str2);
        }
        if (str3.startsWith("de")) {
            return (str + " in " + str2);
        }
        if (str3.startsWith("es")) {
            return (str + " en " + str2);
        }
        if (str3.startsWith("it")) {
            return (str + " tra " + str2);
        }
        if (!str3.startsWith("tr")) {
            return str3.startsWith("id") ? (str + " dalam " + str2) : str3.startsWith("ru") ? (str + " через " + str2) : str3.startsWith("pt") ? (str + " em " + str2) : (str + " in " + str2);
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
        String strL = "ساعتان";
        if (i3 == 0) {
            return i2 != 1 ? i2 != 2 ? i2 < 11 ? (i2 + " ساعات") : (i2 + " ساعة") : "ساعتان" : "ساعة واحدة";
        }
        if (i2 == 1) {
            strL = "ساعة";
        } else if (i2 != 2) {
            strL = i2 < 11 ? (i2 + " ساعات") : (i2 + " ساعة");
        }
        return strL + " و " + formatRemaining$minutesText(i3);
    }

    private static final String formatRemaining$minutesText(int i) {
        if (i == 1) {
            return "دقيقة واحدة";
        }
        if (i == 2) {
            return "دقيقتان";
        }
        return (i + ((3 > i || i >= 11) ? " دقيقة" : " دقائق"));
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
        remoteViews.setTextViewText(R.id.facewidget_remaining, str);
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
        int iR = RangesKt.coerceIn((int) (((j - prayerState.getPrevMillis()) * 100) / nextMillis), 0, 100);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), barLayout(context, R.layout.widget_hero));
        remoteViews.setTextViewText(R.id.widget_hero_prev_name, prayerState.getPrevName());
        remoteViews.setTextViewText(R.id.widget_hero_next_name, prayerState.getNextName());
        remoteViews.setTextViewText(R.id.widget_hero_prev_time, formatTimeShort(prayerState.getPrevMillis()));
        remoteViews.setTextViewText(R.id.widget_hero_next_time, formatTimeShort(prayerState.getNextMillis()));
        remoteViews.setTextViewText(R.id.widget_hero_remaining, faceRemaining(prayerState.getNextName(), compactRemaining(prayerState.getNextMillis() - j), L10n.INSTANCE.getLang().getCode()));
        remoteViews.setProgressBar(R.id.widget_hero_progress, 100, iR, false);
        String strA = HijriDate.INSTANCE.format(calendar);
        if (strA.endsWith(" هـ")) {
            strA = strA.substring(0, strA.length() - 3);
        }
        remoteViews.setTextViewText(R.id.widget_hero_hijri, strA);
        remoteViews.setTextViewText(R.id.widget_hero_greg, new SimpleDateFormat("d MMMM yyyy", new Locale("ar", "EG")).format(calendar.getTime()));
        remoteViews.setOnClickPendingIntent(R.id.widget_hero_root, launchIntent(context));
        RemoteViews remoteViews2 = new RemoteViews(context.getPackageName(), barLayout(context, R.layout.widget_hero_compact));
        remoteViews2.setTextViewText(R.id.widget_hero_compact_name, prayerState.getNextName() + " " + formatTime(prayerState.getNextMillis()));
        StringBuilder sb = new StringBuilder("باقي ");
        sb.append(compactRemaining(prayerState.getNextMillis() - j));
        remoteViews2.setTextViewText(R.id.widget_hero_compact_remaining, sb.toString());
        remoteViews2.setProgressBar(R.id.widget_hero_compact_progress, 100, iR, false);
        remoteViews2.setOnClickPendingIntent(R.id.widget_hero_compact_root, launchIntent(context));
        if (Build.VERSION.SDK_INT >= 31) {
            remoteViews = new RemoteViews(MapsKt.mapOf(new Pair<>(new SizeF(250.0f, 110.0f), remoteViews), new Pair<>(new SizeF(250.0f, 40.0f), remoteViews2)));
        }
        applyToAll(context, appWidgetManager, HeroWidgetProvider.class, remoteViews);
    }

    private final void updateHijri(Context context, AppWidgetManager appWidgetManager, Calendar calendar) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_hijri);
        Triple<Integer,Integer,Integer> v42VarC = HijriDate.INSTANCE.hijriYmd(calendar);
        int iIntValue = ((Integer) v42VarC.getFirst()).intValue();
        int iIntValue2 = ((Integer) v42VarC.getSecond()).intValue();
        remoteViews.setTextViewText(R.id.widget_hijri_day, String.valueOf(((Integer) v42VarC.getThird()).intValue()));
        remoteViews.setTextViewText(R.id.widget_hijri_month, HijriDate.INSTANCE.monthName(iIntValue2));
        remoteViews.setTextViewText(R.id.widget_hijri_year, iIntValue + " هـ");
        remoteViews.setOnClickPendingIntent(R.id.widget_hijri_day, launchIntent(context));
        applyToAll(context, appWidgetManager, HijriWidgetProvider.class, remoteViews);
    }

    private final void updateIqama(Context context, AppWidgetManager appWidgetManager, PrayerState prayerState, long j) {
        IqamaState iqamaStateComputeIqamaState = computeIqamaState(context, prayerState, j);
        if (iqamaStateComputeIqamaState == null) {
            return;
        }
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), barLayout(context, R.layout.widget_iqama));
        remoteViews.setTextViewText(R.id.widget_iqama_countdown, HomeWidgets.mmss(iqamaStateComputeIqamaState.getIqamaMillis() - j));
        remoteViews.setTextViewText(R.id.widget_iqama_adhan, iqamaStateComputeIqamaState.getPrayerName() + " " + formatTimeShort(iqamaStateComputeIqamaState.getAdhanMillis()));
        StringBuilder sb = new StringBuilder("الإقامة ");
        sb.append(formatTimeShort(iqamaStateComputeIqamaState.getIqamaMillis()));
        remoteViews.setTextViewText(R.id.widget_iqama_time, sb.toString());
        remoteViews.setTextViewText(R.id.widget_iqama_next, "الإقامة " + formatTimeShort(iqamaStateComputeIqamaState.getNextIqamaMillis()));
        remoteViews.setProgressBar(R.id.widget_iqama_progress, 100, iqamaStateComputeIqamaState.getProgressPct(), false);
        remoteViews.setOnClickPendingIntent(R.id.widget_iqama_countdown, launchIntent(context));
        applyToAll(context, appWidgetManager, IqamaWidgetProvider.class, remoteViews);
    }

    private final void updateIqamaSmall(Context context, AppWidgetManager appWidgetManager, PrayerState prayerState, long j) {
        IqamaState iqamaStateComputeIqamaState = computeIqamaState(context, prayerState, j);
        if (iqamaStateComputeIqamaState == null) {
            return;
        }
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), barLayout(context, R.layout.widget_iqama_small));
        remoteViews.setTextViewText(R.id.widget_iqama_small_time, formatTimeShort(iqamaStateComputeIqamaState.getIqamaMillis()));
        remoteViews.setProgressBar(R.id.widget_iqama_small_progress, 100, iqamaStateComputeIqamaState.getProgressPct(), false);
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
        remoteViews.setProgressBar(R.id.widget_bar_progress, 100, RangesKt.coerceIn((int) (((j - prayerState.getPrevMillis()) * 100) / nextMillis), 0, 100), false);
        remoteViews.setOnClickPendingIntent(R.id.widget_bar_name, launchIntent(context));
        remoteViews.setOnClickPendingIntent(R.id.widget_bar_time, launchIntent(context));
        applyToAll(context, appWidgetManager, PrayerBarWidgetProvider.class, remoteViews);
    }

    private final void updateSchedule(Context context, AppWidgetManager appWidgetManager, PrayerState prayerState) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_schedule);
        int i = 0;
        List listAsList = Arrays.asList(Integer.valueOf(R.id.widget_name_0), Integer.valueOf(R.id.widget_name_1), Integer.valueOf(R.id.widget_name_2), Integer.valueOf(R.id.widget_name_3), Integer.valueOf(R.id.widget_name_4), Integer.valueOf(R.id.widget_name_5));
        List listAsList2 = Arrays.asList(Integer.valueOf(R.id.widget_time_0), Integer.valueOf(R.id.widget_time_1), Integer.valueOf(R.id.widget_time_2), Integer.valueOf(R.id.widget_time_3), Integer.valueOf(R.id.widget_time_4), Integer.valueOf(R.id.widget_time_5));
        while (i < 6) {
            remoteViews.setTextViewText(((Number) listAsList.get(i)).intValue(), prayerState.getNames().get(i));
            remoteViews.setTextViewText(((Number) listAsList2.get(i)).intValue(), formatTimeShort(prayerState.getTimes().get(i).longValue()));
            int i2 = -12676609;
            int i3 = i == prayerState.getNextIndex() ? -12676609 : -1;
            if (i != prayerState.getNextIndex()) {
                i2 = -5855578;
            }
            remoteViews.setTextColor(((Number) listAsList2.get(i)).intValue(), i3);
            remoteViews.setTextColor(((Number) listAsList.get(i)).intValue(), i2);
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
        remoteViews.setProgressBar(R.id.widget_wide_progress, 100, RangesKt.coerceIn((int) (((j - prayerState.getPrevMillis()) * 100) / nextMillis), 0, 100), false);
        String strA = HijriDate.INSTANCE.format(calendar);
        if (strA.endsWith(" هـ")) {
            strA = strA.substring(0, strA.length() - 3);
        }
        remoteViews.setTextViewText(R.id.widget_wide_hijri, strA);
        remoteViews.setTextViewText(R.id.widget_wide_greg, new SimpleDateFormat("d MMMM yyyy", new Locale("ar", "EG")).format(calendar.getTime()));
        remoteViews.setOnClickPendingIntent(R.id.widget_wide_remaining, launchIntent(context));
        applyToAll(context, appWidgetManager, WideBarWidgetProvider.class, remoteViews);
    }

    public final void pushFaceWidget(Context context, String str) {
        context.getClass();
        str.getClass();
        if (str.equals(FaceWidgetReceiver.PAGE_ID_PRAYER)) {
            PrayerState prayerStateComputeState = computeState(context);
            long jCurrentTimeMillis = System.currentTimeMillis();
            long nextMillis = prayerStateComputeState.getNextMillis() - prayerStateComputeState.getPrevMillis();
            if (nextMillis < 1) {
                nextMillis = 1;
            }
            int iR = RangesKt.coerceIn((int) (((jCurrentTimeMillis - prayerStateComputeState.getPrevMillis()) * 100) / nextMillis), 0, 100);
            String strFaceRemaining = faceRemaining(prayerStateComputeState.getNextName(), compactRemaining(prayerStateComputeState.getNextMillis() - jCurrentTimeMillis), L10n.INSTANCE.getLang().getCode());
            Intent intent = new Intent(FaceWidgetReceiver.ACTION_RESPONSE);
            intent.setPackage("com.android.systemui");
            intent.putExtra("package", context.getPackageName());
            intent.putExtra("pageId", str);
            intent.putExtra("show", true);
            intent.putExtra("origin", pushFaceWidget$build(context, prayerStateComputeState, strFaceRemaining, iR, R.layout.facewidget_prayer));
            intent.putExtra("aod", pushFaceWidget$build(context, prayerStateComputeState, strFaceRemaining, iR, R.layout.facewidget_prayer_aod));
            context.sendBroadcast(intent);
        }
    }





    private final String compactRemaining(long j) {
        return compactRemaining(j, L10n.INSTANCE.getLang().getCode());
    }
}
