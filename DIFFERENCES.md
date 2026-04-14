# Differences Between Original hp.exe and C++ Decompilation (Iteration 5)

This document compares the original `hp.exe` architecture (documented in `ARCHITECTURE.md` from binary analysis) against the C++ reconstruction in `decompilation-src/` (described in `CPP-ARCHITECTURE.md`).

**Status Legend:**
- ✅ **MATCH** - Functionally equivalent implementation
- ⚠️ **PARTIAL** - Implemented but incomplete or simplified
- 🔲 **MISSING** - Not yet implemented
- 🔄 **DIFFERS** - Different approach but equivalent behavior

---

## 1. Initialization Sequence

### WinMain Entry and Setup

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| FPU Configuration | `_control87(0x20000, 0x30000)` | Same | ✅ MATCH |
| System Parameters Save | `SystemParametersInfoA` for mouse/SR | Same | ✅ MATCH |
| Command Line Copy | 2x strncpy to 0x200-byte buffers | Same | ✅ MATCH |
| CLI Parser | `CLI_CommandParser_ParseArgs()` @ 00eb787a | Stubbed (TODO comment) | 🔲 MISSING |
| Single Instance | `FindWindowA` → `TerminateProcess` | Same | ✅ MATCH |
| Window Registration | `RegisterWindowClass()` | Same | ✅ MATCH |
| Registry Load | `LoadGameSettings()` | Same | ✅ MATCH |
| Command Line Flags | `fullscreen`, `widescreen`, etc. | Same | ✅ MATCH |
| Window Creation | `CreateGameWindow(hInst, w, h)` | Same | ✅ MATCH |

### Engine Object Factory

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| Callback Manager Init | `GetOrInitCallbackManager()` @ 00eb5c3e | Global pointers declared | 🔲 MISSING |
| Engine Root Object | Creates 2904-byte object @ DAT_00bef6d0 | `g_pEngineRootObject` declared, not allocated | 🔲 MISSING |
| Magic Number | `{0x88332000000001, 0}` for type ID | Not used | 🔲 MISSING |
| Factory Call | `(*DAT_00e6e874[0])(0xb58, magic)` | Not called | 🔲 MISSING |
| Dual-Entry System | Primary (DAT_00e6e870) + Secondary (DAT_00e6e874) | Pointers declared | 🔲 MISSING |

**Impact:** Engine factory creation is critical for subsystem coordination. Without it, subsystem initialization cannot proceed correctly.

### DirectX and Subsystem Initialization

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| PreDirectXInit | Audio context setup @ 00ec64f9 | Stubbed | ⚠️ PARTIAL |
| DirectX Init | D3D + DirectInput @ 00eb612e | Stubbed | ⚠️ PARTIAL |
| Game Subsystems Init | Callbacks, devices, language @ 00eb496e | Stubbed, sets flag | ⚠️ PARTIAL |
| Window Restore | `ShowWindow` + `UpdateWindow` | Same | ✅ MATCH |

---

## 2. Frame Callback System

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| Callback Slots | 8 slots @ DAT_00e6e880 (0x1c bytes) | `CallbackSlot g_FrameCallbackSlots[8]` | ✅ MATCH |
| InitFrameCallbackSystem | Clears 8 slots @ 00eb8744 | `InitFrameCallbackSystem()` implemented | ✅ MATCH |
| Callback Structure | `{func_ptr, context_ptr}` pairs | Same | ✅ MATCH |
| Registration | Multiple subsystems register callbacks | `RegisterFrameCallback()` implemented | ✅ MATCH |
| Invocation | Called from `GameFrameUpdate` | `InvokeFrameCallbacks()` implemented | ✅ MATCH |
| Primary Callback | @ DAT_008e1644[0], called with `&localTick` | Declared, not dispatched | 🔲 MISSING |
| Secondary Callback | @ DAT_008e1644[1], called with no args | Declared, not dispatched | 🔲 MISSING |

**Impact:** Frame callbacks are central to game logic. Primary/secondary dispatch is not yet hooked up in `GameFrameUpdate`.

---

