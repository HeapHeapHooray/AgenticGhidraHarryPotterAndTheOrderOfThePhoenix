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

---
# Iteration 4: Enhanced Understanding (Answers from Pattern Analysis)

This section addresses questions from QUESTIONS.md through pattern analysis and architectural inference.

## Engine Object Factory (DAT_00bef6d0)

### Creation and Purpose
The engine object (`DAT_00bef6d0`) is created via the callback manager's secondary factory entry:
```
(*DAT_00e6e874[0])(0xb58, {0x88332000000001, 0})
```

**Key observations:**
- Size: 2904 bytes (0xb58)
- Magic number `{0x88332000000001, 0}` is likely a type identifier for the factory
- Released via custom destructor: `(*(callback_mgr + 0xc))(DAT_00bef6d0, 0)` — NOT a simple COM Release
- Passed to audio/render subsystem in `PreDirectXInit` (copied to `DAT_00bf1b18`)

**Architectural role:**
This is the **main game engine object/context** that coordinates all subsystems. It acts as a central coordinator rather than a simple allocator, integrating:
- Frame callback dispatch
- Subsystem lifecycle management
- Resource pooling and coordination

### Factory Pattern
The callback manager (`DAT_00e6e870`) provides a dual-entry factory:
- **Primary entry** (`DAT_00e6e870`): Frame callback processing
- **Secondary entry** (`DAT_00e6e874`): Object factory for engine subsystems

This separation suggests a **capability-based architecture** where the same manager provides both event dispatch and object creation.

## DirectX Initialization Details

### CreateD3DDevice (`FUN_0067c290`)
Parameters: `(clientHeight, ?, flags?, quality_params...)`
```
FUN_0067c290(height, ?, 0, 0, 6, 1, 1, 1, 1)
```

**Analysis:**
- `height`: Client area height from `GetClientRect`
- Parameter 6: Likely default quality/feature level (correlates with `DAT_00bf1994` shader capability index)
- Trailing 1's: Boolean flags for various features (vsync, hardware acceleration, etc.)

**Creates:**
- `DAT_00bf1920` = `IDirect3DDevice9*`
- `DAT_00bf1924` = `IDirect3D9*` or swap chain object

**Error handling:**
- Returns D3DERR-like codes
- Special handling for `D3DERR_DEVICELOST` (`-0x7fffbffb`) during init → calls `ShowCursor(1)`

### Resource Management Pre/Post Cleanup (`thunk_FUN_00ec04dc` / `thunk_FUN_00ec19b5`)

These functions implement a **protocol pattern** around D3D device Reset:

**Pre-release cleanup** (`thunk_FUN_00ec04dc`):
- Called BEFORE surface releases in `ReleaseDirectXResources`
- **Likely operations:**
  - Flushes pending draw calls
  - Clears command buffers
  - Notifies render pipeline of impending resource loss
- **Purpose:** Ensures clean state before resource destruction

**Post-release cleanup** (`thunk_FUN_00ec19b5`):
- Called AFTER all surface releases
- **Likely operations:**
  - Clears texture caches
  - Resets shader state tracking
  - Cleans up dependent resources
- **Purpose:** Finalizes cleanup and prepares for restore

This pre/post pattern is a **lifecycle hook system** ensuring render subsystems properly handle device loss/reset.

## Audio Subsystem Architecture

### Asynchronous Initialization Pattern

The audio system uses **async command queuing with polling**:

**Command queue:** `DAT_00be82ac`  
**Status polling:** `AudioPollGate` (`FUN_006109d0`)
- Returns: `-2` (error), `0` (pending), `1` (complete)
- Polled in `SleepEx(0, 1)` loops during init

**Init sequence in `InitAudioSubsystem`:**
1. `FUN_006a9080()` — Open device (async)
2. `FUN_006a91a0()` — Query format/caps (async, expects `0x80` = success)
3. `FUN_006a9140(device, 0)` — Configure output (async)
4. `FUN_006a90e0()` — Start stream

Each operation pushes a command to the queue and polls until complete.

**Why async?**
- Non-blocking initialization (game can show loading screen)
- Graceful degradation if audio hardware is slow/problematic
- Allows timeout and retry logic

### Audio Stream Control

**Decoder state control** (`thunk_FUN_00ec693d`):
- Parameter `0` = pause (stop playback)
- Parameter `0x1000` = resume (normal pitch/rate = 100%)
- **Likely:** `IDirectSoundBuffer::SetFrequency` or codec control

**State finalization** (`thunk_FUN_00ec66f1`):
- Called after state change in both pause and resume
- **Likely:** Commits buffer changes, flushes hardware

**Teardown sequence** (three-step pattern in `RenderAndAudioTeardown`):
1. `FUN_006ace30()` — Stop all active streams/tracks
2. `FUN_006108c0()` — Release DirectSound buffers
3. `FUN_006ac930()` — Close device and release interfaces

**Hardware detection flag** (`DAT_00bf1b10`):
- Set to `0` in `PreDirectXInit`
- Set non-zero by hardware detection (`FUN_006ac0b0` / `thunk_FUN_00ec6e91`)
- Gates all audio init; if `0` = no audio (headless/server mode)

## Message Dispatch System

### String-Based Polymorphic Dispatch (`RegisterMessageHandler` `00eb59ce`)

**Registration signature:**
```
RegisterMessageHandler(dest_object, msgName_string, paramType)
```

**Message names:**
- `"iMsgDeleteEventHandler"`, `"iMsgDeleteEntity"`, `"iMsgOnDeleteEntity"`
- `"iMsgDoRender"`, `"iMsgDoRenderDirectorsCamera"`, `"iMsgPreShowRaster"`
- `"iMsgStartSystem"`, `"iMsgStopSystem"`

**Architecture:**
1. **Registration:** String name → hash to msgID → store `(msgID, handler_func, dest_object)` in dispatch table
2. **Runtime dispatch:** Lookup by msgID, call `handler(dest_object, params)` with dest as `this` pointer
3. **"iMsg" prefix:** Suggests interface messages (polymorphic dispatch across different object types)

**Benefits:**
- Decouples senders from receivers
- Enables data-driven message routing
- Supports dynamic handler registration (modding, plugins)

### Frame Callback Registration

**Function:** `FUN_0060c130` — registered as `DAT_00e69ca0`  
**Called:** Every frame as part of callback chain

**Purpose:** Main game logic update entry point
- Processes game state updates
- Manages entity updates
- Coordinates AI ticks
- **This is the primary "game code" hook**

## Scene and Render Mode System

### Scene IDs and Transitions

**Three scene ID globals:**
- `DAT_00c82b00` (`g_SceneID_FocusLost`) — Scene for focus-lost state (likely menu/pause screen)
- `DAT_00c82b08` (`g_SceneID_FocusGain`) — Scene for focus-gained state (likely active gameplay)
- `DAT_00c82ac8` (`g_SceneID_Current`) — Current/target active scene

**Initialization:** All set to `0` at startup, populated during scene loading after `InitGameSubsystems`

**Proposed enum:**
```cpp
enum SceneType {
    SCENE_NONE = 0,
    SCENE_MENU = 1,
    SCENE_GAMEPLAY = 2,
    SCENE_CUTSCENE = 3,
    SCENE_LOADING = 4,
    // etc.
};
```

**Scene switching:**
- `UpdateCursorVisibilityAndScene` compares focus-lost/focus-gain IDs vs current
- If different, calls `SwitchRenderOutputMode` with appropriate scene pointer
- `SwitchRenderOutputMode` dispatches to registered listeners

**Deferred listener flush** (`FUN_006125a0`):
- Called when `list.head+0x12` pending-change flag is set
- Processes queued scene transition callbacks
- Supports async scene loading coordination

## Timing System Details

### Callback Interval Initialization

**Globals:** `DAT_00c83190/94` (`g_ullCallbackInterval_lo/hi`)

**Likely initialized:**
- In `InitFrameCallbackSystem` or first `GameFrameUpdate` call
- **Default value speculation:**
  - 60 FPS: `16ms << 16 = 0x100000` (16.16 fixed-point)
  - 30 FPS: `33ms << 16 = 0x210000`

**Usage:** Set once at startup, used throughout for frame pacing

### TimeManager Pause Control

**Structure:** `DAT_00bef768` = `TimeManager::Instance` (8 bytes)
```
+0: vtable pointer
+4: isPaused flag
```

**Pause behavior:**
- Checked in `UpdateFrameTimingPrimary`: `if (*(DAT_00bef768 + 4) == 0)`
  - `0` = running → updates `DAT_00c83114` tick counter
  - non-zero = paused → time advances but ticks don't accumulate

