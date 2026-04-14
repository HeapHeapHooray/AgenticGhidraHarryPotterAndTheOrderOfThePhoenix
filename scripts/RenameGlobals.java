import ghidra.app.script.GhidraScript;
import ghidra.program.model.symbol.SourceType;
import ghidra.program.model.symbol.Symbol;

public class RenameGlobals extends GhidraScript {
    @Override
    public void run() throws Exception {
        rename("00bf1920", "g_pd3dDevice");
    }

    private void rename(String addrStr, String newName) throws Exception {
        Symbol s = getSymbolAt(toAddr(addrStr));
        if (s != null) {
            s.setName(newName, SourceType.USER_DEFINED);
            println("Renamed global at " + addrStr + " to " + newName);
        } else {
            createLabel(toAddr(addrStr), newName, true, SourceType.USER_DEFINED);
            println("Created label at " + addrStr + " for " + newName);
        }
    }
}
