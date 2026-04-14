# Architectural Differences Between Original and C++ Decompilation (Iteration 8)

## Overview

This document compares the original `hp.exe` architecture (documented in ARCHITECTURE.md) with the C++ decompilation implementation (documented in CPP-ARCHITECTURE.md). The goal is functional equivalence while maintaining clean, readable code.

**Date:** April 14, 2026  
**Iteration:** 8 - High-level game systems added  
**Compilation Status:** ✅ Builds successfully (832 KB)

## Summary Statistics

| Metric | Original hp.exe | C++ Decompilation | Status |
|--------|----------------|-------------------|--------|
| Binary Size | 5,427,200 bytes (5.3 MB) | 832,000 bytes (832 KB) | ⚠️ 15% (missing assets, game logic) |
| Compilation | MSVC 2005 | Zig 0.16.0-dev (C++) | ✅ Different toolchain |
| Architecture | x86 Windows | x86 Windows | ✅ Match |
| Code Lines | ~500,000 (estimated) | ~2,780 (main.cpp + globals.h) | 🔲 ~0.56% implemented |
| Subsystems | 20+ | 13 (core + iteration 8) | 🔲 65% coverage |

## Iteration 8 Progress Summary

**New Systems Added:**
1. ✅ Spell System structures (state machine, wand discipline)
2. ✅ Physics (Havok stub interfaces)
3. ✅ Zone Streaming (4 load + 3 unload zones)
4. ✅ Asset Loading (GOF framework handlers)
5. ✅ Animation (skeletal skinning, blend shapes)
6. ✅ AI System (state machine, patrol, sidle)
7. ✅ UI System (screens, buttons, messages)

**Implementation Status:**
- **Structures:** ✅ Complete (all key structures defined)
- **Logic:** 🔲 Stubbed (no actual implementation)
- **Third-Party Integration:** ❌ Missing (Havok, RenderWare, Trinity)

---

## Core Engine Systems

### 1. Timing System

| Feature | Original | C++ Decompilation | Match? |
|---------|----------|-------------------|--------|
| Fixed-point format | 16.16 | 16.16 | ✅ |
| Frame rate target | 60 FPS (16ms) | 60 FPS (16ms) | ✅ |
| Game time scaling | 3x multiplier | 3x multiplier | ✅ |
| Callback interval | 1,048,576 (16ms in 16.16) | 1,048,576 | ✅ |
| Primary callback | UpdateFrameTimingPrimary | UpdateFrameTimingPrimary (stub) | ⚠️ Stubbed |
| Secondary callback | InterpolateFrameTime | InterpolateFrameTime (stub) | ⚠️ Stubbed |

**Differences:**
- Original: Full timing logic with frame interpolation for rendering
- Decompilation: Timing infrastructure present, interpolation stubbed

**Priority:** Medium (works for basic timing, needs interpolation logic)

---

### 2. Memory Allocator

| Feature | Original | C++ Decompilation | Match? |
|---------|----------|-------------------|--------|
| Tagged allocations | ✅ | ✅ Structure defined | ⚠️ Not implemented |
| Free lists | ✅ Per-size buckets | ✅ Structure defined | ⚠️ Not implemented |
| Statistics tracking | ✅ | ✅ Structure defined | ⚠️ Not implemented |
| Header format | AllocHeader struct | AllocHeader struct | ✅ |

**Differences:**
- Original: Full custom allocator with free list management, defragmentation
- Decompilation: Uses `malloc`/`free`, structures defined but not used

**Priority:** Low (standard allocator works for now)

---

### 3. Callback System

| Feature | Original | C++ Decompilation | Match? |
|---------|----------|-------------------|--------|
| Callback slots | 8 slots | 8 slots | ✅ |
| Registration | RegisterCallback(slot, func, ctx) | ✅ Structure | ⚠️ Not implemented |
| Invocation | Every 16ms | ✅ Implemented | ✅ |
| Unregister | UnregisterCallback(slot) | ❌ Not implemented | ❌ |

