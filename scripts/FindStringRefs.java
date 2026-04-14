import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;
import ghidra.program.model.symbol.*;
import ghidra.program.model.address.Address;

public class FindStringRefs extends GhidraScript {
    @Override
    public void run() throws Exception {
        String[] searchStrings = {"iMsgRunningTick", "UpdateCameras", "UpdateCoordinates", "DebugRender"};
        for (String searchString : searchStrings) {
            println("Searching for: " + searchString);
            Address addr = find(null, searchString.getBytes());
            if (addr == null) {
                println("  String not found");
                continue;
            }
            println("  Found string at " + addr);

            Reference[] refs = getReferencesTo(addr);
            for (Reference ref : refs) {
                println("  Reference from " + ref.getFromAddress());
                Function f = getFunctionContaining(ref.getFromAddress());
                if (f != null) {
                    println("    In function: " + f.getName() + " at " + f.getEntryPoint());
                }
            }
        }
    }
}
