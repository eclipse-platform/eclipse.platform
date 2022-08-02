/*******************************************************************************
* Copyright (c) 2010, 2017 IBM Corporation and others.
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
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Provides set of methods to operate on preferences
 */
public class PreferenceManager {

	public static final String ROOT = ""; //$NON-NLS-1$

	private static final String PREF_HAS_MIGRATED = "org.eclipse.core.net.hasMigrated"; //$NON-NLS-1$

	private static final int DEFAULT_INT = -1;
	private static final String DEFAULT_STRING = null;
	private static final boolean DEFAULT_BOOLEAN = false;

	private static boolean migrated = false;

	private IEclipsePreferences defaultScope;
	private IEclipsePreferences currentScope;

	private PreferenceManager(String id) {
		this.defaultScope = DefaultScope.INSTANCE.getNode(id);
	}

	/**
	 * Creates the preferences manager for the node defined by id
	 * in configuration scope.
	 * @param id node name for which node should be created in configuration scope
	 * @return {@link PreferenceManager}
	 */
	public static PreferenceManager createConfigurationManager(String id) {
		PreferenceManager manager = new PreferenceManager(id);
		manager.currentScope = ConfigurationScope.INSTANCE.getNode(id);
		return manager;
	}

	/**
	 * Checks if preference migration was already performed.
	 * @return <code>boolean</code>
	 */
	public boolean isMigrated() {
		return migrated;
	}

	/**
	 * Returns the <code>boolean</code> value associated with the specified <code>key</code>
	 * for specified <code>node</code> in current scope.
	 *
	 * <p>
	 * Returns the value specified in the default scope if there is no value associated with the
	 * <code>key</code> in the current scope, the backing store is inaccessible, or if the associated
	 * value is something that can not be parsed as an integer value.
	 * Use {@link #putBoolean(String, String, boolean)} to set the value of this preference key.
	 * </p>
	 * @param node node
	 * @param key key whose associated value is to be returned as an <code>boolean</code>.
	 * @return the <code>boolean</code> value associated with <code>key</code>, or
	 *         <code>false</code> if the associated value does not exist in either scope or cannot
	 *         be interpreted as an <code>boolean</code>.
	 * @see #putBoolean(String, String, boolean)
	 */
	public boolean getBoolean(String node, String key) {
		return currentScope.node(node).getBoolean(key, defaultScope.node(node).getBoolean(key, DEFAULT_BOOLEAN));
	}

	/**
	 * Returns the <code>int</code> value associated with the specified <code>key</code>
	 * for specified <code>node</code> in current scope.
	 *
	 * <p>
	 * Returns the value specified in the default scope if there is no value associated with the
	 * <code>key</code> in the current scope, the backing store is inaccessible, or if the associated
	 * value is something that can not be parsed as an integer value.
	 * Use {@link #putInt(String, String, int)} to set the value of this preference key.
	 * </p>
	 * @param node node
	 * @param key key whose associated value is to be returned as an <code>int</code>.
	 * @return the <code>int</code> value associated with <code>key</code>, or
	 *         <code>-1</code> if the associated value does not exist in either scope or cannot
	 *         be interpreted as an <code>int</code>.
	 * @see #putInt(String, String, int)
	 */
	public int getInt(String node, String key) {
		return currentScope.node(node).getInt(key, defaultScope.node(node).getInt(key, DEFAULT_INT));
	}

	/**
	 * Returns the <code>String</code> value associated with the specified <code>key</code>
	 * for specified <code>node</code> in current scope.
	 *
	 * <p>
	 * Returns the value specified in the default scope if there is no value associated with the
	 * <code>key</code> in the current scope, the backing store is inaccessible, or if the associated
	 * value is something that can not be parsed as an integer value.
	 * Use {@link #putString(String, String, String)} to set the value of this preference key.
	 * </p>
	 * @param node node
	 * @param key key whose associated value is to be returned as an <code>String</code>.
	 * @return the <code>String</code> value associated with <code>key</code>, or
	 *         <code>null</code> if the associated value does not exist in either scope or cannot
	 *         be interpreted as an <code>String</code>.
	 * @see #putString(String, String, String)
	 */
	public String getString(String node, String key) {
		return currentScope.node(node).get(key, defaultScope.node(node).get(key, DEFAULT_STRING));
	}

	/**
	 * Associates the specified <code>int</code> value with the specified key
	 * for specified <code>node</code> in current scope.
	 *
	 * @param node node
	 * @param key <code>key</code> with which the string form of value is to be associated.
	 * @param value <code>value</code> to be associated with <code>key</code>.
	 * @see #getInt(String, String)
	 */
	public void putInt(String node, String key, int value) {
		currentScope.node(node).putInt(key, value);
	}

