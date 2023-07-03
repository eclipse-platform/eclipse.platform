/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.intro.impl.model.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroIdElement;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.eclipse.ui.internal.intro.impl.util.Util;

/**
 * Base class for handling Intro Extensions.
 */
public class BaseExtensionPointManager {

	// the config extension id
	protected static final String CONFIG = "org.eclipse.ui.intro.config"; //$NON-NLS-1$

	// the configExtension extension id
	protected static final String CONFIG_EXTENSION = "org.eclipse.ui.intro.configExtension"; //$NON-NLS-1$

	// the attribute in the config element to specify the intro part id.
	protected static final String ATT_CONFIG_INTRO_ID = "introId"; //$NON-NLS-1$

	// the attribute in the config element to specify the intro part id.
	protected static final String ATT_CONFIG_EXTENSION_CONFIG_ID = "configId"; //$NON-NLS-1$

	// the id attribute in any intro element.
	protected static final String ATT_ID = AbstractIntroIdElement.ATT_ID;


	protected Map<String, IntroModelRoot> introModels = new HashMap<>();
	protected IExtensionRegistry registry;
	protected SharedConfigExtensionsManager sharedConfigExtensionsManager;
	private String extensionFilter;

	/*
	 * Prevent creation.
	 */
	protected BaseExtensionPointManager() {
		registry = Platform.getExtensionRegistry();
	}

	protected IntroModelRoot loadModel(String attributeName,
			String attributeValue) {

		long start = 0;
		if (Log.logPerformance)
			start = System.currentTimeMillis();

		// get all Config extension point contributions. There could be more
		// than one config contribution, but there should only be one that maps
		// to the cached intro part id.
		IConfigurationElement introConfig = getIntroConfig(attributeName,
			attributeValue);

		// load model with the config elements of the correct contribution. If
		// there are no valid contribution, model stays null.
		if (introConfig != null) {
			// we found matching config. Get all configExtension contributed to
			// this config and pass to model. Load generic config extensions as
			// well.
			String configId = introConfig.getAttribute(ATT_ID);
			IConfigurationElement[] introConfigExtensions = null;
			if (configId == null)
				// if id of config is null, pass empty array.
				introConfigExtensions = new IConfigurationElement[0];
			else
				introConfigExtensions = getIntroConfigExtensions(
					ATT_CONFIG_EXTENSION_CONFIG_ID, configId);

			if (Log.logPerformance)
				Util.logPerformanceTime(
					"BEGIN:  quering registry for configs took: ", start); //$NON-NLS-1$


			IntroModelRoot model = new IntroModelRoot(introConfig,
				introConfigExtensions);
			model.loadModel();
			// add the current model to the hash table of models, only if it is
			// not null. They key is the model id, which is the id of the
			// config that defined this model.
			addCachedModel(model.getId(), model);

			// now load all generic config extension. ie: standbyPart and
			// command contributions.
			loadSharedConfigExtensions();

			if (Log.logPerformance)
				Util
					.logPerformanceTime(
						"loading Intro Model (quering registry/creating & resolving model) took: ", //$NON-NLS-1$
						start);

			return model;
		}
		return null;
	}

	/**
	 * Go through all the config elements and only return the correct config
	 * that maps to the correct intro part id. If there is more than one config
	 * thats maps to the same intro part id, log the fact, and return the first
	 * one. If there are non, return null.
	 *
	 * @param configElements
	 * @return
	 */
	protected IConfigurationElement getIntroConfig(String attrributeName,
			String attributeValue) {

		IConfigurationElement[] configElements = registry
			.getConfigurationElementsFor(CONFIG);

		IConfigurationElement config = getConfigurationFromAttribute(
			configElements, attrributeName, attributeValue);

		if (config == null)
			// if there is no valid config, log the fact.
			Log.warning("No Intro configuration found with " + attrributeName //$NON-NLS-1$
					+ " of value = " + attributeValue); //$NON-NLS-1$

		return config;
	}

