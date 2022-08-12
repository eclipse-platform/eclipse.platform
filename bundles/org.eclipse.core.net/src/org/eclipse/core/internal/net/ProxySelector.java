/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
		if (name != null) {
			switch (name) {
			case DIRECT_PROVIDER:
				return Messages.ProxySelector_0;
			case ECLIPSE_PROVIDER:
				return Messages.ProxySelector_1;
			case NATIVE_PROVIDER:
				return Messages.ProxySelector_2;
			default:
				break;
			}
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
		switch (provider) {
		case DIRECT_PROVIDER:
			service.setProxiesEnabled(false);
			service.setSystemProxiesEnabled(false);
			break;
		case ECLIPSE_PROVIDER:
			service.setProxiesEnabled(true);
			service.setSystemProxiesEnabled(false);
			break;
		case NATIVE_PROVIDER:
			service.setProxiesEnabled(true);
			service.setSystemProxiesEnabled(true);
			break;
		default:
			throw new IllegalArgumentException("Provider not supported"); //$NON-NLS-1$
		}
	}

	public static ProxyData[] getProxyData(String provider) {
		ProxyManager manager = (ProxyManager) ProxyManager.getProxyManager();
		switch (provider) {
		case DIRECT_PROVIDER:
			return new ProxyData[0];
		case ECLIPSE_PROVIDER:
			return castArray(manager.getProxyData());
		case NATIVE_PROVIDER:
			return castArray(manager.getNativeProxyData());
		default:
			throw new IllegalArgumentException("Provider not supported"); //$NON-NLS-1$
		}
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
		switch (provider) {
		case DIRECT_PROVIDER:
			return new String[0];
		case ECLIPSE_PROVIDER:
			return manager.getNonProxiedHosts();
		case NATIVE_PROVIDER:
			return manager.getNativeNonProxiedHosts();
		default:
			throw new IllegalArgumentException("Provider not supported"); //$NON-NLS-1$
		}
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
