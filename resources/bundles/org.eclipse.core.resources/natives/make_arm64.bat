@rem ***************************************************************************
@rem Copyright (c) 2007, 2014 IBM Corporation and others.
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
REM build JNI header file
mkdir bin
cd ..\src
"%JAVA_HOME%\bin\javac.exe" -d ..\natives\bin -h . org\eclipse\core\internal\resources\refresh\win32\Win32Natives.java
move /y org_eclipse_core_internal_resources_refresh_win32_Win32Natives.h ..\natives\bin\ref.h

REM compile and link
cd ..\natives\bin
copy ..\ref.c .
set jdk_include="%JAVA_HOME%\include"
set dll_name=win32refresh.dll

cl.exe -I%jdk_include% -I%jdk_include%\win32 -LD ref.c -Fe%dll_name%
move /y %dll_name% ..\..\..\org.eclipse.core.resources.win32.aarch64\os\win32\aarch64\%dll_name%

REM clean up
cd ..
rmdir /q /s bin