	/**
	 * Associates the specified <code>boolean</code> value with the specified key
	 * for specified <code>node</code> in current scope.
	 *
	 * @param node node
	 * @param key <code>key</code> with which the string form of value is to be associated.
	 * @param value <code>value</code> to be associated with <code>key</code>.
	 * @see #getBoolean(String, String)
	 */
	public void putBoolean(String node, String key, boolean value) {
		currentScope.node(node).putBoolean(key, value);
	}

	/**
	 * Associates the specified <code>String</code> value with the specified key
	 * for specified <code>node</code> in current scope.
	 *
	 * @param node node
	 * @param key <code>key</code> with which the string form of value is to be associated.
	 * @param value <code>value</code> to be associated with <code>key</code>.
	 * @see #getString(String, String)
	 */
	public void putString(String node, String key, String value) {
		currentScope.node(node).put(key, value);
	}

	/**
	 * Register the given listener for notification of preference changes.
	 * Calling this method multiple times with the same listener has no effect. The
	 * given listener argument must not be <code>null</code>.
	 *
	 * @param node node
	 * @param listener the preference change listener to register
	 * @see #removePreferenceChangeListener(String, IEclipsePreferences.IPreferenceChangeListener)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener
	 */
	public void addPreferenceChangeListener(String node, IPreferenceChangeListener listener) {
		((IEclipsePreferences)currentScope.node(node)).addPreferenceChangeListener(listener);
	}

	/**
	 * De-register the given listener from receiving notification of preference changes
	 * Calling this method multiple times with the same listener has no
	 * effect. The given listener argument must not be <code>null</code>.
	 * @param node node
	 * @param listener the preference change listener to remove
	 * @see #addPreferenceChangeListener(String, IEclipsePreferences.IPreferenceChangeListener)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener
	 */
	public void removePreferenceChangeListener(String node, IPreferenceChangeListener listener) {
		((IEclipsePreferences)currentScope.node(node)).removePreferenceChangeListener(listener);
	}

	/**
	 * Register the given listener for changes to this node.
	 * Calling this method multiple times with the same listener has no effect. The
	 * given listener argument must not be <code>null</code>.
	 *
	 * @param node node
	 * @param listener the preference change listener to register
	 * @see #removeNodeChangeListener(String, IEclipsePreferences.INodeChangeListener)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener
	 */
	public void addNodeChangeListener(String node, INodeChangeListener listener) {
		((IEclipsePreferences)currentScope.node(node)).addNodeChangeListener(listener);
	}

	/**
	 * De-register the given listener from receiving event change notifications for this node.
	 * Calling this method multiple times with the same listener has no
	 * effect. The given listener argument must not be <code>null</code>.
	 * @param node node
	 * @param listener the preference change listener to remove
	 * @see #addPreferenceChangeListener(String, IEclipsePreferences.IPreferenceChangeListener)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener
	 */
	public void removeNodeChangeListener(String node, INodeChangeListener listener) {
		((IEclipsePreferences)currentScope.node(node)).removeNodeChangeListener(listener);
	}

	/**
	 * Removes this node and all of its descendants,
	 * invalidating any properties contained in the removed nodes.
	 * @param node name of a node which should be removed
	 * @throws BackingStoreException - if this operation cannot be completed
	 * due to a failure in the backing store, or inability to communicate with it.
	 */
	public void removeNode(String node) throws BackingStoreException {
		currentScope.node(node).removeNode();
	}

	/**
	 * Forces any changes in the contents of current scope
	 * and its descendants to the persistent store.
	 * @throws BackingStoreException - if this operation cannot be completed
	 * due to a failure in the backing store, or inability to communicate with it.
	 */
	public void flush() throws BackingStoreException {
		currentScope.flush();
	}

	/**
	 * Migrate preferences from instance scope to current scope.
	 * @param proxies proxy types for which migration should be performed {@link ProxyType}
	 */
	public void migrate(ProxyType[] proxies) {
		migrated = true;
		if (currentScope.getBoolean(PREF_HAS_MIGRATED, false)
				|| currentScope.name().equals(InstanceScope.SCOPE)) {
			return;
		}
		currentScope.putBoolean(PREF_HAS_MIGRATED, true);
		migrateInstanceScopePreferences(InstanceScope.INSTANCE.getNode(Activator.ID), currentScope, proxies, true);
	}

