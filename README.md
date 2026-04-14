### An attempt to decompile the game Harry Potter and The Order Of The Phoenix using an AI agent and Ghidra.

## Project Status (Iteration 8)

**Current Iteration:** 8 - High-level game systems (spells, physics, streaming, assets, AI, UI)  
**Compilation Status:** ✅ Builds successfully (832 KB executable)  
**Subsystem Coverage:** 65% (13/20 major systems documented)  
**Code Implementation:** ~0.56% (2,780 lines vs ~500,000 estimated)  
**Runnable:** ❌ Not yet (missing asset loading, rendering, physics implementation)

### Progress Summary
- **Iterations 1-4:** Foundation (window management, DirectX, registry, command line, timing)
- **Iteration 5:** Subsystem structures (memory allocator, message dispatch, scene management)
- **Iteration 6:** Frame timing callbacks and dispatch system
- **Iteration 7:** Audio, input, render queue, subsystem integration
- **Iteration 8:** High-level game systems (spells, physics, streaming, assets, AI, UI)

### Key Findings
- **Third-Party Middleware:** Havok Physics 3.x/4.x, RenderWare Graphics, Trinity Sequencer (EA)
- **File Formats:** RCB (animation), HKX (physics), Babble (dialogue), Hull (collision), Spline (paths), Trinity (cutscenes)
- **Architecture:** 16.16 fixed-point timing, 60 FPS target, 3x game speed scaling, 8-slot callback system, FNV-1a message dispatch
- **Game Systems:** Event-driven spell casting, zone streaming (4 load + 3 unload), locator-based AI patrol, 32-button UI support

### Documentation
- **ARCHITECTURE.md** - Complete reverse-engineered architecture of hp.exe (2,527 lines, iterations 1-8)
- **QUESTIONS.md** - 123 questions for further exploration (Q1-Q123)
- **CPP-ARCHITECTURE.md** - C++ decompilation architecture (iteration 8)
- **DIFFERENCES.md** - Comparison between original and decompilation
- **workspace/ITER8-FINDINGS.md** - Detailed iteration 8 exploration results

### Annotated Functions/Structures
- **Functions:** 50+ major functions documented
- **Globals:** 100+ global variables identified
- **Structures:** 30+ data structures defined
- **Subsystems:** 13 major subsystems documented

# Dependencies
- **Zig** (for cross-compiling from Linux to Windows)
- **Ghidra** (for reverse engineering)
- **Original hp.exe** (5.3 MB, from game installation)

# Usage
1. Download Harry_Potter_and_the_Order_of_the_Phoenix_Win_Preinstalled_EN.zip from https://oldgamesdownload.com/harry-potter-and-the-order-of-the-phoenix-eks/
2. Clone this repository
3. Extract `hp.exe` from the downloaded zip into the repository root
4. Open Ghidra and load `HP_Project.gpr`
5. For AI agent-assisted decompilation: Follow instructions in `INSTRUCTIONS-AGENT.txt`

# Building the Decompilation
```bash
cd decompilation-src
./build.sh
```

Output: `../hp_decompiled.exe` (832 KB)

**Note:** Currently compiles but does not run due to missing implementations (asset loading, rendering, physics). See DIFFERENCES.md for details.

# Next Steps
See DIFFERENCES.md "Recommended Implementation Order" section for phased implementation plan.
