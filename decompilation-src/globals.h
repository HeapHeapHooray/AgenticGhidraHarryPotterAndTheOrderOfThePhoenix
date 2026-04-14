#ifndef GLOBALS_H
#define GLOBALS_H

#include <windows.h>
#include <mmsystem.h>
#include <d3d9.h>
#include <dinput.h>

// ── Default window dimensions ─────────────────────────────────────────────────
static const int DEFAULT_WINDOW_WIDTH  = 640;
static const int DEFAULT_WINDOW_HEIGHT = 480;

// ── Buffer sizes ──────────────────────────────────────────────────────────────
static const int CMDLINE_BUFFER_SIZE    = 0x200;   // 512 bytes
static const int REG_PATH_BUFFER_SIZE   = 512;
static const int POSITION_BUFFER_SIZE   = 32;

// ── Default registry-backed graphics settings ─────────────────────────────────
static const int DEFAULT_SHADOW_LOD     = 6;   // ShadowLOD
static const int DEFAULT_OPTION_LOD     = 1;   // OptionLOD
static const int DEFAULT_BRIGHTNESS     = 5;   // OptionBrightness

// ── Maximum number of joystick devices ───────────────────────────────────────
static const int MAX_JOYSTICKS          = 2;

// ── Number of additional display modes stored ────────────────────────────────
static const int MAX_DISPLAY_MODES      = 6;   // Mode0..Mode5 Width/Height pairs

// ── SystemParametersInfo action codes (reconstructed from binary analysis) ────
static const UINT SPI_GET_MOUSE_SPEED          = 0x3A;
static const UINT SPI_SET_MOUSE_SPEED          = 0x3B;
static const UINT SPI_GET_MOUSE_ACCEL          = 0x34;
static const UINT SPI_SET_MOUSE_ACCEL          = 0x35;
static const UINT SPI_GET_SCREEN_READER_PARAMS = 0x32;
static const UINT SPI_SET_SCREEN_READER_PARAMS = 0x33;

// ── uiParam sizes for the SPI calls above ────────────────────────────────────
static const UINT SPI_MOUSE_PARAM_SIZE         = 8;
static const UINT SPI_SCREEN_READER_PARAM_SIZE = 0x18;  // 24 bytes

// ── Bitmask that clears mouse acceleration flag bits 2 and 3 (bit 0 preserved)
static const UINT MOUSE_ACCEL_FLAGS_MASK       = 0xFFFFFFF3;

// ── Frame timing ──────────────────────────────────────────────────────────────
static const DWORD      MAX_DELTA_TIME_MS    = 100;  // spiral-of-death cap
static const DWORD      TARGET_FRAME_TIME_MS = 16;   // ~60 FPS budget
static const DWORD      DEVICE_LOST_SLEEP_MS = 50;   // back-off when D3D device is lost
// Accumulated time is multiplied by 3 to scale the game speed. The timing
// system works in 16.16 fixed-point units (divide by 0x10000 for seconds).
static const ULONGLONG  GAME_TIME_SCALE      = 3;
static const DWORD      TIME_FIXED_SHIFT     = 0x10000; // 16.16 denominator

// ── Delay (ms) before re-acquiring input after a focus change ─────────────────
static const DWORD FOCUS_CHANGE_DELAY_MS     = 2000;

// ── Deferred callback queue: max ms spent per frame ──────────────────────────
static const DWORD DEFERRED_QUEUE_BUDGET_MS  = 2;

// ── Mask to extract the SC_* command from WM_SYSCOMMAND wParam ───────────────
static const WPARAM SYSCMD_MASK              = 0xFFF0;

// ── Non-zero return from WM_ENTERMENULOOP that suppresses default menu handling
static const LRESULT MENU_LOOP_SUPPRESS      = 0x10000;

// ── DirectX sentinel: DAT_00bf1934 is set to this value when the AA path is  ─
// ── active (no separate render target texture is allocated).                 ─
static const DWORD D3D_AA_PATH_SENTINEL      = 0xbacb0ffe;

