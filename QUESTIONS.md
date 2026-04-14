# Questions for Further Exploration (Iteration 7)

## Summary of Iteration 6 Progress

Iteration 6 implemented critical timing infrastructure and callback dispatch. We completed:
- ✅ Frame callback interval initialization (16ms for 60 FPS)
- ✅ Primary timing callback (`UpdateFrameTimingPrimary`)
- ✅ Secondary interpolation callback (`InterpolateFrameTime`)
- ✅ Frame callback invocation system
- ✅ Double-buffered timing with pause state checking
- ✅ Successfully compiled C++ decompilation with zig

Key achievements:
- Game timing now dispatches to primary/secondary callbacks correctly
- TimeManager pause state properly checked before tick updates
- Smooth frame interpolation enabled for rendering
- Code architecture matches original with proper callback flow

## Summary of Iteration 5 Progress

Iteration 5 deepened our understanding of subsystem structures and implementation details. We documented:
- Engine object factory magic number format and dual-entry callback system
- Frame callback slots and timing infrastructure  
- DirectX device creation parameters and shader capability levels
- Audio command queue structure and async patterns
- Message dispatch hash-based implementation
- Scene management three-ID system with listener notifications
- Deferred render queue batching and time budgets
- Subsystem structures (GlobalTempBuffer, RealGraphSystem, Locale, FMV, GameServices, TimeManager)
- Memory allocator free lists and critical sections
- Timing system intervals and interpolation

## Answered in Iteration 6

**Q49: What is the exact value of g_ullCallbackInterval?**
- ✅ ANSWERED: 16ms (60 FPS) in 16.16 fixed-point format
- Value: 16 << 16 = 1,048,576 (0x100000)
- Set during InitFrameCallbackSystem() initialization
- Confirmed through implementation and successful compilation

**Q50: How is frame time interpolation factor calculated?**
- ✅ ANSWERED: Uses double-buffered timing arrays
- Formula: `t = (currentTick - prevTick) / (currTick - prevTick)`
- Result: `prevTime + (currTime - prevTime) * t`
- Clamped to [0.0, 1.0] range for smooth rendering

**Q51: Why is game tick incremented by localTick * 3?**
- ✅ ANSWERED: Game runs at 3x speed internally
- Constant GAME_TIME_SCALE = 3
- Physics/logic updates use accelerated time
- Matches original's tick calculation: `g_dwGameTicks = (accum * 3) / 0x10000`

## New Questions from Iteration 6 Analysis

### Engine Object Factory Deep Dive

1. **What are the exact field offsets within the 2904-byte engine object (DAT_00bef6d0)?**
   - Vtable at +0x00?
   - Magic number at +0x04 and +0x0c?
   - Where are subsystem pointers stored?
   - Is there a subsystem pointer table or individual fields?
   - Can we map the layout by analyzing read/write patterns?

2. **What other magic numbers exist beyond {0x88332000000001, 0}?**
   - Search for similar 16-byte patterns in factory calls
   - Do different object types have different magic numbers?
   - Is there a type registry mapping magic to vtables?
   - Format: `{buildstamp + type_id, flags}`?

3. **What does the custom destructor at callback_mgr+0xc actually do?**
   - Decompile the function to understand teardown order
   - Does it iterate subsystems in reverse init order?
   - Reference counting or immediate destruction?
   - Error handling for failed cleanup?

### Callback Manager Vtable Analysis

4. **What are ALL the methods in the primary vtable (PTR_FUN_00883f3c)?**
   - Beyond factory creation, what operations exist?
   - AddCallback, RemoveCallback, DispatchEvent?
   - GetCallbackCount, ClearCallbacks?
   - Map out complete vtable up to first nullptr

5. **What are ALL the methods in the secondary vtable (PTR_FUN_00883f4c)?**
   - CreateObject(size, magic) is [0]
   - What's at [1], [2], [3]...?
   - DestroyObject, QueryObjectType, GetObjectCount?

