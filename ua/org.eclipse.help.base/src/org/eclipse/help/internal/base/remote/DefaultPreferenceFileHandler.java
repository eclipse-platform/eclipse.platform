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
 *******************************************************************************/
package org.eclipse.help.internal.base.remote;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.IHelpBaseConstants;

public class DefaultPreferenceFileHandler extends PreferenceFileHandler {

	private boolean isRemoteOn, isRemoteHelpPreferred;
	/**
	 * Class handles the default preferences for the Help Content preference page
	 */
	public DefaultPreferenceFileHandler() {

		IEclipsePreferences prefs = DefaultScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);

		namePreference = prefs.get(IHelpBaseConstants.P_KEY_REMOTE_HELP_NAME, ""); //$NON-NLS-1$
		hostPreference = prefs.get(IHelpBaseConstants.P_KEY_REMOTE_HELP_HOST, ""); //$NON-NLS-1$
		pathPreference = prefs.get(IHelpBaseConstants.P_KEY_REMOTE_HELP_PATH, ""); //$NON-NLS-1$
		protocolPreference = prefs.get(IHelpBaseConstants.P_KEY_REMOTE_HELP_PROTOCOL, ""); //$NON-NLS-1$
		portPreference = prefs.get(IHelpBaseConstants.P_KEY_REMOTE_HELP_PORT, ""); //$NON-NLS-1$
		icEnabledPreference = prefs.get(IHelpBaseConstants.P_KEY_REMOTE_HELP_ICEnabled, ""); //$NON-NLS-1$
		isRemoteOn = prefs.getBoolean(IHelpBaseConstants.P_KEY_REMOTE_HELP_ON, false);
		isRemoteHelpPreferred = prefs.getBoolean(IHelpBaseConstants.P_KEY_REMOTE_HELP_PREFERRED, false);

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

	public boolean isRemoteHelpOn() {
		return isRemoteOn;
	}

	public boolean isRemoteHelpPreferred() {
		return isRemoteHelpPreferred;
	}

}
