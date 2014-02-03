/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tom Tromey (tromey@redhat.com) - patch for bug 40972
 *******************************************************************************/
package org.eclipse.ant.internal.core;


import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class AntClassLoader extends URLClassLoader {

    private static final String ANT_PACKAGES_PREFIX = "org.apache.tools"; //$NON-NLS-1$
    private static final String ANT_URL_PREFIX = "org/apache/tools";     //$NON-NLS-1$
    
    private boolean fAllowPluginLoading = false;

    protected ClassLoader[] fPluginLoaders;

    private ClassLoader fContextClassloader = null;
    
    public AntClassLoader(URL[] urls, ClassLoader[] pluginLoaders) {
        super(urls, ClassLoader.getSystemClassLoader());
        fPluginLoaders = pluginLoaders;
    }

    /*
     * @see java.net.URLClassLoader#findClass(java.lang.String)
     */
    protected Class findClass(String name) throws ClassNotFoundException {
        Class result = null;
        //check whether to load the Apache Ant classes from the plug-in class loaders 
        //or to only load from the URLs specified from the Ant runtime classpath preferences setting
        if (fAllowPluginLoading || !(name.startsWith(ANT_PACKAGES_PREFIX))) {
            result = loadClassPlugins(name);
        } 
        
        if (result != null) {
            return result;
        }
        
        return super.findClass(name);
    }

    protected Class loadClassPlugins(String name) {
        //remove this class loader as the context class loader
        //when loading classes from plug-ins...see bug 94471
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        if (fContextClassloader != null) {
            Thread.currentThread().setContextClassLoader(fContextClassloader);
        }
        try {
            Class result = null;
            if (fPluginLoaders != null) {
                for (int i = 0; (i < fPluginLoaders.length) && (result == null); i++) {
                    try {
                        result = fPluginLoaders[i].loadClass(name);
                    } catch (ClassNotFoundException e) {
                        // Ignore exception now. If necessary we'll throw
                        // a ClassNotFoundException in loadClass(String)
                    }
                }
            }
            return result;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
    
    /*
     * @see java.net.URLClassLoader#findResource(java.lang.String)
     */
    public URL findResource(String name) {
    	 if (fAllowPluginLoading || !(name.startsWith(ANT_URL_PREFIX))) {
             URL result = findResourcePlugins(name);
             if (result != null) {
            	 return result;
             }
         } 
    	
    	return super.findResource(name);
    }
    
    private URL findResourcePlugins(String name) {
    	//remove this class loader as the context class loader
    	//when loading resources from plug-ins...see bug 94471
    	ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    	if (fContextClassloader != null) {
    		Thread.currentThread().setContextClassLoader(fContextClassloader);
    	}
    	try {
    		URL result = null;
    		if (fPluginLoaders != null) {
    			for (int i = 0; i < fPluginLoaders.length; i++) {
    				result = fPluginLoaders[i].getResource(name);
    				if (result != null) {
    	    			return result;
    	    		}
    			}
    		}
    	} finally {
    		Thread.currentThread().setContextClassLoader(originalClassLoader);
    	}
    	return null;
    }
    
    /*
     * @see java.net.URLClassLoader#findResources(java.lang.String)
     */
    public Enumeration findResources(String name) throws IOException {
    	ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    	if (fContextClassloader != null) {
    		Thread.currentThread().setContextClassLoader(fContextClassloader);
    	}
    	List all = new ArrayList();
    	try {
    		if (fAllowPluginLoading || !(name.startsWith(ANT_URL_PREFIX) || name.startsWith(ANT_URL_PREFIX, 1))) {
    			if (fPluginLoaders != null) {
    				Enumeration result = null;
    				for (int i = 0; i < fPluginLoaders.length; i++) {
    					result = fPluginLoaders[i].getResources(name);
    					while (result.hasMoreElements()) {
    						all.add(result.nextElement());
    					}
    				}
    			}
    		}

    		Enumeration superResources = super.findResources(name);
    		if (all.isEmpty()) {
    			return superResources;
    		}

    		while (superResources.hasMoreElements()) {
    			all.add(superResources.nextElement());
    		}
    		return Collections.enumeration(all);
    	} finally {
    		Thread.currentThread().setContextClassLoader(originalClassLoader);
    	}
    }
    
    /**
     * Sets whether this class loader will allow Apache Ant classes or resources to be found or
     * loaded from its set of plug-in class loaders.
     * 
     * @param allowLoading whether or not to allow the plug-in class loaders
     * to load the Apache Ant classes or resources
     */
    public void allowPluginClassLoadersToLoadAnt(boolean allowLoading) {
        fAllowPluginLoading = allowLoading;
    }
    
    public void setPluginContextClassloader(ClassLoader classLoader) {
        fContextClassloader = classLoader;
    }
}