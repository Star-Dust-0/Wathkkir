# Guava / JSR-305 noise from play-services-wearable + guava.
-dontwarn com.google.common.**
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**
# Manifest-declared entry points (MainActivity, TileService, complication providers,
# AdhanReceiver, BootReceiver, WearableListenerService) are kept automatically by aapt rules.
