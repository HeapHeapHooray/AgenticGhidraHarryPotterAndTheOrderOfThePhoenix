#include "globals.h"
#include <stdio.h>
#include <float.h>
#include <string>

// Global variable definitions
HWND ghWnd = NULL;
bool bIsFullscreen = false;
bool bHasFocus = true;
int gWidth = 640;
int gHeight = 480;

IDirect3D9* g_pD3D = NULL;
IDirect3DDevice9* g_pd3dDevice = NULL;
IDirect3DSurface9* g_pBackBuffer = NULL;
IDirect3DSurface9* g_pRenderTarget = NULL;

IDirectInput8* g_pDirectInput = NULL;
IDirectInputDevice8* g_pKeyboard = NULL;
IDirectInputDevice8* g_pMouse = NULL;
IDirectInputDevice8* g_pJoystick[2] = {NULL, NULL};

DWORD g_dwLastFrameTime = 0;
ULONGLONG g_ullAccumulatedTime = 0;
bool g_bExitRequested = false;

SystemParams g_savedParams = {0};
DWORD g_dwDelayedOpTimer = 0;

char g_szCmdLine1[0x200] = {0};
char g_szCmdLine2[0x200] = {0};

// Registry helper functions
int ReadRegistrySetting(const char* appName, const char* section, const char* key, int defaultValue) {
    char regPath[512];
    sprintf(regPath, "Software\\Electronic Arts\\%s\\%s", appName, section);

    HKEY hKey;
    DWORD value = defaultValue;
    DWORD dataSize = sizeof(DWORD);

    // Try HKEY_CURRENT_USER first
    if (RegOpenKeyExA(HKEY_CURRENT_USER, regPath, 0, KEY_READ, &hKey) == ERROR_SUCCESS) {
        RegQueryValueExA(hKey, key, NULL, NULL, (LPBYTE)&value, &dataSize);
        RegCloseKey(hKey);
        return value;
    }

    // Try HKEY_LOCAL_MACHINE
    if (RegOpenKeyExA(HKEY_LOCAL_MACHINE, regPath, 0, KEY_READ, &hKey) == ERROR_SUCCESS) {
        RegQueryValueExA(hKey, key, NULL, NULL, (LPBYTE)&value, &dataSize);
        RegCloseKey(hKey);
        return value;
    }

    // Create key in HKCU with default value
    if (RegCreateKeyExA(HKEY_CURRENT_USER, regPath, 0, NULL, 0, KEY_WRITE, NULL, &hKey, NULL) == ERROR_SUCCESS) {
        RegSetValueExA(hKey, key, 0, REG_DWORD, (LPBYTE)&defaultValue, sizeof(DWORD));
        RegCloseKey(hKey);
    }

    return defaultValue;
}

void WriteRegistrySetting(const char* appName, const char* section, const char* key, const char* value) {
    char regPath[512];
    sprintf(regPath, "Software\\Electronic Arts\\%s\\%s", appName, section);

    HKEY hKey;
    if (RegCreateKeyExA(HKEY_CURRENT_USER, regPath, 0, NULL, 0, KEY_WRITE, NULL, &hKey, NULL) == ERROR_SUCCESS) {
        RegSetValueExA(hKey, key, 0, REG_SZ, (LPBYTE)value, strlen(value) + 1);
        RegCloseKey(hKey);
    }
}

void SaveOrRestoreSystemParameters(bool restore) {
    if (restore) {
        // Restore saved parameters
        SystemParametersInfoA(0x3b, 8, &g_savedParams.mouseSpeed, 0);
        SystemParametersInfoA(0x35, 8, &g_savedParams.mouseAccel, 0);
        SystemParametersInfoA(0x33, 0x18, &g_savedParams.screenReader, 0);
    } else {
        // Save current parameters
        SystemParametersInfoA(0x3a, 8, &g_savedParams.mouseSpeed, 0);
        SystemParametersInfoA(0x34, 8, &g_savedParams.mouseAccel, 0);
        SystemParametersInfoA(0x32, 0x18, &g_savedParams.screenReader, 0);

        // Disable mouse acceleration by clearing bits 0, 2, and 3
        if ((g_savedParams.mouseSpeedFlags & 1) == 0) {
            UINT modifiedFlags = g_savedParams.mouseSpeedFlags & 0xFFFFFFF3;
            SystemParametersInfoA(0x3b, 8, &modifiedFlags, 0);
        }

        if ((g_savedParams.mouseAccelFlags & 1) == 0) {
            UINT modifiedFlags = g_savedParams.mouseAccelFlags & 0xFFFFFFF3;
            SystemParametersInfoA(0x35, 8, &modifiedFlags, 0);
        }

        if ((g_savedParams.screenReaderFlags & 1) == 0) {
            UINT modifiedFlags = g_savedParams.screenReaderFlags & 0xFFFFFFF3;
            SystemParametersInfoA(0x33, 0x18, &modifiedFlags, 0);
        }
    }
}

