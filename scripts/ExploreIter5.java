// Iteration 5: Deep dive into data structures and subsystem implementations
// Focuses on understanding structure layouts, vtables, and internal implementations
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.app.decompiler.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.symbol.*;
import ghidra.program.model.pcode.*;
import ghidra.program.model.data.*;
import ghidra.util.task.TaskMonitor;
import java.io.*;

public class ExploreIter5 extends GhidraScript {

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
        File outFile = new File("/home/heap/Documents/AgenticGhidraDecompilationHP/workspace/iter5_exploration.txt");
        out = new PrintWriter(new FileWriter(outFile));
        
        try {
            out.println("=".repeat(80));
            out.println("ITERATION 5 EXPLORATION - Deep Structure Analysis");
            out.println("=".repeat(80));
            out.println();
            
            // Investigate new questions from QUESTIONS.md (Iteration 5)
            exploreEngineObjectFactory();
            exploreCallbackManagerInternals();
            exploreDirectXDeviceParams();
            exploreAudioCommandQueue();
            exploreMessageDispatch();
            exploreSceneManagement();
            exploreDeferredRenderQueue();
            exploreSubsystemStructures();
            exploreFrameCallbackChain();
            exploreMemoryAllocator();
            exploreTimingSystem();
            exploreInputSystem();
            
            out.println("\n" + "=".repeat(80));
            out.println("EXPLORATION COMPLETE");
            out.println("=".repeat(80));
            
        } finally {
            out.close();
            decomp.dispose();
        }
        
