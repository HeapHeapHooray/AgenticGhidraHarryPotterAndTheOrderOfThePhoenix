#ifndef GLOBALS_H
#define GLOBALS_H

#include <windows.h>
#include <d3d9.h>
#include <dinput.h>

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
