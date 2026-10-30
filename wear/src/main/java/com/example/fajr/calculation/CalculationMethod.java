package com.example.fajr.calculation;

public enum CalculationMethod {
    MUSLIM_WORLD_LEAGUE(0, "\u0631\u0627\u0628\u0637\u0629 \u0627\u0644\u0639\u0627\u0644\u0645 \u0627\u0644\u0625\u0633\u0644\u0627\u0645\u064a (18\u00b0)", 18.0d, 17.0d, null, "Muslim World League (18°)"),
    EGYPTIAN(1, "\u0627\u0644\u0647\u064a\u0626\u0629 \u0627\u0644\u0645\u0635\u0631\u064a\u0629 \u0627\u0644\u0639\u0627\u0645\u0629 \u0644\u0644\u0645\u0633\u0627\u062d\u0629 (19.5\u00b0)", 19.5d, 17.5d, null, "Egyptian General Authority of Survey (19.5°)"),
    UMM_AL_QURA(2, "\u0623\u0645 \u0627\u0644\u0642\u0631\u0649 - \u0645\u0643\u0629 \u0627\u0644\u0645\u0643\u0631\u0645\u0629 (18.5\u00b0)", 18.5d, null, 90, "Umm al-Qura, Makkah (18.5°)"),
    ISNA(3, "\u0627\u0644\u062c\u0645\u0639\u064a\u0629 \u0627\u0644\u0625\u0633\u0644\u0627\u0645\u064a\u0629 \u0644\u0634\u0645\u0627\u0644 \u0623\u0645\u0631\u064a\u0643\u0627 (15\u00b0)", 15.0d, 15.0d, null, "Islamic Society of North America (15°)"),
    KARACHI(4, "\u062c\u0627\u0645\u0639\u0629 \u0627\u0644\u0639\u0644\u0648\u0645 \u0627\u0644\u0625\u0633\u0644\u0627\u0645\u064a\u0629 \u0628\u0643\u0631\u0627\u062a\u0634\u064a (18\u00b0)", 18.0d, 18.0d, null, "Univ. of Islamic Sciences, Karachi (18°)"),
    DUBAI(5, "\u062f\u0628\u064a (18.2\u00b0)", 18.2d, 18.2d, null, "Dubai (18.2°)"),
    KUWAIT(6, "\u0627\u0644\u0643\u0648\u064a\u062a (18\u00b0)", 18.0d, 17.5d, null, "Kuwait (18°)"),
    QATAR(7, "\u0642\u0637\u0631 (18\u00b0)", 18.0d, null, 90, "Qatar (18°)");

    public static final Companion INSTANCE = new Companion();
    private final double fajrAngle;
    private final int id;
    private final Double ishaAngle;
    private final Integer ishaMinutesAfterMaghrib;
    private final String titleAr;
    private final String titleEn;

    CalculationMethod(int id, String titleAr, double fajrAngle, Double ishaAngle, Integer ishaMinutesAfterMaghrib, String titleEn) {
        this.id = id;
        this.titleAr = titleAr;
        this.titleEn = titleEn;
        this.fajrAngle = fajrAngle;
        this.ishaAngle = ishaAngle;
        this.ishaMinutesAfterMaghrib = ishaMinutesAfterMaghrib;
    }

    public final double getFajrAngle() { return this.fajrAngle; }
    public final int getId() { return this.id; }
    public final Double getIshaAngle() { return this.ishaAngle; }
    public final Integer getIshaMinutesAfterMaghrib() { return this.ishaMinutesAfterMaghrib; }
    public final String getTitleAr() { return this.titleAr; }

    public final String getTitleEn() { return this.titleEn; }

    public final String title(String lang) { return "ar".equals(lang) ? this.titleAr : this.titleEn; }

    public static final class Companion {
        private Companion() {
        }

        public final CalculationMethod fromId(int id) {
            for (CalculationMethod m : CalculationMethod.values()) {
                if (m.getId() == id) {
                    return m;
                }
            }
            return CalculationMethod.MUSLIM_WORLD_LEAGUE;
        }
    }
}
