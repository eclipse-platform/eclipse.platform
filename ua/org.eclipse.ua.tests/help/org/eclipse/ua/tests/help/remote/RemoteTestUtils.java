/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.server.WebappManager;
import org.osgi.framework.Bundle;

public class RemoteTestUtils {

	public static String createMockContent(String plugin, String path,
			String locale, int port) {
		String result = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0//EN\">"
				+ "<HTML lang =\"" + locale + "\"><HEAD>"
				+ "<TITLE> Content from: " + plugin + "</TITLE></HEAD>"
				+ "<BODY><P>Path is: " + path + ",Port is: "+port+"</P></BODY></HTML>";
		return result;
	}

	public static String getRemoteContent(String plugin, String path,
			String locale) throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/rtopic/" + plugin
				+ path + "?lang=" + locale);
		return readFromURL(url);
	}

	public static String getLocalContent(String plugin, String path)
			throws IOException {
		Bundle bundle = Platform.getBundle(plugin);
		URL url;
		if (bundle != null) {
			url = FileLocator.toFileURL(new URL(bundle.getEntry("/"), path)); //$NON-NLS-1$
		} else {
			throw new IOException("Invalid bundle " + plugin);
		}
		return readFromURL(url);
	}

	public static String readFromURL(URL url) throws IOException {
		try (InputStream is = url.openStream();
				InputStreamReader inputStreamReader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
			StringBuilder buffer = new StringBuilder();
			char[] cbuf = new char[256];
			int len;
			do {
				len = inputStreamReader.read(cbuf);
				if (len > 0) {
					buffer.append(cbuf, 0, len);
				}
			} while (len >= 0);
			return buffer.toString();
		}
	}

}
