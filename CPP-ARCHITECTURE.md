# C++ Decompilation Architecture

## Overview
The C++ implementation is a faithful reconstruction of the original `hp.exe` executable for "Harry Potter and the Order of the Phoenix". It targets Windows x86 (32-bit) and uses DirectX 9 and DirectInput 8 for graphics and input.

## Build System
- **Compiler**: Zig cross-compiler (zig c++)
- **Target**: x86-windows-gnu (32-bit Windows)
- **Output**: hp_decompiled.exe (PE32 executable)
- **Dependencies**: user32, gdi32, ole32, uuid, winmm
- **Build script**: build.sh in decompilation-src/

## Entry Point and Initialization

### WinMain
The entry point performs initialization in this order:

1. **FPU Configuration**: `_control87(0x20000, 0x30000)` - Prevents denormal floating point exceptions
2. **System Parameter Management**: Calls `SaveOrRestoreSystemParameters(false)` to save and disable mouse acceleration
3. **Command Line Storage**: Saves command line arguments to `g_szCmdLine1` and `g_szCmdLine2`
4. **Single Instance Check**: Uses `FindWindowA` to prevent multiple instances
5. **Window Class Registration**: Registers "OrderOfThePhoenixMainWndClass"
6. **Settings Loading**: Reads game configuration from Windows Registry
7. **Window Creation**: Creates main game window (fullscreen or windowed)
8. **DirectX Initialization**: (Not yet implemented - TODO)
9. **DirectInput Initialization**: (Not yet implemented - TODO)
10. **Main Loop**: Enters message and game loop
11. **Cleanup**: Saves window placement and restores system parameters

## Configuration System

### Registry Structure
Settings are stored in the Windows Registry:
- Base path: `HKEY_CURRENT_USER\Software\Electronic Arts\Harry Potter and the Order of the Phoenix\GameSettings`
- Fallback: `HKEY_LOCAL_MACHINE\...\GameSettings` (read-only)
- Auto-creation: Keys are created in HKCU if they don't exist

### ReadRegistrySetting
Reads integer values from registry:
1. Try HKEY_CURRENT_USER first
2. If not found, try HKEY_LOCAL_MACHINE
3. If neither exists, create in HKCU with default value
4. Returns: Integer value or default

### WriteRegistrySetting
Writes string values to HKCU registry:
- Creates registry key path if needed
- Always writes to HKCU (never HKLM)
- Used for saving window placement and settings

### Loaded Settings
- `Width`, `Height`: Screen resolution
- `BitDepth`: Color depth (16/32-bit)
- `ShadowLOD`: Shadow level of detail
- `MaxFarClip`: Maximum draw distance
- `ParticleRate`: Particle system density
- `Fullscreen`: Fullscreen mode flag

### Saved Settings (on exit)
- `PosX`, `PosY`: Window position (border-adjusted)
- `SizeX`, `SizeY`: Window size (border-adjusted)
- `Minimized`: Window minimized state ("true"/"false")
- `Maximized`: Window maximized state ("true"/"false")

## System Parameter Management

### SaveOrRestoreSystemParameters
Saves or restores Windows system-wide settings to optimize for gaming:

**When saving (restore=false):**
1. Reads current values using `SystemParametersInfoA` with GET codes (0x3a, 0x34, 0x32)
2. Stores in `g_savedParams` structure
3. Modifies flags by clearing bits 0, 2, and 3
4. Writes modified values using SET codes (0x3b, 0x35, 0x33)

**Modified parameters:**
- SPI_MOUSESPEED (0x3a/0x3b): Mouse movement speed
- SPI_MOUSE (0x34/0x35): Mouse acceleration curves
- SPI_SCREENREADER (0x32/0x33): Screen reader accessibility flags

**When restoring (restore=true):**
Writes back original saved values using SET codes

## Window Management

### Window Class
- Name: "OrderOfThePhoenixMainWndClass"
- Style: `CS_DBLCLKS | CS_OWNDC | CS_VREDRAW | CS_HREDRAW`
- Background: Black brush
- Cursor: Arrow
- Pre-unregister: Calls `UnregisterClassA` first to handle crashed instances

### Window Creation
Creates window with:
- Fullscreen style: `WS_POPUP` (no borders)
- Windowed style: `WS_OVERLAPPEDWINDOW` (standard borders)
- Rect adjustment: Uses `AdjustWindowRectEx` to account for borders
- Default position: `CW_USEDEFAULT` (Windows decides)
- Size: Adjusted for borders based on desired client area

### Window Procedure (WindowProc)
Handles window messages with special fullscreen behavior:

**WM_DESTROY (0x02):**
- Shows cursor if in fullscreen
- Posts quit message

**WM_ACTIVATE (0x06):**
- On deactivate (fullscreen):
  - Unacquires input devices
  - Shows cursor
  - Sets `bHasFocus = false`
  - Sets delayed operation timer to 2000ms
- On activate (fullscreen):
  - Acquires input devices
  - Hides cursor
  - Sets `bHasFocus = true`
  - Sets delayed operation timer to 2000ms

**WM_SETFOCUS/WM_SETCURSOR (0x07/0x20):**
- Manages cursor visibility based on `bHasFocus`
- Uses ShowCursor loops to ensure proper reference count

**WM_PAINT (0x0f):**
- Fullscreen: `ValidateRect` only
- Windowed: Standard `BeginPaint`/`EndPaint`

**WM_CLOSE (0x10):**
- Saves window placement before destroying

**WM_SYSCOMMAND (0x112):**
- Blocks in fullscreen: SC_MAXIMIZE, SC_SIZE, SC_MOVE, SC_KEYMENU

**WM_NCHITTEST (0x84):**
- Returns HTCLIENT in fullscreen

