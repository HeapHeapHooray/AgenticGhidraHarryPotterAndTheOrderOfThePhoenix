# Iteration 8 Exploration Findings

## Objective
Explore high-level game systems: spell casting, AI, physics, asset loading, UI, and animation.

## Methodology
Used `strings` command on hp.exe to extract relevant keywords and patterns due to Ghidra 12.0 OSGi compatibility issues with headless Java scripts.

---

## 1. Spell System

### Key Findings
- **Spell casting uses event-based system** with states:
  - `BEFORE_CAST`
  - `AFTER_CAST`
  - `SUCCESSFUL_CAST` (misspelled as `SUCESSFUL_CAST` in code)
  - `FAILED_TO_CAST`
  - `GOOD_CAST`
  
- **Wand animation integration**:
  - `wand_blendShape.bs_wand_hold`
  - `wand_blendShape.bs_wand_cast`
  - `hand_rt_blendShape.bs_cast_rt`
  - `anim:spell_cast`
  - `DRAWS_WAND`

- **Spell tracking**:
  - Achievement tracking: `REWARD_DESC_CAST_500_SPELLS`
  - Spell counter: `SUCCESSFUL_SPELLS_CAST_500`
  - Invalid spell detection: `INVALID_SPELL`
  - Finite spell: `SPELL_FINITE`

- **Audio integration**:
  - `AudioWingardiumCast`
  - `AudioCarpeCast`
  - `OnJinxCast`
  - `SetupJinxCast`
  - Combat spells: `COMBAT_SPELL_CASTED_01` through `COMBAT_SPELL_CASTED_07`

- **Spell list data**: `10.4_SpellList` (likely a data file reference)

- **Wand discipline system**:
  - `WANDOUTNOTPUNISH` - Wand out in safe area
  - `WANDOUTPUNISH` - Wand out in restricted area (triggers punishment)
  - `WANDINNOTSCARED` - Wand holstered (NPC not scared)
  - `INVCASTREACTION` - Invalid cast reaction

### Architecture Implications
1. Spell casting is **event-driven** with state machine
2. Integrated with animation blend shapes for visual feedback
3. Audio cues tied to specific spell types
4. Achievement/statistics tracking for spells cast
5. Context-sensitive wand rules (punishment system)

---

## 2. Physics System

### Key Findings
- **Physics Engine: Havok 3.x-4.x**
  - Version strings found: `Havok-3.0.0`, `Havok-3.1.0`, `Havok-4.0.0-b1`, `Havok-4.0.0-r1`
  - License check: `Havok Physics evaluation key has expired or is invalid.`
  - Component enable check: `The following component is not enabled in Havok Prime:`

- **Collision System**:
  - `hkCollisionFilter` - Base collision filter
  - `hkPairwiseCollisionFilter` - Pairwise filtering
  - `hkGroupCollisionFilter` - Group-based filtering  
  - `hkDisableEntityCollisionFilter` - Entity disabling
  - `hkNullCollisionFilter` - Null/passthrough filter
  - `hkCollisionFilterList` - Filter composition
  - `collisionLookupTable` - Hash table for collision pairs
  - `collisionGroups` - Group mask system
  - `noGroupCollisionEnabled` - Disable group filtering flag

- **Rigid Body System**:
  - `hkRigidBody` - Main rigid body class
  - `hkKeyframedRigidMotion` - Keyframe-driven motion
  - `hkFixedRigidMotion` - Static/fixed motion
  - `rigidBodies` - Array/list of bodies
  - `rigidBodyBindings` - Bindings to game objects
  - `rigidBodyFromDisplayObjectTransform` - Transform sync
  - Deactivators:
    - `hkFakeRigidBodyDeactivator` - Fake/placeholder
    - `hkSpatialRigidBodyDeactivator` - Spatial partitioning based
    - `shouldActivateOnRigidBodyTransformChange` - Reactivation on move

- **Physics Data Structures**:
  - `hkPhysicsSystem` - Top-level physics world
  - `hkPhysicsData` - Serialized physics data
  - `physicsSystemBindings` - Bindings to game systems

