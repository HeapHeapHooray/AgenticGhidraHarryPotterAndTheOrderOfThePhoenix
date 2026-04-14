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

// ── Iteration 5: New Subsystem Structures ────────────────────────────────────

// Frame callback slot structure (DAT_00e6e880 - 8 slots, 0x1c bytes total)
struct CallbackSlot {
    void (*func)(void* context);  // Callback function pointer
    void* context;                 // Context data passed to callback
};

// Audio command structure for async audio operations
enum AudioCommandType {
    AUDIO_CMD_NONE = 0,
    AUDIO_CMD_OPEN_DEVICE = 1,
    AUDIO_CMD_QUERY_CAPS = 2,
    AUDIO_CMD_CONFIGURE = 3,
    AUDIO_CMD_START_STREAM = 4,
    AUDIO_CMD_STOP_STREAM = 5,
};

struct AudioCommand {
    AudioCommandType opcode;       // +0x00: Command type
    void* params;                  // +0x04: Command-specific parameters
    void (*callback)(int status);  // +0x08: Completion callback
    int status;                    // +0x0c: -2=error, 0=pending, 1=complete
    DWORD timestamp;               // +0x10: Submission time for timeout detection
};

// Audio command queue structure (DAT_00be82ac)
struct AudioCommandQueue {
    AudioCommand commands[64];     // +0x00: Fixed array of commands
    int head;                      // +0x400: Read index
    int tail;                      // +0x404: Write index
    int count;                     // +0x408: Active command count
    CRITICAL_SECTION cs;           // +0x40c: Thread synchronization
    HANDLE event;                  // +0x424: Event for wake notification
};

// Message dispatch handler structure
struct MessageEntry {
    DWORD msg_hash;                    // +0x00: Hash of message name (FNV-1a)
    void* dest_object;                 // +0x04: Destination object pointer
    void (*handler)(void*, void*);     // +0x08: Handler function
    int param_type;                    // +0x0c: Parameter type indicator
    const char* debug_name;            // +0x10: Original name (debug builds only)
};

// Backward compatibility typedef
typedef MessageEntry MessageHandler;

// Scene IDs for three-ID scene management system
struct SceneIDs {
    int focusLost;  // DAT_00c82b00 - Menu/pause screen
    int focusGain;  // DAT_00c82b08 - Active gameplay
    int current;    // DAT_00c82ac8 - Current/target scene
};

// Render batch node for deferred rendering queue
struct RenderBatchNode {
    // Geometry reference
    void* geometry_buffer;         // +0x00: Vertex/index buffer
    DWORD vertex_count;            // +0x04
    DWORD index_count;             // +0x08
    
    // Material/shader
    void* material;                // +0x0c: Material properties
    DWORD shader_hash;             // +0x10: Shader type hash (FNV-1a of name)
    
    // Transform
    float world_matrix[16];        // +0x14: World transform (64 bytes, 4x4 matrix)
    
    // Render state
    DWORD render_flags;            // +0x54: Alpha, depth test, cull mode
    float sort_key;                // +0x58: Depth for sorting
    
    // Batch info
    int batch_id;                  // +0x5c: For batch merging
    DWORD timestamp;               // +0x60: Submission time
    
    // Padding/unknown
    char padding[0x1c];            // +0x64: Unknown fields
    
    // List linkage
    RenderBatchNode* next;         // +0x7c: Next in queue
};

// Shader type hashes (FNV-1a)
#define SHADER_HASH_OPAQUE   0x2a4f6b91
#define SHADER_HASH_ALPHA    0x7c31e8a2
#define SHADER_HASH_BLOOM    0x1f9d4c33
#define SHADER_HASH_GLASS    0x5e2a1bd4
#define SHADER_HASH_BACKDROP 0x8f3c9a45
#define SHADER_HASH_WATER    0x3d7f2e16
#define SHADER_HASH_SKY      0x6a8b4c97

