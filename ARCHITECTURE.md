# Architecture of hp.exe

## Overview
`hp.exe` is the main executable for "Harry Potter and the Order of the Phoenix". It is a Windows PE application built using Visual Studio 2005 that uses DirectX 9 for rendering.

## Entry Point
The program starts at `___tmainCRTStartup` (`006f54d2`), which is the standard CRT entry point for a Windows application. It performs runtime initialization and calls the `WinMain` function.

## WinMain (`0060dfa0`)
The `WinMain` function is the core entry point for the game logic. Its primary responsibilities include:

1.  **FPU Configuration**: Sets FPU control word using `_control87(0x20000, 0x30000)` to prevent denormal exceptions
2.  **System Parameters Management**:
    - Saves current Windows system parameters (mouse acceleration, screen reader settings)
    - Disables mouse acceleration for optimal gaming performance
    - Uses `SaveOrRestoreSystemParameters` (`0060deb0`) for this purpose
3.  **Single Instance Check**: Uses `FindWindowA` to search for a window with the class name `OrderOfThePhoenixMainWndClass`. If an instance is already running, the new process terminates.
4.  **Window Class Registration**: Calls `RegisterWindowClass` (`00eb4b95`) to register `OrderOfThePhoenixMainWndClass`
5.  **Settings Loading**: Loads game settings from Windows Registry via `ReadRegistrySetting` (`0060ce60`):
    - Resolution: `Width`, `ModeXWidth`, etc.
    - Graphics: `BitDepth`, `ShadowLOD`, `MaxFarClip`, `ParticleRate`
    - Registry path: `HKEY_CURRENT_USER\Software\Electronic Arts\Harry Potter and the Order of the Phoenix\GameSettings`
    - Falls back to `HKEY_LOCAL_MACHINE` if HKCU doesn't exist
6.  **Window Creation**: Calls `CreateGameWindow` (`0060db20`) to create the main game window
7.  **Graphics Initialization**: Initializes DirectX 9 rendering system
8.  **Main Loop**: Enters `MainGameLoop` (`0060dc10`) which handles window messages and game updates
9.  **Cleanup and Termination**: Restores system parameters and terminates

## System Parameters Management (`0060deb0`)
This function saves or restores Windows system settings:
- **param_1 = 0**: Save current parameters and disable mouse acceleration
- **param_1 = 1**: Restore original parameters

Modified system parameters:
- `SPI_SETMOUSESPEED` (0x3a/0x3b): Mouse speed
- `SPI_SETMOUSE` (0x34/0x35): Mouse acceleration curves
- `SPI_SETSCREENREADER` (0x32/0x33): Screen reader flags

The game clears bit 0 and bits 2-3 of the flags to disable acceleration.

## Registry Settings (`0060ce60`)
Reads game configuration from the Windows Registry:
- Constructs path: `Software\Electronic Arts\<app_name>\<section>`
- Searches `HKEY_CURRENT_USER` first, then `HKEY_LOCAL_MACHINE`
- Creates key in HKCU with default value if neither location exists
- Returns integer value or provided default

## Window Management

### Window Class Registration (`00eb4b95`)
Registers the main window class with properties:
- Class name: `OrderOfThePhoenixMainWndClass`
- Style: `CS_DBLCLKS | CS_OWNDC | CS_VREDRAW | CS_HREDRAW`
- Background: Black brush
- Cursor: Arrow
- First calls `UnregisterClassA` to handle crashed previous instances

### Window Creation (`0060db20`)
Creates the main game window:
- Uses global settings for width/height and fullscreen mode
- Window styles:
  - Fullscreen: `WS_POPUP` (no borders)
  - Windowed: `WS_OVERLAPPEDWINDOW` (standard window)
- Calls `AdjustWindowRectEx` to account for borders

### Window Procedure (`0060d6d0`)
Handles window messages with special fullscreen behavior:

**Key messages:**
- `WM_DESTROY` (0x02): Shows cursor and posts quit message
- `WM_ACTIVATE` (0x06): Handles focus changes
  - Deactivate: Shows cursor, pauses game, sets 2000ms delay timer
  - Activate: Hides cursor, resumes game
- `WM_SETFOCUS`/`WM_SETCURSOR` (0x07/0x20): Manages cursor visibility
- `WM_PAINT` (0x0f): Different handling for fullscreen (ValidateRect) vs windowed (BeginPaint/EndPaint)
- `WM_CLOSE` (0x10): Calls cleanup before destroying window
- `WM_SYSCOMMAND` (0x112): Blocks system commands in fullscreen:
  - `SC_MAXIMIZE`, `SC_SIZE`, `SC_MOVE`, `SC_KEYMENU` are blocked
- `WM_NCHITTEST` (0x84): Returns `HTCLIENT` in fullscreen
- `WM_ENTERMENULOOP` (0x120): Returns non-zero in fullscreen

**Global flags:**
- `DAT_008afbd9`: Fullscreen mode flag
- `DAT_00bef6c7`: Window has focus flag
- `DAT_00bef6d8`: Delayed operation timer (2000ms on deactivate)

## Main Game Loop (`0060dc10`)

The main game loop runs continuously until `WM_QUIT` is received:

### Loop Structure:
1. **DirectX Device Management**: Calls `UpdateDirectXDevice` (`0067d310`)
2. **Focus State Handling**:
   - When focus lost (`DAT_00bef6c7`): Shows cursor, pauses rendering
   - When focus regained: Hides cursor, resumes rendering
3. **Message Processing**: Uses `PeekMessageA` to process Windows messages
4. **Game Update** (when no messages):
   - Calculates frame delta time using `GetTickCount`
   - Updates game state
   - Implements frame rate limiting with `Sleep(0)`
5. **Delayed Operations**: Manages `DAT_00bef6d8` timer for deferred tasks

### Global State Variables:
- `DAT_00bef6c5`: Exit request flag
- `DAT_008afbd9`: Fullscreen mode
- `DAT_00bef6c7`: Window has focus
- `DAT_00bef67e`: Cursor visibility state
- `DAT_00bef67c`, `DAT_00bef67d`: Additional state flags
- `DAT_00bef6d8`: Delayed operation timer (milliseconds)
- `DAT_00bef6d7`: Game update enable flag
- `DAT_00c83188`: Frame timing data

## DirectX Device Management (`0067d310`)

Monitors and maintains DirectX 9 device state:

### Device Lost Handling:
- Checks device state via `IDirect3DDevice9::TestCooperativeLevel`
- Handles error codes:
  - `D3DERR_DEVICELOST` (0x88760868): Device lost, sleep 50ms and retry
  - `D3DERR_DEVICENOTRESET` (0x88760869): Device ready for reset

### Recovery Process:
1. Release DirectX resources (calls `FUN_0067cfb0`)
2. Call `IDirect3DDevice9::Reset` with saved parameters
3. Restore DirectX resources (calls `FUN_0067d0c0`)

### Global State:
- `DAT_00bf1920`: Pointer to IDirect3DDevice9 interface
- `DAT_00bf18aa`: Device lost flag

## Key Components
-   **Window Management**: Standard Win32 API with fullscreen/windowed mode support
-   **Configuration**: Registry-based settings with HKCU/HKLM fallback
-   **Rendering**: DirectX 9 with device lost recovery
-   **Input**: Mouse acceleration disabled, cursor managed based on focus
-   **Message Loop**: Integrated with game update loop and frame limiting
