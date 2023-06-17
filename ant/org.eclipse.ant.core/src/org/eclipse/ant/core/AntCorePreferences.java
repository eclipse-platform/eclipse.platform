/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Thierry Lach (thierry.lach@bbdodetroit.com) - bug 40502
 *     Ericsson AB, Hamdan Msheik - Bug 389564
 *     Ericsson AB, Julian Enoch - Bug 389564
 *******************************************************************************/
package org.eclipse.ant.core;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.ant.internal.core.AntClasspathEntry;
import org.eclipse.ant.internal.core.AntObject;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.core.InternalCoreAntMessages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Represents the Ant Core plug-in's preferences providing utilities for extracting, changing and updating the underlying preferences. Clients may not
 * instantiate or subclass this class.
 *
 * @since 2.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
@SuppressWarnings("deprecation")
public class AntCorePreferences implements IPropertyChangeListener {

	static class WrappedClassLoader extends ClassLoader {
		private Bundle bundle;

		public WrappedClassLoader(Bundle bundle) {
			super();
			this.bundle = bundle;
		}

		@Override
		public Class<?> findClass(String name) throws ClassNotFoundException {
			return bundle.loadClass(name);
		}

		@Override
		public URL findResource(String name) {
			return bundle.getResource(name);
		}

		@Override
		protected Enumeration<URL> findResources(String name) throws IOException {
			return bundle.getResources(name);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof WrappedClassLoader)) {
				return false;
			}
			return bundle == ((WrappedClassLoader) obj).bundle;
		}

		@Override
		public int hashCode() {
			return bundle.hashCode();
		}

		@Override
		public String toString() {
			return "WrappedClassLoader(" + bundle.toString() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	static private class Relation {
		BundleRevision from;
		BundleRevision to;

		Relation(BundleRevision from, BundleRevision to) {
			this.from = from;
			this.to = to;
		}

		@Override
		public String toString() {
			return from.toString() + "->" + (to == null ? IAntCoreConstants.EMPTY_STRING : to.toString()); //$NON-NLS-1$
		}
	}

	private IPreferenceChangeListener prefListener = event -> {
		String property = event.getKey();
		if (property.equals(IAntCoreConstants.PREFERENCE_TASKS) || property.startsWith(IAntCoreConstants.PREFIX_TASK)) {
			restoreTasks();
		} else if (property.equals(IAntCoreConstants.PREFERENCE_TYPES) || property.startsWith(IAntCoreConstants.PREFIX_TYPE)) {
			restoreTypes();
		} else if (property.equals(IAntCoreConstants.PREFERENCE_ANT_HOME_ENTRIES)) {
			restoreAntHomeEntries();
		} else if (property.equals(IAntCoreConstants.PREFERENCE_ADDITIONAL_ENTRIES)) {
			restoreAdditionalEntries();
		} else if (property.equals(IAntCoreConstants.PREFERENCE_ANT_HOME)) {
			restoreAntHome();
		} else if (property.equals(IAntCoreConstants.PREFERENCE_PROPERTIES) || property.startsWith(IAntCoreConstants.PREFIX_PROPERTY)) {
			restoreCustomProperties();
		} else if (property.equals(IAntCoreConstants.PREFERENCE_PROPERTY_FILES)) {
			restoreCustomPropertyFiles();
		}
	};

	private List<Task> defaultTasks;
	private List<Type> defaultTypes;
	private List<AntClasspathEntry> extraClasspathURLs;
	private List<Property> defaultProperties;
	private IAntClasspathEntry[] defaultAntHomeEntries;

	private Task[] customTasks;
	private Task[] oldCustomTasks;
	private Type[] customTypes;
	private Type[] oldCustomTypes;
	private IAntClasspathEntry[] antHomeEntries;
	private IAntClasspathEntry[] additionalEntries;
	private Property[] customProperties;
	private Property[] oldCustomProperties;
	private String[] customPropertyFiles;

	private List<WrappedClassLoader> pluginClassLoaders;

	private ClassLoader[] orderedPluginClassLoaders;

	private String antHome;

	private boolean runningHeadless = false;

	protected AntCorePreferences(List<IConfigurationElement> defaultTasks, List<IConfigurationElement> defaultExtraClasspath, List<IConfigurationElement> defaultTypes, boolean headless) {
		this(defaultTasks, defaultExtraClasspath, defaultTypes, Collections.<IConfigurationElement> emptyList(), headless);
	}

	protected AntCorePreferences(List<IConfigurationElement> defaultTasks, List<IConfigurationElement> defaultExtraClasspath, List<IConfigurationElement> defaultTypes, List<IConfigurationElement> defaultProperties, boolean headless) {
		runningHeadless = headless;
		initializePluginClassLoaders();
		extraClasspathURLs = new ArrayList<>(20);
		this.defaultTasks = computeDefaultTasks(defaultTasks);
		this.defaultTypes = computeDefaultTypes(defaultTypes);
		computeDefaultExtraClasspathEntries(defaultExtraClasspath);
		computeDefaultProperties(defaultProperties);
		restoreCustomObjects();

	}

	/**
	 * When a preference changes, update the in-memory cache of the preference.
	 *
	 * @param event
	 *            The property change event that has occurred.
	 * @see org.eclipse.core.runtime.Preferences.IPropertyChangeListener#propertyChange(org.eclipse.core.runtime.Preferences.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(Preferences.PropertyChangeEvent event) {
		// does nothing any longer, see the IPreferenceChangedListener field
	}

	/**
	 * Restores the in-memory model of the preferences from the preference store
	 */
	private void restoreCustomObjects() {
		restoreAntHome();
		restoreTasks();
		restoreTypes();
		restoreAntHomeEntries();
		restoreAdditionalEntries();
		restoreCustomProperties();
		restoreCustomPropertyFiles();
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(AntCorePlugin.PI_ANTCORE);
		if (node != null) {
			node.addPreferenceChangeListener(prefListener);
		}
	}

	private void restoreTasks() {
		String tasks = Platform.getPreferencesService().getString(AntCorePlugin.PI_ANTCORE, IAntCoreConstants.PREFERENCE_TASKS, null, null);
		if (tasks == null || IAntCoreConstants.EMPTY_STRING.equals(tasks)) {
			customTasks = new Task[0];
		} else {
			customTasks = extractTasks(AntCorePlugin.getPlugin().getPluginPreferences(), getArrayFromString(tasks));
		}
	}

	private void restoreTypes() {
		String types = Platform.getPreferencesService().getString(AntCorePlugin.PI_ANTCORE, IAntCoreConstants.PREFERENCE_TYPES, null, null);
		if (types == null || IAntCoreConstants.EMPTY_STRING.equals(types)) {
			customTypes = new Type[0];
		} else {
			customTypes = extractTypes(AntCorePlugin.getPlugin().getPluginPreferences(), getArrayFromString(types));
		}
	}

	private void restoreAntHomeEntries() {
		String entries = Platform.getPreferencesService().getString(AntCorePlugin.PI_ANTCORE, "ant_urls", //$NON-NLS-1$
				null, null); // old constant
		if (entries == null || IAntCoreConstants.EMPTY_STRING.equals(entries)) {
			entries = Platform.getPreferencesService().getString(AntCorePlugin.PI_ANTCORE, IAntCoreConstants.PREFERENCE_ANT_HOME_ENTRIES, null, null);
		} else {
			// torch the old pref
			IEclipsePreferences node = InstanceScope.INSTANCE.getNode(AntCorePlugin.PI_ANTCORE);
			if (node != null) {
				node.remove("ant_urls"); //$NON-NLS-1$
				try {
					node.flush();
				}
				catch (BackingStoreException e) {
					// do nothing
				}
			}
			antHomeEntries = migrateURLEntries(getArrayFromString(entries));
			return;
		}
		if (entries == null || IAntCoreConstants.EMPTY_STRING.equals(entries)) {
			antHomeEntries = getDefaultAntHomeEntries();
		} else {
			antHomeEntries = extractEntries(getArrayFromString(entries));
		}
	}

	private void restoreAdditionalEntries() {
		String entries = Platform.getPreferencesService().getString(AntCorePlugin.PI_ANTCORE, "urls", //$NON-NLS-1$
				null, null); // old constant
		if (entries == null || IAntCoreConstants.EMPTY_STRING.equals(entries)) {
			entries = Platform.getPreferencesService().getString(AntCorePlugin.PI_ANTCORE, IAntCoreConstants.PREFERENCE_ADDITIONAL_ENTRIES, null, null);
		} else {
			IEclipsePreferences node = InstanceScope.INSTANCE.getNode(AntCorePlugin.PI_ANTCORE);
			if (node != null) {
				node.remove("urls"); //$NON-NLS-1$
				try {
					node.flush();
				}
				catch (BackingStoreException e) {
					// do nothing
				}
			}
			additionalEntries = migrateURLEntries(getArrayFromString(entries));
			return;
		}
		if (entries == null || IAntCoreConstants.EMPTY_STRING.equals(entries)) {
			IAntClasspathEntry toolsJarEntry = getToolsJarEntry();
			List<IAntClasspathEntry> userLibs = getUserLibraries();
			if (toolsJarEntry == null) {
				if (userLibs == null) {
					additionalEntries = new IAntClasspathEntry[0];
				} else {
					additionalEntries = userLibs.toArray(new IAntClasspathEntry[userLibs.size()]);
				}
			} else {
				if (userLibs == null) {
					additionalEntries = new IAntClasspathEntry[] { toolsJarEntry };
				} else {
					userLibs.add(toolsJarEntry);
					additionalEntries = userLibs.toArray(new IAntClasspathEntry[userLibs.size()]);
				}
			}
		} else {
			additionalEntries = extractEntries(getArrayFromString(entries));
		}
	}

	/*
	 * Migrates the persisted URL entries restored from a workspace older than 3.0
	 */
	private IAntClasspathEntry[] migrateURLEntries(String[] urlEntries) {
		List<AntClasspathEntry> result = new ArrayList<>(urlEntries.length);
		for (String urlEntry : urlEntries) {
			URL url;
			try {
				url = new URL(urlEntry);
			}
			catch (MalformedURLException e) {
				continue;
			}
			result.add(new AntClasspathEntry(url));
		}
		return result.toArray(new IAntClasspathEntry[result.size()]);
	}

	private void restoreAntHome() {
		antHome = Platform.getPreferencesService().getString(AntCorePlugin.PI_ANTCORE, IAntCoreConstants.PREFERENCE_ANT_HOME, null, null);
		if (antHome == null || IAntCoreConstants.EMPTY_STRING.equals(antHome)) {
			antHome = getDefaultAntHome();
		}
	}

	/**
	 * Returns the absolute path of the default ant.home to use for the build. The default is the org.apache.ant plug-in folder provided with Eclipse.
	 *
	 * @return String absolute path of the default ant.home
	 * @since 3.0
	 */
	public String getDefaultAntHome() {
		IAntClasspathEntry[] entries = getDefaultAntHomeEntries();
		if (entries.length > 0) {
			URL antjar = entries[entries.length - 1].getEntryURL();
			IPath antHomePath = IPath.fromOSString(antjar.getFile());
			// parent directory of the lib directory
			antHomePath = antHomePath.removeLastSegments(2);
			return antHomePath.toFile().getAbsolutePath();
		}
		return null;
	}

	private void restoreCustomProperties() {
		String properties = Platform.getPreferencesService().getString(AntCorePlugin.PI_ANTCORE, IAntCoreConstants.PREFERENCE_PROPERTIES, null, null);
		if (properties == null || IAntCoreConstants.EMPTY_STRING.equals(properties)) {
			customProperties = new Property[0];
		} else {
			customProperties = extractProperties(AntCorePlugin.getPlugin().getPluginPreferences(), getArrayFromString(properties));
		}
	}

	private void restoreCustomPropertyFiles() {
		String propertyFiles = Platform.getPreferencesService().getString(AntCorePlugin.PI_ANTCORE, IAntCoreConstants.PREFERENCE_PROPERTY_FILES, null, null);
		if (propertyFiles == null || IAntCoreConstants.EMPTY_STRING.equals(propertyFiles)) {
			customPropertyFiles = new String[0];
		} else {
			customPropertyFiles = getArrayFromString(propertyFiles);
		}
	}

	protected Task[] extractTasks(Preferences prefs, String[] tasks) {
		List<Task> result = new ArrayList<>(tasks.length);
		for (String taskName : tasks) {
			String[] values = getArrayFromString(prefs.getString(IAntCoreConstants.PREFIX_TASK + taskName));
			if (values.length < 2) {
				continue;
			}
			Task task = new Task();
			task.setTaskName(taskName);
			task.setClassName(values[0]);
			String library = values[1];
			if (library.startsWith(IAntCoreConstants.FILE_PROTOCOL)) {
				// old format where URLs were persisted
				library = library.substring(5);
			}
			task.setLibraryEntry(new AntClasspathEntry(library));
			result.add(task);
		}
		return result.toArray(new Task[result.size()]);
	}

	protected Type[] extractTypes(Preferences prefs, String[] types) {
		List<Type> result = new ArrayList<>(types.length);
		for (String typeName : types) {
			String[] values = getArrayFromString(prefs.getString(IAntCoreConstants.PREFIX_TYPE + typeName));
			if (values.length < 2) {
				continue;
			}
			Type type = new Type();
			type.setTypeName(typeName);
			type.setClassName(values[0]);
			String library = values[1];
			if (library.startsWith(IAntCoreConstants.FILE_PROTOCOL)) {
				// old format where URLs were persisted
				library = library.substring(5);
			}
			type.setLibraryEntry(new AntClasspathEntry(library));
			result.add(type);
		}
		return result.toArray(new Type[result.size()]);
	}

	protected Property[] extractProperties(Preferences prefs, String[] properties) {
		Property[] result = new Property[properties.length];
		for (int i = 0; i < properties.length; i++) {
			String propertyName = properties[i];
			String value = prefs.getString(IAntCoreConstants.PREFIX_PROPERTY + propertyName);
			Property property = new Property();
			property.setName(propertyName);
			property.setValue(value);
			result[i] = property;
		}
		return result;
	}

	private IAntClasspathEntry[] extractEntries(String[] entries) {
		IAntClasspathEntry[] result = new IAntClasspathEntry[entries.length];
		for (int i = 0; i < entries.length; i++) {
			result[i] = new AntClasspathEntry(entries[i]);
		}
		return result;
	}

	/**
	 * Returns the array of URLs that is the default set of URLs defining the Ant classpath.
	 *
	 * Ant running through the command line tries to find tools.jar to help the user. Try emulating the same behavior here.
	 *
	 * @return the default set of URLs defining the Ant classpath
	 * @deprecated use {@link #getDefaultAntHomeEntries()} instead
	 */
	@Deprecated
	public URL[] getDefaultAntURLs() {
		List<URL> result = new ArrayList<>(3);
		for (IAntClasspathEntry entry : getDefaultAntHomeEntries()) {
			result.add(entry.getEntryURL());
		}
		URL toolsURL = getToolsJarURL();
		if (toolsURL != null) {
			result.add(toolsURL);
		}
		return result.toArray(new URL[result.size()]);
	}

	/**
	 * Returns the array of classpath entries that is the default set of entries defining the Ant classpath.
	 *
	 * @return the default set of classpath entries defining the Ant classpath
	 */
	public synchronized IAntClasspathEntry[] getDefaultAntHomeEntries() {
		if (defaultAntHomeEntries == null) {
			ServiceTracker<?, ?> tracker = new ServiceTracker<>(AntCorePlugin.getPlugin().getBundle().getBundleContext(), PackageAdmin.class.getName(), null);
			tracker.open();
			try {
				List<AntClasspathEntry> result = new ArrayList<>(29);
				PackageAdmin packageAdmin = (PackageAdmin) tracker.getService();
				if (packageAdmin != null) {
					ExportedPackage[] packages = packageAdmin.getExportedPackages("org.apache.tools.ant"); //$NON-NLS-1$
					Bundle bundle = findHighestAntVersion(packages);
					if (bundle == null) {
						for (ExportedPackage pckg : packages) {
							bundle = pckg.getExportingBundle();
							if (bundle == null) {
								continue;
							}
							try {
								addLibraries(bundle, result);
								if (result.size() > 0) {
									break;
								}
							}
							catch (IOException ioe) {
								AntCorePlugin.log(ioe); // maintain logging
								result.clear();
								/* continue to try other providers if an exception occurs */
							}
						}
					} else {
						try {
							addLibraries(bundle, result);
						}
						catch (IOException ioe) {
							AntCorePlugin.log(ioe); // maintain logging
						}
					}
				}
				defaultAntHomeEntries = result.toArray(new IAntClasspathEntry[result.size()]);
			}
			finally {
				tracker.close();
			}
		}
		return defaultAntHomeEntries;
	}

	/**
	 * Simple algorithm to find the highest version of <code>org.apache.ant</code> available. If there are other providers that are not
	 * <code>org.apache.ant</code> they are ignored and all versions of <code>org.apache.ant</code> are considered. <br>
	 * <br>
	 * See the following bugs for related history:
	 * <ul>
	 * <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=282851">https://bugs.eclipse.org/bugs/show_bug.cgi?id=282851</a></li>
	 * <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=325125">https://bugs.eclipse.org/bugs/show_bug.cgi?id=325125</a></li>
	 * </ul>
	 *
	 * @param packages
	 *            the live list of {@link ExportedPackage}s to inspect
	 * @return the bundle that represents the highest version of <code>org.apache.ant</code> or <code>null</code> if there are no
	 *         <code>org.apache.ant</code> providers of the <code>org.apache.ant.tools</code> package.
	 */
	Bundle findHighestAntVersion(ExportedPackage[] packages) {
		Bundle bundle = null;
		HashSet<Bundle> bundles = new HashSet<>();
		for (ExportedPackage pckg : packages) {
			bundle = pckg.getExportingBundle();
			if (bundle == null) {
				continue;
			}
			if ("org.apache.ant".equals(bundle.getSymbolicName())) { //$NON-NLS-1$
				bundles.add(bundle);
			}
		}
		Bundle highest = null;
		Bundle temp = null;
		for (Iterator<Bundle> iter = bundles.iterator(); iter.hasNext();) {
			temp = iter.next();
			if (highest == null) {
				highest = temp;
			} else {
				if (highest.getVersion().compareTo(temp.getVersion()) < 0) {
					highest = temp;
				}
			}
		}
		return highest;
	}

	/**
	 * Returns the array of URLs that is the set of URLs defining the Ant classpath.
	 *
	 * @return the set of URLs defining the Ant classpath
	 * @deprecated use getAntHomeClasspathEntries and getToolsJarEntry
	 */
	@Deprecated
	public URL[] getAntURLs() {
		int extra = 0;
		IAntClasspathEntry entry = getToolsJarEntry();
		if (entry != null) {
			extra++;
		}
		URL[] urls = new URL[antHomeEntries.length + extra];
		int i;
		for (i = 0; i < antHomeEntries.length; i++) {
			URL url = antHomeEntries[i].getEntryURL();
			if (url != null) {
				urls[i] = url;
			}
		}
		if (entry != null) {
			urls[i] = entry.getEntryURL();
		}
		return urls;

	}

	/**
	 * Returns the complete list of pre-configured {@link Task}s
	 *
	 * @param tasks
	 *            the {@link IConfigurationElement} handles for contributed {@link Task}s
	 * @return the list of {@link Task}s
	 */
	protected List<Task> computeDefaultTasks(List<IConfigurationElement> tasks) {
		List<Task> result = new ArrayList<>(tasks.size());
		for (IConfigurationElement element : tasks) {
			if (!relevantRunningHeadless(element)) {
				continue;
			}
			Task task = new Task();
			task.setTaskName(element.getAttribute(IAntCoreConstants.NAME));
			task.setClassName(element.getAttribute(AntCorePlugin.CLASS));

			if (configureAntObject(element, task, task.getTaskName(), InternalCoreAntMessages.AntCorePreferences_No_library_for_task)) {
				result.add(task);
			}
		}
		return result;
	}

	private void addURLToExtraClasspathEntries(URL url, IConfigurationElement element) {
		String eclipseRuntime = element.getAttribute(AntCorePlugin.ECLIPSE_RUNTIME);
		boolean eclipseRuntimeRequired = true;
		if (eclipseRuntime != null) {
			eclipseRuntimeRequired = Boolean.parseBoolean(eclipseRuntime);
		}
		Iterator<AntClasspathEntry> itr = extraClasspathURLs.iterator();
		while (itr.hasNext()) {
			IAntClasspathEntry entry = itr.next();
			if (entry.getEntryURL().equals(url)) {
				return;
			}
		}

		AntClasspathEntry entry = new AntClasspathEntry(url);
		entry.setEclipseRuntimeRequired(eclipseRuntimeRequired);
		extraClasspathURLs.add(entry);
	}

	/**
	 * Returns the complete listing of pre-configured {@link Type}s
	 *
	 * @param types
	 *            the list of {@link IConfigurationElement} handles to contributed {@link Type}s
	 * @return the list of {@link Type}s
	 */
	protected List<Type> computeDefaultTypes(List<IConfigurationElement> types) {
		List<Type> result = new ArrayList<>(types.size());
		for (IConfigurationElement element : types) {
			if (!relevantRunningHeadless(element)) {
				continue;
			}
			Type type = new Type();
			type.setTypeName(element.getAttribute(IAntCoreConstants.NAME));
			type.setClassName(element.getAttribute(AntCorePlugin.CLASS));

			if (configureAntObject(element, type, type.getTypeName(), InternalCoreAntMessages.AntCorePreferences_No_library_for_type)) {
				result.add(type);
			}
		}
		return result;
	}

	/*
	 * Create a "file:" URL for the specified File making sure the URL ends with a slash if the File denotes a directory.
	 */
	private URL getClasspathEntryURL(Bundle bundle, String library) throws IOException {
		File urlFile = null;
		if (library.equals("/")) { //$NON-NLS-1$
			urlFile = FileLocator.getBundleFileLocation(bundle).orElseThrow();
		} else {
			try {
				URL fileURL = FileLocator.toFileURL(bundle.getEntry(library));
				if (fileURL != null) {
					urlFile = URIUtil.toFile(URIUtil.toURI(fileURL));
				}
			}
			catch (URISyntaxException e) {
				AntCorePlugin.log(e);
			}
		}

		if (urlFile == null || !urlFile.exists())
			return null;

		String path = urlFile.getAbsolutePath();
		return new URL(IAntCoreConstants.FILE_PROTOCOL + (urlFile.isDirectory() ? path + "/" : path)); //$NON-NLS-1$
	}

	/**
	 * Configures the given {@link AntObject} and returns if it should be retained
	 *
	 * @param element
	 * @param antObject
	 * @param objectName
	 * @param errorMessage
	 * @return <code>true</code> if the object configured and should be retained, <code>false</code> otherwise
	 */
	private boolean configureAntObject(IConfigurationElement element, AntObject antObject, String objectName, String errorMessage) {
		String runtime = element.getAttribute(AntCorePlugin.ECLIPSE_RUNTIME);
		if (runtime != null) {
			antObject.setEclipseRuntimeRequired(Boolean.parseBoolean(runtime));
		}

		String uri = element.getAttribute(AntCorePlugin.URI);
		if (uri != null) {
			antObject.setURI(uri);
		}

		String library = element.getAttribute(AntCorePlugin.LIBRARY);
		if (library == null) {
			IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, NLS.bind(InternalCoreAntMessages.AntCorePreferences_Library_not_specified_for___0__4, new String[] {
					objectName }), null);
			AntCorePlugin.getPlugin().getLog().log(status);
			return false;
		}

		try {
			IContributor contributor = element.getContributor();
			antObject.setPluginLabel(contributor.getName());
			Bundle bundle = Platform.getBundle(contributor.getName());
			URL url = getClasspathEntryURL(bundle, library);
			if (url != null) {
				addURLToExtraClasspathEntries(url, element);
				addPluginClassLoader(bundle);
				antObject.setLibraryEntry(new AntClasspathEntry(url));
				return true;
			}

			// type specifies a library that does not exist
			IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, NLS.bind(errorMessage, new String[] {
					library, element.getContributor().getName() }), null);
			AntCorePlugin.getPlugin().getLog().log(status);
			return false;
		}
		catch (MalformedURLException e) {
			// if the URL does not have a valid format, just log and ignore the exception
			IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_MALFORMED_URL, InternalCoreAntMessages.AntCorePreferences_Malformed_URL__1, e);
			AntCorePlugin.getPlugin().getLog().log(status);
		}
		catch (Exception e) {
			// likely extra classpath entry library that does not exist
			IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, NLS.bind(InternalCoreAntMessages.AntCorePreferences_8, new String[] {
					library, element.getContributor().getName() }), null);
			AntCorePlugin.getPlugin().getLog().log(status);
		}
		return false;
	}

	/*
	 * Computes the extra classpath entries defined plug-ins and fragments.
	 */
	protected void computeDefaultExtraClasspathEntries(List<IConfigurationElement> entries) {
		for (IConfigurationElement element : entries) {
			if (!relevantRunningHeadless(element)) {
				continue;
			}
			String library = element.getAttribute(AntCorePlugin.LIBRARY);
			Bundle bundle = Platform.getBundle(element.getContributor().getName());
			try {
				URL url = getClasspathEntryURL(bundle, library);
				if (url != null) {
					addURLToExtraClasspathEntries(url, element);
					addPluginClassLoader(bundle);
				} else {
					// extra classpath entry that does not exist
					IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, NLS.bind(InternalCoreAntMessages.AntCorePreferences_6, new String[] {
							library, element.getContributor().getName() }), null);
					AntCorePlugin.getPlugin().getLog().log(status);
					continue;
				}
			}
			catch (MalformedURLException e) {
				// if the URL does not have a valid format, just log and ignore the exception
				IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_MALFORMED_URL, InternalCoreAntMessages.AntCorePreferences_Malformed_URL__1, e);
				AntCorePlugin.getPlugin().getLog().log(status);
				continue;
			}
			catch (Exception e) {
				// likely extra classpath entry that does not exist
				IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, NLS.bind(InternalCoreAntMessages.AntCorePreferences_6, new String[] {
						library, element.getContributor().getName() }), null);
				AntCorePlugin.getPlugin().getLog().log(status);
				continue;
			}
		}
	}

	private boolean relevantRunningHeadless(IConfigurationElement element) {
		if (runningHeadless) {
			String headless = element.getAttribute(AntCorePlugin.HEADLESS);
			if (headless != null) {
				boolean headlessProperty = Boolean.parseBoolean(headless);
				if (!headlessProperty) {
					return false;
				}
			}
		}
		return true;
	}

	/*
	 * Scan the Ant property extensions for properties to set.
	 *
	 * @since 3.0
	 */
	private void computeDefaultProperties(List<IConfigurationElement> properties) {
		defaultProperties = new ArrayList<>(properties.size());
		for (IConfigurationElement element : properties) {
			if (!relevantRunningHeadless(element)) {
				continue;
			}
			String name = element.getAttribute(IAntCoreConstants.NAME);
			if (name == null) {
				continue;
			}
			String value = element.getAttribute(IAntCoreConstants.VALUE);
			Property property = null;
			if (value != null) {
				property = new Property(name, value);
				property.setPluginLabel(element.getContributor().getName());
			} else {
				Bundle bundle = Platform.getBundle(element.getContributor().getName());
				if (bundle == null) {
					continue;
				}
				property = new Property();
				property.setName(name);
				property.setPluginLabel(element.getContributor().getName());
				String className = element.getAttribute(AntCorePlugin.CLASS);
				property.setValueProvider(className, getClassLoader(bundle));
			}
			defaultProperties.add(property);
			String runtime = element.getAttribute(AntCorePlugin.ECLIPSE_RUNTIME);
			if (runtime != null) {
				property.setEclipseRuntimeRequired(Boolean.parseBoolean(runtime));
			}
		}
	}

	private WrappedClassLoader getClassLoader(Bundle b) {
		return new WrappedClassLoader(b);
	}

	/**
	 * Returns the IAntClasspathEntry for the tools.jar associated with the path supplied. May return <code>null</code> if no tools.jar is found (e.g.
	 * the path points to a JRE install).
	 *
	 * @param javaHomePath
	 *            path for Java home
	 * @return IAntClasspathEntry tools.jar IAntClasspathEntry or <code>null</code>
	 * @since 3.0
	 */
	public IAntClasspathEntry getToolsJarEntry(IPath javaHomePath) {
		IPath newjh = javaHomePath;
		if ("jre".equalsIgnoreCase(newjh.lastSegment())) { //$NON-NLS-1$
			newjh = newjh.removeLastSegments(1);
		}
		newjh = newjh.append("lib").append("tools.jar"); //$NON-NLS-1$ //$NON-NLS-2$
		File tools = newjh.toFile();
		if (!tools.exists()) {
			// attempt to find in the older 1.1.*
			newjh = newjh.removeLastSegments(1);
			newjh = newjh.append("classes.zip"); //$NON-NLS-1$
			tools = newjh.toFile();
			if (!tools.exists()) {
				return null;
			}
		}

		return new AntClasspathEntry(tools.getAbsolutePath());
	}

	/**
	 * Returns the URL for the tools.jar associated with the System property "java.home" location. If "java.home" has no associated tools.jar (such as
	 * a JRE install), the environment variable "JAVA_HOME" is resolved to check for a tools.jar. May return <code>null</code> if no tools.jar is
	 * found.
	 *
	 * @return URL tools.jar URL or <code>null</code>
	 * @deprecated use getToolsJarEntry()
	 */
	@Deprecated
	public URL getToolsJarURL() {
		IPath path = IPath.fromOSString(System.getProperty("java.home")); //$NON-NLS-1$
		IAntClasspathEntry entry = getToolsJarEntry(path);
		if (entry == null) {
			IDynamicVariable variable = VariablesPlugin.getDefault().getStringVariableManager().getDynamicVariable("env_var"); //$NON-NLS-1$
			String javaHome = null;
			try {
				if (variable != null) {
					javaHome = variable.getValue("JAVA_HOME"); //$NON-NLS-1$
				}
				if (javaHome != null) {
					path = IPath.fromOSString(javaHome);
					entry = getToolsJarEntry(path);
				}
			}
			catch (CoreException e) {
				AntCorePlugin.log(e);
			}
		}
		if (entry != null) {
			return entry.getEntryURL();
		}
		return null;
	}

	/**
	 * Returns the <code>IAntClasspathEntry</code> for the tools.jar associated with the System property "java.home" location. If "java.home" has no
	 * associated tools.jar (such as a JRE install), the environment variable "JAVA_HOME" is resolved to check for a tools.jar. May return
	 * <code>null</code> if no tools.jar is found.
	 *
	 * @return IAntClasspathEntry tools.jar IAntClasspathEntry or <code>null</code>
	 */
	public IAntClasspathEntry getToolsJarEntry() {
		IPath path = IPath.fromOSString(System.getProperty("java.home")); //$NON-NLS-1$
		IAntClasspathEntry entry = getToolsJarEntry(path);
		if (entry == null) {
			IDynamicVariable variable = VariablesPlugin.getDefault().getStringVariableManager().getDynamicVariable("env_var"); //$NON-NLS-1$
			String javaHome = null;
			try {
				if (variable != null) {
					javaHome = variable.getValue("JAVA_HOME"); //$NON-NLS-1$
				}
				if (javaHome != null) {
					path = IPath.fromOSString(javaHome);
					entry = getToolsJarEntry(path);
				}
			}
			catch (CoreException e) {
				AntCorePlugin.log(e);
			}
		}
		return entry;
	}

	/**
	 * Returns the <code>IAntClasspathEntry</code>s for the jars from ${user.home}/.ant/lib May return <code>null</code> if jars are found.
	 *
	 * TODO Should be promoted to API post 3.1
	 *
	 * @return the collection of <code>IAntClasspathEntry</code> found at ${user.home}/.ant/lib or <code>null</code> if none found of location does
	 *         not exist
	 */
	private List<IAntClasspathEntry> getUserLibraries() {
		File libDir = new File(System.getProperty("user.home"), ".ant" + File.separatorChar + "lib"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		URL[] urls = null;
		try {
			urls = getLocationURLs(libDir);
		}
		catch (MalformedURLException e) {
			AntCorePlugin.log(e);
		}
		if (urls == null) {
			return null;
		}

		List<IAntClasspathEntry> entries = new ArrayList<>(urls.length);
		for (URL url : urls) {
			AntClasspathEntry entry = new AntClasspathEntry(url);
			entries.add(entry);
		}
		return entries;
	}

	private URL[] getLocationURLs(File location) throws MalformedURLException {
		URL[] urls = null;
		if (!location.exists()) {
			return urls;
		}
		final String extension = ".jar"; //$NON-NLS-1$
		if (!location.isDirectory()) {
			urls = new URL[1];
			String path = location.getPath();
			if (path.toLowerCase().endsWith(extension)) {
				// make sure the URL is properly escaped
				urls[0] = location.toURI().toURL();
			}
			return urls;
		}

		File[] matches = location.listFiles((FilenameFilter) (dir, name) -> name.toLowerCase().endsWith(extension));

		urls = new URL[matches.length];
		for (int i = 0; i < matches.length; ++i) {
			// make sure the URL is properly escaped
			urls[i] = matches[i].toURI().toURL();
		}
		return urls;
	}

	/**
	 * Add the libraries contributed by the Ant plug-in, to the classpath.
	 *
	 * @param source
	 * @param destination
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private void addLibraries(Bundle source, List<AntClasspathEntry> destination) throws IOException, MalformedURLException {
		ManifestElement[] libraries = null;
		try {
			libraries = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, source.getHeaders(IAntCoreConstants.EMPTY_STRING).get(Constants.BUNDLE_CLASSPATH));
		}
		catch (BundleException e) {
			IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_MALFORMED_URL, InternalCoreAntMessages.AntCorePreferences_0, e);
			AntCorePlugin.getPlugin().getLog().log(status);
			return;
		}
		if (libraries == null) {
			return;
		}
		URL url = null;
		for (ManifestElement library : libraries) {
			url = source.getEntry(library.getValue());
			if (url != null) {
				destination.add(new AntClasspathEntry(FileLocator.toFileURL(url)));
			}
		}
	}

	protected void addPluginClassLoader(Bundle bundle) {
		WrappedClassLoader loader = getClassLoader(bundle);
		if (!pluginClassLoaders.contains(loader)) {
			pluginClassLoaders.add(loader);
		}
	}

	/**
	 * Returns the list of URLs added to the classpath by the extra classpath entries extension point.
	 *
	 * @return the list of extra classpath URLs
	 */
	public URL[] getExtraClasspathURLs() {
		URL[] urls = new URL[extraClasspathURLs.size()];

		for (int i = 0; i < extraClasspathURLs.size(); i++) {
			IAntClasspathEntry entry = extraClasspathURLs.get(i);
			urls[i] = entry.getEntryURL();
		}
		return urls;
	}

	/**
	 * Returns the list of URLs added to the classpath by the extra classpath entries extension point for an Ant build that is occurring without the
	 * Eclipse runtime.
	 *
	 * @return the list of extra classpath URLs
	 * @since 3.0
	 */
	public URL[] getRemoteExtraClasspathURLs() {
		List<URL> urls = new ArrayList<>(extraClasspathURLs.size());

		for (AntClasspathEntry entry : extraClasspathURLs) {
			if (!entry.isEclipseRuntimeRequired()) {
				urls.add(entry.getEntryURL());
			}
		}
		return urls.toArray(new URL[urls.size()]);
	}

	/**
	 * Returns the entire set of URLs that define the Ant runtime classpath. Includes the Ant URLs, the additional URLs and extra classpath URLs.
	 *
	 * @return the entire runtime classpath of URLs
	 */
	public URL[] getURLs() {
		List<URL> result = new ArrayList<>(60);
		if (antHomeEntries != null) {
			addEntryURLs(result, antHomeEntries);
		}
		if (additionalEntries != null && additionalEntries.length > 0) {
			addEntryURLs(result, additionalEntries);
		}

		for (AntClasspathEntry entry : extraClasspathURLs) {
			URL url = entry.getEntryURL();
			if (url != null) {
				result.add(url);
			}
		}

		return result.toArray(new URL[result.size()]);
	}

	private void addEntryURLs(List<URL> result, IAntClasspathEntry[] entries) {
		for (IAntClasspathEntry entry : entries) {
			URL url = entry.getEntryURL();
			if (url != null) {
				result.add(url);
			}
		}
	}

	protected ClassLoader[] getPluginClassLoaders() {
		if (orderedPluginClassLoaders == null) {
			Iterator<WrappedClassLoader> classLoaders = pluginClassLoaders.iterator();
			Map<String, WrappedClassLoader> idToLoader = new HashMap<>(pluginClassLoaders.size());
			List<BundleRevision> bundles = new ArrayList<>(pluginClassLoaders.size());
			while (classLoaders.hasNext()) {
				WrappedClassLoader loader = classLoaders.next();
				idToLoader.put(loader.bundle.getSymbolicName(), loader);
				BundleRevision revision = loader.bundle.adapt(BundleRevision.class);
				if (revision != null) {
					bundles.add(revision);
				}
			}
			List<BundleRevision> sorted = computePrerequisiteOrder(bundles);
			List<WrappedClassLoader> loaders = new ArrayList<>(sorted.size());
			for (BundleRevision revision : sorted) {
				String id = revision.getSymbolicName();
				loaders.add(idToLoader.get(id));
			}
			orderedPluginClassLoaders = loaders.toArray(new ClassLoader[loaders.size()]);
		}
		return orderedPluginClassLoaders;
	}

	/*
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private List<BundleRevision> computePrerequisiteOrder(List<BundleRevision> plugins) {
		List<Relation> prereqs = new ArrayList<>(plugins.size());
		List<BundleRevision> fragments = new ArrayList<>();

		// create a collection of directed edges from plugin to prereq
		for (BundleRevision current : plugins) {
			if ((current.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
				fragments.add(current);
				continue;
			}
			boolean found = false;

			BundleRevision[] prereqList = getDependentBundles(current);
			for (BundleRevision prereq : prereqList) {
				// ensure that we only include values from the original set.
				if (plugins.contains(prereq)) {
					found = true;
					prereqs.add(new Relation(current, prereq));
				}
			}

			// if we didn't find any prereqs for this plugin, add a null prereq
			// to ensure the value is in the output
			if (!found) {
				prereqs.add(new Relation(current, null));
			}
		}

		// The fragments needs to added relatively to their host and to their
		// own prerequisite (bug #43244)
		for (BundleRevision currentFrag : fragments) {

			if (plugins.contains(currentFrag)) {
				BundleWiring wiring = currentFrag.getWiring();
				List<BundleWire> hostWires = wiring == null ? Collections.<BundleWire> emptyList()
						: wiring.getRequiredWires(HostNamespace.HOST_NAMESPACE);
				if (!hostWires.isEmpty()) {
					prereqs.add(new Relation(currentFrag, hostWires.get(0).getProvider()));
				}
			} else {
				AntCorePlugin.getPlugin().getLog().log(new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_MALFORMED_URL, NLS.bind(InternalCoreAntMessages.AntCorePreferences_1, new String[] {
						currentFrag.getSymbolicName() }), null));
			}
		}

		// do a topological sort, insert the fragments into the sorted elements
		return computeNodeOrder(prereqs);
	}

	/*
	 * Copied from org.eclipse.pde.internal.build.site.PDEState.
	 */
	private BundleRevision[] getDependentBundles(BundleRevision root) {
		BundleRevision[] imported = getImportedBundles(root);
		BundleRevision[] required = getRequiredBundles(root);
		BundleRevision[] dependents = new BundleRevision[imported.length + required.length];
		System.arraycopy(imported, 0, dependents, 0, imported.length);
		System.arraycopy(required, 0, dependents, imported.length, required.length);
		return dependents;
	}

	/*
	 * Copied from org.eclipse.pde.internal.build.site.PDEState.
	 */
	private BundleRevision[] getRequiredBundles(BundleRevision root) {
		return getDependantRequirements(root, BundleNamespace.BUNDLE_NAMESPACE);
	}

	/*
	 * Copied from org.eclipse.pde.internal.build.site.PDEState.
	 */
	private BundleRevision[] getImportedBundles(BundleRevision root) {
		return getDependantRequirements(root, PackageNamespace.PACKAGE_NAMESPACE);
	}

	private BundleRevision[] getDependantRequirements(BundleRevision root, String namespace) {
		if (root == null) {
			return new BundleRevision[0];
		}
		BundleWiring wiring = root.getWiring();
		List<BundleWire> requiredWires = wiring == null ? Collections.<BundleWire> emptyList() : wiring.getRequiredWires(namespace);
		ArrayList<BundleRevision> requirementProviders = new ArrayList<>(requiredWires.size());
		for (BundleWire requiredWire : requiredWires) {
			BundleRevision provider = requiredWire.getProvider();
			if (!provider.equals(root) && !requirementProviders.contains(provider)) {
				requirementProviders.add(provider);
			}
		}
		return requirementProviders.toArray(new BundleRevision[requirementProviders.size()]);
	}

	/*
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private void removeArcs(List<Relation> edges, List<BundleRevision> roots, Map<BundleRevision, Integer> counts) {
		for (BundleRevision bundleRevision : roots) {
			Object root = bundleRevision;
			for (Relation edge : edges) {
				if (root.equals(edge.to)) {
					BundleRevision input = edge.from;
					Integer count = counts.get(input);
					if (count != null) {
						counts.put(input, Integer.valueOf(count.intValue() - 1));
					}
				}
			}
		}
	}

	/*
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private List<BundleRevision> computeNodeOrder(List<Relation> edges) {
		Map<BundleRevision, Integer> counts = computeCounts(edges);
		List<BundleRevision> nodes = new ArrayList<>(counts.size());
		while (!counts.isEmpty()) {
			List<BundleRevision> roots = findRootNodes(counts);
			if (roots.isEmpty()) {
				break;
			}
			for (BundleRevision bundleRevision : roots) {
				counts.remove(bundleRevision);
			}
			nodes.addAll(roots);
			removeArcs(edges, roots, counts);
		}
		return nodes;
	}

	/*
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private Map<BundleRevision, Integer> computeCounts(List<Relation> mappings) {
		Map<BundleRevision, Integer> counts = new HashMap<>(5);
		for (Relation mapping : mappings) {
			BundleRevision from = mapping.from;
			Integer fromCount = counts.get(from);
			BundleRevision to = mapping.to;
			if (to == null)
				counts.put(from, Integer.valueOf(0));
			else {
				if (counts.get(to) == null)
					counts.put(to, Integer.valueOf(0));
				fromCount = fromCount == null ? Integer.valueOf(1) : Integer.valueOf(fromCount.intValue() + 1);
				counts.put(from, fromCount);
			}
		}
		return counts;
	}

	/*
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private List<BundleRevision> findRootNodes(Map<BundleRevision, Integer> counts) {
		List<BundleRevision> result = new ArrayList<>(5);
		for (BundleRevision node : counts.keySet()) {
			int count = counts.get(node).intValue();
			if (count == 0) {
				result.add(node);
			}
		}
		return result;
	}

	private void initializePluginClassLoaders() {
		pluginClassLoaders = new ArrayList<>(10);
		// ant.core should always be present
		pluginClassLoaders.add(getClassLoader(AntCorePlugin.getPlugin().getBundle()));
	}

	/**
	 * Returns the default and custom tasks.
	 *
	 * @return the list of default and custom tasks.
	 */
	public List<Task> getTasks() {
		List<Task> result = new ArrayList<>(10);
		if (defaultTasks != null && !defaultTasks.isEmpty()) {
			result.addAll(defaultTasks);
		}
		if (customTasks != null && customTasks.length != 0) {
			result.addAll(Arrays.asList(customTasks));
		}
		return result;
	}

	/**
	 * Returns the default and custom tasks that are relevant when there is no Eclipse runtime context (an Ant build in a separate VM).
	 *
	 * @return the list of default and custom tasks.
	 */
	public List<Task> getRemoteTasks() {
		List<Task> result = new ArrayList<>(10);
		if (defaultTasks != null && !defaultTasks.isEmpty()) {
			Iterator<Task> iter = defaultTasks.iterator();
			while (iter.hasNext()) {
				Task task = iter.next();
				if (!task.isEclipseRuntimeRequired()) {
					result.add(task);
				}
			}
		}
		if (customTasks != null && customTasks.length != 0) {
			result.addAll(Arrays.asList(customTasks));
		}
		return result;
	}

	/**
	 * Returns the user defined custom tasks
	 *
	 * @return the user defined tasks
	 */
	public Task[] getCustomTasks() {
		return customTasks;
	}

	/**
	 * Returns the user defined custom types
	 *
	 * @return the user defined types
	 */
	public Type[] getCustomTypes() {
		return customTypes;
	}

	/**
	 * Returns the custom user properties specified for Ant builds.
	 *
	 * @return the properties defined for Ant builds.
	 */
	public Property[] getCustomProperties() {
		return customProperties;
	}

	/**
	 * Returns the default and custom properties.
	 *
	 * @return the list of default and custom properties.
	 * @since 3.0
	 */
	public List<Property> getProperties() {
		List<Property> result = new ArrayList<>(10);
		if (defaultProperties != null && !defaultProperties.isEmpty()) {
			result.addAll(defaultProperties);
		}
		if (customProperties != null && customProperties.length != 0) {
			result.addAll(Arrays.asList(customProperties));
		}
		return result;
	}

	/**
	 * Returns the default and custom properties that are relevant when there is no Eclipse runtime context (Ant build in a separate VM).
	 *
	 * @return the list of default and custom properties.
	 * @since 3.0
	 */
	public List<Property> getRemoteAntProperties() {
		List<Property> result = new ArrayList<>(10);
		if (defaultProperties != null && !defaultProperties.isEmpty()) {
			Iterator<Property> iter = defaultProperties.iterator();
			while (iter.hasNext()) {
				Property property = iter.next();
				if (!property.isEclipseRuntimeRequired()) {
					result.add(property);
				}
			}
		}
		if (customProperties != null && customProperties.length != 0) {
			result.addAll(Arrays.asList(customProperties));
		}
		return result;
	}

	/**
	 * Returns the custom property files specified for Ant builds performing any required string substitution if indicated.
	 *
	 * @param performStringSubstition
	 *            whether or not to perform the string substitution on the property file strings
	 * @return the property files defined for Ant builds.
	 * @since 3.0
	 */
	public String[] getCustomPropertyFiles(boolean performStringSubstition) {
		if (!performStringSubstition || customPropertyFiles == null || customPropertyFiles.length == 0) {
			return customPropertyFiles;
		}
		List<String> files = new ArrayList<>(customPropertyFiles.length);
		for (String customPropertyFile : customPropertyFiles) {
			String filename = customPropertyFile;
			try {
				filename = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(filename);
				files.add(filename);
			}
			catch (CoreException e) {
				// notify the user via the Ant console of the missing file
				files.add(filename);
			}
		}
		return files.toArray(new String[files.size()]);
	}

	/**
	 * Returns the custom property files specified for Ant builds.
	 *
	 * @return the property files defined for Ant builds.
	 */
	public String[] getCustomPropertyFiles() {
		return getCustomPropertyFiles(true);
	}

	/**
	 * Returns the custom URLs specified for the Ant classpath
	 *
	 * @return the URLs defining the Ant classpath
	 * @deprecated
	 */
	@Deprecated
	public URL[] getCustomURLs() {
		URL[] urls = new URL[additionalEntries.length];
		int i;
		for (i = 0; i < additionalEntries.length; i++) {
			URL url = additionalEntries[i].getEntryURL();
			if (url != null) {
				urls[i] = url;
			}
		}

		return urls;
	}

	/**
	 * Sets the user defined custom tasks. To commit the changes, updatePluginPreferences must be called.
	 *
	 * @param tasks
	 *            The custom tasks.
	 */
	public void setCustomTasks(Task[] tasks) {
		oldCustomTasks = customTasks;
		customTasks = tasks;
	}

	/**
	 * Sets the user defined custom types. To commit the changes, updatePluginPreferences must be called.
	 *
	 * @param types
	 *            The custom types
	 */
	public void setCustomTypes(Type[] types) {
		oldCustomTypes = customTypes;
		customTypes = types;
	}

	/**
	 * Sets the custom URLs specified for the Ant classpath. To commit the changes, updatePluginPreferences must be called.
	 *
	 * @param urls
	 *            the URLs defining the Ant classpath
	 * @deprecated use setAdditionalEntries(IAntClasspathEntry)[]
	 */
	@Deprecated
	public void setCustomURLs(URL[] urls) {
		additionalEntries = new IAntClasspathEntry[urls.length];
		for (int i = 0; i < urls.length; i++) {
			URL url = urls[i];
			IAntClasspathEntry entry = new AntClasspathEntry(url);
			additionalEntries[i] = entry;
		}
	}

	/**
	 * Sets the Ant URLs specified for the Ant classpath. To commit the changes, updatePluginPreferences must be called.
	 *
	 * @param urls
	 *            the URLs defining the Ant classpath
	 * @deprecated use setAntHomeEntires(IAntClasspathEntry[])
	 */
	@Deprecated
	public void setAntURLs(URL[] urls) {
		antHomeEntries = new IAntClasspathEntry[urls.length];
		for (int i = 0; i < urls.length; i++) {
			URL url = urls[i];
			IAntClasspathEntry entry = new AntClasspathEntry(url);
			antHomeEntries[i] = entry;
		}
	}

	/**
	 * Sets the custom property files specified for Ant builds. To commit the changes, updatePluginPreferences must be called.
	 *
	 * @param paths
	 *            the absolute paths defining the property files to use.
	 */
	public void setCustomPropertyFiles(String[] paths) {
		customPropertyFiles = paths;
	}

	/**
	 * Sets the custom user properties specified for Ant builds. To commit the changes, updatePluginPreferences must be called.
	 *
	 * @param properties
	 *            the properties defining the Ant properties
	 */
	public void setCustomProperties(Property[] properties) {
		oldCustomProperties = customProperties;
		customProperties = properties;
	}

	/**
	 * Returns the default and custom types.
	 *
	 * @return all of the defined types
	 */
	public List<Type> getTypes() {
		List<Type> result = new ArrayList<>(10);
		if (defaultTypes != null && !defaultTypes.isEmpty()) {
			result.addAll(defaultTypes);
		}
		if (customTypes != null && customTypes.length != 0) {
			result.addAll(Arrays.asList(customTypes));
		}
		return result;
	}

	/**
	 * Returns the default and custom types that are relevant when there is no Eclipse runtime context (an Ant build in a separate VM).
	 *
	 * @return the list of default and custom types.
	 */
	public List<Type> getRemoteTypes() {
		List<Type> result = new ArrayList<>(10);
		if (defaultTypes != null && !defaultTypes.isEmpty()) {
			Iterator<Type> iter = defaultTypes.iterator();
			while (iter.hasNext()) {
				Type type = iter.next();
				if (!type.isEclipseRuntimeRequired()) {
					result.add(type);
				}
			}
		}
		if (customTypes != null && customTypes.length != 0) {
			result.addAll(Arrays.asList(customTypes));
		}
		return result;
	}

	/**
	 * Returns the default types defined via the type extension point
	 *
	 * @return all of the default types
	 */
	public List<Type> getDefaultTypes() {
		List<Type> result = new ArrayList<>(10);
		if (defaultTypes != null && !defaultTypes.isEmpty()) {
			result.addAll(defaultTypes);
		}
		return result;
	}

	/**
	 * Returns the default tasks defined via the task extension point
	 *
	 * @return all of the default tasks
	 */
	public List<Task> getDefaultTasks() {
		List<Task> result = new ArrayList<>(10);
		if (defaultTasks != null && !defaultTasks.isEmpty()) {
			result.addAll(defaultTasks);
		}
		return result;
	}

	/**
	 * Returns the default properties defined via the properties extension point
	 *
	 * @return all of the default properties
	 * @since 3.0
	 */
	public List<Property> getDefaultProperties() {
		List<Property> result = new ArrayList<>(10);
		if (defaultProperties != null && !defaultProperties.isEmpty()) {
			result.addAll(defaultProperties);
		}
		return result;
	}

	/*
	 * Convert a list of tokens into an array using "," as the tokenizer.
	 */
	protected String[] getArrayFromString(String list) {
		String separator = ","; //$NON-NLS-1$
		if (list == null || list.trim().equals(IAntCoreConstants.EMPTY_STRING)) {
			return new String[0];
		}
		ArrayList<String> result = new ArrayList<>();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals(IAntCoreConstants.EMPTY_STRING)) {
				result.add(token);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * Updates the underlying plug-in preferences to the current state.
	 */
	public void updatePluginPreferences() {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(AntCorePlugin.PI_ANTCORE);
		if (node != null) {
			node.removePreferenceChangeListener(prefListener);
			Preferences prefs = AntCorePlugin.getPlugin().getPluginPreferences();
			updateTasks(prefs);
			updateTypes(prefs);
			updateAntHomeEntries(prefs);
			updateAdditionalEntries(prefs);
			updateProperties(prefs);
			updatePropertyFiles(prefs);
			boolean classpathChanged = AntCorePlugin.getPlugin().getPluginPreferences().needsSaving();
			AntCorePlugin.getPlugin().savePluginPreferences();
			if (classpathChanged) {
				prefs.setValue(IAntCoreConstants.PREFERENCE_CLASSPATH_CHANGED, true);
			}
			prefs.setValue(IAntCoreConstants.PREFERENCE_CLASSPATH_CHANGED, false);
			node.addPreferenceChangeListener(prefListener);
		}

	}

	protected void updateTasks(Preferences prefs) {
		if (oldCustomTasks != null) {
			for (Task oldTask : oldCustomTasks) {
				prefs.setToDefault(IAntCoreConstants.PREFIX_TASK + oldTask.getTaskName());
			}
			oldCustomTasks = null;
		}

		if (customTasks.length == 0) {
			prefs.setValue(IAntCoreConstants.PREFERENCE_TASKS, IAntCoreConstants.EMPTY_STRING);
			return;
		}
		StringBuilder tasks = new StringBuilder();
		for (Task customTask : customTasks) {
			tasks.append(customTask.getTaskName());
			tasks.append(',');
			prefs.setValue(IAntCoreConstants.PREFIX_TASK + customTask.getTaskName(), customTask.getClassName() + "," //$NON-NLS-1$
					+ customTask.getLibraryEntry().getLabel());
		}
		prefs.setValue(IAntCoreConstants.PREFERENCE_TASKS, tasks.toString());
	}

	protected void updateTypes(Preferences prefs) {
		if (oldCustomTypes != null) {
			for (Type oldType : oldCustomTypes) {
				prefs.setToDefault(IAntCoreConstants.PREFIX_TYPE + oldType.getTypeName());
			}
			oldCustomTypes = null;
		}

		if (customTypes.length == 0) {
			prefs.setValue(IAntCoreConstants.PREFERENCE_TYPES, IAntCoreConstants.EMPTY_STRING);
			return;
		}
		StringBuilder types = new StringBuilder();
		for (Type customType : customTypes) {
			types.append(customType.getTypeName());
			types.append(',');
			prefs.setValue(IAntCoreConstants.PREFIX_TYPE + customType.getTypeName(), customType.getClassName() + "," //$NON-NLS-1$
					+ customType.getLibraryEntry().getLabel());
		}
		prefs.setValue(IAntCoreConstants.PREFERENCE_TYPES, types.toString());
	}

	protected void updateProperties(Preferences prefs) {
		if (oldCustomProperties != null) {
			for (Property oldProperty : oldCustomProperties) {
				prefs.setToDefault(IAntCoreConstants.PREFIX_PROPERTY + oldProperty.getName());
			}
			oldCustomProperties = null;
		}

		if (customProperties.length == 0) {
			prefs.setValue(IAntCoreConstants.PREFERENCE_PROPERTIES, IAntCoreConstants.EMPTY_STRING);
			return;
		}
		StringBuilder properties = new StringBuilder();
		for (Property customProperty : customProperties) {
			properties.append(customProperty.getName());
			properties.append(',');
			prefs.setValue(IAntCoreConstants.PREFIX_PROPERTY + customProperty.getName(), customProperty.getValue(false));
		}
		prefs.setValue(IAntCoreConstants.PREFERENCE_PROPERTIES, properties.toString());
	}

	protected void updateAdditionalEntries(Preferences prefs) {
		prefs.setValue("urls", IAntCoreConstants.EMPTY_STRING); // old constant removed //$NON-NLS-1$
		String serialized = IAntCoreConstants.EMPTY_STRING;
		IAntClasspathEntry toolsJarEntry = getToolsJarEntry();
		List<IAntClasspathEntry> userLibs = getUserLibraries();
		if (userLibs == null) {
			userLibs = new ArrayList<>();
		}
		if (toolsJarEntry != null) {
			userLibs.add(toolsJarEntry);
		}
		boolean changed = true;
		if (additionalEntries.length == userLibs.size()) {
			changed = false;
			for (int i = 0; i < additionalEntries.length; i++) {
				if (!additionalEntries[i].equals(userLibs.get(i))) {
					changed = true;
					break;
				}
			}
		}
		if (changed) {
			StringBuilder entries = new StringBuilder();
			for (IAntClasspathEntry additionalEntry : additionalEntries) {
				entries.append(additionalEntry.getLabel());
				entries.append(',');
			}
			serialized = entries.toString();
		}

		prefs.setValue(IAntCoreConstants.PREFERENCE_ADDITIONAL_ENTRIES, serialized);

		String prefAntHome = IAntCoreConstants.EMPTY_STRING;
		if (antHome != null && !antHome.equals(getDefaultAntHome())) {
			prefAntHome = antHome;
		}
		prefs.setValue(IAntCoreConstants.PREFERENCE_ANT_HOME, prefAntHome);
	}

	protected void updateAntHomeEntries(Preferences prefs) {
		prefs.setValue("ant_urls", IAntCoreConstants.EMPTY_STRING); // old constant removed //$NON-NLS-1$

		// see if the custom entries are just the default entries
		IAntClasspathEntry[] defaultEntries = getDefaultAntHomeEntries();
		boolean dflt = false;
		if (defaultEntries.length == antHomeEntries.length) {
			dflt = true;
			for (int i = 0; i < antHomeEntries.length; i++) {
				if (!antHomeEntries[i].equals(defaultEntries[i])) {
					dflt = false;
					break;
				}
			}
		}
		if (dflt) {
			// always want to recalculate the default Ant urls
			// to pick up any changes in the default Ant classpath
			prefs.setValue(IAntCoreConstants.PREFERENCE_ANT_HOME_ENTRIES, IAntCoreConstants.EMPTY_STRING);
			return;
		}
		StringBuilder entries = new StringBuilder();
		for (IAntClasspathEntry antHomeEntry : antHomeEntries) {
			entries.append(antHomeEntry.getLabel());
			entries.append(',');
		}

		prefs.setValue(IAntCoreConstants.PREFERENCE_ANT_HOME_ENTRIES, entries.toString());
	}

	protected void updatePropertyFiles(Preferences prefs) {
		StringBuilder files = new StringBuilder();
		for (String customPropertyFile : customPropertyFiles) {
			files.append(customPropertyFile);
			files.append(',');
		}

		prefs.setValue(IAntCoreConstants.PREFERENCE_PROPERTY_FILES, files.toString());
	}

	/**
	 * Sets the string that defines the Ant home set by the user. May be set to <code>null</code>.
	 *
	 * @param antHome
	 *            the fully qualified path to Ant home
	 */
	public void setAntHome(String antHome) {
		this.antHome = antHome;
	}

	/**
	 * Returns the string that defines the Ant home set by the user or the location of the Eclipse Ant plug-in if Ant home has not been specifically
	 * set by the user. Can return <code>null</code>
	 *
	 * @return the fully qualified path to Ant home
	 */
	public String getAntHome() {
		return antHome;
	}

	/**
	 * Returns the set of classpath entries that compose the libraries added to the Ant runtime classpath from the Ant home location.
	 *
	 * @return the set of ant home classpath entries
	 * @since 3.0
	 */
	public IAntClasspathEntry[] getAntHomeClasspathEntries() {
		return antHomeEntries;
	}

	/**
	 * Returns the set of classpath entries that the user has added to the Ant runtime classpath.
	 *
	 * @return the set of user classpath entries
	 * @since 3.0
	 */
	public IAntClasspathEntry[] getAdditionalClasspathEntries() {
		return additionalEntries;
	}

	/**
	 * Sets the set of classpath entries that compose the libraries added to the Ant runtime classpath from the Ant home location.
	 *
	 * @param entries
	 *            the set of ant home classpath entries
	 * @since 3.0
	 */
	public void setAntHomeClasspathEntries(IAntClasspathEntry[] entries) {
		antHomeEntries = entries;
	}

	/**
	 * Sets the set of classpath entries that the user has added to the Ant runtime classpath.
	 *
	 * @param entries
	 *            the set of user classpath entries
	 * @since 3.0
	 */
	public void setAdditionalClasspathEntries(IAntClasspathEntry[] entries) {
		additionalEntries = entries;
	}

	/**
	 * Returns the list of URLs to added to the classpath for an Ant build that is occurring without the Eclipse runtime.
	 *
	 * @return the list of classpath entries
	 * @since 3.0
	 */
	public URL[] getRemoteAntURLs() {
		List<URL> result = new ArrayList<>(40);
		if (antHomeEntries != null) {
			for (IAntClasspathEntry entry : antHomeEntries) {
				result.add(entry.getEntryURL());
			}
		}
		if (additionalEntries != null && additionalEntries.length > 0) {
			for (IAntClasspathEntry entry : additionalEntries) {
				result.add(entry.getEntryURL());
			}
		}
		if (extraClasspathURLs != null) {
			for (AntClasspathEntry entry : extraClasspathURLs) {
				if (!entry.isEclipseRuntimeRequired()) {
					result.add(entry.getEntryURL());
				}
			}
		}

		return result.toArray(new URL[result.size()]);
	}

	/**
	 * Returns all contributed classpath entries via the <code>extraClasspathEntries</code> extension point.
	 *
	 * @return all contributed classpath entries via the <code>extraClasspathEntries</code> extension point
	 * @since 3.0
	 */
	public IAntClasspathEntry[] getContributedClasspathEntries() {
		return extraClasspathURLs.toArray(new IAntClasspathEntry[extraClasspathURLs.size()]);
	}
}
