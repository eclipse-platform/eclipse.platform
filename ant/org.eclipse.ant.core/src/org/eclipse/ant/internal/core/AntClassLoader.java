/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tom Tromey (tromey@redhat.com) - patch for bug 40972
 *******************************************************************************/
package org.eclipse.ant.internal.core;


import java.net.URL;
import java.net.URLClassLoader;

public class AntClassLoader extends URLClassLoader {
	
	private boolean allowPluginLoading= false;

	protected ClassLoader[] pluginLoaders;
	private static final String ANT_PACKAGES_PREFIX= "org.apache.tools"; //$NON-NLS-1$
	
	public AntClassLoader(URL[] urls, ClassLoader[] pluginLoaders) {
		super(urls, ClassLoader.getSystemClassLoader());
		this.pluginLoaders = pluginLoaders;
	}

	protected Class findClass(String name) throws ClassNotFoundException {
		Class result = null;
		//check whether to load the Apache Ant classes from the plugin class loaders 
		//or to only load from the URLs specified from the Ant runtime classpath preferences setting
		if (allowPluginLoading || !(name.startsWith(ANT_PACKAGES_PREFIX))) {
			result= loadClassPlugins(name);
		} 
		
		if (result == null) {
			result = loadClassURLs(name);
		}
		if (result == null) {
			throw new ClassNotFoundException(name);
		}
		return result;
	}

	protected Class loadClassURLs(String name) {
		try {
			return super.findClass(name);
		} catch (ClassNotFoundException e) {
			// Ignore exception now. If necessary we'll throw
			// a ClassNotFoundException in findClass(String)
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
	
	/**
	 * Sets whether this classloader will allow Apache Ant classes to be found or
	 * loaded from its set of plugin classloaders.
	 * 
	 * @param allowLoading whether or not to allow the plugin classloaders
	 * to load the Apache Ant classes
	 */
	public void allowPluginClassLoadersToLoadAntClasses(boolean allowLoading) {
		this.allowPluginLoading = allowLoading;
	}
}
