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
# makefile for libunixfile_1_0_0.jnilib

CORE.C=../unixfile.c
LIB_NAME_FULL=libunixfile_1_0_0.jnilib

JAVA_HOME=$(shell /usr/libexec/java_home)

$(info JAVA_HOME is $(JAVA_HOME))

JDK_INCLUDE=-I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/darwin
MACOSX_DEPLOYMENT_TARGET=10.10

FRAMEWORKS=-framework CoreServices
# define MACOSX to include Mac OS X specific code
CC_FLAGS=-arch x86_64 -arch arm64 -mmacosx-version-min=10.10 -DMACOSX

core:
	cc $(JDK_INCLUDE) $(CORE.C) -o $(LIB_NAME_FULL) -bundle $(FRAMEWORKS) $(CC_FLAGS)

clean:
	rm -f $(LIB_NAME_FULL)

install: core
	rm -f ../../../../org.eclipse.core.filesystem.macosx/os/macosx/$(LIB_NAME_FULL)
	mv $(LIB_NAME_FULL) ../../../../org.eclipse.core.filesystem.macosx/os/macosx/
