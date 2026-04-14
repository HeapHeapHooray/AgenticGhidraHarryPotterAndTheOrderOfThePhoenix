// Iteration 4 annotations: adds findings from ANSWERS-ITER4.md analysis
// Extends AnnotateAllKnowns.java with deeper understanding of unknown functions
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.symbol.*;

public class AnnotateIter4 extends GhidraScript {

    private FunctionManager fm;
    private SymbolTable st;

    @Override
    public void run() throws Exception {
        fm = currentProgram.getFunctionManager();
        st = currentProgram.getSymbolTable();

        println("=".repeat(60));
        println("Iteration 4 Annotations - Enhanced Understanding");
        println("=".repeat(60));

        annotateNewFindings();
        refineExistingAnnotations();

        println("\nAnnotateIter4: complete.");
    }

    // ── New Findings from Analysis ────────────────────────────────────────────

    private void annotateNewFindings() {
        println("\n[NEW FINDINGS]");

        // Engine object factory details
        fc("00eb5c3e", null,
            "GetOrInitCallbackManager: Returns &DAT_00e6e870 (callback manager singleton).\n" +
            "One-shot init via guard at DAT_00e74c20 bit 0.\n" +
            "NOT a COM object creator - returns callback table base address.\n" +
            "Used in WinMain to create DAT_00bef6d0 via secondary factory entry:\n" +
            "  (*DAT_00e6e874[0])(0xb58, {0x88332000000001, 0})\n" +
            "  Magic {0x88332000000001, 0} is likely a type identifier for the factory.\n" +
            "The 2904-byte (0xb58) engine object coordinates all game subsystems.");

        // D3D device creation
        fc("0067c290", null,
            "CreateD3DDevice: Primary D3D9 device creation wrapper.\n" +
            "Parameters: (clientHeight, ?, flags?, quality_params...)\n" +
            "  height from GetClientRect, param 6 likely default quality/feature level\n" +
            "Creates IDirect3DDevice9* → DAT_00bf1920\n" +
            "Creates IDirect3D9*/swap chain → DAT_00bf1924\n" +
            "Returns D3DERR-like codes; handles D3DERR_DEVICELOST (-0x7fffbffb) specially.\n" +
            "Encapsulates D3D initialization: adapter selection, present params, create calls.");

        // DX resource cleanup callbacks
        fc("00ec04dc", null,
            "Pre-release cleanup callback in ReleaseDirectXResources.\n" +
            "Called BEFORE surface releases.\n" +
            "Likely: notifies render pipeline of impending resource loss,\n" +
            "  flushes pending draw calls, clears command buffers.\n" +
            "Part of pre/post cleanup pattern around D3D device Reset.");

        fc("00ec19b5", null,
            "Post-release cleanup callback in ReleaseDirectXResources.\n" +
            "Called AFTER all surface releases.\n" +
            "Likely: cleanup of dependent resources (texture caches, shader states),\n" +
            "  resets internal tracking structures.\n" +
            "Complements thunk_FUN_00ec04dc as part of resource loss protocol.");

        // Audio polling gate
        fc("006109d0", null,
            "AudioPollGate: Status check for async audio operations.\n" +
            "Polled in SleepEx(0,1) loops during audio init.\n" +
            "Returns: -2 (error), 0 (pending), 1 (complete)\n" +
            "Checks command queue status at DAT_00be82ac.\n" +
            "Used by all async audio operations in InitAudioSubsystem:\n" +
            "  FUN_006a9080 (open device), FUN_006a91a0 (query caps),\n" +
            "  FUN_006a9140 (configure), FUN_006a90e0 (start stream).");

        // Audio decoder control
        fc("00ec693d", null,
            "Audio decoder state control (likely DirectSound playback rate).\n" +
            "Called with 0 on pause, 0x1000 on resume.\n" +
            "0x1000 = normal pitch/playback rate (100%).\n" +
            "0 = stop playback.\n" +
            "Possible: IDirectSoundBuffer::SetFrequency or codec decoder state.\n" +
            "Used in AudioStream_Pause and AudioStream_Resume.");

        fc("00ec66f1", null,
            "Audio state change finalize.\n" +
            "Called after thunk_FUN_00ec693d in both pause and resume paths.\n" +
            "Likely: commits state change (flushes buffers, updates hardware).\n" +
            "Possible: DirectSound Update() or similar commit operation.");

        // Audio teardown sequence
        fc("006ace30", null,
            "Audio teardown step 1 in RenderAndAudioTeardown.\n" +
            "Likely: stops all active audio streams/tracks.");

        fc("006108c0", null,
            "Audio teardown step 2 in RenderAndAudioTeardown.\n" +
            "Likely: releases DirectSound buffers and secondary resources.");

        fc("006ac930", null,
            "Audio teardown step 3 in RenderAndAudioTeardown.\n" +
            "Likely: closes audio device handle and releases DirectSound interface.\n" +
            "Three-step pattern: Stop → Release → Close.");

        // Engine subsystem inits
        fc("00eb87ba", null,
            "Unknown subsystem init called after Locale setup in InitEngineObjects.\n" +
            "Likely: language resource loading or string table initialization.\n" +
            "Possible: font system initialization for localized text.");

        fc("00eb88b2", null,
            "Unknown subsystem init called after FMV setup in InitEngineObjects.\n" +
            "Likely: video codec initialization (Bink/Smacker decoder setup).\n" +
            "Called after 'nofmv' flag check, so only runs if video is enabled.");

        fc("006677c0", null,
            "Final subsystem init in InitEngineObjects.\n" +
            "Likely: shader compiler init or render state setup.\n" +
            "Possible: texture manager initialization.\n" +
            "Position at end suggests finalizing dependencies after all subsystems allocated.");

        // Message dispatch
        fc("00eb59ce", null,
            "RegisterMessageHandler: String-based message dispatch registration.\n" +
            "Params: (dest_object, msgName_string, paramType)\n" +
            "Message names: 'iMsgDeleteEventHandler', 'iMsgDoRender', etc.\n" +
            "Architecture: String → hash → msgID → dispatch table (msgID, handler, dest).\n" +
            "Runtime: lookup by msgID, call handler with dest as 'this' pointer.\n" +
            "'iMsg' prefix suggests interface messages (polymorphic dispatch).");

        // Frame callback registration
        fc("0060c130", null,
            "Frame callback function registered in InitGameSubsystems.\n" +
            "Stored at DAT_00e69ca0, called as part of per-frame callback chain.\n" +
            "Likely: main game logic update entry point.\n" +
            "Processes game state updates, entity updates, AI ticks.\n" +
            "This is the 'game code' hook called every frame by the engine.");

        // Scene mode switching
        fc("006125a0", null,
            "Deferred render mode listener flush, called from SwitchRenderOutputMode.\n" +
            "Triggered when list.head+0x12 pending-change flag is set.\n" +
            "Likely: processes queued scene transition callbacks.\n" +
            "Async scene loading coordination.");

        // GameServices and TimeManager factory
        fc("0060b740", null,
            "GameServicesAndTimeManager_Init: Creates two singleton objects.\n" +
            "Called in FinalizeDeviceSetup after message handler registration.\n" +
            "  DAT_00bef6c8 = GameServices::Open (1 byte) — service locator/DI container\n" +
            "  DAT_00bef768 = TimeManager::Instance (8 bytes) — game time management\n" +
            "    TimeManager+0: vtable pointer\n" +
            "    TimeManager+4: isPaused flag (0=running, non-zero=paused)\n" +
            "These are final dependencies created after core engine init.");
    }

