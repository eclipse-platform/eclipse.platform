#******************************************************************************
# Copyright (c) 2010 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     IBM Corporation - initial API and implementation
#******************************************************************************/
#
# makefile for libfastlinuxfile_1_0_0.so

CORE.C = fastlinuxfile.c
CORE.O = fastlinuxfile.o
LIB_NAME = libfastlinuxfile.so
LIB_NAME_FULL = libfastlinuxfile_1_0_0.so

#Set this to be your OS type
OS_TYPE = linux

#Set this to be the location of your JRE
ifeq (${JAVA_HOME},)
	JAVA_HOME = /usr/lib/jvm/java-17/
endif

JDK_INCLUDE = -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/${OS_TYPE}
OPT_FLAGS=-O2 -g -s -D_LARGEFILE_SOURCE -D_FILE_OFFSET_BITS=64

core:
	gcc $(OPT_FLAGS) -fPIC -c $(JDK_INCLUDE) -o $(CORE.O) $(CORE.C)
	gcc $(OPT_FLAGS) -shared -Wl,-soname,$(LIB_NAME) -o $(LIB_NAME_FULL) $(CORE.O) -lc

clean:
	rm -f $(CORE.O) $(LIB_NAME_FULL)

install: core
	rm -f ../../../../org.eclipse.core.filesystem.linux.x86_64/os/linux/x86_64/libfastlinuxfile_1_0_0.so
	mv libfastlinuxfile_1_0_0.so ../../../../org.eclipse.core.filesystem.linux.x86_64/os/linux/x86_64/
