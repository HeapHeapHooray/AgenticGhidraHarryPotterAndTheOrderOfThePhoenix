// Iteration 8 exploration script for HP.exe
// Focus: Game logic (spells, AI, physics), Asset loading, UI, Animation
// Run in Ghidra Script Manager on hp.exe

import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.*;
import ghidra.program.model.mem.*;
import ghidra.program.model.symbol.*;
import java.util.*;

public class ExploreIter8 extends GhidraScript {
    
    @Override
    public void run() throws Exception {
        println("=== ITERATION 8 EXPLORATION ===");
        println("Objective: Game logic, asset loading, UI, animation systems\n");
        
        // Priority 1: Spell System
        exploreSpellSystem();
        
        // Priority 2: AI System
        exploreAISystem();
        
        // Priority 3: Physics and Collision
        explorePhysicsSystem();
        
        // Priority 4: Asset Loading
        exploreAssetLoading();
        
        // Priority 5: UI System
        exploreUISystem();
        
        // Priority 6: Animation System
        exploreAnimationSystem();
        
        // Priority 7: Audio Deep Dive (DirectSound)
        exploreAudioDeepDive();
        
        // Priority 8: Render Queue Deep Dive (D3D9)
        exploreRenderQueueDeepDive();
        
        // Priority 9: Input Double Buffering
        exploreInputDoubleBuffering();
        
        println("\n=== EXPLORATION COMPLETE ===");
    }
    
    private void exploreSpellSystem() throws Exception {
        println("\n========================================");
        println("SPELL SYSTEM EXPLORATION");
        println("========================================");
        
        // Search for spell-related strings
        println("\n--- Searching for spell name strings ---");
        String[] spellKeywords = {
            "Expelliarmus", "Stupefy", "Protego", "Incendio", "Wingardium", 
            "Reducto", "Petrificus", "Accio", "Reparo", "Lumos",
            "spell", "wand", "cast", "mana", "magic"
        };
        
        for (String keyword : spellKeywords) {
            searchAndPrintString(keyword);
        }
        
        // Search for spell ID enums (common patterns: SPELL_*, kSpell*)
        println("\n--- Searching for spell enum references ---");
        // Look for switch statements with multiple spell cases
        searchForSwitchStatements("spell");
        
        // Search for gesture recognition functions
        println("\n--- Searching for gesture/wand pattern matching ---");
        String[] gestureKeywords = {"gesture", "pattern", "trace", "mouse_path", "wand_move"};
        for (String keyword : gestureKeywords) {
            searchAndPrintString(keyword);
        }
        
        // Look for spell effect application (damage, knockback, status effects)
        println("\n--- Searching for spell effect functions ---");
        searchForFunctionPattern("ApplySpell", "CastSpell", "SpellHit", "SpellEffect");
    }
    
    private void exploreAISystem() throws Exception {
        println("\n========================================");
        println("AI SYSTEM EXPLORATION");
        println("========================================");
        
        // Search for AI state machine
        println("\n--- Searching for AI state strings ---");
        String[] aiStates = {
            "IDLE", "PATROL", "CHASE", "ATTACK", "FLEE", "DEAD", "STUNNED",
            "idle", "patrol", "chase", "attack", "flee", "alert", "search"
        };
        
        for (String state : aiStates) {
            searchAndPrintString(state);
        }
        
        // Search for pathfinding
        println("\n--- Searching for pathfinding functions ---");
        searchForFunctionPattern("FindPath", "PathFind", "AStar", "Navigate", "Waypoint");
        
        // Search for perception system
        println("\n--- Searching for AI perception ---");
        String[] perceptionKeywords = {"sight", "vision", "hear", "detect", "perception", "sense"};
        for (String keyword : perceptionKeywords) {
            searchAndPrintString(keyword);
        }
        
        // Look for behavior tree or decision system
        println("\n--- Searching for AI decision system ---");
        searchForFunctionPattern("UpdateAI", "ThinkAI", "Behavior", "Decision", "AITick");
    }
    
