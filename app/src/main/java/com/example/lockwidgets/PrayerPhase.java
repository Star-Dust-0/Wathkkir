package com.example.lockwidgets;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class PrayerPhase {
    private static final int[] MINUTES = {25, 0, 20, 20, 10, 20};
    public final boolean iqama;
    public final int previousIndex;
    public final long start;
    public final long target;
    public final int targetIndex;

    private PrayerPhase(int i, int i2, long j, long j2, boolean z) {
        this.targetIndex = i;
        this.previousIndex = i2;
        this.start = j;
        this.target = j2;
        this.iqama = z;
    }

    public static String iqamaFor(String str, String str2) {
        if (str.startsWith("ar")) {
            if (str2 == null) {
                str2 = "";
            }
            return "الإقامة ".concat(str2.startsWith("ال") ? "لل".concat(str2.substring(2)) : "ل".concat(str2));
        }
        return iqamaLabel(str) + " " + str2;
    }

    public static String iqamaLabel(String str) {
        return str.startsWith("ar") ? "الإقامة" : (str.startsWith("fr") || str.startsWith("de")) ? "Iqama" : str.startsWith("tr") ? "Kamet" : str.startsWith("ur") ? "اقامت" : str.startsWith("id") ? "Iqamah" : str.startsWith("ru") ? "Икама" : "Iqama";
    }

    public static int iqamaMinutes(int i) {
        return MINUTES[i];
    }

    public static PrayerPhase resolve(long[] jArr, long[] jArr2, long[] jArr3, long j, boolean z, boolean z2) {
        long[] window = window(jArr, jArr2, j, z);
        if (window != null) {
            long j2 = window[0];
            return new PrayerPhase((int) j2, (int) j2, window[1], window[2], true);
        }
        int i = 5;
        long j3 = jArr2[5];
        for (int i2 = 0; i2 < 6; i2++) {
            if (i2 != 1 || z2) {
                long j4 = jArr[i2];
                if (j4 > j) {
                    return new PrayerPhase(i2, i, j3, j4, false);
                }
                i = i2;
                j3 = j4;
            }
        }
        return new PrayerPhase(0, 5, jArr[5], jArr3[0], false);
    }

    public static long[] window(long[] jArr, long[] jArr2, long j, boolean z) {
        for (int i = 5; i >= 0; i--) {
            long j2 = jArr[i];
            int i2 = MINUTES[i];
            long j3 = (i2 * 60000) + j2;
            if (i2 > 0 && ((!z || i != 2) && j2 <= j && j < j3)) {
                return new long[]{i, j2, j3};
            }
        }
        if (jArr2 == null) {
            return null;
        }
        long j4 = jArr2[5];
        if (j4 > j) {
            return null;
        }
        int i3 = MINUTES[5];
        if (j < (i3 * 60000) + j4) {
            return new long[]{5, j4, (i3 * 60000) + j4};
        }
        return null;
    }

    public String label(String[] strArr, String str) {
        boolean z = this.iqama;
        int i = this.targetIndex;
        return z ? iqamaFor(str, strArr[i]) : strArr[i];
    }
}