**Differences:**
- Original: Full callback registration/unregistration API
- Decompilation: Slots exist and are invoked, but registration API missing

**Priority:** Medium (needed for subsystem integration)

---

### 4. Message Dispatch

| Feature | Original | C++ Decompilation | Match? |
|---------|----------|-------------------|--------|
| Hash algorithm | FNV-1a (32-bit) | FNV-1a (32-bit) | ✅ |
| Table size | 256+ entries | 256 entries (configurable) | ✅ |
| Collision handling | Linear probing | ❌ Not implemented | ❌ |
| Handler registration | RegisterMessageHandler | ❌ Not implemented | ❌ |
| Dispatch | DispatchMessage(hash, context) | ❌ Not implemented | ❌ |

**Differences:**
- Original: Full hash table with collision resolution, dynamic registration
- Decompilation: Structures defined, no logic

**Priority:** High (critical for subsystem communication)

---

### 5. Audio System

| Feature | Original | C++ Decompilation | Match? |
|---------|----------|-------------------|--------|
| Command queue | 256-entry ring buffer | 256-entry ring buffer | ✅ |
| Async thread | ✅ AudioThreadProc | ✅ Thread created | ⚠️ Stubbed worker |
| DirectSound | ✅ Full integration | ✅ Device init only | ⚠️ Partial |
| Command types | Play, Stop, SetVolume, SetPitch | ✅ Enum defined | ❌ Not processed |

**Differences:**
- Original: Full DirectSound integration with streaming, 3D positioning
- Decompilation: Thread and queue exist, but no command processing

**Priority:** High (needed for audio playback)

---

### 6. Input System

| Feature | Original | C++ Decompilation | Match? |
|---------|----------|-------------------|--------|
| Double buffering | ✅ Current + previous state | ✅ Structure defined | ⚠️ Partial |
| Edge detection | IsKeyPressed, IsKeyReleased | ✅ Structure for it | ❌ Not implemented |
| Device polling | PollInputDevices every frame | ✅ Structure | ⚠️ Stubbed |
| Joystick support | 4 joysticks | 4 joystick pointers | ✅ |

**Differences:**
- Original: Full DirectInput polling with edge detection
- Decompilation: Devices enumerated, but polling/state update stubbed

**Priority:** High (needed for player control)

---

### 7. Graphics System

| Feature | Original | C++ Decompilation | Match? |
|---------|----------|-------------------|--------|
| D3D9 device | ✅ | ✅ | ✅ |
| Render targets | Multi-RT support | ✅ Structure | ⚠️ Not used |
| Device lost/reset | ✅ Full handling | ✅ Basic handling | ⚠️ Partial |
| Shader capability | Detection + fallback | ✅ Structure | ❌ Not implemented |
| Deferred render queue | ✅ Batching | ✅ Structure | ❌ Not implemented |

**Differences:**
- Original: Full deferred rendering with batching, shader compilation, multi-RT
- Decompilation: D3D device exists, but no rendering logic

**Priority:** Critical (no rendering = no visuals)

---

## High-Level Game Systems (Iteration 8)

### 8. Spell System

| Feature | Original | C++ Decompilation | Match? |
|---------|----------|-------------------|--------|
| State machine | BEFORE_CAST → SUCCESSFUL_CAST | ✅ Enum defined | ❌ No logic |
| Spell types | Wingardium, Carpe, Combat, etc. | ✅ Enum defined | ❌ No data |
| Gesture recognition | Mouse path matching | ❌ Not implemented | ❌ |
| Wand discipline | Safe/restricted zones | ✅ Enum defined | ❌ No zone marking |
| Achievement tracking | successfulCastsCount | ✅ Field in struct | ❌ Not incremented |
| Animation integration | Blend shapes for wand | ✅ Structure | ❌ Not connected |
| Audio integration | Per-spell audio cues | ✅ Structure | ❌ Not triggered |

