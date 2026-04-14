# Architecture of hp.exe

## Overview
`hp.exe` is the main executable for "Harry Potter and the Order of the Phoenix". It is a Windows PE application built using Visual Studio 2005.

## Entry Point
The program starts at `___tmainCRTStartup` (`006f54d2`), which is the standard CRT entry point for a Windows application. It performs runtime initialization and calls the `WinMain` function.

## WinMain (`0060dfa0`)
The `WinMain` function is the core entry point for the game logic. Its primary responsibilities include:
1.  **System Parameters Initialization**: Calls `SystemParametersInfoA` to set or retrieve system-wide parameters (e.g., mouse settings).
2.  **Single Instance Check**: Uses `FindWindowA` to search for a window with the class name `OrderOfThePhoenixMainWndClass`. If an instance is already running, the new process terminates.
3.  **Settings Loading**: Loads game settings such as resolution (`Width`, `ModeXWidth`, etc.), bit depth, and other graphical options from the Windows Registry (`HKEY_CURRENT_USER\Software\Electronic Arts\Harry Potter and the Order of the Phoenix\GameSettings`).
4.  **Window Creation**: Calls `CreateGameWindow` (`0060db20`) to create the main game window with the class `OrderOfThePhoenixMainWndClass`.
5.  **Graphics Initialization**: Performs initialization related to rendering (potentially DirectX, indicated by calls to `FUN_00617bf0` and related data structures).
6.  **Main Loop**: Enters a processing loop (`FUN_0060dc10`) which handles window messages and game updates.
7.  **Termination**: Cleans up resources and terminates the process.

## Key Components
-   **Window Management**: Handled via standard Win32 API calls (`CreateWindowExA`, `ShowWindow`, `UpdateWindow`).
-   **Configuration**: Settings are managed through a set of helper functions that read/write game parameters.
-   **Message Loop**: Centralized in `FUN_0060dc10`.
