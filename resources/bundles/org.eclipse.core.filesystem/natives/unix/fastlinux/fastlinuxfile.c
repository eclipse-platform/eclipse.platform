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
#include <fcntl.h>
#include <dirent.h>

#include "fastlinuxfile.h"

/* The lstat stat field st_mode. */
static jfieldID attrs_st_mode;
/* The lstat stat field st_size. */
static jfieldID attrs_st_size;
/* The lstat stat field st_mtime_sec. */
static jfieldID attrs_st_mtime;
/* The lstat stat field st_mtime_nsec divided by 1 000 000. */
static jfieldID attrs_st_mtime_msec;

static jfieldID errno_fieldID;

static jfieldID nameFieldId;

static jfieldID linkFieldId;

static const jsize INITIAL_LIST_ARRAY_SIZE = 100;

/*
 * Class:     Java_org_eclipse_core_internal_filesystem_local_linux_LinuxFileNatives_initializeLinuxStructStatFieldIDs
 * Method:    initializeLinuxStructStatFieldIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_eclipse_core_internal_filesystem_local_linux_LinuxFileNatives_initializeLinuxStructStatFieldIDs
  (JNIEnv *env, jclass clazz)
{
	jclass LinuxStructStatClass = (*env)->FindClass(env, "org/eclipse/core/internal/filesystem/local/linux/LinuxStructStat");
	if (LinuxStructStatClass == NULL) {
		return; /* exception already pending */
	}
	attrs_st_mode = (*env)->GetFieldID(env, LinuxStructStatClass, "st_mode", "I");
	attrs_st_size = (*env)->GetFieldID(env, LinuxStructStatClass, "st_size", "J");
	attrs_st_mtime = (*env)->GetFieldID(env, LinuxStructStatClass, "st_mtime", "J");
	errno_fieldID = (*env)->GetFieldID(env, LinuxStructStatClass, "errno", "I");
	nameFieldId = (*env)->GetFieldID(env, LinuxStructStatClass, "name", "[B");
	linkFieldId = (*env)->GetFieldID(env, LinuxStructStatClass, "linkFile", "[B");
	attrs_st_mtime_msec = (*env)->GetFieldID(env, LinuxStructStatClass, "st_mtime_msec", "J");
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
	if (result != NULL) {
		memcpy(result, temp, len * sizeof(jbyte));
		result[len] = '\0';
	}
	(*env)->ReleaseByteArrayElements(env, target, temp, 0);
	return result;
}

/*
 * Copy object array contents using java.lang.System.arraycopy.
 */