6. **How are the 8 callback slots (DAT_00e6e880) actually populated?**
   - Which functions call the registration API?
   - When during InitGameSubsystems are they set?
   - Do they correspond to subsystems (audio, render, input, physics, etc.)?
   - Are there priority/ordering guarantees?

### DirectX Parameter Clarification

7. **What is parameter 2 in CreateD3DDevice (currently "unknown")?**
   - Adapter index (for multi-GPU)?
   - Behavior flags (D3DCREATE_*)?
   - Window handle parameter?
   - Check caller to see what value is passed

8. **What do boolean flags 6-9 actually control?**
   - Param 6 = vsync enable?
   - Param 7 = multithreaded D3D?
   - Param 8 = pure device (D3DCREATE_PUREDEVICE)?
   - Param 9 = hardware vertex processing (vs software)?
   - Cross-reference with D3D present parameters

9. **How exactly is g_nShaderCapabilityLevel (DAT_00bf1994) determined?**
   - Which D3DCAPS9 fields are checked?
   - PixelShaderVersion? VertexShaderVersion?
   - Mapping: 0=fixed, 1=SM1.x, 2=SM2.0, 3=SM3.0?
   - Where is this set (function address)?

10. **What specific operations do NotifyPreReleaseResources and NotifyPostReleaseResources perform?**
    - Decompile both functions
    - Do they iterate listener lists?
    - What global state do they modify?
    - Connection to message dispatch system?

### Audio Command Queue Implementation

11. **What is the exact structure layout of AudioCommandQueue (DAT_00be82ac)?**
    - Ring buffer or linked list?
    - How many command slots (array size)?
    - Field offsets for head, tail, count, lock?
    - Command structure size?

12. **What is the structure of individual AudioCommand entries?**
    - Opcode field (enum values)?
    - Parameter storage (union? void*?)
    - Callback function pointer?
    - Status field (int: -2/0/1)?
    - Timestamp or sequence number?

13. **What does AudioPollGate (FUN_006109d0) actually check?**
    - Decompile to see implementation
    - Does it check queue head status?
    - Lock-free read or critical section?
    - Timeout handling?

14. **What does QueryAudioFormatSupport (FUN_006a91a0) query?**
    - DirectSound GetFormat?
    - Specific format flags (16-bit PCM, 44.1kHz, stereo)?
    - Why is 0x80 the expected return value?
    - Bitmask breakdown: 0x80 = bit 7 set, meaning?

15. **What is the audio thread's entry point function?**
    - Created by FUN_00611940
    - What's the actual thread routine address?
    - Does it loop on command queue, sleep, or event-driven?
    - Thread priority and affinity?

16. **What does 0x1000 represent in SetAudioDecoderState?**
    - Verify DirectSound frequency constant
    - Or pitch-scale in 4.12 or 8.8 fixed-point?
    - What are other valid values (half-speed = 0x800? double = 0x2000)?

### Message Dispatch Internals

17. **What hash algorithm is used for message name hashing?**
    - Decompile the hash function
    - CRC32 polynomial?
    - FNV-1a implementation?
    - Custom hash with magic numbers?
    - Collision handling strategy?

18. **Is there a reverse lookup table (msgID → name)?**
    - For debugging purposes
    - Would explain keeping string literals in memory
    - Find any const string tables referencing "iMsg*"

19. **What is the dispatch table data structure?**
    - std::map, std::unordered_map, custom hash table?
    - Load factor and bucket count?
    - Allocation strategy?

20. **What does paramType encode?**
    - Type enum (int, float, string, struct)?
    - Size of parameter structure?
    - Validation function pointer?
    - Examples from known messages?

21. **How are message handlers unregistered?**
    - Is there an UnregisterMessageHandler function?
    - Called during subsystem shutdown?
    - Cleanup on dest object destruction?

### Scene Management Details

