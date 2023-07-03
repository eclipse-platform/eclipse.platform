/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
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

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.IHelpBaseConstants;

public class RemotePreferenceStore {

	private static String namePreference;
	private static String hostPreference;
	private static String pathPreference;
	private static String protocolPreference;
	private static String portPreference;
	private static String icEnabledPreference;
	private static String helpOn;
	private static String defaultPort;
	private static String remoteHelpPreferred;
	private static String pageNotFound;

	public static void savePreferences() throws Exception {
		namePreference = Platform.getPreferencesService().getString
		 (HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_NAME,
					"", null);
		hostPreference = Platform.getPreferencesService().getString
		 (HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_HOST,
					"", null);
		pathPreference = Platform.getPreferencesService().getString
		 (HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_PATH,
					"", null);
		protocolPreference = Platform.getPreferencesService().getString
		 (HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_PROTOCOL,
					"", null);
		portPreference = Platform.getPreferencesService().getString
		 (HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_PORT,
					"", null);
		icEnabledPreference = Platform.getPreferencesService().getString
		 (HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_ICEnabled,
					"", null);
		defaultPort = Platform.getPreferencesService().getString
		 (HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_DEFAULT_PORT,
					"", null);
		helpOn = Platform.getPreferencesService().getString
		 (HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_ON,
					"", null);
		remoteHelpPreferred = Platform.getPreferencesService().getString
		 (HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_PREFERRED,
					"", null);
		pageNotFound = Platform.getPreferencesService().getString
					 (HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_PAGE_NOT_FOUND,
								"", null);
	}

	public static void restorePreferences() throws Exception {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_NAME, namePreference);
		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_HOST, hostPreference);
		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_PATH, pathPreference);
		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_PROTOCOL, protocolPreference);
		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_PORT, portPreference);
		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_DEFAULT_PORT, defaultPort);
		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_ON, helpOn);
		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_ICEnabled, icEnabledPreference);
		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_PREFERRED, remoteHelpPreferred);
		prefs.put(IHelpBaseConstants.P_PAGE_NOT_FOUND, pageNotFound);
	}

	public static void setMockRemoteServer() throws Exception {
		TestServerManager.start("ua.test", 0);
		RemotePreferenceTest.setPreference("remoteHelpOn", "true");
		RemotePreferenceTest.setPreference("remoteHelpHost", "localhost");
		RemotePreferenceTest.setPreference("remoteHelpPath", "/help");
		RemotePreferenceTest.setPreference("remoteHelpProtocol", "http");
		RemotePreferenceTest.setPreference("remoteHelpUseDefaultPort", "");
		RemotePreferenceTest.setPreference("remoteHelpPort", "" + TestServerManager.getPort(0));
		RemotePreferenceTest.setPreference("remoteHelpName", "uatest");
		RemotePreferenceTest.setPreference("remoteHelpICEnabled", "true");
		RemotePreferenceTest.setPreference("remoteHelpICContributed", "false");
	}

	public static void setTwoMockRemoteServers() throws Exception {
		TestServerManager.start("ua.test", 0);
		TestServerManager.start("ua.test2", 1);
		RemotePreferenceTest.setPreference("remoteHelpOn", "true");
		RemotePreferenceTest.setPreference("remoteHelpHost", "localhost,localhost");
		RemotePreferenceTest.setPreference("remoteHelpPath", "/help,/help");
		RemotePreferenceTest.setPreference("remoteHelpProtocol", "http,http");
		RemotePreferenceTest.setPreference("remoteHelpUseDefaultPort", ",");
		RemotePreferenceTest.setPreference("remoteHelpPort", ""
				+ TestServerManager.getPort(0) + ',' + TestServerManager.getPort(1));
		RemotePreferenceTest.setPreference("remoteHelpName", "uatest,uatest2");
		RemotePreferenceTest.setPreference("remoteHelpICEnabled", "true,true");
		RemotePreferenceTest.setPreference("remoteHelpICContributed", "false,false");
	}
	public static void setTwoMockRemoteServersReversePriority() throws Exception {
		TestServerManager.start("ua.test", 0);
		TestServerManager.start("ua.test2", 1);
		RemotePreferenceTest.setPreference("remoteHelpOn", "true");
		RemotePreferenceTest.setPreference("remoteHelpHost", "localhost,localhost");
		RemotePreferenceTest.setPreference("remoteHelpPath", "/help,/help");
		RemotePreferenceTest.setPreference("remoteHelpProtocol", "http,http");
		RemotePreferenceTest.setPreference("remoteHelpUseDefaultPort", "true,true");
		RemotePreferenceTest.setPreference("remoteHelpPort", ""
				+ TestServerManager.getPort(1) + ',' + TestServerManager.getPort(0));
		RemotePreferenceTest.setPreference("remoteHelpName", "uatest,uatest2");
		RemotePreferenceTest.setPreference("remoteHelpICEnabled", "true,true");
		RemotePreferenceTest.setPreference("remoteHelpICContributed", "false,false");
	}

	public static void disableRemoteHelp() throws Exception {
		RemotePreferenceTest.setPreference("remoteHelpOn", "false");
	}

	public static void disableErrorPage() throws Exception {
		RemotePreferenceTest.setPreference(IHelpBaseConstants.P_PAGE_NOT_FOUND, "");
	}

	public static void setMockLocalPriority() throws Exception {
		RemotePreferenceTest.setPreference("remoteHelpOn", "true");
		RemotePreferenceTest.setPreference("remoteHelpPreferred", "false");
	}

	public static void setMockRemotePriority() throws Exception {
		RemotePreferenceTest.setPreference("remoteHelpOn", "true");
		RemotePreferenceTest.setPreference("remoteHelpPreferred", "true");
	}
}
