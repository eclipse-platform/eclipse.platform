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
package org.eclipse.update.internal.core;
 
import java.net.*;



/**
 * 
 * 
 */
public class URLKey {

	private URL url;
	
	/**
	 * Constructor for URLKey.
	 */
	public URLKey(URL url) {
		super();
		this.url = url;
	}

	/**
	 * @see java.lang.Object#equals(Object)
	 */
	public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}

			if (this == obj) {
				return true;
			}

			if (obj instanceof URLKey) {
				return equals(((URLKey) obj).getURL());
			}

			if (!(obj instanceof URL)) {
				return false;
			}

			URL url2 = (URL)obj;
			if (url == url2) {
				return true;
			}

			return UpdateManagerUtils.sameURL(url,url2);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return url.hashCode();
	}

	/**
	 * Returns the url.
	 * @return URL
	 */
	public URL getURL() {
		return url;
	}

}