void LoadGameSettings() {
    const char* appName = "Harry Potter and the Order of the Phoenix";
    const char* section = "GameSettings";

    gWidth = ReadRegistrySetting(appName, section, "Width", 640);
    gHeight = ReadRegistrySetting(appName, section, "Height", 480);
    int bitDepth = ReadRegistrySetting(appName, section, "BitDepth", 32);
    int shadowLOD = ReadRegistrySetting(appName, section, "ShadowLOD", 6);
    int maxFarClip = ReadRegistrySetting(appName, section, "MaxFarClip", 0);
    int particleRate = ReadRegistrySetting(appName, section, "ParticleRate", 0);
    bIsFullscreen = ReadRegistrySetting(appName, section, "Fullscreen", 0) != 0;
}

void SaveWindowPlacement(HWND hWnd) {
    const char* appName = "Harry Potter and the Order of the Phoenix";
    const char* section = "GameSettings";
    WINDOWPLACEMENT wp = {sizeof(WINDOWPLACEMENT)};

    if (!GetWindowPlacement(hWnd, &wp)) {
        return;
    }

    // Only save position if not minimized or maximized
    if (wp.showCmd != SW_SHOWMINIMIZED && wp.showCmd != SW_SHOWMAXIMIZED) {
        RECT adjustRect = {0, 0, 0, 0};
        AdjustWindowRect(&adjustRect, WS_OVERLAPPEDWINDOW, FALSE);

        wp.rcNormalPosition.top -= adjustRect.top;
        wp.rcNormalPosition.left -= adjustRect.left;
        wp.rcNormalPosition.bottom += adjustRect.bottom;
        wp.rcNormalPosition.right += adjustRect.right;

        char buffer[32];

        sprintf(buffer, "%ld", wp.rcNormalPosition.left);
        WriteRegistrySetting(appName, section, "PosX", buffer);

        sprintf(buffer, "%ld", wp.rcNormalPosition.top);
        WriteRegistrySetting(appName, section, "PosY", buffer);

        sprintf(buffer, "%ld", wp.rcNormalPosition.right - wp.rcNormalPosition.left);
        WriteRegistrySetting(appName, section, "SizeX", buffer);

        sprintf(buffer, "%ld", wp.rcNormalPosition.bottom - wp.rcNormalPosition.top);
        WriteRegistrySetting(appName, section, "SizeY", buffer);
    }

    WriteRegistrySetting(appName, section, "Minimized",
                        (wp.showCmd == SW_SHOWMINIMIZED) ? "true" : "false");
    WriteRegistrySetting(appName, section, "Maximized",
                        (wp.showCmd == SW_SHOWMAXIMIZED) ? "true" : "false");
}

void ReleaseDirectXResources() {
    if (g_pRenderTarget) {
        g_pRenderTarget->Release();
        g_pRenderTarget = NULL;
    }

    if (g_pBackBuffer) {
        g_pBackBuffer->Release();
        g_pBackBuffer = NULL;
    }
}

void RestoreDirectXResources() {
    if (!g_pd3dDevice) return;

    // Get back buffer
    g_pd3dDevice->GetBackBuffer(0, 0, D3DBACKBUFFER_TYPE_MONO, &g_pBackBuffer);

    // Get back buffer description
    D3DSURFACE_DESC desc;
    if (g_pBackBuffer) {
        g_pBackBuffer->GetDesc(&desc);
    }

    // Create render target texture
    IDirect3DTexture9* pTexture = NULL;
    if (SUCCEEDED(g_pd3dDevice->CreateTexture(desc.Width, desc.Height, 1,
                                              D3DUSAGE_RENDERTARGET, desc.Format,
                                              D3DPOOL_DEFAULT, &pTexture, NULL))) {
        pTexture->GetSurfaceLevel(0, &g_pRenderTarget);
        pTexture->Release();
    }

    // Set render targets
    if (g_pBackBuffer) {
        g_pd3dDevice->SetRenderTarget(0, g_pBackBuffer);
    }
}

