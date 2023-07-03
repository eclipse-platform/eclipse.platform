/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.webapp.service;

import java.io.IOException;
import java.net.URL;

import org.eclipse.help.internal.server.WebappManager;
import org.eclipse.help.internal.webapp.utils.Utils;
import org.eclipse.ua.tests.help.remote.RemoteTestUtils;

public class ServicesTestUtils extends RemoteTestUtils {

	public static String getRemoteContent(String plugin, String path,
			String locale) throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/vs/service/rtopic/" + plugin
				+ path + "?lang=" + locale);
		return readFromURL(url);
	}

	public static String getLocalContent(String plugin, String path)
			throws IOException {
		String localContent = RemoteTestUtils.getLocalContent(plugin, path);
		try {
			localContent = Utils.updateResponse(localContent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return localContent;
	}

}
