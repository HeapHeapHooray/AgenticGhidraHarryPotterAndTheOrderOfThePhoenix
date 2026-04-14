# C++ Decompilation Architecture

## Overview
The C++ implementation in `decompilation-src/main.cpp` is a reconstruction of the main executable for "Harry Potter and the Order of the Phoenix". It targets Windows x86 (32-bit) and uses DirectX 9 for rendering and DirectInput 8 for input. The code is compiled with the Zig cross-compiler targeting `x86-windows-gnu`.

## Build System
- **Compiler**: `zig c++`, targeting `x86-windows-gnu`
- **Output**: `hp_decompiled.exe` (PE32 Windows executable)
- **Libraries**: `-luser32 -lgdi32 -lole32 -luuid -lwinmm -lc++`
- **Build script**: `decompilation-src/build.sh`
- **Warning**: `-mwindows` flag produces an unused-arg warning (benign)

## Global State (`globals.h` + top of `main.cpp`)

### Window State
| Variable | Type | Purpose |
|----------|------|---------|
| `ghWnd` | `HWND` | Main window handle |
| `bIsFullscreen` | `bool` | True if fullscreen mode |
| `bHasFocus` | `bool` | Window focus state |
| `gWidth` | `int` | Current window width |
| `gHeight` | `int` | Current window height |

### DirectX Surfaces
| Variable | Type | Purpose |
|----------|------|---------|
| `g_pD3D` | `IDirect3D9*` | D3D9 factory object |
| `g_pd3dDevice` | `IDirect3DDevice9*` | Active D3D9 device |
| `g_pBackBuffer` | `IDirect3DSurface9*` | Back buffer (or texture surface in non-AA path) |
| `g_pRenderTarget` | `IDirect3DSurface9*` | Texture render target, or `D3D_AA_PATH_SENTINEL` |
| `g_pAdditionalRT` | `IDirect3DSurface9*` | Additional render target |
| `g_pCachedRT` | `IDirect3DSurface9*` | Last-set render target (avoids redundant calls) |
| `g_pCachedDS` | `IDirect3DSurface9*` | Last-set depth-stencil (avoids redundant calls) |
| `g_pGPUSyncQuery` | `IUnknown*` | D3DQUERYTYPE_EVENT GPU sync query |

### DirectInput Devices
| Variable | Type | Purpose |
|----------|------|---------|
| `g_pDirectInput` | `IDirectInput8*` | DirectInput8 factory |
| `g_pKeyboard` | `IDirectInputDevice8*` | Keyboard device |
| `g_pMouse` | `IDirectInputDevice8*` | Mouse device |
| `g_pJoystick[2]` | `IDirectInputDevice8*` | Up to 2 joysticks |

### Timing State
| Variable | Type | Purpose |
|----------|------|---------|
| `g_dwStartupTime` | `DWORD` | Multimedia timer baseline (set on first `GetGameTime` call) |
| `g_bTimebaseInit` | `bool` | True after first `GetGameTime` call |
| `g_ullAccumTime` | `ULONGLONG` | 64-bit accumulated game time (16.16 fixed point) |
| `g_ullNextCallback` | `ULONGLONG` | Next frame callback fire time |
| `g_ullCallbackInterval` | `ULONGLONG` | Interval between frame callbacks |
| `g_nFrameFlip` | `int` | Double-buffer flip index (XOR-toggled each callback) |
| `g_dwGameTicks` | `DWORD` | `= accum * 3 / 0x10000` (game ticks, 3× speed) |

### System State Flags
| Variable | Purpose |
|----------|---------|
| `g_bExitRequested` | Set to exit `MainLoop` |
| `g_bHasFocusLost` | Window currently does not have focus |
| `g_bCursorVisible` | Current cursor visibility state |
| `g_bGameUpdateEnabled` | Whether game update subsystem is active |
| `g_bAudioWasPaused` | Audio was paused on last focus loss |
| `g_bUpdatesWerePaused` | Physics/updates were paused on last focus loss |
| `g_bDeviceLost` | D3D device is currently lost |
| `g_dwDelayedOpTimer` | Countdown (ms) before resuming paused systems |
| `g_nPauseState` | Current game-object pause state (0–7) |
| `g_nMinFreeMemory` | Low-water mark of free memory across frames |

### Structured State
- `g_savedParams` (`SystemParams`): saved mouse speed/accel/screen reader settings (2+2+6 UINT arrays)
- `g_gfxSettings` (`GraphicsSettings`): all 20+ registry-backed graphics settings
- `g_szCmdLine1/2[512]`: two copies of the original command line

