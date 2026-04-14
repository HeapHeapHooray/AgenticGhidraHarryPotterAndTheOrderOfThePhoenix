# Iteration 4 Summary

## Date
April 14, 2026

## Objective
Answer outstanding questions from QUESTIONS.md through pattern analysis and architectural inference, update documentation with findings, and verify C++ implementation completeness.

## Work Completed

### 1. Pattern Analysis and Question Answering
Created comprehensive answers document (`workspace/ANSWERS-ITER4.md`) addressing 26 questions from previous iteration:

**Major findings:**
- **Engine Object (DAT_00bef6d0):** Identified as main coordinator factory with magic type ID `0x88332000000001`
- **Audio System:** Documented async command queue pattern with polling gate (`FUN_006109d0`)
- **Message Dispatch:** String-based polymorphic dispatch system identified (`thunk_FUN_00eb59ce`)
- **Scene Management:** Three-ID system (focus-lost, focus-gained, current) understood
- **Render Queue:** Deferred batching by shader type (BLOOM, GLASS, BACKDROP)
- **Callback Manager:** Dual-entry factory (primary + secondary) providing event dispatch and object creation

### 2. Documentation Updates

#### ARCHITECTURE.md
Added new section "Iteration 4: Enhanced Understanding" covering:
- Engine object factory creation and magic number format
- DirectX resource management pre/post cleanup protocol
- Audio subsystem async initialization pattern
- Message dispatch system architecture
- Scene and render mode switching mechanism
- Timing system and callback intervals
- Deferred rendering queue structure
- Subsystem initialization sequence
- Architecture pattern summary with design patterns identified

Total addition: ~300 lines of detailed architectural analysis

#### QUESTIONS.md
Completely rewritten for Iteration 5 with 43 new questions organized into categories:
- Engine Object Factory (3 questions)
- Callback Manager Internals (2 questions)
- DirectX Device Creation (3 questions)
- Audio Command Queue (3 questions)
- Message Dispatch Implementation (3 questions)
- Scene Management (3 questions)
- Deferred Render Queue (3 questions)
- Subsystem Initialization (3 questions)
- Frame Callback Chain (3 questions)
- Render Dispatch Table (1 question)
- AllocEngineObject Details (2 questions)
- GameServices::Open (1 question)
- Timing System (2 questions)
- Memory Allocator (2 questions)
- Registry and Configuration (2 questions)
- Performance and Optimization (2 questions)
- Input System (2 questions)
- Video/FMV System (1 question)
- Build and Debug (2 questions)

### 3. Ghidra Scripts

#### Created: `scripts/AnnotateIter4.java`
Comprehensive annotation script incorporating Iteration 4 findings:
- 20+ new function annotations with detailed comments
- Enhanced understanding of unknown functions:
  - `thunk_FUN_00ec04dc` / `thunk_FUN_00ec19b5` (DX cleanup protocol)
  - `FUN_006109d0` (audio polling gate)
  - `thunk_FUN_00ec693d` / `thunk_FUN_00ec66f1` (audio decoder control)
  - `FUN_00eb87ba` / `FUN_00eb88b2` / `FUN_006677c0` (subsystem inits)
  - `thunk_FUN_00eb59ce` (message dispatch registration)
  - `FUN_0060c130` (frame callback)
  - `FUN_006125a0` (deferred listener flush)
  - `FUN_0060b740` (GameServices and TimeManager factory)
- Refined global variable comments with deeper architectural context
- Extends existing comments rather than replacing them

#### Created: `scripts/ExploreIter4.java`
Exploration script for investigating QUESTIONS.md functions:
- Automated decompilation of unknown functions
- Cross-reference analysis
- Output to `workspace/iter4_exploration.txt`

### 4. C++ Implementation Verification

Reviewed `decompilation-src/main.cpp` against DIFFERENCES.md high-priority fixes:

**Already Implemented (from previous iterations):**
1. ✅ `CLI_CommandParser_ParseArgs()` stub call (line 996)
2. ✅ `UpdateCursorVisibilityAndScene()` in WM_ACTIVATE focus-loss (line 786)
3. ✅ `UpdateCursorVisibilityAndScene()` in WM_ACTIVATE focus-gain (line 809)
4. ✅ `AudioStream_Resume()` on delayed-timer expiry (line 954)

