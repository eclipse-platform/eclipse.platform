/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.help.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.IHelpContentProducer;
import org.eclipse.help.internal.HelpPlugin;
import org.osgi.framework.Bundle;

public class ResourceLocator {

	public static final String CONTENTPRODUCER_XP_NAME = "contentProducer"; //$NON-NLS-1$
	public static final String BINDING = "binding"; //$NON-NLS-1$

	public static final String CONTENTPRODUCER_XP_FULLNAME = HelpPlugin.PLUGIN_ID
			+ "." + CONTENTPRODUCER_XP_NAME; //$NON-NLS-1$

	private static Hashtable<String, Object> zipCache = new Hashtable<>();

	private static final Object ZIP_NOT_FOUND = new Object();

	// Indicates there is no dynamic content provider for a particular plugin
	private static final Object STATIC_DOCS_ONLY = ZIP_NOT_FOUND;

	// Map of document content providers by plug-in ID;
	private static Map<String, Object> contentProducers = new HashMap<>(2, 0.5f);

	static class ProducerDescriptor {

		private IHelpContentProducer producer;
		private IConfigurationElement config;

		public ProducerDescriptor(IConfigurationElement config) {
			this.config = config;
		}

		public boolean matches(String refId) {
			IExtension ex = config.getDeclaringExtension();
			String id = ex.getUniqueIdentifier();
			return id != null && id.equals(refId);
		}

		public void reset() {
			producer = null;
		}

		public IHelpContentProducer getProducer() {
			if (producer == null) {
				try {
					Object o = config.createExecutableExtension("producer"); //$NON-NLS-1$
					if (o instanceof IHelpContentProducer)
						producer = (IHelpContentProducer) o;
				} catch (CoreException ce) {
					ILog.of(getClass())
							.error("Exception occurred creating help content producer for plug-in " //$NON-NLS-1$
									+ config.getContributor().getName() + ".", ce); //$NON-NLS-1$
				}
			}
			return producer;
		}
	}
	static {
		Platform.getExtensionRegistry().addRegistryChangeListener(event -> {
			IExtensionDelta[] deltas = event.getExtensionDeltas(HelpPlugin.PLUGIN_ID, CONTENTPRODUCER_XP_NAME);
			for (int i = 0; i < deltas.length; i++) {
				IExtension extension = deltas[i].getExtension();
				String affectedPlugin = extension.getContributor().getName();
				// reset producer for the affected plugin,
				// it will be recreated on demand
				synchronized (contentProducers) {
					Object obj = contentProducers.get(affectedPlugin);
					if (obj instanceof ProducerDescriptor) {
						ProducerDescriptor desc = (ProducerDescriptor) obj;
						desc.reset();
					}
				}
			}
		});
	}

	/**
	 * Obtains content provider for a documentation plug-in, creates one if necessary.
	 *
	 * @param pluginId
	 * @return ITopicContentProvider or null
	 */
	private static IHelpContentProducer getContentProducer(String pluginId) {
		synchronized (contentProducers) {
			Object obj = getProducerDescriptor(pluginId);
			if (obj == null || obj == STATIC_DOCS_ONLY)
				return null;
			return ((ProducerDescriptor) obj).getProducer();
		}
	}

	private static Object getProducerDescriptor(String pluginId) {
		Object descriptor = contentProducers.get(pluginId);
		if (descriptor == null) {
			// first time for the plug-in, so attempt to
			// find and instantiate provider
			descriptor = createContentProducer(pluginId);
			if (descriptor == null) {
				descriptor = STATIC_DOCS_ONLY;
			}
			contentProducers.put(pluginId, descriptor);
		}
		return descriptor;
	}

	/**
	 * Creates content proivider for a documentation plug-in
	 *
	 * @param pluginId
	 * @return ITopicContentProvider or null
	 */
	private static ProducerDescriptor createContentProducer(String pluginId) {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
				CONTENTPRODUCER_XP_FULLNAME);
		if (elements.length == 0) {
			return null;
		}