22. **When and how are the three scene IDs loaded?**
    - Loaded from which files?
    - During InitGameSubsystems or later?
    - Hardcoded or data-driven?
    - Scene ID 0 meaning (special case or null)?

23. **What is the structure of the scene listener list?**
    - Linked list, array, or std::vector?
    - Node structure: {callback, context, priority, next}?
    - How is it sorted (priority-based)?

24. **What does the pending-change flag at listener_list.head+0x12 specifically control?**
    - Byte, word, or dword flag?
    - Set by async scene loading functions?
    - Cleared after FlushDeferredSceneListeners?
    - Prevent recursive scene switches during callback?

25. **What are all the scene types used in the game?**
    - Enumerate by searching for scene ID assignments
    - Menu, gameplay, cutscene, loading, credits?
    - Scene transitions: which can switch to which?

### Deferred Render Queue Structure

26. **What is the complete structure of RenderBatchNode?**
    - Size of each node?
    - All fields before next pointer at +0x7c?
    - Shader ID, material, geometry, transform?
    - Flags (alpha, depth, cull mode)?

27. **How is shader type recognition implemented in BuildRenderBatch?**
    - String comparison on shader name?
    - Hash comparison (Hash("BLOOM"))?
    - Enum stored in shader metadata?
    - Find the actual comparison logic

28. **What are ALL shader types beyond BLOOM, GLASS, BACKDROP?**
    - Search for other type strings/hashes
    - OPAQUE, ALPHA_BLEND, SHADOW_CAST, SKY, WATER, TERRAIN?
    - Render order: opaque → alpha → post-process?

29. **How is the 2ms budget measured and enforced?**
    - QueryPerformanceCounter calls?
    - Start time captured, delta checked in loop?
    - What happens on timeout: break and continue next frame?
    - Is budget configurable or hardcoded?

30. **How are render batches actually rendered?**
    - After ProcessDeferredCallbacks, what function renders them?
    - D3D API calls: SetVertexShader, SetPixelShader, DrawIndexedPrimitive?
    - Batch merging optimization?

### Subsystem Structure Details

31. **What is the complete layout of GlobalTempBuffer (0x3c bytes)?**
    - Map all 60 bytes with field descriptions
    - Callback function/context pairs confirmed at which offsets?
    - What are the "unknown_header[3]" bytes?
    - Additional data beyond callbacks?

32. **What methods are in RealGraphSystem vtable (PTR_FUN_00885010)?**
    - AddNode, RemoveNode, Traverse, Render?
    - Scene graph operations?
    - How many methods total?

33. **What is the exact layout of Locale (0x8c bytes)?**
    - Vtable, current language, string tables, count?
    - How are string tables indexed?
    - String table file format (.loc, .lang)?

34. **What operations are in the Locale vtable?**
    - GetString(id), SetLanguage(lang), LoadStringTable(file)?
    - Runtime language switching support?

35. **What video codec does FMV use (Bink vs Smacker)?**
    - Check for imported functions from binkw32.dll or smackw32.dll
    - Or in-house codec with custom functions?
    - FMV file extensions (.bik, .smk, custom)?

36. **What happens to cutscenes when 'nofmv' is set?**
    - Skip and advance story state?
    - Show static image or black screen?
    - Still play audio track?
    - Find the skip logic

37. **What methods are in GameServices vtable?**
    - Enumerate all GetXXXManager functions
    - How many subsystems are registered?
    - Lazy initialization or all created upfront?

38. **What methods are in TimeManager vtable?**
    - GetGameTime, GetRealTime, Pause, Resume?
    - Delta time calculation?
    - Time scaling support (slow-mo, fast-forward)?

39. **How does InitLanguageResources (FUN_00eb87ba) load string tables?**
    - File paths constructed how?
    - Binary format or text (XML, JSON, INI)?
    - Decompression (zlib, custom)?
    - Language selection from registry or command-line?

