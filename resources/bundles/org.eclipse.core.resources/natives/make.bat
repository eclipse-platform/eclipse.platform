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
REM build JNI header file
cd ..\bin
"C:\Program Files\Java\jdk1.8.0_65\bin\javah.exe" org.eclipse.core.internal.resources.refresh.win32.Win32Natives
move org_eclipse_core_internal_resources_refresh_win32_Win32Natives.h ..\natives\ref2.h

REM compile and link
cd ..\natives
set win_include="C:\Program Files\Microsoft Visual Studio 14.0\VC\include"
set jdk_include="C:\Program Files\Java\jdk1.8.0_65\include"

set dll_name=win32refresh.dll

call "c:\Program Files (x86)\Microsoft Visual Studio 14.0\VC\vcvarsall.bat" amd64_x86
"cl.exe" -I%win_include% -I%jdk_include% -I%jdk_include%\win32 -LD ref.c -Fe%dll_name%
move %dll_name% ..\..\org.eclipse.core.resources.win32.x86\os\win32\x86\%dll_name%

call "c:\Program Files (x86)\Microsoft Visual Studio 14.0\VC\vcvarsall.bat" amd64
"cl.exe" -I%win_include% -I%jdk_include% -I%jdk_include%\win32 -LD ref.c -Fe%dll_name%
move %dll_name% ..\..\org.eclipse.core.resources.win32.x86_64\os\win32\x86_64\%dll_name%