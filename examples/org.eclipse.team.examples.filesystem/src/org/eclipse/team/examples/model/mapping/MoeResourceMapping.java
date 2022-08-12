/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.model.mapping;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.examples.model.ModelObjectElementFile;
import org.eclipse.team.examples.model.ModelResource;

public class MoeResourceMapping extends ModelResourceMapping {

	public MoeResourceMapping(ModelObjectElementFile file) {
		super(file);
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) {
		return new ResourceTraversal[] {
				new ResourceTraversal(new IResource[] {
						getResource()
				}, IResource.DEPTH_ZERO, IResource.NONE)
			};
	}

	private IResource getResource() {
		return ((ModelResource)getModelObject()).getResource();
	}

	@Override
	public boolean contains(ResourceMapping mapping) {
		if (mapping.equals(this))
			return true;
		return false;
	}

}
