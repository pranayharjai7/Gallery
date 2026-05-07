# Phase 14: Hidden Album — Gallery App

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Biometric-gated Hidden Album — requires fingerprint/PIN every open, stores hidden media IDs in encrypted Room DB.

**Depends on:** Phase 06, Phase 07, Phase 02 (Room), Phase 01 use cases

---

## Overview

The Hidden Album is a secure, biometric-gated section of the Gallery app. Every time the screen is opened, the user must authenticate via fingerprint, face unlock, or device PIN/password before any content is shown. Hidden media IDs are stored in an encrypted Room database (via `security-crypto`). The screen supports multi-select and "unhide" actions.

Authentication is handled by `BiometricHelper` (a thin wrapper around AndroidX Biometric). The `HiddenAlbumViewModel` drives UI state transitions: Locked → Authenticating → Unlocked (or Error / HardwareUnavailable). The screen itself is responsible for triggering the biometric prompt on first composition.

---

## File Structure

```
app/src/main/kotlin/com/gallery/ui/hidden/
├── BiometricHelper.kt
├── HiddenAlbumViewModel.kt
└── HiddenAlbumScreen.kt

app/src/test/kotlin/com/gallery/ui/hidden/
└── HiddenAlbumViewModelTest.kt
```

---

## Dependencies / Imports

All use cases, shared types, and common UI components referenced in this phase are defined in earlier phases:

- `GetHiddenMediaUseCase` — Phase 01 (uses `HiddenRepository` + `MediaRepository`)
- `RemoveFromHiddenUseCase` — Phase 01
- `AddToHiddenUseCase` — Phase 01
- `HiddenRepository` — Phase 02 (Room + encrypted storage)
- `MediaItem`, `isVideo` extension — Phase 01 shared types
- `MediaThumbnail`, `EmptyState`, `LoadingState` — Phase 07 common UI
- AndroidX Biometric `1.2.0-alpha05` — `androidx.biometric:biometric`
- `FragmentActivity` — required by `BiometricPrompt`

---

## Task 1: BiometricHelper

**File:** `app/src/main/kotlin/com/gallery/ui/hidden/BiometricHelper.kt`

```kotlin
package com.gallery.ui.hidden

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BiometricHelper(private val activity: FragmentActivity) {

    sealed class AuthResult {
        object Success : AuthResult()
        data class Error(val code: Int, val message: String) : AuthResult()
        object NotEnrolled : AuthResult()
        object HardwareUnavailable : AuthResult()
    }

    suspend fun authenticate(): AuthResult = suspendCoroutine { cont ->
        val manager = BiometricManager.from(activity)
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(activity)
                val prompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult
                        ) {
                            cont.resume(AuthResult.Success)
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {
                            cont.resume(AuthResult.Error(errorCode, errString.toString()))
                        }

                        // Called for each failed attempt — not terminal, do not resume
                        override fun onAuthenticationFailed() {}
                    }
                )
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Hidden Album")
                    .setSubtitle("Authenticate to view hidden photos")
                    .setAllowedAuthenticators(authenticators)
                    .build()
                prompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                cont.resume(AuthResult.NotEnrolled)
            else ->
                cont.resume(AuthResult.HardwareUnavailable)
        }
    }
}
```

**No unit test** — `BiometricHelper` requires a live `FragmentActivity` and cannot be meaningfully unit-tested. Validate manually (see manual test plan below).

**Manual test plan:**

| Scenario | Steps | Expected |
|----------|-------|----------|
| Successful fingerprint auth | Open Hidden Album, authenticate with enrolled fingerprint | Screen unlocks and shows hidden media grid |
| Successful PIN fallback | Open Hidden Album, cancel fingerprint, enter PIN | Screen unlocks |
| Cancelled / too many failures | Open Hidden Album, cancel the prompt | Error state shown with descriptive message |
| No biometric enrolled | Use a device/emulator with no fingerprint/PIN configured | `NotEnrolled` error state shown with settings hint |
| Hardware unavailable | Use emulator with biometric disabled | `HardwareUnavailable` state shown |
| Re-lock on back + re-open | Navigate away and return to Hidden Album | Biometric prompt fires again (LaunchedEffect(Unit)) |

- [ ] **Implement** `BiometricHelper.kt` with the code above.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/hidden/BiometricHelper.kt
  git commit -m "feat(hidden): add BiometricHelper — suspendCoroutine wrapper for AndroidX BiometricPrompt"
  ```

---

## Task 2: HiddenAlbumViewModel + Test

### 2a. HiddenAlbumViewModel

**File:** `app/src/main/kotlin/com/gallery/ui/hidden/HiddenAlbumViewModel.kt`

```kotlin
package com.gallery.ui.hidden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.model.MediaItem
import com.gallery.domain.usecase.GetHiddenMediaUseCase
import com.gallery.domain.usecase.RemoveFromHiddenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI State ---

