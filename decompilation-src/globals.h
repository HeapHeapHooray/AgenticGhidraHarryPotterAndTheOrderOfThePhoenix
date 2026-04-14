#ifndef GLOBALS_H
#define GLOBALS_H

#include <windows.h>
#include <d3d9.h>
#include <dinput.h>

// ── Constants ─────────────────────────────────────────────────────────────────

// Default window dimensions
static const int DEFAULT_WINDOW_WIDTH  = 640;
static const int DEFAULT_WINDOW_HEIGHT = 480;

// Buffer sizes
static const int CMDLINE_BUFFER_SIZE         = 0x200;  // 512 bytes
static const int REG_PATH_BUFFER_SIZE        = 512;
static const int POSITION_BUFFER_SIZE        = 32;

// Default registry-backed game settings
static const int DEFAULT_BIT_DEPTH           = 32;
static const int DEFAULT_SHADOW_LOD          = 6;

// Maximum number of joystick devices
static const int MAX_JOYSTICKS               = 2;

// SystemParametersInfo action codes (reconstructed from binary analysis)
static const UINT SPI_GET_MOUSE_SPEED          = 0x3A;
static const UINT SPI_SET_MOUSE_SPEED          = 0x3B;
static const UINT SPI_GET_MOUSE_ACCEL          = 0x34;
static const UINT SPI_SET_MOUSE_ACCEL          = 0x35;
static const UINT SPI_GET_SCREEN_READER_PARAMS = 0x32;
static const UINT SPI_SET_SCREEN_READER_PARAMS = 0x33;

// uiParam sizes for the SPI calls above
static const UINT SPI_MOUSE_PARAM_SIZE         = 8;
static const UINT SPI_SCREEN_READER_PARAM_SIZE = 0x18;  // 24 bytes

// Bitmask that clears mouse acceleration flag bits 2 and 3
static const UINT MOUSE_ACCEL_FLAGS_MASK       = 0xFFFFFFF3;

// Frame timing
static const DWORD      MAX_DELTA_TIME_MS    = 100;  // spiral-of-death cap
static const DWORD      TARGET_FRAME_TIME_MS = 16;   // ~60 FPS budget
static const DWORD      DEVICE_LOST_SLEEP_MS = 50;   // back-off when D3D device is lost
static const ULONGLONG  GAME_TIME_SCALE      = 3;    // accumulated-time multiplier

// Delay (ms) before re-acquiring input after a focus change
static const DWORD FOCUS_CHANGE_DELAY_MS     = 2000;

// Mask to extract the SC_* command from WM_SYSCOMMAND wParam
static const WPARAM SYSCMD_MASK              = 0xFFF0;

// Non-zero return from WM_ENTERMENULOOP that suppresses default menu handling
static const LRESULT MENU_LOOP_SUPPRESS      = 0x10000;

// ─────────────────────────────────────────────────────────────────────────────

// Function prototypes
int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nShowCmd);
HWND CreateGameWindow(HINSTANCE hInstance, int width, int height);
void MainLoop();
BOOL RegisterWindowClass(HINSTANCE hInstance);
LRESULT CALLBACK WindowProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam);

// System and initialization
void SaveOrRestoreSystemParameters(bool restore);
void LoadGameSettings();
void SaveWindowPlacement(HWND hWnd);

// DirectX functions
void UpdateDirectXDevice();
void ReleaseDirectXResources();
void RestoreDirectXResources();

// DirectInput functions
void UnacquireInputDevices();
void AcquireInputDevices();

// Game frame
void GameFrameUpdate();

// Registry functions
int ReadRegistrySetting(const char* appName, const char* section, const char* key, int defaultValue);
void WriteRegistrySetting(const char* appName, const char* section, const char* key, const char* value);

// Global variables
extern HWND ghWnd;
extern bool bIsFullscreen;
extern bool bHasFocus;
extern int gWidth;
extern int gHeight;

// DirectX globals
extern IDirect3D9* g_pD3D;
extern IDirect3DDevice9* g_pd3dDevice;
extern IDirect3DSurface9* g_pBackBuffer;
extern IDirect3DSurface9* g_pRenderTarget;

// DirectInput globals
extern IDirectInput8* g_pDirectInput;
extern IDirectInputDevice8* g_pKeyboard;
extern IDirectInputDevice8* g_pMouse;
extern IDirectInputDevice8* g_pJoystick[2];

// Timing globals
extern DWORD g_dwLastFrameTime;
extern ULONGLONG g_ullAccumulatedTime;
extern bool g_bExitRequested;

// Saved system parameters
struct SystemParams {
    UINT mouseSpeed;
    UINT mouseSpeedFlags;
    UINT mouseAccel[3];
    UINT mouseAccelFlags;
    UINT screenReader[6];
    UINT screenReaderFlags;
};
extern SystemParams g_savedParams;

// Delayed operation timer (in milliseconds)
extern DWORD g_dwDelayedOpTimer;

#endif // GLOBALS_H
