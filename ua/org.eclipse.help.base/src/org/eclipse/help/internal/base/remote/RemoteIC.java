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

public class RemoteIC {

	private boolean enabled = false;

	private String name = ""; //$NON-NLS-1$

	private String host = ""; //$NON-NLS-1$

	private String path = ""; //$NON-NLS-1$

	private String protocol = ""; //$NON-NLS-1$

	private String port;

	private static final String PROTOCOL_HTTP = "http"; //$NON-NLS-1$

	public RemoteIC(boolean enabled, String name, String host, String path, String port){

		this.enabled = enabled;
		this.name    = name;
		this.host    = host;
		this.path    = path;
		this.port    = port;
		this.protocol    = PROTOCOL_HTTP;
	}

	public RemoteIC(boolean enabled, String name, String host, String path, String protocol, String port){

		this.enabled = enabled;
		this.name    = name;
		this.host    = host;
		this.path    = path;
		this.protocol = protocol;
		this.port    = port;

	}
	public String getHost() {
		return host;
	}

	public String getPath() {
		return path;
	}

	public String getProtocol() {
		return protocol;
	}

	public String getPort() {
		return port;
	}

	public String getName() {
		return name;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setPort(String port) {
		this.port = port;
	}


	//I added this method, which overrides the original "equal" method in the class Object

	@Override
public boolean equals(Object anotherObject)throws ClassCastException {
		if (!(anotherObject instanceof RemoteIC))
			return false;
		if ( !(((RemoteIC) anotherObject).getName().equals(this.getName())))
			return false;
		if ( !(((RemoteIC) anotherObject).getHost().equals(this.getHost())))
			return false;
		if ( !(((RemoteIC) anotherObject).getPath().equals(this.getPath())))
			return false;
		if ( !(((RemoteIC) anotherObject).getProtocol().equals(this.getProtocol())))
			return false;
		if ( !(((RemoteIC) anotherObject).getPort().equals(this.getPort())))
			return false;
		if ( !(((RemoteIC) anotherObject).isEnabled()==this.isEnabled()))
			return false;

		//if we made it here, the the objects are the same
		return true;
	}
}