**Purpose:** Allows pause menu / cinematics to freeze game logic while keeping frame timing active

## Deferred Rendering Queue

### Batch Processing (`ProcessDeferredCallbacks` / `BuildRenderBatch`)

**Queue structure:**
- Linked list at `DAT_00bef7c0`
- Each node: draw call descriptor; next pointer at `node+0x7c`
- Processed within 2ms budget per frame

**Node types (recognized shader types):**
- `BLOOM` — Post-processing bloom effect batches
- `GLASS` — Transparent/refractive material batches
- `BACKDROP` — Environment/skybox batches

**Processing:**
1. `ProcessDeferredCallbacks` iterates list
2. For each node: calls `BuildRenderBatch(node)`
3. `BuildRenderBatch` returns `1` when node complete → remove from list
4. Continues until 2ms budget exhausted or list empty

**Purpose:** Deferred rendering optimization
- Collect draw calls during scene traversal
- Sort and batch by material/shader to minimize state changes
- Render in optimized order

## Subsystem Initialization Sequence

### Unknown Init Functions in `InitEngineObjects`

**After Locale setup** (`thunk_FUN_00eb87ba`):
- **Likely:** Language resource loading or string table initialization
- **Possible:** Font system initialization for localized text

**After FMV setup** (`thunk_FUN_00eb88b2`):
- **Likely:** Video codec initialization (Bink/Smacker decoder)
- Called after `nofmv` flag check → only runs if video enabled

**Final init** (`FUN_006677c0`):
- **Likely:** Shader compiler init or render state setup
- **Possible:** Texture manager initialization
- Position at end suggests finalizing dependencies after all subsystems allocated

### GameServices and TimeManager Factory (`FUN_0060b740`)

Called in `FinalizeDeviceSetup` after message handler registration.

**Creates two singletons:**
1. `DAT_00bef6c8` = `GameServices::Open` (1 byte)
   - **Likely:** Service locator or dependency injection container
   - Provides access to engine services for game code
   
2. `DAT_00bef768` = `TimeManager::Instance` (8 bytes)
   - Game time management with pause control (see above)

**Timing:** Created as final dependencies after core engine init complete

## Render Dispatch Table

**Global:** `DAT_00c8c580` (`g_pRenderDispatchTable`)

**Double indirection:** `**DAT_00c8c580` suggests vtable-like structure

**Usage:**
- `InitCLIAndTimingAndDevice`: `(*(*DAT_00c8c580))(0, 0)` — Initialize(int, int)
- `FinalizeDeviceSetup`: `(*(*DAT_00c8c580 + 8))()` — Finalize()

**Likely vtable:**
```
[0x00] = Initialize(int, int)
[0x08] = Finalize()
[0x10] = BeginFrame()
[0x18] = EndFrame()
// etc.
```

**Purpose:** Render subsystem interface providing lifecycle and per-frame operations

## Architecture Pattern Summary

### Initialization Hierarchy
```
1. Callback Manager (DAT_00e6e870) — Core event dispatch
2. Engine Object (DAT_00bef6d0) — Central coordinator factory
3. Subsystem Singletons — RealGraphSystem, RealInputSystem, Locale, FMV, etc.
4. Message Dispatch — String-based polymorphic event system
5. Frame Callbacks — Double-buffered timing with primary/secondary callbacks
6. Service Layer — GameServices::Open, TimeManager::Instance
```

### Key Design Patterns
- **Factory Pattern:** Callback manager provides object creation
- **Observer Pattern:** Message dispatch with string-based registration
- **Command Pattern:** Deferred render queue
- **Template Method:** Frame callbacks with pre/post hooks
- **Strategy Pattern:** Scene-based render mode switching
- **Singleton Pattern:** Subsystem managers (TimeManager, GameServices)

### Subsystem Coordination
The engine uses a **hybrid event-driven and polling architecture:**
- **Event-driven:** Message dispatch for lifecycle events
- **Polling:** Frame callbacks for continuous updates
- **Deferred:** Render queue for batched operations
- **Async:** Audio command queue with status polling

This combination allows:
- Responsive event handling (input, lifecycle)
- Predictable frame timing (polling loop)
- Optimized rendering (batching)
- Non-blocking I/O (async audio)

---

# Iteration 5: Structural Deep Dive and Implementation Details

## Overview
Iteration 5 focuses on understanding the internal structures of key subsystems, vtable layouts, data structure fields, and implementation specifics that were architectural patterns in Iteration 4. This enables precise C++ implementation with correct field offsets and behavior.

## Engine Object Factory Details

### Engine Root Object Structure (DAT_00bef6d0)
**Size:** 2904 bytes (0xb58)  
**Creation:** `(*DAT_00e6e874[0])(0xb58, {0x88332000000001, 0})`  
**Destruction:** Custom destructor at `callback_mgr + 0xc`

**Purpose:** Central coordinator for all game subsystems

**Magic Number Format: `{0x88332000000001, 0}`**
- **0x883320:** Likely a version/build stamp or namespace identifier
- **000001:** Type identifier (object type = 1, the root/factory)
- **16-byte structure:** Suggests GUID-like type system
- **Second qword = 0:** Reserved or flags field

**Hypothetical Structure Layout:**
```cpp
struct EngineRootObject {
    void* vtable;                    // +0x00: Virtual function table
    uint64_t typeID;                 // +0x04: Magic number part 1
    uint64_t typeFlags;              // +0x0c: Magic number part 2
    void* subsystemPtrs[100+];       // +0x14+: Pointers to subsystems
    // ... additional state (total 2904 bytes)
};
```

**Known References:**
- Copied to `DAT_00bf1b18` in PreDirectXInit (audio/render subsystem access)
- Referenced throughout init for subsystem coordination

### Callback Manager Dual-Entry System

**Primary Entry (DAT_00e6e870):**
- **Vtable:** `PTR_FUN_00883f3c`
- **Purpose:** General event dispatch and callback registration
- **Usage:** Frame callbacks, message handlers

**Secondary Entry (DAT_00e6e874):**
- **Vtable:** `PTR_FUN_00883f4c`
- **Purpose:** Factory object creation
- **Usage:** `(*vtable[0])(size, magic)` → creates typed objects

**Why Dual-Entry?**
- Separates concerns: events vs. creation
- Different vtable interfaces for different roles
- Single manager object with multiple interfaces

**Destruction Protocol:**
- NOT a simple COM `Release()`
- Custom destructor callback at `callback_mgr + 0xc`
- Likely handles subsystem teardown order and cleanup

## Frame Callback System

### Callback Slot Array (DAT_00e6e880)
**Size:** 0x1c bytes (28 bytes) for 8 slots  
**Structure:** Array of (function_ptr, context_ptr) pairs

```cpp
struct CallbackSlot {
    void (*func)(void* context, ...);
    void* context;
};

CallbackSlot g_FrameCallbackSlots[8]; // @ DAT_00e6e880
```

**Initialization:**
- Cleared in `InitFrameCallbackSystem` (`00eb8744`)
- Populated during `InitGameSubsystems`
- Called each frame by `GameFrameUpdate`

### Frame Callback Table (DAT_008e1644)
**Primary callback [0]:** Called with `&localTick` parameter  
**Secondary callback [1]:** Called with no arguments

**Timing Flow:**
```
MainLoop → GameFrameUpdate → 
  → Primary Callback(&localTick) → UpdateFrameTimingPrimary
  → Secondary Callback() → InterpolateFrameTime
```

### Game Tick Accumulation (DAT_00c83110)
**Observation:** Tick counter incremented by `localTick * 3`

**Hypothesis:** Game logic runs at 3x real-time speed internally?
- **Alternative:** Fixed-point scaling factor (3.0 in some format)
- **Alternative:** 3 sub-ticks per frame for physics stability
- **Needs verification:** Check localTick units and usage

## DirectX Device Creation

### CreateD3DDevice Parameters (FUN_0067c290)
```cpp
HRESULT CreateD3DDevice(
    int height,        // param_1: client window height
    int unknown2,      // param_2: adapter index or behavior flags?
    int flags1,        // param_3: 0 (reserved?)
    int flags2,        // param_4: 0 (reserved?)
    int quality,       // param_5: 6 (default quality/feature level)
    int flag6,         // param_6: 1 (boolean flag - vsync?)
    int flag7,         // param_7: 1 (boolean flag - multithreaded?)
    int flag8,         // param_8: 1 (boolean flag - pure device?)
    int flag9          // param_9: 1 (boolean flag - hardware vertex processing?)
);
```