		checkForDuplicateExtensionElements(elements);

		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (!elements[i].getContributor().getName().equals(pluginId)) {
				continue;
			}
			if (BINDING.equals(element.getName())) {
				// producer binding - locate the descriptor
				// with the matching reference Id
				String refId = element.getAttribute("producerId"); //$NON-NLS-1$
				if (refId != null) {
					return findContentProducer(elements, refId);
				}
			} else if (CONTENTPRODUCER_XP_NAME.equals(element.getName())) {
				return new ProducerDescriptor(element);
			}
		}
		return null;
	}

	private static boolean isCheckedForDuplicates = false;

	private static void checkForDuplicateExtensionElements(IConfigurationElement[] elements) {
		if (isCheckedForDuplicates) {
			return;
		}
		isCheckedForDuplicates = true;
		Set<String> logged = new HashSet<>();
		Set<String> keys = new HashSet<>();
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			String pluginName = element.getContributor().getName();
			String key = pluginName;
			if (logged.contains(key)) {
				continue;
			}
			if (keys.contains(key)) {
				ILog.of(ResourceLocator.class).warn(
						"Extension " + CONTENTPRODUCER_XP_FULLNAME + //$NON-NLS-1$
						"in " + pluginName + " contains more than  <" //$NON-NLS-1$ //$NON-NLS-2$
						+ CONTENTPRODUCER_XP_NAME + "> or <" //$NON-NLS-1$
						+ BINDING + "> element. All but the first have been ignored."); //$NON-NLS-1$
				logged.add(key);
			} else {
				keys.add(key);
			}
		}
	}

	private static ProducerDescriptor findContentProducer(IConfigurationElement [] elements, String refId) {
		// try existing ones
		for (Iterator<Object> iter = contentProducers.values().iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof ProducerDescriptor) {
				ProducerDescriptor desc = (ProducerDescriptor) obj;
				if (desc.matches(refId))
					return desc;
			}
		}
		// not created yet. Find the matching configuration element,
		// take its contributing pluginId and get the descriptor
		// for that plug-in
		for (int i=0; i<elements.length; i++) {
			if (CONTENTPRODUCER_XP_NAME.equals(elements[i].getName())) {
				String id = elements[i].getDeclaringExtension().getUniqueIdentifier();
				if (refId.equals(id)) {
					Object obj = getProducerDescriptor(elements[i].getContributor().getName());
					if (obj instanceof ProducerDescriptor)
						return (ProducerDescriptor)obj;
				}
			}
		}
		return null;
	}

	/**
	 * Opens an input stream to a file contained in a plugin. This includes NL lookup.
	 */
	public static InputStream openFromProducer(Bundle pluginDesc, String file, String locale) {
		IHelpContentProducer producer = getContentProducer(pluginDesc.getSymbolicName());
		if (producer == null) {
			return null;
		}
		if (locale == null || locale.length() <= 0) {
			locale = Platform.getNL();
		}
		Locale l;
		if (locale.length() >= 5) {
			l = new Locale(locale.substring(0, 2), locale.substring(3, 5));
		} else if (locale.length() >= 2) {
			l = new Locale(locale.substring(0, 2), ""); //$NON-NLS-1$
		} else {
			l = Locale.getDefault();
		}
		return producer.getInputStream(pluginDesc.getSymbolicName(), file, l);
	}

	/**
	 * Opens an input stream to a file contained in a plugin. This includes includes OS, WS and NL
	 * lookup.
	 *
	 * @param pluginId
	 *            the plugin id of the plugin that contains the file you are trying to find
	 * @param file
	 *            the relative path of the file to find
	 * @param locale
	 *            the locale used as an override or <code>null</code> to use the default locale
	 *
	 * @return an InputStream to the file or <code>null</code> if the file wasn't found
	 */
	public static InputStream openFromPlugin(String pluginId, String file, String locale) {
		Bundle bundle = Platform.getBundle(pluginId);
		if (bundle != null)
			return openFromPlugin(bundle, file, locale);
		return null;
	}

	/**
	 * Opens an input stream to a file contained in a zip in a plugin. This includes OS, WS and NL
	 * lookup.
	 *
	 * @param pluginDesc
	 *            the plugin description of the plugin that contains the file you are trying to find
	 * @param file
	 *            the relative path of the file to find
	 * @param locale
	 *            the locale used as an override or <code>null</code> to use the default locale
	 *
	 * @return an InputStream to the file or <code>null</code> if the file wasn't found
	 */
	public static InputStream openFromZip(Bundle pluginDesc, String zip, String file, String locale) {

		String pluginID = pluginDesc.getSymbolicName();
		Map<String, Object> cache = zipCache;
		ArrayList<String> pathPrefix = getPathPrefix(locale);

		for (int i = 0; i < pathPrefix.size(); i++) {

			// finds the zip file by either using a cached location, or
			// calling Platform.find - the result is cached for future use.
			Object cached = cache.get(pluginID + '/' + pathPrefix.get(i) + zip);
			if (cached == null) {
				try {
					URL url = FileLocator.find(pluginDesc, IPath.fromOSString(pathPrefix.get(i) + zip), null);
					if (url != null) {
						URL realZipURL = FileLocator.toFileURL(FileLocator.resolve(url));
						cached = realZipURL.toExternalForm();
					} else {
						cached = ZIP_NOT_FOUND;
					}
				} catch (IOException ioe) {
					cached = ZIP_NOT_FOUND;
				}
				// cache it
				cache.put(pluginID + '/' + pathPrefix.get(i) + zip, cached);
			}

			if (cached == ZIP_NOT_FOUND || cached.toString().startsWith("jar:")) //$NON-NLS-1$
				continue;

			// cached should be a zip file that is actually on the filesystem
			// now check if the file is in this zip
			try {
				URL jurl = new URL("jar", "", (String) cached + "!/" + file); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				URLConnection jconnection = jurl.openConnection();
				jconnection.setDefaultUseCaches(false);
				jconnection.setUseCaches(false);
				return jconnection.getInputStream();
			} catch (IOException ioe) {
				// a file not found exception is an io exception
				continue;
			}

		} // end for loop

		// we didn't find the file in any zip
		return null;
	}

	/**
	 * Opens an input stream to a file contained in a plugin. This includes includes OS, WS and NL
	 * lookup.
	 *
	 * @param pluginDesc
	 *            the plugin description of the plugin that contains the file you are trying to find
	 * @param file
	 *            the relative path of the file to find
	 * @param locale
	 *            the locale used as an override or <code>null</code> to use the default locale
	 *
	 * @return an InputStream to the file or <code>null</code> if the file wasn't found
	 */
	public static InputStream openFromPlugin(Bundle pluginDesc, String file, String locale) {

		ArrayList<String> pathPrefix = getPathPrefix(locale);
		URL flatFileURL = find(pluginDesc, IPath.fromOSString(file), pathPrefix);
		if (flatFileURL != null)
			try {
				return flatFileURL.openStream();
			} catch (IOException e) {
				return null;
			}
		return null;
	}



	/*
	 * Search the ws, os then nl for a resource. Platform.find can't be used directly with $nl$,
	 * $os$ or $ws$ becuase the root directory will be searched too early.
	 */
	public static URL find(Bundle pluginDesc, IPath flatFilePath, ArrayList<String> pathPrefix) {

		// try to find the actual file.
		for (int i = 0; i < pathPrefix.size(); i++) {
			URL url = FileLocator.find(pluginDesc, IPath.fromOSString(pathPrefix.get(i) + flatFilePath), null);
			if (url != null)
				return url;
		}
		return null;
	}

	public static void clearZipCache() {
		zipCache = new Hashtable<>();
	}

	/*
	 * Gets an ArrayList that has the path prefixes to search.
	 *
	 * @param locale the locale used as an override or <code>null</code> to use the default locale
	 * @return an ArrayList that has path prefixes that need to be search. The returned ArrayList
	 * will have an entry for the root of the plugin.
	 */
	public static ArrayList<String> getPathPrefix(String locale) {
		ArrayList<String> pathPrefix = new ArrayList<>(5);
		// TODO add override for ws and os similar to how it's done with locale
		// now
		String ws = Platform.getWS();
		String os = Platform.getOS();
		if (locale == null)
			locale = Platform.getNL();

		if (ws != null)
			pathPrefix.add("ws/" + ws + '/'); //$NON-NLS-1$

		if (os != null && !os.equals("OS_UNKNOWN")) //$NON-NLS-1$
			pathPrefix.add("os/" + os + '/'); //$NON-NLS-1$

		if (locale != null && locale.length() >= 5)
			pathPrefix.add("nl/" + locale.substring(0, 2) + '/' + locale.substring(3, 5) + '/'); //$NON-NLS-1$

		if (locale != null && locale.length() >= 2)
			pathPrefix.add("nl/" + locale.substring(0, 2) + '/'); //$NON-NLS-1$

		// the plugin root
		pathPrefix.add(""); //$NON-NLS-1$

		return pathPrefix;
	}

	/**
	 * Finds all topics under specified directory (recursively). This includes includes OS, WS and
	 * NL lookup.
	 *
	 * @param pluginDesc
	 *            the plugin description of the plugin that contains the file you are trying to find
	 * @param directory
	 *            the relative path of the directory
	 * @param locale
	 *            the locale used as an override or <code>null</code> to use the default locale
	 *
	 * @return an InputStream to the file or <code>null</code> if the file wasn't found
	 */
	public static Set<String> findTopicPaths(Bundle pluginDesc, String directory, String locale) {
		Set<String> ret = new HashSet<>();
		findTopicPaths(pluginDesc, directory, locale, ret);
		return ret;
	}

	/**
	 * @param pluginDesc
	 * @param directory
	 * @param locale
	 * @param paths
	 */
	private static void findTopicPaths(Bundle pluginDesc, String directory, String locale, Set<String> paths) {
		if (directory.endsWith("/")) //$NON-NLS-1$
			directory = directory.substring(0, directory.length() - 1);
		ArrayList<String> pathPrefix = getPathPrefix(locale);
		for (int i = 0; i < pathPrefix.size(); i++) {
			String path = pathPrefix.get(i) + directory;
			if (path.length() == 0)
				path = "/"; //$NON-NLS-1$
			Enumeration<String> entries = pluginDesc.getEntryPaths(path);
			if (entries != null) {
				while (entries.hasMoreElements()) {
					String topicPath = entries.nextElement();
					if (topicPath.endsWith("/")) { //$NON-NLS-1$
						findTopicPaths(pluginDesc, topicPath, locale, paths);
					} else {
						paths.add(topicPath);
					}
				}
			}
		}
	}

	/**
	 * Create a path for use in error messages that will identify the plugin and
	 * file name as well as a resolved path (if available) which will give
	 * information about which fragment the file was located in
	 * @return pluginId/file followed by a resolved path if the file exists
	 */
	public static String getErrorPath(String pluginId, String file, String locale)  {
		String resolvedPath = pluginId + '/' + file;
		try {
			ArrayList<String> pathPrefix = ResourceLocator.getPathPrefix(locale);
			Bundle bundle = Platform.getBundle(pluginId);
			URL rawURL = ResourceLocator.find(bundle, IPath.fromOSString(file), pathPrefix);
			URL resolvedURL = FileLocator.resolve(rawURL);
			resolvedPath += ", URL = " + resolvedURL.toExternalForm(); //$NON-NLS-1$
		} catch (Exception e) {
		}
		return resolvedPath;
	}
}
