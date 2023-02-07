/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.internal.intro.impl.model.AbstractBaseIntroElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroIdElement;
import org.eclipse.ui.internal.intro.impl.model.util.BundleUtil;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.osgi.framework.Bundle;
import org.w3c.dom.Element;


/**
 * Utilities class for help with loading the intro model from the Platform
 * runtime model and from the DOM of content files.
 */
public class ModelLoaderUtil {

	/**
	 * Utility method to validate an elements name.
	 *
	 * @param element
	 * @param validName
	 * @return
	 */
	public static boolean isValidElementName(IConfigurationElement element,
			String validName) {

		if (element.getName().equals(validName))
			return true;
		// bad element name.
		return false;
	}

	/**
	 * Utility method to verify that there is only a single configElement in the
	 * passed array of elements. If the array is empty, null is returned. If
	 * there is more than one element in the array, the first one is picked, but
	 * this fact is logged. Attribute passed is used for logging.
	 *
	 * @param configElements
	 * @return the first configElement in the array, or null if the array is
	 *         empty.
	 */
	public static IConfigurationElement validateSingleContribution(
			IConfigurationElement[] configElements, String logAttribute) {

		int arraySize = configElements.length;
		if (arraySize == 0)
			// No one contributed to extension. return null.
			return null;

		// we should only have one, so use first one.
		IConfigurationElement configElement = configElements[0];
		if (Log.logInfo) {
			String msg = "Loading " + getLogString(configElement, logAttribute); //$NON-NLS-1$
			Log.info(msg);
		}

		if (arraySize != 1) {
			// we have more than one, warn in the log.
			for (int i = 1; i < arraySize; i++)
				// log each extra extension.
				Log.warning(getLogString(configElements[i], logAttribute)
						+ " ignored due to multiple contributions"); //$NON-NLS-1$
		}
		return configElement;
	}

	/**
	 * Utility method to return a string to display in .log. If logAttribute is
	 * not null, its value is also printed.
	 */
	public static String getLogString(IConfigurationElement element,
			String logAttribute) {
		StringBuilder buffer = new StringBuilder(element.getName());
		buffer.append(" element"); //$NON-NLS-1$
		if (logAttribute != null) {
			buffer.append(" with "); //$NON-NLS-1$
			buffer.append(logAttribute);
			buffer.append("=\""); //$NON-NLS-1$
			buffer.append(element.getAttribute(logAttribute));
		}
		buffer.append("\" in extension: "); //$NON-NLS-1$
		buffer.append(element.getDeclaringExtension()
			.getExtensionPointUniqueIdentifier());
		buffer.append(" in Bundle: "); //$NON-NLS-1$
		buffer.append(element.getContributor().getName());


		return buffer.toString();
	}

	/**
	 * Utility method to return a string to display in .log. If logAttribute is
	 * not null, its value is also printed.
	 */
	public static String getLogString(Bundle bundle, Element element,
			String logAttribute) {
		StringBuilder buffer = new StringBuilder(element.getNodeName());
		buffer.append(" element"); //$NON-NLS-1$
		if (logAttribute != null) {
			buffer.append(" with "); //$NON-NLS-1$
			buffer.append(logAttribute);
			buffer.append("=\""); //$NON-NLS-1$
			buffer.append(element.getAttribute(logAttribute));
		}
		buffer.append("\" from xml file in Bundle:"); //$NON-NLS-1$
		buffer.append(bundle.getSymbolicName());


		return buffer.toString();
	}



	/**
	 * Util class for creating class instances from plugins.
	 *
	 * @param pluginId
	 * @param className
	 * @return
	 */
	public static Object createClassInstance(String pluginId, String className) {
		// quick exits.
		if (pluginId == null || className == null)
			return null;
		Bundle bundle = Platform.getBundle(pluginId);
		if (!BundleUtil.bundleHasValidState(bundle))
			return null;

		Class<?> aClass;
		Object aObject;
		try {
			aClass = bundle.loadClass(className);
			aObject = aClass.getDeclaredConstructor().newInstance();
			return aObject;
		} catch (Exception e) {
			Log.error("Intro Could not instantiate: " + className + " in " //$NON-NLS-1$ //$NON-NLS-2$
					+ pluginId, e);
			return null;
		}
	}



	/**
	 * Creates a key for the given element. Returns null if any id is null along
	 * the path.
	 *
	 * @param element
	 * @return
	 */
	public static StringBuilder createPathToElementKey(
			AbstractIntroIdElement element, boolean full) {
		if (element.getId() == null)
			return null;
		StringBuilder buffer = new StringBuilder(element.getId());
		AbstractBaseIntroElement parent = (AbstractBaseIntroElement) element
			.getParent();
		while (parent != null
				&& !parent.isOfType(AbstractIntroElement.MODEL_ROOT)) {
			if (parent.getId() == null)
				return null;
			buffer.insert(0, parent.getId() + "."); //$NON-NLS-1$
			parent = (AbstractBaseIntroElement) parent.getParent();
		}
		return buffer;
	}
}
