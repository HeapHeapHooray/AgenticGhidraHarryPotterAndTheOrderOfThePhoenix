// Iteration 5: Add comprehensive annotations based on architectural understanding
// Updates function and data comments with detailed structural information
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.symbol.*;

public class AnnotateIter5 extends GhidraScript {

    private FunctionManager fm;
    private SymbolTable symTable;
    private Listing listing;

    @Override
    public void run() throws Exception {
        fm = currentProgram.getFunctionManager();
        symTable = currentProgram.getSymbolTable();
        listing = currentProgram.getListing();

        println("Starting Iteration 5 annotations...");

        // Annotate based on Iteration 4 findings and Iteration 5 questions
        annotateEngineObjectFactory();
        annotateCallbackManager();
        annotateDirectXSystem();
        annotateAudioSystem();
        annotateMessageDispatch();
        annotateSceneManagement();
        annotateRenderQueue();
        annotateSubsystems();
        annotateFrameCallbacks();
        annotateMemorySystem();
        annotateTimingSystem();
        annotateInputSystem();
        annotateGlobalData();

        println("Iteration 5 annotations complete!");
    }

    // ── Engine Object Factory ────────────────────────────────────────────────────

    private void annotateEngineObjectFactory() {
        println("\nAnnotating Engine Object Factory...");

        // Main engine object
        annotateData("00bef6d0",
            "g_pEngineRootObject",
            "Main engine coordinator/factory object (2904 bytes = 0xb58)\n" +
            "Created via callback_mgr secondary factory with magic {0x88332000000001, 0}\n" +
            "Coordinates all game subsystems and manages lifecycle\n" +
            "Destroyed via custom destructor at callback_mgr+0xc");

        // Factory creation function
        annotateFunc("00eb5c3e",
            "GetOrInitCallbackManager",
            "Returns or initializes the singleton callback manager\n" +
            "Provides dual-entry vtable access:\n" +
            "  - Primary entry (DAT_00e6e870): general callbacks\n" +
            "  - Secondary entry (DAT_00e6e874): factory creation\n" +
            "Used to create engine root object with type magic number");

        // Callback manager entries
        annotateData("00e6e870",
            "g_pCallbackManager_Primary",
            "Callback manager primary entry point\n" +
            "Vtable: PTR_FUN_00883f3c\n" +
            "Handles general event dispatch and callback registration");

        annotateData("00e6e874",
            "g_pCallbackManager_Secondary",
            "Callback manager secondary entry point (factory)\n" +
            "Vtable: PTR_FUN_00883f4c\n" +
            "Used for object creation: (*vtable[0])(size, magic)\n" +
            "Creates engine objects with type identification");
    }

    // ── Callback Manager ──────────────────────────────────────────────────────────

    private void annotateCallbackManager() {
        println("\nAnnotating Callback Manager...");

        annotateFunc("00eb8744",
            "InitFrameCallbackSystem",
            "Initializes the per-frame callback system\n" +
            "Clears 8 callback slots at DAT_00e6e880..e6e89c\n" +
            "Callback slots store (function_ptr, context_ptr) pairs\n" +
            "Called during subsystem initialization");

        annotateData("00e6e880",
            "g_FrameCallbackSlots",
            "Frame callback function/context pairs (8 slots, 0x1c bytes)\n" +
            "Each slot: (void* func, void* context)\n" +
            "Populated during InitGameSubsystems\n" +
            "Invoked each frame for game logic updates");
    }

    // ── DirectX System ────────────────────────────────────────────────────────────

    private void annotateDirectXSystem() {
        println("\nAnnotating DirectX System...");

        annotateFunc("0067c290",
            "CreateD3DDevice",
            "Creates IDirect3DDevice9 with 9 parameters:\n" +
            "  1. height: client window height\n" +
            "  2. unknown (likely adapter or behavior flags)\n" +
            "  3-4. flags (both 0)\n" +
            "  5. quality/feature level (6 = default)\n" +
            "  6-9. boolean flags (all 1)\n" +
            "Returns D3DERR codes (checks for D3DERR_DEVICELOST = -0x7fffbffb)\n" +
            "Stores device in DAT_00bf1920, interface/chain in DAT_00bf1924");

        annotateData("00bf1994",
            "g_nShaderCapabilityLevel",
            "Shader model capability level\n" +
            "Determined from D3D device caps query\n" +
            ">2 = extended shader path in RestoreDirectXResources\n" +
            "Likely maps to: 1.x, 2.0, 3.0 shader models");

        annotateFunc("00ec04dc",
            "NotifyPreReleaseResources",
            "Pre-release resource notification handler\n" +
            "Called BEFORE D3D surface releases in ReleaseDirectXResources\n" +
            "Notifies render pipeline of impending resource loss\n" +
            "Flushes pending draw calls, clears command buffers");

        annotateFunc("00ec19b5",
            "NotifyPostReleaseResources",
            "Post-release resource cleanup handler\n" +
            "Called AFTER D3D surface releases\n" +
            "Cleans up dependent resources (texture caches, shader states)\n" +
            "Resets internal tracking structures");
    }