// ── Saved Windows system parameters ──────────────────────────────────────────
// Mirrors the actual memory layout at DAT_008afc44 (mouseSpeed[2]),
// DAT_008afc4c (mouseAccel[2]), DAT_008afc54 (screenReader[6]).
struct SystemParams {
    UINT mouseSpeed[2];       // +0x00  SPI_GETMOUSESPEED buffer (8 bytes)
    UINT mouseAccel[2];       // +0x08  SPI_GETMOUSE buffer (8 bytes)
    UINT screenReader[6];     // +0x10  SPI_GETSCREENREADER buffer (24 bytes)
};
// Convenience accessors for the flag word within each sub-structure.
// Bit 0 set = acceleration already disabled (don't touch).
// Bits 2-3  = acceleration curves to clear.
#define MOUSE_SPEED_FLAGS    mouseSpeed[1]
#define MOUSE_ACCEL_FLAGS    mouseAccel[1]
#define SCREEN_READER_FLAGS  screenReader[1]

// ── Graphics settings loaded from registry ───────────────────────────────────
struct GraphicsSettings {
    int width;               // DAT_00bf1940 (non-zero = fullscreen + this width)
    int bitDepth;            // DAT_00bf1944
    int shadowLOD;           // DAT_00bf1948 (default 6)
    int maxTextureSize;      // DAT_00bf194c
    int maxShadowTextureSize;// DAT_00bf1950
    int maxFarClip;          // DAT_00bf1954
    int cullDistance;        // DAT_00bf195c
    int particleRate;        // DAT_00bf1958
    int particleCullDistance;// DAT_00bf1960
    int disableFog;          // DAT_00bf1964
    int disablePostPro;      // DAT_00bf1968
    int filterFlip;          // DAT_00bf196c
    int aaMode;              // DAT_00bf1970 (0 = AA off)
    int useAdditionalModes;  // DAT_00bf1974
    int optionResolution;    // DAT_00bf197c
    int optionLOD;           // DAT_008ae1ec (default 1)
    int optionBrightness;    // DAT_008ae1f0 (default 5)
    int modeWidths[MAX_DISPLAY_MODES];   // DAT_009a78a0
    int modeHeights[MAX_DISPLAY_MODES];  // DAT_009e4c90
};

// ── Game object pause state machine ──────────────────────────────────────────
// Mirrors DAT_00c7b908.  Values:
//   0 = fully paused
//   4-5 = audio-only paused
//   6 = already paused (no-op state)
//   7 = resuming
enum GamePauseState {
    PAUSE_STATE_FULL    = 0,
    PAUSE_STATE_AUDIO   = 4,
    PAUSE_STATE_NOOP    = 6,
    PAUSE_STATE_RESUME  = 7,
};

// ── Function prototypes ───────────────────────────────────────────────────────
int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nShowCmd);
HWND CreateGameWindow(HINSTANCE hInstance, int width, int height);
void MainLoop();
BOOL RegisterWindowClass(HINSTANCE hInstance);
LRESULT CALLBACK WindowProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam);

// System and initialization
void SaveOrRestoreSystemParameters(bool restore);
void LoadGameSettings();
void SaveWindowPlacement(HWND hWnd);
bool ParseCommandLineArg(const char* cmdLine, const char* flag, const char** valueOut);

// DirectX subsystem init (called from WinMain after window creation)
void InitDirectXAndSubsystems(int height); // thunk_FUN_00eb612e — creates D3D device + DirectInput
void InitGameSubsystems();                 // thunk_FUN_00eb496e — registers callbacks, loads language screen

