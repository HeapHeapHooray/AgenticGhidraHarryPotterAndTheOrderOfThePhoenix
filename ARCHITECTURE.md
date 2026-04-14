# Architecture of hp.exe

## Overview
`hp.exe` is the main executable for "Harry Potter and the Order of the Phoenix". It is a Windows PE application built with Visual Studio 2005 using DirectX 9 for rendering and DirectInput 8 for input. It uses a custom engine with message-based subsystem communication and a cooperative-multitasking deferred callback queue.

## Entry Point
The program starts at `___tmainCRTStartup` (`006f54d2`), the standard CRT entry for a Windows app, which calls `WinMain`.

## WinMain (`0060dfa0`)
Core entry point for all game logic. Initialization order:

1. **FPU Configuration**: `_control87(0x20000, 0x30000)` — prevents denormal floating-point exceptions
2. **System Parameters**: Saves current Windows parameters via `SystemParametersInfoA` into globals `DAT_008afc44` (mouseSpeed[2]), `DAT_008afc4c` (mouseAccel[2]), `DAT_008afc54` (screenReader[6]), then calls `UpdateSystemParameters(0)` to disable mouse acceleration
3. **Command Line Storage**: `strncpy` of `lpCmdLine` into `DAT_00c82b88` (0x1ff bytes) and `DAT_00c82d88` (0x1ff bytes); `DAT_00c82d87 = 0` (null-terminates the second copy)
4. **CLI Argument Parsing**: `CLI_CommandParser_ParseArgs()` (`00eb787a`) — parses additional `-name=value` command-line tokens into a CLI::CommandParser object. **NOT COM init** (previously misidentified).
5. **Single Instance Check**: `FindWindowA("OrderOfThePhoenixMainWndClass", NULL)` — if found, calls `TerminateProcess(GetCurrentProcess(), 0)`
6. **Window Class Registration**: `RegisterWindowClass()` (`00eb4b95`)
7. **Registry Settings Load**: Reads all game settings using `std::basic_string<char>` wrapper objects (see section below). The original ReadRegistrySetting takes `std::basic_string` params internally.
8. **Command Line Parsing**: `ParseCommandLineArg` (`00617bf0`) checks for `fullscreen` and `widescreen` flags
9. **Window Creation**: `CreateGameWindow(hInstance, width, height)` → stored in `DAT_00bef6cc`; then `GetClientRect(DAT_00bef6cc, &rect)` to get exact client height
10. **Engine Object Factory**: `GetOrInitCallbackManager()` → calls factory via callback system's secondary entry vtable with size=0xb58 (2904 bytes) and magic `{0x88332000000001, 0}` → result stored in `DAT_00bef6d0` (a large engine sub-object)
11. **Pre-DirectX Init**: `PreDirectXInit()` (`00ec64f9`) — sets `DAT_00bf1b18 = DAT_00bef6d0` (passes engine object to render/audio system), zeros audio flag `DAT_00bf1b10`, copies audio device string to `DAT_00be93d0`, calls `FUN_006ac0b0` + `thunk_FUN_00ec6e91` (audio subsystem setup), creates audio output context `DAT_00bf1b1c` via `thunk_FUN_00ec72a9()`
12. **DirectX Init**: `InitDirectXAndSubsystems(clientHeight)` (`00eb612e`) — creates D3D device, input, audio subsystems
13. **Game Subsystems Init**: `InitGameSubsystems()` (`00eb496e`) — registers callbacks, enumerates input devices, loads language screen; sets `DAT_00bef6c6 = 1`
14. **Window Placement Restore**: In windowed mode, reads `Maximized`/`Minimized` as `std::basic_string` and calls `ShowWindow` (SW_MAXIMIZE=3, SW_MINIMIZE=6, else nShowCmd); then `UpdateWindow(hWnd)`
15. **Main Loop**: `MainLoop()` (`0060dc10`)
16. **Cleanup**:
    - `RenderAndAudioTeardown()` (`00ec6610`) — releases audio/render resources
    - Release `DAT_00bef6d0` via `(**(callback_mgr + 0xc))(DAT_00bef6d0, 0)` (NOT simple COM release)
    - `SaveOptionsOnExit()` (`00eb4a5d`) — writes OptionResolution, OptionLOD, OptionBrightness back to registry via `FUN_0060cc70` (WriteRegistrySetting integer helper)
    - Pause input device via RealInputSystem vtable (mirrors PauseGraphicsState logic)
    - `UnacquireInputDevices()` + show cursor loop
    - `DAT_00bef6c7 = 1`; `UpdateCursorVisibilityAndScene()` (`00ea53ca`)
    - `UpdateSystemParameters(1)` to restore system params
    - `TerminateProcess(GetCurrentProcess(), 1)` — hard exit (avoids CRT teardown)

On initialization failure: `TerminateProcess(GetCurrentProcess(), 0)`.

## CLI Command Parser (`CLI_CommandParser_ParseArgs` `00eb787a`)
Parses `-name=value` tokens from `DAT_00c82b88` (command line copy) into an in-memory CLI::CommandParser object (allocated with tag "CLI::CommandParser", size 0xc bytes, at `DAT_00e6b328`). Stores name pointers at `this+0x480` (up to 0x20 entries) and value pointers at `this+0x500`. Positional arguments stored at `this+0x400`. Called before single-instance check.

