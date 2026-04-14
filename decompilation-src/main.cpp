#include "globals.h"
#include <stdio.h>
#include <float.h>
#include <string.h>
#include <sstream>
#include <string>

// ── Global variable definitions ───────────────────────────────────────────────

HWND ghWnd = NULL;
bool bIsFullscreen = false;
bool bHasFocus = true;
int gWidth = DEFAULT_WINDOW_WIDTH;
int gHeight = DEFAULT_WINDOW_HEIGHT;

IDirect3D9*       g_pD3D          = NULL;
IDirect3DDevice9* g_pd3dDevice    = NULL;
IDirect3DSurface9* g_pBackBuffer  = NULL;
IDirect3DSurface9* g_pRenderTarget = NULL;
IDirect3DSurface9* g_pAdditionalRT = NULL;
IDirect3DSurface9* g_pCachedRT    = NULL;
IDirect3DSurface9* g_pCachedDS    = NULL;
IUnknown*          g_pGPUSyncQuery = NULL;
D3DPRESENT_PARAMETERS g_d3dpp      = {0};  // DAT_00b94af8 saved for Reset
IUnknown*          g_pComObject    = NULL;  // DAT_00bef6d0

IDirectInput8*       g_pDirectInput  = NULL;
IDirectInputDevice8* g_pKeyboard     = NULL;
IDirectInputDevice8* g_pMouse        = NULL;
IDirectInputDevice8* g_pJoystick[MAX_JOYSTICKS] = {NULL, NULL};

// Timing: mirrors the 64-bit 16.16 fixed-point state from the original.
DWORD    g_dwStartupTime        = 0;
bool     g_bTimebaseInit        = false;
ULONGLONG g_ullAccumTime        = 0; // DAT_00c83198/9c
ULONGLONG g_ullNextCallback     = 0; // DAT_00c831a8/ac
ULONGLONG g_ullCallbackInterval = 0; // DAT_00c83190/94
int      g_nFrameFlip           = 0; // DAT_008e1648
DWORD    g_dwGameTicks          = 0; // DAT_00c83110

bool g_bExitRequested       = false;

SystemParams   g_savedParams = {0};
GraphicsSettings g_gfxSettings = {0};

DWORD g_dwDelayedOpTimer   = 0;
bool  g_bHasFocusLost      = false;
bool  g_bCursorVisible     = false;
bool  g_bGameUpdateEnabled = false;
bool  g_bAudioWasPaused    = false;
bool  g_bUpdatesWerePaused = false;
bool  g_bDeviceLost        = false;
int   g_nPauseState        = 0;
SIZE_T g_nMinFreeMemory    = ~(SIZE_T)0;
bool  g_bSubsysInitialized = false;

char g_szCmdLine1[CMDLINE_BUFFER_SIZE] = {0};
char g_szCmdLine2[CMDLINE_BUFFER_SIZE] = {0};

// ── Registry helpers ──────────────────────────────────────────────────────────

int ReadRegistrySetting(const char* appName, const char* section,
                        const char* key, int defaultValue) {
    char regPath[REG_PATH_BUFFER_SIZE];
    sprintf(regPath, "Software\\Electronic Arts\\%s\\%s", appName, section);

    HKEY hKey;
    DWORD value = (DWORD)defaultValue;
    DWORD dataSize = sizeof(DWORD);

    if (RegOpenKeyExA(HKEY_CURRENT_USER, regPath, 0, KEY_READ, &hKey) == ERROR_SUCCESS) {
        RegQueryValueExA(hKey, key, NULL, NULL, (LPBYTE)&value, &dataSize);
        RegCloseKey(hKey);
        return (int)value;
    }

    if (RegOpenKeyExA(HKEY_LOCAL_MACHINE, regPath, 0, KEY_READ, &hKey) == ERROR_SUCCESS) {
        RegQueryValueExA(hKey, key, NULL, NULL, (LPBYTE)&value, &dataSize);
        RegCloseKey(hKey);
        // Original (FUN_0060cc70): write the found value back to HKCU so future reads are faster.
        HKEY hkcu;
        if (RegCreateKeyExA(HKEY_CURRENT_USER, regPath, 0, NULL, 0,
                            KEY_WRITE, NULL, &hkcu, NULL) == ERROR_SUCCESS) {
            RegSetValueExA(hkcu, key, 0, REG_DWORD, (LPBYTE)&value, sizeof(DWORD));
            RegCloseKey(hkcu);
        }
        return (int)value;
    }

    // Not found anywhere: create with default value in HKCU
    if (RegCreateKeyExA(HKEY_CURRENT_USER, regPath, 0, NULL, 0,
                        KEY_WRITE, NULL, &hKey, NULL) == ERROR_SUCCESS) {
        RegSetValueExA(hKey, key, 0, REG_DWORD, (LPBYTE)&defaultValue, sizeof(DWORD));
        RegCloseKey(hKey);
    }

    return defaultValue;
}

void WriteRegistrySetting(const char* appName, const char* section,
                          const char* key, const char* value) {
    char regPath[REG_PATH_BUFFER_SIZE];
    sprintf(regPath, "Software\\Electronic Arts\\%s\\%s", appName, section);

    HKEY hKey;
    if (RegCreateKeyExA(HKEY_CURRENT_USER, regPath, 0, NULL, 0,
                        KEY_WRITE, NULL, &hKey, NULL) == ERROR_SUCCESS) {
        RegSetValueExA(hKey, key, 0, REG_SZ, (LPBYTE)value, (DWORD)(strlen(value) + 1));
        RegCloseKey(hKey);
    }
}

bool ReadRegistrySettingStr(const char* appName, const char* section,
                            const char* key, const char* defaultValue,
                            char* outBuf, int outBufSize) {
    char regPath[REG_PATH_BUFFER_SIZE];
    sprintf(regPath, "Software\\Electronic Arts\\%s\\%s", appName, section);

    HKEY hKey;
    DWORD dataType, dataSize = (DWORD)outBufSize;

    if (RegOpenKeyExA(HKEY_CURRENT_USER, regPath, 0, KEY_READ, &hKey) == ERROR_SUCCESS) {
        LSTATUS s = RegQueryValueExA(hKey, key, NULL, &dataType, (LPBYTE)outBuf, &dataSize);
        RegCloseKey(hKey);
        if (s == ERROR_SUCCESS) return true;
    }

    if (RegOpenKeyExA(HKEY_LOCAL_MACHINE, regPath, 0, KEY_READ, &hKey) == ERROR_SUCCESS) {
        LSTATUS s = RegQueryValueExA(hKey, key, NULL, &dataType, (LPBYTE)outBuf, &dataSize);
        RegCloseKey(hKey);
        if (s == ERROR_SUCCESS) return true;
    }

    strncpy(outBuf, defaultValue, outBufSize - 1);
    outBuf[outBufSize - 1] = '\0';
    return false;
}

