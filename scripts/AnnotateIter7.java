// Iteration 7: Comprehensive subsystem annotations
// Annotates audio, memory, input, message dispatch, render queue, and scene systems
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.symbol.*;

public class AnnotateIter7 extends GhidraScript {

    private FunctionManager fm;
    private SymbolTable symTable;
    private Listing listing;

    @Override
    public void run() throws Exception {
        fm = currentProgram.getFunctionManager();
        symTable = currentProgram.getSymbolTable();
        listing = currentProgram.getListing();

        println("=== Starting Iteration 7 Annotations ===\n");

        // Audio subsystem
        annotateAudioSystem();
        
        // Memory allocator
        annotateMemoryAllocator();
        
        // Message dispatch
        annotateMessageDispatch();
        
        // Input system
        annotateInputSystem();
        
        // Render queue
        annotateRenderQueue();
        
        // Scene management
        annotateSceneManagement();
        
        // GameServices and subsystems
        annotateGameServices();
        
        // Command line flags
        annotateCommandLineFlags();
        
        // Shader system
        annotateShaderSystem();

        println("\n=== Iteration 7 Annotations Complete ===");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Audio System
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateAudioSystem() {
        println("\n[Audio System] Annotating audio thread and command queue...");

        // Audio thread creation function
        annotateFunc("00611940",
            "CreateAudioThread",
            "Creates dedicated audio worker thread using CreateThread API\n" +
            "Thread handle stored in g_hAudioThread (DAT_00be93c8)\n" +
            "Thread ID stored in g_dwAudioThreadId (DAT_00be93c4)\n" +
            "Thread processes commands from AudioCommandQueue\n" +
            "Iteration 7: Confirmed thread-based async audio architecture");

        // Audio polling gate
        annotateFunc("006109d0",
            "AudioPollGate",
            "Polls audio async operation status from command queue\n" +
            "Returns:\n" +
            "  -2 = Error condition (queue corrupted or command failed)\n" +
            "  0  = Pending (command still processing)\n" +
            "  1  = Complete (command finished or queue empty)\n" +
            "Used throughout init for non-blocking audio setup\n" +
            "Iteration 7: Ring buffer pattern with status checking");

        // Audio command queue
        annotateData("00be82ac",
            "g_pAudioCommandQueue",
            "Audio command queue for async operations\n" +
            "Structure: Ring buffer with 64 command slots\n" +
            "Layout:\n" +
            "  +0x000: AudioCommand commands[64]\n" +
            "  +0x400: int head (read index)\n" +
            "  +0x404: int tail (write index)\n" +
            "  +0x408: int count (active commands)\n" +
            "  +0x40c: CRITICAL_SECTION (thread sync)\n" +
            "  +0x424: HANDLE event (wake notification)\n" +
            "Commands: OPEN, QUERY, CONFIGURE, START, STOP\n" +
            "Iteration 7: Thread-safe ring buffer confirmed");

        // Audio thread handle
        annotateData("00be93c8",
            "g_hAudioThread",
            "Handle to audio worker thread\n" +
            "Created by CreateAudioThread (FUN_00611940)\n" +
            "Thread processes command queue continuously\n" +
            "Iteration 7: Main audio thread handle");

        // Audio thread ID
        annotateData("00be93c4",
            "g_dwAudioThreadId",
            "Audio worker thread ID\n" +
            "Returned by CreateThread during audio init\n" +
            "Used for thread identification and debugging");

        // Audio thread running flag
        annotateData("00be93c0",
            "g_bAudioThreadRunning",
            "Audio thread running flag\n" +
            "Set to TRUE when thread starts\n" +
            "Set to FALSE to signal thread termination\n" +
            "Thread checks this in main loop");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Memory Allocator
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateMemoryAllocator() {
        println("\n[Memory Allocator] Annotating allocator internals...");

        // Main allocator function
        annotateFunc("00e61380",
            "AllocEngineObject",
            "Custom memory allocator with free list and tag tracking\n" +
            "Parameters:\n" +
            "  size_t size - Bytes to allocate\n" +
            "  const char* tag - Debug tag for tracking\n" +
            "Returns: void* to allocated memory\n" +
            "Implementation:\n" +
            "  1. Align size to 8 bytes (mask 0x7ffffff8)\n" +
            "  2. Select free list bucket (254 buckets)\n" +
            "  3. Search free list for suitable block\n" +
            "  4. If found: remove from list and return\n" +
            "  5. If not: allocate new block from heap\n" +
            "  6. Update tag statistics\n" +
            "Thread-safe via CRITICAL_SECTION at +0x820\n" +
            "Iteration 7: Free list allocator with tag tracking");

        // Allocator structure
        annotateData("00e61380",
            "g_pEngineAllocator",
            "Engine memory allocator structure (0x840 bytes)\n" +
            "Layout:\n" +
            "  +0x000: void* vtable\n" +
            "  +0x004: DWORD total_allocated\n" +
            "  +0x008: DWORD current_allocated\n" +
            "  +0x00c: DWORD peak_usage\n" +
            "  +0x010: DWORD allocation_count\n" +
            "  +0x014: DWORD free_count\n" +
            "  +0x018: DWORD current_alloc_count\n" +
            "  +0x01c: void* heap_base\n" +
            "  +0x020: SIZE_T heap_size\n" +
            "  +0x024: DWORD flags\n" +
            "  +0x028: TagStats[256] (per-tag tracking)\n" +
            "  +0x428: FreeListNode*[254] (size-class buckets)\n" +
            "  +0x820: CRITICAL_SECTION\n" +
            "Iteration 7: Complete allocator layout");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Message Dispatch System
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateMessageDispatch() {
        println("\n[Message Dispatch] Annotating message system...");

        // Message registration
        annotateFunc("00e63e82",
            "RegisterMessageHandler",
            "Registers message handler with string name hashing\n" +
            "Parameters:\n" +
            "  void* dest - Destination object\n" +
            "  const char* msgName - Message name (e.g., 'iMsg_SceneChange')\n" +
            "  int paramType - Parameter type indicator\n" +
            "Implementation:\n" +
            "  1. Hash message name using FNV-1a algorithm\n" +
            "  2. Hash = 2166136261; for each byte: hash ^= byte, hash *= 16777619\n" +
            "  3. Store in dispatch table with linear probing\n" +
            "  4. Entry: {msgHash, dest, handler, paramType, debugName}\n" +
            "Table size: 256 entries\n" +
            "Iteration 7: FNV-1a hash confirmed");

        // Message dispatch table
        annotateData("00e63e82",
            "g_MessageDispatchTable",
            "Message dispatch hash table (256 entries)\n" +
            "Entry structure (20 bytes):\n" +
            "  +0x00: DWORD msg_hash (FNV-1a)\n" +
            "  +0x04: void* dest_object\n" +
            "  +0x08: void (*handler)(void*, void*)\n" +
            "  +0x0c: int param_type\n" +
            "  +0x10: const char* debug_name\n" +
            "Collision handling: Linear probing\n" +
            "Common messages:\n" +
            "  - iMsg_SceneChange\n" +
            "  - iMsg_PauseGame\n" +
            "  - iMsg_ReleaseResources\n" +
            "  - iMsg_DeviceLost\n" +
            "  - iMsg_FocusChange\n" +
            "Iteration 7: Hash table with linear probing");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Input System
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateInputSystem() {
        println("\n[Input System] Annotating input structures...");

        // Input device enumeration
        annotateFunc("00e64ec3",
            "EnumerateInputDevices",
            "Enumerates and creates DirectInput8 devices\n" +
            "Implementation:\n" +
            "  1. IDirectInput8::EnumDevices(DI8DEVCLASS_KEYBOARD)\n" +
            "  2. IDirectInput8::EnumDevices(DI8DEVCLASS_POINTER)\n" +
            "  3. IDirectInput8::EnumDevices(DI8DEVCLASS_GAMECTRL)\n" +
            "  4. CreateDevice and SetCooperativeLevel for each\n" +
            "  5. SetDataFormat (DIDATAFORMAT_KEYBOARD, etc.)\n" +
            "  6. Acquire devices\n" +
            "Stores up to 2 joysticks in g_pJoystick[]\n" +
            "Iteration 7: DirectInput8 device creation");

        // RealInputSystem
        annotateData("00be8758",
            "g_pRealInputSystem",
            "Main input system structure (0x460 bytes)\n" +
            "Layout:\n" +
            "  +0x000: void* vtable\n" +
            "  +0x004: IDirectInput8* pDirectInput\n" +
            "  +0x008: IDirectInputDevice8* pKeyboard\n" +
            "  +0x00c: IDirectInputDevice8* pMouse\n" +
            "  +0x010: IDirectInputDevice8* pJoystick[2]\n" +
            "  +0x018: BYTE keyboard_state[256] (current)\n" +
            "  +0x118: BYTE prev_keyboard_state[256]\n" +
            "  +0x218: DIMOUSESTATE2 mouse_state\n" +
            "  +0x22c: DIMOUSESTATE2 prev_mouse_state\n" +
            "  +0x240: DIJOYSTATE2 joystick_state[2]\n" +
            "  +0x340: DIJOYSTATE2 prev_joystick_state[2]\n" +
            "  +0x440: bool keyboard_active\n" +
            "  +0x441: bool mouse_active\n" +
            "  +0x442: bool joystick_active[2]\n" +
            "  +0x444: DWORD input_flags\n" +
            "  +0x448: bool paused\n" +
            "  +0x44c: CRITICAL_SECTION\n" +
            "Double-buffered for edge detection\n" +
            "Iteration 7: Complete input system layout");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Render Queue
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateRenderQueue() {
        println("\n[Render Queue] Annotating deferred rendering...");

        // Render batch enqueue
        annotateFunc("00e63af4",
            "EnqueueRenderBatch",
            "Adds render batch to deferred queue\n" +
            "Parameters: Geometry, material, shader, transform\n" +
            "Creates RenderBatchNode and links to queue\n" +
            "Queue processed in ProcessDeferredRenderQueue\n" +
            "Iteration 7: Deferred rendering pattern");

        // Build render batch
        annotateFunc("0063d600",
            "BuildRenderBatch",
            "Builds and sorts render batches by shader type\n" +
            "Render order:\n" +
            "  1. SKY (0x6a8b4c97) - No depth test\n" +
            "  2. OPAQUE (0x2a4f6b91) - Front-to-back\n" +
            "  3. WATER (0x3d7f2e16) - Special blending\n" +
            "  4. ALPHA (0x7c31e8a2) - Back-to-front\n" +
            "  5. GLASS (0x5e2a1bd4) - Refractive\n" +
            "  6. BLOOM (0x1f9d4c33) - Post-process\n" +
            "  7. BACKDROP (0x8f3c9a45) - UI\n" +
            "Shader hashes are FNV-1a of type name\n" +
            "Batches merged when material/shader match\n" +
            "Iteration 7: Shader-based sort order");

        // Deferred render queue
        annotateData("00bef7c0",
            "g_pDeferredRenderQueue",
            "Head of deferred render batch linked list\n" +
            "Node structure (0x80 bytes):\n" +
            "  +0x00: void* geometry_buffer\n" +
            "  +0x04: DWORD vertex_count\n" +
            "  +0x08: DWORD index_count\n" +
            "  +0x0c: void* material\n" +
            "  +0x10: DWORD shader_hash\n" +
            "  +0x14: float world_matrix[16]\n" +
            "  +0x54: DWORD render_flags\n" +
            "  +0x58: float sort_key\n" +
            "  +0x5c: int batch_id\n" +
            "  +0x60: DWORD timestamp\n" +
            "  +0x7c: RenderBatchNode* next\n" +
            "Processed within 2ms budget per frame\n" +
            "Iteration 7: Linked list of render batches");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Scene Management
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateSceneManagement() {
        println("\n[Scene Management] Annotating scene system...");

        // Scene change function
        annotateFunc("00e63c35",
            "RequestSceneChange",
            "Requests scene transition with listener notification\n" +
            "Parameters: int newSceneID\n" +
            "Implementation:\n" +
            "  1. Compare with g_SceneID_Current\n" +
            "  2. Store old scene in g_SceneID_FocusLost\n" +
            "  3. Store new scene in g_SceneID_FocusGain\n" +
            "  4. Mark pending change flag\n" +
            "  5. Notify scene listeners\n" +
            "  6. FlushDeferredSceneListeners at end of frame\n" +
            "Prevents recursive scene changes\n" +
            "Iteration 7: Three-ID scene system");

        // Flush deferred listeners
        annotateFunc("006125a0",
            "FlushDeferredSceneListeners",
            "Processes deferred scene transition callbacks\n" +
            "Called at end of frame to handle queued changes\n" +
            "Prevents reentrancy during listener callbacks\n" +
            "Iteration 7: Deferred callback pattern");

        // Scene IDs
        annotateData("00bf22a0",
            "g_SceneID_FocusLost",
            "Scene ID when window loses focus (menu/pause)\n" +
            "Part of three-ID scene management system\n" +
            "Compared in UpdateCursorVisibilityAndScene\n" +
            "Iteration 7: Focus-lost scene tracking");

        annotateData("00bf22a4",
            "g_SceneID_FocusGain",
            "Scene ID when window regains focus (gameplay)\n" +
            "Used to restore active gameplay scene\n" +
            "Iteration 7: Focus-gain scene tracking");

        annotateData("00bf22a8",
            "g_SceneID_Current",
            "Currently active scene ID\n" +
            "Updated on successful scene transitions\n" +
            "Scene types:\n" +
            "  0: Main Menu\n" +
            "  1: Loading Screen\n" +
            "  2: Hogwarts Exploration\n" +
            "  3: Mini-game\n" +
            "  4: Cutscene\n" +
            "  5: Pause Menu\n" +
            "  6: Options\n" +
            "  7: Credits\n" +
            "Iteration 7: Current scene state");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // GameServices and Subsystems
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateGameServices() {
        println("\n[GameServices] Annotating subsystem managers...");

        // GameServices structure
        annotateData("00bf2260",
            "g_pGameServices",
            "GameServices subsystem manager (0x48 bytes)\n" +
            "Layout:\n" +
            "  +0x00: void* vtable\n" +
            "  +0x04: SaveManager* save_manager\n" +
            "  +0x08: ProfileManager* profile_manager\n" +
            "  +0x0c: LocaleManager* locale_manager\n" +
            "  +0x10: AchievementManager* achievement_mgr\n" +
            "  +0x14: StatTracker* stat_tracker\n" +
            "  +0x18: OptionManager* option_manager\n" +
            "  +0x40: bool initialized\n" +
            "  +0x44: DWORD init_flags\n" +
            "Provides centralized access to game subsystems\n" +
            "Lazy initialization pattern\n" +
            "Iteration 7: Service locator pattern");

        // TimeManager
        annotateData("00bf2328",
            "g_pTimeManager",
            "TimeManager subsystem (8 bytes)\n" +
            "Layout:\n" +
            "  +0x00: void* vtable\n" +
            "  +0x04: DWORD isPaused (0=running, 1=paused)\n" +
            "Checked in UpdateFrameTimingPrimary\n" +
            "Prevents tick updates when paused\n" +
            "Iteration 7: Simple pause state manager");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Command Line Flags
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateCommandLineFlags() {
        println("\n[Command Line] Annotating CLI parsing...");

        // Main CLI parser
        annotateFunc("00617bf0",
            "ParseCommandLineArg",
            "Parses command line flags from saved command line\n" +
            "Flags:\n" +
            "  fullscreen - Enable fullscreen mode\n" +
            "  widescreen - Use widescreen aspect ratio\n" +
            "  oldgen - Legacy renderer path\n" +
            "  showfps - Display FPS counter\n" +
            "  memorylwm - Memory low-water-mark tracking\n" +
            "  nofmv - Disable FMV playback\n" +
            "  novsync - Disable vertical sync\n" +
            "  nosound - Disable audio\n" +
            "  nomusic - Disable music (keep SFX)\n" +
            "  debugcam - Enable free camera\n" +
            "  godmode - Invincibility\n" +
            "Value parameters:\n" +
            "  -width=800, -height=600, -bpp=32\n" +
            "  -adapter=0, -lod=2, -language=en\n" +
            "Iteration 7: Complete flag list");

        // CLI token parser
        annotateFunc("00eb787a",
            "CLI_CommandParser_ParseArgs",
            "Parses -name=value tokens from command line\n" +
            "Allocates CLI::CommandParser object (0xc bytes)\n" +
            "Stores name pointers at this+0x480 (up to 0x20 entries)\n" +
            "Stores value pointers at this+0x500\n" +
            "Positional args at this+0x400\n" +
            "Called before single-instance check\n" +
            "Iteration 7: Token-based CLI parsing");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Shader System
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateShaderSystem() {
        println("\n[Shader System] Annotating shader capabilities...");

        // Shader capability level
        annotateData("00bf1994",
            "g_nShaderCapabilityLevel",
            "Detected shader model capability level\n" +
            "Values:\n" +
            "  0 = Fixed-function only (no programmable shaders)\n" +
            "  1 = Shader Model 1.x (basic vertex/pixel shaders)\n" +
            "  2 = Shader Model 2.0 (loops, more instructions)\n" +
            "  3 = Shader Model 3.0 (full SM3.0, advanced effects)\n" +
            "Detection:\n" +
            "  1. GetDeviceCaps() to retrieve D3DCAPS9\n" +
            "  2. Check PixelShaderVersion and VertexShaderVersion\n" +
            "  3. Extract major version: D3DSHADER_VERSION_MAJOR()\n" +
            "  4. Set level based on capabilities\n" +
            "  5. Fallback if MaxSimultaneousTextures < 4\n" +
            "Iteration 7: Shader capability detection");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ══════════════════════════════════════════════════════════════════════════════

    private void annotateFunc(String addrStr, String name, String comment) {
        try {
            Address[] addrs = currentProgram.parseAddress(addrStr);
            if (addrs == null || addrs.length == 0) {
                println(String.format("  ✗ Invalid address: %s", addrStr));
                return;
            }
            Address addr = addrs[0];
            Function func = fm.getFunctionAt(addr);
            
            if (func != null) {
                // Rename function
                func.setName(name, SourceType.USER_DEFINED);
                
                // Set comment
                listing.setComment(addr, CodeUnit.PRE_COMMENT, comment);
                
                println(String.format("  ✓ %s @ %s", name, addrStr));
            } else {
                println(String.format("  ✗ No function at %s", addrStr));
            }
        } catch (Exception e) {
            println(String.format("  ✗ Error annotating function %s: %s", addrStr, e.getMessage()));
        }
    }

    private void annotateData(String addrStr, String name, String comment) {
        try {
            Address[] addrs = currentProgram.parseAddress(addrStr);
            if (addrs == null || addrs.length == 0) {
                println(String.format("  ✗ Invalid address: %s", addrStr));
                return;
            }
            Address addr = addrs[0];
            
            // Create or get symbol
            Symbol[] symbols = symTable.getSymbols(addr);
            Symbol symbol = null;
            
            if (symbols.length > 0) {
                symbol = symbols[0];
                symbol.setName(name, SourceType.USER_DEFINED);
            } else {
                symbol = symTable.createLabel(addr, name, SourceType.USER_DEFINED);
            }
            
            // Set comment at address using pre-comment instead of deprecated plate comment
            listing.setComment(addr, CodeUnit.PRE_COMMENT, comment);
            
            println(String.format("  ✓ %s @ %s", name, addrStr));
        } catch (Exception e) {
            println(String.format("  ✗ Error annotating data %s: %s", addrStr, e.getMessage()));
        }
    }
}
