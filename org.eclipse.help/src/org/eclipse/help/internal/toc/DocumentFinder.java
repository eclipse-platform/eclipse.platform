/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.toc;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.util.ResourceLocator;
import org.osgi.framework.Bundle;

public class DocumentFinder {

	public static String[] collectExtraDocuments(TocFile tocFile) {
		String dir = HrefUtil.normalizeDirectoryHref(tocFile.getPluginId(), tocFile.getExtraDir());
		String locale = tocFile.getLocale();

		List<String> result = new ArrayList<>();
		String pluginID = HrefUtil.getPluginIDFromHref(dir);
		if (pluginID == null) {
			return new String[0];
		}
		Bundle pluginDesc = Platform.getBundle(pluginID);
		if (pluginDesc == null || pluginDesc.getState() == Bundle.INSTALLED
				|| pluginDesc.getState() == Bundle.UNINSTALLED)
			return new String[0];
		String directory = HrefUtil.getResourcePathFromHref(dir);
		if (directory == null) {
			// the root - all files in a zip should be indexed
			directory = ""; //$NON-NLS-1$
		}
		// Find doc.zip file
		IPath iPath = new Path("$nl$/doc.zip"); //$NON-NLS-1$
		Map<String, String> override = new HashMap<>(1);
		override.put("$nl$", locale); //$NON-NLS-1$
		URL url = FileLocator.find(pluginDesc, iPath, override);
		if (url == null) {
			url = FileLocator.find(pluginDesc, new Path("doc.zip"), null); //$NON-NLS-1$
		}
		if (url != null) {
			// collect topics from doc.zip file
			result.addAll(collectExtraDocumentsFromZip(pluginID, directory, url));
		}

		// Find topics in plugin
		Set<String> paths = ResourceLocator.findTopicPaths(pluginDesc, directory,
				locale);
		for (Iterator<String> it = paths.iterator(); it.hasNext();) {
			String href = "/" + pluginID + "/" + it.next();  //$NON-NLS-1$//$NON-NLS-2$
			href = HrefUtil.normalizeDirectoryPath(href);
			result.add(href);
		}
		return result.toArray(new String[result.size()]);
	}

	private static List<String> collectExtraDocumentsFromZip(String pluginID, String directory,
			URL url) {
		List<String> result = new ArrayList<>();
		URL realZipURL;
		try {
			realZipURL = FileLocator.toFileURL(FileLocator.resolve(url));
			if (realZipURL.toExternalForm().startsWith("jar:")) { //$NON-NLS-1$
				// doc.zip not allowed in jarred plug-ins.
				return result;
			}
		} catch (IOException ioe) {
			Platform.getLog(DocumentFinder.class).error("IOException occurred, when resolving URL " //$NON-NLS-1$
					+ url.toString() + ".", ioe); //$NON-NLS-1$
			return result;
		}
		try (ZipFile zipFile = new ZipFile(realZipURL.getFile())) {
			result = createExtraTopicsFromZipFile(pluginID, zipFile, directory);
		} catch (IOException ioe) {
			Platform.getLog(DocumentFinder.class).error(
					"IOException occurred, when accessing Zip file " //$NON-NLS-1$
							+ realZipURL.getFile()
							+ ".  File might not be locally available.", ioe); //$NON-NLS-1$
			return new ArrayList<>();
		}
		return result;
	}

	private static List<String> createExtraTopicsFromZipFile(String pluginID, ZipFile zipFile,
			String directory) {
		String constantHrefSegment = "/" + pluginID + "/"; //$NON-NLS-1$ //$NON-NLS-2$
		List<String> result = new ArrayList<>();
		for (Enumeration<? extends ZipEntry> entriesEnum = zipFile.entries(); entriesEnum.hasMoreElements();) {
			ZipEntry zEntry = entriesEnum.nextElement();
			if (zEntry.isDirectory()) {
				continue;
			}
			String docName = zEntry.getName();
			int l = directory.length();
			if (l == 0 || docName.length() > l && docName.charAt(l) == '/'
					&& directory.equals(docName.substring(0, l))) {
				String href = constantHrefSegment + docName;
				result.add(href);
			}
		}
		return result;
	}
}
