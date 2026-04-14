# Ghidra script to export function info and comments
from ghidra.program.model.listing import CodeUnit

fm = currentProgram.getFunctionManager()
listing = currentProgram.getListing()

functions = fm.getFunctions(True)  # True means forward
for f in functions:
    print("Function: {} at {}".format(f.getName(), f.getEntryPoint()))

    # Get comments for the function
    plate = listing.getComment(CodeUnit.PLATE_COMMENT, f.getEntryPoint())
    if plate:
        print("  Plate: {}".format(plate))

    # Iterate through the function's instructions
    addr = f.getEntryPoint()
    while addr < f.getBody().getMaxAddress():
        cu = listing.getCodeUnitAt(addr)
        if cu:
            eol = cu.getComment(CodeUnit.EOL_COMMENT)
            pre = cu.getComment(CodeUnit.PRE_COMMENT)
            post = cu.getComment(CodeUnit.POST_COMMENT)
            if eol or pre or post:
                print("  Addr: {}".format(addr))
                if pre:
                    print("    Pre: {}".format(pre))
                if post:
                    print("    Post: {}".format(post))
                if eol:
                    print("    EOL: {}".format(eol))
        addr = addr.next()