    private void explorePhysicsSystem() throws Exception {
        println("\n========================================");
        println("PHYSICS SYSTEM EXPLORATION");
        println("========================================");
        
        // Check for third-party physics engine imports
        println("\n--- Searching for physics engine imports ---");
        String[] physicsLibs = {"Havok", "PhysX", "Newton", "Bullet", "ODE"};
        for (String lib : physicsLibs) {
            searchAndPrintString(lib);
        }
        
        // Search for collision detection
        println("\n--- Searching for collision functions ---");
        searchForFunctionPattern("Collision", "Intersect", "Overlap", "RayTest", "SweepTest");
        
        // Search for rigid body dynamics
        println("\n--- Searching for physics integration ---");
        String[] physicsKeywords = {"rigid", "velocity", "force", "impulse", "gravity", "friction"};
        for (String keyword : physicsKeywords) {
            searchAndPrintString(keyword);
        }
        
        // Search for character controller
        println("\n--- Searching for character controller ---");
        searchForFunctionPattern("CharacterController", "MoveCharacter", "Jump", "Ground", "Slope");
    }
    
    private void exploreAssetLoading() throws Exception {
        println("\n========================================");
        println("ASSET LOADING EXPLORATION");
        println("========================================");
        
        // Search for file I/O functions
        println("\n--- Searching for file loading functions ---");
        // Look for CreateFile, ReadFile, fopen, fread
        Address[] createFileRefs = findExternalFunctionReferences("CreateFileA");
        if (createFileRefs != null && createFileRefs.length > 0) {
            println("CreateFileA references: " + createFileRefs.length);
            for (int i = 0; i < Math.min(10, createFileRefs.length); i++) {
                Function caller = getFunctionContaining(createFileRefs[i]);
                if (caller != null) {
                    println("  " + createFileRefs[i] + " in " + caller.getName());
                }
            }
        }
        
        // Search for archive/package formats
        println("\n--- Searching for archive format strings ---");
        String[] archiveKeywords = {"pak", "dat", "archive", "bundle", "package", "TOC", "index"};
        for (String keyword : archiveKeywords) {
            searchAndPrintString(keyword);
        }
        
        // Search for compression
        println("\n--- Searching for compression libraries ---");
        String[] compressionLibs = {"zlib", "inflate", "decompress", "uncompress", "LZO", "LZMA"};
        for (String lib : compressionLibs) {
            searchAndPrintString(lib);
        }
        
        // Search for resource manager
        println("\n--- Searching for resource management ---");
        searchForFunctionPattern("LoadResource", "LoadTexture", "LoadMesh", "LoadSound", "LoadLevel");
        searchForFunctionPattern("ResourceManager", "AssetManager", "CacheManager");
        
        // Search for streaming
        println("\n--- Searching for streaming system ---");
        String[] streamingKeywords = {"stream", "async", "preload", "unload", "evict"};
        for (String keyword : streamingKeywords) {
            searchAndPrintString(keyword);
        }
    }
    
    private void exploreUISystem() throws Exception {
        println("\n========================================");
        println("UI SYSTEM EXPLORATION");
        println("========================================");
        
        // Search for UI element types
        println("\n--- Searching for UI widget strings ---");
        String[] uiWidgets = {"button", "menu", "slider", "checkbox", "textbox", "label", "panel", "window"};
        for (String widget : uiWidgets) {
            searchAndPrintString(widget);
        }
        
        // Search for UI rendering
        println("\n--- Searching for UI rendering functions ---");
        searchForFunctionPattern("DrawUI", "RenderUI", "DrawWidget", "DrawText", "DrawButton");
        
        // Search for input handling
        println("\n--- Searching for UI input handling ---");
        searchForFunctionPattern("UIInput", "HandleClick", "HandleKey", "Focus", "Navigate");
        
        // Search for font rendering
        println("\n--- Searching for font system ---");
        String[] fontKeywords = {"font", "glyph", "kerning", "ttf", "bitmap"};
        for (String keyword : fontKeywords) {
            searchAndPrintString(keyword);
        }
    }
    
    private void exploreAnimationSystem() throws Exception {
        println("\n========================================");
        println("ANIMATION SYSTEM EXPLORATION");
        println("========================================");
        
        // Search for animation blending
        println("\n--- Searching for animation functions ---");
        searchForFunctionPattern("PlayAnim", "BlendAnim", "UpdateAnim", "AnimState", "Transition");
        
        // Search for skeletal animation
        println("\n--- Searching for skeleton/bone system ---");
        String[] skelKeywords = {"bone", "skeleton", "joint", "rig", "pose"};
        for (String keyword : skelKeywords) {
            searchAndPrintString(keyword);
        }
        
        // Search for inverse kinematics
        println("\n--- Searching for IK system ---");
        String[] ikKeywords = {"IK", "inverse", "kinematics", "reach", "look_at"};
        for (String keyword : ikKeywords) {
            searchAndPrintString(keyword);
        }
    }
    