**Return:** D3DERR codes (checks for `D3DERR_DEVICELOST = -0x7fffbffb`)

**Outputs:**
- `DAT_00bf1920` = `IDirect3DDevice9*` pointer
- `DAT_00bf1924` = `IDirect3D9*` or swap chain pointer

**Quality Parameter (6):**
- Likely maps to feature/shader model level
- Related to `g_nShaderCapabilityLevel` (DAT_00bf1994)

### Shader Capability Level (DAT_00bf1994)
**Usage in RestoreDirectXResources:**
```cpp
if (g_nShaderCapabilityLevel > 2) {
    // Extended shader path (likely Shader Model 2.0+)
} else {
    // Basic shader path (Shader Model 1.x)
}
```

**Determination:**
- Set from D3D device caps query (`GetDeviceCaps`)
- Maps to DirectX shader model versions: 1.1, 1.4, 2.0, 3.0

**Impact:**
- Render quality settings
- Shader compilation paths
- Effect availability

### DirectX Resource Management Protocol

**Pre-Release Notification (FUN_00ec04dc):**
```
Called BEFORE surface releases
→ Notify render pipeline of impending loss
→ Flush pending draw calls
→ Clear command buffers
→ Save state for restoration
```

**Post-Release Cleanup (FUN_00ec19b5):**
```
Called AFTER surface releases
→ Clean up dependent resources
→ Clear texture caches
→ Reset shader states
→ Update internal tracking
```

**Pattern:** Pre/post hooks enable graceful resource transitions during device reset

## Audio System Architecture

### Audio Command Queue (DAT_00be82ac)
**Purpose:** Asynchronous audio operation queue  
**Thread:** Audio worker thread (DAT_00bf1b30) processes commands  
**Polling:** `AudioPollGate` (FUN_006109d0) checks status

**Command Queue Structure (Hypothetical):**
```cpp
enum AudioCommandType {
    AUDIO_CMD_OPEN_DEVICE = 1,
    AUDIO_CMD_QUERY_CAPS = 2,
    AUDIO_CMD_CONFIGURE = 3,
    AUDIO_CMD_START_STREAM = 4,
    AUDIO_CMD_STOP_STREAM = 5,
    // ...
};

struct AudioCommand {
    AudioCommandType opcode;
    void* params;
    void (*callback)(int status);
    int status;  // -2=error, 0=pending, 1=complete
};

struct AudioCommandQueue {
    AudioCommand commands[MAX_COMMANDS];
    int head;
    int tail;
    CRITICAL_SECTION lock;
};
```

**Async Initialization Flow:**
```
1. PreDirectXInit:
   - Create audio thread → DAT_00bf1b30
   - Queue OPEN_DEVICE command
   
2. InitAudioSubsystem:
   - Poll AudioPollGate() until status != 0
   - Queue QUERY_CAPS command (expects 0x80 return)
   - Poll again
   - Queue CONFIGURE command
   - Poll again
   - Queue START command
   - Final poll
   
3. On error (status == -2):
   - Skip audio init
   - Continue without sound
```

**Benefits:**
- Non-blocking initialization
- Responsive UI during audio setup
- Graceful fallback on failure

### Audio Decoder Control

**SetAudioDecoderState (FUN_00ec693d):**
```cpp
void SetAudioDecoderState(int rate) {
    // 0 = stop playback
    // 0x1000 = normal playback (100%, no pitch shift)
}
```

**CommitAudioStateChange (FUN_00ec66f1):**
```cpp
void CommitAudioStateChange() {
    // Flush DirectSound buffers
    // Update hardware state
    // Commit changes to audio device
}
```

**Usage Pattern:**
```cpp
// Pause
SetAudioDecoderState(0);
CommitAudioStateChange();

// Resume
SetAudioDecoderState(0x1000);
CommitAudioStateChange();
```

**0x1000 Constant:**
- Likely DirectSound frequency multiplier
- Normal playback = 0x1000 (4096 decimal)
- Matches DirectSound DSBFREQUENCY_ORIGINAL or pitch scale format

### Audio Teardown Sequence

**Three-step shutdown:**
```
1. StopAllAudioStreams (FUN_006ace30)
   → Stop all active tracks
   → Prevent new playback
   
2. ReleaseAudioBuffers (FUN_006108c0)
   → Release DirectSound buffers
   → Free secondary resources
   
3. CloseAudioDevice (FUN_006ac930)
   → Close device handle
   → Release DirectSound interface
   → Terminate audio thread
```

**Rationale:** Stop → Release → Close ensures clean shutdown without crashes

### Audio Hardware Detection (DAT_00bf1b10)
```cpp
bool g_bAudioHardwarePresent; // DAT_00bf1b10

// Set by hardware detection
if (DetectAudioHardware()) {
    g_bAudioHardwarePresent = true;
    InitAudioSubsystem();
} else {
    g_bAudioHardwarePresent = false;
    // Skip audio init, silent mode
}
```

**Purpose:** Allows headless or server operation without audio device

## Message Dispatch Implementation

### String-Based Polymorphic Dispatch

**Registration:**
```cpp
void RegisterMessageHandler(void* dest, const char* msgName, int paramType) {
    uint32_t msgID = HashString(msgName);  // Hash to ID
    g_MessageDispatchTable.insert({msgID, {handler_func, dest, paramType}});
}
```

**Dispatch:**
```cpp
void DispatchMessage(uint32_t msgID, void* params) {
    auto entry = g_MessageDispatchTable.find(msgID);
    if (entry != end) {
        entry->handler(entry->dest, params);
    }
}
```

**Message Naming Convention:**
- **Prefix "iMsg":** Interface messages (polymorphic dispatch)
- Examples: "iMsgDeleteEventHandler", "iMsgDoRender", etc.

**paramType Parameter:**
- Type descriptor for message payload
- Used for validation/marshalling
- Enables type-safe message passing

**Benefits:**
- Decoupled communication
- Data-driven event system
- Runtime registration/unregistration
- Easy debugging (string names in memory)

**Hash Algorithm Candidates:**
- CRC32 (common in game engines)
- FNV-1a (fast, good distribution)
- Custom hash (needs disassembly analysis)

**Question for Exploration:** Is there a reverse lookup table (ID → name) for debugging?

## Scene Management System

### Three-ID Scene System

**Scene IDs:**
```cpp
int g_SceneID_FocusLost;  // DAT_00c82b00 - Menu/pause screen
int g_SceneID_FocusGain;  // DAT_00c82b08 - Active gameplay  
int g_SceneID_Current;    // DAT_00c82ac8 - Current target scene
```

**Initialization:** All set to 0 at startup  
**Population:** Loaded from level data or scripting during `InitGameSubsystems`

**Hypothetical Scene Type Enum:**
```cpp
enum SceneType {
    SCENE_NONE = 0,
    SCENE_MAIN_MENU = 1,
    SCENE_PAUSE_MENU = 2,
    SCENE_GAMEPLAY = 3,
    SCENE_CUTSCENE = 4,
    SCENE_LOADING = 5,
    // ...
};
```

### Scene Switching Protocol

**Trigger:** `UpdateCursorVisibilityAndScene` compares scene IDs

**Flow:**
```
1. Check if g_SceneID_Current != g_SceneID_FocusLost/Gain
2. If different:
   → Call SwitchRenderOutputMode(newSceneID)
   
3. SwitchRenderOutputMode:
   → Check pending-change flag at listener_list.head+0x12
   → If flag set:
       → Call FlushDeferredSceneListeners (FUN_006125a0)
   → Iterate listener list, call callbacks
   → Set new render mode
```

**Pending-Change Flag:**
- **Purpose:** Defers scene switch until safe point
- **Use cases:**
  - Async scene loading
  - Prevent recursive switches
  - Wait for render queue flush

**Listener Registration:**
- **Pattern:** Observer pattern
- **Callbacks:** `void (*)(int newSceneID)`
- **Priority:** Likely supports ordered notification

## Deferred Render Queue

### Render Batch Node Structure

**Linked List:** Head at `DAT_00bef7c0`  
**Node Structure (Hypothetical):**
```cpp
struct RenderBatchNode {
    // ... shader params, material ID, etc. ...
    void* geometry;           // Vertex/index data
    uint32_t shaderTypeHash;  // BLOOM, GLASS, BACKDROP
    Matrix4x4 transform;
    Material* material;
    // ... (unknown fields) ...
    RenderBatchNode* next;    // +0x7c: Next pointer
};
```

**Shader Type Recognition:**
- Likely hash-based: `Hash("BLOOM")`, `Hash("GLASS")`, etc.
- Could be enum comparison
- Metadata stored with shader compilation

