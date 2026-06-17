plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.hilt)
  id("kotlin-kapt")
}

android {
    namespace = "com.example.phishtrack"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.phishtrack"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
    }
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-extended")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // MockK for Kotlin mocking in unit tests
  testImplementation("io.mockk:mockk:1.13.12")

  // Robolectric: run Android tests on JVM
  testImplementation("org.robolectric:robolectric:4.13")
  testImplementation("androidx.test:core-ktx:1.6.1")
  testImplementation("androidx.test.ext:junit-ktx:1.2.1")

  // Turbine: test Kotlin Flows
  testImplementation("app.cash.turbine:turbine:1.2.0")

  // Compose UI testing on JVM via Robolectric
  testImplementation("androidx.compose.ui:ui-test-junit4")
  debugImplementation("androidx.compose.ui:ui-test-manifest")

  // Hilt testing support
  testImplementation("com.google.dagger:hilt-android-testing:2.56")
  add("kaptTest", "com.google.dagger:hilt-android-compiler:2.56")

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Hilt Dependency Injection
  implementation(libs.dagger.hilt.android)
  add("kapt", libs.dagger.hilt.compiler)
  implementation(libs.androidx.hilt.navigation.compose)

  // Retrofit & OkHttp Networking
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.gson)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)

  // Room Offline Database Caching
  implementation(libs.androidx.room.runtime)
  add("kapt", libs.androidx.room.compiler)
  implementation(libs.androidx.room.ktx)

  // MapLibre — free vector maps, no API key, OpenFreeMap tiles
  implementation("org.maplibre.gl:android-sdk:11.0.1")

  // Biometrics Lock
  implementation(libs.androidx.biometric)

  // Coil Image Loading
  implementation(libs.coil.compose)
}
