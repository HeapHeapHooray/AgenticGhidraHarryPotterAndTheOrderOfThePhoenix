// Third comment/rename pass based on Q&A exploration
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.*;
import ghidra.program.model.symbol.*;

public class UpdateCommentsV3 extends GhidraScript {

    @Override
    public void run() throws Exception {

        addComment("0067d2e0", "QueryAvailableTextureMem",
            "Queries available video texture memory and handles low-memory state.\n\n" +
            "Calls IDirect3DDevice9::GetAvailableTextureMem (vtable+0x10).\n" +
            "Stores result >> 20 (i.e. in MB) into DAT_00bf193c.\n" +
            "If 0 < DAT_00bf193c < 33 (MB), calls thunk_FUN_00ebe85b() to handle\n" +
            "low video memory condition (may reduce texture quality).");

        addComment("0067ecf0", "SetCachedRenderTargets",
            "Sets render and depth-stencil targets only if they differ from cached values.\n\n" +
            "Params:\n" +
            "  param_1: new render target surface (IDirect3DSurface9*)\n" +
            "  param_2: new depth stencil surface (IDirect3DSurface9*)\n\n" +
            "Calls IDirect3DDevice9::SetRenderTarget(0, param_1) (vtable+0x94)\n" +
            "Calls IDirect3DDevice9::SetDepthStencilSurface(param_2) (vtable+0x9c)\n" +
            "Cached values: DAT_00af1390 (render target), DAT_00ae9250 (depth stencil).");

        addComment("00684f30", "UploadVertexShaders",
            "Uploads/binds previously compiled vertex shaders to the D3D device.\n\n" +
            "Two-pass:\n" +
            "  Pass 1: Release all shader objects in array DAT_009dcc54 (count=DAT_00bf1a08)\n" +
            "  Pass 2: Create/bind shaders via IDirect3DDevice9 vtable+0x16c with\n" +
            "          type index from DAT_009dcc50 and output into DAT_009dcc54\n\n" +
            "Called from InitRenderStates on every device reset.");

        addComment("00685410", "UploadPixelShaders",
            "Uploads extended pixel shader objects to the D3D device.\n\n" +
            "Count: DAT_00bf1a0c shaders, each entry 0x160 bytes at DAT_00e17ce0.\n" +
            "FUN_006855e0: creates/compiles each shader\n" +
            "FUN_00685550: binds each shader to the device.");

        addComment("0067eb90", "SetCachedRenderState",
            "Sets a D3D render state only if the value changed (avoids redundant calls).\n\n" +
            "Calls IDirect3DDevice9::SetRenderState(device, param_1, param_2) (vtable+0xe4).\n" +
            "Caches the last value in array DAT_00b94748[param_1].\n" +
            "Called from InitD3DStateDefaults e.g. with (25=D3DRS_ZENABLE, 8).");

        addComment("0066dfe0", "GetAdapterCapsPtr",
            "Returns a pointer into the adapter capabilities array.\n\n" +
            "Return: DAT_008ae200 * 0x584 + 0x454 + DAT_00bf19a8\n\n" +
            "Where:\n" +
            "  DAT_008ae200: selected adapter/mode index\n" +
            "  0x584: size of each adapter entry\n" +
            "  0x454: offset to D3DCAPS9 data within the entry\n" +
            "  DAT_00bf19a8: additional sub-offset (adapter index)\n\n" +
            "Used in RestoreDirectXResources to check texture capability bits.");

        addComment("00612f00", "InitFrameCallbackSystem",
            "Initialises the frame callback function pointer table.\n\n" +
            "Sets up the object at DAT_00e6e870:\n" +
            "  DAT_00e6e870 = &PTR_FUN_00883f3c  (primary frame callback entry)\n" +
            "  DAT_00e6e874 = &PTR_FUN_00883f4c  (secondary frame callback entry)\n" +
            "  _DAT_00bef728 = &DAT_00e6e870     (pointer to callback table)\n\n" +
            "This initializes DAT_008e1644 (referenced in GameFrameUpdate) so that\n" +
            "the double-buffered frame callbacks work correctly.\n" +
            "Called once from MainLoop or thunk_FUN_00eb5c3e with _atexit cleanup.");

        addComment("00eb5c3e", "GetOrInitCallbackManager",
            "Returns pointer to the frame callback manager object (DAT_00e6e870).\n\n" +
            "One-shot init guard at _DAT_00e74c20 bit 0:\n" +
            "  First call: runs InitFrameCallbackSystem and registers FUN_007b5640 cleanup\n" +
            "  Subsequent calls: returns immediately\n" +
            "Returns: &DAT_00e6e870 (the callback manager singleton).");

        addComment("00eb612e", "InitDirectXAndSubsystems",
            "Initializes DirectX device and core engine subsystems.\n\n" +
            "  FUN_00614370(height, ?, hWnd): creates the D3D device and sets present params\n" +
            "  FUN_0060c2e0(): additional graphics init\n" +
            "  DAT_008df65a = 1, DAT_00aeea5c = 2: subsystem state flags\n" +
            "  FUN_006147f0(): finalizes device setup\n" +
            "  thunk_FUN_00eb60d3(): possibly DirectInput or audio init\n\n" +
            "Returns: OR of all sub-init failure codes (0 = all succeeded).");

        addComment("00eb496e", "InitGameSubsystems",
            "Initializes game sub-systems after window and DirectX are ready.\n\n" +
            "  _DAT_00e69ca0 = FUN_0060c130: registers a callback/handler\n" +
            "  FUN_00688370(4, 0): likely DirectInput device enumeration or setup\n" +
            "  FUN_005090a0('LanguageSelect'): loads localization / language selection screen\n\n" +
            "Returns 1 on success.");

        addComment("00eb4a20", "ParseIntFromString",
            "Parses an integer from a std::basic_string using std::istringstream.\n\n" +
            "Input: in_EAX = pointer to std::basic_string<char>\n" +
            "Output: unaff_ESI = parsed integer\n\n" +
            "Used by WinMain when reading window placement registry values\n" +
            "(PosX, PosY, SizeX, SizeY).");

        addComment("0063d600", "BuildRenderBatch",
            "Builds and submits a deferred render batch node.\n\n" +
            "This is the per-node processor in the deferred render queue\n" +
            "(linked list rooted at DAT_00bef7c0, processed by ProcessDeferredCallbacks).\n\n" +
            "Each node (param_1) describes a geometry draw call:\n" +
            "  +0x78: current vertex index (0xffffffff = done)\n" +
            "  +0x50: mesh data pointer\n" +
            "  +0x34: LOD/quantization parameter\n" +
            "  +0x60: shader name override (or NULL)\n" +
            "  +0x70: transparency flag (0=opaque, 1=alpha)\n" +
            "  +0x84: render flags\n" +
            "  +0x7c: next node pointer (for list walking)\n\n" +
            "Shader types recognised: 'BLOOM', 'GLASS', 'BACKDROP'\n" +
            "Calls FUN_006a0130 to look up shader index, sets render state bits\n" +
            "in the material table at DAT_00ae9258.\n" +
            "Returns: 0 = more vertices to process, 1 = node complete (remove from list)");

        addComment("0058b790", "PauseGameObjects",
            "Pauses all game objects/systems via a state machine.\n\n" +
            "State machine: DAT_00c7b908 (current state, values 0-7):\n" +
            "  Default (not 4/5/6): full pause\n" +
            "    - triggers 'chapter-cutscene-in' animation via FUN_006a4510\n" +
            "    - calls vtable+0x24 (Pause) on: DAT_00c7c370, DAT_00c7b924,\n" +
            "      and the array at DAT_00c7ba4c (stride 0x12*4)\n" +
            "    - records pause time: _DAT_00c7b90c = DAT_00c8311c/3 + param_1\n" +
            "    - sets DAT_00c7b908 = 0 (paused state)\n" +
            "  States 4-5: partial pause (audio only via DAT_00c7c370)\n" +
            "    - sets DAT_00c7b908 = 6\n" +
            "  State 6: already paused, no-op\n\n" +
            "DAT_00c7c370: audio/music manager (has vtable Pause/Resume at +0x24/+0x28)\n" +
            "DAT_00c7b924: secondary pausable system\n" +
            "DAT_00c6d7e0: audio pause flag");

        addComment("0058b8a0", "ResumeGameObjects",
            "Resumes all paused game objects/systems.\n\n" +
            "State machine using DAT_00c7b908:\n" +
            "  States 0-3: full resume\n" +
            "    - triggers 'default' animation via FUN_006a4510\n" +
            "    - records resume time: _DAT_00c7b90c = DAT_00c8311c/3 + 1000\n" +
            "    - sets DAT_00c7b908 = 7\n" +
            "    - falls through to state 4\n" +
            "  State 4: resumes DAT_00c7c370, DAT_00c7b924, DAT_00c7ba4c array\n" +
            "    - calls vtable+0x28 (Resume) on all\n" +
            "    - also resumes sub-systems at DAT_00c7d038 (+0xc, stride 0xb*4)\n" +
            "  State 6: partial resume (DAT_00c7c370 only)\n" +
            "    - sets DAT_00c7b908 = 5");

        addComment("0061ef80", "PauseAudioManager",
            "Pauses the audio/music manager.\n\n" +
            "in_EAX is `this` pointer (called as a method).\n" +
            "Checks if *(*(in_EAX+4) + 4) != 0 (music is loaded/playing).\n" +
            "If so, calls FUN_006a9ea0() to actually pause the audio stream.");

        addComment("00612530", "DispatchRenderListeners",
            "Dispatches a render-mode change notification to a listener list.\n\n" +
            "Implements an observer pattern: param_1 points to a list head.\n" +
            "If list is non-empty:\n" +
            "  Sets dirty flag at list.head+0x13\n" +
            "  Iterates all nodes, for each node with vtable method at +2:\n" +
            "    Calls (*node.vtable[1])(param_1) to notify listener\n" +
            "  Clears dirty flag\n" +
            "  If pending-change flag at list.head+0x12: calls FUN_006125a0()\n\n" +
            "Returns 1 if dispatched, 0 if list was empty.");

        addComment("00eb6dbc", "QueryMemoryAllocatorMax",
            "Queries the maximum available block size from a memory allocator.\n\n" +
            "Uses a critical section (unaff_EDI+0x4e4) for thread safety.\n" +
            "Scans a priority-indexed free list (unaff_EDI+0x428, 0xfe entries)\n" +
            "and a linked list (unaff_EDI+0x30..0x3c) for the largest free block.\n" +
            "Values are masked to 8-byte alignment: & 0x7ffffff8.\n\n" +
            "Return: size of largest free contiguous block in the allocator\n" +
            "Caller (MainLoop) stores minimum in DAT_008afb08 for memory pressure tracking.");

        printf("UpdateCommentsV3: all comments applied\n");
    }

    private void addComment(String addrStr, String newName, String comment) {
        try {
            Address addr = currentProgram.getAddressFactory().getAddress(addrStr);
            Function f = getFunctionAt(addr);
            if (f == null) {
                printf("SKIP: %s\n", addrStr);
                return;
            }
            String cur = f.getName();
            if (cur.startsWith("FUN_") || cur.startsWith("thunk_FUN_") || cur.equals(cur)) {
                // only rename if still auto-named
                if (cur.startsWith("FUN_") || cur.startsWith("thunk_FUN_")) {
                    f.setName(newName, SourceType.USER_DEFINED);
                    printf("Renamed %s -> %s\n", addrStr, newName);
                }
            }
            f.setComment(comment);
            printf("Comment: %s (%s)\n", f.getName(), addrStr);
        } catch (Exception e) {
            printf("ERROR %s: %s\n", addrStr, e.getMessage());
        }
    }
}