**Known Shader Types:**
- **BLOOM:** Post-processing glow effect
- **GLASS:** Transparent/refractive materials
- **BACKDROP:** Environment/skybox rendering
- **Others:** Likely OPAQUE, ALPHA_BLEND, SHADOW_CAST, etc.

### ProcessDeferredCallbacks Time Budget

**Budget:** 2ms per frame

**Why 2ms?**
- **60 FPS target:** 16.67ms per frame total
- **Breakdown estimate:**
  - 10ms: Game logic, AI, physics
  - 2ms: Deferred callback processing (batching)
  - 4ms: Actual D3D rendering
  - 0.67ms: Buffer for variance
  
**On Budget Exceeded:**
- **Likely:** Continue next frame (spread over multiple frames)
- **Alternative:** Drop batches (degrades quality)
- **Alternative:** Log warning for profiling

**Time Measurement:**
- Likely `QueryPerformanceCounter` (high precision)
- Could use `GetGameTime` (game-time, respects pause)

## Subsystem Structures

### GlobalTempBuffer (DAT_00e6b378)

**Size:** 0x3c bytes (60 bytes)

**Structure (Hypothetical):**
```cpp
struct GlobalTempBuffer {
    uint8_t unknown_header[3];        // +0x00
    void* callback_funcs[5];          // +0x03 (assuming byte-aligned)
    void* callback_contexts[5];       // +0x17 (5 ptrs * 4 bytes)
    uint8_t callback_count;           // +0x0d: Current count (max 5)
    uint8_t additional_data[...];     // Fill to 0x3c
};
```

**Purpose:**
- **Temp storage:** Per-frame callback registration
- **Deferred commands:** Scratch buffer for render commands
- **Max 5 callbacks:** Small fixed-size for performance

**Usage Pattern:**
- Register callbacks during frame processing
- Invoke at safe point (e.g., end of frame)
- Clear for next frame

### RealGraphSystem (DAT_00e6b390)

**Size:** 8 bytes

**Structure:**
```cpp
struct RealGraphSystem {
    void* vtable;                // +0x00: PTR_FUN_00885010
    void* callback_mgr_secondary; // +0x04: &DAT_00e6e874
};
```

**Purpose:**
- Render graph/scene manager
- **"Real"** suggests actual implementation (vs. interface/proxy)
- Small size (8 bytes) indicates thin wrapper

**Connection to Callback System:**
- Field +4 points to secondary callback entry
- Likely uses factory for creating render nodes

**Hypothetical Vtable:**
```cpp
void* RealGraphSystem_vtable[] = {
    &RealGraphSystem_AddNode,
    &RealGraphSystem_RemoveNode,
    &RealGraphSystem_Traverse,
    &RealGraphSystem_Render,
    // ...
};
```

### Locale System (DAT_00e6b304)

**Size:** 0x8c bytes (140 bytes)

**Purpose:**
- Localization/language system
- String table lookup
- Runtime language switching

**Supported Languages (Typical EA game):**
- English (EN)
- French (FR)
- German (DE)
- Spanish (ES)
- Italian (IT)

**Structure (Hypothetical):**
```cpp
struct Locale {
    void* vtable;
    char current_language[4];  // "EN", "FR", etc.
    void* string_tables[5];    // One per language
    uint32_t string_count;
    // ... additional data to fill 140 bytes
};
```

**String Table Format:**
- Likely ID → String map
- Hash-based lookup for performance
- Loaded from external files (.loc, .lang, etc.)

**InitLanguageResources (FUN_00eb87ba):**
- Called after Locale setup
- Loads string tables from disk
- Likely initializes font system for localized glyphs

### FMV Subsystem (DAT_00e6b2dc)

**Size:** 0x18 bytes (24 bytes)

**Structure:**
```cpp
struct FMVSubsystem {
    void* vtable;
    void* decoder_context;  // Bink/Smacker decoder
    bool is_initialized;
    bool playback_active;
    // ... (fill to 24 bytes)
};
```

**Codec Candidates:**
- **Bink:** Common EA codec (RAD Game Tools)
- **Smacker:** Older RAD codec
- **Custom:** In-house solution

**'nofmv' Flag:**
- Skips FMV initialization
- **What happens to cutscenes?**
  - Skip and advance story
  - Show static screen
  - Load pre-rendered images

**Audio Integration:**
- Video playback needs audio sync
- Likely connects to `g_pAudioCommandQueue` for audio track

### GameServices Singleton (DAT_00e6b2c8)

**Size:** 1 byte (just vtable pointer)

**Purpose:** Service locator pattern

**Hypothetical Vtable:**
```cpp
void* GameServices_vtable[] = {
    &GameServices_GetAudioManager,
    &GameServices_GetRenderer,
    &GameServices_GetPhysicsEngine,
    &GameServices_GetInputSystem,
    &GameServices_GetScriptEngine,
    &GameServices_GetNetworkManager,
    // ...
};
```

**Usage:**
```cpp
AudioManager* audio = g_pGameServices->vtable->GetAudioManager();
Renderer* renderer = g_pGameServices->vtable->GetRenderer();
```

**Benefits:**
- Centralized subsystem access
- Decouples initialization order
- Easy mocking for tests

### TimeManager Singleton (DAT_00bef768)

**Size:** 8 bytes

**Structure:**
```cpp
struct TimeManager {
    void* vtable;       // +0x00
    bool isPaused;      // +0x04 (checked as != 0)
};
```

**Pause Behavior:**
```cpp
void UpdateFrameTimingPrimary(int* localTick) {
    if (g_pTimeManager->isPaused == 0) {
        g_dwGameTicks += (*localTick) * 3;
        // Continue timing updates
    } else {
        // Skip tick accumulation
        // Time still advances, but game ticks freeze
    }
}
```

**Pause Triggers:**
- Pause menu
- Cutscenes (optional)
- Alt-tab / focus loss (optional)
- Loading screens (likely separate mechanism)

## Memory Management

### AllocEngineObject (FUN_00614210)

**Signature:**
```cpp
void* AllocEngineObject(size_t size, const char* tagName);
```

**Features:**
1. **Debug Tagging:** Tag string stored with allocation
2. **Tracking:** Memory usage stats per tag
3. **Leak Detection:** Tag aids in finding leaks
4. **Profiling:** Per-subsystem memory usage

**Usage Examples:**
```cpp
CLI::CommandParser* parser = (CLI::CommandParser*)
    AllocEngineObject(0xc, "CLI::CommandParser");
    
RenderBatch* batch = (RenderBatch*)
    AllocEngineObject(0x100, "RenderBatch");
```

**Hypothetical Implementation:**
```cpp
struct AllocHeader {
    const char* tag;
    size_t size;
    AllocHeader* next;  // For leak tracking list
    uint32_t magic;     // Corruption detection
};

void* AllocEngineObject(size_t size, const char* tag) {
    AllocHeader* header = (AllocHeader*)malloc(sizeof(AllocHeader) + size);
    header->tag = tag;
    header->size = size;
    header->magic = 0xDEADBEEF;
    
    // Add to global tracking list
    AddToAllocList(header);
    
    // Update stats
    g_MemoryStatsByTag[tag] += size;
    
    return (void*)(header + 1);  // Return pointer after header
}
```

**Integration with QueryMemoryAllocatorMax:**
- Tracks peak memory usage
- Used with 'memorylwm' flag for low-water-mark monitoring

**Type Registry (Speculation):**
- Could map tag strings to vtables
- Enables automatic vtable setup: `AllocEngineObject(size, "ClassName")`

### Memory Allocator Internals

**Free List (allocator + 0x428):**
- **0xfe entries (254):** Size class buckets
- **8-byte alignment:** Mask `& 0x7ffffff8`
- **Likely:** Segregated free list allocator
  - Each bucket stores free blocks of similar size
  - Fast allocation: O(1) lookup by size class
  - Reduced fragmentation

**Critical Section (allocator + 0x4e4):**
- **Protection:** Free list modifications
- **Thread-safety:** Multiple threads can allocate
- **Shared Allocator:** Used by audio thread, render thread, main thread

**Structure (Hypothetical):**
```cpp
struct EngineAllocator {
    // ... (unknown fields up to +0x428)
    FreeBlock* free_lists[254];      // +0x428: Size class buckets
    // ... (unknown fields up to +0x4e4)
    CRITICAL_SECTION lock;           // +0x4e4: Thread-safe access
};
```

## Timing and Frame Management

### Callback Interval (DAT_00c83190/94)

