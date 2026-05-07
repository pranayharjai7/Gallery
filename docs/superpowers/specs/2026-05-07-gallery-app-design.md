# Gallery App — Design Spec

**Date:** 2026-05-07  
**Platform:** Android 12+ (min SDK 31, target SDK 35)  
**Scope:** Local-only gallery for photos and videos, with editing and biometric-protected hidden album  

---

## 1. Tech Stack & Architecture

| Concern | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 (Material You) |
| Architecture | MVVM + Clean Architecture (UI → Domain → Data) |
| Navigation | Jetpack Navigation Compose |
| Media access | MediaStore API + Photo Picker |
| Image loading | Coil 3 (Compose-native, video thumbnail support) |
| Video playback | Media3 / ExoPlayer |
| Dependency injection | Hilt |
| Async | Kotlin Coroutines + Flow |
| Local persistence | Room (Trash, Hidden, Favorites) |
| Permissions | Accompanist Permissions |
| Biometrics | AndroidX Biometric |
| Background work | WorkManager (trash auto-purge) |
| Build | Gradle Kotlin DSL |

**Min SDK:** 31 (Android 12)  
**Target SDK:** 35  

### Layer breakdown

- **UI layer:** Compose screens + ViewModels. Each screen has one ViewModel exposing a `UiState` sealed class via `StateFlow`.
- **Domain layer:** Use cases (one action per class) that orchestrate repository calls. Pure Kotlin, no Android dependencies.
- **Data layer:** Repositories backed by MediaStore (`ContentResolver`) and Room. Expose `Flow` to domain.

---

## 2. Screens & Navigation

### Bottom Navigation (4 tabs)

| Tab | Screen | Icon |
|---|---|---|
| Photos | `PhotosScreen` | Image |
| Albums | `AlbumsScreen` | Grid |
| Search | `SearchScreen` | Magnifier |
| More | `MoreScreen` | Menu |

### Full screen list

| Screen | Route | Description |
|---|---|---|
| `PhotosScreen` | `photos` | Staggered timeline grid grouped by date |
| `AlbumsScreen` | `albums` | 3-col compact grid, Smart + My Albums sections |
| `SearchScreen` | `search` | Search bar + filter chips, results grid |
| `MoreScreen` | `more` | Links to Settings, Trash, Hidden Album |
| `ViewerScreen` | `viewer/{mediaId}` | Immersive full-screen viewer |
| `AlbumDetailScreen` | `album/{albumId}` | Grid of media inside an album |
| `PhotoEditScreen` | `edit/photo/{mediaId}` | Photo editor |
| `VideoEditScreen` | `edit/video/{mediaId}` | Video editor |
| `TrashScreen` | `trash` | Recently deleted, 30-day auto-purge |
| `HiddenAlbumScreen` | `hidden` | Biometric-gated hidden media |
| `SettingsScreen` | `settings` | App preferences |

### Navigation rules

- Bottom nav handles the 4 root destinations; back stack is managed per-tab
- Tapping any media item navigates to `ViewerScreen` with a shared element transition on the thumbnail
- Long-pressing any item enters multi-select mode; top bar switches to contextual action bar
- Predictive back gesture supported on all screens (Android 13+ API)

---

## 3. Data Layer

### Media access

All media metadata is sourced directly from `MediaStore` via `ContentResolver`. No local mirror database for the main library — queries are reactive via `Flow` using `ContentObserver`.

**Columns queried:** `_ID`, `DISPLAY_NAME`, `DATE_TAKEN`, `SIZE`, `WIDTH`, `HEIGHT`, `DURATION`, `MIME_TYPE`, `BUCKET_ID`, `BUCKET_DISPLAY_NAME`, `LATITUDE`, `LONGITUDE`

### Domain models

```kotlin
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateTaken: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val duration: Long?,       // null for photos
    val mimeType: String,
    val bucketId: Long,
    val bucketName: String,
    val location: LatLng?
)

data class Album(
    val id: String,
    val name: String,
    val coverUri: Uri,
    val count: Int,
    val type: AlbumType        // SMART | CUSTOM
)
```

### Repositories

| Repository | Backing store | Purpose |
|---|---|---|
| `MediaRepository` | MediaStore | Query all photos/videos, watch for changes |
| `AlbumRepository` | MediaStore | Group by bucket, build smart albums |
| `TrashRepository` | Room + private app dir | Soft-delete, restore, auto-purge |
| `HiddenRepository` | Encrypted Room DB | Store hidden item IDs, exclude from all other queries |
| `FavoritesRepository` | Room | Store favorited item IDs |

### Smart Albums

Built automatically from MediaStore bucket names + MIME types:

- **Camera** — `DCIM/Camera` bucket
- **Screenshots** — `Pictures/Screenshots` bucket
- **Downloads** — `Download` bucket
- **Videos** — MIME type `video/*`
- **Slow Mo** — bucket name contains "slow"
- **WhatsApp** — bucket name contains "WhatsApp"
- **Favorites** — IDs in FavoritesRepository
- **Hidden** — IDs in HiddenRepository (shown only in `HiddenAlbumScreen`)

### Editing (non-destructive)

Edits always produce a **new copy**. The original file is never modified. New file is written via `MediaStore.createWriteRequest()` (Android 11+ API, always available on our min SDK 31).

---

## 4. Key Features

### 4.1 Staggered Grid (OnePlus-style)

