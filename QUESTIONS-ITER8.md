# Questions for Further Exploration (Iteration 8)

## Summary of Iteration 7 Progress

Iteration 7 completed comprehensive subsystem architecture documentation:
- ✅ Audio thread implementation and command queue structure
- ✅ Memory allocator internal structure with free lists
- ✅ Message dispatch hash algorithm (FNV-1a)
- ✅ Input system device structures (RealInputSystem)
- ✅ GameServices subsystem pattern
- ✅ Shader capability detection logic
- ✅ Scene management three-ID system details
- ✅ Render queue batching and time budgets
- ✅ Complete vtable mappings (primary + secondary)
- ✅ Complete command line flag enumeration
- ✅ Resource notification system

All major subsystems are now architecturally understood. Iteration 8 focuses on:
1. Game logic implementations (spells, AI, physics)
2. Asset loading and streaming
3. Scripting/event system
4. Network/multiplayer (if present)
5. Performance profiling hooks
6. Remaining edge cases and special functions

## New Questions from Iteration 7 Completion

### Game Logic - Spell System

1. **How is the spell system implemented?**
   - Spell data structure (ID, name, mana cost, effect type, animation)
   - Spell casting state machine
   - Targeting system (ray casting, area of effect)
   - Spell effect application to targets
   - Cooldown management

2. **What spells are in the game and how are they identified?**
   - Enumerate spell IDs by searching for spell name strings
   - Mapping: spell_id → spell_data structure
   - Unlocking progression (which spells available when)
   - Upgrade system (if exists)

3. **How does the wand gesture recognition work?**
   - Mouse movement tracking and pattern matching
   - Gesture templates (shapes for each spell)
   - Tolerance parameters and scoring
   - Success/failure threshold

### Game Logic - AI System

4. **What is the AI state machine structure?**
   - State enumeration (IDLE, PATROL, CHASE, ATTACK, FLEE, etc.)
   - Transition conditions between states
   - Per-state behavior functions
   - AI update frequency

5. **How is pathfinding implemented?**
   - Algorithm: A*, Dijkstra, or waypoint navigation?
   - Nav mesh structure or grid-based?
   - Obstacle avoidance
   - Path smoothing

6. **What is the NPC behavior tree or decision system?**
   - Behavior priority system
   - Perception system (sight, sound ranges)
   - Memory/alertness system
   - Group coordination (if NPCs work together)

### Game Logic - Physics and Collision

7. **What physics engine is used (if any)?**
   - Custom implementation vs third-party (Havok, PhysX)?
   - Rigid body dynamics
   - Collision detection (broad phase + narrow phase)
   - Constraint solver

8. **How is collision detection structured?**
   - Collision primitives (sphere, box, capsule, mesh)
   - Spatial partitioning (octree, BSP, grid)?
   - Collision layers/groups
   - Collision callbacks

9. **What is the character controller implementation?**
   - Movement state machine (walk, run, jump, crouch)
   - Ground detection and slope handling
   - Gravity and jump physics
   - Animation blending with movement

### Asset Loading - Resource Manager

10. **How does the resource manager work?**
    - Resource types (texture, mesh, sound, script)
    - Loading states (unloaded, loading, loaded, error)
    - Reference counting for resource lifetime
    - Cache eviction policy (LRU, priority-based)

11. **What is the file format for game assets?**
    - Archive format (.pak, .dat, custom)?
    - Compression (zlib, LZO, custom)?
    - Encryption/obfuscation
    - File table of contents structure

12. **How does level streaming work?**
    - Level divided into zones/chunks?
    - Streaming triggers (distance, portals)?
    - Load priority system
    - Unload criteria for memory management

13. **What is the texture loading pipeline?**
    - DDS format or custom?
    - Mip-map generation
    - Texture atlasing
    - Async loading with placeholder textures

### Scripting and Events

14. **Is there a scripting system (Lua, custom)?**
    - Script VM or native code?
    - Script → C++ binding mechanism
    - Hot-reloading support
    - Error handling and sandboxing

15. **How is the event/trigger system implemented?**
    - Event types (enter_zone, use_object, dialogue_choice, etc.)
    - Trigger volumes and collision detection
    - Event queue and dispatch
    - Save/load of triggered events

16. **What is the dialogue system structure?**
    - Dialogue tree format
    - Branching conditions
    - Voice-over synchronization
    - Subtitle display and localization

17. **How are cutscenes and cinematics handled?**
    - Pre-rendered video vs in-engine?
    - Camera path system
    - Actor animation sequencing
    - Skip handling

### Save/Load System Deep Dive

18. **What is the complete save file format?**
    - Header: magic, version, checksum, timestamp
    - Sections: player data, world state, inventory, quest flags
    - Serialization of dynamic objects
    - Backward compatibility handling

19. **How is world state saved?**
    - Which objects persist (opened doors, destroyed items)?
    - Delta encoding vs full snapshot?
    - Compression ratio achieved
    - Maximum save file size

