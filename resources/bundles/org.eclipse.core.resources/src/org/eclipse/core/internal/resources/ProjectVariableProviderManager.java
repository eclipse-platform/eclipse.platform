/*******************************************************************************
 * Copyright (c) 2008, 2015 Freescale Semiconductor and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Freescale Semiconductor - initial API and implementation
 *     IBM Corporation - ongoing development
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.variableresolvers.PathVariableResolver;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

/**
 * Repository for all variable providers available through the extension points.
 * @since 3.6
 */
public class ProjectVariableProviderManager {

	public static class Descriptor {
		private final PathVariableResolver provider;
		private final String name;
		private final String value;

		public Descriptor(IExtension extension, IConfigurationElement element) throws RuntimeException, CoreException {
			name = element.getAttribute("variable"); //$NON-NLS-1$
			value = element.getAttribute("value"); //$NON-NLS-1$
			PathVariableResolver p = null;
			try {
				String classAttribute = "class"; //$NON-NLS-1$
				if (element.getAttribute(classAttribute) != null)
					p = (PathVariableResolver) element.createExecutableExtension(classAttribute);
			} catch (CoreException e) {
				Policy.log(e);
			}
			provider = p;
			if (name == null)
				fail(NLS.bind(Messages.mapping_invalidDef, extension.getUniqueIdentifier()));
		}

		protected void fail(String reason) throws CoreException {
			throw new ResourceException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, 1, reason, null));
		}

		public String getName() {
			return name;
		}

		public String getValue(String variable, IResource resource) {
			if (value != null)
				return value;
			return provider.getValue(variable, resource);
		}

		public String[] getVariableNames(String variable, IResource resource) {
			if (provider != null)
				return provider.getVariableNames(variable, resource);
			if (name.equals(variable))
				return new String[] {variable};
			return null;
		}
	}

	private static final Map<String, Descriptor> descriptors = getDescriptorMap();
	private static final Descriptor[] descriptorsArray = descriptors.values().toArray(Descriptor[]::new);
	private static final ProjectVariableProviderManager instance = new ProjectVariableProviderManager();

	public static ProjectVariableProviderManager getDefault() {
		return instance;
	}

	public Descriptor[] getDescriptors() {
		return descriptorsArray;
	}

	private static Map<String, Descriptor> getDescriptorMap() {
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_VARIABLE_PROVIDERS);
		IExtension[] extensions = point.getExtensions();
		Map<String, Descriptor> d = new HashMap<>(extensions.length * 2 + 1);
		for (IExtension extension : extensions) {
			IConfigurationElement[] elements = extension.getConfigurationElements();
			for (IConfigurationElement element : elements) {
				String elementName = element.getName();
				if (elementName.equalsIgnoreCase("variableResolver")) { //$NON-NLS-1$
					Descriptor desc = null;
					try {
						desc = new Descriptor(extension, element);
					} catch (CoreException e) {
						Policy.log(e);
					}
					if (desc != null)
						d.put(desc.getName(), desc);
				}
			}
		}
		return Map.copyOf(d);
	}

	public Descriptor findDescriptor(String name) {
		Descriptor result = descriptors.get(name);
		return result;
	}
}
