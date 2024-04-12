/*******************************************************************************
 * Copyright (c) 2022 Rolf Theunissen and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rolf Theunissen <rolf.theunissen@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.net;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.internal.net.AbstractProxyProvider;
import org.eclipse.core.internal.net.Activator;
import org.eclipse.core.internal.net.Policy;
import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.internal.net.StringUtil;
import org.eclipse.core.internal.net.proxy.win32.winhttp.ProxyBypass;
import org.eclipse.core.internal.net.proxy.win32.winhttp.ProxyProviderUtil;
import org.eclipse.core.internal.net.proxy.win32.winhttp.StaticProxyConfig;
import org.eclipse.core.net.proxy.IProxyData;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WTypes.LPWSTR;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * The <code>ProxyProvivider</code> gets its settings from the "internet options
 * &gt;&gt; connection settings". For this it uses the Windows WinHttp API.
 *
 * @see "http://msdn2.microsoft.com/en-us/library/aa382925(VS.85).aspx"
 */
public class ProxyProvider extends AbstractProxyProvider {

	private static final String LIBRARY_NAME = "winhttp";
	private static final String USER_AGENT = "WinHttpProxyProvider";
	private static final ProxyData[] EMPTY_PROXIES = new ProxyData[0];

	private static WinHttp fWinHttp;
	private static boolean isWinHttpLoaded = false;

	private Pointer hHttpSession;
	private boolean isWinHttpInitialized = false;

	/* Cache static configuration */
	private StaticProxyConfig fStaticProxyConfig;
	private String fProxiesString;
	private String fProxyBypassString;

	static {
		try {
			fWinHttp = Native.load(LIBRARY_NAME, WinHttp.class, W32APIOptions.UNICODE_OPTIONS);
			fWinHttp = (WinHttp) Native.synchronizedLibrary(fWinHttp);
			if (Policy.DEBUG_SYSTEM_PROVIDERS) {
				Policy.debug("Loaded library " + System.mapLibraryName(LIBRARY_NAME));
			}
			isWinHttpLoaded = true;
		} catch (UnsatisfiedLinkError e) {
			if (Policy.DEBUG_SYSTEM_PROVIDERS) {
				Policy.debug("Could not load library " + System.mapLibraryName(LIBRARY_NAME));
			}
			Activator.logError("Problem during initializing system proxy configuration.", e);
		}
	}

	public ProxyProvider() {
		if (isWinHttpLoaded) {
			initialize();
		}
	}

	@Override
	public IProxyData[] select(URI uri) {
		IProxyData[] proxies = getSystemProxyInfo(uri);
		if (Policy.DEBUG) {
			Policy.debug("WindowsProxyProvider#select result for [" + uri + "]");
			for (IProxyData proxy : proxies) {
				System.out.println("	" + proxy);
			}
		}
		return proxies;
	}

