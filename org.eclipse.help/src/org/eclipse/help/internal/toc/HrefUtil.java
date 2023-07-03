/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.toc;

import org.eclipse.core.runtime.IPath;

public class HrefUtil {
	private static final String PLUGINS_ROOT_SLASH = "PLUGINS_ROOT/"; //$NON-NLS-1$

	/**
	 * Creates /pluginid/directory from directory name
	 */
	public static String normalizeDirectoryHref(String pluginID, String dir) {
		// "" is treated as if extra directory was not provided
		if (dir == null || dir.length() <= 0)
			return null;
		// "." means all the files in the plugin
		if (".".equals(dir)) //$NON-NLS-1$
			dir = ""; //$NON-NLS-1$
		// remove not needed trailing separator
		if (dir.length() > 0 && dir.lastIndexOf('/') == dir.length() - 1) {
			dir = dir.substring(0, dir.length() - 1);
		}
		return normalizeHref(pluginID, dir);
	}

	/**
	 * Creates /pluginid/href from href relative to the current plugin
	 *
	 * @param pluginID
	 *            id of a plugin to which href is relative
	 * @param href
	 *            relative href ex: path[#anchorID] ex:
	 *            ../pluginID/path[#anchorID]
	 * @return String representation of href, formatted as
	 *         /pluginID/path[#anchorID]
	 */
	public final static String normalizeHref(String pluginID, String href) {
		if (href == null)
			return null;
		if (href.startsWith("http:") //$NON-NLS-1$
				|| href.startsWith("https:") //$NON-NLS-1$
				|| href.startsWith("file:") //$NON-NLS-1$
				|| href.startsWith("jar:")) //$NON-NLS-1$
			// external doc
			return href;
		href = normalizeDirectoryPath(href);
		if (href.startsWith("/")) //$NON-NLS-1$
			// already normalized
			return href;
		if (href.startsWith("../")) { //$NON-NLS-1$
			return href.substring(2);
		}
		if (href.startsWith(PLUGINS_ROOT_SLASH)) {
			return href.substring(PLUGINS_ROOT_SLASH.length() - 1);
		}
		if (href.length() > 0) {
			StringBuilder buf = new StringBuilder(2 + pluginID.length()
					+ href.length());
			buf.append('/').append(pluginID);
			buf.append('/').append(href);
			return buf.toString();
		}
		return "/" + pluginID; //$NON-NLS-1$
	}
	/**
	 * Parses href and obtains plugin id
	 *
	 * @param href
	 *            String in format /string1[/string2]
	 * @return plugin ID, or null
	 */
	public static String getPluginIDFromHref(String href) {
		if (href == null || href.length() < 2 || href.charAt(0) != '/')
			return null;
		int secondSlashIx = href.indexOf("/", 1); //$NON-NLS-1$
		if (secondSlashIx < 0) // href is /pluginID
			return href.substring(1);
		// href is /pluginID/path[#anchorID]
		return href.substring(1, secondSlashIx);
	}

	/**
	 * Parses href and obtains resource path relative to the plugin
	 *
	 * @param href
	 *            String in format /string1[/[string2]][#string3]
	 * @return relative resource path, or null
	 */
	public static String getResourcePathFromHref(String href) {
		if (href == null)
			return null;
		// drop anchor id
		int anchorIx = href.lastIndexOf("#"); //$NON-NLS-1$
		if (anchorIx >= 0) //anchor exists, drop it
			href = href.substring(0, anchorIx);
		if (href.length() < 2 || href.charAt(0) != '/')
			return null;
		int secondSlashIx = href.indexOf("/", 1); //$NON-NLS-1$
		if (secondSlashIx < 0) // href is /pluginID
			return null;
		if (secondSlashIx + 1 < href.length()) // href is /pluginID/path
			return href.substring(secondSlashIx + 1);
		// href is /pluginID/
		return ""; //$NON-NLS-1$
	}

	/**
	 * Parses directory path and obtains simple form path
	 *
	 * @param href
	 *            directory path in format a/../c/1 or a//b/c
	 *            to  /c/1 and a/b/c
	 * @return normalized directory path, or null
	 */
	public static String normalizeDirectoryPath(String href) {
		if (href != null) {
			return IPath.fromOSString(href).toString();
		}
		return null;
	}
}
