# Architectural Differences Between Original and C++ Decompilation (Iteration 7)

## Overview

This document compares the original `hp.exe` architecture (documented in ARCHITECTURE.md) with the C++ decompilation implementation (documented in CPP-ARCHITECTURE.md). The goal is functional equivalence while maintaining clean, readable code.

**Date:** April 14, 2026
**Iteration:** 7
**Compilation Status:** ✅ Builds successfully

## Summary Statistics

| Metric | Original hp.exe | C++ Decompilation | Status |
|--------|----------------|-------------------|--------|
| Binary Size | 5,427,200 bytes | 823,808 bytes | ⚠️ 15% (missing game logic) |
| Compilation | MSVC 2005 | Zig cc (GCC-compatible) | ✅ Different toolchain |
| Architecture | x86 Windows | x86 Windows | ✅ Match |
| Code Lines | ~500,000 (est.) | ~2,070 (structures + stubs) | 🔲 ~0.4% implemented |

## Critical Gaps (Priority 1 - Must Implement)

### 1. Audio Thread Worker Function

**Original:**
- Thread created by `FUN_00611940` (CreateThread API)
- Worker function polls `AudioCommandQueue` via `AudioPollGate` (FUN_006109d0)
- Processes commands with DirectSound API calls
- Runs continuously until `g_bAudioThreadRunning = false`

**C++ Implementation:**
- ✅ Thread handle `g_hAudioThread` defined
- ✅ `AudioCommandQueue` structure complete
- ✅ `AudioPollGate()` signature defined
- 🔲 `AudioThreadProc()` declared but not implemented
- 🔲 No actual DirectSound command processing

**Impact:** High - No audio playback
**Fix Priority:** Immediate
**Estimated Effort:** 200-300 lines

### 2. Message Dispatch Table Lookup

**Original:**
- Hash table with 256 entries
- FNV-1a hashing: `hash = (hash ^ byte) * 16777619`
- Linear probing on collision
- Lookup by `msgID`, dispatch to `handler(dest, params)`

**C++ Implementation:**
- ✅ `MessageEntry` structure complete (msg_hash, handler, dest, param_type)
- ✅ `HashMessageName()` implemented correctly (FNV-1a)
- ✅ `RegisterMessageHandler()` stores entries
- 🔲 `DispatchMessage()` stub only - no actual dispatch logic

**Impact:** High - No event-driven communication
**Fix Priority:** Immediate
**Estimated Effort:** 50-100 lines

### 3. Render Batch Building and Sorting

**Original:**
- `BuildRenderBatch` (FUN_0063d600) creates linked list at `g_pDeferredRenderQueue`
- Sorts by shader hash (SKY, OPAQUE, WATER, ALPHA, GLASS, BLOOM, BACKDROP)
- Processes within 2ms budget via `QueryPerformanceCounter`
- Batch merging for state change reduction

**C++ Implementation:**
- ✅ `RenderBatchNode` structure complete with correct offsets
- ✅ Shader hash constants defined
- ✅ `g_pDeferredRenderQueue` global declared
- 🔲 `BuildRenderBatch()` stub only
- 🔲 `ProcessDeferredRenderQueue()` stub only
- 🔲 No sorting, no time budget enforcement

**Impact:** High - No rendering
**Fix Priority:** Immediate
**Estimated Effort:** 300-400 lines

## Major Gaps (Priority 2 - Important for Functionality)

### 4. Scene Listener Notification

**Original:**
- Linked list of `SceneListener` structs
- `NotifySceneListeners(oldID, newID)` iterates and calls callbacks
- `FlushDeferredSceneListeners` (FUN_006125a0) processes deferred changes
- Prevents recursive scene switches during callbacks

**C++ Implementation:**
- ✅ `SceneIDs` structure defined
- ✅ Function signatures declared
- 🔲 No listener list
- 🔲 No callback mechanism
- 🔲 No deferred flush logic

**Impact:** Medium - Scene transitions won't work
**Fix Priority:** High
**Estimated Effort:** 150-200 lines

### 5. Frame Callback Registration and Invocation

**Original:**
- 8 slots in `g_FrameCallbackSlots` (DAT_00e6e880)
- `RegisterFrameCallback(func, context)` fills slots
- `InvokeFrameCallbacks()` calls all registered callbacks each frame
- Used for subsystem updates (audio, physics, AI, etc.)

**C++ Implementation:**
- ✅ `CallbackSlot[8]` array defined
- ✅ `InvokeFrameCallbacks()` loops through slots
- 🔲 No actual callbacks registered
- 🔲 No subsystem update functions

**Impact:** Medium - No per-frame game logic updates
**Fix Priority:** High
**Estimated Effort:** 100-150 lines per subsystem

### 6. Memory Allocator Implementation

