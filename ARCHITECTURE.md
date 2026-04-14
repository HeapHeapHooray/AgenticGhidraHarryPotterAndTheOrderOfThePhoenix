# Architecture of hp.exe

## Overview
`hp.exe` is the main executable for "Harry Potter and the Order of the Phoenix". It is a Windows PE application built with Visual Studio 2005 using DirectX 9 for rendering and DirectInput 8 for input.

## Entry Point
The program starts at `___tmainCRTStartup` (`006f54d2`), the standard CRT entry for a Windows app, which calls `WinMain`.

## WinMain (`0060dfa0`)
Core entry point for all game logic. Initialization order:

1. **FPU Configuration**: `_control87(0x20000, 0x30000)` — prevents denormal floating-point exceptions
2. **System Parameters**: Saves current Windows parameters (`SPI_GETMOUSESPEED`, `SPI_GETMOUSE`, `SPI_GETSCREENREADER`) into globals `DAT_008afc44`, `DAT_008afc4c`, `DAT_008afc54`, then calls `UpdateSystemParameters(0)` to disable mouse acceleration
3. **Command Line Storage**: `strncpy` of `lpCmdLine` into `DAT_00c82b88` (0x1ff bytes) and `DAT_00c82d88` (0x1ff bytes)
4. **COM/Thread Init**: `thunk_FUN_00eb787a()` — likely CoInitialize or thread pool setup
5. **Single Instance Check**: `FindWindowA("OrderOfThePhoenixMainWndClass", NULL)` — if found, calls `TerminateProcess(GetCurrentProcess(), 0)`
6. **Window Class Registration**: `RegisterWindowClass()` (`00eb4b95`)
7. **Registry Settings Load**: Reads all game settings (see section below)
8. **Command Line Parsing**: `ParseCommandLineArg` checks for `fullscreen` and `widescreen` flags
9. **Window Creation**: `CreateGameWindow(hInstance, width, height)` → stored in `DAT_00bef6cc`
10. **Subsystem Init**: `thunk_FUN_00eb5c3e()`, `thunk_FUN_00eb612e()`, `thunk_FUN_00eb496e()` — likely DirectX + DirectInput initialization
11. **Window Placement Restore**: In windowed mode, reads `Maximized`/`Minimized`/placement from registry and calls `ShowWindow` accordingly
12. **Main Loop**: `MainLoop()` (`0060dc10`)
13. **Cleanup**: `thunk_FUN_00ec6610()` (render teardown), `UnacquireInputDevices()`, `UpdateSystemParameters(1)`, `TerminateProcess` (hard exit)

On initialization failure: `TerminateProcess(GetCurrentProcess(), 0)`.

## Registry Settings (`0060ce60` for int, `0060ca20` for string)

All settings under `HKEY_CURRENT_USER\Software\Electronic Arts\Harry Potter and the Order of the Phoenix\GameSettings`:

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
| `Maximized` | `"false"` | Whether window is maximized |
| `Minimized` | `"false"` | Whether window is minimized |

### Fallback
- Reads HKCU first, then HKLM. If key is not found anywhere, creates it in HKCU with the default value.
- `ReadRegistrySetting` uses `std::basic_string` internally. Throws C++ exception if section name is empty.

## Command Line Parsing (`ParseCommandLineArg` `00617bf0`)
Parses named flags from the saved command line (`DAT_00c82b88`):
- `fullscreen` — sets `DAT_008afbd9=1` (fullscreen mode), optionally reads a width value
- `widescreen` — sets `DAT_008ae1dc` to a widescreen aspect ratio constant (`DAT_008b6a84`; default is `DAT_007d52fc`)

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

### Window Class Registration (`00eb4b95`)
- Class: `OrderOfThePhoenixMainWndClass`
- Style: `CS_DBLCLKS | CS_OWNDC | CS_VREDRAW | CS_HREDRAW`
- Background: black brush; cursor: arrow
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
1. `PauseGraphicsState()` (`FUN_00617b60`)
2. `UnacquireInputDevices()`
3. `ShowCursor` loop until ≥ 1
4. `DAT_00bef6c7 = 1` (has-focus flag set to "lost focus")
5. `thunk_FUN_00ea53ca()` — likely render pause
6. If delayed-op timer is 0 (not already pending):
   - If audio state allows: `PauseAudio()` (`FUN_0061ef80`), set `DAT_00bef6d4`
   - If game update state allows: `PauseGameUpdates(0)` (`FUN_0058b790`), set `DAT_00bef6d5`
7. `DAT_00bef6d8 = 0` (reset delayed timer)