    // ── Refined Existing Annotations ──────────────────────────────────────────

    private void refineExistingAnnotations() {
        println("\n[REFINED ANNOTATIONS]");

        // Enhance engine object global comment
        gn("00bef6d0", null,
            "g_pEngineObject: 2904-byte (0xb58) main engine factory/coordinator.\n" +
            "Created via (*DAT_00e6e874[0])(0xb58, {0x88332000000001, 0}).\n" +
            "Magic 0x88332000000001 is type identifier for callback factory.\n" +
            "Released via (*(callback_mgr+0xc))(g_pEngineObject, 0) — custom destructor at offset +0xc.\n" +
            "Passed to audio subsystem in PreDirectXInit (copied to DAT_00bf1b18).\n" +
            "Central coordinator for all game subsystems.");

        // Enhance audio present flag
        gn("00bf1b10", null,
            "g_bAudioPresent: Audio hardware detection flag.\n" +
            "Set to 0 in PreDirectXInit, then set non-zero by FUN_006ac0b0/thunk_FUN_00ec6e91.\n" +
            "Guards all audio init in InitAudioSubsystem.\n" +
            "0 = no audio device (headless/server mode?), non-zero = audio available.");

        // Enhance TimeManager
        gn("00bef768", null,
            "g_pTimeManager: TimeManager::Instance singleton (8 bytes).\n" +
            "Created by FUN_0060b740 via thunk_FUN_00eb797e in FinalizeDeviceSetup.\n" +
            "  +0: vtable pointer\n" +
            "  +4: isPaused flag (checked in UpdateFrameTimingPrimary)\n" +
            "       0 = running (updates DAT_00c83114 tick counter)\n" +
            "       non-zero = paused/frozen (time advances but ticks don't accumulate)");

        // Enhance scene IDs
        gn("00c82b00", null,
            "g_SceneID_FocusLost: Scene ID for focus-lost render mode (menu/pause screen?).\n" +
            "Set to 0 at startup, populated during scene loading.\n" +
            "Compared vs g_SceneID_Current in UpdateCursorVisibilityAndScene.\n" +
            "Used by SwitchRenderOutputMode when cursor is shown (focus lost).");

        gn("00c82b08", null,
            "g_SceneID_FocusGain: Scene ID for focus-gained render mode (active gameplay?).\n" +
            "Set to 0 at startup, populated during scene loading.\n" +
            "Compared vs g_SceneID_Current in UpdateCursorVisibilityAndScene.\n" +
            "Used by SwitchRenderOutputMode when cursor is hidden (focus gained).");

        gn("00c82ac8", null,
            "g_SceneID_Current: Current/target active scene ID.\n" +
            "Compared against g_SceneID_FocusLost/FocusGain to trigger scene transitions.");

        // Enhance render dispatch table
        gn("00c8c580", null,
            "g_pRenderDispatchTable: Render subsystem vtable/dispatch table.\n" +
            "Double indirection (**DAT_00c8c580) suggests vtable-like structure.\n" +
            "Used in:\n" +
            "  InitCLIAndTimingAndDevice: (*(*DAT_00c8c580))(0, 0) — Initialize(int,int)\n" +
            "  FinalizeDeviceSetup: (*(*DAT_00c8c580 + 8))() — Finalize()\n" +
            "Additional entries likely exist for render operations.");

        // Enhance deferred batch list
        gn("00bef7c0", null,
            "g_pDeferredBatchList: Linked list head for deferred render-batch nodes.\n" +
            "Processed by ProcessDeferredCallbacks within 2ms budget per frame.\n" +
            "Each node is a draw call descriptor; next pointer at node+0x7c.\n" +
            "BuildRenderBatch processes nodes and returns 1 when complete.\n" +
            "Recognized shader types: BLOOM, GLASS, BACKDROP.\n" +
            "Purpose: batches draw calls by material/shader to minimize state changes.");

        // Enhance callback interval
        gn("00c83190", null,
            "g_ullCallbackInterval_lo: Frame callback interval low 32-bits (16.16 fixed).\n" +
            "Likely initialized to 16ms<<16=0x100000 (60 FPS) or 33ms<<16=0x210000 (30 FPS).\n" +
            "Set once at startup in InitFrameCallbackSystem or first GameFrameUpdate.\n" +
            "Used throughout for frame pacing.");

        gn("00c83194", null,
            "g_ullCallbackInterval_hi: Frame callback interval high 32-bits.\n" +
            "Forms 64-bit interval with g_ullCallbackInterval_lo.\n" +
            "Likely 0 for typical frame intervals (16-33ms fits in low 32 bits).");

        // Enhance frame callback registration
        gn("00e69ca0", null,
            "g_pfnFrameCallback: Frame callback function pointer.\n" +
            "Set to FUN_0060c130 in InitGameSubsystems.\n" +
            "Main game logic update entry point called every frame.\n" +
            "Processes game state, entity updates, AI ticks.");

        // Enhance audio command queue
        gn("00be82ac", null,
            "g_pAudioCmdQueue: Async audio command queue.\n" +
            "Used by all audio operations in InitAudioSubsystem.\n" +
            "Polled via AudioPollGate (FUN_006109d0) in SleepEx(0,1) loops.\n" +
            "Queue status: -2 (error), 0 (pending), 1 (complete).");
    }