    // ── Audio System ──────────────────────────────────────────────────────────────

    private void annotateAudioSystem() {
        println("\nAnnotating Audio System...");

        annotateData("00be82ac",
            "g_pAudioCommandQueue",
            "Async audio command queue structure\n" +
            "Used for non-blocking audio initialization and operations\n" +
            "Commands queued and processed by audio thread\n" +
            "Polled via AudioPollGate for status");

        annotateFunc("006109d0",
            "AudioPollGate",
            "Audio async operation status gate\n" +
            "Returns:\n" +
            "  -2 = error\n" +
            "   0 = pending (operation in progress)\n" +
            "   1 = complete/success\n" +
            "Polled in SleepEx(0,1) loops during audio init");

        annotateData("00bf1b30",
            "g_hAudioThread",
            "Audio worker thread handle\n" +
            "Created in PreDirectXInit via FUN_00611940\n" +
            "Processes audio command queue asynchronously\n" +
            "Enables non-blocking audio initialization");

        annotateFunc("00611940",
            "CreateAudioThread",
            "Creates audio worker thread\n" +
            "Thread processes command queue at DAT_00be82ac\n" +
            "Stores handle in DAT_00bf1b30");

        annotateFunc("006a91a0",
            "QueryAudioFormatSupport",
            "Queries audio hardware format/capability support\n" +
            "Expected return: 0x80 (format support flag or caps bitmask)\n" +
            "Called during audio device initialization");

        annotateFunc("00ec693d",
            "SetAudioDecoderState",
            "Controls audio decoder/playback state\n" +
            "  0 = stop playback\n" +
            "  0x1000 = normal playback rate (100%, no pitch shift)\n" +
            "Used in AudioStream_Pause and AudioStream_Resume");

        annotateFunc("00ec66f1",
            "CommitAudioStateChange",
            "Commits audio state change to hardware\n" +
            "Called after SetAudioDecoderState\n" +
            "Flushes buffers and updates DirectSound state");

        annotateFunc("006ace30",
            "StopAllAudioStreams",
            "Stops all active audio streams/tracks\n" +
            "First step in audio teardown sequence");

        annotateFunc("006108c0",
            "ReleaseAudioBuffers",
            "Releases DirectSound buffers and secondary resources\n" +
            "Second step in audio teardown");

        annotateFunc("006ac930",
            "CloseAudioDevice",
            "Closes audio device handle and DirectSound interface\n" +
            "Final step in audio teardown sequence");

        annotateData("00bf1b10",
            "g_bAudioHardwarePresent",
            "Audio hardware detection flag\n" +
            "0 = no audio device (headless/server mode)\n" +
            "Non-zero = audio available, proceed with init\n" +
            "Guards all audio init in InitAudioSubsystem");
    }

    // ── Message Dispatch ──────────────────────────────────────────────────────────

    private void annotateMessageDispatch() {
        println("\nAnnotating Message Dispatch...");

        annotateFunc("00eb59ce",
            "RegisterMessageHandler",
            "Registers string-based message handler\n" +
            "Parameters: (dest_object, msgName, paramType)\n" +
            "Implementation:\n" +
            "  1. Hash message name to ID\n" +
            "  2. Store (msgID, handler_func, dest) in dispatch table\n" +
            "  3. Runtime: lookup by msgID, call handler\n" +
            "Message names use 'iMsg' prefix (interface messages)");
    }

    // ── Scene Management ──────────────────────────────────────────────────────────