20. **What is the checkpoint/auto-save system?**
    - Trigger conditions for auto-save
    - Number of auto-save slots
    - Save-in-progress handling (don't save during save)
    - Corruption recovery mechanism

### Performance and Profiling

21. **Are there built-in performance profiling hooks?**
    - CPU profiling markers
    - GPU timing queries
    - Memory allocation tracking
    - Frame time breakdown display

22. **What is the level-of-detail (LOD) system?**
    - LOD levels per model (how many)
    - Distance thresholds for switching
    - Transition smoothing (cross-fade, pop-in)?
    - Dynamic LOD based on performance

23. **How is occlusion culling implemented?**
    - Frustum culling (view frustum)
    - Occlusion queries (hardware or software)?
    - Portal-based culling
    - Potentially Visible Set (PVS) precomputation

24. **What optimizations exist for draw call batching?**
    - Static batching (combined meshes)
    - Dynamic batching (instancing)
    - Material batching (texture atlases)
    - Vertex buffer optimization

### Multiplayer/Network (If Exists)

25. **Is there any multiplayer or network code?**
    - Search for WinSock functions (WSAStartup, connect, send, recv)
    - Network protocol (TCP, UDP, both)?
    - Packet structure
    - Client-server or peer-to-peer?

26. **If multiplayer exists, how is state synchronization handled?**
    - Entity replication system
    - Input prediction and reconciliation
    - Lag compensation
    - Cheat prevention

### UI System

27. **How is the UI system implemented?**
    - Immediate mode (draw every frame) or retained mode (widget tree)?
    - UI layout system (anchors, relative positioning)
    - Input handling and focus management
    - UI rendering (dedicated pass, shader, etc.)

28. **What is the menu navigation system?**
    - Menu state machine
    - Controller/keyboard navigation
    - Animation and transitions
    - Accessibility features (scaling, contrast)

29. **How are fonts rendered?**
    - Bitmap fonts or TrueType?
    - Font atlas generation
    - Text shaping and kerning
    - Localization character set support

### Audio System Deep Dive

30. **What audio codec is used for music and SFX?**
    - MP3, OGG Vorbis, WMA, custom?
    - Streaming vs preloaded
    - Compression ratio
    - Multi-channel support (stereo, 5.1)

31. **How is 3D positional audio implemented?**
    - Sound source attenuation curve
    - Doppler effect
    - Reverb zones
    - Occlusion by geometry

32. **What is the music system (interactive/adaptive)?**
    - Layer blending based on game state
    - Crossfade timing
    - Loop points and cue markers
    - Music prioritization over SFX

### Animation System

33. **What is the animation blending system?**
    - Animation states and transitions
    - Blend tree structure
    - Inverse kinematics (IK) usage
    - Animation compression

34. **How are skeletal animations stored?**
    - Bone hierarchy structure
    - Keyframe format (position, rotation, scale)
    - Interpolation method (linear, cubic)
    - Animation data size per clip

35. **Is there a facial animation system?**
    - Blend shapes/morph targets
    - Bone-based facial rig
    - Lip-sync automation
    - Expression presets

### Platform-Specific

36. **Are there console-specific code paths?**
    - #ifdef checks for Xbox 360, PS3, Wii?
    - Platform abstraction layer
    - Different asset formats per platform
    - Input mapping differences

37. **How is the PC version different from console?**
    - Higher resolution textures
    - Additional graphics options
    - Mouse+keyboard controls
    - Save location differences

### Debugging and Development

38. **What debug features are compiled in?**
    - Console commands (if accessible)
    - Cheat codes
    - Developer menu
    - Debug visualization (collision boxes, nav mesh, etc.)

39. **Are there assert/logging systems?**
    - Assertion macro expansion
    - Log file location and format
    - Log levels (ERROR, WARN, INFO, DEBUG)
    - Crash dump generation

40. **What build configurations exist?**
    - Debug vs Release differences
    - Shipping build (all debug code removed)?
    - Profile build (optimized but with profiling)
    - Retail vs demo version differences

## Investigation Strategy for Iteration 8

### Priority 1: Game Logic
- Decompile spell casting functions
- Analyze AI state machines
- Understand physics integration

### Priority 2: Asset Pipeline
- Reverse engineer archive format
- Document resource loading flow
- Map streaming system

### Priority 3: Remaining Systems
- UI implementation details
- Animation blending
- Performance tools

## Success Criteria for Iteration 8

- [ ] At least 15 new questions answered
- [ ] Game logic core loop documented
- [ ] Asset format partially reversed
- [ ] C++ implementation includes game logic stubs
- [ ] DIFFERENCES.md shows < 5 major gaps
- [ ] Ready for final polish iteration

---

This iteration focuses on the game-specific logic that sits above the engine infrastructure we've already documented. Understanding these systems completes the architectural picture and enables full decompilation.