sealed class HiddenAuthState {
    object Locked : HiddenAuthState()
    object Authenticating : HiddenAuthState()
    object Unlocked : HiddenAuthState()
    data class Error(val message: String) : HiddenAuthState()
    object HardwareUnavailable : HiddenAuthState()
}

data class HiddenAlbumUiState(
    val authState: HiddenAuthState = HiddenAuthState.Locked,
    val items: List<MediaItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false
)

// --- ViewModel ---

@HiltViewModel
class HiddenAlbumViewModel @Inject constructor(
    private val getHiddenMedia: GetHiddenMediaUseCase,
    private val removeFromHidden: RemoveFromHiddenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HiddenAlbumUiState())
    val uiState: StateFlow<HiddenAlbumUiState> = _uiState.asStateFlow()

    // Called from screen after successful biometric authentication
    fun onAuthSuccess() {
        _uiState.update { it.copy(authState = HiddenAuthState.Unlocked, isLoading = true) }
        viewModelScope.launch {
            getHiddenMedia().collect { items ->
                _uiState.update { it.copy(items = items, isLoading = false) }
            }
        }
    }

    fun onAuthError(message: String) =
        _uiState.update { it.copy(authState = HiddenAuthState.Error(message)) }

    fun onHardwareUnavailable() =
        _uiState.update { it.copy(authState = HiddenAuthState.HardwareUnavailable) }

    // --- Selection ---

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newIds = if (id in state.selectedIds) state.selectedIds - id
                        else state.selectedIds + id
            state.copy(selectedIds = newIds)
        }
    }

    fun clearSelection() = _uiState.update { it.copy(selectedIds = emptySet()) }

    // --- Actions ---

    fun removeFromHiddenItem(mediaId: Long) {
        viewModelScope.launch { removeFromHidden(mediaId) }
    }

    fun unhideSelected() {
        val ids = _uiState.value.selectedIds
        viewModelScope.launch {
            ids.forEach { removeFromHidden(it) }
            clearSelection()
        }
    }
}
```

### 2b. ViewModel Unit Tests

**File:** `app/src/test/kotlin/com/gallery/ui/hidden/HiddenAlbumViewModelTest.kt`

```kotlin
package com.gallery.ui.hidden

