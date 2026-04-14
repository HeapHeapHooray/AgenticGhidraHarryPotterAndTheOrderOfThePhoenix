// Iteration 4: Explore functions mentioned in QUESTIONS.md to answer outstanding questions
// Focuses on understanding unknown function purposes and filling documentation gaps
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.app.decompiler.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.symbol.*;
import ghidra.program.model.pcode.*;
import ghidra.util.task.TaskMonitor;
import java.io.*;

public class ExploreIter4 extends GhidraScript {

    private DecompInterface decomp;
    private FunctionManager fm;
    private PrintWriter out;

    @Override
    public void run() throws Exception {
        fm = currentProgram.getFunctionManager();
        
        // Initialize decompiler
        decomp = new DecompInterface();
        decomp.openProgram(currentProgram);
        
        // Output to workspace folder
        File outFile = new File("/home/heap/Documents/AgenticGhidraDecompilationHP/workspace/iter4_exploration.txt");
        out = new PrintWriter(new FileWriter(outFile));
        
        try {
            out.println("=".repeat(80));
            out.println("ITERATION 4 EXPLORATION - QUESTIONS.md Investigation");
            out.println("=".repeat(80));
            out.println();
            
            // Investigate questions from QUESTIONS.md
            exploreDirectXInitChain();
            exploreAudioSystem();
            exploreEngineObjectSystem();
            exploreMessageSystem();
            exploreSceneRenderMode();
            exploreTiming();
            exploreMissingImplementations();
            
            out.println("\n" + "=".repeat(80));
            out.println("EXPLORATION COMPLETE");
            out.println("=".repeat(80));
            
        } finally {
            out.close();
            decomp.dispose();
        }
        
        println("Exploration complete. Output written to: " + outFile.getAbsolutePath());
    }
    
    // ── DirectX Initialization Chain Questions ──────────────────────────────────
    
    private void exploreDirectXInitChain() {
        section("DirectX Initialization Chain");
        
        // Q1-2: What is DAT_00bef6d0 and how is it created?
        subsection("Q1-2: Engine Object Creation (DAT_00bef6d0)");
        exploreFunction("00eb5c3e", "GetOrInitCallbackManager");
        
        // Q3: What is FUN_0067c290 (CreateD3DDevice)?
        subsection("Q3: CreateD3DDevice");
        exploreFunction("0067c290", "CreateD3DDevice");
        
        // Q4: What is FUN_0067bb20 in RestoreDirectXResources?
        subsection("Q4: Unknown in RestoreDirectXResources");
        exploreFunction("0067bb20", "Unknown stub");
        
        // Q6: What do thunk_FUN_00ec04dc and thunk_FUN_00ec19b5 do?
        subsection("Q6: Pre/post release cleanup in ReleaseDirectXResources");
        exploreFunction("00ec04dc", "Pre-release cleanup");
        exploreFunction("00ec19b5", "Post-release cleanup");
    }
    
    // ── Audio System Questions ───────────────────────────────────────────────────
    
    private void exploreAudioSystem() {
        section("Audio System");
        
        // Q7: What is FUN_006109d0 (audio polling gate)?
        subsection("Q7: Audio Polling Gate");
        exploreFunction("006109d0", "AudioPollGate");
        
        // Q8-9: Audio decoder control and finalize
        subsection("Q8-9: Audio Decoder Control");
        exploreFunction("00ec693d", "Audio decoder state control");
        exploreFunction("00ec66f1", "Audio finalize");
        
        // Q10: Audio teardown functions
        subsection("Q10: Audio Teardown Functions");
        exploreFunction("006ace30", "Audio teardown 1");
        exploreFunction("006108c0", "Audio teardown 2");
        exploreFunction("006ac930", "Audio teardown 3");
    }
    
    // ── Engine Object System Questions ───────────────────────────────────────────
    
    private void exploreEngineObjectSystem() {
        section("Engine Object System");
        
        // Q12: AllocEngineObject
        subsection("Q12: Engine Object Allocator");
        exploreFunction("00614210", "AllocEngineObject");
        
        // Q13: GlobalTempBuffer
        subsection("Q13: GlobalTempBuffer (DAT_00e6b378)");
        exploreDataRef("00e6b378", "g_pGlobalTempBuffer");
        
        // Q14: RealGraphSystem
        subsection("Q14: RealGraphSystem (DAT_00e6b390)");
        exploreDataRef("00e6b390", "g_pRealGraphSystem");
        
        // Q15-16: Unknown init functions
        subsection("Q15-16: Unknown Engine Init Functions");
        exploreFunction("00eb87ba", "Unknown init 1");
        exploreFunction("00eb88b2", "Unknown init 2");
        exploreFunction("006677c0", "Unknown init 3");
    }
    
    // ── Message System Questions ─────────────────────────────────────────────────
    
    private void exploreMessageSystem() {
        section("Message System");
        
        // Q17: Message handler registration
        subsection("Q17: Message Handler Registration");
        exploreFunction("00eb59ce", "RegisterMessageHandler");
        
        // Q18: Frame callback
        subsection("Q18: Registered Frame Callback");
        exploreFunction("0060c130", "FrameCallbackRegistration");
    }
    