    private void exploreAudioDeepDive() throws Exception {
        println("\n========================================");
        println("AUDIO DEEP DIVE - DirectSound Implementation");
        println("========================================");
        
        // Find DirectSound API calls
        println("\n--- DirectSound API usage ---");
        String[] dsoundAPIs = {
            "DirectSoundCreate", "IDirectSound_CreateSoundBuffer", 
            "IDirectSoundBuffer_Play", "IDirectSoundBuffer_Stop",
            "IDirectSoundBuffer_SetVolume", "IDirectSoundBuffer_SetFrequency",
            "IDirectSoundBuffer_Lock", "IDirectSoundBuffer_Unlock"
        };
        
        for (String api : dsoundAPIs) {
            Address[] refs = findExternalFunctionReferences(api);
            if (refs != null && refs.length > 0) {
                println(api + ": " + refs.length + " references");
                for (int i = 0; i < Math.min(5, refs.length); i++) {
                    Function caller = getFunctionContaining(refs[i]);
                    if (caller != null) {
                        println("  " + refs[i] + " in " + caller.getName());
                    }
                }
            }
        }
        
        // Search for audio codec usage
        println("\n--- Audio codec detection ---");
        String[] codecs = {"mp3", "ogg", "vorbis", "wma", "wav", "pcm"};
        for (String codec : codecs) {
            searchAndPrintString(codec);
        }
        
        // Look at audio thread worker implementation
        println("\n--- Analyzing AudioThreadProc implementation ---");
        Function audioThread = findFunctionByName("AudioThreadProc");
        if (audioThread != null) {
            analyzeFunction(audioThread);
        }
    }
    
    private void exploreRenderQueueDeepDive() throws Exception {
        println("\n========================================");
        println("RENDER QUEUE DEEP DIVE - D3D9 Implementation");
        println("========================================");
        
        // Find D3D9 rendering API calls
        println("\n--- D3D9 rendering API usage ---");
        String[] d3d9APIs = {
            "IDirect3DDevice9_Clear", "IDirect3DDevice9_BeginScene", "IDirect3DDevice9_EndScene",
            "IDirect3DDevice9_Present", "IDirect3DDevice9_DrawIndexedPrimitive",
            "IDirect3DDevice9_SetVertexShader", "IDirect3DDevice9_SetPixelShader",
            "IDirect3DDevice9_SetTexture", "IDirect3DDevice9_SetRenderState",
            "IDirect3DDevice9_SetStreamSource", "IDirect3DDevice9_SetIndices"
        };
        
        for (String api : d3d9APIs) {
            Address[] refs = findExternalFunctionReferences(api);
            if (refs != null && refs.length > 0) {
                println(api + ": " + refs.length + " references");
                for (int i = 0; i < Math.min(3, refs.length); i++) {
                    Function caller = getFunctionContaining(refs[i]);
                    if (caller != null) {
                        println("  " + refs[i] + " in " + caller.getName());
                    }
                }
            }
        }
        
        // Analyze ProcessDeferredRenderQueue
        println("\n--- Analyzing ProcessDeferredRenderQueue ---");
        Address renderQueueAddr = toAddr(0x00e63af0); // Known from iter7
        Function renderFunc = getFunctionAt(renderQueueAddr);
        if (renderFunc != null) {
            analyzeFunction(renderFunc);
        }
    }
    
    private void exploreInputDoubleBuffering() throws Exception {
        println("\n========================================");
        println("INPUT DOUBLE BUFFERING EXPLORATION");
        println("========================================");
        
        // Analyze PollInputDevices for buffer swap logic
        println("\n--- Analyzing PollInputDevices implementation ---");
        Address pollInputAddr = toAddr(0x00e64ec0); // Known from iter7
        Function pollFunc = getFunctionAt(pollInputAddr);
        if (pollFunc != null) {
            analyzeFunction(pollFunc);
        }
        
        // Look for edge detection functions (IsKeyPressed, IsKeyReleased)
        println("\n--- Searching for input edge detection ---");
        searchForFunctionPattern("IsKeyPressed", "IsKeyReleased", "IsButtonDown", "WasPressed", "WasReleased");
        
        // Check RealInputSystem structure usage
        println("\n--- Analyzing RealInputSystem at 0x00be8758 ---");
        Address inputSystemAddr = toAddr(0x00be8758);
        analyzeDataAtAddress(inputSystemAddr, 0x460);
    }
    
    // ========== HELPER FUNCTIONS ==========
    
