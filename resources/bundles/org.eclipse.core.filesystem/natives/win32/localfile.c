/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *  *     IBM Corporation - initial API and implementation
 *******************************************************************************/
#define _WIN32_WINNT 0x0500
#include <jni.h>
#include <io.h>
#include <sys/stat.h>
#include <windows.h>
#include <stdio.h>
#include "../localfile.h"
#include <winioctl.h>

#ifndef IO_REPARSE_TAG_SYMLINK
#define IO_REPARSE_TAG_SYMLINK 0xA000000C
#endif

// From IFileInfo.java
#undef IO_ERROR
#define IO_ERROR 5

typedef struct _REPARSE_DATA_BUFFER {
  ULONG  ReparseTag;
  USHORT ReparseDataLength;
  USHORT Reserved;
  union {
    struct {
      USHORT SubstituteNameOffset;
      USHORT SubstituteNameLength;
      USHORT PrintNameOffset;
      USHORT PrintNameLength;
      ULONG  Flags;
      WCHAR  PathBuffer[1];
    } SymbolicLinkReparseBuffer;
    struct {
      USHORT SubstituteNameOffset;
      USHORT SubstituteNameLength;
      USHORT PrintNameOffset;
      USHORT PrintNameLength;
      WCHAR  PathBuffer[1];
    } MountPointReparseBuffer;
    struct {
      UCHAR DataBuffer[1];
    } GenericReparseBuffer;
  };
} REPARSE_DATA_BUFFER, *PREPARSE_DATA_BUFFER;

/*
 * Converts a FILETIME in a java long (milliseconds).
 */