        println("Exploration complete. Output written to: " + outFile.getAbsolutePath());
    }
    
    // ── Engine Object Factory ────────────────────────────────────────────────────
    
    private void exploreEngineObjectFactory() {
        section("Engine Object Factory (DAT_00bef6d0)");
        
        // Q1: What is the full structure of the engine object?
        subsection("Q1: Engine Object Structure (0xb58 = 2904 bytes)");
        exploreDataStructure("00bef6d0", 2904, "Engine Root Object");
        
        // Q2: What is the magic number format?
        subsection("Q2: Magic Number {0x88332000000001, 0} Usage");
        exploreFunction("00eb5c3e", "GetOrInitCallbackManager - factory call site");
        
        // Q3: What is the custom destructor?
        subsection("Q3: Engine Object Destructor (callback_mgr + 0xc)");
        exploreDataRef("00e6e870", "Callback Manager Primary Entry");
        exploreDataRef("00e6e874", "Callback Manager Secondary Entry");
    }
    
    // ── Callback Manager Internals ───────────────────────────────────────────────
    
    private void exploreCallbackManagerInternals() {
        section("Callback Manager Internals");
        
        // Q4: Full vtable structure
        subsection("Q4: Callback Manager Vtables");
        exploreVTable("00883f3c", "Primary Entry Vtable");
        exploreVTable("00883f4c", "Secondary Entry Vtable");
        
        // Q5: InitFrameCallbackSystem slot population
        subsection("Q5: Frame Callback Slots (DAT_00e6e880..e6e89c)");
        exploreFunction("00eb8744", "InitFrameCallbackSystem");
        exploreDataStructure("00e6e880", 0x1c, "Frame Callback Slot Array (8 slots)");
    }
    
    // ── DirectX Device Creation ──────────────────────────────────────────────────
    
    private void exploreDirectXDeviceParams() {
        section("DirectX Device Creation");
        
        // Q6: All 9 parameters to CreateD3DDevice
        subsection("Q6: CreateD3DDevice Parameter Analysis");
        exploreFunction("0067c290", "CreateD3DDevice (9 params)");
        
        // Q7: Shader capability level
        subsection("Q7: Shader Capability Level (DAT_00bf1994)");
        exploreDataRef("00bf1994", "g_nShaderCapabilityLevel");
        
        // Q8: Resource notification system
        subsection("Q8: Resource Notification (thunk_FUN_00ec04dc / 00ec19b5)");
        exploreFunction("00ec04dc", "Pre-release resource notification");
        exploreFunction("00ec19b5", "Post-release resource notification");
    }
    
    // ── Audio Command Queue ───────────────────────────────────────────────────────
    
    private void exploreAudioCommandQueue() {
        section("Audio Command Queue");
        
        // Q9: Structure of audio command queue
        subsection("Q9: Audio Command Queue Structure (DAT_00be82ac)");
        exploreDataStructure("00be82ac", 256, "Audio Command Queue");
        exploreDataRef("00be82ac", "Audio Queue References");
        
        // Q10: Audio thread relationship
        subsection("Q10: Audio Thread (DAT_00bf1b30)");
        exploreDataRef("00bf1b30", "Audio Thread Handle");
        exploreFunction("00611940", "Audio Thread Creation Function");
        
        // Q11: FUN_006a91a0 expected return value 0x80
        subsection("Q11: Audio Format Support Check");
        exploreFunction("006a91a0", "Audio format/capability query");
    }
    
    // ── Message Dispatch Implementation ───────────────────────────────────────────
    
    private void exploreMessageDispatch() {
        section("Message Dispatch Implementation");
        
        // Q12: Message ID hash algorithm
        subsection("Q12: Message String Hashing");
        exploreFunction("00eb59ce", "RegisterMessageHandler");
        // Look for hash function calls
        
        // Q13: Dispatch table structure
        subsection("Q13: Message Dispatch Table");
        // Search for dispatch table initialization
        
        // Q14: paramType parameter meaning
        subsection("Q14: Message Parameter Type Descriptor");
        // Analyze RegisterMessageHandler usage patterns
    }
    
    // ── Scene Management ──────────────────────────────────────────────────────────
    
    private void exploreSceneManagement() {
        section("Scene Management");
        
        // Q15: Scene ID population
        subsection("Q15: Scene ID Loading (when/how populated)");
        exploreDataRef("00c82b00", "g_SceneID_FocusLost");
        exploreDataRef("00c82b08", "g_SceneID_FocusGain");
        exploreDataRef("00c82ac8", "g_SceneID_Current");
        
        // Q16: Scene listener list structure
        subsection("Q16: Scene Listener Registration");
        exploreFunction("00612530", "SwitchRenderOutputMode");
        
        // Q17: Pending-change flag
        subsection("Q17: Scene Pending Change Flag (list.head+0x12)");
        exploreFunction("006125a0", "Deferred listener flush");
    }
    
    // ── Deferred Render Queue ─────────────────────────────────────────────────────
    
    private void exploreDeferredRenderQueue() {
        section("Deferred Render Queue");
        
        // Q18: Render batch node structure
        subsection("Q18: Render Batch Node Layout");
        exploreDataRef("00bef7c0", "Deferred Render Queue Head");
        exploreFunction("0063d600", "BuildRenderBatch");
        
        // Q19: Shader type recognition
        subsection("Q19: Shader Type Recognition Logic");
        // Analyze BuildRenderBatch for shader type checks
        
        // Q20: 2ms budget rationale
        subsection("Q20: ProcessDeferredCallbacks Time Budget");
        exploreFunction("0060dc10", "MainLoop - callback processing");
    }
    
    // ── Subsystem Initialization ──────────────────────────────────────────────────
    
    private void exploreSubsystemStructures() {
        section("Subsystem Structures");
        
        // Q21: GlobalTempBuffer structure
        subsection("Q21: GlobalTempBuffer Details (DAT_00e6b378)");
        exploreDataStructure("00e6b378", 0x3c, "GlobalTempBuffer");
        
        // Q22: RealGraphSystem role
        subsection("Q22: RealGraphSystem (DAT_00e6b390)");
        exploreDataStructure("00e6b390", 8, "RealGraphSystem");
        exploreVTable("00885010", "RealGraphSystem vtable");
        
        // Q23: Locale system
        subsection("Q23: Locale/Localization System (DAT_00e6b304)");
        exploreDataStructure("00e6b304", 0x8c, "Locale Object");
    }
    
    // ── Frame Callback Chain ──────────────────────────────────────────────────────
    
    private void exploreFrameCallbackChain() {
        section("Frame Callback Chain");
        
        // Q24: DAT_008e1644 callback table
        subsection("Q24: Frame Callback Table Entries");
        exploreDataRef("008e1644", "Frame Callback Function Table");
        
        // Q25: UpdateFrameTimingPrimary details
        subsection("Q25: Primary Frame Timing Update");
        // Find and analyze the primary callback function
        
        // Q26: InterpolateFrameTime usage
        subsection("Q26: Frame Time Interpolation");
        // Find interpolation function
        
        // Q27: Render dispatch table methods
        subsection("Q27: Render Dispatch Table (DAT_00c8c580)");
        exploreDataRef("00c8c580", "g_pRenderDispatchTable");
        exploreVTable("00c8c580", "Render Dispatch Vtable");
    }
    
    // ── Memory Allocator ──────────────────────────────────────────────────────────
    
    private void exploreMemoryAllocator() {
        section("Memory Allocator");
        
        // Q28: AllocEngineObject tracking
        subsection("Q28-29: AllocEngineObject Implementation");
        exploreFunction("00614210", "AllocEngineObject");
        
        // Q33: Free list structure
        subsection("Q33: Allocator Free List (allocator+0x428)");
        // Analyze allocator structure
        
        // Q34: Critical section
        subsection("Q34: Allocator Critical Section (allocator+0x4e4)");
        // Find allocator base and examine lock usage
    }
    
    // ── Timing System ─────────────────────────────────────────────────────────────
    
    private void exploreTimingSystem() {
        section("Timing System");
        
        // Q31: Frame callback interval
        subsection("Q31: Callback Interval Initialization");
        exploreDataRef("00c83190", "g_ullCallbackInterval_lo");
        exploreDataRef("00c83194", "g_ullCallbackInterval_hi");
        
        // Q32: TimeManager pause flag
        subsection("Q32: TimeManager Pause Triggers");
        exploreDataRef("00bef768", "g_pTimeManager");
        // Find all places that set the pause flag at +4
    }
    
    // ── Input System ──────────────────────────────────────────────────────────────
    
    private void exploreInputSystem() {
        section("Input System");
        
        // Q39: RealInputSystem structure
        subsection("Q39: RealInputSystem Structure (DAT_00e6b384)");
        exploreDataStructure("00e6b384", 0x34, "RealInputSystem");
        
        // Q40: Ordinal_5 custom cursor
        subsection("Q40: Custom Cursor Control (Ordinal_5)");
        // Find ordinal imports and cursor control functions
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
            
            // Get parameter info
            Parameter[] params = func.getParameters();
            if (params.length > 0) {
                out.println("\nParameters:");
                for (Parameter p : params) {
                    out.println("  " + p.getDataType().getName() + " " + p.getName());
                }
            }
            
            // Decompile
            DecompileResults results = decomp.decompileFunction(func, 30, TaskMonitor.DUMMY);
            if (results.decompileCompleted()) {
                out.println("\nDecompilation:");
                String code = results.getDecompiledFunction().getC();
                // Limit output if too large
                if (code.length() > 4000) {
                    out.println(code.substring(0, 4000));
                    out.println("\n... (truncated, " + (code.length() - 4000) + " more chars)");
                } else {
                    out.println(code);
                }
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
            
            // Get data at this address
            Data data = currentProgram.getListing().getDataAt(addr);
            if (data != null) {
                out.println("Type: " + data.getDataType().getName());
                out.println("Length: " + data.getLength() + " bytes");
            }
            
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
    
    private void exploreDataStructure(String addrStr, int size, String description) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            out.println("Data Structure @ " + addrStr + " (" + description + ")");
            out.println("Size: " + size + " bytes (0x" + Integer.toHexString(size) + ")");
            
            // Get data at this address
            Data data = currentProgram.getListing().getDataAt(addr);
            if (data != null) {
                out.println("Current Type: " + data.getDataType().getName());
                
                // If it's a structure, print its fields
                DataType dt = data.getDataType();
                if (dt instanceof Structure) {
                    Structure struct = (Structure) dt;
                    out.println("\nStructure Fields:");
                    DataTypeComponent[] components = struct.getComponents();
                    for (DataTypeComponent comp : components) {
                        out.println(String.format("  +0x%x: %s %s",
                            comp.getOffset(),
                            comp.getDataType().getName(),
                            comp.getFieldName() != null ? comp.getFieldName() : "(unnamed)"));
                    }
                }
            }
            
            // Show first few bytes as hex
            out.println("\nFirst bytes (hex):");
            byte[] bytes = new byte[Math.min(size, 64)];
            currentProgram.getMemory().getBytes(addr, bytes);
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                if (i % 16 == 0) {
                    if (i > 0) out.println(hex.toString());
                    hex = new StringBuilder(String.format("  %04x: ", i));
                }
                hex.append(String.format("%02x ", bytes[i] & 0xFF));
            }
            if (hex.length() > 0) out.println(hex.toString());
            
            // Get references
            out.println("\nReferences to this structure:");
            Reference[] refs = currentProgram.getReferenceManager().getReferencesTo(addr);
            if (refs.length == 0) {
                out.println("  (no references found)");
            } else {
                for (int i = 0; i < Math.min(refs.length, 15); i++) {
                    Address fromAddr = refs[i].getFromAddress();
                    Function func = fm.getFunctionContaining(fromAddr);
                    String funcName = (func != null) ? func.getName() : "???";
                    out.println("  " + refs[i].getReferenceType() + " from " + funcName + " @ " + fromAddr);
                }
                if (refs.length > 15) {
                    out.println("  ... and " + (refs.length - 15) + " more");
                }
            }
            
        } catch (Exception e) {
            out.println("ERROR exploring structure " + addrStr + ": " + e.getMessage());
        }
    }
    
    private void exploreVTable(String addrStr, String description) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            out.println("VTable @ " + addrStr + " (" + description + ")");
            
            // Read vtable entries (up to 20)
            out.println("\nVTable Methods:");
            for (int i = 0; i < 20; i++) {
                Address entryAddr = addr.add(i * 4); // Assume 32-bit pointers
                
                try {
                    int funcPtr = currentProgram.getMemory().getInt(entryAddr);
                    if (funcPtr == 0) break; // Null entry, end of vtable
                    
                    Address funcAddr = currentProgram.getAddressFactory().getAddress(
                        String.format("%08x", funcPtr));
                    Function func = fm.getFunctionAt(funcAddr);
                    
                    String funcName = (func != null) ? func.getName() : "???";
                    out.println(String.format("  [%2d] @ 0x%x: %s", i, funcPtr, funcName));
                    
                } catch (Exception e) {
                    break; // Invalid address, stop
                }
            }
            
        } catch (Exception e) {
            out.println("ERROR exploring vtable " + addrStr + ": " + e.getMessage());
        }
    }
}
