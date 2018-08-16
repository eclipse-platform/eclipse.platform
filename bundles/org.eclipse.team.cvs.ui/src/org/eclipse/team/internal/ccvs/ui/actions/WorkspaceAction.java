/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.internal.ui.dialogs.IPromptCondition;
import org.eclipse.team.internal.ui.dialogs.PromptingDialog;

/**
 * This class represents an action performed on a local CVS workspace
 */
public abstract class WorkspaceAction extends CVSAction {

	protected CVSTag resourceCommonTag = null;
	
	public interface IProviderAction {
		public IStatus execute(CVSTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws CVSException;
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#beginExecution(IAction)
	 */
	protected boolean beginExecution(IAction action) throws TeamException {
		if (super.beginExecution(action)) {
			// Ensure that the required sync info is loaded
			if (requiresLocalSyncInfo()) {
				// There is a possibility of the selection containing an orphaned subtree.
				// If it does, they will be purged and enablement rechecked before the
				// operation is performed.
				handleOrphanedSubtrees();
				// Check enablement just in case the sync info wasn't loaded
				if (!isEnabled()) {
					MessageDialog.openInformation(getShell(), CVSUIMessages.CVSAction_disabledTitle, CVSUIMessages.CVSAction_disabledMessage); //
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/*
	 * Determine if any of the selected resources are deascendants of
	 * an orphaned CVS subtree and if they are, purge the CVS folders.
	 */
	private boolean handleOrphanedSubtrees() {
		// invoke the inherited method so that overlaps are maintained
		IResource[] resources = getSelectedResources();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			handleOrphanedSubtree(resource);
		}
		return false;
	}

	/*
	 * Determine if the resource is a descendant of an orphaned subtree.
	 * If it is, purge the CVS folders of the subtree.
	 */
	private void handleOrphanedSubtree(IResource resource) {
		try {
			if (!CVSWorkspaceRoot.isSharedWithCVS(resource)) return ;
			ICVSFolder folder;
			if (resource.getType() == IResource.FILE) {
				folder = CVSWorkspaceRoot.getCVSFolderFor(resource.getParent());
			} else {
				folder = CVSWorkspaceRoot.getCVSFolderFor((IContainer)resource);
			}
			handleOrphanedSubtree(folder);
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
		}
	}
	
	/*
	 * Recursively check for and handle orphaned CVS folders
	 */
	private void handleOrphanedSubtree(final ICVSFolder folder) throws CVSException {
		if (folder.getIResource().getType() == IResource.PROJECT) return;
		if (CVSWorkspaceRoot.isOrphanedSubtree((IContainer)folder.getIResource())) {
			try {
				run(new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							folder.unmanage(null);
						} catch (CVSException e) {
							CVSProviderPlugin.log(e);
						}
					}
				}, true, PROGRESS_BUSYCURSOR);
			} catch (InvocationTargetException e) {
				// Ignore this since we logged the one we care about above
			} catch (InterruptedException e) {
				throw new OperationCanceledException();
			}
		}
		handleOrphanedSubtree(folder.getParent());
	}
	
	/**
	 * Return true if the sync info is loaded for all selected resources.
	 * The purpose of this method is to allow enablement code to be as fast
	 * as possible. If the sync info is not loaded, the menu should be enabled
	 * and, if choosen, the action will verify that it is indeed enabled before
	 * performing the associated operation
	 */
	protected boolean isSyncInfoLoaded(IResource[] resources) throws CVSException {
		return EclipseSynchronizer.getInstance().isSyncInfoLoaded(resources, getEnablementDepth());
	}

	/**
	 * Returns the resource depth of the action for use in determining if the required
	 * sync info is loaded. The default is IResource.DEPTH_INFINITE. Sunclasses can override
	 * as required.
	 */
	protected int getActionDepth() {
		return IResource.DEPTH_INFINITE;
	}

	/**
	 * Returns the resource depth of the action enablement for use in determining if the required
	 * sync info is loaded. The default is IResource.DEPTH_ZERO. Sunclasses can override
	 * as required.
	 */
	protected int getEnablementDepth() {
		return IResource.DEPTH_ZERO;
	}
	
	/**
	 * Ensure that the sync info for all the provided resources has been loaded.
	 * If an out-of-sync resource is found, prompt to refresh all the projects involved.
	 */
	protected boolean ensureSyncInfoLoaded(IResource[] resources) throws CVSException {
		boolean keepTrying = true;
		while (keepTrying) {
			try {
				EclipseSynchronizer.getInstance().ensureSyncInfoLoaded(resources, getActionDepth());
				keepTrying = false;
			} catch (CVSException e) {
				if (e.getStatus().getCode() == IResourceStatus.OUT_OF_SYNC_LOCAL) {
					// determine the projects of the resources involved
					Set projects = new HashSet();
					for (int i = 0; i < resources.length; i++) {
						IResource resource = resources[i];
						projects.add(resource.getProject());
					}
					// prompt to refresh
					if (promptToRefresh(getShell(), (IResource[]) projects.toArray(new IResource[projects.size()]), e.getStatus())) {
						for (Iterator iter = projects.iterator();iter.hasNext();) {
							IProject project = (IProject) iter.next();
							try {
								project.refreshLocal(IResource.DEPTH_INFINITE, null);
							} catch (CoreException coreException) {
								throw CVSException.wrapException(coreException);
							}
						}
					} else {
						return false;
					}
				} else {
					throw e;
				}
			}
		}
		return true;
	}
	
	/**
	 * Override to ensure that the sync info is available before performing the
	 * real <code>isEnabled()</code> test.
	 * 
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#setActionEnablement(IAction)
	 */
	protected void setActionEnablement(IAction action) {
		try {
			boolean requires = requiresLocalSyncInfo();
			if (!requires || isSyncInfoLoaded(getSelectedResources())) {
				super.setActionEnablement(action);
			} else {
				// If the sync info is not loaded, enable the menu item
				// Performing the action will ensure that the action should really
				// be enabled before anything else is done
				action.setEnabled(true);
			}
		} catch (CVSException e) {
			// We couldn't determine if the sync info was loaded.
			// Enable the action so that performing the action will
			// reveal the error to the user.
			action.setEnabled(true);
		}
	}

	/**
	 * Return true if the action requires the sync info for the selected resources.
	 * If the sync info is required, the real enablement code will only be run if
	 * the sync info is loaded from disc. Otherwise, the action is enabled and
	 * performing the action will load the sync info and verify that the action is truely
	 * enabled before doing anything else.
	 * 
	 * This implementation returns <code>true</code>. Subclasses must override if they do
	 * not require the sync info of the selected resources.
	 * 
	 * @return boolean
	 */
	protected boolean requiresLocalSyncInfo() {
		return true;
	}

	protected boolean promptToRefresh(final Shell shell, final IResource[] resources, final IStatus status) {
		final boolean[] result = new boolean[] { false};
		Runnable runnable = new Runnable() {
			public void run() {
				Shell shellToUse = shell;
				if (shell == null) {
					shellToUse = new Shell(Display.getCurrent());
				}
				String question;
				if (resources.length == 1) {
					question = NLS.bind(CVSUIMessages.CVSAction_refreshQuestion, new String[] { status.getMessage(), resources[0].getFullPath().toString() });
				} else {
					question = NLS.bind(CVSUIMessages.CVSAction_refreshMultipleQuestion, new String[] { status.getMessage() });
				}
				result[0] = MessageDialog.openQuestion(shellToUse, CVSUIMessages.CVSAction_refreshTitle, question);
			}
		};
		Display.getDefault().syncExec(runnable);
		return result[0];
	}

	/**
	 * Most CVS workspace actions modify the workspace and thus should
	 * save dirty editors.
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#needsToSaveDirtyEditors()
	 */
	protected boolean needsToSaveDirtyEditors() {
		return true;
	}

	/**
	 * The action is enabled for the appropriate resources. This method checks
	 * that:
	 * <ol>
	 * <li>there is no overlap between a selected file and folder (overlapping
	 * folders is allowed because of logical vs. physical mapping problem in
	 * views)
	 * <li>the state of the resources match the conditions provided by:
	 * <ul>
	 * <li>isEnabledForIgnoredResources()
	 * <li>isEnabledForManagedResources()
	 * <li>isEnabledForUnManagedResources() (i.e. not ignored and not managed)
	 * </ul>
	 * </ol>
	 * @see TeamAction#isEnabled()
	 */
	public boolean isEnabled() {
		
		// allow the super to decide enablement. if the super doesn't know it will return false.
		boolean enabled = super.isEnabled();
		if(enabled) return true;
		
		// invoke the inherited method so that overlaps are maintained
		IResource[] resources = getSelectedResourcesWithOverlap();
		
		// disable if no resources are selected
		if(resources.length==0) return false;
		
		// disable properly for single resource enablement
		if (!isEnabledForMultipleResources() && resources.length != 1) return false;
		
		// validate enabled for each resource in the selection
		List folderPaths = new ArrayList();
		List filePaths = new ArrayList();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			
			// only enable for accessible resources
			if(resource.getType() == IResource.PROJECT) {
				if (! resource.isAccessible()) return false;
			}
			
			// no CVS actions are enabled if the selection contains a linked resource
			if (CVSWorkspaceRoot.isLinkedResource(resource)) return false;
			
			// only enable for resources in a project shared with CVS
			if(RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId()) == null) {
				return false;
			}
			
			// collect files and folders separately to check for overlap later
			IPath resourceFullPath = resource.getFullPath();
			if(resource.getType() == IResource.FILE) {
				filePaths.add(resourceFullPath);
			} else {
				folderPaths.add(resourceFullPath);
			}
			
			// ensure that resource management state matches what the action requires
			ICVSResource cvsResource = getCVSResourceFor(resource);
			try {
				if (!isEnabledForCVSResource(cvsResource)) {
					return false;
				}
			} catch (CVSException e) {
				if (!isEnabledForException(e))
					return false;
			}
		}
		// Ensure that there is no overlap between files and folders
		// NOTE: folder overlap must be allowed because of logical vs. physical
		if(!folderPaths.isEmpty()) {
			for (Iterator fileIter = filePaths.iterator(); fileIter.hasNext();) {
				IPath resourcePath = (IPath) fileIter.next();
				for (Iterator it = folderPaths.iterator(); it.hasNext();) {
					IPath folderPath = (IPath) it.next();
					if (folderPath.isPrefixOf(resourcePath)) {
						return false;
					}
				}
			}
		}
		return true;
	}