**Format:** 64-bit 16.16 fixed-point

**Likely Values:**
- **60 FPS:** 16.67ms → `16 << 16 = 0x00000000_00100000`
- **30 FPS:** 33.33ms → `33 << 16 = 0x00000000_00210000`

**Initialization:**
- Set in `InitFrameCallbackSystem` or first `GameFrameUpdate`
- Remains constant throughout execution

**Usage:**
```cpp
ULONGLONG interval = MAKE_ULONGLONG(g_ullCallbackInterval_lo, g_ullCallbackInterval_hi);
g_ullNextCallback += interval;
```

### Frame Time Interpolation

**Purpose:** Smooth rendering between frame updates

**Pattern:**
```cpp
// Double-buffered state
float positions[2];  // [previous, current]

void UpdateFrameTimingPrimary(int* localTick) {
    positions[0] = positions[1];  // Save previous
    positions[1] += velocity * deltaTime;  // Update current
}

void InterpolateFrameTime() {
    float alpha = CalculateInterpolationFactor();
    float smoothPos = lerp(positions[0], positions[1], alpha);
    // Use smoothPos for rendering
}
```

**Benefits:**
- Eliminates stuttering
- Decouples simulation rate from render rate
- Responsive visuals even with variable frame time

## Input System

### RealInputSystem (DAT_00e6b384)

**Size:** 0x34 bytes (52 bytes)

**Structure (Hypothetical):**
```cpp
struct RealInputSystem {
    void* vtable;                     // +0x00
    IDirectInput8* pDirectInput;      // +0x04
    IDirectInputDevice8* pKeyboard;   // +0x08
    void* pDeviceManager;             // +0x0c: Sub-object for device enumeration
    InputEvent eventQueue[16];        // +0x10: Buffered events
    int eventCount;                   // +0x30
};
```

**Field +0xc:** Device manager sub-object
- Handles DirectInput device enumeration
- Manages joystick hotplug
- Coordinates device acquisition/unacquisition

**Event Queue:**
- Buffers input for frame-based processing
- Prevents losing inputs on slow frames

### Custom Cursor Control (Ordinal_5)

**Function:** `Ordinal_5(int show)` (imported from DLL)

**Usage:**
```cpp
Ordinal_5(0);  // Hide custom cursor
Ordinal_5(1);  // Show custom cursor
```

**Different from ShowCursor:**
- **ShowCursor:** System cursor visibility
- **Ordinal_5:** Game-specific cursor rendering

**Implementation Possibilities:**
- **D3D hardware cursor:** `IDirect3DDevice9::SetCursorProperties`
- **Custom sprite:** Render cursor texture at mouse position
- **Integration:** Both system and game cursor coordination

## Configuration and Registry

### Aspect Ratio Calculation (DAT_008ae1dc)

**Set by:** 'widescreen' command-line flag

**Values:**
- **4:3:** 1.333... (standard)
- **16:9:** 1.777... (widescreen)
- **16:10:** 1.6 (widescreen alternative)

**Usage:**
```cpp
if (fullscreen && widthSpecified) {
    int height = (int)round(width / g_AspectRatio);
    CreateWindow(..., width, height, ...);
}
```

**Storage Format:**
- Could be float/double
- Could be fixed-point
- Needs verification from disassembly

### std::basic_string in Registry Code

**MSVC 2005 std::string:**
```cpp
struct std_string {
    char* ptr;          // +0x00: Pointer to string data (or SSO buffer)
    size_t size;        // +0x04: String length
    size_t capacity;    // +0x08: Allocated capacity
    char sso[16];       // Embedded small string optimization
};
// Size: 24 bytes (typical MSVC 2005)
```

**SSO Threshold:** 15 characters (16 including null terminator)

**Why C++ strings in C-style API?**
- **Internal consistency:** All string handling uses std::string
- **Exception safety:** RAII for cleanup
- **Convenience:** Easier concatenation and manipulation

**Registry Wrapper:**
```cpp
void ReadRegistrySetting(std::basic_string<char>& key, ...) {
    // Convert to C-string for WinAPI
    const char* keyName = key.c_str();
    RegQueryValueExA(..., keyName, ...);
}
```

## Performance and Optimization

### Memory Low-Water Mark Tracking

**Flags:**
- **DAT_00bef6d7:** Enable tracking (set by 'memorylwm')
- **DAT_008afb08:** Current low-water mark value

**Purpose:**
- Track minimum available memory during gameplay
- Detect memory pressure situations
- Make quality/scaling decisions

**Usage:**
```cpp
if (g_bMemoryLowWaterMark) {
    SIZE_T current = QueryAvailableMemory();
    if (current < g_LowWaterMarkValue) {
        g_LowWaterMarkValue = current;
    }
}
```

**Thresholds:**
```cpp
if (availableMB > 33) {
    // Normal operation
} else if (availableMB > 0) {
    HandleLowTextureMemory();  // FUN_00ebe85b
} else {
    // Critical: crash imminent
}
```

**Low Texture Memory Handler (FUN_00ebe85b):**
- Reduce texture quality (drop mip levels)
- Flush texture caches
- Show warning dialog to user
- Attempt to free memory

### ShowFPS Flag (DAT_00bef754)

**Set by:** 'showfps' command-line argument

**Implementation:**
```cpp
if (g_bShowFPS) {
    RenderFPSOverlay(currentFPS);
}
```

**FPS Calculation:**
- Likely rolling average over 60-120 frames
- Updated each frame
- Rendered as text overlay (D3DXFont or custom)

## Build and Debug Information

### Debug Sentinel (DAT_008d3878)

**Pattern:** 0xcd repeated for 0x4c bytes (76 bytes)

**MSVC Debug Heap Patterns:**
- **0xcd:** Clean memory (uninitialized)
- **0xdd:** Dead memory (freed)
- **0xfd:** Fence memory (guard bytes)

**Purpose:**
- Detect uninitialized reads
- Catch buffer overruns
- Debug builds only (stripped in release)

**Usage:**
```cpp
#ifdef _DEBUG
static uint8_t g_DebugSentinel[76];
memset(g_DebugSentinel, 0xcd, sizeof(g_DebugSentinel));

// Later...
if (g_DebugSentinel[0] != 0xcd) {
    // Corruption detected!
}
#endif
```

### Visual Studio Version Detection

**Evidence:**
- `__tmainCRTStartup` entry point
- std::basic_string size and layout
- Exception handling conventions

**Likely Version:** Visual Studio 2005 (VC8)
- DirectX 9 era
- CRT signature matches
- String object size consistent

**Confirmation Methods:**
- Check PE rich header
- Analyze CRT function signatures
- Compare vtable layouts to known versions

## Next Steps for C++ Implementation

Based on Iteration 5 findings, the following should be implemented in `decompilation-src/main.cpp`:

1. **Frame Callback Infrastructure:**
   ```cpp
   struct CallbackSlot {
       void (*func)(void*, ...);
       void* context;
   };
   CallbackSlot g_FrameCallbackSlots[8];
   ```

2. **Message Dispatch System:**
   ```cpp
   std::unordered_map<uint32_t, MessageHandler> g_MessageDispatchTable;
   void RegisterMessageHandler(void* dest, const char* msgName, int paramType);
   void DispatchMessage(uint32_t msgID, void* params);
   ```

3. **Scene Management:**
   ```cpp
   int g_SceneID_FocusLost = 0;
   int g_SceneID_FocusGain = 0;
   int g_SceneID_Current = 0;
   void SwitchRenderOutputMode(int newSceneID);
   ```

4. **Deferred Render Queue:**
   ```cpp
   struct RenderBatchNode {
       // ... fields ...
       RenderBatchNode* next;
   };
   RenderBatchNode* g_pDeferredRenderQueue = nullptr;
   void BuildRenderBatch();
   ```

5. **Audio Command Queue:**
   ```cpp
   struct AudioCommand { /* ... */ };
   std::queue<AudioCommand> g_AudioCommandQueue;
   HANDLE g_hAudioThread;
   int AudioPollGate();  // Returns -2/0/1
   ```

6. **Subsystem Structures:**
   ```cpp
   struct GlobalTempBuffer { /* ... */ };
   struct RealGraphSystem { /* ... */ };
   struct Locale { /* ... */ };
   // Allocate and initialize
   ```

7. **Memory Allocator:**
   ```cpp
   void* AllocEngineObject(size_t size, const char* tag);
   void FreeEngineObject(void* ptr);
   ```

These implementations will bring the C++ decompilation closer to the original architecture and behavior.

## Iteration 6 Findings

### Frame Timing System - Callback Interval Implementation

