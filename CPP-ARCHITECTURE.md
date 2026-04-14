# C++ Decompilation Architecture (Iteration 8)

## Overview

This document describes the architecture of the C++ decompiled version of `hp.exe` as implemented in `decompilation-src/`. The decompilation aims for functional equivalence while maintaining clean, readable C++ code without assembly instructions, hard-coded addresses, or magic constants.

**Current Status:** Iteration 8 - High-level game systems (spells, physics, streaming, assets, AI, UI) added

**Build System:** Zig cc 0.16.0-dev (cross-compilation to Windows x86)

**Target Platform:** Windows x86 (32-bit, same as original hp.exe)

**Compilation Status:** ✅ Builds successfully (832 KB executable with PDB)

## Project Structure

```
decompilation-src/
├── main.cpp            Main implementation (1971 lines)
├── globals.h           Global declarations and structures (809 lines)
├── build.sh            Build script using zig cc
└── ../hp_decompiled.exe Compiled output (832 KB)
```

## Key Design Principles

1. **No Assembly:** Pure C++ implementation, no inline assembly or asm blocks
2. **No Hard-coded Addresses:** All memory references use typed pointers and structures
3. **No Magic Numbers:** Constants are named and explained with comments
4. **Clean Structure:** Logical separation of subsystems
5. **Commented TODOs:** Incomplete implementations marked clearly for future work
6. **Accurate Structures:** Data structures match original binary layouts where known

## Architecture Layers

### Layer 1: Platform & API Integration
- Windows API (window management, registry, message loop)
- DirectX 9 (D3D device, Present, Reset, Lost device handling)
- DirectInput 8 (keyboard, mouse, joystick enumeration and polling)
- DirectSound (audio device initialization, thread-based playback)

### Layer 2: Core Engine
- **Timing:** 16.16 fixed-point, 60 FPS target, 3x game speed scaling
- **Memory:** Custom allocator with tagged allocations and free lists
- **Callbacks:** 8-slot frame callback system with primary/secondary timing
- **Message Dispatch:** FNV-1a hash-based message routing

### Layer 3: Subsystems
- **Scene Management:** Three-ID system (old, current, new) with listener notifications
- **Audio:** Command queue (ring buffer), async thread worker
- **Input:** Double-buffered state with edge detection
- **Render:** Deferred batch queue with 2ms time budget
- **TimeManager:** Pause state management with 4 states

### Layer 4: High-Level Game Systems (Iteration 8)
- **Spell Casting:** State machine, gesture recognition (stub), wand discipline
- **Physics:** Havok 3.x/4.x stub interfaces (rigid bodies, collision, raycasting)
- **Zone Streaming:** 4 load zones + 3 unload zones, hybrid streaming
- **Asset Loading:** GOF framework with type-specific handlers (RCB, HKX, Babble, Hull, Spline)
- **Animation:** Skeletal skinning (GPU), blend shapes with specular tint
- **AI:** State-based (idle variants, patrol, wander, chase, attack, sidle)
- **UI:** Screen-based, 32-button mapping, message queue

## Build Configuration

**Compiler:** Zig 0.16.0-dev C++ mode  
**Target:** x86-windows-gnu (32-bit)  
**Libraries Linked:**
- c++ (C++ standard library)
- user32 (Windows user interface)
- gdi32 (Graphics Device Interface)
- ole32 (COM/OLE)
- uuid (GUID support)
- winmm (Multimedia timers)
- dsound (DirectSound)

**Build Command:**
```bash
cd decompilation-src && ./build.sh
```

## Data Structures Overview

### Core Engine Structures

**CallbackSlot (8 slots):**
```cpp
struct CallbackSlot {
    void (*pfnCallback)(void*);
    void* pContext;
};
```

**MessageHandler (hash table):**
```cpp
struct MessageHandler {
    void (*pfnHandler)(void*);
    void* pContext;
    unsigned int messageHash;  // FNV-1a
};
```

**AudioCommand (queue of 256):**
```cpp
struct AudioCommand {
    AudioCommandType opcode;
    void* params;
    void (*callback)(int status);
    int status;  // -2=error, 0=pending, 1=complete
};
```

**SceneIDs:**
```cpp
struct SceneIDs {
    int oldSceneID;
    int currentSceneID;
    int newSceneID;
};
```

**TimeManager:**
```cpp
struct TimeManager {
    GamePauseState pauseState;  // UNPAUSED, PAUSE_REQUESTED, PAUSED, UNPAUSE_REQUESTED
    bool isPaused;
};
```

### Iteration 8 Structures

**SpellSystemState:**
```cpp
struct SpellSystemState {
    SpellCastState currentState;      // State machine
    SpellType lastCastSpell;
    int successfulCastsCount;          // Achievement tracking
    WandDisciplineState wandState;     // Safe/restricted zones
    bool gestureInProgress;
    float gestureStartTime;
};
```