40. **How does InitVideoCodec (FUN_00eb88b2) initialize the decoder?**
    - License key check for Bink?
    - Codec DLL loading (LoadLibrary)?
    - Decoder context allocation?
    - Supported resolutions and formats?

41. **What does FinalizeRenderInit (FUN_006677c0) do?**
    - Shader compilation or loading precompiled?
    - Texture manager initialization?
    - Render state cache setup?
    - D3DX helper initialization (D3DXCreateFont, D3DXCreateSprite)?

### Memory Allocator Deep Dive

42. **What is the complete structure of EngineAllocator?**
    - Size of entire structure?
    - Fields before free_lists at +0x428?
    - Fields between +0x428 and critical_section at +0x4e4?
    - Total allocated, peak usage, alloc count stats?

43. **How are the 254 free list buckets sized?**
    - Bucket[0] = 8 bytes, [1] = 16, [2] = 24, ... linear?
    - Or exponential: [0]=8, [1]=16, [2]=32, [3]=64, ... ?
    - Maximum allocation size (bucket[253])?

44. **What is the 8-byte alignment mask (& 0x7ffffff8) used for?**
    - Ensures all allocations 8-byte aligned?
    - Stores metadata in low 3 bits?
    - Bit 0-2 = flags (allocated, tagged, etc.)?

45. **Which threads use the shared allocator?**
    - Main thread, audio thread, render thread?
    - Network thread (if multiplayer)?
    - Streaming thread for level loading?

46. **What stats does AllocEngineObject track per tag?**
    - Total allocated bytes?
    - Current bytes (allocated - freed)?
    - Allocation count?
    - Peak usage?
    - Leak report at shutdown?

47. **Is there automatic vtable setup based on tag?**
    - Type registry: "ClassName" → vtable pointer?
    - Or caller sets vtable manually after alloc?
    - Find type registry structure if it exists

48. **How does QueryMemoryAllocatorMax relate to AllocEngineObject?**
    - Decompile QueryMemoryAllocatorMax
    - Does it return peak usage from AllocEngineObject stats?
    - Or system-wide memory query?

### Timing System Implementation

49. **What is the exact value of g_ullCallbackInterval?**
    - Decompile initialization code
    - Is it 16ms (60 FPS) or 33ms (30 FPS)?
    - Configurable via registry or command-line?
    - Fixed-point format confirmed as 16.16?

50. **How is frame time interpolation factor calculated?**
    - Decompile InterpolateFrameTime function
    - Alpha = (currentTime - lastUpdateTime) / interval?
    - Clamped to [0.0, 1.0]?
    - Used for which rendering calculations?

51. **Why is game tick incremented by localTick * 3?**
    - Is localTick in milliseconds and game ticks in sub-ticks?
    - Verify actual values passed to UpdateFrameTimingPrimary
    - Decompile tick calculation logic
    - Connection to physics step rate?

52. **What triggers TimeManager pause (sets +4 field non-zero)?**
    - Pause menu opens?
    - Find all write references to g_pTimeManager+4
    - Focus loss vs explicit pause?
    - Cutscene playback?

53. **What methods are in DAT_008e1644 callback table beyond [0] and [1]?**
    - More than 2 entries?
    - Tertiary callback for cleanup or logging?
    - Find array size and enumerate all

### Input System Details

54. **What is the complete structure of RealInputSystem (0x34 bytes)?**
    - Map all 52 bytes
    - Device manager sub-object size and structure?
    - Event queue size (16 events * how many bytes each)?
    - Buffered vs immediate input mode?

55. **What is the structure of the device manager sub-object at +0xc?**
    - Manages keyboard, mouse, joystick
    - Device enumeration state machine?
    - Hotplug detection for joysticks?

56. **What is the structure of InputEvent?**
    - Type (keyboard, mouse, joystick)?
    - Device ID, button/key code, state (down/up)?
    - Timestamp, delta (for mouse movement)?
    - Size of one event?

