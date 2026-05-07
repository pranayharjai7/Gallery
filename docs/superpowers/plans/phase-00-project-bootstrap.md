# Phase 0: Project Bootstrap — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a buildable Android project skeleton with all dependencies, manifest, and resources in place.

**Depends on:** Nothing — this is the first phase.

---

## Task 0.1: Gradle Version Catalog

Create `gradle/libs.versions.toml` with all library versions, aliases, plugins, and bundles needed for the project.

- [ ] Create the directory `gradle/` at the project root if it does not already exist.
- [ ] Create `gradle/libs.versions.toml` with the following content:

```toml
[versions]
agp                     = "8.4.0"
kotlin                  = "2.0.0"
ksp                     = "2.0.0-1.0.22"
composeBom              = "2024.09.00"
hilt                    = "2.51.1"
hiltExt                 = "1.2.0"
hiltWork                = "1.2.0"
room                    = "2.6.1"
coil                    = "3.0.0-rc02"
media3                  = "1.4.0"
workmanager             = "2.9.1"
navigation              = "2.8.0"
datastore               = "1.1.1"
securityCrypto          = "1.1.0-alpha06"
biometric               = "1.2.0-alpha05"
coroutines              = "1.8.1"
turbine                 = "1.1.0"
mockk                   = "1.13.12"
accompanist             = "0.36.0"
coreKtx                 = "1.13.1"
lifecycleRuntimeKtx     = "2.8.6"
activityCompose         = "1.9.2"
junit                   = "4.13.2"
androidxJunit           = "1.2.1"
espresso                = "3.6.1"

[libraries]
# AndroidX Core
androidx-core-ktx                   = { group = "androidx.core",             name = "core-ktx",                      version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx      = { group = "androidx.lifecycle",        name = "lifecycle-runtime-ktx",         version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose           = { group = "androidx.activity",         name = "activity-compose",              version.ref = "activityCompose" }

# Compose BOM + UI
androidx-compose-bom                = { group = "androidx.compose",          name = "compose-bom",                   version.ref = "composeBom" }
androidx-compose-ui                 = { group = "androidx.compose.ui",       name = "ui" }
androidx-compose-ui-graphics        = { group = "androidx.compose.ui",       name = "ui-graphics" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui",       name = "ui-tooling-preview" }
androidx-compose-ui-tooling         = { group = "androidx.compose.ui",       name = "ui-tooling" }
androidx-compose-ui-test-manifest   = { group = "androidx.compose.ui",       name = "ui-test-manifest" }
androidx-compose-ui-test-junit4     = { group = "androidx.compose.ui",       name = "ui-test-junit4" }
androidx-compose-material3          = { group = "androidx.compose.material3", name = "material3" }

# Navigation
androidx-navigation-compose         = { group = "androidx.navigation",       name = "navigation-compose",            version.ref = "navigation" }
androidx-navigation-testing         = { group = "androidx.navigation",       name = "navigation-testing",            version.ref = "navigation" }

# Hilt
hilt-android                        = { group = "com.google.dagger",         name = "hilt-android",                  version.ref = "hilt" }
hilt-android-compiler               = { group = "com.google.dagger",         name = "hilt-android-compiler",         version.ref = "hilt" }
hilt-android-testing                = { group = "com.google.dagger",         name = "hilt-android-testing",          version.ref = "hilt" }
hilt-navigation-compose             = { group = "androidx.hilt",             name = "hilt-navigation-compose",       version.ref = "hiltExt" }
hilt-work                           = { group = "androidx.hilt",             name = "hilt-work",                     version.ref = "hiltWork" }
hilt-compiler                       = { group = "androidx.hilt",             name = "hilt-compiler",                 version.ref = "hiltExt" }

# Room
room-runtime                        = { group = "androidx.room",             name = "room-runtime",                  version.ref = "room" }
room-ktx                            = { group = "androidx.room",             name = "room-ktx",                      version.ref = "room" }
room-compiler                       = { group = "androidx.room",             name = "room-compiler",                 version.ref = "room" }
room-testing                        = { group = "androidx.room",             name = "room-testing",                  version.ref = "room" }

# Coil
coil-compose                        = { group = "io.coil-kt.coil3",         name = "coil-compose",                  version.ref = "coil" }
coil-video                          = { group = "io.coil-kt.coil3",         name = "coil-video",                    version.ref = "coil" }

# Media3
media3-exoplayer                    = { group = "androidx.media3",           name = "media3-exoplayer",              version.ref = "media3" }
media3-ui                           = { group = "androidx.media3",           name = "media3-ui",                     version.ref = "media3" }

# WorkManager
workmanager-runtime-ktx             = { group = "androidx.work",             name = "work-runtime-ktx",              version.ref = "workmanager" }
workmanager-testing                 = { group = "androidx.work",             name = "work-testing",                  version.ref = "workmanager" }

# DataStore
datastore-preferences               = { group = "androidx.datastore",        name = "datastore-preferences",         version.ref = "datastore" }

# Security & Biometric
security-crypto                     = { group = "androidx.security",         name = "security-crypto",               version.ref = "securityCrypto" }
biometric-ktx                       = { group = "androidx.biometric",        name = "biometric-ktx",                 version.ref = "biometric" }

# Coroutines
kotlinx-coroutines-android          = { group = "org.jetbrains.kotlinx",    name = "kotlinx-coroutines-android",    version.ref = "coroutines" }
kotlinx-coroutines-test             = { group = "org.jetbrains.kotlinx",    name = "kotlinx-coroutines-test",       version.ref = "coroutines" }

# Accompanist
accompanist-permissions             = { group = "com.google.accompanist",    name = "accompanist-permissions",       version.ref = "accompanist" }

# Test — unit
junit                               = { group = "junit",                     name = "junit",                         version.ref = "junit" }
turbine                             = { group = "app.cash.turbine",          name = "turbine",                       version.ref = "turbine" }
mockk                               = { group = "io.mockk",                  name = "mockk",                         version.ref = "mockk" }

# Test — instrumented
androidx-junit                      = { group = "androidx.test.ext",         name = "junit",                         version.ref = "androidxJunit" }
androidx-espresso-core              = { group = "androidx.test.espresso",    name = "espresso-core",                 version.ref = "espresso" }
mockk-android                       = { group = "io.mockk",                  name = "mockk-android",                 version.ref = "mockk" }

[plugins]
android-application    = { id = "com.android.application",            version.ref = "agp" }
android-library        = { id = "com.android.library",                version.ref = "agp" }
kotlin-android         = { id = "org.jetbrains.kotlin.android",       version.ref = "kotlin" }
kotlin-compose         = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt                   = { id = "com.google.dagger.hilt.android",     version.ref = "hilt" }
ksp                    = { id = "com.google.devtools.ksp",             version.ref = "ksp" }
```

