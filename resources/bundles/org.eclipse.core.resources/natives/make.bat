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
@rem     IBM Corporation - initial API and implementation
@rem ***************************************************************************
@echo off
REM build JNI header file
cd %~dp0\..\src

"%JAVA_HOME%\bin\javac" -h . org\eclipse\core\internal\resources\refresh\win32\Win32Natives.java
del org\eclipse\core\internal\resources\refresh\win32\Win32Natives.class
move org_eclipse_core_internal_resources_refresh_win32_Win32Natives.h ..\natives\ref.h

REM compile and link
if "%MSVC_HOME%"=="" set MSVC_HOME=C:\Program Files\Microsoft Visual Studio\2022\Community
cd ..\natives

set dll_name=win32refresh.dll

call "%MSVC_HOME%\VC\Auxiliary\Build\vcvarsall.bat" amd64
"cl.exe" -I%JAVA_HOME%\include -I%JAVA_HOME%\include\win32 -LD ref.c -Fe%dll_name%
move %dll_name% ..\..\org.eclipse.core.resources.win32.x86_64\os\win32\x86_64\%dll_name%