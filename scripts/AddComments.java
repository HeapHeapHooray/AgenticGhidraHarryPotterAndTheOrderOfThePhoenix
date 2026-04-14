import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.Address;

public class AddComments extends GhidraScript {
    @Override
    public void run() throws Exception {
        addComment("006f54d2", CodeUnit.PLATE_COMMENT, "CRT entry point. Initializes runtime and calls WinMain.");
        addComment("0060dfa0", CodeUnit.PLATE_COMMENT, "WinMain: Main game entry point. Initializes system parameters, checks for existing window, loads settings, and creates the main window.");
        addComment("0060db20", CodeUnit.PLATE_COMMENT, "Creates the main game window using 'OrderOfThePhoenixMainWndClass'.");
    }

    private void addComment(String addrStr, int type, String comment) throws Exception {
        Address addr = toAddr(addrStr);
        Listing listing = currentProgram.getListing();
        listing.setComment(addr, type, comment);
        println("Added comment to " + addrStr);
    }
}
