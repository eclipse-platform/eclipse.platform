package org.eclipse.update.internal;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 
import java.net.URL;

import org.eclipse.update.internal.core.UpdateManagerUtils;


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
		if (obj instanceof URL)
			return UpdateManagerUtils.sameURL(url,(URL)obj);
		else 
			return false;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return url.hashCode();
	}

}
