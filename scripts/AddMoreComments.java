// Add comments for newly analyzed functions
//@category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.*;
import ghidra.program.model.symbol.*;

public class AddMoreComments extends GhidraScript {

    @Override
    public void run() throws Exception {

        // DirectX resource release
        addFunctionComment("0067cfb0", "ReleaseDirectXResources",
            "Releases DirectX resources before device reset\n\n" +
            "Released resources:\n" +
            "- DAT_00b95034: Unknown DirectX interface\n" +
            "- DAT_00bf1934: Render target or texture surface\n" +
            "- DAT_00bf1930: Back buffer surface\n" +
            "- DAT_00bf1938: Additional render target\n\n" +
            "Also clears:\n" +
            "- DAT_00af1390: Cached surface pointer\n" +
            "- DAT_00ae9250: Cached surface pointer\n\n" +
            "Calls helper functions:\n" +
            "- thunk_FUN_00ec04dc(): Pre-release cleanup\n" +
            "- thunk_FUN_00ec19b5(): Post-release cleanup");

        // DirectX resource restoration
        addFunctionComment("0067d0c0", "RestoreDirectXResources",
            "Restores DirectX resources after device reset\n\n" +
            "Restoration process:\n" +
            "1. Call initialization functions (FUN_00675950, FUN_0067b820, FUN_0067bb20)\n" +
            "2. Get back buffer from device (GetBackBuffer)\n" +
            "3. Get surface description from back buffer\n" +
            "4. Create render target texture using IDirect3DDevice9::CreateTexture\n" +
            "5. Get surface from texture (GetSurfaceLevel)\n" +
            "6. Set render targets using IDirect3DDevice9::SetRenderTarget\n" +
            "7. Call FUN_00674430() for final initialization\n\n" +
            "Global surfaces set:\n" +
            "- DAT_00bf1930: Back buffer surface\n" +
            "- DAT_00bf1934: Texture surface (or special marker 0xbacb0ffe)\n" +
            "- DAT_00bf1938: Additional render target\n" +
            "- DAT_00af1390, DAT_00ae9250: Cached surface pointers");

        // Game update function
        addFunctionComment("00618140", "GameFrameUpdate",
            "Main game frame update with delta time management\n\n" +
            "Frame timing:\n" +
            "1. Calls FUN_00636830() - pre-update logic\n" +
            "2. Gets current time from FUN_00618010()\n" +
            "3. Calculates delta time since last frame\n" +
            "4. Caps delta at 100ms (0x640000 microseconds) to prevent spiral of death\n" +
            "5. Accumulates delta time in 64-bit counters\n" +
            "6. Multiplies accumulated time by 3 for game speed\n" +
            "7. Calls frame callbacks when timing thresholds are met\n\n" +
            "Global timing variables:\n" +
            "- DAT_00e6e5e0, DAT_00e6e5e4: Last frame time (64-bit)\n" +
            "- DAT_00c83198, DAT_00c8319c: Accumulated game time (64-bit)\n" +
            "- DAT_00c831a8, DAT_00c831ac: Next callback time (64-bit)\n" +
            "- DAT_00c83190, DAT_00c83194: Callback interval (64-bit)\n" +
            "- DAT_008e1648: Frame flip/toggle flag\n\n" +
            "Callback functions:\n" +
            "- FUN_00617f50: Primary frame callback\n" +
            "- FUN_00617ee0: Secondary frame callback\n" +
            "- DAT_008e1644: Function pointer table for callbacks");

        // Input unacquire
        addFunctionComment("0068dac0", "UnacquireInputDevices",
            "Unacquires all DirectInput devices (when losing focus)\n\n" +
            "Devices unacquired:\n" +
            "1. Keyboard (DAT_00e6a070) - IDirectInputDevice8::Unacquire\n" +
            "2. Mouse (DAT_00e6a194) - IDirectInputDevice8::Unacquire\n" +
            "3. Calls Ordinal_5(0) - likely ShowCursor(TRUE)\n" +
            "4. Two joysticks in array at DAT_00e6a42c (stride 0x248 bytes)\n\n" +
            "On failure, calls fatal error function FUN_0066f810\n" +
            "Returns 1 on success");

        // Input acquire
        addFunctionComment("0068da30", "AcquireInputDevices",
            "Acquires all DirectInput devices (when gaining focus)\n\n" +
            "Devices acquired:\n" +
            "1. Keyboard (DAT_00e6a070) - IDirectInputDevice8::Acquire\n" +
            "2. Mouse (DAT_00e6a194) - IDirectInputDevice8::Acquire\n" +
            "3. Calls Ordinal_5(1) - likely ShowCursor(FALSE)\n" +
            "4. Two joysticks in array at DAT_00e6a42c (stride 0x248 bytes)\n\n" +
            "On failure, calls fatal error function FUN_0066f810\n" +
            "Returns 1 on success");

        // Window placement save
        addFunctionComment("0060d220", "SaveWindowPlacement",
            "Saves window position and state to registry before closing\n\n" +
            "Saved settings (in GameSettings):\n" +
            "- PosX: Window X position (adjusted for border)\n" +
            "- PosY: Window Y position (adjusted for border)\n" +
            "- SizeX: Window width (adjusted for border)\n" +
            "- SizeY: Window height (adjusted for border)\n" +
            "- Minimized: \"true\"/\"false\" (SW_SHOWMINIMIZED)\n" +
            "- Maximized: \"true\"/\"false\" (SW_SHOWMAXIMIZED)\n\n" +
            "Uses GetWindowPlacement to get current window state\n" +
            "Adjusts rect using AdjustWindowRect with style 0xcf0000\n" +
            "Calls FUN_0060c670 (WriteRegistrySetting) to save values");

        // Registry write function
        addFunctionComment("0060c670", "WriteRegistrySetting",
            "Writes a string value to Windows Registry\n\n" +
            "Similar to ReadRegistrySetting but writes instead of reads\n" +
            "Registry path: HKEY_CURRENT_USER\\Software\\Electronic Arts\\<app>\\<section>\n" +
            "Creates registry key if it doesn't exist");

        printf("Additional comments added successfully!\n");
    }

    private void addFunctionComment(String addressStr, String functionName, String comment) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addressStr);
            Function func = getFunctionAt(addr);

            if (func != null) {
                // Rename function if not already named properly
                String currentName = func.getName();
                if (currentName.startsWith("FUN_") || !currentName.equals(functionName)) {
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
