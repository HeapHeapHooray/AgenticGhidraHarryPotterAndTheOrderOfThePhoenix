// Verify Iteration 6 annotations are present
// @category Analysis

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.symbol.*;

public class VerifyAnnotations extends GhidraScript {

    @Override
    public void run() throws Exception {
        println("=== Verifying Iteration 6 Annotations ===\n");
        
        // Check functions
        checkFunction("00618010", "GetGameTime");
        checkFunction("00617f50", "UpdateFrameTimingPrimary");
        checkFunction("00617ee0", "InterpolateFrameTime");
        checkFunction("00618140", "GameFrameUpdate");
        
        // Check data symbols
        checkSymbol("00c83190", "g_ullCallbackInterval_Low");
        checkSymbol("00c83110", "g_dwGameTicks");
        checkSymbol("00c83130", "g_nFrameFlip");
        checkSymbol("00e6e880", "g_FrameCallbackSlots");
        
        println("\n=== Verification Complete ===");
    }
    
    private void checkFunction(String addrStr, String expectedName) {
        try {
            Address[] addrs = currentProgram.parseAddress(addrStr);
            if (addrs == null || addrs.length == 0) {
                println(String.format("✗ Invalid address: %s", addrStr));
                return;
            }
            Address addr = addrs[0];
            
            FunctionManager fm = currentProgram.getFunctionManager();
            Function func = fm.getFunctionAt(addr);
            
            if (func != null) {
                String actualName = func.getName();
                if (expectedName.equals(actualName)) {
                    println(String.format("✓ Function @ %s: %s", addrStr, actualName));
                    String comment = func.getComment();
                    if (comment != null && comment.length() > 0) {
                        println(String.format("  Comment: %s...", 
                            comment.substring(0, Math.min(60, comment.length()))));
                    }
                } else {
                    println(String.format("✗ Function @ %s: expected '%s', got '%s'", 
                        addrStr, expectedName, actualName));
                }
            } else {
                println(String.format("✗ No function at %s", addrStr));
            }
        } catch (Exception e) {
            println(String.format("✗ Error checking %s: %s", addrStr, e.getMessage()));
        }
    }
    
    private void checkSymbol(String addrStr, String expectedName) {
        try {
            Address[] addrs = currentProgram.parseAddress(addrStr);
            if (addrs == null || addrs.length == 0) {
                println(String.format("✗ Invalid address: %s", addrStr));
                return;
            }
            Address addr = addrs[0];
            
            SymbolTable symTable = currentProgram.getSymbolTable();
            Symbol[] symbols = symTable.getSymbols(addr);
            
            if (symbols.length > 0) {
                String actualName = symbols[0].getName();
                if (expectedName.equals(actualName)) {
                    println(String.format("✓ Symbol @ %s: %s", addrStr, actualName));
                } else {
                    println(String.format("✗ Symbol @ %s: expected '%s', got '%s'", 
                        addrStr, expectedName, actualName));
                }
            } else {
                println(String.format("✗ No symbol at %s", addrStr));
            }
        } catch (Exception e) {
            println(String.format("✗ Error checking %s: %s", addrStr, e.getMessage()));
        }
    }
}
