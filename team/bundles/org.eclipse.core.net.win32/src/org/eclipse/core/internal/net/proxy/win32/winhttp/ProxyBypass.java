/*******************************************************************************
 * Copyright (c) 2008, 2010 compeople AG and others.
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
 *  IBM Corporation - Add proxy providers layer on the top of ProxyManager (bug 255616)
 *******************************************************************************/
package org.eclipse.core.internal.net.proxy.win32.winhttp;

import java.net.URI;

import org.eclipse.core.internal.net.StringUtil;

/**
 * Encapsulates the windows specific proxy bypass list. It transforms the native
 * API proxy bypass list into queryable java object.
 *
 * @see "http://msdn2.microsoft.com/en-us/library/aa383912(VS.85).aspx"
 */
public class ProxyBypass {

	private final String proxyBypass;

	private final String proxyBypassEntries[];

	private final static String BYPASS_LOCAL_ADDESSES_TOKEN = "<local>"; //$NON-NLS-1$

	/**
	 * Create a ProxyBypass instance from the proxy bypass list string.
	 *
	 * @param proxyBypass
	 */
	public ProxyBypass(String proxyBypass) {
		this.proxyBypass = proxyBypass != null ? proxyBypass : ""; //$NON-NLS-1$

		if (proxyBypass != null) {
			proxyBypassEntries = StringUtil.split(proxyBypass, new String[] {
					";", "|" }); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			proxyBypassEntries = new String[0];
		}
	}

	/**
	 * Check whether the given uri should bypass the proxy.
	 *
	 * @param uri
	 * @return true if the the uri should bypass the proxy; otherwise false
	 */
	public boolean bypassProxyFor(URI uri) {
		final String host = uri.getHost();
		if (host == null)
			return false;
		return (isLocal(host) && isBypassLocalAddresses(proxyBypass))
				|| isInBypassList(host);
	}

	/**
	 * @param proxyBypass
	 * @param uri
	 * @return
	 */
	private boolean isInBypassList(String host) {
		for (String entry : proxyBypassEntries) {
			if (StringUtil.hostMatchesFilter(host, entry)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param uri
	 * @return
	 */
	private static boolean isLocal(String host) {
		return !host.contains("."); //$NON-NLS-1$
	}

	/**
	 * @param addressListString
	 * @return
	 */
	private static boolean isBypassLocalAddresses(String proxyBypass) {
		return proxyBypass.contains(BYPASS_LOCAL_ADDESSES_TOKEN);
	}

	public String[] getNonProxiedHosts() {
		String ret = proxyBypass.replace("|", ";"); //$NON-NLS-1$ //$NON-NLS-2$
		return StringUtil.split(ret, new String[] { ";" }); //$NON-NLS-1$
	}

}
