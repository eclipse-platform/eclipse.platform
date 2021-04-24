/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.internal.net.ProxySelector;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.junit.*;

public class SystemProxyTest {

	private boolean isProxiesDefault;
	private boolean isSystemProxiesDefault;

	private Map<String, IProxyData> proxyDataMap = new HashMap<>();

	@Before
	public void setUp() throws Exception {
		isSystemProxiesDefault = isSystemProxiesEnabled();
		setSystemProxiesEnabled(true);
		isProxiesDefault = isProxiesEnabled();
		setProxiesEnabled(true);
	}

	@After
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

	private void assertProxyDataEqual(IProxyData actual, IProxyData expected) {
		ProxyData data = (ProxyData) actual;
		ProxyData expectedData = (ProxyData) expected;
		assertTrue(expectedData.getType() == data.getType());
		assertEquals(expectedData.getHost(), data.getHost());
		assertEquals(expectedData.getPort(), data.getPort());
		assertEquals(expectedData.getUserId(), data.getUserId());
		assertEquals(expectedData.getPassword(), data.getPassword());
		assertEquals(expectedData.isRequiresAuthentication(), data.isRequiresAuthentication());
		assertEquals(expectedData.getSource(), data.getSource());
	}

	private void checkGetProxyDataForHost() {
		checkGetProxyDataForHost("http://www.something.org");
		checkGetProxyDataForHost("https://www.something.org");
		checkGetProxyDataForHost("socks://www.something.org");
		checkGetProxyDataForHost("www.something.org");
	}

	private void checkGetProxyDataForHost(String host) {
		IProxyData[] proxiesData = getProxyManager().getProxyDataForHost(host);
		assertNotNull(proxiesData);

		Map<String, String> typeMap = new HashMap<>();
		for (IProxyData p : proxiesData) {
			assertProxyDataEqual(p, (IProxyData) proxyDataMap.get(p.getType()));
			typeMap.put(p.getType(), p.getType());
		}

		assertEquals(3, typeMap.size());
	}

	private void checkProxySelector() {
		IProxyData[] proxiesData = ProxySelector.getProxyData("Native");
		assertNotNull(proxiesData);

		Map<String, String> typeMap = new HashMap<>();
		for (IProxyData p : proxiesData) {
			assertProxyDataEqual(p, (IProxyData) proxyDataMap.get(p.getType()));
			typeMap.put(p.getType(), p.getType());
		}

		assertEquals(3, typeMap.size());
	}

	/**
	 * This test needs system env set. See {@link #initializeTestProxyData()} for
	 * values.
	 */
	@Test
	public void testGetProxyDataForHost_LinuxEnvSettings() {
		initializeTestProxyData("LINUX_ENV");
		checkGetProxyDataForHost();
	}

	/**
	 * This test needs system env set. See {@link #initializeTestProxyData()} for
	 * values.
	 */
	@Test
	public void testProxySelector_LinuxEnvSettings() {
		initializeTestProxyData("LINUX_ENV");
		checkProxySelector();
	}

	/**
	 * This test needs Gnome settings set. See {@link #initializeTestProxyData()}
	 * for values.
	 */
	@Test
	public void testGetProxyDataForHost_LinuxGnomeSettings() {
		initializeTestProxyData("LINUX_GNOME");
		checkGetProxyDataForHost();
	}

	/**
	 * This test needs Gnome settings set. See {@link #initializeTestProxyData()}
	 * for values.
	 */
	@Test
	public void testProxySelector_LinuxGnomeSettings() {
		initializeTestProxyData("LINUX_GNOME");
		checkProxySelector();
	}

	/**
	 * This test needs Windows IE settings manually set. See
	 * {@link #initializeTestProxyData()} for values.
	 */
	@Test
	public void testGetProxyDataForHost_WindowsIEManualSettings() {
		initializeTestProxyData("WINDOWS_IE");
		checkGetProxyDataForHost();
	}

	/**
	 * This test needs Windows IE settings manually set. See
	 * {@link #initializeTestProxyData()} for values.
	 */
	@Test
	public void testProxySelector_WindowsIEManualSettings() {
		initializeTestProxyData("WINDOWS_IE");
		checkProxySelector();
	}

	/**
	 * This test needs Windows IE settings manually set. See
	 * {@link #initializeTestProxyData()} for values. Additionally set
	 * <code>"eclipse.*;nonexisting.com;*.eclipse.org;www.*.com;*.test.*"</code> as
	 * proxy bypass in the IE settings.
	 *
	 * @throws URISyntaxException
	 */
	@Test
	public void testNonProxiedHosts_WindowsIEManualSettings() throws URISyntaxException {
		IProxyData[] proxiesData = getProxyManager().select(new URI("http://eclipse"));
		assertEquals(1, proxiesData.length);

		proxiesData = getProxyManager().select(new URI("http://eclipse.org/bugs"));
		assertEquals(0, proxiesData.length);

		proxiesData = getProxyManager().select(new URI("http://nonexisting.com"));
		assertEquals(0, proxiesData.length);

		proxiesData = getProxyManager().select(new URI("http://www.eclipse.org"));
		assertEquals(0, proxiesData.length);

		proxiesData = getProxyManager().select(new URI("http://www.myDomain.com"));
		assertEquals(0, proxiesData.length);

		proxiesData = getProxyManager().select(new URI("http://www.test.edu"));
		assertEquals(0, proxiesData.length);
	}

	private void initializeTestProxyData(String proxyDataSource) {
		proxyDataMap.put(IProxyData.HTTP_PROXY_TYPE,
				new ProxyData(IProxyData.HTTP_PROXY_TYPE, "127.0.0.1", 8081, false, proxyDataSource));
		proxyDataMap.put(IProxyData.HTTPS_PROXY_TYPE,
				new ProxyData(IProxyData.HTTPS_PROXY_TYPE, "127.0.0.2", 8082, false, proxyDataSource));
		proxyDataMap.put(IProxyData.SOCKS_PROXY_TYPE,
				new ProxyData(IProxyData.SOCKS_PROXY_TYPE, "127.0.0.3", 8083, false, proxyDataSource));
	}
}