## 3. Message Dispatch System

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| Hash Algorithm | Unknown (CRC32/FNV/custom) | FNV-1a implemented | 🔄 DIFFERS |
| RegisterMessageHandler | @ 00eb59ce, stores (msgID, handler, dest, paramType) | Implemented (linear table) | ⚠️ PARTIAL |
| Dispatch Table | Hash table or array | Linear array (256 slots) | 🔄 DIFFERS |
| Message ID Storage | Stored with handler | Not stored yet | 🔲 MISSING |
| DispatchMessage | Lookup by ID, call handler | Skeleton implemented | 🔲 MISSING |
| Message Names | "iMsgDeleteEventHandler", "iMsgDoRender", etc. | Not registered | 🔲 MISSING |

**Impact:** Message dispatch infrastructure exists but mapping and actual dispatch are incomplete.

**Recommendation:** Add msgID field to `MessageHandler` struct, implement hash table lookup.

---

## 4. Audio Command Queue

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| Queue Structure | @ DAT_00be82ac, ring buffer or linked list | Ring buffer (32 slots) | ✅ MATCH |
| Command Structure | Opcode, params, callback, status | Same | ✅ MATCH |
| Audio Thread | Created @ DAT_00bf1b30 via FUN_00611940 | `g_hAudioThread` declared | 🔲 MISSING |
| EnqueueAudioCommand | Adds command to queue | Implemented | ✅ MATCH |
| AudioPollGate | @ 006109d0, returns -2/0/1 | Implemented | ✅ MATCH |
| Thread Worker | Processes queue asynchronously | Not implemented | 🔲 MISSING |
| Async Init | Open → Query → Configure → Start | Not implemented | 🔲 MISSING |

**Impact:** Audio queue structure is ready, but thread creation and processing logic are missing.

**Recommendation:** Implement `CreateThread` call and worker function that processes queue in loop.

---

## 5. Scene Management

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| Scene IDs | DAT_00c82b00 (focus-lost), DAT_00c82b08 (focus-gain), DAT_00c82ac8 (current) | `SceneIDs g_SceneIDs` | ✅ MATCH |
| Three-ID System | Compared in `UpdateCursorVisibilityAndScene` | Structure declared | ⚠️ PARTIAL |
| LoadSceneIDs | Loads from level data or scripting | Stubbed | 🔲 MISSING |
| SwitchRenderOutputMode | @ 00612530, notifies listeners | `SwitchRenderOutputModeEx()` partial | ⚠️ PARTIAL |
| Scene Listeners | Linked list or array of callbacks | Not implemented | 🔲 MISSING |
| Pending-Change Flag | @ listener_list.head+0x12 | Not implemented | 🔲 MISSING |
| FlushDeferredSceneListeners | @ 006125a0 | Stubbed | 🔲 MISSING |

**Impact:** Scene management structure exists but listener notification is incomplete.

**Recommendation:** Implement listener list as `std::vector` or linked list, add registration function.

---

## 6. Deferred Render Queue

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| Queue Head | @ DAT_00bef7c0, linked list | `RenderBatchNode* g_pDeferredRenderQueue` | ✅ MATCH |
| Node Structure | Next pointer @ +0x7c | `RenderBatchNode` with next @ end | ✅ MATCH |
| Shader Type Hash | BLOOM, GLASS, BACKDROP, etc. | Declared in struct | ✅ MATCH |
| BuildRenderBatch | @ 0063d600, sorts by shader | Stubbed | 🔲 MISSING |
| ProcessDeferredRenderQueue | 2ms time budget | Implemented (iteration only) | ⚠️ PARTIAL |
| Time Budget Check | `timeGetTime()` loop | Implemented | ✅ MATCH |
| Shader Recognition | String/hash/enum comparison | Not implemented | 🔲 MISSING |

**Impact:** Queue iteration works, but batch building (shader sorting, D3D calls) is missing.

**Recommendation:** Implement shader type hash table and D3D state sorting logic.

---

## 7. Memory Allocator

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| AllocEngineObject | @ 00614210, debug-aware with tags | Implemented | ✅ MATCH |
| Alloc Header | Tag, size, next, magic | Same | ✅ MATCH |
| Magic Number | 0xDEADBEEF corruption detection | Same | ✅ MATCH |
| Tracking List | Global linked list for leak detection | Declared, not updated | ⚠️ PARTIAL |
| Per-Tag Stats | Memory usage by tag | Not implemented | 🔲 MISSING |
| Free Lists | @ allocator+0x428, 254 buckets | Not implemented | 🔲 MISSING |
| Critical Section | @ allocator+0x4e4, thread-safe | Not implemented | 🔲 MISSING |
| FreeEngineObject | Releases memory | Implemented | ✅ MATCH |