- **Collision Response**:
  - `toiCollisionResponseRotateNormal` - TOI (Time of Impact) response
  - `collisionTolerance` - Penetration tolerance
  - `collisionDetails` - Detailed collision info
  - `collisionEntries` - Collision event entries
  - `collisionListeners` - Event listeners
  - `CollisionListnr` - Listener interface

- **Special Features**:
  - `Continuous Physics` - CCD (Continuous Collision Detection)
  - `cylBaseRadiusFactorForHeightFieldCollisions` - Heightfield collision tuning
  - `CollisionMesh` - Static collision geometry
  - `PhysicsMotion` - Motion state enum/struct

- **Plugin Integration**:
  - `rwID_HAVOK_HKX_DATA` - RenderWare Havok export data ID
  - `PluginCollisionMesh` - Plugin-based collision mesh
  - `Havok::HybridRef` - Hybrid reference counting

- **Raycasting**:
  - `hkMoppAabbCastVirtualMachine.cpp` - MOPP (Memory Optimized Partial Polytope) raycast VM
  - `iterativeLinearCastMaxIterations` - Linear cast iteration limit
  - `iterativeLinearCastEarlyOutDistance` - Early termination distance
  - `maxCastIterations` - Max raycast iterations

### Architecture Implications
1. **Havok 3.x/4.x mixed version** - likely upgraded mid-development
2. **Group-based collision filtering** with multiple filter types
3. **Deactivation/sleeping system** for performance optimization
4. **Keyframed animation** support for moving platforms
5. **MOPP acceleration structure** for optimized raycasts
6. **RenderWare integration** for asset pipeline
7. **Listener-based collision events** for gameplay logic

---

## 3. Asset Loading System

### Key Findings
- **Loading Screens**:
  - `MSG_GAME_LOADING` - Loading message ID
  - `LoadingScreenDisable` - Disable loading screen
  - `EnterLoadingState` - Enter loading state machine
  - `LoadLevelState` - Level-specific loading state

- **Zone Streaming System**:
  - Load zones: `LoadZone1`, `LoadZone2`, `LoadZone3`, `LoadZone4`
  - Unload zones: `UnloadZone1`, `UnloadZone2`, `UnloadZone3`, `UnloadZoneName`
  - `LOAD_ZONE` / `UNLOAD_ZONE` - Message/event IDs
  - `HybridLoadZone` - Hybrid loading trigger
  - `HybridNoLoadBox` - No-load boundary box
  - Zone preload boxes:
    - `OC_01_EntranceHall_PreloadBox`
    - `Herbology_Corridor_ZonePreload`
  - `HybridCutscenePreloadTrigger` - Cutscene asset preloading

- **Save/Load System**:
  - `LoadGame` / `Load_Game_Options` - Load game functions
  - `4.0_Load_Game` - Load game UI screen
  - `LW_LOADGAME` / `GUI_LOADGAME` - Load game GUI elements
  - `newgame_2_mainmenu`, `main_menu_newgame` - New game flow
  - `Loading %s: pre-allocated %d for header and %d for body buffers` - Pre-allocation message
  - `*Loading Save Game: %s...` - Save game loading
  - `MSG_LOAD_COMPLETE` / `MSG_LOAD_FAILED` - Load result messages

- **Resource Management**:
  - `ModelAssetManager` - Model asset manager class
  - `GOF::ResourceHandlers::Babble::Initialise` - Babble (dialogue) resource handler
  - `GOF::ResourceHandlers::HullResourceHandler::Initialise` - Hull (collision) resource handler
  - `GOF::ResourceHandlers::SplineResourceHandler::Initialise` - Spline path resource handler
  - `EAUK::MEMCARD::Icon::RegisterAsset` - Memory card icon asset registration

