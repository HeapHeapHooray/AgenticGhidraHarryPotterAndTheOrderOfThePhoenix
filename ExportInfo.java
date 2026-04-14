// Ghidra script to export function info and comments
// @category Decompilation
import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;

public class ExportInfo extends GhidraScript {
    @Override
    public void run() throws Exception {
        FunctionManager fm = currentProgram.getFunctionManager();
        Listing listing = currentProgram.getListing();

        FunctionIterator functions = fm.getFunctions(true);
        while (functions.hasNext()) {
            Function f = functions.next();
            println("Function: " + f.getName() + " at " + f.getEntryPoint());

            String plate = listing.getComment(CodeUnit.PLATE_COMMENT, f.getEntryPoint());
            if (plate != null) {
                println("  Plate: " + plate);
            }

            Address addr = f.getEntryPoint();
            while (addr != null && addr.compareTo(f.getBody().getMaxAddress()) <= 0) {
                CodeUnit cu = listing.getCodeUnitAt(addr);
                if (cu != null) {
                    String eol = cu.getComment(CodeUnit.EOL_COMMENT);
                    String pre = cu.getComment(CodeUnit.PRE_COMMENT);
                    String post = cu.getComment(CodeUnit.POST_COMMENT);
                    if (eol != null || pre != null || post != null) {
                        println("  Addr: " + addr);
                        if (pre != null) println("    Pre: " + pre);
                        if (post != null) println("    Post: " + post);
                        if (eol != null) println("    EOL: " + eol);
                    }
                }
                addr = addr.add(1); // This is not ideal for variable length instructions, but let's see
                // Better way to iterate through code units:
                cu = listing.getCodeUnitAfter(addr.subtract(1));
                if (cu == null) break;
                addr = cu.getAddress();
            }
        }
    }
}
