# Questions for Further Exploration (Iteration 2)

## DirectX Initialization Chain

1. What is `thunk_FUN_00eb5c3e()`?
   - Called in WinMain after window creation
   - Returns a COM-like pointer used to create DAT_00bef6d0
   - Likely initializes DirectX or some engine sub-system
   - DAT_00bef6d0 is released via vtable method +0xc on exit

2. What is `thunk_FUN_00eb612e(height)`?
   - Called with window client height after DirectX creation
   - Returns 0 on failure (causes TerminateProcess)
   - Likely creates the DirectX device or swap chain

3. What is `thunk_FUN_00eb496e()`?
   - Called right before setting DAT_00bef6c6 = 1
   - Likely completes DirectInput initialization

4. What initializes `DAT_00bf1924`?
   - Used in RestoreDirectXResources as `IDirect3D9*` or swap chain
   - Called via vtable +0x14 to get back buffer
   - Where is it first set?

5. What does `FUN_00684f30()` do?
   - Called from InitRenderStates unconditionally
   - Likely sets up some D3D state or caps check

6. What determines `DAT_00bf1994`?
   - Shader/capability level index
   - If > 2, extended state init path (FUN_00685410) is taken
   - Set during device creation?

7. What is `DAT_00bf19a8` and the capability check at `0x474 + DAT_00bf19a8`?
   - Used in InitD3DStateDefaults to check texture op capabilities
   - Offset 0x474 in what structure? D3DCAPS9?

## Game State Object (`DAT_00e6b384`)

8. What is the game state object at `DAT_00e6b384`?
   - Used in WindowProc and MainLoop
   - Field at +0xc: pointer to another object
   - On that sub-object: vtable +0x10 returns something, then +0x218 gives input device
   - What class is this? A scene/world manager?

9. What is `DAT_00e6b2dc`?
   - Used in WindowProc focus loss: `*(int**)(DAT_00e6b2dc + 4)`
   - Points to an object with a flag at +0xa0
   - Checked before calling PauseAudio

10. What is `DAT_00c7b908`?
    - Compared to 1 before calling PauseGameUpdates
    - If == 1, the call is skipped
    - Game state enum? Pause mode?

## Timing System

11. What is the exact tick frequency / callback interval?
    - `DAT_00c83190/94` is the callback interval (64-bit)
    - What value is it set to? How is it calculated?
    - Where is it initialized?

12. What triggers `DAT_008e1644` callbacks?
    - Primary callback: `(*DAT_008e1644[0])(&localTick)`
    - Secondary callback: `(*DAT_008e1644[1])()`
    - What systems register these callbacks? Renderer? Physics?

13. What are the frame rate constants?
    - `DAT_008475d8`: float constant for frame budget calc
    - `DAT_00845594`: float constant
    - `DAT_00845320`: float constant
    - Together they compute target frame period. What values?

14. What is `DAT_00c83188`?
    - Used in GameFrameUpdate: `uVar5 = DAT_00c83188 + _DAT_00c831a0`
    - Also read in MainLoop for frame timing calculation
    - Is this a per-frame time accumulator?

## Scene Management

15. What are scene IDs `DAT_00c82b00`, `DAT_00c82b08`, `DAT_00c82ac8`?
    - Compared in MainLoop for render mode switching
    - How many scene IDs exist? Are they enums?
    - When does DAT_00c82b08 ≠ DAT_00c82ac8? (triggers render mode change)

16. What does `SwitchRenderOutputMode (FUN_00612530)` do?
    - Called with a pointer to a scene ID
    - Does it switch between in-game / pause screen / loading screen?

## Audio System

17. What is `PauseAudio (FUN_0061ef80)` and its counterpart?
    - Called on focus loss; audio resumes via `thunk_FUN_00ec67e8()`
    - Is this streaming audio, music, or all audio?

18. What COM object is `DAT_00bef6d0`?
    - Created via `(*(code*)**(undefined4**)(iVar12+4))(0xb58, &uStack_d0)`
    - `uStack_d0 = 0x88332000000001`
    - Released via vtable +0xc on exit
    - Could be a DirectSound or XAudio2 interface?

## Game Update System

19. What does `thunk_FUN_00eb6dbc()` return?
    - Result stored in `DAT_008afb08` (minimum over frames)
    - Called each frame when `DAT_00bef6d7` is set
    - Available video memory? Frame counter?

20. What is `FUN_00612f00()`?
    - One-shot initialization with `_atexit(FUN_007b5640)` cleanup
    - Only called when DAT_00bef6d7 (game update enabled) is set
    - What system does it initialize?

21. What is `FUN_0063d600(node)`?
    - Called in ProcessDeferredCallbacks per node
    - Returns 1 when node's work is done
    - What kind of work items are queued? Particle spawning? Animation events?

## Registry and Settings

22. Are there additional registry sections beyond `GameSettings`?
    - The registry path supports any section name
    - Are there sections like `AudioSettings`, `ControlSettings`?

23. What is `ReadRegistrySettingStr (FUN_0060ca20)` used for besides placement?
    - Used for Maximized/Minimized (string comparison to "true")
    - Are there other string settings?

## Missing Analysis

24. What initializes the DirectInput devices?
    - `DAT_00e6a070` (keyboard), `DAT_00e6a194` (mouse), `DAT_00e6a42c` (joysticks)
    - Where is DirectInput8Create called?

25. What is `FUN_0067d2e0` (PreDeviceCheck)?
    - Called before every TestCooperativeLevel
    - Likely flushes GPU query or state

26. What is `DAT_00bf1980`?
    - Compared to 0x10 in InitD3DStateDefaults
    - If 0x10 (16-bit colour depth?), blend state path changes

27. What is `DAT_008ae1fc`?
    - Checked in CreateGPUSyncQuery: controls swap effect flag
    - Is this a capability flag from device caps?

28. What is `FUN_0067ecf0(local_c, 0)` in ReleaseDirectXResources?
    - Called with a surface pointer before releasing it
    - Likely unregisters or clears the surface from some cache
