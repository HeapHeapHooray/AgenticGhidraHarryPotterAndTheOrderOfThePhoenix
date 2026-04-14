// Iteration 6: Comprehensive annotations for frame timing and callback system
// Renames functions, variables, and adds detailed comments based on C++ implementation
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.symbol.*;

public class AnnotateIter6 extends GhidraScript {

    private FunctionManager fm;
    private SymbolTable symTable;
    private Listing listing;

    @Override
    public void run() throws Exception {
        fm = currentProgram.getFunctionManager();
        symTable = currentProgram.getSymbolTable();
        listing = currentProgram.getListing();

        println("=== Starting Iteration 6 Annotations ===\n");

        // Core timing system
        annotateTimingSystem();
        
        // Frame callback infrastructure
        annotateFrameCallbacks();
        
        // Timing callback functions
        annotateTimingCallbacks();
        
        // Game frame update
        annotateGameFrameUpdate();
        
        // Double-buffered timing data
        annotateTimingBuffers();
        
        // Additional globals discovered
        annotateGlobalData();

        println("\n=== Iteration 6 Annotations Complete ===");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Timing System Core
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateTimingSystem() {
        println("\n[Timing System] Annotating core timing infrastructure...");

        // GetGameTime function
        annotateFunc("00618010",
            "GetGameTime",
            "High-resolution game timer using Windows multimedia API\n" +
            "Returns: Milliseconds since first call (game startup)\n" +
            "Implementation:\n" +
            "  1. timeGetDevCaps() to get timer capabilities\n" +
            "  2. timeBeginPeriod(wPeriodMin) for high resolution\n" +
            "  3. timeGetTime() to get current time\n" +
            "  4. timeEndPeriod(wPeriodMin) to restore\n" +
            "  5. First call captures startup base time in g_dwStartupTime\n" +
            "  6. Returns (currentTime - startupTime)\n" +
            "Iteration 6: Confirmed 1ms resolution timer");

        // Callback interval global
        annotateData("00c83190",
            "g_ullCallbackInterval_Low",
            "Callback interval (16.16 fixed-point) - LOW 32 bits\n" +
            "Value: 16ms << 16 = 1,048,576 (0x100000)\n" +
            "Combined with +0x04 forms 64-bit interval\n" +
            "Set during InitFrameCallbackSystem()\n" +
            "Iteration 6: Confirmed 16ms for 60 FPS");

        annotateData("00c83194",
            "g_ullCallbackInterval_High",
            "Callback interval (16.16 fixed-point) - HIGH 32 bits\n" +
            "Forms 64-bit value with address-0x04\n" +
            "Total interval: 16ms in 16.16 fixed-point format");

        // Accumulated time
        annotateData("00c83198",
            "g_ullAccumTime_Low",
            "Accumulated game time (16.16 fixed-point) - LOW 32 bits\n" +
            "Incremented by delta each frame\n" +
            "Compared against g_ullNextCallback to trigger callbacks\n" +
            "Used to compute g_dwGameTicks = (accum * 3) / 0x10000");

        annotateData("00c8319c",
            "g_ullAccumTime_High",
            "Accumulated game time (16.16 fixed-point) - HIGH 32 bits\n" +
            "Forms 64-bit value with address-0x04");

        // Next callback time
        annotateData("00c831a8",
            "g_ullNextCallback_Low",
            "Next callback trigger time (16.16 fixed-point) - LOW 32 bits\n" +
            "When g_ullAccumTime >= this value, callbacks are triggered\n" +
            "Incremented by g_ullCallbackInterval each trigger\n" +
            "Iteration 6: Triggers every 16ms");

        annotateData("00c831ac",
            "g_ullNextCallback_High",
            "Next callback trigger time (16.16 fixed-point) - HIGH 32 bits\n" +
            "Forms 64-bit value with address-0x04");

        // Game ticks counter
        annotateData("00c83110",
            "g_dwGameTicks",
            "Game tick counter (scaled 3x)\n" +
            "Formula: (g_ullAccumTime * 3) / 0x10000\n" +
            "Game runs at 3x speed internally\n" +
            "Used for physics and game logic updates\n" +
            "Iteration 6: Updated by UpdateFrameTimingPrimary()");

        // Tick counter
        annotateData("00c83114",
            "g_dwTickCounter",
            "Frame tick accumulator\n" +
            "Incremented by (localTick * 3) in UpdateFrameTimingPrimary()\n" +
            "Only incremented when TimeManager not paused\n" +
            "Used for precise frame timing");

        // Frame flip index
        annotateData("00c83130",
            "g_nFrameFlip",
            "Double-buffer flip index (0 or 1)\n" +
            "Toggles each callback interval (XOR 1)\n" +
            "Indexes into g_dwTickBuffer[] and g_dwTimeBuffer[]\n" +
            "Enables smooth frame interpolation\n" +
            "Iteration 6: Toggled in GameFrameUpdate()");

        // Startup time
        annotateData("00e6e5e8",
            "g_dwStartupTime",
            "Game startup timestamp from timeGetTime()\n" +
            "Captured on first GetGameTime() call\n" +
            "Subtracted from current time to get elapsed time\n" +
            "Used as time base for entire game session");

        // Last frame time
        annotateData("00e6e5e0",
            "g_ullLastFrameTime_Low",
            "Last frame timestamp (16.16 fixed-point) - LOW 32 bits\n" +
            "Stored on first GameFrameUpdate() call\n" +
            "Used to compute frame delta time");

        annotateData("00e6e5e4",
            "g_ullLastFrameTime_High",
            "Last frame timestamp (16.16 fixed-point) - HIGH 32 bits\n" +
            "Forms 64-bit value with address-0x04");

        // Timebase init flag
        annotateData("00e6b388",
            "g_bTimebaseInit",
            "One-shot initialization flag for timing system\n" +
            "0 = not initialized, capture startup time on next call\n" +
            "1 = initialized, use captured startup time\n" +
            "Set by GetGameTime() on first call");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Frame Callback Infrastructure
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateFrameCallbacks() {
        println("\n[Frame Callbacks] Annotating callback system...");

        // InitFrameCallbackSystem
        annotateFunc("00eb8744",
            "InitFrameCallbackSystem",
            "Initializes the per-frame callback system\n" +
            "Operations:\n" +
            "  1. Clears all 8 callback slots at g_FrameCallbackSlots\n" +
            "  2. Initializes g_ullCallbackInterval to 16ms (60 FPS)\n" +
            "Callback slots store (function_ptr, context_ptr) pairs\n" +
            "One-shot guard via g_bFrameCallbackInit flag\n" +
            "Iteration 6: Now sets callback interval to 16 << 16");

        // Frame callback slots
        annotateData("00e6e880",
            "g_FrameCallbackSlots",
            "Frame callback function/context pairs (8 slots)\n" +
            "Structure: array of {void* func, void* context}\n" +
            "Total size: 0x1c bytes (28 bytes) = 7 pointers\n" +
            "Layout: func0, ctx0, func1, ctx1, func2, ctx2, func3, ctx3...\n" +
            "Cleared by InitFrameCallbackSystem()\n" +
            "Populated during InitGameSubsystems()\n" +
            "Invoked each frame via InvokeFrameCallbacks()\n" +
            "Iteration 6: Fully functional callback system");

        // Callback table pointer
        annotateData("008e1644",
            "g_pCallbackTable",
            "Pointer to frame callback function table\n" +
            "Set to &PTR_PTR_008d0f94 in FinalizeDeviceSetup()\n" +
            "Table layout:\n" +
            "  [0] = Primary callback (game logic update)\n" +
            "  [1] = Secondary callback (render/interpolation)\n" +
            "Primary called with &localTick parameter\n" +
            "Secondary called with no parameters\n" +
            "Iteration 6: Both callbacks now implemented");

        annotateData("008d0f94",
            "g_CallbackFunctionTable",
            "Frame callback function pointer array\n" +
            "Entry 0: Primary callback for game logic\n" +
            "Entry 1: Secondary callback for rendering\n" +
            "Referenced by g_pCallbackTable (DAT_008e1644)");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Timing Callback Functions (NEW in Iteration 6)
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateTimingCallbacks() {
        println("\n[Timing Callbacks] Annotating primary/secondary callbacks...");

        // UpdateFrameTimingPrimary
        annotateFunc("00617f50",
            "UpdateFrameTimingPrimary",
            "Primary timing callback - updates tick counter and double-buffers\n" +
            "Parameters:\n" +
            "  localTick: Pointer to current tick value\n" +
            "Operations:\n" +
            "  1. Check TimeManager pause state (g_pTimeManager+4)\n" +
            "  2. If not paused: increment g_dwTickCounter by (*localTick * 3)\n" +
            "  3. Store localTick in g_dwTickBuffer[g_nFrameFlip]\n" +
            "  4. Store current time in g_dwTimeBuffer[g_nFrameFlip]\n" +
            "Double-buffering enables smooth interpolation\n" +
            "Called when g_ullAccumTime >= g_ullNextCallback\n" +
            "Iteration 6: Fully implemented in C++ decompilation");

        // InterpolateFrameTime
        annotateFunc("00617ee0",
            "InterpolateFrameTime",
            "Secondary timing callback - smooth frame interpolation\n" +
            "Formula: result = prevTime + (currTime - prevTime) * t\n" +
            "Where: t = (currentTick - prevTick) / (currTick - prevTick)\n" +
            "Implementation:\n" +
            "  1. Get previous buffer index: prevFlip = g_nFrameFlip XOR 1\n" +
            "  2. Read prev/curr ticks from g_dwTickBuffer[]\n" +
            "  3. Read prev/curr times from g_dwTimeBuffer[]\n" +
            "  4. Calculate interpolation factor t\n" +
            "  5. Clamp t to [0.0, 1.0]\n" +
            "  6. Interpolate time value\n" +
            "Provides smooth rendering between physics updates\n" +
            "Iteration 6: Fully implemented in C++ decompilation");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Game Frame Update
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateGameFrameUpdate() {
        println("\n[Game Frame Update] Annotating main update loop...");

        annotateFunc("00618140",
            "GameFrameUpdate",
            "Main game frame update - timing and callback dispatch\n" +
            "Execution flow:\n" +
            "  1. ProcessDeferredCallbacks() - 2ms budget for render queue\n" +
            "  2. GetGameTime() - current time in milliseconds\n" +
            "  3. Convert to 16.16 fixed-point: tFixed = tMs << 16\n" +
            "  4. First frame: initialize accum and next callback times\n" +
            "  5. Compute delta = tFixed - g_ullAccumTime\n" +
            "  6. Cap delta at 100ms (spiral-of-death prevention)\n" +
            "  7. Accumulate: g_ullAccumTime += delta\n" +
            "  8. Update game ticks: g_dwGameTicks = (accum * 3) / 0x10000\n" +
            "  9. If accum >= next callback time:\n" +
            "     a. Advance next callback by interval (16ms)\n" +
            "     b. Toggle frame flip: g_nFrameFlip XOR= 1\n" +
            "     c. UpdateFrameTimingPrimary(&g_dwGameTicks)\n" +
            "     d. InvokeFrameCallbacks() - all registered callbacks\n" +
            "     e. InterpolateFrameTime() - smooth rendering\n" +
            "Iteration 6: Primary/secondary callbacks now dispatched!");

        // ProcessDeferredCallbacks
        annotateFunc("00636830",
            "ProcessDeferredCallbacks",
            "Processes deferred render batch queue with 2ms time budget\n" +
            "Queue structure:\n" +
            "  - Linked list at g_pDeferredRenderQueue (DAT_00bef7c0)\n" +
            "  - Each node: draw call descriptor with next at +0x7c\n" +
            "Processing:\n" +
            "  1. Start timer using timeGetTime()\n" +
            "  2. While queue not empty AND elapsed < 2ms:\n" +
            "     a. Call BuildRenderBatch(node)\n" +
            "     b. If returns 1 (complete): remove node\n" +
            "     c. Continue to next node\n" +
            "  3. If budget exhausted: continue next frame\n" +
            "Shader batching: BLOOM, GLASS, BACKDROP, etc.\n" +
            "Purpose: spread expensive batch building across frames");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Double-Buffered Timing Data
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateTimingBuffers() {
        println("\n[Timing Buffers] Annotating double-buffered arrays...");

        // Tick buffer
        annotateData("00c83128",
            "g_dwTickBuffer",
            "Double-buffered tick values [2]\n" +
            "Indexed by g_nFrameFlip (0 or 1)\n" +
            "Updated by UpdateFrameTimingPrimary()\n" +
            "Current: g_dwTickBuffer[g_nFrameFlip]\n" +
            "Previous: g_dwTickBuffer[g_nFrameFlip XOR 1]\n" +
            "Used for frame interpolation in InterpolateFrameTime()\n" +
            "Iteration 6: Confirmed 2 DWORD array");

        // Time buffer
        annotateData("00c83120",
            "g_dwTimeBuffer",
            "Double-buffered time values [2]\n" +
            "Indexed by g_nFrameFlip (0 or 1)\n" +
            "Updated by UpdateFrameTimingPrimary()\n" +
            "Stores GetGameTime() results for interpolation\n" +
            "Used in InterpolateFrameTime() to compute smooth t\n" +
            "Iteration 6: Confirmed 2 DWORD array");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Additional Global Data
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateGlobalData() {
        println("\n[Global Data] Annotating timing-related globals...");

        // Timing constants
        annotateData("008475d8",
            "TIMING_CONST_1_DIV_65536",
            "Floating-point constant: 1.0 / 65536.0\n" +
            "Used for 16.16 fixed-point to float conversion\n" +
            "Value: 0.0000152587890625\n" +
            "Usage: multiply fixed-point value by this to get float");

        annotateData("00845594",
            "TIMING_CONST_0_001",
            "Floating-point constant: 0.001\n" +
            "Milliseconds to seconds conversion factor\n" +
            "Used in tick calculations: ms * 0.001 = seconds");

        annotateData("00845320",
            "TIMING_CONST_1000_0",
            "Floating-point constant: 1000.0\n" +
            "Seconds to milliseconds conversion factor\n" +
            "Used in timing calculations: seconds * 1000.0 = ms");

        // TimeManager
        annotateData("00bef768",
            "g_pTimeManager",
            "Pointer to TimeManager singleton instance (8 bytes)\n" +
            "Structure layout:\n" +
            "  +0x00: vtable pointer\n" +
            "  +0x04: isPaused flag (0=running, non-zero=paused)\n" +
            "Created in FUN_0060b740 via thunk_FUN_00eb797e()\n" +
            "Checked by UpdateFrameTimingPrimary() before tick updates\n" +
            "Iteration 6: Pause checking implemented");

        // Engine root object
        annotateData("00bef6d0",
            "g_pEngineRootObject",
            "Main engine coordinator/factory object (2904 bytes = 0xb58)\n" +
            "Created via callback manager secondary factory:\n" +
            "  (*g_pCallbackManager_Secondary[0])(0xb58, magic)\n" +
            "Magic number: {0x88332000000001, 0} (16 bytes)\n" +
            "Coordinates all game subsystems and manages lifecycle\n" +
            "Destroyed via custom destructor at callback_mgr+0xc\n" +
            "Iteration 6: Factory creation pending for Iteration 7");

        // Deferred render queue
        annotateData("00bef7c0",
            "g_pDeferredRenderQueue",
            "Head pointer to deferred render batch linked list\n" +
            "Each node: draw call descriptor\n" +
            "Next pointer at node+0x7c\n" +
            "Processed by ProcessDeferredCallbacks() with 2ms budget\n" +
            "Shader types: BLOOM, GLASS, BACKDROP, OPAQUE, etc.\n" +
            "Batch building optimizes state changes");

        // Memory low-water mark
        annotateData("008afb08",
            "g_nMinFreeMemory",
            "Memory allocator low-water mark\n" +
            "Tracks minimum available memory (SIZE_T)\n" +
            "Updated when g_bGameUpdateEnabled (memorylwm flag)\n" +
            "Queried via QueryMemoryAllocatorMax()\n" +
            "Used for memory pressure detection");

        // Game update enabled flag
        annotateData("00bef6d7",
            "g_bGameUpdateEnabled",
            "Game update subsystem enabled flag\n" +
            "Set by 'memorylwm' command-line flag\n" +
            "When true: tracks memory allocator statistics\n" +
            "Controls low-water mark monitoring in MainLoop()");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Helper Functions
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateFunc(String addrStr, String name, String comment) {
        try {
            Address[] addrs = currentProgram.parseAddress(addrStr);
            if (addrs == null || addrs.length == 0) {
                println(String.format("  ✗ Invalid address: %s", addrStr));
                return;
            }
            Address addr = addrs[0];
            Function func = fm.getFunctionAt(addr);
            
            if (func != null) {
                // Rename function
                func.setName(name, SourceType.USER_DEFINED);
                
                // Set comment
                func.setComment(comment);
                
                println(String.format("  ✓ %s @ %s", name, addrStr));
            } else {
                println(String.format("  ✗ No function at %s", addrStr));
            }
        } catch (Exception e) {
            println(String.format("  ✗ Error at %s: %s", addrStr, e.getMessage()));
        }
    }

    private void annotateData(String addrStr, String name, String comment) {
        try {
            Address[] addrs = currentProgram.parseAddress(addrStr);
            if (addrs == null || addrs.length == 0) {
                println(String.format("  ✗ Invalid address: %s", addrStr));
                return;
            }
            Address addr = addrs[0];
            
            // Create or get symbol
            Symbol[] symbols = symTable.getSymbols(addr);
            Symbol symbol = null;
            
            if (symbols.length > 0) {
                symbol = symbols[0];
                symbol.setName(name, SourceType.USER_DEFINED);
            } else {
                symbol = symTable.createLabel(addr, name, SourceType.USER_DEFINED);
            }
            
            // Set comment at address using pre-comment instead of deprecated plate comment
            listing.setComment(addr, CodeUnit.PRE_COMMENT, comment);
            
            println(String.format("  ✓ %s @ %s", name, addrStr));
        } catch (Exception e) {
            println(String.format("  ✗ Error at %s: %s", addrStr, e.getMessage()));
        }
    }
}
