# Differences Between Original and C++ Decompilation

This document compares ARCHITECTURE.md (original hp.exe) with CPP-ARCHITECTURE.md (C++ decompilation) to identify gaps and areas needing improvement for functional equivalence.

## Missing Implementations

### 1. DirectX Initialization
**Original (ARCHITECTURE.md):**
- Initializes DirectX 9 rendering system
- Creates device with specific present parameters
- Sets up multiple render targets

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ❌ DirectX initialization marked as TODO
- ❌ No device creation code
- ❌ No D3DPRESENT_PARAMETERS setup
- ✅ Has device lost recovery framework

**Action needed:**
- Implement DirectX device creation with proper parameters
- Determine adapter selection logic
- Identify present parameters (buffer format, swap effect, etc.)

### 2. DirectInput Initialization
**Original (ARCHITECTURE.md):**
- Initializes DirectInput 8
- Creates keyboard, mouse, and joystick devices
- Sets up data formats and cooperative levels

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ❌ DirectInput initialization marked as TODO
- ✅ Has acquire/unacquire framework
- ✅ Has device pointers defined

**Action needed:**
- Implement DirectInput8Create
- Create and configure keyboard device
- Create and configure mouse device
- Enumerate and create joystick devices

### 3. Game Update Functions
**Original (ARCHITECTURE.md):**
- GameFrameUpdate calls multiple unknown functions
- FUN_00636830(): Pre-update logic
- FUN_00617f50: Primary frame callback
- FUN_00617ee0: Secondary frame callback
- Function pointer table at DAT_008e1644

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ✅ Has frame timing with delta cap
- ✅ Has accumulation and 3x speed multiplier
- ❌ Missing actual game update logic
- ❌ Missing frame callbacks
- ❌ Missing function pointer table

**Action needed:**
- Identify and implement pre-update function
- Implement frame callback system
- Set up function pointer table for callbacks

### 4. Graphics Initialization Functions
**Original (ARCHITECTURE.md):**
- RestoreDirectXResources calls:
  - FUN_00675950()
  - FUN_0067b820()
  - FUN_0067bb20()
  - FUN_00674430()

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ✅ Has basic resource restore framework
- ❌ Missing these initialization functions

**Action needed:**
- Identify what these functions do
- Implement equivalent C++ code

### 5. Window Close Cleanup
**Original (ARCHITECTURE.md):**
- WM_CLOSE calls FUN_0060d220 before DestroyWindow
- FUN_0060d220 identified as SaveWindowPlacement

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ✅ WM_CLOSE calls SaveWindowPlacement
- ✅ Correctly implemented

**No action needed** ✓

## Implementation Accuracy Issues

### 6. System Parameter Values
**Original (ARCHITECTURE.md):**
- Exact parameter values saved in structures
- Flag manipulation clears specific bits

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ✅ Correctly saves parameters
- ⚠️ Assumes flags are at specific offsets in structure
- ⚠️ May not match exact memory layout

**Action needed:**
- Verify SystemParams structure matches original memory layout
- Confirm flag offset assumptions

### 7. Window Procedure Message Handling
**Original (ARCHITECTURE.md):**
- Handles many window messages
- Complex focus management with global flags
- Multiple DAT_ global variables referenced

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ✅ Implements key messages
- ⚠️ May be missing some edge cases
- ⚠️ Simplified global flag management

**Action needed:**
- Review all message handling in original WindowProc
- Ensure all global flags are properly represented
- Verify WM_ACTIVATE logic matches exactly

### 8. Registry Reading with std::string
**Original (ARCHITECTURE.md):**
- Uses std::basic_string for registry paths
- Complex C++ string construction
- Throws exceptions on errors

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ✅ Uses simple C string handling
- ⚠️ Doesn't throw exceptions
- ⚠️ Simplified error handling

**Action needed:**
- Consider if exception handling is needed
- Verify error paths match original behavior

### 9. DirectX Resource Management
**Original (ARCHITECTURE.md):**
- Releases 4 different DirectX interfaces
- Complex conditional logic (checks for 0xbacb0ffe marker)
- Calls helper functions before/after release

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ✅ Releases render target and back buffer
- ❌ Missing checks for special marker value
- ❌ Missing DAT_00b95034 interface
- ❌ Missing helper function calls

**Action needed:**
- Identify all DirectX interfaces that need release
- Implement special marker logic (0xbacb0ffe)
- Implement helper functions (thunk_FUN_00ec04dc, etc.)

### 10. Cursor Management
**Original (ARCHITECTURE.md):**
- Uses ShowCursor in loops: `while (ShowCursor(1) < 1);`
- Ensures cursor visibility level reaches target
- Reference counting aware

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ✅ Uses ShowCursor loops correctly
- ✅ Implements reference counting aware code

**No action needed** ✓

## Missing Global State

### 11. Timing Variables
**Original (ARCHITECTURE.md):**
- Many 64-bit timing variables:
  - DAT_00e6e5e0, DAT_00e6e5e4: Last frame time
  - DAT_00c83198, DAT_00c8319c: Accumulated time
  - DAT_00c831a8, DAT_00c831ac: Next callback time
  - DAT_00c83190, DAT_00c83194: Callback interval
  - DAT_008e1648: Frame flip flag

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ⚠️ Uses DWORD and ULONGLONG
- ⚠️ May not match exact variable count
- ❌ Missing frame flip flag
- ❌ Missing callback interval/time variables