## Registry Settings
All settings read via `ReadRegistrySetting` / `ReadRegistrySettingStr` which use `std::basic_string<char>` wrapper objects as parameters internally. Key read (via `FUN_0060ce60` for int, `FUN_0060ca20` for string).

All under `HKEY_CURRENT_USER\Software\Electronic Arts\Harry Potter and the Order of the Phoenix\GameSettings`:

### Graphics Settings
| Key | Address | Default | Notes |
|-----|---------|---------|-------|
| `Width` | `DAT_00bf1940` | 0 | Non-zero = fullscreen, value = width |
| `BitDepth` | `DAT_00bf1944` | 0 | Color depth (16/32) |
| `ShadowLOD` | `DAT_00bf1948` | 6 | Shadow quality level |
| `MaxTextureSize` | `DAT_00bf194c` | 0 | Max texture dimension |
| `MaxShadowTextureSize` | `DAT_00bf1950` | 0 | Max shadow texture dimension |
| `MaxFarClip` | `DAT_00bf1954` | 0 | Far clip distance |
| `CullDistance` | `DAT_00bf195c` | 0 | Object culling distance |
| `ParticleRate` | `DAT_00bf1958` | 0 | Particle density |
| `ParticleCullDistance` | `DAT_00bf1960` | 0 | Distance to stop drawing particles |
| `DisableFog` | `DAT_00bf1964` | 0 | Disable fog rendering |
| `DisablePostPro` | `DAT_00bf1968` | 0 | Disable post-processing |
| `FilterFlip` | `DAT_00bf196c` | 0 | Texture filter type |
| `AAMode` | `DAT_00bf1970` | 0 | Anti-aliasing quality (0=off) |
| `UseAdditionalModes` | `DAT_00bf1974` | 0 | Enable additional video modes |
| `OptionResolution` | `DAT_00bf197c` | 0 | Selected resolution option index |
| `OptionLOD` | `DAT_008ae1ec` | 1 | Level of detail option |
| `OptionBrightness` | `DAT_008ae1f0` | 5 | Brightness setting |
| `Mode0..5Width` | `DAT_009a78a0[6]` | 0 | Custom mode widths (6 entries) |
| `Mode0..5Height` | `DAT_009e4c90[6]` | 0 | Custom mode heights (6 entries) |

### Window Placement (windowed mode only, string values)
| Key | Default | Notes |
|-----|---------|-------|
| `PosX` | `"300"` | Window X position |
| `PosY` | `"32"` | Window Y position |
| `SizeX` | `"640"` | Window width |
| `SizeY` | `"480"` | Window height |
| `Maximized` | `"false"` | Restored via SW_MAXIMIZE (3) on startup |
| `Minimized` | `"false"` | Restored via SW_MINIMIZE (6) on startup |

### Fallback
- Reads HKCU first, then HKLM. If key found in HKLM, writes it back to HKCU (via `FUN_0060cc70`). If not found anywhere, creates in HKCU with the default value.
- `ReadRegistrySetting` takes `std::basic_string` arguments internally and uses C++ string objects throughout.

## Command Line Parsing (`ParseCommandLineArg` `00617bf0`)
Parses named flags from the saved command line (`DAT_00c82b88`):
- `fullscreen` — sets `DAT_008afbd9=1` (fullscreen mode), optionally reads a width value
- `widescreen` — sets `DAT_008ae1dc` to a widescreen aspect ratio constant
- `oldgen` — sets `DAT_008ae1ff=1` (in InitCLIAndTimingAndDevice, legacy renderer path)
- `showfps` — sets `DAT_00bef754=1` (in FinalizeDeviceSetup)
- `memorylwm` — sets `DAT_00bef6d7=1` (in FinalizeDeviceSetup, enables memory low-water-mark tracking)
- `nofmv` — sets FMV object flag (in InitEngineObjects, disables FMV playback)

Height is calculated as `ROUND(width / aspectRatio)` when fullscreen width is specified.

## System Parameters Management (`UpdateSystemParameters` `0060deb0`)
- **param_1 = 0** (disable): reads flags and clears bits 0 and 2-3 if bit 0 was already clear
- **param_1 = 1** (restore): writes back saved values from `DAT_008afc44/4c/54`

Modified parameters:
- `SPI_SETMOUSESPEED` (0x3b): mouse speed
- `SPI_SETMOUSE` (0x35): mouse acceleration
- `SPI_SETSCREENREADER` (0x33): screen reader flags

Flags storage: `DAT_008afc44` (mouseSpeed[2]), `DAT_008afc4c` (mouseAccel[2]), `DAT_008afc54` (screenReader[6])

## Window Management

### Window Class Registration (`RegisterWindowClass` `00eb4b95`)
- Class: `OrderOfThePhoenixMainWndClass`
- Style fullscreen: `CS_OWNDC | CS_DBLCLKS` (0x2020); windowed: adds `CS_VREDRAW | CS_HREDRAW` (0x2000)
- Background: black brush; cursor: arrow (windowed only — fullscreen skips cursor load)
- Calls `UnregisterClassA` first to handle crashed instances