    /**
	 * Method isEnabledForCVSResource.
	 * @param cvsResource
	 * @return boolean
	 */
	protected boolean isEnabledForCVSResource(ICVSResource cvsResource) throws CVSException {
		boolean managed = false;
		boolean ignored = false;
		boolean added = false;
		if (cvsResource.isIgnored()) {
			ignored = true;
		} else if (cvsResource.isFolder()) {
			managed = ((ICVSFolder)cvsResource).isCVSFolder();
		} else {
			ResourceSyncInfo info = cvsResource.getSyncInfo();
			managed = info != null;
			if (managed) added = info.isAdded();
		}
		if (managed && ! isEnabledForManagedResources()) return false;
		if ( ! managed && ! isEnabledForUnmanagedResources()) return false;
		if ( ignored && ! isEnabledForIgnoredResources()) return false;
		if (added && ! isEnabledForAddedResources()) return false;
		if ( ! cvsResource.exists() && ! isEnabledForNonExistantResources()) return false;
		return true;
	}
	
	/**
	 * Method isEnabledForIgnoredResources.
	 * @return boolean
	 */
	protected boolean isEnabledForIgnoredResources() {
		return false;
	}
	
	/**
	 * Method isEnabledForUnmanagedResources.
	 * @return boolean
	 */
	protected boolean isEnabledForUnmanagedResources() {
		return false;
	}
	
