// Second-iteration comment and rename pass based on deeper decompilation analysis
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.*;
import ghidra.program.model.symbol.*;

public class UpdateCommentsV2 extends GhidraScript {

    @Override
    public void run() throws Exception {

        // ── Timing system ────────────────────────────────────────────────────────

        addComment("00618010", "GetGameTime",
            "Returns elapsed game time in milliseconds relative to startup.\n\n" +
            "Uses high-resolution multimedia timer:\n" +
            "  timeGetDevCaps(caps, 8) - get timer capabilities\n" +
            "  timeBeginPeriod(caps.wPeriodMin) - increase timer resolution\n" +
            "  timeGetTime() - read current time\n" +
            "  timeEndPeriod(caps.wPeriodMin) - restore timer resolution\n\n" +
            "Globals:\n" +
            "  DAT_00e6b388: one-time init flag (set on first call)\n" +
            "  _DAT_00e6e5e8: startup timestamp (captured on first call)\n\n" +
            "Returns: (timeGetTime() - startupTime), so 0 at launch, growing over time");

        addComment("00617f50", "UpdateFrameTimingPrimary",
            "Updates double-buffered primary frame timing state.\n\n" +
            "Called by GameFrameUpdate when accumulated time exceeds the callback interval.\n" +
            "Stores current timing values into a double buffer indexed by DAT_00c83130.\n\n" +
            "Inputs: param_1 = current game tick value (from __alldiv result)\n\n" +
            "Globals written:\n" +
            "  DAT_00c83114: cumulative tick counter (incremented by tick * 3)\n" +
            "  DAT_00c83130: double-buffer flip index (toggled 0/1 each call)\n" +
            "  DAT_00c83128 + index*4: tick timestamp for current half-buffer\n" +
            "  DAT_00c83120 + index*4: game time value for current half-buffer\n" +
            "  _DAT_008e1640: stores frame-set index from DAT_00bef768+4");

        addComment("00617ee0", "InterpolateFrameTime",
            "Linear interpolation between two double-buffered frame timing values.\n\n" +
            "Used to compute a smooth in-between time value for rendering.\n\n" +
            "Params:\n" +
            "  param_1: pointer to current tick (input)\n" +
            "  param_2: pointer to result interpolated value (output)\n\n" +
            "Algorithm:\n" +
            "  t = (currentTick - prevBufferTick) / (currBufferTick - prevBufferTick)\n" +
            "  result = prevGameTime + (currGameTime - prevGameTime) * t\n\n" +
            "Uses DAT_00c83128/c83120 double buffers and DAT_00c83130 flip index.");

        addComment("00636830", "ProcessDeferredCallbacks",
            "Processes deferred/timed callback items from a linked list.\n\n" +
            "Walks the singly-linked list rooted at DAT_00bef7c0:\n" +
            "  For each node: calls FUN_0063d600(node) to process it\n" +
            "  If FUN_0063d600 returns non-zero (done), removes node from list\n" +
            "    by reading next pointer at node+0x7c and advancing head\n" +
            "  Loops until list is empty OR less than 2ms has elapsed since entry\n\n" +
            "Time is checked via GetGameTime (same logic as FUN_00618010).\n" +
            "This is a cooperative scheduler - budget is 2ms per game frame.");

        // ── DirectX ──────────────────────────────────────────────────────────────

        addComment("00675950", "InitRenderStates",
            "Initialises per-device render state depending on shader/LOD capability.\n\n" +
            "Calls FUN_00684f30() unconditionally.\n" +
            "If DAT_00bf1994 > 2, also calls FUN_00685410() (extended state init).\n\n" +
            "DAT_00bf1994 is the shader/feature-level capability index set during\n" +
            "device creation.");

        addComment("0067b820", "CreateGPUSyncQuery",
            "Creates a DirectX GPU event query for synchronization.\n\n" +
            "Calls IDirect3DDevice9::CreateQuery (vtable offset 0xe0) on g_pd3dDevice:\n" +
            "  Query type: 0xe0 (D3DQUERYTYPE_EVENT)\n" +
            "  Usage flags: DAT_00bf19ac | 0x208\n" +
            "  Multisample: 0\n" +
            "  Swap effect flag: (DAT_008ae1fc != 0) ? 0 : 2\n" +
            "  Output: stored in DAT_00b95034\n\n" +
            "Used to detect when the GPU has finished rendering a frame.\n" +
            "Only called if DAT_00b95034 == NULL (first time or after device reset).");

        addComment("00674430", "InitD3DStateDefaults",
            "Initialises the D3D render-state and sampler-state default tables.\n\n" +
            "Sets many _DAT_00e44xxx globals to default values (blending modes,\n" +
            "texture addressing, filter modes, etc.).\n\n" +
            "Key effects:\n" +
            "  - Clears DAT_00bf18a8/ac, DAT_00bf18c0/c4 (render state dirty flags)\n" +
            "  - Fills 0x4c bytes at DAT_008d3878 with 0xcd (debug sentinel)\n" +
            "  - Sets _DAT_00e44600=6, _DAT_00e4464c=6, etc. (sampler filter states)\n" +
            "  - Conditionally overrides blend states based on hardware capability:\n" +
            "      if !(caps.TextureOpCaps & 0x80) || DAT_00bf1980==0x10\n" +
            "      (i.e. hardware can't do certain texture ops, or 16-bit colour)\n" +
            "  - Sets _DAT_00d7ea68=5, _DAT_008d38ac=5 (fog state)\n" +
            "  - Calls thunk_FUN_00ebfe83() and FUN_0067eb90(25,8)");

        addComment("0067d2e0", "PreDeviceCheck",
            "Called at the top of UpdateDirectXDevice before TestCooperativeLevel.\n" +
            "Likely flushes pending state or handles deferred device operations.\n" +
            "Exact purpose requires further analysis.");

        // ── Window management ─────────────────────────────────────────────────────

        addComment("0060db20", "CreateGameWindow",
            "Creates the main game window.\n\n" +
            "Fullscreen mode (DAT_008afbd9 != 0):\n" +
            "  Style: WS_POPUP (0x80000000)\n" +
            "  uFlags: 0x2003 (SWP_NOMOVE|SWP_NOSIZE|SWP_SHOWWINDOW?)\n" +
            "  Position: (0,0) via SetWindowPos\n" +
            "  hWndInsertAfter: HWND_TOPMOST (-1) - stays on top\n" +
            "  dwExStyle: WS_EX_TOPMOST (8)\n" +
            "  SetMenu(hWnd, NULL) - remove menu bar\n" +
            "  SetThreadExecutionState(0x80000002) - prevent sleep/screensaver\n\n" +
            "Windowed mode:\n" +
            "  Style: WS_OVERLAPPEDWINDOW (0xcf0000)\n" +
            "  uFlags: 3 (SWP_NOMOVE|SWP_NOSIZE)\n" +
            "  Position: from registry PosX/PosY (defaults 300,32)\n\n" +
            "Globals:\n" +
            "  DAT_00bf1940: screen width / fullscreen flag (0=windowed)\n" +
            "  DAT_008afbd9: fullscreen flag (bool)\n" +
            "Returned HWND is stored in DAT_00bef6cc by caller.");

        addComment("0060d6d0", "WindowProc",
            "Main window procedure. Handles all Win32 messages.\n\n" +
            "Additional messages vs. first pass:\n" +
            "  WM_SIZE (0x05): return 0 (suppress default)\n" +
            "  WM_ERASEBKGND (0x14): return 0 (suppress erase)\n" +
            "  WM_ACTIVATEAPP (0x1c): return 0\n" +
            "  WM_SETFOCUS/WM_SETCURSOR (0x07/0x20):\n" +
            "    When gaining focus: also calls ShowCursor loop + D3D ShowCursor(0)\n" +
            "    When losing focus: FUN_00617b60() + UnacquireInputDevices()\n" +
            "  WM_WTSSESSION_CHANGE (0x218):\n" +
            "    param_3==0 or 7: return 1 (session connect/disconnect)\n" +
            "    other: return -1\n\n" +
            "WM_ACTIVATE focus-loss side effects:\n" +
            "  - FUN_0061ef80(): pause music/audio\n" +
            "  - FUN_0058b790(0): pause physics/updates\n" +
            "  - Sets DAT_00bef6d4 (audio pause flag)\n" +
            "  - Sets DAT_00bef6d5 (update pause flag)\n" +
            "  - Checks DAT_00c7b908 before calling FUN_0058b790\n\n" +
            "DAT_00e6b384 is a game-state object; +0xc is a sub-object pointer\n" +
            "DAT_00e6b2dc+4 is a pointer to an object with a flag at +0xa0");

        addComment("0060dc10", "MainLoop",
            "Main game loop. Runs until WM_QUIT or DAT_00bef6c5 exit flag.\n\n" +
            "Focus-loss handling (fullscreen, no focus):\n" +
            "  When DAT_00bef67e transitions: calls FUN_00612530 to change\n" +
            "  graphics output mode (e.g. minimise render or switch scene).\n" +
            "  DAT_00c82b00/08/ac are scene/mode IDs checked against each other.\n\n" +
            "Frame rate limiter:\n" +
            "  uStack_44 = ROUND((accumHigh * DAT_008475d8 + accumLow) * DAT_00845594 * DAT_00845320)\n" +
            "  These floats encode the target frame period based on accumulated time.\n\n" +
            "Game update sub-system:\n" +
            "  thunk_FUN_00eb6dbc(): queries available video memory (or similar)\n" +
            "  result stored in DAT_008afb08 (min across frames = memory pressure?)\n" +
            "  FUN_00612f00(): one-shot init, registered with _atexit(FUN_007b5640)\n\n" +
            "Delayed operation timer (DAT_00bef6d8):\n" +
            "  When it expires:\n" +
            "    - If DAT_00bef6d4==0 and game ptr non-null: thunk_FUN_00ec67e8()\n" +
            "    - If DAT_00bef6d5==0: FUN_0058b8a0() (resume physics/updates)");

        // ── WinMain extra details ─────────────────────────────────────────────────

        addComment("0060dfa0", "WinMain",
            "Main entry point for Harry Potter and the Order of the Phoenix.\n\n" +
            "Registry settings loaded (all from GameSettings):\n" +
            "  Width         (DAT_00bf1940) = non-zero triggers fullscreen\n" +
            "  BitDepth      (DAT_00bf1944)\n" +
            "  ShadowLOD     (DAT_00bf1948, default 6)\n" +
            "  MaxTextureSize(DAT_00bf194c)\n" +
            "  MaxShadowTextureSize (DAT_00bf1950)\n" +
            "  MaxFarClip    (DAT_00bf1954)\n" +
            "  CullDistance  (DAT_00bf195c)\n" +
            "  ParticleRate  (DAT_00bf1958)\n" +
            "  ParticleCullDistance (DAT_00bf1960)\n" +
            "  DisableFog    (DAT_00bf1964)\n" +
            "  DisablePostPro(DAT_00bf1968)\n" +
            "  FilterFlip    (DAT_00bf196c)\n" +
            "  AAMode        (DAT_00bf1970) - anti-aliasing quality\n" +
            "  UseAdditionalModes (DAT_00bf1974)\n" +
            "  Mode0..5Width (array DAT_009a78a0[6])\n" +
            "  Mode0..5Height(array DAT_009e4c90[6])\n" +
            "  OptionResolution (DAT_00bf197c)\n" +
            "  OptionLOD     (DAT_008ae1ec, default 1)\n" +
            "  OptionBrightness (DAT_008ae1f0, default 5)\n\n" +
            "Command-line flags parsed by FUN_00617bf0:\n" +
            "  'fullscreen' - forces fullscreen + optional width value\n" +
            "  'widescreen' - sets DAT_008ae1dc to widescreen aspect ratio constant\n\n" +
            "Window placement restored from registry in windowed mode\n" +
            "(PosX default 300, PosY default 32, SizeX default 640, SizeY default 480)\n" +
            "Maximized/Minimized flags also restored.\n\n" +
            "After MainLoop exits:\n" +
            "  Calls thunk_FUN_00ec6610() (render teardown?)\n" +
            "  Releases DAT_00bef6d0 COM object\n" +
            "  Calls UnacquireInputDevices + ShowCursor\n" +
            "  Calls UpdateSystemParameters(1) to restore system params\n" +
            "  TerminateProcess(GetCurrentProcess(), 1) - hard exit\n\n" +
            "On window creation failure: TerminateProcess with exit code 0.");

        // ── Helper functions ──────────────────────────────────────────────────────

        addComment("00617bf0", "ParseCommandLineArg",
            "Parses a single named argument from the game's command line string.\n\n" +
            "Params:\n" +
            "  param_1: pointer to command line buffer (DAT_00c82b88)\n" +
            "  param_2: argument name to find (e.g. 'fullscreen', 'widescreen')\n" +
            "  param_3: pointer-to-pointer for value string, or NULL to just check presence\n\n" +
            "Returns: non-zero if the argument was found on the command line.\n" +
            "If param_3 != NULL and a value follows the flag, *param_3 points to the value.");

        addComment("0060ca20", "ReadRegistrySettingStr",
            "Reads a string value from the Windows Registry (as opposed to ReadRegistrySetting\n" +
            "which reads integers).\n\n" +
            "Used to read window placement strings (PosX, SizeX, Maximized, Minimized)\n" +
            "and compare them with expected values like 'true'.\n\n" +
            "Returns a std::basic_string with the value (default supplied as param_4).\n" +
            "Registry path: HKCU\\Software\\Electronic Arts\\<app>\\<section>\\<key>");

        addComment("00617b60", "PauseGraphicsState",
            "Called when the window loses focus or on WM_SETCURSOR while unfocused.\n" +
            "Likely pauses or saves the current graphics/rendering state.\n" +
            "Paired with AcquireInputDevices to handle alt-tab in fullscreen.");

        addComment("0061ef80", "PauseAudio",
            "Called when the window loses focus (WM_ACTIVATE deactivate).\n" +
            "Pauses background music or audio systems.\n" +
            "Counterpart: resume is triggered when DAT_00bef6d8 timer expires\n" +
            "via thunk_FUN_00ec67e8().");

        addComment("0058b790", "PauseGameUpdates",
            "Called with param_1=0 to pause the physics/game-object update system.\n" +
            "Called when the window loses focus.\n" +
            "Counterpart: FUN_0058b8a0() resumes when the delayed op timer expires.");

        addComment("0058b8a0", "ResumeGameUpdates",
            "Called when the delayed operation timer (DAT_00bef6d8) expires.\n" +
            "Resumes the physics/game-object update system that was paused by\n" +
            "FUN_0058b790(0) on focus loss.");

        addComment("00612530", "SwitchRenderOutputMode",
            "Switches the rendering output mode or scene.\n" +
            "Called from MainLoop when focus state changes.\n" +
            "Params: pointer to a mode/scene ID\n" +
            "Uses DAT_00c82b00, DAT_00c82b08, DAT_00c82ac8 as mode IDs.");

        printf("UpdateCommentsV2: all comments applied\n");
    }

    private void addComment(String addrStr, String newName, String comment) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            Function f = getFunctionAt(addr);
            if (f == null) {
                printf("SKIP (no function): %s\n", addrStr);
                return;
            }
            String cur = f.getName();
            if (cur.startsWith("FUN_") || cur.startsWith("thunk_FUN_")) {
                f.setName(newName, SourceType.USER_DEFINED);
                printf("Renamed %s -> %s\n", addrStr, newName);
            }
            f.setComment(comment);
            printf("Comment set: %s (%s)\n", f.getName(), addrStr);
        } catch (Exception e) {
            printf("ERROR %s: %s\n", addrStr, e.getMessage());
        }
    }
}
