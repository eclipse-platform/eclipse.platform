/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Thierry Lach (thierry.lach@bbdodetroit.com) - bug 40502
 *******************************************************************************/
package org.eclipse.ant.core;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
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
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;


/**
 * Represents the Ant Core plug-in's preferences providing utilities for
 * extracting, changing and updating the underlying preferences.
 * Clients may not instantiate or subclass this class.
 * @since 2.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class AntCorePreferences implements org.eclipse.core.runtime.Preferences.IPropertyChangeListener {

	private List defaultTasks;
	private List defaultTypes;
	private List extraClasspathURLs;
	private List defaultProperties;
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
	
	private List pluginClassLoaders;
	
	private ClassLoader[] orderedPluginClassLoaders;
	
	private String antHome;
	
	private boolean runningHeadless= false;

	static private class Relation {
		Object from;
		Object to;

		Relation(Object from, Object to) {
			this.from = from;
			this.to = to;
		}

		public String toString() {
			return from.toString() + "->" + (to == null ? "" : to.toString()); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	class WrappedClassLoader extends ClassLoader {
		private Bundle bundle;
		public WrappedClassLoader(Bundle bundle) {
			super();
			this.bundle = bundle;
		}
		/* (non-Javadoc)
		 * @see java.lang.ClassLoader#findClass(java.lang.String)
		 */
		public Class findClass(String name) throws ClassNotFoundException {
			return bundle.loadClass(name);
		}
		/* (non-Javadoc)
		 * @see java.lang.ClassLoader#findResource(java.lang.String)
		 */
		public URL findResource(String name) {
			return bundle.getResource(name);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.ClassLoader#findResources(java.lang.String)
		 */
		protected Enumeration findResources(String name) throws IOException {
			return bundle.getResources(name);
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (!(obj instanceof WrappedClassLoader)) {
				return false;
			}
			return bundle == ((WrappedClassLoader) obj).bundle;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return bundle.hashCode();
		}
		public String toString() {
			return "WrappedClassLoader(" + bundle.toString() + ")";  //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	protected AntCorePreferences(List defaultTasks, List defaultExtraClasspath, List defaultTypes, boolean headless) {
		this(defaultTasks, defaultExtraClasspath, defaultTypes, Collections.EMPTY_LIST, headless);
	}
	
	protected AntCorePreferences(List defaultTasks, List defaultExtraClasspath, List defaultTypes, List defaultProperties, boolean headless) {
		runningHeadless= headless;
		initializePluginClassLoaders();
		extraClasspathURLs = new ArrayList(20);
		this.defaultTasks = computeDefaultTasks(defaultTasks);
		this.defaultTypes = computeDefaultTypes(defaultTypes);
		computeDefaultExtraClasspathEntries(defaultExtraClasspath);
		computeDefaultProperties(defaultProperties);
		restoreCustomObjects();
		
	}
	
	/**
	 * When a preference changes, update the in-memory cache of the preference.
	 * @param event The property change event that has occurred.
	 * @see org.eclipse.core.runtime.Preferences.IPropertyChangeListener#propertyChange(org.eclipse.core.runtime.Preferences.PropertyChangeEvent)
	 */
	public void propertyChange(Preferences.PropertyChangeEvent event) {
		Preferences prefs = AntCorePlugin.getPlugin().getPluginPreferences();
		String property= event.getProperty();
		if (property.equals(IAntCoreConstants.PREFERENCE_TASKS) || property.startsWith(IAntCoreConstants.PREFIX_TASK)) {
			restoreTasks(prefs);
		} else if (property.equals(IAntCoreConstants.PREFERENCE_TYPES) || property.startsWith(IAntCoreConstants.PREFIX_TYPE)) {
			restoreTypes(prefs);
		} else if (property.equals(IAntCoreConstants.PREFERENCE_ANT_HOME_ENTRIES)) {
			restoreAntHomeEntries(prefs);
		} else if (property.equals(IAntCoreConstants.PREFERENCE_ADDITIONAL_ENTRIES)) {
			restoreAdditionalEntries(prefs);
		} else if (property.equals(IAntCoreConstants.PREFERENCE_ANT_HOME)) {
			restoreAntHome(prefs);
		} else if (property.equals(IAntCoreConstants.PREFERENCE_PROPERTIES) || property.startsWith(IAntCoreConstants.PREFIX_PROPERTY)) {
			restoreCustomProperties(prefs);
		} else if (property.equals(IAntCoreConstants.PREFERENCE_PROPERTY_FILES)) {
			restoreCustomPropertyFiles(prefs);
		}
	}

	/**
	 * Restores the in-memory model of the preferences from the preference store
	 */
	private void restoreCustomObjects() {
		Preferences prefs = AntCorePlugin.getPlugin().getPluginPreferences();
		restoreAntHome(prefs);
		restoreTasks(prefs);
		restoreTypes(prefs);
		restoreAntHomeEntries(prefs);
		restoreAdditionalEntries(prefs);
		restoreCustomProperties(prefs);
		restoreCustomPropertyFiles(prefs);
		prefs.addPropertyChangeListener(this);
	}
	
	private void restoreTasks(Preferences prefs) {
		 String tasks = prefs.getString(IAntCoreConstants.PREFERENCE_TASKS);
		 if (tasks.equals("")) { //$NON-NLS-1$
			 customTasks = new Task[0];
		 } else {
			 customTasks = extractTasks(prefs, getArrayFromString(tasks));
		 }
	}
	
	private void restoreTypes(Preferences prefs) {
		String types = prefs.getString(IAntCoreConstants.PREFERENCE_TYPES);
		if (types.equals("")) {//$NON-NLS-1$
			customTypes = new Type[0];
		} else {
			customTypes = extractTypes(prefs, getArrayFromString(types));
		}
	}
	
	private void restoreAntHomeEntries(Preferences prefs) {
		String entries = prefs.getString("ant_urls"); //old constant //$NON-NLS-1$
		if (entries.equals("")) {//$NON-NLS-1$
			entries= prefs.getString(IAntCoreConstants.PREFERENCE_ANT_HOME_ENTRIES);
		} else {
			prefs.setToDefault("ant_urls"); //$NON-NLS-1$
			antHomeEntries= migrateURLEntries(getArrayFromString(entries));
			return;
		}
		if (entries.equals("")) {//$NON-NLS-1$
			antHomeEntries= getDefaultAntHomeEntries();
		} else {
			antHomeEntries= extractEntries(getArrayFromString(entries));
		}
	}
	
	private void restoreAdditionalEntries(Preferences prefs) {
		String entries = prefs.getString("urls"); //old constant //$NON-NLS-1$
		if (entries.equals("")) {//$NON-NLS-1$
			entries = prefs.getString(IAntCoreConstants.PREFERENCE_ADDITIONAL_ENTRIES);
		} else {
			prefs.setToDefault("urls"); //$NON-NLS-1$
			additionalEntries= migrateURLEntries(getArrayFromString(entries));
			return;
		}
		if (entries.equals("")) {//$NON-NLS-1$
			IAntClasspathEntry toolsJarEntry= getToolsJarEntry();
			List userLibs= getUserLibraries();
			if (toolsJarEntry == null) {
				if (userLibs == null) {
					additionalEntries= new IAntClasspathEntry[0];
				} else {
				    additionalEntries= (IAntClasspathEntry[]) userLibs.toArray(new IAntClasspathEntry[userLibs.size()]);
				}
			} else {
				if (userLibs == null) {
					additionalEntries= new IAntClasspathEntry[] {toolsJarEntry};
				} else {
					userLibs.add(toolsJarEntry);
					additionalEntries= (IAntClasspathEntry[]) userLibs.toArray(new IAntClasspathEntry[userLibs.size()]);
				}
			}
		} else {
			additionalEntries= extractEntries(getArrayFromString(entries));
		}
	}
	
	/*
	 * Migrates the persisted url entries restored from a workspace older than 3.0
	 */
	private IAntClasspathEntry[] migrateURLEntries(String[] urlEntries) {
		List result = new ArrayList(urlEntries.length);
		for (int i = 0; i < urlEntries.length; i++) {
			URL url;
			try {
				url = new URL (urlEntries[i]);
			} catch (MalformedURLException e) {
				continue;
			}
			result.add(new AntClasspathEntry(url));
		}
		return (IAntClasspathEntry[])result.toArray(new IAntClasspathEntry[result.size()]);
	}

	private void restoreAntHome(Preferences prefs) {
		antHome= prefs.getString(IAntCoreConstants.PREFERENCE_ANT_HOME);
		if (antHome == null || antHome.length() == 0) {
			antHome= getDefaultAntHome();
		}
	}
	
	/**
	 * Returns the absolute path of the default ant.home to use for the build.
	 * The default is the org.apache.ant plug-in folder provided with Eclipse.
	 * 
	 * @return String absolute path of the default ant.home
	 * @since 3.0
	 */
	public String getDefaultAntHome() {
		IAntClasspathEntry[] entries= getDefaultAntHomeEntries();
		if (entries.length > 0) {
			URL antjar= entries[0].getEntryURL();
			IPath antHomePath= new Path(antjar.getFile());
			//parent directory of the lib directory
			antHomePath= antHomePath.removeLastSegments(2);
			return antHomePath.toFile().getAbsolutePath();
		} 
		return null;
	}
	
	private void restoreCustomProperties(Preferences prefs) {
		String properties = prefs.getString(IAntCoreConstants.PREFERENCE_PROPERTIES);
		if (properties.equals("")) {//$NON-NLS-1$
			customProperties = new Property[0];
		} else {
			customProperties = extractProperties(prefs, getArrayFromString(properties));
		}
	}
	
	private void restoreCustomPropertyFiles(Preferences prefs) {
		String propertyFiles= prefs.getString(IAntCoreConstants.PREFERENCE_PROPERTY_FILES);
		if (propertyFiles.equals("")) { //$NON-NLS-1$
			customPropertyFiles= new String[0];
		} else {
			customPropertyFiles= getArrayFromString(propertyFiles);
		}
	}

	protected Task[] extractTasks(Preferences prefs, String[] tasks) {
		List result = new ArrayList(tasks.length);
		for (int i = 0; i < tasks.length; i++) {
			String taskName = tasks[i];
			String[] values = getArrayFromString(prefs.getString(IAntCoreConstants.PREFIX_TASK + taskName));
			if (values.length < 2) {
				continue;
			}
			Task task = new Task();
			task.setTaskName(taskName);
			task.setClassName(values[0]);
			String library= values[1];
			if (library.startsWith("file:")) { //$NON-NLS-1$
				//old format where URLs were persisted
				library= library.substring(5);
			}
			task.setLibraryEntry(new AntClasspathEntry(library));
			result.add(task);
		}
		return (Task[]) result.toArray(new Task[result.size()]);
	}

	protected Type[] extractTypes(Preferences prefs, String[] types) {
		List result = new ArrayList(types.length);
		for (int i = 0; i < types.length; i++) {
			String typeName = types[i];
			String[] values = getArrayFromString(prefs.getString(IAntCoreConstants.PREFIX_TYPE + typeName));
			if (values.length < 2) {
				continue;
			}
			Type type = new Type();
			type.setTypeName(typeName);
			type.setClassName(values[0]);
			String library= values[1];
			if (library.startsWith("file:")) { //$NON-NLS-1$
				//old format where URLs were persisted
				library= library.substring(5);
			}
			type.setLibraryEntry(new AntClasspathEntry(library));
			result.add(type);
		}
		return (Type[]) result.toArray(new Type[result.size()]);
	}
	
	protected Property[] extractProperties(Preferences prefs, String[] properties) {
		Property[] result = new Property[properties.length];
		for (int i = 0; i < properties.length; i++) {
			String propertyName = properties[i];
			String[] values = getArrayFromString(prefs.getString(IAntCoreConstants.PREFIX_PROPERTY + propertyName));
			if (values.length < 1) {
				continue;
			}
			Property property = new Property();
			property.setName(propertyName);
			property.setValue(values[0]);
			result[i]= property;
		}
		return result;
	}

	private IAntClasspathEntry[] extractEntries(String[] entries) {
		IAntClasspathEntry[] result = new IAntClasspathEntry[entries.length];
		for (int i = 0; i < entries.length; i++) {
			result[i]= new AntClasspathEntry(entries[i]);
		}
		return result;
	}

	/**
	 * Returns the array of URLs that is the default set of URLs defining
	 * the Ant classpath.
	 * 
	 * Ant running through the command line tries to find tools.jar to help the
	 * user. Try emulating the same behavior here.
	 *
	 * @return the default set of URLs defining the Ant classpath
	 * @deprecated
	 */
	public URL[] getDefaultAntURLs() {
		IAntClasspathEntry[] entries= getDefaultAntHomeEntries();
		List result= new ArrayList(3);
		for (int i = 0; i < entries.length; i++) {
			IAntClasspathEntry entry = entries[i];
			result.add(entry.getEntryURL());
		}
		URL toolsURL= getToolsJarURL();
		if (toolsURL != null) {
			result.add(toolsURL);
		}
		return (URL[]) result.toArray(new URL[result.size()]);
	}
	
	/**
	 * Returns the array of classpath entries that is the default set of entries defining
	 * the Ant classpath.
	 *
	 * @return the default set of classpath entries defining the Ant classpath
	 */
	public synchronized IAntClasspathEntry[] getDefaultAntHomeEntries() {
		if (defaultAntHomeEntries == null) {
			ServiceTracker tracker = new ServiceTracker(AntCorePlugin.getPlugin().getBundle().getBundleContext(), PackageAdmin.class.getName(), null);
			tracker.open();
			try {
				List result = new ArrayList(29);
				PackageAdmin packageAdmin = (PackageAdmin) tracker.getService();
				if (packageAdmin != null) {
					ExportedPackage[] packages = packageAdmin.getExportedPackages("org.apache.tools.ant"); //$NON-NLS-1$
					Bundle bundle = findHighestAntVersion(packages);
					if(bundle == null) {
						for (int i = 0; i < packages.length; i++) {
							bundle = packages[i].getExportingBundle();
							if(bundle == null) {
								continue;
							}
							try {
								addLibraries(bundle, result);
								if(result.size() > 0) {
									break;
								}
							}
							catch(IOException ioe) {
								AntCorePlugin.log(ioe); // maintain logging
								result.clear();
								/*continue to try other providers if an exception occurs*/
							}
						}
					}
					else {
						try {
							addLibraries(bundle, result);
						} catch (IOException ioe) {
							AntCorePlugin.log(ioe); // maintain logging
						}
					}
				}
				defaultAntHomeEntries = (IAntClasspathEntry[]) result.toArray(new IAntClasspathEntry[result.size()]);
			} finally {
				tracker.close();
			}
		}
		return defaultAntHomeEntries;
	}
	
	/**
	 * Simple algorithm to find the highest version of <code>org.apache.ant</code> 
	 * available. If there are other providers that are not <code>org.apache.ant</code>
	 * <code>null</code> is returned so that all bundles will be inspected 
	 * for contributed libraries.
	 * <p>
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=282851
	 * </p>
	 * @param packages the live list of {@link ExportedPackage}s to inspect
	 * @return the bundle that represents the highest version of <code>org.apache.ant</code> or <code>null</code>
	 * if there are other providers for the <code>org.apache.ant.tools</code> packages.
	 */
	Bundle findHighestAntVersion(ExportedPackage[] packages) {
		Bundle bundle = null;
		HashSet bundles = new HashSet();
		for (int i = 0; i < packages.length; i++) {
			bundle = packages[i].getExportingBundle();
			if(bundle == null) {
				continue;
			}
			if("org.apache.ant".equals(bundle.getSymbolicName())) { //$NON-NLS-1$
				bundles.add(bundle);
			}
			else {
				return null;
			}
		}
		Bundle highest = null;
		Bundle temp = null;
		for (Iterator iter = bundles.iterator(); iter.hasNext();) {
			temp = (Bundle)iter.next();
			if(highest == null) {
				highest = temp;
			}
			else {
				if(highest.getVersion().compareTo(temp.getVersion()) < 0) {
					highest = temp;
				}
			}
		}
		return highest;
	}
	
	/**
	 * Returns the array of URLs that is the set of URLs defining the Ant
	 * classpath.
	 * 
	 * @return the set of URLs defining the Ant classpath
	 * @deprecated use getAntHomeClasspathEntries and getToolsJarEntry
	 */
	public URL[] getAntURLs() {
		int extra= 0;
		IAntClasspathEntry entry= getToolsJarEntry();
		if (entry != null) {
			extra++;
		}
		URL[] urls= new URL[antHomeEntries.length + extra];
		int i;
		for (i = 0; i < antHomeEntries.length; i++) {
			URL url = antHomeEntries[i].getEntryURL();
			if (url != null) {
				urls[i]= url;
			}
		}
		if (entry != null) {
			urls[i]= entry.getEntryURL();
		}
		return urls;
		
	}

	protected List computeDefaultTasks(List tasks) {
		List result = new ArrayList(tasks.size());
		for (Iterator iterator = tasks.iterator(); iterator.hasNext();) {
			IConfigurationElement element = (IConfigurationElement) iterator.next();
			if (!relevantRunningHeadless(element)) {
				continue;
			}
			Task task = new Task();
			task.setTaskName(element.getAttribute(AntCorePlugin.NAME));
			task.setClassName(element.getAttribute(AntCorePlugin.CLASS));
			
			configureAntObject(result, element, task, task.getTaskName(), InternalCoreAntMessages.AntCorePreferences_No_library_for_task);
		}
		return result;
	}

	private void addURLToExtraClasspathEntries(URL url, IConfigurationElement element) {
		String eclipseRuntime= element.getAttribute(AntCorePlugin.ECLIPSE_RUNTIME);
		boolean eclipseRuntimeRequired= true;
		if (eclipseRuntime != null) {
			eclipseRuntimeRequired= Boolean.valueOf(eclipseRuntime).booleanValue();
		}
		Iterator itr= extraClasspathURLs.iterator();
		while (itr.hasNext()) {
			IAntClasspathEntry entry = (IAntClasspathEntry) itr.next();
			if (entry.getEntryURL().equals(url)) {
				return;
			}
		}
		
		AntClasspathEntry entry= new AntClasspathEntry(url);
		entry.setEclipseRuntimeRequired(eclipseRuntimeRequired);
		extraClasspathURLs.add(entry);
	}

	protected List computeDefaultTypes(List types) {
		List result = new ArrayList(types.size());
		for (Iterator iterator = types.iterator(); iterator.hasNext();) {
			IConfigurationElement element = (IConfigurationElement) iterator.next();
			if (!relevantRunningHeadless(element)) {
				continue;
			}
			Type type = new Type();
			type.setTypeName(element.getAttribute(AntCorePlugin.NAME));
			type.setClassName(element.getAttribute(AntCorePlugin.CLASS));
			
			configureAntObject(result, element, type, type.getTypeName(), InternalCoreAntMessages.AntCorePreferences_No_library_for_type);
		}
		return result;
	}

	private void configureAntObject(List result, IConfigurationElement element, AntObject antObject, String objectName, String errorMessage) {
		String runtime = element.getAttribute(AntCorePlugin.ECLIPSE_RUNTIME);
		if (runtime != null) {
			antObject.setEclipseRuntimeRequired(Boolean.valueOf(runtime).booleanValue());
		}
		
		String uri = element.getAttribute(AntCorePlugin.URI);
		if (uri != null) {
			antObject.setURI(uri);
		}
		
		String library = element.getAttribute(AntCorePlugin.LIBRARY);
		if (library == null) {
			IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, NLS.bind(InternalCoreAntMessages.AntCorePreferences_Library_not_specified_for___0__4, new String[]{objectName}), null);
			AntCorePlugin.getPlugin().getLog().log(status);
			return;
		}
		
		try {
            IContributor contributor= element.getContributor();
			antObject.setPluginLabel(contributor.getName());
			Bundle bundle = Platform.getBundle(contributor.getName());
			URL url = FileLocator.toFileURL(bundle.getEntry(library));
			File urlFile = new File(url.getPath());
			if (urlFile.exists()) {
				url = new URL("file:" +  urlFile.getAbsolutePath()); //$NON-NLS-1$
				addURLToExtraClasspathEntries(url, element);
				result.add(antObject);
				addPluginClassLoader(bundle);
				antObject.setLibraryEntry(new AntClasspathEntry(url));
				return;
			} 

			//type specifies a library that does not exist
			IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, NLS.bind(errorMessage, new String[]{url.toExternalForm(), element.getContributor().getName()}), null);
			AntCorePlugin.getPlugin().getLog().log(status);
			return;
		} catch (MalformedURLException e) {
			// if the URL does not have a valid format, just log and ignore the exception
			IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_MALFORMED_URL, InternalCoreAntMessages.AntCorePreferences_Malformed_URL__1, e);
			AntCorePlugin.getPlugin().getLog().log(status);
			return;
		} catch (Exception e) {
			//likely extra classpath entry library that does not exist
			IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, NLS.bind(InternalCoreAntMessages.AntCorePreferences_8, new String[]{library,  element.getContributor().getName()}), null);
			AntCorePlugin.getPlugin().getLog().log(status);
			return;
		}
	}

	/*
	 * Computes the extra classpath entries defined plug-ins and fragments.
	 */
	protected void computeDefaultExtraClasspathEntries(List entries) {
		for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
			IConfigurationElement element = (IConfigurationElement) iterator.next();
			if (!relevantRunningHeadless(element)) {
				continue;
			}
			String library = element.getAttribute(AntCorePlugin.LIBRARY);
			Bundle bundle = Platform.getBundle(element.getContributor().getName());
			try {
				URL url = FileLocator.toFileURL(bundle.getEntry(library));
				File urlFile = new File(url.getPath());
				if (urlFile.exists()) {
					url = new URL("file:" +  urlFile.getAbsolutePath()); //$NON-NLS-1$
					addURLToExtraClasspathEntries(url, element);  
					addPluginClassLoader(bundle);
				} else {
					//extra classpath entry that does not exist
					IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, NLS.bind(InternalCoreAntMessages.AntCorePreferences_6, new String[]{url.toExternalForm(), element.getContributor().getName()}), null);
					AntCorePlugin.getPlugin().getLog().log(status);
					continue;
				}
			} catch (MalformedURLException e) {
				//if the URL does not have a valid format, just log and ignore the exception
				IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_MALFORMED_URL, InternalCoreAntMessages.AntCorePreferences_Malformed_URL__1, e);
				AntCorePlugin.getPlugin().getLog().log(status);
				continue;
			} catch (Exception e) {
				//likely extra classpath entry that does not exist
				IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, NLS.bind(InternalCoreAntMessages.AntCorePreferences_6, new String[]{library, element.getContributor().getName()}), null);
				AntCorePlugin.getPlugin().getLog().log(status);
				continue;
			}
		}
	}
	
	private boolean relevantRunningHeadless(IConfigurationElement element) {
		if (runningHeadless) {
			String headless = element.getAttribute(AntCorePlugin.HEADLESS);
			if (headless != null) {
				boolean headlessProperty= Boolean.valueOf(headless).booleanValue();
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
	private void computeDefaultProperties(List properties) {
		defaultProperties = new ArrayList(properties.size());
		for (Iterator iterator = properties.iterator(); iterator.hasNext();) {
			IConfigurationElement element = (IConfigurationElement) iterator.next();
			if (!relevantRunningHeadless(element)) {
				continue;
			}
			String name = element.getAttribute(AntCorePlugin.NAME);
			if (name == null) {
				continue;
			}
			String value = element.getAttribute(AntCorePlugin.VALUE);
			Property property= null;
			if (value != null) {
				property = new Property(name, value);
				property.setPluginLabel(element.getContributor().getName());
			} else {
				Bundle bundle= Platform.getBundle(element.getContributor().getName());
				if (bundle == null) {
					continue;
				}
				property = new Property();
				property.setName(name);
				property.setPluginLabel(element.getContributor().getName());
				String className = element.getAttribute(AntCorePlugin.CLASS);
				property.setValueProvider(className, new WrappedClassLoader(bundle));
			}
			defaultProperties.add(property);
			String runtime = element.getAttribute(AntCorePlugin.ECLIPSE_RUNTIME);
			if (runtime != null) {
				property.setEclipseRuntimeRequired(Boolean.valueOf(runtime).booleanValue());
			}
		}
	}

	/**
	 * Returns the IAntClasspathEntry for the tools.jar associated with the path supplied.
	 * May return <code>null</code> if no tools.jar is found (e.g. the path
	 * points to a JRE install).
	 * 
	 * @param javaHomePath path for Java home
	 * @return IAntClasspathEntry tools.jar IAntClasspathEntry or <code>null</code>
	 * @since 3.0
	 */
	public IAntClasspathEntry getToolsJarEntry(IPath javaHomePath) {
		if ("jre".equalsIgnoreCase(javaHomePath.lastSegment())) { //$NON-NLS-1$
			javaHomePath = javaHomePath.removeLastSegments(1);
		}
		javaHomePath= javaHomePath.append("lib").append("tools.jar"); //$NON-NLS-1$ //$NON-NLS-2$
		File tools= javaHomePath.toFile();
		if (!tools.exists()) {
			//attempt to find in the older 1.1.* 
			javaHomePath= javaHomePath.removeLastSegments(1);
			javaHomePath= javaHomePath.append("classes.zip"); //$NON-NLS-1$
			tools= javaHomePath.toFile();
			if (!tools.exists()) {
				return null;
			}
		}
        
		return new AntClasspathEntry(tools.getAbsolutePath());    
	}

	/**
	 * Returns the URL for the tools.jar associated with the System property "java.home"
	 * location. If "java.home" has no associated tools.jar (such as a JRE install), the environment variable "JAVA_HOME" is
	 * resolved to check for a tools.jar.
     * May return <code>null</code> if no tools.jar is found.
	 * 
	 * @return URL tools.jar URL or <code>null</code>
	 * @deprecated use getToolsJarEntry()
	 */
	public URL getToolsJarURL() {
		IPath path = new Path(System.getProperty("java.home")); //$NON-NLS-1$
		IAntClasspathEntry entry= getToolsJarEntry(path);
		if (entry == null) {
			IDynamicVariable variable = VariablesPlugin.getDefault().getStringVariableManager().getDynamicVariable("env_var"); //$NON-NLS-1$
			String javaHome= null;
			try {
				if (variable != null) {
					javaHome = variable.getValue("JAVA_HOME"); //$NON-NLS-1$
				}
				if (javaHome != null) {
					path= new Path(javaHome);
					entry= getToolsJarEntry(path);
				}
			} catch (CoreException e) {
			}
		}
		if (entry != null) {
			return entry.getEntryURL();
		}
		return null;
	}
	
	/**
	 * Returns the <code>IAntClasspathEntry</code> for the tools.jar associated with the System property "java.home"
	 * location.
	 * If "java.home" has no associated tools.jar (such as a JRE install), the environment variable "JAVA_HOME" is
	 * resolved to check for a tools.jar. 
	 * May return <code>null</code> if no tools.jar is found.
	 * 
	 * @return IAntClasspathEntry tools.jar IAntClasspathEntry or <code>null</code>
	 */
	public IAntClasspathEntry getToolsJarEntry() {
		IPath path = new Path(System.getProperty("java.home")); //$NON-NLS-1$
		IAntClasspathEntry entry= getToolsJarEntry(path);
		if (entry == null) {
			IDynamicVariable variable = VariablesPlugin.getDefault().getStringVariableManager().getDynamicVariable("env_var"); //$NON-NLS-1$
			String javaHome= null;
			try {
				if (variable != null) {
					javaHome = variable.getValue("JAVA_HOME"); //$NON-NLS-1$
				}
				if (javaHome != null) {
					path= new Path(javaHome);
					entry= getToolsJarEntry(path);
				}
			} catch (CoreException e) {
			}
		}
		return entry;
	}
	
	/**
	 * Returns the <code>IAntClasspathEntry</code>s for the jars from ${user.home}/.ant/lib
	 * May return <code>null</code> if jars are found.
	 * 
	 * TODO Should be promoted to API post 3.1
	 * 
	 * @return the collection of <code>IAntClasspathEntry</code> found at ${user.home}/.ant/lib or
	 * <code>null</code> if none found of location does not exist
	 */
	private List getUserLibraries() {
		File libDir= new File(System.getProperty("user.home"), ".ant" + File.separatorChar + "lib"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		URL[] urls= null;
		try {
			urls = getLocationURLs(libDir);
		} catch (MalformedURLException e) {
		}
		if (urls == null) {
			return null;
		}
		
		List entries= new ArrayList(urls.length);
		for (int i = 0; i < urls.length; i++) {
			AntClasspathEntry entry= new AntClasspathEntry(urls[i]);
			entries.add(entry);
		}
		return entries;
	}
	
	 private URL[] getLocationURLs(File location) throws MalformedURLException {
		 URL[] urls= null;
		 if (!location.exists()) {
			 return urls;
		 }
		 final String extension= ".jar"; //$NON-NLS-1$
		 if (!location.isDirectory()) {
			 urls = new URL[1];
			 String path= location.getPath();
			 if (path.toLowerCase().endsWith(extension)) {
				 urls[0]= location.toURL();
			 }
			 return urls;
		 }
		 
		 File[] matches= location.listFiles(
			 new FilenameFilter() {
				 public boolean accept(File dir, String name) {
					 return name.toLowerCase().endsWith(extension);
				 }
			 });
		 
		 urls= new URL[matches.length];
		 for (int i = 0; i < matches.length; ++i) {
			 urls[i] = matches[i].toURL();
		 }
		 return urls;
	 }
	 
	/**
	 * Add the libraries contributed by the Ant plug-in, to the classpath.
	 * @param source
	 * @param destination
	 * @throws IOException 
	 * @throws MalformedURLException
	 */
	private void addLibraries(Bundle source, List destination) throws IOException, MalformedURLException {
		ManifestElement[] libraries = null;
		try {
			libraries = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, (String) source.getHeaders("").get(Constants.BUNDLE_CLASSPATH)); //$NON-NLS-1$
		} catch (BundleException e) {
			IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_MALFORMED_URL, InternalCoreAntMessages.AntCorePreferences_0, e);
			AntCorePlugin.getPlugin().getLog().log(status);
			return;
		}
		if (libraries == null) {
			return;
		}
		URL url = null;
		for (int i = 0; i < libraries.length; i++) {
			url = source.getEntry(libraries[i].getValue());
			if(url != null) {
				destination.add(new AntClasspathEntry(FileLocator.toFileURL(url)));
			}
		}
	}

	protected void addPluginClassLoader(Bundle bundle) {
		WrappedClassLoader loader = new WrappedClassLoader(bundle);
		if (!pluginClassLoaders.contains(loader)) {
			pluginClassLoaders.add(loader);
		}
	}

	/**
	 * Returns the list of URLs added to the classpath by the extra classpath
	 * entries extension point.
	 * 
	 * @return the list of extra classpath URLs
	 */
	public URL[] getExtraClasspathURLs() {
		URL[] urls= new URL[extraClasspathURLs.size()];
		
		for (int i = 0; i < extraClasspathURLs.size(); i++) {
				IAntClasspathEntry entry = (IAntClasspathEntry) extraClasspathURLs.get(i);
				urls[i]= entry.getEntryURL();	
		}
		return urls;
	}
	
	/**
	 * Returns the list of URLs added to the classpath by the extra classpath
	 * entries extension point for an Ant build that is occurring without the Eclipse runtime.
	 * 
	 * @return the list of extra classpath URLs
	 * @since 3.0
	 */
	public URL[] getRemoteExtraClasspathURLs() {
		List urls= new ArrayList(extraClasspathURLs.size());
		
		for (int i = 0; i < extraClasspathURLs.size(); i++) {
				IAntClasspathEntry entry = (IAntClasspathEntry) extraClasspathURLs.get(i);
				if (!entry.isEclipseRuntimeRequired()) {
					urls.add(entry.getEntryURL());
				}
		}
		return (URL[])urls.toArray(new URL[urls.size()]);
	}
	
	/**
	 * Returns the entire set of URLs that define the Ant runtime classpath.
	 * Includes the Ant URLs, the additional URLs and extra classpath URLs.
	 * 
	 * @return the entire runtime classpath of URLs
	 */
	public URL[] getURLs() {
		List result = new ArrayList(60);
		if (antHomeEntries != null) {
			addEntryURLs(result, antHomeEntries);
		}
		if (additionalEntries != null && additionalEntries.length > 0) {
			addEntryURLs(result, additionalEntries);
		}
		
		for (int i = 0; i < extraClasspathURLs.size(); i++) {
			IAntClasspathEntry entry = (IAntClasspathEntry) extraClasspathURLs.get(i);
			URL url= entry.getEntryURL();
			if (url != null) {
				result.add(url);
			}	
		}
		
		return (URL[]) result.toArray(new URL[result.size()]);
	}

	private void addEntryURLs(List result, IAntClasspathEntry[] entries) {
		for (int i = 0; i < entries.length; i++) {
			IAntClasspathEntry entry = entries[i];
			URL url= entry.getEntryURL();
			if (url != null) {
				result.add(url);
			}
		}
	}

	protected ClassLoader[] getPluginClassLoaders() {
		if (orderedPluginClassLoaders == null) {
			Iterator classLoaders= pluginClassLoaders.iterator();
			Map idToLoader= new HashMap(pluginClassLoaders.size());
			List bundles = new ArrayList(pluginClassLoaders.size());
			while (classLoaders.hasNext()) {
				WrappedClassLoader loader = (WrappedClassLoader) classLoaders.next();
				idToLoader.put(loader.bundle.getSymbolicName(), loader);
				bundles.add(Platform.getPlatformAdmin().getState(false).getBundle(loader.bundle.getBundleId()));
			}
			List descriptions = computePrerequisiteOrder(bundles);
			List loaders = new ArrayList(descriptions.size());
			for (Iterator iter = descriptions.iterator(); iter.hasNext(); ) {
				String id =((BundleDescription) iter.next()).getSymbolicName();
				loaders.add(idToLoader.get(id));
			}
			orderedPluginClassLoaders = (WrappedClassLoader[]) loaders.toArray(new WrappedClassLoader[loaders.size()]);
		}
		return orderedPluginClassLoaders;
	}
	
	/*
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private List computePrerequisiteOrder(List plugins) {
		List prereqs = new ArrayList(plugins.size());
		List fragments = new ArrayList();

		// create a collection of directed edges from plugin to prereq
		for (Iterator iter = plugins.iterator(); iter.hasNext();) {
			BundleDescription current = (BundleDescription) iter.next();
			if (current.getHost() != null) {
				fragments.add(current);
				continue;
			}
			boolean found = false;

			BundleDescription[] prereqList = getDependentBundles(current);
			for (int j = 0; j < prereqList.length; j++) {
				// ensure that we only include values from the original set.
				if (plugins.contains(prereqList[j])) {
					found = true;
					prereqs.add(new Relation(current, prereqList[j]));
				}
			}

			// if we didn't find any prereqs for this plugin, add a null prereq
			// to ensure the value is in the output
			if (!found) {
				prereqs.add(new Relation(current, null));
			}
		}

		//The fragments needs to added relatively to their host and to their
		// own prerequisite (bug #43244)
		for (Iterator iter = fragments.iterator(); iter.hasNext();) {
			BundleDescription current = (BundleDescription) iter.next();

			if (plugins.contains(current.getHost().getBundle())) {
				prereqs.add(new Relation(current, current.getHost().getSupplier()));
			} else {
				AntCorePlugin.getPlugin().getLog().log(new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_MALFORMED_URL, NLS.bind(InternalCoreAntMessages.AntCorePreferences_1, new String[] {current.getSymbolicName()}), null));
			}

			BundleDescription[] prereqList = getDependentBundles(current);
			for (int j = 0; j < prereqList.length; j++) {
				// ensure that we only include values from the original set.
				if (plugins.contains(prereqList[j])) {
					prereqs.add(new Relation(current, prereqList[j]));
				}
			}
		}

		// do a topological sort, insert the fragments into the sorted elements
		return computeNodeOrder(prereqs);
	}

	/*
	 * Copied from org.eclipse.pde.internal.build.site.PDEState.
	 */
	private BundleDescription[] getDependentBundles(BundleDescription root) {
		BundleDescription[] imported = getImportedBundles(root);
		BundleDescription[] required = getRequiredBundles(root);
		BundleDescription[] dependents = new BundleDescription[imported.length + required.length];
		System.arraycopy(imported, 0, dependents, 0, imported.length);
		System.arraycopy(required, 0, dependents, imported.length, required.length);
		return dependents;
	}
	
	/*
	 * Copied from org.eclipse.pde.internal.build.site.PDEState.
	 */
	private BundleDescription[] getRequiredBundles(BundleDescription root) {
		if (root == null) {
			return new BundleDescription[0];
		}
		return root.getResolvedRequires();
	}

	/*
	 * Copied from org.eclipse.pde.internal.build.site.PDEState.
	 */
	private BundleDescription[] getImportedBundles(BundleDescription root) {
		if (root == null) {
			return new BundleDescription[0];
		}
		ExportPackageDescription[] packages = root.getResolvedImports();
		ArrayList resolvedImports = new ArrayList(packages.length);
		for (int i = 0; i < packages.length; i++) {
			if (!root.getLocation().equals(packages[i].getExporter().getLocation()) && !resolvedImports.contains(packages[i].getExporter())) {
				resolvedImports.add(packages[i].getExporter());
			}
		}
		return (BundleDescription[]) resolvedImports.toArray(new BundleDescription[resolvedImports.size()]);
	}

	/*
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private void removeArcs(List edges, List roots, Map counts) {
		for (Iterator j = roots.iterator(); j.hasNext();) {
			Object root = j.next();
			for (int i = 0; i < edges.size(); i++) {
				if (root.equals(((Relation) edges.get(i)).to)) {
					Object input = ((Relation) edges.get(i)).from;
					Integer count = (Integer) counts.get(input);
					if (count != null) {
						counts.put(input, new Integer(count.intValue() - 1));
					}
				}
			}
		}
	}

	/*
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private List computeNodeOrder(List edges) {
		Map counts = computeCounts(edges);
		List nodes = new ArrayList(counts.size());
		while (!counts.isEmpty()) {
			List roots = findRootNodes(counts);
			if (roots.isEmpty()) {
				break;
			}
			for (Iterator i = roots.iterator(); i.hasNext();) {
				counts.remove(i.next());
			}
			nodes.addAll(roots);
			removeArcs(edges, roots, counts);
		}
		return nodes;
	}

	/*
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private Map computeCounts(List mappings) {
		Map counts = new HashMap(5);
		for (int i = 0; i < mappings.size(); i++) {
			Object from = ((Relation) mappings.get(i)).from;
			Integer fromCount = (Integer) counts.get(from);
			Object to = ((Relation) mappings.get(i)).to;
			if (to == null)
				counts.put(from, new Integer(0));
			else {
				if (((Integer) counts.get(to)) == null)
					counts.put(to, new Integer(0));
				fromCount = fromCount == null ? new Integer(1) : new Integer(fromCount.intValue() + 1);
				counts.put(from, fromCount);
			}
		}
		return counts;
	}

	/*
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private List findRootNodes(Map counts) {
		List result = new ArrayList(5);
		for (Iterator i = counts.keySet().iterator(); i.hasNext();) {
			Object node = i.next();
			int count = ((Integer) counts.get(node)).intValue();
			if (count == 0) {
				result.add(node);
			}
		}
		return result;
	}
	
	private void initializePluginClassLoaders() {
		pluginClassLoaders = new ArrayList(10);
		// ant.core should always be present
		pluginClassLoaders.add(new WrappedClassLoader(AntCorePlugin.getPlugin().getBundle()));
	}

	/**
	 * Returns the default and custom tasks.
	 * 
	 * @return the list of default and custom tasks.
	 */
	public List getTasks() {
		List result = new ArrayList(10);
		if (defaultTasks != null && !defaultTasks.isEmpty()) {
			result.addAll(defaultTasks);
		}
		if (customTasks != null && customTasks.length != 0) {
			result.addAll(Arrays.asList(customTasks));
		}
		return result;
	}
	
	/**
	 * Returns the default and custom tasks that are relevant when there is no
	 * Eclipse runtime context (an Ant build in a separate VM).
	 * 
	 * @return the list of default and custom tasks.
	 */
	public List getRemoteTasks() {
		List result = new ArrayList(10);
		if (defaultTasks != null && !defaultTasks.isEmpty()) {
			Iterator iter= defaultTasks.iterator();
			while (iter.hasNext()) {
				Task task = (Task) iter.next();
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
	 * @return the user defined tasks
	 */
	public Task[] getCustomTasks() {
		return customTasks;
	}

	/**
	 * Returns the user defined custom types
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
	public List getProperties() {
		List result = new ArrayList(10);
		if (defaultProperties != null && !defaultProperties.isEmpty()) {
			result.addAll(defaultProperties);
		}
		if (customProperties != null && customProperties.length != 0) {
			result.addAll(Arrays.asList(customProperties));
		}
		return result;
	}
	
	/**
	 * Returns the default and custom properties that are relevant when there is no
	 * Eclipse runtime context (Ant build in a separate VM).
	 * 
	 * @return the list of default and custom properties.
	 * @since 3.0
	 */
	public List getRemoteAntProperties() {
		List result = new ArrayList(10);
		if (defaultProperties != null && !defaultProperties.isEmpty()) {
			Iterator iter= defaultProperties.iterator();
			while (iter.hasNext()) {
				Property property = (Property) iter.next();
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
	 * Returns the custom property files specified for Ant builds performing any required 
	 * string substitution if indicated.
	 * 
	 * @param performStringSubstition whether or not to perform the string substitution on the property file strings
	 * @return the property files defined for Ant builds.
	 * @since 3.0
	 */
	public String[] getCustomPropertyFiles(boolean performStringSubstition) {
		if (!performStringSubstition || customPropertyFiles == null || customPropertyFiles.length == 0) {
			return customPropertyFiles;
		}
		List files= new ArrayList(customPropertyFiles.length);
		for (int i = 0; i < customPropertyFiles.length; i++) {
			String filename= customPropertyFiles[i];
			 try {
				filename = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(filename);
				files.add(filename);
			} catch (CoreException e) {
				//notify the user via the Ant console of the missing file
				files.add(filename);
			}
		}
		return (String[])files.toArray(new String[files.size()]);
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
	public URL[] getCustomURLs() {
		URL[] urls= new URL[additionalEntries.length];
		int i;
		for (i = 0; i < additionalEntries.length; i++) {
			URL url = additionalEntries[i].getEntryURL();
			if (url != null) {
				urls[i]=url;
			}
		}
	
		return urls;
	}

	/**
	 * Sets the user defined custom tasks.
	 * To commit the changes, updatePluginPreferences must be
	 * called.
	 * @param tasks
	 */
	public void setCustomTasks(Task[] tasks) {
		oldCustomTasks= customTasks;
		customTasks = tasks;
	}

	/**
	 * Sets the user defined custom types.
	 * To commit the changes, updatePluginPreferences must be
	 * called.
	 * @param types The custom types
	 */
	public void setCustomTypes(Type[] types) {
		oldCustomTypes= customTypes;
		customTypes = types;
	}

	/**
	 * Sets the custom URLs specified for the Ant classpath.
	 * To commit the changes, updatePluginPreferences must be
	 * called.
	 * 
	 * @param urls the URLs defining the Ant classpath
	 * @deprecated use setAdditionalEntries(IAntClasspathEntry)[]
	 */
	public void setCustomURLs(URL[] urls) {
		additionalEntries= new IAntClasspathEntry[urls.length];
		for (int i = 0; i < urls.length; i++) {
			URL url = urls[i];
			IAntClasspathEntry entry= new AntClasspathEntry(url);
			additionalEntries[i]= entry;
		}
	}
	
	/**
	 * Sets the Ant URLs specified for the Ant classpath. To commit the changes,
	 * updatePluginPreferences must be called.
	 * 
	 * @param urls the URLs defining the Ant classpath
	 * @deprecated use setAntHomeEntires(IAntClasspathEntry[])
	 */
	public void setAntURLs(URL[] urls) {
		antHomeEntries= new IAntClasspathEntry[urls.length];
		for (int i = 0; i < urls.length; i++) {
			URL url = urls[i];
			IAntClasspathEntry entry= new AntClasspathEntry(url);
			antHomeEntries[i]= entry;
		}
	}
	
	/**
	 * Sets the custom property files specified for Ant builds. To commit the
	 * changes, updatePluginPreferences must be called.
	 * 
	 * @param paths the absolute paths defining the property files to use.
	 */
	public void setCustomPropertyFiles(String[] paths) {
		customPropertyFiles = paths;
	}
	
	/**
	 * Sets the custom user properties specified for Ant builds. To commit the
	 * changes, updatePluginPreferences must be called.
	 * 
	 * @param properties the properties defining the Ant properties
	 */
	public void setCustomProperties(Property[] properties) {
		oldCustomProperties= customProperties;
		customProperties = properties;
	}

	/**
	 * Returns the default and custom types.
	 * 
	 * @return all of the defined types
	 */
	public List getTypes() {
		List result = new ArrayList(10);
		if (defaultTypes != null && !defaultTypes.isEmpty()) {
			result.addAll(defaultTypes);
		}
		if (customTypes != null && customTypes.length != 0) {
			result.addAll(Arrays.asList(customTypes));
		}
		return result;
	}
	
	/**
	 * Returns the default and custom types that are relevant when there is no
	 * Eclipse runtime context (an Ant build in a separate VM).
	 * 
	 * @return the list of default and custom types.
	 */
	public List getRemoteTypes() {
		List result = new ArrayList(10);
		if (defaultTypes != null && !defaultTypes.isEmpty()) {
			Iterator iter= defaultTypes.iterator();
			while (iter.hasNext()) {
				Type type = (Type) iter.next();
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
	public List getDefaultTypes() {
		List result = new ArrayList(10);
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
	public List getDefaultTasks() {
		List result = new ArrayList(10);
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
	public List getDefaultProperties() {
		List result = new ArrayList(10);
		if (defaultProperties != null && !defaultProperties.isEmpty()) {
			result.addAll(defaultProperties);
		}
		return result;
	}

	/*
	 * Convert a list of tokens into an array using "," as the tokenizer.
	 */
	protected String[] getArrayFromString(String list) {
		String separator= ","; //$NON-NLS-1$
		if (list == null || list.trim().equals("")) { //$NON-NLS-1$
			return new String[0];
		}
		ArrayList result = new ArrayList();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) { //$NON-NLS-1$
				result.add(token);
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	/**
	 * Updates the underlying plug-in preferences to the current state.
	 */
	public void updatePluginPreferences() {
		Preferences prefs = AntCorePlugin.getPlugin().getPluginPreferences();
		prefs.removePropertyChangeListener(this);
		updateTasks(prefs);
		updateTypes(prefs);
		updateAntHomeEntries(prefs);
		updateAdditionalEntries(prefs);
		updateProperties(prefs);
		updatePropertyFiles(prefs);
		boolean classpathChanged= AntCorePlugin.getPlugin().getPluginPreferences().needsSaving();
		AntCorePlugin.getPlugin().savePluginPreferences();
		if (classpathChanged) {
			prefs.setValue(IAntCoreConstants.PREFERENCE_CLASSPATH_CHANGED, true);
		}
		prefs.setValue(IAntCoreConstants.PREFERENCE_CLASSPATH_CHANGED, false);
		prefs.addPropertyChangeListener(this);
	}

	protected void updateTasks(Preferences prefs) {
		if (oldCustomTasks != null) {
			for (int i = 0; i < oldCustomTasks.length; i++) {
				Task oldTask = oldCustomTasks[i];
				prefs.setToDefault(IAntCoreConstants.PREFIX_TASK + oldTask.getTaskName());
			}
			oldCustomTasks= null;	
		}	
		
		if (customTasks.length == 0) {
			prefs.setValue(IAntCoreConstants.PREFERENCE_TASKS, ""); //$NON-NLS-1$
			return;
		}
		StringBuffer tasks = new StringBuffer();
		for (int i = 0; i < customTasks.length; i++) {
			tasks.append(customTasks[i].getTaskName());
			tasks.append(',');
			prefs.setValue(IAntCoreConstants.PREFIX_TASK + customTasks[i].getTaskName(), customTasks[i].getClassName() + "," + customTasks[i].getLibraryEntry().getLabel()); //$NON-NLS-1$
		}
		prefs.setValue(IAntCoreConstants.PREFERENCE_TASKS, tasks.toString());
	}

	protected void updateTypes(Preferences prefs) {
		if (oldCustomTypes != null) {
			for (int i = 0; i < oldCustomTypes.length; i++) {
				Type oldType = oldCustomTypes[i];
				prefs.setToDefault(IAntCoreConstants.PREFIX_TYPE + oldType.getTypeName());
			}
			oldCustomTypes= null;	
		}	
				
		if (customTypes.length == 0) {
			prefs.setValue(IAntCoreConstants.PREFERENCE_TYPES, ""); //$NON-NLS-1$
			return;
		}
		StringBuffer types = new StringBuffer();
		for (int i = 0; i < customTypes.length; i++) {
			types.append(customTypes[i].getTypeName());
			types.append(',');
			prefs.setValue(IAntCoreConstants.PREFIX_TYPE + customTypes[i].getTypeName(), customTypes[i].getClassName() + "," + customTypes[i].getLibraryEntry().getLabel()); //$NON-NLS-1$
		}
		prefs.setValue(IAntCoreConstants.PREFERENCE_TYPES, types.toString());
	}
	
	protected void updateProperties(Preferences prefs) {
		if (oldCustomProperties != null) {
			for (int i = 0; i < oldCustomProperties.length; i++) {
				Property oldProperty = oldCustomProperties[i];
				prefs.setToDefault(IAntCoreConstants.PREFIX_PROPERTY + oldProperty.getName());
			}
			oldCustomProperties= null;
		}
		
		if (customProperties.length == 0) {
			prefs.setValue(IAntCoreConstants.PREFERENCE_PROPERTIES, ""); //$NON-NLS-1$
			return;
		}
		StringBuffer properties = new StringBuffer();
		for (int i = 0; i < customProperties.length; i++) {
			properties.append(customProperties[i].getName());
			properties.append(',');
			prefs.setValue(IAntCoreConstants.PREFIX_PROPERTY + customProperties[i].getName(), customProperties[i].getValue(false));
		}
		prefs.setValue(IAntCoreConstants.PREFERENCE_PROPERTIES, properties.toString());
	}

	protected void updateAdditionalEntries(Preferences prefs) {
		prefs.setValue("urls", ""); //old constant removed  //$NON-NLS-1$//$NON-NLS-2$
		String serialized= ""; //$NON-NLS-1$
		IAntClasspathEntry toolsJarEntry= getToolsJarEntry();
		List userLibs= getUserLibraries();
		if (userLibs == null) {
			userLibs= new ArrayList();
		} 
		if (toolsJarEntry != null) {
			userLibs.add(toolsJarEntry);
		}
		boolean changed= true;
		if (additionalEntries.length == userLibs.size()) {
			changed= false;
			for (int i = 0; i < additionalEntries.length; i++) {
				if (!additionalEntries[i].equals(userLibs.get(i))) {
					changed= true;
					break;
				}
			}
		}
		if (changed) {
			StringBuffer entries = new StringBuffer();
			for (int i = 0; i < additionalEntries.length; i++) {
				entries.append(additionalEntries[i].getLabel());
				entries.append(',');
			}
			serialized= entries.toString();
		}
		
		prefs.setValue(IAntCoreConstants.PREFERENCE_ADDITIONAL_ENTRIES, serialized);
		
		String prefAntHome= ""; //$NON-NLS-1$
		if (antHome != null && !antHome.equals(getDefaultAntHome())) {
			prefAntHome= antHome;
		} 
		prefs.setValue(IAntCoreConstants.PREFERENCE_ANT_HOME, prefAntHome);
	}
	
	protected void updateAntHomeEntries(Preferences prefs) {
		prefs.setValue("ant_urls", ""); //old constant removed  //$NON-NLS-1$//$NON-NLS-2$
		
		//see if the custom entries are just the default entries
		IAntClasspathEntry[] defaultEntries= getDefaultAntHomeEntries();
		boolean dflt= false;
		if (defaultEntries.length == antHomeEntries.length) {
			dflt= true;
			for (int i = 0; i < antHomeEntries.length; i++) {
				if (!antHomeEntries[i].equals(defaultEntries[i])) {
					dflt= false;
					break;
				}
			}
		}
		if (dflt) {
			//always want to recalculate the default Ant urls
			//to pick up any changes in the default Ant classpath
			prefs.setValue(IAntCoreConstants.PREFERENCE_ANT_HOME_ENTRIES, ""); //$NON-NLS-1$
			return;
		}
		StringBuffer entries = new StringBuffer();
		for (int i = 0; i < antHomeEntries.length; i++) {
			entries.append(antHomeEntries[i].getLabel());
			entries.append(',');
		}
		
		prefs.setValue(IAntCoreConstants.PREFERENCE_ANT_HOME_ENTRIES, entries.toString());
	}
	
	protected void updatePropertyFiles(Preferences prefs) {
		StringBuffer files = new StringBuffer();
		for (int i = 0; i < customPropertyFiles.length; i++) {
			files.append(customPropertyFiles[i]);
			files.append(',');
		}
		
		prefs.setValue(IAntCoreConstants.PREFERENCE_PROPERTY_FILES, files.toString());
	}
	
	/**
	 * Sets the string that defines the Ant home set by the user.
	 * May be set to <code>null</code>.
	 * 
	 * @param antHome the fully qualified path to Ant home
	 */
	public void setAntHome(String antHome) {
		this.antHome= antHome;
	}
	
	/**
	 * Returns the string that defines the Ant home set by the user or the location 
	 * of the Eclipse Ant plug-in if Ant home has not been specifically set by the user.
	 * Can return <code>null</code>
	 * 
	 * @return the fully qualified path to Ant home
	 */
	public String getAntHome() {
		return antHome;
	}
	
	/**
	 * Returns the set of classpath entries that compose the libraries added to the
	 * Ant runtime classpath from the Ant home location.
	 * 
	 * @return the set of ant home classpath entries
	 * @since 3.0
	 */
	public IAntClasspathEntry[] getAntHomeClasspathEntries() {
		return antHomeEntries;
	}
	
	/**
	 * Returns the set of classpath entries that the user has added to the
	 * Ant runtime classpath.
	 * 
	 * @return the set of user classpath entries
	 * @since 3.0
	 */
	public IAntClasspathEntry[] getAdditionalClasspathEntries() {
		return additionalEntries;
	}
	
	/**
	 * Sets the set of classpath entries that compose the libraries added to the
	 * Ant runtime classpath from the Ant home location.
	 * 
	 * @param entries the set of ant home classpath entries
	 * @since 3.0
	 */
	public void setAntHomeClasspathEntries(IAntClasspathEntry[] entries) {
		antHomeEntries= entries;
	}
	
	/**
	 * Sets the set of classpath entries that the user has added to the 
	 * Ant runtime classpath.
	 * 
	 * @param entries the set of user classpath entries
	 * @since 3.0
	 */
	public void setAdditionalClasspathEntries(IAntClasspathEntry[] entries) {
		additionalEntries= entries;
	}

	/**
	 * Returns the list of URLs to added to the classpath for an Ant build that is 
	 * occurring without the Eclipse runtime.
	 * 
	 * @return the list of classpath entries
	 * @since 3.0
	 */
	public URL[] getRemoteAntURLs() {
		List result = new ArrayList(40);
		if (antHomeEntries != null) {
			for (int i = 0; i < antHomeEntries.length; i++) {
				IAntClasspathEntry entry = antHomeEntries[i];
				result.add(entry.getEntryURL());
			}
		}
		if (additionalEntries != null && additionalEntries.length > 0) {
			for (int i = 0; i < additionalEntries.length; i++) {
				IAntClasspathEntry entry = additionalEntries[i];
				result.add(entry.getEntryURL());	
			}
		}
		if (extraClasspathURLs != null) {
			for (int i = 0; i < extraClasspathURLs.size(); i++) {
				IAntClasspathEntry entry = (IAntClasspathEntry) extraClasspathURLs.get(i);
				if (!entry.isEclipseRuntimeRequired()) {
					result.add(entry.getEntryURL());
				}
			}
		}
		
		return (URL[]) result.toArray(new URL[result.size()]);
	}
	
	/**
	 * Returns all contributed classpath entries via the 
	 * <code>extraClasspathEntries</code> extension point.
	 * 
	 * @return all contributed classpath entries via the 
	 * <code>extraClasspathEntries</code> extension point
	 * @since 3.0
	 */
	public IAntClasspathEntry[]getContributedClasspathEntries() {
		return (IAntClasspathEntry[]) extraClasspathURLs.toArray(new IAntClasspathEntry[extraClasspathURLs.size()]);
	}
}