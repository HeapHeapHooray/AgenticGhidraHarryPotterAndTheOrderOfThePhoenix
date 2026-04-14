// Iteration 3, step 4: targeted exploration based on QUESTIONS.md
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.app.decompiler.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.mem.*;
import java.io.*;

public class ExploreIter3c extends GhidraScript {

    private DecompInterface decompiler;
    private PrintWriter out;

    @Override
    public void run() throws Exception {
        decompiler = new DecompInterface();
        decompiler.openProgram(currentProgram);

        String outPath = "/home/heap/Documents/AgenticGhidraDecompilationHP/workspace/iter3c_explored.txt";
        out = new PrintWriter(new FileWriter(outPath));
        out.println("=== ITERATION 3C: TARGETED EXPLORATION ===\n");

        // Q1: thunk_FUN_00ec64f9 - called in WinMain before DirectX init
        decompile("00ec64f9", "Q1_PreDirectXInit");

        // Q5: thunk_FUN_00eb4a5d - WinMain cleanup
        decompile("00eb4a5d", "Q5_WinMainCleanupThunk");

        // Q6: thunk_FUN_00ec04dc / thunk_FUN_00ec19b5 - DX resource pre/post
        decompile("00ec04dc", "Q6_DXPreRelease");
        decompile("00ec19b5", "Q6_DXPostRelease");

        // Q13/14: What are DAT_00e6b378 (GlobalTempBuffer) and DAT_00e6b390 (RealGraphSystem)?
        dumpMemoryWords("00e6b370", 16, "EngineObjectPointers");

        // Q17: thunk_FUN_00eb59ce (RegisterMessageHandler)
        decompile("00eb59ce", "Q17_RegisterMessageHandler");

        // Q21: callback interval - where is DAT_00c83190/94 set?
        // Search for writes to that address - try GameFrameUpdate and nearby init
        decompile("00618140", "GameFrameUpdate");
        decompile("00612f00", "InitFrameCallbackSystem_full");

        // Q22: DAT_00bef768 - what is it?
        dumpMemoryWords("00bef760", 12, "TimingControlBlock_bef760");

        // Q25: FUN_0060b740 - called in FinalizeDeviceSetup
        decompile("0060b740", "Q25_PostMsgHandlerSetup");

        // Q26: DAT_00c8c580 - render dispatch table
        dumpMemoryWords("00c8c578", 8, "RenderDispatchTable");
        decompile("00690470", "RenderDispatchEntry0");  // address from GlobalTempBuffer init

        // FUN_0067bb20 - the "empty stub" in RestoreDirectXResources
        decompile("0067bb20", "Q4_RestoreResourcesHelper");

        // What is FUN_006109d0 (audio polling gate)?
        decompile("006109d0", "Q7_AudioPollGate");

        // thunk_FUN_00ec693d (audio decoder control - seek/speed)
        decompile("00ec693d", "Q8_AudioDecoderControl");

        // What does FUN_006ac010 do (called in all async audio ops)?
        decompile("006ac010", "AudioAsyncPump");

        // What is FUN_00610ba0 (also called in audio init chain)?
        decompile("00610ba0", "Q7b_AudioResultGet");

        // What is FUN_006119c0 (in RenderAndAudioTeardown)?
        decompile("006119c0", "Q10_AudioTeardown1");
        decompile("006ace30", "Q10_AudioTeardown2");
        decompile("006108c0", "Q10_AudioTeardown3");
        decompile("006ac930", "Q10_AudioTeardown4");

        // What is FUN_0060b740 (in FinalizeDeviceSetup)?
        decompile("0060b740", "Q25b_PostMsgHandlerSetup");

        // What is DAT_00c83190/94? Look for writes near GameFrameUpdate init
        dumpMemoryWords("00c83188", 24, "TimingCallbackState_c83188");

        out.close();
        println("Iter3c exploration written to: " + outPath);
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
                float fval = Float.intBitsToFloat(val);
                out.printf("  %s: 0x%08X  float=%.6f\n", a, val, fval);
            }
            out.println();
        } catch (Exception e) {
            out.println("MEM ERROR " + addrStr + " [" + label + "]: " + e.getMessage() + "\n");
        }
    }
}