// ── System parameter management ───────────────────────────────────────────────
// Saves or restores mouse acceleration and screen reader settings.
// restore=false: reads current values, clears acceleration bits, writes back.
// restore=true:  writes saved values back verbatim.
// Only modifies settings if bit 0 of the flag word is clear (acceleration active).
void SaveOrRestoreSystemParameters(bool restore) {
    if (restore) {
        SystemParametersInfoA(SPI_SET_MOUSE_SPEED,          SPI_MOUSE_PARAM_SIZE,
                              &g_savedParams.mouseSpeed,   0);
        SystemParametersInfoA(SPI_SET_MOUSE_ACCEL,          SPI_MOUSE_PARAM_SIZE,
                              &g_savedParams.mouseAccel,   0);
        SystemParametersInfoA(SPI_SET_SCREEN_READER_PARAMS, SPI_SCREEN_READER_PARAM_SIZE,
                              &g_savedParams.screenReader, 0);
    } else {
        SystemParametersInfoA(SPI_GET_MOUSE_SPEED,          SPI_MOUSE_PARAM_SIZE,
                              &g_savedParams.mouseSpeed,   0);
        SystemParametersInfoA(SPI_GET_MOUSE_ACCEL,          SPI_MOUSE_PARAM_SIZE,
                              &g_savedParams.mouseAccel,   0);
        SystemParametersInfoA(SPI_GET_SCREEN_READER_PARAMS, SPI_SCREEN_READER_PARAM_SIZE,
                              &g_savedParams.screenReader, 0);

        // Only disable acceleration if bit 0 is already 0 (not already disabled).
        if ((g_savedParams.MOUSE_SPEED_FLAGS & 1) == 0) {
            UINT f = g_savedParams.MOUSE_SPEED_FLAGS & MOUSE_ACCEL_FLAGS_MASK;
            UINT buf[2] = { g_savedParams.mouseSpeed[0], f };
            SystemParametersInfoA(SPI_SET_MOUSE_SPEED, SPI_MOUSE_PARAM_SIZE, buf, 0);
        }
        if ((g_savedParams.MOUSE_ACCEL_FLAGS & 1) == 0) {
            UINT f = g_savedParams.MOUSE_ACCEL_FLAGS & MOUSE_ACCEL_FLAGS_MASK;
            UINT buf[2] = { g_savedParams.mouseAccel[0], f };
            SystemParametersInfoA(SPI_SET_MOUSE_ACCEL, SPI_MOUSE_PARAM_SIZE, buf, 0);
        }
        if ((g_savedParams.SCREEN_READER_FLAGS & 1) == 0) {
            UINT f = g_savedParams.SCREEN_READER_FLAGS & MOUSE_ACCEL_FLAGS_MASK;
            UINT buf[6];
            memcpy(buf, g_savedParams.screenReader, sizeof(buf));
            buf[1] = f;
            SystemParametersInfoA(SPI_SET_SCREEN_READER_PARAMS, SPI_SCREEN_READER_PARAM_SIZE,
                                  buf, 0);
        }
    }
}

// ── Command-line argument parser ──────────────────────────────────────────────
// Searches cmdLine for " <flag>" (case-sensitive).
// If valueOut != NULL and a value token follows, *valueOut is set to point to it.
// Returns true if the flag was found.
bool ParseCommandLineArg(const char* cmdLine, const char* flag, const char** valueOut) {
    if (!cmdLine || !flag) return false;

    const char* p = cmdLine;
    size_t flagLen = strlen(flag);

    while (*p) {
        // Skip whitespace
        while (*p == ' ' || *p == '\t') p++;
        if (!*p) break;

        if (_strnicmp(p, flag, flagLen) == 0) {
            const char* after = p + flagLen;
            if (*after == '\0' || *after == ' ' || *after == '\t' || *after == '=') {
                if (valueOut) {
                    if (*after == '=') after++;
                    while (*after == ' ' || *after == '\t') after++;
                    *valueOut = (*after && *after != '-') ? after : NULL;
                }
                return true;
            }
        }
        // Advance past this token
        while (*p && *p != ' ' && *p != '\t') p++;
    }
    return false;
}

// ── Settings load ─────────────────────────────────────────────────────────────
void LoadGameSettings() {
    static const char* kApp     = "Harry Potter and the Order of the Phoenix";
    static const char* kSection = "GameSettings";

    // Width: non-zero value triggers fullscreen AND sets the screen width.
    g_gfxSettings.width = ReadRegistrySetting(kApp, kSection, "Width", 0);
    if (g_gfxSettings.width != 0) {
        bIsFullscreen = true;
        gWidth = g_gfxSettings.width;
    }

    g_gfxSettings.bitDepth            = ReadRegistrySetting(kApp, kSection, "BitDepth", 0);
    g_gfxSettings.shadowLOD           = ReadRegistrySetting(kApp, kSection, "ShadowLOD", DEFAULT_SHADOW_LOD);
    g_gfxSettings.maxTextureSize      = ReadRegistrySetting(kApp, kSection, "MaxTextureSize", 0);
    g_gfxSettings.maxShadowTextureSize= ReadRegistrySetting(kApp, kSection, "MaxShadowTextureSize", 0);
    g_gfxSettings.maxFarClip          = ReadRegistrySetting(kApp, kSection, "MaxFarClip", 0);
    g_gfxSettings.cullDistance        = ReadRegistrySetting(kApp, kSection, "CullDistance", 0);
    g_gfxSettings.particleRate        = ReadRegistrySetting(kApp, kSection, "ParticleRate", 0);
    g_gfxSettings.particleCullDistance= ReadRegistrySetting(kApp, kSection, "ParticleCullDistance", 0);
    g_gfxSettings.disableFog          = ReadRegistrySetting(kApp, kSection, "DisableFog", 0);
    g_gfxSettings.disablePostPro      = ReadRegistrySetting(kApp, kSection, "DisablePostPro", 0);
    g_gfxSettings.filterFlip          = ReadRegistrySetting(kApp, kSection, "FilterFlip", 0);
    g_gfxSettings.aaMode              = ReadRegistrySetting(kApp, kSection, "AAMode", 0);
    g_gfxSettings.useAdditionalModes  = ReadRegistrySetting(kApp, kSection, "UseAdditionalModes", 0);
    g_gfxSettings.optionResolution    = ReadRegistrySetting(kApp, kSection, "OptionResolution", 0);
    g_gfxSettings.optionLOD           = ReadRegistrySetting(kApp, kSection, "OptionLOD", DEFAULT_OPTION_LOD);
    g_gfxSettings.optionBrightness    = ReadRegistrySetting(kApp, kSection, "OptionBrightness", DEFAULT_BRIGHTNESS);

    // Load up to 6 custom display mode width/height pairs
    char keyBuf[32];
    for (int i = 0; i < MAX_DISPLAY_MODES; i++) {
        sprintf(keyBuf, "Mode%dWidth", i);
        g_gfxSettings.modeWidths[i]  = ReadRegistrySetting(kApp, kSection, keyBuf, 0);
        sprintf(keyBuf, "Mode%dHeight", i);
        g_gfxSettings.modeHeights[i] = ReadRegistrySetting(kApp, kSection, keyBuf, 0);
    }
}