    // ── Scene / Render Mode Questions ────────────────────────────────────────────
    
    private void exploreSceneRenderMode() {
        section("Scene / Render Mode");
        
        // Q19-20: Scene IDs and render mode switching
        subsection("Q19-20: Scene ID Globals");
        exploreDataRef("00c82b00", "g_SceneID_FocusLost");
        exploreDataRef("00c82b08", "g_SceneID_FocusGain");
        exploreDataRef("00c82ac8", "g_SceneID_Current");
        
        subsection("Render Mode Switching");
        exploreFunction("00612530", "SwitchRenderOutputMode");
        exploreFunction("006125a0", "Unknown deferred listener flush?");
    }
    
    // ── Timing Questions ──────────────────────────────────────────────────────────
    
    private void exploreTiming() {
        section("Timing System");
        
        // Q21: Callback interval
        subsection("Q21: Callback Interval Initialization");
        exploreDataRef("00c83190", "g_ullCallbackInterval_lo");
        exploreDataRef("00c83194", "g_ullCallbackInterval_hi");
        
        // Q22: TimeManager flag
        subsection("Q22: TimeManager Pause Flag");
        exploreDataRef("00bef768", "g_pTimeManager");
        exploreFunction("00eb797e", "TimeManager::Instance creator");
    }
    
    // ── Missing Implementations ──────────────────────────────────────────────────
    
    private void exploreMissingImplementations() {
        section("Missing Implementations");
        
        // Q24: BuildRenderBatch callers
        subsection("Q24: BuildRenderBatch Callers");
        exploreFunction("0063d600", "BuildRenderBatch");
        
        // Q25: Unknown function in FinalizeDeviceSetup
        subsection("Q25: GameServices and TimeManager Init");
        exploreFunction("0060b740", "GameServicesAndTimeManager_Init");
        
        // Q26: Render dispatch table
        subsection("Q26: Render Dispatch Table");
        exploreDataRef("00c8c580", "g_pRenderDispatchTable");
    }
    
    // ── Helper Methods ───────────────────────────────────────────────────────────
    
    private void section(String title) {
        out.println("\n" + "=".repeat(80));
        out.println(title.toUpperCase());
        out.println("=".repeat(80));
    }
    
    private void subsection(String title) {
        out.println("\n" + "-".repeat(80));
        out.println(title);
        out.println("-".repeat(80));
    }
    
    private void exploreFunction(String addrStr, String description) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            Function func = fm.getFunctionAt(addr);
            
            if (func == null) {
                out.println("NO FUNCTION AT " + addrStr + " (" + description + ")");
                return;
            }
            
            out.println("Function: " + func.getName() + " @ " + addrStr);
            out.println("Description: " + description);
            
            // Get existing comment
            String comment = func.getComment();
            if (comment != null && !comment.isEmpty()) {
                out.println("\nExisting Comment:");
                out.println(comment);
            }
            
            // Decompile
            DecompileResults results = decomp.decompileFunction(func, 30, TaskMonitor.DUMMY);
            if (results.decompileCompleted()) {
                out.println("\nDecompilation:");
                out.println(results.getDecompiledFunction().getC());
            } else {
                out.println("\nDecompilation FAILED: " + results.getErrorMessage());
            }
            
            // Find cross-references
            out.println("\nCalled from:");
            Function[] callers = func.getCallingFunctions(TaskMonitor.DUMMY);
            if (callers.length == 0) {
                out.println("  (no callers found)");
            } else {
                for (int i = 0; i < Math.min(callers.length, 10); i++) {
                    out.println("  " + callers[i].getName() + " @ " + callers[i].getEntryPoint());
                }
                if (callers.length > 10) {
                    out.println("  ... and " + (callers.length - 10) + " more");
                }
            }
            
        } catch (Exception e) {
            out.println("ERROR exploring " + addrStr + ": " + e.getMessage());
        }
    }
    
    private void exploreDataRef(String addrStr, String description) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            out.println("Data @ " + addrStr + " (" + description + ")");
            
            // Get references TO this address
            out.println("\nRead/Write references:");
            Reference[] refs = currentProgram.getReferenceManager().getReferencesTo(addr);
            if (refs.length == 0) {
                out.println("  (no references found)");
            } else {
                for (int i = 0; i < Math.min(refs.length, 20); i++) {
                    Address fromAddr = refs[i].getFromAddress();
                    Function func = fm.getFunctionContaining(fromAddr);
                    String funcName = (func != null) ? func.getName() : "???";
                    out.println("  " + refs[i].getReferenceType() + " from " + funcName + " @ " + fromAddr);
                }
                if (refs.length > 20) {
                    out.println("  ... and " + (refs.length - 20) + " more");
                }
            }
            
        } catch (Exception e) {
            out.println("ERROR exploring data " + addrStr + ": " + e.getMessage());
        }
    }
}
