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
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *  IBM Corporation - Add proxy providers layer on the top of ProxyManager (bug 255616)
 *******************************************************************************/
package org.eclipse.core.internal.net.proxy.win32.winhttp;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.internal.net.Activator;
import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.net.proxy.IProxyData;

/**
 * The <code>WinHttpProxyProvivider</code> gets its settings from the
 * "internet options &gt;&gt; connection settings". For this it uses the Windows
 * WinHttp API.
 *
 * @see "http://msdn2.microsoft.com/en-us/library/aa382925(VS.85).aspx"
 */
public class WinHttpProxyProvider {

	private WinHttpCurrentUserIEProxyConfig proxyConfig;
	private StaticProxyConfig staticProxyConfig;
	private String wpadAutoConfigUrl;
	private boolean tryWpadGetUrl;
	private boolean tryPac;

	// Buffered delayed logging to avoid deadlocks. Logging itself might trigger
	// through listeners/appenders other threads to do some communication which in
	// turn uses this proxy provider.
	private String logMessage;
	private Throwable logThrowable;

	private static final ProxyData[] EMPTY_PROXIES = new ProxyData[0];
	private static final String MY_NAME = WinHttpProxyProvider.class.getName();

	/**
	 * Retrieve the proxies that are suitable for the given uri. An empty array
	 * of proxies indicates that no proxy should be used (direct connection).
	 * This method considers already the �no proxy for� definition of the
	 * internet options dialog.
	 *
	 * @param uri
	 * @return an array of proxies
	 */
	public IProxyData[] getProxyData(URI uri) {
		logMessage = null;
		IProxyData[] proxies;
		synchronized (this) {
			proxies = getProxyDataUnsynchronized(uri);
		}
		if (logMessage != null)
			Activator.logError(logMessage, logThrowable);
		return proxies;
	}

	public IProxyData[] getProxyData() {
		logMessage = null;
		IProxyData[] proxies;
		synchronized (this) {
			proxies = getProxyDataUnsynchronized();
		}
		if (logMessage != null)
			Activator.logError(logMessage, logThrowable);
		return proxies;
	}