**Focus Gain (WM_ACTIVATE, wParam≠0, fullscreen):**
1. Via game object at `DAT_00e6b384+0xc`: calls vtable method to get input device and acquires it
2. `AcquireInputDevices()`
3. `ShowCursor` loop until < 0
4. `thunk_FUN_00ea53ca()`
5. `DAT_00bef6c7 = 0`
6. `DAT_00bef6d8 = 2000` (2-second delay before re-enabling updates)

### Window Placement Save (`SaveWindowPlacement` `0060d220`)
Saves position/state via `WriteRegistrySetting` (`0060c670`) on WM_CLOSE.

## Main Game Loop (`MainLoop` `0060dc10`)
Runs until `WM_QUIT` received or `DAT_00bef6c5` exit flag is set.

**Per-iteration sequence:**
1. `UpdateDirectXDevice()` — check/recover D3D device
2. **Focus state** (fullscreen only):
   - No focus + cursor state changed (`DAT_00bef67e`): calls `SwitchRenderOutputMode` with scene IDs from `DAT_00c82b00/08/ac`
   - Has focus but flag says "lost": `UnacquireInputDevices()`, show cursor, `SwitchRenderOutputMode`
3. `PeekMessageA` (PM_REMOVE) — if `WM_QUIT (0x12)`: return `wParam`
4. No messages:
   - Compute frame budget: `uStack_44 = ROUND((accumHigh * k1 + accumLow) * k2 * k3)` using constants at `DAT_008475d8`, `DAT_00845594`, `DAT_00845320`
   - `GameFrameUpdate()`
   - If `DAT_00bef6d7` (game update enabled): lazy-init `FUN_00612f00()` with atexit cleanup; call `thunk_FUN_00eb6dbc()` and track min in `DAT_008afb08`
   - Manage `DAT_00bef6d8` delayed-op countdown; on expiry: optionally `thunk_FUN_00ec67e8()` (resume audio) and `ResumeGameUpdates()` (`FUN_0058b8a0`)
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
   - Toggle `DAT_008e1648` (frame flip index)
   - Store current timing in double-buffer at `DAT_00c83170+flip*8`
   - Call `UpdateFrameTimingPrimary(&local_4)` and `(*DAT_008e1644[0])(&local_4)`
8. Check secondary callback buffer, call `InterpolateFrameTime` and `(*DAT_008e1644[1])()`

### Key timing globals
| Address | Name | Purpose |
|---------|------|---------|
| `DAT_00e6e5e0/e4` | last frame time | 64-bit microseconds (16.16 fixed) |
| `DAT_00c83198/9c` | accumulated time | 64-bit |
| `DAT_00c83110` | game ticks | = accum*3/0x10000 |
| `DAT_00c83114` | tick counter | incremented in primary callback |
| `DAT_00c831a8/ac` | next callback time | 64-bit |
| `DAT_00c83190/94` | callback interval | 64-bit |
| `DAT_008e1648` | frame flip index | 0 or 1, toggled each callback |
| `DAT_00c83170` | timing double-buffer | 2×8 bytes |
| `DAT_008e1644` | callback table ptr | function pointer array [primary, secondary] |

### Deferred Callback Queue (`ProcessDeferredCallbacks` `00636830`)
Linked list at `DAT_00bef7c0`. Each node has next pointer at `+0x7c`.
- Per frame: processes as many nodes as possible within 2ms
- `FUN_0063d600(node)` returns 1 when the node's work is done; node is then removed

## DirectX Device Management

### UpdateDirectXDevice (`DirectX_UpdateDevice` `0067d310`)
1. Calls `PreDeviceCheck()` (`FUN_0067d2e0`)
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
   - `FUN_0067ecf0(local_c, 0)`
   - Release surface `local_c`
   - Release `DAT_00bf1934`
5. Release `DAT_00bf1930` (back buffer) if non-null
6. Release `DAT_00bf1938` (additional render target) if non-null
7. `thunk_FUN_00ec19b5()` — post-release cleanup

### RestoreDirectXResources (`0067d0c0`)
1. `InitRenderStates()` (`FUN_00675950`)
2. If `DAT_00b95034 == NULL`: `CreateGPUSyncQuery()` (`FUN_0067b820`)
3. `FUN_0067bb20()` — (empty stub in current analysis)
4. Get back buffer via `DAT_00bf1924` vtable +0x14 → `DAT_00bf1930`; cache in `DAT_00af1390`
5. Get back buffer surface description
6. `CreateTexture` (vtable +0x74) for additional render target → `DAT_00bf1938`
7. If `DAT_00bf1970 == 0` (AAMode == off) and HW supports it:
   - `CreateTexture` (vtable +0x5c) with same dimensions → `local_28`
   - Release old `DAT_00bf1930`, get `GetSurfaceLevel(0)` → `DAT_00af1390`
   - `DAT_00bf1934 = local_28`; `DAT_00bf1930 = DAT_00af1390`
