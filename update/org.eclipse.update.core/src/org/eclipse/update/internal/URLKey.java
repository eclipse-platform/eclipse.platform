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
