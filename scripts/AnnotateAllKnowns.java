// Comprehensive annotation pass: rename all known functions and globals,
// and add detailed comments reflecting ARCHITECTURE.md research.
// Supersedes AddComments.java and AddCommentsIter3.java.
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.symbol.*;
import ghidra.program.model.mem.*;

public class AnnotateAllKnowns extends GhidraScript {

    private FunctionManager fm;
    private SymbolTable st;
    private Listing listing;

    @Override
    public void run() throws Exception {
        fm      = currentProgram.getFunctionManager();
        st      = currentProgram.getSymbolTable();
        listing = currentProgram.getListing();

        annotateFunctions();
        annotateGlobals();

        println("AnnotateAllKnowns: complete.");
    }

    // ── Functions ─────────────────────────────────────────────────────────────

    private void annotateFunctions() throws Exception {

        // ── Entry / WinMain ─────────────────────────────────────────────────

        fc("006f54d2", null,
            "CRT entry point (___tmainCRTStartup). Initializes the C runtime and calls WinMain.");

        fc("0060dfa0", null,
            "WinMain: main game entry point for Harry Potter and the Order of the Phoenix.\n" +
            "Initialization order:\n" +
            "  1. _control87(0x20000,0x30000) — disable denormal FP exceptions\n" +
            "  2. SaveOrRestoreSystemParameters(0) — save+disable mouse acceleration\n" +
            "  3. strncpy cmdline to DAT_00c82b88 and DAT_00c82d88\n" +
            "  4. CLI_CommandParser_ParseArgs() — parse -name=value tokens (00eb787a)\n" +
            "  5. FindWindowA single-instance guard\n" +
            "  6. RegisterWindowClass()\n" +
            "  7. LoadGameSettings() (registry)\n" +
            "  8. ParseCommandLineArg for 'fullscreen' and 'widescreen'\n" +
            "  9. CreateGameWindow() -> GetClientRect for client height\n" +
            " 10. GetOrInitCallbackManager() factory -> DAT_00bef6d0 (2904-byte engine object)\n" +
            " 11. PreDirectXInit() (00ec64f9)\n" +
            " 12. InitDirectXAndSubsystems(clientHeight) (00eb612e)\n" +
            " 13. InitGameSubsystems() (00eb496e); sets DAT_00bef6c6=1\n" +
            " 14. Restore Maximized/Minimized state; UpdateWindow\n" +
            " 15. MainLoop()\n" +
            " 16. Cleanup: RenderAndAudioTeardown, release DAT_00bef6d0 via callback mgr,\n" +
            "     SaveOptionsOnExit, pause RealInputSystem, UnacquireInputDevices,\n" +
            "     UpdateCursorVisibilityAndScene, SaveOrRestoreSystemParameters(1),\n" +
            "     TerminateProcess(hProc,1)");

        // ── Window management ────────────────────────────────────────────────

        fc("0060db20", "CreateGameWindow",
            "Creates the main game window using 'OrderOfThePhoenixMainWndClass'.\n" +
            "Params: (hInstance, width, height). Position read from registry inside.\n" +
            "Fullscreen: WS_POPUP, HWND_TOPMOST, SetMenu(NULL), SetThreadExecutionState.\n" +
            "Windowed: WS_OVERLAPPEDWINDOW, position from registry (default 300,32).\n" +
            "Sequence: CreateWindowExA -> ShowWindow(SW_HIDE) -> SetWindowPos -> ShowWindow(SW_SHOW).");

        fc("00eb4b95", "RegisterWindowClass",
            "Registers 'OrderOfThePhoenixMainWndClass'.\n" +
            "Calls UnregisterClassA first (handles crashed previous instances).\n" +
            "Fullscreen: CS_OWNDC|CS_DBLCLKS. Windowed: adds CS_VREDRAW|CS_HREDRAW.\n" +
            "Cursor: IDC_ARROW (windowed only). Background: black brush.");

        fc("0060d6d0", "WindowProc",
            "Main window procedure for OrderOfThePhoenixMainWndClass.\n" +
            "WM_DESTROY: show cursor in fullscreen, PostQuitMessage(0)\n" +
            "WM_SIZE/ERASEBKGND/ACTIVATEAPP: return 0 (suppressed)\n" +
            "WM_ACTIVATE:\n" +
            "  Focus loss: PauseGraphicsState, UnacquireInputDevices, show cursor,\n" +
            "    DAT_00bef6c7=1, UpdateCursorVisibilityAndScene(AL=1),\n" +
            "    PauseAudioManager, PauseGameObjects(0), DAT_00bef6d8=0\n" +
            "  Focus gain: RealInputSystem vtable acquire, AcquireInputDevices, hide cursor,\n" +
            "    UpdateCursorVisibilityAndScene(AL=0), DAT_00bef6c7=0, DAT_00bef6d8=2000\n" +
            "WM_SETFOCUS/WM_SETCURSOR: hide cursor and bind D3D cursor (or show arrow + PauseGraphicsState)\n" +
            "WM_PAINT: ValidateRect (fullscreen) or BeginPaint/EndPaint (windowed)\n" +
            "WM_CLOSE: windowed -> SaveWindowPlacement + DestroyWindow\n" +
            "WM_SYSCOMMAND: fullscreen blocks SC_MAXIMIZE/SIZE/MOVE/KEYMENU\n" +
            "WM_NCHITTEST: fullscreen -> HTCLIENT\n" +
            "WM_ENTERMENULOOP: fullscreen -> return 0x10000\n" +
            "WM_WTSSESSION_CHANGE: wParam 0/7 -> return 1; else -1");

        fc("0060d220", "SaveWindowPlacement",
            "Saves windowed-mode window state to registry on WM_CLOSE.\n" +
            "Uses GetWindowPlacement + AdjustWindowRect border correction.\n" +
            "Writes: PosX, PosY, SizeX, SizeY, Maximized, Minimized as REG_SZ.");

        // ── Main loop / timing ───────────────────────────────────────────────

        fc("0060dc10", "MainLoop",
            "Main game loop. Runs until WM_QUIT or exit flag (DAT_00bef6c5) set.\n" +
            "Per frame:\n" +
            "  1. UpdateDirectXDevice() — check/recover lost device\n" +
            "  2. Focus state: cursor/scene switch via SwitchRenderOutputMode\n" +
            "  3. PeekMessageA(PM_REMOVE) — dispatch messages or WM_QUIT -> exit\n" +
            "  4. No messages: GameFrameUpdate()\n" +
            "  5. If DAT_00bef6d7: lazy InitFrameCallbackSystem + QueryMemoryAllocatorMax tracking\n" +
            "  6. DAT_00bef6d8 countdown: on expiry, AudioStream_Resume + ResumeGameObjects\n" +
            "  7. Frame rate cap: Sleep(0) if elapsed < target budget");

        fc("00618010", "GetGameTime",
            "High-resolution game timer using Windows multimedia API.\n" +
            "timeGetDevCaps + timeBeginPeriod(min) + timeGetTime + timeEndPeriod.\n" +
            "First call: captures startup baseline in DAT_00e6e5e8 (init flag in DAT_00e6b388).\n" +
            "Returns milliseconds since first call (game startup time).");

        fc("00618140", "GameFrameUpdate",
            "64-bit 16.16 fixed-point frame timing update.\n" +
            "1. ProcessDeferredCallbacks() — run deferred render batch queue (2ms budget)\n" +
            "2. GetGameTime() -> 16.16 fixed: (high=t>>16, low=t<<16)\n" +
            "3. First call: init DAT_00e6e5e0/e4 = (low,high); init next-callback time\n" +
            "4. Delta = current - last, capped at 0x640000 (100ms in 16.16)\n" +
            "5. Accumulate DAT_00c83198/9c += delta\n" +
            "6. DAT_00c83110 = accum * 3 / 0x10000 (game ticks, 3x speed)\n" +
            "7. If accum >= DAT_00c831a8/ac (next callback):\n" +
            "   - Advance next callback by DAT_00c83190/94 (interval)\n" +
            "   - Toggle DAT_00c83130 (frame flip index)\n" +
            "   - Call UpdateFrameTimingPrimary(&localTick) + (*DAT_008e1644[0])(&localTick)\n" +
            "   - Call InterpolateFrameTime() + (*DAT_008e1644[1])()");

        fc("00617f50", "UpdateFrameTimingPrimary",
            "Updates timing double-buffer when frame callback interval fires.\n" +
            "If DAT_00bef768+4 == 0: increments DAT_00c83114 tick counter by localTick*3.\n" +
            "Toggles DAT_00c83130 (frame flip index).\n" +
            "DAT_00c83128[flip] = localTick; DAT_00c83120[flip] = game time.\n" +
            "Uses constants DAT_00845594 (0.001) and DAT_00845320 (1000.0).\n" +
            "Called from GameFrameUpdate as the primary frame callback.");

        fc("00617ee0", "InterpolateFrameTime",
            "Smooth frame time interpolation between double-buffered timing values.\n" +
            "Reads current/previous tick and time from DAT_00c83128/c83120 double-buffer.\n" +
            "Computes t = (currTick - prevTick) / (currTick - prevTick).\n" +
            "Result = prevTime + (currTime - prevTime) * t.\n" +
            "Called from GameFrameUpdate as the secondary frame callback.");

        fc("00636830", "ProcessDeferredCallbacks",
            "Processes the deferred render-batch linked list at DAT_00bef7c0.\n" +
            "Each node is a draw-call descriptor. Per frame: process nodes within a 2ms budget.\n" +
            "Calls BuildRenderBatch(node) which returns 1 when node's work is complete.\n" +
            "Completed nodes are removed from the list.\n" +
            "Time checked using timeGetTime/timeBeginPeriod pattern (same as GetGameTime).");

        fc("0063d600", "BuildRenderBatch",
            "Processes one deferred render-batch node.\n" +
            "Builds entries in the material table and issues D3D draw calls.\n" +
            "Recognized shader types: BLOOM, GLASS, BACKDROP.\n" +
            "Returns 1 when the node's work is complete (caller removes from list).");

        // ── Frame callback system ────────────────────────────────────────────

        fc("00612f00", "InitFrameCallbackSystem",
            "Initializes the frame-callback manager singleton at DAT_00e6e870.\n" +
            "  DAT_00e6e870 = &PTR_FUN_00883f3c (primary callback entry vtable)\n" +
            "  DAT_00e6e874 = &PTR_FUN_00883f4c (secondary callback entry vtable)\n" +
            "  DAT_00bef728 = &DAT_00e6e870 (pointer used as DAT_008e1644 = frame callback table ptr)\n" +
            "  Clears 8 callback slots at DAT_00e6e880..e6e89c.\n" +
            "Protected by one-shot guard at DAT_00e74c20 bit 0.\n" +
            "Called from InitCLIAndTimingAndDevice and PreDirectXInit.");

        fc("00eb5c3e", "GetOrInitCallbackManager",
            "Returns &DAT_00e6e870 (frame-callback manager singleton pointer).\n" +
            "One-shot init guard at DAT_00e74c20 bit 0 — first call initializes,\n" +
            "subsequent calls just return the pointer.\n" +
            "Registers atexit cleanup via FUN_007b5640.\n" +
            "NOT a COM object creator. Used in WinMain before PreDirectXInit to\n" +
            "create the 2904-byte engine object (DAT_00bef6d0) via the callback factory.");

        // ── DirectX init chain ───────────────────────────────────────────────

        fc("00ec64f9", "PreDirectXInit",
            "Sets up audio/render context before D3D device creation (thunk_FUN_00ec64f9).\n" +
            "  DAT_00bf1b18 = DAT_00bef6d0 (passes engine factory obj to audio subsystem)\n" +
            "  DAT_00bf1b10 = 0 (clears audio-present flag)\n" +
            "  Copies audio device string from .rodata (0x7d3ddb) into DAT_00be93d0\n" +
            "  FUN_006ac0b0(), thunk_FUN_00ec6e91() — audio hardware detection\n" +
            "  DAT_00bf1b1c = thunk_FUN_00ec72a9() — create audio output context\n" +
            "  DAT_00bf1b20 = FUN_00611940(DAT_00bf1b1c) — create audio buffer/thread handle\n" +
            "  DAT_00bf1b14 = 1 (audio pre-init done flag)\n" +
            "  One-shot InitFrameCallbackSystem guard.");

        fc("00eb612e", "InitDirectXAndSubsystems",
            "__thiscall, called from WinMain with client height.\n" +
            "Returns OR of sub-init failure codes (0 = success).\n" +
            "  1. InitCLIAndTimingAndDevice(height, ?, hWnd) (00614370)\n" +
            "  2. InitEngineObjects() (0060c2e0)\n" +
            "  3. DAT_008df65a=1, DAT_00aeea5c=2 (subsystem state flags)\n" +
            "  4. FinalizeDeviceSetup() (006147f0)\n" +
            "  5. InitAudioSubsystem() (00eb60d3)");

        fc("00614370", "InitCLIAndTimingAndDevice",
            "First major step inside InitDirectXAndSubsystems.\n" +
            "  1. AllocEngineObject(0xc,'CLI::CommandParser') -> DAT_00e6b328\n" +
            "  2. One-shot InitFrameCallbackSystem guard (DAT_00e74c20 & 1)\n" +
            "  3. Reset timing globals: DAT_00c83110/14/18/1c/28/c/20/4/30 = 0\n" +
            "  4. FUN_0064ae00(&DAT_00c8e490) — init render batch system\n" +
            "  5. FUN_0067c290(height,?,0,0,6,1,1,1,1) — CreateD3DDevice\n" +
            "  6. On D3DERR (-0x7fffbffb): ShowCursor(1)\n" +
            "  7. ParseCommandLineArg('oldgen',...) -> DAT_008ae1ff=1\n" +
            "  8. (*(*DAT_00c8c580))(0,0) — initial render callback dispatch");

        fc("0060c2e0", "InitEngineObjects",
            "Second major step inside InitDirectXAndSubsystems.\n" +
            "Allocates and wires up engine subsystem singletons:\n" +
            "  GlobalTempBuffer  (0x3c bytes) -> DAT_00e6b378\n" +
            "  RealGraphSystem   (8 bytes)    -> DAT_00e6b390 (vtable PTR_FUN_00885010)\n" +
            "  RealInputSystem   (0x34 bytes) -> FUN_00617890() init -> DAT_00e6b384\n" +
            "  Locale            (0x8c bytes) -> DAT_00e6b304\n" +
            "  FMV               (0x18 bytes) -> DAT_00e6b2dc; checks 'nofmv' flag\n" +
            "  Also calls thunk_FUN_00eb87ba, thunk_FUN_00eb88b2, FUN_006677c0");

        fc("006147f0", "FinalizeDeviceSetup",
            "Third major step inside InitDirectXAndSubsystems.\n" +
            "  Sets DAT_008e1644 = &PTR_PTR_008d0f94 (frame callback table pointer)\n" +
            "  Registers 8 message handlers via thunk_FUN_00eb59ce:\n" +
            "    iMsgDeleteEventHandler, iMsgDeleteEntity, iMsgOnDeleteEntity,\n" +
            "    iMsgDoRender, iMsgDoRenderDirectorsCamera, iMsgPreShowRaster,\n" +
            "    iMsgStartSystem, iMsgStopSystem\n" +
            "  DAT_00bef750 = 1 (device ready flag)\n" +
            "  DAT_00bef754 = 'showfps' cmdline flag\n" +
            "  DAT_00bef6d7 = 'memorylwm' cmdline flag\n" +
            "  Calls FUN_0060b740() -> GameServices::Open + TimeManager::Instance\n" +
            "    DAT_00bef6c8 = GameServices::Open (1 byte)\n" +
            "    DAT_00bef768 = TimeManager::Instance (8 bytes, via thunk_FUN_00eb797e)");

        fc("00eb60d3", "InitAudioSubsystem",
            "Final step of InitDirectXAndSubsystems. Checks DAT_00bf1b10 (audio present).\n" +
            "If non-zero (audio hardware found by PreDirectXInit):\n" +
            "  FUN_006a9080() — open audio device (async: polls AudioPollGate + SleepEx(0,1))\n" +
            "  FUN_006a91a0() — query audio format/caps (expects 0x80 result)\n" +
            "  FUN_006a9140(device,0) — configure audio output\n" +
            "  FUN_006a90e0() — start audio stream\n" +
            "All audio operations use async SleepEx-polling via DAT_00be82ac command queue.\n" +
            "Returns true on success, false on failure.");

        fc("00eb496e", "InitGameSubsystems",
            "Called from WinMain after InitDirectXAndSubsystems.\n" +
            "  Sets DAT_00e69ca0 = FUN_0060c130 (registers frame callback)\n" +
            "  Enumerates DirectInput devices: FUN_00688370(4, 0)\n" +
            "  Loads language selection screen: FUN_005090a0('LanguageSelect')\n" +
            "  DAT_00bef6c6 = 1 (subsystem initialized flag)\n" +
            "  Returns 1 on success.");

        fc("00eb4a5d", "SaveOptionsOnExit",
            "Saves user option settings back to registry on clean exit (thunk_FUN_00eb4a5d).\n" +
            "Writes via FUN_0060cc70 (integer WriteRegistrySetting helper):\n" +
            "  OptionResolution (DAT_00bf197c)\n" +
            "  OptionLOD        (DAT_008ae1ec)\n" +
            "  OptionBrightness (DAT_008ae1f0)\n" +
            "Called in WinMain cleanup after RenderAndAudioTeardown.");

        // ── CLI / settings ───────────────────────────────────────────────────

        fc("00eb787a", "CLI_CommandParser_ParseArgs",
            "CLI::CommandParser method: parses '-name=value' command-line tokens.\n" +
            "EDI = this (CLI::CommandParser object at DAT_00e6b328, allocated 0xc bytes).\n" +
            "  Fills name pointers at this+0x480 (up to 0x20 entries)\n" +
            "  Fills value pointers at this+0x500\n" +
            "  Positional args at this+0x400\n" +
            "Additional flags set by subsequent ParseCommandLineArg calls:\n" +
            "  -oldgen   -> DAT_008ae1ff=1 (legacy renderer, set in InitCLIAndTimingAndDevice)\n" +
            "  -showfps  -> DAT_00bef754=1 (set in FinalizeDeviceSetup)\n" +
            "  -memorylwm-> DAT_00bef6d7=1 (set in FinalizeDeviceSetup)\n" +
            "  -nofmv    -> FMV disabled (set in InitEngineObjects)\n" +
            "Called before single-instance check in WinMain. NOT COM init.");

        fc("00617bf0", "ParseCommandLineArg",
            "Case-insensitive cmdline flag scanner on the saved cmdline buffer.\n" +
            "Searches for ' <flag>' in the string; returns true if found.\n" +
            "If valueOut != NULL and '=' follows flag: sets *valueOut to value token.\n" +
            "Called for 'fullscreen', 'widescreen' in WinMain.\n" +
            "Also called for 'oldgen', 'showfps', 'memorylwm', 'nofmv' in init chain.");

        fc("0060deb0", "SaveOrRestoreSystemParameters",
            "Saves and restores Windows mouse acceleration and screen reader parameters.\n" +
            "param_1=0 (disable): reads current values into DAT_008afc44/4c/54,\n" +
            "  clears bits 2-3 using mask 0xFFFFFFF3 if bit 0 was clear (accel active),\n" +
            "  writes back via SPI_SETMOUSESPEED(0x3b), SPI_SETMOUSE(0x35), SPI_SETSCREENREADER(0x33).\n" +
            "param_1=1 (restore): writes saved values back verbatim.\n" +
            "Called with 0 at WinMain entry, with 1 at exit.");

        // ── Registry ─────────────────────────────────────────────────────────

        fc("0060ce60", "ReadRegistrySetting_Int",
            "Reads a DWORD registry value from HKCU, falls back to HKLM.\n" +
            "Path: HKCU/HKLM\\Software\\Electronic Arts\\<app>\\<section>.\n" +
            "If found in HKLM, writes back to HKCU via FUN_0060cc70.\n" +
            "If not found: creates in HKCU with defaultValue.\n" +
            "Uses std::basic_string<char> for parameters internally.\n" +
            "Returns int value.");

        fc("0060ca20", "ReadRegistrySetting_Str",
            "Reads a REG_SZ registry value into caller buffer.\n" +
            "HKCU -> HKLM fallback. No HKCU write-back on HKLM hit.\n" +
            "Returns true if key found; false returns defaultValue in buffer.");

        fc("0060cc70", "WriteRegistrySetting_Int",
            "Writes an integer value as REG_DWORD to HKCU.\n" +
            "Used by SaveOptionsOnExit and ReadRegistrySetting_Int (HKLM write-back).");

        fc("0060c670", "WriteRegistrySetting_Str",
            "Writes a string value as REG_SZ to HKCU.\n" +
            "Used by SaveWindowPlacement (PosX/PosY/SizeX/SizeY/Maximized/Minimized).");

        // ── DirectX device management ────────────────────────────────────────

        fc("0067d310", "UpdateDirectXDevice",
            "Checks and recovers the D3D9 device each frame.\n" +
            "  1. PreDeviceCheck() (0067d2e0) — query texture mem, low-mem handler\n" +
            "  2. TestCooperativeLevel (vtable+0xc)\n" +
            "  D3DERR_DEVICELOST (-0x7789f798): DAT_00bf18aa=1, Sleep(50ms)\n" +
            "  D3DERR_DEVICENOTRESET (-0x7789f797): ReleaseDirectXResources -> Reset(&DAT_00b94af8) -> RestoreDirectXResources\n" +
            "  Unexpected error: FatalError('Invalid device lost state %d\\n', hr) — no return\n" +
            "  Success: DAT_00bf18aa=0");

        fc("0067d2e0", "PreDeviceCheck",
            "Queries available texture memory before TestCooperativeLevel.\n" +
            "Calls GetAvailableTextureMem (device vtable+0x10).\n" +
            "Stores result >> 20 (MB) in DAT_00bf193c.\n" +
            "If 0 < MB < 33: calls thunk_FUN_00ebe85b() (low texture memory handler).");

        fc("0067cfb0", "ReleaseDirectXResources",
            "Releases D3D surfaces and resources before device Reset.\n" +
            "  1. Release DAT_00b95034 (GPU sync query)\n" +
            "  2. thunk_FUN_00ec04dc() — pre-release cleanup (unknown)\n" +
            "  3. Clear DAT_00af1390, DAT_00ae9250 (cached surface ptrs)\n" +
            "  4. If DAT_00bf1934 != NULL && != 0xbacb0ffe sentinel:\n" +
            "     Get back buffer from DAT_00bf1924 vtable+0x14\n" +
            "     SetCachedRenderTargets(surface,0) — unregisters from cache\n" +
            "     Release surface and DAT_00bf1934\n" +
            "  5. Release DAT_00bf1930 (back buffer)\n" +
            "  6. Release DAT_00bf1938 (additional render target)\n" +
            "  7. thunk_FUN_00ec19b5() — post-release cleanup (unknown)");

        fc("0067d0c0", "RestoreDirectXResources",
            "Rebuilds D3D resources after successful device Reset.\n" +
            "  1. InitRenderStates() (00675950) — upload shaders; extended path if DAT_00bf1994>2\n" +
            "  2. If DAT_00b95034==NULL: CreateGPUSyncQuery() (0067b820)\n" +
            "  3. FUN_0067bb20() — confirmed empty stub\n" +
            "  4. GetBackBuffer(vtable+0x14) -> DAT_00bf1930; cache in DAT_00af1390\n" +
            "  5. Get surface descriptor\n" +
            "  6. CreateTexture (vtable+0x74) for additional RT -> DAT_00bf1938\n" +
            "  7. If DAT_00bf1970==0 (AA off) && caps.TextureOpCaps & 0x80:\n" +
            "     CreateTexture (vtable+0x5c) -> texture\n" +
            "     Release old DAT_00bf1930, GetSurfaceLevel(0) -> DAT_00af1390\n" +
            "     DAT_00bf1934 = texture; DAT_00bf1930 = DAT_00af1390\n" +
            "  8. Else: DAT_00bf1934 = 0xbacb0ffe (AA sentinel)\n" +
            "  9. DAT_00ae9250 = DAT_00bf1938\n" +
            " 10. SetRenderTarget + SetDepthStencilSurface\n" +
            " 11. InitD3DStateDefaults() (00674430)");

        fc("0067ecf0", "SetCachedRenderTargets",
            "Avoids redundant SetRenderTarget/SetDepthStencilSurface calls.\n" +
            "Checks against cached values in DAT_00af1390 (RT) and DAT_00ae9250 (DS).\n" +
            "Uses device vtable offsets +0x94 (SetRenderTarget) and +0x9c (SetDepthStencilSurface).");

        fc("00675950", "InitRenderStates",
            "Uploads vertex and pixel shaders to the D3D device and sets initial render states.\n" +
            "Extended path taken if DAT_00bf1994 > 2 (higher shader capability level).");

        fc("0067b820", "CreateGPUSyncQuery",
            "Creates a D3DQUERYTYPE_EVENT (0xe0) GPU sync query.\n" +
            "Flags: DAT_00bf19ac | 0x208. Swap effect: (DAT_008ae1fc != 0) ? 0 : 2.\n" +
            "Stored in DAT_00b95034. Called from RestoreDirectXResources when query is NULL.");

        fc("00674430", "InitD3DStateDefaults",
            "Initializes D3D render state, sampler state, and texture stage state to defaults.\n" +
            "Clears dirty flags DAT_00bf18a8/ac/c0/c4.\n" +
            "Fills DAT_008d3878 (0x4c bytes) with 0xcd sentinel.\n" +
            "Sets fog defaults (DAT_00d7ea68=5).\n" +
            "Conditionally adjusts blend states if !(caps.TextureOpCaps & 0x80)\n" +
            "  or DAT_00bf1980 == 0x10 (16-bit colour mode).\n" +
            "Calls thunk_FUN_00ebfe83() and SetCachedRenderState(0x19, 8).");

        fc("0067c290", "CreateD3DDevice",
            "Creates the Direct3D 9 device.\n" +
            "Called from InitCLIAndTimingAndDevice with params: (height,?,0,0,6,1,1,1,1).\n" +
            "On D3DERR_DEVICELOST (-0x7fffbffb) during init: calls ShowCursor(1).");

        // ── Render mode switching ────────────────────────────────────────────

        fc("00612530", "SwitchRenderOutputMode",
            "Dispatches a render-mode change to the registered listener list.\n" +
            "Uses scene IDs: compares DAT_00c82b00 or DAT_00c82b08 against target DAT_00c82ac8.\n" +
            "Called from UpdateCursorVisibilityAndScene on cursor state change,\n" +
            "and from MainLoop focus-loss/gain handling.");

        fc("00ea53ca", "UpdateCursorVisibilityAndScene",
            "Updates cursor visibility state and switches the render scene.\n" +
            "Called with new cursor-visible state in AL register.\n" +
            "Compares AL vs DAT_00bef67e (current cursor state).\n" +
            "Also checks DAT_00bef67c and DAT_00bef67d (additional conditions).\n" +
            "If state changed:\n" +
            "  AL=0 (cursor hidden = focus gained):\n" +
            "    if DAT_00c82b08 != DAT_00c82ac8 -> SwitchRenderOutputMode(&DAT_00c82b08)\n" +
            "  AL!=0 (cursor shown = focus lost):\n" +
            "    if DAT_00c82b00 != DAT_00c82ac8 -> SwitchRenderOutputMode(&DAT_00c82b00)\n" +
            "Updates DAT_00bef67e to new state.\n" +
            "Called in WM_ACTIVATE focus-loss, focus-gain, and WinMain cleanup.");

        fc("00617b60", "PauseGraphicsState",
            "Pauses input device on focus loss (custom calling conv: 'this' in EAX).\n" +
            "EAX = game state object (DAT_00e6b384 = RealInputSystem).\n" +
            "  If *(this+0xc) != 0: vtable[0x10] on sub-object -> device interface ptr\n" +
            "  At device+0x218: IDirectInputDevice8*\n" +
            "  Calls vtable[0x20] on device (Pause/Unacquire).\n" +
            "Called from WM_ACTIVATE focus-loss and WM_SETFOCUS while unfocused.");

        // ── Input management ─────────────────────────────────────────────────

        fc("00617890", "InputSystem_Init",
            "Initializes the RealInputSystem object and stores it in DAT_00e6b384.\n" +
            "  Vtable = PTR_FUN_00884ff4; field +0x8 = 0x20\n" +
            "  Copies string 'RealInputSystem::RealInputSystem' to DAT_00bef6e8\n" +
            "  One-shot InitFrameCallbackSystem guard\n" +
            "Called from InitEngineObjects.");

        fc("0068da30", "AcquireInputDevices",
            "Acquires keyboard, mouse, and up to 2 joysticks via DirectInput.\n" +
            "Also calls Ordinal_5(0) for custom cursor hide.\n" +
            "Fatal error via FUN_0066f810 if any device fails to acquire.\n" +
            "Called on WM_ACTIVATE focus gain.");

        fc("0068dac0", "UnacquireInputDevices",
            "Unacquires keyboard, mouse, and up to 2 joysticks.\n" +
            "Also calls Ordinal_5(1) for custom cursor show.\n" +
            "Called on WM_ACTIVATE focus loss and WinMain cleanup.");

        fc("00688370", "EnumerateInputDevices",
            "Enumerates DirectInput devices. Called from InitGameSubsystems as FUN_00688370(4,0).\n" +
            "Sets up keyboard (DAT_00e6a070), mouse (DAT_00e6a194), joysticks (DAT_00e6a42c, stride 0x248).");

        // ── Audio system ──────────────────────────────────────────────────────

        fc("0061ef80", "PauseAudioManager",
            "Pauses the audio manager on focus loss.\n" +
            "Checks if a music track is loaded, then calls AudioStream_Pause (FUN_006a9ea0).\n" +
            "Called from WM_ACTIVATE focus-loss path before delayed timer is set.");

        fc("006a9ea0", "AudioStream_Pause",
            "Pauses the active audio stream. Called with 'this' in EAX.\n" +
            "  this+0x1c = audio timer pointer\n" +
            "  this+0x28 = pause flag (set to 1)\n" +
            "  this+0x16 = pause timestamp (set to GetGameTime())\n" +
            "  Calls thunk_FUN_00ec693d(0) (decoder speed=0), thunk_FUN_00ec66f1() (flush).");

        fc("00ec67e8", "AudioStream_Resume",
            "Resumes a paused audio stream. Called with 'this' in EAX.\n" +
            "  this+0x1c = audio timer pointer\n" +
            "  this+0x28 = pause flag (cleared to 0)\n" +
            "  Computes elapsed time since pause via GetGameTime()\n" +
            "  Advances timer: if pitch==0x1000 (normal), add elapsed directly;\n" +
            "    else scale by pitch/0x1000 (pitch-speed correction)\n" +
            "  Updates this+0x17 (cumulative playback time)\n" +
            "  Calls thunk_FUN_00ec693d(0x1000) (resume decoder), thunk_FUN_00ec66f1() (finalize)\n" +
            "Called from MainLoop delayed-timer expiry.");

        fc("006a9080", "AudioDevice_Open",
            "Opens the audio output device. Called from InitAudioSubsystem.\n" +
            "Async: polls AudioPollGate (FUN_006109d0) + SleepEx(0,1) until complete.\n" +
            "Uses command queue at DAT_00be82ac.");

        fc("006a91a0", "AudioDevice_QueryCaps",
            "Queries audio format/capabilities. Called from InitAudioSubsystem.\n" +
            "Expects 0x80 (success) result. Async via command queue DAT_00be82ac.");

        fc("006a9140", "AudioDevice_Configure",
            "Configures audio output (format, buffer size, etc). Called with (device, 0).\n" +
            "Async via command queue DAT_00be82ac.");

        fc("006a90e0", "AudioDevice_Start",
            "Starts the audio stream playback. Called from InitAudioSubsystem.");

        fc("006109d0", "AudioPollGate",
            "Checks if an async audio operation has completed.\n" +
            "Polled in a SleepEx(0,1) loop in InitAudioSubsystem.");

        fc("006ac010", "AudioAsyncPump",
            "Pumps the async audio command queue (DAT_00be82ac).\n" +
            "Called from all async audio operations.");

        // ── Render / audio teardown ──────────────────────────────────────────

        fc("00ec6610", "RenderAndAudioTeardown",
            "Full audio/render cleanup on exit. Called from WinMain before input cleanup.\n" +
            "  1. If DAT_00bf1b1c set: thunk_FUN_00eb584e(), FUN_006119c0(); clear DAT_00bf1b18/1c\n" +
            "  2. FUN_006ace30() — audio teardown step 1\n" +
            "  3. FUN_006108c0() — audio teardown step 2\n" +
            "  4. FUN_006ac930() — audio teardown step 3\n" +
            "  5. If DAT_00bf1b2c set: close handle DAT_00bf1b34; clear DAT_00bf1b30/2c");

        // ── Engine object allocator ──────────────────────────────────────────

        fc("00614210", "AllocEngineObject",
            "Engine heap allocator factory: AllocEngineObject(size, tagName).\n" +
            "Creates objects with debug tags for all engine subsystems.\n" +
            "Examples: 'CLI::CommandParser' (0xc bytes), 'GlobalTempBuffer' (0x3c bytes),\n" +
            "  'RealGraphSystem' (8 bytes), 'RealInputSystem' (0x34 bytes),\n" +
            "  'Locale' (0x8c bytes), 'FMV' (0x18 bytes).");

        fc("00eb6dbc", "QueryMemoryAllocatorMax",
            "Returns the size of the largest free block in the engine's internal allocator.\n" +
            "Thread-safe via critical section at allocator+0x4e4.\n" +
            "Scans priority-indexed free list (allocator+0x428, 0xfe entries)\n" +
            "and linked list at allocator+0x30..0x3c.\n" +
            "Values masked to &0x7ffffff8 (8-byte alignment).\n" +
            "Result tracked as low-water mark in DAT_008afb08.");

        // ── Game subsystem init / services ───────────────────────────────────

        fc("0060b740", "GameServicesAndTimeManager_Init",
            "Called from FinalizeDeviceSetup.\n" +
            "Creates GameServices::Open object (1 byte) -> DAT_00bef6c8.\n" +
            "Creates TimeManager::Instance (8 bytes) via thunk_FUN_00eb797e -> DAT_00bef768.\n" +
            "DAT_00bef768+4 is checked in UpdateFrameTimingPrimary to gate tick counting.");

        fc("00eb59ce", "RegisterMessageHandler",
            "Registers a message handler callback in the engine message dispatch system.\n" +
            "Called from FinalizeDeviceSetup for 8 message types:\n" +
            "  iMsgDeleteEventHandler, iMsgDeleteEntity, iMsgOnDeleteEntity,\n" +
            "  iMsgDoRender, iMsgDoRenderDirectorsCamera, iMsgPreShowRaster,\n" +
            "  iMsgStartSystem, iMsgStopSystem");

        fc("0060c130", "FrameCallbackRegistration",
            "Frame callback function registered in InitGameSubsystems.\n" +
            "Stored at DAT_00e69ca0. Called as part of the per-frame callback chain.");

        fc("005090a0", "LoadLanguageScreen",
            "Loads the language/localization selection screen.\n" +
            "Called from InitGameSubsystems with 'LanguageSelect'.");

        // ── Pause/resume ─────────────────────────────────────────────────────

        fc("0058b790", "PauseGameObjects",
            "Pauses game systems via state machine on DAT_00c7b908.\n" +
            "Triggers 'chapter-cutscene-in' animation.\n" +
            "Calls vtable+0x24 (Pause) on all registered systems:\n" +
            "  DAT_00c7c370 (audio/music manager), DAT_00c7b924, array at DAT_00c7ba4c.\n" +
            "Records pause time: DAT_00c7b90c = DAT_00c8311c/3 + param.\n" +
            "States: 0=full pause, 4-5=audio-only, 6=no-op.");

        fc("0058b8a0", "ResumeGameObjects",
            "Resumes game systems via state machine on DAT_00c7b908.\n" +
            "Triggers 'default' animation.\n" +
            "Calls vtable+0x28 (Resume) on all registered systems.\n" +
            "States: 7=resuming.");

        fc("0064ae00", "InitRenderBatchSystem",
            "Initializes the render batch / deferred draw-call system.\n" +
            "Called from InitCLIAndTimingAndDevice with &DAT_00c8e490.");

        println("annotateFunctions() done.");
    }

