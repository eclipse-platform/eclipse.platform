/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
 * Manages FeatureFactory extension point
 */
public final class FeatureTypeFactory {

	private static FeatureTypeFactory inst;
	private Map factories;

	private static final String SIMPLE_EXTENSION_ID = "featureTypes";	//$NON-NLS-1$	

	/*
	 * hide constructor
	 */
	private FeatureTypeFactory() {
	}

	/*
	 * Singleton pattern
	 */
	public static FeatureTypeFactory getInstance() {
		if (inst == null)
			inst = new FeatureTypeFactory();
		return inst;
	}

	/*
	 * return the factory for the associated type
	 */
	public IFeatureFactory getFactory(String type) throws CoreException {
		//
		Object instance = getFactories().get(type);
		if (instance == null) {
			instance = createFactoryFor(type);
			getFactories().put(type, instance);
		}
		return (IFeatureFactory) instance;
	}

	/*
	 * creates a factory for the associated type and cache it 
	 */
	private IFeatureFactory createFactoryFor(String type) throws CoreException {
		IFeatureFactory result = null;

		String pluginID =
			UpdateCore.getPlugin().getBundle().getSymbolicName();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements =
			registry.getConfigurationElementsFor(pluginID, SIMPLE_EXTENSION_ID, type);

		if (elements == null || elements.length == 0) {
			throw Utilities.newCoreException(
					NLS.bind(Messages.FeatureTypeFactory_UnableToFindFeatureFactory, (new String[] { type })),
					null);
		} 

		IConfigurationElement element = elements[0];
		result = (IFeatureFactory) element.createExecutableExtension("class");	//$NON-NLS-1$
		return result;
	}

	/*
	 * 
	 */
	private Map getFactories() {
		if (factories == null)
			factories = new HashMap();
		return factories;
	}
}
