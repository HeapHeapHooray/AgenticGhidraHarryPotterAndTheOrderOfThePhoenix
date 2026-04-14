// Explore unknown functions identified in QUESTIONS.md iteration 2
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.app.decompiler.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import java.io.*;

public class ExploreUnknowns extends GhidraScript {

    private DecompInterface decompiler;
    private PrintWriter out;

    @Override
    public void run() throws Exception {
        decompiler = new DecompInterface();
        decompiler.openProgram(currentProgram);

        String outPath = "/home/heap/Documents/AgenticGhidraDecompilationHP/workspace/unknowns_explored.txt";
        out = new PrintWriter(new FileWriter(outPath));

        // Questions 1-3: DirectX/DI init thunks
        decompile("00684f30", "Q5_DirectXRenderCapsInit");
        decompile("00685410", "Q5b_ExtendedStateInit");
        decompile("0067d2e0", "Q25_PreDeviceCheck");
        decompile("0067ecf0", "Q28_ReleaseSurfaceCache");

        // Q4: DAT_00bf1924 init - search around WinMain/DirectX setup
        decompile("00eb612e", "Q2_DirectXDeviceCreate");
        decompile("00eb5c3e", "Q1_EngineSubsystemInit");
        decompile("00eb496e", "Q3_DirectInputFinalInit");

        // Q8: game state object
        decompile("00eb4a20", "Q8_GameStateHelper");

        // Q15-16: scene switching
        decompile("00612530", "Q16_SwitchRenderOutputMode");

        // Q19-20: frame update helpers
        decompile("00eb6dbc", "Q19_QueryVideoMemory");
        decompile("00612f00", "Q20_GameUpdateInit");

        // Q21: deferred callback node processor
        decompile("0063d600", "Q21_ProcessCallbackNode");

        // Q10: game pause state
        decompile("0058b790", "PauseGameUpdates_full");
        decompile("0058b8a0", "ResumeGameUpdates_full");

        // Q17: audio
        decompile("0061ef80", "PauseAudio_full");

        // Q26-27: capability checks helpers
        decompile("0067eb90", "Q26_D3DCapInit");
        decompile("0066dfe0", "Q27_GetHWCaps");

        // Fatal error function
        decompile("0066f810", "FatalError");

        out.close();
        println("Exploration output: " + outPath);
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
            out.println("ERROR " + addrStr + ": " + e.getMessage() + "\n");
        }
    }
}
