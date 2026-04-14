# Differences Between Original Binary and C++ Decompilation

This document compares the architecture documented in `ARCHITECTURE.md` (from binary analysis)
against the C++ reconstruction in `decompilation-src/` (described in `CPP-ARCHITECTURE.md`).
Items marked **MISSING** represent gaps to address in the next iteration.

---

## 1. WinMain Initialization Sequence

| Step | Original (`ARCHITECTURE.md`) | C++ (`main.cpp`) | Status |
|------|------------------------------|-----------------|--------|
| FPU config | `_control87(0x20000, 0x30000)` | Same | OK |
| System params | `SaveOrRestoreSystemParameters(false)` | Same | OK |
| Cmdline copy | strncpy to two 512-byte buffers | Same | OK |
| CLI parser | `CLI_CommandParser_ParseArgs()` before single-instance check | TODO comment present, call absent | **MISSING** call |
| Single instance | `FindWindowA` → TerminateProcess | Same | OK |
| Window class | `RegisterWindowClass()` | Same | OK |
| Registry load | `LoadGameSettings()` | Same | OK |
| Cmdline parse | `fullscreen`, `widescreen` | Same | OK |
| Window creation | `CreateGameWindow(hInstance, width, height)` | Same signature | OK |
| Get client rect | `GetClientRect(hWnd, &rect)` for exact client height | `GetClientRect(ghWnd, &clientRect)` | OK |
| Engine factory | Creates `DAT_00bef6d0` (2904-byte engine object) via callback manager factory | `g_pComObject` declared, never created | **MISSING** factory call |
| PreDirectXInit | `PreDirectXInit()` | Called (empty stub) | OK (stub) |
| DirectX init | `InitDirectXAndSubsystems(clientHeight)` | Called (empty stub) | OK (stub) |
| Subsystems init | `InitGameSubsystems()` + `DAT_00bef6c6 = 1` | Called (empty stub) + `g_bSubsysInitialized = true` | OK (stub) |
| Window restore | `ShowWindow(SW_MAXIMIZE/MINIMIZE/nShowCmd)` after init; then `UpdateWindow` | Same order | OK |
| Main loop | `MainLoop()` | Same | OK |
| Render teardown | `RenderAndAudioTeardown()` (`00ec6610`) | TODO comment | **MISSING** |
| Engine obj release | `(**(callback_mgr + 0xc))(DAT_00bef6d0, 0)` via callback system | TODO comment, `g_pComObject = NULL` | **MISSING** |
| Save options | `SaveOptionsOnExit()` (`00eb4a5d`) | Called (empty stub) | OK (stub) |
| Input pause | Pause via RealInputSystem vtable | TODO comment | **MISSING** |
| Unacquire | `UnacquireInputDevices()` | Same | OK |
| Show cursor | Loop until ≥ 1 | Same | OK |
| Focus flag | `DAT_00bef6c7 = 1` | `g_bHasFocusLost = true` | OK |
| Cursor/scene | `UpdateCursorVisibilityAndScene()` | Called (empty stub) | OK (stub) |
| Param restore | `SaveOrRestoreSystemParameters(true)` | Same | OK |
| Hard exit | `TerminateProcess(hProc, 1)` | Same | OK |

---

## 2. Registry System

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| HKCU → HKLM fallback | Yes | Yes | OK |
| HKCU write-back on HKLM hit | Calls `FUN_0060cc70` to write value back to HKCU | Implemented inline | OK |
| Auto-create in HKCU | Yes | Yes | OK |
| `std::basic_string` internal use | Uses C++ string objects as parameters internally | Plain C strings | DIFFERS (implementation detail) |

---

## 3. System Parameter Management

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| Storage layout | `mouseSpeed[2]`, `mouseAccel[2]`, `screenReader[6]` | Same struct layout | OK |
| SPI get/set codes | 0x3a/3b, 0x34/35, 0x32/33 | Same constants via globals.h | OK |
| Bit check | bit 0 clear = acceleration active | Same | OK |
| Mask | `0xFFFFFFF3` | `MOUSE_ACCEL_FLAGS_MASK = 0xFFFFFFF3` | OK |

