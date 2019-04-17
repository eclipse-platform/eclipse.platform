/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.operations;

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A specialized Replace operation that will update managed resources and
 * unmanaged resources that are conflicting additions (so that the remote is fetched)
 */
public class OverrideAndUpdateOperation extends ReplaceOperation {

	private IResource[] conflictingAdditions;
	private final IProject project;

	public OverrideAndUpdateOperation(IWorkbenchPart part, IProject project, IResource[] allResources, IResource[] conflictingAdditions, CVSTag tag, boolean recurse) {
		super(part, allResources, tag, recurse);
		this.project = project;
		this.conflictingAdditions = conflictingAdditions;
	}

	@Override
	protected ICVSResource[] getResourcesToUpdate(ICVSResource[] resources, IProgressMonitor monitor) throws CVSException {
		// Add the conflicting additions to the list of resources to update
		Set<ICVSResource> update = new HashSet<>();
		ICVSResource[] conflicts = getCVSArguments(conflictingAdditions);
		update.addAll(Arrays.asList(conflicts));
		update.addAll(Arrays.asList(super.getResourcesToUpdate(resources, monitor)));
		return update.toArray(new ICVSResource[update.size()]);
	}
	
	@Override
	protected ResourceMappingContext getResourceMappingContext() {
		return new SingleProjectSubscriberContext(CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber(), false, project);
	}
	
	@Override
	protected SynchronizationScopeManager createScopeManager(boolean consultModels) {
		return new SingleProjectScopeManager(getJobName(), getSelectedMappings(), getResourceMappingContext(), consultModels, project);
	}

}