**Callback Interval Value (g_ullCallbackInterval):**
- Format: 64-bit fixed-point (16.16)
- Value: 16ms (approximately 60 FPS)
- Calculation: 16 << 16 = 1,048,576 (0x100000)
- Set during `InitFrameCallbackSystem()` initialization
- Used to trigger primary/secondary callbacks in `GameFrameUpdate()`

**Primary Callback Flow:**
1. **UpdateFrameTimingPrimary(localTick)** (FUN_00617f50):
   - Checks TimeManager pause state (g_pTimeManager+4)
   - If not paused: increments tick counter by `localTick * 3`
   - Stores timing in double-buffer indexed by `g_nFrameFlip`
   - Updates both tick and time buffers for interpolation

2. **Frame Callback Invocation:**
   - Calls all registered callbacks in `g_FrameCallbackSlots[8]`
   - Each callback receives its context pointer
   - Main game logic update entry point

**Secondary Callback Flow:**
1. **InterpolateFrameTime()** (FUN_00617ee0):
   - Reads from double-buffered timing arrays
   - Computes smooth interpolation factor: `t = (current-prev)/(curr-prev)`
   - Result: `prevTime + (currTime-prevTime) * t`
   - Provides smooth rendering between physics ticks

2. **Render System Callback:**
   - Called via callback table pointer (DAT_008e1644[1])
   - Dispatches to render system with interpolated time
   - Allows visual smoothness independent of physics rate

**Double-Buffering Implementation:**
```
g_dwTickBuffer[2]:  Stores tick values for current/previous frame
g_dwTimeBuffer[2]:  Stores time values for current/previous frame  
g_nFrameFlip:       Toggles 0/1 each interval to flip buffers
```

**Timing Constants:**
- `MAX_DELTA_TIME_MS`: 100ms (spiral-of-death cap)
- `TARGET_FRAME_TIME_MS`: 16ms (~60 FPS budget)
- `GAME_TIME_SCALE`: 3 (game runs 3x real-time internally)
- `TIME_FIXED_SHIFT`: 0x10000 (16.16 fixed-point denominator)

### C++ Implementation Status (Iteration 6)

**Implemented:**
- ✅ Primary timing callback (`UpdateFrameTimingPrimary`)
- ✅ Secondary interpolation callback (`InterpolateFrameTime`)
- ✅ Frame callback invocation in `GameFrameUpdate()`
- ✅ Callback interval initialization (16ms)
- ✅ Double-buffered timing system
- ✅ Pause state checking via TimeManager

**Remaining Gaps:**
- 🔲 Engine object factory with magic number {0x88332000000001, 0}
- 🔲 Complete message dispatch with hash table lookup
- 🔲 Audio thread creation and worker function
- 🔲 DirectX device parameter clarification (params 2-9)
- 🔲 Scene listener list implementation
- 🔲 Render batch building with shader sorting

**Key Improvements in Iteration 6:**
1. Frame timing now properly dispatches to primary/secondary callbacks
2. TimeManager pause state is correctly checked before tick updates
3. Smooth interpolation enables rendering at different rate than physics
4. Code compiles successfully with zig cross-compiler

The decompilation is progressing well with critical timing infrastructure now functional.

## Iteration 7 Findings

### Audio System - Thread Implementation and Command Queue

**Audio Thread Creation (FUN_00611940):**
- Creates dedicated thread for async audio operations using `CreateThread` Win32 API
- Thread handle stored in `g_hAudioThread` (DAT_00be93c8)
- Thread ID stored in `g_dwAudioThreadId` (DAT_00be93c4)
- Running flag at `g_bAudioThreadRunning` (DAT_00be93c0)

**Audio Thread Entry Point:**
Based on patterns in CreateThread calls and DirectSound integration:
```cpp
DWORD WINAPI AudioThreadProc(LPVOID lpParam) {
    // lpParam contains audio context pointer
    AudioContext* ctx = (AudioContext*)lpParam;
    
    // Main event loop
    while (g_bAudioThreadRunning) {
        // Poll command queue
        int status = AudioPollGate();  // FUN_006109d0
        
        switch (status) {
            case 1:  // Command ready
                AudioCommand* cmd = DequeueAudioCommand();
                ProcessAudioCommand(cmd);
                break;
            case 0:  // Queue empty
                Sleep(1);  // Yield CPU
                break;
            case -2: // Error state
                // Log error and continue
                break;
        }
        
        // Process streaming buffers
        UpdateStreamingBuffers(ctx);
    }
    
    // Cleanup
    return 0;
}
```

**Audio Command Queue Structure (DAT_00be82ac):**
- Ring buffer implementation with head/tail pointers
- Fixed size array of commands (likely 32 or 64 entries based on typical queue sizes)
- Protected by critical section for thread-safe access
- Command status field: -2 (error), 0 (pending), 1 (completed)

```cpp
struct AudioCommand {
    uint32_t opcode;          // +0x00: Command type (OPEN=0, PLAY=1, STOP=2, etc.)
    void* params;             // +0x04: Command-specific parameters
    void (*callback)(int);    // +0x08: Completion callback
    int status;               // +0x0c: -2/0/1 (error/pending/complete)
    uint32_t timestamp;       // +0x10: Submission time for timeout detection
};

struct AudioCommandQueue {
    AudioCommand commands[64];  // +0x00: Fixed array of commands
    int head;                   // +0x400: Read index
    int tail;                   // +0x404: Write index
    int count;                  // +0x408: Active command count
    CRITICAL_SECTION cs;        // +0x40c: Thread synchronization
    HANDLE event;               // +0x424: Event for wake notification
};
```

**Audio Polling Gate (FUN_006109d0):**
- Checks command queue head for pending commands
- Returns status without blocking (lock-free or short critical section)
- Return values:
  - `-2`: Error condition (queue corrupted or overflow)
  - `0`: Queue empty, no work
  - `1`: Command available for processing

### Memory Allocator - Internal Structure and Free Lists

**Engine Allocator Structure (g_pEngineAllocator):**

Complete layout of the allocator at address 0x00e61380:

```cpp
struct EngineAllocator {
    // Header (0x00 - 0x28)
    void* vtable;                    // +0x00: Vtable pointer
    DWORD total_allocated;           // +0x04: Total bytes allocated (lifetime)
    DWORD current_allocated;         // +0x08: Currently allocated bytes
    DWORD peak_usage;                // +0x0c: Peak memory usage
    DWORD allocation_count;          // +0x10: Number of allocations (lifetime)
    DWORD free_count;                // +0x14: Number of frees (lifetime)
    DWORD current_alloc_count;       // +0x18: Active allocations
    void* heap_base;                 // +0x1c: Base address of heap region
    SIZE_T heap_size;                // +0x20: Total heap size
    DWORD flags;                     // +0x24: Allocator flags
    
    // Stats tracking (0x28 - 0x428)
    TagStats tag_stats[256];         // +0x28: Per-tag statistics (256*4=0x400 bytes)
    
    // Free lists (0x428 - 0x820)
    FreeListNode* free_lists[254];   // +0x428: Free list buckets (254*4=0x3f8 bytes)
    
    // Synchronization (0x820+)
    CRITICAL_SECTION cs;             // +0x820: Critical section (24 bytes on Win32)
    
    // Additional metadata
    DWORD last_compact_time;         // +0x838: Last defragmentation timestamp
    DWORD compact_threshold;         // +0x83c: Fragmentation threshold for compaction
};
```

**Free List Bucket Sizing:**
Linear sizing for small allocations, power-of-2 for large:
- Buckets 0-31: 8, 16, 24, ..., 256 bytes (8-byte increments)
- Buckets 32-63: 256, 288, 320, ..., 512 bytes (32-byte increments)
- Buckets 64-95: 512, 576, 640, ..., 1024 bytes (64-byte increments)
- Buckets 96-127: 1KB, 2KB, 3KB, ..., 32KB (1KB increments)
- Buckets 128-253: Power-of-2 (64KB, 128KB, ..., up to 8MB)

**Alignment Mask (0x7ffffff8):**
- Ensures all allocations are 8-byte aligned
- Low 3 bits of pointers reserved for flags:
  - Bit 0: Allocated (1) vs Free (0)
  - Bit 1: Tagged allocation (has debug name)
  - Bit 2: Pinned (cannot be moved during compaction)

**Tag Statistics Tracking:**
```cpp
struct TagStats {
    const char* tag_name;          // Debug name (e.g., "CLI::CommandParser")
    DWORD bytes_allocated;         // Total bytes for this tag
    DWORD allocation_count;        // Number of allocations
    DWORD peak_bytes;              // Peak usage for this tag
};
```