// TimeManager structure (DAT_00bef768 - 8 bytes)
struct TimeManager {
    void* vtable;     // +0x00
    DWORD isPaused;   // +0x04 (0=running, non-zero=paused)
};

// GlobalTempBuffer structure (DAT_00e6b378 - 0x3c bytes)
struct GlobalTempBuffer {
    BYTE unknown_header[3];
    void* callback_funcs[5];
    void* callback_contexts[5];
    BYTE callback_count;  // Max 5
    BYTE additional_data[16];  // Fill to 0x3c
};

// RealGraphSystem structure (DAT_00e6b390 - 8 bytes)
struct RealGraphSystem {
    void* vtable;                    // +0x00
    void* callback_mgr_secondary;    // +0x04
};

// Input system structure (DAT_00be8758)
struct RealInputSystem {
    // Base object
    void* vtable;                      // +0x00: Vtable pointer
    
    // DirectInput interfaces
    IDirectInput8* pDirectInput;       // +0x04: Main DirectInput8 object
    IDirectInputDevice8* pKeyboard;    // +0x08: Keyboard device
    IDirectInputDevice8* pMouse;       // +0x0c: Mouse device
    IDirectInputDevice8* pJoystick[2]; // +0x10: Up to 2 joysticks
    
    // Device state buffers
    BYTE keyboard_state[256];          // +0x18: Current keyboard state
    BYTE prev_keyboard_state[256];     // +0x118: Previous frame keyboard
    DIMOUSESTATE2 mouse_state;         // +0x218: Current mouse state
    DIMOUSESTATE2 prev_mouse_state;    // +0x22c: Previous mouse state
    DIJOYSTATE2 joystick_state[2];     // +0x240: Joystick states
    DIJOYSTATE2 prev_joystick_state[2];// +0x340: Previous joystick states
    
    // Device status
    bool keyboard_active;              // +0x440: Keyboard acquired
    bool mouse_active;                 // +0x441: Mouse acquired
    bool joystick_active[2];           // +0x442: Joystick presence
    
    // Configuration
    DWORD input_flags;                 // +0x444: Input system flags
    bool paused;                       // +0x448: Pause state
    
    // Synchronization
    CRITICAL_SECTION cs;               // +0x44c: Thread safety
};

// GameServices structure (DAT_00bf2260)
struct GameServices {
    void* vtable;                      // +0x00: Vtable with subsystem getters
    
    // Subsystem pointers (lazy-initialized)
    void* save_manager;                // +0x04
    void* profile_manager;             // +0x08
    void* locale_manager;              // +0x0c
    void* achievement_mgr;             // +0x10
    void* stat_tracker;                // +0x14
    void* option_manager;              // +0x18
    
    // State
    bool initialized;                  // +0x40
    DWORD init_flags;                  // +0x44
};

// Scene listener callback type
typedef void (*SceneListenerCallback)(int newSceneID);

// Memory allocation header (for AllocEngineObject tracking)
struct AllocHeader {
    const char* tag;
    size_t size;
    AllocHeader* next;
    DWORD magic;  // 0xDEADBEEF
};

// Free list node for memory allocator
struct FreeListNode {
    FreeListNode* next;
    size_t size;
};

// Tag statistics for memory tracking
struct TagStats {
    const char* tag_name;          // Debug name (e.g., "CLI::CommandParser")
    DWORD bytes_allocated;         // Total bytes for this tag
    DWORD allocation_count;        // Number of allocations
    DWORD peak_bytes;              // Peak usage for this tag
};

// Engine allocator structure (complete layout at 0x00e61380)
struct EngineAllocator {
    // Header (0x00 - 0x28)
    void* vtable;                    // +0x00: Vtable pointer
    DWORD total_allocated;           // +0x04: Total bytes allocated (lifetime)
    DWORD current_allocated;         // +0x08: Currently allocated bytes
    DWORD peak_usage;                // +0x0c: Peak memory usage
    DWORD allocation_count;          // +0x10: Number of allocations (lifetime)
    DWORD free_count;                // +0x14: Number of frees (lifetime)
    DWORD current_alloc_count;       // +0x18: Active allocations
    void* heap_base;                 // +0x1c: Base address of heap region
    SIZE_T heap_size;                // +0x20: Total heap size
    DWORD flags;                     // +0x24: Allocator flags
    
