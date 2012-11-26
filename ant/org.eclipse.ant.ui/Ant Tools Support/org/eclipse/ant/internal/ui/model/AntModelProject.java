/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * Portions Copyright  2000-2004 The Apache Software Foundation
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Apache Software License v2.0 which 
 * accompanies this distribution and is available at 
 * http://www.apache.org/licenses/LICENSE-2.0.
 * 
 * Contributors:
 *     IBM Corporation - derived implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.model;

import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.types.Path;
import org.eclipse.ant.internal.core.IAntCoreConstants;

/**
 * Derived from the original Ant Project class
 * This class allows property values to be written multiple times.
 * This facilitates incremental parsing of the Ant build file
 * It also attempts to ensure that we clean up after ourselves and allows
 * more manipulation of properties resulting from incremental parsing.
 * Also allows the Eclipse additions to the Ant runtime classpath.
 */
public class AntModelProject extends Project {
	
	/**
	 * Delegate to maintain property chaining - to make sure our project is alerted 
	 * to new properties being set
	 */
	class AntPropertyHelper implements PropertyHelper.PropertySetter {
		/* (non-Javadoc)
		 * @see org.apache.tools.ant.PropertyHelper.PropertySetter#setNew(java.lang.String, java.lang.Object, org.apache.tools.ant.PropertyHelper)
		 */
		public boolean setNew(String property, Object value, PropertyHelper propertyHelper) {
			setNewProperty(property, value.toString());
			return false;
		}

		/* (non-Javadoc)
		 * @see org.apache.tools.ant.PropertyHelper.PropertySetter#set(java.lang.String, java.lang.Object, org.apache.tools.ant.PropertyHelper)
		 */
		public boolean set(String property, Object value, PropertyHelper propertyHelper) {
			return false;
		}
	}
	
	private AntPropertyNode fCurrentConfiguringPropertyNode;
	private Map idrefs = Collections.synchronizedMap(new HashMap());
	private static Object loaderLock = new Object();
	private Hashtable loaders = null;
	
	/**
	 * Constructor
	 * <p>
	 * Allows us to register a {@link PropertyHelper.PropertySetter} delegate for this project
	 * </p>
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public AntModelProject() {
		PropertyHelper.getPropertyHelper(this).add(new AntPropertyHelper());
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Project#setNewProperty(java.lang.String, java.lang.String)
	 */
	public void setNewProperty(String name, String value) {
		if(PropertyHelper.getPropertyHelper(this).getProperty(name) != null) {
			return;
		}
		//allows property values to be over-written for this parse session
		//there is currently no way to remove properties from the Apache Ant project
		//the project resets it properties for each parse...see reset()
		if (fCurrentConfiguringPropertyNode != null) {
			fCurrentConfiguringPropertyNode.addProperty(name, value);
		}
		super.setProperty(name, value);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Project#fireBuildFinished(java.lang.Throwable)
	 */
	public void fireBuildFinished(Throwable exception) {
		super.fireBuildFinished(exception);
		Enumeration e= getBuildListeners().elements();
		while (e.hasMoreElements()) {
			BuildListener listener = (BuildListener) e.nextElement();
			removeBuildListener(listener);
		}
	}
	
	/**
	 * Reset the project
	 */
	public void reset() {
		getTargets().clear();
		setDefault(null);
		setDescription(null);
		setName(IAntCoreConstants.EMPTY_STRING);
		synchronized (loaderLock) {
			if(loaders != null) {
				Iterator i = loaders.entrySet().iterator();
				Entry e = null;
				while(i.hasNext()) {
					e = (Entry) i.next();
					AntClassLoader acl = (AntClassLoader) e.getValue();
					acl.cleanup();
					acl.clearAssertionStatus();
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Project#getProperty(java.lang.String)
	 */
	public String getProperty(String name) {
		//override as we cannot remove properties from the Apache Ant project
		String result= super.getProperty(name);
		if (result == null) {
			return getUserProperty(name);
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Project#addIdReference(java.lang.String, java.lang.Object)
	 */
	public void addIdReference(String id, Object value) {
		//XXX hack because we cannot look up references by id in Ant 1.8.x
		//see https://issues.apache.org/bugzilla/show_bug.cgi?id=49659
		idrefs.put(id, value);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Project#getReference(java.lang.String)
	 */
	public Object getReference(String key) {
		Object ref = super.getReference(key);/*references.get(key);*/
		if(ref == null) {
			ref = idrefs.get(key);
			if(ref instanceof UnknownElement) {
				UnknownElement ue = (UnknownElement) ref;
				ue.maybeConfigure();
				return ue.getRealThing();
			}
		}
		return ref;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Project#getProperties()
	 */
	public Hashtable getProperties() {
		//override as we cannot remove properties from the Apache Ant project
		Hashtable allProps = super.getProperties();
		allProps.putAll(getUserProperties());
		allProps.put("basedir", getBaseDir().getPath()); //$NON-NLS-1$
		return allProps;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Project#setBaseDir(java.io.File)
	 */
	public void setBaseDir(File baseDir) throws BuildException {
		super.setBaseDir(baseDir);
		setNewProperty("basedir", getBaseDir().getPath()); //$NON-NLS-1$
	}

	/**
	 * @param node the property node that is currently being configured
	 */
	public void setCurrentConfiguringProperty(AntPropertyNode node) {
		fCurrentConfiguringPropertyNode= node;
	}
	
	 /* (non-Javadoc)
     * @see org.apache.tools.ant.Project#createClassLoader(org.apache.tools.ant.types.Path)
     */
    public AntClassLoader createClassLoader(Path path) {
    	synchronized (loaderLock) {
    		if(loaders == null) {
    			loaders = new Hashtable(8);
    		}
    		Path p = path;
    		if(p == null) {
    			p = new Path(this);
    		}
    		String pstr = p.toString();
    		AntClassLoader loader = (AntClassLoader) loaders.get(pstr);
    		if(loader == null) {
    			loader = super.createClassLoader(path);
    	    	if (path == null) {
    	    		//use the "fake" Eclipse runtime classpath for Ant
    	    		loader.setClassPath(Path.systemClasspath);
    	    	}
    	    	loaders.put(pstr, loader);
    		}
    		return loader;
		}
    }
}