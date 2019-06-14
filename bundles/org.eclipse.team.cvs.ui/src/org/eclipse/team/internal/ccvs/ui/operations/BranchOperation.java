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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.actions.CVSAction;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;
import org.eclipse.team.internal.ccvs.ui.tags.BranchPromptDialog;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * Perform a CVS branch operaiton
 */
public class BranchOperation extends RepositoryProviderOperation {
	
	private boolean update;
	private CVSTag rootVersionTag;
	private CVSTag branchTag;
	
	public BranchOperation(IWorkbenchPart part, ResourceMapping[] mappers) {
		super(part, mappers);
	}
	
	public void setTags(CVSTag rootVersionTag, CVSTag branchTag, boolean updateToBranch) {
		this.rootVersionTag = rootVersionTag;
		this.branchTag = branchTag;
		this.update = updateToBranch;
	}
	
	@Override
	protected boolean shouldRun() {
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, monitor -> {
				try {
					buildScope(monitor);
				} catch (CVSException e) {
					throw new InvocationTargetException(e);
				}
			});
		} catch (InvocationTargetException e1) {
			CVSUIPlugin.openError(getShell(), null, null, e1);
		} catch (InterruptedException e1) {
			throw new OperationCanceledException();
		}
		
		IResource[] resources = getTraversalRoots();
		boolean allSticky = areAllResourcesSticky(resources);
		String initialVersionName = calculateInitialVersionName(resources,allSticky);
		final BranchPromptDialog dialog = new BranchPromptDialog(getShell(),
											CVSUIMessages.BranchWizard_title, 
											resources, 
											allSticky, 
											initialVersionName);
		if (dialog.open() != Window.OK) return false;		
		
		// Capture the dialog info in local variables
		final String tagString = dialog.getBranchTagName();
		update = dialog.getUpdate();
		branchTag = new CVSTag(tagString, CVSTag.BRANCH);
		
		// Only set the root version tag if the name from the dialog differs from the initial name
		String versionString = dialog.getVersionTagName();
		if (versionString != null 
				&& (initialVersionName == null || !versionString.equals(initialVersionName))) {
			rootVersionTag = new CVSTag(versionString, CVSTag.VERSION);
		}
								
		// For non-projects determine if the tag being loaded is the same as the resource's parent
		// If it's not, warn the user that they will be mixing tags
		if (update) {
			try {
				if(!CVSAction.checkForMixingTags(getShell(), resources, branchTag)) {
					return false;
				}
			} catch (CVSException e) {
				CVSUIPlugin.log(e);
			}
		}
		return super.shouldRun();
	}

	@Override
	protected void execute(CVSTeamProvider provider, IResource[] providerResources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
		try {
			monitor.beginTask(null, 100);
			makeBranch(provider, providerResources, rootVersionTag, branchTag, update, recurse, Policy.subMonitorFor(monitor, 90));										
			updateRememberedTags(providerResources);
			if (update) {
				updateWorkspaceSubscriber(provider, getCVSArguments(providerResources), recurse, Policy.subMonitorFor(monitor, 10));
			}
			collectStatus(Status.OK_STATUS);
		} catch (TeamException e) {
			// Accumulate the status which will be displayed by CVSAction#endOperation(IAction)
			collectStatus(e.getStatus());
		} finally {
			monitor.done();
		}
	}

	private void makeBranch(CVSTeamProvider provider, IResource[] resources, final CVSTag versionTag, final CVSTag branchTag, boolean moveToBranch, boolean recurse, IProgressMonitor monitor) throws TeamException {
		
		// Determine the total amount of work
		int totalWork = (versionTag!= null ? 60 : 40) + (moveToBranch ? 20 : 0);
		monitor.beginTask(CVSUIMessages.CVSTeamProvider_makeBranch, totalWork);  
		try {
			// Build the arguments list
			ICVSResource[] arguments = getCVSArguments(resources);
			LocalOption[] localOptions = getLocalOptions(recurse);
			
			// Tag the remote resources
			IStatus status = null;
			if (versionTag != null) {
				// Version using a custom tag command that skips added but not commited reesources
				Session session = new Session(getRemoteLocation(provider), getLocalRoot(provider), true /* output to console */);
				session.open(Policy.subMonitorFor(monitor, 5), true /* open for modification */);
				try {
					status = Command.CUSTOM_TAG.execute(
						session,
						Command.NO_GLOBAL_OPTIONS,
						localOptions,
						versionTag,
						arguments,
						null,
						Policy.subMonitorFor(monitor, 35));
				} finally {
					session.close();
				}
				if (status.isOK()) {
					// Branch using the tag
					session = new Session(getRemoteLocation(provider), getLocalRoot(provider), true /* output to console */);
					session.open(Policy.subMonitorFor(monitor, 5), true /* open for modification */);
					try {
						status = Command.CUSTOM_TAG.execute(
							session,
							Command.NO_GLOBAL_OPTIONS,
							localOptions,
							branchTag,
							arguments,
							null,
						Policy.subMonitorFor(monitor, 15));
					} finally {
						session.close();
					}
				}
			} else {
				// Just branch using tag
				Session session = new Session(getRemoteLocation(provider), getLocalRoot(provider), true /* output to console */);
				session.open(Policy.subMonitorFor(monitor, 5), true /* open for modification */);
				try {
					status = Command.CUSTOM_TAG.execute(
						session,
						Command.NO_GLOBAL_OPTIONS,
						localOptions,
						branchTag,
						arguments,
						null,
						Policy.subMonitorFor(monitor, 35));
				} finally {
					session.close();
				}

			}
			if ( ! status.isOK()) {
				throw new CVSServerException(status);
			}
			
			// Set the tag of the local resources to the branch tag (The update command will not
			// properly update "cvs added" and "cvs removed" resources so a custom visitor is used
			if (moveToBranch) {
				setTag(provider, resources, branchTag, recurse, Policy.subMonitorFor(monitor, 20));
			}
		} finally {
			monitor.done();
		}
	}
	
	/*
	 * This method sets the tag for a project.
	 * It expects to be passed an InfiniteSubProgressMonitor
	 */
	private void setTag(final CVSTeamProvider provider, final IResource[] resources, final CVSTag tag, final boolean recurse, IProgressMonitor monitor) throws TeamException {
		getLocalRoot(provider).run(progress -> {
			try {
				// 512 ticks gives us a maximum of 2048 which seems reasonable for folders and
				// files in a project
				progress.beginTask(null, 100);
				final IProgressMonitor monitor1 = Policy.infiniteSubMonitorFor(progress, 100);
				monitor1.beginTask(NLS.bind(CVSUIMessages.CVSTeamProvider_folderInfo,
						new String[] { provider.getProject().getName() }), 512);

				// Visit all the children folders in order to set the root in the folder sync
				// info
				for (IResource resource : resources) {
					CVSWorkspaceRoot.getCVSResourceFor(resource).accept(new ICVSResourceVisitor() {
						@Override
						public void visitFile(ICVSFile file) throws CVSException {
							monitor1.worked(1);
							// ResourceSyncInfo info = file.getSyncInfo();
							byte[] syncBytes = file.getSyncBytes();
							if (syncBytes != null) {
								monitor1.subTask(NLS.bind(CVSUIMessages.CVSTeamProvider_updatingFile,
										new String[] { file.getName() }));
								file.setSyncBytes(ResourceSyncInfo.setTag(syncBytes, tag), ICVSFile.UNKNOWN);
							}
						}

						@Override
						public void visitFolder(ICVSFolder folder) throws CVSException {
							monitor1.worked(1);
							FolderSyncInfo info = folder.getFolderSyncInfo();
							if (info != null) {
								monitor1.subTask(NLS.bind(CVSUIMessages.CVSTeamProvider_updatingFolder,
										new String[] { info.getRepository() }));
								MutableFolderSyncInfo newInfo = info.cloneMutable();
								newInfo.setTag(tag);
								folder.setFolderSyncInfo(newInfo);
							}
						}
					}, recurse);
				}
			} finally {
				progress.done();
			}
		}, monitor);
	}
	
	private void updateRememberedTags(IResource[] providerResources) throws CVSException {
		if (rootVersionTag != null || update) {
			for (IResource providerResource : providerResources) {
				ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(providerResource);
				RepositoryManager manager = CVSUIPlugin.getPlugin().getRepositoryManager();
				if (rootVersionTag != null) {
					manager.addTags(cvsResource, new CVSTag[] { rootVersionTag });
				}
				if (update) {
					manager.addTags(cvsResource, new CVSTag[] { branchTag });
				}
			}
		}
	}
	
	@Override
	protected String getTaskName() {
		return CVSUIMessages.BranchOperation_0; 
	}
	
	@Override
	protected String getTaskName(CVSTeamProvider provider) {
		return NLS.bind(CVSUIMessages.BranchOperation_1, new String[] { provider.getProject().getName() }); 
	}
	
	/**
	 * Answers <code>true</code> if all resources in the array have a sticky tag
	 */
	private boolean areAllResourcesSticky(IResource[] resources) {
		for (IResource resource : resources) {
			if (!hasStickyTag(resource)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Answers <code>true</code> if the resource has a sticky tag
	 */
	private boolean hasStickyTag(IResource resource) {
		try {
			ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);			
			CVSTag tag;
			if(cvsResource.isFolder()) {
				FolderSyncInfo folderInfo = ((ICVSFolder)cvsResource).getFolderSyncInfo();
				tag = folderInfo.getTag();
			} else {
				ResourceSyncInfo info = cvsResource.getSyncInfo();
				tag = info.getTag();
			}
			if(tag!=null) {
				int tagType = tag.getType();
				if(tagType==CVSTag.VERSION) {
					return true;
				}
			}
		} catch(CVSException e) {
			CVSUIPlugin.log(e);
			return false;
		}
		return false;
	}
	
	private String calculateInitialVersionName(IResource[] resources, boolean allSticky) {
		String versionName = "";		 //$NON-NLS-1$
		try {
			if(allSticky) {
				IResource stickyResource = resources[0];									
				if(stickyResource.getType()==IResource.FILE) {
					ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor((IFile)stickyResource);
					versionName = cvsFile.getSyncInfo().getTag().getName();
				} else {
					ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor((IContainer)stickyResource);
					versionName = cvsFolder.getFolderSyncInfo().getTag().getName();
				}
			}
		} catch(CVSException e) {
			CVSUIPlugin.log(e);
			versionName = ""; //$NON-NLS-1$
		}
		return versionName;
	}
	
	@Override
	protected boolean isReportableError(IStatus status) {
		return super.isReportableError(status)
				|| status.getCode() == CVSStatus.TAG_ALREADY_EXISTS;
	}
}