	protected IProxyData[] getSystemProxyInfo(URI uri) {
		WinHttp.WinHttpCurrentUserIEProxyConfig proxyConfig = getProxyConfig();
		if (proxyConfig == null) {
			if (Policy.DEBUG_SYSTEM_PROVIDERS) {
				Policy.debug("Error getting proxy configuration");
			}
			return EMPTY_PROXIES;
		}

		try {
			// Dynamic configuration - WPAD and/or PAC
			// WinHTTP PAC only supports 'http[s]:', see ERROR_WINHTTP_UNRECOGNIZED_SCHEME
			if (isWinHttpInitialized && (proxyConfig.fAutoDetect || proxyConfig.lpszAutoConfigUrl != null)
					&& (IProxyData.HTTP_PROXY_TYPE.equalsIgnoreCase(uri.getScheme())
							|| IProxyData.HTTPS_PROXY_TYPE.equalsIgnoreCase(uri.getScheme()))) {
				WinHttp.WinHttpAutoProxyOptions autoProxyOptions = new WinHttp.WinHttpAutoProxyOptions();
				WinHttp.WinHttpProxyInfo proxyInfo = new WinHttp.WinHttpProxyInfo();

				// WPAD: Web Proxy Auto-Discovery configuration
				if (proxyConfig.fAutoDetect) {
					if (Policy.DEBUG_SYSTEM_PROVIDERS) {
						Policy.debug("Dynamic proxy configuration using WPAD");
					}
					autoProxyOptions.dwFlags = WinHttp.AUTOPROXY_AUTO_DETECT;
					autoProxyOptions.dwAutoDetectFlags = WinHttp.AUTO_DETECT_TYPE_DHCP | WinHttp.AUTO_DETECT_TYPE_DNS_A;
				}
				// PAC: Proxy-Auto Configuration
				// When both WPAD and PAC are set, WinHTTP tries PAC only after WPAD failed
				if (proxyConfig.lpszAutoConfigUrl != null) {
					if (Policy.DEBUG_SYSTEM_PROVIDERS) {
						Policy.debug("Dynamic proxy configuration using PAC url");
					}
					autoProxyOptions.dwFlags |= WinHttp.AUTOPROXY_CONFIG_URL;
					autoProxyOptions.lpszAutoConfigUrl = proxyConfig.getAutoConfigUrl();
				}
				autoProxyOptions.fAutoLogonIfChallenged = true;

				try {
					getProxyForUrl(hHttpSession, uri.toString(), autoProxyOptions, proxyInfo);
					if (Policy.DEBUG_SYSTEM_PROVIDERS) {
						Policy.debug("Dynamic proxy configuration returned: Proxy '" + proxyInfo.getProxy()
								+ "'; ProxyByPass '" + proxyInfo.getProxyBypass() + "';");
					}

					ProxyBypass proxyBypass = new ProxyBypass(proxyInfo.getProxyBypass());
					if (proxyBypass.bypassProxyFor(uri)) {
						return EMPTY_PROXIES;
					}
					return toArray(ProxyProviderUtil.getProxies(proxyInfo.getProxy()));
				} catch (LastErrorException e) {
					// WPAD/PAC errors are intermittent, they can disappear when network
					// configuration changes. Ignore errors, continue to static configuration.
					if (Policy.DEBUG_SYSTEM_PROVIDERS) {
						Policy.debug("Dynamic proxy configuration returned error: " + formatMessage(e.getErrorCode()));
					}
				} finally {
					proxyInfo.free();
				}

			}
			// Static configuration
			if (proxyConfig.lpszProxy != null) {
				if (Policy.DEBUG_SYSTEM_PROVIDERS) {
					Policy.debug("Static proxy configuration: Proxy '" + proxyConfig.getProxy() + "'; ProxyByPass '"
							+ proxyConfig.getProxyBypass() + "';");
				}
				StaticProxyConfig staticProxyConfig = getStaticConfig(proxyConfig.getProxy(),
						proxyConfig.getProxyBypass());

				List<IProxyData> proxies = new ArrayList<>();
				staticProxyConfig.select(uri, proxies);
				return toArray(proxies);
			}

			// Default configuration direct connection
			if (Policy.DEBUG_SYSTEM_PROVIDERS) {
				Policy.debug("No proxy configuration");
			}
			return EMPTY_PROXIES;

		} finally {
			proxyConfig.free();
		}
	}

	@Override
	protected IProxyData[] getProxyData() {
		WinHttp.WinHttpCurrentUserIEProxyConfig proxyConfig = getProxyConfig();
		if (proxyConfig == null) {
			if (Policy.DEBUG_SYSTEM_PROVIDERS) {
				Policy.debug("Error getting proxy configuration");
			}
			return EMPTY_PROXIES;
		}

		try {
			if (isWinHttpInitialized && (proxyConfig.fAutoDetect || proxyConfig.lpszAutoConfigUrl != null)) {
				// Dynamic configuration
				if (Policy.DEBUG_SYSTEM_PROVIDERS) {
					Policy.debug("Dynamic proxy configuration");
				}
				ProxyData data = new ProxyData(IProxyData.HTTP_PROXY_TYPE, "", -1, false, "WINDOWS_IE");
				data.setDynamic(true);
				return new IProxyData[] { data };
			} else {
				// Static Configuration
				if (Policy.DEBUG_SYSTEM_PROVIDERS) {
					Policy.debug("Static proxy configuration");
				}
				if (proxyConfig.lpszProxy != null) {
					StaticProxyConfig staticProxyConfig = getStaticConfig(proxyConfig.getProxy(),
							proxyConfig.getProxyBypass());
					return staticProxyConfig.getProxyData();
				}
			}
		} finally {
			proxyConfig.free();
		}
		// Default configuration direct connection
		if (Policy.DEBUG_SYSTEM_PROVIDERS) {
			Policy.debug("No proxy configuration");
		}
		return EMPTY_PROXIES;
	}

