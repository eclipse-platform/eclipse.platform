#**********************************************************************
# Copyright (c) 2008, 2018 Oakland Software Incorporated and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#	Oakland Software Incorporated - initial submission
#	IBM Corporation - refactoring, bug 245849
#   Red Hat - GSettings implementation and code clean up (bug 394087)
#
#**********************************************************************
#
# makefile for libgnomeproxy-1.0.0.so

GNOMEPROXY.C = ../gnomeproxy.c
GNOMEPROXY.O = gnomeproxy.o
LIB_NAME = gnomeproxy.so
LIB_NAME_FULL = libgnomeproxy-1.0.0.so

OS_TYPE = linux
JDK_INCLUDE = -I${JAVA_HOME}/include -I${JAVA_HOME}/include/${OS_TYPE}
INCLUDE = `pkg-config --cflags gio-2.0`
COMPILER_FLAGS = -O0 -fPIC -g3 -Wall -c -fmessage-length=0 ${TARGET_ENVIRONMENT}

LIBS := `pkg-config --libs gio-2.0`
LINKER_FLAGS= ${TARGET_ENVIRONMENT}

all: link

compile:
	@echo "Building file: $(GNOMEPROXY.O)"
	@echo "Invoking: GCC C Compiler"
	gcc $(INCLUDE) $(JDK_INCLUDE) $(COMPILER_FLAGS) -o $(GNOMEPROXY.O) $(GNOMEPROXY.C)
	@echo "Finished building: $(GNOMEPROXY.O)"
	@echo " "

link: compile
	@echo "Building target: $(LIB_NAME_FULL)"
	@echo "Invoking: GCC C Linker"
	gcc $(LINKER_FLAGS) -shared -Wl,-soname,$(LIB_NAME) -o $(LIB_NAME_FULL) $(LIBS) $(GNOMEPROXY.O) -lc
	@echo "Finished building target: $(LIB_NAME_FULL)"
	@echo " "

clean:
	-$(RM) $(GNOMEPROXY.O) $(LIB_NAME_FULL)
	-@echo " "