**hkPhysicsSystem (Havok stub):**
```cpp
struct hkPhysicsSystem {
    void* vtable;
    void* worldData;
    int rigidBodyCount;
    void* rigidBodies;       // Array of hkRigidBody*
    void* collisionFilter;   // Group-based filtering
    int versionMajor;        // 3 or 4
    int versionMinor;
};
```

**LoadZone:**
```cpp
struct LoadZone {
    char zoneName[64];
    ZoneLoadState state;     // UNLOADED, LOADING, LOADED, UNLOADING
    void* pAssetList;
    size_t memoryUsed;
    int priority;            // 0-3
};
```

**StreamingManager:**
```cpp
struct StreamingManager {
    LoadZone loadZones[4];    // 4 concurrent
    LoadZone unloadZones[3];  // 3 concurrent
    bool loadingScreenActive;
    bool hybridStreamingActive;
    float playerPosX, playerPosY, playerPosZ;
};
```

**AssetManager:**
```cpp
struct AssetManager {
    AssetEntry* pAssetTable;        // Hash table
    int assetCount;
    int maxAssets;
    ResourceHandler* handlers[16];  // Per resource type
};
```

**AssetEntry:**
```cpp
struct AssetEntry {
    char filename[MAX_PATH];
    ResourceType type;        // MODEL, TEXTURE, ANIMATION_RCB, PHYSICS_HKX, etc.
    void* pData;
    size_t headerSize;        // Pre-allocated sizes
    size_t bodySize;
    bool isLoaded;
};
```

**SkeletalMesh:**
```cpp
struct SkeletalMesh {
    int numBones;
    int numBonesPerVertex;    // 1-4
    unsigned int boneIndexOffset;
    unsigned int boneWeightOffset;
    void* pBoneMatrices;      // Matrix palette for GPU
    BlendShape* pBlendShapes;
    int blendShapeCount;
};
```

**BlendShape:**
```cpp
struct BlendShape {
    char name[64];
    float weight;             // 0.0-1.0
    float specularTint;       // Lighting modulation
    void* pMorphTargetData;   // Vertex deltas
};
```

**AIAgent:**
```cpp
struct AIAgent {
    AIState currentState;     // IDLE, PATROL, WANDER, CHASE, ATTACK, SIDLE, FLEE
    AIState previousState;
    bool canSidle;            // Capability flag
    int patrolLocatorCount;
    int currentPatrolLocator;
    AILocator* pPatrolLocators;
    float stateTimer;
};
```

**UISystem:**
```cpp
struct UISystem {
    UIScreen currentScreen;   // SPLASH, MAIN_MENU_2_0, START_MENU, LOAD_GAME_4_0, etc.
    UIScreen previousScreen;
    UIButton buttons[32];     // 32-button support
    int messageQueueHead;
    int messageQueueTail;
    UIMessage messageQueue[64];
};
```

## Initialization Flow

### WinMain Entry
1. FPU configuration: `_control87(0x20000, 0x30000)` (disable denormal exceptions)
2. Save system parameters (mouse speed, acceleration, screen reader)
3. Command line parsing and storage (2 copies)
4. Single instance check via `FindWindowA("OrderOfThePhoenixMainWndClass", NULL)`
5. Window class registration
6. Registry settings load (graphics, window placement)
7. Command line argument parsing (fullscreen, widescreen, etc.)
8. Window creation and client rect measurement

### Engine Initialization
1. **Engine Object Factory:** Create 2904-byte engine root object with magic `{0x88332000000001, 0}`
2. **Pre-DirectX Init:** Audio subsystem setup, device string copy
3. **DirectX Init:** D3D9 device, DirectInput8, DirectSound creation
4. **Subsystem Init:** Callbacks, message dispatch, scene management, audio thread, input polling

### Iteration 8 Systems Init
1. **Spell System:** State machine init, spell cast counter = 0, wand = holstered
2. **Physics (stub):** Havok interface setup (no actual physics without SDK)
3. **Streaming:** Initialize 4 load zones and 3 unload zones
4. **Asset Manager:** Register resource handlers (RCB, HKX, Babble, Hull, Spline, etc.)
5. **AI:** Initialize agent state machines
6. **UI:** Set initial screen (SPLASH or MAIN_MENU)

### Main Loop Entry
1. Window placement restore (maximized/minimized/normal)
2. `UpdateWindow(hWnd)`
3. `MainLoop()` - message pump and frame updates

## Main Loop Architecture

