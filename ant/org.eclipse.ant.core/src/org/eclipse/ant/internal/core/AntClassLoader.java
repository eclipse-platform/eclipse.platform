/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.core;


import java.net.URL;
import java.net.URLClassLoader;

import org.eclipse.core.boot.BootLoader;

public class AntClassLoader extends URLClassLoader {

	protected ClassLoader[] pluginLoaders;
	private static final String ANT_PACKAGES_PREFIX= "org.apache.tools"; //$NON-NLS-1$
	private static final String[] ECLIPSE_ANT_PACKAGES_PREFIXES= new String[4];
	private static final boolean devMode= BootLoader.inDevelopmentMode();
	
	static {
		if (devMode) {
			ECLIPSE_ANT_PACKAGES_PREFIXES[0]= "org.eclipse.ant.internal.core.ant"; //$NON-NLS-1$
			ECLIPSE_ANT_PACKAGES_PREFIXES[1]= "org.eclipse.ui.externaltools.internal.ant.logger"; //$NON-NLS-1$
			ECLIPSE_ANT_PACKAGES_PREFIXES[2]= "org.eclipse.ui.externaltools.internal.ant.inputhandler"; //$NON-NLS-1$
			ECLIPSE_ANT_PACKAGES_PREFIXES[3]= "org.eclipse.ant.tests.core.support"; //$NON-NLS-1$
		}
	}
	
	public AntClassLoader(URL[] urls, ClassLoader[] pluginLoaders) {
		super(urls, ClassLoader.getSystemClassLoader());
		this.pluginLoaders = pluginLoaders;
	}

	public Class loadClass(String name) throws ClassNotFoundException {
		Class result = null;
		//do not load the "base" ant classes from the plugin class loaders 
		//these should only be specified from the Ant runtime classpath preferences setting
		if (!(name.startsWith(ANT_PACKAGES_PREFIX))) {
			if (devMode) { 
				if (shouldPluginsLoad(name)) {
					result= loadClassPlugins(name);
				}
		  	} else {
				result= loadClassPlugins(name);
		  	}
		}
		if (result == null) {
			result = loadClassURLs(name);
		}
		if (result == null) {
			throw new ClassNotFoundException(name);
		}
		return result;
	}
	
	/**
	 * Used during development mode to disallow plugins from loading classes
	 * that should be loaded only be this class loader.
	 */
	private boolean shouldPluginsLoad(String name) {
		for (int i = 0; i < ECLIPSE_ANT_PACKAGES_PREFIXES.length; i++) {
			String prefix = ECLIPSE_ANT_PACKAGES_PREFIXES[i];
			if (name.startsWith(prefix)) {
				return false;	
			}
		}
		return true;
	}

	protected Class loadClassURLs(String name) {
		try {
			return super.loadClass(name);
		} catch (ClassNotFoundException e) {
			// Ignore exception now. If necessary we'll throw
			// a ClassNotFoundException in loadClass(String)
		}
		return null;
	}

	protected Class loadClassPlugins(String name) {
		Class result = null;
		if (pluginLoaders != null) {
			for (int i = 0; (i < pluginLoaders.length) && (result == null); i++) {
				try {
					result = pluginLoaders[i].loadClass(name);
				} catch (ClassNotFoundException e) {
					// Ignore exception now. If necessary we'll throw
					// a ClassNotFoundException in loadClass(String)
				}
			}
		}
		return result;
	}
}
