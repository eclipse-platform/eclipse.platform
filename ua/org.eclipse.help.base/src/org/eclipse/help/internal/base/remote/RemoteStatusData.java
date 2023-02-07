/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
 *     Alex Blewitt - Bug 474070
 *******************************************************************************/
package org.eclipse.help.internal.base.remote;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.IHelpBaseConstants;
import org.eclipse.help.internal.base.util.TestConnectionUtility;

public class RemoteStatusData {

	private static int TIMEOUT = 60 * 1000;


	/*
	 * Convenience method to see if any remote help
	 * is down
	 */
	public static boolean isAnyRemoteHelpUnavailable()
	{
		ArrayList<URL> sites = getRemoteSites();
		if (sites.isEmpty())
			return false;

		for (int s=0;s<sites.size();s++)
			if (!isConnected(sites.get(s)))
				return true;

		return false;
	}

	/*
	 * Checks each URL in the ArrayList site to see if
	 * a network connection can be opened to
	 * url+/index.jsp
	 *
	 * Returns a subset of sites that cannot be connected.
	 * May be empty, or may be the same as sites
	 */
	public static ArrayList<URL> checkSitesConnectivity(ArrayList<URL> sites)
	{
		ArrayList<URL> badSites = new ArrayList<>();

		for (int i=0;i<sites.size();i++)
			if (!isConnected(sites.get(i)))
				badSites.add(sites.get(i));

		return badSites;
	}

	public static boolean isConnected(URL site)
	{
		ConnectionCache cache = ConnectionCache.getCache();
		try{
			return cache.isConnected(site);
		}catch(CoreException e)
		{
			boolean connected = TestConnectionUtility.testConnection(site.getHost(),
					"" + site.getPort(), site.getPath(), site.getProtocol()); //$NON-NLS-1$
			cache.put(site, connected);
			cache.resetTimer();
			return connected;
		}
	}

	/*
	 * Loads the remote sites stored in preferences,
	 * and places them as URLs in an ArrayList.
	 *
	 * Returns the ArrayList with sites in URL form
	 */
	public static ArrayList<URL> getRemoteSites()
	{
		ArrayList<URL> sites = new ArrayList<>();

		boolean remoteHelpEnabled =
			Platform.getPreferencesService().getBoolean(
					HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_ON, false,null);

		if (!remoteHelpEnabled)
			return sites;

		String hosts[] = Platform.getPreferencesService().getString(
				HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_HOST, "", null).split(","); //$NON-NLS-1$ //$NON-NLS-2$
		String paths[] = Platform.getPreferencesService().getString(
				HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_PATH, "", null).split(","); //$NON-NLS-1$ //$NON-NLS-2$
		String protocols[] = Platform.getPreferencesService().getString(
				HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_PROTOCOL, "", null).split(","); //$NON-NLS-1$ //$NON-NLS-2$
		String ports[] = Platform.getPreferencesService().getString(
				HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_PORT, "", null).split(","); //$NON-NLS-1$ //$NON-NLS-2$
		String enableds[] = Platform.getPreferencesService().getString(
				HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_ICEnabled, "", null).split(","); //$NON-NLS-1$ //$NON-NLS-2$


		for (int i=0;i<hosts.length;i++)
		{
			try{
				if (enableds[i].equalsIgnoreCase("true")) //$NON-NLS-1$
				{
					URL url = new URL(
							protocols[i]+"://"+hosts[i]+':'+ports[i]+paths[i]); //$NON-NLS-1$
					sites.add(url);
				}
			}
			catch(Exception ex){
			}
		}
		return sites;
	}

	public static void clearCache() {
		ConnectionCache.clear();
	}

	private static class ConnectionCache
	{
		private static ConnectionCache instance;

		private Hashtable<URL, Boolean> cache;
		private long start;

		private ConnectionCache(){

			cache = new Hashtable<>();
			resetTimer();
		}

		public void resetTimer()
		{
			start = new Date().getTime();
		}

		public static ConnectionCache getCache()
		{
			if (instance==null || instance.isExpired())
			{
				instance = new ConnectionCache();
			}
			return instance;
		}

		public static void clear()
		{
			instance = null;
		}

		public boolean isExpired()
		{
			long now = new Date().getTime();

			return (now > start + TIMEOUT);
		}

		public boolean isConnected(URL url) throws CoreException
		{
			Boolean b = cache.get(url);
			if (b==null)
				throw new CoreException(new Status(IStatus.ERROR,HelpBasePlugin.PLUGIN_ID,"Cache Unavailable")); //$NON-NLS-1$

			return b.booleanValue();
		}

		public void put(URL url,boolean connected)
		{
			cache.put(url,Boolean.valueOf(connected));
		}
	}

}
