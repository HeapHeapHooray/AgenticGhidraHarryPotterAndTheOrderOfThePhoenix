# Iteration 7 Analysis

## Date
April 14, 2026

## Objective
Address remaining unanswered questions from iteration 6, with focus on:
- Audio thread implementation and worker function
- Memory allocator free list structure details
- Message dispatch hash algorithm
- Input system device structures
- Game logic subsystems (GameServices, save system)
- DirectX shader capability detection
- Complete vtable mappings

## Priority Areas for Exploration

### 1. Audio Thread Implementation (Q15)

**Current Knowledge:**
- Audio thread created by FUN_00611940
- Uses CreateThread API
- Has an entry point function (thread proc)
- Likely polls audio command queue

**Analysis Needed:**
- Identify audio thread entry function address
- Map thread main loop structure
- Identify key calls: DirectSound operations, command queue polling
- Understand thread termination conditions

**Expected Findings:**
```cpp
DWORD WINAPI AudioThreadProc(LPVOID lpParam) {
    // Initialize DirectSound context
    // Main loop:
    while (g_bAudioThreadRunning) {
        // Poll command queue
        int status = AudioPollGate();  // FUN_006109d0
        if (status == 1) {  // Command ready
            // Process audio command
            AudioCommand* cmd = DequeueAudioCommand();
            // Execute based on opcode
        }
        // Sleep or wait on event
    }
    // Cleanup and return
}
```

### 2. Memory Allocator Free List Structure (Q42-48)

**Current Knowledge:**
- Allocator at 0x00e61380
- Free lists at offset +0x428
- 254 free list buckets
- 8-byte alignment (mask 0x7ffffff8)
- Critical section at +0x4e4
- Tag-based allocation tracking

**Analysis Needed:**
- Complete structure layout from 0x00 to end
- Free list bucket sizing scheme (linear vs exponential)
- Metadata stored in allocation headers
- Stats tracking per tag
- Type registry (if exists)

**Likely Structure:**
```cpp
struct FreeListNode {
    FreeListNode* next;
    size_t size;
    // Possibly more metadata
};

struct EngineAllocator {
    // Header fields (0x00 - 0x428)
    void* vtable;                    // +0x00
    DWORD total_allocated;           // +0x04
    DWORD peak_usage;                // +0x08
    DWORD allocation_count;          // +0x0c
    // ... more stats ...
    
    // Free lists (0x428 - 0x4e4)
    FreeListNode* free_lists[254];   // +0x428 (254*4 = 0x3f8 = 1016 bytes)
    
    // Synchronization (0x4e4+)
    CRITICAL_SECTION cs;             // +0x4e4 (24 bytes on Win32)
    
    // Tag tracking (if present)
    TagStats tag_stats[MAX_TAGS];    // After critical section
};
```

**Bucket Sizing Analysis:**
Most likely linear for small sizes, with larger buckets for big allocations:
- Buckets 0-31: 8, 16, 24, 32, ..., 256 (linear, 8-byte steps)
- Buckets 32-127: aligned to 16, 32, 64 bytes
- Buckets 128+: power-of-2 or page-aligned sizes

### 3. Message Dispatch Hash Implementation (Q17-22)

**Current Knowledge:**
- Message registration at FUN_00e63e82
- String names hashed to integer IDs
- Dispatch table stores (msgID, handler, dest_object)
- "iMsg" prefix = interface messages

**Analysis Needed:**
- Hash algorithm used (CRC32, FNV-1a, custom?)
- Collision handling (chaining, open addressing?)
- Dispatch table size and structure
- Message parameter passing mechanism

**Expected Hash Patterns:**
Common game engine hashes:
1. **FNV-1a:** Fast, good distribution
   ```cpp
   uint32_t hash = 2166136261u;
   for (char c : str) {
       hash ^= (uint8_t)c;
       hash *= 16777619u;
   }
   ```

2. **Custom multiply-shift:** Simple, fast
   ```cpp
   uint32_t hash = 0;
   for (char c : str) {
       hash = hash * 33 + (uint8_t)c;
   }
   ```

3. **CRC32:** More robust, potentially slower
   - Uses lookup table at known data section

**Dispatch Structure:**
```cpp
struct MessageEntry {
    uint32_t msgID;       // Hashed from string name
    void (*handler)(void* dest, void* params);
    void* dest_object;    // Destination for this message
    int param_type;       // Type indicator for params
};
```

