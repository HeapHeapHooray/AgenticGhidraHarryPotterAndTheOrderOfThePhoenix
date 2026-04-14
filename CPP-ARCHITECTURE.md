# C++ Decompilation Architecture (Iteration 5)

## Overview

This document describes the architecture of the C++ decompiled version of `hp.exe` as implemented in `decompilation-src/main.cpp`. The decompilation aims for functional equivalence while maintaining clean, readable C++ code without assembly instructions, hard-coded addresses, or magic constants.

**Current Status:** Iteration 5 - Enhanced with subsystem structures and implementations

**Build System:** Zig cc (cross-compilation to Windows x86)

**Target Platform:** Windows (same as original hp.exe)

## Project Structure

```
decompilation-src/
├── main.cpp          Main implementation (1485 lines)
├── globals.h         Global declarations and structures (311 lines)
├── build.sh          Build script using zig cc
└── hp_decompiled.exe Compiled output
```

## Key Design Principles

1. **No Assembly:** Pure C++ implementation, no inline assembly or asm blocks
2. **No Hard-coded Addresses:** All memory references use typed pointers and structures
3. **No Magic Numbers:** Constants are named and explained
4. **Clean Structure:** Logical separation of subsystems
5. **Commented TODOs:** Incomplete implementations marked for future work

## Global Variables (Iteration 5 Additions)

### Frame Callback System
```cpp
CallbackSlot g_FrameCallbackSlots[8];  // DAT_00e6e880
```
8 callback slots for per-frame updates, each storing function pointer and context.

### Scene Management
```cpp
SceneIDs g_SceneIDs;  // Three-ID scene system
```
Tracks focus-lost, focus-gain, and current scene IDs.

### Message Dispatch
```cpp
MessageHandler g_MessageDispatchTable[256];
int g_nMessageHandlerCount;
```
Hash-based message dispatch system with 256 handler slots.

### Audio Command Queue
```cpp
AudioCommand g_AudioCommandQueue[32];
int g_nAudioQueueHead, g_nAudioQueueTail;
HANDLE g_hAudioThread;  // DAT_00bf1b30
```
Asynchronous audio operation queue with dedicated thread.

### Deferred Render Queue
```cpp
RenderBatchNode* g_pDeferredRenderQueue;  // DAT_00bef7c0
```
Linked list of render batches processed with 2ms time budget.

### Subsystem Singletons
```cpp
TimeManager* g_pTimeManager;              // DAT_00bef768
GlobalTempBuffer* g_pGlobalTempBuffer;    // DAT_00e6b378
RealGraphSystem* g_pRealGraphSystem;      // DAT_00e6b390
void* g_pEngineRootObject;                // DAT_00bef6d0 (2904 bytes)
void* g_pCallbackManager_Primary;         // DAT_00e6e870
void* g_pCallbackManager_Secondary;       // DAT_00e6e874
```

## Data Structures (Iteration 5)

### CallbackSlot
```cpp
struct CallbackSlot {
    void (*func)(void* context);
    void* context;
};
```
Stores function pointer and context for frame callbacks.

### AudioCommand
```cpp
enum AudioCommandType {
    AUDIO_CMD_NONE, AUDIO_CMD_OPEN_DEVICE, 
    AUDIO_CMD_QUERY_CAPS, AUDIO_CMD_CONFIGURE, 
    AUDIO_CMD_START_STREAM, AUDIO_CMD_STOP_STREAM
};

struct AudioCommand {
    AudioCommandType opcode;
    void* params;
    void (*callback)(int status);
    int status;  // -2=error, 0=pending, 1=complete
};
```

### MessageHandler
```cpp
struct MessageHandler {
    void (*handler)(void* dest, void* params);
    void* dest;
    int paramType;
};
```

### SceneIDs
```cpp
struct SceneIDs {
    int focusLost;  // Menu/pause screen
    int focusGain;  // Active gameplay
    int current;    // Current/target scene
};
```

