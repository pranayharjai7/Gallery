# Phase 19: Permissions & Error States — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Request `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` at launch, show empty states with grant-access CTAs when denied, and handle all error/corrupt-media states across the app.

**Depends on:** Phase 08 (PhotosScreen), Phase 09 (AlbumsScreen), Phase 11 (SearchScreen — all screens need the permission guard)

---

## Overview

On Android 13+ media permissions are split: `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` must be requested separately from the legacy `READ_EXTERNAL_STORAGE`. This phase:

1. Wraps the entire app in a `MediaPermissionsWrapper` composable (Accompanist Permissions) that handles the full grant/rationale/denied lifecycle.
2. Ensures the app `AndroidManifest.xml` declares all required permissions with correct `maxSdkVersion` guards.
3. Adds graceful error rendering to `MediaThumbnail` for corrupt or unreadable media files.
4. Verifies the complete build via a final checklist.

---

## File Structure

```
app/src/main/kotlin/com/gallery/ui/common/
└── PermissionsScreen.kt          (new — MediaPermissionsWrapper + helper screens)

app/src/main/kotlin/com/gallery/
└── MainActivity.kt               (update — wrap GalleryScaffold with MediaPermissionsWrapper)

app/src/main/kotlin/com/gallery/ui/common/
└── MediaThumbnail.kt             (update — add error drawable to AsyncImage)

app/src/main/res/drawable/
└── ic_broken_image.xml           (new — vector drawable)

app/src/main/AndroidManifest.xml  (update — add permission declarations)
```

---

## Dependencies to Verify in `build.gradle.kts`

```kotlin
// Accompanist Permissions
implementation("com.google.accompanist:accompanist-permissions:0.36.0")

// Already present — Coil for AsyncImage error placeholder
implementation("io.coil-kt.coil3:coil-compose:<version>")

// Activity Compose (for LocalContext / Intent)
implementation("androidx.activity:activity-compose:<version>")
```

---

## Task 1: AndroidManifest.xml — Permission Declarations

**File:** `app/src/main/AndroidManifest.xml`

Add the following `<uses-permission>` elements inside the `<manifest>` root, before the `<application>` tag:

```xml
<!-- Android 13+ granular media permissions -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

<!-- Android 12 and below — legacy storage permission -->
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />

<!-- Required for MediaStore IS_PENDING write operations -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

**Notes:**
- `READ_EXTERNAL_STORAGE` with `maxSdkVersion="32"` ensures the permission is only requested on Android 12L and below; on Android 13+ the granular permissions take precedence.
- `WRITE_EXTERNAL_STORAGE` is only needed on Android 9 (API 28) and below; from API 29 onward scoped storage removes the requirement.
- No changes to `targetSdkVersion`; it should already be 34+ to match Kotlin 2.0 / Material 3 requirements.

---

## Task 2: PermissionsScreen — MediaPermissionsWrapper

**File:** `app/src/main/kotlin/com/gallery/ui/common/PermissionsScreen.kt`

Three composable functions are defined in this file:

- `MediaPermissionsWrapper` — the public entry point; branches on permission state
- `PermissionRationaleScreen` — shown when the system allows us to explain why we need the permission (i.e., the user denied once but has not checked "Don't ask again")
- `PermissionDeniedScreen` — shown when the permission is permanently denied; deep-links to the app's Settings page

```kotlin
package com.gallery.ui.common

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import android.Manifest

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MediaPermissionsWrapper(content: @Composable () -> Unit) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    )

    when {
        // All permissions granted — show the real app content
        permissionsState.allPermissionsGranted -> content()

        // At least one permission was denied but we can show a rationale
        permissionsState.shouldShowRationale ->
            PermissionRationaleScreen(
                onGrant = { permissionsState.launchMultiplePermissionRequest() }
            )

        // First launch or permanent denial — request immediately then
        // fall back to the denied screen
        else -> {
            LaunchedEffect(Unit) {
                permissionsState.launchMultiplePermissionRequest()
            }
            PermissionDeniedScreen()
        }
    }
}

@Composable
private fun PermissionRationaleScreen(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Permission Required",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Gallery needs access to your photos and videos to display them.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant) {
            Text("Grant Access")
        }
    }
}

@Composable
private fun PermissionDeniedScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Access Denied",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Open Settings to grant photo and video access.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
            context.startActivity(intent)
        }) {
            Text("Open Settings")
        }
    }
}
```

**State machine summary:**

| Accompanist state | `allPermissionsGranted` | `shouldShowRationale` | Behavior |
|---|---|---|---|
| First launch | `false` | `false` | `LaunchedEffect` fires the system dialog; `PermissionDeniedScreen` shown behind it |
| After deny (soft) | `false` | `true` | `PermissionRationaleScreen` with "Grant Access" button |
| After deny + "Don't ask again" | `false` | `false` | `PermissionDeniedScreen` with Settings deep-link |
| Granted | `true` | — | App content rendered normally |

**Android version compatibility:**
- On Android 12 and below, `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` are unknown to the system, so `rememberMultiplePermissionsState` treats them as automatically granted. No special branching is needed; the `READ_EXTERNAL_STORAGE` permission declared in the manifest (see Task 1) covers those API levels via the system's automatic upgrade.

---

## Task 3: Wire Permissions into MainActivity

**File:** `app/src/main/kotlin/com/gallery/MainActivity.kt`

Wrap the existing `GalleryTheme { ... }` content block with `MediaPermissionsWrapper`:

```kotlin
// Before (Phase 05):
setContent {
    GalleryTheme {
        GalleryScaffold()   // or NavGraph / AppNavigation composable
    }
}

