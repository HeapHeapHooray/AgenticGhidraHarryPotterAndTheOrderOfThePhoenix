// Second exploration pass for iteration 3 - deeper dives
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.app.decompiler.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.mem.*;
import java.io.*;

public class ExploreIter3b extends GhidraScript {

    private DecompInterface decompiler;
    private PrintWriter out;

    @Override
    public void run() throws Exception {
        decompiler = new DecompInterface();
        decompiler.openProgram(currentProgram);

        String outPath = "/home/heap/Documents/AgenticGhidraDecompilationHP/workspace/iter3b_explored.txt";
        out = new PrintWriter(new FileWriter(outPath));

        out.println("=== ITERATION 3B EXPLORATION ===\n");

        // Frame timing constants used in UpdateFrameTimingPrimary
        dumpMemoryWords("00845590", 16, "TimingConstants_0x845590");
        dumpMemoryWords("00845318", 16, "TimingConstants_0x845318");
        dumpMemoryWords("00845818", 8,  "TimingConstants_0x845818");

        // WinMain to understand thunk_FUN_00eb787a call context
        decompile("0060dfa0", "WinMain");

        // The thunk wrapper that WinMain calls (likely wraps CLI_CommandParser_ParseArgs)
        // Let's find the actual thunk by looking near 00eb787a
        dumpMemoryBytes("00eb7870", 32, "ThunkArea_near_eb787a");

        // Audio init chain
        decompile("006a9080", "AudioDevice_Open");
        decompile("006a91a0", "AudioDevice_QueryFormat");
        decompile("006a9140", "AudioDevice_Configure");
        decompile("006a90e0", "AudioDevice_Start");
        decompile("006a9ea0", "AudioStream_Pause");

        // Input system init
        decompile("00617890", "InputSystem_Init");

        // FUN_006677c0 (called at end of InitEngineObjects)
        decompile("006677c0", "UnknownInit_006677c0");

        // FUN_006125a0 (called in SwitchRenderOutputMode on pending change)
        decompile("006125a0", "RenderObserver_ProcessPendingChange");

        // DAT_00c82b00 area - scene IDs used in UpdateCursorVisibilityAndScene
        dumpMemoryWords("00c82ac0", 32, "SceneIDArea");

        // DAT_00bef770 and DAT_00bef755 (set in InitCLIAndTimingAndDevice)
        dumpMemoryWords("00bef750", 8, "SubsystemFlags_bef750");

        // What creates DAT_00e6b384? Search around it
        dumpMemoryWords("00e6b380", 16, "GameStateArea_e6b380");

        // FUN_00617d70 (called in CLI_CommandParser_ParseArgs)
        decompile("00617d70", "CLI_ParseValue");

        // thunk_FUN_00eb59ce (message handler registration in FinalizeDeviceSetup)
        decompile("00eb59ce", "RegisterMessageHandler");

        // FUN_0067c290 (the actual D3D device creator)
        decompile("0067c290", "CreateD3DDevice");

        // FUN_00614210 (allocator/factory used throughout init)
        decompile("00614210", "AllocEngineObject");

        // FUN_0064ae00 (called in InitCLIAndTimingAndDevice with DAT_00c8e490)
        decompile("0064ae00", "InitRenderBatchSystem");

        // thunk_FUN_00eb496e full body (InitGameSubsystems)
        decompile("005090a0", "LoadScreen_LanguageSelect");

        // FUN_0060c130 (registered as callback in InitGameSubsystems)
        decompile("0060c130", "GameCallback_0060c130");

        out.close();
        println("Iter3b exploration written to: " + outPath);
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
            out.println("=== MEMORY DUMP (words) @ " + addrStr + " [" + label + "] ===");
            for (int i = 0; i < count; i += 4) {
                Address a = addr.add(i);
                int val = mem.getInt(a);
                // Print as float too
                float fval = Float.intBitsToFloat(val);
                out.printf("  %s: 0x%08X  float=%.8f  int=%d\n", a, val, fval, val);
            }
            out.println();
        } catch (Exception e) {
            out.println("MEM ERROR " + addrStr + " [" + label + "]: " + e.getMessage() + "\n");
        }
    }

    private void dumpMemoryBytes(String addrStr, int count, String label) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            Memory mem = currentProgram.getMemory();
            out.println("=== MEMORY DUMP (bytes) @ " + addrStr + " [" + label + "] ===");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                if (i % 16 == 0) {
                    if (i > 0) out.println(sb.toString());
                    sb = new StringBuilder();
                    sb.append(String.format("  %s: ", addr.add(i)));
                }
                sb.append(String.format("%02X ", mem.getByte(addr.add(i)) & 0xFF));
            }
            if (sb.length() > 0) out.println(sb.toString());
            out.println();
        } catch (Exception e) {
            out.println("MEM ERROR " + addrStr + " [" + label + "]: " + e.getMessage() + "\n");
        }
    }
}