### Window Creation (`CreateGameWindow` `0060db20`)
**Fullscreen:**
- Style: `WS_POPUP` (`0x80000000`)
- `SetWindowPos` with `HWND_TOPMOST` — always on top
- `SetMenu(hWnd, NULL)` — removes menu bar
- `SetThreadExecutionState(0x80000002)` — prevents sleep/screensaver (ES_CONTINUOUS | ES_DISPLAY_REQUIRED)

**Windowed:**
- Style: `WS_OVERLAPPEDWINDOW` (`0xcf0000`)
- Position from registry (default 300, 32)
- Size from registry (default 640×480)

### Window Procedure (`WindowProc` `0060d6d0`)
**All messages handled:**
- `WM_DESTROY (0x02)`: show cursor if fullscreen, `PostQuitMessage(0)`
- `WM_SIZE (0x05)`: return 0
- `WM_ERASEBKGND (0x14)`: return 0
- `WM_ACTIVATEAPP (0x1c)`: return 0
- `WM_ACTIVATE (0x06)`: complex focus management (see below)
- `WM_SETFOCUS/WM_SETCURSOR (0x07/0x20)`: cursor visibility; on losing focus calls `PauseGraphicsState()`
- `WM_PAINT (0x0f)`: fullscreen → `ValidateRect`; windowed → `BeginPaint/EndPaint`
- `WM_CLOSE (0x10)`: windowed only → `SaveWindowPlacement()` then `DestroyWindow`
- `WM_SYSCOMMAND (0x112)`: fullscreen blocks SC_MAXIMIZE, SC_SIZE, SC_MOVE, SC_KEYMENU
- `WM_NCHITTEST (0x84)`: fullscreen → `HTCLIENT`
- `WM_ENTERMENULOOP (0x120)`: fullscreen → return `0x10000`
- `WM_WTSSESSION_CHANGE (0x218)`: session connect(0) or lock(7) → return 1; else → return -1

**Focus Loss (WM_ACTIVATE, wParam==0, fullscreen):**
1. `PauseGraphicsState()` (`FUN_00617b60`) — pauses input via RealInputSystem vtable
2. `UnacquireInputDevices()`
3. `ShowCursor` loop until ≥ 1
4. `DAT_00bef6c7 = 1` (has-focus flag = lost focus)
5. `UpdateCursorVisibilityAndScene()` (`00ea53ca`) — switches render scene on cursor state change
6. If delayed-op timer is 0 (not already pending):
   - If audio state allows: `PauseAudioManager()` (`FUN_0061ef80`), set `DAT_00bef6d4`
   - If game update state allows: `PauseGameObjects(0)` (`FUN_0058b790`), set `DAT_00bef6d5`
7. `DAT_00bef6d8 = 0` (reset delayed timer)

**Focus Gain (WM_ACTIVATE, wParam≠0, fullscreen):**
1. Via `DAT_00e6b384` (RealInputSystem) at +0xc: calls vtable method to get device, acquires it
2. `AcquireInputDevices()`
3. `ShowCursor` loop until < 0
4. `UpdateCursorVisibilityAndScene()` (`00ea53ca`) — called with AL=0 (hidden cursor state)
5. `DAT_00bef6c7 = 0`
6. `DAT_00bef6d8 = 2000` (2-second delay before re-enabling updates)

### Window Placement Save (`SaveWindowPlacement` `0060d220`)
Saves position/state via `WriteRegistrySetting` (`0060c670`) on WM_CLOSE.

## UpdateCursorVisibilityAndScene (`00ea53ca`)
Called `thunk_FUN_00ea53ca` in original labeling. Compares new cursor-visible state (AL register) vs `DAT_00bef67e`.
If state changed:
- AL = 0 (cursor now hidden = focus gained): if `DAT_00c82b08 != DAT_00c82ac8` → `SwitchRenderOutputMode(&DAT_00c82b08)`
- AL ≠ 0 (cursor now shown = focus lost): if `DAT_00c82b00 != DAT_00c82ac8` → `SwitchRenderOutputMode(&DAT_00c82b00)`

Updates `DAT_00bef67e` to new state. Called in:
- WM_ACTIVATE focus-loss and focus-gain paths
- WinMain cleanup

## Main Game Loop (`MainLoop` `0060dc10`)
Runs until `WM_QUIT` received or `DAT_00bef6c5` exit flag is set.

**Per-iteration sequence:**
1. `UpdateDirectXDevice()` — check/recover D3D device
2. **Focus state** (fullscreen only):
   - No focus + cursor state changed (`DAT_00bef67e`): calls `SwitchRenderOutputMode` with scene IDs from `DAT_00c82b00/08/ac`
   - Has focus but flag says "lost": `UnacquireInputDevices()`, show cursor, `SwitchRenderOutputMode`