8. Else: `DAT_00bf1934 = 0xbacb0ffe` (sentinel - AA path, no extra surface)
9. `DAT_00ae9250 = DAT_00bf1938`
10. `SetRenderTarget(0, DAT_00af1390)` and secondary render target
11. `InitD3DStateDefaults()` (`FUN_00674430`)

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
| `DAT_009dcc50/54` | Vertex shader type/pointer pairs (count: `DAT_00bf1a08`) |
| `DAT_00e17ce0` | Pixel shader object array (count: `DAT_00bf1a0c`, stride 0x160) |
| `DAT_00bf1a08` | Vertex shader count |
| `DAT_00bf1a0c` | Pixel shader count |
| `DAT_00bf1994` | Shader/feature level capability (> 2 = extended) |
| `DAT_00bf193c` | Available texture memory in MB (updated by QueryAvailableTextureMem) |

### D3D State Defaults (`InitD3DStateDefaults` `00674430`)
Initializes large arrays of render state, sampler state, and texture stage state defaults.
Uses `SetCachedRenderState` (`0067eb90`) which caches values in `DAT_00b94748` to avoid redundant D3D calls.
Conditionally adjusts blend states for hardware that lacks certain texture operation capabilities (checked via `GetAdapterCapsPtr` + 0x3c bit tests).
Also sets: fog state defaults (`_DAT_00d7ea68=5`), clears `DAT_008d3878` sentinel area with `0xcd`.

### GPU Sync Query (`CreateGPUSyncQuery` `0067b820`)
Creates a `D3DQUERYTYPE_EVENT` query (vtable offset 0xe0 on device).
Stored in `DAT_00b95034`. Used for GPU-CPU synchronization.
The `swap effect` flag (`DAT_008ae1fc`) selects between DISCARD (2) and COPY (0) queries.

### Cached Render Target Setter (`SetCachedRenderTargets` `0067ecf0`)
Avoids redundant `SetRenderTarget`/`SetDepthStencilSurface` calls by caching the last set values in `DAT_00af1390` (render target) and `DAT_00ae9250` (depth stencil).

### Video Memory Monitor (`QueryAvailableTextureMem` `0067d2e0`)
Calls `GetAvailableTextureMem` (vtable+0x10), stores result in MB in `DAT_00bf193c`.
If result < 33MB, calls low-memory handler. Used to detect texture memory pressure.

### Adapter Capabilities (`GetAdapterCapsPtr` `0066dfe0`)
Returns pointer to the D3DCAPS9 data: `DAT_008ae200 * 0x584 + 0x454 + DAT_00bf19a8`
Each adapter entry is 0x584 bytes; offset 0x454 into the entry holds capability data.

## DirectInput Management

### Input Devices
- `DAT_00e6a070`: `IDirectInputDevice8*` keyboard
- `DAT_00e6a194`: `IDirectInputDevice8*` mouse
- `DAT_00e6a42c`: first joystick entry; stride = 0x248 bytes; 2 joysticks

### AcquireInputDevices (`0068da30`) / UnacquireInputDevices (`0068dac0`)
Acquire/unacquire all devices when gaining/losing focus.
Fatal error via `FUN_0066f810` if any device fails.
Also calls `Ordinal_5(1/0)` — custom cursor show/hide.

## Game State and Audio

### DirectX and Subsystem Initialization (`InitDirectXAndSubsystems` `00eb612e`)
Called from WinMain after window creation with window height:
1. `FUN_00614370(height, ?, hWnd)` — creates D3D device and set present params
2. `FUN_0060c2e0()` — additional graphics init
3. Sets `DAT_008df65a=1`, `DAT_00aeea5c=2` (subsystem state flags)
4. `FUN_006147f0()` — finalizes device setup
5. `thunk_FUN_00eb60d3()` — DirectInput or audio init
Returns 0 on success (OR of all sub-failures).

### Game Subsystem Initialization (`InitGameSubsystems` `00eb496e`)
Registers callbacks, enumerates DirectInput devices (`FUN_00688370(4, 0)`), and loads the language/localization selection screen (`FUN_005090a0("LanguageSelect")`).

### Frame Callback System (`InitFrameCallbackSystem` `00612f00`)
Initializes the frame callback singleton at `DAT_00e6e870`:
- `DAT_00e6e870 = &PTR_FUN_00883f3c` (primary callback entry)
- `DAT_00e6e874 = &PTR_FUN_00883f4c` (secondary callback entry)
- `_DAT_00bef728 = &DAT_00e6e870` (pointer to the table, = `DAT_008e1644`)

### Pause/Resume System
- **Focus loss**: `PauseAudioManager()` (`FUN_0061ef80`) + `PauseGameObjects(0)` (`FUN_0058b790`)
- **Resume**: triggered by `DAT_00bef6d8` expiry:
  - `thunk_FUN_00ec67e8()` resumes audio
  - `ResumeGameObjects()` (`FUN_0058b8a0`) resumes game objects