---

## 4. Window Creation

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| Signature | `CreateGameWindow(hInstance, width, height)` | Same | OK |
| Fullscreen style | `WS_POPUP` | Same | OK |
| Windowed style | `WS_OVERLAPPEDWINDOW` | Same | OK |
| TOPMOST | `HWND_TOPMOST` in fullscreen | Same | OK |
| SetMenu | `SetMenu(hWnd, NULL)` in fullscreen | Same | OK |
| SetThreadExecutionState | `ES_CONTINUOUS \| ES_DISPLAY_REQUIRED` | Same | OK |
| ShowWindow sequence | `ShowWindow(hWnd, 0)` then `SetWindowPos` | Same | OK |
| Class style | Fullscreen: `CS_OWNDC \| CS_DBLCLKS`; windowed: adds `CS_VREDRAW \| CS_HREDRAW` | Always uses all four flags | MINOR DIFFERS |

---

## 5. WindowProc / Message Handling

| Message | Original | C++ | Status |
|---------|----------|-----|--------|
| `WM_DESTROY` | Show cursor + `PostQuitMessage(0)` | Same | OK |
| `WM_SIZE/ERASEBKGND/ACTIVATEAPP` | Return 0 | Same | OK |
| `WM_ACTIVATE` focus-loss | `PauseGraphicsState()`, unacquire, show cursor, `g_bHasFocusLost=1`, **`UpdateCursorVisibilityAndScene(AL=1)`**, delayed timer | PauseGraphicsState stub, unacquire, show cursor, `g_bHasFocusLost=true`; UpdateCursorVisibilityAndScene is **TODO** | **MISSING** `UpdateCursorVisibilityAndScene` call |
| `WM_ACTIVATE` focus-gain | RealInputSystem vtable acquire, `AcquireInputDevices`, show cursor, **`UpdateCursorVisibilityAndScene(AL=0)`**, `g_bHasFocusLost=0`, delay timer | TODO vtable acquire, `AcquireInputDevices`; UpdateCursorVisibilityAndScene is **TODO** | **MISSING** both calls |
| `WM_SETFOCUS/SETCURSOR` | `PauseGraphicsState()` on focus-loss path | `PauseGraphicsState()` called (stub) | OK (stub) |
| `WM_SYSCOMMAND` | Block SC_MAXIMIZE/SIZE/MOVE/KEYMENU | Same | OK |
| `WM_NCHITTEST` | `HTCLIENT` fullscreen | Same | OK |
| `WM_ENTERMENULOOP` | Return `0x10000` | Same | OK |
| `WM_WTSSESSION_CHANGE` | wParam 0/7 → 1, else -1 | Same | OK |
| `WM_PAINT` | `ValidateRect` or `BeginPaint/EndPaint` | Same | OK |

---

## 6. Main Loop

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| Exit condition | `WM_QUIT` or exit flag | Same | OK |
| Focus-loss cursor handling | `SwitchRenderOutputMode` with specific scene IDs from `DAT_00c82b00/08/ac` | `SwitchRenderOutputMode()` called but stub — no scene IDs passed | OK (stub) |
| `GameFrameUpdate` | Called | Called | OK |
| Lazy `InitFrameCallbackSystem` | One-shot init guard before `QueryMemoryAllocatorMax` | Not present | **MISSING** |
| Memory allocator query | `QueryMemoryAllocatorMax()` tracks `g_nMinFreeMemory` | Called, but stub returns 0 | OK (stub returns 0) |
| Delayed timer countdown | Subtracts from timer each frame | `timeGetTime()` delta subtracted | OK |
| Resume audio on expiry | `AudioStream_Resume()` (`00ec67e8`) | TODO comment | **MISSING** |
| Resume game objects | `ResumeGameObjects()` | Same | OK |
| Frame rate cap | `Sleep(0)` if under budget | Same | OK |
| WM_QUIT filter | Filters `(UINT)-1` | Same | OK |

