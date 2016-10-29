/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.mapping;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.*;
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

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IResourceMappingScope#getInputMappings()
	 */
	public ResourceMapping[] getInputMappings() {
		return wrappedScope.getInputMappings();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IResourceMappingScope#getMappings()
	 */
	public ResourceMapping[] getMappings() {
		return getInputMappings();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IResourceMappingScope#getTraversals()
	 */
	public ResourceTraversal[] getTraversals() {
		CompoundResourceTraversal result = new CompoundResourceTraversal();
		ResourceMapping[] mappings = getMappings();
		for (int i = 0; i < mappings.length; i++) {
			ResourceMapping mapping = mappings[i];
			ResourceTraversal[] traversals = getTraversals(mapping);
			result.addTraversals(traversals);
		}
		return result.asTraversals();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IResourceMappingScope#getTraversals(org.eclipse.core.resources.mapping.ResourceMapping)
	 */
	public ResourceTraversal[] getTraversals(ResourceMapping mapping) {
		if (!contains(mapping)) {
			return null;
		}
		return wrappedScope.getTraversals(mapping);
	}

	private boolean contains(ResourceMapping mapping) {
		ResourceMapping[] mappings = getMappings();
		for (int i = 0; i < mappings.length; i++) {
			ResourceMapping child = mappings[i];
			if (child.equals(mapping)) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IResourceMappingScope#hasAdditionalMappings()
	 */
	public boolean hasAdditionalMappings() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.IResourceMappingScope#hasAdditonalResources()
	 */
	public boolean hasAdditonalResources() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.IResourceMappingScope#asInputScope()
	 */
	public ISynchronizationScope asInputScope() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.ISynchronizationScope#getProjects()
	 */
	public IProject[] getProjects() {
		return wrappedScope.getProjects();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.ISynchronizationScope#getContext()
	 */
	public ResourceMappingContext getContext() {
		return wrappedScope.getContext();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.ISynchronizationScope#refresh(org.eclipse.core.resources.mapping.ResourceMapping[])
	 */
	public void refresh(ResourceMapping[] mappings) {
		wrappedScope.refresh(mappings);
	}
}