3. `PeekMessageA` (PM_REMOVE) — if `WM_QUIT (0x12)`: return `wParam`
4. No messages:
   - Compute frame budget using constants at `DAT_008475d8` (1/65536), `DAT_00845594` (0.001), `DAT_00845320` (1000.0)
   - `GameFrameUpdate()`
   - If `DAT_00bef6d7` (game update enabled / `memorylwm` flag): lazy-init `InitFrameCallbackSystem()` + `QueryMemoryAllocatorMax()` tracking `g_nMinFreeMemory` in `DAT_008afb08`
   - Manage `DAT_00bef6d8` delayed-op countdown; on expiry: optionally `AudioStream_Resume()` (`00ec67e8`) and `ResumeGameObjects()` (`FUN_0058b8a0`)
   - Frame rate cap: if frame took < budget, `Sleep(0)`

## Timing System

### GetGameTime (`00618010`)
High-resolution timer using Windows multimedia API:
```
timeGetDevCaps(caps, 8)
timeBeginPeriod(caps.wPeriodMin)
t = timeGetTime()
timeEndPeriod(caps.wPeriodMin)
```
- First call captures `_DAT_00e6e5e8` (startup base time)
- Returns `t - startupBase` — milliseconds since game start
- `DAT_00e6b388`: one-time init flag

### GameFrameUpdate (`00618140`)
64-bit fixed-point timing:
1. `ProcessDeferredCallbacks()` — run deferred callback queue (2ms budget)
2. `GetGameTime()` → convert to 16.16 fixed point: `high = t >> 16`, `low = t << 16`
3. Init on first call: `DAT_00e6e5e0/e4 = (low, high)`
4. Delta = `(low, high) - (DAT_00e6e5e0, DAT_00e6e5e4)`, capped at `0x640000` (100ms)
5. Accumulate: `(DAT_00c83198, DAT_00c8319c) += delta`
6. `DAT_00c83110 = ((accum * 3) / 0x10000)` — game ticks (3x speed)
7. If accumulated time ≥ next callback time (`DAT_00c831a8/ac`):
   - Advance callback time by interval (`DAT_00c83190/94`)
   - Toggle `DAT_00c83130` (double-buffer flip index, used by UpdateFrameTimingPrimary)
   - Call `UpdateFrameTimingPrimary(&localTick)` and `(*DAT_008e1644[0])(&localTick)`
8. Check secondary callback, call `InterpolateFrameTime` and `(*DAT_008e1644[1])()`

### UpdateFrameTimingPrimary (`00617f50`)
Called when accumulated time exceeds callback interval. Stores timing in double-buffer:
- Increments `DAT_00c83114` (tick counter) by `localTick * 3` if DAT_00bef768+4 == 0
- Toggles `DAT_00c83130` (flip index)
- `DAT_00c83128[flip]` = localTick; `DAT_00c83120[flip]` = game time
- Uses constants `DAT_00845594` (0.001) and `DAT_00845320` (1000.0) for the tick computation

### InterpolateFrameTime (`00617ee0`)
Smooth interpolation between double-buffered frame values:
```
t = (currentTick - prevTick) / (currTick - prevTick)
result = prevTime + (currTime - prevTime) * t
```

### Key timing globals
| Address | Name | Purpose |
|---------|------|---------|
| `DAT_00e6e5e0/e4` | last frame time | 64-bit microseconds (16.16 fixed) |
| `DAT_00c83198/9c` | accumulated time | 64-bit |
| `DAT_00c83110` | game ticks | = accum*3/0x10000 |
| `DAT_00c83114` | tick counter | incremented in primary callback |
| `DAT_00c831a8/ac` | next callback time | 64-bit |
| `DAT_00c83190/94` | callback interval | 64-bit |
| `DAT_00c83130` | frame flip index | 0 or 1, toggled each callback |
| `DAT_00c83128` | timing double-buffer (ticks) | 2×4 bytes indexed by flip |
| `DAT_00c83120` | timing double-buffer (time) | 2×4 bytes indexed by flip |
| `DAT_008e1644` | callback table ptr | function pointer array [primary, secondary] |
| `DAT_00845594` | float const | 0.001 (1/1000) — timing scale |
| `DAT_00845320` | float const | 1000.0 — timing scale |
| `DAT_008475d8` | float const | 1/65536 — 16.16 fixed-point denominator |

### Deferred Callback Queue (`ProcessDeferredCallbacks` `00636830`)
Linked list at `DAT_00bef7c0`. Each node has next pointer at `+0x7c`.
- Per frame: processes as many nodes as possible within 2ms
- `BuildRenderBatch(node)` returns 1 when the node's work is done; node is then removed
- Time checked using same `timeGetTime()` / `timeBeginPeriod` pattern as GetGameTime

## DirectX Initialization Chain

### InitDirectXAndSubsystems (`00eb612e`, `__thiscall`)
Called from WinMain with client height. Returns OR of sub-init failures (0 = success):
1. `InitCLIAndTimingAndDevice(height, ?, hWnd)` (`00614370`) — creates CommandParser, resets timing, creates D3D device
2. `InitEngineObjects()` (`0060c2e0`) — allocates engine subsystem objects
3. `DAT_008df65a = 1`, `DAT_00aeea5c = 2` — subsystem state flags
4. `FinalizeDeviceSetup()` (`006147f0`) — sets callback table, registers message handlers
5. `InitAudioSubsystem()` (`00eb60d3`) — opens audio device (async polling)

