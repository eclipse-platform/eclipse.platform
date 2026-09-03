
set "MSVC_HOME=C:\Program Files\Microsoft Visual Studio\2022\Community"
call "%MSVC_HOME%\VC\Auxiliary\Build\vcvarsall.bat" x64

@echo on

jextract --output src-gen ^
         --include-function GetLastError ^
         --include-function GetShortPathNameW ^
         --include-function GetFileAttributesW ^
         --include-function SetFileAttributesW ^
         --include-function FindFirstFileW ^
         --include-function FindClose ^
         --include-typedef WIN32_FIND_DATAW ^
         --include-struct _WIN32_FIND_DATAW ^
         --include-typedef FILETIME ^
         --include-struct _FILETIME ^
         --include-constant MAXDWORD ^
         --include-constant FILE_ATTRIBUTE_NORMAL ^
         --include-constant FILE_ATTRIBUTE_ARCHIVE ^
         --include-constant FILE_ATTRIBUTE_READONLY ^
         --include-constant FILE_ATTRIBUTE_HIDDEN ^
         --include-constant FILE_ATTRIBUTE_DIRECTORY ^
         --include-constant FILE_ATTRIBUTE_REPARSE_POINT ^
         --include-constant IO_REPARSE_TAG_SYMLINK ^
         --include-constant INVALID_FILE_ATTRIBUTES ^
         --include-constant INVALID_HANDLE_VALUE ^
         --include-constant ERROR_FILE_NOT_FOUND ^
         --include-constant ERROR_PATH_NOT_FOUND ^
         --target-package com.microsoft ^
         --header-class-name Windows ^
         --library kernel32 ^
         --include-dir "%INCLUDE%" ^
         "Windows.h"