57. **How does Ordinal_5 custom cursor work?**
    - Find the imported DLL
    - Decompile wrapper function
    - D3D SetCursorProperties or sprite rendering?
    - Cursor texture/image storage location?

58. **How does RealInputSystem coordinate with system cursor?**
    - ShowCursor calls when Ordinal_5 changes?
    - Exclusive mode (hide system, show game) or both?
    - Windowed vs fullscreen behavior difference?

### Configuration Deep Dive

59. **What is the exact value format of g_AspectRatio (DAT_008ae1dc)?**
    - Float (4 bytes), double (8 bytes), or fixed-point?
    - Verify size by checking read operations
    - What values for 4:3, 16:9, 16:10?
    - Is there a 21:9 ultrawide option?

60. **What is the complete std::basic_string layout in MSVC 2005?**
    - Verify 24-byte size
    - SSO buffer size (15 or 16 chars)?
    - Alignment requirements?
    - Are there debug build vs release build differences?

61. **What registry fallback order is used?**
    - HKCU → HKLM → create with default
    - Write-back to HKCU if found in HKLM?
    - Any HKEY_CLASSES_ROOT or HKEY_USERS checks?

62. **What are ALL command-line flags beyond the known ones?**
    - Known: fullscreen, widescreen, oldgen, showfps, memorylwm, nofmv
    - Search for all ParseCommandLineArg calls
    - Debug flags (e.g., nocollision, godmode, unlockall)?
    - Developer/QA flags?

### Performance and Optimization

63. **How is memory low-water mark used for quality scaling?**
    - Find all reads of g_bMemoryLowWaterMark and g_LowWaterMarkValue
    - What decisions are made based on value?
    - Dynamic texture quality reduction?
    - LOD (level of detail) adjustments?

64. **What does HandleLowTextureMemory (FUN_00ebe85b) do exactly?**
    - Decompile function
    - Texture cache flush algorithm?
    - Mip level reduction?
    - User notification (dialog box)?

65. **How is FPS calculated for the showfps overlay?**
    - Rolling average over how many frames?
    - Delta time accumulation?
    - Displayed via D3DXFont or custom text rendering?
    - Update frequency (every frame or periodic)?

66. **What profiling/instrumentation exists beyond debug tags?**
    - Frame time breakdown available?
    - GPU timing queries?
    - Subsystem performance counters?
    - Log file output for profiling data?

### Build and Debug

67. **What is stored in g_DebugSentinel beyond the 0xcd pattern?**
    - Is it ever read or just written?
    - Find all references to DAT_008d3878
    - Is it checked at shutdown?
    - Part of debug heap corruption detection?

68. **Can we extract the exact Visual Studio version from the PE?**
    - Rich header analysis
    - CRT version strings embedded?
    - Linker version (link.exe /version)?
    - Debug symbols (PDB) metadata?

69. **Are there other debug patterns (0xdd, 0xfd) in the binary?**
    - Search for repetitive byte patterns
    - Dead memory tracking?
    - Guard page implementations?

70. **What compiler optimizations were used?**
    - /O2 (maximize speed) or /O1 (minimize size)?
    - Inlining aggressive or conservative?
    - Whole program optimization (LTCG)?
    - Affects decompilation accuracy

### Render Dispatch Table

71. **What are ALL methods in g_pRenderDispatchTable (DAT_00c8c580) vtable?**
    - [0] Initialize, [8] Finalize, what else?
    - BeginFrame, EndFrame, Present?
    - SetCamera, SetLight, SetMaterial?
    - DrawPrimitive, DrawIndexedPrimitive?
    - Total method count?

72. **How does the render dispatch table relate to D3D device?**
    - Wrapper around IDirect3DDevice9?
    - Additional abstraction for multi-platform?
    - State caching layer?

### Unidentified Functions

