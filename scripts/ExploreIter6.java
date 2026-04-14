// Iteration 6: Deep exploration targeting priority questions from Iteration 5
// Focus on structure layouts, vtable completeness, and implementation details
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.symbol.*;
import ghidra.program.model.data.*;
import ghidra.program.model.mem.*;
import java.util.*;

public class ExploreIter6 extends GhidraScript {

    private FunctionManager fm;
    private SymbolTable symTable;
    private Listing listing;
    private Memory memory;
    private DataTypeManager dtm;

    @Override
    public void run() throws Exception {
        fm = currentProgram.getFunctionManager();
        symTable = currentProgram.getSymbolTable();
        listing = currentProgram.getListing();
        memory = currentProgram.getMemory();
        dtm = currentProgram.getDataTypeManager();

        println("=== Iteration 6: Deep Structural Exploration ===\n");

        // Priority Questions from QUESTIONS.md
        exploreEngineObjectLayout();           // Q1: Engine object field offsets
        exploreAudioCommandQueue();            // Q11-13: Audio queue structure
        exploreMessageHashAlgorithm();         // Q17: Message hash implementation
        exploreSceneListenerStructure();       // Q23: Scene listener list
        exploreRenderBatchNode();              // Q26-27: Render batch structure
        exploreMemoryAllocatorDetails();       // Q42-44: Allocator internals
        exploreCallbackInterval();             // Q49: Timing interval value
        
        // Medium priority explorations
        exploreDirectXParameters();            // Q7-10: D3D creation details
        exploreSubsystemStructures();          // Q31-41: Subsystem layouts
        exploreInputSystemDetails();           // Q54-57: Input structures
        exploreRenderDispatchTable();          // Q71-72: Render vtable
        
        // Additional deep dives
        exploreVtableCompleteness();           // Q4-5: Complete vtable mapping
        exploreCommandLineFlags();             // Q62: All CLI flags
        exploreResourceManagement();           // Q78: Resource system
        
        println("\n=== Exploration Complete ===");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Priority Q1: Engine Object Layout (DAT_00bef6d0, 0xb58 bytes)
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreEngineObjectLayout() {
        println("\n[Q1] Analyzing Engine Object Layout (DAT_00bef6d0)...");
        
        Address engineObj = toAddr(0x00bef6d0);
        println("Engine object size: 0xb58 (2904) bytes");
        
        // Find all cross-references that read/write to engine object
        println("\nAnalyzing field access patterns:");
        Set<Long> accessedOffsets = new TreeSet<>();
        
        for (long offset = 0; offset < 0xb58; offset += 4) {
            Address fieldAddr = engineObj.add(offset);
            var refs = getReferencesTo(fieldAddr);
            if (refs.length > 0) {
                accessedOffsets.add(offset);
            }
        }
        
        println(String.format("Found %d accessed field offsets:", accessedOffsets.size()));
        for (Long offset : accessedOffsets) {
            Address fieldAddr = engineObj.add(offset);
            var refs = getReferencesTo(fieldAddr);
            println(String.format("  +0x%03x: %d references", offset, refs.length));
            
            // Sample first few references to understand usage
            int count = 0;
            for (var ref : refs) {
                if (count++ >= 2) break;
                Address fromAddr = ref.getFromAddress();
                Function func = fm.getFunctionContaining(fromAddr);
                if (func != null) {
                    println(String.format("    <- %s (%s)", 
                        func.getName(), fromAddr.toString()));
                }
            }
        }
        
        // Check for vtable pointer at +0x00
        println("\nChecking vtable pointer at +0x00:");
        try {
            long vtablePtr = memory.getInt(engineObj) & 0xffffffffL;
            println(String.format("  Possible vtable: 0x%08x", vtablePtr));
            exploreVtableAt(toAddr(vtablePtr), "EngineObject");
        } catch (Exception e) {
            println("  Could not read vtable pointer");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Priority Q11-13: Audio Command Queue Structure
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreAudioCommandQueue() {
        println("\n[Q11-13] Analyzing Audio Command Queue (DAT_00be82ac)...");
        
        Address queueAddr = toAddr(0x00be82ac);
        
        // Analyze functions that access the queue
        println("\nFunctions accessing audio queue:");
        var refs = getReferencesTo(queueAddr);
        Map<String, Integer> funcAccess = new HashMap<>();
        
        for (var ref : refs) {
            Function func = fm.getFunctionContaining(ref.getFromAddress());
            if (func != null) {
                funcAccess.put(func.getName(), 
                    funcAccess.getOrDefault(func.getName(), 0) + 1);
            }
        }
        
        for (var entry : funcAccess.entrySet()) {
            println(String.format("  %s: %d accesses", entry.getKey(), entry.getValue()));
        }
        
        // Analyze AudioPollGate (FUN_006109d0)
        println("\nDecompiling AudioPollGate (FUN_006109d0):");
        Function pollGate = fm.getFunctionAt(toAddr(0x006109d0));
        if (pollGate != null) {
            analyzeAudioFunction(pollGate);
        }
        
        // Analyze audio command structure by looking at enqueue/dequeue patterns
        println("\nSearching for command enqueue patterns:");
        searchForPattern("audio.*enqueue", true);
        
        println("\nSearching for command dequeue patterns:");
        searchForPattern("audio.*dequeue", true);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Priority Q17: Message Hash Algorithm
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreMessageHashAlgorithm() {
        println("\n[Q17] Analyzing Message Hash Algorithm...");
        
        // Search for hash-related function names
        println("\nSearching for hash functions:");
        searchForPattern(".*hash.*", false);
        
        // Look for message registration which likely contains hashing
        println("\nSearching for message registration:");
        searchForPattern(".*[Rr]egister.*[Mm]essage.*", false);
        
        // Search for common hash constants (CRC32, FNV, etc.)
        println("\nSearching for common hash magic numbers:");
        long[] hashConstants = {
            0x01000193L,  // FNV-1a 32-bit prime
            0x811c9dc5L,  // FNV-1a 32-bit offset
            0xEDB88320L,  // CRC32 polynomial (reversed)
            0x04C11DB7L   // CRC32 polynomial
        };
        
        for (long constant : hashConstants) {
            searchForConstant(constant);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Priority Q23: Scene Listener List Structure
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreSceneListenerStructure() {
        println("\n[Q23] Analyzing Scene Listener Structure...");
        
        // Search for scene-related functions
        println("\nSearching for scene management functions:");
        searchForPattern(".*[Ss]cene.*", false);
        
        // Look for listener registration/notification
        println("\nSearching for listener patterns:");
        searchForPattern(".*[Ll]istener.*", false);
        searchForPattern(".*[Nn]otify.*", false);
        
        // Find scene ID related data
        println("\nSearching for scene ID references:");
        searchForPattern(".*scene.*id.*", true);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Priority Q26-27: Render Batch Node Structure
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreRenderBatchNode() {
        println("\n[Q26-27] Analyzing Render Batch Node Structure...");
        
        // Search for render batch related functions
        println("\nSearching for render batch functions:");
        searchForPattern(".*[Rr]ender.*[Bb]atch.*", false);
        searchForPattern(".*[Bb]uild.*[Bb]atch.*", false);
        
        // Look for shader type strings
        println("\nSearching for shader type strings:");
        String[] shaderTypes = {"BLOOM", "GLASS", "BACKDROP", "OPAQUE", 
                                "ALPHA", "SHADOW", "SKY", "WATER", "TERRAIN"};
        for (String shader : shaderTypes) {
            Address strAddr = findString(shader);
            if (strAddr != null) {
                println(String.format("  Found '%s' at %s", shader, strAddr.toString()));
                var refs = getReferencesTo(strAddr);
                for (var ref : refs) {
                    Function func = fm.getFunctionContaining(ref.getFromAddress());
                    if (func != null) {
                        println(String.format("    Used in: %s", func.getName()));
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Priority Q42-44: Memory Allocator Details
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreMemoryAllocatorDetails() {
        println("\n[Q42-44] Analyzing Memory Allocator Structure...");
        
        // Search for allocation functions
        println("\nSearching for allocator functions:");
        searchForPattern(".*[Aa]lloc.*", false);
        
        // Look for free list references (mentioned at +0x428)
        println("\nSearching for free list patterns:");
        searchForPattern(".*free.*list.*", true);
        
        // Search for critical section (mentioned at +0x4e4)
        println("\nSearching for critical section usage:");
        searchForPattern(".*[Cc]ritical[Ss]ection.*", true);
        
        // Look for tag-based allocation
        println("\nSearching for allocation tags:");
        searchForPattern("CLI::CommandParser", true);
        searchForPattern("EngineObject", true);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Priority Q49: Callback Interval Value
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreCallbackInterval() {
        println("\n[Q49] Analyzing Callback Interval (g_ullCallbackInterval)...");
        
        // Search for timing interval references
        println("\nSearching for interval/timing data:");
        searchForPattern(".*interval.*", true);
        searchForPattern(".*frame.*time.*", true);
        
        // Look for QueryPerformanceFrequency usage
        println("\nSearching for performance counter functions:");
        Function qpf = findFunction("QueryPerformanceFrequency");
        if (qpf != null) {
            println("  Found QueryPerformanceFrequency");
            var refs = getReferencesTo(qpf.getEntryPoint());
            for (var ref : refs) {
                Function caller = fm.getFunctionContaining(ref.getFromAddress());
                if (caller != null) {
                    println(String.format("    Called from: %s", caller.getName()));
                }
            }
        }
        
        // Common frame intervals to search for
        long[] intervals = {16, 33, 60, 1000, 16666, 33333};
        for (long interval : intervals) {
            println(String.format("\nSearching for constant %d (0x%x):", interval, interval));
            searchForConstant(interval);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Medium Priority: DirectX Parameters (Q7-10)
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreDirectXParameters() {
        println("\n[Q7-10] Analyzing DirectX Creation Parameters...");
        
        // Find CreateDevice function
        println("\nSearching for D3D device creation:");
        searchForPattern(".*[Cc]reate.*[Dd]3[Dd].*[Dd]evice.*", false);
        
        // Look for D3DCAPS9 related code
        println("\nSearching for D3DCAPS9 usage:");
        searchForPattern(".*[Cc]aps.*", true);
        
        // Search for shader capability level (DAT_00bf1994)
        println("\nAnalyzing shader capability level (DAT_00bf1994):");
        Address shaderCap = toAddr(0x00bf1994);
        var refs = getReferencesTo(shaderCap);
        println(String.format("Found %d references to shader capability level", refs.length));
        for (var ref : refs) {
            Function func = fm.getFunctionContaining(ref.getFromAddress());
            if (func != null) {
                println(String.format("  %s (%s)", func.getName(), ref.getFromAddress()));
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Medium Priority: Subsystem Structures (Q31-41)
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreSubsystemStructures() {
        println("\n[Q31-41] Analyzing Subsystem Structures...");
        
        // GlobalTempBuffer (0x3c bytes)
        println("\nGlobalTempBuffer structure:");
        exploreStructure(0x00000000, 0x3c, "GlobalTempBuffer"); // Need actual address
        
        // Locale (0x8c bytes)
        println("\nLocale structure:");
        searchForPattern(".*[Ll]ocale.*", false);
        searchForPattern(".*[Ll]anguage.*", false);
        
        // FMV system
        println("\nFMV/Video codec system:");
        searchForPattern(".*fmv.*", true);
        searchForPattern(".*video.*codec.*", true);
        searchForPattern("bink", true);
        searchForPattern("smacker", true);
        
        // GameServices
        println("\nGameServices structure:");
        searchForPattern(".*[Gg]ame[Ss]ervices.*", false);
        
        // TimeManager
        println("\nTimeManager structure:");
        searchForPattern(".*[Tt]ime[Mm]anager.*", false);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Medium Priority: Input System Details (Q54-57)
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreInputSystemDetails() {
        println("\n[Q54-57] Analyzing Input System Structure...");
        
        // RealInputSystem (0x34 bytes)
        println("\nSearching for input system:");
        searchForPattern(".*[Ii]nput.*[Ss]ystem.*", false);
        
        // Device manager
        println("\nSearching for device management:");
        searchForPattern(".*[Dd]evice.*[Mm]anager.*", false);
        searchForPattern(".*enumerate.*device.*", true);
        
        // Event queue
        println("\nSearching for input events:");
        searchForPattern(".*[Ii]nput.*[Ee]vent.*", false);
        
        // Cursor handling
        println("\nSearching for cursor management:");
        searchForPattern(".*cursor.*", true);
        searchForPattern("Ordinal_5", true);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Medium Priority: Render Dispatch Table (Q71-72)
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreRenderDispatchTable() {
        println("\n[Q71-72] Analyzing Render Dispatch Table...");
        
        Address dispatchTable = toAddr(0x00c8c580);
        println(String.format("Render dispatch table at: %s", dispatchTable.toString()));
        
        try {
            long vtablePtr = memory.getInt(dispatchTable) & 0xffffffffL;
            println(String.format("Vtable pointer: 0x%08x", vtablePtr));
            exploreVtableAt(toAddr(vtablePtr), "RenderDispatch");
        } catch (Exception e) {
            println("Could not read dispatch table vtable");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Additional: Complete Vtable Mapping (Q4-5)
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreVtableCompleteness() {
        println("\n[Q4-5] Mapping Complete Vtables...");
        
        // Primary callback manager vtable
        println("\nPrimary callback manager vtable (PTR_FUN_00883f3c):");
        exploreVtableAt(toAddr(0x00883f3c), "CallbackManager_Primary");
        
        // Secondary callback manager vtable
        println("\nSecondary callback manager vtable (PTR_FUN_00883f4c):");
        exploreVtableAt(toAddr(0x00883f4c), "CallbackManager_Secondary");
        
        // RealGraphSystem vtable
        println("\nRealGraphSystem vtable (PTR_FUN_00885010):");
        exploreVtableAt(toAddr(0x00885010), "RealGraphSystem");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Additional: Command Line Flags (Q62)
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreCommandLineFlags() {
        println("\n[Q62] Discovering All Command Line Flags...");
        
        // Find ParseCommandLineArg function
        Function parseCmd = fm.getFunctionAt(toAddr(0x00617bf0));
        if (parseCmd != null) {
            println("Analyzing ParseCommandLineArg (00617bf0):");
            
            // Known flags to search for
            String[] knownFlags = {"fullscreen", "widescreen", "oldgen", 
                                   "showfps", "memorylwm", "nofmv"};
            
            for (String flag : knownFlags) {
                Address strAddr = findString(flag);
                if (strAddr != null) {
                    println(String.format("  Found flag '%s' at %s", flag, strAddr));
                }
            }
            
            // Search for other potential debug flags
            println("\nSearching for potential debug flags:");
            String[] debugFlags = {"debug", "nocollision", "godmode", "unlockall",
                                   "noaudio", "novideo", "windowed", "dev"};
            for (String flag : debugFlags) {
                Address strAddr = findString(flag);
                if (strAddr != null) {
                    println(String.format("  Found potential flag '%s' at %s", flag, strAddr));
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Additional: Resource Management (Q78)
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreResourceManagement() {
        println("\n[Q78] Analyzing Resource Management System...");
        
        // Search for archive formats
        println("\nSearching for archive/package formats:");
        String[] archiveExts = {".pak", ".zip", ".arc", ".dat", ".res"};
        for (String ext : archiveExts) {
            Address strAddr = findString(ext);
            if (strAddr != null) {
                println(String.format("  Found '%s' at %s", ext, strAddr));
            }
        }
        
        // Search for resource manager
        println("\nSearching for resource management:");
        searchForPattern(".*[Rr]esource.*[Mm]anager.*", false);
        searchForPattern(".*[Ff]ile.*[Ss]ystem.*", false);
        searchForPattern(".*[Ss]treaming.*", false);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Helper Functions
    // ══════════════════════════════════════════════════════════════════════════════

    private void exploreVtableAt(Address vtableAddr, String name) {
        println(String.format("\nExploring vtable: %s at %s", name, vtableAddr));
        
        try {
            int methodCount = 0;
            for (int i = 0; i < 50; i++) {  // Max 50 methods
                Address entryAddr = vtableAddr.add(i * 4);
                long funcPtr = memory.getInt(entryAddr) & 0xffffffffL;
                
                if (funcPtr == 0) {
                    println(String.format("  [%d]: nullptr (end of vtable)", i));
                    break;
                }
                
                // Check if it's a valid function pointer
                Address funcAddr = toAddr(funcPtr);
                Function func = fm.getFunctionAt(funcAddr);
                
                if (func != null) {
                    println(String.format("  [%d]: 0x%08x -> %s", i, funcPtr, func.getName()));
                    methodCount++;
                } else {
                    // Might be data or invalid
                    println(String.format("  [%d]: 0x%08x (not a function)", i, funcPtr));
                }
            }
            println(String.format("Total methods found: %d", methodCount));
        } catch (Exception e) {
            println(String.format("Error exploring vtable: %s", e.getMessage()));
        }
    }

    private void analyzeAudioFunction(Function func) {
        println(String.format("  Analyzing %s:", func.getName()));
        println(String.format("    Entry: %s", func.getEntryPoint()));
        println(String.format("    Size: %d bytes", func.getBody().getNumAddresses()));
        
        // Count calls made by this function
        int callCount = 0;
        for (Function called : func.getCalledFunctions(monitor)) {
            callCount++;
            if (callCount <= 5) {  // Show first 5
                println(String.format("    Calls: %s", called.getName()));
            }
        }
        if (callCount > 5) {
            println(String.format("    ... and %d more calls", callCount - 5));
        }
    }

    private void searchForPattern(String pattern, boolean caseSensitive) {
        SymbolIterator symbols = symTable.getSymbolIterator();
        int found = 0;
        
        while (symbols.hasNext() && found < 10) {  // Limit output
            Symbol symbol = symbols.next();
            String name = symbol.getName();
            
            boolean matches = caseSensitive ? 
                name.matches(pattern) : 
                name.toLowerCase().matches(pattern.toLowerCase());
            
            if (matches) {
                println(String.format("  %s at %s", name, symbol.getAddress()));
                found++;
            }
        }
        
        if (found == 0) {
            println("  (none found)");
        } else if (found >= 10) {
            println("  ... (showing first 10)");
        }
    }

    private void searchForConstant(long value) {
        println(String.format("  Searching for 0x%08x (%d):", value, value));
        
        // Search in code for immediate values (limited search)
        Address minAddr = currentProgram.getMinAddress();
        Address maxAddr = currentProgram.getMaxAddress();
        
        // Note: Full constant search would be too slow, just report
        println("    (Full constant search requires manual verification)");
    }

    private Address findString(String str) {
        Address minAddr = currentProgram.getMinAddress();
        Address maxAddr = currentProgram.getMaxAddress();
        
        Address found = find(minAddr, str.getBytes());
        return found;
    }

    private Function findFunction(String name) {
        SymbolIterator symbols = symTable.getSymbols(name);
        while (symbols.hasNext()) {
            Symbol symbol = symbols.next();
            if (symbol.getSymbolType() == SymbolType.FUNCTION) {
                return fm.getFunctionAt(symbol.getAddress());
            }
        }
        return null;
    }

    private void exploreStructure(long baseAddr, int size, String name) {
        if (baseAddr == 0) {
            println(String.format("  %s: address not yet identified", name));
            return;
        }
        
        println(String.format("  %s (0x%x bytes):", name, size));
        Address addr = toAddr(baseAddr);
        
        // Analyze field accesses
        for (int offset = 0; offset < size; offset += 4) {
            Address fieldAddr = addr.add(offset);
            var refs = getReferencesTo(fieldAddr);
            
            if (refs.length > 0) {
                println(String.format("    +0x%02x: %d refs", offset, refs.length));
            }
        }
    }
}