- [ ] Verify the file is valid TOML by visually checking that every `version.ref` key matches a key defined in the `[versions]` block.
- [ ] Stage and commit:

```bash
git add gradle/libs.versions.toml && git commit -m "chore: add gradle version catalog (libs.versions.toml)"
```

---

## Task 0.2: Root Build Files

Create the three root-level Gradle build files that configure the project structure, plugin declarations, and JVM properties.

- [ ] Create `settings.gradle.kts` at the project root with the following content:

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Gallery"
include(":app")
```

- [ ] Create root `build.gradle.kts` at the project root with the following content:

```kotlin
// Top-level build file — plugin declarations only; no code goes here.
plugins {
    alias(libs.plugins.android.application)  apply false
    alias(libs.plugins.android.library)      apply false
    alias(libs.plugins.kotlin.android)       apply false
    alias(libs.plugins.kotlin.compose)       apply false
    alias(libs.plugins.hilt)                 apply false
    alias(libs.plugins.ksp)                  apply false
}
```

- [ ] Create `gradle.properties` at the project root with the following content:

```properties
# Project-wide Gradle settings
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true

# AndroidX
android.useAndroidX=true

# Kotlin
kotlin.code.style=official

# Compose compiler metrics (disable in CI to reduce noise)
# kotlin.compose.compiler.metrics=true
# kotlin.compose.compiler.reports=true
```

- [ ] Stage and commit:

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties && git commit -m "chore: add root build files (settings, build, gradle.properties)"
```

