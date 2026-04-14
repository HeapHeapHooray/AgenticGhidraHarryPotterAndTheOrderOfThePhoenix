#include "globals.h"
#include <stdio.h>

// Mocking some functions that were FUN_ calls
void LoadGameSettings() {
    // Original code used FUN_0060ce60 to read settings
    // For now, we use defaults or mock it
    gWidth = 800;
    gHeight = 600;
    bIsFullscreen = false;
}

int WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nShowCmd) {
    // Single instance check
    if (FindWindowA("OrderOfThePhoenixMainWndClass", NULL)) {
        return 0;
    }

    LoadGameSettings();

    if (!RegisterWindowClass(hInstance)) {
        return 0;
    }

    ghWnd = CreateGameWindow(hInstance, gWidth, gHeight);
    if (!ghWnd) {
        return 0;
    }

    ShowWindow(ghWnd, nShowCmd);
    UpdateWindow(ghWnd);

    MainLoop();

    return 0;
}

BOOL RegisterWindowClass(HINSTANCE hInstance) {
    WNDCLASSA wc = {0};
    wc.lpfnWndProc = WindowProc;
    wc.hInstance = hInstance;
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    wc.lpszClassName = "OrderOfThePhoenixMainWndClass";
    wc.hbrBackground = (HBRUSH)GetStockObject(BLACK_BRUSH);
    wc.style = CS_DBLCLKS | CS_OWNDC | CS_VREDRAW | CS_HREDRAW;

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

void MainLoop() {
    MSG msg;
    while (true) {
        if (PeekMessageA(&msg, NULL, 0, 0, PM_REMOVE)) {
            if (msg.message == WM_QUIT) {
                break;
            }
            TranslateMessage(&msg);
            DispatchMessageA(&msg);
        } else {
            // Game update logic would go here
            // FUN_0067d310();
            Sleep(1); // Prevent 100% CPU in mock
        }
    }
}

LRESULT CALLBACK WindowProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam) {
    switch (message) {
        case WM_DESTROY:
            PostQuitMessage(0);
            return 0;
        case WM_PAINT:
            if (!bIsFullscreen) {
                PAINTSTRUCT ps;
                BeginPaint(hWnd, &ps);
                EndPaint(hWnd, &ps);
            } else {
                ValidateRect(hWnd, NULL);
            }
            return 0;
        case WM_SYSCOMMAND:
            if (bIsFullscreen) {
                WPARAM cmd = wParam & 0xFFF0;
                if (cmd == SC_MAXIMIZE || cmd == SC_SIZE || cmd == SC_MOVE || cmd == SC_KEYMENU) {
                    return 0;
                }
            }
            break;
    }
    return DefWindowProcA(hWnd, message, wParam, lParam);
}
