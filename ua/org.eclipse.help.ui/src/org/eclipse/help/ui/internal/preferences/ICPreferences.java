/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/

package org.eclipse.help.ui.internal.preferences;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.IHelpBaseConstants;
import org.osgi.service.prefs.BackingStoreException;


public class ICPreferences {

	public final static String DELIMITER = ","; //$NON-NLS-1$


	public static void setICs(List<IC> ics)
	{
		String name = "", host = "", path = "", protocol="", port = "", enabled = ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

		for (int i=0;i<ics.size();i++)
		{
			name += ics.get(i).getName() + DELIMITER;
			protocol += ics.get(i).getProtocol() + DELIMITER;
			host += ics.get(i).getHost() + DELIMITER;
			port += ics.get(i).getPort() + DELIMITER;
			path += ics.get(i).getPath() + DELIMITER;
			enabled += ics.get(i).isEnabled() + DELIMITER;
		}

		// Remove trailing commas
		if(ics.size()!=0)
		{
			name = name.substring(0,name.length()-1);
			protocol = protocol.substring(0,protocol.length()-1);
			host = host.substring(0,host.length()-1);
			port = port.substring(0,port.length()-1);
			path = path.substring(0,path.length()-1);
			enabled = enabled.substring(0,enabled.length()-1);
		}

		set("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_NAME, name); //$NON-NLS-1$
		set("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_HOST, host); //$NON-NLS-1$
		set("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_PATH, path); //$NON-NLS-1$
		set("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_PROTOCOL, protocol); //$NON-NLS-1$
		set("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_PORT, port); //$NON-NLS-1$
		set("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_ICEnabled, enabled); //$NON-NLS-1$

		HelpPlugin.getTocManager().clearCache();
	}

	public static List<IC> getICs()
	{
		return prefsToICs(
				ICPreferences.get("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_NAME).split(DELIMITER), //$NON-NLS-1$
				ICPreferences.get("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_PROTOCOL).split(DELIMITER), //$NON-NLS-1$
				ICPreferences.get("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_HOST).split(DELIMITER), //$NON-NLS-1$
				ICPreferences.get("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_PORT).split(DELIMITER), //$NON-NLS-1$
				ICPreferences.get("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_PATH).split(DELIMITER), //$NON-NLS-1$
				ICPreferences.get("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_ICEnabled).split(DELIMITER)); //$NON-NLS-1$
	}

	public static List<IC> prefsToICs(String names[],String protocols[],String hosts[],String ports[],String paths[],String states[])
	{
		List<IC> ics = new ArrayList<>();

		for (int i=0;i<names.length;i++)
		{
			if (!names[i].isEmpty())
			{
				try {
					IC ic = new IC(
						names[i],
						(protocols.length>i ? protocols[i] : "http") + "://" + //$NON-NLS-1$ //$NON-NLS-2$
						hosts[i]+":"+ //$NON-NLS-1$
						ports[i]+
						paths[i],
						"true".equalsIgnoreCase(states[i])); //$NON-NLS-1$
					ics.add(ic);
				} catch (MalformedURLException e) {
					ILog.of(ICPreferences.class).error(e.getMessage(), e);
				}
			}
		}
		return ics;
	}



	public static List<IC> getDefaultICs()
	{
		return prefsToICs(
				getDefault("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_NAME).split(DELIMITER), //$NON-NLS-1$
				getDefault("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_PROTOCOL).split(DELIMITER), //$NON-NLS-1$
				getDefault("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_HOST).split(DELIMITER), //$NON-NLS-1$
				getDefault("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_PORT).split(DELIMITER), //$NON-NLS-1$
				getDefault("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_PATH).split(DELIMITER), //$NON-NLS-1$
				getDefault("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_ICEnabled).split(DELIMITER)); //$NON-NLS-1$
	}


	/**
	 * Returns a default preference for the given name
	 *
	 * @param plugin - Name of the plugin containing this preference
	 * @param name - Name of the preference to retrieve
	 * @return value, or empty string if no preference found
	 */
	public static String getDefault(String plugin,String name)
	{
		return getDefaultNode(plugin).get(name, ""); //$NON-NLS-1$
	}

	public static void setRemoteHelp(boolean enabled)
	{
		set("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_ON,enabled+""); //$NON-NLS-1$ //$NON-NLS-2$
		HelpPlugin.getTocManager().clearCache();
	}

	public static void setRemoteHelpPreferred(boolean remotePreferred)
	{
		set("org.eclipse.help.base",IHelpBaseConstants.P_KEY_REMOTE_HELP_PREFERRED,remotePreferred+""); //$NON-NLS-1$ //$NON-NLS-2$
		HelpPlugin.getTocManager().clearCache();
	}

	/**
	 * Sets a preference
	 *
	 * @param plugin - Name of the plugin containing this preference
	 * @param name - Name of the preference
	 * @param value - Value to set
	 */
	public static void set(String plugin,String name,String value)
	{
		set(getNode(plugin),name,value);
	}

	/**
	 * Set a preference in the given node.
	 *
	 * @param node
	 * @param name
	 * @param value
	 */
	public static void set(IEclipsePreferences node,String name,String value)
	{
		node.put(name, value);
		try {
			node.flush();
		} catch (BackingStoreException e) {} //Nothing we can do, move on
	}

	/**
	 * Returns the preference found for the given name
	 *
	 * @param plugin - Name of the plugin containing this preference
	 * @param name - Name of the preference to retrieve
	 * @return value, or empty string if no preference found
	 */
	public static String get(String plugin,String name)
	{
		return getNode(plugin).get(name, ""); //$NON-NLS-1$
	}

	/**
	 * Get the IEclipsePreferences node for the given plugin
	 *
	 * @param plugin
	 * @return
	 */
	public static IEclipsePreferences getNode(String plugin)
	{
		IEclipsePreferences p = InstanceScope.INSTANCE.getNode(plugin);
		return p;
	}


	/**
	 * Get the default IEclipsePreferences node for the given plugin
	 *
	 * @param plugin
	 * @return
	 */
	public static IEclipsePreferences getDefaultNode(String plugin)
	{
		IEclipsePreferences p = DefaultScope.INSTANCE.getNode(plugin);
		return p;
	}
}
