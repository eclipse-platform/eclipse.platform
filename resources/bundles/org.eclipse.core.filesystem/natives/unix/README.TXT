This directory contains the source code for Unix-based platforms,
including Linux and Mac OS X. These platforms
all share the same C source, but have different makefiles.

Command to build the native library:
make -f Makefile clean install

Don't commit the library that was locally built on the developer machine.
