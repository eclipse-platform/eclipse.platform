/*******************************************************************************
 * Copyright (c) 2008, 2017 compeople AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 		compeople AG (Stefan Liebig) - initial API and implementation
 * 		IBM Corporation - implementation
 *******************************************************************************/
package org.eclipse.core.internal.net;

import java.net.URI;

import org.eclipse.core.internal.net.proxy.win32.winhttp.WinHttpProxyProvider;
import org.eclipse.core.net.proxy.IProxyData;

public class WindowsProxyProvider extends AbstractProxyProvider {

	private static final String LIBRARY_NAME = "jWinHttp-1.0.0"; //$NON-NLS-1$

	private static boolean jWinHttpLoaded = false;

	static {
		try {
			System.loadLibrary(LIBRARY_NAME);
			if (Policy.DEBUG_SYSTEM_PROVIDERS)
				Policy.debug("Loaded " + LIBRARY_NAME + " library"); //$NON-NLS-1$ //$NON-NLS-2$
			jWinHttpLoaded = true;
		} catch (final UnsatisfiedLinkError e) {
			Activator.logError(
					"Could not load library: " + System.mapLibraryName(LIBRARY_NAME), e); //$NON-NLS-1$
		}
	}

	private WinHttpProxyProvider winHttpProxyProvider;

	public WindowsProxyProvider() {
		if (jWinHttpLoaded) {
			winHttpProxyProvider = new WinHttpProxyProvider();
		} else {
			winHttpProxyProvider = null;
		}
	}

	@Override
	public IProxyData[] select(URI uri) {
		IProxyData[] proxies = new IProxyData[0];
		if (jWinHttpLoaded) {
			proxies = winHttpProxyProvider.getProxyData(uri);
		}
		if (Policy.DEBUG) {
			Policy.debug("WindowsProxyProvider#select result for [" + uri + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			for (IProxyData proxy : proxies) {
				System.out.println("	" + proxy); //$NON-NLS-1$
			}
		}
		return proxies;
	}

	@Override
	protected IProxyData[] getProxyData() {
		if (jWinHttpLoaded) {
			return winHttpProxyProvider.getProxyData();
		}
		return new IProxyData[0];
	}

	@Override
	protected String[] getNonProxiedHosts() {
		if (jWinHttpLoaded) {
			return winHttpProxyProvider.getNonProxiedHosts();
		}
		return new String[0];
	}

}
