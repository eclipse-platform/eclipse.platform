/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.IHelpBaseConstants;
import org.eclipse.help.internal.base.remote.PreferenceFileHandler;
import org.eclipse.help.internal.base.remote.RemoteIC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RemotePreferenceTest {

	public static void setPreference(String name, String value) {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
		prefs.put(name, value);
	}

	@BeforeEach
	public void setUp() throws Exception {
		RemotePreferenceStore.savePreferences();
	}

	@AfterEach
	public void tearDown() throws Exception {
		RemotePreferenceStore.restorePreferences();
	}

	private void setToDefault(String preference) {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
		prefs.remove(preference);
	}

	@Test
	public void testDefaults() {
		setToDefault(IHelpBaseConstants.P_KEY_REMOTE_HELP_NAME);
		setToDefault(IHelpBaseConstants.P_KEY_REMOTE_HELP_HOST);
		setToDefault(IHelpBaseConstants.P_KEY_REMOTE_HELP_PATH);
		setToDefault(IHelpBaseConstants.P_KEY_REMOTE_HELP_PORT);
		setToDefault(IHelpBaseConstants.P_KEY_REMOTE_HELP_ICEnabled);
		setToDefault(IHelpBaseConstants.P_KEY_REMOTE_HELP_ON);
		setToDefault(IHelpBaseConstants.P_KEY_REMOTE_HELP_DEFAULT_PORT);
		PreferenceFileHandler handler = new PreferenceFileHandler();
		assertEquals(0, handler.getTotalRemoteInfocenters());
		assertThat(handler.getEnabledEntries()).isEmpty();
	}

	/*
	 * Test the default settings from Eclipse 3.3
	 */
	@Test
	public void test33Defaults() {
		setPreference("remoteHelpOn", "false");
		setPreference("remoteHelpHost", "");
		setPreference("remoteHelpPath", "/help");
		setPreference("remoteHelpUseDefaultPort", "true");
		setPreference("remoteHelpPort", "80");
		setPreference("remoteHelpName", "");
		setPreference("remoteHelpICEnabled", "");
		setPreference("remoteHelpICContributed", "");
		PreferenceFileHandler handler = new PreferenceFileHandler();
		assertEquals(0, handler.getTotalRemoteInfocenters());
		assertThat(handler.getEnabledEntries()).isEmpty();
	}

	/*
	 * Test settings which worked in Eclipse 3.3 to read a remote infocenter
	 */
	@Test
	public void test33Remote() {
		setPreference("remoteHelpOn", "true");
		setPreference("remoteHelpHost", "localhost");
		setPreference("remoteHelpPath", "/help");
		setPreference("remoteHelpUseDefaultPort", "false");
		setPreference("remoteHelpPort", "8081");
		setPreference("remoteHelpName", "");
		setPreference("remoteHelpICEnabled", "");
		setPreference("remoteHelpICContributed", "");
		PreferenceFileHandler handler = new PreferenceFileHandler();
		assertEquals(1, handler.getTotalRemoteInfocenters());
		assertThat(handler.getEnabledEntries()).hasSize(1);
	}

	@Test
	public void testZeroRemoteInfocenters() {
		setPreference("remoteHelpOn", "true");
		setPreference("remoteHelpHost", "");
		setPreference("remoteHelpPath", "");
		setPreference("remoteHelpUseDefaultPort", "");
		setPreference("remoteHelpPort", "");
		setPreference("remoteHelpName", "");
		setPreference("remoteHelpICEnabled", "");
		setPreference("remoteHelpICContributed", "");
		PreferenceFileHandler handler = new PreferenceFileHandler();
		assertEquals(0, handler.getTotalRemoteInfocenters());
		assertThat(handler.getHostEntries()).isEmpty();
		assertThat(handler.getPortEntries()).isEmpty();
		assertThat(handler.getEnabledEntries()).isEmpty();
		assertThat(handler.getPathEntries()).isEmpty();
	}

	@Test
	public void testOneRemoteInfocenter() {
		setPreference("remoteHelpOn", "true");
		setPreference("remoteHelpHost", "localhost");
		setPreference("remoteHelpPath", "/help");
		setPreference("remoteHelpUseDefaultPort", "");
		setPreference("remoteHelpPort", "8081");
		setPreference("remoteHelpName", "local");
		setPreference("remoteHelpICEnabled", "true");
		setPreference("remoteHelpICContributed", "false");
		PreferenceFileHandler handler = new PreferenceFileHandler();
		assertEquals(1, handler.getTotalRemoteInfocenters());
		assertThat(handler.getHostEntries()).hasSize(1);
		assertEquals("localhost", handler.getHostEntries()[0]);
		assertThat(handler.getPortEntries()).hasSize(1);
		assertEquals("8081", handler.getPortEntries()[0]);
		assertThat(handler.getEnabledEntries()).hasSize(1);
		assertEquals("true", handler.getEnabledEntries()[0].toLowerCase());
		assertThat(handler.getPathEntries()).hasSize(1);
		assertEquals("/help", handler.getPathEntries()[0].toLowerCase());
	}

	@Test
	public void testTwoRemoteInfocenters() {
		setPreference("remoteHelpHost", "localhost,www.eclipse.org");
		setPreference("remoteHelpPath", "/help,/eclipse/help");
		setPreference("remoteHelpUseDefaultPort", "");
		setPreference("remoteHelpPort", "8081,8082");
		setPreference("remoteHelpName", "local,remote");
		setPreference("remoteHelpICEnabled", "true,false");
		setPreference("remoteHelpICContributed", "false,false");
		PreferenceFileHandler handler = new PreferenceFileHandler();
		assertEquals(2, handler.getTotalRemoteInfocenters());
		assertThat(handler.getHostEntries()).hasSize(2);
		assertEquals("localhost", handler.getHostEntries()[0]);
		assertEquals("www.eclipse.org", handler.getHostEntries()[1]);
		assertThat(handler.getPortEntries()).hasSize(2);
		assertEquals("8081", handler.getPortEntries()[0]);
		assertEquals("8082", handler.getPortEntries()[1]);
		assertThat(handler.getEnabledEntries()).hasSize(2);
		assertEquals("true", handler.getEnabledEntries()[0].toLowerCase());
		assertEquals("false", handler.getEnabledEntries()[1].toLowerCase());
		assertThat(handler.getPathEntries()).hasSize(2);
		assertEquals("/help", handler.getPathEntries()[0].toLowerCase());
		assertEquals("/eclipse/help", handler.getPathEntries()[1].toLowerCase());
	}

	@Test
	public void testOnePathTwoOfEverythingElse() {
		setPreference("remoteHelpOn", "true");
		setPreference("remoteHelpHost", "localhost");
		setPreference("remoteHelpPath", "/help,/nhelp");
		setPreference("remoteHelpUseDefaultPort", "true,false");
		setPreference("remoteHelpPort", "8081,8082");
		setPreference("remoteHelpName", "local,remote");
		setPreference("remoteHelpICEnabled", "true,false");
		setPreference("remoteHelpICContributed", "false,true");
		PreferenceFileHandler handler = new PreferenceFileHandler();
		assertEquals(1, handler.getTotalRemoteInfocenters());
		assertThat(handler.getHostEntries()).hasSize(1);
		assertEquals("localhost", handler.getHostEntries()[0]);
		assertThat(handler.getPortEntries()).hasSize(1);
		assertEquals("8081", handler.getPortEntries()[0]);
		assertThat(handler.getEnabledEntries()).hasSize(1);
		assertEquals("true", handler.getEnabledEntries()[0].toLowerCase());
		assertThat(handler.getPathEntries()).hasSize(1);
		assertEquals("/help", handler.getPathEntries()[0].toLowerCase());
	}

	@Test
	public void testPathOnly() {
		setPreference("remoteHelpOn", "true");
		setPreference("remoteHelpHost", "localhost");
		setPreference("remoteHelpPath", "");
		setPreference("remoteHelpUseDefaultPort", "");
		setPreference("remoteHelpPort", "");
		setPreference("remoteHelpName", "");
		setPreference("remoteHelpICEnabled", "");
		setPreference("remoteHelpICContributed", "");
		PreferenceFileHandler handler = new PreferenceFileHandler();
		assertEquals(1, handler.getTotalRemoteInfocenters());
		assertThat(handler.getHostEntries()).hasSize(1);
		assertEquals("localhost", handler.getHostEntries()[0]);
		assertThat(handler.getPortEntries()).hasSize(1);
		//assertEquals("80", handler.getPortEntries()[0]);
		assertThat(handler.getEnabledEntries()).hasSize(1);
		//assertEquals("true", handler.getEnabledEntries()[0].toLowerCase());
		assertThat(handler.getPathEntries()).hasSize(1);
		//assertEquals("/help", handler.getPathEntries()[0].toLowerCase());
	}

	@Test
	public void testWriteNoRemote() {
		PreferenceFileHandler.commitRemoteICs(new RemoteIC[0]);
		PreferenceFileHandler handler = new PreferenceFileHandler();
		assertEquals(0, handler.getTotalRemoteInfocenters());
		assertThat(handler.getHostEntries()).isEmpty();
		assertThat(handler.getPortEntries()).isEmpty();
		assertThat(handler.getEnabledEntries()).isEmpty();
		assertThat(handler.getPathEntries()).isEmpty();
	}

	@Test
	public void testWriteOneRemote() {
		RemoteIC[] ic = {new RemoteIC(true, "name", "host", "/help", "http","8080")};
		PreferenceFileHandler.commitRemoteICs(ic);
		PreferenceFileHandler handler = new PreferenceFileHandler();
		assertEquals(1, handler.getTotalRemoteInfocenters());
		assertThat(handler.getHostEntries()).hasSize(1);
		assertEquals("host", handler.getHostEntries()[0]);
		assertThat(handler.getPortEntries()).hasSize(1);
		assertEquals("8080", handler.getPortEntries()[0]);
		assertThat(handler.getEnabledEntries()).hasSize(1);
		assertEquals("true", handler.getEnabledEntries()[0].toLowerCase());
		assertThat(handler.getPathEntries()).hasSize(1);
		assertEquals("/help", handler.getPathEntries()[0].toLowerCase());
	}

	@Test
	public void testWriteTwoRemote() {
		RemoteIC[] ic = {new RemoteIC(true, "name", "host", "/help", "http", "8080"),
				new RemoteIC(false, "remote", "remotehost", "/help2", "http", "8081")};
		PreferenceFileHandler.commitRemoteICs(ic);
		PreferenceFileHandler handler = new PreferenceFileHandler();
		assertEquals(2, handler.getTotalRemoteInfocenters());
		assertThat(handler.getHostEntries()).hasSize(2);
		assertEquals("host", handler.getHostEntries()[0]);
		assertEquals("remotehost", handler.getHostEntries()[1]);
		assertThat(handler.getPortEntries()).hasSize(2);
		assertEquals("8080", handler.getPortEntries()[0]);
		assertEquals("8081", handler.getPortEntries()[1]);
		assertThat(handler.getEnabledEntries()).hasSize(2);
		assertEquals("true", handler.getEnabledEntries()[0].toLowerCase());
		assertEquals("false", handler.getEnabledEntries()[1].toLowerCase());
		assertThat(handler.getPathEntries()).hasSize(2);
		assertEquals("/help", handler.getPathEntries()[0].toLowerCase());
		assertEquals("/help2", handler.getPathEntries()[1].toLowerCase());
	}

}
