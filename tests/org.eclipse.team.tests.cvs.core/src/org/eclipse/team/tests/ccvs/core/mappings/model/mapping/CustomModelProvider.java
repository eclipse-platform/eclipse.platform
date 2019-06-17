/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.mappings.model.mapping;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.tests.ccvs.core.mappings.model.ModelObject;
import org.eclipse.team.tests.ccvs.core.mappings.model.ModelProject;

/**
 * The model provider for our example
 */
public class CustomModelProvider extends
		org.eclipse.core.resources.mapping.ModelProvider {

	public static final String ID = "org.eclipse.team.tests.cvs.core.bug302163_ModelProvider";

	public CustomModelProvider() {
		super();
	}

	@Override
	public IStatus validateChange(IResourceDelta delta, IProgressMonitor monitor) {
		return super.validateChange(delta, monitor);
	}

	@Override
	public ResourceMapping[] getMappings(IResource resource,
			ResourceMappingContext context, IProgressMonitor monitor)
			throws CoreException {
		if (ModelProject.isModProject(resource.getProject())) {
			ModelObject object = ModelObject.create(resource);
			if (object != null)
				return new ResourceMapping[] { ModelResourceMapping
						.create(object) };
		}
		return super.getMappings(resource, context, monitor);
	}
}
