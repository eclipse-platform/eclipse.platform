/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.model.mapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.model.*;

/**
 * The model provider for our example
 */
public class ExampleModelProvider extends
		org.eclipse.core.resources.mapping.ModelProvider {

	public static final String ID = "org.eclipse.team.examples.filesystem.modelProvider";

	public ExampleModelProvider() {
		super();
	}
	
	public IStatus validateChange(IResourceDelta delta, IProgressMonitor monitor) {
		// Visit the changes in the delta to look for changes we care about
		final List problems = new ArrayList();
		try {
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();
					if (ModelObjectElementFile.isMoeFile(resource)) {
						// Removal may leave a stale reference in a MOD file
						if (delta.getKind() == IResourceDelta.REMOVED) {
							IStatus status = new ModelStatus(IStatus.ERROR, FileSystemPlugin.ID, getDescriptor().getId(), 
									NLS.bind("Deleting file {0} may corrupt any model definition that references it.", resource.getFullPath()));
							problems.add(status);
						}
					}
					if (ModelObjectDefinitionFile.isModFile(resource)) {
						// Removal may leave unreferenced MOE files around
						if (delta.getKind() == IResourceDelta.REMOVED) {
							IStatus status = new ModelStatus(IStatus.WARNING, FileSystemPlugin.ID, getDescriptor().getId(), 
									NLS.bind("Deleting file {0} may result in unreferenced element files.", resource.getFullPath()));
							problems.add(status);
						}
						if (delta.getKind() == IResourceDelta.ADDED 
								&& ((delta.getFlags() & IResourceDelta.COPIED_FROM) > 0)) {
							// Copying will result in two MOD files that reference the same elements
							IStatus status = new ModelStatus(IStatus.ERROR, FileSystemPlugin.ID, getDescriptor().getId(), 
									NLS.bind("Copying file {0} may corrupt the model defintion.", delta.getMovedFromPath()));
							problems.add(status);
						}
					}
					return delta.getResource().getType() == IResource.ROOT 
						|| ModelProject.isModProject(delta.getResource().getProject());
				}
			});
		} catch (CoreException e) {
			FileSystemPlugin.log(e);
		}
		if (problems.size() == 1)
			return (IStatus)problems.get(0);
		else if (problems.size() > 1) {
			return new MultiStatus(FileSystemPlugin.ID, 0, (IStatus[]) problems.toArray(new IStatus[problems.size()]), "Multiple potential side effects have been found.",  null);
		}
		return super.validateChange(delta, monitor);
	}
	
	public ResourceMapping[] getMappings(IResource resource, ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
		if (ModelProject.isModProject(resource.getProject())) {
			ModelObject object = ModelObject.create(resource);
			if (object != null)
				return new ResourceMapping[] { object.getAdapter(ResourceMapping.class) };
		}
		return super.getMappings(resource, context, monitor);
	}

}
