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
 * Wrapper for Win32 WINHTTP_PROXY_INFO Structure.
 * <p>
 * The fields will be written by the jni glue code.
 * </p>
 */
public class WinHttpProxyInfo {

	// WinHttpOpen dwAccessType values (also for
	// WINHTTP_PROXY_INFO::dwAccessType)
	public static final int WINHTTP_ACCESS_TYPE_DEFAULT_PROXY= 0;

	public static final int WINHTTP_ACCESS_TYPE_NO_PROXY= 1;

	public static final int WINHTTP_ACCESS_TYPE_NAMED_PROXY= 3;

	public int accessType;

	public String proxy;

	public String proxyBypass;

	/**
	 * @return the accessType
	 */
	public int getAccessType() {
		return accessType;
	}

	/**
	 * @return the proxy
	 */
	public String getProxy() {
		return proxy;
	}

	/**
	 * @return the proxyBypass
	 */
	public String getProxyBypass() {
		return proxyBypass;
	}

}