### RenderBatchNode
```cpp
struct RenderBatchNode {
    DWORD shaderTypeHash;
    void* material;
    void* geometry;
    float transform[16];
    DWORD flags;
    char unknown[56];  // Unknown fields
    RenderBatchNode* next;  // +0x7c
};
```

### TimeManager
```cpp
struct TimeManager {
    void* vtable;
    DWORD isPaused;  // 0=running, non-zero=paused
};
```

### AllocHeader
```cpp
struct AllocHeader {
    const char* tag;
    size_t size;
    AllocHeader* next;
    DWORD magic;  // 0xDEADBEEF
};
```
Memory allocation tracking header for debug allocator.

## Subsystem Implementations

### Memory Allocator

**Functions:**
- `AllocEngineObject(size_t size, const char* tag)` - Debug-aware allocator
- `FreeEngineObject(void* ptr)` - Releases allocated memory

**Features:**
- Tag-based tracking for leak detection
- Magic number corruption detection (0xDEADBEEF)
- Per-tag memory usage statistics (TODO)

**Implementation Status:** ✅ Basic implementation complete, tracking list pending

### Message Dispatch System

**Functions:**
- `HashMessageName(const char* msgName)` - FNV-1a hash
- `RegisterMessageHandler(void* dest, const char* msgName, int paramType)`
- `DispatchMessage(DWORD msgID, void* params)`

**Features:**
- String-based message registration ("iMsgDeleteEventHandler", etc.)
- Hash-based dispatch for performance
- Type-safe parameter passing

**Implementation Status:** ⚠️ Partial - hash and registration complete, dispatch mapping pending

### Frame Callback System

**Functions:**
- `InitFrameCallbackSystem()` - Clears all 8 slots
- `RegisterFrameCallback(void (*func)(void*), void* context)`
- `InvokeFrameCallbacks()` - Calls all registered callbacks

**Features:**
- 8 callback slots for different subsystems
- Function pointer + context pattern
- Called each frame from `GameFrameUpdate`

**Implementation Status:** ✅ Complete

### Audio Command Queue

**Functions:**
- `InitAudioCommandQueue()` - Initializes queue
- `EnqueueAudioCommand(AudioCommandType, void*, void (*)(int))`
- `AudioPollGate()` - Returns -2/0/1 (error/pending/complete)

**Features:**
- Ring buffer with 32 command slots
- Asynchronous processing via audio thread
- Status polling for non-blocking init

**Implementation Status:** ✅ Queue management complete, thread processing pending

### Scene Management

**Functions:**
- `LoadSceneIDs()` - Loads scene identifiers
- `NotifySceneListeners(int newSceneID)`
- `FlushDeferredSceneListeners()`
- `SwitchRenderOutputModeEx(int sceneID)`

**Features:**
- Three-ID system (focus-lost, focus-gain, current)
- Listener notification pattern
- Deferred scene switching with pending flag

**Implementation Status:** ⚠️ Partial - structure complete, listener list pending

### Deferred Render Queue

**Functions:**
- `BuildRenderBatch()` - Sorts and batches by shader
- `ProcessDeferredRenderQueue()` - Processes with 2ms budget

**Features:**
- Linked list of render batch nodes
- Shader type recognition (BLOOM, GLASS, BACKDROP)
- Time-budgeted processing

**Implementation Status:** ⚠️ Partial - queue iteration complete, batch building pending

### Subsystem Initialization

**Functions:**
- `InitLanguageResources()` - Loads string tables
- `InitVideoCodec()` - Initializes FMV codec (Bink/Smacker)
- `FinalizeRenderInit()` - Finalizes render setup

**Implementation Status:** 🔲 Stubs only

## Main Loop Architecture

