/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mikael Barbero (Eclipse Foundation) - 286681 handle WAIT_ABANDONED_0 return value
 *******************************************************************************/
#include <windows.h>
#include "ref.h"

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    FindFirstChangeNotificationW
 * Signature: (Ljava/lang/String;ZI)J
 */
JNIEXPORT jlong JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_FindFirstChangeNotificationW
(JNIEnv * env, jclass this, jstring lpPathName, jboolean bWatchSubtree, jint dwNotifyFilter) {
	jlong result;
	jsize numberOfChars;
	jchar *path;
	const jchar *temp;

	// create a new byte array to hold the prefixed and null terminated path
	numberOfChars= (*env)->GetStringLength(env, lpPathName);
	path= malloc((numberOfChars + 5) * sizeof(jchar));
	//path= malloc((numberOfChars + 4) * sizeof(jchar));

	// get the path characters from the vm, copy them, and release them
	temp= (*env)->GetStringChars(env, lpPathName, JNI_FALSE);
	memcpy(path + 4, temp, numberOfChars * sizeof(jchar));
	(*env)->ReleaseStringChars(env, lpPathName, temp);

	// prefix the path to enable long filenames, and null terminate it
	path[0] = L'\\';
	path[1] = L'\\';
	path[2] = L'?';
	path[3] = L'\\';
	path[(numberOfChars + 4)] = L'\0';

	// make the request and free the memory
	//printf("%S\n", path);
	result = (jlong) FindFirstChangeNotificationW(path, bWatchSubtree, dwNotifyFilter);
	free(path);
	
	return result;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    FindFirstChangeNotificationA
 * Signature: ([BZI)J
 */
JNIEXPORT jlong JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_FindFirstChangeNotificationA
(JNIEnv * env, jclass this, jbyteArray lpPathName, jboolean bWatchSubtree, jint dwNotifyFilter) {
	jlong result;
	jsize numberOfChars;
	jbyte *path, *temp;
	
	// create a new byte array to hold the null terminated path
	numberOfChars = (*env)->GetArrayLength(env, lpPathName);
	path = malloc((numberOfChars + 1) * sizeof(jbyte));

	// get the path bytes from the vm, copy them, and release them
	temp = (*env)->GetByteArrayElements(env, lpPathName, 0);
	memcpy(path, temp, numberOfChars * sizeof(jbyte));
	(*env)->ReleaseByteArrayElements(env, lpPathName, temp, 0);

	// null terminate the path, make the request, and release the path memory
	path[numberOfChars] = '\0';
	result = (jlong) FindFirstChangeNotificationA(path, bWatchSubtree, dwNotifyFilter);
	free(path);

	return result;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    FindCloseChangeNotification
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_FindCloseChangeNotification
(JNIEnv *env, jclass this, jlong hChangeHandle){
	return (jboolean) FindCloseChangeNotification((HANDLE) hChangeHandle);
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    FindNextChangeNotification
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_FindNextChangeNotification
(JNIEnv *env, jclass this, jlong hChangeHandle){
	return (jboolean) FindNextChangeNotification((HANDLE) hChangeHandle);
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    WaitForMultipleObjects
 * Signature: (I[JZI)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_WaitForMultipleObjects
(JNIEnv *env, jclass this, jint nCount, jlongArray lpHandles, jboolean bWaitAll, jint dwMilliseconds) {
	int i;
	jint result;
	HANDLE handles[MAXIMUM_WAIT_OBJECTS];
	jlong *handlePointers = (*env)->GetLongArrayElements(env, lpHandles, 0);

	for (i = 0; i < nCount; i++) {
		handles[i] = (HANDLE) handlePointers[i];
	}

	result = WaitForMultipleObjects(nCount, handles, bWaitAll, dwMilliseconds);
	(*env)->ReleaseLongArrayElements(env, lpHandles, handlePointers, 0);

	return result;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    IsUnicode
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_IsUnicode
  (JNIEnv *env, jclass this) {
  	OSVERSIONINFO osvi;
  	memset(&osvi, 0, sizeof(OSVERSIONINFO));
  	osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFO);
  	if (! GetVersionEx (&osvi) ) 
    	return JNI_FALSE;
    if (osvi.dwMajorVersion >= 5)
    	return JNI_TRUE;
    return JNI_FALSE;
}
  
/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    GetLastError
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_GetLastError
(JNIEnv *env, jclass this){
	return GetLastError();
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    FILE_NOTIFY_CHANGE_LAST_WRITE
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_FILE_1NOTIFY_1CHANGE_1LAST_1WRITE
(JNIEnv *env, jclass this) {
	return FILE_NOTIFY_CHANGE_LAST_WRITE;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    FILE_NOTIFY_CHANGE_DIR_NAME
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_FILE_1NOTIFY_1CHANGE_1DIR_1NAME
(JNIEnv *env, jclass this) {
	return FILE_NOTIFY_CHANGE_DIR_NAME;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    FILE_NOTIFY_CHANGE_ATTRIBUTES
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_FILE_1NOTIFY_1CHANGE_1ATTRIBUTES
(JNIEnv *env, jclass this) {
	return FILE_NOTIFY_CHANGE_ATTRIBUTES;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    FILE_NOTIFY_CHANGE_SIZE
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_FILE_1NOTIFY_1CHANGE_1SIZE
(JNIEnv *env, jclass this) {
	return FILE_NOTIFY_CHANGE_SIZE;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    FILE_NOTIFY_CHANGE_FILE_NAME
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_FILE_1NOTIFY_1CHANGE_1FILE_1NAME
(JNIEnv *env, jclass this) {
	return FILE_NOTIFY_CHANGE_FILE_NAME;
}


/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    FILE_NOTIFY_CHANGE_SECURITY
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_FILE_1NOTIFY_1CHANGE_1SECURITY
(JNIEnv *env, jclass this) {
	return FILE_NOTIFY_CHANGE_SECURITY;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    MAXIMUM_WAIT_OBJECTS
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_MAXIMUM_1WAIT_1OBJECTS
(JNIEnv *env, jclass this) {
	return MAXIMUM_WAIT_OBJECTS;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    MAX_PATH
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_MAX_1PATH
(JNIEnv *env, jclass this) {
	return MAX_PATH;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    INFINITE
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_INFINITE
(JNIEnv *env, jclass this) {
	return INFINITE;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    WAIT_OBJECT_0
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_WAIT_1OBJECT_10
(JNIEnv *env, jclass this) {
	return WAIT_OBJECT_0;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    WAIT_ABANDONED_0
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_WAIT_1ABANDONED_10
(JNIEnv *env, jclass this) {
	return WAIT_ABANDONED_0;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    WAIT_FAILED
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_WAIT_1FAILED
(JNIEnv *env, jclass this) {
	return WAIT_FAILED;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    WAIT_TIMEOUT
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_WAIT_1TIMEOUT
(JNIEnv *env, jclass this) {
	return WAIT_TIMEOUT;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    ERROR_INVALID_HANDLE
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_ERROR_1INVALID_1HANDLE
(JNIEnv *env, jclass this) {
	return ERROR_INVALID_HANDLE;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    ERROR_SUCCESS
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_ERROR_1SUCCESS
(JNIEnv *env, jclass this) {
	return ERROR_SUCCESS;
}

/*
 * Class:     org_eclipse_core_internal_resources_refresh_win32_Win32Natives
 * Method:    INVALID_HANDLE_VALUE
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_eclipse_core_internal_resources_refresh_win32_Win32Natives_INVALID_1HANDLE_1VALUE
(JNIEnv * env, jclass this) {
	return (jlong)INVALID_HANDLE_VALUE;
}