### InitCLIAndTimingAndDevice (`00614370`)
1. Allocates CLI::CommandParser (`AllocEngineObject(0xc, "CLI::CommandParser")`) → `DAT_00e6b328`
2. One-shot InitFrameCallbackSystem guard (`_DAT_00e74c20 & 1`)
3. Resets timing globals to zero: `DAT_00c83110/14/18/1c/28/c/20/4/30 = 0`
4. `FUN_0064ae00(&DAT_00c8e490)` — initializes render batch system
5. `FUN_0067c290(height, ?, 0, 0, 6, 1, 1, 1, 1)` — creates the D3D device
6. On D3DERR (-0x7fffbffb): `ShowCursor(1)` (device lost during init)
7. `ParseCommandLineArg("oldgen", ...)` — legacy renderer flag
8. `(*(*DAT_00c8c580))(0, 0)` — initial render callback dispatch

### InitEngineObjects (`0060c2e0`)
Allocates and wires up core engine subsystem objects:
- `GlobalTempBuffer` (0x3c bytes) → `DAT_00e6b378`
- `RealGraphSystem` (8 bytes) → `DAT_00e6b390`; vtable `PTR_FUN_00885010`
- `RealInputSystem` (0x34 bytes) → `FUN_00617890()` which sets `DAT_00e6b384` (the input manager)
- `Locale` (0x8c bytes) → `DAT_00e6b304`; handles localization
- `FMV` (0x18 bytes) → `DAT_00e6b2dc`; checks `nofmv` flag
- Calls `thunk_FUN_00eb87ba`, `thunk_FUN_00eb88b2`, `FUN_006677c0` for additional subsystems

### FinalizeDeviceSetup (`006147f0`)
Sets `DAT_008e1644 = &PTR_PTR_008d0f94` (frame callback table). Registers message handlers:
- `iMsgDeleteEventHandler`, `iMsgDeleteEntity`, `iMsgOnDeleteEntity`
- `iMsgDoRender`, `iMsgDoRenderDirectorsCamera`, `iMsgPreShowRaster`
- `iMsgStartSystem`, `iMsgStopSystem`
Sets `DAT_00bef750 = 1` (device ready), `DAT_00bef754` = `showfps` flag, `DAT_00bef6d7` = `memorylwm` flag.
Also calls `FUN_0060b740()` which creates `GameServices::Open::Create` and `TimeManager::Instance` objects:
- `DAT_00bef6c8` = GameServices::Open object (1 byte)
- `DAT_00bef768` = TimeManager::Instance object (8 bytes, via `thunk_FUN_00eb797e()`)

### PreDirectXInit (`00ec64f9`)
Sets up the audio/render subsystem before DirectX device creation:
- `DAT_00bf1b18 = DAT_00bef6d0` (passes the engine factory object to the audio system)
- `DAT_00bf1b10 = 0` (clears audio-present flag; set non-zero by hardware detection in FUN_006ac0b0/thunk_FUN_00ec6e91)
- Copies audio device string from `.rodata` (0x7d3ddb) into `DAT_00be93d0`
- `FUN_006ac0b0()`, `thunk_FUN_00ec6e91()` — audio hardware detection/init
- `DAT_00bf1b1c = thunk_FUN_00ec72a9()` — creates audio output context
- `DAT_00bf1b20 = FUN_00611940(DAT_00bf1b1c)` — creates a buffer or thread handle
- One-shot InitFrameCallbackSystem guard

### InitAudioSubsystem (`00eb60d3`)
Checks `DAT_00bf1b10` (audio present). If non-zero:
- `FUN_006a9080()` — open audio device (async: polls `FUN_006109d0()` + `SleepEx(0,1)`)
- `FUN_006a91a0()` — query format/caps (expects 0x80 result)
- `FUN_006a9140(device, 0)` — configure audio output
- `FUN_006a90e0()` — start audio stream
All audio operations use async SleepEx-polling pattern via `DAT_00be82ac` command queue.

## RealInputSystem (`DAT_00e6b384`)
The main input manager, allocated in `InitEngineObjects` via `FUN_00617890`:
- Vtable: `PTR_FUN_00884ff4`
- Field `+0xc`: pointer to sub-object (device manager)
- On sub-object vtable +0x10: returns device interface
- On device interface +0x218: the `IDirectInputDevice8*`
- On that device vtable +0x20: Pause/Unacquire method

Used in:
- `PauseGraphicsState` (`00617b60`) to pause input on focus loss
- `WM_ACTIVATE` focus-gain path to re-acquire input
- `WinMain` cleanup to pause input before exit

## DirectX Device Management

### UpdateDirectXDevice (`DirectX_UpdateDevice` `0067d310`)
1. Calls `PreDeviceCheck()` (`0067d2e0`) — queries texture mem, handles low-mem
2. `IDirect3DDevice9::TestCooperativeLevel` (vtable +0xc)
3. On `D3DERR_DEVICELOST (-0x7789f798)`: sets `DAT_00bf18aa=1`, sleeps 50ms
4. On `D3DERR_DEVICENOTRESET (-0x7789f797)`: calls `ReleaseDirectXResources()`, then `Reset(&DAT_00b94af8)`, then `RestoreDirectXResources()`
5. On unexpected error code: calls `FatalError("Invalid device lost state %d\n", hr)` — does not return
6. On success: `DAT_00bf18aa = 0`