---

## Task 0.3: App Module Build File

Create `app/build.gradle.kts` with the full Android application configuration, compile options, Compose setup, and all dependency declarations wired from the version catalog.

- [ ] Create the `app/` directory at the project root if it does not already exist.
- [ ] Create `app/build.gradle.kts` with the following content:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace  = "com.gallery"
    compileSdk = 35

    defaultConfig {
        applicationId         = "com.gallery"
        minSdk                = 31
        targetSdk             = 35
        versionCode           = 1
        versionName           = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── Core ─────────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // ── Compose ──────────────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ── Navigation ───────────────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)

    // ── Hilt ─────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // ── Room ─────────────────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── Coil ─────────────────────────────────────────────────────────────
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // ── Media3 ───────────────────────────────────────────────────────────
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // ── WorkManager ──────────────────────────────────────────────────────
    implementation(libs.workmanager.runtime.ktx)

    // ── DataStore ────────────────────────────────────────────────────────
    implementation(libs.datastore.preferences)

    // ── Security & Biometric ─────────────────────────────────────────────
    implementation(libs.security.crypto)
    implementation(libs.biometric.ktx)

    // ── Coroutines ───────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── Accompanist ──────────────────────────────────────────────────────
    implementation(libs.accompanist.permissions)

    // ── Unit tests ───────────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.room.testing)

    // ── Instrumented tests ───────────────────────────────────────────────
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.workmanager.testing)
    ksp(libs.hilt.android.compiler)
}
```

- [ ] Stage and commit:

```bash
git add app/build.gradle.kts && git commit -m "chore: add app module build file with all dependencies"
```

---

## Task 0.4: AndroidManifest

Create the full `AndroidManifest.xml` for the app module, declaring permissions, the application class, the main activity, and the FileProvider.

- [ ] Create the directory `app/src/main/` if it does not already exist.
- [ ] Create `app/src/main/AndroidManifest.xml` with the following content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Media permissions (Android 12+) -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

    <!-- Biometric -->
    <uses-permission android:name="android.permission.USE_BIOMETRIC" />

    <application
        android:name=".GalleryApp"
        android:allowBackup="true"
        android:enableOnBackInvokedCallback="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Gallery">

        <!-- Main entry point -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- FileProvider for sharing files from internal storage -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>

</manifest>
```

- [ ] Stage and commit:

```bash
git add app/src/main/AndroidManifest.xml && git commit -m "chore: add AndroidManifest with permissions, activity, and FileProvider"
```

---

## Task 0.5: Resources

Create the required resource files: string resources, the Material 3 theme, the FileProvider paths descriptor, and the ProGuard rules.

- [ ] Create the directory `app/src/main/res/values/` if it does not already exist.
- [ ] Create `app/src/main/res/values/strings.xml` with the following content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Gallery</string>
</resources>
```

- [ ] Create `app/src/main/res/values/themes.xml` with the following content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!--
        Base application theme.
        NoActionBar is required because the top bar is provided by Compose Scaffold / TopAppBar.
        DayNight enables automatic light/dark switching based on system setting.
    -->
    <style name="Theme.Gallery" parent="Theme.Material3.DayNight.NoActionBar" />
</resources>
```

- [ ] Create the directory `app/src/main/res/xml/` if it does not already exist.
- [ ] Create `app/src/main/res/xml/file_paths.xml` with the following content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <!--
        Exposes files in the app's internal files-dir subdirectory "trash/"
        via FileProvider so they can be shared outside the app if needed.
    -->
    <files-path
        name="trash"
        path="trash/" />
</paths>
```

- [ ] Create `app/proguard-rules.pro` with the following content:

```pro
# ── Room ─────────────────────────────────────────────────────────────────────
# Keep all @Entity, @Dao, @Database, @TypeConverter annotated classes so Room
# can reflect on them at runtime.
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keep @androidx.room.TypeConverter class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# ── Hilt / Dagger ─────────────────────────────────────────────────────────────
# Hilt generates component and module classes at compile time; keep them.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}