	/**
	 * Go through all the configExtension elements and return an array of all
	 * extensions that match the attribute and its value. If there are non,
	 * return empty array. This also loads all standby contributions.
	 */
	protected IConfigurationElement[] getIntroConfigExtensions(
			String attrributeName, String attributeValue) {

		IConfigurationElement[] configExtensionElements = registry
			.getConfigurationElementsFor(CONFIG_EXTENSION);

		/*
		 * Extension filter is used for performance testing to only load contributions
		 * from a specific plug-in (fixed data set).
		 */
		if (extensionFilter != null) {
			List<IConfigurationElement> filtered = new ArrayList<>();
			for (int i=0;i<configExtensionElements.length;++i) {
				if (extensionFilter.equals(configExtensionElements[i].getContributor().getName())) {
					filtered.add(configExtensionElements[i]);
				}
			}
			configExtensionElements = filtered.toArray(new IConfigurationElement[filtered.size()]);
		}

		IConfigurationElement[] configExtensions = getConfigurationsFromAttribute(
			configExtensionElements, attrributeName, attributeValue);

		return configExtensions;
	}

	/**
	 * Add a model to the cache. This method is private because only this
	 * manager class knows how to load an intro model.
	 *
	 * @param modelId
	 * @param model
	 */
	protected void addCachedModel(String modelId, IntroModelRoot model) {
		introModels.put(modelId, model);
	}

	/**
	 * Gets the given model from the cache.
	 *
	 * @param modelId
	 */
	protected IntroModelRoot getCachedModel(String configId) {
		return introModels.get(configId);
	}

	/**
	 * Go through all the config elements and only return the correct config
	 * with an attribute of the given value. If there is more than one
	 * configuration element that maps to the attribute value log the fact, and
	 * return the first one. If there are non, return null.
	 *
	 * @param configElements
	 * @return
	 */
	protected IConfigurationElement getConfigurationFromAttribute(
			IConfigurationElement[] configElements, String attributeName,
			String attributeValue) {

		// find all configs with given attribute and attibute value.
		IConfigurationElement[] filteredConfigElements = getConfigurationsFromAttribute(
			configElements, attributeName, attributeValue);
		// now validate that we got only one.
		IConfigurationElement config = ModelLoaderUtil
			.validateSingleContribution(filteredConfigElements, attributeName);
		return config;
	}

	/**
	 * Go through all the config elements and return an array of matching
	 * configs with an attribute of the given value. If there are non, return
	 * empty array.
	 */
	protected IConfigurationElement[] getConfigurationsFromAttribute(
			IConfigurationElement[] configElements, String attributeName,
			String attributeValue) {

		// find all configs with given attribute and attibute value.
		Vector<IConfigurationElement> elements = new Vector<>();
		for (int i = 0; i < configElements.length; i++) {
			String currentAttributeValue = configElements[i]
				.getAttribute(attributeName);
			if (currentAttributeValue != null
					&& currentAttributeValue.equals(attributeValue))
				elements.add(configElements[i]);
		}

		// now return array.
		IConfigurationElement[] filteredConfigElements = new IConfigurationElement[elements
			.size()];
		elements.copyInto(filteredConfigElements);

		return filteredConfigElements;
	}

	/**
	 * Loads all shared config extennsions (ie: standby parts and commands).
	 */
	protected void loadSharedConfigExtensions() {
		sharedConfigExtensionsManager = new SharedConfigExtensionsManager(
			registry);
		sharedConfigExtensionsManager.loadSharedConfigExtensions();
	}


	/**
	 * @return Returns the sharedConfigExtensionsManager.
	 */
	public SharedConfigExtensionsManager getSharedConfigExtensionsManager() {
		return sharedConfigExtensionsManager;
	}

	/**
	 * @return Returns the cached introModels.
	 */
	public Map<String, IntroModelRoot> getIntroModels() {
		return introModels;
	}

	/*
	 * Internal test hook for restricting which extensions to load.
	 */
	public void setExtensionFilter(String pluginId) {
		extensionFilter = pluginId;
	}
}
