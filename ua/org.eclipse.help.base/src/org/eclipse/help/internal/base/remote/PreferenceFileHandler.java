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
package org.eclipse.help.internal.base.remote;

import java.util.ArrayList;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.IHelpBaseConstants;
import org.osgi.service.prefs.BackingStoreException;



public class PreferenceFileHandler {

	protected String[] nameEntries, hostEntries, pathEntries, protocolEntries, portEntries, isICEnabled = null;

	protected String namePreference, hostPreference, pathPreference, protocolPreference, portPreference, icEnabledPreference;

	protected int numEntries = 0, numHostEntries=0;

	protected static String PREFERENCE_ENTRY_DELIMITER = ","; //$NON-NLS-1$

	public static final int LOCAL_HELP_ONLY=0;
	public static final int LOCAL_HELP_PRIORITY=1;
	public static final int REMOTE_HELP_PRIORITY=2;

	public PreferenceFileHandler() {

		/*
		 * Preference values are currently comma separated
		 */

		// TODO: Decide if comma is a good delimiter, or if we should use a different delimiter.

		namePreference = Platform.getPreferencesService().getString
			(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_NAME, "", null); //$NON-NLS-1$
		hostPreference = Platform.getPreferencesService().getString
			(HelpBasePlugin.PLUGIN_ID,IHelpBaseConstants.P_KEY_REMOTE_HELP_HOST, "", null); //$NON-NLS-1$
		pathPreference = Platform.getPreferencesService().getString
			(HelpBasePlugin.PLUGIN_ID,IHelpBaseConstants.P_KEY_REMOTE_HELP_PATH, "", null); //$NON-NLS-1$
		protocolPreference = Platform.getPreferencesService().getString
		(HelpBasePlugin.PLUGIN_ID,IHelpBaseConstants.P_KEY_REMOTE_HELP_PROTOCOL, "", null); //$NON-NLS-1$
		portPreference = Platform.getPreferencesService().getString
			(HelpBasePlugin.PLUGIN_ID,IHelpBaseConstants.P_KEY_REMOTE_HELP_PORT, "", null); //$NON-NLS-1$
		icEnabledPreference =Platform.getPreferencesService().getString
			(HelpBasePlugin.PLUGIN_ID,IHelpBaseConstants.P_KEY_REMOTE_HELP_ICEnabled, "", null); //$NON-NLS-1$

		//Get host array first, and initialize values
		if(hostPreference.length()==0)
		{
			this.hostEntries=new String[0];
			numHostEntries=0;
		}
		else
		{
			this.hostEntries = hostPreference.split(PREFERENCE_ENTRY_DELIMITER);
			numHostEntries=hostEntries.length;
		}

		// Get the preference values
		this.nameEntries = getValues(namePreference, ""); //$NON-NLS-1$
		this.pathEntries = getValues(pathPreference, "/"); //$NON-NLS-1$
		this.protocolEntries = getValues(protocolPreference, "http"); //$NON-NLS-1$
		this.portEntries = getValues(portPreference, "80"); //$NON-NLS-1$
		this.isICEnabled = getValues(icEnabledPreference, "true"); //$NON-NLS-1$


		// The size of any of the array elements should equal the number of remote infocenters
		if (this.nameEntries == null)
			this.numEntries = 0;
		else
			this.numEntries = this.nameEntries.length;
	}

	protected String[] getValues(String preferenceEntry, String appendString) {

		if (numHostEntries==0) //preference equals ""
			return  new String[0];//NEW

		// Split the string and return an array of Strings
		String [] currEntries;
		String [] updatedArray=null;

		if(!preferenceEntry.isEmpty())
			currEntries=preferenceEntry.split(PREFERENCE_ENTRY_DELIMITER);
		else
			currEntries = new String[0];

		if(currEntries.length!=numHostEntries) //Current Entry not equals to Hosts
		{
			int i;

			updatedArray=new String[numHostEntries];

			if(currEntries.length>numHostEntries) //More in this array then host.  Only take values for # of hosts
			{
				for(i=0;i<numHostEntries;i++)
				{
					updatedArray[i]=currEntries[i];
				}

			}
			else //Less values.  Append values based off or array types
			{
				int entryCount=0;

				for(i=0;i<currEntries.length;i++)
				{
					updatedArray[i]=currEntries[i];
					entryCount=entryCount+1;
				}

				for(i=entryCount;i<numHostEntries;i++)
				{
					updatedArray[i]=appendString;
				}
			}
			currEntries=updatedArray;
		}

		return currEntries;

	}