    private void searchAndPrintString(String searchStr) throws Exception {
        Address[] results = findStringReferences(searchStr);
        if (results != null && results.length > 0) {
            println("\"" + searchStr + "\": " + results.length + " references");
            for (int i = 0; i < Math.min(5, results.length); i++) {
                Function func = getFunctionContaining(results[i]);
                if (func != null) {
                    println("  " + results[i] + " in " + func.getName());
                } else {
                    println("  " + results[i] + " (no function)");
                }
            }
        }
    }
    
    private Address[] findStringReferences(String str) throws Exception {
        List<Address> results = new ArrayList<>();
        Memory mem = currentProgram.getMemory();
        
        // Search in all memory blocks
        for (MemoryBlock block : mem.getBlocks()) {
            if (block.isInitialized()) {
                Address found = mem.findBytes(block.getStart(), str.getBytes(), null, true, monitor);
                while (found != null && block.contains(found)) {
                    results.add(found);
                    Address searchStart = found.add(1);
                    if (searchStart.compareTo(block.getEnd()) >= 0) break;
                    found = mem.findBytes(searchStart, str.getBytes(), null, true, monitor);
                }
            }
        }
        
        return results.toArray(new Address[0]);
    }
    
    private void searchForFunctionPattern(String... patterns) throws Exception {
        FunctionManager funcMgr = currentProgram.getFunctionManager();
        for (Function func : funcMgr.getFunctions(true)) {
            String funcName = func.getName();
            for (String pattern : patterns) {
                if (funcName.toLowerCase().contains(pattern.toLowerCase())) {
                    println("Function match: " + funcName + " at " + func.getEntryPoint());
                    break;
                }
            }
        }
    }
    
    private void searchForSwitchStatements(String context) throws Exception {
        println("Searching for switch statements (context: " + context + ")...");
        // This would require more complex analysis - placeholder for now
        println("  (Manual inspection recommended)");
    }
    
    private Address[] findExternalFunctionReferences(String extFuncName) throws Exception {
        List<Address> refs = new ArrayList<>();
        
        // Find external function
        SymbolTable symTable = currentProgram.getSymbolTable();
        SymbolIterator symbols = symTable.getSymbols(extFuncName);
        
        for (Symbol sym : symbols) {
            if (sym.getSymbolType() == SymbolType.FUNCTION) {
                Reference[] references = getReferencesTo(sym.getAddress());
                for (Reference ref : references) {
                    refs.add(ref.getFromAddress());
                }
            }
        }
        
        return refs.toArray(new Address[0]);
    }
    
    private Function findFunctionByName(String name) {
        FunctionManager funcMgr = currentProgram.getFunctionManager();
        for (Function func : funcMgr.getFunctions(true)) {
            if (func.getName().equals(name)) {
                return func;
            }
        }
        return null;
    }
    
    private void analyzeFunction(Function func) throws Exception {
        println("\nAnalyzing function: " + func.getName() + " at " + func.getEntryPoint());
        println("  Body size: " + func.getBody().getNumAddresses() + " bytes");
        println("  Parameters: " + func.getParameterCount());
        println("  Local variables: " + func.getLocalVariables().length);
        
        // Print first 20 instructions
        println("  First instructions:");
        InstructionIterator instIter = currentProgram.getListing().getInstructions(func.getBody(), true);
        int count = 0;
        while (instIter.hasNext() && count < 20) {
            Instruction inst = instIter.next();
            println("    " + inst.getAddress() + ": " + inst);
            count++;
        }
    }
    
    private void analyzeDataAtAddress(Address addr, int size) throws Exception {
        println("\nAnalyzing data at " + addr + " (size: 0x" + Integer.toHexString(size) + ")");
        
        // Get references to this address
        Reference[] refsTo = getReferencesTo(addr);
        println("  References TO this address: " + refsTo.length);
        for (int i = 0; i < Math.min(10, refsTo.length); i++) {
            Function caller = getFunctionContaining(refsTo[i].getFromAddress());
            if (caller != null) {
                println("    " + refsTo[i].getFromAddress() + " in " + caller.getName());
            }
        }
        
        // Get references from this region
        Address endAddr = addr.add(size);
        Reference[] refsFrom = getReferencesFrom(addr);
        println("  References FROM this region: " + refsFrom.length);
        for (int i = 0; i < Math.min(10, refsFrom.length); i++) {
            println("    " + refsFrom[i].getFromAddress() + " -> " + refsFrom[i].getToAddress());
        }
    }
}
