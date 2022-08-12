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
package org.eclipse.core.resources.mapping;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;

/**
 * A resource mapping that obtains the traversals for its model object
 * from a set of child mappings.
 * <p>
 * This class is not intended to be subclasses by clients.
 *
 * @since 3.2
 */
public final class CompositeResourceMapping extends ResourceMapping {
	private final ResourceMapping[] mappings;
	private final Object modelObject;
	private IProject[] projects;
	private String providerId;

	/**
	 * Create a composite mapping that obtains its traversals from a set of sub-mappings.
	 * @param modelObject the model object for this mapping
	 * @param mappings the sub-mappings from which the traversals are obtained
	 */
	public CompositeResourceMapping(String providerId, Object modelObject, ResourceMapping[] mappings) {
		this.modelObject = modelObject;
		this.mappings = mappings;
		this.providerId = providerId;
	}

	@Override
	public boolean contains(ResourceMapping mapping) {
		for (ResourceMapping childMapping : mappings) {
			if (childMapping.contains(mapping)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the resource mappings contained in this composite.
	 * @return Return the resource mappings contained in this composite.
	 */
	public ResourceMapping[] getMappings() {
		return mappings;
	}

	@Override
	public Object getModelObject() {
		return modelObject;
	}

	@Override
	public String getModelProviderId() {
		return providerId;
	}

	@Override
	public IProject[] getProjects() {
		if (projects == null) {
			Set<IProject> result = new HashSet<>();
			for (ResourceMapping mapping : mappings) {
				result.addAll(Arrays.asList(mapping.getProjects()));
			}
			projects = result.toArray(new IProject[result.size()]);
		}
		return projects;
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, mappings.length);
		List<ResourceTraversal> result = new ArrayList<>();
		for (ResourceMapping mapping : mappings) {
			Collections.addAll(result, mapping.getTraversals(context, subMonitor.newChild(1)));
		}
		return result.toArray(new ResourceTraversal[result.size()]);
	}

}