// ── Window placement save ─────────────────────────────────────────────────────
void SaveWindowPlacement(HWND hWnd) {
    static const char* kApp     = "Harry Potter and the Order of the Phoenix";
    static const char* kSection = "GameSettings";

    WINDOWPLACEMENT wp = {sizeof(WINDOWPLACEMENT)};
    if (!GetWindowPlacement(hWnd, &wp)) return;

    // Adjust the normal rect by removing border thickness so saved values
    // refer to the client area position, matching the original's logic.
    RECT adj = {0, 0, 0, 0};
    AdjustWindowRect(&adj, 0xcf0000, FALSE);

    RECT r = wp.rcNormalPosition;
    r.left   -= adj.left;
    r.top    -= adj.top;
    r.right  += adj.right;
    r.bottom += adj.bottom;

    char buf[POSITION_BUFFER_SIZE];

    sprintf(buf, "%ld", r.left);
    WriteRegistrySetting(kApp, kSection, "PosX", buf);
    sprintf(buf, "%ld", r.top);
    WriteRegistrySetting(kApp, kSection, "PosY", buf);
    sprintf(buf, "%ld", r.right - r.left);
    WriteRegistrySetting(kApp, kSection, "SizeX", buf);
    sprintf(buf, "%ld", r.bottom - r.top);
    WriteRegistrySetting(kApp, kSection, "SizeY", buf);

    WriteRegistrySetting(kApp, kSection, "Minimized",
                         (wp.showCmd == SW_SHOWMINIMIZED) ? "true" : "false");
    WriteRegistrySetting(kApp, kSection, "Maximized",
                         (wp.showCmd == SW_SHOWMAXIMIZED) ? "true" : "false");
}

// ── DirectX resource management ───────────────────────────────────────────────

void ReleaseDirectXResources() {
    // Pre-release cleanup (thunk_FUN_00ec04dc — unknown, called before surface releases)
    // TODO: thunk_FUN_00ec04dc()

    // Release GPU sync query
    if (g_pGPUSyncQuery) {
        g_pGPUSyncQuery->Release();
        g_pGPUSyncQuery = NULL;
    }

    // Clear cached surface pointers before releasing (matches original order)
    g_pCachedRT = NULL;
    g_pCachedDS = NULL;

    // Release render target texture (if not the AA sentinel value).
    // When AA path is active, DAT_00bf1934 = 0xbacb0ffe and nothing is released.
    if (g_pRenderTarget && g_pRenderTarget != (IDirect3DSurface9*)D3D_AA_PATH_SENTINEL) {
        g_pRenderTarget->Release();
        g_pRenderTarget = NULL;
    }

    if (g_pBackBuffer) {
        g_pBackBuffer->Release();
        g_pBackBuffer = NULL;
    }

    if (g_pAdditionalRT) {
        g_pAdditionalRT->Release();
        g_pAdditionalRT = NULL;
    }

    // Post-release cleanup (thunk_FUN_00ec19b5 — unknown, called after surface releases)
    // TODO: thunk_FUN_00ec19b5()
}

// ── DirectX subsystem init stubs ──────────────────────────────────────────────
// These are stubs; the full implementations are in deeper engine code.

void InitRenderStates() {
    // FUN_00675950: Uploads vertex/pixel shaders to device and sets initial render states.
    // TODO: UploadVertexShaders(), UploadPixelShaders()
}

void CreateGPUSyncQuery() {
    // FUN_0067b820: Creates a D3DQUERYTYPE_EVENT query for GPU-CPU synchronization.
    // Stored in g_pGPUSyncQuery. Type depends on g_gfxSettings.filterFlip (swap effect).
    // TODO: g_pd3dDevice->CreateQuery(D3DQUERYTYPE_EVENT, (IDirect3DQuery9**)&g_pGPUSyncQuery)
}

void InitD3DStateDefaults() {
    // FUN_00674430: Initializes render state, sampler state, and texture stage state
    // tables to DirectX defaults. Uses SetCachedRenderState to avoid redundant calls.
    // Also conditionally adjusts blend states for hardware capability differences.
    // TODO: set render state cache array, sampler states, texture stage states
}

void PauseGraphicsState() {
    // FUN_00617b60: Pauses render output. Called on focus loss to prevent
    // rendering while the window does not have focus.
    // TODO: implement render pause
}

void SwitchRenderOutputMode() {
    // FUN_00612530: Dispatches a render-mode change to the listener list.
    // Uses scene IDs (DAT_00c82b00/08) compared against target (DAT_00c82ac8).
    // TODO: implement observer dispatch
}

SIZE_T QueryMemoryAllocatorMax() {
    // thunk_FUN_00eb6dbc: Returns the size of the largest free block in the
    // game's internal memory allocator (thread-safe via critical section).
    // Used to track memory pressure (low-water mark in g_nMinFreeMemory).
    return 0; // TODO: implement allocator query
}

void CLI_CommandParser_ParseArgs() {
    // FUN_00eb787a: Parses extended command-line arguments from g_szCmdLine1 into a
    // CLI::CommandParser object (DAT_00e6b328, allocated 0xc bytes with tag "CLI::CommandParser").
    // Stores up to 0x20 name pointers at this+0x480 and value pointers at this+0x500.
    // Positional args at this+0x400.
    // Additional flags processed here: -oldgen, -showfps, -memorylwm, -nofmv.
    // TODO: AllocEngineObject(0xc, "CLI::CommandParser") and parse g_szCmdLine1
}