**Impact:** Basic allocation works, but tracking and stats are incomplete.

**Recommendation:** Add global tracking list updates in alloc/free, implement stats query function.

---

## 8. Timing System

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| Fixed-Point Format | 64-bit 16.16 | Same | ✅ MATCH |
| Callback Interval | @ DAT_00c83190/94, likely 16ms (60 FPS) | `g_ullCallbackInterval` declared | ⚠️ PARTIAL |
| Interval Initialization | Set in `InitFrameCallbackSystem` or first `GameFrameUpdate` | Not set | 🔲 MISSING |
| Game Tick Counter | @ DAT_00c83110, incremented by localTick * 3 | `g_dwGameTicks` implemented | ✅ MATCH |
| Time Scale | * 3 / 0x10000 | Same | ✅ MATCH |
| TimeManager Singleton | @ DAT_00bef768, vtable + isPaused | `TimeManager*` declared | ⚠️ PARTIAL |
| Pause Flag | @ +0x04, 0=running | Declared in struct | ✅ MATCH |
| UpdateFrameTimingPrimary | Checks pause, updates ticks | Not implemented | 🔲 MISSING |
| InterpolateFrameTime | Smooth interpolation | Not implemented | 🔲 MISSING |

**Impact:** Timing infrastructure exists but callback dispatch and interpolation are missing.

**Recommendation:** Set `g_ullCallbackInterval = 16 << 16` in init, implement primary/secondary timing updates.

---

## 9. DirectX Device Creation

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| CreateD3DDevice | @ 0067c290, 9 parameters | Function exists | ⚠️ PARTIAL |
| Parameter 1 | Client height | Passed | ✅ MATCH |
| Parameters 2-9 | Adapter, flags, quality, booleans | Unknown/stubbed | 🔲 MISSING |
| Device Storage | @ DAT_00bf1920 (IDirect3DDevice9*) | `g_pd3dDevice` | ✅ MATCH |
| Interface Storage | @ DAT_00bf1924 (IDirect3D9*) | `g_pD3D` | ✅ MATCH |
| Shader Capability Level | @ DAT_00bf1994, set from D3DCAPS9 | Not set | 🔲 MISSING |
| NotifyPreReleaseResources | @ 00ec04dc, flush/save state | Stubbed | 🔲 MISSING |
| NotifyPostReleaseResources | @ 00ec19b5, clear caches | Stubbed | 🔲 MISSING |

**Impact:** Device creation parameters need clarification for correct initialization.

**Recommendation:** Decompile `CreateD3DDevice` to determine all 9 parameters, implement resource notification.

---

## 10. Subsystem Structures

### GlobalTempBuffer (DAT_00e6b378)

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| Size | 0x3c bytes (60) | Same | ✅ MATCH |
| Callback Count | @ +0x0d, max 5 | Declared | ✅ MATCH |
| Callback Pairs | Function + context ptrs | Declared | ✅ MATCH |
| Allocation | Via `AllocEngineObject` | `GlobalTempBuffer*` declared | 🔲 MISSING |

### RealGraphSystem (DAT_00e6b390)

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| Size | 8 bytes | Same | ✅ MATCH |
| Vtable | @ PTR_FUN_00885010 | Declared | ✅ MATCH |
| Callback Mgr Ptr | @ +0x04, points to DAT_00e6e874 | Declared | ✅ MATCH |
| Allocation | Via engine object factory | Not allocated | 🔲 MISSING |

### TimeManager (DAT_00bef768)

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| Size | 8 bytes | Same | ✅ MATCH |
| isPaused Flag | @ +0x04 | Declared | ✅ MATCH |
| Creation | In `FUN_0060b740` | Not created | 🔲 MISSING |

**Impact:** Subsystem structures are defined but not allocated/initialized.

**Recommendation:** Implement factory functions and lifecycle management.

---

## 11. Subsystem Initialization Functions

| Function | Original | C++ Implementation | Status |
|----------|----------|-------------------|--------|
| InitLanguageResources | @ 00eb87ba, loads string tables | Stubbed | 🔲 MISSING |
| InitVideoCodec | @ 00eb88b2, Bink/Smacker init | Stubbed | 🔲 MISSING |
| FinalizeRenderInit | @ 006677c0, shaders/textures | Stubbed | 🔲 MISSING |