    private void annotateSceneManagement() {
        println("\nAnnotating Scene Management...");

        annotateData("00c82b00",
            "g_SceneID_FocusLost",
            "Scene ID for focus-lost state (menu/pause screen)\n" +
            "Part of three-ID scene management system\n" +
            "Initialized to 0, populated during scene loading\n" +
            "Compared in UpdateCursorVisibilityAndScene");

        annotateData("00c82b08",
            "g_SceneID_FocusGain",
            "Scene ID for focus-gained state (active gameplay)\n" +
            "Restored when application regains focus\n" +
            "Part of three-ID scene management system");

        annotateData("00c82ac8",
            "g_SceneID_Current",
            "Current/target scene ID\n" +
            "Updated during scene transitions\n" +
            "Drives render mode switching");

        annotateFunc("00612530",
            "SwitchRenderOutputMode",
            "Switches render output mode based on scene\n" +
            "Notifies scene listener list\n" +
            "Checks pending-change flag at list.head+0x12\n" +
            "Supports async scene loading coordination");

        annotateFunc("006125a0",
            "FlushDeferredSceneListeners",
            "Flushes deferred scene transition listeners\n" +
            "Called when pending-change flag is set\n" +
            "Processes queued scene transition callbacks");
    }

    // ── Render Queue ──────────────────────────────────────────────────────────────

    private void annotateRenderQueue() {
        println("\nAnnotating Render Queue...");

        annotateData("00bef7c0",
            "g_pDeferredRenderQueue",
            "Head of deferred render batch linked list\n" +
            "Nodes have next pointer at +0x7c\n" +
            "Batched by shader type: BLOOM, GLASS, BACKDROP, etc.\n" +
            "Processed within 2ms budget per frame");

        annotateFunc("0063d600",
            "BuildRenderBatch",
            "Builds and sorts render batch by shader type\n" +
            "Recognizes shader types via string/enum/hash\n" +
            "Batches draw calls to minimize state changes\n" +
            "Deferred rendering optimization");

        annotateData("00c8c580",
            "g_pRenderDispatchTable",
            "Render subsystem vtable/dispatch table\n" +
            "  [0] = Initialize(int, int)\n" +
            "  [8] = Finalize()\n" +
            "  [16+] = BeginFrame, EndFrame, Present, SetCamera, etc.\n" +
            "Interface to entire render subsystem");
    }

    // ── Subsystems ────────────────────────────────────────────────────────────────

    private void annotateSubsystems() {
        println("\nAnnotating Subsystems...");

        annotateData("00e6b378",
            "g_pGlobalTempBuffer",
            "Global temporary buffer (0x3c = 60 bytes)\n" +
            "Field +0xd: callback count (max 5)\n" +
            "Fields +3, +4: callback pairs (func_ptr, context_ptr)\n" +
            "Used for per-frame callback registration\n" +
            "Scratch buffer for deferred render commands");

        annotateData("00e6b390",
            "g_pRealGraphSystem",
            "Render graph/scene manager (8 bytes)\n" +
            "Vtable: PTR_FUN_00885010\n" +
            "Field +1: pointer to DAT_00e6e874 (secondary callback)\n" +
            "Coordinates scene rendering via callback system\n" +
            "Small size indicates mostly vtable + single pointer");

        annotateData("00e6b304",
            "g_pLocale",
            "Localization/language system (0x8c = 140 bytes)\n" +
            "Handles multiple languages (EN, FR, DE, ES, IT)\n" +
            "String table lookup and runtime language switching\n" +
            "Initialized during subsystem setup");

        annotateFunc("00eb87ba",
            "InitLanguageResources",
            "Initializes language resource loading\n" +
            "Called after Locale setup\n" +
            "Likely loads string tables or font system");

        annotateFunc("00eb88b2",
            "InitVideoCodec",
            "Initializes video codec (Bink/Smacker)\n" +
            "Called after FMV setup\n" +
            "Prepares video decoder for cutscenes");

        annotateFunc("006677c0",
            "FinalizeRenderInit",
            "Finalizes render initialization\n" +
            "Called at end of InitEngineObjects\n" +
            "Likely shader compiler init or texture manager setup\n" +
            "Position suggests finalizing dependencies");

        annotateData("00e6b2dc",
            "g_pFMV",
            "FMV (Full Motion Video) subsystem (0x18 = 24 bytes)\n" +
            "Vtable + few fields\n" +
            "Codec: likely Bink or Smacker\n" +
            "Disabled by 'nofmv' command-line flag\n" +
            "Integration with audio system for video playback");

        annotateData("00e6b2c8",
            "g_pGameServices",
            "GameServices::Open singleton (1 byte - just vtable ptr)\n" +
            "Service locator pattern\n" +
            "Provides: GetAudioManager, GetRenderer, GetPhysics, etc.\n" +
            "Created in FUN_0060b740 during FinalizeDeviceSetup");

        annotateFunc("0060b740",
            "InitGameServicesAndTimeManager",
            "Creates GameServices::Open and TimeManager::Instance\n" +
            "Both singleton-style factory calls\n" +
            "Called after message handler registration\n" +
            "Final dependencies for game initialization");
    }