---

## 7. Timing System

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| `GetGameTime` implementation | `timeGetDevCaps + timeBeginPeriod + timeGetTime + timeEndPeriod` | Same | OK |
| 16.16 fixed-point arithmetic | Two separate 32-bit halves (`DAT_00e6e5e0` low, `DAT_00e6e5e4` high) | Single `ULONGLONG` | FUNCTIONALLY EQUIVALENT |
| Delta cap | `0x640000` (100ms in 16.16) | `MAX_DELTA_TIME_MS << 16` | OK |
| Game ticks formula | `accum * 3 / 0x10000` | Same | OK |
| Frame flip toggle | `DAT_00c83130 ^= 1` | `g_nFrameFlip ^= 1` | OK |
| Primary callback | `UpdateFrameTimingPrimary()` + `(*DAT_008e1644[0])(&localTick)` | TODO comment | **MISSING** |
| Secondary callback | `InterpolateFrameTime()` + `(*DAT_008e1644[1])()` | TODO comment | **MISSING** |
| Timing double-buffer | `DAT_00c83170[flip*8]` stores tick/time | Not implemented | **MISSING** |

---

## 8. DirectX Device Management

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| `PreDeviceCheck` before `TestCooperativeLevel` | `PreDeviceCheck()` (`0067d2e0`) | `GetAvailableTextureMem` inline | OK (equivalent) |
| Device lost sleep | 50ms | `DEVICE_LOST_SLEEP_MS (50)` | OK |
| `D3DPRESENT_PARAMETERS` | Saved struct `DAT_00b94af8`, reused for Reset | `g_d3dpp` global, updated before Reset | OK |
| `ReleaseDirectXResources` pre-cleanup | `thunk_FUN_00ec04dc()` | TODO comment | **MISSING** |
| `ReleaseDirectXResources` post-cleanup | `thunk_FUN_00ec19b5()` | TODO comment | **MISSING** |
| `RestoreDirectXResources` render states | `InitRenderStates()` | Called (empty stub) | OK (stub) |
| `RestoreDirectXResources` GPU sync | `CreateGPUSyncQuery()` if null | Called conditionally (stub) | OK (stub) |
| `RestoreDirectXResources` empty stub | `FUN_0067bb20()` (confirmed empty) | Not called | OK (no-op) |
| `RestoreDirectXResources` AA sentinel | `0xbacb0ffe` | `D3D_AA_PATH_SENTINEL = 0xbacb0ffe` | OK |
| `RestoreDirectXResources` hw cap check | Checks `TextureOpCaps & 0x80` before texture RT create | Not checked (always creates) | MINOR DIFFERS |
| `InitD3DStateDefaults` | Called at end of restore | Called (empty stub) | OK (stub) |
| `ReleaseDirectXResources` RT release | Gets back buffer from swap chain + `SetCachedRenderTargets` | Direct null-check release of `g_pRenderTarget` | SIMPLIFIED |
| `FatalError` on bad `TestCooperativeLevel` | Calls `FatalError(...)` — does not return | Comment noting this | **MISSING** |

---

## 9. DirectInput Management

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| Acquire cursor control | `Ordinal_5(0)` — custom cursor hide | `ShowCursor(FALSE)` | MINOR DIFFERS |
| Unacquire cursor control | `Ordinal_5(1)` — custom cursor show | `ShowCursor(TRUE)` | MINOR DIFFERS |
| Fatal error on failure | `FUN_0066f810` on acquire failure | No error checking | **MISSING** |
| DirectInput enumeration | `FUN_00688370(4, 0)` in `InitGameSubsystems` | Not implemented | **MISSING** (in stub) |

