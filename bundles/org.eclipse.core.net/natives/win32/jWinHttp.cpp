/*******************************************************************************
 * Copyright (c) 2008 compeople AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/


#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


#define WIN32_LEAN_AND_MEAN	

#include <stdio.h>
#include <iostream>
#include <windows.h>
#include <winhttp.h>
#include <objbase.h>

#include "jWinHttp.h"

using namespace std;

// Remember the GetLastError() after a failed WinHttp... call.
static int lastError;

BOOL APIENTRY DllMain( HANDLE hModule, 
                       DWORD  ul_reason_for_call, 
                       LPVOID lpReserved
					 ) {
    switch (ul_reason_for_call) {
		case DLL_PROCESS_ATTACH:
			#ifdef _DEBUG
				cout << "DLL_PROCESS_ATTACH - jWinHttp" << endl;
			#endif
			break;
		case DLL_THREAD_ATTACH:
			#ifdef _DEBUG
				cout << "DLL_THREAD_ATTACH - jWinHttp" << endl;
			#endif
			break;
		case DLL_THREAD_DETACH:
			#ifdef _DEBUG
				cout << "DLL_THREAD_DETACH - jWinHttp" << endl;
			#endif
			break;
		case DLL_PROCESS_DETACH:
			#ifdef _DEBUG
				cout << "DLL_PROCESS_DETACH - jWinHttp" << endl;
			#endif
			break;
    }
    return TRUE;
}


/*
 * Helper for some ugly things!
 * ............................
 */

const jchar * getStringChars( JNIEnv * env, jstring jString ) {
	if ( jString != NULL ) {
		return env->GetStringChars( jString, NULL );
	} else {
		return NULL;
	}
}

void releaseStringChars( JNIEnv * env, jstring jString, const jchar * jCharString ) {
	if ( jString != NULL ) {
		env->ReleaseStringChars( jString, jCharString );
	}
}

jobject newString( JNIEnv * env, LPWSTR string ) {
	return env->NewString( (const jchar *)string, lstrlenW( string ) );
}

void setStringField( JNIEnv * env, jclass jClass, jobject jObject, const char * field, LPWSTR value ) {
	if ( value != NULL ) {
		jfieldID jFieldId = env->GetFieldID( jClass, field, "Ljava/lang/String;" );
		env->SetObjectField( jObject, jFieldId, newString( env, value ) );
		GlobalFree( value );
	}
}

jstring getStringField( JNIEnv * env, jclass jClass, jobject jObject, const char * field ) {
	jfieldID jFieldId = env->GetFieldID( jClass, field, "Ljava/lang/String;" );
	return (jstring)env->GetObjectField( jObject, jFieldId );
}

void setBooleanField( JNIEnv * env, jclass jClass, jobject jObject, const char * field, BOOL value ) {
	jfieldID jFieldId = env->GetFieldID( jClass, field, "Z" );
	env->SetBooleanField( jObject, jFieldId, value );
}

jboolean getBooleanField( JNIEnv * env, jclass jClass, jobject jObject, const char * field ) {
	jfieldID jFieldId = env->GetFieldID( jClass, field, "Z" );
	return env->GetBooleanField( jObject, jFieldId );
}

void setIntField( JNIEnv * env, jclass jClass, jobject jObject, const char * field, jint value ) {
	jfieldID jFieldId = env->GetFieldID( jClass, field, "I" );
	env->SetIntField( jObject, jFieldId, value );
}

jint getIntField( JNIEnv * env, jclass jClass, jobject jObject, const char * field ) {
	jfieldID jFieldId = env->GetFieldID( jClass, field, "I" );
	return env->GetIntField( jObject, jFieldId );
}

#ifdef _DEBUG
	LPCWSTR null( const LPCWSTR string ) {
		if ( string == NULL ) {
			return (LPCWSTR) L"null";
		} else {
			return string;
		}
	}

	LPWSTR null( const LPWSTR string ) {
		if ( string == NULL ) {
			return (LPWSTR) L"null";
		} else {
			return string;
		}
	}
#endif

/*
 * The real ugly work goes on here!
 * ................................
 */