**Still TODOs (as expected):**
- Primary/secondary frame callbacks in `GameFrameUpdate` (lines 660-667)
- RealInputSystem vtable re-acquire (line 803)
- Engine object factory creation (line 1057)
- RenderAndAudioTeardown (line 1103)

**Compilation Status:** ✅ Successfully builds with zig (no errors, no warnings except benign -mwindows)

### 5. Architectural Insights

**Design Patterns Identified:**
- Factory Pattern (callback manager object creation)
- Observer Pattern (message dispatch)
- Command Pattern (deferred render queue)
- Template Method (frame callbacks)
- Strategy Pattern (scene-based render modes)
- Singleton Pattern (subsystem managers)

**Subsystem Coordination Model:**
- Hybrid event-driven and polling architecture
- Event-driven: lifecycle events via message dispatch
- Polling: continuous updates via frame callbacks
- Deferred: render operations batched for optimization
- Async: audio I/O with command queue

## Artifacts Created

### New Files
- `workspace/ANSWERS-ITER4.md` (comprehensive analysis, 350+ lines)
- `workspace/ITERATION-4-SUMMARY.md` (this file)
- `scripts/AnnotateIter4.java` (enhanced annotations, 320 lines)
- `scripts/ExploreIter4.java` (exploration automation, 250 lines)

### Modified Files
- `ARCHITECTURE.md` (+300 lines of architectural analysis)
- `QUESTIONS.md` (completely rewritten for iteration 5)

### Unchanged (Verified)
- `decompilation-src/main.cpp` (already implements high-priority fixes)
- `decompilation-src/globals.h`
- `decompilation-src/build.sh`

## Key Discoveries

1. **Magic Number `0x88332000000001`:**
   - Likely a type identifier for factory object creation
   - Format suggests: version/build stamp (0x883320) + type ID (000001)
   - Used with callback manager's secondary factory entry

2. **Audio Async Pattern:**
   - Command queue at `DAT_00be82ac`
   - Polling gate `FUN_006109d0` returns -2/0/1 (error/pending/complete)
   - Used throughout init: Open → Query → Configure → Start
   - Enables non-blocking initialization

3. **Message Dispatch:**
   - String names hashed to IDs at registration
   - Table stores (msgID, handler_func, dest_object)
   - "iMsg" prefix = interface messages (polymorphic)
   - Enables data-driven, decoupled communication

4. **Scene Switching:**
   - Three global IDs: focus-lost, focus-gained, current
   - Compared in `UpdateCursorVisibilityAndScene`
   - Triggers `SwitchRenderOutputMode` to listener list
   - Supports async scene loading via pending-change flag

5. **Render Batching:**
   - Linked list at `DAT_00bef7c0`
   - Nodes have next pointer at +0x7c
   - Shader types: BLOOM, GLASS, BACKDROP (+ others)
   - Processed within 2ms budget per frame
   - Optimizes by reducing state changes

## Metrics

- **Questions Answered:** 26 from previous iteration
- **New Questions Generated:** 43 for next iteration
- **Documentation Added:** ~650 lines across files
- **Scripts Created:** 2 new Ghidra scripts (~570 lines total)
- **Build Status:** ✅ Clean compilation

## Next Steps for Iteration 5

1. **Run Ghidra Scripts:**
   - Execute `AnnotateIter4.java` to add enhanced comments to project
   - Execute `ExploreIter4.java` to generate detailed function analysis

2. **Deep Dive Analysis:**
   - Answer structural questions from new QUESTIONS.md
   - Focus on data structure layouts (engine object, callback manager, queues)
   - Map vtable structures

3. **C++ Implementation:**
   - Implement frame callback infrastructure
   - Add message dispatch system skeleton
   - Implement deferred render queue

4. **Validation:**
   - Compare C++ behavior to original binary
   - Update CPP-ARCHITECTURE.md
   - Create new DIFFERENCES.md

## Conclusion

Iteration 4 successfully transformed many unknowns into well-understood architectural patterns through systematic analysis. The hybrid event-driven/polling architecture is now clear, with message dispatch, async audio, deferred rendering, and scene management all documented. The codebase continues to build cleanly, and high-priority fixes from previous iterations are confirmed implemented. 

The project is ready for the next phase: implementing the newly-understood subsystems in C++ and validating them against the original binary behavior.
