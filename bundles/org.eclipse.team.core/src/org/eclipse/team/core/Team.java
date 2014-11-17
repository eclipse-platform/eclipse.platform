/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.importing.provisional.IBundleImporter;
import org.eclipse.team.core.mapping.IStorageMerger;
import org.eclipse.team.internal.core.*;
import org.eclipse.team.internal.core.importing.BundleImporterExtension;

/**
 * The Team class provides a global point of reference for the global ignore set
 * and the text/binary registry.
 * 
 * @since 2.0
 */
public final class Team {
    
    private static class StringMappingWrapper implements IFileTypeInfo {
        
        private final IStringMapping fMapping;

        public StringMappingWrapper(IStringMapping mapping) {
            fMapping= mapping;
        }

        public String getExtension() {
            return fMapping.getString();
        }

        public int getType() {
            return fMapping.getType();
        }
        
    }
	
	private static final String PREF_TEAM_IGNORES = "ignore_files"; //$NON-NLS-1$
	private static final String PREF_TEAM_SEPARATOR = "\n"; //$NON-NLS-1$
	public static final Status OK_STATUS = new Status(IStatus.OK, TeamPlugin.ID, IStatus.OK, Messages.ok, null); 
	
	// File type constants
	public static final int UNKNOWN = 0;
	public static final int TEXT = 1;
	public static final int BINARY = 2;
	

	// The ignore list that is read at startup from the persisted file
	protected static SortedMap globalIgnore, pluginIgnore;
	private static StringMatcher[] ignoreMatchers;
    
    private final static FileContentManager fFileContentManager;
    
	private static List fBundleImporters;

    static {
        fFileContentManager= new FileContentManager();
    }
	
	
	/**
     * Return the type of the given IStorage. First, we check whether a mapping has
     * been defined for the name of the IStorage. If this is not the case, we check for
     * a mapping with the extension. If no mapping is defined, UNKNOWN is returned.
	 * 
	 * Valid return values are:
	 * Team.TEXT
	 * Team.BINARY
	 * Team.UNKNOWN
	 * 
	 * @param storage  the IStorage
	 * @return whether the given IStorage is TEXT, BINARY, or UNKNOWN
     * 
     * @deprecated Use <code>getFileContentManager().getType(IStorage storage)</code> instead.
	 */
	public static int getType(IStorage storage) {
        return fFileContentManager.getType(storage);
	}

	/**
	 * Returns whether the given file or folder with its content should be ignored.
	 * 
	 * This method answers true if the file matches one of the global ignore
	 * patterns, or if the file is marked as derived.
	 * 
	 * @param resource the file or folder
	 * @return whether the file should be ignored
	 */
	public static boolean isIgnoredHint(IResource resource) {
		if (resource.isDerived()) return true;
		return matchesEnabledIgnore(resource);
	}
	
	/**
	 * Returns whether the given file should be ignored.
	 * @param file file to check
	 * @return <code>true</code> if this file should be ignored, and <code>false</code> otherwise
	 * @deprecated use isIgnoredHint(IResource) instead
	 */
	public static boolean isIgnoredHint(IFile file) {
		if (file.isDerived()) return true;
		return matchesEnabledIgnore(file);
	}
	
	private static boolean matchesEnabledIgnore(IResource resource) {
		StringMatcher[] matchers = getStringMatchers();
		for (int i = 0; i < matchers.length; i++) {
			String resourceName = resource.getName();
			if(matchers[i].isPathPattern()) {
				resourceName = resource.getFullPath().toString();
			}
			if (matchers[i].match(resourceName)) return true;
		}
		return false;
	}
	
	/**
	 * Returns whether the given file should be ignored.
	 * @param file file to check
	 * @return <code>true</code> if this file should be ignored, and <code>false</code> otherwise
	 * @deprecated use isIgnoredHint instead
	 */
	public static boolean isIgnored(IFile file) {
		return matchesEnabledIgnore(file);
	}

	
	/**
     * Return all known file types.
	 * 
	 * @return all known file types
     * @deprecated Use <code>getFileContentManager().getExtensionMappings()</code> instead.
	 */
	public static IFileTypeInfo[] getAllTypes() {
        final IStringMapping [] mappings= fFileContentManager.getExtensionMappings();
        final IFileTypeInfo [] infos= new IFileTypeInfo[mappings.length];
        for (int i = 0; i < infos.length; i++) {
            infos[i]= new StringMappingWrapper(mappings[i]);
        }
        return infos;
	}
    