```
MainLoop() {
    while (!g_bExitRequested) {
        // Windows messages
        while (PeekMessage(&msg, NULL, 0, 0, PM_REMOVE)) {
            TranslateMessage(&msg);
            DispatchMessage(&msg);
        }
        
        // Frame update
        if (!g_bDeviceLost && g_bGameUpdateEnabled) {
            UpdateFrame();
        }
        
        // Render
        if (CanRender()) {
            BeginScene() → RenderScene() → EndScene() → Present();
        }
        
        // Device lost handling
        if (g_bDeviceLost) {
            Sleep(50ms);
            AttemptDeviceReset();
        }
    }
}
```

## Frame Update Flow

```
UpdateFrame() {
    1. Get deltaTime from timeGetTime()
    2. Cap deltaTime to MAX_DELTA_TIME_MS (100ms, spiral-of-death prevention)
    3. Accumulate time in 16.16 fixed-point: g_ullAccumTime += (deltaTime << 16)
    4. Check if callback interval reached (16ms = 60 FPS)
    5. If reached:
       a. UpdateFrameTimingPrimary() - primary timing callback
       b. Update g_dwGameTicks = (accumTime * 3) / 0x10000  (3x speed scaling)
       c. Invoke 8 frame callback slots
       d. InterpolateFrameTime() - secondary timing callback
       e. ProcessDeferredRenderQueue(2ms budget)
       f. Advance g_ullNextCallback += g_ullCallbackInterval
}
```

## Timing System Details

**16.16 Fixed-Point:**
- Upper 16 bits = whole seconds
- Lower 16 bits = fractional seconds
- Divide by 0x10000 (65536) to get floating-point seconds

**Game Time Scaling:**
- Real time is multiplied by 3 for game logic
- Physics/AI run at 3x speed
- Rendering interpolates for smooth 60 FPS display

**Callback Interval:**
- Set to 16ms (1,048,576 in 16.16 format)
- Equivalent to 60 FPS
- Primary callback: update game state
- Secondary callback: interpolate for rendering

## Subsystem Details

### Audio System
- **Queue:** 256-entry ring buffer
- **Thread:** Asynchronous worker (`AudioThreadProc`)
- **Commands:** PlaySound, StopSound, SetVolume, SetPitch
- **DirectSound:** Buffer creation, 3D positioning, streaming

### Input System
- **Double Buffering:** Current frame and previous frame states
- **Edge Detection:** IsKeyPressed (curr && !prev), IsKeyReleased (!curr && prev)
- **Devices:** Keyboard, mouse, 4 joysticks
- **Polling:** `PollInputDevices()` called every frame

### Message Dispatch
- **Hash Algorithm:** FNV-1a (32-bit)
- **Table Size:** Configurable (default 256 entries)
- **Collision Handling:** Linear probing
- **Handler Format:** `void (*handler)(void* context)`

### Scene Management
- **Three-ID System:** oldSceneID, currentSceneID, newSceneID
- **Transitions:** old → current → new
- **Listeners:** Notified on scene change (linked list)

### Render Queue
- **Deferred Batching:** Linked list of RenderBatchNode
- **Time Budget:** 2ms per frame
- **Processing:** ProcessDeferredRenderQueue() walks list until budget exhausted

## Iteration 8 System Details

### Spell System
**State Machine:**
```
IDLE → BEFORE_CAST → GESTURE_RECOGNITION → GOOD_CAST → SUCCESSFUL_CAST → AFTER_CAST
                                                ↓
                                         FAILED_TO_CAST
```

**Wand Discipline:**
- WANDINNOTSCARED: Holstered, NPCs neutral
- WANDOUTNOTPUNISH: Out in safe zone
- WANDOUTPUNISH: Out in restricted zone (triggers NPC punishment)

**Achievement Tracking:**
- `successfulCastsCount` increments on SUCCESSFUL_CAST
- At 500 casts, achievement unlocked

### Physics System (Havok Stub)
**Note:** Actual Havok SDK not linked. Structures are stubs for future integration or replacement (e.g., Bullet Physics).

**Components:**
- hkPhysicsSystem - World container
- hkRigidBody - Rigid body dynamics
- Motion types: FIXED (static), KEYFRAMED (animated), DYNAMIC
- Collision groups: 32-bit masks for filtering
- MOPP raycasting: Memory Optimized Partial Polytope VM
- Deactivation: Spatial partitioning-based sleep system

### Zone Streaming
**4 Load Zones:**
- Selected based on player position
- Priority system (0-3)
- Async loading

**3 Unload Zones:**
- Freed as player moves away
- Memory budget enforcement

**Hybrid Streaming:**
- Seamless without loading screens
- Preload triggers for cutscenes/transitions
- No-load boxes for small rooms

### Asset Loading
**Resource Handlers:**
- ResourceHandler interface: Initialise(), Load(), Unload()
- Handlers registered per ResourceType
- Pre-allocation: headerSize + bodySize calculated before load
- Validation: Format checks, version checks, CRC