**WM_ENTERMENULOOP (0x120):**
- Returns non-zero to prevent menu in fullscreen

### SaveWindowPlacement
Saves current window state to registry before exit:
1. Calls `GetWindowPlacement` to get current state
2. Adjusts rect using `AdjustWindowRect` with style 0xcf0000
3. Saves: PosX, PosY, SizeX, SizeY, Minimized, Maximized

## DirectX 9 Management

### Device Lost Handling (UpdateDirectXDevice)
Monitors device state and handles recovery:

**Normal operation:**
- `TestCooperativeLevel` returns D3D_OK
- No action needed

**Device lost (D3DERR_DEVICELOST = 0x88760868):**
- Sleep 50ms and return
- Wait for device to become ready for reset

**Device ready for reset (D3DERR_DEVICENOTRESET = 0x88760869):**
1. Call `ReleaseDirectXResources()`
2. Prepare D3DPRESENT_PARAMETERS
3. Call `IDirect3DDevice9::Reset`
4. Call `RestoreDirectXResources()`

### Resource Release (ReleaseDirectXResources)
Releases COM objects before device reset:
- `g_pRenderTarget`: Additional render target surface
- `g_pBackBuffer`: Back buffer surface
- Sets pointers to NULL after release

### Resource Restoration (RestoreDirectXResources)
Recreates resources after device reset:
1. Get back buffer: `IDirect3DDevice9::GetBackBuffer`
2. Get surface description from back buffer
3. Create render target texture with same format
4. Get surface level from texture
5. Set render targets on device

## DirectInput Management

### Input Devices
- `g_pKeyboard`: IDirectInputDevice8* for keyboard
- `g_pMouse`: IDirectInputDevice8* for mouse
- `g_pJoystick[2]`: Array of 2 joystick devices

### UnacquireInputDevices
Called when losing window focus:
1. Unacquire keyboard: `IDirectInputDevice8::Unacquire`
2. Unacquire mouse
3. Show cursor: `ShowCursor(TRUE)`
4. Unacquire both joysticks

### AcquireInputDevices
Called when gaining window focus:
1. Acquire keyboard: `IDirectInputDevice8::Acquire`
2. Acquire mouse
3. Hide cursor: `ShowCursor(FALSE)`
4. Acquire both joysticks

## Game Loop and Timing

### MainLoop
Runs continuously until exit requested:

**Per-frame sequence:**
1. `UpdateDirectXDevice()` - Check/recover device state
2. Handle focus state - Show/hide cursor appropriately
3. `PeekMessageA` - Process Windows messages (non-blocking)
4. If WM_QUIT received, set `g_bExitRequested` and break
5. If no messages, update game:
   - Call `GameFrameUpdate()`
   - Handle delayed operation timer
   - Frame rate limiting with `Sleep(0)`

### GameFrameUpdate
Implements frame timing with delta capping:

**Timing algorithm:**
1. Get current time: `GetTickCount()`
2. Calculate delta: `currentTime - g_dwLastFrameTime`
3. **Cap delta at 100ms** to prevent "spiral of death"
4. Update `g_dwLastFrameTime = currentTime`
5. **Accumulate time**: `g_ullAccumulatedTime += deltaTime * 3`
   - Multiplied by 3 for game speed
6. Trigger game updates based on accumulated time

**Purpose of delta cap:**
- Prevents massive time jumps when game hitches
- Keeps game responsive even when framerate drops
- Standard technique to avoid physics instability

### Delayed Operation Timer
`g_dwDelayedOpTimer` is set to 2000ms when focus changes:
- Counts down each frame
- When reaches 0, performs deferred operations
- Prevents immediate state changes on rapid focus switching

## Global State

### Window State
- `ghWnd`: Main window handle
- `bIsFullscreen`: Fullscreen mode flag
- `bHasFocus`: Window has keyboard focus
- `gWidth`, `gHeight`: Screen/window resolution

### DirectX State
- `g_pD3D`: IDirect3D9 interface
- `g_pd3dDevice`: IDirect3DDevice9 interface
- `g_pBackBuffer`: Back buffer surface
- `g_pRenderTarget`: Additional render target

### DirectInput State
- `g_pDirectInput`: IDirectInput8 interface
- `g_pKeyboard`, `g_pMouse`: Input devices
- `g_pJoystick[2]`: Joystick devices

### Timing State
- `g_dwLastFrameTime`: Last frame timestamp (milliseconds)
- `g_ullAccumulatedTime`: Total accumulated game time
- `g_bExitRequested`: Exit request flag

### System State
- `g_savedParams`: Saved Windows system parameters
- `g_dwDelayedOpTimer`: Delayed operation countdown
- `g_szCmdLine1`, `g_szCmdLine2`: Command line arguments

## Key Design Decisions

1. **FPU Control**: Explicitly set to prevent denormal exceptions that slow down floating point math
2. **System Parameters**: Temporarily disable mouse acceleration for consistent input
3. **Single Instance**: Use FindWindowA to prevent multiple game instances
4. **Registry Fallback**: Try HKCU first, then HKLM, create in HKCU if missing
5. **Window Persistence**: Save exact position and state for next launch
6. **Device Lost Handling**: Proper DirectX resource release/restore cycle
7. **Input Management**: Acquire/unacquire DirectInput devices on focus changes
8. **Frame Delta Cap**: 100ms maximum to prevent physics instability
9. **Game Speed**: 3x multiplier on accumulated time
10. **Delayed Operations**: 2000ms timer to prevent rapid state changes

## Compilation
Built using Zig cross-compiler for Windows x86:
```bash
zig c++ -target x86-windows-gnu -lc++ -luser32 -lgdi32 -lole32 -luuid -lwinmm main.cpp -o hp_decompiled.exe
```

Output: 776KB PE32 executable for Windows
