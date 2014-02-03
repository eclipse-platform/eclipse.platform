/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;

import org.eclipse.update.configurator.*;
import org.eclipse.update.core.*;

/**
 * Keeps track of all the features and plugins installed by Update mgr
 * so they can be uninstalled later.
 * The info is persisted in the .config/registry file and each entry has a key=key where
 * for feature this key is feature_<id>_<version> and for plugins
 * key is plugin_<id>_<version>. Normally, getVersionedIdentifier() will
 * return <id>_<version>. Eg: feature_org.eclipse.platform_3.0.0
 * 
 */
public class InstallRegistry extends Properties {

    private static final long serialVersionUID = 1L;
    private File file = null;
	private final static String REGISTRY = "registry"; //$NON-NLS-1$
	private static InstallRegistry instance;
	
	// plugins installed in this eclipse session
	private HashMap justInstalledPlugins = new HashMap();
	
	/**
	 * Creates empty Properties.
	 */
	private InstallRegistry() {
		super();
		String configFile =
			ConfiguratorUtils
				.getCurrentPlatformConfiguration()
				.getConfigurationLocation()
				.getFile();
		file = new File(configFile);
		file = file.getParentFile();
		file = new File(file, REGISTRY);
		restore();
	}

	/**
	 * Singleton
	 */
	public static InstallRegistry getInstance() {
		if (instance == null)
			instance = new InstallRegistry();
		return instance;
	}

	/**
	 * Restores contents of the Properties from a file.
	 * @return true if persistant data was read in
	 */
	public boolean restore() {
		InputStream in = null;
		boolean loaded = false;
		clear();
		// Test if we have a contribution file to start with
		// If this is a clean start, then we will not have a 
		// contribution file. return false.
		if (!file.exists())
			return loaded;
		try {
			in = new FileInputStream(file);
			super.load(in);
			loaded = true;
		} catch (IOException e) {
			UpdateCore.log(e);
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
				}
		}
		return loaded;
	}
	/**
	 * Saves contents of the table to a file.
	 * @return true if operation was successful
	 */
	public synchronized boolean save() {
		OutputStream out = null;
		boolean ret = false;
		try {
			out = new FileOutputStream(file);
			super.store(out, "This is a generated file; do not edit."); //$NON-NLS-1$
			ret = true;
		} catch (IOException e) {
			UpdateCore.log(e);
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
			}
		}
		return ret;
	}
	
	/**
	 * Registers an installed feature so it can be uninstalled later.
	 * @param feature feature to register.
	 */
	public static synchronized void registerFeature(IFeature feature) {
		String name = "feature_"+feature.getVersionedIdentifier(); //$NON-NLS-1$
		if (InstallRegistry.getInstance().get(name) == null) {
			InstallRegistry.getInstance().put(name, name);
			// we save after each registration
			InstallRegistry.getInstance().save();
		}
	}
	
	/**
	 * Registers an installed feature so it can be uninstalled later.
	 * @param pluginEntry plugin to register.
	 */
	public static synchronized void registerPlugin(IPluginEntry pluginEntry) {
		String name = "plugin_"+pluginEntry.getVersionedIdentifier(); //$NON-NLS-1$
		if (InstallRegistry.getInstance().get(name) == null) {
			InstallRegistry.getInstance().put(name, name);
			// we save after each registration
			InstallRegistry.getInstance().save();
		}
		
		// add plugin to the list of just installed plugins .
		InstallRegistry.getInstance().justInstalledPlugins.put(name,name);
	}
	
	/**
	 * Removes specified feature from registry
	 *
	 */
	public static synchronized void unregisterFeature(IFeature feature) {
		String name = "feature_"+feature.getVersionedIdentifier(); //$NON-NLS-1$
		InstallRegistry.getInstance().remove(name);
	}
	
	/**
	 * Removes specified plugin from registry
	 *
	 */
	public static synchronized void unregisterPlugin(IPluginEntry pluginEntry) {
		String name = "plugin_"+pluginEntry.getVersionedIdentifier(); //$NON-NLS-1$
		InstallRegistry.getInstance().remove(name);
		
		// remove the plugin from the list of just installed plugins (if needed).
		InstallRegistry.getInstance().justInstalledPlugins.remove(name);
	}
	
	/**
	 * Returns true if the plugin was installed during this eclipse session
	 * @param pluginEntry
	 * @return
	 */
	public boolean isPluginJustInstalled(IPluginEntry pluginEntry) {
		String name = "plugin_"+pluginEntry.getVersionedIdentifier(); //$NON-NLS-1$
		return InstallRegistry.getInstance().justInstalledPlugins.get(name) != null;
	}
	
	/**
	 * This method is only needed for the update JUnit tests.
	 *
	 */
	public static void cleanup() {
		InstallRegistry.getInstance().justInstalledPlugins.clear();
	}
}
