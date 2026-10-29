package com.example.prayer.alerts;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.fajr.data.FajrPreferences;
import com.example.utils.NotifPrefs;
import com.example.utils.Str;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class PrayerAlertStore {
    public static final int $stable = 0;
    public static final PrayerAlertStore INSTANCE = new PrayerAlertStore();
    private static final String PREFS = "prayer_alerts";

    private PrayerAlertStore() {
    }

    private final SharedPreferences p(Context context) {
        return context.getSharedPreferences(PREFS, 0);
    }

    public final boolean autoDnd(Context context, int i) {
        context.getClass();
        migrate(context);
        return p(context).getBoolean("dnd_" + i, false);
    }

    public final void clearOverride(Context context, int i) {
        context.getClass();
        p(context).edit().remove("tone_ov_" + i).remove("tone_ov_title_" + i).remove("vib_ov_" + i).apply();
        PrayerAlertReceiver.Companion.refreshChannels(context);
    }

    public final String effectiveTone(Context context, int i) {
        context.getClass();
        String str = toneOverride(context, i);
        return str == null ? NotifPrefs.INSTANCE.tone(context) : str.equals("default") ? "" : str;
    }

    public final boolean effectiveVibration(Context context, int i) {
        context.getClass();
        Boolean vibrationOverride = vibrationOverride(context, i);
        return vibrationOverride != null ? vibrationOverride.booleanValue() : NotifPrefs.INSTANCE.vibration(context);
    }

    public final boolean hasOverride(Context context, int i) {
        context.getClass();
        return (toneOverride(context, i) == null && vibrationOverride(context, i) == null) ? false : true;
    }

    public final boolean iqamaReminder(Context context, int i) {
        context.getClass();
        migrate(context);
        return p(context).getBoolean("iqama_" + i, false);
    }

    public final boolean isEnabled(Context context, int i) {
        context.getClass();
        migrate(context);
        return p(context).getBoolean("enabled_" + i, false);
    }

    public final boolean isFajrAlarmActive(Context context) {
        context.getClass();
        migrate(context);
        return new FajrPreferences(context).isAlarmEnabled();
    }

    public final synchronized void migrate(Context context) {
        try {
            context.getClass();
            SharedPreferences p = p(context);
            if (p.getBoolean("migrated_v1", false)) {
                return;
            }
            FajrPreferences fajrPreferences = new FajrPreferences(context);
            if (fajrPreferences.isAlarmEnabled()) {
                p.edit().putBoolean("enabled_0", true).apply();
                fajrPreferences.setAlarmEnabled(false);
            }
            p.edit().putBoolean("migrated_v1", true).apply();
        } catch (Throwable th) {
            throw th;
        }
    }

    public final int offsetMinutes(Context context, int i) {
        context.getClass();
        migrate(context);
        return p(context).getInt("offset_" + i, 0);
    }

    public final void restore(Context context, int i, Map<String, ? extends Object> map) {
        context.getClass();
        map.getClass();
        SharedPreferences.Editor edit = p(context).edit();
        Set<String> keySet = p(context).getAll().keySet();
        ArrayList arrayList = new ArrayList();
        for (Object obj : keySet) {
            String str = (String) obj;
            str.getClass();
            if (str.endsWith("_" + i) && !map.containsKey(str)) {
                arrayList.add(obj);
            }
        }
        int size = arrayList.size();
        int i2 = 0;
        while (i2 < size) {
            Object obj2 = arrayList.get(i2);
            i2++;
            edit.remove((String) obj2);
        }
        for (Map.Entry<String, ? extends Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Boolean) {
                edit.putBoolean(key, ((Boolean) value).booleanValue());
            } else if (value instanceof Integer) {
                edit.putInt(key, ((Number) value).intValue());
            } else if (value instanceof Long) {
                edit.putLong(key, ((Number) value).longValue());
            } else if (value instanceof Float) {
                edit.putFloat(key, ((Number) value).floatValue());
            } else if (value instanceof String) {
                edit.putString(key, (String) value);
            }
        }
        edit.apply();
    }

    public final void setAutoDnd(Context context, int i, boolean z) {
        context.getClass();
        migrate(context);
        p(context).edit().putBoolean("dnd_" + i, z).apply();
    }

    public final void setEnabled(Context context, int i, boolean z) {
        context.getClass();
        migrate(context);
        p(context).edit().putBoolean("enabled_" + i, z).apply();
    }

    public final void setIqamaReminder(Context context, int i, boolean z) {
        context.getClass();
        migrate(context);
        p(context).edit().putBoolean("iqama_" + i, z).apply();
    }

    public final void setOffsetMinutes(Context context, int i, int i2) {
        context.getClass();
        migrate(context);
        p(context).edit().putInt(Str.K(i, "offset_"), com.example.utils.Str.coerce(i2, 0, 60)).apply();
    }

    public final void setToneOverride(Context context, int i, String str, String str2) {
        context.getClass();
        str.getClass();
        SharedPreferences.Editor edit = p(context).edit();
        String K = Str.K(i, "tone_ov_");
        if (str.length() == 0) {
            str = "default";
        }
        SharedPreferences.Editor putString = edit.putString(K, str);
        String K2 = Str.K(i, "tone_ov_title_");
        if (str2 == null) {
            str2 = "";
        }
        putString.putString(K2, str2).apply();
        PrayerAlertReceiver.Companion.refreshChannels(context);
    }

    public final void setVibrationOverride(Context context, int i, boolean z) {
        context.getClass();
        p(context).edit().putBoolean("vib_ov_" + i, z).apply();
        PrayerAlertReceiver.Companion.refreshChannels(context);
    }

    public final Map<String, Object> snapshot(Context context, int i) {
        context.getClass();
        Map<String, ?> all = p(context).getAll();
        all.getClass();
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            String str = "_" + i;
            key.getClass();
            if (key.endsWith(str)) {
                linkedHashMap.put(entry.getKey(), entry.getValue());
            }
        }
        return linkedHashMap;
    }

    public final String toneOverride(Context context, int i) {
        context.getClass();
        return p(context).getString("tone_ov_" + i, null);
    }

    public final String toneOverrideTitle(Context context, int i) {
        context.getClass();
        String string = p(context).getString("tone_ov_title_" + i, null);
        if (string == null || com.example.utils.Str.isBlank(string)) {
            return null;
        }
        return string;
    }

    public final Boolean vibrationOverride(Context context, int i) {
        context.getClass();
        if (!p(context).contains("vib_ov_" + i)) {
            return null;
        }
        return Boolean.valueOf(p(context).getBoolean("vib_ov_" + i, true));
    }
}
