@rem ***************************************************************************
@rem Copyright (c) 2007, 2024 IBM Corporation and others.
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
@rem     Tue Ton - add auto-search for MSVC compiler
@rem ***************************************************************************
REM ----------------------------------------------------------------------------
REM
REM Required environment variables:
REM   JAVA_HOME - path to the JDK11+ installation for x64 or ARM64 architecture
REM
REM ----------------------------------------------------------------------------
@echo off
echo
echo INFO Starting build of Win32 resource binaries.

@rem Specify VisualStudio Edition: 'Community', 'Enterprise', 'Professional' etc.
IF "x.%MSVC_EDITION%"=="x." set "MSVC_EDITION=auto"

@rem Specify VisualStudio Version: '2022', '2019', '2017' etc.
IF "x.%MSVC_VERSION%"=="x." set "MSVC_VERSION=auto"

@rem Search for a usable Visual Studio
@rem ---------------------------------
IF "%MSVC_HOME%"=="" CALL :ECHO "'MSVC_HOME' was not provided, auto-searching for Visual Studio..."
@rem Bug 574007: Path used on Azure build machines
IF "%MSVC_HOME%"=="" CALL :FindVisualStudio "%ProgramFiles(x86)%\Microsoft Visual Studio\$MSVC_VERSION$\BuildTools"
@rem Bug 578519: Common installation paths; VisualStudio is installed in x64 ProgramFiles since VS2022
IF "%MSVC_HOME%"=="" CALL :FindVisualStudio "%ProgramFiles%\Microsoft Visual Studio\$MSVC_VERSION$\$MSVC_EDITION$"
@rem Bug 578519: Common installation paths; VisualStudio is installed in x86 ProgramFiles before VS2022
IF "%MSVC_HOME%"=="" CALL :FindVisualStudio "%ProgramFiles(x86)%\Microsoft Visual Studio\$MSVC_VERSION$\$MSVC_EDITION$"
@rem Report
IF NOT EXIST "%MSVC_HOME%" (
	CALL :ECHO "WARNING: Microsoft Visual Studio was not found (for edition=%MSVC_EDITION% version=%MSVC_VERSION%)"
    CALL :ECHO "         Refer steps for Windows native setup: https://www.eclipse.org/swt/swt_win_native.php"
) ELSE (
	CALL :ECHO "MSVC_HOME: %MSVC_HOME%"
)

@rem Check for a usable JDK
IF "%JAVA_HOME%"=="" CALL :ECHO "'JAVA_HOME' was not provided"
IF NOT EXIST "%JAVA_HOME%" (
    CALL :ECHO "WARNING: 64-bit Java JDK not found. Please set JAVA_HOME to the JDK directory containing the intended JDK native headers."
)

@REM Compose host architecture string for MSVC
IF "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
  SET HOST_ARCH=x64
) ELSE IF "%PROCESSOR_ARCHITECTURE%"=="ARM64" (
  SET HOST_ARCH=arm64
) ELSE (
  CALL :ECHO "ERROR: Unknown host architecture: %PROCESSOR_ARCHITECTURE%."
  EXIT /B 1
)

@REM %TARGET_ARCH% may be specified by the caller for cross-compiling.
@REM If not, build for builder machine's architecture
IF "%TARGET_ARCH%"=="" (
  SET TARGET_ARCH=%HOST_ARCH%
)

@REM Compose build argument for MSVC
IF "%TARGET_ARCH%"=="%HOST_ARCH%" (
  SET BUILD_ARCH=%TARGET_ARCH%
) ELSE (
  SET BUILD_ARCH=%HOST_ARCH%_%TARGET_ARCH%
)

@REM Select build's output directory (if not specified) based on target arch
IF "%TARGET_ARCH%"=="x64" (
  IF "x.%OUTPUT_DIR%"=="x." SET OUTPUT_DIR=..\..\..\org.eclipse.core.resources.win32.x86_64\os\win32\x86_64
) ELSE IF "%TARGET_ARCH%"=="arm64" (
  IF "x.%OUTPUT_DIR%"=="x." SET OUTPUT_DIR=..\..\..\org.eclipse.core.resources.win32.aarch64\os\win32\aarch64
) ELSE (
  CALL :ECHO "ERROR: Unknown target architecture: %TARGET_ARCH%."
  EXIT /B 1
)

call "%MSVC_HOME%\VC\Auxiliary\Build\vcvarsall.bat" %BUILD_ARCH%

