# Questions for Further Exploration

## DirectX Initialization
1. What are the exact DirectX device creation parameters?
   - What adapter is selected?
   - What present parameters are used (buffer format, swap effect, etc.)?
   - What device flags and behavior flags are specified?
   - Location: Need to trace calls from WinMain to device creation

2. What resources are released in `FUN_0067cfb0` during device lost recovery?
   - Textures, vertex buffers, index buffers?
   - Render targets?
   - Any custom DirectX resources?

3. What resources are restored in `FUN_0067d0c0` after device reset?
   - How are they recreated?
   - Are there any state settings that need to be reapplied?

## Game State and Update Logic
4. What functions are called during the game update phase in MainGameLoop?
   - What is `FUN_00618140`?
   - What is `thunk_FUN_00eb6dbc`?
   - What is `FUN_0058b8a0`?
   - What is the purpose of `DAT_008afb08`?

5. What is the target frame rate?
   - How is `uStack_44` calculated and what does it represent?
   - What are the constants `DAT_008475d8`, `DAT_00845594`, `DAT_00845320`?

6. What triggers the delayed operation timer (`DAT_00bef6d8`)?
   - Why is it set to 2000ms on focus loss?
   - What happens when the timer expires?

## Registry Settings
7. What other settings are loaded from the registry besides the ones documented?
   - Are there audio settings?
   - Control/input settings?
   - Difficulty or gameplay settings?

8. How are default values determined if registry keys don't exist?
   - Are there hardcoded defaults?
   - Are defaults read from a configuration file?

## Global Variables and Flags
9. What do the various global flags control?
   - `DAT_00bef67c`: Purpose unknown
   - `DAT_00bef67d`: Purpose unknown
   - `DAT_00bef6d4`: Purpose unknown
   - `DAT_00bef6d5`: Purpose unknown
   - `DAT_00c82b08`, `DAT_00c82b00`, `DAT_00c82ac8`: What do these represent?

10. What is `DAT_00e6b384` and what structure does it point to?
    - Accessed in both MainGameLoop and WindowProc
    - Has an offset +0xc that points to another structure
    - Seems related to game state management

11. What is `DAT_00bf1920` and what is its full structure?
    - Confirmed to be IDirect3DDevice9* or similar
    - What other DirectX interfaces are stored globally?

## Window and Input Management
12. What is `FUN_00617b60`?
    - Called during focus loss/activation changes
    - Seems related to graphics or input state

13. What is `FUN_0068dac0` and `FUN_0068da30`?
    - Called during activation/deactivation
    - Paired calls suggest state save/restore

14. What is `FUN_0060d220`?
    - Called in WM_CLOSE before DestroyWindow
    - Cleanup function?

15. What is the cursor management strategy?
    - Why multiple ShowCursor calls in loops?
    - What is the reference counting behavior?

## System Parameters
16. Why are these specific system parameters modified?
    - SPI_SETMOUSESPEED (0x3a/0x3b)
    - SPI_SETMOUSE (0x34/0x35)
    - SPI_SETSCREENREADER (0x32/0x33)
    - What are the exact values being set?

17. What are the data structures at `DAT_008afc44`, `DAT_008afc4c`, `DAT_008afc54`?
    - These store the saved system parameters
    - What is their exact format?

## Error Handling
18. How does the game handle DirectX device creation failure?
    - What fallback mechanisms exist?
    - Are there any user-facing error messages?

19. How are registry read/write errors handled?
    - What happens if registry access is denied?
    - C++ exceptions are thrown - are they caught somewhere?

## Command Line Arguments
20. What command line arguments does the game support?
    - param_3 in WinMain is lpCmdLine
    - It's copied to `DAT_00c82b88` and `DAT_00c82d88`
    - How are these arguments parsed and used?
