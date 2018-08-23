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

import org.eclipse.core.internal.net.StringUtil;

/**
 * Wrapper for Win32 WINHTTP_CURRENT_USER_IE_PROXY_CONFIG structure.<br>
 * Plus a few helper methods that enrich the plain C structure.
 * <p>
 * The fields will be written by the jni glue code.
 * </p>
 */
public class WinHttpCurrentUserIEProxyConfig {

	public boolean isAutoDetect;
	public String autoConfigUrl;
	public String proxy;
	public String proxyBypass;

	/**
	 * @return the autoConfigUrl
	 */
	public String getAutoConfigUrl() {
		return autoConfigUrl;
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

	/**
	 * I auto detection requested?
	 *
	 * @return the isAutoDetect
	 */
	public boolean isAutoDetect() {
		return isAutoDetect;
	}

	/**
	 * Is a auto config url reqested?
	 *
	 * @return true if there is a auto config url
	 */
	public boolean isAutoConfigUrl() {
		return autoConfigUrl != null && autoConfigUrl.length() != 0;
	}

	/**
	 * Are static proxies defined?
	 *
	 * @return the isStaticProxy
	 */
	public boolean isStaticProxy() {
		return proxy != null && proxy.length() != 0;
	}

	/**
	 * Did the auto-detect change?
	 *
	 * @param proxyConfig
	 *            the proxy config; maybe null
	 * @return true if changed
	 */
	public boolean autoDetectChanged(WinHttpCurrentUserIEProxyConfig proxyConfig) {
		if (proxyConfig == null)
			return true;
		return isAutoDetect != proxyConfig.isAutoDetect;
	}

	/**
	 * Did the auto-config url change?
	 *
	 * @param proxyConfig
	 *            the proxy config; maybe null
	 * @return true if changed
	 */
	public boolean autoConfigUrlChanged(
			WinHttpCurrentUserIEProxyConfig proxyConfig) {
		if (proxyConfig == null)
			return true;
		return !StringUtil.equals(autoConfigUrl,
				proxyConfig.autoConfigUrl);
	}

	/**
	 * Did the static proxy information change?
	 *
	 * @param proxyConfig
	 *            the proxy config; maybe null
	 * @return true if changed
	 */
	public boolean staticProxyChanged(
			WinHttpCurrentUserIEProxyConfig proxyConfig) {
		if (proxyConfig == null)
			return true;
		return !(StringUtil.equals(proxy, proxyConfig.proxy) && StringUtil
				.equals(proxyBypass, proxyConfig.proxyBypass));
	}

}
