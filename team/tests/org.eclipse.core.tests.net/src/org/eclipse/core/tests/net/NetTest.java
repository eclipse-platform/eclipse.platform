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
package org.eclipse.core.tests.net;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.internal.net.ProxyType;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class NetTest {

	private static final boolean BUG_338097 = true;

	private boolean isSetEnabled;
	private boolean isProxiesDefault;
	private boolean isSystemProxiesDefault;
	private final Map<String, IProxyData> dataCache = new HashMap<>();

	@BeforeEach
	public void setUp() throws Exception {
		isSystemProxiesDefault = isSystemProxiesEnabled();
		setSystemProxiesEnabled(false);
		isProxiesDefault = isProxiesEnabled();
		setProxiesEnabled(true);
		isSetEnabled = true;
		dataCache.clear();
		ProxyType.socksSystemPropertySetting = ProxyType.ALWAYS_SET;
	}

	@AfterEach
	public void tearDown() throws Exception {
		setProxiesEnabled(isProxiesDefault);
		setSystemProxiesEnabled(isSystemProxiesDefault);
		IProxyData[] data = getProxyManager().getProxyData();
		for (IProxyData proxyData : data) {
			proxyData.disable();
		}
		getProxyManager().setProxyData(data);
	}

	private IProxyService getProxyManager() {
		return Activator.getProxyService();
	}

	private void assertProxyDataEqual(IProxyData expected) {
		ProxyData expectedData = (ProxyData) expected;
		ProxyData data = (ProxyData) getProxyManager().getProxyData(expectedData.getType());
		assertEquals(expectedData.getType(), data.getType());
		assertEquals(expectedData.getHost(), data.getHost());
		assertEquals(expectedData.getPort(), data.getPort());
		assertEquals(expectedData.getUserId(), data.getUserId());
		assertEquals(expectedData.getPassword(), data.getPassword());
		assertEquals(expectedData.isRequiresAuthentication(), data.isRequiresAuthentication());
		assertEquals(expectedData.getSource(), data.getSource());
		assertSystemPropertiesMatch(data);
	}

	public void assertSystemPropertiesMatch(IProxyData proxyData) {
		if (proxyData.getType().equals(IProxyData.HTTP_PROXY_TYPE)) {
			assertHttpSystemProperties(proxyData, "http");
		} else if (proxyData.getType().equals(IProxyData.HTTPS_PROXY_TYPE)) {
			assertHttpSystemProperties(proxyData, "https");
		} else if (proxyData.getType().equals(IProxyData.SOCKS_PROXY_TYPE)) {
			assertSocksSystemProperties(proxyData);
		}
	}

	private void assertHttpSystemProperties(IProxyData proxyData, String keyPrefix) {
		Properties sysProps = System.getProperties();

		if (this.getProxyManager().isProxiesEnabled()) {
			boolean isSet = Boolean.getBoolean(keyPrefix + ".proxySet");
			assertEquals(proxyData.getHost() != null, isSet); // $NON-NLS-1$
			assertEquals(proxyData.getHost(), sysProps.get(keyPrefix + ".proxyHost")); //$NON-NLS-1$
			String portString = (String) sysProps.get(keyPrefix + ".proxyPort"); //$NON-NLS-1$
			int port = -1;
			if (portString != null)
				port = Integer.parseInt(portString);
			assertEquals(proxyData.getPort(), port);
			if (isSet)
				assertEquals(ProxyType.convertHostsToPropertyString(this.getProxyManager().getNonProxiedHosts()),
						sysProps.get(keyPrefix + ".nonProxyHosts")); //$NON-NLS-1$
			else
				assertNull(sysProps.get(keyPrefix + ".nonProxyHosts"));
			assertEquals(proxyData.getUserId(), sysProps.get(keyPrefix + ".proxyUser")); //$NON-NLS-1$
			assertEquals(proxyData.getUserId(), sysProps.get(keyPrefix + ".proxyUserName")); //$NON-NLS-1$
			assertEquals(proxyData.getPassword(), sysProps.get(keyPrefix + ".proxyPassword")); //$NON-NLS-1$
		} else {
			assertNull(sysProps.get(keyPrefix + ".proxySet"));
			assertNull(sysProps.get(keyPrefix + ".proxyHost"));
			assertNull(sysProps.get(keyPrefix + ".proxyPort"));
			assertNull(sysProps.get(keyPrefix + ".nonProxyHosts"));
			assertNull(sysProps.get(keyPrefix + ".proxyUser"));
			assertNull(sysProps.get(keyPrefix + ".proxyUserName"));
			assertNull(sysProps.get(keyPrefix + ".proxyPassword"));
		}
	}

	private void assertSocksSystemProperties(IProxyData proxyData) {
		Properties sysProps = System.getProperties();
		if (this.getProxyManager().isProxiesEnabled()) {
			assertEquals(proxyData.getHost(), sysProps.get("socksProxyHost")); //$NON-NLS-1$
			String portString = (String) sysProps.get("socksProxyPort"); //$NON-NLS-1$
			int port = -1;
			if (portString != null)
				port = Integer.parseInt(portString);
			assertEquals(proxyData.getPort(), port);
		} else {
			assertNull(sysProps.get("socksProxyHost"));
			assertNull(sysProps.get("socksProxyPort"));
		}
	}

	private IProxyData getProxyData(String type) {
		IProxyData data = dataCache.get(type);
		if (data == null) {
			data = this.getProxyManager().getProxyData(type);
			assertProxyDataEqual(data);
		}
		return data;
	}

	private void setProxyData(IProxyData data) throws CoreException {
		if (isSetEnabled) {
			this.getProxyManager().setProxyData(new IProxyData[] { data });
			assertProxyDataEqual(data);
		} else {
			dataCache.put(data.getType(), data);
		}
	}

	private void disableProxy(IProxyData proxyData) throws CoreException {
		proxyData.disable();
		setProxyData(proxyData);
	}

	private void changeProxyData(IProxyData oldData, IProxyData data) throws CoreException {
		// Make sure that setting the host doesn't change the persisted settings
		if (isSetEnabled)
			assertProxyDataEqual(oldData);
		// Now set it in the manager and assert that it is set
		setProxyData(data);
	}

	private void setHost(String type) throws CoreException {
		String host = "www.eclipse.org";
		setHost(type, host);
	}

	private void setHost(String type, String host) throws CoreException {
		IProxyData data = getProxyData(type);
		IProxyData oldData = getProxyData(type);
		data.setHost(host);
		changeProxyData(oldData, data);
	}

	private void setPort(String type, int port) throws CoreException {
		IProxyData data = getProxyData(type);
		IProxyData oldData = getProxyData(type);
		data.setPort(port);
		changeProxyData(oldData, data);
	}

	private void setUser(String type, String user, String password) throws CoreException {
		IProxyData data = getProxyData(type);
		IProxyData oldData = getProxyData(type);
		data.setUserid(user);
		data.setPassword(password);
		changeProxyData(oldData, data);
	}

	private void setDataTest(String type) throws CoreException {
		setHost(type, "www.eclipse.org");
		setPort(type, 1024);
		setUser(type, "me", "passw0rd");
	}

	private boolean isProxiesEnabled() {
		return this.getProxyManager().isProxiesEnabled();
	}

	private boolean isSystemProxiesEnabled() {
		return this.getProxyManager().isProxiesEnabled();
	}

	private void setProxiesEnabled(boolean enabled) {
		this.getProxyManager().setProxiesEnabled(enabled);
		if (enabled && this.getProxyManager().isSystemProxiesEnabled() && !this.getProxyManager().hasSystemProxies()) {
			assertEquals(false, this.getProxyManager().isProxiesEnabled());
		} else {
			assertEquals(enabled, this.getProxyManager().isProxiesEnabled());
		}
	}

	private void setSystemProxiesEnabled(boolean enabled) {
		this.getProxyManager().setSystemProxiesEnabled(enabled);
		assertEquals(enabled, this.getProxyManager().isSystemProxiesEnabled());
	}

	private void delaySettingData() {
		isSetEnabled = false;
	}

	private void performSettingData() throws CoreException {
		IProxyData[] data = dataCache.values().toArray(new IProxyData[dataCache.size()]);
		this.getProxyManager().setProxyData(data);
		for (IProxyData proxyData : data) {
			assertProxyDataEqual(proxyData);
		}
		isSetEnabled = true;
		dataCache.clear();
	}

	@Test
	public void testIndividualSetAndClear() throws CoreException {
		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);
		IProxyData[] data = this.getProxyManager().getProxyData();
		for (IProxyData proxyData : data) {
			disableProxy(proxyData);
		}
	}

	@Test
	public void testAllSetAndClear() throws CoreException {
		delaySettingData();
		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);
		performSettingData();

		delaySettingData();
		IProxyData[] data = this.getProxyManager().getProxyData();
		for (IProxyData proxyData : data) {
			disableProxy(proxyData);
		}
		performSettingData();
	}

	@Test
	public void testSetWhenDisabled() throws CoreException {
		setProxiesEnabled(false);
		String type = IProxyData.HTTP_PROXY_TYPE;
		setHost(type);
	}

	@Test
	public void testDisableAfterSet() throws CoreException {
		String type = IProxyData.HTTP_PROXY_TYPE;
		setHost(type);
		IProxyData data = this.getProxyManager().getProxyData(type);
		setProxiesEnabled(false);
		assertProxyDataEqual(data);
	}

	@Test
	@Disabled("Disabled due to bug 403311")
	@SuppressWarnings("deprecation")
	public void _testSimpleHost() throws CoreException {

		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);

		assertThat(this.getProxyManager().getProxyDataForHost("www.randomhot.com")).hasSize(3);
		assertThat(this.getProxyManager().getProxyDataForHost("www.randomhot.com", IProxyData.HTTP_PROXY_TYPE))
				.isNotNull();
		assertThat(this.getProxyManager().getProxyDataForHost("localhost")).isEmpty();
		assertThat(this.getProxyManager().getProxyDataForHost("localhost", IProxyData.HTTP_PROXY_TYPE)).isNull();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testHostPattern() throws CoreException {
		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);

		String[] oldHosts = this.getProxyManager().getNonProxiedHosts();
		this.getProxyManager().setNonProxiedHosts(new String[] { "*ignore.com" });

		assertThat(this.getProxyManager().getProxyDataForHost("www.randomhot.com")).hasSize(3);
		assertThat(this.getProxyManager().getProxyDataForHost("www.randomhot.com", IProxyData.HTTP_PROXY_TYPE))
				.isNotNull();
		assertThat(this.getProxyManager().getProxyDataForHost("www.ignore.com")).isEmpty();
		assertThat(this.getProxyManager().getProxyDataForHost("www.ignore.com", IProxyData.HTTP_PROXY_TYPE)).isNull();
		assertThat(this.getProxyManager().getProxyDataForHost("ignore.com")).isEmpty();
		assertThat(this.getProxyManager().getProxyDataForHost("ignore.com", IProxyData.HTTP_PROXY_TYPE)).isNull();

		this.getProxyManager().setNonProxiedHosts(oldHosts);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testHostPatternBug505906() throws CoreException {
		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);

		String[] oldHosts = this.getProxyManager().getNonProxiedHosts();
		this.getProxyManager().setNonProxiedHosts(new String[] { "ignore.com" });

		assertThat(this.getProxyManager().getProxyDataForHost("ignore.com.randomhot.com")).hasSize(3);
		assertThat(this.getProxyManager().getProxyDataForHost("ignore.com.randomhot.com", IProxyData.HTTP_PROXY_TYPE))
				.isNotNull();
		assertThat(this.getProxyManager().getProxyDataForHost("www.ignore.com")).isEmpty();
		assertThat(this.getProxyManager().getProxyDataForHost("www.ignore.com", IProxyData.HTTP_PROXY_TYPE)).isNull();
		assertThat(this.getProxyManager().getProxyDataForHost("ignore.com")).isEmpty();
		assertThat(this.getProxyManager().getProxyDataForHost("ignore.com", IProxyData.HTTP_PROXY_TYPE)).isNull();

		this.getProxyManager().setNonProxiedHosts(oldHosts);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testBug238796() throws CoreException {
		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);

		String[] oldHosts = this.getProxyManager().getNonProxiedHosts();

		this.getProxyManager().setNonProxiedHosts(new String[] { "nonexisting.com" });

		assertThat(this.getProxyManager().getProxyDataForHost("NONEXISTING.COM")).isEmpty();
		assertThat(this.getProxyManager().getProxyDataForHost("NONEXISTING.COM", IProxyData.HTTP_PROXY_TYPE)).isNull();

		this.getProxyManager().setNonProxiedHosts(oldHosts);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testBug247408() throws CoreException, URISyntaxException {
		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);

		IProxyData data1 = this.getProxyManager().getProxyDataForHost("randomhost.com", IProxyData.HTTP_PROXY_TYPE);
		IProxyData[] data2 = this.getProxyManager().select(new URI("http://randomhost.com"));
		assertThat(data2).singleElement().isEqualTo(data1);

		IProxyData data3 = this.getProxyManager().getProxyDataForHost("randomhost.com", null);
		IProxyData[] data4 = this.getProxyManager().select(new URI(null, "randomhost.com", null, null));
		assertThat(data3).isNull();
		assertThat(data4).isEmpty();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testBug255981() throws CoreException, URISyntaxException {
		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);

		this.getProxyManager().setProxiesEnabled(false);

		IProxyData data = this.getProxyManager().getProxyDataForHost("randomhost.com", IProxyData.HTTP_PROXY_TYPE);
		assertThat(data).isNull();

		IProxyData[] data2 = this.getProxyManager().select(new URI("http://randomhost.com"));
		assertThat(data2).isEmpty();

		IProxyData data3[] = this.getProxyManager().getProxyDataForHost("http://randomhost.com");
		assertThat(data3).isEmpty();
	}

	@Test
	public void testBug257503() throws CoreException {
		if (BUG_338097)
			return;

		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);

		validateSystemProperties(true);
		setSystemProxiesEnabled(true);
		validateSystemProperties(false);
		setSystemProxiesEnabled(false);
		validateSystemProperties(true);

	}

	@Test
	public void testNonProxyHosts() throws CoreException {
		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);

		String[] oldHosts = this.getProxyManager().getNonProxiedHosts();

		// add new host to the nonProxiedHosts list
		String testHost = "bug284540.com";
		ArrayList<String> hostsList = new ArrayList<>();
		hostsList.addAll(Arrays.asList(oldHosts));
		hostsList.add(testHost);
		String[] newHosts = hostsList.toArray(new String[] {});

		this.getProxyManager().setNonProxiedHosts(newHosts);

		// check if system properties are updated
		String sysPropNonProxyHosts = System.getProperty("http.nonProxyHosts");
		assertThat(sysPropNonProxyHosts).contains(testHost);

		this.getProxyManager().setNonProxiedHosts(oldHosts);
	}

	private void validateSystemProperties(boolean present) {
		validateProperty("http.proxySet", "true", present);
		validateProperty("http.proxyHost", "www.eclipse.org", present);
		validateProperty("http.proxyPort", "1024", present);
		validateProperty("http.proxyUser", "me", present);
		validateProperty("http.proxyUserName", "me", present);
		validateProperty("http.proxyPassword", "passw0rd", present);

		validateProperty("https.proxySet", "true", present);
		validateProperty("https.proxyHost", "www.eclipse.org", present);
		validateProperty("https.proxyPort", "1024", present);
		validateProperty("https.proxyUser", "me", present);
		validateProperty("https.proxyUserName", "me", present);
		validateProperty("https.proxyPassword", "passw0rd", present);

		validateProperty("socksProxyHost", "www.eclipse.org", present);
		validateProperty("socksProxyPort", "1024", present);
	}

	private void validateProperty(String key, String expected, boolean equals) {
		String actual = System.getProperties().getProperty(key);
		if (equals) {
			assertThat(actual).isEqualTo(expected);
		} else {
			assertThat(actual).isNotEqualTo(expected);
		}
	}

}
