/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     James D Miles (IBM Corp.) - bug 191368, Policy URL doesn't support UTF-8 characters
 *******************************************************************************/
package org.eclipse.update.internal.core;


import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
/**
 * Encodes a <code>URL</code> into an <code>ASCII</code> readable
 * <code>URL</code> that is safe for transport. Encoded
 * <code>URL</code>s can be decoded using the <code>URLDecoder</code>.
 *
 * @see URLDecoder
 */
public final class URLEncoder {
	/**
	 * Prevents instances from being created.
	 */
	private URLEncoder() {
	}
	/**
	 * Encodes the given file and reference parts of a <code>URL</code> into
	 * an <code>ASCII</code> readable <code>String</code> that is safe for
	 * transport. Returns the result.
	 *
	 * @return the result of encoding the given file and reference parts of
	 *         a <code>URL</code> into an <code>ASCII</code> readable
	 *         <code>String</code> that is safe for transport
	 */
	public static String encode(String file, String query, String ref) {
		StringBuffer buf = new StringBuffer();
		StringTokenizer tokenizer = new StringTokenizer(file, "/", true); //$NON-NLS-1$

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (token.equals("/")) { //$NON-NLS-1$
				buf.append(token);
			} else {
				buf.append(encodeSegment(token));
			}
		}

		if (query != null){
			buf.append('?');
			buf.append(query);
		}

		if (ref != null) {
			buf.append('#');
			buf.append(encodeSegment(ref));
		}

		return buf.toString();
	}
	/**
	 * Encodes the given <code>URL</code> into an <code>ASCII</code>
	 * readable <code>URL</code> that is safe for transport. Returns the
	 * result.
	 *
	 * @return the result of encoding the given <code>URL</code> into an
	 *         <code>ASCII</code> readable <code>URL</code> that is safe for
	 *         transport
	 */
	public static URL encode(URL url) throws MalformedURLException {
		// encode the path not the file as the URL may contain a query
		String file = url.getPath();
		String query = url.getQuery();
		String ref = url.getRef();
		String auth = url.getAuthority();
		String host = url.getHost();
		int port = url.getPort();
		String userinfo = url.getUserInfo();

		// do not encode if there is an authority, such as in
		// ftp://user:password@host:port/path 
		// because the URL constructor does not allow it
		URL result = url;
		if (auth == null || auth.equals("") || userinfo == null) // $NON-NLS-1$ $NON-NLS-2$  //$NON-NLS-1$
			result =  new URL(url.getProtocol(), host, port, encode(file, query, ref));
		return result;
	}
	private static String encodeSegment(String segment) {
		
		// if we find a '%' in the string, consider the URL to be already encoded
		if (segment.indexOf('%')!=-1) return segment;
		
		StringBuffer result = new StringBuffer(segment.length());

		for (int i = 0; i < segment.length(); ++i) {
			char c = segment.charAt(i);
			if (mustEncode(c)) {
				byte[] bytes = null;
				try {
					bytes = new Character(c).toString().getBytes("UTF8"); //$NON-NLS-1$
				} catch (UnsupportedEncodingException e) {
					Assert.isTrue(false, e.getMessage());
				}
				for (int j = 0; j < bytes.length; ++j) {
					result.append('%');
					result.append(Integer.toHexString((bytes[j] >> 4) & 0x0F));
					result.append(Integer.toHexString(bytes[j] & 0x0F));
				}
			} else {
				result.append(c);
			}
		}

		return result.toString();
	}
	
	private static boolean mustEncode(char c) {
		if (c >= 'a' && c <= 'z') {
			return false;
		}

		if (c >= 'A' && c <= 'Z') {
			return false;
		}

		if (c >= '0' && c <= '9') {
			return false;
		}

		if (c >= '\'' && c <= '.') {
			return false;
		}

		if (c == '!' || c == '$' || c == '_' || c == '[' || c == ']') {
			return false;
		}

		// needed otherwise file:///c:/file/ becomes file:///C%3a/file/
		if (c ==':'){
			return false;
		}
		return true;
	}
}