73. **What do the unknown init functions actually do?**
    - FUN_00eb87ba (after Locale setup) - confirmed language resources?
    - FUN_00eb88b2 (after FMV setup) - confirmed video codec?
    - FUN_006677c0 (end of InitEngineObjects) - confirmed render finalize?
    - Decompile all three and identify operations

### Cross-Cutting Concerns

74. **Is there exception handling (C++ try/catch)?**
    - Find __CxxFrameHandler entries
    - What exceptions are caught?
    - Error recovery strategy?

75. **Is there a logging system?**
    - Log file written to disk?
    - OutputDebugString for dev builds?
    - Log levels (error, warning, info, debug)?

76. **What networking/multiplayer exists?**
    - Any network sockets or libraries imported?
    - Multiplayer game modes?
    - Or single-player only?

77. **What scripting system is used?**
    - Lua, Python, or custom?
    - Script file loading (.lua, .py, .script)?
    - Integration with message dispatch?

78. **Is there a resource manager/virtual file system?**
    - .pak, .zip, or custom archive format?
    - File loading abstraction?
    - Streaming support for large assets?

## Next Steps for Iteration 6

1. **Run Ghidra scripts:**
   - Execute ExploreIter5.java to analyze structures
   - Execute AnnotateIter5.java to add comprehensive comments
   - Review output for answers to above questions

2. **Decompile specific functions:**
   - Focus on functions mentioned in questions (hash algo, init functions, etc.)
   - Analyze assembly for struct layouts
   - Map vtables completely

3. **Implement C++ prototypes:**
   - Based on structure understanding, implement:
     - Frame callback system
     - Message dispatch
     - Audio command queue
     - Scene management
     - Deferred render queue
   - Compile and validate

4. **Compare behavior:**
   - Run original hp.exe with logging
   - Run C++ decompilation with equivalent logging
   - Compare initialization order, memory layout, timing

5. **Document findings:**
   - Update ARCHITECTURE.md with answers
   - Update CPP-ARCHITECTURE.md with C++ implementation details
   - Create DIFFERENCES.md comparing original vs decompiled behavior

## Priority Questions

**High Priority (Critical for C++ implementation):**
- Q1: Engine object field offsets
- Q11-13: Audio command queue structure
- Q17: Message hash algorithm
- Q23: Scene listener list structure
- Q26-27: Render batch node structure
- Q42-44: Memory allocator details
- Q49: Exact callback interval value

**Medium Priority (Improves accuracy):**
- Q7-10: DirectX parameters
- Q31-41: Subsystem structures
- Q54-57: Input system details
- Q71-72: Render dispatch vtable

**Low Priority (Nice to have):**
- Q2: Other magic numbers
- Q62: All command-line flags
- Q68: Exact VS version
- Q73-78: Cross-cutting concerns

---

## Iteration 8: High-Level Game Systems Questions

### Spell System

**Q79: How does the spell gesture recognition work?**
- Is it mouse path matching?
- Distance-based pattern recognition?
- Time-based sequence detection?
- What is the tolerance for inaccurate gestures?
- Where is the gesture data stored?

**Q80: What determines GOOD_CAST vs SUCCESSFUL_CAST?**
- Accuracy threshold?
- Speed of gesture?
- Context-based (enemy hit, puzzle solved)?
- Multiple success levels?

**Q81: Where is the spell list data loaded from?**
- Is `10.4_SpellList` a file path?
- What format (text, binary, XML)?
- How are spell parameters defined (damage, range, cooldown)?

**Q82: How does the wand discipline system work?**
- How are zones marked as WANDOUTPUNISH vs WANDOUTNOTPUNISH?
- What is the punishment mechanism?
- Which NPCs enforce the rules?
- Is there a warning before punishment?

**Q83: How is spell audio synchronized?**
- Pre-cast audio (`AudioWingardiumCast`)?
- During-cast audio?
- Impact/effect audio?
- How is 3D audio positioning handled?