    // Stats tracking (0x28 - 0x428)
    TagStats tag_stats[256];         // +0x28: Per-tag statistics (256*4=0x400 bytes)
    
    // Free lists (0x428 - 0x820)
    FreeListNode* free_lists[254];   // +0x428: Free list buckets (254*4=0x3f8 bytes)
    
    // Synchronization (0x820+)
    CRITICAL_SECTION cs;             // +0x820: Critical section (24 bytes on Win32)
    
    // Additional metadata
    DWORD last_compact_time;         // +0x838: Last defragmentation timestamp
    DWORD compact_threshold;         // +0x83c: Fragmentation threshold for compaction
};

// ── Function prototypes ───────────────────────────────────────────────────────
int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nShowCmd);
HWND CreateGameWindow(HINSTANCE hInstance, int width, int height);
void MainLoop();
BOOL RegisterWindowClass(HINSTANCE hInstance);
LRESULT CALLBACK WindowProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam);

// CLI command parser (called before single-instance guard in WinMain)
void CLI_CommandParser_ParseArgs();            // FUN_00eb787a — parses -name=value tokens

// System and initialization
void SaveOrRestoreSystemParameters(bool restore);
void LoadGameSettings();
void SaveWindowPlacement(HWND hWnd);
void SaveOptionsOnExit();                  // thunk_FUN_00eb4a5d — writes OptionResolution/LOD/Brightness to registry
bool ParseCommandLineArg(const char* cmdLine, const char* flag, const char** valueOut);

// DirectX subsystem init (called from WinMain after window creation)
void PreDirectXInit();                     // thunk_FUN_00ec64f9 — audio/render context setup (before DirectX)
void InitDirectXAndSubsystems(int height); // thunk_FUN_00eb612e — creates D3D device + DirectInput
void InitGameSubsystems();                 // thunk_FUN_00eb496e — registers callbacks, loads language screen

// DirectX functions
void UpdateDirectXDevice();
void ReleaseDirectXResources();
void RestoreDirectXResources();
void InitRenderStates();                   // FUN_00675950 — uploads shaders, sets initial render states
void CreateGPUSyncQuery();                 // FUN_0067b820 — creates D3DQUERYTYPE_EVENT sync query
void InitD3DStateDefaults();               // FUN_00674430 — initializes render/sampler/texture stage defaults
void PauseGraphicsState();                 // FUN_00617b60 — pauses input via RealInputSystem vtable on focus loss

// Cursor state and render scene switch (thunk_FUN_00ea53ca):
// Compares new visible state with cached g_bCursorVisible; if changed,
// calls SwitchRenderOutputMode on the appropriate scene list.
// Called on WM_ACTIVATE and in WinMain cleanup.
void UpdateCursorVisibilityAndScene();     // FUN_00ea53ca

// Render mode switching
void SwitchRenderOutputMode();             // FUN_00612530 — dispatches to render listener list
void SwitchRenderOutputModeEx(int sceneID); // Enhanced version with scene ID parameter

// Memory allocator (Iteration 5)
void* AllocEngineObject(size_t size, const char* tag);  // FUN_00614210 — debug-aware allocator
void FreeEngineObject(void* ptr);
SIZE_T QueryMemoryAllocatorMax();          // thunk_FUN_00eb6dbc — returns largest free block in allocator

// Message dispatch system (Iteration 5)
void RegisterMessageHandler(void* dest, const char* msgName, int paramType);  // FUN_00eb59ce
void DispatchMessage(DWORD msgID, void* params);
DWORD HashMessageName(const char* msgName);  // Hash function for message dispatch

