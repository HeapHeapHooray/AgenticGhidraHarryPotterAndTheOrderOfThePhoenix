# Questions for Further Exploration (Iteration 5)

## Summary of Iteration 4 Progress

Iteration 4 answered many questions from the previous iteration through pattern analysis and architectural inference. See `workspace/ANSWERS-ITER4.md` for detailed answers. Key findings:

- **Engine Object (DAT_00bef6d0):** Identified as main coordinator factory with magic type ID `0x88332000000001`
- **Audio System:** Documented async command queue pattern with polling gate
- **Message Dispatch:** String-based polymorphic dispatch system identified
- **Scene Management:** Three-ID system (focus-lost, focus-gained, current) understood
- **Render Queue:** Deferred batching by shader type (BLOOM, GLASS, BACKDROP)

## New Questions from Iteration 4 Analysis

### Engine Object Factory

1. **What is the full structure of the engine object at DAT_00bef6d0?**
   - Size: 0xb58 (2904 bytes)
   - Likely contains: vtable + subsystem pointers + state flags
   - Need to map out field offsets and their purposes
   - Which subsystems are directly referenced from this object?

2. **What is the magic number format `{0x88332000000001, 0}`?**
   - Is this a GUID/type identifier?
   - Is `0x883320` a version/build stamp?
   - Why is it a 16-byte value (two 64-bit parts)?
   - Are there other magic numbers for different object types?

3. **What is the custom destructor at `callback_mgr + 0xc`?**
   - Is this a vtable entry or function pointer field?
   - Does it handle reference counting?
   - What cleanup does it perform beyond simple free()?

### Callback Manager Internals

4. **What is the full vtable structure of the callback manager?**
   - `DAT_00e6e870` = primary entry (vtable `PTR_FUN_00883f3c`)
   - `DAT_00e6e874` = secondary entry (vtable `PTR_FUN_00883f4c`)
   - What other methods exist beyond factory creation?
   - How does the dual-entry design work?

5. **How does InitFrameCallbackSystem populate the callback slots?**
   - Clears `DAT_00e6e880..e6e89c` (8 slots)
   - When and how are these 8 slots filled?
   - What is the signature of callback functions?
   - Are there different callback priorities or phases?

### DirectX Device Creation

6. **What are all 9 parameters to CreateD3DDevice (FUN_0067c290)?**
   - `(height, ?, 0, 0, 6, 1, 1, 1, 1)`
   - Parameter 2: unknown purpose
   - Parameter 3-4: both 0 (flags? reserved?)
   - Parameter 5: 6 (quality/feature level?)
   - Parameters 6-9: all 1 (boolean flags?)
   - What do each of these control?

7. **How does the shader capability level (DAT_00bf1994) get set?**
   - Used in RestoreDirectXResources (>2 = extended shader path)
   - Determined by device caps query?
   - Maps to shader model versions (1.x, 2.0, 3.0)?

8. **What operations do thunk_FUN_00ec04dc and thunk_FUN_00ec19b5 actually perform?**
   - Current answer is "likely flushes/clears" — need specifics
   - Do they iterate over registered listeners?
   - Do they access global state tables?
   - Are these part of a larger resource notification system?

### Audio Command Queue

9. **What is the structure of the audio command queue at DAT_00be82ac?**
   - How are commands enqueued?
   - What fields does each command have? (opcode, params, callback, status)
   - Is it a ring buffer, linked list, or array?
   - Thread-safe implementation details?

10. **What is the relationship between audio thread (DAT_00bf1b30) and command queue?**
    - Is the thread created in PreDirectXInit via FUN_00611940?
    - Does thread loop on queue polling?
    - What triggers thread exit?

11. **What does FUN_006a91a0 return when it expects 0x80?**
    - Is 0x80 a format support flag?
    - Or a caps/features bitmask?
    - What happens on other return values?

### Message Dispatch Implementation

12. **How are message IDs hashed from strings?**
    - What hash algorithm? (CRC32, FNV, custom?)
    - Is there a string table for reverse lookup?
    - Are collisions handled?

13. **What is the dispatch table structure?**
    - Array vs hash table?
    - How many message types are registered?
    - Can handlers be unregistered dynamically?

14. **What is the paramType parameter in RegisterMessageHandler?**
    - Type descriptor for message payload?
    - Used for validation/marshalling?
    - Examples of different param types?

### Scene Management

15. **How and when are the scene IDs populated?**
    - All start at 0
    - Loaded from level data files?
    - Set by scripting system?
    - Who calls the scene loading functions?

16. **What is the scene listener list structure used by SwitchRenderOutputMode?**
    - How do listeners register?
    - What is the callback signature?
    - Priority/ordering of listener notification?

17. **What does the pending-change flag at list.head+0x12 indicate?**
    - Set during async scene load?
    - Prevents recursive scene switches?
    - Defers switch until safe point?

### Deferred Render Queue

18. **What is the complete structure of a render batch node?**
    - Known: next pointer at +0x7c
    - What other fields? (shader ID, material params, vertex range, transform)
    - How large is each node?

19. **How does shader type recognition work in BuildRenderBatch?**
    - String comparison? Enum? Hash?
    - Where is shader metadata stored?
    - Beyond BLOOM/GLASS/BACKDROP, what other types exist?

20. **Why 2ms budget for ProcessDeferredCallbacks?**
    - Is this configurable?
    - What happens if budget exceeded? (continue next frame, drop batches, warning?)
    - How is time measured? (GetGameTime vs QueryPerformanceCounter?)

### Subsystem Initialization