void PreDirectXInit() {
    // thunk_FUN_00ec64f9: Sets up audio/render context before D3D device creation.
    // Stores DAT_00bef6d0 (engine object) into DAT_00bf1b18 (audio subsystem reference).
    // Clears audio-present flag DAT_00bf1b10 (set non-zero by FUN_006ac0b0/hardware detect).
    // Copies audio device string into DAT_00be93d0.
    // Creates audio output context DAT_00bf1b1c via thunk_FUN_00ec72a9.
    // Sets DAT_00bf1b14 = 1 (audio pre-init flag).
    // TODO: FUN_006ac0b0(), thunk_FUN_00ec6e91() (audio hardware detection)
    // TODO: DAT_00bf1b1c = thunk_FUN_00ec72a9()
}

void InitDirectXAndSubsystems(int height) {
    // thunk_FUN_00eb612e: Creates D3D device (InitCLIAndTimingAndDevice),
    // engine objects (InitEngineObjects), registers message handlers (FinalizeDeviceSetup),
    // initializes audio subsystem (InitAudioSubsystem).
    // Sets DAT_008df65a=1, DAT_00aeea5c=2.
    // TODO: implement
    (void)height;
}

void InitGameSubsystems() {
    // thunk_FUN_00eb496e: Registers frame callbacks, enumerates DirectInput
    // devices (FUN_00688370(4,0)), loads language selection screen.
    // Sets DAT_00bef6c6 = 1 (subsystem initialized flag).
    // TODO: implement
}

void SaveOptionsOnExit() {
    // thunk_FUN_00eb4a5d: Writes user-modified settings back to registry on exit.
    // Saves OptionResolution (DAT_00bf197c), OptionLOD (DAT_008ae1ec),
    // OptionBrightness (DAT_008ae1f0) via WriteRegistrySetting integer helper (FUN_0060cc70).
    // Original uses std::basic_string for the key/section names internally.
    static const char* kApp     = "Harry Potter and the Order of the Phoenix";
    static const char* kSection = "GameSettings";
    char buf[32];
    sprintf(buf, "%d", g_gfxSettings.optionResolution);
    WriteRegistrySetting(kApp, kSection, "OptionResolution", buf);
    sprintf(buf, "%d", g_gfxSettings.optionLOD);
    WriteRegistrySetting(kApp, kSection, "OptionLOD", buf);
    sprintf(buf, "%d", g_gfxSettings.optionBrightness);
    WriteRegistrySetting(kApp, kSection, "OptionBrightness", buf);
}

void UpdateCursorVisibilityAndScene() {
    // FUN_00ea53ca: Compares the requested cursor-visible state with the cached
    // g_bCursorVisible (DAT_00bef67e). If they differ, dispatches a render mode change:
    //   cursor=false (focus gained): SwitchRenderOutputMode(&DAT_00c82b08)
    //   cursor=true  (focus lost):  SwitchRenderOutputMode(&DAT_00c82b00)
    // State updates: DAT_00bef67c, DAT_00bef67d are also checked (additional conditions).
    // The actual state is passed in AL (EAX low byte) — the new cursor-visible flag.
    // This stub passes the current g_bCursorVisible as the desired new state.
    // TODO: implement SwitchRenderOutputMode dispatch
    (void)SwitchRenderOutputMode;  // suppress unused warning
}

void RestoreDirectXResources() {
    if (!g_pd3dDevice) return;

    // Step 1: Re-upload shaders and set initial render states
    InitRenderStates();

    // Step 2: Create GPU sync query if not already present
    if (!g_pGPUSyncQuery) {
        CreateGPUSyncQuery();
    }

    // Get back buffer
    g_pd3dDevice->GetBackBuffer(0, 0, D3DBACKBUFFER_TYPE_MONO, &g_pBackBuffer);
    g_pCachedRT = g_pBackBuffer;

    // Get back buffer surface descriptor
    D3DSURFACE_DESC desc = {D3DFMT_UNKNOWN};
    if (g_pBackBuffer) {
        g_pBackBuffer->GetDesc(&desc);
    }

    // Create additional render target (always, regardless of AA mode)
    g_pd3dDevice->CreateRenderTarget(desc.Width, desc.Height, desc.Format,
                                     D3DMULTISAMPLE_NONE, 0, FALSE,
                                     &g_pAdditionalRT, NULL);

    if (g_gfxSettings.aaMode == 0) {
        // Non-AA path: create a texture render target for post-processing.
        // The original also performs a hardware capability check here before
        // deciding to create this path.
        IDirect3DTexture9* pTex = NULL;
        if (SUCCEEDED(g_pd3dDevice->CreateTexture(desc.Width, desc.Height, 1,
                                                   D3DUSAGE_RENDERTARGET, desc.Format,
                                                   D3DPOOL_DEFAULT, &pTex, NULL))) {
            // Release old back buffer reference, replace with texture surface
            if (g_pBackBuffer) g_pBackBuffer->Release();
            pTex->GetSurfaceLevel(0, &g_pBackBuffer);
            g_pRenderTarget = (IDirect3DSurface9*)(IUnknown*)pTex;
            g_pCachedRT = g_pBackBuffer;
        }
    } else {
        // AA path: mark with sentinel so release code skips it
        g_pRenderTarget = (IDirect3DSurface9*)D3D_AA_PATH_SENTINEL;
    }

    g_pCachedDS = g_pAdditionalRT;

    // Bind render targets
    if (g_pCachedRT) g_pd3dDevice->SetRenderTarget(0, g_pCachedRT);
    if (g_pCachedDS) g_pd3dDevice->SetDepthStencilSurface(g_pCachedDS);

    // Step 6: Initialize D3D render state, sampler state, and texture stage defaults
    InitD3DStateDefaults();
}

void UpdateDirectXDevice() {
    if (!g_pd3dDevice) return;

    // Query available texture memory; if < 33 MB, trigger low-memory handling
    UINT availMB = g_pd3dDevice->GetAvailableTextureMem() >> 20;
    if (availMB > 0 && availMB < 33) {
        // Low video memory: could reduce texture quality here
    }

    HRESULT hr = g_pd3dDevice->TestCooperativeLevel();
    if (FAILED(hr)) {
        g_bDeviceLost = true;

        if (hr == D3DERR_DEVICELOST) {
            Sleep(DEVICE_LOST_SLEEP_MS);
            return;
        }

        if (hr == D3DERR_DEVICENOTRESET) {
            ReleaseDirectXResources();

            // Reuse the saved present parameters (g_d3dpp, matching DAT_00b94af8).
            // These were set up by InitDirectXAndSubsystems; only update volatile fields.
            g_d3dpp.BackBufferWidth  = gWidth;
            g_d3dpp.BackBufferHeight = gHeight;
            g_d3dpp.hDeviceWindow    = ghWnd;
            g_d3dpp.Windowed         = !bIsFullscreen;

            if (SUCCEEDED(g_pd3dDevice->Reset(&g_d3dpp))) {
                RestoreDirectXResources();
            }
        }
        // Unexpected error: original calls FatalError("Invalid device lost state %d\n", hr)
        return;
    }

    g_bDeviceLost = false;
}

