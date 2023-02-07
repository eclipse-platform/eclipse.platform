/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;


public class HttpsUtility {

	private static final String PATH_TOC = "/toc"; //$NON-NLS-1$
	private static final String PARAM_LANG = "lang"; //$NON-NLS-1$

	private final static int SOCKET_TIMEOUT = 5000; //milliseconds

	public static HttpsURLConnection getConnection(URL httpsURL)
	{
		try
		{
			SSLContext sc = SSLContext.getInstance("TLS"); //$NON-NLS-1$
			sc.init( null, null, new java.security.SecureRandom() );
			HttpsURLConnection con = (HttpsURLConnection)httpsURL.openConnection();
			con.setSSLSocketFactory(sc.getSocketFactory());
			return con;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	public static InputStream getHttpsStream(URL httpsURL)
	{
		try {
			HttpsURLConnection con = getConnection(httpsURL);
			return con==null ? null : con.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static InputStream getHttpsInputStream(String thisProtocol,String thisHost, String thisPort, String thisPath, String locale)
	{
		try {
			URL url = new URL(thisProtocol, thisHost, Integer.parseInt(thisPort),
					thisPath + PATH_TOC + '?' + PARAM_LANG + '=' + locale);
			return getHttpsStream(url);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static URL getHttpsURL(String thisProtocol,String thisHost, int thisPort, String thisPath)
	{
		try {
			return new URL(thisProtocol, thisHost, Integer.valueOf(thisPort) .intValue(),
					thisPath + PATH_TOC);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static URL getHttpsURL(String urlPath)
	{
		try {
			return new URL(urlPath);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static URL getHttpsURL(String thisProtocol,String thisHost, String thisPort, String thisPath)
	{
		return getHttpsURL(thisProtocol,thisHost,Integer.parseInt(thisPort),thisPath);
	}

	public static boolean canConnectToHttpsURL(String urlConnection)
	{
		try
		{
			HttpsURLConnection testConnection = getConnection(new URL(urlConnection));
			testConnection.setConnectTimeout(SOCKET_TIMEOUT);
			testConnection.connect();
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}
}
