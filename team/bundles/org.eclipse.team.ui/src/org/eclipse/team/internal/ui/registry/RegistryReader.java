/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.registry;

import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils.Sorter;
import org.osgi.framework.Bundle;

public abstract class RegistryReader {
	protected static final String TAG_DESCRIPTION = "description"; //$NON-NLS-1$
	protected static Hashtable<String, IExtension[]> extensionPoints = new Hashtable<>();

	/**
	 * Creates an extension.  If the extension plugin has not
	 * been loaded a busy cursor will be activated during the duration of
	 * the load.
	 *
	 * @param element the configuration element defining the extension
	 * @param classAttribute the name of the attribute carrying the class
	 * @return the extension object
	 * @throws CoreException if the extension cannot be created
	 */
	public static Object createExtension(final IConfigurationElement element,
			final String classAttribute) throws CoreException {
		try {
			// If plugin has been loaded create extension.
			// Otherwise, show busy cursor then create extension.
			if (isActivated(element.getDeclaringExtension()
					.getContributor().getName())) {
				return element.createExecutableExtension(classAttribute);
			}
			final Object[] ret = new Object[1];
			final CoreException[] exc = new CoreException[1];
			BusyIndicator.showWhile(null, () -> {
				try {
					ret[0] = element
							.createExecutableExtension(classAttribute);
				} catch (CoreException e) {
					exc[0] = e;
				}
			});
			if (exc[0] != null) {
				throw exc[0];
			}
			return ret[0];

		} catch (CoreException core) {
			throw core;
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, TeamUIPlugin.ID,
					IStatus.ERROR, NLS.bind(TeamUIMessages.RegistryReader_0, element.getNamespaceIdentifier(), element.getName()),e));
		}
	}

	private static boolean isActivated(String bundleId) {
		return isActivated(Platform.getBundle(bundleId));
	}

	private static boolean isActivated(Bundle bundle) {
		return bundle != null && (bundle.getState() & (Bundle.ACTIVE | Bundle.STOPPING)) != 0;
	}

	/**
	 * The constructor.
	 */
	protected RegistryReader() {
	}
	/**
	 * This method extracts description as a sub-element of the given element.
	 *
	 * @return description string if defined, or empty string if not.
	 */
	protected String getDescription(IConfigurationElement config) {
		IConfigurationElement[] children = config.getChildren(TAG_DESCRIPTION);
		if (children.length >= 1) {
			return children[0].getValue();
		}
		return ""; //$NON-NLS-1$
	}
	/**
	 * Logs the error in the workbench log using the provided text and the
	 * information in the configuration element.
	 */
	protected void logError(IConfigurationElement element, String text) {
		IExtension extension = element.getDeclaringExtension();
		StringBuilder buf = new StringBuilder();
		buf.append("Plugin " + extension.getContributor().getName() + ", extension " + extension.getExtensionPointUniqueIdentifier()); //$NON-NLS-2$//$NON-NLS-1$
		buf.append("\n" + text); //$NON-NLS-1$
		TeamUIPlugin.log(IStatus.ERROR, buf.toString(), null);
	}
	/**
	 * Logs a very common registry error when a required attribute is missing.
	 */
	protected void logMissingAttribute(IConfigurationElement element, String attributeName) {
		logError(element, "Required attribute '" + attributeName + "' not defined"); //$NON-NLS-2$//$NON-NLS-1$
	}

	/**
	 * Logs a very common registry error when a required child is missing.
	 */
	protected void logMissingElement(IConfigurationElement element, String elementName) {
		logError(element, "Required sub element '" + elementName + "' not defined"); //$NON-NLS-2$//$NON-NLS-1$
	}

	/**
	 * Logs a registry error when the configuration element is unknown.
	 */
	protected void logUnknownElement(IConfigurationElement element) {
		logError(element, "Unknown extension tag found: " + element.getName()); //$NON-NLS-1$
	}
	/**
	 * Apply a reproducible order to the list of extensions provided, such that
	 * the order will not change as extensions are added or removed.
	 */
	protected IExtension[] orderExtensions(IExtension[] extensions) {
		// By default, the order is based on plugin id sorted
		// in ascending order. The order for a plugin providing
		// more than one extension for an extension point is
		// dependent in the order listed in the XML file.
		Sorter sorter = new Sorter() {
			@Override
			public boolean compare(Object extension1, Object extension2) {
				String s1 = ((IExtension) extension1).getContributor().getName();
				String s2 = ((IExtension) extension2).getContributor().getName();
				//Return true if elementTwo is 'greater than' elementOne
				return s2.compareToIgnoreCase(s1) > 0;
			}
		};

		Object[] sorted = sorter.sort(extensions);
		IExtension[] sortedExtension = new IExtension[sorted.length];
		System.arraycopy(sorted, 0, sortedExtension, 0, sorted.length);
		return sortedExtension;
	}
	/**
	 * Implement this method to read element's attributes. If children should
	 * also be read, then implementor is responsible for calling <code>readElementChildren</code>.
	 * Implementor is also responsible for logging missing attributes.
	 *
	 * @return true if element was recognized, false if not.
	 */
	protected abstract boolean readElement(IConfigurationElement element);
	/**
	 * Read the element's children. This is called by the subclass' readElement
	 * method when it wants to read the children of the element.
	 */
	protected void readElementChildren(IConfigurationElement element) {
		readElements(element.getChildren());
	}
	/**
	 * Read each element one at a time by calling the subclass implementation
	 * of <code>readElement</code>.
	 *
	 * Logs an error if the element was not recognized.
	 */
	protected void readElements(IConfigurationElement[] elements) {
		for (IConfigurationElement element : elements) {
			if (!readElement(element)) {
				logUnknownElement(element);
			}
		}
	}
	/**
	 * Read one extension by looping through its configuration elements.
	 */
	protected void readExtension(IExtension extension) {
		readElements(extension.getConfigurationElements());
	}
	/**
	 * Start the registry reading process using the supplied plugin ID and
	 * extension point.
	 * @param registry the registry
	 * @param pluginId the plug-in id
	 * @param extensionPoint the extension point
	 */
	public void readRegistry(IExtensionRegistry registry, String pluginId, String extensionPoint) {
		String pointId = pluginId + "-" + extensionPoint; //$NON-NLS-1$
		IExtension[] extensions = extensionPoints.get(pointId);
		if (extensions == null) {
			IExtensionPoint point = registry.getExtensionPoint(pluginId, extensionPoint);
			if (point == null)
				return;
			extensions = point.getExtensions();
			extensionPoints.put(pointId, extensions);
		}
		for (IExtension extension : extensions) {
			readExtension(extension);
		}
	}
}
