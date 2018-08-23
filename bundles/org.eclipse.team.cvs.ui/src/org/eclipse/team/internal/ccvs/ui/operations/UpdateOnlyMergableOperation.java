/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This operation performs an update that will only affect files
 * that have no conflicts or automergable conflicts.
 */
public class UpdateOnlyMergableOperation extends SingleCommandOperation {

	List skippedFiles = new ArrayList();
	private final IProject project;
	
	public UpdateOnlyMergableOperation(IWorkbenchPart part, IProject project, IResource[] resources, LocalOption[] localOptions) {
		super(part, asResourceMappers(resources), localOptions);
		this.project = project;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.SingleCommandOperation#executeCommand(org.eclipse.team.internal.ccvs.core.client.Session, org.eclipse.team.internal.ccvs.core.CVSTeamProvider, org.eclipse.core.resources.IResource[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus executeCommand(Session session, CVSTeamProvider provider, ICVSResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
		UpdateMergableOnly update = new UpdateMergableOnly();
		IStatus status = update.execute(
			session,
			Command.NO_GLOBAL_OPTIONS, 
			getLocalOptions(recurse), 
			resources,
			null, 
			monitor);
		if (status.getSeverity() != IStatus.ERROR) {
			addSkippedFiles(update.getSkippedFiles());
			return OK;
		} 
		return status;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getTaskName()
	 */
	protected String getTaskName() {
		return CVSUIMessages.UpdateOnlyMergeable_taskName; 
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getTaskName(org.eclipse.team.internal.ccvs.core.CVSTeamProvider)
	 */
	protected String getTaskName(CVSTeamProvider provider) {
		return NLS.bind(CVSUIMessages.UpdateOperation_0, new String[] { provider.getProject().getName() }); 
	}
	
	protected void addSkippedFiles(IFile[] files) {
		skippedFiles.addAll(Arrays.asList(files));
	}
	
	public IFile[] getSkippedFiles() {
		return (IFile[]) skippedFiles.toArray(new IFile[skippedFiles.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.UpdateOperation#getResourceMappingContext()
	 */
	protected ResourceMappingContext getResourceMappingContext() {
		return new SingleProjectSubscriberContext(CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber(), false, project);
	}
	
	protected SynchronizationScopeManager createScopeManager(boolean consultModels) {
		return new SingleProjectScopeManager(getJobName(), getSelectedMappings(), getResourceMappingContext(), consultModels, project);
	}
}
