@rem ***************************************************************************
@rem Copyright (c) 2002, 2011 IBM Corporation and others.
m
m This program and the accompanying materials
@rem are made available under the terms of the Eclipse Public License 2.0
@rem which accompanies this distribution, and is available at
m https://www.eclipse.org/legal/epl-2.0/
m
m SPDX-License-Identifier: EPL-2.0
@rem
@rem Contributors:
@rem     IBM Corporation - initial API and implementation
@rem ***************************************************************************
@echo off
rem VCM UI benchmark script
rem Expects the following plugins to be installed:
rem   org.eclipse.core.tests.harness
rem   org.eclipse.team.core
rem   org.eclipse.team.cvs.core
rem   org.eclipse.team.cvs.ui
rem   org.eclipse.team.tests.cvs.core
rem   org.eclipse.team.ui
rem   org.eclipse.vcm.core
rem   org.eclipse.vcm.core.cvs
rem   org.eclipse.vcm.tests.core
rem   org.eclipse.vcm.tests.ui
rem   org.eclipse.vcm.ui
rem   org.eclipse.vcm.ui.cvs
rem   org.junit

set ROOT=D:\PerformanceTesting

set ECLIPSE=%ROOT%\eclipse
set REPOSITORY_PROPERTIES=%ROOT%\repository.properties
set TEST=vcmui.benchmark.all
set LOG=%ROOT%\%TEST%.xml
set REPEAT=21
set IGNOREFIRST=

set PLUGINS=%ECLIPSE%\plugins
set WORKSPACE=%ECLIPSE%\workspace
set JRE=%ROOT%\jre
set JAVA=%JRE%\bin\java.exe
set HARNESS=org.eclipse.team.tests.cvs.core.harness

set VMARGS=-Declipse.tests.vcm.properties=%REPOSITORY_PROPERTIES%
set PROGARGS=-dev bin -application %HARNESS% -test %TEST% -log %LOG% -purge -repeat %REPEAT% %IGNOREFIRST%

pushd %ECLIPSE%
echo Purging the workspace: %WORKSPACE%
del /S /F /Q %WORKSPACE% >NUL:
@echo on
@echo Running VCM UI benchmark test
%JAVA% -cp startup.jar %VMARGS% org.eclipse.core.launcher.UIMain %PROGARGS%
@echo off
popd