jint copyObjectArray(JNIEnv *env, jobject source, jobject target, jsize length)
{
	jclass systemClass;
	jmethodID arraycopyMethod;

	systemClass = (*env)->FindClass(env, "java/lang/System");
	if (systemClass == NULL) {
		return -1;
	}
	arraycopyMethod = (*env)->GetStaticMethodID(env, systemClass, "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
	if (arraycopyMethod == NULL) {
		(*env)->DeleteLocalRef(env, systemClass);
		return -1;
	}

	(*env)->CallStaticVoidMethod(env, systemClass, arraycopyMethod, source, 0, target, 0, length);
	(*env)->DeleteLocalRef(env, systemClass);
	if ((*env)->ExceptionCheck(env)){
		return -1;
	}

	return 0;
}

/*
 * Fills LinuxStructStat object with data from struct stat.
 */
jint convertStatToObject(JNIEnv *env, struct stat info, int errnoValue, jobject stat_object)
{
	if (attrs_st_mode == 0) return -1;
	(*env)->SetIntField(env, stat_object, attrs_st_mode, info.st_mode);

	if (attrs_st_size == 0) return -1;
	(*env)->SetLongField(env, stat_object, attrs_st_size, info.st_size);

	if (attrs_st_mtime == 0) return -1;
	(*env)->SetLongField(env, stat_object, attrs_st_mtime, info.st_mtime);

	if (errno_fieldID == 0) return -1;
	(*env)->SetIntField(env, stat_object, errno_fieldID, errnoValue);

	if (attrs_st_mtime_msec == 0) return -1;
	(*env)->SetLongField(env, stat_object, attrs_st_mtime_msec, (info.st_mtim.tv_nsec / (1000 * 1000)));

	return 0;
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_linux_LinuxFileNatives
 * Method:    chmod
 * Signature: ([BI)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_linux_LinuxFileNatives_chmod
  (JNIEnv *env, jclass clazz, jbyteArray path, jint mode)
{
	int code;
	char *name;

	name = (char*) getByteArray(env, path);
	if (name == NULL) {
		return -1;
	}
	code = chmod(name, mode);
	free(name);
	return code;
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_linux_LinuxFileNatives
 * Method:    stat
 * Signature: ([BLorg/eclipse/core/internal/filesystem/local/linux/LinuxStructStat;)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_linux_LinuxFileNatives_stat
  (JNIEnv *env, jclass clazz, jbyteArray path, jobject buf)
{
	jint code;
	char *name;
	struct stat info = {0};

	name = (char*) getByteArray(env, path);
	if (name == NULL) {
		return -1;
	}
	code = fstatat(AT_FDCWD, name, &info, 0);
	free(name);
	return convertStatToObject(env, info, code == 0 ? 0 : errno, buf);
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_linux_LinuxFileNatives
 * Method:    lstat
 * Signature: ([BLorg/eclipse/core/internal/filesystem/local/linux/LinuxStructStat;)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_linux_LinuxFileNatives_lstat
  (JNIEnv *env, jclass clazz, jbyteArray path, jobject buf)
{
	jint code;
	char *name;
	struct stat info = {0};

	name = (char*) getByteArray(env, path);
	if (name == NULL) {
		return -1;
	}
	code = fstatat(AT_FDCWD, name, &info, AT_SYMLINK_NOFOLLOW);
	free(name);
	return convertStatToObject(env, info, code == 0 ? 0 : errno, buf);
}

/*
 * Class:     org_eclipse_core_internal_filesystem_local_linux_LinuxFileNatives
 * Method:    readlink
 * Signature: ([B[BJ)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_linux_LinuxFileNatives_readlink
    (JNIEnv *env, jclass clazz, jbyteArray path, jbyteArray buf, jlong bufsiz) {
	jbyte *name;
	int len;
	char temp[PATH_MAX+1];

	name = getByteArray(env, path);
	if (name == NULL) {
		return -1;
	}
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
 * Class:     org_eclipse_core_internal_filesystem_local_linux_LinuxFileNatives
 * Method:    getflag
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_filesystem_local_linux_LinuxFileNatives_getflag
  (JNIEnv *env, jclass clazz, jbyteArray buf)
{
	char *flag;
	jint ret = -1;

	flag = (char*) getByteArray(env, buf);
	if (flag == NULL) {
		return -1;
	}
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
	free(flag);
	return ret;
}

JNIEXPORT jobjectArray JNICALL Java_org_eclipse_core_internal_filesystem_local_linux_LinuxFileNatives_listDir
  (JNIEnv *env, jclass clazz, jbyteArray path)
{
	char *name;
	DIR *dir = NULL;
	jsize arrayLength;
	int count = 0;
	int i;
	struct dirent *entry;
	jobjectArray namesArray;
	jobjectArray resultStatsArray = NULL;
	jobjectArray result = NULL;
	jclass byteArrayClass;

	byteArrayClass = (*env)->FindClass(env, "[B");
	if (byteArrayClass == NULL) {
		return NULL;
	}
	namesArray = (*env)->NewObjectArray(env, INITIAL_LIST_ARRAY_SIZE, byteArrayClass, NULL);
	if (namesArray == NULL) {
		return NULL;
	}
	arrayLength = INITIAL_LIST_ARRAY_SIZE;

	name = (char*) getByteArray(env, path);
	if (name == NULL) {
		goto cleanup;
	}
	dir = opendir(name);
	free(name);
	if (dir == NULL) {
		goto cleanup;
	}
	while ((entry = readdir(dir)) != NULL) {
		jbyteArray nameBytes;
		jsize nameLen;
		jobjectArray grownArray;

		/* Skip . and .. */
		if (entry->d_name[0] == '.' &&
			(entry->d_name[1] == '\0' ||
			(entry->d_name[1] == '.' && entry->d_name[2] == '\0'))) {
			continue;
		}

		if (count >= arrayLength) {
			jsize newLength = arrayLength > 0 ? arrayLength * 2 : 1;

			if (newLength < arrayLength) {
				goto cleanup;
			}

			grownArray = (*env)->NewObjectArray(env, newLength, byteArrayClass, NULL);
			if (grownArray == NULL) {
				goto cleanup;
			}

			if (copyObjectArray(env, namesArray, grownArray, arrayLength) != 0) {
				(*env)->DeleteLocalRef(env, grownArray);
				goto cleanup;
			}

			(*env)->DeleteLocalRef(env, namesArray);
			namesArray = grownArray;
			arrayLength = newLength;
		}

		/* Add the directory entry name as raw bytes to the names array */
		nameLen = (jsize)strlen(entry->d_name);
		nameBytes = (*env)->NewByteArray(env, nameLen);
		if (nameBytes == NULL) {
			goto cleanup;
		}
		(*env)->SetByteArrayRegion(env, nameBytes, 0, nameLen, (const jbyte*)entry->d_name);
		(*env)->SetObjectArrayElement(env, namesArray, count, nameBytes);
		(*env)->DeleteLocalRef(env, nameBytes);
		if ((*env)->ExceptionCheck(env)) {
			goto cleanup;
		}

		count++;
	}

	resultStatsArray = (*env)->NewObjectArray(env, count, byteArrayClass, NULL);
	if (resultStatsArray == NULL) {
		goto cleanup;
	}

	for (i = 0; i < count; i++) {
		jobject existingName = (*env)->GetObjectArrayElement(env, namesArray, i);
		if ((*env)->ExceptionCheck(env)) {
			goto cleanup;
		}
		(*env)->SetObjectArrayElement(env, resultStatsArray, i, existingName);
		(*env)->DeleteLocalRef(env, existingName);
		if ((*env)->ExceptionCheck(env)) {
			goto cleanup;
		}
	}

	result = resultStatsArray;
	resultStatsArray = NULL;

cleanup:
	if (dir != NULL) {
		closedir(dir);
	}
	if (namesArray != NULL) {
		(*env)->DeleteLocalRef(env, namesArray);
	}
	if (resultStatsArray != NULL) {
		(*env)->DeleteLocalRef(env, resultStatsArray);
	}
	return result;
}

JNIEXPORT jobjectArray JNICALL Java_org_eclipse_core_internal_filesystem_local_linux_LinuxFileNatives_listDirAndGetFileInfos
	(JNIEnv *env, jclass clazz, jbyteArray path)
{
	char *name;
	DIR *dir = NULL;
	int directoryFd;
	jsize arrayLength;
	int count = 0;
	int i;
	struct dirent *entry;
	jclass LinuxStructStatClass;
	jmethodID LinuxStructStatCtor;
	jobjectArray statArray = NULL;
	jobjectArray resultStatsArray = NULL;
	jobjectArray result = NULL;

	LinuxStructStatClass = (*env)->FindClass(env, "org/eclipse/core/internal/filesystem/local/linux/LinuxStructStat");
	if (LinuxStructStatClass == NULL) {
		return NULL;
	}

	statArray = (*env)->NewObjectArray(env, INITIAL_LIST_ARRAY_SIZE, LinuxStructStatClass, NULL);
	if (statArray == NULL) {
		return NULL;
	}
	arrayLength = INITIAL_LIST_ARRAY_SIZE;

	LinuxStructStatCtor = (*env)->GetMethodID(env, LinuxStructStatClass, "<init>", "()V");
	if (LinuxStructStatCtor == NULL) {
		goto cleanup;
	}
	name = (char*) getByteArray(env, path);
	if (name == NULL) {
		goto cleanup;
	}
	dir = opendir(name);
	free(name);
	if (dir == NULL) {
		goto cleanup;
	}
	directoryFd = dirfd(dir);
	if (directoryFd == -1) {
		goto cleanup;
	}
	while ((entry = readdir(dir)) != NULL) {
		struct stat st;
		jobject statObject = NULL;
		int statErrno = 0;

		/* Skip . and .. */
		if (entry->d_name[0] == '.' &&
			(entry->d_name[1] == '\0' ||
			 (entry->d_name[1] == '.' && entry->d_name[2] == '\0'))) {
			continue;
		}

		if (count >= arrayLength) {
			jsize newLength = arrayLength > 0 ? arrayLength * 2 : 1;
			jobjectArray grownStatArray;

			if (newLength < arrayLength) {
				goto cleanup;
			}

			grownStatArray = (*env)->NewObjectArray(env, newLength, LinuxStructStatClass, NULL);
			if (grownStatArray == NULL) {
				goto cleanup;
			}

			if (copyObjectArray(env, statArray, grownStatArray, arrayLength) != 0) {
				(*env)->DeleteLocalRef(env, grownStatArray);
				goto cleanup;
			}

			(*env)->DeleteLocalRef(env, statArray);

			statArray = grownStatArray;
			arrayLength = newLength;
		}

		statObject = (*env)->NewObject(env, LinuxStructStatClass, LinuxStructStatCtor);
		if (statObject == NULL) {
			goto cleanup;
		}

		/* Collect file stats. Keep processing even if stat/readlink fails. */
		if (fstatat(directoryFd, entry->d_name, &st, AT_SYMLINK_NOFOLLOW) != 0) {
			memset(&st, 0, sizeof(st));
			statErrno = errno;
		} else if (S_ISLNK(st.st_mode)) {
			char linkPath[PATH_MAX + 1];
			jbyteArray linkBytes;
			jsize linkLen;
			ssize_t linkPathLen;

			/* Follow symlink and update stat for further processing. */
			if (fstatat(directoryFd, entry->d_name, &st, 0) != 0) {
				memset(&st, 0, sizeof(st));
				statErrno = errno;
			}

			/* Read link target if possible. */
			linkPathLen = readlinkat(directoryFd, entry->d_name, linkPath, PATH_MAX);
			if (linkPathLen >= 0) {
				linkPath[linkPathLen] = '\0';
			}

			linkLen = (linkPathLen >= 0) ? (jsize)linkPathLen : 0;
			linkBytes = (*env)->NewByteArray(env, linkLen);
			if (linkBytes == NULL) {
				(*env)->DeleteLocalRef(env, statObject);
				goto cleanup;
			}
			if (linkLen > 0) {
				(*env)->SetByteArrayRegion(env, linkBytes, 0, linkLen, (const jbyte*)linkPath);
			}
			(*env)->SetObjectField(env, statObject, linkFieldId, linkBytes);
			(*env)->DeleteLocalRef(env, linkBytes);
			if ((*env)->ExceptionCheck(env)) {
				(*env)->DeleteLocalRef(env, statObject);
				goto cleanup;
			}
		}

		if (convertStatToObject(env, st, statErrno, statObject) != 0) {
			(*env)->DeleteLocalRef(env, statObject);
			goto cleanup;
		}

		{
			jbyteArray nameBytes;
			jsize nameLen = (jsize)strlen(entry->d_name);
			nameBytes = (*env)->NewByteArray(env, nameLen);
			if (nameBytes == NULL) {
				(*env)->DeleteLocalRef(env, statObject);
				goto cleanup;
			}
			(*env)->SetByteArrayRegion(env, nameBytes, 0, nameLen, (const jbyte*)entry->d_name);
			(*env)->SetObjectField(env, statObject, nameFieldId, nameBytes);
			(*env)->DeleteLocalRef(env, nameBytes);
			if ((*env)->ExceptionCheck(env)) {
				(*env)->DeleteLocalRef(env, statObject);
				goto cleanup;
			}
		}

		(*env)->SetObjectArrayElement(env, statArray, count, statObject);
		if ((*env)->ExceptionCheck(env)) {
			(*env)->DeleteLocalRef(env, statObject);
			goto cleanup;
		}
		(*env)->DeleteLocalRef(env, statObject);

		count++;
	}

	resultStatsArray = (*env)->NewObjectArray(env, count, LinuxStructStatClass, NULL);
	if (resultStatsArray == NULL) {
		goto cleanup;
	}

	for (i = 0; i < count; i++) {
		jobject existingStat = (*env)->GetObjectArrayElement(env, statArray, i);
		if ((*env)->ExceptionCheck(env)) {
			if (existingStat != NULL) {
				(*env)->DeleteLocalRef(env, existingStat);
			}
			goto cleanup;
		}

		(*env)->SetObjectArrayElement(env, resultStatsArray, i, existingStat);
		(*env)->DeleteLocalRef(env, existingStat);
		if ((*env)->ExceptionCheck(env)) {
			goto cleanup;
		}
	}

	result = resultStatsArray;
	resultStatsArray = NULL;

cleanup:
	if (dir != NULL) {
		closedir(dir);
	}
	if (statArray != NULL) {
		(*env)->DeleteLocalRef(env, statArray);
	}
	if (resultStatsArray != NULL) {
		(*env)->DeleteLocalRef(env, resultStatsArray);
	}
	return result;
}