	/**
	 * This methods writes the remote infocenters in the table model to the preferences.ini.
	 *
	 * @param remoteICs List of RemoteIC Objects
	 *
	 */
	public static void commitRemoteICs(RemoteIC[] remoteICs) {

		RemoteIC remote_ic = null;
		String name = "", host = "", path = "", protocol="", port = "", enabledString = ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		boolean enabled;

		int numICs = remoteICs.length;

		if (numICs > 0) {

			remote_ic = remoteICs[0];
			name = remote_ic.getName();
			host = remote_ic.getHost();
			path = remote_ic.getPath();
			protocol = remote_ic.getProtocol();
			port = remote_ic.getPort();
			enabled = remote_ic.isEnabled();
			enabledString = enabled + ""; //$NON-NLS-1$

			for (int i = 1; i < numICs; i++) {
				remote_ic = remoteICs[i];
				name = name + PREFERENCE_ENTRY_DELIMITER + remote_ic.getName();
				host = host + PREFERENCE_ENTRY_DELIMITER + remote_ic.getHost();
				path = path + PREFERENCE_ENTRY_DELIMITER + remote_ic.getPath();
				protocol = protocol + PREFERENCE_ENTRY_DELIMITER + remote_ic.getProtocol();
				port = port + PREFERENCE_ENTRY_DELIMITER + remote_ic.getPort();
				enabledString = enabledString + PREFERENCE_ENTRY_DELIMITER + remote_ic.isEnabled();
			}

		}

		// Save new strings to preferences

		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);

		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_NAME, name);
		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_HOST, host);
		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_PATH, path);
		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_PROTOCOL, protocol);
		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_PORT, port);
		prefs.put(IHelpBaseConstants.P_KEY_REMOTE_HELP_ICEnabled, enabledString);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
		}
	}

	/**
	 *
	 * This method returns an ArrayList containing all RemoteIC entries in the preferences
	 *
	 */
	public ArrayList<RemoteIC> getRemoteICList() {
		ArrayList<RemoteIC> remoteICList = new ArrayList<>();

		// Load the preferences in org.eclipse.help.base/preferences.ini
		RemoteIC initRemoteIC;
		int totalICs = this.getTotalRemoteInfocenters();
		String host, name, path, protocol, port, enabledDisabled;
		boolean currEnabled;

		for (int i = 0; i < totalICs; i++) {

			host = (this.getHostEntries())[i];
			name = (this.getNameEntries())[i];
			path = (this.getPathEntries())[i];
			protocol = (this.getProtocolEntries())[i];
			port = (this.getPortEntries())[i];
			enabledDisabled = (this.getEnabledEntries())[i];
			if (enabledDisabled.equalsIgnoreCase("true")) //$NON-NLS-1$
			{
				currEnabled = true;
			} else {
				currEnabled = false;
			}

			initRemoteIC = new RemoteIC(currEnabled, name, host, path, protocol,port);
			remoteICList.add(initRemoteIC);

		}

		return remoteICList;

	}

	public static int getEmbeddedHelpOption() {
		boolean isRemoteOn = Platform.getPreferencesService().getBoolean
		(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_ON, false, null);

		boolean isRemotePreferred = Platform.getPreferencesService().getBoolean
		(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_PREFERRED, false, null);

		if(!isRemoteOn)
		{
			return LOCAL_HELP_ONLY;
		}
		else if(!isRemotePreferred)
		{
			return LOCAL_HELP_PRIORITY;
		}
		else
		{
			return REMOTE_HELP_PRIORITY;
		}
	}

	public String[] getHostEntries() {
		return hostEntries;
	}

	public String[] getNameEntries() {
		return nameEntries;
	}

	public String[] getPathEntries() {
		return pathEntries;
	}

	public String[] getProtocolEntries() {
		return protocolEntries;
	}

	public String[] getPortEntries() {
		return portEntries;
	}

	public String[] getEnabledEntries() {
		return isICEnabled;
	}

	public int getTotalRemoteInfocenters() {
		return numEntries;
	}

	public String[] isEnabled() {
		return isICEnabled;
	}

	public String getDelimeter() {
		return PREFERENCE_ENTRY_DELIMITER;
	}

}