**Action needed:**
- Add missing timing variables
- Ensure 64-bit variable usage matches original
- Implement frame flip/toggle mechanism

### 12. Focus and State Flags
**Original (ARCHITECTURE.md):**
- DAT_00bef6c5: Exit flag
- DAT_008afbd9: Fullscreen mode
- DAT_00bef6c7: Has focus
- DAT_00bef67e: Cursor visibility state
- DAT_00bef67c, DAT_00bef67d: Unknown flags
- DAT_00bef6d8: Delayed op timer
- DAT_00bef6d7: Game update enable flag
- DAT_00bef6d4, DAT_00bef6d5: Unknown flags

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ✅ Has g_bExitRequested (exit flag)
- ✅ Has bIsFullscreen
- ✅ Has bHasFocus
- ✅ Has g_dwDelayedOpTimer
- ❌ Missing cursor visibility state flag
- ❌ Missing unknown flags (may be important)
- ❌ Missing game update enable flag

**Action needed:**
- Add all missing global flags
- Investigate purpose of unknown flags
- Ensure flag usage matches original logic

### 13. DirectX Global Interfaces
**Original (ARCHITECTURE.md):**
- DAT_00bf1920: IDirect3DDevice9*
- DAT_00bf1930: Back buffer
- DAT_00bf1934: Render target/texture
- DAT_00bf1938: Additional render target
- DAT_00af1390: Cached surface pointer
- DAT_00ae9250: Cached surface pointer
- DAT_00b95034: Unknown DirectX interface

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ✅ Has g_pd3dDevice
- ✅ Has g_pBackBuffer
- ✅ Has g_pRenderTarget
- ❌ Missing additional render target
- ❌ Missing cached surface pointers
- ❌ Missing unknown interface

**Action needed:**
- Add missing DirectX interface pointers
- Implement caching mechanism

## Missing Features

### 14. Command Line Argument Processing
**Original (ARCHITECTURE.md):**
- Copies to DAT_00c82b88 and DAT_00c82d88
- Length: 0x200 bytes each
- Purpose unknown

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ✅ Has g_szCmdLine1 and g_szCmdLine2
- ✅ Copies command line
- ❌ Doesn't process/parse arguments

**Action needed:**
- Determine if arguments are parsed
- Implement parsing logic if needed

### 15. Registry Settings Completeness
**Original (ARCHITECTURE.md):**
- Reads: Width, BitDepth, ShadowLOD, MaxFarClip, ParticleRate
- Potentially more settings not documented

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ✅ Reads documented settings
- ✅ Adds Height setting
- ❌ May be missing other settings

**Action needed:**
- Search for all registry reads in original
- Implement any missing settings

### 16. Error Handling and Logging
**Original (ARCHITECTURE.md):**
- FUN_0066f810: Fatal error function with message
- Used for DirectInput acquire failures
- Likely logs errors or shows message box

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- ❌ No error logging
- ❌ No fatal error handling
- ⚠️ Silent failures possible

**Action needed:**
- Implement error logging function
- Add fatal error handling
- Add message boxes for critical errors

## Structural Differences

### 17. Code Organization
**Original (ARCHITECTURE.md):**
- Functions at specific addresses
- Global variables at fixed addresses
- Complex interconnected call graph

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- Clean modular functions
- Named global variables
- Simplified call structure

**Note:** This is expected - decompiled code is cleaner than compiled code

### 18. Data Structures
**Original (ARCHITECTURE.md):**
- Complex C++ objects with vtables
- std::string usage throughout
- Multiple levels of indirection

**C++ Decompilation (CPP-ARCHITECTURE.md):**
- Simple C-style structures
- Minimal std::string usage
- Direct pointers

**Action needed:**
- Consider if C++ objects are necessary for functionality
- May need class structures for DirectX interfaces

## Summary

### Critical Missing Implementations:
1. DirectX initialization and device creation
2. DirectInput initialization and device creation
3. Game update logic and frame callbacks
4. Multiple global state variables
5. Error logging and fatal error handling

### Minor Issues:
1. SystemParams structure layout verification needed
2. Some global flags missing
3. Registry setting completeness check needed
4. Exception handling consideration

### Correctly Implemented:
1. ✅ Window class registration and creation
2. ✅ System parameter save/restore
3. ✅ Window placement persistence
4. ✅ Device lost recovery framework
5. ✅ Input acquire/unacquire framework
6. ✅ Frame timing with delta capping
7. ✅ Registry read/write with fallback
8. ✅ Main message loop structure
9. ✅ Window procedure message handling
10. ✅ Cursor management

### Next Steps:
The C++ decompilation provides a solid foundation with correct structure and many key features. The main work remaining is:
1. Implement DirectX and DirectInput initialization
2. Add missing global variables
3. Implement game update callbacks
4. Add error handling
5. Fill in TODO sections
6. Verify exact behavior matches original

The architecture is sound and compilation successful. Further iteration will close the functional gaps.
