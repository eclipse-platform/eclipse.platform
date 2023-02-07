/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;


public class ProxyUtil {

	public static boolean isAuthConnSupported()
	{
		try {
			Class.forName("org.eclipse.core.net.proxy.IProxyService"); //$NON-NLS-1$
			return true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static IProxyData getProxy(URL url)
	{
		if (!isAuthConnSupported())
			return null;

		IProxyService service = ProxyManager.getProxyManager();
		IProxyData data[];

		if (!service.isProxiesEnabled())
			return null;

		try {
			URI uri = url.toURI();
			if (shouldBypass(uri))
				return null;
			data = service.select(uri);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}

		if (data.length==0)
			return null;
		return data[0];
	}

	public static boolean shouldBypass(URI uri)
	{
		String host = uri.getHost();
		if (host==null)
			return true;

		List<String> hosts = getProxyBypassHosts();
		if (hosts.contains(host))
			return true;
		if ((host.equals("localhost") || host.equals("127.0.0.1")) && hosts.contains("<local>")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return true;
		return false;
	}


	public static List<String> getProxyBypassHosts()
	{
		List<String> hosts = new ArrayList<>();
		if (!isAuthConnSupported())
			return hosts;

		IProxyService service = ProxyManager.getProxyManager();
		String manuals[] = service.getNonProxiedHosts();
		String natives[] = null;
		if (service instanceof ProxyManager)
			natives = ((ProxyManager)service).getNativeNonProxiedHosts();

		Collections.addAll(hosts, manuals);
		if (natives!=null)
			Collections.addAll(hosts, natives);
		return hosts;
	}

	public static URLConnection getConnection(URL url) throws IOException
	{
		IProxyData data = getProxy(url);
		if (data==null)
			return url.openConnection();

		if (data.isRequiresAuthentication())
			Authenticator.setDefault(new ProxyAuthenticator(data.getUserId(),data.getPassword()));
		else
			Authenticator.setDefault(null);

		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getHost(), data.getPort()));
		return url.openConnection(proxy);
	}


	public static InputStream getStream(URL url) throws IOException
	{
		return getConnection(url).getInputStream();
	}


	private static class ProxyAuthenticator extends Authenticator {

		private String user, password;

		public ProxyAuthenticator(String user, String password) {
			this.user = user;
			this.password = password;
		}

		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(user, password.toCharArray());
		}
	}
}
