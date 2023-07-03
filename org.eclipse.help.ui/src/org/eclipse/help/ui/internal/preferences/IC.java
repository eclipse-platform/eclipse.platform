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

package org.eclipse.help.ui.internal.preferences;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Information Center (IC) - stores URL info.
 *
 * @author chaustin
 *
 */
public class IC {

	private String name;
	private boolean enabled;
	private String protocol;
	private String host;
	private String path;
	private int port = -1;


	public IC(String name,String href,boolean enabled) throws MalformedURLException
	{
		this.name = name;
		this.enabled = enabled;
		setHref(href);
	}

	public void setHref(String href) throws MalformedURLException
	{
		if (href.startsWith(":")) //$NON-NLS-1$
			href = "http"+href; //$NON-NLS-1$

		setHref(new URL(href));
	}

	public void setHref(URL url)
	{
		this.protocol = url.getProtocol();
		this.host = url.getHost();
		this.path = url.getPath();
		this.port = url.getPort();
		if (port==-1)
		{
			if (protocol.equals("http")) //$NON-NLS-1$
				port = 80;
			else if (protocol.equals("https")) //$NON-NLS-1$
				port = 443;
		}
	}

	public String getHref()
	{
		String portString = ":"+port; //$NON-NLS-1$
		if (port==80 && protocol.equals("http")) //$NON-NLS-1$
			portString = ""; //$NON-NLS-1$
		else if (port==443 && protocol.equals("https")) //$NON-NLS-1$
			portString = ""; //$NON-NLS-1$

		return protocol+"://"+host+portString+path; //$NON-NLS-1$
	}


	public String getName()
	{
		return name;
	}


	public boolean isEnabled()
	{
		return enabled;
	}
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public String toString()
	{
		return name+" ("+getHref()+")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests to see if 2 ICs are equal.  The state (enabled or disabled) is ignored
	 * during this operation.
	 *
	 */
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof IC))
			return false;

		IC candidate = (IC)o;

		if (getName().equals(candidate.getName())
				&& getPath().equals(candidate.getPath())
				&& getPort()==candidate.getPort()
				&& getProtocol().equals(candidate.getProtocol()))
		{
			try {
				InetAddress host1 = InetAddress.getByName(getHost());
				InetAddress host2 = InetAddress.getByName(candidate.getHost());

				if (host1.getHostAddress().equals(host2.getHostAddress()))
					return true;
			} catch (UnknownHostException e) {
				if (getHost().equals(candidate.getHost()))
					return true;
			}
		}

		return false;
	}
}