**Differences:**
- Original: Full spell casting with gesture recognition, context-sensitive wand rules, audio/animation integration
- Decompilation: Data structures only, no implementation

**Priority:** Critical (core gameplay mechanic)

**Missing:**
- Mouse gesture capture and pattern matching algorithm
- Spell parameter data (damage, range, cooldown, effects)
- SpellList file parsing ("10.4_SpellList")
- Context detection for wand discipline zones
- Event triggers for BEFORE_CAST, SUCCESSFUL_CAST, etc.

---

### 9. Physics System (Havok)

| Feature | Original | C++ Decompilation | Match? |
|---------|----------|-------------------|--------|
| Physics engine | Havok 3.x/4.x (commercial) | ❌ Stub interfaces only | ❌ |
| Rigid bodies | hkRigidBody | ✅ Stub struct | ❌ No logic |
| Collision filtering | Group masks (32-bit) | ✅ Field in struct | ❌ Not used |
| MOPP raycasting | Virtual machine for casts | ❌ Not implemented | ❌ |
| Deactivation/sleep | Spatial partitioning | ❌ Not implemented | ❌ |
| Keyframed motion | For animated objects | ✅ Enum value | ❌ Not supported |
| RenderWare integration | HKX export plugin | ❌ Not present | ❌ |

**Differences:**
- Original: Full Havok Physics SDK integration with rigid bodies, collision detection, raycasting
- Decompilation: Stub structures, **no actual physics**

**Priority:** Critical (needed for player movement, collisions, object interactions)

**Missing:**
- Havok SDK linkage (commercial license required) OR
- Replacement physics engine (Bullet Physics, Box2D, etc.)
- hkPhysicsSystem initialization and stepping
- Collision group setup and filtering
- Rigid body creation from game objects
- MOPP data parsing and raycast VM
- Physics-animation synchronization

**Recommendation:** Replace with Bullet Physics (open source, similar API)

---

### 10. Zone Streaming

| Feature | Original | C++ Decompilation | Match? |
|---------|----------|-------------------|--------|
| Load zones | 4 concurrent | ✅ Array of 4 LoadZone | ❌ No manager |
| Unload zones | 3 concurrent | ✅ Array of 3 LoadZone | ❌ No manager |
| Hybrid streaming | Seamless (no loading screen) | ✅ Flag in struct | ❌ Not implemented |
| Player position tracking | For zone selection | ✅ Fields in struct | ❌ Not updated |
| Preload triggers | Cutscenes, transitions | ❌ Not implemented | ❌ |
| Memory budgeting | Per-zone limits | ✅ Field (memoryUsed) | ❌ Not enforced |

**Differences:**
- Original: Full streaming manager with async loading, player-position-based zone selection, memory budgets
- Decompilation: Data structures only, no streaming logic

**Priority:** High (needed for level loading)

**Missing:**
- Streaming state machine (UNLOADED → LOADING → LOADED → UNLOADING)
- Player position monitoring and zone selection algorithm
- Async asset loading (threading or coroutines)
- Memory budget enforcement
- Preload trigger volumes (HybridCutscenePreloadTrigger, HybridLoadZone)
- No-load box handling
- Loading screen activation/deactivation

---

### 11. Asset Loading (GOF Framework)

| Feature | Original | C++ Decompilation | Match? |
|---------|----------|-------------------|--------|
| Resource handlers | Per-type (RCB, HKX, Babble, etc.) | ✅ Handler array | ❌ No handlers |
| Pre-allocation | Header + body buffers | ✅ Fields in struct | ❌ Not used |
| Asset hash table | For fast lookup | ✅ pAssetTable | ❌ Not implemented |
| File formats | RCB, HKX, Babble, Hull, Spline, Trinity | ✅ Enum values | ❌ No parsers |
| Validation | Format, version, CRC checks | ❌ Not implemented | ❌ |
| GOF framework | EA UK resource system | ❌ Not present | ❌ |