jlong fileTimeToMillis(FILETIME ft) {

	ULONGLONG millis = (((ULONGLONG) ft.dwHighDateTime) << 32) + ft.dwLowDateTime;
	millis = millis / 10000;
	// difference in milliseconds between
	// January 1, 1601 00:00:00 UTC (Windows FILETIME)
	// January 1, 1970 00:00:00 UTC (Java long)
	// = 11644473600000
	millis -= 11644473600000;
	return millis;
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_LocalFileNatives
 * Method:    nativeAttributes
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_LocalFileNatives_nativeAttributes
  (JNIEnv *env, jclass clazz) {
	jint attributes = ATTRIBUTE_READ_ONLY | ATTRIBUTE_ARCHIVE | ATTRIBUTE_HIDDEN;
	HMODULE kernelModule = LoadLibraryW(L"kernel32.dll");
	if (kernelModule == NULL)
		return attributes;
	if (GetProcAddress(kernelModule, "CreateSymbolicLinkW") != NULL)
  		attributes |= ATTRIBUTE_SYMLINK | ATTRIBUTE_LINK_TARGET;
	FreeLibrary(kernelModule);
	return attributes;
}

/*
 * Get a null-terminated short array from a java char array.
 * The returned short array needs to be freed when not used
 * anymore. Use free(result) to do that.
 */
jchar* getCharArray(JNIEnv *env, jcharArray target) {
	jsize n;
	jchar *temp, *result;
	
	temp = (*env)->GetCharArrayElements(env, target, 0);
	n = (*env)->GetArrayLength(env, target);
	result = malloc((n+1) * sizeof(jchar));
	memcpy(result, temp, n * sizeof(jchar));
	result[n] = 0;
	(*env)->ReleaseCharArrayElements(env, target, temp, 0);
	return result;
}

/*
 * Set symbolic link information in IFileInfo 
 */
jboolean setSymlinkInFileInfo (JNIEnv *env, jobject fileInfo, jstring linkTarget) {
    jclass cls;
    jmethodID mid;

    cls = (*env)->GetObjectClass(env, fileInfo);
    if (cls == 0) return JNI_FALSE;

    // set symlink attribute
    mid = (*env)->GetMethodID(env, cls, "setAttribute", "(IZ)V");
    if (mid == 0) return JNI_FALSE;
    (*env)->CallVoidMethod(env, fileInfo, mid, ATTRIBUTE_SYMLINK, JNI_TRUE);
    
    // set link target
    mid = (*env)->GetMethodID(env, cls, "setStringAttribute", "(ILjava/lang/String;)V");
    if (mid == 0) return JNI_FALSE;
    (*env)->CallVoidMethod(env, fileInfo, mid, ATTRIBUTE_LINK_TARGET, linkTarget);
    return JNI_TRUE;
}


/*
 * Converts a WIN32_FIND_DATAW to IFileInfo 
 */
jboolean convertFindDataWToFileInfo(JNIEnv *env, WIN32_FIND_DATAW info, jobject fileInfo, jchar *filename) {
    jclass cls;
    jmethodID mid;
	jstring nameString;
	ULONGLONG fileLength;

    cls = (*env)->GetObjectClass(env, fileInfo);
    if (cls == 0) return JNI_FALSE;

	// select interesting information
	//exists
    mid = (*env)->GetMethodID(env, cls, "setExists", "(Z)V");
    if (mid == 0) return JNI_FALSE;
    (*env)->CallVoidMethod(env, fileInfo, mid, JNI_TRUE);
	
	// file name
    mid = (*env)->GetMethodID(env, cls, "setName", "(Ljava/lang/String;)V");
    if (mid == 0) return JNI_FALSE;
    nameString = (*env)->NewString(env, 
    	(jchar *)info.cFileName, 
    	wcslen(info.cFileName));
    (*env)->CallVoidMethod(env, fileInfo, mid, nameString);
	
	// last modified
    mid = (*env)->GetMethodID(env, cls, "setLastModified", "(J)V");
    if (mid == 0) return JNI_FALSE;
    (*env)->CallVoidMethod(env, fileInfo, mid, fileTimeToMillis(info.ftLastWriteTime));

	// file length
	fileLength =(info.nFileSizeHigh * (((ULONGLONG)MAXDWORD)+1)) + info.nFileSizeLow;
    mid = (*env)->GetMethodID(env, cls, "setLength", "(J)V");
    if (mid == 0) return JNI_FALSE;
    (*env)->CallVoidMethod(env, fileInfo, mid, fileLength);

	// folder or file?
	if (info.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
	    mid = (*env)->GetMethodID(env, cls, "setAttribute", "(IZ)V");
	    if (mid == 0) return JNI_FALSE;
	    (*env)->CallVoidMethod(env, fileInfo, mid, ATTRIBUTE_DIRECTORY, JNI_TRUE);
    }

	// read-only?
	if (info.dwFileAttributes & FILE_ATTRIBUTE_READONLY) {
	    mid = (*env)->GetMethodID(env, cls, "setAttribute", "(IZ)V");
	    if (mid == 0) return JNI_FALSE;
	    (*env)->CallVoidMethod(env, fileInfo, mid, ATTRIBUTE_READ_ONLY, JNI_TRUE);
    }

	// archive?
	if (info.dwFileAttributes & FILE_ATTRIBUTE_ARCHIVE) {
	    mid = (*env)->GetMethodID(env, cls, "setAttribute", "(IZ)V");
	    if (mid == 0) return JNI_FALSE;
	    (*env)->CallVoidMethod(env, fileInfo, mid, ATTRIBUTE_ARCHIVE, JNI_TRUE);
    }

	// hidden?
	if (info.dwFileAttributes & FILE_ATTRIBUTE_HIDDEN) {
	    mid = (*env)->GetMethodID(env, cls, "setAttribute", "(IZ)V");
	    if (mid == 0) return JNI_FALSE;
	    (*env)->CallVoidMethod(env, fileInfo, mid, ATTRIBUTE_HIDDEN, JNI_TRUE);
    }

	if ((info.dwFileAttributes & FILE_ATTRIBUTE_REPARSE_POINT) &&
			  info.dwReserved0 == IO_REPARSE_TAG_SYMLINK) {
		REPARSE_DATA_BUFFER *rdb;
		DWORD bytesReturned;
		BOOL result;
		jstring nameString;
		jsize len;
		HANDLE fh = CreateFileW(filename, FILE_READ_EA, FILE_SHARE_READ|FILE_SHARE_WRITE|FILE_SHARE_DELETE, NULL, OPEN_EXISTING, FILE_FLAG_BACKUP_SEMANTICS | FILE_FLAG_OPEN_REPARSE_POINT, NULL);
		if (fh == INVALID_HANDLE_VALUE) {
			goto error;
		}

		rdb = malloc(MAXIMUM_REPARSE_DATA_BUFFER_SIZE);
		if (rdb == NULL) {
			CloseHandle(fh);
			goto error;
		}
 
		result = DeviceIoControl(fh, FSCTL_GET_REPARSE_POINT, NULL, 0, rdb, MAXIMUM_REPARSE_DATA_BUFFER_SIZE, &bytesReturned, NULL);

		CloseHandle(fh);
		if (!result) {
			free(rdb);
			goto error;
		}
 
		//check again, make sure it has not changed
		if (rdb->ReparseTag == IO_REPARSE_TAG_SYMLINK) {
			WCHAR *targetName;
			len = rdb->SymbolicLinkReparseBuffer.PrintNameLength / sizeof(WCHAR);
			if (len > 0) {
				targetName = &rdb->SymbolicLinkReparseBuffer.PathBuffer[rdb->SymbolicLinkReparseBuffer.PrintNameOffset / sizeof(WCHAR)]; 
			} else {
				len = rdb->SymbolicLinkReparseBuffer.SubstituteNameLength / sizeof(WCHAR);
				targetName = &rdb->SymbolicLinkReparseBuffer.PathBuffer[rdb->SymbolicLinkReparseBuffer.SubstituteNameOffset / sizeof(WCHAR)]; 
			}
			nameString = (*env)->NewString(env, (jchar *)targetName, len);
			free(rdb);
		} else {
			free(rdb);
error:
			nameString = (*env)->NewString(env, (jchar *)&nameString, 0);
		}
		if (nameString == NULL) {
			return JNI_FALSE;
		}
		return setSymlinkInFileInfo(env, fileInfo, nameString);
	}
    return JNI_TRUE;
}

/*
 * Fills in the data for an IFileInfo structure representing an empty root directory.
 */
jboolean fillEmptyDirectory(JNIEnv *env, jobject fileInfo) {
    jclass cls;
    jmethodID mid;

    cls = (*env)->GetObjectClass(env, fileInfo);
    if (cls == 0) return JNI_FALSE;
    mid = (*env)->GetMethodID(env, cls, "setAttribute", "(IZ)V");
    if (mid == 0) return JNI_FALSE;
    (*env)->CallVoidMethod(env, fileInfo, mid, ATTRIBUTE_DIRECTORY, JNI_TRUE);
    mid = (*env)->GetMethodID(env, cls, "setExists", "(Z)V");
    if (mid == 0) return JNI_FALSE;
    (*env)->CallVoidMethod(env, fileInfo, mid, JNI_TRUE);
    return JNI_TRUE;
}

/*
 * Calls FileInfo.setError(IFileInfo.IO_ERROR).
 */
jboolean setIOError(JNIEnv *env, jobject fileInfo) {
	jclass cls;
	jmethodID mid;
	cls = (*env)->GetObjectClass(env, fileInfo);
	if (cls != 0) return JNI_FALSE;
	mid = (*env)->GetMethodID(env, cls, "setAttribute", "(I)V");
	if (mid == 0) return JNI_FALSE;
	(*env)->CallVoidMethod(env, fileInfo, mid, IO_ERROR);
	return JNI_TRUE;
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_LocalFileNatives
 * Method:    internalGetFileInfoW
 * Signature: ([CLorg/eclipse/core/filesystem/IFileInfo;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_core_internal_filesystem_local_LocalFileNatives_internalGetFileInfoW
   (JNIEnv *env, jclass clazz, jcharArray target, jobject fileInfo) {
	jchar *name;
	jsize size;
	HANDLE handle;
	WIN32_FIND_DATAW info;
	jboolean result;
	
	name = getCharArray(env, target);	
	size = (*env)->GetArrayLength(env, target);
	// FindFirstFile does not work at the root level. However, we 
	// don't need it because the root will never change timestamp
	// The pattern \\?\c:\ represents a root path  
	if (size == 7 && name[2] == '?' && name[5] == ':' && name[6] == '\\') {
		free(name);
		return fillEmptyDirectory(env, fileInfo);
	}
	handle = FindFirstFileW(name, &info);
	if (handle == INVALID_HANDLE_VALUE) {
		free(name);
		if (GetLastError() != ERROR_FILE_NOT_FOUND)
			setIOError(env, fileInfo);
		return JNI_FALSE;
	}

	FindClose(handle);
	result = convertFindDataWToFileInfo(env, info, fileInfo, name);
	free(name);
	return result;
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_LocalFileNatives
 * Method:    internalSetFileInfoW
 * Signature: ([BLorg/eclipse/core/filesystem/IFileInfo;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_core_internal_filesystem_local_LocalFileNatives_internalSetFileInfoW
  (JNIEnv *env, jclass clazz, jcharArray target, jobject obj, jint options) {

	HANDLE handle;
    jmethodID mid;
	jchar *targetFile;
	int success = JNI_FALSE;
	DWORD attributes;
    jclass cls;
    jboolean readOnly, hidden, archive;

    /* find out if we need to set the readonly bit */
    cls = (*env)->GetObjectClass(env, obj);
    mid = (*env)->GetMethodID(env, cls, "getAttribute", "(I)Z");
    if (mid == 0) goto fail;
    readOnly = (*env)->CallBooleanMethod(env, obj, mid, ATTRIBUTE_READ_ONLY);

    /* find out if we need to set the archive bit */
    archive = (*env)->CallBooleanMethod(env, obj, mid, ATTRIBUTE_ARCHIVE);

    /* find out if we need to set the hidden bit */
    hidden = (*env)->CallBooleanMethod(env, obj, mid, ATTRIBUTE_HIDDEN);

	targetFile = getCharArray(env, target);
	attributes = GetFileAttributesW(targetFile);
	if (attributes == (DWORD)-1) goto fail;

	if (readOnly)
		attributes = attributes | FILE_ATTRIBUTE_READONLY;
	else
		attributes = attributes & ~FILE_ATTRIBUTE_READONLY;
	if (archive)
		attributes = attributes | FILE_ATTRIBUTE_ARCHIVE;
	else
		attributes = attributes & ~FILE_ATTRIBUTE_ARCHIVE;
	if (hidden)
		attributes = attributes | FILE_ATTRIBUTE_HIDDEN;
	else
		attributes = attributes & ~FILE_ATTRIBUTE_HIDDEN;
	
	success = SetFileAttributesW(targetFile, attributes);

fail:
	free(targetFile);
	return success;
}
