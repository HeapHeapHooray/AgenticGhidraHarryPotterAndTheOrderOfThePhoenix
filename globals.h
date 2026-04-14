#include <windows.h>
#include <tchar.h>

// Function prototypes
int WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nShowCmd);
HWND CreateGameWindow(HINSTANCE hInstance, int width, int height);
void MainLoop();
BOOL RegisterWindowClass(HINSTANCE hInstance);
LRESULT CALLBACK WindowProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam);

// Global variables (placeholders for now)
HWND ghWnd = NULL;
bool bIsFullscreen = false;
int gWidth = 800;
int gHeight = 600;
// ... more to be added as identified
