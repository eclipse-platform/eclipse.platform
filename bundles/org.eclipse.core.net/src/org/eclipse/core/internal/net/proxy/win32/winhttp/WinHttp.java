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
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.net.proxy.win32.winhttp;

/**
 * This is the Win32 WinHttp wrapper.
 * <p>
 * Not complete, but offers what we currently need
 * </p>
 */
public final class WinHttp {

	/**
	 * The constant indicates the null proxy name parameter
	 */
	static final String NO_PROXY_NAME= null;

	/**
	 * The constant indicates the null proxy bypass parameter
	 */
	static final String NO_PROXY_BYPASS= null;

	/**
	 * WinHttpOpen - see Microsoft SDK Documentation
	 *
	 * @param userAgent
	 * @param accessType
	 * @param proxyName
	 * @param proxyBypass
	 * @param flags
	 * @return the handle
	 */
	public static native int open(String userAgent, int accessType, String proxyName, String proxyBypass, int flags);

	/**
	 * WinHttpCloseHandle - see Microsoft SDK Documentation
	 *
	 * @param hInternet
	 * @return true on success
	 */
	public static native boolean closeHandle(int hInternet);

	/**
	 * WinHttpGetIEProxyConfigForCurrentUser - see Microsoft SDK Documentation
	 *
	 * @param proxyConfig
	 * @return true on success
	 */
	public static native boolean getIEProxyConfigForCurrentUser(WinHttpCurrentUserIEProxyConfig proxyConfig);

	/**
	 * WinHttpGetProxyForUrl - see Microsoft SDK Documentation
	 *
	 * @param hSession
	 * @param url
	 * @param autoProxyOptions
	 * @param proxyInfo
	 * @return true on success
	 */
	public static native boolean getProxyForUrl(int hSession, String url, WinHttpAutoProxyOptions autoProxyOptions, WinHttpProxyInfo proxyInfo);

	/**
	 * WinHttpDetectAutoProxyConfigUrl - see Microsoft SDK Documentation
	 *
	 * @param autoProxyHolder
	 * @return true on success
	 */
	public static native boolean detectAutoProxyConfigUrl(AutoProxyHolder autoProxyHolder);

	/**
	 * GetLastError - see Microsoft SDK Documentation
	 *
	 * @return the last error code (win32)
	 */
	public static native int getLastError();

	/**
	 * GetLastErrorMessage - formats the last error
	 *
	 * @return the readable last error code
	 */
	public static native String getLastErrorMessage();

}
