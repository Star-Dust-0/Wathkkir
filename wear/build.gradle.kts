plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.wathakker.wear"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.wathakker.app.compare"
    minSdk = 30
    targetSdk = 36
    versionCode = 73
    versionName = "1.1.2"
  }

  signingConfigs {
    create("debugConfig") {
      storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debugConfig")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  lint { abortOnError = false }
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.addAll(listOf("-Xlint:none"))
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:${providers.gradleProperty("composeBom").getOrElse("2025.10.01")}"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.foundation:foundation")
  implementation("androidx.wear.compose:compose-foundation:1.5.0")
  implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")
  implementation("androidx.wear.tiles:tiles:1.4.1")
  implementation("androidx.wear.protolayout:protolayout:1.2.1")
  implementation("androidx.wear.protolayout:protolayout-expression:1.2.1")
  implementation("androidx.wear:wear-remote-interactions:1.1.0")
  implementation("com.google.guava:guava:33.3.1-android")
  implementation("com.google.android.gms:play-services-wearable:19.0.0")
  implementation("androidx.wear.tiles:tiles-renderer:1.4.1")
  implementation("androidx.wear.watchface:watchface-complications-rendering:1.2.1")
}
