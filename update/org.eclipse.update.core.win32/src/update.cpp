/**********************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - Initial API and implementation
 *    Morten Tokle (tokle@broadpark.no) - fix for disk size under Windows 2003 bug #142815
 **********************************************************************/

# include <update.h>
# include <windows.h>
# include <winioctl.h> // IOCTL codes: MediaType

// Windows Version
int WIN98 = 0;
int WINNT = 1;
int WINME = 2;
int WIN2000 = 3;
int WINXP = 4;
int WIN2003 = 5;

// set to 1 for DEBUG
int DEBUG = 0;

// set to 0 to run on Windows 95 *Unsupported*
int NOWIN95 = 1;


// GLOBAL METHODS
// ---------------

/*
 *
 */
jstring WindowsTojstring( JNIEnv* jnienv, char* buf )
{
  jstring rtn = 0;
  wchar_t* buffer = 0;
  int bufferLen = strlen(buf);  
  if( bufferLen == 0 ){
    rtn = jnienv ->NewStringUTF(buf);
	if (DEBUG)
		printf("WindowsToJString Buffer is empty\n");    
  } else {
    int length = MultiByteToWideChar( CP_ACP, 0, (LPCSTR)buf, bufferLen, NULL, 0 );
    buffer = (wchar_t*)malloc( length*2 + 1 );
    if(int err=MultiByteToWideChar( CP_ACP, 0, (LPCSTR)buf, bufferLen, (LPWSTR)buffer, length ) >0 ){
      rtn = jnienv->NewString((jchar*)buffer, length );
    } else {
		if (DEBUG)
			printf("MultiByteToWideChar %i\n",err);    
    }
  }
  if( buffer )
   free( buffer );
  return rtn;
}



/*
 * calls GetVolumeInformation to retrive the label of the volume
 * Returns NULL if an error occurs
 * @param driveLetter path to the drive "c:\\"
 * @prama jnienv JNIEnvironment
 */
jstring getLabel(TCHAR driveLetter[],JNIEnv * jnienv){

	jstring result = NULL;
	TCHAR buf[128];	
	
	// always return null as UNICODE is not implemented
	// how can we get the label of a volume as UNICODE char ?
	return result;
	
	int err = GetVolumeInformation(
		driveLetter,
		buf,
		sizeof(buf) - 1,
		NULL,
		NULL,
		NULL,
		NULL,
		0);
	if (err){
		result = WindowsTojstring(jnienv, buf);
	} else {
		if (DEBUG)
			printf("Error GetVolumeInformation %i\n",err);				
 	}
	return result;
}

/*
 * returns the Version of Windows
 * int 0 WIN98;
 * int 1 WINNT;
 * int 2 WINME;
 * int 3 WIN2000;
 * int 4 WINXP;
 * int 5 WIN2003;
 * returns -1 otherwise
 */
int getWindowsVersion(){
	OSVERSIONINFOEX osvi;
	int UNKNOWN = -1;
	
	ZeroMemory(&osvi, sizeof(OSVERSIONINFOEX));
	osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEX);
	
	if(!(GetVersionEx((OSVERSIONINFO *)&osvi))){
		if (DEBUG)
			printf("UNKNOWN VERSION: Cannot execute GetVersionEx\n");
		// if OSVERSIONEX doesn't work, try OSVERSIONINFO
		osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFO);			 	
		if(!(GetVersionEx((OSVERSIONINFO *)&osvi))){
		if (DEBUG)
			printf("UNKNOWN VERSION: Cannot execute GetVersionEx\n");
			return UNKNOWN;
		}
	}
	
	switch(osvi.dwPlatformId){
		case VER_PLATFORM_WIN32_NT:
			if (DEBUG)
				printf("VERSION NT: Maj %i Min %i\n",osvi.dwMajorVersion,osvi.dwMinorVersion);				 	
			if(osvi.dwMajorVersion<=4)
				return WINNT;
			if(osvi.dwMajorVersion==5){
				if (osvi.dwMinorVersion==0)
					return WIN2000;
				if (osvi.dwMinorVersion==1)
					return WINXP;
				if (osvi.dwMinorVersion==2)
					return WIN2003;
			} else {
				return UNKNOWN;
			};
			break;
		case VER_PLATFORM_WIN32_WINDOWS:
			if (DEBUG)
				printf("VERSION Non NT: Maj %i Min %i\n",osvi.dwMajorVersion,osvi.dwMinorVersion);				 	
			if(osvi.dwMajorVersion==4){
				if (osvi.dwMinorVersion==10)
					return WIN98;
				if (osvi.dwMinorVersion==90)
					return WINME;
			} else {
				return UNKNOWN;
			}
			break;
		default:
			if (DEBUG)
				printf("VERSION UNKNOWN: Maj %i Min %i\n",osvi.dwMajorVersion,osvi.dwMinorVersion);				 	
			return UNKNOWN;
	}
	return UNKNOWN;
} 