	void migrateInstanceScopePreferences(Preferences instanceScope, Preferences configuration, ProxyType[] proxies, boolean isInitialize) {
		migrateUpdateHttpProxy(instanceScope, proxies, isInitialize);

		// migrate enabled status
		if (configuration.get(ProxyManager.PREF_ENABLED, null) == null) {
			String instanceEnabled = instanceScope.get(ProxyManager.PREF_ENABLED, null);
			if (instanceEnabled != null)
				configuration.put(ProxyManager.PREF_ENABLED, instanceEnabled);
		}

		// migrate enabled status
		if (configuration.get(ProxyManager.PREF_OS, null) == null) {
			String instanceEnabled = instanceScope.get(ProxyManager.PREF_OS, null);
			if (instanceEnabled != null)
				configuration.put(ProxyManager.PREF_OS, instanceEnabled);
		}

		// migrate non proxied hosts if not already set
		if (configuration.get(ProxyManager.PREF_NON_PROXIED_HOSTS, null) == null) {
			String instanceNonProxiedHosts = instanceScope.get(ProxyManager.PREF_NON_PROXIED_HOSTS, null);
			if (instanceNonProxiedHosts != null) {
				configuration.put(ProxyManager.PREF_NON_PROXIED_HOSTS, instanceNonProxiedHosts);
			}
		}

		// migrate proxy data
		PreferenceManager instanceManager = PreferenceManager.createInstanceManager(instanceScope);
		for (ProxyType type : proxies) {
			IProxyData data = type.getProxyData(ProxyType.DO_NOT_VERIFY);
			if (data.getHost() == null) {
				ProxyType instanceType = new ProxyType(type.getName(), instanceManager);
				IProxyData instanceData = instanceType.getProxyData(ProxyType.DO_NOT_VERIFY);
				if (instanceData.getHost() != null)
					type.setProxyData(instanceData);
			}
		}

		// if this an import we should remove the old node
		if (!isInitialize) {
			try {
				instanceScope.removeNode();
			} catch (BackingStoreException e) {
				// ignore
			}
		}
	}

	private void migrateUpdateHttpProxy(Preferences instanceScope, ProxyType[] proxies, boolean isInitialize) {
		if (!instanceScope.getBoolean(PREF_HAS_MIGRATED, false)) {
			// Only set the migration bit when initializing
			if (isInitialize)
				instanceScope.putBoolean(PREF_HAS_MIGRATED, true);
			String httpProxyHost = getHostToMigrate(isInitialize /* checkSystemProperties */);
			int port = getPortToMigrate(isInitialize /* checkSystemProperties */);
			boolean httpProxyEnable = getEnablementToMigrate(isInitialize /* checkSystemProperties */);
			if (httpProxyHost != null) {
				ProxyData proxyData = new ProxyData(IProxyData.HTTP_PROXY_TYPE,
						httpProxyHost, port, false, null);
				for (ProxyType type : proxies) {
					if (type.getName().equals(proxyData.getType())) {
						type.updatePreferencesIfMissing(proxyData);
					}
				}
				if (httpProxyEnable) {
					instanceScope.putBoolean(ProxyManager.PREF_ENABLED, true);
				}
			}
		}
	}

	private String getHostToMigrate(boolean checkSystemProperties) {
		String httpProxyHost = ""; //$NON-NLS-1$
		if (checkSystemProperties && "".equals(httpProxyHost)) { //$NON-NLS-1$
			httpProxyHost = System.getProperty("http.proxyHost", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if ("".equals(httpProxyHost)) //$NON-NLS-1$
			httpProxyHost = null;
		return httpProxyHost;
	}

	private int getPortToMigrate(boolean checkSystemProperties) {
		String httpProxyPort = ""; //$NON-NLS-1$
		if (checkSystemProperties && "".equals(httpProxyPort)) { //$NON-NLS-1$
			httpProxyPort = System.getProperty("http.proxyPort", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		int port = -1;
		if (httpProxyPort != null && !"".equals(httpProxyPort)) //$NON-NLS-1$
			try {
				port = Integer.parseInt(httpProxyPort);
			} catch (NumberFormatException e) {
				// Ignore
			}
		return port;
	}

	private boolean getEnablementToMigrate(boolean checkSystemProperties) {
		boolean httpProxyEnable = false;
		if (checkSystemProperties) {
			httpProxyEnable = Boolean.getBoolean("http.proxySet"); //$NON-NLS-1$
		}
		return httpProxyEnable;
	}

	private static PreferenceManager createInstanceManager(Preferences instance) {
		PreferenceManager manager = new PreferenceManager(Activator.ID);
		manager.currentScope = (IEclipsePreferences) instance;
		return manager;
	}
}