## Registry System

### `ReadRegistrySetting` (int values)
1. Try `HKEY_CURRENT_USER\Software\Electronic Arts\<app>\<section>`
2. Try `HKEY_LOCAL_MACHINE\...\<section>`
3. If not found anywhere: create key in HKCU with default, return default
- Returns `int`; uses `RegQueryValueExA` with `REG_DWORD`

### `WriteRegistrySetting` (string values)
- Creates `HKCU` path with `RegCreateKeyExA`
- Writes with `RegSetValueExA` as `REG_SZ`
- Used exclusively for window placement persistence

### `ReadRegistrySettingStr` (string values)
- Same HKCU → HKLM fallback as above
- Copies to caller-provided buffer; no HKCU write-back on miss
- Returns `true` if key was found

### `LoadGameSettings`
Reads all game settings from `HKCU/.../GameSettings`:
- `Width` (0 = windowed; non-zero = fullscreen width)
- `BitDepth`, `ShadowLOD` (default 6), `MaxTextureSize`, `MaxShadowTextureSize`
- `MaxFarClip`, `CullDistance`, `ParticleRate`, `ParticleCullDistance`
- `DisableFog`, `DisablePostPro`, `FilterFlip`, `AAMode` (0 = off), `UseAdditionalModes`
- `OptionResolution`, `OptionLOD` (default 1), `OptionBrightness` (default 5)
- `Mode0..5Width` / `Mode0..5Height` — 6 custom display mode pairs

### `SaveWindowPlacement`
Saves window state on `WM_CLOSE` in windowed mode:
- Queries `GetWindowPlacement` and adjusts rect by `AdjustWindowRect` border offsets
- Writes `PosX`, `PosY`, `SizeX`, `SizeY`, `Maximized`, `Minimized` as strings

## System Parameter Management

### `SaveOrRestoreSystemParameters(bool restore)`
- **restore=false**: Reads current `mouseSpeed[2]`, `mouseAccel[2]`, `screenReader[6]` via `SystemParametersInfoA`. For each, if bit 0 of the flag word is 0 (acceleration not already disabled), clears bits 2–3 using `MOUSE_ACCEL_FLAGS_MASK = 0xFFFFFFF3` and writes back.
- **restore=true**: Writes the saved arrays back verbatim.
- Called with `false` at startup, `true` at shutdown.

## Command Line Parsing

### `ParseCommandLineArg`
- Case-insensitive scan for ` <flag>` in the command line string
- Recognizes flag termination by `\0`, space, tab, or `=`
- If `valueOut != NULL` and `=` follows the flag, returns pointer to value token
- Called for `fullscreen` (sets `bIsFullscreen`, optionally reads width) and `widescreen` (sets aspect ratio to 16:9)

## Window Management

### `RegisterWindowClass`
- Calls `UnregisterClassA` first to handle crashed previous instances
- Class: `OrderOfThePhoenixMainWndClass`
- Style: `CS_DBLCLKS | CS_OWNDC | CS_VREDRAW | CS_HREDRAW`
- Cursor: `IDC_ARROW`, background: black brush

### `CreateGameWindow(hInstance, x, y, width, height)`
**Fullscreen path:**
- Style `WS_POPUP`, ex-style `WS_EX_TOPMOST`
- `AdjustWindowRectEx` for borderless sizing
- Creates at (0,0), then `ShowWindow(SW_HIDE)` + `SetWindowPos(HWND_TOPMOST, SWP_SHOWWINDOW)`
- Calls `SetMenu(NULL)` and `SetThreadExecutionState(ES_CONTINUOUS | ES_DISPLAY_REQUIRED)`

**Windowed path:**
- Style `WS_OVERLAPPEDWINDOW`
- Position and size from registry (defaults: 300,32, 640×480)