/*
 * Returns the type of Removable Drive 
 * Returns 
 * org_eclipse_update_configuration_LocalSystemInfo_VOLUME_FLOPPY_3
 * org_eclipse_update_configuration_LocalSystemInfo_VOLUME_FLOPPY_5
 * org_eclipse_update_configuration_LocalSystemInfo_VOLUME_REMOVABLE
 */
jlong getFloppy(TCHAR driveLetter[]){

	TCHAR floppyPath[8];
	HANDLE handle;
	DISK_GEOMETRY geometry[20];
	DWORD dw;
	jlong UNKNOWN = org_eclipse_update_configuration_LocalSystemInfo_VOLUME_REMOVABLE;

	sprintf(floppyPath, "\\\\.\\%c:", driveLetter[0]);
	if (DEBUG)
		printf("Path %s\n",floppyPath);
	// BUG 25719
    SetErrorMode( SEM_FAILCRITICALERRORS ); 		
	handle=CreateFile(floppyPath,0,FILE_SHARE_READ,NULL,OPEN_ALWAYS,0,NULL);
	if (handle==INVALID_HANDLE_VALUE){
		if (DEBUG)
			printf("Invalid Handle %s\n",floppyPath);
		return UNKNOWN;
	} else {
		if(DeviceIoControl(handle,
			IOCTL_DISK_GET_MEDIA_TYPES,0,0,
			geometry,sizeof(geometry),&dw,0) 
			&& dw>0) {
			switch(geometry[0].MediaType){
			 case F5_160_512:
			 case F5_180_512:
			 case F5_320_512:
			 case F5_320_1024:
			 case F5_360_512:
			 case F5_1Pt2_512:
				if (DEBUG)
					printf("Found 5 1/4 Drive\n");				 	
			 	return org_eclipse_update_configuration_LocalSystemInfo_VOLUME_FLOPPY_5;
			 case F3_720_512:			 				 				 				 				 
			 case F3_1Pt44_512:
			 case F3_2Pt88_512:
			 case F3_20Pt8_512:				 				 				 
				if (DEBUG)
					printf("Found 3 1/2 Drive\n");				 	
			 	return org_eclipse_update_configuration_LocalSystemInfo_VOLUME_FLOPPY_3;
			 default:
			 	return UNKNOWN;
			}
		}
	}
	return UNKNOWN;	
}

/*
 * Returns the UNC name of a remote drive
 * (\\Machine\path\path1\path2$)
 * returns NULL if an error occurs
 */
 jstring getRemoteNetworkName(TCHAR driveLetter[],JNIEnv * jnienv){
 	
 	unsigned long size =256;
 	TCHAR buf[256];	
 	TCHAR drivePath[2];
 	DWORD err;
 	jstring result = NULL;
 	
 	// always return NULL as UNICODE not implemented
	// how can we get the label of a remote network name as UNICODE char ? 	
 	return result;
 	
	sprintf(drivePath, "%c:", driveLetter[0]); 	
 	err = WNetGetConnection(drivePath,buf,&size);
 	
 	if (err==WN_SUCCESS){
		result = WindowsTojstring(jnienv,buf);
	} else {
		if (DEBUG)
			printf("Error WNEtGetConnection %i",err);				
 	}
	return result;
 }


// JNI METHODS
// ---------------

