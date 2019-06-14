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

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This method checks out one or more remote folders from the same repository
 * into an existing project or folder in the workspace. The target project
 * must either be shared with the same repository or it must not be shared 
 * with any repository
 */
public class CheckoutIntoOperation extends CheckoutOperation {

	private boolean recursive;
	private ICVSFolder localFolder;
	private String localFolderName;

	/**
	 * Constructor which takes a set of remote folders and the local folder into which the folders should be
	 * loaded.
	 */
	public CheckoutIntoOperation(IWorkbenchPart part, ICVSRemoteFolder[] remoteFolders, IContainer localFolder, boolean recursive) {
		super(part, remoteFolders);
		this.recursive = recursive;
		this.localFolder = CVSWorkspaceRoot.getCVSFolderFor(localFolder);
	}

	/**
	 * Constructor which takes a single remote folder and the target local folder.
	 */
	public CheckoutIntoOperation(IWorkbenchPart part, ICVSRemoteFolder remoteFolder, IContainer localFolder, boolean recursive) {
		this(part, new ICVSRemoteFolder[] { remoteFolder }, localFolder.getParent(), recursive);
		this.localFolderName = localFolder.getName();
	}
	
	@Override
	protected String getTaskName() {
		ICVSRemoteFolder[] remoteFolders = getRemoteFolders();
		String localFolderName = ""; //$NON-NLS-1$
		localFolderName = getLocalFolder().getIResource().getFullPath().toString();
		return NLS.bind(CVSUIMessages.CheckoutIntoOperation_taskname, new String[] { Integer.valueOf(remoteFolders.length).toString(), localFolderName });  
	}

	/**
	 * @return
	 */
	public ICVSFolder getLocalFolder() {
		return localFolder;
	}
	
	/**
	 * @return
	 */
	public boolean isRecursive() {
		return recursive;
	}
	
