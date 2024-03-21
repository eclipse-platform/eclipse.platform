@rem ***************************************************************************
@rem Copyright (c) 2009, 2024 IBM Corporation and others.
@rem
@rem This program and the accompanying materials
@rem are made available under the terms of the Eclipse Public License 2.0
@rem which accompanies this distribution, and is available at
@rem https://www.eclipse.org/legal/epl-2.0/
@rem
@rem SPDX-License-Identifier: EPL-2.0
@rem
@rem Contributors:
@rem     IBM Corporation - initial API and implementation
@rem ***************************************************************************
@echo off

cd %~dp0

del localfile.obj
del localfile_1_0_0*

if "%MSVC_HOME%"=="" set MSVC_HOME=C:\Program Files\Microsoft Visual Studio\2022\Community

call "%MSVC_HOME%\VC\Auxiliary\Build\vcvarsall.bat" x64

set lib_includes=UUID.LIB LIBCMT.LIB OLDNAMES.LIB KERNEL32.LIB
set dll_name=localfile_1_0_0

cl ..\localfile.c -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" -LD -Fe%dll_name% /link %lib_includes% /Subsystem:CONSOLE
