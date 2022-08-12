/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	private static final int DEFAULT_PREF_PROXY_PORT = -1;
	private static final boolean DEFAULT_PREF_PROXY_HAS_AUTH = false;
	private static final boolean DEFAULT_PREF_ENABLED = true;
	private static final boolean DEFAULT_PREF_OS = true;
	private static final String DEFAULT_PREF_NON_PROXIED_HOSTS = "localhost|127.0.0.1"; //$NON-NLS-1$
	public PreferenceInitializer() {
		super();
	}

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences node = DefaultScope.INSTANCE.getNode(Activator.ID);
		node.put(ProxyManager.PREF_NON_PROXIED_HOSTS, DEFAULT_PREF_NON_PROXIED_HOSTS);
		node.putBoolean(ProxyManager.PREF_ENABLED, DEFAULT_PREF_ENABLED);
		node.putBoolean(ProxyManager.PREF_OS, DEFAULT_PREF_OS);

		Preferences type = node.node(ProxyType.PREF_PROXY_DATA_NODE).node(IProxyData.HTTP_PROXY_TYPE);
		type.putInt(ProxyType.PREF_PROXY_PORT, DEFAULT_PREF_PROXY_PORT);
		type.putBoolean(ProxyType.PREF_PROXY_HAS_AUTH, DEFAULT_PREF_PROXY_HAS_AUTH);

		type = node.node(ProxyType.PREF_PROXY_DATA_NODE).node(IProxyData.HTTPS_PROXY_TYPE);
		type.putInt(ProxyType.PREF_PROXY_PORT, DEFAULT_PREF_PROXY_PORT);
		type.putBoolean(ProxyType.PREF_PROXY_HAS_AUTH, DEFAULT_PREF_PROXY_HAS_AUTH);

		type = node.node(ProxyType.PREF_PROXY_DATA_NODE).node(IProxyData.SOCKS_PROXY_TYPE);
		type.putInt(ProxyType.PREF_PROXY_PORT, DEFAULT_PREF_PROXY_PORT);
		type.putBoolean(ProxyType.PREF_PROXY_HAS_AUTH, DEFAULT_PREF_PROXY_HAS_AUTH);

		try {
			node.flush();
		} catch (BackingStoreException e) {
			Activator.logInfo("Could not store default preferences", e); //$NON-NLS-1$
		}
	}

}
