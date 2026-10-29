package com.example.fajr.calculation;

/* JADX WARN: Enum visitor error
jadx.core.utils.exceptions.JadxRuntimeException: Init of enum field 'EGYPTIAN' uses external variables
	at jadx.core.dex.visitors.EnumVisitor.createEnumFieldByConstructor(EnumVisitor.java:451)
	at jadx.core.dex.visitors.EnumVisitor.processEnumFieldByField(EnumVisitor.java:372)
	at jadx.core.dex.visitors.EnumVisitor.processEnumFieldByWrappedInsn(EnumVisitor.java:337)
	at jadx.core.dex.visitors.EnumVisitor.extractEnumFieldsFromFilledArray(EnumVisitor.java:322)
	at jadx.core.dex.visitors.EnumVisitor.extractEnumFieldsFromInsn(EnumVisitor.java:262)
	at jadx.core.dex.visitors.EnumVisitor.extractEnumFieldsFromInvoke(EnumVisitor.java:293)
	at jadx.core.dex.visitors.EnumVisitor.extractEnumFieldsFromInsn(EnumVisitor.java:266)
	at jadx.core.dex.visitors.EnumVisitor.convertToEnum(EnumVisitor.java:151)
	at jadx.core.dex.visitors.EnumVisitor.visit(EnumVisitor.java:100)
 */
/* JADX WARN: Failed to restore enum class, 'enum' modifier and super class removed */
/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class CalculationMethod {
    private static final /* synthetic */ CalculationMethod[] $VALUES;
    public static final CalculationMethod DUBAI;
    public static final CalculationMethod EGYPTIAN;
    public static final Companion INSTANCE;
    public static final CalculationMethod ISNA;
    public static final CalculationMethod KARACHI;
    public static final CalculationMethod KUWAIT;
    public static final CalculationMethod MUSLIM_WORLD_LEAGUE = new CalculationMethod("MUSLIM_WORLD_LEAGUE", 0, 0, "رابطة العالم الإسلامي (18°)", 18.0d, Double.valueOf(17.0d), null, "Muslim World League (18°)");
    public static final CalculationMethod QATAR;
    public static final CalculationMethod UMM_AL_QURA;
    private final double fajrAngle;
    private final int id;
    private final Double ishaAngle;
    private final Integer ishaMinutesAfterMaghrib;
    private final String titleAr;
    private final String titleEn;

    private static /* synthetic */ CalculationMethod[] $values() {
        return new CalculationMethod[]{MUSLIM_WORLD_LEAGUE, EGYPTIAN, UMM_AL_QURA, ISNA, KARACHI, DUBAI, KUWAIT, QATAR};
    }

    static {
        Double valueOf = Double.valueOf(17.5d);
        EGYPTIAN = new CalculationMethod("EGYPTIAN", 1, 1, "الهيئة المصرية العامة للمساحة (19.5°)", 19.5d, valueOf, null, "Egyptian General Authority of Survey (19.5°)");
        UMM_AL_QURA = new CalculationMethod("UMM_AL_QURA", 2, 2, "أم القرى - مكة المكرمة (18.5°)", 18.5d, null, 90, "Umm al-Qura, Makkah (18.5°)");
        ISNA = new CalculationMethod("ISNA", 3, 3, "الجمعية الإسلامية لشمال أمريكا (15°)", 15.0d, Double.valueOf(15.0d), null, "Islamic Society of North America (15°)");
        KARACHI = new CalculationMethod("KARACHI", 4, 4, "جامعة العلوم الإسلامية بكراتشي (18°)", 18.0d, Double.valueOf(18.0d), null, "Univ. of Islamic Sciences, Karachi (18°)");
        DUBAI = new CalculationMethod("DUBAI", 5, 5, "دبي (18.2°)", 18.2d, Double.valueOf(18.2d), null, "Dubai (18.2°)");
        KUWAIT = new CalculationMethod("KUWAIT", 6, 6, "الكويت (18°)", 18.0d, valueOf, null, "Kuwait (18°)");
        QATAR = new CalculationMethod("QATAR", 7, 7, "قطر (18°)", 18.0d, null, 90, "Qatar (18°)");
        $VALUES = $values();
        INSTANCE = new Companion(0);
    }

    private CalculationMethod(String str, int i, int i2, String str2, double d, Double d2, Integer num, String str3) {
        this.id = i2;
        this.titleAr = str2;
        this.titleEn = str3;
        this.fajrAngle = d;
        this.ishaAngle = d2;
        this.ishaMinutesAfterMaghrib = num;
    }


    public static CalculationMethod[] values() {
        return (CalculationMethod[]) $VALUES.clone();
    }

    public final double getFajrAngle() {
        return this.fajrAngle;
    }

    public final int getId() {
        return this.id;
    }

    public final Double getIshaAngle() {
        return this.ishaAngle;
    }

    public final Integer getIshaMinutesAfterMaghrib() {
        return this.ishaMinutesAfterMaghrib;
    }

    public final String getTitleAr() {
        return this.titleAr;
    }

    public final String getTitleEn() {
        return this.titleEn;
    }

    public final String title(String str) {
        return "ar".equals(str) ? this.titleAr : this.titleEn;
    }

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static final class Companion {
        public /* synthetic */ Companion(int i) {
            this();
        }

        public final CalculationMethod fromId(int i) {
            for (CalculationMethod calculationMethod : CalculationMethod.values()) {
                if (calculationMethod.getId() == i) {
                    return calculationMethod;
                }
            }
            return CalculationMethod.MUSLIM_WORLD_LEAGUE;
        }

        private Companion() {
        }
    }
}
