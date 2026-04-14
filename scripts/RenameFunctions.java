import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Function;
import ghidra.program.model.symbol.SourceType;

public class RenameFunctions extends GhidraScript {
    @Override
    public void run() throws Exception {
        rename("00eb4b95", "RegisterWindowClass");
        rename("0060d6d0", "WindowProc");
        rename("0060dc10", "MainLoop");
        rename("0060dfa0", "WinMain");
        rename("0060db20", "CreateGameWindow");
        rename("0060ce60", "ReadRegistrySetting");
        rename("0060deb0", "UpdateSystemParameters");
        rename("0067d310", "DirectX_UpdateDevice");
    }

    private void rename(String addrStr, String newName) throws Exception {
        Function f = getFunctionAt(toAddr(addrStr));
        if (f != null) {
            f.setName(newName, SourceType.USER_DEFINED);
            println("Renamed " + addrStr + " to " + newName);
        } else {
            println("No function at " + addrStr);
        }
    }
}
