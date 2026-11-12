# Keep rules prepared for enabling R8/minification on the release build.
# The lock-screen and home widgets (LockWidgets.java / HomeWidgets.java) reach these
# classes only by reflection (Class.forName / getMethod), so shrinking would strip
# them and blank every widget - this is exactly the 3.1.0 widget regression.

# FajrPreferences: getLatitude/getLongitude/getCalculationMethodId via reflection
-keep class com.example.fajr.data.FajrPreferences { *; }

# CalculationMethod.Companion.fromId via reflection
-keep class com.example.fajr.calculation.CalculationMethod { *; }
-keep class com.example.fajr.calculation.CalculationMethod$Companion { *; }

# PrayerAdjustments.INSTANCE.calculateAll(...) via reflection
-keep class com.example.fajr.calculation.PrayerAdjustments { *; }

# DayPrayerTimes getters (getFajr ... getIsha) via reflection
-keep class com.example.fajr.calculation.PrayerTimesCalculator { *; }
-keep class com.example.fajr.calculation.PrayerTimesCalculator$DayPrayerTimes { *; }

# Widget providers and receivers are declared in the manifest but never referenced
# from code; keep them from being removed or renamed.
-keep class com.example.widget.** { *; }
-keep class com.example.lockwidgets.** { *; }
-keep class com.example.receiver.** { *; }
-keep class com.example.prayer.alerts.** { *; }
-keep class com.example.fajr.alarm.** { *; }