- **Resource Loading**:
  - `Component::Animate::LoadRCB` - Load RCB (animation?) file
  - `cRealFont::Load() WARNING - failed to load resource!` - Font loading
  - `FONT::Load() WARNING - failed to load resource!` - Font loading (alternate)
  - `Entry %s loaded successfully` - Generic resource load success
  - `Check validity of loaded data` - Validation step
  - `ERROR: TrinitySequencer::parseResource : sequence binary resource isn't valid` - Sequencer validation

- **Memory Management**:
  - `Should be using an overloaded method for this function using preallocated rather than dynamically allocated memory` - Pre-allocation preference
  - Pre-allocated buffer system for headers and bodies

- **Locators**:
  - `%s_Load_Save_Locator` - Save point locator
  - `%s_Load_Locator` - General load point locator

### Architecture Implications
1. **Zone-based streaming** with 4 concurrent load zones
2. **Pre-allocation strategy** for loading to avoid fragmentation
3. **Resource handler system** with type-specific handlers (Babble, Hull, Spline)
4. **Preload triggers** for cutscenes and zone transitions
5. **Trinity sequencer** for scripted sequences
6. **RCB file format** for animation data
7. **Hybrid system** for seamless zone transitions
8. **GOF (EA UK framework)** resource management architecture

---

## 4. Animation System

### Key Findings
- **Skeletal Animation**:
  - `numBonesPerVertex` - Skinning weights count
  - `boneIndexOffset` - Bone index buffer offset
  - `boneWeightOffset` - Bone weight buffer offset

- **Blend Shapes**:
  - `blendParams` - Blend shape parameters (repeated many times - suggests array/table)
  - `blendspecularTint` - Specular tint blending (shader-level blending)
  - Multiple instances suggest per-material or per-object blending

- **Animation Files**: Found 587 animation-related strings
  - Many blend shape references
  - Bone and skeleton references
  - Blend parameter arrays

### Architecture Implications
1. **GPU-accelerated skinning** with bone indices and weights
2. **Blend shape morphing** for facial animation or deformations
3. **Per-material blending** with specular tint control
4. **Large animation dataset** (587+ entries)

---

## 5. AI System

### Key Findings
- **AI States**:
  - `Idle` - Base idle state
  - `attack` - Attack behavior
  - Various idle types:
    - `scared_idle`, `angry_idle`, `sulking_idle`
    - `shy_idle`, `excited_idle`, `confused_idle`, `sarcastic_idle`
    - `gs_idle_breathe` - Breathing idle animation
    - `Scare_Owl_idle_breathe` - Owl-specific idle

- **AI Behaviors**:
  - `chess:attack` - Chess piece attack (likely animated chess)
  - `FLYING_BOOK_CHASE_SNITCH` - Flying book chasing behavior
  - `Owlery_Interior_OwlChaseTrigger` - Owl chase trigger
  - `Nag-ChaseOwl` - Chase owl nag/reminder
  - `LW01_Chase_End` - Chase sequence end
  - `MoM_DC_Chase_Locator` - Ministry of Magic chase locator

- **Patrol System**:
  - `Grand_Staircase_Lower_GryffStudent_PatrolLocator_1/2/3` - Student patrol points
  - `HybridScriptWanderMusicActions` - Wandering with music actions
  - `WANDER_MUSIC_GLOBAL` - Global wandering music flag
  - Wander navigation:
    - `DH_Wandering_ExitCastle` / `UH_Wandering_ExitCastle`
    - `DH_Wandering_EnterFromExt` / `UH_Wandering_EnterFromExt`

- **Movement Behaviors**:
  - Sidle system:
    - `sidle_left`, `sidle_right`
    - `sidle_idle_left`, `sidle_idle_right`
    - `sidle_hit_left`, `sidle_hit_right`
    - `sidle_to_climb`
    - `climb_from_sidle_left/right`
    - `climb_from_sidle_left/right_jump`
    - `Sidle_Ledge_1_Med_1`
    - `PROMPT_CLIMB_SIDLE_LEFT/RIGHT/FAST`
    - `Agent Can Sidle` - AI capability flag
  