- Default: 3-column uniform grid
- Every ~8 items, one media item is promoted to a featured large tile spanning 2 columns
- Promoted positions are pre-computed in the ViewModel (`StaggeredGridState`) so layout is stable across recompositions and config changes
- Video items show a play icon overlay and duration badge (e.g. `0:42`)
- Date headers separate groups: `TODAY`, `YESTERDAY`, `MAY 2025`, etc.
- Grid density is user-configurable: Compact (4-col) / Normal (3-col) / Large (2-col)

### 4.2 Photo Viewer

- Full-screen `HorizontalPager` with black background
- Tap anywhere → toggle top + bottom overlays with fade animation
- **Top overlay:** back button · item counter (12 / 47) · overflow menu (Set as wallpaper, Info, Move to album, Use as contact photo)
- **Bottom overlay:** action row (Share, Edit, Favorite, Delete) + scrollable horizontal thumbnail strip; current item highlighted with `PrimaryContainer` border
- Pinch-to-zoom via `TransformableState` (max 5×)
- Shared element transition from grid thumbnail on open/close
- Video items: full ExoPlayer controls (play/pause, seek bar, volume, fullscreen toggle)

### 4.3 Photo Editor

Tabs: **Crop · Adjust · Filters · Draw**

| Tab | Controls |
|---|---|
| Crop | Free-form handles · aspect ratio presets (1:1, 4:3, 16:9, 9:16) · flip H/V · rotate 90° |
| Adjust | Brightness · Contrast · Saturation · Warmth · Exposure — each a `Slider` from -100 to +100 |
| Filters | Horizontal scrollable strip of preset LUT-based filters with live preview thumbnails |
| Draw | Color picker · brush size slider · eraser |

Save action: writes edited bitmap as new file via `MediaStore.createWriteRequest()`, adds it to the same album as the original.

### 4.4 Video Editor

- Media3 preview player with frame-accurate seek
- Trim handles (drag start/end) with thumbnail filmstrip scrubber
- Mute audio toggle
- Rotate 90° (clockwise)
- Transcodes via `MediaTransformer` (Media3), saves as new copy

### 4.5 Hidden Album

- First access: user must enroll biometric (or device PIN fallback)
- Hidden item IDs stored in Room DB encrypted with `EncryptedSharedPreferences`-managed key (Jetpack Security)
- All `MediaRepository` queries exclude hidden IDs via `NOT IN (...)` filter
- `BiometricPrompt` shown every time `HiddenAlbumScreen` is opened — no persistent unlocked state
- Fallback: device credential (PIN / pattern / password) if biometric not enrolled
- If biometric hardware unavailable: show info dialog, lock feature

### 4.6 Trash

- Soft-delete: original file moved to `filesDir/trash/`, entry added to Room `TrashEntity(id, originalUri, deletedAt, originalBucketId)`
- Trash screen shows media with days-remaining badge ("28 days left")
- **Restore:** file moved back, re-inserted into MediaStore via `MediaStore.createWriteRequest()`
- **Permanent delete:** file deleted from `filesDir/trash/` and Room entry removed
- **Auto-purge:** `WorkManager` `PeriodicWorkRequest` runs daily, permanently deletes items where `deletedAt < now - 30 days`

### 4.7 Multi-select

- Long-press any item → selection mode
- Top bar replaced by contextual bar: "N selected" · Select All · Share · Move to Album · Add to Album · Delete
- Drag finger across grid for batch selection
- Tap outside selection or press back to exit selection mode

### 4.8 "On This Day" Memories

- Card shown at top of Photos timeline (dismissible per-day)
- Queries MediaStore for items with `DATE_TAKEN` on same calendar day (any prior year)
- Tapping opens a full-screen auto-advancing slideshow viewer
- Card not shown if fewer than 3 matching items

---

## 5. Theming, Permissions & Settings

### Material You Theming

- `DynamicColorScheme` applied on Android 12+ — pulls colors from current wallpaper
- Fallback: purple/violet baseline Material 3 palette (not reached since min SDK 31, but defensive)
- Light/Dark follows system by default; user override persisted in `DataStore`
- `MaterialTheme` wraps the entire app at `MainActivity` level

### Permissions

| Permission | When requested | Denied behavior |
|---|---|---|
| `READ_MEDIA_IMAGES` | App first launch | Empty state + "Grant Permission" CTA |
| `READ_MEDIA_VIDEO` | App first launch | Same as above |
| `USE_BIOMETRIC` | First Hidden Album access | Biometric feature disabled, info dialog shown |

No `WRITE_EXTERNAL_STORAGE` needed — all writes go through `MediaStore.createWriteRequest()`.

### Settings

| Setting | Options | Default |
|---|---|---|
| Theme | Light / Dark / System | System |
| Dynamic color | On / Off | On |
| Grid size | Compact (4-col) / Normal (3-col) / Large (2-col) | Normal |
| Slideshow interval | 2s / 4s / 6s | 4s |
| Hidden album | Enable/Disable · Reset biometric | Enabled |
| About | App version, open-source licenses | — |

### Error states

| State | Behavior |
|---|---|
| Permission denied | Illustrated empty state screen with "Grant Access" button |
| Empty album | Illustrated empty state with album name |
| Corrupt/unreadable media | Placeholder tile with broken-image icon; no crash |
| Biometric unavailable | Fallback to device credential; if neither available, feature locked with info dialog |
| Trash restore conflict | Toast: "Could not restore — file may have been deleted externally" |

---

## 6. Out of Scope

- Cloud sync or backup
- AI-based features (face grouping, scene detection)
- Sharing to social media (handled by system share sheet)
- Widget
