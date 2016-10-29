/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.net;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

/**
 * This class adapts ProxyManager to add additional layer of providers on its
 * top.
 */
public class ProxySelector {

	private static final String DIRECT_PROVIDER = "Direct"; //$NON-NLS-1$
	private static final String ECLIPSE_PROVIDER = "Manual"; //$NON-NLS-1$
	private static final String NATIVE_PROVIDER = "Native"; //$NON-NLS-1$

	public static String[] getProviders() {
		return new String[] { DIRECT_PROVIDER, ECLIPSE_PROVIDER,
				NATIVE_PROVIDER };
	}

	public static String unlocalizeProvider(String name) {
		if (Messages.ProxySelector_0.equals(name)) {
			return DIRECT_PROVIDER;
		} else if (Messages.ProxySelector_1.equals(name)) {
			return ECLIPSE_PROVIDER;
		} else if (Messages.ProxySelector_2.equals(name)) {
			return NATIVE_PROVIDER;
		}
		Assert.isTrue(false);
		return null;
	}

	public static String localizeProvider(String name) {
		if (DIRECT_PROVIDER.equals(name)) {
			return Messages.ProxySelector_0;
		} else if (ECLIPSE_PROVIDER.equals(name)) {
			return Messages.ProxySelector_1;
		} else if (NATIVE_PROVIDER.equals(name)) {
			return Messages.ProxySelector_2;
		}
		Assert.isTrue(false);
		return null;
	}

	public static String getDefaultProvider() {
		IProxyService service = ProxyManager.getProxyManager();
		if (!service.isProxiesEnabled()) {
			return DIRECT_PROVIDER;
		} else if (service.isProxiesEnabled()
				&& !service.isSystemProxiesEnabled()) {
			return ECLIPSE_PROVIDER;
		}
		return NATIVE_PROVIDER;
	}

	public static void setActiveProvider(String provider) {
		IProxyService service = ProxyManager.getProxyManager();
		if (provider.equals(DIRECT_PROVIDER)) {
			service.setProxiesEnabled(false);
			service.setSystemProxiesEnabled(false);
		} else if (provider.equals(ECLIPSE_PROVIDER)) {
			service.setProxiesEnabled(true);
			service.setSystemProxiesEnabled(false);
		} else if (provider.equals(NATIVE_PROVIDER)) {
			service.setProxiesEnabled(true);
			service.setSystemProxiesEnabled(true);
		} else {
			throw new IllegalArgumentException("Provider not supported"); //$NON-NLS-1$
		}
	}

	public static ProxyData[] getProxyData(String provider) {
		ProxyManager manager = (ProxyManager) ProxyManager.getProxyManager();
		if (provider.equals(DIRECT_PROVIDER)) {
			return new ProxyData[0];
		} else if (provider.equals(ECLIPSE_PROVIDER)) {
			return castArray(manager.getProxyData());
		} else if (provider.equals(NATIVE_PROVIDER)) {
			return castArray(manager.getNativeProxyData());
		}
		throw new IllegalArgumentException("Provider not supported"); //$NON-NLS-1$
	}

	private static ProxyData[] castArray(IProxyData data[]) {
		ProxyData[] ret = new ProxyData[data.length];
		System.arraycopy(data, 0, ret, 0, data.length);
		return ret;
	}

	public static void setProxyData(String provider, ProxyData proxies[]) {
		if (provider.equals(ECLIPSE_PROVIDER)) {
			IProxyService service = ProxyManager.getProxyManager();
			try {
				service.setProxyData(proxies);
			} catch (CoreException e) {
				// Should never occur since ProxyManager does not
				// declare CoreException to be thrown
				throw new RuntimeException(e);
			}
		} else {
			throw new IllegalArgumentException(
					"Provider does not support setting proxy data"); //$NON-NLS-1$
		}
	}

	public static boolean canSetProxyData(String provider) {
		if (provider.equals(ECLIPSE_PROVIDER)) {
			return true;
		}
		return false;
	}

	public static String[] getBypassHosts(String provider) {
		ProxyManager manager = (ProxyManager) ProxyManager.getProxyManager();
		if (provider.equals(DIRECT_PROVIDER)) {
			return new String[0];
		} else if (provider.equals(ECLIPSE_PROVIDER)) {
			return manager.getNonProxiedHosts();
		} else if (provider.equals(NATIVE_PROVIDER)) {
			return manager.getNativeNonProxiedHosts();
		}
		throw new IllegalArgumentException("Provider not supported"); //$NON-NLS-1$
	}

	public static void setBypassHosts(String provider, String hosts[]) {
		if (provider.equals(ECLIPSE_PROVIDER)) {
			IProxyService service = ProxyManager.getProxyManager();
			try {
				service.setNonProxiedHosts(hosts);
			} catch (CoreException e) {
				// Should never occur since ProxyManager does not
				// declare CoreException to be thrown
				throw new RuntimeException(e);
			}
		} else {
			throw new IllegalArgumentException(
					"Provider does not support setting bypass hosts"); //$NON-NLS-1$
		}
	}

	public static boolean canSetBypassHosts(String provider) {
		if (provider.equals(ECLIPSE_PROVIDER)) {
			return true;
		}
		return false;
	}

}