	/**
	 * Returns the list of global ignores.
	 * @return all ignore infos representing globally ignored patterns
	 */
	public synchronized static IIgnoreInfo[] getAllIgnores() {
		// The ignores are cached and when the preferences change the
		// cache is cleared. This makes it faster to lookup without having
		// to re-parse the preferences.
		initializeIgnores();
		IIgnoreInfo[] result = getIgnoreInfo(globalIgnore);
		return result;
	}

	private static void initializeIgnores() {
		if (globalIgnore == null) {
			globalIgnore = new TreeMap();
			pluginIgnore = new TreeMap();
			ignoreMatchers = null;
			try {
				readIgnoreState();
			} catch (TeamException e) {
				TeamPlugin.log(IStatus.ERROR, Messages.Team_Error_loading_ignore_state_from_disk_1, e); 
			}
			initializePluginIgnores(pluginIgnore, globalIgnore);
		}
	}

	private static IIgnoreInfo[] getIgnoreInfo(Map gIgnore) {
		IIgnoreInfo[] result = new IIgnoreInfo[gIgnore.size()];
		Iterator e = gIgnore.entrySet().iterator();
		int i = 0;
		while (e.hasNext() ) {
			Map.Entry entry = (Entry) e.next();
			final String pattern = (String) entry.getKey();
			final boolean enabled = ((Boolean)entry.getValue()).booleanValue();
			result[i++] = new IIgnoreInfo() {
				private String p = pattern;
				private boolean e1 = enabled;
				public String getPattern() {
					return p;
				}
				public boolean getEnabled() {
					return e1;
				}
			};
		}
		return result;
	}

	private synchronized static StringMatcher[] getStringMatchers() {
		if (ignoreMatchers==null) {
			IIgnoreInfo[] ignorePatterns = getAllIgnores();
			ArrayList matchers = new ArrayList(ignorePatterns.length);
			for (int i = 0; i < ignorePatterns.length; i++) {
				if (ignorePatterns[i].getEnabled()) {
					matchers.add(new StringMatcher(ignorePatterns[i].getPattern(), true, false));
				}
			}
			ignoreMatchers = new StringMatcher[matchers.size()];
			ignoreMatchers = (StringMatcher[]) matchers.toArray(ignoreMatchers);
		}
		return ignoreMatchers;
	}
	
	
	/**
     * Set the file type for the give extensions. This
     * will replace the existing file types with this new list.
	 *
	 * Valid types are:
	 * Team.TEXT
	 * Team.BINARY
	 * Team.UNKNOWN
	 * 
	 * @param extensions  the file extensions
	 * @param types  the file types
     * 
     * @deprecated Use <code>getFileContentManager().setExtensionMappings()</code> instead.
	 */
	public static void setAllTypes(String[] extensions, int[] types) {
        fFileContentManager.addExtensionMappings(extensions, types);
	}
    
	/**
	 * Add patterns to the list of global ignores.
	 * 
	 * @param patterns Array of patterns to set
	 * @param enabled Array of booleans indicating if given pattern is enabled 
	 */
	public static void setAllIgnores(String[] patterns, boolean[] enabled) {
		initializeIgnores();
		globalIgnore = new TreeMap();
		ignoreMatchers = null;
		for (int i = 0; i < patterns.length; i++) {
			globalIgnore.put(patterns[i], Boolean.valueOf(enabled[i]));
		}
		// Now set into preferences
		StringBuffer buf = new StringBuffer();
		Iterator e = globalIgnore.entrySet().iterator();
		while (e.hasNext()) {
			Map.Entry entry = (Entry) e.next();
			String pattern = (String) entry.getKey();
			Boolean value = (Boolean) entry.getValue();
			boolean isCustom = (!pluginIgnore.containsKey(pattern)) ||
				!((Boolean)pluginIgnore.get(pattern)).equals(value);
			if (isCustom) {
				buf.append(pattern);
				buf.append(PREF_TEAM_SEPARATOR);
				boolean en = value.booleanValue();
				buf.append(en);
				buf.append(PREF_TEAM_SEPARATOR);
			}
			
		}
		TeamPlugin.getPlugin().getPluginPreferences().setValue(PREF_TEAM_IGNORES, buf.toString());
	}
	
	