/*
 * Class:     org_eclipse_update_configuration_LocalSystemInfo
 * Method:    nativeGetFreeSpace
 * Signature: (Ljava/io/File;)J
 */
JNIEXPORT jlong JNICALL Java_org_eclipse_update_configuration_LocalSystemInfo_nativeGetFreeSpace(
	JNIEnv * jnienv,
	jclass javaClass,
	jobject file) {

	// to retrive the String
	jclass cls;
	jmethodID id;
	jobject obj;

	// java.io.File.getAbsolutePath()
	const TCHAR * lpDirectoryName;

	// Windows Parameters
	__int64 i64FreeBytesAvailableToCaller;
	__int64 i64TotalNumberOfBytes;
	__int64 i64TotalNumberOfFreeBytes;

	// the result
	jlong result = org_eclipse_update_configuration_LocalSystemInfo_SIZE_UNKNOWN;

	// first, obtain the Path from the java.io.File parameter
	cls = jnienv -> GetObjectClass(file);
	id = jnienv -> GetMethodID(cls, "getAbsolutePath", "()Ljava/lang/String;");
	obj = jnienv -> CallObjectMethod(file, id);
	lpDirectoryName = jnienv -> GetStringUTFChars((jstring) obj, 0);
	if (DEBUG)
		printf("Directory: [%s]\n",lpDirectoryName);

	if (int win = (int)getWindowsVersion()<0 && NOWIN95){
		// windows 95 or other
		if (DEBUG)
			printf("Unsupported Windows: %i\n",win);		
		return result;
	}

	int err = GetDiskFreeSpaceEx(
				lpDirectoryName,
				(PULARGE_INTEGER) & i64FreeBytesAvailableToCaller,
				(PULARGE_INTEGER) & i64TotalNumberOfBytes,
				(PULARGE_INTEGER) & i64TotalNumberOfFreeBytes);
			
	if (err) {
		result = (jlong) i64FreeBytesAvailableToCaller;
	}

	return result;
}

