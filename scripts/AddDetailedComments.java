// Add detailed comments to key functions in hp.exe
//@category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.*;
import ghidra.program.model.symbol.*;

public class AddDetailedComments extends GhidraScript {

    @Override
    public void run() throws Exception {

        // WinMain function (0060dfa0)
        addFunctionComment("0060dfa0", "WinMain - Main entry point for the game",
            "This function performs the following initialization:\n" +
            "1. Sets FPU control word to prevent denormal exceptions\n" +
            "2. Saves current system parameters (mouse acceleration, screen reader settings)\n" +
            "3. Disables mouse acceleration for gaming\n" +
            "4. Checks for existing instance using FindWindowA\n" +
            "5. Registers window class 'OrderOfThePhoenixMainWndClass'\n" +
            "6. Reads game settings from registry:\n" +
            "   - Width, BitDepth, ShadowLOD, MaxFarClip, ParticleRate, etc.\n" +
            "7. Creates main game window\n" +
            "8. Initializes DirectX/graphics\n" +
            "9. Enters main game loop\n" +
            "10. Restores system parameters on exit");

        // System Parameters function (0060deb0)
        addFunctionComment("0060deb0", "SaveOrRestoreSystemParameters",
            "Saves or restores Windows system parameters\n" +
            "param_1: 0 = save current and disable, 1 = restore original\n" +
            "Parameters modified:\n" +
            "- SPI_SETMOUSESPEED (0x3a/0x3b): Mouse speed\n" +
            "- SPI_SETMOUSE (0x34/0x35): Mouse acceleration\n" +
            "- SPI_SETSCREENREADER (0x32/0x33): Screen reader flags\n" +
            "The game clears bit 0 and bits 2-3 to disable acceleration");

        // Registry reading function (0060ce60)
        addFunctionComment("0060ce60", "ReadRegistrySetting",
            "Reads a game setting from Windows Registry\n" +
            "param_1: Application name (e.g., 'Harry Potter and the Order of the Phoenix')\n" +
            "param_2: Section/subkey name (e.g., 'GameSettings')\n" +
            "param_3: Default value if key doesn't exist\n" +
            "Returns: Integer value from registry or default\n\n" +
            "Registry path: HKEY_CURRENT_USER\\Software\\Electronic Arts\\<param_1>\\<param_2>\n" +
            "Falls back to HKEY_LOCAL_MACHINE if HKCU doesn't exist\n" +
            "Creates key in HKCU if neither location has the setting");

        // Main game loop (0060dc10)
        addFunctionComment("0060dc10", "MainGameLoop",
            "Main game loop that runs until WM_QUIT is received\n\n" +
            "Loop structure:\n" +
            "1. Call DirectX device update (FUN_0067d310)\n" +
            "2. Handle focus changes (show/hide cursor, pause/resume)\n" +
            "3. Process Windows messages with PeekMessageA\n" +
            "4. If no messages, update game state:\n" +
            "   - Calculate frame time and delta\n" +
            "   - Call game update functions\n" +
            "   - Implement frame rate limiting with Sleep()\n" +
            "5. Handle delayed operations (DAT_00bef6d8 timer)\n\n" +
            "Global flags used:\n" +
            "- DAT_00bef6c5: Exit flag\n" +
            "- DAT_008afbd9: Fullscreen mode flag\n" +
            "- DAT_00bef6c7: Window has focus flag\n" +
            "- DAT_00bef67e: Cursor visibility state\n" +
            "- DAT_00bef6d8: Delayed operation timer");

        // DirectX device management (0067d310)
        addFunctionComment("0067d310", "UpdateDirectXDevice",
            "Checks and maintains DirectX device state\n\n" +
            "Handles device lost scenarios:\n" +
            "- D3DERR_DEVICELOST (0x88760868): Device is lost, sleep and retry\n" +
            "- D3DERR_DEVICENOTRESET (0x88760869): Device can be reset, attempt recovery\n\n" +
            "Recovery process:\n" +
            "1. Release resources (FUN_0067cfb0)\n" +
            "2. Call IDirect3DDevice9::Reset\n" +
            "3. Restore resources (FUN_0067d0c0)\n\n" +
            "Sets DAT_00bf18aa flag when device is lost");

        // Window procedure
        addFunctionComment("0060d6d0", "WindowProc",
            "Main window procedure for the game window\n\n" +
            "Key message handling:\n" +
            "- WM_DESTROY (0x02): Show cursor, post quit message\n" +
            "- WM_ACTIVATE (0x06): Handle focus changes\n" +
            "  * Deactivate: Show cursor, pause game, set 2000ms delay\n" +
            "  * Activate: Hide cursor, resume game\n" +
            "- WM_SETFOCUS/WM_SETCURSOR (0x07/0x20): Manage cursor visibility\n" +
            "- WM_PAINT (0x0f): Different handling for fullscreen vs windowed\n" +
            "- WM_CLOSE (0x10): Call cleanup before destroying window\n" +
            "- WM_SYSCOMMAND (0x112): Block certain commands in fullscreen\n" +
            "  * SC_MAXIMIZE, SC_SIZE, SC_MOVE, SC_KEYMENU blocked\n" +
            "- WM_NCHITTEST (0x84): Return HTCLIENT in fullscreen\n" +
            "- WM_ENTERMENULOOP (0x120): Return non-zero in fullscreen\n" +
            "- WM_WTSSESSION_CHANGE (0x218): Handle session changes");

        // Window creation
        addFunctionComment("0060db20", "CreateGameWindow",
            "Creates and configures the main game window\n\n" +
            "Uses global settings:\n" +
            "- DAT_00bf1940: Screen width\n" +
            "- DAT_008afbd9: Fullscreen flag\n\n" +
            "Window styles:\n" +
            "- Fullscreen: WS_POPUP (no borders)\n" +
            "- Windowed: WS_OVERLAPPEDWINDOW (standard window)\n" +
            "Adjusts window rect to account for borders using AdjustWindowRectEx");

        // Register window class
        addFunctionComment("00eb4b95", "RegisterWindowClass",
            "Registers the window class 'OrderOfThePhoenixMainWndClass'\n\n" +
            "First calls UnregisterClassA to ensure clean state\n" +
            "(handles case where previous instance crashed)\n\n" +
            "Window class properties:\n" +
            "- Style: CS_DBLCLKS | CS_OWNDC | CS_VREDRAW | CS_HREDRAW\n" +
            "- Background: Black brush\n" +
            "- Cursor: Arrow\n" +
            "- Window procedure: WindowProc (0060d6d0)");

        printf("Comments added successfully!\n");
    }

    private void addFunctionComment(String addressStr, String functionName, String comment) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addressStr);
            Function func = getFunctionAt(addr);

            if (func != null) {
                // Rename function if not already named
                String currentName = func.getName();
                if (currentName.startsWith("FUN_")) {
                    func.setName(functionName, SourceType.USER_DEFINED);
                    printf("Renamed %s to %s\n", addressStr, functionName);
                }

                // Set function comment
                func.setComment(comment);
                printf("Added comment to %s (%s)\n", functionName, addressStr);
            } else {
                printf("WARNING: No function found at %s\n", addressStr);
            }
        } catch (Exception e) {
            printf("ERROR processing %s: %s\n", addressStr, e.getMessage());
        }
    }
}