// ── Input device acquire/unacquire ────────────────────────────────────────────

void UnacquireInputDevices() {
    if (g_pKeyboard) g_pKeyboard->Unacquire();
    if (g_pMouse)    g_pMouse->Unacquire();
    ShowCursor(TRUE);
    for (int i = 0; i < MAX_JOYSTICKS; i++) {
        if (g_pJoystick[i]) g_pJoystick[i]->Unacquire();
    }
}

void AcquireInputDevices() {
    if (g_pKeyboard) g_pKeyboard->Acquire();
    if (g_pMouse)    g_pMouse->Acquire();
    ShowCursor(FALSE);
    for (int i = 0; i < MAX_JOYSTICKS; i++) {
        if (g_pJoystick[i]) g_pJoystick[i]->Acquire();
    }
}

// ── Audio/update pause system ─────────────────────────────────────────────────
// These are stubs; the real implementations are complex state machines that
// interact with game object lists.

void AudioStream_Resume() {
    // thunk_FUN_00ec67e8: Resumes the active audio stream after a focus-loss pause.
    // Uses in_EAX (non-standard calling convention) as the audio stream/track object:
    //   this+0x28 = pause flag → clear to 0
    //   this+0x16/17 = pause timestamp / cumulative playback time (updated)
    //   Calls thunk_FUN_00ec693d(0x1000) (decoder speed), thunk_FUN_00ec66f1() (flush)
    //   Pitch correction: if pitch == 0x1000, add elapsed directly; else scale by pitch/0x1000
    // TODO: call on active audio track object
}

void PauseAudioManager() {
    // Original: checks if a music track is loaded, then calls FUN_006a9ea0()
    // to pause the audio stream.
    // TODO: implement audio manager pause
}

void PauseGameObjects(int param) {
    // Original: state machine on DAT_00c7b908.
    // Triggers "chapter-cutscene-in" animation, calls Pause() vtable method
    // on all registered game systems (audio manager, physics, etc.).
    // Records pause time: _DAT_00c7b90c = DAT_00c8311c/3 + param
    if (g_nPauseState < 4 || g_nPauseState > 5) {
        // Full pause path
        // TODO: pause animations via FUN_0040efb0
        // TODO: call vtable+0x24 on DAT_00c7c370, DAT_00c7b924, and DAT_00c7ba4c array
        g_nPauseState = PAUSE_STATE_FULL;
    } else {
        // Audio-only pause path (states 4-5)
        // TODO: pause audio manager only
        g_nPauseState = PAUSE_STATE_NOOP;
    }
}

void ResumeGameObjects() {
    // Original: state machine resumes all paused systems.
    // Triggers "default" animation and calls Resume() vtable method on all systems.
    if (g_nPauseState <= 3) {
        // Full resume path
        // TODO: resume animations, call vtable+0x28 on all systems
        g_nPauseState = PAUSE_STATE_RESUME;
    } else if (g_nPauseState == PAUSE_STATE_NOOP) {
        // Partial resume (audio-only path)
        // TODO: resume audio manager
        g_nPauseState = 5;
    }
}

// ── High-resolution timing ────────────────────────────────────────────────────
// Uses the Windows multimedia timer for better resolution than GetTickCount().
// Returns milliseconds since the first call (game startup).
DWORD GetGameTime() {
    TIMECAPS caps;
    timeGetDevCaps(&caps, sizeof(caps));
    timeBeginPeriod(caps.wPeriodMin);
    DWORD t = timeGetTime();
    timeEndPeriod(caps.wPeriodMin);

    if (!g_bTimebaseInit) {
        g_bTimebaseInit = true;
        g_dwStartupTime = t;
    }
    return t - g_dwStartupTime;
}

// ── Deferred render batch queue ───────────────────────────────────────────────
// In the original, DAT_00bef7c0 is a linked list of draw-call descriptor nodes.
// Each frame we process up to DEFERRED_QUEUE_BUDGET_MS worth of nodes.
// Node processing (BuildRenderBatch) fills the material table and issues D3D calls.
void ProcessDeferredCallbacks() {
    // TODO: implement deferred render batch processing.
    // Original: while list non-empty AND elapsed < 2ms:
    //   call BuildRenderBatch(head); if done, advance head = head->next
}

// ── Frame update ──────────────────────────────────────────────────────────────
void GameFrameUpdate() {
    // Step 1: process any pending deferred render batches (2ms budget)
    ProcessDeferredCallbacks();

    // Step 2: get current time as 16.16 fixed point
    DWORD tMs = GetGameTime();
    ULONGLONG tFixed = ((ULONGLONG)tMs << 16);  // ms * 0x10000

    // Step 3: compute delta, capped at 100ms to prevent spiral of death
    if (g_ullAccumTime == 0) {
        // First frame: record baseline, no update this tick
        g_ullAccumTime = tFixed;
        g_ullNextCallback = tFixed;
        return;
    }

    ULONGLONG delta = tFixed - g_ullAccumTime;
    const ULONGLONG kMaxDelta = (ULONGLONG)MAX_DELTA_TIME_MS << 16;
    if (delta > kMaxDelta) {
        delta = kMaxDelta;
    }

    // Step 4: accumulate and compute game ticks (3x speed, / 0x10000)
    g_ullAccumTime += delta;
    g_dwGameTicks = (DWORD)((g_ullAccumTime * GAME_TIME_SCALE) / TIME_FIXED_SHIFT);

    // Step 5: trigger frame callbacks when accumulated time reaches next callback time
    if (g_ullAccumTime >= g_ullNextCallback) {
        g_ullNextCallback += g_ullCallbackInterval;
        g_nFrameFlip ^= 1;

        // Store current timing in the double-buffer at DAT_00c83170[flip*8].
        // TODO: DAT_00c83170[g_nFrameFlip * 2] = g_dwGameTicks; (timing double-buffer)

        // Primary callback: UpdateFrameTimingPrimary (FUN_00617f50).
        // Stores tick/time in DAT_00c83128/c83120 double-buffer indexed by flip.
        // Increments DAT_00c83114 tick counter. Then fires (*DAT_008e1644[0])(&localTick).
        // TODO: UpdateFrameTimingPrimary(&g_dwGameTicks);
        // TODO: (*DAT_008e1644[0])(&g_dwGameTicks);

        // Secondary callback: InterpolateFrameTime (FUN_00617ee0) — smooth frame interpolation.
        // Computes t = (curr-prev)/(curr-prev) between double-buffered timing values.
        // Then fires (*DAT_008e1644[1])() — dispatches to render listener list.
        // TODO: InterpolateFrameTime();
        // TODO: (*DAT_008e1644[1])();
    }
}

