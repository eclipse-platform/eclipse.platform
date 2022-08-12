/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
 *******************************************************************************/

#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>
#include <limits.h>
#include <jni.h>

#if defined MACOSX
#include <CoreServices/CoreServices.h>
#endif

#include "unixfile.h"

/* The lstat stat field st_mode. */
static jfieldID attrs_st_mode;
/* The lstat stat field st_size. */
static jfieldID attrs_st_size;
/* The lstat stat field st_mtime_sec. */
static jfieldID attrs_st_mtime;
/* The lstat stat field st_mtime_nsec divided by 1 000 000. Only filled on Linux based operating systems. */
static jfieldID attrs_st_mtime_msec;
/* Only filled on MACOSX. */
static jfieldID attrs_st_flags;

/*
 * Class:     Java_org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives_initializeStructStatFieldIDs
 * Method:    initializeStructStatFieldIDs
 * Signature: ()V
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives_initializeStructStatFieldIDs
  (JNIEnv *env, jclass clazz)
{
    jclass structStatClass = (*env)->FindClass(env, "org/eclipse/core/internal/filesystem/local/unix/StructStat");

    attrs_st_mode = (*env)->GetFieldID(env, structStatClass, "st_mode", "I");
    attrs_st_size = (*env)->GetFieldID(env, structStatClass, "st_size", "J");
    attrs_st_mtime = (*env)->GetFieldID(env, structStatClass, "st_mtime", "J");

#ifdef MACOSX
    attrs_st_flags = (*env)->GetFieldID(env, structStatClass, "st_flags", "J");
#endif

#ifndef MACOSX
    attrs_st_mtime_msec = (*env)->GetFieldID(env, structStatClass, "st_mtime_msec", "J");
#endif
}

/*
 * Get a null-terminated byte array from a java byte array. The returned bytearray
 * needs to be freed when not used anymore. Use free(result) to do that.
 */
jbyte* getByteArray(JNIEnv *env, jbyteArray target)
{
	unsigned int len;
	jbyte *temp, *result;

	temp = (*env)->GetByteArrayElements(env, target, 0);
	len = (*env)->GetArrayLength(env, target);
	result = malloc((len + 1) * sizeof(jbyte));
	memcpy(result, temp, len * sizeof(jbyte));
	result[len] = '\0';
	(*env)->ReleaseByteArrayElements(env, target, temp, 0);
	return result;
}

/*
 * Fills StructStat object with data from struct stat.
 */