@rem if call to vcvarsall.bat (which sets up environment) silently fails, then provide advice to user.
WHERE cl
if %ERRORLEVEL% NEQ 0 (
    CALL :ECHO "ERROR: cl (Microsoft C compiler) not found on path. Please install Microsoft Visual Studio."
    CALL :ECHO "         If already installed, try launching eclipse from the 'Developer Command Prompt for VS'"
    CALL :ECHO "         Refer steps for SWT Windows native setup: https://www.eclipse.org/swt/swt_win_native.php"
    CALL :ECHO "ERROR: exit 1"	
    EXIT 1
)

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
mkdir %OUTPUT_DIR%
move /y %dll_name% %OUTPUT_DIR%\%dll_name%

REM clean up
cd ..
rmdir /q /s bin

GOTO :EOF

@rem Find Visual Studio
@rem %1 = path template with '$MSVC_VERSION$' and '$MSVC_EDITION$' tokens
:FindVisualStudio
	@rem Early return if already found
	IF NOT "%MSVC_HOME%"=="" GOTO :EOF

	IF "%MSVC_VERSION%"=="auto" (
		CALL :FindVisualStudio2 "%~1" "2022"
		CALL :FindVisualStudio2 "%~1" "2019"
		CALL :FindVisualStudio2 "%~1" "2017"
	) ELSE (
		CALL :FindVisualStudio2 "%~1" "%MSVC_VERSION%"
	)
GOTO :EOF

@rem Find Visual Studio
@rem %1 = path template with '$MSVC_VERSION$' and '$MSVC_EDITION$' tokens
@rem %2 = value for '$MSVC_VERSION$'
:FindVisualStudio2
	@rem Early return if already found
	IF NOT "%MSVC_HOME%"=="" GOTO :EOF

	IF "%MSVC_EDITION%"=="auto" (
		CALL :FindVisualStudio3 "%~1" "%~2" "Community"
		CALL :FindVisualStudio3 "%~1" "%~2" "Enterprise"
		CALL :FindVisualStudio3 "%~1" "%~2" "Professional"
	) ELSE (
		CALL :FindVisualStudio3 "%~1" "%~2" "%MSVC_EDITION%"
	)
GOTO :EOF

@rem Find Visual Studio
@rem %1 = path template with '$MSVC_VERSION$' and '$MSVC_EDITION$' tokens
@rem %2 = value for '$MSVC_VERSION$'
@rem %3 = value for '$MSVC_EDITION$'
:FindVisualStudio3
	@rem Early return if already found
	IF NOT "%MSVC_HOME%"=="" GOTO :EOF

	SET "TESTED_VS_PATH=%~1"
	@rem Substitute '$MSVC_VERSION$' and '$MSVC_EDITION$'
	CALL SET "TESTED_VS_PATH=%%TESTED_VS_PATH:$MSVC_VERSION$=%~2%%"
	CALL SET "TESTED_VS_PATH=%%TESTED_VS_PATH:$MSVC_EDITION$=%~3%%"

	@rem If the folder isn't there, then skip it without printing errors
	IF NOT EXIST "%TESTED_VS_PATH%" GOTO :EOF

	@rem Try this path
	CALL :TryToUseVisualStudio "%TESTED_VS_PATH%"
GOTO :EOF

@rem Test Visual Studio and set '%MSVC_HOME%' on success
@rem %1 = tested path
:TryToUseVisualStudio
	SET "TESTED_VS_PATH=%~1"
	IF NOT EXIST "%TESTED_VS_PATH%\VC\Auxiliary\Build\vcvarsall.bat" (
		CALL :ECHO "-- VisualStudio '%TESTED_VS_PATH%' is bad: 'vcvarsall.bat' not found"
		GOTO :EOF
	)
	CALL :ECHO "-- VisualStudio '%TESTED_VS_PATH%' looks good, selecting it"
	SET "MSVC_HOME=%TESTED_VS_PATH%"
GOTO :EOF

@rem Regular ECHO has trouble with special characters such as ().
@rem At the same time, if its argument is quoted, the quotes are printed literally.
@rem The workaround is to escape all special characters with ^
:ECHO
	SET "ECHO_STRING=%~1"
	SET "ECHO_STRING=%ECHO_STRING:<=^<%"
	SET "ECHO_STRING=%ECHO_STRING:>=^>%"
	SET "ECHO_STRING=%ECHO_STRING:(=^(%"
	SET "ECHO_STRING=%ECHO_STRING:)=^)%"
	ECHO %ECHO_STRING%
GOTO :EOF
