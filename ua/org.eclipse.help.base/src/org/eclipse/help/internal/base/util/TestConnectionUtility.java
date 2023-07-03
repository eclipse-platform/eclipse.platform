/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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

 package org.eclipse.help.internal.base.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.eclipse.help.internal.base.remote.HttpsUtility;

public class TestConnectionUtility {

	//This class provides a utility for testing if a connection
	//can be made to a given URL
	private static final String PATH_TOC = "/toc"; //$NON-NLS-1$
	private static final String PROTOCOL = "http"; //$NON-NLS-1$
	private static final String PROTOCOL_HTTPS = "https"; //$NON-NLS-1$

	private final static int SOCKET_TIMEOUT = 5000; //milliseconds

	public static boolean testConnection(String thisHost, String thisPort,
			String thisPath, String thisProtocol) {

		boolean validConnection = true;
		String urlConnection = ""; //$NON-NLS-1$

		// Build connection string
		if (thisPort.equals("80")) //$NON-NLS-1$
			urlConnection = thisProtocol + "://" + thisHost + thisPath; //$NON-NLS-1$
		else
			urlConnection = thisProtocol + "://" + thisHost + ":" + thisPort + thisPath; //$NON-NLS-1$ //$NON-NLS-2$

		if(thisProtocol.equalsIgnoreCase(PROTOCOL))
		{
			// Test Connection. If exception thrown, invalid connection
			try {
				// Validate Toc connection...
				URL testTocURL = new URL(urlConnection + PATH_TOC);
				validConnection = isValidToc(testTocURL);
			} catch (MalformedURLException e) {
				validConnection = false;
			}
		}
		else if(thisProtocol.equalsIgnoreCase(PROTOCOL_HTTPS))
		{
			// Validate Toc connection...
			validConnection = HttpsUtility.canConnectToHttpsURL(urlConnection + PATH_TOC);
		}
		return validConnection;
	}

	private static boolean isValidToc(URL url)
	{
		InputStream in = null;
		try{
			URLConnection connection = ProxyUtil.getConnection(url);
			setTimeout(connection, SOCKET_TIMEOUT);
			connection.connect();
			in = connection.getInputStream();
			if (in!=null)
			{
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
					String line;
					while ((line = reader.readLine()) != null) {
						if (line.indexOf("<tocContributions>") > -1) { //$NON-NLS-1$
							reader.close();
							return true;
						}
					}
				}
			}
		}catch (Exception ex){}
		finally{
			try {
				if (in!=null)
					in.close();
			} catch (IOException e) {}
		}
		return false;
	}

	private static void setTimeout(URLConnection conn, int milliseconds) {
		conn.setConnectTimeout(milliseconds);
	}
}