	/*
	 * IGNORE
	 * 
	 * Reads the ignores currently defined by extensions.
	 */
	private static void initializePluginIgnores(SortedMap pIgnore, SortedMap gIgnore) {
		TeamPlugin plugin = TeamPlugin.getPlugin();
		if (plugin != null) {
			IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(TeamPlugin.ID, TeamPlugin.IGNORE_EXTENSION);
			if (extension != null) {
				IExtension[] extensions =  extension.getExtensions();
				for (int i = 0; i < extensions.length; i++) {
					IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
					for (int j = 0; j < configElements.length; j++) {
						String pattern = configElements[j].getAttribute("pattern"); //$NON-NLS-1$
						if (pattern != null) {
							String selected = configElements[j].getAttribute("enabled"); //$NON-NLS-1$
							if (selected == null) {
								// Check for selected because this used to be the field name
								selected = configElements[j].getAttribute("selected"); //$NON-NLS-1$
							}
							boolean enabled = selected != null
									&& selected.equalsIgnoreCase("true"); //$NON-NLS-1$
							if (!pIgnore.containsKey(pattern)) {
								pIgnore.put(pattern, Boolean.valueOf(enabled));
							} else if (!Boolean.valueOf(enabled).equals(
									pIgnore.get(pattern))) {
								if(Policy.DEBUG){
									TeamPlugin
											.log(IStatus.WARNING,
													NLS.bind(
															Messages.Team_Conflict_occured_for_ignored_resources_pattern,
															new Object[] {
																	pattern,
																	collectContributingExtentionsToDisplay(
																			pattern,
																			extensions) }),
													null);
								}
								// if another plug-in already added this pattern
								// change the value only to disabled
								if (!enabled) {
									pIgnore.put(pattern,
											Boolean.FALSE);
								}
							}
						}
					}
				}

				Iterator it = pIgnore.keySet().iterator();
				while (it.hasNext()) {
					Object pattern = it.next();
					if (!gIgnore.containsKey(pattern)) {
						gIgnore.put(pattern, pIgnore.get(pattern));
					}
				}
			}
		}
	}