    // ── Frame Callbacks ───────────────────────────────────────────────────────────

    private void annotateFrameCallbacks() {
        println("\nAnnotating Frame Callbacks...");

        annotateData("008e1644",
            "g_FrameCallbackTable",
            "Frame callback function table\n" +
            "  [0] = primary callback (called with &localTick)\n" +
            "  [1] = secondary callback (called with no args)\n" +
            "Invoked each frame for timing and logic updates");

        annotateFunc("0060c130",
            "GameLogicUpdate",
            "Main game logic update entry point\n" +
            "Registered as frame callback in InitGameSubsystems\n" +
            "Stored in DAT_00e69ca0\n" +
            "Processes: game state, entity updates, AI ticks");
    }

    // ── Memory System ─────────────────────────────────────────────────────────────

    private void annotateMemorySystem() {
        println("\nAnnotating Memory System...");

        annotateFunc("00614210",
            "AllocEngineObject",
            "Debug-aware heap allocator\n" +
            "Parameters: (size, tagName)\n" +
            "Features:\n" +
            "  - Custom heap with tracking/profiling\n" +
            "  - Tag string for leak detection\n" +
            "  - Memory usage stats per tag\n" +
            "  - Integrates with QueryMemoryAllocatorMax\n" +
            "May automatically set up vtables via type registry");
    }

    // ── Timing System ─────────────────────────────────────────────────────────────

    private void annotateTimingSystem() {
        println("\nAnnotating Timing System...");

        annotateData("00c83190",
            "g_ullCallbackInterval_lo",
            "Callback interval low 32 bits (16.16 fixed-point)\n" +
            "Likely 16ms (60 FPS) = 0x100000 or 33ms (30 FPS) = 0x210000\n" +
            "Initialized in InitFrameCallbackSystem or first GameFrameUpdate\n" +
            "Used for frame pacing throughout game");

        annotateData("00c83194",
            "g_ullCallbackInterval_hi",
            "Callback interval high 32 bits\n" +
            "Part of 64-bit interval value");

        annotateData("00bef768",
            "g_pTimeManager",
            "TimeManager::Instance singleton (8 bytes)\n" +
            "  +0: vtable pointer\n" +
            "  +4: isPaused flag (0=running, non-zero=paused)\n" +
            "When paused: time advances but ticks don't accumulate\n" +
            "Created in FUN_0060b740");
    }

    // ── Input System ──────────────────────────────────────────────────────────────

    private void annotateInputSystem() {
        println("\nAnnotating Input System...");

        annotateData("00e6b384",
            "g_pRealInputSystem",
            "RealInputSystem object (0x34 = 52 bytes)\n" +
            "Field +0xc: pointer to device manager sub-object\n" +
            "Queues input events for processing\n" +
            "Manages DirectInput device enumeration and acquisition");
    }

    // ── Global Data ───────────────────────────────────────────────────────────────