/*
 * Class:     org_eclipse_update_configuration_LocalSystemInfo
 * Method:    nativeGetLabel
 * Signature: (Ljava/io/File;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_eclipse_update_configuration_LocalSystemInfo_nativeGetLabel(
	JNIEnv * jnienv,
	jclass javaClass,
	jobject file) {

	// to retrive the String
	jclass cls;
	jmethodID id;
	jobject obj;

	// java.io.File.getAbsolutePath()
	const TCHAR * lpDirectoryName;

	// obtain the String from the parameter
	cls = jnienv -> GetObjectClass(file);
	id = jnienv -> GetMethodID(cls, "getAbsolutePath", "()Ljava/lang/String;");
	obj = jnienv -> CallObjectMethod(file, id);
	lpDirectoryName = jnienv -> GetStringUTFChars((jstring) obj, 0);
	if (DEBUG)
		printf("Directory: [%s]\n",lpDirectoryName);

	//
	jstring result = NULL;
	int floppy;
	
	if (int win = (int)getWindowsVersion()<0 && NOWIN95){
		// windows 95 or other
		if (DEBUG)
			printf("Unsupported Windows: %i\n",win);		
		return result;
	}
		
	// Make sure we have a String of the Form: <letter>:
	if (':' == lpDirectoryName[1]) {
		TCHAR driveLetter[4]; // i.e. -> C:\\
		memcpy(driveLetter, lpDirectoryName, 2);
		strcpy(driveLetter + 2, "\\");
		switch (GetDriveType(driveLetter)) {
			case DRIVE_REMOTE :
				// check name of machine and path of remote
				if (DEBUG)
					printf("Remote Drive");
				result = getRemoteNetworkName(driveLetter,jnienv);				
				break;
			default :
				if (DEBUG)
					printf("Another Drive at %s", driveLetter);
				result = getLabel(driveLetter,jnienv);
				break;
		} 
	} 

	return result;
}

/*
 * Class:     org_eclipse_update_configuration_LocalSystemInfo
 * Method:    nativeGetType
 * Signature: (Ljava/io/File;)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_update_configuration_LocalSystemInfo_nativeGetType(
	JNIEnv * jnienv,
	jclass javaClass,
	jobject file) {

	// to retrive the String
	jclass cls;
	jmethodID id;
	jobject obj;

	// java.io.File.getAbsolutePath()
	const TCHAR * lpDirectoryName;

	// obtain the String from the parameter
	cls = jnienv -> GetObjectClass(file);
	id = jnienv -> GetMethodID(cls, "getAbsolutePath", "()Ljava/lang/String;");
	obj = jnienv -> CallObjectMethod(file, id);
	lpDirectoryName = jnienv -> GetStringUTFChars((jstring) obj, 0);
	if (DEBUG)
		printf("Directory: [%s]\n",lpDirectoryName);

	int result = org_eclipse_update_configuration_LocalSystemInfo_VOLUME_UNKNOWN;
	if (int win = (int)getWindowsVersion()<0 && NOWIN95){
		// windows 95 or other
		if (DEBUG)
			printf("Unsupported Windows: %i\n",win);		
		return result;
	}
	
	// Make sure we have a String of the Form: <letter>:
	if (':' == lpDirectoryName[1]) {
		TCHAR driveLetter[4]; //C:\\
		memcpy(driveLetter, lpDirectoryName, 2);
		strcpy(driveLetter + 2, "\\");

		switch (GetDriveType(driveLetter)) {
			case DRIVE_REMOVABLE :
				// check if floppy 3.5, floppy 5.25	
				// or other removable device (USB,PCMCIA ...)
				if (DEBUG)
					printf("Removable Device");
				result = getFloppy(driveLetter);					
				break;
			case DRIVE_CDROM :
				result = org_eclipse_update_configuration_LocalSystemInfo_VOLUME_CDROM;
				break;
			case DRIVE_FIXED :
				result = org_eclipse_update_configuration_LocalSystemInfo_VOLUME_FIXED;
				break;
			case DRIVE_REMOTE :
				result = org_eclipse_update_configuration_LocalSystemInfo_VOLUME_REMOTE;
				break;
			case DRIVE_NO_ROOT_DIR :
				result = org_eclipse_update_configuration_LocalSystemInfo_VOLUME_INVALID_PATH;
				break;
			case DRIVE_RAMDISK :
				result = org_eclipse_update_configuration_LocalSystemInfo_VOLUME_RAMDISK;
				break;			
			case DRIVE_UNKNOWN :
			default :
				result = org_eclipse_update_configuration_LocalSystemInfo_VOLUME_UNKNOWN;
				break;
		}
	} else {
		result = org_eclipse_update_configuration_LocalSystemInfo_VOLUME_INVALID_PATH;
	}

	return result;
}

/*
 * Class:     org_eclipse_update_configuration_LocalSystemInfo
 * Method:    nativeListMountPoints
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_eclipse_update_configuration_LocalSystemInfo_nativeListMountPoints(
	JNIEnv * jnienv,
	jclass javaClass) {

	//
	DWORD logDrives;
	UINT drive;
	TCHAR driveName[100];
	jobjectArray returnArray;
	int nDrive = 0;

	// 
	jclass stringClass;
	jobject empty;
	int index = 0;
	jobject str;


	logDrives = GetLogicalDrives();
	for (drive = 0; drive < 32; drive++) {
		if (logDrives & (1 << drive)) {
			nDrive++;
		}
	}

	stringClass = jnienv -> FindClass("java/lang/String");
	empty = jnienv -> NewStringUTF("");
	returnArray = jnienv -> NewObjectArray(nDrive, stringClass, empty);

	if (int win = (int)getWindowsVersion()<0 && NOWIN95){
		// windows 95 or other
		if (DEBUG)
			printf("Unsupported Windows: %i\n",win);	
		return NULL;
	}

	for (drive = 0; drive < 32; drive++) {
		if (logDrives & (1 << drive)) {
			sprintf(driveName, "%c:\\", drive + 'A');
			str = jnienv -> NewStringUTF(driveName);
			jnienv -> SetObjectArrayElement(returnArray, index, str);
			index++;
		}
	}

	return returnArray;
}

