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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
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
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(pluginID,SIMPLE_EXTENSION_ID,type);
		if (elements==null || elements.length==0){
			throw Utilities.newCoreException(NLS.bind(Messages.SiteTypeFactory_UnableToFindSiteFactory, (new String[] { type })),null);
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
