/*******************************************************************************
 * Copyright (c) 2021 Joerg Kubitz and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Joerg Kubitz - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.filesystem;

import java.net.URI;
import org.eclipse.core.filesystem.IFileStore;

/**
 * Provides internal utility functions for comparing FileStores and paths
 */
public final class FileStoreUtil {

	private FileStoreUtil() {
		// Not to be instantiated
	}

	/**
	 * Compares URIs by their normalized values.
	 * This implementation has a memory hotspot when uri is not normalized.
	 * see bug 570896!
	 * @since org.eclipse.core.filesystem 1.9
	 */
	private static int comparePathUri(URI uri1, URI uri2) {
		if (uri1 == null && uri2 == null)
			return 0;
		int compare;
		// Fixed compare contract sgn(compare(x, y)) == -sgn(compare(y, x))
		// in case of Exceptions:
		if ((compare = nullsLast(uri1, uri2)) != 0)
			return compare;
		// note: If uri is already normal u.normalize() will just return u:
		return compareNormalisedUri(uri1.normalize(), uri2.normalize());
	}

	private static int compareNormalisedUri(URI uri1, URI uri2) {
		int c;
		// avoid to use IPath here due to high ephemeral memory allocation (Bug 570896)
		if ((c = compareStringOrNull(uri1.getAuthority(), uri2.getAuthority())) != 0)
			return c;
		if ((c = compareStringOrNull(uri1.getScheme(), uri2.getScheme())) != 0)
			return c;
		if ((c = comparePathSegments(uri1.getPath(), uri2.getPath())) != 0)
			return c;
		if ((c = compareStringOrNull(uri1.getQuery(), uri2.getQuery())) != 0)
			return c;
		return c;
	}

	static int nullsLast(Object c1, Object c2) {
		if (c1 == null) {
			if (c2 == null)
				return 0;
			return 1;
		}
		if (c2 == null)
			return -1;
		return 0;
	}

	public static int comparePathSegments(String p1, String p2) {
		int compare;
		compare = compareSlashFirst(p1, p2);
		if (compare != 0)
			return compare;
		// all segments are equal, so compare based on number of segments
		int segmentCount1 = countCharButNotAtEnd(p1, '/');
		int segmentCount2 = countCharButNotAtEnd(p2, '/');
		compare = segmentCount1 - segmentCount2;
		return compare;
	}

	static int compareSlashFirst(String value, String other) {
		int len1 = value.length();
		int len2 = other.length();
		int lim = Math.min(len1, len2);
		for (int k = 0; k < lim; k++) {
			char c1 = value.charAt(k);
			char c2 = other.charAt(k);
			if (c1 != c2) {
				// '/' first
				if (c1 == '/')
					return -1;
				if (c2 == '/')
					return 1;
				return c1 - c2;
			}
		}
		// ignore "/" at the end
		if (value.endsWith("/")) //$NON-NLS-1$
			len1 -= 1;
		if (other.endsWith("/")) //$NON-NLS-1$
			len2 -= 1;
		return len1 - len2;
	}

	static int countCharButNotAtEnd(String str, char c) {
		int count = 0;
		for (int i = 0; i < str.length() - 1; i++) {
			if (str.charAt(i) == c)
				count++;
		}
		return count;
	}

	/**
	 * Compares two strings that are possibly null.
	 * @since org.eclipse.core.filesystem 1.9
	 */
	private static int compareStringOrNull(String string1, String string2) {
		if (string1 == null) {
			if (string2 == null)
				return 0;
			return 1;
		}
		if (string2 == null)
			return -1;
		return string1.compareTo(string2);
	}

	/**
	 * Compares two file stores by comparing their URIs.
	 * @param fileStore1 the first fileStore to compare, cannot be null
	 * @param fileStore2 the second fileStore to compare, cannot be null
	 * @return 0 if the fileStores are equal, 1 if fileStore1 is bigger than fileStore2, -1 otherwise
	 */
	public static int compareFileStore(IFileStore fileStore1, IFileStore fileStore2) {
		int compare = compareStringOrNull(fileStore1.getFileSystem().getScheme(), fileStore2.getFileSystem().getScheme());
		if (compare != 0)
			return compare;
		// compare based on URI path segment values
		URI uri1;
		URI uri2;
		try {
			uri1 = fileStore1.toURI();
		} catch (Exception e1) {
			// protect against misbehaving 3rd party code in file system implementations
			uri1 = null;
		}
		try {
			uri2 = fileStore2.toURI();
		} catch (Exception e2) {
			// protect against misbehaving 3rd party code in file system implementations
			uri2 = null;
		}
		// use old slow compare for compatibility reason. Does have a memory hotspot see bug 570896
		return comparePathUri(uri1, uri2);
	}
}
