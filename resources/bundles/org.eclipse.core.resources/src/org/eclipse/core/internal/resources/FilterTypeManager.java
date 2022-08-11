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
 *     Serge Beauchamp (Freescale Semiconductor) - initial API and implementation
 *     IBM Corporation - ongoing development
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.HashMap;
import org.eclipse.core.resources.IFilterMatcherDescriptor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.filtermatchers.AbstractFileInfoMatcher;
import org.eclipse.core.runtime.*;

/**
 *  This class collects all the registered {@link AbstractFileInfoMatcher} instances along
 *  with their properties.
 */
class FilterTypeManager implements IManager {

	private static final String FILTER_ELEMENT = "filterMatcher"; //$NON-NLS-1$

	private HashMap<String, IFilterMatcherDescriptor> factories = new HashMap<>();

	public FilterTypeManager() {
		IExtensionPoint point = RegistryFactory.getRegistry().getExtensionPoint(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_FILTER_MATCHERS);
		if (point != null) {
			// initial population
			for (IExtension extension : point.getExtensions()) {
				processExtension(extension);
			}
			RegistryFactory.getRegistry().addListener(new IRegistryEventListener() {
				@Override
				public void added(IExtension[] extensions) {
					for (IExtension extension : extensions)
						processExtension(extension);
				}

				@Override
				public void added(IExtensionPoint[] extensionPoints) {
					// nothing to do
				}

				@Override
				public void removed(IExtension[] extensions) {
					for (IExtension extension : extensions)
						processRemovedExtension(extension);
				}

				@Override
				public void removed(IExtensionPoint[] extensionPoints) {
					// nothing to do
				}
			});
		}
	}

	public IFilterMatcherDescriptor getFilterDescriptor(String id) {
		return factories.get(id);
	}

	public IFilterMatcherDescriptor[] getFilterDescriptors() {
		return factories.values().toArray(new IFilterMatcherDescriptor[0]);
	}

	protected void processExtension(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (IConfigurationElement element : elements) {
			if (element.getName().equalsIgnoreCase(FILTER_ELEMENT)) {
				IFilterMatcherDescriptor desc = new FilterDescriptor(element);
				factories.put(desc.getId(), desc);
			}
		}
	}

	protected void processRemovedExtension(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (IConfigurationElement element : elements) {
			if (element.getName().equalsIgnoreCase(FILTER_ELEMENT)) {
				IFilterMatcherDescriptor desc = new FilterDescriptor(element, false);
				factories.remove(desc.getId());
			}
		}
	}

	@Override
	public void shutdown(IProgressMonitor monitor) {
		//nothing to do
	}

	@Override
	public void startup(IProgressMonitor monitor) {
		//nothing to do
	}
}