    // ── Helper Methods ─────────────────────────────────────────────────────────

    /** Set or extend function comment. */
    private void fc(String addrStr, String newName, String comment) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            Function f = fm.getFunctionAt(addr);
            if (f == null) {
                println("  NO FUNC @ " + addrStr);
                return;
            }
            
            String existing = f.getComment();
            if (existing != null && !existing.isEmpty()) {
                // Extend existing comment
                f.setComment(existing + "\n\n[ITER4 ENHANCEMENT]\n" + comment);
                println("  ENHANCED " + f.getName() + " @ " + addrStr);
            } else {
                f.setComment(comment);
                println("  COMMENTED " + f.getName() + " @ " + addrStr);
            }
            
            if (newName != null) {
                String curName = f.getName();
                if (curName.startsWith("FUN_") || curName.startsWith("thunk_FUN_")) {
                    f.setName(newName, SourceType.USER_DEFINED);
                    println("    → RENAMED to " + newName);
                }
            }
        } catch (Exception e) {
            println("  fc ERROR " + addrStr + ": " + e.getMessage());
        }
    }

    /** Enhance global comment (keeps existing name). */
    private void gn(String addrStr, String newName, String comment) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            
            // Add EOL comment
            listing = currentProgram.getListing();
            String existing = listing.getComment(CodeUnit.EOL_COMMENT, addr);
            if (existing != null && !existing.isEmpty()) {
                listing.setComment(addr, CodeUnit.EOL_COMMENT, existing + "\n[ITER4] " + comment);
                println("  ENHANCED global @ " + addrStr);
            } else {
                listing.setComment(addr, CodeUnit.EOL_COMMENT, comment);
                println("  COMMENTED global @ " + addrStr);
            }
            
            // Optionally rename if requested
            if (newName != null) {
                Symbol sym = st.getPrimarySymbol(addr);
                if (sym != null) {
                    String existing = sym.getName();
                    if (existing.startsWith("DAT_") || existing.startsWith("PTR_")) {
                        sym.setName(newName, SourceType.USER_DEFINED);
                        println("    → RENAMED to " + newName);
                    }
                }
            }
        } catch (Exception e) {
            println("  gn ERROR " + addrStr + ": " + e.getMessage());
        }
    }
}