### `WindowProc`
Message handlers:
| Message | Action |
|---------|--------|
| `WM_DESTROY` | Show cursor in fullscreen, `PostQuitMessage(0)` |
| `WM_SIZE`, `WM_ERASEBKGND`, `WM_ACTIVATEAPP` | Return 0 (suppress default) |
| `WM_ACTIVATE` | Full focus-management logic (see below) |
| `WM_SETFOCUS`, `WM_SETCURSOR` | Hide/show cursor; bind D3D cursor |
| `WM_PAINT` | Fullscreen: `ValidateRect`; windowed: `BeginPaint/EndPaint` |
| `WM_CLOSE` | Windowed: `SaveWindowPlacement` + `DestroyWindow` |
| `WM_SYSCOMMAND` | Fullscreen: block SC_MAXIMIZE/SIZE/MOVE/KEYMENU |
| `WM_NCHITTEST` | Fullscreen: return `HTCLIENT` |
| `WM_ENTERMENULOOP` | Fullscreen: return `MENU_LOOP_SUPPRESS (0x10000)` |
| `WM_WTSSESSION_CHANGE` | wParam 0 or 7 → return 1; else return -1 |

**Focus loss (`WM_ACTIVATE`, `wParam==WA_INACTIVE`, fullscreen):**
1. `UnacquireInputDevices()`
2. `ShowCursor` loop until cursor count ≥ 1
3. `g_bHasFocusLost = true`
4. If `g_dwDelayedOpTimer == 0`: `PauseAudioManager()`, `PauseGameObjects(0)`; set pause flags
5. `g_dwDelayedOpTimer = 0`

**Focus gain (`WM_ACTIVATE`, `wParam!=WA_INACTIVE`, fullscreen):**
1. `AcquireInputDevices()`
2. `ShowCursor` loop until cursor count < 0
3. `g_bHasFocusLost = false`
4. `g_dwDelayedOpTimer = FOCUS_CHANGE_DELAY_MS (2000)`

## DirectX Device Management

### `ReleaseDirectXResources`
1. Release `g_pGPUSyncQuery` if non-null
2. Clear `g_pCachedRT` and `g_pCachedDS`
3. If `g_pRenderTarget` is non-null and not `D3D_AA_PATH_SENTINEL (0xbacb0ffe)`: release it
4. Release `g_pBackBuffer` if non-null
5. Release `g_pAdditionalRT` if non-null

### `RestoreDirectXResources`
1. `GetBackBuffer(0, 0, MONO)` → `g_pBackBuffer`; set `g_pCachedRT = g_pBackBuffer`
2. Get surface descriptor from back buffer
3. `CreateRenderTarget` → `g_pAdditionalRT`
4. **Non-AA path** (`g_gfxSettings.aaMode == 0`):
   - `CreateTexture(..., D3DUSAGE_RENDERTARGET)` → texture
   - Release original back buffer, `GetSurfaceLevel(0)` → new `g_pBackBuffer`
   - `g_pRenderTarget = (IDirect3DSurface9*)pTex`
5. **AA path**: `g_pRenderTarget = (IDirect3DSurface9*)D3D_AA_PATH_SENTINEL`
6. Set `g_pCachedDS = g_pAdditionalRT`
7. Call `SetRenderTarget(0, g_pCachedRT)` and `SetDepthStencilSurface(g_pCachedDS)`
8. TODO: `InitD3DStateDefaults()`

### `UpdateDirectXDevice`
1. Query available texture memory (`GetAvailableTextureMem() >> 20`); if 0 < MB < 33: low-memory handler (stub)
2. `TestCooperativeLevel`:
   - `D3DERR_DEVICELOST`: set `g_bDeviceLost=true`, sleep 50ms, return
   - `D3DERR_DEVICENOTRESET`: `ReleaseDirectXResources` → `Reset` → `RestoreDirectXResources`
   - Unexpected error: comment notes original calls `FatalError` (not yet implemented)
   - Success: `g_bDeviceLost = false`

## DirectInput Management

### `AcquireInputDevices` / `UnacquireInputDevices`
- Acquires/unacquires keyboard, mouse, and up to 2 joysticks
- `AcquireInputDevices` hides cursor with `ShowCursor(FALSE)`
- `UnacquireInputDevices` shows cursor with `ShowCursor(TRUE)`

## Audio and Game Object Pause System

### `PauseAudioManager` (stub)
- Original checks if music track is loaded, then calls `FUN_006a9ea0` to pause audio stream
- Current implementation: empty stub with comment

### `PauseGameObjects(int param)`
State machine on `g_nPauseState`:
- If state is not 4 or 5: full pause → set state to `PAUSE_STATE_FULL (0)`
- If state is 4 or 5: audio-only pause → set state to `PAUSE_STATE_NOOP (6)`
- TODO: trigger animations, call vtable Pause() on all registered systems