	private IProxyData[] getProxyDataUnsynchronized() {
		WinHttpCurrentUserIEProxyConfig newProxyConfig = new WinHttpCurrentUserIEProxyConfig();
		if (!WinHttp.getIEProxyConfigForCurrentUser(newProxyConfig)) {
			logError(
					"WinHttp.GetIEProxyConfigForCurrentUser failed with error '" + WinHttp.getLastErrorMessage() + "' #" + WinHttp.getLastError() + ".", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return EMPTY_PROXIES;
		}

		// Explicit proxies defined?
		if (newProxyConfig.isStaticProxy()) {
			// Yes, let�s see if we are still up-to-date
			if (newProxyConfig.staticProxyChanged(proxyConfig))
				staticProxyConfig = new StaticProxyConfig(newProxyConfig
						.getProxy(), newProxyConfig.getProxyBypass());

			return staticProxyConfig.getProxyData();
		}

		// Let�s find out if auto detect has changed.
		if (newProxyConfig.autoDetectChanged(proxyConfig)) {
			tryWpadGetUrl = newProxyConfig.isAutoDetect();
			if (!tryWpadGetUrl)
				wpadAutoConfigUrl = null;
		}

		// Let�s find out if pac file url has changed.
		if (newProxyConfig.autoConfigUrlChanged(proxyConfig))
			tryPac = newProxyConfig.isAutoConfigUrl();

		if (!tryPac && wpadAutoConfigUrl == null)
			return new IProxyData[0];

		ProxyData data = new ProxyData(IProxyData.HTTP_PROXY_TYPE, "", -1, //$NON-NLS-1$
				false, "WINDOWS_IE"); //$NON-NLS-1$
		data.setDynamic(true);
		return new IProxyData[] { data };
	}

	public String[] getNonProxiedHosts() {
		logMessage = null;
		String[] hosts;
		synchronized (this) {
			hosts = getNonProxiedHostsUnsynchronized();
		}
		if (logMessage != null)
			Activator.logError(logMessage, logThrowable);
		return hosts;
	}

	private String[] getNonProxiedHostsUnsynchronized() {
		WinHttpCurrentUserIEProxyConfig newProxyConfig = new WinHttpCurrentUserIEProxyConfig();
		if (!WinHttp.getIEProxyConfigForCurrentUser(newProxyConfig)) {
			logError(
					"WinHttp.GetIEProxyConfigForCurrentUser failed with error '" + WinHttp.getLastErrorMessage() + "' #" + WinHttp.getLastError() + ".", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return new String[0];
		}
		if (newProxyConfig.isStaticProxy()) {
			// Yes, let�s see if we are still up-to-date
			if (newProxyConfig.staticProxyChanged(proxyConfig))
				staticProxyConfig = new StaticProxyConfig(newProxyConfig
						.getProxy(), newProxyConfig.getProxyBypass());
			return staticProxyConfig.getNonProxiedHosts();
		}
		return null;
	}

	/**
	 * This method is the not synchronized counterpart of
	 * <code>getProxyData</code>.
	 *
	 * @param uri
	 * @return an array of proxies
	 */
	private IProxyData[] getProxyDataUnsynchronized(URI uri) {
		WinHttpCurrentUserIEProxyConfig newProxyConfig = new WinHttpCurrentUserIEProxyConfig();
		if (!WinHttp.getIEProxyConfigForCurrentUser(newProxyConfig)) {
			logError(
					"WinHttp.GetIEProxyConfigForCurrentUser failed with error '" + WinHttp.getLastErrorMessage() + "' #" + WinHttp.getLastError() + ".", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return EMPTY_PROXIES;
		}

		List<IProxyData> proxies = new ArrayList<>();

		// Let�s find out if auto detect has changed.
		if (newProxyConfig.autoDetectChanged(proxyConfig)) {
			tryWpadGetUrl = newProxyConfig.isAutoDetect();
			if (!tryWpadGetUrl)
				wpadAutoConfigUrl = null;
		}

		// Let�s find out if pac file url has changed.
		if (newProxyConfig.autoConfigUrlChanged(proxyConfig))
			tryPac = newProxyConfig.isAutoConfigUrl();

		// Explicit proxies defined?
		if (newProxyConfig.isStaticProxy()) {
			// Yes, let�s see if we are still up-to-date
			if (newProxyConfig.staticProxyChanged(proxyConfig))
				staticProxyConfig = new StaticProxyConfig(newProxyConfig
						.getProxy(), newProxyConfig.getProxyBypass());

			staticProxyConfig.select(uri, proxies);
		}
		proxyConfig = newProxyConfig;

		if (!tryPac && wpadAutoConfigUrl == null)
			return toArray(proxies);

		// Create the WinHTTP session.
		int hHttpSession = WinHttp.open(MY_NAME,
				WinHttpProxyInfo.WINHTTP_ACCESS_TYPE_NO_PROXY,
				WinHttp.NO_PROXY_NAME, WinHttp.NO_PROXY_BYPASS, 0);
		if (hHttpSession == 0) {
			logError(
					"WinHttp.Open failed with error'" + WinHttp.getLastErrorMessage() + "' #" + WinHttp.getLastError() + ".", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return toArray(proxies);
		}

		try {
			pacSelect(hHttpSession, uri, proxies);
			wpadSelect(hHttpSession, uri, proxies);
		} finally {
			WinHttp.closeHandle(hHttpSession);
		}

		return toArray(proxies);
	}

	protected void pacSelect(int hHttpSession, URI uri, List<IProxyData> proxies) {
		if (!tryPac)
			return;
		List<IProxyData> pacProxies = pacSelect(hHttpSession, proxyConfig
				.getAutoConfigUrl(), uri);
		if (pacProxies == null)
			tryPac = false;
		else
			proxies.addAll(pacProxies);

	}

	protected void wpadSelect(int hHttpSession, URI uri, List<IProxyData> proxies) {
		if (tryWpadGetUrl) {
			tryWpadGetUrl = false;
			AutoProxyHolder autoProxyHolder = new AutoProxyHolder();
			autoProxyHolder
					.setAutoDetectFlags(WinHttpAutoProxyOptions.WINHTTP_AUTO_DETECT_TYPE_DHCP
							| WinHttpAutoProxyOptions.WINHTTP_AUTO_DETECT_TYPE_DNS_A);
			boolean ok = WinHttp.detectAutoProxyConfigUrl(autoProxyHolder);
			if (!ok) {
				logError(
						"WinHttp.DetectAutoProxyConfigUrl for wpad failed with error '" + WinHttp.getLastErrorMessage() + "' #" + WinHttp.getLastError() + ".", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return;
			}
			wpadAutoConfigUrl = autoProxyHolder.getAutoConfigUrl();
		}
		if (wpadAutoConfigUrl == null)
			return;
		List<IProxyData> wpadProxies = pacSelect(hHttpSession, wpadAutoConfigUrl, uri);
		if (wpadProxies == null)
			wpadAutoConfigUrl = null;
		else
			proxies.addAll(wpadProxies);
	}

	/**
	 * Retrieve the proxies from the specified pac file url.
	 *
	 * @param hHttpSession
	 * @param configUrl
	 * @param uri
	 * @return a list of proxies (IProxyData) or null in case of an error.
	 */
	protected List<IProxyData> pacSelect(int hHttpSession, String configUrl, URI uri) {
		// Don�t ask for anything else than http or https since that is not
		// supported by WinHttp pac file support:
		// ERROR_WINHTTP_UNRECOGNIZED_SCHEME
		if (!IProxyData.HTTP_PROXY_TYPE.equalsIgnoreCase(uri.getScheme())
				&& !IProxyData.HTTPS_PROXY_TYPE.equalsIgnoreCase(uri
						.getScheme()))
			return Collections.emptyList();
		// Set up the autoproxy call.
		WinHttpAutoProxyOptions autoProxyOptions = new WinHttpAutoProxyOptions();
		autoProxyOptions
				.setFlags(WinHttpAutoProxyOptions.WINHTTP_AUTOPROXY_CONFIG_URL);
		autoProxyOptions.setAutoConfigUrl(configUrl);
		autoProxyOptions.setAutoLogonIfChallenged(true);
		WinHttpProxyInfo proxyInfo = new WinHttpProxyInfo();

		boolean ok = WinHttp.getProxyForUrl(hHttpSession, uri.toString(),
				autoProxyOptions, proxyInfo);
		if (!ok) {
			logError(
					"WinHttp.GetProxyForUrl for pac failed with error '" + WinHttp.getLastErrorMessage() + "' #" + WinHttp.getLastError() + ".", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return null;
		}
		ProxyBypass proxyBypass = new ProxyBypass(proxyInfo.getProxyBypass());
		if (proxyBypass.bypassProxyFor(uri))
			return Collections.emptyList();
		return ProxyProviderUtil.getProxies(proxyInfo.getProxy());
	}

	private void logError(String message, Throwable throwable) {
		this.logMessage = message;
		this.logThrowable = throwable;
	}

	private static IProxyData[] toArray(List<IProxyData> proxies) {
		return proxies.toArray(new IProxyData[proxies.size()]);
	}

}