	@Override
	protected IStatus checkout(final ICVSRemoteFolder folder, IProgressMonitor monitor) throws CVSException {
		final IStatus[] result = new IStatus[] { null };
		try {
			// Wrap in a workspace runnable to avoid auto-build.
			// This is necessary because CVS has a POST_BUILD
			// listener that will clear the sync info from
			// orphaned (i.e. unmanaged) subtrees and we will
			// purposely create some which we subsequently manage
			// from their parent (see manageFolders())
			ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable) monitor1 -> result[0] = checkout(folder,
					getLocalFolder(), isRecursive(), monitor1), getSchedulingRule(), 0, monitor);
		} catch (CoreException e) {
			result[0] = CVSException.wrapException(e).getStatus();
		}
		return result[0];
	}
	
	@Override
	protected void checkout(final ICVSRemoteFolder[] folders, IProgressMonitor monitor) throws CVSException {
		// Batch sync info changes with the CVS synchronizer to optimize cache writing
		EclipseSynchronizer.getInstance().run(getSchedulingRule(), monitor1 -> {
			monitor1.beginTask(null, 100);
			try {
				// Obtain a scheduling rule on the projects were about to overwrite
				CheckoutIntoOperation.super.checkout(folders, Policy.subMonitorFor(monitor1, 90));
				refreshRoot(getLocalRoot(getLocalFolder()), Policy.subMonitorFor(monitor1, 10));
			} finally {
				monitor1.done();
			}
		}, Policy.monitorFor(monitor));
	}
	
	/*
	 * Prepare the local folders to receive the remote folders. If localFolderName is not null, then
	 * if will be the only target folder of the checkout. Otherwise, the remote folder
	 * could expand to multiple local folders witinb the given parent folder.
	 */
	private ICVSFolder[] prepareLocalFolders(Session session, ICVSRemoteFolder remoteFolder, ICVSFolder parentFolder, String localFolderName, IProgressMonitor monitor) throws CVSException {
		Set<ICVSFolder> targetFolderSet = new HashSet<>();
		monitor.beginTask(null, 30);
		if (localFolderName == null) {
			
			// Determine which local folders will be afected
			IStatus status = Request.EXPAND_MODULES.execute(session, new String[] { remoteFolder.getRepositoryRelativePath()}, Policy.subMonitorFor(monitor, 10));
			if (status.getCode() == CVSStatus.SERVER_ERROR) {
				addError(status);
				return null;
			}
			
			// Convert the module expansions to target folders
			String[] expansions = session.getModuleExpansions();
			for (String expansion : expansions) {
				String childPath = new Path(null, expansion).segment(0);
				ICVSResource resource = parentFolder.getChild(childPath);
				if (resource != null && !resource.isFolder()) {
					// The target folder conflicts with an existing file
					addError(new CVSStatus(IStatus.ERROR, NLS.bind(CVSUIMessages.CheckoutIntoOperation_targetIsFile, new String[] { remoteFolder.getName(), resource.getIResource().getFullPath().toString() }))); 
					return null;
				}
				targetFolderSet.add(parentFolder.getFolder(childPath));
			}
			
		} else {
			targetFolderSet.add(parentFolder.getFolder(localFolderName));
		}
		
		final ICVSFolder[] targetFolders = targetFolderSet.toArray(new ICVSFolder[targetFolderSet.size()]);
		
		// Ensure that the checkout will not conflict with existing resources
		IStatus status = validateTargetFolders(remoteFolder, targetFolders, Policy.subMonitorFor(monitor, 10));
		if (!status.isOK()) {
			addError(status);
			return null;
		}
		
		// Prepare the target projects to receive resources
		status = scrubFolders(remoteFolder, targetFolders, Policy.subMonitorFor(monitor, 10));
		// return the target projects if the scrub succeeded
		if (status.isOK()) {
			return targetFolders;
		} else {
			addError(status);
			return null;
		}
	}
	
	/*
	 * Ensure that the new folders will not conflict with existing folders (even those that are pruned).
	 */
	private IStatus validateTargetFolders(ICVSRemoteFolder remoteFolder, ICVSFolder[] targetFolders, IProgressMonitor monitor) throws CVSException {
		for (ICVSFolder targetFolder : targetFolders) {
			FolderSyncInfo localInfo = targetFolder.getFolderSyncInfo();
			FolderSyncInfo remoteInfo = remoteFolder.getFolderSyncInfo();
			
			if (!remoteInfo.isSameMapping(localInfo)) {
				if (localInfo != null ) {
					if (isRemoteChildOfParent(targetFolder)) {
						// if the local folder is child of it's parent remotely (i.e. path of child is parent/child)
						// then the remote cannot be loaded.
						String message;
						if (targetFolder.exists()) {
							message = NLS.bind(CVSUIMessages.CheckoutIntoOperation_targetIsFolder, new String[] { remoteFolder.getName(), targetFolder.getIResource().getFullPath().toString() }); 
						} else {
							message = NLS.bind(CVSUIMessages.CheckoutIntoOperation_targetIsPrunedFolder, new String[] { remoteFolder.getName(), targetFolder.getFolderSyncInfo().getRepository() }); 
						}
						return new CVSStatus(IStatus.ERROR, message);
					}
				}
				// Verify that no other folders in the local workspace are mapped to the remote folder
				IStatus status = validateUniqueMapping(remoteFolder, targetFolder, Policy.subMonitorFor(monitor, 10));
				if (!status.isOK()) return status;
			}
		}
		return OK;
	}

	/*
	 * Return true if the given local folder is a direct descendant of it's local parent in 
	 * the repository as well
	 */
	private boolean isRemoteChildOfParent(ICVSFolder targetFolder) throws CVSException {
		FolderSyncInfo localInfo = targetFolder.getFolderSyncInfo();
		if (localInfo == null) return false;
		FolderSyncInfo parentInfo = targetFolder.getParent().getFolderSyncInfo();
		if (parentInfo == null) return false;
		IPath childPath = new Path(null, localInfo.getRepository());
		IPath parentPath = new Path(null, parentInfo.getRepository());
		return parentPath.isPrefixOf(childPath);
	}

	/**
	 * @param targetFolder
	 * @return
	 */
	private IContainer getLocalRoot(ICVSFolder targetFolder) throws CVSException {
		return targetFolder.getIResource().getProject();
	}

	/*
	 * Ensure that there is no equivalent mapping alreay in the local workspace
	 */
	private IStatus validateUniqueMapping(final ICVSRemoteFolder remoteFolder, final ICVSFolder targetFolder, IProgressMonitor iProgressMonitor) throws CVSException {
		
		final IContainer root = getLocalRoot(targetFolder);
		final FolderSyncInfo remoteInfo = remoteFolder.getFolderSyncInfo();
		if (remoteInfo.equals(FolderSyncInfo.VIRTUAL_DIRECTORY)) {
			// We can't really check the mapping ahead of time
			// so we'll let the operation continue
			return OK;
		}
		ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(root);
		try {
			cvsFolder.accept(new ICVSResourceVisitor() {
				@Override
				public void visitFile(ICVSFile file) throws CVSException {
					// do nothing
				}
				@Override
				public void visitFolder(ICVSFolder folder) throws CVSException {
					if (!folder.isCVSFolder()) return;
					IResource resource = folder.getIResource();
					if (resource == null) return;
					FolderSyncInfo info = folder.getFolderSyncInfo();
					if (info.isSameMapping(remoteInfo)) {
						throw new CVSException(NLS.bind(CVSUIMessages.CheckoutIntoOperation_mappingAlreadyExists, (new Object[] {
							remoteFolder.getName(), 
							targetFolder.getIResource().getFullPath().toString(),
							resource.getFullPath().toString()
						})));
					}
					folder.acceptChildren(this);
				}
			});
		} catch (CVSException e) {
			return e.getStatus();
		}
		return OK;
	}
	
	/*
	 * Purge the local contents of the given folders
	 */
	private IStatus scrubFolders(ICVSRemoteFolder remoteFolder, ICVSFolder[] targetFolders, IProgressMonitor monitor) throws CVSException {
		monitor.beginTask(null, 100 * targetFolders.length);
		
		// Prompt first before any work is done
		if (targetFolders.length > 1) {
			setInvolvesMultipleResources(true);
		}
		for (ICVSFolder targetFolder : targetFolders) {
			if (needsPromptForOverwrite(targetFolder, Policy.subMonitorFor(monitor, 50)) && !promptToOverwrite(targetFolder)) {
				return new CVSStatus(IStatus.INFO, NLS.bind(CVSUIMessages.CheckoutIntoOperation_cancelled, new String[] { remoteFolder.getName() })); 
			}
		}
		for (ICVSFolder targetFolder : targetFolders) {
			IStatus status = scrubFolder(targetFolder, Policy.subMonitorFor(monitor, 50));
			if (!status.isOK()) return status;
		}
		monitor.done();
		return OK;
	}

	private boolean needsPromptForOverwrite(ICVSFolder targetFolder, IProgressMonitor monitor) throws CVSException {
		return targetFolder.isModified(monitor);
	}

	private boolean promptToOverwrite(ICVSFolder folder) {
		return promptToOverwrite(
			CVSUIMessages.CheckoutOperation_confirmOverwrite,  
			NLS.bind(CVSUIMessages.CheckoutIntoOperation_overwriteMessage, new String[] { folder.getName() }),
			folder.getIResource()); 
	}
	
	private IStatus scrubFolder(ICVSFolder folder, IProgressMonitor monitor) throws CVSException {
		if (folder.exists() || folder.isCVSFolder()) {
			// Unmanage first so we don't get outgoing deletions
			folder.unmanage(Policy.subMonitorFor(monitor, 50));
			if (folder.exists()) folder.delete();
		}
		return OK;
	}

	/* private */ IStatus checkout(final ICVSRemoteFolder remoteFolder, ICVSFolder parentFolder, boolean recurse, IProgressMonitor monitor) throws CVSException {
		// Open a connection session to the repository
		monitor.beginTask(null, 100);
		ICVSRepositoryLocation repository = remoteFolder.getRepository();
		Session session = new Session(repository, parentFolder);
		try {
			session.open(Policy.subMonitorFor(monitor, 5), false /* read-only */);
			
			// Determine which local folders will be affected
			String localName = localFolderName;
			if (localName == null) {
				IPath path = new Path(null, remoteFolder.getRepositoryRelativePath());
				if (path.segmentCount() > 1) {
					localName = path.lastSegment();
				}
			}
			ICVSFolder[] targetFolders = prepareLocalFolders(session, remoteFolder, parentFolder, localName, Policy.subMonitorFor(monitor, 5));
			if (targetFolders == null) {
				// an error occured and has been added to the operation's error list
				return getLastError();
			}
			
			// Add recurse option
			List<LocalOption> localOptions = new ArrayList<>();
			if (!recurse)
				localOptions.add(Command.DO_NOT_RECURSE);
			if (localName != null) {
				localOptions.add(Checkout.makeDirectoryNameOption(localName));
			}
			
			// Prune empty directories if pruning enabled
			if (CVSProviderPlugin.getPlugin().getPruneEmptyDirectories()) 
				localOptions.add(Command.PRUNE_EMPTY_DIRECTORIES);
			// Add the options related to the CVSTag
			CVSTag tag = remoteFolder.getTag();
			if (tag == null) {
				// A null tag in a remote resource indicates HEAD
				tag = CVSTag.DEFAULT;
			}
			localOptions.add(Update.makeTagOption(tag));
			
			// Perform the checkout
			IStatus status = Command.CHECKOUT.execute(session,
				Command.NO_GLOBAL_OPTIONS,
				localOptions.toArray(new LocalOption[localOptions.size()]),
				new String[] { remoteFolder.getRepositoryRelativePath() },
				null,
				Policy.subMonitorFor(monitor, 80));
			if (!status.isOK()) {
				return status;
			}
			
			manageFolders(targetFolders, repository.getLocation(false));
			
			return OK;
			
		} finally {
			session.close();
		}
	}

	private void manageFolders(ICVSFolder[] targetFolders, String root) throws CVSException {
		for (ICVSFolder targetFolder : targetFolders) {
			manageFolder(targetFolder, root);
		}
	}
	
	private static void manageFolder(ICVSFolder folder, String root) throws CVSException {
		// Ensure that the parent is a CVS folder
		ICVSFolder parent = folder.getParent();
		if (!parent.isCVSFolder()) {
			parent.setFolderSyncInfo(new FolderSyncInfo(FolderSyncInfo.VIRTUAL_DIRECTORY, root, CVSTag.DEFAULT, true));
			IResource resource = parent.getIResource();
			if (resource.getType() != IResource.PROJECT) {
				manageFolder(parent, root);
			}
		}
		// reset the folder sync info so it will be managed by it's parent
		folder.setFolderSyncInfo(folder.getFolderSyncInfo());
	}

	/*
	 * Bring the provided projects into the workspace
	 */
	private static void refreshRoot(IContainer root, IProgressMonitor monitor) throws CVSException {
			try {
				IProject project = root.getProject();
				CVSTeamProvider provider = (CVSTeamProvider)RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId());
				if (provider == null) {
					ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(project);
					if (folder.isCVSFolder()) {
						// Register the project with Team
						RepositoryProvider.map(project, CVSProviderPlugin.getTypeId());
						
						// TODO: This should be somewhere else
						provider = (CVSTeamProvider)RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId());
						provider.setWatchEditEnabled(CVSProviderPlugin.getPlugin().isWatchEditEnabled());
					}
				}
			} catch (TeamException e) {
				throw CVSException.wrapException(e);
			}
	}
	
	@Override
	protected ISchedulingRule getSchedulingRule() {
		//use the modfiy rule for the time being
		//TODO: Just lock the project not the entire workspace (so can't use modifyRule)
		//since the project already exists 
		IProject tempProject = getLocalFolder().getIResource().getProject();
		IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace().getRuleFactory();
		return ruleFactory.modifyRule(tempProject);
	}

}