**Differences:**
- Original: Full GOF resource management framework with type-specific handlers, pre-allocation, validation
- Decompilation: Structures defined, **no file I/O**

**Priority:** Critical (no assets = no game)

**Missing:**
- File I/O for .pak/.dat archives
- RCB animation file parser
- HKX physics data parser (Havok format)
- Babble dialogue/subtitle parser
- Hull collision hull parser
- Spline path parser
- Trinity cutscene sequence parser
- GOF::ResourceHandlers::* implementations
- Pre-allocation size calculation
- Asset hash table insertion/lookup
- Resource validation (format magic, version, CRC)

---

### 12. Animation System

| Feature | Original | C++ Decompilation | Match? |
|---------|----------|-------------------|--------|
| Skeletal skinning | GPU (vertex shader) | ✅ Structure | ❌ No shader |
| Bone count | Per mesh | ✅ Field (numBones) | ❌ Not used |
| Bones per vertex | 1-4 | ✅ Field | ❌ Not used |
| Matrix palette | For GPU upload | ✅ pBoneMatrices | ❌ Not uploaded |
| Blend shapes | Morph targets | ✅ BlendShape struct | ❌ Not applied |
| Specular tint blending | Lighting modulation | ✅ Field | ❌ Not used |
| RCB file format | Animation data | ❌ Parser missing | ❌ |

**Differences:**
- Original: Full GPU-accelerated skeletal animation with blend shapes, lighting modulation
- Decompilation: Structures defined, **no animation playback**

**Priority:** High (needed for character/object movement)

**Missing:**
- RCB file format parser
- Bone hierarchy loading
- Keyframe interpolation (linear, bezier, etc.)
- Matrix palette calculation (bone transforms)
- GPU buffer upload (bone indices, weights, matrices)
- Vertex shader for skinning
- Blend shape weight application
- Specular tint shader integration

---

### 13. AI System

| Feature | Original | C++ Decompilation | Match? |
|---------|----------|-------------------|--------|
| AI states | Idle, Patrol, Wander, Chase, Attack, Sidle, Flee | ✅ Enum | ❌ No FSM |
| Idle variants | Scared, angry, sulking, shy, etc. | ✅ Enum values | ❌ Not used |
| Patrol locators | Named waypoints | ✅ AILocator struct | ❌ No data |
| Wander system | Music-driven navigation | ❌ Not implemented | ❌ |
| Chase behavior | Target tracking | ❌ Not implemented | ❌ |
| Sidle system | Ledge traversal | ✅ canSidle flag | ❌ No detection |
| Capability flags | Per-agent features | ✅ Fields in struct | ❌ Not checked |

**Differences:**
- Original: Full AI system with behavior trees/FSMs, locator-based navigation, personality variants
- Decompilation: Structures defined, **no AI logic**

**Priority:** High (needed for NPC behavior)

**Missing:**
- AI state machine tick/update function
- State transition logic
- Patrol locator data loading from levels
- Locator cycling algorithm
- Wander navigation with music triggers
- Chase target selection and tracking
- Sidle ledge detection algorithm
- Personality state selection (random, scripted, time-based)
- Mini-game AI (NifflerTimeAttackMiniGameInfo)

---

### 14. UI System

| Feature | Original | C++ Decompilation | Match? |
|---------|----------|-------------------|--------|
| UI screens | Main Menu 2.0, Load Game 4.0, etc. | ✅ Enum | ❌ No rendering |
| Button mapping | 32 buttons (Xbox, PS, generic) | ✅ Array | ❌ Not polled |
| Message queue | 64-entry ring buffer | ✅ Array | ❌ Not processed |
| Screen transitions | State machine | ✅ currentScreen field | ❌ No transitions |
| UI versioning | "2.0_", "4.0_" prefixes | ✅ Enum naming | ✅ Match |
| Edge detection | Button press/release | ✅ wasPressed field | ❌ Not updated |