---

## 10. Pause/Resume System

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| Full pause | Triggers animation, vtable+0x24 on all systems, records pause time | State machine skeleton without vtable calls | **MISSING** |
| Audio-only pause | Pauses systems via vtable | State machine skeleton | **MISSING** |
| Full resume | Triggers animation, vtable+0x28 on all systems | State machine skeleton | **MISSING** |
| `PauseAudioManager` | Checks music, calls `FUN_006a9ea0` | Empty stub | **MISSING** |
| `ProcessDeferredCallbacks` | Processes linked list at `DAT_00bef7c0` within 2ms | Empty stub | **MISSING** |

---

## 11. Global Variable Mapping

| Original address | C++ variable | Status |
|-----------------|-------------|--------|
| `DAT_00bef6cc` | `ghWnd` | OK |
| `DAT_008afbd9` | `bIsFullscreen` | OK |
| `DAT_00bef6c7` | `g_bHasFocusLost` | OK |
| `DAT_00bef67e` | `g_bCursorVisible` | OK |
| `DAT_00bef6d8` | `g_dwDelayedOpTimer` | OK |
| `DAT_00bef6d7` | `g_bGameUpdateEnabled` | OK |
| `DAT_00bef6d4` | `g_bAudioWasPaused` | OK |
| `DAT_00bef6d5` | `g_bUpdatesWerePaused` | OK |
| `DAT_00bf18aa` | `g_bDeviceLost` | OK |
| `DAT_00c7b908` | `g_nPauseState` | OK |
| `DAT_008afb08` | `g_nMinFreeMemory` | OK (stub returns 0, so never updates) |
| `DAT_00bef6c5` | `g_bExitRequested` | OK |
| `DAT_00bef6c6` | `g_bSubsysInitialized` | OK |
| `DAT_00bef6d0` | `g_pComObject` (declared, never created) | **MISSING** factory |
| `DAT_00e6b384` | RealInputSystem — not exposed as a global | **MISSING** |
| `DAT_00b94af8` | `g_d3dpp` | OK |

---

## Summary: Priority Fixes for Next Iteration

**High priority (functional correctness):**
1. Add `CLI_CommandParser_ParseArgs()` stub call before single-instance check in WinMain
2. Add `UpdateCursorVisibilityAndScene()` call in `WM_ACTIVATE` focus-loss path (with cursor-visible=true)
3. Add `UpdateCursorVisibilityAndScene()` call in `WM_ACTIVATE` focus-gain path (with cursor-visible=false)
4. Add TODO for RealInputSystem vtable input re-acquire in `WM_ACTIVATE` focus-gain path
5. Add `AudioStream_Resume()` stub call on delayed-timer expiry in `MainLoop`
6. Add primary callback TODO in `GameFrameUpdate` (`UpdateFrameTimingPrimary + (*DAT_008e1644[0])`)
7. Add secondary callback TODO in `GameFrameUpdate` (`InterpolateFrameTime + (*DAT_008e1644[1])`)

**Medium priority (structural fidelity):**
8. Add engine object factory TODO in WinMain (before `PreDirectXInit`): creates `g_pComObject`
9. Add missing `thunk_FUN_00ec04dc()` and `thunk_FUN_00ec19b5()` TODO stubs in DX resource management
10. Fix `RegisterWindowClass`: apply `CS_OWNDC | CS_DBLCLKS` only in fullscreen; add `CS_VREDRAW | CS_HREDRAW` in windowed mode
11. Implement `SaveOptionsOnExit`: write OptionResolution/LOD/Brightness to registry

**Low priority (implementation details):**
12. `AcquireInputDevices`/`UnacquireInputDevices`: note `Ordinal_5` stub difference
13. Add error checking in `AcquireInputDevices` (fatal error on failure)
14. `RestoreDirectXResources`: add hardware capability check (`TextureOpCaps & 0x80`) before texture RT path