/*
 * Class:     org_eclipse_core_internal_net_proxy_win32_winhttp_WinHttp
 * Method:    open
 * Signature: (Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_net_proxy_win32_winhttp_WinHttp_open
  (JNIEnv * env, jclass jClass, jstring jUserAgent, jint jAccessType, jstring jProxyName, jstring jProxyBypass, jint jFlags ) {

	#ifdef _DEBUG
		cout << "WinHttp_open - entered" << endl;
	#endif

	const jchar * userAgent = (const jchar *) L"jWinHttp Java Wrapper";
	const jchar * proxyName = NULL;
	const jchar * proxyBypass = NULL;

	userAgent = getStringChars( env, jUserAgent );
	proxyName = getStringChars( env, jProxyName );
	proxyBypass = getStringChars( env, jProxyBypass );

	CoInitialize( NULL );    // --> http://support.microsoft.com/?kbid=834742

	int hInternet = (int) WinHttpOpen( (LPCWSTR)userAgent, jAccessType, (LPCWSTR)proxyName, (LPCWSTR)proxyBypass, jFlags );

	if ( hInternet == NULL ) {
		lastError = GetLastError();
		#ifdef _DEBUG
			cout << "WinHttpOpen() failed with " << lastError << endl; 
		#endif
	} else {
		lastError = 0;
	}


	#ifdef _DEBUG
		cout << "WinHttpOpen() returned: " << hInternet << endl;
	#endif
	

	releaseStringChars( env, jUserAgent, userAgent );
	releaseStringChars( env, jProxyName, proxyName );
	releaseStringChars( env, jProxyBypass, proxyBypass );
	
	#ifdef _DEBUG
		cout << "WinHttp_open - exit" << endl;
	#endif

	return hInternet;
}

/*
 * Class:     org_eclipse_core_internal_net_proxy_win32_winhttp_WinHttp
 * Method:    closeHandle
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_core_internal_net_proxy_win32_winhttp_WinHttp_closeHandle
  (JNIEnv * env, jclass jClass, jint jInternet) {

	#ifdef _DEBUG
		cout << "WinHttp_closeHandle - entered" << endl;
	#endif

	BOOL ok = WinHttpCloseHandle( (void *) jInternet );

	if ( ! ok ) {
		lastError = GetLastError();
		#ifdef _DEBUG
			cout << "WinHttpClose() failed with " << lastError << endl; 
		#endif
	} else {
		lastError = 0;
	}

	CoUninitialize();

	#ifdef _DEBUG
		cout << "WinHttp_closeHandle - exit" << endl;
	#endif

	return ok;
}

/*
 * Class:     org_eclipse_core_internal_net_proxy_win32_winhttp_WinHttp
 * Method:    getIEProxyConfigForCurrentUser
 * Signature: (Lorg/eclipse/core/internal/net/proxy/win32/winhttp/WinHttpCurrentUserIEProxyConfig;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_core_internal_net_proxy_win32_winhttp_WinHttp_getIEProxyConfigForCurrentUser
  (JNIEnv * env, jclass jClass, jobject jWinHttpCurrentUserIEProxyConfig)  {

	#ifdef _DEBUG
		cout << "WinHttp_getIEProxyConfigForCurrentUser - entered" << endl;
	#endif

	WINHTTP_CURRENT_USER_IE_PROXY_CONFIG proxyConfig;
	ZeroMemory( &proxyConfig, sizeof( proxyConfig ) );

	BOOL ok = WinHttpGetIEProxyConfigForCurrentUser( &proxyConfig );

	if ( ! ok ) {
		lastError = GetLastError();
		#ifdef _DEBUG
			cout << "HttpGetIEProxyConfigForCurrentUser() failed with " << lastError << endl; 
		#endif
	}

	if ( ok ) {

		#ifdef _DEBUG
			cout << "proxyConfig.fAutoDetect: " << proxyConfig.fAutoDetect << endl;
			wcout << L"proxyConfig.lpszProxy: " << null( proxyConfig.lpszProxy ) << endl;
			wcout << L"proxyConfig.lpszProxyBypass: " << null( proxyConfig.lpszProxyBypass ) << endl;
		#endif

		lastError = 0;
		jclass jWinHttpCurrentUserIEProxyConfigClass = env->GetObjectClass( jWinHttpCurrentUserIEProxyConfig );
		setBooleanField( env,jWinHttpCurrentUserIEProxyConfigClass, jWinHttpCurrentUserIEProxyConfig, "isAutoDetect", proxyConfig.fAutoDetect );
		setStringField( env, jWinHttpCurrentUserIEProxyConfigClass, jWinHttpCurrentUserIEProxyConfig, "autoConfigUrl", proxyConfig.lpszAutoConfigUrl );
		setStringField( env, jWinHttpCurrentUserIEProxyConfigClass, jWinHttpCurrentUserIEProxyConfig, "proxy", proxyConfig.lpszProxy );
		setStringField( env, jWinHttpCurrentUserIEProxyConfigClass, jWinHttpCurrentUserIEProxyConfig, "proxyBypass", proxyConfig.lpszProxyBypass );
	}


	#ifdef _DEBUG
		cout << "WinHttp_getIEProxyConfigForCurrentUser - exit" << endl;
	#endif

	return ok;
}

/*
 * Class:     org_eclipse_core_internal_net_proxy_win32_winhttp_WinHttp
 * Method:    getProxyForUrl
 * Signature: (ILjava/lang/String;Lorg/eclipse/core/internal/net/proxy/win32/winhttp/WinHttpAutoProxyOptions;Lorg/eclipse/core/internal/net/proxy/win32/winhttp/WinHttpProxyInfo;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_core_internal_net_proxy_win32_winhttp_WinHttp_getProxyForUrl
  (JNIEnv * env, jclass jClass, jint jInternet, jstring jUrl, jobject jWinHttpAutoProxyOptions, jobject jWinHttpProxyInfo )  {

	#ifdef _DEBUG
		cout << "WinHttp_getProxyForUrl - entered" << endl;
	#endif

	WINHTTP_AUTOPROXY_OPTIONS autoProxyOptions;
	ZeroMemory( &autoProxyOptions, sizeof( autoProxyOptions ) );

	jclass jWinHttpAutoProxyOptionsClass = env->GetObjectClass( jWinHttpAutoProxyOptions );

	autoProxyOptions.dwFlags = getIntField( env, jWinHttpAutoProxyOptionsClass, jWinHttpAutoProxyOptions, "flags" );
	autoProxyOptions.dwAutoDetectFlags = getIntField( env, jWinHttpAutoProxyOptionsClass, jWinHttpAutoProxyOptions, "autoDetectFlags" );
	jstring jAutoConfigUrl = getStringField( env, jWinHttpAutoProxyOptionsClass, jWinHttpAutoProxyOptions, "autoConfigUrl" );
	autoProxyOptions.lpszAutoConfigUrl = (LPCWSTR)getStringChars( env, jAutoConfigUrl );

	// The ´reserved´ fields will not be transfered!
	// - String reservedPointer
	// - int reservedInt

	autoProxyOptions.fAutoLogonIfChallenged = getBooleanField( env, jWinHttpAutoProxyOptionsClass, jWinHttpAutoProxyOptions, "autoLogonIfChallenged" );

	#ifdef _DEBUG
		cout << "autoProxyOptions.dwFlags: " << autoProxyOptions.dwFlags << endl;
		cout << "autoProxyOptions.dwAutoDetectFlags: " << autoProxyOptions.dwAutoDetectFlags << endl;
		wcout << L"autoProxyOptions.lpszAutoConfigUrl: " << null( autoProxyOptions.lpszAutoConfigUrl ) << endl;
		cout << "autoProxyOptions.fAutoLogonIfChallenged: " << autoProxyOptions.fAutoLogonIfChallenged << endl;
	#endif

	WINHTTP_PROXY_INFO proxyInfo;
	ZeroMemory( &proxyInfo, sizeof( proxyInfo ) );

	const jchar * url = getStringChars( env, jUrl );

	BOOL ok = WinHttpGetProxyForUrl( (void *)jInternet, (LPCWSTR)url, &autoProxyOptions, &proxyInfo );

	if ( ! ok ) {
		lastError = GetLastError();
		#ifdef _DEBUG
			cout << "WinHttpGetProxyForUrl() failed with " << lastError << endl; 
		#endif
	}

	releaseStringChars( env, jUrl, url );
	releaseStringChars( env, jAutoConfigUrl, (const jchar *)autoProxyOptions.lpszAutoConfigUrl );

	if ( ok ) {
		lastError = 0;
		jclass jWinHttpProxyInfoClass = env->GetObjectClass( jWinHttpProxyInfo );

		#ifdef _DEBUG
			cout << "proxyInfo.dwAccessType: " << proxyInfo.dwAccessType << endl;
			wcout << L"proxyInfo.lpszProxy: " << null( proxyInfo.lpszProxy ) << endl;
			wcout << L"proxyInfo.lpszProxyBypass: " << null( proxyInfo.lpszProxyBypass ) << endl;
		#endif

		setIntField( env, jWinHttpProxyInfoClass, jWinHttpProxyInfo, "accessType", proxyInfo.dwAccessType );
		setStringField( env, jWinHttpProxyInfoClass, jWinHttpProxyInfo, "proxy", proxyInfo.lpszProxy );
		setStringField( env, jWinHttpProxyInfoClass, jWinHttpProxyInfo, "proxyBypass", proxyInfo.lpszProxyBypass );
	}

	#ifdef _DEBUG
		cout << "WinHttp_getProxyForUrl - exit" << endl;
	#endif

	return ok;
}

/*
 * Class:     org_eclipse_core_internal_net_proxy_win32_winhttp_WinHttp
 * Method:    detectAutoProxyConfigUrl
 * Signature: (Lorg/eclipse/core/internal/net/proxy/win32/winhttp/AutoProxyHolder;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_core_internal_net_proxy_win32_winhttp_WinHttp_detectAutoProxyConfigUrl
  (JNIEnv * env, jclass jClass, jobject jAutoProxyHolder) {

	#ifdef _DEBUG
		cout << "WinHttp_detectAutoProxyConfigUrl - entered" << endl;
	#endif

	jclass jAutoProxyHolderClass = env->GetObjectClass( jAutoProxyHolder );
	DWORD dwAutoDetectFlags = getIntField( env, jAutoProxyHolderClass, jAutoProxyHolder, "autoDetectFlags" );

	#ifdef _DEBUG
		cout << "autoProxyHolder.autoDetectFlags: " << dwAutoDetectFlags << endl;
	#endif

	LPWSTR pwszAutoConfigUrl;

	BOOL ok = WinHttpDetectAutoProxyConfigUrl( dwAutoDetectFlags, &pwszAutoConfigUrl );

	if ( ! ok ) {
		lastError = GetLastError();
		#ifdef _DEBUG
			cout << "WinHttpDetectAutoProxyConfigUrl() failed with " << lastError << endl; 
		#endif
	}

	if ( ok ) {
		lastError = 0;

		#ifdef _DEBUG
			wcout << L"autoConfigUrl: " << null( pwszAutoConfigUrl ) << endl;
		#endif

		setStringField( env, jAutoProxyHolderClass, jAutoProxyHolder, "autoConfigUrl", pwszAutoConfigUrl );
	}

	#ifdef _DEBUG
		cout << "WinHttp_detectAutoProxyConfigUrl - exit" << endl;
	#endif

	return ok;
}


/*
 * Class:     org_eclipse_core_internal_net_proxy_win32_winhttp_WinHttp
 * Method:    getLastError
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_core_internal_net_proxy_win32_winhttp_WinHttp_getLastError
  (JNIEnv * env, jclass jClass) {

	#ifdef _DEBUG
		cout << "WinHttp_getLastError - entered" << endl;
	#endif

	#ifdef _DEBUG
		cout << "WinHttp_getLastError - exit" << endl;
	#endif

	return lastError;
}

/*
 * Class:     org_eclipse_core_internal_net_proxy_win32_winhttp_WinHttp
 * Method:    getLastErrorMessage
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_eclipse_core_internal_net_proxy_win32_winhttp_WinHttp_getLastErrorMessage
  (JNIEnv * env, jclass jClass) {

	#ifdef _DEBUG
		cout << "WinHttp_getLastErrorMessage - entered" << endl;
	#endif

	LPVOID lpMsgBuf = NULL;
	DWORD result = 0;

	if ( lastError >= WINHTTP_ERROR_BASE && lastError <= WINHTTP_ERROR_LAST ) {
		HMODULE hModule = GetModuleHandle( "winhttp.dll" );

		if ( hModule == NULL ) {
			lpMsgBuf = "Could not retrieve error message, because ´GetModuleHandle( \"winhttp.dll\" )´ failed.";
		} else {
			result = FormatMessage( FORMAT_MESSAGE_ALLOCATE_BUFFER | 
									FORMAT_MESSAGE_FROM_HMODULE | 
									FORMAT_MESSAGE_IGNORE_INSERTS,
									hModule,
									lastError,
									MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ), // Default language
					 				(LPTSTR) &lpMsgBuf,
									0,
									NULL );
		}

	} else {
		result = FormatMessage( FORMAT_MESSAGE_ALLOCATE_BUFFER | 
								FORMAT_MESSAGE_FROM_SYSTEM | 
								FORMAT_MESSAGE_IGNORE_INSERTS,
								NULL,
								lastError,
								MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ), // Default language
					 			(LPTSTR) &lpMsgBuf,
								0,
								NULL );
	}
		
	if ( lpMsgBuf == NULL ) {	
		#ifdef _DEBUG
			cout << "WinHttp_getLastErrorMessage() failed with " << GetLastError() << " for error code " << lastError << endl; 
		#endif

		lpMsgBuf = "Could not retrieve error message.";
	}

	jstring string = env->NewStringUTF( (char *) lpMsgBuf );

	if ( result > 0 ) {
		// Free dynamically allocated buffer
		LocalFree( lpMsgBuf );
	}

	#ifdef _DEBUG
		cout << "WinHttp_getLastErrorMessage - exit" << endl;
	#endif

	return string;
}