import android.net.Uri
import app.cash.turbine.test
import com.gallery.domain.model.MediaItem
import com.gallery.domain.usecase.GetHiddenMediaUseCase
import com.gallery.domain.usecase.RemoveFromHiddenUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HiddenAlbumViewModelTest {

    private lateinit var getHiddenMedia: GetHiddenMediaUseCase
    private lateinit var removeFromHidden: RemoveFromHiddenUseCase

    @Before
    fun setUp() {
        getHiddenMedia = mockk()
        removeFromHidden = mockk(relaxed = true)
    }

    private fun buildVm() = HiddenAlbumViewModel(getHiddenMedia, removeFromHidden)

    private fun fakeItem(id: Long) = MediaItem(
        id = id,
        uri = Uri.EMPTY,
        name = "hidden_$id.jpg",
        dateTaken = System.currentTimeMillis(),
        size = 1024L,
        width = 1080,
        height = 1920,
        duration = null,
        mimeType = "image/jpeg",
        bucketId = 0L,
        bucketName = "Hidden",
        location = null
    )

    // -----------------------------------------------------------------------
    // Auth state transitions
    // -----------------------------------------------------------------------

    @Test
    fun `initial state is Locked`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())
        val vm = buildVm()
        assertIs<HiddenAuthState.Locked>(vm.uiState.value.authState)
    }

    @Test
    fun `onAuthSuccess transitions to Unlocked and starts loading media`() = runTest {
        val items = listOf(fakeItem(1L), fakeItem(2L))
        every { getHiddenMedia() } returns flowOf(items)

        val vm = buildVm()

        vm.uiState.test {
            awaitItem() // Locked initial state

            vm.onAuthSuccess()

            val loadingState = awaitItem()
            assertIs<HiddenAuthState.Unlocked>(loadingState.authState)
            assertTrue(loadingState.isLoading)

            val loadedState = awaitItem()
            assertIs<HiddenAuthState.Unlocked>(loadedState.authState)
            assertFalse(loadedState.isLoading)
            assertEquals(2, loadedState.items.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAuthError sets Error state with provided message`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())

        val vm = buildVm()
        vm.onAuthError("Authentication cancelled")

        val state = vm.uiState.value
        assertIs<HiddenAuthState.Error>(state.authState)
        assertEquals("Authentication cancelled", (state.authState as HiddenAuthState.Error).message)
    }

    @Test
    fun `onHardwareUnavailable sets HardwareUnavailable state`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())

        val vm = buildVm()
        vm.onHardwareUnavailable()

        assertIs<HiddenAuthState.HardwareUnavailable>(vm.uiState.value.authState)
    }

    // -----------------------------------------------------------------------
    // Selection
    // -----------------------------------------------------------------------

    @Test
    fun `toggleSelection adds id when not selected`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleSelection(10L)

        assertTrue(10L in vm.uiState.value.selectedIds)
    }

    @Test
    fun `toggleSelection removes id when already selected`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleSelection(10L)
        vm.toggleSelection(10L)

        assertFalse(10L in vm.uiState.value.selectedIds)
    }

    @Test
    fun `clearSelection empties selectedIds`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleSelection(1L)
        vm.toggleSelection(2L)
        vm.clearSelection()

        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    @Test
    fun `unhideSelected calls removeFromHidden for each selected id and clears selection`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())

        val vm = buildVm()
        vm.toggleSelection(5L)
        vm.toggleSelection(7L)
        vm.unhideSelected()

        coVerify(exactly = 1) { removeFromHidden(5L) }
        coVerify(exactly = 1) { removeFromHidden(7L) }
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `removeFromHiddenItem delegates to RemoveFromHiddenUseCase`() = runTest {
        every { getHiddenMedia() } returns flowOf(emptyList())

        val vm = buildVm()
        vm.removeFromHiddenItem(42L)

        coVerify { removeFromHidden(42L) }
    }
}
```

- [ ] **Run test: FAIL** — Create the test file first; verify it fails to compile (no production code yet).
- [ ] **Implement** `HiddenAlbumViewModel.kt` with the code above.
- [ ] **Run test: PASS** — All 9 tests green.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/hidden/HiddenAlbumViewModel.kt \
          app/src/test/kotlin/com/gallery/ui/hidden/HiddenAlbumViewModelTest.kt
  git commit -m "feat(hidden): add HiddenAlbumViewModel with auth states, selection, unhide actions and unit tests"
  ```

---

## Task 3: HiddenAlbumScreen

**File:** `app/src/main/kotlin/com/gallery/ui/hidden/HiddenAlbumScreen.kt`

```kotlin
package com.gallery.ui.hidden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.domain.model.isVideo
import com.gallery.ui.common.EmptyState
import com.gallery.ui.common.LoadingState
import com.gallery.ui.common.MediaThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAlbumScreen(
    onBack: () -> Unit,
    onMediaClick: (Long) -> Unit,
    viewModel: HiddenAlbumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as FragmentActivity
    val biometricHelper = remember(activity) { BiometricHelper(activity) }

    // Trigger biometric prompt every time the screen enters composition.
    // LaunchedEffect(Unit) fires once per composition lifecycle — re-navigating
    // back and forward will re-trigger because the composable is removed and
    // re-added to the back stack.
    LaunchedEffect(Unit) {
        when (val result = biometricHelper.authenticate()) {
            is BiometricHelper.AuthResult.Success ->
                viewModel.onAuthSuccess()
            is BiometricHelper.AuthResult.Error ->
                viewModel.onAuthError(result.message)
            BiometricHelper.AuthResult.NotEnrolled ->
                viewModel.onAuthError(
                    "No biometric enrolled. Set up fingerprint or PIN in device settings."
                )
            BiometricHelper.AuthResult.HardwareUnavailable ->
                viewModel.onHardwareUnavailable()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Album") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.selectedIds.isNotEmpty()) {
                        IconButton(onClick = { viewModel.unhideSelected() }) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = "Unhide selected")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val auth = uiState.authState) {
            is HiddenAuthState.Locked,
            is HiddenAuthState.Authenticating -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is HiddenAuthState.Error -> {
                EmptyState(
                    message = auth.message,
                    icon = Icons.Default.Lock,
                    modifier = Modifier.padding(padding)
                )
            }

            is HiddenAuthState.HardwareUnavailable -> {
                EmptyState(
                    message = "Biometric authentication is not available on this device.",
                    icon = Icons.Default.Lock,
                    modifier = Modifier.padding(padding)
                )
            }

            is HiddenAuthState.Unlocked -> {
                when {
                    uiState.isLoading -> {
                        LoadingState(modifier = Modifier.padding(padding))
                    }

                    uiState.items.isEmpty() -> {
                        EmptyState(
                            message = "No hidden photos",
                            icon = Icons.Default.Lock,
                            modifier = Modifier.padding(padding)
                        )
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentPadding = PaddingValues(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(uiState.items, key = { it.id }) { item ->
                                MediaThumbnail(
                                    uri = item.uri,
                                    isVideo = item.isVideo,
                                    duration = item.duration,
                                    isSelected = item.id in uiState.selectedIds,
                                    onClick = {
                                        if (uiState.selectedIds.isNotEmpty()) {
                                            viewModel.toggleSelection(item.id)
                                        } else {
                                            onMediaClick(item.id)
                                        }
                                    },
                                    onLongClick = { viewModel.toggleSelection(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Implement** `HiddenAlbumScreen.kt` with the code above.
- [ ] Verify it compiles against `HiddenAlbumViewModel`, `BiometricHelper`, and all common UI components.
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/hidden/HiddenAlbumScreen.kt
  git commit -m "feat(hidden): add HiddenAlbumScreen — biometric gate, grid, multi-select unhide"
  ```

---

## Task 4: Wire into NavGraph

**File:** `app/src/main/kotlin/com/gallery/ui/navigation/GalleryNavGraph.kt` (existing)

Add the `hiddenAlbum` composable destination inside `NavHost { ... }`:

```kotlin
composable(Screen.HiddenAlbum.route) {
    HiddenAlbumScreen(
        onBack = { navController.popBackStack() },
        onMediaClick = { mediaId ->
            navController.navigate(Screen.Viewer.createRoute(mediaId))
        }
    )
}
```

Add `Screen.HiddenAlbum` to the sealed class / object in your `Screen` definitions file:

```kotlin
object HiddenAlbum : Screen("hidden_album")
```

The Hidden Album is reachable from the Albums screen (e.g., a special "Hidden" album tile) — not from the bottom nav bar, since it should not be discoverable at a glance.

- [ ] Open `GalleryNavGraph.kt` and add the route above.
- [ ] Add `Screen.HiddenAlbum` to the `Screen` sealed class.
- [ ] Add a "Hidden" album entry in the Albums screen that navigates to `Screen.HiddenAlbum.route` (can be a simple locked tile at the bottom of the album list).
- [ ] **Git commit:**
  ```
  git add app/src/main/kotlin/com/gallery/ui/navigation/GalleryNavGraph.kt
  git commit -m "feat(navigation): register HiddenAlbum route; add hidden album tile to Albums screen"
  ```

---

## Run Full Phase 14 Test Suite

```
./gradlew :app:testDebugUnitTest --tests "com.gallery.ui.hidden.*"
```

Expected: all `HiddenAlbumViewModelTest` tests pass. `BiometricHelper` and `HiddenAlbumScreen` are not unit-testable — validate with manual test plan in Task 1.

---

## Acceptance Criteria

- [ ] `BiometricHelper.authenticate()` is a `suspend` function that resumes exactly once per call
- [ ] `HiddenAuthState` has all five states: `Locked`, `Authenticating`, `Unlocked`, `Error`, `HardwareUnavailable`
- [ ] Biometric prompt fires every time `HiddenAlbumScreen` enters composition (via `LaunchedEffect(Unit)`)
- [ ] `HiddenAlbumScreen` shows `CircularProgressIndicator` while `Locked` or `Authenticating`
- [ ] `HiddenAlbumScreen` shows `EmptyState` with lock icon on `Error` or `HardwareUnavailable`
- [ ] `HiddenAlbumScreen` shows media grid only when `Unlocked`
- [ ] Long-press activates multi-select; unhide icon appears in `TopAppBar` when items are selected
- [ ] Tapping the unhide icon calls `unhideSelected()`, which calls `removeFromHidden` for each selected ID
- [ ] Tapping a media tile when not in selection mode calls `onMediaClick`
- [ ] Tapping a media tile when in selection mode toggles selection
- [ ] `HiddenAlbum` route is reachable from the Albums screen
- [ ] Navigating away and back re-triggers the biometric prompt (no cached unlock state)
- [ ] All ViewModel tests pass

---

## Summary

| Task | Production file(s) | Test file(s) | Tests |
|------|--------------------|--------------|-------|
| 1 | `BiometricHelper.kt` | _(manual tests only)_ | — |
| 2 | `HiddenAlbumViewModel.kt` | `HiddenAlbumViewModelTest.kt` | 9 |
| 3 | `HiddenAlbumScreen.kt` | _(UI tests — later phase)_ | — |
| 4 | `GalleryNavGraph.kt` (modified) | — | — |