    private void annotateGlobalData() {
        println("\nAnnotating Global Data...");

        annotateData("00c82b88",
            "g_szCommandLine1",
            "First command line copy (0x1ff bytes)\n" +
            "Saved copy of lpCmdLine from WinMain\n" +
            "Used for parsing command-line arguments");

        annotateData("00c82d88",
            "g_szCommandLine2",
            "Second command line copy (0x1ff bytes)\n" +
            "Null-terminated at +0x1ff (DAT_00c82d87)\n" +
            "Backup copy for CLI parsing");

        annotateData("00e6b328",
            "g_pCLICommandParser",
            "CLI::CommandParser object (0xc bytes base + arrays)\n" +
            "  +0x400: positional arguments\n" +
            "  +0x480: name pointers (up to 0x20 entries)\n" +
            "  +0x500: value pointers\n" +
            "Allocated with tag 'CLI::CommandParser'\n" +
            "Parses -name=value tokens");

        annotateFunc("00eb787a",
            "CLI_CommandParser_ParseArgs",
            "Parses command-line arguments into CLI::CommandParser\n" +
            "Processes -name=value tokens from DAT_00c82b88\n" +
            "Stores parsed args at object+0x400/0x480/0x500\n" +
            "Called before single-instance check in WinMain");

        annotateData("008afc44",
            "g_SavedMouseSpeed",
            "Saved mouse speed parameters (2 DWORDs)\n" +
            "Retrieved via SystemParametersInfoA(SPI_GET_MOUSE_SPEED)\n" +
            "Restored on exit via UpdateSystemParameters(1)");

        annotateData("008afc4c",
            "g_SavedMouseAccel",
            "Saved mouse acceleration parameters (2 DWORDs)\n" +
            "Retrieved via SystemParametersInfoA(SPI_GET_MOUSE_ACCEL)\n" +
            "Acceleration disabled during gameplay");

        annotateData("008afc54",
            "g_SavedScreenReader",
            "Saved screen reader parameters (6 DWORDs)\n" +
            "Retrieved via SystemParametersInfoA(SPI_GET_SCREEN_READER)\n" +
            "Restored on exit");

        annotateData("008ae1dc",
            "g_AspectRatio",
            "Aspect ratio constant\n" +
            "Set by 'widescreen' command-line flag\n" +
            "Used to calculate window height from width\n" +
            "Likely 4:3 vs 16:9 values");

        annotateData("008afbd9",
            "g_bFullscreenMode",
            "Fullscreen mode flag\n" +
            "Set by 'fullscreen' command-line argument\n" +
            "Drives window creation mode");

        annotateData("008ae1ff",
            "g_bOldGenRenderer",
            "Old generation renderer flag\n" +
            "Set by 'oldgen' command-line argument\n" +
            "Enables legacy renderer path in InitCLIAndTimingAndDevice");

        annotateData("00bef754",
            "g_bShowFPS",
            "Show FPS overlay flag\n" +
            "Set by 'showfps' command-line argument\n" +
            "Enables frame rate display in FinalizeDeviceSetup");

        annotateData("00bef6d7",
            "g_bMemoryLowWaterMark",
            "Memory low-water-mark tracking flag\n" +
            "Set by 'memorylwm' command-line argument\n" +
            "Enables memory usage tracking in FinalizeDeviceSetup\n" +
            "Used for quality scaling or warning dialogs");

        annotateData("008afb08",
            "g_LowWaterMarkValue",
            "Memory low-water-mark value storage\n" +
            "Tracks minimum available memory\n" +
            "Used with DAT_00bef6d7 flag");

        annotateFunc("00ebe85b",
            "HandleLowTextureMemory",
            "Low texture memory handler\n" +
            "Called when 0 < available MB < 33\n" +
            "Actions: reduce texture quality, flush caches, warn user");

        annotateData("008d3878",
            "g_DebugSentinel",
            "Debug sentinel pattern (0x4c bytes filled with 0xcd)\n" +
            "0xcd = MSVC debug heap uninitialized pattern\n" +
            "Used to detect uninitialized state access");

        annotateData("00c83110",
            "g_dwGameTicks",
            "Game tick counter\n" +
            "Incremented by localTick * 3 in UpdateFrameTimingPrimary\n" +
            "Why *3? Game may run at 3x real-time internally");
    }

    // ── Helper Methods ───────────────────────────────────────────────────────────

    private void annotateFunc(String addrStr, String newName, String comment) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            Function func = fm.getFunctionAt(addr);

            if (func == null) {
                println("  SKIP: No function at " + addrStr);
                return;
            }

            // Rename if not already named (and not default FUN_* name)
            String currentName = func.getName();
            if (currentName.startsWith("FUN_") || currentName.startsWith("thunk_FUN_")) {
                func.setName(newName, SourceType.USER_DEFINED);
                println("  Renamed: " + addrStr + " -> " + newName);
            } else if (!currentName.equals(newName)) {
                println("  Keep existing name: " + currentName + " @ " + addrStr);
            }

            // Set comment
            func.setComment(comment);
            println("  Commented: " + newName + " @ " + addrStr);

        } catch (Exception e) {
            println("  ERROR: " + addrStr + " - " + e.getMessage());
        }
    }

    private void annotateData(String addrStr, String newLabel, String comment) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);

            // Try to set symbol name
            Symbol existingSym = symTable.getPrimarySymbol(addr);
            if (existingSym == null || existingSym.getName().startsWith("DAT_")) {
                Symbol newSym = symTable.createLabel(addr, newLabel, SourceType.USER_DEFINED);
                println("  Labeled: " + addrStr + " -> " + newLabel);
            } else {
                println("  Keep existing label: " + existingSym.getName() + " @ " + addrStr);
            }

            // Set comment on the data
            listing.setComment(addr, CodeUnit.PLATE_COMMENT, comment);
            println("  Commented data: " + newLabel + " @ " + addrStr);

        } catch (Exception e) {
            println("  ERROR: " + addrStr + " - " + e.getMessage());
        }
    }
}
