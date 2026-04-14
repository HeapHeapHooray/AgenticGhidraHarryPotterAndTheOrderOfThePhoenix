import pyghidra
import os

# Set Ghidra path
ghidra_path = "/snap/ghidra/current/ghidra_12.0_PUBLIC"
pyghidra.start(install_dir=ghidra_path)

project_dir = "/home/heap/Documents/AgenticGhidraHarryPotterAndTheOrderOfThePhoenix"
project_name = "HP_Project"
program_name = "hp.exe"

# Use open_project and program_context
with pyghidra.open_project(project_dir, project_name) as project:
    # In some versions of pyghidra, program_context takes (project, program_path)
    # program_path is relative to the root folder of the project
    with pyghidra.program_context(project, "/" + program_name) as program:
        fm = program.getFunctionManager()
        
        total_functions = 0
        annotated_functions = 0
        
        func_iter = fm.getFunctions(True)
        while func_iter.hasNext():
            func = func_iter.next()
            total_functions += 1
            
            name = func.getName()
            comment = func.getComment()
            
            # Consider a function "annotated" if:
            # 1. Name doesn't start with "FUN_" or "thunk_" (default Ghidra naming)
            # 2. Has a non-empty comment
            is_default_name = name.startswith("FUN_") or name.startswith("thunk_")
            is_annotated = not is_default_name or (
                comment is not None and len(comment.strip()) > 0
            )
            
            if is_annotated:
                annotated_functions += 1
                if annotated_functions <= 20:
                    print(f"Annotated Example: {name}")
                
        print("=== Function Count Results ===")
        print(f"Total Functions: {total_functions}")
        print(f"Annotated Functions: {annotated_functions}")
        print(f"Unannotated Functions: {total_functions - annotated_functions}")
        if total_functions > 0:
            coverage = annotated_functions * 100.0 / total_functions
            print(f"Coverage: {coverage:.2f}%")
        else:
            print("Coverage: 0.00%")