### 4. Input System Structure (Q54-60)

**Current Knowledge:**
- RealInputSystem at DAT_00be8758
- Device enumeration at FUN_00e64ec3
- DirectInput8 devices: keyboard, mouse, joysticks
- Supports up to 2 joysticks based on MAX_JOYSTICKS

**Analysis Needed:**
- RealInputSystem structure layout
- Device state buffer structure
- Input event queue (if exists)
- Callback registration for input events

**Expected Structure:**
```cpp
struct InputDeviceState {
    BYTE keyboard[256];           // Keyboard state
    DIMOUSESTATE2 mouse;          // Mouse state
    DIJOYSTATE2 joystick[2];      // Joystick states
    bool device_active[4];        // Device presence flags
};

struct RealInputSystem {
    void* vtable;                     // +0x00
    IDirectInput8* pDirectInput;      // +0x04
    IDirectInputDevice8* devices[4];  // +0x08 (keyboard, mouse, joy0, joy1)
    InputDeviceState state;           // Current input state
    InputDeviceState prev_state;      // Previous frame state (for edge detection)
    CRITICAL_SECTION cs;              // Thread safety
    bool paused;                      // Pause state
};
```

### 5. GameServices System (Q37-41)

**Current Knowledge:**
- GameServices at DAT_00bf2260
- Provides access to various game managers
- Save system integration
- Profile data management

**Analysis Needed:**
- GameServices vtable methods
- List of all registered subsystem managers
- Save/load function implementations
- Profile data structure

**Expected GameServices API:**
```cpp
struct GameServices {
    void* vtable;
    
    // Subsystem getters (lazy init pattern common)
    SaveManager* (*GetSaveManager)();
    ProfileManager* (*GetProfileManager)();
    AchievementManager* (*GetAchievementManager)();
    // ... more managers ...
    
    // Data
    void* subsystem_pointers[16];  // Cached manager instances
};
```

### 6. Shader Capability Detection (Q9)

**Current Knowledge:**
- g_nShaderCapabilityLevel at DAT_00bf1994
- Set during D3D device creation
- Likely checks D3DCAPS9 structure
- Levels: 0=fixed, 1=SM1.x, 2=SM2.0, 3=SM3.0

**Analysis Needed:**
- Function that sets this value
- Which D3DCAPS9 fields are checked
- Fallback logic if capabilities insufficient

**Expected Detection:**
```cpp
void DetectShaderCapabilities(IDirect3DDevice9* device) {
    D3DCAPS9 caps;
    device->GetDeviceCaps(&caps);
    
    DWORD ps_version = caps.PixelShaderVersion;
    DWORD vs_version = caps.VertexShaderVersion;
    
    if (D3DSHADER_VERSION_MAJOR(ps_version) >= 3 &&
        D3DSHADER_VERSION_MAJOR(vs_version) >= 3) {
        g_nShaderCapabilityLevel = 3;  // SM 3.0
    } else if (D3DSHADER_VERSION_MAJOR(ps_version) >= 2 &&
               D3DSHADER_VERSION_MAJOR(vs_version) >= 2) {
        g_nShaderCapabilityLevel = 2;  // SM 2.0
    } else if (D3DSHADER_VERSION_MAJOR(ps_version) >= 1 &&
               D3DSHADER_VERSION_MAJOR(vs_version) >= 1) {
        g_nShaderCapabilityLevel = 1;  // SM 1.x
    } else {
        g_nShaderCapabilityLevel = 0;  // Fixed function
    }
}
```

### 7. Complete Vtable Mappings (Q4-5)

**Current Knowledge:**
- Primary callback manager vtable: PTR_FUN_00883f3c
- Secondary factory vtable: PTR_FUN_00883f4c
- Secondary[0] = CreateObject(size, magic)

**Analysis Needed:**
- All methods in both vtables
- Method signatures
- Purpose of each method

**Expected Primary Vtable:**
```cpp
[0] AddRef() / QueryInterface()?
[1] Release() / AddRef()?
[2] Release()?
[3] AddCallback(slot, func, context)?
[4] RemoveCallback(slot)?
[5] DispatchCallbacks()?
[6] GetCallbackCount()?
[7] ClearCallbacks()?
// ... more ...
```