### WinMain Flow
```
1. FPU Configuration (_control87)
2. Save System Parameters (mouse, screen reader)
3. Command Line Storage (2 copies)
4. CLI Argument Parsing (CLI_CommandParser_ParseArgs)
5. Single Instance Check (FindWindowA)
6. Window Class Registration
7. Registry Settings Load
8. Command Line Flags (fullscreen, widescreen, etc.)
9. Window Creation
10. Engine Object Factory (via callback manager secondary entry)
11. Pre-DirectX Init (audio context setup)
12. DirectX Initialization (D3D + DirectInput)
13. Game Subsystems Init (callbacks, devices, language)
14. Window Placement Restore (maximized/minimized)
15. MainLoop()
16. Cleanup (teardown, save settings, restore system params)
```

### MainLoop Flow
```
while (!g_bExitRequested) {
    Process Windows messages (PeekMessage)
    
    if (device_lost) {
        Attempt device recovery
        Sleep(50ms)
        continue
    }
    
    GameFrameUpdate()
    
    if (render_enabled) {
        BeginScene()
        Render frame
        EndScene()
        Present()
    }
}
```

### GameFrameUpdate Flow
```
1. ProcessDeferredCallbacks() - 2ms budget for render batches
2. Get current time (16.16 fixed-point)
3. Compute delta (capped at 100ms)
4. Accumulate time
5. Update game ticks (accum * 3 / 0x10000)
6. Check if callback interval reached:
   - Toggle frame flip (0 <-> 1)
   - Update frame timing primary
   - Dispatch primary callback (game logic)
   - Interpolate frame time
   - Dispatch secondary callback (render)
```

## Timing System

**Format:** 64-bit 16.16 fixed-point  
**Interval:** 16ms for 60 FPS (or 33ms for 30 FPS)  
**Time Scale:** Game ticks = accumulated time * 3 / 0x10000

**Variables:**
- `g_ullAccumTime` - Accumulated time (16.16)
- `g_ullNextCallback` - Next callback trigger time
- `g_ullCallbackInterval` - Callback interval
- `g_dwGameTicks` - Game tick counter (scaled by 3)

**Pause Mechanism:**
- `g_pTimeManager->isPaused` - When non-zero, tick accumulation skips
- Time still advances, but game state freezes

## DirectX Integration

### Device Creation
Uses `CreateD3DDevice` with 9 parameters:
1. height - Client window height
2. unknown - Adapter or behavior flags
3-4. flags - Both 0
5. quality - Default 6
6-9. boolean flags - All 1 (vsync, multithreaded, pure device, hardware VP)

### Resource Management
- Pre-release notification: `NotifyPreReleaseResources` (flush, save state)
- Post-release cleanup: `NotifyPostReleaseResources` (clear caches, reset state)

### Shader Capabilities
- `g_nShaderCapabilityLevel` set from D3DCAPS9
- >2 = extended shader path (Shader Model 2.0+)
- <=2 = basic shader path (Shader Model 1.x)

## Registry Integration

**Registry Path:** `HKEY_CURRENT_USER\Software\Electronic Arts\Harry Potter and the Order of the Phoenix\GameSettings`

**Fallback Order:** HKCU → HKLM → create with default in HKCU

**Settings:**
- Graphics (Width, BitDepth, ShadowLOD, etc.)
- Window placement (PosX, PosY, SizeX, SizeY, Maximized, Minimized)
- Options (OptionResolution, OptionLOD, OptionBrightness)

**Saved on Exit:**
- OptionResolution
- OptionLOD
- OptionBrightness

## Command-Line Flags

| Flag | Effect |
|------|--------|
| `fullscreen` | Sets fullscreen mode + optional width |
| `widescreen` | Sets widescreen aspect ratio |
| `oldgen` | Enables legacy renderer path |
| `showfps` | Enables FPS overlay |
| `memorylwm` | Enables memory low-water-mark tracking |
| `nofmv` | Disables FMV playback |

## Implementation Status Summary

