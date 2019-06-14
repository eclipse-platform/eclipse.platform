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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.team.core.mapping.ISynchronizationScope;

/**
 * This scope wraps another scope and treats the input mappings of
 * that wrapped scope as the mappings of this scope.
 */
public class ResourceMappingInputScope extends AbstractResourceMappingScope {

	ISynchronizationScope wrappedScope;

	public ResourceMappingInputScope(ISynchronizationScope wrappedScope) {

		this.wrappedScope = wrappedScope;
	}

	@Override
	public ResourceMapping[] getInputMappings() {
		return wrappedScope.getInputMappings();
	}

	@Override
	public ResourceMapping[] getMappings() {
		return getInputMappings();
	}

	@Override
	public ResourceTraversal[] getTraversals() {
		CompoundResourceTraversal result = new CompoundResourceTraversal();
		ResourceMapping[] mappings = getMappings();
		for (ResourceMapping mapping : mappings) {
			ResourceTraversal[] traversals = getTraversals(mapping);
			result.addTraversals(traversals);
		}
		return result.asTraversals();
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMapping mapping) {
		if (!contains(mapping)) {
			return null;
		}
		return wrappedScope.getTraversals(mapping);
	}

	private boolean contains(ResourceMapping mapping) {
		ResourceMapping[] mappings = getMappings();
		for (ResourceMapping child : mappings) {
			if (child.equals(mapping)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasAdditionalMappings() {
		return false;
	}

	@Override
	public boolean hasAdditonalResources() {
		return false;
	}

	@Override
	public ISynchronizationScope asInputScope() {
		return this;
	}

	@Override
	public IProject[] getProjects() {
		return wrappedScope.getProjects();
	}

	@Override
	public ResourceMappingContext getContext() {
		return wrappedScope.getContext();
	}

	@Override
	public void refresh(ResourceMapping[] mappings) {
		wrappedScope.refresh(mappings);
	}
}
