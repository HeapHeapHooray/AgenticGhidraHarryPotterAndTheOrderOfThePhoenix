# Differences Between Original Binary and C++ Decompilation

This document compares the architecture documented in `ARCHITECTURE.md` (from binary analysis)
against the C++ reconstruction in `decompilation-src/` (described in `CPP-ARCHITECTURE.md`).
Items marked **MISSING** represent gaps to address in the next iteration.

---

## 1. WinMain Initialization Sequence

| Step | Original (`ARCHITECTURE.md`) | C++ (`main.cpp`) | Status |
|------|------------------------------|-----------------|--------|
| FPU config | `_control87(0x20000, 0x30000)` | `_control87(0x20000, 0x30000)` | OK |
| System params | Save + disable mouse accel | `SaveOrRestoreSystemParameters(false)` | OK |
| Cmdline copy | strncpy to two 512-byte buffers | strncpy to `g_szCmdLine1/2` | OK |
| COM/thread init | `thunk_FUN_00eb787a()` before single-instance check | Not present | **MISSING** |
| Single instance | `FindWindowA` → TerminateProcess | `FindWindowA` → TerminateProcess | OK |
| Window class | `RegisterWindowClass()` | `RegisterWindowClass()` | OK |
| Registry load | `LoadGameSettings()` | `LoadGameSettings()` | OK |
| Cmdline parse | `fullscreen`, `widescreen` | `ParseCommandLineArg` for both | OK |
| Window creation | `CreateGameWindow(hInstance, width, height)` — **2 args, no x/y** | `CreateGameWindow(hInstance, x, y, width, height)` — **4 args** | SIGNATURE DIFFERS |
| DirectX init | `thunk_FUN_00eb612e(height)` | TODO comment | **MISSING** |
| Game subsystems | `thunk_FUN_00eb496e()` | TODO comment | **MISSING** |
| Window placement | Restore after subsystem init | Restore after window create, before MainLoop | ORDER DIFFERS |
| Main loop | `MainLoop()` | `MainLoop()` | OK |
| Render teardown | `thunk_FUN_00ec6610()` | TODO comment | **MISSING** |
| COM release | Release `DAT_00bef6d0` | TODO comment | **MISSING** |
| Input cleanup | `UnacquireInputDevices()` | `UnacquireInputDevices()` | OK |
| Param restore | `SaveOrRestoreSystemParameters(true)` | `SaveOrRestoreSystemParameters(true)` | OK |
| Hard exit | `TerminateProcess(hProc, 1)` | `TerminateProcess(hProc, 1)` | OK |

**Key issue**: COM/thread initialization (`thunk_FUN_00eb787a`) happens before the single-instance check in the original but is absent from the C++ code. `CreateGameWindow` signature has extra x/y parameters not present in the original's 2-arg form.

---

## 2. Registry System

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| HKCU → HKLM fallback | Yes | Yes | OK |
| Auto-create in HKCU | Yes | Yes | OK |
| `std::basic_string` internal use | Yes — uses C++ string objects internally | No — plain C strings | DIFFERS (implementation detail) |
| HKCU write-back on HKLM hit | Original calls a helper (`FUN_0060cc70`) that creates the HKCU key | C++ returns value without HKCU write-back | **MISSING** |

---

## 3. System Parameter Management

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| Storage layout | `mouseSpeed[2]`, `mouseAccel[2]`, `screenReader[6]` | Same struct layout | OK |
| Get calls | `SPI_GETMOUSESPEED (0x3a)`, `SPI_GETMOUSE (0x34)`, `SPI_GETSCREENREADER (0x32)` | Same codes via globals.h constants | OK |
| Set calls | `SPI_SETMOUSESPEED (0x3b)`, `SPI_SETMOUSE (0x35)`, `SPI_SETSCREENREADER (0x33)` | Same | OK |
| Bit check | bit 0 clear = acceleration active | bit 0 clear = acceleration active | OK |
| Mask | `0xFFFFFFF3` clears bits 2–3 | `MOUSE_ACCEL_FLAGS_MASK = 0xFFFFFFF3` | OK |

---

## 4. Window Creation

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| Signature | `CreateGameWindow(hInstance, width, height)` | `CreateGameWindow(hInstance, x, y, width, height)` | EXTRA PARAMS |
| Fullscreen style | `WS_POPUP` | `WS_POPUP` | OK |
| Windowed style | `WS_OVERLAPPEDWINDOW` | `WS_OVERLAPPEDWINDOW` | OK |
| TOPMOST | `HWND_TOPMOST` in fullscreen | `HWND_TOPMOST` in fullscreen | OK |
| SetMenu | `SetMenu(hWnd, NULL)` in fullscreen | `SetMenu(hWnd, NULL)` | OK |
| SetThreadExecutionState | `ES_CONTINUOUS \| ES_DISPLAY_REQUIRED` | Same | OK |
| ShowWindow sequence | `ShowWindow(hWnd, 0)` then `SetWindowPos` | Same logic | OK |