**Expected Secondary (Factory) Vtable:**
```cpp
[0] CreateObject(size, magic) - confirmed
[1] DestroyObject(ptr)?
[2] QueryObjectType(ptr)?
[3] GetObjectCount()?
[4] EnumerateObjects()?
// ... more ...
```

## Investigation Strategy

### Step 1: Decompilation Analysis
For each priority function, analyze decompiled code looking for:
- Control flow patterns (loops, conditionals)
- Data structure access patterns (offset calculations)
- API calls (DirectX, Win32)
- String references (for message names, debug output)

### Step 2: Cross-Reference Mapping
Build call graphs for:
- Audio subsystem (thread creation → worker loop → DirectSound calls)
- Memory allocator (alloc → free list selection → block splitting/merging)
- Message system (registration → hashing → dispatch)
- Input system (enumeration → state polling → event generation)

### Step 3: Data Structure Reconstruction
Use access patterns to infer:
- Field offsets from base pointer arithmetic
- Array sizes from loop bounds
- String/buffer lengths from memcpy/strcpy size args
- Vtable sizes from last non-null entry

### Step 4: Pattern Recognition
Identify common patterns:
- COM-like reference counting
- Observer pattern in callbacks
- Factory pattern in object creation
- Command pattern in deferred operations
- Singleton pattern in managers

## Expected Outcomes

### Documentation
1. ARCHITECTURE.md additions:
   - "Audio Thread Architecture" section
   - "Memory Allocator Internals" section
   - "Message Dispatch Implementation" section
   - Expanded subsystem structure diagrams

2. Updated function annotations for:
   - All audio-related functions
   - Memory allocator and helpers
   - Message registration/dispatch
   - Input device enumeration
   - GameServices accessors

### Code Implementation
1. C++ additions to main.cpp:
   - AudioThreadProc implementation
   - Complete EngineAllocator structure
   - Message hash function
   - GameServices structure

2. New header: subsystems.h
   - All subsystem structure definitions
   - Manager class declarations
   - Vtable layouts as struct pointers

### Verification
1. Compilation success with zig
2. Binary size comparison (should grow closer to original)
3. Architectural alignment check (CPP-ARCHITECTURE vs ARCHITECTURE)

## Questions This Iteration Should Answer

**Audio (Q15):**
- ✅ Audio thread entry point address
- ✅ Thread main loop structure
- ✅ Key DirectSound operations in thread

**Memory (Q42-48):**
- ✅ Complete EngineAllocator structure
- ✅ Free list bucket sizing scheme
- ✅ Metadata storage format
- ✅ Tag tracking mechanism

**Messages (Q17-22):**
- ✅ Hash algorithm identification
- ✅ Collision handling method
- ✅ Dispatch table structure
- ✅ Parameter passing mechanism

**Input (Q54-60):**
- ✅ RealInputSystem structure layout
- ✅ Device state buffer format
- ✅ Event queue structure (if exists)

**GameServices (Q37-41):**
- ✅ Vtable methods enumeration
- ✅ Registered subsystems list
- ✅ Save/load implementation overview

**Shaders (Q9):**
- ✅ Detection function location
- ✅ D3DCAPS9 fields checked
- ✅ Capability level mapping

**Vtables (Q4-5):**
- ✅ Complete primary vtable
- ✅ Complete secondary vtable
- ✅ Method signatures

## Next Steps After Analysis

1. Write ExploreIter7.java script encoding findings
2. Write AnnotateIter7.java with all new comments
3. Update ARCHITECTURE.md with discoveries
4. Update QUESTIONS.md with remaining gaps
5. Implement structures in C++ (main.cpp + new subsystems.h)
6. Compile and verify
7. Generate CPP-ARCHITECTURE.md from implementation
8. Compare and create DIFFERENCES.md
9. Refine C++ based on differences
10. Commit iteration 7 progress

## Success Criteria

- [ ] All priority questions (Q4-5, Q9, Q15, Q17-22, Q37-48, Q54-60) answered
- [ ] At least 15 new function identifications with comments
- [ ] At least 5 complete structure layouts documented
- [ ] C++ code compiles without errors
- [ ] DIFFERENCES.md shows < 10 major architectural gaps
- [ ] Ready for iteration 8 deeper dive into game logic

---

This analysis framework will guide the iteration 7 exploration and ensure systematic coverage of remaining unknowns.