	@Override
	protected String[] getNonProxiedHosts() {
		WinHttp.WinHttpCurrentUserIEProxyConfig proxyConfig = getProxyConfig();
		if (proxyConfig == null) {
			// No configuration
			return null;
		}

		try {
			if (isWinHttpInitialized && (proxyConfig.fAutoDetect || proxyConfig.lpszAutoConfigUrl != null)) {
				// Dynamic configuration
				if (Policy.DEBUG_SYSTEM_PROVIDERS) {
					Policy.debug("Dynamic proxy configuration");
				}
				return null;
			} else if (proxyConfig.lpszProxy != null) {
				// Static configuration
				if (Policy.DEBUG_SYSTEM_PROVIDERS) {
					Policy.debug("Static proxy configuration");
				}
				StaticProxyConfig staticProxyConfig = getStaticConfig(proxyConfig.getProxy(),
						proxyConfig.getProxyBypass());
				return staticProxyConfig.getNonProxiedHosts();
			}
		} finally {
			proxyConfig.free();
		}
		if (Policy.DEBUG_SYSTEM_PROVIDERS) {
			Policy.debug("No proxy configuration");
		}
		return null;
	}

	private void initialize() {
		try {
			hHttpSession = fWinHttp.WinHttpOpen(USER_AGENT, WinHttp.ACCESS_TYPE_NO_PROXY, WinHttp.NO_PROXY_NAME,
					WinHttp.NO_PROXY_BYPASS, 0);
			isWinHttpInitialized = true;
		} catch (LastErrorException e) {
			isWinHttpInitialized = false;
			Activator.logError("Problem during initializing WinHTTP session:" + formatMessage(e.getErrorCode()), null);
		}

	}

	private WinHttp.WinHttpCurrentUserIEProxyConfig getProxyConfig() {
		if (isWinHttpLoaded) {
			WinHttp.WinHttpCurrentUserIEProxyConfig proxyConfig = new WinHttp.WinHttpCurrentUserIEProxyConfig();
			try {
				fWinHttp.WinHttpGetIEProxyConfigForCurrentUser(proxyConfig);
				return proxyConfig;
			} catch (LastErrorException e) {
				proxyConfig.free();
				Activator.logError("Problem during loading proxy configuration: " + formatMessage(e.getErrorCode()),
						null);
			}
		}
		return null;
	}

	private StaticProxyConfig getStaticConfig(String proxyString, String proxyBypassString) {
		if (fStaticProxyConfig == null || !StringUtil.equals(fProxiesString, proxyString)
				|| !StringUtil.equals(fProxyBypassString, proxyBypassString)) {
			fStaticProxyConfig = new StaticProxyConfig(proxyString, proxyBypassString);
			fProxiesString = proxyString;
			fProxyBypassString = proxyBypassString;
		}
		return fStaticProxyConfig;
	}

	/**
	 * https://docs.microsoft.com/en-us/windows/win32/winhttp/autoproxy-cache To
	 * improve performance when WinHTTP uses the out-of-process resolver, it is
	 * necessary to first try resolving with fAutoLogonIfChallenged set to false.
	 */
	private static boolean getProxyForUrl(Pointer hSession, String lpcwszUrl,
			WinHttp.WinHttpAutoProxyOptions pAutoProxyOptions, WinHttp.WinHttpProxyInfo pProxyInfo)
			throws LastErrorException {
		try {
			return fWinHttp.WinHttpGetProxyForUrl(hSession, lpcwszUrl, pAutoProxyOptions, pProxyInfo);
		} catch (LastErrorException e) {
			if (e.getErrorCode() == WinHttp.ERROR_LOGIN_FAILURE) {
				pAutoProxyOptions.fAutoLogonIfChallenged = true;
				return fWinHttp.WinHttpGetProxyForUrl(hSession, lpcwszUrl, pAutoProxyOptions, pProxyInfo);
			}
			throw e;
		}
	}

	private static IProxyData[] toArray(List<IProxyData> proxies) {
		return proxies.toArray(new IProxyData[proxies.size()]);
	}

	private interface WinHttp extends StdCallLibrary {
		// WinHttp error codes
		int ERROR_BASE = 12000;
		int ERROR_LOGIN_FAILURE = ERROR_BASE + 15;
		int ERROR_LAST = ERROR_BASE + 186;

		// Values for WinHttpOpen dwAccessType
		int ACCESS_TYPE_NO_PROXY = 1;

		// Prettifiers for optional parameters WinHttpOpen pszProxy and pszProxyBypass
		String NO_PROXY_NAME = null;
		String NO_PROXY_BYPASS = null;

		// Flags for WINHTTP_AUTOPROXY_OPTIONS::dwFlags
		int AUTOPROXY_AUTO_DETECT = 0x00000001;
		int AUTOPROXY_CONFIG_URL = 0x00000002;

		// Flags for WINHTTP_AUTOPROXY_OPTIONS::dwAutoDetectFlags
		int AUTO_DETECT_TYPE_DHCP = 0x00000001;
		int AUTO_DETECT_TYPE_DNS_A = 0x00000002;