void UpdateDirectXDevice() {
    if (!g_pd3dDevice) return;

    HRESULT hr = g_pd3dDevice->TestCooperativeLevel();

    if (FAILED(hr)) {
        if (hr == D3DERR_DEVICELOST) {
            // Device is lost, sleep and retry
            Sleep(50);
            return;
        }

        if (hr == D3DERR_DEVICENOTRESET) {
            // Device can be reset
            ReleaseDirectXResources();

            // Reset device with saved parameters
            D3DPRESENT_PARAMETERS d3dpp = {0};
            d3dpp.BackBufferWidth = gWidth;
            d3dpp.BackBufferHeight = gHeight;
            d3dpp.BackBufferFormat = D3DFMT_UNKNOWN;
            d3dpp.BackBufferCount = 1;
            d3dpp.SwapEffect = D3DSWAPEFFECT_DISCARD;
            d3dpp.hDeviceWindow = ghWnd;
            d3dpp.Windowed = !bIsFullscreen;

            if (SUCCEEDED(g_pd3dDevice->Reset(&d3dpp))) {
                RestoreDirectXResources();
            }
        }
    }
}

void UnacquireInputDevices() {
    if (g_pKeyboard) {
        g_pKeyboard->Unacquire();
    }

    if (g_pMouse) {
        g_pMouse->Unacquire();
    }

    ShowCursor(TRUE);

    for (int i = 0; i < 2; i++) {
        if (g_pJoystick[i]) {
            g_pJoystick[i]->Unacquire();
        }
    }
}

void AcquireInputDevices() {
    if (g_pKeyboard) {
        g_pKeyboard->Acquire();
    }

    if (g_pMouse) {
        g_pMouse->Acquire();
    }

    ShowCursor(FALSE);

    for (int i = 0; i < 2; i++) {
        if (g_pJoystick[i]) {
            g_pJoystick[i]->Acquire();
        }
    }
}

void GameFrameUpdate() {
    DWORD currentTime = GetTickCount();
    DWORD deltaTime = currentTime - g_dwLastFrameTime;

    // Cap delta at 100ms to prevent spiral of death
    if (deltaTime > 100) {
        deltaTime = 100;
    }

    g_dwLastFrameTime = currentTime;

    // Accumulate time (multiply by 3 for game speed)
    g_ullAccumulatedTime += (ULONGLONG)deltaTime * 3;

    // Trigger game updates based on accumulated time
    // (Actual game logic would go here)
}

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nShowCmd) {
    // Set FPU control word to prevent denormal exceptions
    _control87(0x20000, 0x30000);

    // Save and modify system parameters
    SaveOrRestoreSystemParameters(false);

    // Save command line
    strncpy(g_szCmdLine1, lpCmdLine, sizeof(g_szCmdLine1) - 1);
    strncpy(g_szCmdLine2, lpCmdLine, sizeof(g_szCmdLine2) - 1);

    // Single instance check
    if (FindWindowA("OrderOfThePhoenixMainWndClass", NULL)) {
        return 0;
    }

    // Register window class
    if (!RegisterWindowClass(hInstance)) {
        return 0;
    }

    // Load game settings from registry
    LoadGameSettings();

    // Create window
    ghWnd = CreateGameWindow(hInstance, gWidth, gHeight);
    if (!ghWnd) {
        return 0;
    }

    ShowWindow(ghWnd, nShowCmd);
    UpdateWindow(ghWnd);

    // TODO: Initialize DirectX
    // TODO: Initialize DirectInput

    // Enter main loop
    MainLoop();

    // Save window placement
    SaveWindowPlacement(ghWnd);

    // Restore system parameters
    SaveOrRestoreSystemParameters(true);

    return 0;
}

