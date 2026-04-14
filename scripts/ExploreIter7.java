// Iteration 7: Deep exploration of remaining unknowns and subsystem implementations
// Focus on audio thread, memory management details, and game logic subsystems
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.symbol.*;
import ghidra.program.model.data.*;
import ghidra.program.model.mem.*;
import java.util.*;

public class ExploreIter7 extends GhidraScript {

    private FunctionManager fm;
    private SymbolTable symTable;
    private Listing listing;
    private Memory memory;

    @Override
    public void run() throws Exception {
        fm = currentProgram.getFunctionManager();
        symTable = currentProgram.getSymbolTable();
        listing = currentProgram.getListing();
        memory = currentProgram.getMemory();

        println("=== Iteration 7: Subsystem Implementation Deep Dive ===\n");

        // High priority unexplored areas
        exploreAudioThreadFunction();           // Q15: Audio thread entry point
        exploreMemoryAllocatorInternals();      // Q42-45: Free list structure
        exploreMessageDispatchImplementation(); // Q17-22: Message hashing and dispatch
        exploreSceneManagementSystem();         // Q23-25: Scene IDs and listener notifications
        exploreInputSystemStructures();         // Q54-60: Input device enumeration and state
        
        // Game logic subsystems
        exploreGameServicesSystem();            // Q37-41: Save system, profile data
        exploreTimeManagerStructure();          // Q47-51: Timing and interpolation
        exploreRenderQueueSystem();             // Q26-30: Batch queue and time budgets
        
        // Medium priority - DirectX and resources
        exploreShaderCapabilityDetection();     // Q9: Shader level determination
        exploreResourceNotificationSystem();    // Q10: Pre/Post release resources
        
        // Lower priority - completeness
        exploreAllVtables();                    // Q4-5: Complete vtable mapping
        exploreAllCommandLineFlags();           // Q62: Comprehensive CLI parsing
        
        println("\n=== Iteration 7 Exploration Complete ===");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Q15: Audio Thread Entry Point and Implementation
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreAudioThreadFunction() {
        println("\n[Q15] Exploring Audio Thread Entry Point...");
        
        // Audio thread creation is in FUN_00611940
        Function createAudioThread = fm.getFunctionAt(toAddr(0x00611940));
        if (createAudioThread != null) {
            println("Analyzing CreateAudioThread function:");
            println("  Name: " + createAudioThread.getName());
            
            // Look for CreateThread API call
            for (Instruction inst : listing.getInstructions(createAudioThread.getBody(), true)) {
                if (inst.getMnemonicString().equals("CALL")) {
                    Address target = getCallTarget(inst);
                    if (target != null) {
                        Function callee = fm.getFunctionAt(target);
                        if (callee != null && callee.getName().contains("CreateThread")) {
                            println(String.format("  Found CreateThread call at %s", inst.getAddress()));
                            
                            // CreateThread's 3rd param is lpStartAddress (thread function)
                            // Look backwards for PUSH or MOV that sets this parameter
                            Instruction current = inst.getPrevious();
                            int paramCount = 0;
                            while (current != null && paramCount < 6) {
                                String mnem = current.getMnemonicString();
                                if (mnem.equals("PUSH") || mnem.startsWith("MOV")) {
                                    paramCount++;
                                    if (paramCount == 4) { // 3rd param (0-indexed from top of stack)
                                        println("  Thread function parameter instruction: " + current);
                                        // Try to extract the address
                                        for (int i = 0; i < current.getNumOperands(); i++) {
                                            Object[] objs = current.getOpObjects(i);
                                            for (Object obj : objs) {
                                                if (obj instanceof Address) {
                                                    Address threadFunc = (Address) obj;
                                                    println(String.format("  Audio thread entry: %s", threadFunc));
                                                    analyzeAudioThreadFunction(threadFunc);
                                                }
                                            }
                                        }
                                    }
                                }
                                current = current.getPrevious();
                            }
                        }
                    }
                }
            }
        }
    }

    private void analyzeAudioThreadFunction(Address funcAddr) {
        println("\n  Analyzing audio thread function at " + funcAddr + ":");
        Function audioThread = fm.getFunctionAt(funcAddr);
        if (audioThread != null) {
            println("    Function: " + audioThread.getName());
            println("    Signature: " + audioThread.getSignature().getPrototypeString());
            
            // Look for main loop pattern (while/for)
            // and key function calls
            Set<String> calledFunctions = new LinkedHashSet<>();
            for (Instruction inst : listing.getInstructions(audioThread.getBody(), true)) {
                if (inst.getMnemonicString().equals("CALL")) {
                    Address target = getCallTarget(inst);
                    if (target != null) {
                        Function callee = fm.getFunctionAt(target);
                        if (callee != null) {
                            calledFunctions.add(String.format("%s (%s)", 
                                callee.getName(), target.toString()));
                        }
                    }
                }
            }
            
            println("    Called functions:");
            for (String func : calledFunctions) {
                println("      - " + func);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Q42-45: Memory Allocator Free List Structure
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreMemoryAllocatorInternals() {
        println("\n[Q42-45] Exploring Memory Allocator Free List Structure...");
        
        // Allocate function is at 00e61380
        Function allocFunc = fm.getFunctionAt(toAddr(0x00e61380));
        if (allocFunc != null) {
            println("Analyzing memory allocator: " + allocFunc.getName());
            println("  Signature: " + allocFunc.getSignature().getPrototypeString());
            
            // Count instructions and calls
            int instCount = 0;
            int callCount = 0;
            for (Instruction inst : listing.getInstructions(allocFunc.getBody(), true)) {
                instCount++;
                if (inst.getMnemonicString().equals("CALL")) {
                    callCount++;
                }
            }
            println(String.format("  Instructions: %d, Calls: %d", instCount, callCount));
        }
        
        // Also check the free function
        println("\nAnalyzing free function...");
        // Common pattern: free is near alloc or in vtable
        for (long offset = -0x100; offset < 0x100; offset += 0x10) {
            Address addr = toAddr(0x00e61380 + offset);
            Function func = fm.getFunctionAt(addr);
            if (func != null && (func.getName().toLowerCase().contains("free") || 
                                 func.getName().toLowerCase().contains("dealloc"))) {
                println("  Found potential free function: " + func.getName() + " at " + addr);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Q17-22: Message Dispatch Hash Implementation
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreMessageDispatchImplementation() {
        println("\n[Q17-22] Exploring Message Hash and Dispatch Implementation...");
        
        // Message registration is FUN_00e63e82
        Function msgRegister = fm.getFunctionAt(toAddr(0x00e63e82));
        if (msgRegister != null) {
            println("Analyzing message registration: " + msgRegister.getName());
            println("  Signature: " + msgRegister.getSignature().getPrototypeString());
            
            // Look for constant multipliers (hash magic numbers)
            for (Instruction inst : listing.getInstructions(msgRegister.getBody(), true)) {
                String mnem = inst.getMnemonicString();
                if (mnem.equals("IMUL") || mnem.equals("MUL")) {
                    println("  Hash multiply instruction: " + inst + " at " + inst.getAddress());
                } else if (mnem.equals("XOR")) {
                    println("  XOR instruction (hash mix?): " + inst + " at " + inst.getAddress());
                }
            }
        }
        
        // Also look at dispatch function
        println("\nSearching for message dispatch function...");
        SymbolIterator symbols = symTable.getSymbols("Dispatch");
        while (symbols.hasNext()) {
            Symbol sym = symbols.next();
            if (sym.getSymbolType() == SymbolType.FUNCTION) {
                println("  Found dispatch function: " + sym.getName() + " at " + sym.getAddress());
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Q23-25: Scene Management and Listener System
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreSceneManagementSystem() {
        println("\n[Q23-25] Exploring Scene Management System...");
        
        // Scene change is FUN_00e63c35
        Function sceneChange = fm.getFunctionAt(toAddr(0x00e63c35));
        if (sceneChange != null) {
            println("Analyzing scene change function: " + sceneChange.getName());
            
            // Look for the three scene ID globals
            println("\nSearching for scene ID globals...");
            
            // DAT_00bf22a0, DAT_00bf22a4, DAT_00bf22a8 are the three IDs
            Address[] sceneIDs = {
                toAddr(0x00bf22a0),
                toAddr(0x00bf22a4),
                toAddr(0x00bf22a8)
            };
            
            String[] names = {"CurrentSceneID", "PreviousSceneID", "PendingSceneID"};
            for (int i = 0; i < sceneIDs.length; i++) {
                println(String.format("  %s (DAT_%08x):", names[i], sceneIDs[i].getOffset()));
                Reference[] refs = getReferencesTo(sceneIDs[i]);
                println(String.format("    %d references", refs.length));
                
                // Show first few write references
                int count = 0;
                for (Reference ref : refs) {
                    if (count++ >= 3) break;
                    if (ref.getReferenceType().isWrite()) {
                        Function func = fm.getFunctionContaining(ref.getFromAddress());
                        if (func != null) {
                            println(String.format("      Write from: %s", func.getName()));
                        }
                    }
                }
            }
            
            // Look for listener notification calls
            println("\nSearching for listener notification in scene change...");
            for (Instruction inst : listing.getInstructions(sceneChange.getBody(), true)) {
                if (inst.getMnemonicString().equals("CALL")) {
                    Address target = getCallTarget(inst);
                    if (target != null) {
                        Function callee = fm.getFunctionAt(target);
                        if (callee != null && (callee.getName().toLowerCase().contains("notify") ||
                                               callee.getName().toLowerCase().contains("listener"))) {
                            println(String.format("    Notify call: %s at %s", 
                                callee.getName(), inst.getAddress()));
                        }
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Q54-60: Input System Structures
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreInputSystemStructures() {
        println("\n[Q54-60] Exploring Input System Structures...");
        
        // Input device enumeration is at FUN_00e64ec3
        Function inputEnum = fm.getFunctionAt(toAddr(0x00e64ec3));
        if (inputEnum != null) {
            println("Analyzing input device enumeration: " + inputEnum.getName());
            println("  Signature: " + inputEnum.getSignature().getPrototypeString());
            
            // Look for DirectInput API calls
            for (Instruction inst : listing.getInstructions(inputEnum.getBody(), true)) {
                if (inst.getMnemonicString().equals("CALL")) {
                    Address target = getCallTarget(inst);
                    if (target != null) {
                        Function callee = fm.getFunctionAt(target);
                        if (callee != null && callee.getName().contains("DirectInput")) {
                            println("  DirectInput API: " + callee.getName() + " at " + inst.getAddress());
                        }
                    }
                }
            }
        }
        
        // Look for global input state structures
        println("\nSearching for input state globals...");
        // DAT_00be8758 is RealInputSystem
        Address inputSystem = toAddr(0x00be8758);
        println(String.format("RealInputSystem at %s:", inputSystem));
        Reference[] refs = getReferencesTo(inputSystem);
        println(String.format("  %d references", refs.length));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Q37-41: GameServices System
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreGameServicesSystem() {
        println("\n[Q37-41] Exploring GameServices System...");
        
        // GameServices global is DAT_00bf2260
        Address gameServices = toAddr(0x00bf2260);
        println(String.format("GameServices at %s:", gameServices));
        
        Reference[] refs = getReferencesTo(gameServices);
        println(String.format("  %d references", refs.length));
        
        // Find functions that use GameServices
        Map<String, Integer> functionUsage = new LinkedHashMap<>();
        for (Reference ref : refs) {
            Function func = fm.getFunctionContaining(ref.getFromAddress());
            if (func != null) {
                String name = func.getName();
                functionUsage.put(name, functionUsage.getOrDefault(name, 0) + 1);
            }
        }
        
        println("\n  Functions using GameServices:");
        for (Map.Entry<String, Integer> entry : functionUsage.entrySet()) {
            println(String.format("    %s: %d accesses", entry.getKey(), entry.getValue()));
        }
        
        // Look for save/load related functions
        println("\n  Searching for save/load functions...");
        for (String name : functionUsage.keySet()) {
            String lower = name.toLowerCase();
            if (lower.contains("save") || lower.contains("load") || 
                lower.contains("profile") || lower.contains("game")) {
                Function func = fm.getFunction(name);
                if (func != null) {
                    println(String.format("    Potential save/load: %s at %s", 
                        name, func.getEntryPoint()));
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Q47-51: TimeManager Structure and Timing Implementation
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreTimeManagerStructure() {
        println("\n[Q47-51] Exploring TimeManager Structure...");
        
        // TimeManager global is DAT_00bf2328
        Address timeManager = toAddr(0x00bf2328);
        println(String.format("TimeManager at %s:", timeManager));
        Reference[] refs = getReferencesTo(timeManager);
        println(String.format("  %d references to TimeManager", refs.length));
        
        // Look for the timing callback functions
        // UpdateFrameTimingPrimary is FUN_006145f0
        Function primaryTiming = fm.getFunctionAt(toAddr(0x006145f0));
        if (primaryTiming != null) {
            println("\nAnalyzing primary timing callback: " + primaryTiming.getName());
            println("  Signature: " + primaryTiming.getSignature().getPrototypeString());
        }
        
        // Look for g_ullCallbackInterval
        println("\nSearching for callback interval global (g_ullCallbackInterval)...");
        // This should be set to 16 << 16 = 0x100000 for 60 FPS
        for (long addr = 0x008ae000; addr < 0x008af000; addr += 4) {
            try {
                long value = memory.getLong(toAddr(addr)) & 0xffffffffffffffffL;
                if (value == 0x100000L) {
                    println(String.format("  Found potential interval at %08x: 0x%x (16ms in 16.16 fixed)", 
                        addr, value));
                }
            } catch (Exception e) {
                // Ignore invalid addresses
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Q26-30: Render Queue System
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreRenderQueueSystem() {
        println("\n[Q26-30] Exploring Render Queue System...");
        
        // Render queue enqueue is FUN_00e63af4
        Function renderEnqueue = fm.getFunctionAt(toAddr(0x00e63af4));
        if (renderEnqueue != null) {
            println("Analyzing render queue enqueue: " + renderEnqueue.getName());
            println("  Signature: " + renderEnqueue.getSignature().getPrototypeString());
        }
        
        // Look for batch processing function
        println("\nSearching for render batch processing...");
        SymbolIterator symbols = symTable.getSymbols("Render");
        int count = 0;
        while (symbols.hasNext() && count++ < 20) {
            Symbol sym = symbols.next();
            if (sym.getSymbolType() == SymbolType.FUNCTION) {
                String name = sym.getName();
                if (name.toLowerCase().contains("batch") || 
                    name.toLowerCase().contains("queue") ||
                    name.toLowerCase().contains("process")) {
                    println(String.format("  Found: %s at %s", name, sym.getAddress()));
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Q9: Shader Capability Level Detection
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreShaderCapabilityDetection() {
        println("\n[Q9] Exploring Shader Capability Detection...");
        
        // g_nShaderCapabilityLevel is DAT_00bf1994
        Address shaderLevel = toAddr(0x00bf1994);
        println(String.format("g_nShaderCapabilityLevel at %s:", shaderLevel));
        
        Reference[] refs = getReferencesTo(shaderLevel);
        println(String.format("  %d references", refs.length));
        
        // Find where it's written
        for (Reference ref : refs) {
            if (ref.getReferenceType().isWrite()) {
                Address fromAddr = ref.getFromAddress();
                Function func = fm.getFunctionContaining(fromAddr);
                if (func != null) {
                    println(String.format("  Written in: %s at %s", 
                        func.getName(), fromAddr));
                    println("    Signature: " + func.getSignature().getPrototypeString());
                    break; // Only show first write location
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Q10: Resource Notification System
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreResourceNotificationSystem() {
        println("\n[Q10] Exploring Resource Notification System...");
        
        // NotifyPreReleaseResources is FUN_00e63d76
        // NotifyPostReleaseResources is FUN_00e63df2
        
        Address[] funcs = {toAddr(0x00e63d76), toAddr(0x00e63df2)};
        String[] names = {"NotifyPreReleaseResources", "NotifyPostReleaseResources"};
        
        for (int i = 0; i < funcs.length; i++) {
            Function func = fm.getFunctionAt(funcs[i]);
            if (func != null) {
                println(String.format("\nAnalyzing %s:", names[i]));
                println("  Function: " + func.getName());
                println("  Signature: " + func.getSignature().getPrototypeString());
                
                // Count calls to understand what it does
                int callCount = 0;
                for (Instruction inst : listing.getInstructions(func.getBody(), true)) {
                    if (inst.getMnemonicString().equals("CALL")) {
                        callCount++;
                    }
                }
                println(String.format("  Makes %d function calls", callCount));
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Q4-5: Complete Vtable Mapping
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreAllVtables() {
        println("\n[Q4-5] Exploring All Vtables...");
        
        // Primary callback manager vtable: PTR_FUN_00883f3c
        // Secondary factory vtable: PTR_FUN_00883f4c
        
        Address[] vtables = {toAddr(0x00883f3c), toAddr(0x00883f4c)};
        String[] names = {"CallbackManager Primary", "CallbackManager Secondary (Factory)"};
        
        for (int i = 0; i < vtables.length; i++) {
            println(String.format("\n%s vtable at %s:", names[i], vtables[i]));
            
            try {
                for (int slot = 0; slot < 20; slot++) {
                    Address slotAddr = vtables[i].add(slot * 4);
                    long funcPtr = memory.getInt(slotAddr) & 0xffffffffL;
                    
                    if (funcPtr == 0) {
                        println(String.format("  [%2d] nullptr (end of vtable)", slot));
                        break;
                    }
                    
                    Address funcAddr = toAddr(funcPtr);
                    Function func = fm.getFunctionAt(funcAddr);
                    String funcName = func != null ? func.getName() : "unknown";
                    
                    println(String.format("  [%2d] %s (%s)", slot, funcName, funcAddr));
                }
            } catch (Exception e) {
                println("  Error reading vtable: " + e.getMessage());
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Q62: All Command Line Flags
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreAllCommandLineFlags() {
        println("\n[Q62] Exploring All Command Line Flags...");
        
        // ParseCommandLineArg is at 00617bf0
        Function parseCmd = fm.getFunctionAt(toAddr(0x00617bf0));
        if (parseCmd != null) {
            println("Analyzing ParseCommandLineArg: " + parseCmd.getName());
            
            // Look for string comparisons
            println("\nSearching for flag strings...");
            Set<String> flags = new LinkedHashSet<>();
            
            for (Instruction inst : listing.getInstructions(parseCmd.getBody(), true)) {
                // Look for PUSH instructions with string references
                if (inst.getMnemonicString().equals("PUSH")) {
                    for (int i = 0; i < inst.getNumOperands(); i++) {
                        Object[] objs = inst.getOpObjects(i);
                        for (Object obj : objs) {
                            if (obj instanceof Address) {
                                Data data = listing.getDataAt((Address) obj);
                                if (data != null && data.getDataType().getName().contains("string")) {
                                    String str = (String) data.getValue();
                                    if (str != null && str.length() > 0) {
                                        flags.add(str);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            println("\nFound command line flags:");
            for (String flag : flags) {
                println("  - " + flag);
            }
        }
        
        // Also check CLI_CommandParser_ParseArgs
        println("\nAnalyzing CLI_CommandParser_ParseArgs...");
        Function cliParser = fm.getFunctionAt(toAddr(0x00eb787a));
        if (cliParser != null) {
            println("  Function: " + cliParser.getName());
            // This parses -name=value tokens
            println("  Parses -name=value format tokens");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ══════════════════════════════════════════════════════════════════════════════

    private Address getCallTarget(Instruction inst) {
        if (!inst.getMnemonicString().equals("CALL")) {
            return null;
        }
        
        Reference[] refs = inst.getReferencesFrom();
        for (Reference ref : refs) {
            if (ref.getReferenceType().isCall()) {
                return ref.getToAddress();
            }
        }
        return null;
    }

    private Address toAddr(long offset) {
        return currentProgram.getAddressFactory().getDefaultAddressSpace().getAddress(offset);
    }
}
