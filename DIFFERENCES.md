# Differences and Decompiler Artifacts

## std::string handling
The decompiler shows many calls to `std::basic_string` constructors and destructors. These are often represented as `FUN_` calls or have complex stack layouts that might not directly map to clean C++ code without proper type definitions.

## Global Data
Many settings are stored in global variables (e.g., `DAT_00bf1940`, `DAT_008afbd9`). These need to be properly named and typed in the C++ implementation.

## Function Injections
The decompiler warns about `__SEH_prolog4` and `__SEH_epilog4` being replaced with injections. These are standard MSVC security/exception handling routines.

## Window Creation
In `CreateGameWindow` (`FUN_0060db20`), the `in_EAX` variable is used uninitialized in the decompiler output, but it likely comes from a register that Ghidra didn't track correctly or it's part of a larger structure.
```c
  local_10.bottom = in_EAX + param_4;
```
This likely should be `local_10.top + param_4` or similar.