21. **What does GlobalTempBuffer (DAT_00e6b378) actually store?**
    - Size: 0x3c bytes
    - Field +0xd: callback count (max 5)
    - What are the callback pairs at +3 and +4?
    - Used for what temporary data?

22. **What is RealGraphSystem's (DAT_00e6b390) role?**
    - Only 8 bytes (vtable + pointer to DAT_00e6e874)
    - If it's a render graph/scene manager, where is the graph data?
    - Is this a proxy/handle to a larger structure?

23. **What language/localization system does Locale (DAT_00e6b304) implement?**
    - Size: 0x8c bytes
    - Handles which languages? (English, French, German, Spanish, Italian?)
    - String table format?
    - Runtime switching support?

### Frame Callback Chain

24. **What functions are stored in DAT_008e1644 callback table?**
    - `[0]` = primary callback (called with `&localTick`)
    - `[1]` = secondary callback (called with no args)
    - What are the actual function addresses?
    - Are there more than 2 entries?

25. **What does UpdateFrameTimingPrimary do with localTick parameter?**
    - Increments tick counter by `localTick * 3`
    - Why multiply by 3? (game runs at 3x real-time?)
    - What uses the tick counter?

26. **What is InterpolateFrameTime's output used for?**
    - Smooth interpolation between double-buffered values
    - Who reads the interpolated result?
    - Used for rendering smoothness?

### Render Dispatch Table

27. **What are all the methods in g_pRenderDispatchTable (DAT_00c8c580)?**
    - `[0]` = Initialize(int, int)
    - `[8]` = Finalize()
    - What other render operations? (BeginFrame, EndFrame, Present, SetCamera, etc.)
    - Is this the interface to the entire render subsystem?

### AllocEngineObject Details

28. **What tracking/profiling does AllocEngineObject implement?**
    - Debug tags for leak detection
    - Memory usage stats per tag?
    - Callstack capture?
    - Integration with QueryMemoryAllocatorMax?

29. **Does AllocEngineObject set up vtables automatically?**
    - Or does caller assign vtable after alloc?
    - Is there a type registry that maps tags to vtables?

### GameServices::Open

30. **What services does GameServices::Open provide?**
    - Only 1 byte — likely just a vtable pointer?
    - Service locator pattern suggests methods like:
      - GetAudioManager(), GetRenderer(), GetPhysics(), etc.
    - Or is it more of a capability flags byte?

### Timing System

31. **What is the actual frame callback interval value?**
    - Speculation: 16ms (60 FPS) or 33ms (30 FPS)
    - Need to verify actual initialization
    - Does it vary by platform or settings?

32. **What pauses the TimeManager (sets field +4 non-zero)?**
    - Pause menu?
    - Cutscenes?
    - Alt-tab / focus loss?
    - Loading screens?

### Memory Allocator

33. **What is the structure of the free list at allocator+0x428?**
    - 0xfe entries (254)
    - Indexed by priority/size class?
    - How does 8-byte alignment mask (& 0x7ffffff8) work?

34. **What is the critical section at allocator+0x4e4 protecting?**
    - Free list modifications?
    - Is allocator used from multiple threads?
    - Which subsystems share the allocator?

## Registry and Configuration

35. **How is the aspect ratio (DAT_008ae1dc) calculated from 'widescreen' flag?**
    - Set to a constant or computed?
    - Values for 4:3 vs 16:9?
    - Used where in rendering?

36. **What is the format of the std::basic_string used in registry code?**
    - Size of the string object (24 bytes for MSVC 2005?)
    - SSO (small string optimization) threshold?
    - Why use C++ strings internally if API is C-style?

## Performance and Optimization

37. **How does the 'memorylwm' flag low-water mark tracking help?**
    - Stored in DAT_008afb08
    - Used for what decisions? (quality scaling, warning dialogs?)
    - Does it affect DAT_00bef6d7 (game update enabled) behavior?

38. **What triggers the low texture memory handler (thunk_FUN_00ebe85b)?**
    - Called when 0 < available MB < 33
    - Does it reduce texture quality?
    - Flush caches?
    - Show warning to user?

## Input System

39. **What is the structure of RealInputSystem (DAT_00e6b384)?**
    - Size: 0x34 bytes
    - Field +0xc: pointer to device manager sub-object
    - What are the other fields?
    - How does it queue input events?

40. **What is Ordinal_5 custom cursor control?**
    - Called with (0) to hide, (1) to show
    - Different from ShowCursor()?
    - Game-specific cursor rendering?
    - D3D hardware cursor?

## Video/FMV System

41. **What codec does FMV (DAT_00e6b2dc) support?**
    - Bink, Smacker, or custom?
    - Size: 0x18 bytes (24 bytes) — vtable + few fields
    - Integration with audio system?
    - Disabled by 'nofmv' flag — what happens to cutscenes?

## Build and Debug

42. **What is the significance of the 0xcd sentinel in DAT_008d3878?**
    - Fills 0x4c bytes
    - Debug pattern (0xcd = uninitialized in MSVC debug heap)
    - Used to detect uninitialized state access?

43. **What Visual Studio version was used to build hp.exe?**
    - Evidence: CRT entry __tmainCRTStartup
    - String objects suggest VS 2005+
    - Can we determine exact version from binary?

## Next Steps for Iteration 5

1. Use Ghidra script `ExploreIter4.java` to decompile the newly-understood functions and examine their implementations in detail
2. Answer architectural questions by examining actual decompiled code
3. Build C++ implementation of the newly-understood subsystems (message dispatch, render queue, etc.)
4. Validate understanding by comparing C++ behavior to original