The original `CreateGameWindow` takes only `(hInstance, width, height)` — position comes from registry within the function. The C++ version takes x, y as parameters which is incorrect.

---

## 5. WindowProc / Message Handling

| Message | Original | C++ | Status |
|---------|----------|-----|--------|
| `WM_DESTROY` | Show cursor + `PostQuitMessage(0)` | Same | OK |
| `WM_SIZE/ERASEBKGND/ACTIVATEAPP` | Return 0 | Return 0 | OK |
| `WM_ACTIVATE` focus-loss | PauseGraphicsState + UnacquireInputDevices + ... | UnacquireInputDevices + ... (PauseGraphicsState is TODO) | **MISSING** `PauseGraphicsState` |
| `WM_ACTIVATE` focus-gain | Via `DAT_00e6b384+0xc` vtable for input acquire, then AcquireInputDevices | `AcquireInputDevices()` directly | MISSING vtable-based acquire |
| `WM_ACTIVATE` focus-gain | Calls `thunk_FUN_00ea53ca()` (render pause) | Not present | **MISSING** |
| `WM_SETFOCUS/SETCURSOR` | Calls `PauseGraphicsState()` on focus loss path | TODO comment | **MISSING** `PauseGraphicsState` |
| `WM_SYSCOMMAND` | Blocks SC_MAXIMIZE/SIZE/MOVE/KEYMENU | Same | OK |
| `WM_NCHITTEST` | `HTCLIENT` fullscreen | Same | OK |
| `WM_ENTERMENULOOP` | Return `0x10000` | Same | OK |
| `WM_WTSSESSION_CHANGE` | wParam 0/7 → 1, else -1 | Same | OK |
| `WM_PAINT` | `ValidateRect` or `BeginPaint/EndPaint` | Same | OK |

---

## 6. Main Loop

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| Exit condition | `DAT_00bef6c5` flag or `WM_QUIT` | `g_bExitRequested` or `WM_QUIT` | OK |
| Focus-loss cursor handling | Calls `SwitchRenderOutputMode` with scene IDs | Just cursor show/hide, no `SwitchRenderOutputMode` | **MISSING** |
| Frame budget computation | Complex `ROUND((accumHigh * k1 + accumLow) * k2 * k3)` | Simple `timeGetTime()` elapsed | SIMPLIFIED |
| `GameFrameUpdate` | Called | Called | OK |
| Memory allocator query | `QueryMemoryAllocatorMax()` tracks `g_nMinFreeMemory` | `g_nMinFreeMemory` declared but not updated | **MISSING** |
| Delayed timer countdown | Subtracts from timer each frame | `timeGetTime()` delta subtracted | OK (approach differs) |
| Resume audio on timer expiry | `thunk_FUN_00ec67e8()` | TODO comment | **MISSING** |
| Resume game objects | `ResumeGameObjects()` | `ResumeGameObjects()` | OK |
| Frame rate cap | `Sleep(0)` if under budget | `Sleep(0)` if under `TARGET_FRAME_TIME_MS` | OK |
| WM_QUIT filter | filters `(UINT)-1` | filters `(UINT)-1` | OK |

---

## 7. Timing System

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| `GetGameTime` | `timeGetDevCaps` + `timeBeginPeriod` + `timeGetTime` + `timeEndPeriod` | Same | OK |
| Startup baseline | `_DAT_00e6e5e8` | `g_dwStartupTime` | OK |
| 16.16 fixed-point conversion | `high = t >> 16`, `low = t << 16` (separate 32-bit halves) | `tFixed = (ULONGLONG)tMs << 16` (single 64-bit) | IMPLEMENTATION DIFFERS |
| Delta cap | `0x640000` (100ms in 16.16) | `(ULONGLONG)MAX_DELTA_TIME_MS << 16` | OK |
| Game ticks formula | `accum * 3 / 0x10000` | `(g_ullAccumTime * 3) / TIME_FIXED_SHIFT` | OK |
| Primary callback | `UpdateFrameTimingPrimary + (*DAT_008e1644[0])(&local_4)` | TODO comment | **MISSING** |
| Secondary callback | `InterpolateFrameTime + (*DAT_008e1644[1])()` | TODO comment | **MISSING** |
| Timing double-buffer | `DAT_00c83170[flip*8]` stores tick/time | Not implemented | **MISSING** |

The original uses **two separate 32-bit halves** for the 64-bit fixed-point value (`DAT_00e6e5e0` = low, `DAT_00e6e5e4` = high). The C++ uses a single `ULONGLONG`. This is functionally equivalent but differs structurally from the original.

---