// ── Window management ─────────────────────────────────────────────────────────

BOOL RegisterWindowClass(HINSTANCE hInstance) {
    // Unregister first to handle a crashed previous instance that left the class behind
    UnregisterClassA("OrderOfThePhoenixMainWndClass", hInstance);

    WNDCLASSA wc = {0};
    // Original: fullscreen uses CS_OWNDC | CS_DBLCLKS (0x2020);
    //           windowed also adds CS_VREDRAW | CS_HREDRAW.
    if (bIsFullscreen) {
        wc.style = CS_OWNDC | CS_DBLCLKS;
        // Fullscreen: no default cursor (cursor hidden; game manages it via D3D)
    } else {
        wc.style  = CS_DBLCLKS | CS_OWNDC | CS_VREDRAW | CS_HREDRAW;
        wc.hCursor = LoadCursorA(NULL, IDC_ARROW);
    }
    wc.lpfnWndProc   = WindowProc;
    wc.hInstance     = hInstance;
    wc.hbrBackground = (HBRUSH)GetStockObject(BLACK_BRUSH);
    wc.lpszClassName = "OrderOfThePhoenixMainWndClass";

    return RegisterClassA(&wc) != 0;
}

// width, height: desired client area size.
// Position in windowed mode is read from the registry (defaults: 300, 32).
// In fullscreen mode the window is always placed at (0,0) TOPMOST.
HWND CreateGameWindow(HINSTANCE hInstance, int width, int height) {
    static const char* kApp     = "Harry Potter and the Order of the Phoenix";
    static const char* kSection = "GameSettings";

    int x, y;
    char buf[POSITION_BUFFER_SIZE];
    RECT rect;
    DWORD dwStyle;
    DWORD dwExStyle;
    HWND hWndInsertAfter;

    if (bIsFullscreen) {
        // Fullscreen: WS_POPUP (borderless), always on top, no menu
        x = 0; y = 0;
        dwStyle         = WS_POPUP;
        dwExStyle       = WS_EX_TOPMOST;
        hWndInsertAfter = HWND_TOPMOST;
    } else {
        // Windowed: restore position from registry
        ReadRegistrySettingStr(kApp, kSection, "PosX", "300", buf, sizeof(buf));  x = atoi(buf);
        ReadRegistrySettingStr(kApp, kSection, "PosY", "32",  buf, sizeof(buf));  y = atoi(buf);
        dwStyle         = WS_OVERLAPPEDWINDOW;
        dwExStyle       = 0;
        hWndInsertAfter = NULL;
    }

    rect = {x, y, x + width, y + height};

    AdjustWindowRectEx(&rect, dwStyle, FALSE, dwExStyle);

    HWND hWnd = CreateWindowExA(
        dwExStyle,
        "OrderOfThePhoenixMainWndClass",
        "",       // no title bar text (original passes empty string)
        dwStyle,
        rect.left, rect.top,
        rect.right - rect.left, rect.bottom - rect.top,
        NULL, NULL, hInstance, NULL);

    if (!hWnd) return NULL;

    // Show hidden first, then position (matches original ShowWindow(hWnd,0) + SetWindowPos)
    ShowWindow(hWnd, SW_HIDE);

    if (bIsFullscreen) {
        SetWindowPos(hWnd, hWndInsertAfter, 0, 0, 0, 0,
                     SWP_NOMOVE | SWP_NOSIZE | SWP_SHOWWINDOW);
        SetMenu(hWnd, NULL);  // Remove menu bar in fullscreen
        // Prevent display from sleeping while the game is fullscreen
        SetThreadExecutionState(ES_CONTINUOUS | ES_DISPLAY_REQUIRED);
    } else {
        SetWindowPos(hWnd, NULL, 0, 0, 0, 0,
                     SWP_NOMOVE | SWP_NOSIZE);
    }

    ShowWindow(hWnd, SW_SHOW);
    return hWnd;
}

