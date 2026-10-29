plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.wathakker.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wathakker.app"
        minSdk = 30
        targetSdk = 36
        versionCode = 73
        versionName = "1.1.2"
    }

    signingConfigs {
        create("morphe") {
            val ks = System.getenv("MORPHE_KEYSTORE_FILE")
            if (ks != null) {
                storeFile = file(ks)
                storePassword = System.getenv("MORPHE_STORE_PASSWORD")
                keyAlias = System.getenv("MORPHE_KEY_ALIAS")
                keyPassword = System.getenv("MORPHE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("MORPHE_KEYSTORE_FILE") != null) signingConfig = signingConfigs.getByName("morphe")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    testOptions { unitTests.isIncludeAndroidResources = true }

}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.wear.protolayout:protolayout-expression-pipeline:1.3.0")
    testImplementation("org.robolectric:robolectric:4.15.1")
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.wear.compose:compose-foundation:1.5.0")
    implementation("androidx.wear.tiles:tiles:1.5.0")
    implementation("androidx.wear.tiles:tiles-renderer:1.5.0")
    implementation("androidx.wear.protolayout:protolayout:1.3.0")
    implementation("androidx.wear.protolayout:protolayout-expression:1.3.0")
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")
    implementation("androidx.wear.watchface:watchface-complications-rendering:1.2.1")
    implementation("androidx.wear:wear-remote-interactions:1.1.0")
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    implementation("com.google.guava:guava:33.4.0-android")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