# ── Kotlin serialization / coroutines ─────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ── General Android ───────────────────────────────────────────────────────────
# Preserve Parcelable implementations.
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
```

- [ ] Stage and commit:

```bash
git add app/src/main/res/values/strings.xml \
        app/src/main/res/values/themes.xml \
        app/src/main/res/xml/file_paths.xml \
        app/proguard-rules.pro \
  && git commit -m "chore: add resource files (strings, theme, file_paths) and ProGuard rules"
```

---

## Task 0.6: Generate Gradle Wrapper

Generate the Gradle wrapper scripts and JAR so the project can be built on any machine without a pre-installed Gradle installation.

- [ ] Ensure Gradle 8.7 or later is installed on the machine (run `gradle --version` to check). If Gradle is not installed, install it via SDKMAN (`sdk install gradle 8.7`) or the system package manager before proceeding.
- [ ] From the project root, run the following command to generate the wrapper:

```bash
gradle wrapper --gradle-version 8.7 --distribution-type bin
```

- [ ] Confirm the following files were created:

```
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

- [ ] Open `gradle/wrapper/gradle-wrapper.properties` and verify the `distributionUrl` line reads:

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
```

- [ ] Make `gradlew` executable (required on Linux/macOS; harmless on Windows):

```bash
chmod +x gradlew
```

- [ ] Stage and commit:

```bash
git add gradlew gradlew.bat gradle/wrapper/ && git commit -m "chore: add Gradle 8.7 wrapper"
```

---

## Task 0.7: Verify Build

Run a debug assembly to confirm that the Gradle configuration, version catalog, and all module wiring are valid before any source files are added.

- [ ] Create the minimal Kotlin source stubs required so the app module compiles. Create `app/src/main/java/com/gallery/GalleryApp.kt`:

```kotlin
package com.gallery

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GalleryApp : Application()
```

- [ ] Create `app/src/main/java/com/gallery/MainActivity.kt`:

```kotlin
package com.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlaceholderScreen()
        }
    }
}

@Composable
private fun PlaceholderScreen() {
    Text(text = "Gallery — Phase 0 bootstrap")
}
```

- [ ] From the project root, run the debug assembly:

```bash
./gradlew assembleDebug --stacktrace
```

- [ ] Confirm the final line of output is:

```
BUILD SUCCESSFUL in <time>
```

- [ ] If the build fails, check the error output for the first `> Task :app:...` failure line, address the specific issue (e.g., a missing `version.ref` alias, a typo in the catalog, or a missing source directory), and re-run until `BUILD SUCCESSFUL` is achieved.
- [ ] Stage and commit:

```bash
git add app/src/main/java/com/gallery/GalleryApp.kt \
        app/src/main/java/com/gallery/MainActivity.kt \
  && git commit -m "chore: add bootstrap Application and MainActivity stubs; Phase 0 complete"
```

---

## Phase 0 Completion Checklist

- [ ] `gradle/libs.versions.toml` — all versions and libraries declared
- [ ] `settings.gradle.kts` — project name and `:app` module included
- [ ] Root `build.gradle.kts` — all plugins declared with `apply false`
- [ ] `gradle.properties` — JVM args, AndroidX flag, Kotlin style set
- [ ] `app/build.gradle.kts` — namespace, SDKs, Java 17, Compose, all deps wired
- [ ] `app/src/main/AndroidManifest.xml` — permissions, application class, activity, FileProvider
- [ ] `app/src/main/res/values/strings.xml` — `app_name` defined
- [ ] `app/src/main/res/values/themes.xml` — `Theme.Gallery` defined
- [ ] `app/src/main/res/xml/file_paths.xml` — `trash/` path exposed
- [ ] `app/proguard-rules.pro` — Room and Hilt keep rules in place
- [ ] Gradle wrapper at version 8.7 present and executable
- [ ] `GalleryApp.kt` and `MainActivity.kt` stubs present
- [ ] `./gradlew assembleDebug` exits with `BUILD SUCCESSFUL`