LRESULT CALLBACK WindowProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam) {
    switch (message) {
        case WM_DESTROY:
            if (bIsFullscreen) {
                while (ShowCursor(TRUE) < 1);  // ensure cursor is visible on exit
            }
            PostQuitMessage(0);
            return 0;

        // WM_SIZE, WM_ERASEBKGND, WM_ACTIVATEAPP: suppress default behavior
        case WM_SIZE:
        case WM_ERASEBKGND:
        case WM_ACTIVATEAPP:
            return 0;

        case WM_ACTIVATE:
            if (bIsFullscreen) {
                if (LOWORD(wParam) == WA_INACTIVE) {
                    // Window losing focus ─────────────────────────────────────
                    // (1) Pause graphics/rendering state
                    PauseGraphicsState();  // FUN_00617b60

                    UnacquireInputDevices();
                    while (ShowCursor(TRUE) < 1);
                    g_bHasFocusLost = true;

                    // (2) Update cursor state and dispatch render scene switch
                    // Original: UpdateCursorVisibilityAndScene() called with AL=1 (cursor shown = focus lost).
                    g_bCursorVisible = true;
                    UpdateCursorVisibilityAndScene();

                    // (3) If delayed-op timer is not already running, queue pauses
                    if (g_dwDelayedOpTimer == 0) {
                        // Pause audio if a music track is loaded
                        PauseAudioManager();
                        g_bAudioWasPaused = true;

                        // Pause game objects/physics
                        PauseGameObjects(0);
                        g_bUpdatesWerePaused = true;
                    }
                    g_dwDelayedOpTimer = 0;
                } else {
                    // Window gaining focus ────────────────────────────────────
                    // Original: acquires input via RealInputSystem (DAT_00e6b384) at vtable+0xc,
                    // then calls AcquireInputDevices().
                    // TODO: (*((RealInputSystem+0xc)->vtable[0x10/0x218]))() — device acquire via game obj
                    AcquireInputDevices();
                    while (ShowCursor(FALSE) >= 0);
                    // Update cursor state and dispatch render scene switch
                    // Original: UpdateCursorVisibilityAndScene() called with AL=0 (cursor hidden = focus gained).
                    g_bCursorVisible = false;
                    UpdateCursorVisibilityAndScene();
                    g_bHasFocusLost = false;
                    g_dwDelayedOpTimer = FOCUS_CHANGE_DELAY_MS;
                }
            }
            return 0;

        case WM_SETFOCUS:
        case WM_SETCURSOR:
            if (!g_bHasFocusLost) {
                // Have focus: hide cursor and bind D3D cursor
                while (ShowCursor(FALSE) >= 0);
                if (g_pd3dDevice) {
                    g_pd3dDevice->ShowCursor(FALSE);
                }
            } else {
                // Lost focus: show system arrow cursor
                SetCursor(LoadCursorA(NULL, IDC_ARROW));
                PauseGraphicsState();  // FUN_00617b60
                UnacquireInputDevices();
                while (ShowCursor(TRUE) < 1);
            }
            return 0;

        case WM_PAINT:
            if (bIsFullscreen) {
                ValidateRect(hWnd, NULL);
            } else {
                PAINTSTRUCT ps;
                BeginPaint(hWnd, &ps);
                EndPaint(hWnd, &ps);
            }
            return 0;

        case WM_CLOSE:
            if (!bIsFullscreen) {
                SaveWindowPlacement(hWnd);
            }
            DestroyWindow(hWnd);
            return 0;

        case WM_SYSCOMMAND:
            if (bIsFullscreen) {
                WPARAM cmd = wParam & SYSCMD_MASK;
                if (cmd == SC_MAXIMIZE || cmd == SC_SIZE ||
                    cmd == SC_MOVE     || cmd == SC_KEYMENU) {
                    return 0;   // block these in fullscreen
                }
            }
            break;

        case WM_NCHITTEST:
            if (bIsFullscreen) {
                return HTCLIENT;
            }
            break;

        case WM_ENTERMENULOOP:
            if (bIsFullscreen) {
                return MENU_LOOP_SUPPRESS;
            }
            break;

        case WM_WTSSESSION_CHANGE:
            // Terminal Services session connect (0) or lock (7): acknowledge.
            // Any other value: return -1 (reject).
            if (wParam == 0 || wParam == 7) {
                return 1;
            }
            return -1;
    }

    return DefWindowProcA(hWnd, message, wParam, lParam);
}

// ── Main game loop ────────────────────────────────────────────────────────────
void MainLoop() {
    MSG msg = {0};
    bool bCursorStateChanged = false;

    while (!g_bExitRequested) {
        // Maintain D3D device state (check/recover device lost)
        UpdateDirectXDevice();

        // Handle focus state transitions in fullscreen mode
        if (bIsFullscreen) {
            if (g_bHasFocusLost) {
                // Window lost focus: unacquire and show cursor
                UnacquireInputDevices();
                while (ShowCursor(TRUE) < 1);
                if (!g_bCursorVisible) {
                    g_bCursorVisible = true;
                    bCursorStateChanged = true;
                    // Original also calls SwitchRenderOutputMode with scene IDs
                    // from DAT_00c82b00/08 vs target DAT_00c82ac8
                    SwitchRenderOutputMode();
                }
            } else {
                if (g_bCursorVisible) {
                    g_bCursorVisible = false;
                    bCursorStateChanged = true;
                    // Original also calls SwitchRenderOutputMode here
                    SwitchRenderOutputMode();
                }
            }
        }

        // Process Windows messages (non-blocking)
        if (PeekMessageA(&msg, NULL, 0, 0, PM_REMOVE)) {
            if (msg.message == WM_QUIT) {
                g_bExitRequested = true;
                break;
            }
            if (msg.message != (UINT)-1) {   // filter invalid messages
                TranslateMessage(&msg);
                DispatchMessageA(&msg);
            }
        } else {
            // No messages: update game frame
            DWORD frameStart = timeGetTime();

            GameFrameUpdate();

            // Track minimum available memory (low-water mark for memory pressure).
            // Only when game update subsystem is enabled (matching original condition).
            if (g_bGameUpdateEnabled) {
                SIZE_T available = QueryMemoryAllocatorMax();
                if (available < g_nMinFreeMemory) {
                    g_nMinFreeMemory = available;
                }
            }

            // Count down the focus-change delayed operation timer.
            // When it reaches zero, resume any paused audio and game systems.
            DWORD elapsed = timeGetTime() - frameStart;
            if (g_dwDelayedOpTimer > 0) {
                if (elapsed < g_dwDelayedOpTimer) {
                    g_dwDelayedOpTimer -= elapsed;
                } else {
                    g_dwDelayedOpTimer = 0;

                    // Resume audio if it was paused on focus loss
                    if (g_bAudioWasPaused) {
                        // AudioStream_Resume() (thunk_FUN_00ec67e8): resumes audio stream.
                        // Uses in_EAX as this; sets pause flag, restores pitch, calls decoder.
                        AudioStream_Resume();
                    }

                    // Resume game objects/physics
                    if (g_bUpdatesWerePaused) {
                        ResumeGameObjects();
                    }

                    g_bAudioWasPaused    = false;
                    g_bUpdatesWerePaused = false;
                }
            }

            // Frame rate limiting: yield if we finished before the budget
            DWORD frameTime = timeGetTime() - frameStart;
            if (frameTime < TARGET_FRAME_TIME_MS) {
                Sleep(0);
            }
        }
    }
}

