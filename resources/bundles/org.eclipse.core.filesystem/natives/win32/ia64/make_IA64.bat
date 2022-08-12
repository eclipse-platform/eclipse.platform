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
del localfile.obj
del localfile_1_0_0*

call "%MSSDK%\bin\SetEnv.Cmd" /Release /ia64 /xp

set win_include=%MSSDK%\include
set lib_includes=UUID.LIB LIBCMT.LIB OLDNAMES.LIB KERNEL32.LIB
set jdk_include=%JAVA_HOME%\include
set dll_name=localfile_1_0_0

cl localfile.c -I%win_include% -I%jdk_include% -I%jdk_include%\win32 -LD -Fe%dll_name% /link %lib_includes%
