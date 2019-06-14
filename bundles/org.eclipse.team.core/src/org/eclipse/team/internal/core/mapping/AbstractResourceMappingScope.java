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
package org.eclipse.team.internal.core.mapping;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.team.internal.core.subscribers.AbstractSynchronizationScope;

/**
 * Class that contains common resource mapping scope code.
 */
public abstract class AbstractResourceMappingScope extends AbstractSynchronizationScope {

	@Override
	public ResourceMapping getMapping(Object modelObject) {
		ResourceMapping[] mappings = getMappings();
		for (ResourceMapping mapping : mappings) {
			if (mapping.getModelObject().equals(modelObject))
				return mapping;
		}
		return null;
	}

	@Override
	public ResourceMapping[] getMappings(String id) {
		Set<ResourceMapping> result = new HashSet<>();
		ResourceMapping[] mappings = getMappings();
		for (ResourceMapping mapping : mappings) {
			if (mapping.getModelProviderId().equals(id)) {
				result.add(mapping);
			}
		}
		return result.toArray(new ResourceMapping[result.size()]);

	}

	@Override
	public ResourceTraversal[] getTraversals(String modelProviderId) {
		ResourceMapping[] mappings = getMappings(modelProviderId);
		CompoundResourceTraversal traversal = new CompoundResourceTraversal();
		for (ResourceMapping mapping : mappings) {
			ResourceTraversal[] traversals = getTraversals(mapping);
			if (traversals != null)
				traversal.addTraversals(traversals);
		}
		return traversal.asTraversals();
	}

	@Override
	public ModelProvider[] getModelProviders() {
		Set<ModelProvider> result = new HashSet<>();
		ResourceMapping[] mappings = getMappings();
		for (ResourceMapping mapping : mappings) {
			ModelProvider modelProvider = mapping.getModelProvider();
			if (modelProvider != null)
				result.add(modelProvider);
		}
		return result.toArray(new ModelProvider[result.size()]);
	}

}