**File Formats:**
- RCB: Animation data
- HKX: Havok physics data
- Babble: Dialogue/subtitle data
- Hull: Collision hulls
- Spline: Path splines for cameras, NPCs
- Trinity: Cutscene sequences

### Animation System
**Skeletal Skinning:**
- GPU-accelerated (vertex shader)
- 1-4 bones per vertex
- Matrix palette uploaded per frame
- Bone indices and weights in vertex attributes

**Blend Shapes:**
- Morph target interpolation
- Weight: 0.0 (base mesh) to 1.0 (full blend)
- Specular tint modulation for lighting effects
- Used for facial expressions, lip-sync

### AI System
**States:**
- Idle variants: scared, angry, sulking, shy, excited, confused, sarcastic
- Active: patrol, wander, chase, attack, sidle, flee

**Locator-Based Navigation:**
- Patrol: cycle through named locators
- Wander: random walk with music triggers
- Chase: target tracking with end markers

**Sidle System:**
- Ledge detection
- sidle_left/right animations
- climb_from_sidle transitions
- Capability flag: `canSidle`

### UI System
**Screens:**
- Versioned: "2.0_Main_Menu", "4.0_Load_Game"
- State machine with transitions
- Previous screen tracked for back navigation

**Button Mapping:**
- 32 buttons: BUTTON1-BUTTON32
- Platform-specific: ButtonX, ButtonA (Xbox), button_square (PlayStation)
- Edge detection for press/release events

**Message Queue:**
- 64-entry ring buffer
- Message types: GAME_LOADING, LOAD_COMPLETE, LOAD_FAILED, etc.
- Decoupled from game logic

## Differences from Original

### Similarities
- Timing system (16.16 fixed-point, 60 FPS, 3x scaling)
- Callback system (8 slots)
- Message dispatch (FNV-1a hash)
- Audio command queue (256 entries)
- Scene management (three-ID system)
- Input double-buffering
- Spell state machine structure
- Zone streaming architecture (4 load, 3 unload)
- Asset manager with type-specific handlers

### Differences
- **No Havok SDK:** Stub interfaces only (original links against Havok 3.x/4.x)
- **No RenderWare:** Original uses RW asset pipeline (HKX export, etc.)
- **No Trinity Sequencer:** Cutscene system stubbed
- **Clean C++ vs. Optimized Assembly:** Zig-compiled vs. MSVC-optimized
- **Single Platform:** Windows x86 only (original: Windows, Xbox 360, PS2/PS3)
- **Gesture Recognition:** Stubbed (original likely uses mouse path matching)
- **AI Behavior Trees:** Stubbed (structure only, no logic)
- **Resource Files:** Original loads .pak/.dat archives (not implemented)

## Missing Implementations

### Critical (prevents running)
1. Havok physics or replacement
2. Asset file loading (RCB, HKX parsing)
3. Texture/model loading and D3D upload
4. Shader compilation and rendering
5. UI rendering (fonts, buttons, menus)

### Important (limits functionality)
1. Spell gesture recognition algorithm
2. AI behavior tree execution
3. Zone streaming trigger logic
4. Trinity sequencer playback
5. Audio file decoding (MP3, Ogg, etc.)

### Nice to have (polish)
1. Blend shape animation blending
2. Particle systems
3. Post-processing effects
4. Achievement system integration

## Next Implementation Steps

Based on analysis, recommended order:

1. **Asset Loading Foundation:**
   - Implement file I/O for .pak/.dat archives
   - Parse RCB animation format
   - Load textures (DDS, TGA) and upload to D3D9

2. **Minimal Physics:**
   - Replace Havok with Bullet Physics or Box2D
   - Implement collision groups and filters
   - Basic rigid body dynamics

3. **Spell System:**
   - Keyboard-based spell triggers (no gesture recognition)
   - Spell effect application (damage, knockback)
   - Wand discipline zone marking

4. **Zone Streaming:**
   - Implement streaming state machine
   - Player position-based zone selection
   - Async asset loading

5. **AI Basics:**
   - Patrol locator cycling
   - Simple chase behavior (proximity-based)
   - Idle state with personality variants

6. **UI Rendering:**
   - Font rendering (bitmap or TrueType)
   - Button hit testing
   - Screen transition logic

This allows the decompilation to run and demonstrate core gameplay, even without perfect accuracy.

## Compilation Success

✅ **Build Status:** Compiles without errors  
✅ **Output Size:** 832 KB (includes debug PDB)  
✅ **Dependencies:** All Windows/DirectX libraries linked  
✅ **Warnings:** None (clean compile)  

**Command:**
```bash
cd decompilation-src && ./build.sh
```

**Output:**
```
Build successful: ../hp_decompiled.exe
```