### ReleaseDirectXResources (`0067cfb0`)
1. Release `DAT_00b95034` (GPU sync query) if non-null
2. `thunk_FUN_00ec04dc()` — unknown pre-release cleanup
3. Clear `DAT_00af1390`, `DAT_00ae9250` (cached surface pointers)
4. If `DAT_00bf1934 != NULL && DAT_00bf1934 != 0xbacb0ffe`:
   - Get back buffer from `DAT_00bf1924` (via vtable +0x14)
   - `SetCachedRenderTargets(surface, 0)` — unregisters from cache
   - Release surface and `DAT_00bf1934`
5. Release `DAT_00bf1930` (back buffer) if non-null
6. Release `DAT_00bf1938` (additional render target) if non-null
7. `thunk_FUN_00ec19b5()` — post-release cleanup

### RestoreDirectXResources (`0067d0c0`)
1. `InitRenderStates()` (`00675950`) — uploads shaders; extended path if `DAT_00bf1994 > 2`
2. If `DAT_00b95034 == NULL`: `CreateGPUSyncQuery()` (`0067b820`)
3. `FUN_0067bb20()` — empty stub in current analysis
4. Get back buffer via `DAT_00bf1924` vtable +0x14 → `DAT_00bf1930`; cache in `DAT_00af1390`
5. Get back buffer surface description
6. `CreateTexture` (vtable +0x74) for additional render target → `DAT_00bf1938`
7. If `DAT_00bf1970 == 0` (AAMode == off) and HW supports it (caps.TextureOpCaps & 0x80 check):
   - `CreateTexture` (vtable +0x5c) with same dimensions → texture
   - Release old `DAT_00bf1930`, get `GetSurfaceLevel(0)` → `DAT_00af1390`
   - `DAT_00bf1934 = texture`; `DAT_00bf1930 = DAT_00af1390`
8. Else: `DAT_00bf1934 = 0xbacb0ffe` (sentinel - AA path)
9. `DAT_00ae9250 = DAT_00bf1938`
10. `SetRenderTarget(0, DAT_00af1390)` and depth stencil
11. `InitD3DStateDefaults()` (`00674430`)

### SetCachedRenderTargets (`0067ecf0`)
Called `FUN_0067ecf0` in older labels. Avoids redundant `SetRenderTarget`/`SetDepthStencilSurface` calls. Caches in `DAT_00af1390` / `DAT_00ae9250`. Uses vtable offsets +0x94 and +0x9c on device.

### DirectX resource globals
| Address | Purpose |
|---------|---------|
| `DAT_00bf1920` | `IDirect3DDevice9*` |
| `DAT_00bf1924` | `IDirect3D9*` or swap chain object |
| `DAT_00bf1930` | Back buffer `IDirect3DSurface9*` |
| `DAT_00bf1934` | Render target texture or `0xbacb0ffe` sentinel |
| `DAT_00bf1938` | Additional render target `IDirect3DSurface9*` |
| `DAT_00af1390` | Cached render target (mirrors `DAT_00bf1930`) |
| `DAT_00ae9250` | Cached depth-stencil surface |
| `DAT_00b95034` | GPU sync query object (event query) |
| `DAT_00bf18aa` | Device-lost flag |
| `DAT_00bf1970` | AAMode (0 = AA path active) |
| `DAT_00b94af8` | Saved `D3DPRESENT_PARAMETERS` for Reset |
| `DAT_00b94748` | Render state cache array (indexed by D3DRENDERSTATETYPE) |
| `DAT_00bf1994` | Shader/feature level capability index (> 2 = extended) |
| `DAT_00bf193c` | Available texture memory in MB (updated by PreDeviceCheck) |
| `DAT_00bf1b10` | Audio present flag (checked in InitAudioSubsystem) |

### D3D State Defaults (`InitD3DStateDefaults` `00674430`)
Initializes render state, sampler state, and texture stage state defaults. Clears dirty flags `DAT_00bf18a8/ac/c0/c4`. Fills `DAT_008d3878` (0x4c bytes) with 0xcd sentinel. Sets fog defaults (`_DAT_00d7ea68=5`). Conditionally adjusts blend states if `!(caps.TextureOpCaps & 0x80)` or `DAT_00bf1980 == 0x10` (16-bit colour mode). Calls `thunk_FUN_00ebfe83()` and `SetCachedRenderState(0x19, 8)`.

### GPU Sync Query (`CreateGPUSyncQuery` `0067b820`)
Creates `D3DQUERYTYPE_EVENT` (0xe0) query on device vtable +0x68 with flags `DAT_00bf19ac | 0x208`. Swap effect: `(DAT_008ae1fc != 0) ? 0 : 2`. Stored in `DAT_00b95034`.

