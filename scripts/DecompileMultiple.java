// Decompile multiple functions and export to workspace
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.app.decompiler.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import java.io.*;

public class DecompileMultiple extends GhidraScript {

    private DecompInterface decompiler;
    private PrintWriter out;

    @Override
    public void run() throws Exception {
        decompiler = new DecompInterface();
        decompiler.openProgram(currentProgram);

        // Output file in workspace
        String outPath = "/home/heap/Documents/AgenticGhidraDecompilationHP/workspace/decompiled_functions.txt";
        out = new PrintWriter(new FileWriter(outPath));

        // Functions to analyze - addresses from ARCHITECTURE.md and DIFFERENCES.md
        String[][] functionsToAnalyze = {
            // Timing system
            {"00618010", "GetCurrentTime"},
            {"00617f50", "PrimaryFrameCallback"},
            {"00617ee0", "SecondaryFrameCallback"},
            {"00618140", "GameFrameUpdate"},

            // Pre-update / game logic
            {"00636830", "PreUpdateLogic"},

            // DirectX initialization chain
            {"00675950", "DirectXInit_1"},
            {"0067b820", "DirectXInit_2"},
            {"0067bb20", "DirectXInit_3"},
            {"00674430", "DirectXFinalInit"},
            {"0067cfb0", "ReleaseDirectXResources"},
            {"0067d0c0", "RestoreDirectXResources"},
            {"0067d310", "UpdateDirectXDevice"},

            // Input system
            {"0068da30", "AcquireInputDevices"},
            {"0068dac0", "UnacquireInputDevices"},

            // Window management
            {"0060d6d0", "WindowProc"},
            {"0060db20", "CreateGameWindow"},
            {"0060dc10", "MainGameLoop"},
            {"0060dfa0", "WinMain"},
            {"0060deb0", "SaveOrRestoreSystemParameters"},
            {"0060ce60", "ReadRegistrySetting"},
            {"0060c670", "WriteRegistrySetting"},
            {"0060d220", "SaveWindowPlacement"},

            // Fatal error
            {"0066f810", "FatalError"},

            // Unknown - from game loop
            {"0058b8a0", "UnknownGameLoop1"},

            // Helper functions referenced in resource management
            {"00617b60", "GraphicsStateChange"},
        };

        for (String[] entry : functionsToAnalyze) {
            String addr = entry[0];
            String name = entry[1];
            decompileAndPrint(addr, name);
        }

        out.close();
        println("Decompiled functions written to: " + outPath);
    }

    private void decompileAndPrint(String addressStr, String suggestedName) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addressStr);
            if (addr == null) {
                out.println("=== ERROR: Invalid address " + addressStr + " ===\n");
                return;
            }

            Function func = getFunctionAt(addr);
            if (func == null) {
                out.println("=== NO FUNCTION AT " + addressStr + " (suggested: " + suggestedName + ") ===\n");
                return;
            }

            String funcName = func.getName();
            out.println("=== FUNCTION: " + funcName + " @ " + addressStr + " (suggested: " + suggestedName + ") ===");
            out.println("Signature: " + func.getSignature());

            // Print existing comment if any
            String comment = func.getComment();
            if (comment != null && !comment.isEmpty()) {
                out.println("--- Existing Comment ---");
                out.println(comment);
                out.println("--- End Comment ---");
            }

            // Decompile
            DecompileResults results = decompiler.decompileFunction(func, 60, monitor);
            if (results.decompileCompleted()) {
                out.println("--- Decompiled C ---");
                out.println(results.getDecompiledFunction().getC());
                out.println("--- End Decompiled ---");
            } else {
                out.println("Decompilation failed: " + results.getErrorMessage());
            }

            out.println();
        } catch (Exception e) {
            out.println("=== EXCEPTION for " + addressStr + ": " + e.getMessage() + " ===\n");
        }
    }
}