		boolean WinHttpCloseHandle(Pointer hInternet) throws LastErrorException;

		Pointer WinHttpOpen(String pszAgent, int dwAccessType, String pszProxy, String pszProxyByPass, int dwFlags)
				throws LastErrorException;

		boolean WinHttpGetIEProxyConfigForCurrentUser(WinHttpCurrentUserIEProxyConfig pProxyConfig)
				throws LastErrorException;

		boolean WinHttpGetProxyForUrl(Pointer hSession, String lpcwszUrl, WinHttpAutoProxyOptions pAutoProxyOptions,
				WinHttpProxyInfo pProxyInfo) throws LastErrorException;

		@FieldOrder({ "dwFlags", "dwAutoDetectFlags", "lpszAutoConfigUrl", "lpvReserved", "dwReserved",
				"fAutoLogonIfChallenged" })
		@SuppressWarnings("unused")
		static class WinHttpAutoProxyOptions extends Structure {
			public int dwFlags;
			public int dwAutoDetectFlags;
			public String lpszAutoConfigUrl;
			public Pointer lpvReserved;
			public int dwReserved;
			public boolean fAutoLogonIfChallenged;
		}

		@FieldOrder({ "dwAccessType", "lpszProxy", "lpszProxyBypass" })
		@SuppressWarnings("unused")
		static class WinHttpProxyInfo extends Structure {
			public int dwAccessType;
			public LPWSTR lpszProxy;
			public LPWSTR lpszProxyBypass;

			public String getProxy() {
				return lpszProxy == null ? null : lpszProxy.getValue();
			}

			public String getProxyBypass() {
				return lpszProxyBypass == null ? null : lpszProxyBypass.getValue();
			}

			public void free() {
				if (lpszProxy != null) {
					Kernel32Util.freeGlobalMemory(lpszProxy.getPointer());
				}
				if (lpszProxyBypass != null) {
					Kernel32Util.freeGlobalMemory(lpszProxyBypass.getPointer());
				}
			}
		}

		@FieldOrder({ "fAutoDetect", "lpszAutoConfigUrl", "lpszProxy", "lpszProxyBypass" })
		static class WinHttpCurrentUserIEProxyConfig extends Structure {
			public boolean fAutoDetect;
			public LPWSTR lpszAutoConfigUrl;
			public LPWSTR lpszProxy;
			public LPWSTR lpszProxyBypass;

			public String getAutoConfigUrl() {
				return lpszAutoConfigUrl == null ? null : lpszAutoConfigUrl.getValue();
			}

			public String getProxy() {
				return lpszProxy == null ? null : lpszProxy.getValue();
			}

			public String getProxyBypass() {
				return lpszProxyBypass == null ? null : lpszProxyBypass.getValue();
			}

			public void free() {
				if (lpszAutoConfigUrl != null) {
					Kernel32Util.freeGlobalMemory(lpszAutoConfigUrl.getPointer());
				}
				if (lpszProxy != null) {
					Kernel32Util.freeGlobalMemory(lpszProxy.getPointer());
				}
				if (lpszProxyBypass != null) {
					Kernel32Util.freeGlobalMemory(lpszProxyBypass.getPointer());
				}
			}
		}
	}

	private static String formatMessage(int code) {
		if (code >= WinHttp.ERROR_BASE && code <= WinHttp.ERROR_LAST) {
			HMODULE hmodule = Kernel32.INSTANCE.GetModuleHandle(System.mapLibraryName(LIBRARY_NAME));

			if (hmodule == null) {
				return "Error code " + code + "; No error message due to failure of 'GetModuleHandle("
						+ System.mapLibraryName(LIBRARY_NAME) + ")'.";
			} else {
				PointerByReference msgBuf = new PointerByReference();
				int size = Kernel32.INSTANCE.FormatMessage(
						WinBase.FORMAT_MESSAGE_ALLOCATE_BUFFER | WinBase.FORMAT_MESSAGE_FROM_HMODULE
								| WinBase.FORMAT_MESSAGE_IGNORE_INSERTS,
						hmodule.getPointer(), code, WinNT.LANG_USER_DEFAULT, msgBuf, 0, null);
				if (size == 0) {
					return "Error code " + code + "; No error message due to error " + Native.getLastError();
				}

				Pointer ptr = msgBuf.getValue();
				try {
					String str = ptr.getWideString(0);
					return str.trim();
				} finally {
					Kernel32Util.freeLocalMemory(ptr);
				}
			}
		}

		try {
			return Kernel32Util.formatMessage(code);
		} catch (LastErrorException e) {
			return "Error code " + code + "; No error message due to error " + e.getErrorCode();
		}
	}

}
