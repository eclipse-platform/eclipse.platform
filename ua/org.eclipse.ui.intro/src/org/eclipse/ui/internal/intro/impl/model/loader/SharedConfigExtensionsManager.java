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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.ui.internal.intro.impl.model.IntroStandbyContentPart;
import org.eclipse.ui.internal.intro.impl.model.IntroURLAction;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.eclipse.ui.internal.intro.impl.util.Util;

/**
 * Class for handling all shared Intro Config Extensions. These are StandbyPart
 * and Command contributions. Once defined these contributions are visible by
 * all intro configs.
 */

public class SharedConfigExtensionsManager {

	private IExtensionRegistry registry;

	// Holds all standbyPart extensions
	private Map<String, IntroStandbyContentPart> standbyParts = new HashMap<>();

	// Holds all command extensions
	private Map<String, IntroURLAction> commands = new HashMap<>();

	/*
	 * Prevent creation.
	 */
	protected SharedConfigExtensionsManager(IExtensionRegistry registry) {
		this.registry = registry;
	}

	/**
	 * Loads all shared config extennsions (ie: standby parts and commands.
	 */
	protected void loadSharedConfigExtensions() {
		// simply create model classes for all standbyPart elements under a
		// configExtension.

		long start = 0;
		// if we need to log performance, capture time.
		if (Log.logPerformance)
			start = System.currentTimeMillis();

		IConfigurationElement[] configExtensionElements = registry
			.getConfigurationElementsFor(BaseExtensionPointManager.CONFIG_EXTENSION);

		if (Log.logPerformance)
			Util.logPerformanceTime(
				"quering registry for configExtensions took: ", start); //$NON-NLS-1$

		for (int i = 0; i < configExtensionElements.length; i++) {
			IConfigurationElement element = configExtensionElements[i];
			if (!ModelLoaderUtil.isValidElementName(element,
				IntroStandbyContentPart.TAG_STANDBY_CONTENT_PART)
					&& !ModelLoaderUtil.isValidElementName(element,
						IntroURLAction.TAG_ACTION))
				// if extension is not a standbypart or command, ignore.
				continue;
			createModelClass(element);
		}
	}


	/**
	 * Create an intro standby part or an intro command model class.
	 *
	 * @param element
	 */
	private void createModelClass(IConfigurationElement element) {
		if (element.getName().equals(
			IntroStandbyContentPart.TAG_STANDBY_CONTENT_PART)) {
			IntroStandbyContentPart standbyPartContent = new IntroStandbyContentPart(
				element);
			if (standbyPartContent.getId() == null)
				// no id, ignore.
				return;
			standbyParts.put(standbyPartContent.getId(), standbyPartContent);
		} else {
			IntroURLAction introURLCommand = new IntroURLAction(element);
			if (introURLCommand.getName() == null
					|| introURLCommand.getReplaceValue() == null)
				// no name or resolvedValue, ignore.
				return;
			commands.put(introURLCommand.getName(), introURLCommand);
		}
	}



	/**
	 * @return Returns a standbyPart basd on its registred id.
	 */
	public IntroStandbyContentPart getStandbyPart(String partId) {
		if (partId == null)
			return null;
		return standbyParts.get(partId);
	}

	/**
	 * @return Returns the command from its name.
	 */
	public IntroURLAction getCommand(String commandName) {
		if (commandName == null)
			return null;
		return commands.get(commandName);
	}

}