	/**
	 * Method isEnabledForManagedResources.
	 * @return boolean
	 */
	protected boolean isEnabledForManagedResources() {
		return true;
	}

	/**
	 * Method isEnabledForAddedResources.
	 * @return boolean
	 */
	protected boolean isEnabledForAddedResources() {
		return true;
	}
	
	/**
	 * Method isEnabledForAddedResources.
	 * @return boolean
	 */
	protected boolean isEnabledForMultipleResources() {
		return true;
	}
	
	/**
	 * Method isEnabledForNonExistantResources.
	 * @return boolean
	 */
	protected boolean isEnabledForNonExistantResources() {
		return false;
	}

	protected void executeProviderAction(IProviderAction action, IResource[] resources, IProgressMonitor monitor) throws InvocationTargetException {
		Hashtable table = getProviderMapping(resources);
		Set keySet = table.keySet();
		monitor.beginTask(null, keySet.size() * 1000);
		Iterator iterator = keySet.iterator();

		while (iterator.hasNext()) {
			IProgressMonitor subMonitor = SubMonitor.convert(monitor, 1000);
			CVSTeamProvider provider = (CVSTeamProvider)iterator.next();
			List list = (List)table.get(provider);
			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
			try {
				addStatus(action.execute(provider, providerResources, subMonitor));
			} catch (CVSException e) {
				throw new InvocationTargetException(e);
			}

		}
	}
	