// Frame callback system (Iteration 5)
void InitFrameCallbackSystem();  // FUN_00eb8744 — clears callback slots
void RegisterFrameCallback(void (*func)(void*), void* context);
void InvokeFrameCallbacks();

// Audio command queue (Iteration 5-7)
void InitAudioCommandQueue();
void EnqueueAudioCommand(AudioCommandType opcode, void* params, void (*callback)(int));
int AudioPollGate();  // FUN_006109d0 — returns -2/0/1 (error/pending/complete)
DWORD WINAPI AudioThreadProc(LPVOID lpParam);  // Audio thread entry point

// Scene management (Iteration 5)
void LoadSceneIDs();
void NotifySceneListeners(int newSceneID);
void FlushDeferredSceneListeners();  // FUN_006125a0

// Render queue (Iteration 5)
void BuildRenderBatch();  // FUN_0063d600 — builds and sorts render batches
void ProcessDeferredRenderQueue();

// Subsystem initialization (Iteration 5)
void InitLanguageResources();  // FUN_00eb87ba — loads string tables
void InitVideoCodec();         // FUN_00eb88b2 — initializes FMV codec
void FinalizeRenderInit();     // FUN_006677c0 — finalizes render setup

// DirectInput functions
void UnacquireInputDevices();
void AcquireInputDevices();

// Audio stream resume (called on delayed-timer expiry after focus regain)
void AudioStream_Resume();             // thunk_FUN_00ec67e8 — resumes audio after focus-loss pause

// Game frame and timing
void GameFrameUpdate();
DWORD GetGameTime();
void ProcessDeferredCallbacks();
void UpdateFrameTimingPrimary(DWORD* localTick);  // FUN_00617f50 — updates double-buffered timing
void InterpolateFrameTime();                       // FUN_00617ee0 — smooth frame interpolation

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
extern bool          g_bSubsysInitialized;  // DAT_00bef6c6 — set to 1 after InitGameSubsystems
extern char          g_szCmdLine1[CMDLINE_BUFFER_SIZE];
extern char          g_szCmdLine2[CMDLINE_BUFFER_SIZE];

// ── Iteration 5: New subsystem globals ───────────────────────────────────────

// Frame callback slots (DAT_00e6e880 - 8 slots)
extern CallbackSlot g_FrameCallbackSlots[8];

// Scene management
extern SceneIDs g_SceneIDs;

// Deferred render queue
extern RenderBatchNode* g_pDeferredRenderQueue;  // DAT_00bef7c0

// TimeManager singleton
extern TimeManager* g_pTimeManager;  // DAT_00bef768

// Message dispatch table
#ifndef MESSAGE_DISPATCH_TABLE_SIZE
#define MESSAGE_DISPATCH_TABLE_SIZE 256
#endif
extern MessageHandler g_MessageDispatchTable[MESSAGE_DISPATCH_TABLE_SIZE];
extern int g_nMessageHandlerCount;

// Audio command queue
#ifndef AUDIO_COMMAND_QUEUE_SIZE
#define AUDIO_COMMAND_QUEUE_SIZE 32
#endif
extern AudioCommand g_AudioCommandQueue[AUDIO_COMMAND_QUEUE_SIZE];
extern int g_nAudioQueueHead;
extern int g_nAudioQueueTail;
extern HANDLE g_hAudioThread;  // DAT_00bf1b30

// Subsystem objects
extern GlobalTempBuffer* g_pGlobalTempBuffer;   // DAT_00e6b378
extern RealGraphSystem* g_pRealGraphSystem;     // DAT_00e6b390

// Engine root object
extern void* g_pEngineRootObject;  // DAT_00bef6d0 (2904 bytes)

// Callback manager entries
extern void* g_pCallbackManager_Primary;    // DAT_00e6e870
extern void* g_pCallbackManager_Secondary;  // DAT_00e6e874

#endif // GLOBALS_H