### Game Object Pause State Machine (`DAT_00c7b908`)
Controls which systems are paused (values 0–7):
- **State 0 (full pause)**: "chapter-cutscene-in" animation, vtable+0x24 (Pause) on all systems. Records pause time in `_DAT_00c7b90c = DAT_00c8311c/3 + param_1`.
- **States 4–5 (audio-only pause)**: pauses `DAT_00c7c370` (audio manager) only, transitions to state 6.
- **State 6**: fully paused, no-op.
- **State 7 (resuming)**: "default" animation, vtable+0x28 (Resume) on all systems, clears `DAT_00c6d7e0`.

Pausable systems: `DAT_00c7c370` (audio/music manager), `DAT_00c7b924` (secondary system), array at `DAT_00c7ba4c` (stride 0x12×4), additional systems at `DAT_00c7d038+0xc` (stride 0xb×4).

### Deferred Render Batch Queue
Linked list at `DAT_00bef7c0` (processed by `ProcessDeferredCallbacks`).
Each node is a draw call descriptor (geometry + shader + flags).
`BuildRenderBatch` (`0063d600`) processes one node per call, building entries in the material table at `DAT_00ae9258`.
Recognized shader types: `BLOOM`, `GLASS`, `BACKDROP`.
Returns 0 while more vertices remain; 1 when the node is complete (removed from list).

### Memory Pressure Tracking
`QueryMemoryAllocatorMax` (`00eb6dbc`) queries the game's internal memory allocator for the largest free block size (thread-safe via critical section). The minimum value across all frames is tracked in `DAT_008afb08`, providing a low-water-mark for memory pressure detection.

### Global State Flags
| Address | Name | Purpose |
|---------|------|---------|
| `DAT_00bef6c5` | exit flag | Non-zero = exit MainLoop |
| `DAT_008afbd9` | fullscreen | Boolean fullscreen mode |
| `DAT_00bef6c7` | focus lost | 1 = window does not have focus |
| `DAT_00bef67e` | cursor state | Current cursor visibility state |
| `DAT_00bef67c` | unknown | Checked in focus-loss cursor path |
| `DAT_00bef67d` | unknown | Checked in focus-loss cursor path |
| `DAT_00bef6d8` | delayed op timer | ms countdown; expires → resume audio/updates |
| `DAT_00bef6d7` | update enabled | Game update sub-system enabled flag |
| `DAT_00bef6d4` | audio paused | Audio was paused on focus loss |
| `DAT_00bef6d5` | updates paused | Physics/updates paused on focus loss |
| `DAT_00c82b08` | scene ID | Checked against DAT_00c82ac8 in render mode switch |
| `DAT_00c82b00` | scene ID 2 | Alternate scene ID for render mode switch |
| `DAT_00c82ac8` | target scene | Target scene ID for SwitchRenderOutputMode |
| `DAT_00e6b384` | game state obj | Main game-state object pointer |
| `DAT_00c7b908` | pause state | State check before PauseGameUpdates |
| `DAT_008afb08` | min memory | Minimum available video memory across frames |
| `DAT_00bf192c` | fullscreen flag 2 | Set to 1 in fullscreen path |
| `DAT_00bef6cc` | window handle | Main HWND (duplicate of ghWnd?) |
| `DAT_00bef6d0` | COM object | Created via vtable in WinMain, released on exit |

## Frame Rendering

### Double-Buffer Callback System
The timing system uses a double buffer (flip index `DAT_008e1648`) to smoothly pass timing data to callbacks:
- `UpdateFrameTimingPrimary` stores tick/time in `DAT_00c83128/c83120` indexed by flip
- `InterpolateFrameTime` reads from the opposite buffer to interpolate between two frames
- Callback table at `DAT_008e1644`: `[0]` = primary callback, `[1]` = secondary callback

### Render Output Modes (`SwitchRenderOutputMode` `00612530`)
Called on focus transitions to change the rendering mode/scene.
Uses scene IDs `DAT_00c82b00/08` compared against target `DAT_00c82ac8`.

## Key Components
- **Window Management**: Full Win32 with fullscreen/windowed, placement persistence, session change handling
- **Configuration**: Registry (HKCU/HKLM fallback) with extensive settings, command-line override
- **Rendering**: DirectX 9, device lost recovery, dual render targets, GPU sync query, D3D state tables
- **Input**: DirectInput 8, keyboard/mouse/2 joysticks, acquire/unacquire on focus change
- **Timing**: High-resolution multimedia timer, 16.16 fixed point, 3× game speed multiplier, double-buffered callbacks
- **Audio**: Pause on focus loss, 2-second delayed resume
- **Message Loop**: Integrated with game update, frame limiter, deferred callback queue
