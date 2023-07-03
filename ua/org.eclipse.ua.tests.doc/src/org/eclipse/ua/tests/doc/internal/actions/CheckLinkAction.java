/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.doc.internal.actions;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.ILiveHelpAction;
import org.eclipse.help.internal.base.HelpBasePlugin;

public class CheckLinkAction implements ILiveHelpAction {

	private static final String HELP_TOPIC = "/help/topic";
	private static Map<String, String> links = new HashMap<>();
	private String link;
	public final static String ALL_PAGES_LOADED = "ALL_PAGES_LOADED";
	public final static String CHECK_LINKS = "CHECK_LINKS";

	@Override
	public void setInitializationString(String data) {
		link = data;
	}

	@Override
	public void run() {
		//System.out.println("Link = " + link);
		if (ALL_PAGES_LOADED.equals(link)) {
			LoadTocAction.showErrors();
		} else if (CHECK_LINKS.equals(link)) {
			LoadTocAction.showErrors();
			checkLinks();
		} else if (link.startsWith("htt")){
			//String lastPage = LoadTocAction.lastPage;
			links.put(link, VisitPageAction.lastPageVisited);
		}
	}

	private void checkLinks() {
		String errorPage = Platform.getPreferencesService().getString(HelpBasePlugin.PLUGIN_ID, "page_not_found", null, null); //$NON-NLS-1$
		setPageNotFoundPreference("");
		System.out.println("Start checking " + links.size() + " links");
		int count = 0;
		for (Map.Entry<String, String> entry : links.entrySet()) {
			String next = entry.getKey();
			count++;
			if (count % 1000 == 0)  {
				System.out.println("Checked " + count + " links");
			}
			//System.out.println("Process " + next);
			URL url;
			boolean opened;
			try {
				url = new URL(next);
				//URLConnection connection = url.openConnection();
				//connection.
				try (InputStream input = url.openStream()) {
					int nextChar = input.read();
					if (nextChar == -1) {
						System.out.println("Cannot read " + next);
						opened = false;
					} else {
						opened = true;
					}
				}
			} catch (Exception e) {
				opened = false;
			}
			if (!opened) {
				String containingPage = entry.getValue();
				System.out.println("Cannot open link from " + trimPath(containingPage)
						+ " to " + trimPath(next));
			}
		}
		//EclipseConnector.setNotFoundCallout(null);
		setPageNotFoundPreference(errorPage);
		links = new HashMap<>();
		System.out.println("End check links");
	}

	private String trimPath(String next) {
		String result = next;
		int htIndex = result.indexOf(HELP_TOPIC);
		if (htIndex >  0) {
			result = result.substring(htIndex + HELP_TOPIC.length());
		}
		int queryIndex = result.lastIndexOf('?');
		if (queryIndex > 0) {
			result = result.substring(0, queryIndex);
		}
		return result;
	}

	private void setPageNotFoundPreference(String value) {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
		prefs.put("page_not_found", value);
	}

}