// After (Phase 19):
setContent {
    GalleryTheme {
        MediaPermissionsWrapper {
            GalleryScaffold()   // or NavGraph / AppNavigation composable
        }
    }
}
```

`MediaPermissionsWrapper` must be inside `GalleryTheme` so that `MaterialTheme.colorScheme` is available to the rationale/denied screens.

No other changes to `MainActivity` are required.

---

## Task 4: Corrupt Media Error Handling in MediaThumbnail

### 4a. Broken-image vector drawable

**File:** `app/src/main/res/drawable/ic_broken_image.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
  <path
      android:fillColor="@android:color/transparent"
      android:strokeColor="#FF000000"
      android:strokeWidth="0"/>
  <path
      android:fillColor="#FF757575"
      android:pathData="M21,5v6.59l-3,-3.01 -4,4.01 -4,-4 -4,4 -3,-3.01L3,5c0,-1.1 0.9,-2 2,-2h14c1.1,0 2,0.9 2,2zM18,11.42l3,3.01L21,19c0,1.1 -0.9,2 -2,2L5,21c-1.1,0 -2,-0.9 -2,-2v-6.58l3,2.99 4,-4 4,4 4,-3.99z"/>
</vector>
```

### 4b. Update MediaThumbnail.kt

**File:** `app/src/main/kotlin/com/gallery/ui/common/MediaThumbnail.kt`

Replace the plain `AsyncImage` call with a `Coil` `ImageRequest` that specifies the `error` drawable and an `onError` callback for logging:

```kotlin
// Add import:
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.gallery.R

// Replace existing AsyncImage inside MediaThumbnail with:
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(uri)
        .crossfade(true)
        .error(R.drawable.ic_broken_image)
        .build(),
    contentDescription = contentDescription,
    contentScale = ContentScale.Crop,
    onError = { state ->
        // Log the failure; telemetry / crash reporting can be added here
        android.util.Log.w("MediaThumbnail", "Failed to load media: $uri — ${state.result.throwable.message}")
    },
    modifier = modifier
)
```

The `ic_broken_image` drawable will be rendered at the thumbnail's normal size, maintaining grid layout integrity even when individual items cannot be decoded.

---

## Task 5: Final Build Verification

Run the following commands in order and confirm each passes before proceeding to the next.

### 5a. Unit tests

```bash
./gradlew :app:testDebugUnitTest
```

Expected output: `BUILD SUCCESSFUL` with `0 tests failed`.

Key test classes that must all pass:
- `com.gallery.ui.editor.video.VideoEditViewModelTest` (Phase 17)
- `com.gallery.ui.photos.PhotosViewModelTest` (Phase 08)
- `com.gallery.ui.viewer.ViewerViewModelTest` (Phase 10)
- All DAO tests in `com.gallery.data.*`

### 5b. Debug APK build

```bash
./gradlew :app:assembleDebug
```

Expected output: `BUILD SUCCESSFUL`. The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

### 5c. Instrumented tests (requires emulator or device)

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected output: `BUILD SUCCESSFUL`. All Room DAO tests and any UI instrumented tests pass.

### 5d. Install on device / emulator

```bash
./gradlew :app:installDebug
```

### 5e. Manual test checklist

Run through the following scenarios on an Android 13+ device (or API 33 emulator) and a separate API 29 emulator:

**Permissions flow:**
- [ ] Fresh install: system permission dialog appears immediately on first launch
- [ ] Deny once: rationale screen appears with "Grant Access" button on next launch
- [ ] Tap "Grant Access": system dialog re-appears
- [ ] Deny with "Don't ask again": denied screen appears with "Open Settings" button
- [ ] Tap "Open Settings": device Settings opens on the app's permission page
- [ ] Grant permission from Settings, return to app: Photos grid loads

**Core navigation:**
- [ ] Photos tab loads media grid with date headers
- [ ] Tap a photo opens the full-screen viewer
- [ ] Swipe left/right between items in the viewer
- [ ] Pinch-to-zoom works on photos (up to 5×)
- [ ] Tap a video item — ExoPlayer controls appear and video plays
- [ ] Bottom nav switches between Photos, Albums, Search tabs

**Editing & actions:**
- [ ] Tap Edit on a photo → photo editor opens (Phase 12/13)
- [ ] Tap Edit on a video → video editor opens; trim/mute/rotate; tap Save; new file appears in gallery
- [ ] Trash an item from the viewer; confirm it moves to the trash album
- [ ] Restore the item from the trash album
- [ ] Hidden album requires biometric authentication before showing contents

**Memories:**
- [ ] "On This Day" card appears at top of Photos grid when memories exist
- [ ] Dismiss button hides the card for the session
- [ ] Tapping the card opens the slideshow; images auto-advance every 4 seconds

**Error states:**
- [ ] A deliberately corrupt image file (renamed non-image as `.jpg`) shows the broken-image icon in the grid

---

## Final Commit

```bash
git add -p   # stage all changes carefully
git commit -m "feat: complete Gallery app implementation"
```

---

## Acceptance Criteria

- [ ] `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` are declared in `AndroidManifest.xml` with appropriate `maxSdkVersion` guards for the legacy `READ_EXTERNAL_STORAGE`
- [ ] On first launch the system permission dialog is presented without any extra tap required
- [ ] Rationale screen shown on soft-deny with a working "Grant Access" button
- [ ] Denied screen shown on hard-deny with a working "Open Settings" deep-link
- [ ] App content is completely hidden behind the permission gate — no media is accessible without a grant
- [ ] Corrupt or unloadable media items show the broken-image icon placeholder instead of a blank space
- [ ] `./gradlew assembleDebug` completes with `BUILD SUCCESSFUL`
- [ ] `./gradlew testDebugUnitTest` completes with 0 test failures
- [ ] `./gradlew connectedDebugAndroidTest` completes with 0 test failures on a running emulator
