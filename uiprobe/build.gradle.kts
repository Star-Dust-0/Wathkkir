// Verification-only widget host (tools/widgethost). Not part of the released apps.
plugins { id("com.android.application") }
android {
    namespace = "com.example.widgethost"
    compileSdk = 36
    defaultConfig { applicationId = "com.example.uiprobe"; minSdk = 23; targetSdk = 34; versionCode = 1; versionName = "1" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    sourceSets["main"].manifest.srcFile("../tools/widgethost/AndroidManifest.xml")
    sourceSets["main"].java.srcDirs("../tools/widgethost")
}
