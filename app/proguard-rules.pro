# Wathakker release (R8) keep rules.

# Reflective access from the home/lock widget renderers:
# Class.forName("com.example.fajr.data.FajrPreferences").getMethod("getCityNameLocalized", ...)
# Class.forName("com.example.fajr.calculation.CalculationMethod").getField("Companion"/"INSTANCE"), fromId(int)
# Class.forName("com.example.fajr.calculation.PrayerAdjustments").getMethod("calculateAll", ...)
-keep class com.example.fajr.data.FajrPreferences { *; }
-keep class com.example.fajr.calculation.** { *; }

# Room instantiates AppDatabase_Impl and the DAO _Impl classes reflectively.
-keep class com.example.data.** { *; }

# App widgets / lock-screen widgets / tick + face receivers: several are also
# referenced by string-built ComponentName("com.example.widget." + kind + "WidgetProvider").
-keep class com.example.widget.** { *; }
-keep class com.example.lockwidgets.** { *; }

# Alarm + boot receivers (manifest-declared; belt-and-suspenders).
-keep class com.example.prayer.alerts.** { *; }
-keep class com.example.receiver.** { *; }
-keep class com.example.prayer.EventReminderReceiver { *; }

# Kotlin/Java metadata needed by reflection-adjacent code.
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,AnnotationDefault
# Keep original file/line info in release stack traces (cheap, vital for field diagnosis).
-keepattributes SourceFile,LineNumberTable


# Third-party platform-detection warnings are harmless.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