// ── WinMain ───────────────────────────────────────────────────────────────────
int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance,
                   LPSTR lpCmdLine, int nShowCmd) {

    // Prevent denormal floating-point numbers from causing a performance hit
    _control87(0x20000, 0x30000);

    // Save current system parameters (mouse acceleration, screen reader) and
    // disable mouse acceleration while the game is running.
    SaveOrRestoreSystemParameters(false);

    // Keep two copies of the command line (matches original strncpy to both buffers)
    strncpy(g_szCmdLine1, lpCmdLine, sizeof(g_szCmdLine1) - 1);
    strncpy(g_szCmdLine2, lpCmdLine, sizeof(g_szCmdLine2) - 1);

    // CLI command-line argument parsing: CLI_CommandParser_ParseArgs() (FUN_00eb787a).
    // Parses "-name=value" tokens from the command line into a CLI::CommandParser object.
    // Called before single-instance check. NOT COM init (previously misidentified).
    // Allocates CLI::CommandParser (0xc bytes, tag "CLI::CommandParser") at DAT_00e6b328.
    // Stores up to 0x20 name/value pairs (at this+0x480 / this+0x500).
    CLI_CommandParser_ParseArgs();

    // Single-instance guard: terminate if another instance is already running
    if (FindWindowA("OrderOfThePhoenixMainWndClass", NULL) != NULL) {
        TerminateProcess(GetCurrentProcess(), 0);
        return 0;
    }

    // Register window class (or fail and exit)
    if (!RegisterWindowClass(hInstance)) {
        TerminateProcess(GetCurrentProcess(), 0);
        return 0;
    }

    // Load all game settings from registry
    LoadGameSettings();

    // Parse command-line overrides
    if (ParseCommandLineArg(lpCmdLine, "fullscreen", NULL)) {
        bIsFullscreen = true;
        const char* widthStr = NULL;
        ParseCommandLineArg(lpCmdLine, "fullscreen", &widthStr);
        if (widthStr) {
            gWidth = atoi(widthStr);
        }
    }

    bool bWidescreen = ParseCommandLineArg(lpCmdLine, "widescreen", NULL);
    float aspectRatio = bWidescreen ? (16.0f / 9.0f) : (4.0f / 3.0f);

    // Determine window size (position is handled inside CreateGameWindow)
    static const char* kApp     = "Harry Potter and the Order of the Phoenix";
    static const char* kSection = "GameSettings";
    int winW, winH;

    if (!bIsFullscreen) {
        // Windowed mode: read size from registry or derive from gWidth
        if (gWidth != 0) {
            winW = gWidth;
            winH = (int)(gWidth / aspectRatio);
        } else {
            char buf[POSITION_BUFFER_SIZE];
            ReadRegistrySettingStr(kApp, kSection, "SizeX", "640", buf, sizeof(buf));  winW = atoi(buf);
            ReadRegistrySettingStr(kApp, kSection, "SizeY", "480", buf, sizeof(buf));  winH = atoi(buf);
        }
    } else {
        winW = (gWidth != 0) ? gWidth : DEFAULT_WINDOW_WIDTH;
        winH = (int)(winW / aspectRatio);
    }

    // Create main window (position is read from registry inside CreateGameWindow)
    ghWnd = CreateGameWindow(hInstance, winW, winH);
    if (!ghWnd) {
        TerminateProcess(GetCurrentProcess(), 0);
        return 0;
    }

    // Engine object factory: creates the main engine sub-object (DAT_00bef6d0).
    // Original: GetOrInitCallbackManager() → calls factory via callback system vtable
    // with size=0xb58 (2904 bytes) and magic {0x88332000000001, 0}; result in DAT_00bef6d0.
    // Released at exit via (**(callback_mgr + 0xc))(DAT_00bef6d0, 0).
    // TODO: g_pComObject = GetOrInitCallbackManager().CreateEngineObject(0xb58, ...)

    // Pre-DirectX init: sets up audio/render context (thunk_FUN_00ec64f9).
    // Passes engine object to audio subsystem, creates audio output context.
    // Must be called BEFORE InitDirectXAndSubsystems.
    PreDirectXInit();

    // Initialize DirectX device and core engine subsystems (thunk_FUN_00eb612e).
    // Called with actual client height from GetClientRect.
    RECT clientRect = {0};
    GetClientRect(ghWnd, &clientRect);
    int clientHeight = clientRect.bottom - clientRect.top;
    InitDirectXAndSubsystems(clientHeight);

    // Game subsystem init (thunk_FUN_00eb496e): registers callbacks, enumerates devices,
    // loads language selection screen. Sets g_bSubsysInitialized = true.
    InitGameSubsystems();
    g_bSubsysInitialized = true;  // DAT_00bef6c6 = 1

    // Restore maximized/minimized state in windowed mode.
    // Original uses SW_MAXIMIZE=3 and SW_MINIMIZE=6 (checked from std::basic_string "true" compare).
    if (!bIsFullscreen) {
        char maximized[POSITION_BUFFER_SIZE];
        char minimized[POSITION_BUFFER_SIZE];
        ReadRegistrySettingStr(kApp, kSection, "Maximized", "false", maximized, sizeof(maximized));
        ReadRegistrySettingStr(kApp, kSection, "Minimized", "false", minimized, sizeof(minimized));

        if (strcmp(maximized, "true") == 0) {
            ShowWindow(ghWnd, SW_MAXIMIZE);   // = 3
        } else if (strcmp(minimized, "true") == 0) {
            ShowWindow(ghWnd, SW_MINIMIZE);   // = 6 (original uses 6, not SW_SHOWMINIMIZED=2)
        } else {
            ShowWindow(ghWnd, nShowCmd);
        }
    }

    // UpdateWindow forces an immediate WM_PAINT before entering the main loop.
    UpdateWindow(ghWnd);

    // Enter main game loop
    MainLoop();

    // Cleanup ─────────────────────────────────────────────────────────────────

    // Render+audio teardown: releases audio/render resources (RenderAndAudioTeardown,
    // thunk_FUN_00ec6610). Stops audio threads, closes handles.
    // TODO: RenderAndAudioTeardown()

    // Release engine object DAT_00bef6d0 via callback manager destructor.
    // Original: (**(callback_mgr + 0xc))(DAT_00bef6d0, 0)
    // This is NOT a COM vtable release — it goes through the callback system.
    if (g_pComObject) {
        // TODO: (**(code**)(GetOrInitCallbackManager() + 0xc))(g_pComObject, 0)
        g_pComObject = NULL;
    }

    // Save option settings back to registry before exit (SaveOptionsOnExit, thunk_FUN_00eb4a5d).
    // Writes OptionResolution, OptionLOD, OptionBrightness.
    SaveOptionsOnExit();

    // Pause input via RealInputSystem vtable (mirrors PauseGraphicsState logic).
    // Original calls vtable on DAT_00e6b384 (RealInputSystem) to pause the input device.
    // TODO: if (g_pRealInputSystem && g_pRealInputSystem->subObj) { (*vtable[...])(); }

    UnacquireInputDevices();

    // Show cursor and mark as lost focus
    while (ShowCursor(TRUE) < 1);
    g_bHasFocusLost = true;

    // Signal cursor state change and switch render scene (UpdateCursorVisibilityAndScene).
    UpdateCursorVisibilityAndScene();

    // Restore system parameters (mouse acceleration etc.)
    SaveOrRestoreSystemParameters(true);

    // Original uses TerminateProcess instead of a normal return to avoid CRT teardown
    TerminateProcess(GetCurrentProcess(), 1);
    return 0;
}
