/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.security;

import java.net.*;


 
/**
 * Manages a handle to a keystore
 */
public class KeystoreHandle {
	
	private URL location;
	private String type;
	
	public KeystoreHandle(URL url, String type){
		this.location = url;
		this.type = type;
	}

	/**
	 * Gets the location.
	 * @return Returns a URL
	 */
	public URL getLocation() {
		return location;
	}

	/**
	 * Sets the location.
	 * @param location The location to set
	 */
	public void setLocation(URL location) {
		this.location = location;
	}

	/**
	 * Gets the type.
	 * @return Returns a String
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the type.
	 * @param type The type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

}
