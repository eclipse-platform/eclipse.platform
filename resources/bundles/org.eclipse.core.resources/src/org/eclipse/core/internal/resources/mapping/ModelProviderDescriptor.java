/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources.mapping;

import java.util.*;
import org.eclipse.core.expressions.*;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

public class ModelProviderDescriptor implements IModelProviderDescriptor {

	private String id;
	private String[] extendedModels;
	private String label;
	private ModelProvider provider;
	private Expression enablementRule;

	private static EvaluationContext createEvaluationContext(Object element) {
		EvaluationContext result = new EvaluationContext(null, element);
		return result;
	}

	public ModelProviderDescriptor(IExtension extension) throws CoreException {
		readExtension(extension);
	}

	private boolean convert(EvaluationResult eval) {
		if (eval == EvaluationResult.FALSE)
			return false;
		return true;
	}

	protected void fail(String reason) throws CoreException {
		throw new ResourceException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, 1, reason, null));
	}

	@Override
	public String[] getExtendedModels() {
		return extendedModels;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public IResource[] getMatchingResources(IResource[] resources) throws CoreException {
		Set<IResource> result = new HashSet<>();
		for (IResource resource : resources) {
			EvaluationContext evalContext = createEvaluationContext(resource);
			if (matches(evalContext)) {
				result.add(resource);
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	@Override
	public synchronized ModelProvider getModelProvider() throws CoreException {
		if (provider == null) {
			IExtension extension = Platform.getExtensionRegistry().getExtension(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_MODEL_PROVIDERS, id);
			IConfigurationElement[] elements = extension.getConfigurationElements();
			for (IConfigurationElement element : elements) {
				if (element.getName().equalsIgnoreCase("modelProvider")) { //$NON-NLS-1$
					try {
						provider = (ModelProvider) element.createExecutableExtension("class"); //$NON-NLS-1$
						provider.init(this);
					} catch (ClassCastException e) {
						String message = NLS.bind(Messages.mapping_wrongType, id);
						throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, Platform.PLUGIN_ERROR, message, e));
					}
				}
			}
		}
		return provider;
	}

	public boolean matches(IEvaluationContext context) throws CoreException {
		if (enablementRule == null)
			return false;
		return convert(enablementRule.evaluate(context));
	}

	/**
	 * Initialize this descriptor based on the provided extension point.
	 */
	protected void readExtension(IExtension extension) throws CoreException {
		//read the extension
		id = extension.getUniqueIdentifier();
		if (id == null)
			fail(Messages.mapping_noIdentifier);
		label = extension.getLabel();
		IConfigurationElement[] elements = extension.getConfigurationElements();
		int count = elements.length;
		ArrayList<String> extendsList = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			IConfigurationElement element = elements[i];
			String name = element.getName();
			if (name.equalsIgnoreCase("extends-model")) { //$NON-NLS-1$
				String attribute = element.getAttribute("id"); //$NON-NLS-1$
				if (attribute == null)
					fail(NLS.bind(Messages.mapping_invalidDef, id));
				extendsList.add(attribute);
			} else if (name.equalsIgnoreCase(ExpressionTagNames.ENABLEMENT)) {
				enablementRule = ExpressionConverter.getDefault().perform(element);
			}
		}
		extendedModels = extendsList.toArray(new String[extendsList.size()]);
	}

	@Override
	public ResourceTraversal[] getMatchingTraversals(ResourceTraversal[] traversals) throws CoreException {
		List<ResourceTraversal> result = new ArrayList<>();
		for (ResourceTraversal traversal : traversals) {
			if (getMatchingResources(traversal.getResources()).length > 0) {
				result.add(traversal);
			}
		}
		return result.toArray(new ResourceTraversal[result.size()]);
	}

}
