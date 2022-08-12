@rem ***************************************************************************
@rem Copyright (c) 2005, 2014 IBM Corporation and others.
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
del localfile_3_2_0*
set win_include=k:\msvc60\vc98\include
set lib_includes=K:\Msvc60\VC98\Lib\UUID.LIB K:\Msvc60\VC98\Lib\LIBCMT.LIB K:\Msvc60\VC98\Lib\OLDNAMES.LIB K:\Msvc60\VC98\Lib\KERNEL32.LIB
set jdk_include=d:\vm\jdk1.5.0_05\include
set dll_name=localfile_1_0_0
cl -I%win_include% -I%jdk_include% -I%jdk_include%\win32 -LD localfile.c -Fe%dll_name% /link %lib_includes%
