/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.mapping.ModelStatus;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.model.ModelObject;
import org.eclipse.team.examples.model.ModelObjectDefinitionFile;
import org.eclipse.team.examples.model.ModelObjectElementFile;
import org.eclipse.team.examples.model.ModelProject;

/**
 * The model provider for our example
 */
public class ExampleModelProvider extends
org.eclipse.core.resources.mapping.ModelProvider {

	public static final String ID = "org.eclipse.team.examples.filesystem.modelProvider";

	public ExampleModelProvider() {
		super();
	}

	@Override
	public IStatus validateChange(IResourceDelta delta, IProgressMonitor monitor) {
		// Visit the changes in the delta to look for changes we care about
		final List<IStatus> problems = new ArrayList<>();
		try {
			delta.accept(delta1 -> {
				IResource resource = delta1.getResource();
				if (ModelObjectElementFile.isMoeFile(resource)) {
					// Removal may leave a stale reference in a MOD file
					if (delta1.getKind() == IResourceDelta.REMOVED) {
						IStatus status1 = new ModelStatus(IStatus.ERROR, FileSystemPlugin.ID, getDescriptor().getId(),
								NLS.bind("Deleting file {0} may corrupt any model definition that references it.", resource.getFullPath()));
						problems.add(status1);
					}
				}
				if (ModelObjectDefinitionFile.isModFile(resource)) {
					// Removal may leave unreferenced MOE files around
					if (delta1.getKind() == IResourceDelta.REMOVED) {
						IStatus status2 = new ModelStatus(IStatus.WARNING, FileSystemPlugin.ID, getDescriptor().getId(),
								NLS.bind("Deleting file {0} may result in unreferenced element files.", resource.getFullPath()));
						problems.add(status2);
					}
					if (delta1.getKind() == IResourceDelta.ADDED
							&& ((delta1.getFlags() & IResourceDelta.COPIED_FROM) > 0)) {
						// Copying will result in two MOD files that reference the same elements
						IStatus status3 = new ModelStatus(IStatus.ERROR, FileSystemPlugin.ID, getDescriptor().getId(),
								NLS.bind("Copying file {0} may corrupt the model defintion.", delta1.getMovedFromPath()));
						problems.add(status3);
					}
				}
				return delta1.getResource().getType() == IResource.ROOT
						|| ModelProject.isModProject(delta1.getResource().getProject());
			});
		} catch (CoreException e) {
			FileSystemPlugin.log(e);
		}
		if (problems.size() == 1)
			return problems.get(0);
		else if (problems.size() > 1) {
			return new MultiStatus(FileSystemPlugin.ID, 0, problems.toArray(new IStatus[problems.size()]), "Multiple potential side effects have been found.",  null);
		}
		return super.validateChange(delta, monitor);
	}

	@Override
	public ResourceMapping[] getMappings(IResource resource, ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
		if (ModelProject.isModProject(resource.getProject())) {
			ModelObject object = ModelObject.create(resource);
			if (object != null)
				return new ResourceMapping[] { object.getAdapter(ResourceMapping.class) };
		}
		return super.getMappings(resource, context, monitor);
	}

}
