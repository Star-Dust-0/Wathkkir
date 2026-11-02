// Wear-only verification renderer; keep phone inspection helper minSdk23 unchanged.
plugins { id("com.android.application") }
android {
 namespace="com.example.widgethost"
 compileSdk=36
 defaultConfig { applicationId="com.example.tilehost"; minSdk=26; targetSdk=33; versionCode=1; versionName="1" }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 sourceSets["main"].manifest.srcFile("../tools/tilehost/AndroidManifest.xml")
 sourceSets["main"].java.srcDirs("../tools/tilehost")
}
dependencies { implementation("androidx.wear.watchface:watchface-complications-data-source:1.2.1"); implementation("androidx.wear.tiles:tiles-renderer:1.4.1"); implementation("androidx.wear.protolayout:protolayout:1.3.0") }