**Impact:** Subsystem content (strings, videos, shaders) not loaded.

**Recommendation:** Decompile these functions to understand file loading and initialization.

---

## 12. Cleanup and Teardown

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| RenderAndAudioTeardown | @ 00ec6610, 3-step audio shutdown | TODO comment | 🔲 MISSING |
| Engine Object Destroy | Via callback_mgr+0xc destructor | TODO comment | 🔲 MISSING |
| SaveOptionsOnExit | @ 00eb4a5d, writes 3 settings | Stubbed | ⚠️ PARTIAL |
| RealInputSystem Pause | Via vtable call | TODO comment | 🔲 MISSING |
| UnacquireInputDevices | Releases DirectInput devices | Implemented | ✅ MATCH |
| System Param Restore | `SaveOrRestoreSystemParameters(true)` | Implemented | ✅ MATCH |

**Impact:** Cleanup is incomplete, may cause resource leaks or crashes on exit.

**Recommendation:** Implement proper teardown order: audio → render → input → engine objects.

---

## 13. Registry System

| Component | Original | C++ Implementation | Status |
|-----------|----------|-------------------|--------|
| HKCU → HKLM Fallback | Yes | Yes | ✅ MATCH |
| Write-back to HKCU | On HKLM hit | Yes | ✅ MATCH |
| Auto-create in HKCU | With default value | Yes | ✅ MATCH |
| std::basic_string Use | Internally uses C++ strings | Uses C strings | 🔄 DIFFERS |

**Impact:** Functional equivalence despite implementation difference.

---

## 14. Implementation Approach Differences

### Memory Layout

| Aspect | Original | C++ Implementation | Impact |
|--------|----------|-------------------|--------|
| Global Variables | Fixed addresses (DAT_*) | C++ globals (g_*) | 🔄 DIFFERS - Functionally equivalent |
| Heap Allocation | Custom allocator with free lists | `malloc`/`free` with debug headers | 🔄 DIFFERS - Simplified |
| Structures | Raw memory offsets | C++ structs | 🔄 DIFFERS - Better type safety |
| VTables | Manual vtable pointers | Potential C++ virtual classes | 🔄 DIFFERS - Cleaner code |

### Code Organization

| Aspect | Original | C++ Implementation | Impact |
|--------|----------|-------------------|--------|
| Functions | Scattered across binary | Logically grouped in main.cpp | 🔄 DIFFERS - Better readability |
| Magic Numbers | Embedded constants | Named #define constants | 🔄 DIFFERS - Self-documenting |
| Jumps | Assembly jumps and gotos | Structured control flow | 🔄 DIFFERS - Maintainable |
| Hard-coded Addresses | Yes | None | 🔄 DIFFERS - Portable |

---

## Summary of Gaps (Priority Order)

### High Priority (Blocks Functionality)

1. **Engine Object Factory Creation** - Without this, subsystems can't initialize
2. **Frame Callback Dispatch** - Game logic and rendering won't update
3. **Audio Thread Creation** - Audio initialization will hang
4. **Callback Interval Initialization** - Frame timing won't work correctly

### Medium Priority (Reduces Accuracy)

5. **Message Dispatch Mapping** - Event system won't route correctly
6. **Scene Listener List** - Scene transitions won't notify properly
7. **Render Batch Building** - Rendering won't be optimized
8. **DirectX Parameter Clarification** - Device may not initialize correctly
9. **Shader Capability Detection** - Wrong shader path may be used

### Low Priority (Nice to Have)

10. **Memory Allocator Stats** - Leak detection and profiling unavailable
11. **Subsystem Initialization Details** - Content won't load (strings, videos, shaders)
12. **Proper Teardown** - May cause leaks, but doesn't affect gameplay
13. **CLI Parser Implementation** - Command-line args won't be parsed correctly

---

## Next Iteration Focus

Based on this analysis, Iteration 6 should prioritize:

1. Implement engine object factory with magic number
2. Hook up frame callback dispatch (primary/secondary)
3. Set callback interval (16ms or 33ms)
4. Implement message dispatch lookup and dispatch logic
5. Create audio thread and worker function
6. Clarify DirectX device parameters

These changes will significantly improve functional equivalence with the original binary.