    // ── Globals ───────────────────────────────────────────────────────────────

    private void annotateGlobals() throws Exception {

        // Window / app state
        gn("00bef6cc", "g_hWnd",              "Main game window handle (HWND)");
        gn("008afbd9", "g_bIsFullscreen",      "True when running in fullscreen mode");
        gn("00bef6c5", "g_bExitRequested",     "Non-zero = exit MainLoop (set by WM_QUIT)");
        gn("00bef6c7", "g_bHasFocusLost",      "1 = window does not have focus");
        gn("00bef67e", "g_bCursorVisible",      "Current cursor visibility state (0=hidden,1=shown)");
        gn("00bef67c", "g_bCursorCondA",        "Checked in UpdateCursorVisibilityAndScene (purpose TBD)");
        gn("00bef67d", "g_bCursorCondB",        "Checked in UpdateCursorVisibilityAndScene (purpose TBD)");
        gn("00bef6d8", "g_dwDelayedOpTimer",    "Countdown ms; on expiry -> resume audio/updates after focus regain");
        gn("00bef6d7", "g_bGameUpdateEnabled",  "Set by 'memorylwm' cmdline flag; enables memory low-water-mark tracking");
        gn("00bef6d4", "g_bAudioWasPaused",     "Audio was paused on last focus loss");
        gn("00bef6d5", "g_bUpdatesWerePaused",  "Physics/updates were paused on last focus loss");
        gn("00bef6c6", "g_bSubsysInitialized",  "Set to 1 after InitGameSubsystems completes");
        gn("00bef6e8", "g_szRealInputSystemTag", "String: 'RealInputSystem::RealInputSystem' (debug tag)");

        // Engine objects
        gn("00bef6d0", "g_pEngineObject",       "2904-byte engine factory/root object created by GetOrInitCallbackManager; released via callback manager vtable at cleanup");
        gn("00e6b384", "g_pRealInputSystem",    "RealInputSystem singleton (0x34 bytes); vtable PTR_FUN_00884ff4; set by FUN_00617890");
        gn("00e6b378", "g_pGlobalTempBuffer",   "GlobalTempBuffer (0x3c bytes); allocated in InitEngineObjects");
        gn("00e6b390", "g_pRealGraphSystem",    "RealGraphSystem (8 bytes); vtable PTR_FUN_00885010; allocated in InitEngineObjects");
        gn("00e6b304", "g_pLocale",             "Locale subsystem (0x8c bytes); handles localization");
        gn("00e6b2dc", "g_pFMV",               "FMV subsystem (0x18 bytes); disabled by 'nofmv' cmdline flag");
        gn("00e6b328", "g_pCLICommandParser",   "CLI::CommandParser object (0xc bytes); filled by CLI_CommandParser_ParseArgs");
        gn("00bef6c8", "g_pGameServices",       "GameServices::Open object (1 byte); created by FUN_0060b740");
        gn("00bef768", "g_pTimeManager",        "TimeManager::Instance (8 bytes); created by FUN_0060b740 via thunk_FUN_00eb797e");

        // Frame callback system
        gn("00e6e870", "g_FrameCallbackPrimary",   "Frame callback manager primary entry; vtable PTR_FUN_00883f3c");
        gn("00e6e874", "g_FrameCallbackSecondary",  "Frame callback manager secondary entry; vtable PTR_FUN_00883f4c");
        gn("008e1644",  "g_pFrameCallbackTable",    "Pointer to frame callback table; = &DAT_00e6e870 (set by FinalizeDeviceSetup + InitFrameCallbackSystem)");
        gn("00e74c20",  "g_nCallbackInitGuard",     "One-shot init guard for InitFrameCallbackSystem (bit 0 tested)");
        gn("00e69ca0",  "g_pfnFrameCallback",       "Frame callback function ptr; set to FUN_0060c130 by InitGameSubsystems");
        gn("00bef728",  "g_pCallbackManagerPtr",    "Ptr = &DAT_00e6e870 (the callback manager); used as DAT_008e1644");

        // Timing globals
        gn("00c83110", "g_dwGameTicks",          "Game ticks = accum*3/0x10000 (3x speed, 16.16 scale)");
        gn("00c83114", "g_nTickCounter",          "Incremented by localTick*3 in UpdateFrameTimingPrimary");
        gn("00c83130", "g_nFrameFlip",            "Double-buffer flip index (0 or 1); toggled each callback interval");
        gn("00c83198", "g_ullAccumTime_lo",        "64-bit accumulated time low DWORD (16.16 fixed-point)");
        gn("00c8319c", "g_ullAccumTime_hi",        "64-bit accumulated time high DWORD");
        gn("00c831a8", "g_ullNextCallback_lo",     "64-bit next callback fire time low DWORD");
        gn("00c831ac", "g_ullNextCallback_hi",     "64-bit next callback fire time high DWORD");
        gn("00c83190", "g_ullCallbackInterval_lo", "64-bit callback interval low DWORD");
        gn("00c83194", "g_ullCallbackInterval_hi", "64-bit callback interval high DWORD");
        gn("00c83128", "g_aTimingBuf_ticks",       "Timing double-buffer: tick values [flip=0][flip=1]");
        gn("00c83120", "g_aTimingBuf_time",        "Timing double-buffer: time values [flip=0][flip=1]");
        gn("00e6e5e0", "g_ullLastFrameTime_lo",    "64-bit last frame time low DWORD (16.16 fixed-point baseline)");
        gn("00e6e5e4", "g_ullLastFrameTime_hi",    "64-bit last frame time high DWORD");
        gn("00e6b388", "g_bTimerBaseInit",         "One-shot init flag for GetGameTime startup baseline");
        gn("00e6e5e8", "g_dwTimerStartupBase",     "Startup time baseline for GetGameTime");
        gn("00845594", "g_fTimingScale_0001",      "Float constant 0.001 (1/1000) used in UpdateFrameTimingPrimary");
        gn("00845320", "g_fTimingScale_1000",      "Float constant 1000.0 used in UpdateFrameTimingPrimary");
        gn("008475d8", "g_fFixed16Denom",          "Float constant 1/65536 (16.16 fixed-point denominator)");

        // Command line buffers
        gn("00c82b88", "g_szCmdLine1",    "First copy of lpCmdLine (0x200 bytes) — scanned by ParseCommandLineArg");
        gn("00c82d88", "g_szCmdLine2",    "Second copy of lpCmdLine (0x200 bytes)");

        // System parameter buffers
        gn("008afc44", "g_mouseSpeed",    "Saved SPI_GETMOUSESPEED buffer (2x UINT = 8 bytes)");
        gn("008afc4c", "g_mouseAccel",    "Saved SPI_GETMOUSE buffer (2x UINT = 8 bytes)");
        gn("008afc54", "g_screenReader",  "Saved SPI_GETSCREENREADER buffer (6x UINT = 24 bytes)");
        gn("008afb08", "g_nMinFreeMemory","Low-water mark: smallest free-allocator block seen across frames");

        // DirectX resource globals
        gn("00bf1920", "g_pd3dDevice",    "IDirect3DDevice9* active device");
        gn("00bf1924", "g_pD3D",          "IDirect3D9* or swap chain object");
        gn("00bf1930", "g_pBackBuffer",   "IDirect3DSurface9* back buffer (or texture surface in non-AA path)");
        gn("00bf1934", "g_pRenderTarget", "IDirect3DSurface9* render target texture, or 0xbacb0ffe sentinel (AA path)");
        gn("00bf1938", "g_pAdditionalRT", "IDirect3DSurface9* additional render target");
        gn("00af1390", "g_pCachedRT",     "Cached SetRenderTarget surface (avoids redundant calls)");
        gn("00ae9250", "g_pCachedDS",     "Cached SetDepthStencilSurface (avoids redundant calls)");
        gn("00b95034", "g_pGPUSyncQuery", "D3DQUERYTYPE_EVENT GPU sync query object");
        gn("00bf18aa", "g_bDeviceLost",   "1 = D3D9 device is currently lost");
        gn("00b94af8", "g_d3dpp",         "D3DPRESENT_PARAMETERS saved for device Reset (DAT_00b94af8)");
        gn("00b94748", "g_aRenderStateCache", "Render state cache array indexed by D3DRENDERSTATETYPE");
        gn("00bf1994", "g_nShaderCapLevel",   "Shader/feature capability level index (>2 = extended shaders)");
        gn("00bf193c", "g_nTexMemMB",         "Available texture memory in MB (updated by PreDeviceCheck)");
        gn("00bf19ac", "g_dwQueryFlags",      "Flags used by CreateGPUSyncQuery");
        gn("00bf18a8", "g_bRS_DirtyA",        "Render state dirty flag A (cleared by InitD3DStateDefaults)");
        gn("00bf18ac", "g_bRS_DirtyB",        "Render state dirty flag B");
        gn("00bf18c0", "g_bRS_DirtyC",        "Render state dirty flag C");
        gn("00bf18c4", "g_bRS_DirtyD",        "Render state dirty flag D");
        gn("008d3878", "g_aD3DStateSentinel",  "0x4c-byte block filled with 0xcd sentinel by InitD3DStateDefaults");
        gn("00bf1980", "g_nColourDepth16",     "Non-zero if 16-bit colour mode; affects InitD3DStateDefaults blend states");
        gn("00c8c580", "g_pRenderDispatchTable","Render callback dispatch table; (*(*DAT_00c8c580))(0,0) called at end of InitCLIAndTimingAndDevice");
        gn("00c8e490", "g_RenderBatchState",   "Render batch system state block; passed to InitRenderBatchSystem");

        // Graphics settings (all under GameSettings registry key)
        gn("00bf1940", "g_nScreenWidth",    "RegistryKey: Width. Non-zero = fullscreen + this is the width");
        gn("00bf1944", "g_nBitDepth",       "RegistryKey: BitDepth (0/16/32)");
        gn("00bf1948", "g_nShadowLOD",      "RegistryKey: ShadowLOD (default 6)");
        gn("00bf194c", "g_nMaxTexSize",     "RegistryKey: MaxTextureSize");
        gn("00bf1950", "g_nMaxShadowTexSize","RegistryKey: MaxShadowTextureSize");
        gn("00bf1954", "g_nMaxFarClip",     "RegistryKey: MaxFarClip");
        gn("00bf195c", "g_nCullDistance",   "RegistryKey: CullDistance");
        gn("00bf1958", "g_nParticleRate",   "RegistryKey: ParticleRate");
        gn("00bf1960", "g_nParticleCull",   "RegistryKey: ParticleCullDistance");
        gn("00bf1964", "g_bDisableFog",     "RegistryKey: DisableFog");
        gn("00bf1968", "g_bDisablePostPro", "RegistryKey: DisablePostPro");
        gn("00bf196c", "g_nFilterFlip",     "RegistryKey: FilterFlip");
        gn("00bf1970", "g_nAAMode",         "RegistryKey: AAMode (0=off; non-zero uses AA path in RestoreDirectXResources)");
        gn("00bf1974", "g_bUseAddlModes",   "RegistryKey: UseAdditionalModes");
        gn("00bf197c", "g_nOptionResolution","RegistryKey: OptionResolution (saved by SaveOptionsOnExit)");
        gn("008ae1ec", "g_nOptionLOD",      "RegistryKey: OptionLOD (default 1; saved by SaveOptionsOnExit)");
        gn("008ae1f0", "g_nOptionBrightness","RegistryKey: OptionBrightness (default 5; saved by SaveOptionsOnExit)");
        gn("008ae1dc", "g_fAspectRatio",    "Aspect ratio: 16/9 (widescreen) or 4/3 (standard)");
        gn("008ae1ff", "g_bOldGenRenderer", "Set by 'oldgen' cmdline flag; enables legacy renderer path");
        gn("008ae1fc", "g_nSwapEffect",     "Swap effect selector for CreateGPUSyncQuery (0=DISCARD, non-zero=other)");

        // Device state flags
        gn("00bef750", "g_bDeviceReady",    "Set to 1 by FinalizeDeviceSetup when D3D device is ready");
        gn("00bef754", "g_bShowFPS",        "Set by 'showfps' cmdline flag");
        gn("00bef755", "g_bInitFlag_bef755","Set to 1 in InitCLIAndTimingAndDevice (purpose TBD)");
        gn("008df65a", "g_bSubsysStateA",   "Set to 1 in InitDirectXAndSubsystems (subsystem state flag)");
        gn("00aeea5c", "g_nSubsysStateB",   "Set to 2 in InitDirectXAndSubsystems (subsystem state flag)");

        // Audio globals
        gn("00bf1b10", "g_bAudioPresent",   "Non-zero if audio hardware was detected; checked in InitAudioSubsystem");
        gn("00bf1b14", "g_bAudioPreInit",   "Set to 1 in PreDirectXInit (audio pre-init done)");
        gn("00bf1b18", "g_pAudioEngineRef", "Copy of g_pEngineObject passed to audio subsystem by PreDirectXInit");
        gn("00bf1b1c", "g_pAudioContext",   "Audio output context created by thunk_FUN_00ec72a9; checked in RenderAndAudioTeardown");
        gn("00bf1b20", "g_pAudioBuffer",    "Audio buffer/thread handle created by FUN_00611940(g_pAudioContext)");
        gn("00bf1b2c", "g_bAudioThreadRunning", "Non-zero if audio pump thread is running; close DAT_00bf1b34 on teardown");
        gn("00bf1b30", "g_dwAudioThreadId", "Audio pump thread ID");
        gn("00bf1b34", "g_hAudioThread",    "Audio pump thread handle; closed in RenderAndAudioTeardown");
        gn("00be93d0", "g_szAudioDeviceName","Audio device name string (copied from .rodata in PreDirectXInit)");
        gn("00be82ac", "g_pAudioCmdQueue",  "Async audio command queue used by all audio operations");

        // Pause/resume system
        gn("00c7b908", "g_nPauseState",     "Game pause state: 0=full pause, 4-5=audio-only, 6=no-op, 7=resuming");
        gn("00c7b90c", "g_dwPauseTime",     "Pause timestamp: recorded by PauseGameObjects");
        gn("00c7c370", "g_pAudioManager",   "Audio/music manager system object; paused/resumed in pause state machine");
        gn("00c7b924", "g_pSecondarySystem","Secondary pausable system; paused/resumed in pause state machine");
        gn("00c7ba4c", "g_aPausableSystems","Array of pausable systems (stride 0x12*4)");
        gn("00c8311c", "g_dwCurrentTick",   "Current tick counter / base time (used in PauseGameObjects)");

        // Deferred callback / render batch
        gn("00bef7c0", "g_pDeferredBatchList","Linked list head for deferred render-batch nodes (processed by ProcessDeferredCallbacks)");

        // DirectInput device ptrs
        gn("00e6a070", "g_pKeyboardDevice", "IDirectInputDevice8* keyboard");
        gn("00e6a194", "g_pMouseDevice",    "IDirectInputDevice8* mouse");
        gn("00e6a42c", "g_aJoystickDevices","Array of joystick entries; stride 0x248 bytes; up to 2 entries");

        // Scene / render mode IDs
        gn("00c82b88", "g_szCmdLine1",      "First copy of lpCmdLine (0x200 bytes)");  // duplicate of earlier; no-op
        gn("00c82b00", "g_SceneID_FocusLost",  "Scene ID used by SwitchRenderOutputMode when focus lost (cursor shown)");
        gn("00c82b08", "g_SceneID_FocusGain",  "Scene ID used by SwitchRenderOutputMode when focus gained (cursor hidden)");
        gn("00c82ac8", "g_SceneID_Current",    "Current active scene ID; compared in UpdateCursorVisibilityAndScene");

        println("annotateGlobals() done.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Set comment + optionally rename a function. */
    private void fc(String addrStr, String newName, String comment) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            Function f = fm.getFunctionAt(addr);
            if (f == null) {
                println("  NO FUNC @ " + addrStr + (newName != null ? " (" + newName + ")" : ""));
                return;
            }
            f.setComment(comment);
            if (newName != null) {
                String existing = f.getName();
                if (existing.startsWith("FUN_") || existing.startsWith("thunk_FUN_") ||
                    existing.startsWith("Ordinal_")) {
                    f.setName(newName, SourceType.USER_DEFINED);
                    println("  RENAMED " + existing + " -> " + newName);
                } else {
                    println("  KEPT " + existing + " (wanted: " + newName + ")");
                }
            } else {
                println("  COMMENTED @ " + addrStr + " (" + f.getName() + ")");
            }
        } catch (Exception e) {
            println("  fc ERROR " + addrStr + ": " + e.getMessage());
        }
    }

    /** Rename a global data symbol and add an EOL comment. */
    private void gn(String addrStr, String newName, String comment) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            // Rename symbol
            Symbol sym = st.getPrimarySymbol(addr);
            if (sym != null) {
                String existing = sym.getName();
                if (existing.startsWith("DAT_") || existing.startsWith("PTR_") ||
                    existing.startsWith("LAB_") || existing.startsWith("byte_") ||
                    existing.startsWith("dword_") || existing.startsWith("undefined")) {
                    sym.setName(newName, SourceType.USER_DEFINED);
                    println("  GLOBAL " + existing + " -> " + newName);
                } else {
                    println("  GLOBAL KEPT " + existing + " (wanted: " + newName + ")");
                }
            } else {
                // Create label
                st.createLabel(addr, newName, SourceType.USER_DEFINED);
                println("  GLOBAL CREATED " + newName + " @ " + addrStr);
            }
            // Add EOL comment
            CodeUnit cu = listing.getCodeUnitAt(addr);
            if (cu != null) {
                cu.setComment(CodeUnit.EOL_COMMENT, comment);
            }
        } catch (Exception e) {
            println("  gn ERROR " + addrStr + " [" + newName + "]: " + e.getMessage());
        }
    }
}
