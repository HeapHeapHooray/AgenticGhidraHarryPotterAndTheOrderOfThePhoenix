# Questions for Further Exploration (Iteration 3)

## DirectX Initialization Chain

1. What does `thunk_FUN_00ec64f9()` do?
   - Called in WinMain immediately after creating DAT_00bef6d0, before InitDirectXAndSubsystems
   - Possible: initializes the engine object created in the previous step

2. What is `DAT_00bef6d0` exactly?
   - Created via `(*DAT_00e6e874[0])(0xb58, {0x88332000000001, 0})`
   - 2904-byte allocation through callback manager secondary factory
   - Released via `(*(callback_mgr + 0xc))(DAT_00bef6d0, 0)` — NOT a vtable method call
   - What class does this correspond to? Some scene graph root?

3. What is `FUN_0067c290` (CreateD3DDevice)?
   - Called from InitCLIAndTimingAndDevice: `FUN_0067c290(height, ?, 0, 0, 6, 1, 1, 1, 1)`
   - 9 parameters — what are they all?
   - Returns D3DERR-like error code
   - Where does it create the IDirect3DDevice9 and store DAT_00bf1924?

4. What is `FUN_0067bb20()` in RestoreDirectXResources?
   - Called after CreateGPUSyncQuery with no obvious side effects
   - The current analysis says "empty stub" but may do something in some paths

5. What is `thunk_FUN_00eb4a5d()` in WinMain cleanup?
   - Called after RenderAndAudioTeardown, before input pause on exit
   - Unknown purpose — audio or resource cleanup?

6. What does `thunk_FUN_00ec04dc()` do in ReleaseDirectXResources?
   - Called BEFORE surface releases — likely notifies the render pipeline
   - And `thunk_FUN_00ec19b5()` called AFTER — confirms pre/post cleanup pattern

## Audio System

7. What is the full audio init chain in FUN_006109d0?
   - Used as the polling gate in all async audio operations
   - Returns -2 on error, 1 on success, 0 while pending
   - What event/condition does it wait for?

8. What is `thunk_FUN_00ec693d(state)` (audio decoder control)?
   - Called with 0 on pause, 0x1000 on resume
   - The 0x1000 value matches the "normal pitch" constant — likely seek/speed control
   - Is this a DirectSound or custom streaming decoder call?

9. What is `thunk_FUN_00ec66f1()` (audio finalize)?
   - Called after decoder state change in both pause and resume

10. What does `FUN_006ace30()`, `FUN_006108c0()`, `FUN_006ac930()` do in RenderAndAudioTeardown?
    - Three audio/resource teardown calls made sequentially
    - What each one releases?

11. What does `DAT_00bf1b10` guard in InitAudioSubsystem?
    - If zero: returns true (no audio) without initializing
    - Is this a "disable audio" flag set by hardware caps or command line?

## Engine Object System

12. What is `FUN_00614210` (AllocEngineObject)?
    - Called as `AllocEngineObject(size, tag_string)` throughout init
    - Returns a heap pointer, stores vtable + wires things up
    - Is this a simple malloc wrapper or does it also do vtable setup?

13. What is `DAT_00e6b378` (GlobalTempBuffer)?
    - Used in InitEngineObjects as a temp buffer for something
    - Field at +0xd = callback count (max 5); pairs at +3 and +4
    - What are these callback pairs? Render hooks?

14. What is `DAT_00e6b390` (RealGraphSystem)?
    - Vtable: PTR_FUN_00885010
    - Field +1 = &DAT_00e6e874 (secondary callback entry)
    - This is likely the render graph/scene manager

15. What is `thunk_FUN_00eb87ba()` and `thunk_FUN_00eb88b2()` in InitEngineObjects?
    - Called after locale and FMV setup respectively
    - Unknown purpose — could be networking or additional subsystems

16. What is `FUN_006677c0()`?
    - Called at the very end of InitEngineObjects
    - Unknown — possibly renderer or shader init

## Message System

17. What is `thunk_FUN_00eb59ce(dest, msgName, paramType)`?
    - Used in FinalizeDeviceSetup to register message handlers
    - How does this message dispatch system work?
    - Are message IDs looked up by name at runtime?

18. What does `FUN_0060c130` do (registered callback in InitGameSubsystems)?
    - Set as `_DAT_00e69ca0 = FUN_0060c130`
    - Called when? On what event?

## Scene / Render Mode

19. What are the actual scene ID values in `DAT_00c82b00/08/ac`?
    - They're set to 0 at startup — what populates them?
    - When does `DAT_00c82b08 != DAT_00c82ac8`? (triggers render mode change)
    - Is there an enum of scene types?

20. What does `FUN_006125a0()` do when called from SwitchRenderOutputMode?
    - Called when `list.head+0x12` pending-change flag is set
    - Is this a deferred listener flush?

## Timing

21. What is the callback interval (`DAT_00c83190/94`)?
    - Where is it initialized? What is its default value?
    - Is it set per-frame or only once at startup?

22. What is `DAT_00bef768` (checked in UpdateFrameTimingPrimary)?
    - `iVar1 = *(DAT_00bef768 + 4)` is checked before tick accumulation
    - If 0: updates DAT_00c83114 (tick counter); if non-zero: skips
    - What controls this flag? A paused/unpaused state?

## Registry / Command Line

23. What happens with the `std::basic_string` parameters in ReadRegistrySetting?
    - Original allocates temporary std::basic_string objects for each call
    - This creates/destroys C++ string heap objects on every registry read
    - The C++ decompilation uses plain const char* — is this close enough functionally?

## Missing Implementations

24. What calls `FUN_0063d600` (BuildRenderBatch / ProcessCallbackNode)?
    - What types of work items are in the deferred queue?
    - Are these shader batches, particle emitters, or something else?

25. What does `FUN_0060b740()` do (called in FinalizeDeviceSetup)?
    - Called after message handler registration
    - Possible: final renderer state setup or scene graph init

26. What is `DAT_00c8c580` (render dispatch table)?
    - Used in InitCLIAndTimingAndDevice and FinalizeDeviceSetup
    - `(*(*DAT_00c8c580))(0, 0)` — initial render dispatch
    - `(*(*DAT_00c8c580 + 8))()` — second dispatch in FinalizeDeviceSetup
