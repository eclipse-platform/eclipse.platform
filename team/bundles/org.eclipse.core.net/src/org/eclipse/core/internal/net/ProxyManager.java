/*******************************************************************************
 * Copyright (c) 2007, 2025 IBM Corporation and others.
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
 *     Tue Ton - support for FreeBSD
 *******************************************************************************/
package org.eclipse.core.internal.net;

import java.net.Authenticator;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.net.internal.proxy.freebsd.ProxyProviderFreeBSD;
import org.eclipse.core.net.internal.proxy.linux.ProxyProviderLinux;
import org.eclipse.core.net.internal.proxy.win32.ProxyProviderWin32;
import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class ProxyManager implements IProxyService, IPreferenceChangeListener {

	static final String PREF_NON_PROXIED_HOSTS = "nonProxiedHosts"; //$NON-NLS-1$
	static final String PREF_ENABLED = "proxiesEnabled"; //$NON-NLS-1$
	static final String PREF_OS = "systemProxiesEnabled"; //$NON-NLS-1$

	private static IProxyService proxyManager;

	private AbstractProxyProvider nativeProxyProvider;

	private final PreferenceManager preferenceManager;

	ListenerList<IProxyChangeListener> listeners = new ListenerList<>(ListenerList.IDENTITY);
	private String[] nonProxiedHosts;
	private final ProxyType[] proxies = new ProxyType[] {
			new ProxyType(IProxyData.HTTP_PROXY_TYPE),
			new ProxyType(IProxyData.HTTPS_PROXY_TYPE),
			new ProxyType(IProxyData.SOCKS_PROXY_TYPE)
		};

	private ProxyManager() {
		try {
			String os = System.getProperty("osgi.os"); //$NON-NLS-1$
			if (os != null) {
				nativeProxyProvider = switch (os) {
				case Constants.OS_FREEBSD -> new ProxyProviderFreeBSD();
				case Constants.OS_LINUX -> new ProxyProviderLinux();
				case Constants.OS_WIN32 -> new ProxyProviderWin32();
				default -> null;
				};
			}
		} catch (Exception e) {
			Activator.logInfo("Problems occurred during the proxy provider initialization.", e); //$NON-NLS-1$
		}
		preferenceManager = Activator.getInstance().getPreferenceManager();
	}

	/**
	 * Return the proxy manager.
	 * @return the proxy manager
	 */
	public synchronized static IProxyService getProxyManager() {
		if (proxyManager == null)
			proxyManager = new ProxyManager();
		return proxyManager;
	}

	@Override
	public void addProxyChangeListener(IProxyChangeListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeProxyChangeListener(IProxyChangeListener listener) {
		listeners.remove(listener);
	}

	private void fireChange(final IProxyChangeEvent event) {
		for (final IProxyChangeListener listener : listeners) {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					listener.proxyInfoChanged(event);
				}
				@Override
				public void handleException(Throwable exception) {
					// Logged by SafeRunner
				}
			});
		}
	}

	@Override
	public synchronized String[] getNonProxiedHosts() {
		checkMigrated();
		if (nonProxiedHosts == null) {
			String prop = preferenceManager.getString(PreferenceManager.ROOT, PREF_NON_PROXIED_HOSTS);
			nonProxiedHosts = ProxyType.convertPropertyStringToHosts(prop);
		}
		if (nonProxiedHosts.length == 0)
			return nonProxiedHosts;
		String[] result = new String[nonProxiedHosts.length];
		System.arraycopy(nonProxiedHosts, 0, result, 0, nonProxiedHosts.length );
		return result;
	}

	public String[] getNativeNonProxiedHosts() {
		if (hasSystemProxies()) {
			return nativeProxyProvider.getNonProxiedHosts();
		}
		return new String[0];
	}

	@Override
	public void setNonProxiedHosts(String[] hosts) {
		checkMigrated();
		Assert.isNotNull(hosts);
		for (String host : hosts) {
			Assert.isNotNull(host);
			Assert.isTrue(host.length() > 0);
		}
		String[] oldHosts = nonProxiedHosts;
		if (Arrays.equals(oldHosts, hosts)) {
			return;
		}
		nonProxiedHosts = hosts;
		preferenceManager.putString(PreferenceManager.ROOT, PREF_NON_PROXIED_HOSTS, ProxyType.convertHostsToPropertyString(nonProxiedHosts));
		try {
			preferenceManager.flush();
		} catch (BackingStoreException e) {
			Activator.logError(
					"An error occurred while writing out the non-proxied hosts list", e); //$NON-NLS-1$
		}
		IProxyData[] data = getProxyData();
		IProxyChangeEvent event = new ProxyChangeEvent(IProxyChangeEvent.NONPROXIED_HOSTS_CHANGED, oldHosts, getNonProxiedHosts(), data, new IProxyData[0]);
		updateSystemProperties();
		fireChange(event);
	}


	@Override
	public IProxyData[] getProxyData() {
		checkMigrated();
		IProxyData[] result = new IProxyData[proxies.length];
		for (int i = 0; i < proxies.length; i++) {
			ProxyType type = proxies[i];
			result[i] = type.getProxyData(ProxyType.VERIFY_EQUAL);
		}
		return resolveType(result);
	}

	public IProxyData[] getNativeProxyData() {
		if (hasSystemProxies()) {
			return resolveType(nativeProxyProvider.getProxyData());
		}
		return new IProxyData[0];
	}

	@Override
	public void setProxyData(IProxyData[] proxies) {
		checkMigrated();
		doSetProxyData(proxies);
	}

	private void doSetProxyData(IProxyData[] proxyDatas) {
		IProxyData[] oldData = getProxyData();
		String[] hosts = getNonProxiedHosts();
		IProxyData[] changedProxies = internalSetProxyData(proxyDatas);
		if (changedProxies.length > 0) {
			IProxyChangeEvent event = new ProxyChangeEvent(IProxyChangeEvent.PROXY_SERVICE_ENABLEMENT_CHANGE, hosts, hosts, oldData, changedProxies);
			fireChange(event);
		}
	}

	private IProxyData[] internalSetProxyData(IProxyData[] proxyDatas) {
		List<IProxyData> result = new ArrayList<>();
		for (IProxyData proxyData : proxyDatas) {
			ProxyType type = getType(proxyData);
			if (type != null && type.setProxyData(proxyData)) {
				result.add(proxyData);
			}
		}
		return result.toArray(new IProxyData[result.size()]);
	}

	private ProxyType getType(IProxyData proxyData) {
		for (ProxyType type : proxies) {
			if (type.getName().equals(proxyData.getType())) {
				return type;
			}
		}
		return null;
	}

	@Override
	public boolean isProxiesEnabled() {
		checkMigrated();
		return internalIsProxiesEnabled()
				&& (!isSystemProxiesEnabled() || (isSystemProxiesEnabled() && hasSystemProxies()));
	}

	private boolean internalIsProxiesEnabled() {
		return preferenceManager.getBoolean(PreferenceManager.ROOT, PREF_ENABLED);
	}

	@Override
	public void setProxiesEnabled(boolean enabled) {
		checkMigrated();
		boolean current = internalIsProxiesEnabled();
		if (current == enabled)
			return;
		// Setting the preference will trigger the system property update
		// (see preferenceChange)
		preferenceManager.putBoolean(PreferenceManager.ROOT, PREF_ENABLED, enabled);
	}

	private void internalSetEnabled(boolean enabled, boolean systemEnabled) {
		Properties sysProps = System.getProperties();
		sysProps.put("proxySet", enabled ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sysProps.put("systemProxySet", systemEnabled ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		updateSystemProperties();
		try {
			preferenceManager.flush();
		} catch (BackingStoreException e) {
			Activator.logError(
					"An error occurred while writing out the enablement state", e); //$NON-NLS-1$
		}
		String[] hosts = getNonProxiedHosts();
		IProxyData[] data = getProxyData();
		IProxyChangeEvent event = new ProxyChangeEvent(IProxyChangeEvent.PROXY_DATA_CHANGED, hosts, hosts, data, data);
		fireChange(event);
	}

	private void updateSystemProperties() {
		for (ProxyType type : proxies) {
			type.updateSystemProperties(internalGetProxyData(type.getName(), ProxyType.DO_NOT_VERIFY));
		}
	}

	public void initialize() {
		checkMigrated();
		preferenceManager.addPreferenceChangeListener(PreferenceManager.ROOT, this);
		// Now initialize each proxy type
		for (ProxyType type : proxies) {
			type.initialize();
		}
		registerAuthenticator();
	}

	@Override
	public IProxyData getProxyData(String type) {
		checkMigrated();
		return resolveType(internalGetProxyData(type, ProxyType.VERIFY_EQUAL));
	}

	private IProxyData internalGetProxyData(String type, int verifySystemProperties) {
		for (ProxyType pt : proxies) {
			if (pt.getName().equals(type)) {
				return pt.getProxyData(verifySystemProperties);
			}
		}
		return null;
	}

	@Override
	public IProxyData[] getProxyDataForHost(String host) {
		checkMigrated();
		if (!internalIsProxiesEnabled()) {
			return new IProxyData[0];
		}
		URI uri = tryGetURI(host);
		if (uri == null) {
			return new IProxyData[0];
		}
		if (hasSystemProxies() && isSystemProxiesEnabled()) {
			return resolveType(nativeProxyProvider.select(uri));
		}

		if (isHostFiltered(uri))
			return new IProxyData[0];
		IProxyData[] data = getProxyData();
		List<IProxyData> result = new ArrayList<>();
		for (IProxyData proxyData : data) {
			if (proxyData.getHost() != null)
				result.add(proxyData);
		}
		IProxyData ret[] = result.toArray(new IProxyData[result.size()]);
		return resolveType(ret);
	}

	public static URI tryGetURI(String host) {
		try {
			int i = host.indexOf(":"); //$NON-NLS-1$
			if (i == -1) {
				return new URI("//" + host); //$NON-NLS-1$
			}
			return new URI(host.substring(i + 1));
		} catch (URISyntaxException e) {
			return null;
		}
	}

	private boolean isHostFiltered(URI uri) {
		String host = uri.getHost();
		if (host != null) {
			String[] filters = getNonProxiedHosts();
			for (String filter : filters) {
				if (StringUtil.hostMatchesFilter(host, filter))
					return true;
			}
		}
		return false;
	}

	@Override
	public IProxyData getProxyDataForHost(String host, String type) {
		checkMigrated();
		if (!internalIsProxiesEnabled()) {
			return null;
		}
		if (hasSystemProxies() && isSystemProxiesEnabled())
			try {
				URI uri = new URI(type, "//" + host, null); //$NON-NLS-1$
				IProxyData[] proxyDatas = nativeProxyProvider.select(uri);
				return proxyDatas.length > 0 ? resolveType(proxyDatas[0]) : null;
			} catch (URISyntaxException e) {
				return null;
			}

		IProxyData[] data = getProxyDataForHost(host);
		for (IProxyData proxyData : data) {
			if (proxyData.getType().equalsIgnoreCase(type)
					&& proxyData.getHost() != null)
				return resolveType(proxyData);
		}
		return null;
	}

	private void registerAuthenticator() {
		Authenticator a = getPluggedInAuthenticator();
		if (a != null) {
			Authenticator.setDefault(a);
		}
	}

	private Authenticator getPluggedInAuthenticator() {
		IExtension[] extensions = RegistryFactory.getRegistry().getExtensionPoint(Activator.ID, Activator.PT_AUTHENTICATOR).getExtensions();
		if (extensions.length == 0)
			return null;
		IExtension extension = extensions[0];
		IConfigurationElement[] configs = extension.getConfigurationElements();
		if (configs.length == 0) {
			Activator.log(IStatus.ERROR, NLS.bind("Authenticator {0} is missing required fields", (new Object[] {extension.getUniqueIdentifier()})), null);//$NON-NLS-1$
			return null;
		}
		try {
			IConfigurationElement config = configs[0];
			return (Authenticator) config.createExecutableExtension("class");//$NON-NLS-1$
		} catch (CoreException ex) {
			Activator.log(IStatus.ERROR, NLS.bind("Unable to instantiate authenticator {0}", (new Object[] {extension.getUniqueIdentifier()})), ex);//$NON-NLS-1$
			return null;
		}
	}

	private synchronized void checkMigrated() {
		if (preferenceManager.isMigrated() || !Activator.getInstance().instanceLocationAvailable()) {
			return;
		}
		preferenceManager.migrate(proxies);
	}

	void migrateInstanceScopePreferences(Preferences instance,
			Preferences configuration, boolean isInitialize) {
		preferenceManager.migrateInstanceScopePreferences(instance,
				configuration, proxies, isInitialize);
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (event.getKey().equals(PREF_ENABLED) || event.getKey().equals(PREF_OS)) {
			checkMigrated();
			internalSetEnabled(preferenceManager.getBoolean(PreferenceManager.ROOT, PREF_ENABLED),
					preferenceManager.getBoolean(PreferenceManager.ROOT, PREF_OS));
		}
	}

	@Override
	public boolean hasSystemProxies() {
		return nativeProxyProvider != null;
	}

	@Override
	public boolean isSystemProxiesEnabled() {
		checkMigrated();
		return preferenceManager.getBoolean(PreferenceManager.ROOT, PREF_OS);
	}

	@Override
	public void setSystemProxiesEnabled(boolean enabled) {
		checkMigrated();
		boolean current = isSystemProxiesEnabled();
		if (current == enabled)
			return;
		// Setting the preference will trigger the system property update
		// (see preferenceChange)
		preferenceManager.putBoolean(PreferenceManager.ROOT, PREF_OS, enabled);
	}

	@Override
	public IProxyData[] select(URI uri) {
		String host = uri.getHost();
		if (host != null) {
			IProxyData data = getProxyDataForHost(host, uri.getScheme());
			if (data != null) {
				return resolveType(new IProxyData[] { data });
			}
		}
		return new IProxyData[0];
	}

	public IProxyData resolveType(IProxyData data) {
		if (data == null) {
			return null;
		}
		ProxyData d = (ProxyData) data;
		if (d.getType().equalsIgnoreCase(IProxyData.HTTP_PROXY_TYPE)) {
			d.setType(IProxyData.HTTP_PROXY_TYPE);
		} else if (d.getType().equalsIgnoreCase(IProxyData.HTTPS_PROXY_TYPE)) {
			d.setType(IProxyData.HTTPS_PROXY_TYPE);
		} else if (d.getType().equalsIgnoreCase(IProxyData.SOCKS_PROXY_TYPE)) {
			d.setType(IProxyData.SOCKS_PROXY_TYPE);
		}
		return d;
	}

	public IProxyData[] resolveType(IProxyData[] data) {
		if (data == null) {
			return null;
		}
		for (IProxyData d : data) {
			resolveType(d);
		}
		return data;
	}

}