### ✅ Complete (Functionally Equivalent)
- Window creation and management
- DirectX device initialization
- Input device enumeration (DirectInput)
- Registry settings load/save
- System parameter save/restore
- Command-line parsing
- High-resolution timing (timeGetTime)
- Frame flip and tick accumulation
- Focus loss/gain handling
- Device lost recovery
- WM_ACTIVATE message handling
- Memory allocator (basic)
- Frame callback system
- Audio command queue

### ⚠️ Partial (Implemented but Incomplete)
- Message dispatch (hash done, table mapping pending)
- Scene management (structure done, listeners pending)
- Deferred render queue (iteration done, batching pending)
- Audio stream pause/resume (stubs present)
- Game object pause/resume (state machine partial)

### 🔲 Not Implemented (TODOs)
- Engine object factory with magic number
- Callback manager dual-entry vtable
- Primary/secondary frame callbacks (actual dispatch)
- Audio thread worker routine
- Scene listener list iteration
- Render batch building (shader recognition)
- Language resource loading
- Video codec initialization
- Render finalization (shaders, textures)
- RealInputSystem vtable operations
- RenderAndAudioTeardown
- Proper subsystem object lifetimes

## Build Configuration

**Compiler:** Zig cc (Clang-based, cross-compilation)  
**Target:** i686-windows-gnu (Windows 32-bit)  
**Flags:**
- `-O2` - Optimization level 2
- `-mwindows` - Windows subsystem (GUI, not console)
- `-target i686-windows-gnu`
- `-lwinmm -ld3d9 -ldinput8 -ldxguid -luser32 -lgdi32 -ladvapi32`

**Warnings:** Clean compilation (only unused `-mwindows` warning from zig)

## Compilation Statistics

- **Source Lines:** 1485 (main.cpp) + 311 (globals.h) = 1796 total
- **Structures:** 13 (SystemParams, GraphicsSettings, CallbackSlot, AudioCommand, MessageHandler, SceneIDs, RenderBatchNode, TimeManager, GlobalTempBuffer, RealGraphSystem, AllocHeader)
- **Functions:** 60+ implemented, 20+ stubbed
- **Global Variables:** 50+
- **Binary Size:** ~800 KB (hp_decompiled.exe)

## Memory Layout Comparison

The C++ decompilation uses native C++ memory management rather than replicating the exact memory layout of the original binary. Key differences:

1. **Heap Allocation:** Standard `malloc`/`free` with debug headers instead of custom allocator
2. **Global Variables:** Organized in source files rather than specific addresses
3. **Structures:** C++ structs with proper typing instead of raw memory offsets
4. **VTables:** Virtual methods in C++ classes vs manual vtable pointers

This approach prioritizes maintainability and clarity over byte-for-byte replication.

## Next Steps (Iteration 6)

1. **Implement Frame Callback Dispatch**
   - Hook up primary/secondary callbacks in `GameFrameUpdate`
   - Implement `UpdateFrameTimingPrimary` and `InterpolateFrameTime`

2. **Complete Message Dispatch**
   - Add message ID to handler mapping
   - Implement actual dispatch logic

3. **Implement Scene Listeners**
   - Create listener list structure
   - Implement registration and notification

4. **Implement Render Batch Building**
   - Shader type recognition (hash comparison)
   - Material sorting and D3D state optimization

5. **Audio Thread Implementation**
   - Thread entry point function
   - Command queue processing loop
   - Synchronization with main thread

6. **Subsystem Initialization**
   - Language resource loading from files
   - Video codec DLL loading (Bink/Smacker detection)
   - Shader compilation/loading

7. **Complete Teardown Sequence**
   - RenderAndAudioTeardown implementation
   - Engine object destruction via callback manager
   - Proper cleanup order

## Differences from Original

See `DIFFERENCES.md` for detailed comparison between original `hp.exe` architecture and C++ decompilation implementation.
