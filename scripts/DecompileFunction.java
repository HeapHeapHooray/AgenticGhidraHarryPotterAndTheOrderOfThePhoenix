import ghidra.app.script.GhidraScript;
import ghidra.app.decompiler.*;
import ghidra.program.model.listing.Function;
import ghidra.program.model.address.Address;

public class DecompileFunction extends GhidraScript {
    @Override
    public void run() throws Exception {
        String[] args = getScriptArgs();
        if (args.length < 1) {
            println("Usage: DecompileFunction <address>");
            return;
        }

        Address addr = toAddr(args[0]);
        Function f = getFunctionAt(addr);
        if (f == null) {
            println("No function at " + addr);
            return;
        }

        DecompInterface di = new DecompInterface();
        di.openProgram(currentProgram);
        DecompileResults res = di.decompileFunction(f, 30, monitor);
        if (res.decompileCompleted()) {
            println(res.getDecompiledFunction().getC());
        } else {
            println("Decompilation failed: " + res.getErrorMessage());
        }
    }
}
