/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.model.mapping;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.model.*;

public class ModResourceMapping extends ModelResourceMapping {

	public ModResourceMapping(ModelObjectDefinitionFile file) {
		super(file);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.ResourceMapping#getTraversals(org.eclipse.core.resources.mapping.ResourceMappingContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		Set resources = getLocalResources();
		if (context instanceof RemoteResourceMappingContext) {
			monitor.beginTask(null, IProgressMonitor.UNKNOWN);
			RemoteResourceMappingContext remoteContext = (RemoteResourceMappingContext) context;
			if (remoteContext.hasRemoteChange(getResource(), SubMonitor.convert(monitor, IProgressMonitor.UNKNOWN))) {
				IResource[] remoteResources = ModelObjectDefinitionFile.getReferencedResources(
						getResource().getProject().getName(), 
						remoteContext.fetchRemoteContents((IFile)getResource(), 
								SubMonitor.convert(monitor, IProgressMonitor.UNKNOWN)));
				for (int i = 0; i < remoteResources.length; i++) {
					IResource resource = remoteResources[i];
					resources.add(resource);
				}
			}
			if (remoteContext.isThreeWay() 
					&& remoteContext.hasLocalChange(getResource(), SubMonitor.convert(monitor, IProgressMonitor.UNKNOWN))) {
				IResource[] remoteResources = ModelObjectDefinitionFile.getReferencedResources(
						getResource().getProject().getName(),
						remoteContext.fetchBaseContents((IFile)getResource(), 
								SubMonitor.convert(monitor, IProgressMonitor.UNKNOWN)));
				for (int i = 0; i < remoteResources.length; i++) {
					IResource resource = remoteResources[i];
					resources.add(resource);
				}
			}
			monitor.done();
		}
		return new ResourceTraversal[] { 
				new ResourceTraversal((IResource[]) resources.toArray(new IResource[resources.size()]), 
						IResource.DEPTH_ZERO, IResource.NONE)
			};
	}

	private IResource getResource() {
		return ((ModelResource)getModelObject()).getResource();
	}
	
	private Set getLocalResources() throws CoreException {
		ModelObjectDefinitionFile mdf = (ModelObjectDefinitionFile)getModelObject();
		Set resources = new HashSet();
		resources.add(mdf.getResource());
		ModelObjectElementFile[] files = mdf.getModelObjectElementFiles();
		for (int i = 0; i < files.length; i++) {
			ModelObjectElementFile file = files[i];
			resources.add(file.getResource());
		}
		return resources;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.ResourceMapping#contains(org.eclipse.core.resources.mapping.ResourceMapping)
	 */
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
