/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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
 *     Sopot Cela - Bug 466829
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.search;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.util.Version;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.util.ProxyUtil;
import org.eclipse.help.internal.util.ResourceLocator;
import org.osgi.framework.Bundle;

public class PluginIndex {

	private static final String COMPLETE_FILENAME = "indexed_complete"; //$NON-NLS-1$

	private String pluginId;

	/**
	 * index path as defined in plugin.xml, e.g. "index"
	 */
	private String path;

	private SearchIndex targetIndex;

	/**
	 * path prefixes where index is found e.g. "", "nl/en/US", "ws/gtk"
	 */
	private List<String> indexIDs;

	/**
	 * resolved directory paths (Strings) corresponding to indexes at given
	 * prefixes, e.g. //d/eclipse/...../os/linux/index,
	 */
	private List<String> resolvedPaths;

	public PluginIndex(String pluginId, String path, SearchIndex targetIndex) {
		super();
		this.pluginId = pluginId;
		this.path = path;
		this.targetIndex = targetIndex;
	}

	private void resolve() {
		if (indexIDs != null) {
			// resolved
			return;
		}
		indexIDs = new ArrayList<>();
		resolvedPaths = new ArrayList<>();
		Bundle bundle = Platform.getBundle(pluginId);
		if (bundle == null) {
			return;
		}
		boolean found = false;
		ArrayList<String> availablePrefixes = ResourceLocator.getPathPrefix(targetIndex
				.getLocale());
		for (String prefix : availablePrefixes) {
			IPath prefixedPath = new Path(prefix + path);
			// find index at this directory in plugin or fragments
			URL url = FileLocator.find(bundle, prefixedPath, null);
			if (url == null) {
				continue;
			}
			found = true;
			if (!isCompatible(bundle, prefixedPath)) {
				continue;
			}
			URL resolved;
			try {
				resolved = FileLocator.resolve(url);
			} catch (IOException ioe) {
				Platform.getLog(getClass()).error(
						"Help index directory at " //$NON-NLS-1$
						+ prefixedPath + " for plugin " //$NON-NLS-1$
						+ bundle.getSymbolicName() + " cannot be resolved.", //$NON-NLS-1$
						ioe);
				continue;
			}
			if ("file".equals(resolved.getProtocol())) { //$NON-NLS-1$
				indexIDs.add(getIndexId(prefix));
				resolvedPaths.add(resolved.getFile());
				if (isComplete(bundle, prefixedPath)) {
					// don't process default language index
					break;
				}
			} else {
				try {
					// extract index from jarred bundles
					URL localURL = FileLocator.toFileURL(url);
					if ("file".equals(localURL.getProtocol())) { //$NON-NLS-1$
						indexIDs.add(getIndexId(prefix));
						resolvedPaths.add(localURL.getFile());
						if (isComplete(bundle, prefixedPath)) {
							// don't process default language index
							break;
						}
					}
				} catch (IOException ioe) {
					Platform.getLog(getClass())
							.error("Help index directory at " + prefixedPath + " for plugin " + bundle.getSymbolicName() //$NON-NLS-1$ //$NON-NLS-2$
									+ " cannot be resolved.", ioe); //$NON-NLS-1$
					continue;
				}
			}
		}
		if (!found) {
			Platform.getLog(getClass()).error("Help index declared, but missing for plugin " + getPluginId() + ".");//$NON-NLS-1$ //$NON-NLS-2$

		}
	}

	public boolean isCompatible(Bundle bundle, IPath prefixedPath) {
		URL url = FileLocator.find(bundle, prefixedPath.append(SearchIndex.DEPENDENCIES_VERSION_FILENAME), null);
		if (url == null) {
			Platform.getLog(getClass()).error(
					prefixedPath.append(SearchIndex.DEPENDENCIES_VERSION_FILENAME) + " file missing from help index \"" //$NON-NLS-1$
							+ path + "\" of plugin " + getPluginId(), //$NON-NLS-1$
					null);

			return false;
		}
		try (InputStream in = ProxyUtil.getStream(url)) {
			Properties prop = new Properties();
			prop.load(in);
			String lucene = prop.getProperty(SearchIndex.DEPENDENCIES_KEY_LUCENE);
			String analyzer = prop.getProperty(SearchIndex.DEPENDENCIES_KEY_ANALYZER);

			if (!targetIndex.isLuceneCompatible(lucene) || !targetIndex.isAnalyzerCompatible(analyzer)) {
				String message = "Unable to consume Lucene index from bundle '" + bundle.toString() //$NON-NLS-1$
						+ "'. The index should be rebuilt with Lucene " + Version.LATEST; //$NON-NLS-1$
				Platform.getLog(getClass()).warn(message);
				return false;
			}
		} catch (MalformedURLException mue) {
			return false;
		} catch (IOException ioe) {
			Platform.getLog(getClass()).error(
					"IOException accessing prebuilt index.", ioe); //$NON-NLS-1$
		}
		return true;
	}

	private boolean isComplete(Bundle bundle, IPath prefixedPath) {
		URL url = FileLocator.find(bundle, prefixedPath.append(COMPLETE_FILENAME), null);
		return url != null;
	}

	/**
	 * Creates id of prebuilt index
	 *
	 * @param prefix
	 *            index directory prefix, e.g. "", "ws/gtk"
	 * @return indexId string, e.g. "/", "/ws/gtk"
	 */
	private String getIndexId(String prefix) {
		if (prefix.length() == 0) {
			// root
			return "/"; //$NON-NLS-1$
		}
		return "/" + prefix.substring(0, prefix.length() - 1); //$NON-NLS-1$
	}

	@Override
	public boolean equals(Object obj) {
		if ( !(obj instanceof PluginIndex) ) {
			return false;
		}
		PluginIndex index = (PluginIndex) obj;
		return pluginId.equals(index.pluginId) && path.equals(index.path);
	}

	@Override
	public int hashCode() {
		return pluginId.hashCode() + path.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder(pluginId);
		ret.append(":"); //$NON-NLS-1$
		ret.append(path);
		ret.append("="); //$NON-NLS-1$
		if (indexIDs == null) {
			ret.append("unresolved"); //$NON-NLS-1$
		} else {
			for (int i = 0; i < indexIDs.size(); i++) {
				ret.append(indexIDs.get(i));
				ret.append("@"); //$NON-NLS-1$
				ret.append(resolvedPaths.get(i));
			}
		}
		return ret.toString();
	}

	public List<String> getIDs() {
		resolve();
		return indexIDs;
	}

	/**
	 * @return list of paths (string) to an index directory. Paths are ordered
	 *         from
	 */
	public List<String> getPaths() {
		resolve();
		return resolvedPaths;
	}

	public String getPluginId() {
		return pluginId;
	}

}
