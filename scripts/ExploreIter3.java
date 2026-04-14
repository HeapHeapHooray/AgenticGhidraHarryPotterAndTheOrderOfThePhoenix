// Explore unknown functions for iteration 3 - targets from QUESTIONS.md iter 2
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.app.decompiler.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.symbol.*;
import ghidra.program.model.mem.*;
import java.io.*;

public class ExploreIter3 extends GhidraScript {

    private DecompInterface decompiler;
    private PrintWriter out;

    @Override
    public void run() throws Exception {
        decompiler = new DecompInterface();
        decompiler.openProgram(currentProgram);

        String outPath = "/home/heap/Documents/AgenticGhidraDecompilationHP/workspace/iter3_explored.txt";
        out = new PrintWriter(new FileWriter(outPath));

        out.println("=== ITERATION 3 EXPLORATION ===\n");

        // Q1: COM/thread init - called in WinMain before single-instance check
        decompile("00eb787a", "Q1_COMThreadInit");

        // Q2: DirectX device creation (thunk_FUN_00eb612e)
        decompile("00eb612e", "Q2_InitDirectXAndSubsystems");

        // DirectX subchain - called by Q2
        decompile("00614370", "Q2a_CreateD3DDevice");
        decompile("0060c2e0", "Q2b_AdditionalGraphicsInit");
        decompile("006147f0", "Q2c_FinalizeDeviceSetup");
        decompile("00eb60d3", "Q2d_DirectInputOrAudioInit");

        // Q3: DirectInput final init (thunk_FUN_00eb496e)
        decompile("00eb496e", "Q3_InitGameSubsystems");

        // Render pause - thunk_FUN_00ea53ca (called on WM_ACTIVATE both paths)
        decompile("00ea53ca", "Q_RenderPause");

        // Render teardown - thunk_FUN_00ec6610 (cleanup at WinMain exit)
        decompile("00ec6610", "Q_RenderTeardown");

        // Audio resume - thunk_FUN_00ec67e8 (called on delayed op timer expiry)
        decompile("00ec67e8", "Q_AudioResume");

        // Frame timing constants area - read globals near 0x008475d8
        dumpMemoryWords("008475d0", 16, "FrameTimingConstants");

        // Game state object - DAT_00e6b384 and vtable usage
        decompile("00eb4b95", "Q8_RegisterWindowClass");  // also creates game state?
        decompile("006180d0", "Q8_GameStateHelper2");

        // Q9: DAT_00e6b2dc object used in focus loss
        dumpMemoryWords("00e6b2d0", 8, "FocusLossObject");

        // Q11: callback interval init - where is DAT_00c83190/94 set?
        decompile("00612f00", "Q11_FrameCallbackInit");

        // Q15-16: scene IDs and SwitchRenderOutputMode
        decompile("00612530", "Q16_SwitchRenderOutputMode");
        dumpMemoryWords("00c82b00", 8, "SceneIDs");

        // Q18: COM object creation - what's thunk_FUN_00eb5c3e?
        decompile("00eb5c3e", "Q18_EngineSubsystemCreate");

        // Q21: ProcessDeferredCallbacks
        decompile("00636830", "ProcessDeferredCallbacks");

        // Q24: DirectInput init - FUN_00688370
        decompile("00688370", "Q24_DirectInputDeviceEnum");

        // Q25: PreDeviceCheck - before TestCooperativeLevel each frame
        decompile("0067d2e0", "Q25_PreDeviceCheck");

        // Q28: SetCachedRenderTargets - called with surface in ReleaseDirectXResources
        decompile("0067ecf0", "Q28_SetCachedRenderTargets");

        // Additional: InitRenderStates internals
        decompile("00675950", "InitRenderStates");
        decompile("00674430", "InitD3DStateDefaults");
        decompile("0067b820", "CreateGPUSyncQuery");

        // PauseGraphicsState - what exactly does it do?
        decompile("00617b60", "PauseGraphicsState");

        // Focus-gain vtable path - what's at DAT_00e6b384+0xc?
        dumpMemoryWords("00e6b380", 8, "GameStateObject");

        // UpdateFrameTimingPrimary and InterpolateFrameTime
        decompile("00617f50", "UpdateFrameTimingPrimary");
        decompile("00617ee0", "InterpolateFrameTime");

        // Audio pause full
        decompile("0061ef80", "PauseAudioFull");

        // Fatal error function
        decompile("0066f810", "FatalError");

        // QueryMemoryAllocatorMax
        decompile("00eb6dbc", "QueryMemoryAllocatorMax");

        out.close();
        println("Iteration 3 exploration written to: " + outPath);
    }

    private void decompile(String addrStr, String label) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            Function f = getFunctionAt(addr);
            if (f == null) {
                out.println("=== NO FUNCTION: " + addrStr + " (" + label + ") ===\n");
                return;
            }
            out.println("=== " + f.getName() + " @ " + addrStr + " [" + label + "] ===");
            out.println("Sig: " + f.getSignature());
            String cmt = f.getComment();
            if (cmt != null && !cmt.isEmpty()) {
                out.println("Comment: " + cmt);
            }
            DecompileResults r = decompiler.decompileFunction(f, 60, monitor);
            if (r.decompileCompleted()) {
                out.println(r.getDecompiledFunction().getC());
            } else {
                out.println("Decompile failed: " + r.getErrorMessage());
            }
            out.println();
        } catch (Exception e) {
            out.println("ERROR " + addrStr + " [" + label + "]: " + e.getMessage() + "\n");
        }
    }

    private void dumpMemoryWords(String addrStr, int count, String label) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            Memory mem = currentProgram.getMemory();
            out.println("=== MEMORY DUMP @ " + addrStr + " [" + label + "] ===");
            for (int i = 0; i < count; i += 4) {
                Address a = addr.add(i);
                int val = mem.getInt(a);
                out.printf("  %s: 0x%08X (%d)\n", a, val, val);
            }
            out.println();
        } catch (Exception e) {
            out.println("MEM ERROR " + addrStr + " [" + label + "]: " + e.getMessage() + "\n");
        }
    }
}
