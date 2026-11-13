# Keep rules for the R8/minified release build.
# The widget/refresh/receiver classes are kept defensively: widgets and their tap
# routing must survive shrinking exactly as in the launcher app - the 3.1.0
# widget regression came from an empty keep list.

# FajrPreferences: getLatitude/getLongitude/getCalculationMethodId via reflection
-keep class com.example.fajr.data.FajrPreferences { *; }

# CalculationMethod.INSTANCE.fromId (named companion)
-keep class com.example.fajr.calculation.CalculationMethod { *; }
-keep class com.example.fajr.calculation.CalculationMethod$INSTANCE { *; }

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
