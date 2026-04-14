// Add/update comments for iteration 3 findings
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.symbol.*;

public class AddCommentsIter3 extends GhidraScript {

    @Override
    public void run() throws Exception {
        Listing listing = currentProgram.getListing();
        FunctionManager fm = currentProgram.getFunctionManager();

        // thunk_FUN_00eb787a: NOT COM init - it's a CLI CommandParser method.
        // Parses "-name=value" tokens into key/value arrays on the CommandParser object.
        setFunctionComment(fm, "00eb787a",
            "CLI::CommandParser method: parses '-name=value' command-line tokens.\n" +
            "EDI = this (CLI::CommandParser object).\n" +
            "Fills name array at this+0x480 and value array at this+0x500 (up to 0x20 entries).\n" +
            "Positional args stored at this+0x400.\n" +
            "Called from WinMain initialization chain via thunk wrapper before single-instance check.");
        renameFunctionIfUnnamed(fm, "00eb787a", "CLI_CommandParser_ParseArgs");

        // FUN_00ea53ca: Update cursor visibility state and switch render scene.
        setFunctionComment(fm, "00ea53ca",
            "UpdateCursorVisibilityAndScene: called with new cursor state in AL register.\n" +
            "Compares AL (new cursor-visible flag) vs DAT_00bef67e (current state).\n" +
            "If changed:\n" +
            "  AL=0 (cursor hidden, gained focus): if DAT_00c82b08 != DAT_00c82ac8, calls SwitchRenderOutputMode(&DAT_00c82b08)\n" +
            "  AL!=0 (cursor shown, lost focus):  if DAT_00c82b00 != DAT_00c82ac8, calls SwitchRenderOutputMode(&DAT_00c82b00)\n" +
            "This function IS the 'thunk_FUN_00ea53ca' stub referenced in WinMain/WindowProc.\n" +
            "Called on WM_ACTIVATE focus-gain and focus-loss paths.");
        renameFunctionIfUnnamed(fm, "00ea53ca", "UpdateCursorVisibilityAndScene");

        // FUN_00ec6610: Render+audio teardown on exit.
        setFunctionComment(fm, "00ec6610",
            "RenderAndAudioTeardown: full cleanup called from WinMain on exit.\n" +
            "If DAT_00bf1b1c set: calls thunk_FUN_00eb584e(), FUN_006119c0(), clears DAT_00bf1b18/1c\n" +
            "Then calls FUN_006ace30() (audio teardown), FUN_006108c0() (audio 2), FUN_006ac930() (audio 3)\n" +
            "If DAT_00bf1b2c set: closes handle DAT_00bf1b34 (renderer thread handle?)");
        renameFunctionIfUnnamed(fm, "00ec6610", "RenderAndAudioTeardown");

        // FUN_00ec67e8: Audio stream resume with timing correction.
        setFunctionComment(fm, "00ec67e8",
            "AudioStream_Resume: resumes a paused audio stream and adjusts playback position.\n" +
            "this (in EAX) = audio stream/track object.\n" +
            "  this+0x1c = audio timer pointer; this+0x28 = pause flag (cleared)\n" +
            "Computes elapsed time since pause via GetGameTime() and advances timer position.\n" +
            "If pitch == 0x1000 (normal): adds elapsed time directly to timer.\n" +
            "Else: scales elapsed by pitch/0x1000 (pitch-speed correction).\n" +
            "Updates this+0x17 (total playback time counter).\n" +
            "If this+0x12 == 0 and *this != 0: calls thunk_FUN_00ec693d(0x1000) (seek/resume decoder)\n" +
            "If this+0x1c != 0: calls thunk_FUN_00ec66f1() (finalize resume)");
        renameFunctionIfUnnamed(fm, "00ec67e8", "AudioStream_Resume");

        // GetOrInitCallbackManager (thunk_FUN_00eb5c3e): singleton getter.
        setFunctionComment(fm, "00eb5c3e",
            "GetOrInitCallbackManager: returns &DAT_00e6e870 (frame callback manager singleton).\n" +
            "One-shot init guard at _DAT_00e74c20 bit 0:\n" +
            "  First call: initializes DAT_00e6e870/874, registers atexit cleanup (FUN_007b5640)\n" +
            "  Returns &DAT_00e6e870 always.\n" +
            "DAT_00e6e870 = primary callback entry, DAT_00e6e874 = secondary callback entry.\n" +
            "This is NOT a COM object creator; it is the frame-callback manager.");

        // FUN_00614370: CLI/timing/device init (first step of InitDirectXAndSubsystems)
        setFunctionComment(fm, "00614370",
            "InitCLIAndTimingAndDevice: first major init inside InitDirectXAndSubsystems.\n" +
            "1. Allocates CLI::CommandParser (FUN_00614210(0xc, 'CLI::CommandParser'))\n" +
            "2. One-shot InitFrameCallbackSystem guard\n" +
            "3. Resets timing globals (DAT_00c83110, c83114, c83118, c8311c, c83128/c, c83120/4, c83130)\n" +
            "4. Calls FUN_0067c290(height,?,0,0,6,1,1,1,1) = CreateD3DDevice()\n" +
            "5. On D3DERR result (-0x7fffbffb), calls ShowCursor(1)\n" +
            "6. Checks 'oldgen' cmdline flag -> sets DAT_008ae1ff=1\n" +
            "7. Calls initial render callback via (*(*DAT_00c8c580))(0,0)");
        renameFunctionIfUnnamed(fm, "00614370", "InitCLIAndTimingAndDevice");

        // FUN_0060c2e0: second major init step
        setFunctionComment(fm, "0060c2e0",
            "InitEngineObjects: second init step of InitDirectXAndSubsystems.\n" +
            "Allocates/initializes engine subsystem objects:\n" +
            "  GlobalTempBuffer (0x3c bytes) -> DAT_00e6b378\n" +
            "  RealGraphSystem (8 bytes) -> DAT_00e6b390 (vtable PTR_FUN_00885010)\n" +
            "  RealInputSystem (0x34 bytes) -> FUN_00617890() init\n" +
            "  Locale (0x8c bytes) -> DAT_00e6b304\n" +
            "  FMV (0x18 bytes) -> DAT_00e6b2dc; checks 'nofmv' cmdline flag\n" +
            "Also calls thunk_FUN_00eb87ba, thunk_FUN_00eb88b2, FUN_006677c0");
        renameFunctionIfUnnamed(fm, "0060c2e0", "InitEngineObjects");

        // FUN_006147f0: finalize device setup, register message handlers
        setFunctionComment(fm, "006147f0",
            "FinalizeDeviceSetup: third init step of InitDirectXAndSubsystems.\n" +
            "Sets DAT_008e1644 = &PTR_PTR_008d0f94 (the frame callback table pointer).\n" +
            "Registers message handlers via thunk_FUN_00eb59ce:\n" +
            "  iMsgDeleteEventHandler, iMsgDeleteEntity, iMsgOnDeleteEntity\n" +
            "  iMsgDoRender, iMsgDoRenderDirectorsCamera, iMsgPreShowRaster\n" +
            "  iMsgStartSystem, iMsgStopSystem\n" +
            "Sets DAT_00bef750=1 (device ready flag)\n" +
            "Sets DAT_00bef754 = 'showfps' cmdline flag\n" +
            "Sets DAT_00bef6d7 = 'memorylwm' cmdline flag (enables memory low-water-mark tracking)");
        renameFunctionIfUnnamed(fm, "006147f0", "FinalizeDeviceSetup");

        // FUN_00eb60d3: audio init (final step of InitDirectXAndSubsystems)
        setFunctionComment(fm, "00eb60d3",
            "InitAudioSubsystem: called as last step by InitDirectXAndSubsystems.\n" +
            "Checks DAT_00bf1b10 (audio present flag); if zero, returns true (no audio).\n" +
            "If audio hardware present:\n" +
            "  FUN_006a9080(): allocate/open audio device\n" +
            "  FUN_006a91a0(): query audio format/caps (expects 0x80 = success)\n" +
            "  FUN_006a9140(device, 0): configure audio output\n" +
            "  FUN_006a90e0(): start audio stream\n" +
            "Returns true on success, false on failure.");
        renameFunctionIfUnnamed(fm, "00eb60d3", "InitAudioSubsystem");

        // PauseGraphicsState: method on game state object (called via EAX)
        setFunctionComment(fm, "00617b60",
            "PauseGraphicsState: method on game-state object (in_EAX = this).\n" +
            "Called on WM_ACTIVATE focus-loss and WM_SETFOCUS while unfocused.\n" +
            "  If *(this+0xc) != 0: calls vtable[0x10] on sub-object -> gets device ptr\n" +
            "  At device ptr offset 0x218: gets input device\n" +
            "  Calls vtable[0x20] on input device (unacquire/pause)\n" +
            "NOTE: this function is called with 'this' passed in EAX (custom calling convention).\n" +
            "The caller in WindowProc likely loads DAT_00e6b384 into EAX before calling.");

        println("AddCommentsIter3.java: comments added successfully.");
    }

    private void setFunctionComment(FunctionManager fm, String addrStr, String comment) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            Function f = fm.getFunctionAt(addr);
            if (f != null) {
                f.setComment(comment);
                println("Set comment on: " + f.getName() + " @ " + addrStr);
            } else {
                println("No function at: " + addrStr);
            }
        } catch (Exception e) {
            println("Error setting comment at " + addrStr + ": " + e.getMessage());
        }
    }

    private void renameFunctionIfUnnamed(FunctionManager fm, String addrStr, String newName) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            Function f = fm.getFunctionAt(addr);
            if (f != null) {
                String existing = f.getName();
                // Only rename if it still has the default FUN_ name
                if (existing.startsWith("FUN_") || existing.startsWith("thunk_FUN_")) {
                    f.setName(newName, SourceType.USER_DEFINED);
                    println("Renamed: " + existing + " -> " + newName);
                } else {
                    println("Kept existing name: " + existing + " (wanted: " + newName + ")");
                }
            }
        } catch (Exception e) {
            println("Error renaming at " + addrStr + ": " + e.getMessage());
        }
    }
}