	protected void executeProviderAction(IProviderAction action, IProgressMonitor monitor) throws InvocationTargetException {
		executeProviderAction(action, getSelectedResources(), monitor);
	}

	/**
	 * Given the current selection this method returns a text label that can
	 * be shown to the user that reflects the tags in the current selection.
	 * These can be used in the <b>Compare With</b> and <b>Replace With</b> actions.
	 */
	protected String calculateActionTagValue() {
		try {
			resourceCommonTag = null;
			IResource[] resources = getSelectedResources();
			CVSTag commonTag = null;
			boolean sameTagType = true;
			boolean multipleSameNames = true;
			
			for (int i = 0; i < resources.length; i++) {
				ICVSResource cvsResource = getCVSResourceFor(resources[i]);
				CVSTag tag = null;
				if(cvsResource.isFolder()) {
					FolderSyncInfo info = ((ICVSFolder)cvsResource).getFolderSyncInfo();
					if(info != null) {
						tag = info.getTag();
					}
					if (tag != null && tag.getType() == CVSTag.BRANCH) {
						tag = Util.getAccurateFolderTag(resources[i], tag);
					}
				} else {
					tag = CVSAction.getAccurateFileTag(cvsResource);
				}
				if(tag == null) {
					tag = new CVSTag();
				}
				if(commonTag == null) {
					commonTag = tag;
				} else if(!commonTag.equals(tag)) {
					if(commonTag.getType() != tag.getType()) {
						sameTagType = false;
					}
					if(!commonTag.getName().equals(tag.getName())) {
						multipleSameNames = false;
					}
				}
			}
			
			// set text to default
			String actionText = CVSUIMessages.ReplaceWithLatestAction_multipleTags;
			if(commonTag != null) {
				int tagType = commonTag.getType();
				String tagName = commonTag.getName();
				if(tagType != CVSTag.HEAD) {
					resourceCommonTag = commonTag;
				}
				// multiple tag names but of the same type
				if(sameTagType && !multipleSameNames) {
					if(tagType == CVSTag.BRANCH) {
						actionText = CVSUIMessages.ReplaceWithLatestAction_multipleBranches; //
					} else {
						actionText = CVSUIMessages.ReplaceWithLatestAction_multipleVersions;
					}
				// same tag names and types
				} else if(sameTagType && multipleSameNames) {
					if(tagType == CVSTag.BRANCH) {
						actionText = NLS.bind(CVSUIMessages.ReplaceWithLatestAction_singleBranch, new String[] { tagName }); //
					} else if(tagType == CVSTag.VERSION){
						actionText = NLS.bind(CVSUIMessages.ReplaceWithLatestAction_singleVersion, new String[] { tagName });
					} else if(tagType == CVSTag.HEAD) {
						actionText = NLS.bind(CVSUIMessages.ReplaceWithLatestAction_singleHEAD, new String[] { tagName });
					}
				}
			}
			
			return actionText;
		} catch (CVSException e) {
			// silently ignore
			return CVSUIMessages.ReplaceWithLatestAction_multipleTags; //
		}
	}

	protected IResource[] checkOverwriteOfDirtyResources(IResource[] resources, IProgressMonitor monitor) throws CVSException, InterruptedException {
		List dirtyResources = new ArrayList();
		IResource[] selectedResources = getSelectedResources();
		
		try {
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(null, selectedResources.length * 100);
			monitor.setTaskName(CVSUIMessages.ReplaceWithAction_calculatingDirtyResources);
			for (int i = 0; i < selectedResources.length; i++) {
				IResource resource = selectedResources[i];
				ICVSResource cvsResource = getCVSResourceFor(resource);
				if(cvsResource.isModified(Policy.subMonitorFor(monitor, 100))) {
					dirtyResources.add(resource);
				}
			}
		} finally {
			monitor.done();
		}
		
		PromptingDialog dialog = new PromptingDialog(getShell(), selectedResources,
				getPromptCondition((IResource[]) dirtyResources.toArray(new IResource[dirtyResources.size()])), CVSUIMessages.ReplaceWithAction_confirmOverwrite);
		return dialog.promptForMultiple();
	}

	/**
	 * This is a helper for the CVS UI automated tests. It allows the tests to ignore prompting dialogs.
	 * @param resources
	 */
	protected IPromptCondition getPromptCondition(IResource[] resources) {
		return getOverwriteLocalChangesPrompt(resources);
	}
}