**Differences:**
- Original: Full UI system with rendering, input handling, screen transitions
- Decompilation: Structures defined, **no UI rendering or logic**

**Priority:** High (needed for menus, HUD)

**Missing:**
- UI rendering (fonts, textures, buttons, menus)
- Button input polling and mapping
- Button edge detection (press, release)
- Message queue processing
- Screen transition state machine
- Font rendering (TrueType or bitmap)
- UI layout definitions
- HUD elements (health, stamina, spell selection, etc.)

---

## Third-Party Middleware

### Havok Physics

| Component | Original | C++ Decompilation |
|-----------|----------|-------------------|
| Version | 3.x/4.x (mixed) | Stub only |
| License | Commercial | ❌ Not licensed |
| Integration | Full SDK | ❌ No SDK |
| Status | ✅ Functional | ❌ Missing |

**Impact:** Game cannot run without physics. Needs replacement (Bullet Physics recommended).

### RenderWare Graphics

| Component | Original | C++ Decompilation |
|-----------|----------|-------------------|
| Purpose | Asset pipeline | ❌ Not present |
| HKX export | Havok data | ❌ Missing |
| Status | ✅ Functional | ❌ Missing |

**Impact:** Cannot load assets exported with RenderWare tools. Needs custom parsers.

### Trinity Sequencer (EA)

| Component | Original | C++ Decompilation |
|-----------|----------|-------------------|
| Purpose | Cutscene scripting | ❌ Not present |
| Binary format | Proprietary | ❌ Parser missing |
| Status | ✅ Functional | ❌ Missing |

**Impact:** Cutscenes cannot play. Low priority (gameplay works without cutscenes).

---

## File Format Support

| Format | Purpose | Original | C++ Decompilation |
|--------|---------|----------|-------------------|
| RCB | Animation data | ✅ | ❌ Parser missing |
| HKX | Havok physics | ✅ | ❌ Parser missing |
| Babble | Dialogue/subtitles | ✅ | ❌ Parser missing |
| Hull | Collision hulls | ✅ | ❌ Parser missing |
| Spline | Path data | ✅ | ❌ Parser missing |
| Trinity | Cutscene scripts | ✅ | ❌ Parser missing |
| .pak/.dat | Archive files | ✅ (likely) | ❌ Not implemented |

**Priority:** Critical (cannot load game data without parsers)

---

## Functional Gaps Summary

### Critical (Prevents Running)
1. ❌ **Asset loading** - No file I/O, no parsers
2. ❌ **Physics engine** - Havok stub, needs replacement
3. ❌ **Rendering** - No scene rendering, no shaders
4. ❌ **UI rendering** - No fonts, no button rendering
5. ❌ **Input handling** - Polling stubbed

### High Priority (Limits Functionality)
1. ❌ **Spell gesture recognition** - Core gameplay missing
2. ❌ **AI behavior execution** - NPCs won't act
3. ❌ **Zone streaming logic** - Levels won't load
4. ❌ **Animation playback** - Characters won't animate
5. ❌ **Message dispatch** - Subsystem communication broken

### Medium Priority (Reduces Accuracy)
1. ⚠️ **Audio command processing** - Thread exists, but stubbed
2. ⚠️ **Callback registration** - Slots exist, but no API
3. ⚠️ **Memory allocator** - Uses malloc instead of custom allocator
4. ⚠️ **Frame interpolation** - Timing works, but no smoothing

### Low Priority (Nice to Have)
1. ⚠️ **Blend shape animation** - For facial expressions
2. ❌ **Trinity cutscene playback** - Not essential for gameplay
3. ❌ **Achievement integration** - Spell counter not incremented
4. ❌ **Mini-game AI** - Special encounters

---

## Recommended Implementation Order

