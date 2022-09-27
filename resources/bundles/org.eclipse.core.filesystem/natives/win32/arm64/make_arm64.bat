@rem ***************************************************************************
@rem Copyright (c) 2009, 2014 IBM Corporation and others.
@rem
@rem This program and the accompanying materials
@rem are made available under the terms of the Eclipse Public License 2.0
@rem which accompanies this distribution, and is available at
@rem https://www.eclipse.org/legal/epl-2.0/
@rem
@rem SPDX-License-Identifier: EPL-2.0
@rem
@rem Contributors:
@rem     Tue Ton - initial implementation
@rem ***************************************************************************
REM ----------------------------------------------------------------------------
REM Note: execute this batch file manually in a Visual Studio Developer Command Prompt window
REM       so that all VS command-line tools are already available in the path.
REM
REM Required environment variables:
REM   JAVA_HOME - path to the JDK11+ installation for ARM64
REM
REM ----------------------------------------------------------------------------
del localfile.obj
del localfile_1_0_0*

set lib_includes=UUID.LIB LIBCMT.LIB OLDNAMES.LIB KERNEL32.LIB
set jdk_include=%JAVA_HOME%\include
set dll_name=localfile_1_0_0

cl.exe ..\localfile.c -I"%jdk_include%" -I"%jdk_include%\win32" -LD -Fe%dll_name% /link %lib_includes% /Subsystem:CONSOLE
