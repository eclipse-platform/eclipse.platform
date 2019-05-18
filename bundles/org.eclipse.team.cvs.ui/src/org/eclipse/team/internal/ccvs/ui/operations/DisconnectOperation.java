/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Disconnect the given projects from CVS control
 */
public class DisconnectOperation extends RepositoryProviderOperation {

	private boolean unmanage;

	public DisconnectOperation(IWorkbenchPart part, IProject[] projects, boolean unmanage) {
		super(part, projects);
		this.unmanage = unmanage;
	}

	@Override
	protected void execute(
		CVSTeamProvider provider,
		IResource[] resources,
		boolean recurse, IProgressMonitor monitor)
		throws CVSException, InterruptedException {
		
		// This method will be invoked for each provider being disconnected
		monitor.beginTask(null, IProgressMonitor.UNKNOWN);
		IProject project = provider.getProject();
		try {
			RepositoryProvider.unmap(project);
		} catch (TeamException e) {
			throw CVSException.wrapException(e);
		}
		if (unmanage) {
			ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(project);
			cvsFolder.unmanage(monitor);
			EclipseSynchronizer.getInstance().deconfigure(project, Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN));
		}
		monitor.done();
	}

	@Override
	protected String getTaskName() {
		return CVSUIMessages.DisconnectOperation_0; 
	}

	@Override
	protected String getTaskName(CVSTeamProvider provider) {
		return NLS.bind(CVSUIMessages.DisconnectOperation_1, new String[] { provider.getProject().getName() }); 
	}
	
	@Override
	public boolean canRunAsJob() {
		// Do not run in the background
		return false;
	}
	
	@Override
	public boolean consultModelsForMappings() {
		return false;
	}
	
	@Override
	protected ISchedulingRule getSchedulingRule(CVSTeamProvider provider) {
		return ResourcesPlugin.getWorkspace().getRuleFactory().modifyRule(provider.getProject());
	}

}
