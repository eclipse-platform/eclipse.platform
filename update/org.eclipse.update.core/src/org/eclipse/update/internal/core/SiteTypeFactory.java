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
package org.eclipse.update.internal.core;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;

/**
 * 
 */
public final class SiteTypeFactory {
	

	/**
	 * extension point ID
	 */
	public static final String SIMPLE_EXTENSION_ID = "siteTypes";	 //$NON-NLS-1$
	

	private static SiteTypeFactory inst;
	
	private Map factories;

	/**
	 * hide ctr 
	 */
	private SiteTypeFactory() {
	}

	public static SiteTypeFactory getInstance() {
		if (inst == null)
			inst = new SiteTypeFactory();
		return inst;
	}


	/**
	 * return the factory for the type
	 */
	public ISiteFactory getFactory(String type) throws CoreException {
			//
			Object instance = getFactories().get(type);
			if (instance==null) {
				instance = createFactoryFor(type);
				getFactories().put(type,instance);
			}
			return (ISiteFactory) instance;
	}

	/**
	 * 
	 */
	private ISiteFactory createFactoryFor(String type) throws CoreException {
		ISiteFactory result = null;
		
		String pluginID = UpdateCore.getPlugin().getBundle().getSymbolicName();
		IPluginRegistry pluginRegistry = Platform.getPluginRegistry();
		IConfigurationElement[] elements = pluginRegistry.getConfigurationElementsFor(pluginID,SIMPLE_EXTENSION_ID,type);
		if (elements==null || elements.length==0){
			throw Utilities.newCoreException(Policy.bind("SiteTypeFactory.UnableToFindSiteFactory",type),null); //$NON-NLS-1$
		} else {
			IConfigurationElement element = elements[0];
			result = (ISiteFactory)element.createExecutableExtension("class"); //$NON-NLS-1$
		}
		return result;
	}

	/**
	 * Gets the actories.
	 * @return Returns a Map
	 */
	private Map getFactories() {
		if (factories==null) factories = new HashMap();
			return factories;
	}


}