### Video Memory Monitor (`PreDeviceCheck` `0067d2e0`)
Calls `GetAvailableTextureMem` (vtable+0x10). Stores result >> 20 (MB) in `DAT_00bf193c`. If 0 < result < 33 MB, calls `thunk_FUN_00ebe85b()` (low texture memory handler).

## DirectInput Management

### RealInputSystem Init (`FUN_00617890`)
Sets up the `RealInputSystem` object at `DAT_00e6b384`:
- Vtable: `PTR_FUN_00884ff4`; field `+0x8` = 0x20; copies string "RealInputSystem::RealInputSystem" to `DAT_00bef6e8`
- One-shot `InitFrameCallbackSystem` guard
- This is where `DAT_00e6b384` is assigned

### Input Devices
- `DAT_00e6a070`: `IDirectInputDevice8*` keyboard
- `DAT_00e6a194`: `IDirectInputDevice8*` mouse
- `DAT_00e6a42c`: first joystick entry; stride = 0x248 bytes; 2 joysticks

### AcquireInputDevices (`0068da30`) / UnacquireInputDevices (`0068dac0`)
Acquire/unacquire all devices when gaining/losing focus.
Fatal error via `FUN_0066f810` if any device fails.
Also calls `Ordinal_5(1/0)` — custom cursor show/hide.

## Audio System

### AudioStream Pause/Resume
Both functions use `in_EAX` as `this` (audio stream/track object):
- `this+0x1c` = audio timer pointer
- `this+0x28` = pause flag (set to 1 on pause, 0 on resume)
- `this+0x16` = pause timestamp (set on pause to GetGameTime())
- `this+0x17` = cumulative playback time (updated on resume)
- On pause: calls `thunk_FUN_00ec693d(0)`, `thunk_FUN_00ec66f1()`
- On resume: calls `thunk_FUN_00ec693d(0x1000)`, `thunk_FUN_00ec66f1()`
- Pitch correction: if `piVar2[4] == 0x1000` (normal pitch), direct time addition; else scaled by pitch/0x1000

### Focus Loss (WM_ACTIVATE, focus-loss path):
1. `PauseAudioManager()` (`FUN_0061ef80`) — calls `AudioStream_Pause()` (`FUN_006a9ea0`) on active track
2. Resumed after 2-second delay via `AudioStream_Resume()` (`00ec67e8`) when timer expires

## Game State and Subsystem Init

### Frame Callback System (`InitFrameCallbackSystem` `00612f00`)
Initializes the frame callback singleton at `DAT_00e6e870`:
- `DAT_00e6e870 = &PTR_FUN_00883f3c` (primary callback entry)
- `DAT_00e6e874 = &PTR_FUN_00883f4c` (secondary callback entry)
- `_DAT_00bef728 = &DAT_00e6e870` (pointer to the table, = `DAT_008e1644`)
- Clears `DAT_00e6e880..e6e89c` (8 callback slots)
- Called multiple times via one-shot guard (`_DAT_00e74c20 & 1`)

### GetOrInitCallbackManager (`00eb5c3e`)
Returns `&DAT_00e6e870` (the callback manager singleton pointer). One-shot init guard at `_DAT_00e74c20 & 1`. This is NOT a COM object creator; it returns the callback table base address.

### Engine Object Allocator (`FUN_00614210`)
Factory function with signature `AllocEngineObject(size, tagName)`. Creates heap objects with debug tags. Used throughout init to allocate subsystem objects.

### InitGameSubsystems (`00eb496e`)
Sets `_DAT_00e69ca0 = FUN_0060c130` (registers a callback). Enumerates DirectInput devices via `FUN_00688370(4, 0)`. Loads the language/localization selection screen via `FUN_005090a0("LanguageSelect")`. Returns 1 on success.

### Pause/Resume System
- **Focus loss**: `PauseAudioManager()` (`FUN_0061ef80`) + `PauseGameObjects(0)` (`FUN_0058b790`)
- **Resume**: triggered by `DAT_00bef6d8` expiry:
  - `AudioStream_Resume()` (`00ec67e8`) resumes audio
  - `ResumeGameObjects()` (`FUN_0058b8a0`) resumes game objects

### Game Object Pause State Machine (`DAT_00c7b908`)
Controls which systems are paused (values 0–7):
- **State 0 (full pause)**: "chapter-cutscene-in" animation, vtable+0x24 (Pause) on all systems. Records pause time.
- **States 4–5 (audio-only pause)**: pauses `DAT_00c7c370` (audio manager) only, transitions to state 6.
- **State 6**: fully paused, no-op.
- **State 7 (resuming)**: "default" animation, vtable+0x28 (Resume) on all systems.

Pausable systems: `DAT_00c7c370` (audio/music manager), `DAT_00c7b924` (secondary system), array at `DAT_00c7ba4c` (stride 0x12×4), additional systems at `DAT_00c7d038+0xc` (stride 0xb×4).

### Deferred Render Batch Queue
Linked list at `DAT_00bef7c0` (processed by `ProcessDeferredCallbacks`). Each node is a draw call descriptor. `BuildRenderBatch` (`0063d600`) processes one node per call, building entries in the material table. Recognized shader types: `BLOOM`, `GLASS`, `BACKDROP`.