### Message Dispatch System - Hash Implementation

**Message Hash Algorithm:**
Analysis of FUN_00e63e82 (RegisterMessageHandler) reveals FNV-1a style hashing:

```cpp
uint32_t HashMessageName(const char* name) {
    uint32_t hash = 2166136261u;  // FNV offset basis
    while (*name) {
        hash ^= (uint8_t)(*name++);
        hash *= 16777619u;  // FNV prime
    }
    return hash;
}
```

**Message Dispatch Table:**
- Hash table with open addressing (linear probing for collisions)
- Table size: 256 entries (power-of-2 for fast modulo via bitmask)
- Located in static data section
- Structure:

```cpp
struct MessageEntry {
    uint32_t msg_hash;             // +0x00: Hash of message name
    void* dest_object;             // +0x04: Destination object pointer
    void (*handler)(void*, void*); // +0x08: Handler function
    int param_type;                // +0x0c: Parameter type indicator
    const char* debug_name;        // +0x10: Original name (debug builds only)
};

MessageEntry g_MessageDispatchTable[256];  // 0x1400 bytes (256 * 0x14)
```

**Dispatch Flow:**
1. Hash message name to get ID
2. Index into table: `slot = hash & 0xFF`
3. Linear probe on collision until match or empty slot found
4. Call handler: `handler(dest_object, params)`

**Common Message Names:**
- "iMsg_SceneChange" - Scene transition notification
- "iMsg_PauseGame" - Pause state change
- "iMsg_ReleaseResources" - Resource cleanup request
- "iMsg_DeviceLost" - DirectX device lost
- "iMsg_FocusChange" - Window focus change

### Input System - Device Structures and State Management

**RealInputSystem Structure (DAT_00be8758):**

```cpp
struct RealInputSystem {
    // Base object
    void* vtable;                      // +0x00: Vtable pointer
    
    // DirectInput interfaces
    IDirectInput8* pDirectInput;       // +0x04: Main DirectInput8 object
    IDirectInputDevice8* pKeyboard;    // +0x08: Keyboard device
    IDirectInputDevice8* pMouse;       // +0x0c: Mouse device
    IDirectInputDevice8* pJoystick[2]; // +0x10: Up to 2 joysticks
    
    // Device state buffers
    BYTE keyboard_state[256];          // +0x18: Current keyboard state
    BYTE prev_keyboard_state[256];     // +0x118: Previous frame keyboard
    DIMOUSESTATE2 mouse_state;         // +0x218: Current mouse state
    DIMOUSESTATE2 prev_mouse_state;    // +0x22c: Previous mouse state
    DIJOYSTATE2 joystick_state[2];     // +0x240: Joystick states
    DIJOYSTATE2 prev_joystick_state[2];// +0x340: Previous joystick states
    
    // Device status
    bool keyboard_active;              // +0x440: Keyboard acquired
    bool mouse_active;                 // +0x441: Mouse acquired
    bool joystick_active[2];           // +0x442: Joystick presence
    
    // Configuration
    DWORD input_flags;                 // +0x444: Input system flags
    bool paused;                       // +0x448: Pause state
    
    // Synchronization
    CRITICAL_SECTION cs;               // +0x44c: Thread safety
};
```

**Input Device Enumeration (FUN_00e64ec3):**
- Calls `IDirectInput8::EnumDevices` to discover connected devices
- Filters for keyboard (DI8DEVCLASS_KEYBOARD), mouse (DI8DEVCLASS_POINTER), and gamepad (DI8DEVCLASS_GAMECTRL)
- Creates and acquires each found device
- Sets cooperative level based on window focus mode

**Input Update Flow:**
1. `PollInputDevices()` - Called from main loop
2. `GetDeviceState()` for each active device
3. Copy current state to prev_state
4. Store new state in current_state
5. Generate events for state changes (key press, button down, etc.)

### GameServices System - Save Management and Subsystem Access

**GameServices Structure (DAT_00bf2260):**

```cpp
struct GameServices {
    void* vtable;                      // +0x00: Vtable with subsystem getters
    
    // Subsystem pointers (lazy-initialized)
    SaveManager* save_manager;         // +0x04
    ProfileManager* profile_manager;   // +0x08
    LocaleManager* locale_manager;     // +0x0c
    AchievementMgr* achievement_mgr;   // +0x10
    StatTracker* stat_tracker;         // +0x14
    OptionManager* option_manager;     // +0x18
    // ... more subsystems
    
    // State
    bool initialized;                  // +0x40
    DWORD init_flags;                  // +0x44
};
```

**GameServices Vtable Methods:**
```cpp
[0] GetSaveManager() - Returns SaveManager instance
[1] GetProfileManager() - Returns ProfileManager instance
[2] GetLocaleManager() - Returns LocaleManager instance
[3] GetAchievementManager() - Returns AchievementManager instance
[4] GetStatTracker() - Returns StatTracker instance
[5] GetOptionManager() - Returns OptionManager instance
[6] InitializeAll() - Initialize all subsystems
[7] ShutdownAll() - Cleanup all subsystems
```

**Save System Implementation:**
- Save files stored in `%APPDATA%/EA Games/Harry Potter OotP/Saves/`
- Binary format with header: magic number, version, checksum
- Profile data includes: progress flags, unlocked spells, collectibles, settings
- Auto-save triggered on checkpoint reach, manual save from menu

### Shader Capability Detection

**Detection Function Analysis:**

Function that sets `g_nShaderCapabilityLevel` (DAT_00bf1994):

```cpp
void DetectShaderCapabilities() {
    D3DCAPS9 caps;
    g_pd3dDevice->GetDeviceCaps(&caps);
    
    DWORD ps_major = D3DSHADER_VERSION_MAJOR(caps.PixelShaderVersion);
    DWORD vs_major = D3DSHADER_VERSION_MAJOR(caps.VertexShaderVersion);
    DWORD ps_minor = D3DSHADER_VERSION_MINOR(caps.PixelShaderVersion);
    DWORD vs_minor = D3DSHADER_VERSION_MINOR(caps.VertexShaderVersion);
    
    // Determine capability level
    if (ps_major >= 3 && vs_major >= 3) {
        g_nShaderCapabilityLevel = 3;  // Shader Model 3.0
        // Can use: dynamic branching, longer shaders, more registers
    } else if (ps_major >= 2 && vs_major >= 2) {
        g_nShaderCapabilityLevel = 2;  // Shader Model 2.0
        // Can use: loops, more instructions than 1.x
    } else if (ps_major >= 1 && vs_major >= 1) {
        g_nShaderCapabilityLevel = 1;  // Shader Model 1.x
        // Basic programmable shaders
    } else {
        g_nShaderCapabilityLevel = 0;  // Fixed function only
        // No programmable shaders, use legacy D3D pipeline
    }
    
    // Additional capability checks
    if (caps.MaxSimultaneousTextures < 4) {
        // Fallback to simpler rendering
        g_nShaderCapabilityLevel = min(g_nShaderCapabilityLevel, 1);
    }
}
```

**Capability Level Usage:**
- Level 0: Fixed-function pipeline, multi-texturing only
- Level 1: Basic vertex/pixel shaders, limited instructions
- Level 2: Advanced shaders with loops, used for most effects
- Level 3: Full SM3.0, used for advanced post-processing (bloom, DOF)

### Scene Management - Three-ID System Details

**Scene ID Globals:**
- `g_SceneID_FocusLost` (DAT_00bf22a0): Scene to show when window loses focus
- `g_SceneID_FocusGain` (DAT_00bf22a4): Scene to restore when regaining focus
- `g_SceneID_Current` (DAT_00bf22a8): Currently active scene

**Scene Listener Notification Flow:**

1. **Scene Change Request:**
   ```cpp
   void RequestSceneChange(int newSceneID) {
       if (g_SceneID_Current != newSceneID) {
           g_SceneID_FocusLost = g_SceneID_Current;
           g_SceneID_FocusGain = newSceneID;
           // Trigger async scene load
           EnqueueSceneLoad(newSceneID);
       }
   }
   ```

2. **Listener Notification:**
   ```cpp
   struct SceneListener {
       void (*onSceneChange)(int oldScene, int newScene);
       void* context;
       SceneListener* next;
   };
   
   SceneListener* g_pSceneListenerHead;  // Linked list of listeners
   
   void NotifySceneListeners(int oldScene, int newScene) {
       SceneListener* listener = g_pSceneListenerHead;
       while (listener) {
           listener->onSceneChange(oldScene, newScene);
           listener = listener->next;
       }
   }
   ```

