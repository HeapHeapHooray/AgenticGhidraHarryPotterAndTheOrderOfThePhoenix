# Iteration 8 exploration script for HP.exe
# Focus: Game logic (spells, AI, physics), Asset loading, UI, Animation
# Run in Ghidra with Jython

from ghidra.program.model.address import Address
from ghidra.program.model.listing import *
from ghidra.program.model.mem import *
from ghidra.program.model.symbol import *


def findStringReferences(searchStr):
    """Find all references to a string in memory"""
    results = []
    mem = currentProgram.getMemory()

    for block in mem.getBlocks():
        if block.isInitialized():
            found = mem.findBytes(block.getStart(), searchStr, None, True, monitor)
            while found and block.contains(found):
                results.append(found)
                searchStart = found.add(1)
                if searchStart.compareTo(block.getEnd()) >= 0:
                    break
                found = mem.findBytes(searchStart, searchStr, None, True, monitor)

    return results


def searchAndPrint(keyword):
    """Search for a keyword and print references"""
    results = findStringReferences(keyword)
    if results:
        print('"{}" found: {} references'.format(keyword, len(results)))
        for i, addr in enumerate(results[:5]):
            func = getFunctionContaining(addr)
            if func:
                print("  {} in {}".format(addr, func.getName()))
            else:
                print("  {} (no function)".format(addr))


def findExternalRefs(extFuncName):
    """Find references to external function"""
    symTable = currentProgram.getSymbolTable()
    symbols = symTable.getSymbols(extFuncName)

    refs = []
    for sym in symbols:
        if sym.getSymbolType() == SymbolType.FUNCTION:
            references = getReferencesTo(sym.getAddress())
            for ref in references:
                refs.append(ref.getFromAddress())

    return refs


print("=== ITERATION 8 EXPLORATION ===")
print("Objective: Game logic, asset loading, UI, animation systems\n")

# SPELL SYSTEM
print("\n========================================")
print("SPELL SYSTEM EXPLORATION")
print("========================================")

spellKeywords = [
    "Expelliarmus",
    "Stupefy",
    "Protego",
    "Incendio",
    "Wingardium",
    "Reducto",
    "Petrificus",
    "Accio",
    "Reparo",
    "Lumos",
    "spell",
    "wand",
    "cast",
    "mana",
    "magic",
]

for keyword in spellKeywords:
    searchAndPrint(keyword)

# AI SYSTEM
print("\n========================================")
print("AI SYSTEM EXPLORATION")
print("========================================")

aiKeywords = [
    "IDLE",
    "PATROL",
    "CHASE",
    "ATTACK",
    "FLEE",
    "DEAD",
    "STUNNED",
    "idle",
    "patrol",
    "chase",
    "attack",
    "flee",
    "alert",
]

for keyword in aiKeywords:
    searchAndPrint(keyword)

# PHYSICS
print("\n========================================")
print("PHYSICS SYSTEM EXPLORATION")
print("========================================")

physicsKeywords = ["Havok", "PhysX", "collision", "velocity", "gravity"]

for keyword in physicsKeywords:
    searchAndPrint(keyword)

# ASSET LOADING
print("\n========================================")
print("ASSET LOADING EXPLORATION")
print("========================================")

# Check CreateFileA usage
createFileRefs = findExternalRefs("CreateFileA")
print("CreateFileA references: {}".format(len(createFileRefs)))
for i, addr in enumerate(createFileRefs[:10]):
    func = getFunctionContaining(addr)
    if func:
        print("  {} in {}".format(addr, func.getName()))

assetKeywords = ["pak", "dat", ".dds", ".tga", ".wav", ".mp3"]

for keyword in assetKeywords:
    searchAndPrint(keyword)

# UI SYSTEM
print("\n========================================")
print("UI SYSTEM EXPLORATION")
print("========================================")

uiKeywords = ["button", "menu", "slider", "font", "text"]

for keyword in uiKeywords:
    searchAndPrint(keyword)

# ANIMATION
print("\n========================================")
print("ANIMATION SYSTEM EXPLORATION")
print("========================================")

animKeywords = ["anim", "bone", "skeleton", "blend"]

for keyword in animKeywords:
    searchAndPrint(keyword)

print("\n=== EXPLORATION COMPLETE ===")