void MainLoop() {
    MSG msg;
    g_dwLastFrameTime = GetTickCount();

    while (!g_bExitRequested) {
        // Update DirectX device state
        UpdateDirectXDevice();

        // Handle focus state
        if (!bHasFocus) {
            // Window doesn't have focus, show cursor and pause
            ShowCursor(TRUE);
        }

        // Process Windows messages
        if (PeekMessageA(&msg, NULL, 0, 0, PM_REMOVE)) {
            if (msg.message == WM_QUIT) {
                g_bExitRequested = true;
                break;
            }

            TranslateMessage(&msg);
            DispatchMessageA(&msg);
        } else {
            // No messages, update game
            DWORD startTime = GetTickCount();

            // Update game frame
            GameFrameUpdate();

            // Handle delayed operations timer
            if (g_dwDelayedOpTimer > 0) {
                DWORD elapsed = GetTickCount() - startTime;
                if (elapsed < g_dwDelayedOpTimer) {
                    g_dwDelayedOpTimer -= elapsed;
                } else {
                    g_dwDelayedOpTimer = 0;
                    // Perform delayed operations here
                }
            }

            // Frame rate limiting
            DWORD frameTime = GetTickCount() - startTime;
            if (frameTime < 16) {  // Target ~60 FPS
                Sleep(0);
            }
        }
    }
}

BOOL RegisterWindowClass(HINSTANCE hInstance) {
    // Unregister first to handle crashed previous instance
    UnregisterClassA("OrderOfThePhoenixMainWndClass", hInstance);

    WNDCLASSA wc = {0};
    wc.style = CS_DBLCLKS | CS_OWNDC | CS_VREDRAW | CS_HREDRAW;
    wc.lpfnWndProc = WindowProc;
    wc.hInstance = hInstance;
    wc.hCursor = LoadCursorA(NULL, IDC_ARROW);
    wc.hbrBackground = (HBRUSH)GetStockObject(BLACK_BRUSH);
    wc.lpszClassName = "OrderOfThePhoenixMainWndClass";

    return RegisterClassA(&wc) != 0;
}

HWND CreateGameWindow(HINSTANCE hInstance, int width, int height) {
    RECT rect = {0, 0, width, height};
    DWORD dwStyle = bIsFullscreen ? WS_POPUP : WS_OVERLAPPEDWINDOW;
    AdjustWindowRectEx(&rect, dwStyle, FALSE, 0);

    HWND hWnd = CreateWindowExA(
        0,
        "OrderOfThePhoenixMainWndClass",
        "Harry Potter and the Order of the Phoenix",
        dwStyle,
        CW_USEDEFAULT, CW_USEDEFAULT,
        rect.right - rect.left, rect.bottom - rect.top,
        NULL, NULL, hInstance, NULL
    );

    return hWnd;
}

LRESULT CALLBACK WindowProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam) {
    switch (message) {
        case WM_DESTROY:
            if (bIsFullscreen) {
                // Show cursor when exiting fullscreen
                while (ShowCursor(TRUE) < 1);
            }
            PostQuitMessage(0);
            return 0;

        case WM_ACTIVATE:
            if (bIsFullscreen) {
                if (LOWORD(wParam) == WA_INACTIVE) {
                    // Losing focus
                    UnacquireInputDevices();
                    while (ShowCursor(TRUE) < 1);
                    bHasFocus = false;
                    g_dwDelayedOpTimer = 2000;  // 2 second delay
                } else {
                    // Gaining focus
                    AcquireInputDevices();
                    while (ShowCursor(FALSE) >= 0);
                    bHasFocus = true;
                    g_dwDelayedOpTimer = 2000;
                }
            }
            return 0;

        case WM_SETFOCUS:
        case WM_SETCURSOR:
            if (bHasFocus) {
                while (ShowCursor(FALSE) >= 0);
            } else {
                SetCursor(LoadCursorA(NULL, IDC_ARROW));
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
            SaveWindowPlacement(hWnd);
            DestroyWindow(hWnd);
            return 0;

        case WM_SYSCOMMAND:
            if (bIsFullscreen) {
                WPARAM cmd = wParam & 0xFFF0;
                if (cmd == SC_MAXIMIZE || cmd == SC_SIZE ||
                    cmd == SC_MOVE || cmd == SC_KEYMENU) {
                    return 0;  // Block these commands in fullscreen
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
                return 0x10000;  // Non-zero to prevent menu
            }
            break;
    }

    return DefWindowProcA(hWnd, message, wParam, lParam);
}