### `ResumeGameObjects`
State machine on `g_nPauseState`:
- If state ≤ 3: full resume → set state to `PAUSE_STATE_RESUME (7)`, fall through
- If state == 6: partial resume → set state to 5
- TODO: trigger animations, call vtable Resume() on all registered systems

## Timing System

### `GetGameTime`
```cpp
DWORD GetGameTime() {
    TIMECAPS caps;
    timeGetDevCaps(&caps, sizeof(caps));
    timeBeginPeriod(caps.wPeriodMin);
    DWORD t = timeGetTime();
    timeEndPeriod(caps.wPeriodMin);
    if (!g_bTimebaseInit) { g_bTimebaseInit = true; g_dwStartupTime = t; }
    return t - g_dwStartupTime;
}
```
- Uses Windows multimedia timer for sub-millisecond resolution
- Returns ms since first call (game startup baseline)

### `GameFrameUpdate`
1. `ProcessDeferredCallbacks()` — deferred render batch queue (2ms budget)
2. `GetGameTime()` → convert to 16.16 fixed point: `tFixed = (ULONGLONG)tMs << 16`
3. **First call**: set `g_ullAccumTime = g_ullNextCallback = tFixed`, return
4. Compute delta, cap at `MAX_DELTA_TIME_MS (100ms) << 16`
5. `g_ullAccumTime += delta`
6. `g_dwGameTicks = (g_ullAccumTime * 3) / 0x10000` (3× speed multiplier)
7. If `g_ullAccumTime >= g_ullNextCallback`:
   - `g_ullNextCallback += g_ullCallbackInterval`
   - `g_nFrameFlip ^= 1`
   - TODO: call primary and secondary frame callbacks via `DAT_008e1644`

### `ProcessDeferredCallbacks` (stub)
- Original processes linked list at `DAT_00bef7c0` within a 2ms time budget
- Current implementation: empty stub with comment

## Main Game Loop

### `MainLoop`
```
while (!g_bExitRequested):
  1. UpdateDirectXDevice()
  2. Fullscreen focus management: unacquire/show cursor on focus loss
  3. PeekMessage(PM_REMOVE):
     - WM_QUIT → set g_bExitRequested
     - Otherwise: TranslateMessage + DispatchMessage
  4. No messages:
     a. GameFrameUpdate()
     b. Manage g_dwDelayedOpTimer countdown:
        - On expiry: resume audio (TODO), ResumeGameObjects()
     c. Frame rate cap: if elapsed < TARGET_FRAME_TIME_MS (16ms): Sleep(0)
```
- Uses `timeGetTime()` for frame elapsed measurement
- `g_bExitRequested` is the exit condition (set by `WM_QUIT`)

## WinMain Initialization Sequence
1. `_control87(0x20000, 0x30000)` — FPU denormal prevention
2. `SaveOrRestoreSystemParameters(false)` — save + disable mouse accel
3. `strncpy` command line to `g_szCmdLine1` and `g_szCmdLine2`
4. `FindWindowA` single-instance guard → `TerminateProcess` if duplicate
5. `RegisterWindowClass`
6. `LoadGameSettings` — read all registry settings
7. `ParseCommandLineArg` for `fullscreen` and `widescreen`
8. Determine window position/size from registry or defaults
9. `CreateGameWindow(hInstance, winX, winY, winW, winH)`
10. Restore `Maximized`/`Minimized` state (windowed only)
11. `UpdateWindow`
12. TODO: `InitDirectXAndSubsystems(winH)`
13. TODO: `InitGameSubsystems()`
14. `MainLoop()`
15. TODO: render teardown, COM object release
16. `UnacquireInputDevices()`, show cursor
17. `SaveOrRestoreSystemParameters(true)` — restore mouse accel
18. `TerminateProcess(GetCurrentProcess(), 1)` — hard exit (no CRT teardown)

## Known Gaps / TODOs
- `InitDirectXAndSubsystems` and `InitGameSubsystems` are not yet implemented (calls stubbed out)
- `PauseAudioManager`, `PauseGameObjects`, `ResumeGameObjects` are stubs (state machine logic without vtable calls)
- `ProcessDeferredCallbacks` is a stub
- `GameFrameUpdate` does not yet call the frame callback function pointers
- `InitD3DStateDefaults` is noted in `RestoreDirectXResources` but not called
- Render teardown on exit is commented as TODO
- `FatalError` on unexpected `TestCooperativeLevel` result is not implemented
- `PauseGraphicsState` call in `WM_ACTIVATE`/`WM_SETFOCUS` is a TODO comment