## 8. DirectX Device Management

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| `PreDeviceCheck()` before `TestCooperativeLevel` | Called | `GetAvailableTextureMem` inline (functional equivalent) | OK |
| `TestCooperativeLevel` | vtable+0xc | Direct method call | OK |
| Device lost sleep | 50ms | `DEVICE_LOST_SLEEP_MS (50)` | OK |
| `D3DPRESENT_PARAMETERS` | Stored struct at `DAT_00b94af8` | Local struct created on demand | DIFFERS — original reuses saved params |
| `ReleaseDirectXResources` pre-cleanup | `thunk_FUN_00ec04dc()` | Not present | **MISSING** |
| `ReleaseDirectXResources` post-cleanup | `thunk_FUN_00ec19b5()` | Not present | **MISSING** |
| `RestoreDirectXResources` first step | `InitRenderStates()` | TODO in comment | **MISSING** `InitRenderStates` |
| GPU sync query | `CreateGPUSyncQuery()` if null | Not called in `RestoreDirectXResources` | **MISSING** |
| AA sentinel | `0xbacb0ffe` | `D3D_AA_PATH_SENTINEL = 0xbacb0ffe` | OK |
| `InitD3DStateDefaults` | Called at end of restore | TODO comment | **MISSING** |
| `ReleaseDirectXResources` RT release | Gets back buffer from swap chain, calls `FUN_0067ecf0`, releases separately | Direct release of `g_pRenderTarget` | SIMPLIFIED |

---

## 9. DirectInput Management

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| `AcquireInputDevices` | Also calls `Ordinal_5(0)` (custom hide cursor) | `ShowCursor(FALSE)` only | MINOR DIFFERS |
| `UnacquireInputDevices` | Also calls `Ordinal_5(1)` | `ShowCursor(TRUE)` only | MINOR DIFFERS |
| Fatal error on failure | `FUN_0066f810` called on acquire failure | No error checking | **MISSING** |
| DirectInput init | `FUN_00688370(4, 0)` in `InitGameSubsystems` | Not implemented | **MISSING** |

---

## 10. Pause/Resume System

| Aspect | Original | C++ | Status |
|--------|----------|-----|--------|
| Full pause | Triggers animation, calls vtable+0x24 on all systems, records pause time | State machine stub only | **MISSING** vtable calls |
| Audio-only pause | Pauses `DAT_00c7c370` via vtable | State machine stub only | **MISSING** |
| Full resume | Triggers animation, calls vtable+0x28 on all systems, records resume time | State machine stub only | **MISSING** vtable calls |
| `PauseAudioManager` | Checks music loaded, calls `FUN_006a9ea0` | Empty stub | **MISSING** |
| `ProcessDeferredCallbacks` | Processes linked list at `DAT_00bef7c0` within 2ms budget | Empty stub | **MISSING** |

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
| `DAT_008afb08` | `g_nMinFreeMemory` | declared, **not updated** |
| `DAT_00e6b384` | not present | **MISSING** |
| `DAT_00bef6d0` | not present | **MISSING** |
| `DAT_00bef6c5` | `g_bExitRequested` | OK |

---

## Summary: Priority Fixes for Next Iteration

**High priority (functional correctness):**
1. Add `thunk_FUN_00eb787a()` (COM/thread init) stub before single-instance check in WinMain
2. Add `InitDirectXAndSubsystems(height)` and `InitGameSubsystems()` stub calls in WinMain
3. Fix `CreateGameWindow` signature: remove `x, y` params; read position from registry inside the function
4. Add `PauseGraphicsState()` TODO stubs in `WM_ACTIVATE` focus-loss and `WM_SETFOCUS`
5. Add `thunk_FUN_00ea53ca()` (render pause) stub to `WM_ACTIVATE` both paths
6. Add HKCU write-back when key found in HKLM (`ReadRegistrySetting`)
7. Add `InitRenderStates()`, `CreateGPUSyncQuery()`, `InitD3DStateDefaults()` stub calls in `RestoreDirectXResources`
8. Add `thunk_FUN_00ec04dc()` and `thunk_FUN_00ec19b5()` pre/post cleanup stubs in `ReleaseDirectXResources`
9. Add frame callback dispatch stubs in `GameFrameUpdate` (primary + secondary)
10. Add `SwitchRenderOutputMode` stub call in the focus-loss path of `MainLoop`
11. Update `g_nMinFreeMemory` via `QueryMemoryAllocatorMax()` stub call in `MainLoop`
12. Add `thunk_FUN_00ec67e8()` (resume audio) stub call on timer expiry in `MainLoop`

**Medium priority (structural fidelity):**
13. Add persistent `D3DPRESENT_PARAMETERS g_d3dpp` global (matching `DAT_00b94af8`)
14. Add `g_pComObject` (IUnknown*) for `DAT_00bef6d0` — created in WinMain, released at exit
15. Add vtable-based input acquire via game state object in focus-gain path of `WM_ACTIVATE`
16. Move window placement restore to after `InitDirectXAndSubsystems` call (matching original order)

**Low priority (implementation details):**
17. `AcquireInputDevices`/`UnacquireInputDevices`: add `Ordinal_5` custom cursor call stubs
18. Add error checking in `AcquireInputDevices` (fatal error on failure)