Based on critical path analysis:

### Phase 1: Make it Run
1. ✅ **Compile successfully** (DONE - iteration 8)
2. **Asset loading foundation**:
   - Basic file I/O for loose files (skip .pak/.dat archives for now)
   - Stub asset loaders (return dummy data)
   - Load textures (DDS/TGA) and upload to D3D9
3. **Minimal rendering**:
   - Clear screen (solid color)
   - Render single textured quad (test texture loading)
   - Basic camera and projection matrices
4. **Input handling**:
   - Implement PollInputDevices (keyboard only)
   - Edge detection for key presses
   - Wire up to WinMain message loop

**Goal:** Window opens, clears to color, responds to key presses, exits cleanly

### Phase 2: Make it Move
1. **Physics replacement**:
   - Integrate Bullet Physics (open source)
   - Replace hkPhysicsSystem with btDynamicsWorld
   - Replace hkRigidBody with btRigidBody
   - Basic collision detection (box collider for player)
2. **Player controller**:
   - WASD movement (apply forces to player rigid body)
   - Camera following player
   - Ground detection (raycasts)
3. **Basic rendering**:
   - Render simple geometry (boxes, spheres)
   - Basic diffuse lighting
   - Depth buffer and Z-testing

**Goal:** Player can move around a simple level with physics

### Phase 3: Make it Playable
1. **Spell system**:
   - Keyboard-based spell triggers (no gesture recognition)
   - Spell projectiles (rigid bodies with collision)
   - Basic spell effects (damage, knockback)
2. **AI basics**:
   - Idle state (NPC stands still)
   - Patrol (cycle through hardcoded waypoints)
   - Chase (simple proximity detection)
3. **Zone streaming**:
   - Load/unload based on player position
   - Trigger volumes (LoadZone1-4)
   - Async loading (thread per zone)
4. **UI basics**:
   - Bitmap font rendering
   - Simple HUD (health bar, spell name)
   - Pause menu

**Goal:** Functional gameplay loop - cast spells, fight enemies, navigate zones

### Phase 4: Make it Accurate
1. **Animation system**:
   - RCB file parser
   - Skeletal skinning shader
   - Blend shape support
2. **Advanced AI**:
   - Sidle system (ledge traversal)
   - Behavior trees
   - Personality variants
3. **Advanced rendering**:
   - Deferred rendering
   - Post-processing
   - Particle systems
4. **Audio**:
   - Audio command queue processing
   - DirectSound buffer management
   - 3D positional audio

**Goal:** Close to original experience

---

## Testing Strategy

### Unit Tests
- Timing system (verify 60 FPS, 3x scaling)
- FNV-1a hash function
- Message dispatch hash table
- Audio queue ring buffer operations

### Integration Tests
- D3D device creation and reset
- DirectInput device enumeration
- DirectSound initialization
- Physics world creation (Bullet)

### Functional Tests
- Player movement with physics
- Spell casting (keyboard triggers)
- Zone loading/unloading
- UI screen transitions

### Comparison Tests
- Compare timing values with original (logging)
- Compare physics behavior (position, velocity)
- Compare rendering output (screenshots)

---

## Conclusion

**Current State:** Iteration 8 adds high-level game system structures (spells, physics, streaming, assets, AI, UI), bringing subsystem coverage to 65%. Code compiles successfully but **cannot run** due to missing implementations.

**Next Steps:** Focus on Phase 1 (Make it Run) - asset loading, basic rendering, input handling. This will enable visual feedback and interaction, making further development easier.

**Estimated Effort:**
- Phase 1: 20-40 hours
- Phase 2: 40-80 hours
- Phase 3: 80-160 hours
- Phase 4: 160-320+ hours

**Total:** ~300-600 hours for functional decompilation matching original gameplay.

**Blocker:** Third-party middleware (Havok, RenderWare) requires either licensing or open-source replacements (Bullet Physics, custom parsers).
