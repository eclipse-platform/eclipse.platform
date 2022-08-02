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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;

/**
 * Concrete implementation of the {@link ISynchronizationScope} interface for
 * use by clients.
 *
 * @see org.eclipse.core.resources.mapping.ResourceMapping
 *
 * @since 3.2
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ResourceMappingScope extends AbstractResourceMappingScope {

	private ResourceMapping[] inputMappings;
	private final Map<ResourceMapping, ResourceTraversal[]> mappingsToTraversals = Collections.synchronizedMap(new HashMap<>());
	private boolean hasAdditionalMappings;
	private boolean hasAdditionalResources;
	private final CompoundResourceTraversal compoundTraversal = new CompoundResourceTraversal();
	private final SynchronizationScopeManager manager;

	public ResourceMappingScope(ResourceMapping[] selectedMappings, SynchronizationScopeManager manager) {
		inputMappings = selectedMappings;
		this.manager = manager;
	}

	/**
	 * Add the mapping and its traversals to the scope. This method should
	 * only be invoked during the scope building process.
	 * @param mapping the mapping being added to the scope
	 * @param traversals the traversals for that mapping
	 * @return the added traversals
	 */
	public ResourceTraversal[] addMapping(ResourceMapping mapping, ResourceTraversal[] traversals) {
		ResourceTraversal[] newTraversals = compoundTraversal.getUncoveredTraversals(traversals);
		mappingsToTraversals.put(mapping, traversals);
		compoundTraversal.addTraversals(traversals);
		return newTraversals;
	}

	@Override
	public ResourceMapping[] getInputMappings() {
		return inputMappings;
	}

	@Override
	public ResourceMapping[] getMappings() {
		if (mappingsToTraversals.isEmpty())
			return inputMappings;
		return mappingsToTraversals.keySet().toArray(new ResourceMapping[mappingsToTraversals.size()]);
	}

	@Override
	public ResourceTraversal[] getTraversals() {
		return compoundTraversal.asTraversals();
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMapping mapping) {
		return mappingsToTraversals.get(mapping);
	}

	@Override
	public boolean hasAdditionalMappings() {
		return hasAdditionalMappings;
	}

	/**
	 * Set whether the scope has additional mappings to the input mappings.
	 * This method should only be invoked during the scope building process.
	 * @param hasAdditionalMappings whether the scope has additional mappings
	 */
	public void setHasAdditionalMappings(boolean hasAdditionalMappings) {
		this.hasAdditionalMappings = hasAdditionalMappings;
	}

	/**
	 * Set whether this scope has additional resources.
	 * This method should only be invoked during the scope building process.
	 * @param hasAdditionalResources whether the scope has additional resources
	 */
	public void setHasAdditionalResources(boolean hasAdditionalResources) {
		this.hasAdditionalResources = hasAdditionalResources;
	}

	@Override
	public boolean hasAdditonalResources() {
		return hasAdditionalResources;
	}

	public CompoundResourceTraversal getCompoundTraversal() {
		return compoundTraversal;
	}

	@Override
	public ISynchronizationScope asInputScope() {
		return new ResourceMappingInputScope(this);
	}

	@Override
	public IProject[] getProjects() {
		ResourceMappingContext context = getContext();
		if (context instanceof RemoteResourceMappingContext) {
			RemoteResourceMappingContext rrmc = (RemoteResourceMappingContext) context;
			return rrmc.getProjects();
		}
		return ResourcesPlugin.getWorkspace().getRoot().getProjects();
	}

	@Override
	public ResourceMappingContext getContext() {
		return manager.getContext();
	}

	@Override
	public void refresh(ResourceMapping[] mappings) {
		manager.refresh(mappings);
	}

	public void reset() {
		mappingsToTraversals.clear();
		compoundTraversal.clear();
		hasAdditionalMappings = false;
		hasAdditionalResources = false;
	}
}