// DirectX functions
void UpdateDirectXDevice();
void ReleaseDirectXResources();
void RestoreDirectXResources();
void InitRenderStates();                   // FUN_00675950 — uploads shaders, sets initial render states
void CreateGPUSyncQuery();                 // FUN_0067b820 — creates D3DQUERYTYPE_EVENT sync query
void InitD3DStateDefaults();               // FUN_00674430 — initializes render/sampler/texture stage defaults
void PauseGraphicsState();                 // FUN_00617b60 — pauses render output on focus loss

// Render mode switching
void SwitchRenderOutputMode();             // FUN_00612530 — dispatches to render listener list

// Memory allocator query
SIZE_T QueryMemoryAllocatorMax();          // thunk_FUN_00eb6dbc — returns largest free block in allocator

// DirectInput functions
void UnacquireInputDevices();
void AcquireInputDevices();

// Game frame and timing
void GameFrameUpdate();
DWORD GetGameTime();
void ProcessDeferredCallbacks();

// Game object pause/resume
void PauseGameObjects(int param);
void ResumeGameObjects();
void PauseAudioManager();

// Registry functions
int  ReadRegistrySetting(const char* appName, const char* section,
                         const char* key, int defaultValue);
void WriteRegistrySetting(const char* appName, const char* section,
                          const char* key, const char* value);
bool ReadRegistrySettingStr(const char* appName, const char* section,
                            const char* key, const char* defaultValue,
                            char* outBuf, int outBufSize);

// Global variables
extern HWND ghWnd;
extern bool bIsFullscreen;
extern bool bHasFocus;
extern int gWidth;
extern int gHeight;

// DirectX globals
extern IDirect3D9*       g_pD3D;
extern IDirect3DDevice9* g_pd3dDevice;
extern IDirect3DSurface9* g_pBackBuffer;
extern IDirect3DSurface9* g_pRenderTarget;
extern IDirect3DSurface9* g_pAdditionalRT;
extern IDirect3DSurface9* g_pCachedRT;
extern IDirect3DSurface9* g_pCachedDS;
extern IUnknown*          g_pGPUSyncQuery;
extern D3DPRESENT_PARAMETERS g_d3dpp;  // DAT_00b94af8 saved present params for Reset
extern IUnknown*          g_pComObject; // DAT_00bef6d0 created in WinMain, released at exit

// DirectInput globals
extern IDirectInput8*      g_pDirectInput;
extern IDirectInputDevice8* g_pKeyboard;
extern IDirectInputDevice8* g_pMouse;
extern IDirectInputDevice8* g_pJoystick[MAX_JOYSTICKS];

// Timing globals
extern DWORD    g_dwStartupTime;       // multimedia timer baseline
extern ULONGLONG g_ullAccumTime;       // 64-bit accumulated time (16.16 fixed)
extern ULONGLONG g_ullNextCallback;    // next callback time
extern ULONGLONG g_ullCallbackInterval;// callback interval
extern int      g_nFrameFlip;          // double-buffer flip index (0 or 1)
extern DWORD    g_dwGameTicks;         // = accum * 3 / 0x10000
extern bool     g_bExitRequested;
extern bool     g_bTimebaseInit;       // set after first GetGameTime call

// System state
extern SystemParams  g_savedParams;
extern GraphicsSettings g_gfxSettings;
extern DWORD         g_dwDelayedOpTimer;
extern bool          g_bHasFocusLost;       // DAT_00bef6c7
extern bool          g_bCursorVisible;      // DAT_00bef67e
extern bool          g_bGameUpdateEnabled;  // DAT_00bef6d7
extern bool          g_bAudioWasPaused;     // DAT_00bef6d4
extern bool          g_bUpdatesWerePaused;  // DAT_00bef6d5
extern bool          g_bDeviceLost;         // DAT_00bf18aa
extern int           g_nPauseState;         // DAT_00c7b908
extern SIZE_T        g_nMinFreeMemory;       // DAT_008afb08 low-water mark
extern char          g_szCmdLine1[CMDLINE_BUFFER_SIZE];
extern char          g_szCmdLine2[CMDLINE_BUFFER_SIZE];

#endif // GLOBALS_H
