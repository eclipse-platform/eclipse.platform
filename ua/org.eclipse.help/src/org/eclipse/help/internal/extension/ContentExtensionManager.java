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
package org.eclipse.help.internal.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.AbstractContentExtensionProvider;
import org.eclipse.help.IContentExtension;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.UAElementFactory;

/*
 * Manages content extensions (contributions into anchors and element
 * replacements) for user assistance documents.
 */
public class ContentExtensionManager {

	private static final String EXTENSION_POINT_ID_CONTENT_EXTENSION = HelpPlugin.PLUGIN_ID + ".contentExtension"; //$NON-NLS-1$
	private static final String ELEMENT_NAME_CONTENT_EXTENSION_PROVIDER = "contentExtensionProvider"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_CLASS = "class"; //$NON-NLS-1$
	private static final ContentExtension[] EMPTY_ARRAY = new ContentExtension[0];

	private AbstractContentExtensionProvider[] contentExtensionProviders;
	private Map<String, List<ContentExtension>> extensionsByPath;
	private Map<String, List<ContentExtension>> replacesByPath;

	/*
	 * Returns all known extensions for the given locale.
	 */
	public ContentExtension[] getExtensions(String locale) {
		if (extensionsByPath == null) {
			loadExtensions(locale);
		}
		List<ContentExtension> extensions = new ArrayList<>();
		Iterator<List<ContentExtension>> iter = extensionsByPath.values().iterator();
		while (iter.hasNext()) {
			extensions.addAll(iter.next());
		}
		iter = replacesByPath.values().iterator();
		while (iter.hasNext()) {
			extensions.addAll(iter.next());
		}
		return extensions.toArray(new ContentExtension[extensions.size()]);
	}

	/*
	 * Get all extensions of the given type whose target matches the given path.
	 */
	public ContentExtension[] getExtensions(String path, int type, String locale) {
		if (extensionsByPath == null) {
			loadExtensions(locale);
		}
		Map<String, List<ContentExtension>> map = (type == IContentExtension.CONTRIBUTION ? extensionsByPath
				: replacesByPath);
		List<ContentExtension> extensions = map.get(path);
		if (extensions != null) {
			return extensions.toArray(new ContentExtension[extensions.size()]);
		}
		return EMPTY_ARRAY;
	}

	/*
	 * Clears all cached data, forcing the manager to query the
	 * providers again next time a request is made.
	 */
	public void clearCache() {
		if (extensionsByPath != null) {
			extensionsByPath.clear();
		}
		if (replacesByPath != null) {
			replacesByPath.clear();
		}
	}

	/*
	 * Retrieves all extensions from all providers and organizes them by
	 * type.
	 */
	private void loadExtensions(String locale) {
		extensionsByPath = new HashMap<>();
		replacesByPath = new HashMap<>();
		contentExtensionProviders = getContentExtensionProviders();
		for (int i=0;i<contentExtensionProviders.length;++i) {
			IContentExtension[] extensions = contentExtensionProviders[i].getContentExtensions(locale);
			for (int j=0;j<extensions.length;++j) {
				ContentExtension extension = (extensions[j] instanceof ContentExtension ? (ContentExtension)extensions[j] : (ContentExtension)UAElementFactory.newElement(extensions[j]));
				String content = extension.getContent();
				String path = extension.getPath();
				if (content != null && path != null) {
					int type = extension.getType();
					Map<String, List<ContentExtension>> map = (type == IContentExtension.CONTRIBUTION ? extensionsByPath
							: replacesByPath);
					content = normalizePath(content);
					path = normalizePath(path);
					extension.setContent(content);
					extension.setPath(path);
					List<ContentExtension> list = map.get(path);
					if (list == null) {
						list = new ArrayList<>();
						map.put(path, list);
					}
					list.add(extension);
				}
			}
		}
	}

	/*
	 * Returns all registered content extension providers (potentially cached).
	 */
	private AbstractContentExtensionProvider[] getContentExtensionProviders() {
		if (contentExtensionProviders == null) {
			List<AbstractContentExtensionProvider> providers = new ArrayList<>();
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_POINT_ID_CONTENT_EXTENSION);
			for (int i=0;i<elements.length;++i) {
				IConfigurationElement elem = elements[i];
				if (elem.getName().equals(ELEMENT_NAME_CONTENT_EXTENSION_PROVIDER)) {
					try {
						AbstractContentExtensionProvider provider = (AbstractContentExtensionProvider)elem.createExecutableExtension(ATTRIBUTE_NAME_CLASS);
						providers.add(provider);
					}
					catch (CoreException e) {
						// log and skip
						String msg = "Error instantiating user assistance content extension provider class \"" + elem.getAttribute(ATTRIBUTE_NAME_CLASS) + '"'; //$NON-NLS-1$
						ILog.of(getClass()).error(msg, e);
					}
				}
			}
			contentExtensionProviders = providers.toArray(new AbstractContentExtensionProvider[providers.size()]);
		}
		return contentExtensionProviders;
	}

	/*
	 * Normalizes the given path by adding a leading slash if one doesn't
	 * exist, and converting the final slash into a '#' if it is thought to
	 * separate the end of the document with the element (legacy form).
	 */
	private String normalizePath(String path) {
		int bundleStart, bundleEnd;
		int pathStart, pathEnd;
		int elementStart, elementEnd;

		bundleStart = path.charAt(0) == '/' ? 1 : 0;
		bundleEnd = path.indexOf('/', bundleStart);

		pathStart = bundleEnd + 1;
		pathEnd = path.indexOf('#', pathStart);
		if (pathEnd == -1) {
			int lastSlash = path.lastIndexOf('/');
			if (lastSlash > 0) {
				int secondLastSlash = path.lastIndexOf('/', lastSlash - 1);
				if (secondLastSlash != -1) {
					String secondLastToken = path.substring(secondLastSlash, lastSlash);
					boolean hasDot = (secondLastToken.indexOf('.') != -1);
					if (hasDot) {
						pathEnd = lastSlash;
					}
				}
			}
			if (pathEnd == -1) {
				pathEnd = path.length();
			}
		}

		elementStart = Math.min(pathEnd + 1, path.length());
		elementEnd = path.length();

		if (bundleEnd > bundleStart && pathStart > bundleEnd && pathEnd > pathStart && elementStart >= pathEnd && elementEnd >= elementStart) {
			String bundleId = path.substring(bundleStart, bundleEnd);
			String relativePath = path.substring(pathStart, pathEnd);
			String elementId = path.substring(elementStart, elementEnd);
			path = '/' + bundleId + '/' + relativePath;
			if (elementId.length() > 0) {
				path += '#' + elementId;
			}
		}
		return path;
	}
}