**Q84: What is SPELL_FINITE used for?**
- Cancel all active spells?
- Counter-spell mechanics?
- End-of-combat cleanup?

### Physics System (Havok)

**Q85: How was the Havok version upgraded?**
- Was data migrated from 3.x to 4.0?
- Are there version-specific code paths?
- Where is the version check performed?
- Does it affect save game compatibility?

**Q86: What are the 32 collision groups used for?**
- Player vs environment?
- NPC vs NPC?
- Spell projectiles vs geometry?
- Trigger volumes?
- What is the collision matrix?

**Q87: How is the MOPP data generated?**
- At asset export time (RenderWare)?
- Runtime generation?
- Pre-computed and cached?
- How large are MOPP trees?

**Q88: What objects use keyframed motion?**
- Moving platforms?
- Elevators?
- Animated doors?
- Moving staircases (Grand Staircase)?
- Cutscene-driven objects?

**Q89: How does the deactivation system work?**
- What triggers sleep?
- How long before deactivation?
- Spatial partitioning details?
- Wake-up criteria (proximity, collision)?

**Q90: How is Havok integrated with the game loop?**
- Fixed timestep or variable?
- Substeps for stability?
- Where in the frame callback system?
- Synchronization with render thread?

### Zone Streaming

**Q91: How are the 4 load zones selected?**
- Player position-based?
- Directional prediction (where player is heading)?
- Priority system?
- Manual level design placement?

**Q92: What is the load/unload latency?**
- How long does a zone take to load?
- Asynchronous or blocks main thread?
- Progress tracking?
- Fallback if loading too slow?

**Q93: How does HybridLoadZone differ from regular LoadZone?**
- "Hybrid" = seamless + loading screen?
- Different asset priorities?
- Transition animation system?

**Q94: What happens if player moves too fast?**
- Unloaded zone entered before loading complete?
- Forced loading screen?
- Invisible walls?
- Slow-down mechanics?

**Q95: How much memory is allocated per zone?**
- Fixed budget?
- Dynamic based on zone complexity?
- How is memory freed on unload?

### Asset Loading

**Q96: What is the RCB file format structure?**
- Header layout?
- Animation keyframe encoding?
- Compression method?
- Bone hierarchy data?

**Q97: How does pre-allocation work?**
- Header buffer size calculation?
- Body buffer size calculation?
- Where are sizes stored (in file header)?
- Fallback if pre-allocation fails?

**Q98: What is the "Babble" system?**
- Dialogue trees?
- Subtitle data?
- Lip-sync data?
- Localization handling?

**Q99: How are Hull resources used?**
- Physics collision hulls?
- Simplified geometry for collision?
- Per-LOD hulls?
- Dynamic vs static hulls?

**Q100: What are Spline resources for?**
- Camera paths?
- NPC patrol routes?
- Object animation paths (flying keys, etc.)?
- Cutscene camera splines?

**Q101: How does Trinity Sequencer work?**
- Scripting language or binary format?
- Event timeline?
- Actor control (camera, NPCs, props)?
- Integration with gameplay systems?

### Animation

**Q102: How many blend shapes per character?**
- Facial expression count?
- Lip-sync phonemes?
- Damage deformations?

**Q103: How are blend shapes weighted?**
- Animator keyframes?
- Procedural (e.g., lip-sync from audio)?
- Player input (e.g., facial expressions in cutscenes)?

**Q104: What does blendSpecularTint do?**
- Modify specular color per blend shape?
- Used for sweating, blushing?
- Per-vertex or per-material?

**Q105: How is skeletal skinning performed?**
- Vertex shader or CPU?
- Matrix palette size (max bones)?
- Dual quaternion skinning or linear blend?

**Q106: What is the RCB animation compression?**
- Keyframe reduction?
- Quaternion compression?
- Curve fitting?

### AI System

