/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.team.core.*;
import org.eclipse.team.core.mapping.IChangeGroupingRequestor;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;


/**
 * This class represents the CVS Provider's capabilities in the absence of a
 * particular project.
 */

public class CVSTeamProviderType extends RepositoryProviderType implements IAdaptable {
	
	private static AutoShareJob autoShareJob;
	
	public static class AutoShareJob extends Job {

		List projectsToShare = new ArrayList();
		
		AutoShareJob() {
			super(CVSMessages.CVSTeamProviderType_0);
		}

		public boolean isQueueEmpty() {
			return projectsToShare.isEmpty();
		}

		@Override
		public boolean shouldSchedule() {
			return !isQueueEmpty();
		}
		
		@Override
		public boolean shouldRun() {
			synchronized (projectsToShare) {
				for (Iterator iter = projectsToShare.iterator(); iter.hasNext();) {
					IProject project = (IProject) iter.next();
					if (RepositoryProvider.isShared(project)) {
						iter.remove();
					}
				}
				return !projectsToShare.isEmpty();
			}
		}
		
		public void share(IProject project) {
			if (!RepositoryProvider.isShared(project)) {
				synchronized (projectsToShare) {
					if (!projectsToShare.contains(project))
						projectsToShare.add(project);
				}
				if(getState() == Job.NONE && !isQueueEmpty())
					schedule();
			}
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			IProject next = null;
			next = getNextProject();
			monitor.beginTask(null, IProgressMonitor.UNKNOWN);
			while (next != null) {
				autoconnectCVSProject(next, Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN));
				next = getNextProject();
			}
			monitor.done();
			return Status.OK_STATUS;
		}

		private IProject getNextProject() {
			IProject next = null;
			synchronized (projectsToShare) {
				if (!projectsToShare.isEmpty()) {
					next = (IProject)projectsToShare.remove(0);
				}
			}
			return next;
		}
		
		/*
		 * Auto-connect to the repository using CVS/ directories
		 */
		private void autoconnectCVSProject(IProject project, IProgressMonitor monitor) {
			try {
				ICVSFolder folder = (ICVSFolder)CVSWorkspaceRoot.getCVSResourceFor(project);
				FolderSyncInfo info = folder.getFolderSyncInfo();
				if (info != null) {	
					// Set the sharing
					CVSWorkspaceRoot.setSharing(project, info, monitor);
				}
			} catch (TeamException e) {
				CVSProviderPlugin.log(IStatus.ERROR, "Could not auto-share project " + project.getName(), e); //$NON-NLS-1$
			}
		}
	}
	
	private synchronized static AutoShareJob getAutoShareJob() {
		if (autoShareJob == null) {
			autoShareJob = new AutoShareJob();
			autoShareJob.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					// Reschedule the job if it has unprocessed projects
					if (!autoShareJob.isQueueEmpty()) {
						autoShareJob.schedule();
					}
				}
			});
			autoShareJob.setSystem(true);
			autoShareJob.setPriority(Job.SHORT);
			// Must run with the workspace rule to ensure that projects added while we're running
			// can be shared
			autoShareJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
		}
		return autoShareJob;
	}
	
	/**
	 * @see org.eclipse.team.core.RepositoryProviderType#supportsProjectSetImportRelocation()
	 */
	public boolean supportsProjectSetImportRelocation() {
		return false;
	}

	@Override
	public ProjectSetCapability getProjectSetCapability() {
		return new CVSProjectSetCapability();
	}
	
	@Override
	public void metaFilesDetected(IProject project, IContainer[] containers) {
		for (IContainer container : containers) {
			IContainer cvsDir = null;
			if (container.getName().equals("CVS")) { //$NON-NLS-1$
				cvsDir = container;
			} else {
				IResource resource = container.findMember("CVS"); //$NON-NLS-1$
				if (resource.getType() != IResource.FILE) {
					cvsDir = (IContainer)resource;
				}
			}
			try {
				if (cvsDir != null && !cvsDir.isTeamPrivateMember())
					cvsDir.setTeamPrivateMember(true);
			} catch (CoreException e) {
				TeamPlugin.log(IStatus.ERROR, "Could not flag meta-files as team-private for " + cvsDir.getFullPath(), e); //$NON-NLS-1$
			}
		}
		if (CVSProviderPlugin.getPlugin().isAutoshareOnImport())
			getAutoShareJob().share(project);
	}
	
	@Override
	public Subscriber getSubscriber() {
		return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ActiveChangeSetManager.class || adapter == IChangeGroupingRequestor.class)
			return adapter.cast(CVSProviderPlugin.getPlugin().getChangeSetManager());
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
}
