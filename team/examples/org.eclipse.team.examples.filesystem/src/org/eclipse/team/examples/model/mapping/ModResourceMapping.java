/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.model.ModelObject;
import org.eclipse.team.examples.model.ModelObjectDefinitionFile;
import org.eclipse.team.examples.model.ModelObjectElementFile;
import org.eclipse.team.examples.model.ModelResource;

public class ModResourceMapping extends ModelResourceMapping {

	public ModResourceMapping(ModelObjectDefinitionFile file) {
		super(file);
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		Set<IResource> resources = getLocalResources();
		if (context instanceof RemoteResourceMappingContext) {
			monitor.beginTask(null, IProgressMonitor.UNKNOWN);
			RemoteResourceMappingContext remoteContext = (RemoteResourceMappingContext) context;
			if (remoteContext.hasRemoteChange(getResource(), SubMonitor.convert(monitor, IProgressMonitor.UNKNOWN))) {
				IResource[] remoteResources = ModelObjectDefinitionFile.getReferencedResources(
						getResource().getProject().getName(),
						remoteContext.fetchRemoteContents((IFile)getResource(),
								SubMonitor.convert(monitor, IProgressMonitor.UNKNOWN)));
				Collections.addAll(resources, remoteResources);
			}
			if (remoteContext.isThreeWay()
					&& remoteContext.hasLocalChange(getResource(), SubMonitor.convert(monitor, IProgressMonitor.UNKNOWN))) {
				IResource[] remoteResources = ModelObjectDefinitionFile.getReferencedResources(
						getResource().getProject().getName(),
						remoteContext.fetchBaseContents((IFile)getResource(),
								SubMonitor.convert(monitor, IProgressMonitor.UNKNOWN)));
				Collections.addAll(resources, remoteResources);
			}
			monitor.done();
		}
		return new ResourceTraversal[] {
				new ResourceTraversal(resources.toArray(new IResource[resources.size()]),
						IResource.DEPTH_ZERO, IResource.NONE)
		};
	}

	private IResource getResource() {
		return ((ModelResource)getModelObject()).getResource();
	}

	private Set<IResource> getLocalResources() throws CoreException {
		ModelObjectDefinitionFile mdf = (ModelObjectDefinitionFile)getModelObject();
		Set<IResource> resources = new HashSet<>();
		resources.add(mdf.getResource());
		ModelObjectElementFile[] files = mdf.getModelObjectElementFiles();
		for (ModelObjectElementFile file : files) {
			resources.add(file.getResource());
		}
		return resources;
	}

	@Override
	public boolean contains(ResourceMapping mapping) {
		if (mapping instanceof ModelResourceMapping) {
			ModelObject object = (ModelObject)mapping.getModelObject();
			if (object instanceof ModelResource) {
				IResource resource = ((ModelResource) object).getResource();
				try {
					return getLocalResources().contains(resource);
				} catch (CoreException e) {
					FileSystemPlugin.log(e);
				}
			}
		}
		return false;
	}

}