**Q107: How are personality states selected?**
- Random on spawn?
- Scripted per-NPC?
- Time-of-day based?
- Influenced by player actions?

**Q108: How does the patrol system work?**
- Cycle through locators in order?
- Random locator selection?
- Wait time at each locator?
- Reaction to player proximity?

**Q109: What triggers chase behavior?**
- Line-of-sight?
- Proximity?
- Audio cues (spell cast, running)?
- Alert propagation between NPCs?

**Q110: How does the sidle system work?**
- Automatic transition at ledge edge?
- Player input required?
- How is "Agent Can Sidle" capability checked?
- Ledge detection algorithm?

**Q111: What are mini-game AI parameters?**
- NifflerTimeAttackMiniGameInfo contents?
- Difficulty scaling?
- Scoring system?

**Q112: How does broadPhaseBorderBehaviour work?**
- Physics boundary handling?
- Teleport back vs collision?
- Warning system before boundary?

### UI System

**Q113: What does UI versioning mean (2.0_, 4.0_)?**
- Different layouts for different platforms?
- Patched versions?
- A/B testing remnants?

**Q114: How are 32 buttons mapped?**
- What does button 25-32 represent?
- Keyboard + gamepad combined?
- Touch screen support?

**Q115: How does the message system work?**
- MSG_ IDs in a lookup table?
- Event queue?
- Priority system?

**Q116: How is the UI rendered?**
- Separate UI render pass?
- Immediate mode or retained mode?
- Texture atlas for UI elements?

**Q117: What is the memory card icon system?**
- PS2 legacy code?
- Still used on PS3/Xbox 360?
- Icon format?

### Cross-System Questions

**Q118: How do spells interact with physics?**
- Spell projectiles as rigid bodies?
- Force application on hit?
- Havok collision filters for spells?

**Q119: How does AI react to spells?**
- Dodge behavior?
- Counter-spell?
- Fear/flee state?
- Damage calculation?

**Q120: How are animations synchronized with physics?**
- Root motion from animation?
- Physics affects animation (ragdoll)?
- Blend between animation and physics?

**Q121: How does streaming affect AI?**
- AI unloaded with zone?
- AI state saved/restored?
- Patrol routes spanning multiple zones?

**Q122: How do cutscenes interact with all systems?**
- Disable physics?
- Override AI?
- Lock player input?
- Trinity Sequencer control?

**Q123: How is the Havok-RenderWare pipeline integrated?**
- Exporter plugins?
- Runtime conversion?
- Data validation?

### Investigation Strategy for Iteration 9

1. **Spell gesture recognition:**
   - Search for mouse input tracking
   - Look for pattern matching algorithms
   - Find spell parameter tables

2. **Havok physics integration:**
   - Analyze hkPhysicsSystem creation
   - Find collision group setup
   - Locate physics step function

3. **Zone streaming manager:**
   - Find streaming state machine
   - Analyze load zone selection
   - Memory budget management

4. **Animation system:**
   - Analyze RCB file parsing
   - Find blend shape application
   - GPU skinning shader code

5. **AI state machine:**
   - Find AI update loop
   - Analyze state transition logic
   - Sidle system implementation

6. **UI rendering:**
   - Find UI render function
   - Button mapping table
   - Message dispatch system

### Priority Questions for C++ Implementation

**Critical:**
- Q79: Spell gesture recognition (core gameplay)
- Q90: Havok integration with game loop
- Q91-94: Zone streaming mechanics
- Q107-110: AI system architecture

**High:**
- Q96: RCB format (needed for animation)
- Q101: Trinity Sequencer (cutscenes)
- Q118-120: Cross-system interactions

**Medium:**
- Q81-84: Spell system details
- Q85-89: Havok advanced features
- Q102-106: Animation details
- Q113-117: UI implementation

**Low:**
- Q111: Mini-game AI
- Q112: Border behavior
- Q121-123: Advanced integrations