**Original:**
- Custom allocator at 0x00e61380
- Free lists with 254 size classes
- Tag-based tracking for debugging
- Thread-safe with `CRITICAL_SECTION`
- Stats: total_allocated, peak_usage, per-tag counts

**C++ Implementation:**
- ✅ `EngineAllocator` structure complete
- ✅ `FreeListNode` defined
- ✅ `TagStats` structure defined
- 🔲 `AllocEngineObject()` uses default `malloc()` 
- 🔲 `FreeEngineObject()` uses default `free()`
- 🔲 No free list management
- 🔲 No tag tracking

**Impact:** Low - Works but missing debug info
**Fix Priority:** Medium
**Estimated Effort:** 400-500 lines

### 7. Input State Polling

**Original:**
- `RealInputSystem` at DAT_00be8758
- Double-buffered state (current + previous)
- Edge detection for key press/release events
- Polls via `IDirectInputDevice8::GetDeviceState()`
- Thread-safe access via critical section

**C++ Implementation:**
- ✅ `RealInputSystem` structure complete
- ✅ DirectInput devices created
- 🔲 No polling loop
- 🔲 No state copying
- 🔲 No edge detection

**Impact:** Medium - No input handling
**Fix Priority:** High
**Estimated Effort:** 200-250 lines

## Architectural Alignments (Correct)

### ✅ Timing System
- 16.16 fixed-point format implemented correctly
- 60 FPS target (16ms interval)
- 3x game speed scaling
- Double-buffered tick/time arrays
- Spiral-of-death cap (100ms max delta)

### ✅ Window and DirectX Initialization
- Same window class registration
- Same DirectX device creation parameters
- Present parameters structure (g_d3dpp) saved for Reset
- DirectInput8 creation sequence matches
- Keyboard, mouse, joystick devices created

### ✅ Registry Settings
- Same registry paths: `HKCU\Software\Electronic Arts\Harry Potter and the Order of the Phoenix\GameSettings`
- All graphics settings keys match
- Window placement saved/restored correctly
- Fallback HKCU → HKLM → default chain

### ✅ System Parameters Management
- Mouse speed/acceleration save/restore
- Screen reader settings backup
- `UpdateSystemParameters(0)` disables on start
- `UpdateSystemParameters(1)` restores on exit

### ✅ Message Loop
- Standard Win32 message pump
- WM_ACTIVATEAPP focus handling
- WM_SYSCOMMAND interception (screensaver/monitor power)
- WM_ENTERMENULOOP suppression

## Data Structure Accuracy

| Structure | Original Size | C++ Size | Match | Notes |
|-----------|--------------|----------|-------|-------|
| `AudioCommand` | ~20 bytes | 20 bytes | ✅ | Added timestamp field |
| `AudioCommandQueue` | ~0x428 bytes | 0x428 bytes | ✅ | Complete |
| `EngineAllocator` | ~0x840 bytes | ~0x840 bytes | ✅ | All fields documented |
| `RealInputSystem` | ~0x460 bytes | ~0x460 bytes | ✅ | Complete with padding |
| `MessageEntry` | 20 bytes | 20 bytes | ✅ | 5 fields * 4 bytes |
| `RenderBatchNode` | 0x80 bytes | 0x80 bytes | ✅ | Next at +0x7c confirmed |
| `CallbackSlot` | 8 bytes | 8 bytes | ✅ | func + context |
| `TimeManager` | 8 bytes | 8 bytes | ✅ | vtable + isPaused |
| `GameServices` | ~0x48 bytes | 0x48 bytes | ✅ | 6 managers + state |

## Recommendations for Iteration 8

### Must Implement (Critical Path):
1. **Audio Thread Worker** - Enables sound
2. **Message Dispatch** - Enables event system
3. **Render Batch Processing** - Enables graphics
4. **Input Polling** - Enables interaction

### Should Implement (Important):
5. **Scene Listeners** - Enables scene transitions
6. **Frame Callbacks** - Enables game logic updates

### Nice to Have (Completeness):
7. **Memory Allocator** - Better debugging
8. **Command Line Flags** - Developer features

## Conclusion

The C++ decompilation has achieved **excellent architectural alignment** with the original `hp.exe`:
- ✅ All major data structures match original layouts
- ✅ Initialization sequence is correct
- ✅ Timing system is functionally equivalent
- ✅ Window/DirectX/Input setup matches

**Key Remaining Work:**
- 🔲 Implement critical function stubs (~1,500 lines estimated)
- 🔲 Add actual game logic (spells, AI, physics) (~20,000+ lines estimated)
- 🔲 Asset loading and streaming (~5,000 lines estimated)

**Current Completion Estimate:** ~15% of engine code, ~0.5% of total game code

The foundation is solid and well-documented. Iteration 8 should focus on implementing the critical path functions to achieve a runnable (if minimal) game.

---

*Last Updated: Iteration 7 - April 14, 2026*