- **Animation Behaviors**:
  - `Roll behavior` - Rolling animation behavior
  - `Loop behavior` - Looping animation behavior
  - `8broadPhaseBorderBehaviour` / `BroadPhaseBorderBehaviour` - Physics broadphase behavior

- **Combat**:
  - `OnMeleeAttack` - Melee attack event
  - `AudioMospAttackStart` / `AudioMospAttackStop` - Mosp attack audio
  - `NifflerTimeAttackMiniGameInfo` - Niffler mini-game info

### Architecture Implications
1. **State-based AI** with emotional/personality variants
2. **Locator-based patrol system** with named waypoints
3. **Scripted wandering** with music integration
4. **Sidle/ledge traversal system** for platforming AI
5. **Hybrid scripting** for complex behaviors
6. **Mini-game AI** for special encounters
7. **Behavior tree or FSM** with loop/roll modifiers

---

## 6. UI System

### Key Findings
- **Menu Structure**:
  - `2.0_Main_Menu` - Main menu screen
  - `start_menu` - Start menu
  - `newgame_2_mainmenu` - New game to main menu transition
  - `packshot_2_mainmenu` - Pack shot to main menu
  - `main_menu_newgame` - Main menu to new game

- **GUI Elements**:
  - `GUI_PRESS_START` - Press start prompt
  - `GUI_TASKS` - Tasks menu
  - `GUI_LOCATIONS` - Locations menu
  - `GUI_PERCENT_DISCOVERED` - Discovery percentage
  - `GUI_SYMBOL_PERCENT` - Percent symbol
  - `gui_audio` - Audio menu
  - `GUI_YES` / `GUI_NO` - Confirmation dialogs
  - `GUI_CONFIRMSAVE` - Save confirmation
  - `GUI_MEMORYCARD_REMOVED_DISABLE_AUTOSAVE` - Memory card removal warning

- **Button Mapping**:
  - Generic buttons: `BUTTON1` through `BUTTON32`
  - Xbox buttons: `ButtonX`, `ButtonA`
  - PlayStation button: `button_square`

- **Error Messages**:
  - `MSG_GAME_LOADING` - Loading message
  - `MSG_LOAD_COMPLETE` - Load success
  - `MSG_LOAD_FAILED` - Load failure

### Architecture Implications
1. **Screen-based UI** with numbered versions (2.0_, 4.0_)
2. **Multi-platform input** (Xbox, PlayStation button naming)
3. **32 button support** - likely for complex controller mapping
4. **Transition system** between UI screens
5. **Message-based UI updates** (MSG_ prefix)
6. **Memory card management** with autosave control

---

## Summary of Key Discoveries

### Third-Party Libraries
1. **Havok Physics 3.x/4.x** - Full physics and collision system
2. **RenderWare Graphics** - Asset pipeline integration
3. **Trinity Sequencer** - Cutscene/sequence system

### Game Systems Identified
1. **Event-driven spell casting** with animation and audio integration
2. **Zone streaming** with 4-zone concurrent loading
3. **AI patrol and wandering** with locator-based waypoints
4. **Ledge/sidle traversal** system for platforming
5. **Blend shape animation** for character expressions
6. **Resource handler architecture** (GOF framework)

### File Formats
- **RCB** - Animation files
- **SpellList** - Spell definitions
- **HKX** - Havok physics data
- **Babble** - Dialogue/audio data
- **Hull** - Collision hulls
- **Spline** - Path data

### Next Steps for Architecture Documentation
1. Document Havok integration details in ARCHITECTURE.md
2. Add spell system state machine
3. Add zone streaming architecture
4. Add AI behavior system
5. Document resource handler system
6. Add animation system details

### Questions for Next Iteration
1. How are spell patterns recognized (mouse gestures)?
2. What is the spell damage/effect calculation system?
3. How does the zone streaming manager work?
4. What is the AI decision-making algorithm?
5. How are blend shapes applied in the render pipeline?
6. What is the RCB file format structure?
7. How does the Trinity sequencer work?
8. What is the Havok-RenderWare integration layer?