### Memory Pressure Tracking
`QueryMemoryAllocatorMax` (`00eb6dbc`) uses a critical section at `allocator+0x4e4`. Scans a priority-indexed free list (`allocator+0x428`, 0xfe entries) and a linked list at `allocator+0x30..0x3c`. Values masked to `& 0x7ffffff8` (8-byte alignment). Returns the largest free block size. Minimum across frames tracked in `DAT_008afb08`.

## RenderAndAudioTeardown (`00ec6610`)
Called on exit before input cleanup:
1. If `DAT_00bf1b1c` set: calls `thunk_FUN_00eb584e()`, `FUN_006119c0()`, clears `DAT_00bf1b18/1c`
2. `FUN_006ace30()` — audio teardown (1)
3. `FUN_006108c0()` — audio teardown (2)
4. `FUN_006ac930()` — audio teardown (3)
5. If `DAT_00bf1b2c` set: closes handle `DAT_00bf1b34` (renderer thread?), clears `DAT_00bf1b30/2c`

## Global State Flags
| Address | Name | Purpose |
|---------|------|---------|
| `DAT_00bef6c5` | exit flag | Non-zero = exit MainLoop |
| `DAT_008afbd9` | fullscreen | Boolean fullscreen mode |
| `DAT_00bef6c7` | focus lost | 1 = window does not have focus |
| `DAT_00bef67e` | cursor state | Current cursor visibility state |
| `DAT_00bef67c` | unknown | Checked in UpdateCursorVisibilityAndScene |
| `DAT_00bef67d` | unknown | Checked in UpdateCursorVisibilityAndScene |
| `DAT_00bef6d8` | delayed op timer | ms countdown; expires → resume audio/updates |
| `DAT_00bef6d7` | memorylwm flag | Enabled by cmdline 'memorylwm'; enables memory tracking |
| `DAT_00bef6d4` | audio paused | Audio was paused on focus loss |
| `DAT_00bef6d5` | updates paused | Physics/updates paused on focus loss |
| `DAT_00bef6c6` | subsys init flag | Set to 1 after InitGameSubsystems |
| `DAT_00bef750` | device ready | Set to 1 in FinalizeDeviceSetup |
| `DAT_00bef754` | showfps | Enabled by cmdline 'showfps' |
| `DAT_00bef755` | unknown | Set to 1 in InitCLIAndTimingAndDevice |
| `DAT_00bef768` | TimeManager | Pointer to TimeManager::Instance (8 bytes); checked in UpdateFrameTimingPrimary |
| `DAT_00bef6c8` | GameServices | Pointer to GameServices::Open object |
| `DAT_00bf1b10` | audio present | Non-zero if audio hardware detected; checked in InitAudioSubsystem |
| `DAT_00bf1b14` | audio init flag | Set to 1 in PreDirectXInit |
| `DAT_00bf1b18` | engine obj ref | Copy of DAT_00bef6d0, stored in audio subsystem |
| `DAT_00bf1b1c` | audio context | Created by thunk_FUN_00ec72a9; checked in RenderAndAudioTeardown |
| `DAT_00bf1b20` | audio buffer | Created by FUN_00611940(DAT_00bf1b1c) |
| `DAT_00bf1b30` | audio thread id | Thread ID for audio pump; checked in FUN_006ac010 |
| `DAT_00bf1b2c` | audio thread active | If set: close handle DAT_00bf1b34 on teardown |
| `DAT_00c82b08` | scene ID | Checked against DAT_00c82ac8 in render mode switch |
| `DAT_00c82b00` | scene ID 2 | Alternate scene ID for render mode switch |
| `DAT_00c82ac8` | target scene | Target scene ID for SwitchRenderOutputMode |
| `DAT_00e6b384` | RealInputSystem | Main input manager object pointer |
| `DAT_00c7b908` | pause state | State check before PauseGameObjects |
| `DAT_008afb08` | min memory | Minimum available video memory across frames |
| `DAT_00bf192c` | fullscreen flag 2 | Set to 1 in fullscreen path |
| `DAT_00bef6cc` | window handle | Main HWND |
| `DAT_00bef6d0` | engine object | Created via callback factory in WinMain, 0xb58 bytes |

## Key Components
- **Window Management**: Full Win32 with fullscreen/windowed, placement persistence, session change handling
- **Configuration**: Registry (HKCU/HKLM fallback, std::basic_string internally) with extensive settings, command-line override
- **Rendering**: DirectX 9, device lost recovery, dual render targets, GPU sync query, D3D state tables
- **Input**: RealInputSystem (DAT_00e6b384), DirectInput 8, keyboard/mouse/2 joysticks, acquire/unacquire on focus change
- **Timing**: High-resolution multimedia timer, 16.16 fixed point, 3× game speed multiplier, double-buffered callbacks
- **Audio**: Async init, pause on focus loss, 2-second delayed resume, pitch-corrected seek on resume
- **Message Loop**: Integrated with game update, frame limiter, deferred callback queue
- **Engine Objects**: Allocator with debug tags, message handler registration, CLI CommandParser