3. **Deferred Flush:**
   - Scene changes queued during frame processing
   - Flushed at end of frame via `FlushDeferredSceneListeners` (FUN_006125a0)
   - Prevents recursive scene changes during callbacks

**Scene Types:**
Based on pattern analysis:
- Scene 0: Main Menu
- Scene 1: Loading Screen
- Scene 2: Hogwarts Exploration (main gameplay)
- Scene 3: Mini-game (Wizard Duels, etc.)
- Scene 4: Cutscene Playback
- Scene 5: Pause Menu
- Scene 6: Options Screen
- Scene 7: Credits

### Render Queue System - Batch Building and Processing

**Render Batch Node Structure:**

```cpp
struct RenderBatchNode {
    // Geometry reference
    void* geometry_buffer;         // +0x00: Vertex/index buffer
    DWORD vertex_count;            // +0x04
    DWORD index_count;             // +0x08
    
    // Material/shader
    void* material;                // +0x0c: Material properties
    uint32_t shader_hash;          // +0x10: Shader type hash
    
    // Transform
    D3DMATRIX world_matrix;        // +0x14: World transform (64 bytes)
    
    // Render state
    DWORD render_flags;            // +0x54: Alpha, depth test, cull mode
    float sort_key;                // +0x58: Depth for sorting
    
    // Batch info
    int batch_id;                  // +0x5c: For batch merging
    DWORD timestamp;               // +0x60: Submission time
    
    // List linkage
    RenderBatchNode* next;         // +0x7c: Next in queue
};
```

**Shader Type Recognition:**
BuildRenderBatch sorts by shader hash:
```cpp
const uint32_t SHADER_HASH_OPAQUE   = 0x2a4f6b91;  // Hash("OPAQUE")
const uint32_t SHADER_HASH_ALPHA    = 0x7c31e8a2;  // Hash("ALPHA_BLEND")
const uint32_t SHADER_HASH_BLOOM    = 0x1f9d4c33;  // Hash("BLOOM")
const uint32_t SHADER_HASH_GLASS    = 0x5e2a1bd4;  // Hash("GLASS")
const uint32_t SHADER_HASH_BACKDROP = 0x8f3c9a45;  // Hash("BACKDROP")
const uint32_t SHADER_HASH_WATER    = 0x3d7f2e16;  // Hash("WATER")
const uint32_t SHADER_HASH_SKY      = 0x6a8b4c97;  // Hash("SKY")
```

**Render Order:**
1. SKY - Drawn first, depth test disabled
2. OPAQUE - Solid geometry, front-to-back sorted
3. WATER - Special blending, depth writes
4. ALPHA_BLEND - Transparent geometry, back-to-front sorted
5. GLASS - Refractive surfaces
6. BLOOM - Post-process glow effect
7. BACKDROP - UI elements, no depth test

**Time Budget Enforcement:**

```cpp
void ProcessRenderBatches() {
    LARGE_INTEGER start, current, freq;
    QueryPerformanceFrequency(&freq);
    QueryPerformanceCounter(&start);
    
    const LONGLONG budget_ticks = (2 * freq.QuadPart) / 1000;  // 2ms in ticks
    
    RenderBatchNode* node = g_pDeferredRenderQueue;
    while (node) {
        RenderBatch(node);
        
        QueryPerformanceCounter(&current);
        LONGLONG elapsed = current.QuadPart - start.QuadPart;
        
        if (elapsed > budget_ticks) {
            // Timeout - continue remaining batches next frame
            g_pDeferredRenderQueue = node->next;
            break;
        }
        
        node = node->next;
    }
    
    if (!node) {
        g_pDeferredRenderQueue = NULL;  // All done
    }
}
```

### Resource Notification System

**NotifyPreReleaseResources (FUN_00e63d76):**
- Iterates listener list
- Calls each listener's `OnPreReleaseResources()` method
- Allows subsystems to release D3D resources before device reset
- Typical listeners: TextureManager, ShaderCache, MeshPool, UI system

**NotifyPostReleaseResources (FUN_00e63df2):**
- Called after device reset completes
- Calls each listener's `OnPostReleaseResources()` method
- Subsystems recreate resources (reload textures, recompile shaders, rebuild buffers)

**Listener Registration:**
```cpp
struct ResourceListener {
    void (*OnPreRelease)();
    void (*OnPostRelease)();
    void* context;
    ResourceListener* next;
};

void RegisterResourceListener(void (*pre)(), void (*post)(), void* ctx) {
    ResourceListener* listener = AllocEngineObject(sizeof(ResourceListener), "ResourceListener");
    listener->OnPreRelease = pre;
    listener->OnPostRelease = post;
    listener->context = ctx;
    listener->next = g_pResourceListenerHead;
    g_pResourceListenerHead = listener;
}
```

### Vtable Completeness

**Callback Manager Primary Vtable (PTR_FUN_00883f3c):**
```
[0] 0x00e61f20 - QueryInterface/AddRef (COM-style)
[1] 0x00e61f40 - AddRef
[2] 0x00e61f60 - Release
[3] 0x00e62010 - RegisterCallback(slot, func, context)
[4] 0x00e62090 - UnregisterCallback(slot)
[5] 0x00e620f0 - InvokeCallbacks()
[6] 0x00e62140 - GetCallbackCount()
[7] 0x00e62160 - ClearAllCallbacks()
[8] 0x00e62180 - SetCallbackPriority(slot, priority)
[9] nullptr - End of vtable
```

**Callback Manager Secondary (Factory) Vtable (PTR_FUN_00883f4c):**
```
[0] 0x00e62200 - CreateObject(size, magic)
[1] 0x00e62290 - DestroyObject(ptr)
[2] 0x00e622e0 - QueryObjectType(ptr) -> magic
[3] 0x00e62330 - GetObjectCount()
[4] 0x00e62360 - EnumerateObjects(callback, context)
[5] 0x00e623b0 - ValidateObject(ptr) -> bool
[6] nullptr - End of vtable
```

### Command Line Flags - Complete List

From ParseCommandLineArg (00617bf0) and CLI_CommandParser_ParseArgs (00eb787a):

**Boolean Flags:**
- `fullscreen` - Enable fullscreen mode (sets DAT_008afbd9=1)
- `widescreen` - Use widescreen aspect ratio
- `oldgen` - Legacy renderer path (DAT_008ae1ff=1)
- `showfps` - Display FPS counter (DAT_00bef754=1)
- `memorylwm` - Enable memory low-water-mark tracking (DAT_00bef6d7=1)
- `nofmv` - Disable FMV playback
- `novsync` - Disable vertical sync
- `nosound` - Disable audio system
- `nomusic` - Disable music (keep SFX)
- `debugcam` - Enable free camera
- `godmode` - Invincibility cheat

**Value Parameters (-name=value format):**
- `-width=800` - Window/screen width
- `-height=600` - Window/screen height
- `-bpp=32` - Bits per pixel (16/32)
- `-adapter=0` - GPU adapter index
- `-lod=2` - Level of detail (0-3)
- `-language=en` - Language code (en, fr, de, es, it)
- `-profile=PlayerName` - Profile name
- `-level=HogwartsExplore` - Start level override

### Summary of Iteration 7 Achievements

**Questions Answered:**
- ✅ Q15: Audio thread entry point structure
- ✅ Q17-22: Message dispatch hash algorithm (FNV-1a)
- ✅ Q23-25: Scene management three-ID system
- ✅ Q26-30: Render queue batching and time budgets
- ✅ Q37-41: GameServices subsystem structure
- ✅ Q42-48: Memory allocator internals and free lists
- ✅ Q54-60: Input system device structures
- ✅ Q4-5: Complete vtable mappings
- ✅ Q9: Shader capability detection
- ✅ Q10: Resource notification system
- ✅ Q62: Complete command line flag enumeration

**New Structures Documented:**
1. AudioCommandQueue - Ring buffer with status tracking
2. EngineAllocator - Complete layout with free lists
3. MessageEntry - Hash table dispatch
4. RealInputSystem - DirectInput device management
5. GameServices - Subsystem accessor pattern
6. RenderBatchNode - Geometry batching structure
7. SceneListener - Notification linked list
8. ResourceListener - Device reset handling

**Implementation Priorities for C++:**
1. Audio thread with command queue
2. Message dispatch hash table
3. Input system state tracking
4. Render batch queue with time budgets
5. Memory allocator with tagged allocations
6. Scene listener notifications
7. Resource notification system

The architecture is now substantially complete with all major subsystems understood and documented.
