#!/bin/bash

# Build script for HP decompilation using Zig
# This cross-compiles for Windows x86 target

ZIG=/home/heap/Documents/zig-x86_64-linux-0.16.0-dev.3153+d6f43caad/zig

# Build for Windows x86 (32-bit) to match original exe
$ZIG c++ \
    -target x86-windows-gnu \
    -lc++ \
    -luser32 \
    -lgdi32 \
    -lole32 \
    -luuid \
    -lwinmm \
    -ldsound \
    -mwindows \
    main.cpp \
    -o ../hp_decompiled.exe

if [ $? -eq 0 ]; then
    echo "Build successful: ../hp_decompiled.exe"
else
    echo "Build failed"
    exit 1
fi
