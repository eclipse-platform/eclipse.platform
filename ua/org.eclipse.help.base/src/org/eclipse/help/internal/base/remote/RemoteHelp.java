/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.IHelpBaseConstants;

/*
 * Convenience methods for querying the remote help settings.
 */
public class RemoteHelp {

	private static final String PROTOCOL_HTTP = "http"; //$NON-NLS-1$
	private static final String PROTOCOL_HTTPS = "https"; //$NON-NLS-1$
	private static ListenerList<IPreferenceChangeListener> listeners;
	private static Throwable error;

	/*
	 * Adds a listener that will be notified whenever the user changes the
	 * remote help server preferences.
	 */
	public static void addPreferenceChangeListener(IPreferenceChangeListener listener) {
		if (listeners == null) {
			listeners = new ListenerList<>();
		}
		listeners.add(listener);
	}

	/*
	 * Removes a listener.
	 */
	public static void removePreferenceChangeListener(IPreferenceChangeListener listener) {
		if (listeners != null) {
			listeners.remove(listener);
		}
	}

	/*
	 * Signals all registered listeners that remote help preferences have
	 * changed.
	 */
	public static void notifyPreferenceChange() {
		if (listeners != null) {
			for (IPreferenceChangeListener listener : listeners)
				listener.preferenceChange(null);
		}
	}

	public static URL getURL(int ic, String pathSuffix) throws MalformedURLException {
		PreferenceFileHandler handler = new PreferenceFileHandler();
		String host = handler.getHostEntries()[ic];
		String path = handler.getPathEntries()[ic] + pathSuffix;
		String protocol = handler.getProtocolEntries()[ic];
		int port;
		URL url =null;
		try {
			port = Integer.parseInt(handler.getPortEntries()[ic]);
		} catch (NumberFormatException e) {
			throw new MalformedURLException();
		}
		if(protocol.equalsIgnoreCase(PROTOCOL_HTTPS))
			url = HttpsUtility.getHttpsURL(protocol,host,port,path);
		else
			url = new URL(PROTOCOL_HTTP, host, port, path);

		return url;
	}

	/*
	 * Returns whether or not the help system is currently configured for remote
	 * help content.
	 */
	public static boolean isEnabled() {
		return Platform.getPreferencesService().getBoolean(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_REMOTE_HELP_ON, false, null);
	}

	/*
	 * Clears the error status for remote help.
	 */
	public static void clearError() {
		error = null;
	}

	/*
	 * Returns the error produced during a previous remote help
	 * query, or null if there was none.
	 */
	public static Throwable getError() {
		return error;
	}

	/*
	 * Sets the latest exception to have occured while communicating
	 * with the remote help server.
	 */
	public static void setError(Throwable t) {
		error = t;
	}

}