jint convertStatToObject(JNIEnv *env, struct stat info, jobject stat_object)
{
	jclass cls;
	jfieldID fid;
	jboolean readOnly;

	cls = (*env)->GetObjectClass(env, stat_object);
	if (cls == 0) return -1;

	if (attrs_st_mode == 0) return -1;
	(*env)->SetIntField(env, stat_object, attrs_st_mode, info.st_mode);

	if (attrs_st_size == 0) return -1;
	(*env)->SetLongField(env, stat_object, attrs_st_size, info.st_size);

	if (attrs_st_mtime == 0) return -1;
	(*env)->SetLongField(env, stat_object, attrs_st_mtime, info.st_mtime);

#ifndef MACOSX
	if (attrs_st_mtime_msec == 0) return -1;
	(*env)->SetLongField(env, stat_object, attrs_st_mtime_msec, (info.st_mtim.tv_nsec / (1000 * 1000)));
#endif


#ifdef MACOSX
	if (attrs_st_flags == 0) return -1;
	(*env)->SetLongField(env, stat_object, attrs_st_flags, info.st_flags);
#endif

	return 0;
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives
 * Method:    chmod
 * Signature: ([BI)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives_chmod
  (JNIEnv *env, jclass clazz, jbyteArray path, jint mode)
{
	int code;
	char *name;

	name = (char*) getByteArray(env, path);
	code = chmod(name, mode);
	free(name);
	return code;
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives
 * Method:    chflags
 * Signature: ([BI)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives_chflags
  (JNIEnv *env, jclass clazz, jbyteArray path, jint flags)
{
#ifdef MACOSX
	int code;
	char *name;

	name = (char*) getByteArray(env, path);
	code = chflags(name, flags);
	free(name);
	return code;
#else
	return -1;
#endif
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives
 * Method:    stat
 * Signature: ([BLorg/eclipse/core/internal/filesystem/local/unix/StructStat;)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives_stat
  (JNIEnv *env, jclass clazz, jbyteArray path, jobject buf)
{
	jint code;
	char *name;
	struct stat info;

	name = (char*) getByteArray(env, path);
	code = stat(name, &info);
	free(name);
	if (code != -1)
		return convertStatToObject(env, info, buf);
	else
		return code;
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives
 * Method:    lstat
 * Signature: ([BLorg/eclipse/core/internal/filesystem/local/unix/StructStat;)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives_lstat
  (JNIEnv *env, jclass clazz, jbyteArray path, jobject buf)
{
	jint code;
	char *name;
	struct stat info;

	name = (char*) getByteArray(env, path);
	code = lstat(name, &info);
	free(name);
	if (code != -1)
		return convertStatToObject(env, info, buf);
	else
		return code;
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives
 * Method:    readlink
 * Signature: ([B[BJ)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives_readlink
    (JNIEnv *env, jclass clazz, jbyteArray path, jbyteArray buf, jlong bufsiz) {
	jint code;
 	jbyte *name;
 	int len;
	char temp[PATH_MAX+1];
	jstring linkTarget = NULL;

	name = getByteArray(env, path);
  	len = readlink((const char*)name, temp, PATH_MAX);
  	free(name);
	if (len > 0) {
		temp[len] = 0;
		(*env)->SetByteArrayRegion(env, buf, 0, len, (jbyte*) temp);
	}
	else {
		temp[0] = 0;
		(*env)->SetByteArrayRegion(env, buf, 0, 0, (jbyte*) temp);
	}
	return len;
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives
 * Method:    errno
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives_errno
  (JNIEnv *env, jclass clazz)
{
	return errno;
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives
 * Method:    libattr
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives_libattr
  (JNIEnv *env, jclass clazz)
{
#ifdef MACOSX
	return UNICODE_SUPPORTED | CHFLAGS_SUPPORTED;
#else
	return 0;
#endif
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives
 * Method:    tounicode
 * Signature: ([C)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives_tounicode
  (JNIEnv *env, jclass clazz, jcharArray buf)
{
#ifdef MACOSX
	jchar *temp;
	jsize length;
	CFStringRef str_ref;
	CFIndex str_size;
	jbyte *unicode_bytes;
	jbyteArray ret;

	temp = (*env)->GetCharArrayElements(env, buf, 0);
	length = (*env)->GetArrayLength(env, buf);
	str_ref = CFStringCreateWithCharacters(kCFAllocatorDefault, temp, length);
	str_size = CFStringGetMaximumSizeForEncoding(length, kCFStringEncodingUTF8) + 1;
	unicode_bytes = (jbyte*) calloc(str_size, sizeof(jbyte));
	CFStringGetCString(str_ref, (char*) unicode_bytes, str_size, kCFStringEncodingUTF8);
	ret = (*env)->NewByteArray(env, str_size);
	if (ret == NULL)
		return NULL;
	(*env)->SetByteArrayRegion(env, ret, 0, str_size, unicode_bytes);
	CFRelease(str_ref);
	(*env)->ReleaseCharArrayElements(env, buf, temp, 0);
	free(unicode_bytes);
	return ret;
#else
	return NULL;
#endif
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives
 * Method:    getflag
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_unix_UnixFileNatives_getflag
  (JNIEnv *env, jclass clazz, jbyteArray buf)
{
	char *flag;
	jint ret = -1;

	flag = (char*) getByteArray(env, buf);
	if (strcmp(flag, "PATH_MAX") == 0)
		ret = PATH_MAX;
	else if (strcmp(flag, "S_IFMT") == 0)
		ret = S_IFMT;
	else if (strcmp(flag, "S_IFLNK") == 0)
		ret = S_IFLNK;
	else if (strcmp(flag, "S_IFDIR") == 0)
		ret = S_IFDIR;
	else if (strcmp(flag, "S_IRUSR") == 0)
		ret = S_IRUSR;
	else if (strcmp(flag, "S_IWUSR") == 0)
		ret = S_IWUSR;
	else if (strcmp(flag, "S_IXUSR") == 0)
		ret = S_IXUSR;
	else if (strcmp(flag, "S_IRGRP") == 0)
		ret = S_IRGRP;
	else if (strcmp(flag, "S_IWGRP") == 0)
		ret = S_IWGRP;
	else if (strcmp(flag, "S_IXGRP") == 0)
		ret = S_IXGRP;
	else if (strcmp(flag, "S_IROTH") == 0)
		ret = S_IROTH;
	else if (strcmp(flag, "S_IWOTH") == 0)
		ret = S_IWOTH;
	else if (strcmp(flag, "S_IXOTH") == 0)
		ret = S_IXOTH;
#ifdef MACOSX
	else if (strcmp(flag, "UF_IMMUTABLE") == 0)
		ret = UF_IMMUTABLE;
	else if (strcmp(flag, "SF_IMMUTABLE") == 0)
		ret = SF_IMMUTABLE;
#endif
	free(flag);
	return ret;
}
