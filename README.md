# Gallery

A modern, feature-rich Android gallery app built with Jetpack Compose and Clean Architecture. Browse, organize, edit, and protect your photos and videos — all on-device, with no cloud required.

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%2012%2B-3DDC84?logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-2024.09-4285F4?logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Architecture-Clean%20MVVM-orange" />
  <img src="https://img.shields.io/badge/License-MIT-blue" />
</p>

---

## Features

### Media Browsing
- Grid view of all photos and videos, grouped by date
- Mosaic layout with featured tiles for a dynamic, magazine-style feed
- Configurable grid density (Small / Medium / Large)
- Full-screen viewer with pinch-to-zoom, pan gestures, and horizontal swipe between items
- Thumbnail strip at the bottom for quick navigation

### Albums
- **Smart Albums** — automatically populated: Camera, Screenshots, Downloads, Videos, Slow Motion, WhatsApp
- **Custom Albums** — your own folder-based collections

### Search
- Real-time search with 300 ms debounce
- Filter chips to narrow results by type
- Results exclude hidden and trashed items

### Photo Editor
| Tab | Tools |
|-----|-------|
| **Crop** | Rotate, flip (H/V), aspect ratio presets (Free, 1:1, 4:3, 16:9, 9:16) |
| **Adjust** | Brightness, Contrast, Saturation, Warmth, Exposure |
| **Filters** | Pre-made one-tap filters |
| **Draw** | Freehand annotation with color picker and brush size |

### Video Editor
- Trim and basic video editing powered by Media3 Transformer

### Trash
- Soft-delete: items are recoverable for 30 days
- Per-item restore or bulk restore
- Manual purge or automatic expiry via WorkManager
- System-level delete dialog (Android 12+ `MediaStore.createDeleteRequest`)

### Hidden Album
- Biometric-protected (fingerprint / face recognition)
- Hidden items are excluded from all other views and search

### Memories
- *On This Day* slideshow — auto-rotating (configurable interval)

### Favorites
- One-tap favorite toggle from the viewer
- Dedicated favorites view

### Settings
- Theme: Light / Dark / System default
- Dynamic color (Material You)
- Grid size
- Slideshow interval
- Clear cache / reset preferences

---

## Architecture

The project follows **Clean Architecture** with an **MVVM** presentation layer.

```
com.pranayharjai7.gallery/
├── ui/          # Screens, ViewModels, Compose components
├── domain/      # Use cases, repository interfaces, domain models
├── data/        # MediaStore, Room, DataStore, WorkManager
└── di/          # Hilt modules
```

Data flows **upward** through use cases and **downward** via `StateFlow` / `Flow`:

```
MediaStore / Room / DataStore
        ↓
   Repository (impl)
        ↓
    Use Case
        ↓
   ViewModel  →  UI State (StateFlow)
        ↓
   Compose Screen
```

---

## Tech Stack

| Layer | Library | Version |
|-------|---------|---------|
| Language | Kotlin | 2.0.0 |
| UI | Jetpack Compose BOM | 2024.09.00 |
| UI | Material 3 | — |
| Navigation | Navigation Compose | 2.8.0 |
| DI | Hilt | 2.51.1 |
| Database | Room | 2.6.1 |
| Preferences | DataStore | 1.1.1 |
| Image loading | Coil | 3.0.0-rc02 |
| Video playback | Media3 ExoPlayer | 1.4.0 |
| Video editing | Media3 Transformer | 1.4.0 |
| Background jobs | WorkManager | 2.9.1 |
| Biometrics | Biometric KTX | 1.2.0-alpha05 |
| Coroutines | Kotlinx Coroutines | 1.8.1 |
| Permissions | Accompanist Permissions | 0.36.0 |
| Unit testing | JUnit + MockK + Turbine | 4.13.2 / 1.13.12 / 1.1.0 |
| UI testing | Espresso + Compose Test | 3.6.1 |

**Build tooling:** Gradle 9.0.0 · AGP 8.4.0 · KSP · JVM 17

---

## Requirements

| Requirement | Value |
|-------------|-------|
| Android version | 12 (API 31) or higher |
| Compile SDK | 35 (Android 15) |

### Permissions

| Permission | Purpose |
|------------|---------|
| `READ_MEDIA_IMAGES` | Access photos |
| `READ_MEDIA_VIDEO` | Access videos |
| `READ_MEDIA_VISUAL_USER_SELECTED` | Partial media access (Android 14+) |
| `ACCESS_MEDIA_LOCATION` | EXIF location metadata |
| `USE_BIOMETRIC` | Hidden album authentication |

---

## Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35

### Build

```bash
git clone https://github.com/pranayharjai7/Gallery.git
cd Gallery
./gradlew assembleDebug
```

### Run tests

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests (device/emulator required)
./gradlew connectedDebugAndroidTest
```

---

## Project Structure

```
app/src/
├── main/java/com/pranayharjai7/gallery/
│   ├── ui/
│   │   ├── photos/        # Main grid, mosaic layout, date grouping
│   │   ├── albums/        # Smart + custom albums
│   │   ├── search/        # Debounced search with filter chips
│   │   ├── viewer/        # Full-screen viewer with pager
│   │   ├── editor/
│   │   │   ├── photo/     # Crop / Adjust / Filters / Draw
│   │   │   └── video/     # Video trim editor
│   │   ├── trash/         # 30-day soft-delete bin
│   │   ├── hidden/        # Biometric-locked album
│   │   ├── memories/      # On This Day slideshow
│   │   ├── settings/      # Theme, grid, preferences
│   │   ├── common/        # Shared Compose components
│   │   ├── theme/         # MaterialTheme, color schemes
│   │   └── navigation/    # NavHost + routes
│   ├── domain/
│   │   ├── model/         # MediaItem, Album, TrashItem, SmartAlbum
│   │   ├── repository/    # Interfaces
│   │   └── usecase/       # 17 focused use cases
│   ├── data/
│   │   ├── mediastore/    # MediaStore queries
│   │   ├── room/          # Favorite, Hidden, Trash DAOs + entities
│   │   ├── prefs/         # ThemeMode, GridSize, UserPreferences
│   │   └── work/          # Trash expiry WorkManager tasks
│   └── di/                # Hilt modules
├── test/                  # Unit tests (ViewModels, Use Cases)
└── androidTest/           # Instrumented tests (Navigation, Hilt)
```

---

## License

```
MIT License

Copyright (c) 2024 Pranay Harjai

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```