	private static String collectContributingExtentionsToDisplay(
			String patternToFind, IExtension[] extensions) {
		StringBuffer sb = new StringBuffer();
		boolean isFirst = true;
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] configElements = extensions[i]
					.getConfigurationElements();
			for (int j = 0; j < configElements.length; j++) {
				if (patternToFind.equals(configElements[j]
						.getAttribute("pattern"))) { //$NON-NLS-1$
					if (!isFirst) {
						sb.append(", "); //$NON-NLS-1$
					}
					isFirst = false;
					sb.append(extensions[i].getContributor().getName());
				}
			}
		}
		return sb.toString();
	}

	/*
	 * IGNORE
	 * 
	 * Reads global ignore preferences and populates globalIgnore
	 */
	private static void readIgnoreState() throws TeamException {
		if (readBackwardCompatibleIgnoreState()) return;
		Preferences pref = TeamPlugin.getPlugin().getPluginPreferences();
		if (!pref.contains(PREF_TEAM_IGNORES)) return;
		pref.addPropertyChangeListener(new Preferences.IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				// when a property is changed, invalidate our cache so that
				// properties will be recalculated.
				if(event.getProperty().equals(PREF_TEAM_IGNORES))
					globalIgnore = null;
			}
		});
		String prefIgnores = pref.getString(PREF_TEAM_IGNORES);
		StringTokenizer tok = new StringTokenizer(prefIgnores, PREF_TEAM_SEPARATOR);
		String pattern, enabled;
		try {
			while (true) {
				pattern = tok.nextToken();
				if (pattern.length()==0) return;
				enabled = tok.nextToken();
				globalIgnore.put(pattern, Boolean.valueOf(enabled));
			} 
		} catch (NoSuchElementException e) {
			return;
		}
	}

	/*
	 * For backward compatibility, we still look at if we have .globalIgnores
	 */
	private static boolean readBackwardCompatibleIgnoreState() throws TeamException {
		String GLOBALIGNORE_FILE = ".globalIgnores"; //$NON-NLS-1$
		IPath pluginStateLocation = TeamPlugin.getPlugin().getStateLocation().append(GLOBALIGNORE_FILE);
		File f = pluginStateLocation.toFile();
		if (!f.exists()) return false;
		try {
			DataInputStream dis = new DataInputStream(new FileInputStream(f));
			try {
				int ignoreCount = 0;
				try {
					ignoreCount = dis.readInt();
				} catch (EOFException e) {
					// Ignore the exception, it will occur if there are no ignore
					// patterns stored in the provider state file.
					return false;
				}
				for (int i = 0; i < ignoreCount; i++) {
					String pattern = dis.readUTF();
					boolean enabled = dis.readBoolean();
					globalIgnore.put(pattern, Boolean.valueOf(enabled));
				}
			} finally {
				dis.close();
			}
			f.delete();
		} catch (FileNotFoundException e) {
			// not a fatal error, there just happens not to be any state to read
		} catch (IOException ex) {
			throw new TeamException(new Status(IStatus.ERROR, TeamPlugin.ID, 0, Messages.Team_readError, ex));			 
		}
		return true;
	}
	/**
	 * Initialize the registry, restoring its state.
	 * 
	 * This method is called by the plug-in upon startup, clients should not call this method
	 */
	public static void startup() {
		// Register a delta listener that will tell the provider about a project move and meta-file creation
		ResourcesPlugin.getWorkspace().addResourceChangeListener(new TeamResourceChangeListener(), IResourceChangeEvent.POST_CHANGE);
	}
	
	/**
	 * Shut down the registry, persisting its state.
	 * 
	 * This method is called by the plug-in upon shutdown, clients should not call this method
	 */	
	public static void shutdown() {
		TeamPlugin.getPlugin().savePluginPreferences();
	}
	/**
	 * @deprecated 
	 * 		Use {@link org.eclipse.team.core.RepositoryProviderType#getProjectSetCapability()}
	 * 		to obtain an instance of {@link ProjectSetCapability} instead.
	 */
	public static IProjectSetSerializer getProjectSetSerializer(String id) {
		TeamPlugin plugin = TeamPlugin.getPlugin();
		if (plugin != null) {
			IExtensionPoint extension = RegistryFactory.getRegistry().getExtensionPoint(TeamPlugin.ID, TeamPlugin.PROJECT_SET_EXTENSION);
			if (extension != null) {
				IExtension[] extensions =  extension.getExtensions();
				for (int i = 0; i < extensions.length; i++) {
					IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
					for (int j = 0; j < configElements.length; j++) {
						String extensionId = configElements[j].getAttribute("id"); //$NON-NLS-1$
						if (extensionId != null && extensionId.equals(id)) {
							try {
								return (IProjectSetSerializer)configElements[j].createExecutableExtension("class"); //$NON-NLS-1$
							} catch (CoreException e) {
								TeamPlugin.log(e);
								return null;
							}
						}
					}
				}
			}		
		}
		return null;
	}
	

	/**
	 * Return the default ignore infos
	 * (i.e. those that are specified in
	 * plugin manifests).
	 * @return the default ignore infos.
	 * @since 3.0
	 */
	public static IIgnoreInfo[] getDefaultIgnores() {
		SortedMap gIgnore = new TreeMap();
		SortedMap pIgnore = new TreeMap();
		initializePluginIgnores(pIgnore, gIgnore);
		return getIgnoreInfo(gIgnore);
	}

	/**
     * TODO: change to file content manager
	 * Return the default file type bindings
	 * (i.e. those that are specified in
	 * plugin manifests).
	 * @return the default file type bindings
	 * @since 3.0
     * @deprecated Use Team.getFileContentManager().getDefaultExtensionMappings() instead.
	 */
	public static IFileTypeInfo[] getDefaultTypes() {
        return asFileTypeInfo(getFileContentManager().getDefaultExtensionMappings());
	}

    private static IFileTypeInfo [] asFileTypeInfo(IStringMapping [] mappings) {
        final IFileTypeInfo [] infos= new IFileTypeInfo[mappings.length];
        for (int i = 0; i < infos.length; i++) {
            infos[i]= new StringMappingWrapper(mappings[i]);
        }
        return infos;
    }

    /**
     * Get the file content manager which implements the API for manipulating the mappings between
     * file names, file extensions and content types.
     *  
     * @return an instance of IFileContentManager
     * 
     * @see IFileContentManager
     * 
     * @since 3.1
     */
    public static IFileContentManager getFileContentManager() {
        return fFileContentManager;
    }
    
	/**
	 * Creates a storage merger for the given content type.
	 * If no storage merger is registered for the given content type <code>null</code> is returned.
	 *
	 * @param type the type for which to find a storage merger
	 * @return a storage merger for the given type, or <code>null</code> if no
	 *   storage merger has been registered
	 *   
	 * @since 3.4
	 */
    public static IStorageMerger createMerger(IContentType type) {
    	return StorageMergerRegistry.getInstance().createStreamMerger(type);
    }
    
	/**
	 * Creates a storage merger for the given file extension.
	 * If no storage merger is registered for the file extension <code>null</code> is returned.
	 *
	 * @param extension the extension for which to find a storage merger
	 * @return a stream merger for the given type, or <code>null</code> if no
	 *   storage merger has been registered
	 *   
	 * @since 3.4
	 */
    public static IStorageMerger createMerger(String extension) {
    	return StorageMergerRegistry.getInstance().createStreamMerger(extension);
    }
    
	/**
	 * Creates a storage merger for the given content type.
	 * If no storage merger is registered for the given content type <code>null</code> is returned.
	 *
	 * @param type the type for which to find a storage merger
	 * @return a storage merger for the given type, or <code>null</code> if no
	 *   storage merger has been registered
	 * @deprecated Use {@link #createMerger(IContentType)} instead.
	 * @since 3.2
	 */
    public IStorageMerger createStorageMerger(IContentType type) {
    	return createMerger(type);
    }
    
	/**
	 * Creates a storage merger for the given file extension.
	 * If no storage merger is registered for the file extension <code>null</code> is returned.
	 *
	 * @param extension the extension for which to find a storage merger
	 * @return a stream merger for the given type, or <code>null</code> if no
	 *   storage merger has been registered
	 * @deprecated Use {@link #createMerger(String)} instead.
	 * @since 3.2
	 */
    public IStorageMerger createStorageMerger(String extension) {
    	return createMerger(extension);
    }

	/**
	 * Returns the available bundle importers.
	 * 
	 * <p>
	 * <strong>EXPERIMENTAL</strong>. This interface has been added as part of a
	 * work in progress. There is no guarantee that this API will work or that
	 * it will remain the same. Please do not use this API without consulting
	 * with the Team team.
	 * </p>
	 * 
	 * @return IBundleImporter[] returns the available bundle importers
	 * @since 3.6
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public synchronized static IBundleImporter[] getBundleImporters() {
		if (fBundleImporters == null) {
			fBundleImporters = new ArrayList();
			IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(TeamPlugin.EXTENSION_POINT_BUNDLE_IMPORTERS);
			if (point != null) {
				IConfigurationElement[] infos = point.getConfigurationElements();
				for (int i = 0; i < infos.length; i++) {
					fBundleImporters.add(new BundleImporterExtension(infos[i]));
				}
			}
		}
		return (IBundleImporter[]) fBundleImporters.toArray(new IBundleImporter[fBundleImporters.size()]);
	}
}
