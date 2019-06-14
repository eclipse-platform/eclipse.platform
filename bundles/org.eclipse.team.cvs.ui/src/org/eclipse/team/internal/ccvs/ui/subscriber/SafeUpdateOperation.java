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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.*;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.*;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * This update action will update all mergable resources first and then prompt the
 * user to overwrite any resources that failed the safe update.
 * 
 * Subclasses should determine how the update should handle conflicts by implementing 
 * the getOverwriteLocalChanges() method.
 */
public abstract class SafeUpdateOperation extends CVSSubscriberOperation {

	private boolean promptBeforeUpdate = false;
	
	private SyncInfoSet skipped = new SyncInfoSet();
	
	protected SafeUpdateOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, boolean promptBeforeUpdate) {
		super(configuration, elements);
		this.promptBeforeUpdate = promptBeforeUpdate;
	}
	
	@Override
	public boolean shouldRun() {
		return promptIfNeeded();
	}

	/**
	 * Run the operation for the sync infos from the given project.
	 * 
	 * @param projectSyncInfos the project syncInfos
	 * @param project the project
	 * @param monitor a progress monitor
	 * @throws InvocationTargetException
	 */
	@Override
	protected void run(final Map projectSyncInfos, final IProject project,
			IProgressMonitor monitor) throws InvocationTargetException {
		try {
			IResource[] resources = getIResourcesFrom(((SyncInfoSet) projectSyncInfos
					.get(project)).getSyncInfos());
			ResourceMapping[] selectedMappings = Utils
					.getResourceMappings(resources);
			ResourceMappingContext context = new SingleProjectSubscriberContext(
					CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber(),
					false, project);
			SynchronizationScopeManager manager = new SingleProjectScopeManager(
					getJobName(), selectedMappings, context, true, project);
			manager.initialize(null);

			// Pass the scheduling rule to the synchronizer so that sync change
			// events and cache commits to disk are batched
			EclipseSynchronizer.getInstance().run(getUpdateRule(manager),
					monitor1 -> {
						try {
							runWithProjectRule(project, (SyncInfoSet) projectSyncInfos.get(project), monitor1);
						} catch (TeamException e) {
							throw CVSException.wrapException(e);
						}
					}, Policy.subMonitorFor(monitor, 100));
		} catch (TeamException e) {
			throw new InvocationTargetException(e);
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

	private ISchedulingRule getUpdateRule(SynchronizationScopeManager manager) {
		ISchedulingRule rule = null;
		ResourceMapping[] mappings = manager.getScope().getMappings();
		for (ResourceMapping mapping : mappings) {
			IProject[] mappingProjects = mapping.getProjects();
			for (IProject mappingProject : mappingProjects) {
				if (rule == null) {
					rule = mappingProject;
				} else {
					rule = MultiRule.combine(rule, mappingProject);
				}
			}
		}
		return rule;
	}
	
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		skipped.clear();
		super.run(monitor);
		try {
			handleFailedUpdates(monitor);
		} catch (TeamException e) {
			throw new InvocationTargetException(e);
		}
	}
	
	@Override
	public void runWithProjectRule(IProject project, SyncInfoSet syncSet, IProgressMonitor monitor) throws TeamException {
		try {
			monitor.beginTask(null, 100);
			
			// Remove the cases that are known to fail (adding them to skipped list)
			removeKnownFailureCases(syncSet);
			
			// Run the update on the remaining nodes in the set
			// The update will fail for conflicts that turn out to be non-automergable
			safeUpdate(project, syncSet, Policy.subMonitorFor(monitor, 100));
			
			// Remove all failed conflicts from the original sync set
			syncSet.rejectNodes(new FastSyncInfoFilter() {
				@Override
				public boolean select(SyncInfo info) {
					return skipped.getSyncInfo(info.getLocal()) != null;
				}
			});
			
			// Signal for the ones that were updated
			updated(syncSet.getResources());
		} finally {
			monitor.done();
		}
	}

	/**
	 * @param syncSet
	 * @return
	 */
	private SyncInfoSet removeKnownFailureCases(SyncInfoSet syncSet) {
		// First, remove any known failure cases
		FastSyncInfoFilter failFilter = getKnownFailureCases();
		SyncInfo[] willFail = syncSet.getNodes(failFilter);
		syncSet.rejectNodes(failFilter);
		for (SyncInfo info : willFail) {
			skipped.add(info);
		}
		return syncSet;
	}

	private void handleFailedUpdates(IProgressMonitor monitor) throws TeamException {
		// Handle conflicting files that can't be merged, ask the user what should be done.
		if(! skipped.isEmpty()) {
			if(getOverwriteLocalChanges()) {				
				// Ask the user if a replace should be performed on the remaining nodes
				if(promptForOverwrite(skipped)) {
					overwriteUpdate(skipped, monitor);
					if (!skipped.isEmpty()) {
						updated(skipped.getResources());
					}
				}
			} else {
				// Warn the user that some nodes could not be updated. This can happen if there are
				// files with conflicts that are not auto-mergeable.					
				warnAboutFailedResources(skipped);		
			}
		}
	}
	
	protected boolean getOverwriteLocalChanges(){
		return false;
	}

	/**
	 * Perform a safe update on the resources in the provided set. Any included resources
	 * that cannot be updated safely wil be added to the skippedFiles list.
	 * @param syncSet the set containing the resources to be updated
	 * @param monitor
	 */
	protected void safeUpdate(IProject project, SyncInfoSet syncSet, IProgressMonitor monitor) throws TeamException {
		SyncInfo[] changed = syncSet.getSyncInfos();
		if (changed.length == 0) return;
		
		// The list of sync resources to be updated using "cvs update"
		List<SyncInfo> updateShallow = new ArrayList<>();
		// A list of sync resource folders which need to be created locally 
		// (incoming addition or previously pruned)
		Set<SyncInfo> parentCreationElements = new HashSet<>();
		// A list of sync resources that are incoming deletions.
		// We do these first to avoid case conflicts
		List<SyncInfo> updateDeletions = new ArrayList<>();
	
		for (SyncInfo changedNode : changed) {
			// Make sure that parent folders exist
			SyncInfo parent = getParent(changedNode);
			if (parent != null && isOutOfSync(parent)) {
				// We need to ensure that parents that are either incoming folder additions
				// or previously pruned folders are recreated.
				parentCreationElements.add(parent);
			}
			
			IResource resource = changedNode.getLocal();
			int kind = changedNode.getKind();
			boolean willBeAttempted = false;
			if (resource.getType() == IResource.FILE) {	
				// Not all change types will require a "cvs update"
				// Some can be deleted locally without performing an update
				switch (kind & SyncInfo.DIRECTION_MASK) {
					case SyncInfo.INCOMING:
						switch (kind & SyncInfo.CHANGE_MASK) {
							case SyncInfo.DELETION:
								// Incoming deletions can just be deleted instead of updated
								updateDeletions.add(changedNode);
								willBeAttempted = true;
								break;
							default:
								// add the file to the list of files to be updated
								updateShallow.add(changedNode);
								willBeAttempted = true;
								break;
						}
						break;
					case SyncInfo.CONFLICTING:
						switch (kind & SyncInfo.CHANGE_MASK) {
							case SyncInfo.CHANGE:
								// add the file to the list of files to be updated
								updateShallow.add(changedNode);
								willBeAttempted = true;
								break;
						}
						break;
				}
				if (!willBeAttempted) {
					skipped.add(syncSet.getSyncInfo(resource));
				}
			} else {
				// Special handling for folders to support shallow operations on files
				// (i.e. folder operations are performed using the sync info already
				// contained in the sync info.
				if (isOutOfSync(changedNode)) {
					parentCreationElements.add(changedNode);
				}
			}

		}
		try {
			monitor.beginTask(null, 100);

			if (updateDeletions.size() > 0) {
				runUpdateDeletions(updateDeletions.toArray(new SyncInfo[updateDeletions.size()]), Policy.subMonitorFor(monitor, 25));
			}			
			if (parentCreationElements.size() > 0) {
				makeInSync(parentCreationElements.toArray(new SyncInfo[parentCreationElements.size()]), Policy.subMonitorFor(monitor, 25));				
			}
			if (updateShallow.size() > 0) {
				runSafeUpdate(project, updateShallow.toArray(new SyncInfo[updateShallow.size()]), Policy.subMonitorFor(monitor, 50));
			}
		} finally {
			monitor.done();
		}
		return;
	}

	/**
	 * Perform an overwrite (unsafe) update on the resources in the provided set.
	 * The passed sync set may containe resources from multiple projects and
	 * it cannot be assumed that any scheduling rule is held when this method
	 * is invoked.
	 * @param syncSet the set containing the resources to be updated
	 * @param monitor
	 */
	protected abstract void overwriteUpdate(SyncInfoSet syncSet, IProgressMonitor monitor) throws TeamException;

	/*
	 * Return a filter which selects the cases that we know ahead of time
	 * will fail on an update
	 */
	protected FastSyncInfoFilter getKnownFailureCases() {
		return new OrSyncInfoFilter(new FastSyncInfoFilter[] {
			// Conflicting additions of files will fail
			new AndSyncInfoFilter(new FastSyncInfoFilter[] {
				FastSyncInfoFilter.getDirectionAndChangeFilter(SyncInfo.CONFLICTING, SyncInfo.ADDITION),
				new FastSyncInfoFilter() {
					@Override
					public boolean select(SyncInfo info) {
						return info.getLocal().getType() == IResource.FILE;
					}
				}
			}),
			// Conflicting changes of files will fail if the local is not managed
			// or is an addition
			new AndSyncInfoFilter(new FastSyncInfoFilter[] {
				FastSyncInfoFilter.getDirectionAndChangeFilter(SyncInfo.CONFLICTING, SyncInfo.CHANGE),
				new FastSyncInfoFilter() {
					@Override
					public boolean select(SyncInfo info) {
						if (info.getLocal().getType() == IResource.FILE) {
							try {
								ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor((IFile)info.getLocal());
								byte[] syncBytes = cvsFile.getSyncBytes();
								return (syncBytes == null || ResourceSyncInfo.isAddition(syncBytes));
							} catch (CVSException e) {
								CVSUIPlugin.log(e);
								// Fall though and try to update
							}
						}
						return false;
					}
				}
			}),
			// Conflicting changes involving a deletion on one side will aways fail
			new AndSyncInfoFilter(new FastSyncInfoFilter[] {
				FastSyncInfoFilter.getDirectionAndChangeFilter(SyncInfo.CONFLICTING, SyncInfo.CHANGE),
				new FastSyncInfoFilter() {
					@Override
					public boolean select(SyncInfo info) {
						IResourceVariant remote = info.getRemote();
						IResourceVariant base = info.getBase();
						if (info.getLocal().exists()) {
							// local != base and no remote will fail
							return (base != null && remote == null);
						} else {
							// no local and base != remote
							return (base != null && remote != null && !base.equals(remote));
						}
					}
				}
			}),
			// Conflicts where the file type is binary will work but are not merged
			// so they should be skipped
			new AndSyncInfoFilter(new FastSyncInfoFilter[] {
				FastSyncInfoFilter.getDirectionAndChangeFilter(SyncInfo.CONFLICTING, SyncInfo.CHANGE),
				new FastSyncInfoFilter() {
					@Override
					public boolean select(SyncInfo info) {
						IResource local = info.getLocal();
						if (local.getType() == IResource.FILE) {
							try {
								ICVSFile file = CVSWorkspaceRoot.getCVSFileFor((IFile)local);
								byte[] syncBytes = file.getSyncBytes();
								if (syncBytes != null) {
									return ResourceSyncInfo.isBinary(syncBytes);
								}
							} catch (CVSException e) {
								// There was an error obtaining or interpreting the sync bytes
								// Log it and skip the file
								CVSProviderPlugin.log(e);
								return true;
							}
						}
						return false;
					}
				}
			}),
			// Outgoing changes may not fail but they are skipped as well
			new SyncInfoDirectionFilter(SyncInfo.OUTGOING)
		});
	}
	
	/**
	 * Warn user that some files could not be updated.
	 * Note: This method is designed to be overridden by test cases.
	 */
	protected void warnAboutFailedResources(final SyncInfoSet syncSet) {
		TeamUIPlugin.getStandardDisplay()
				.syncExec(() -> MessageDialog.openInformation(getShell(),
						CVSUIMessages.SafeUpdateAction_warnFilesWithConflictsTitle,
						CVSUIMessages.SafeUpdateAction_warnFilesWithConflictsDescription));
	}
	
	/**
	 * This method is invoked for all resources in the sync set that are incoming deletions.
	 * It is done separately to allow deletions to be performed before additions that may
	 * be the same name with different letter case.
	 * @param nodes the SyncInfo nodes that are incoming deletions
	 * @param monitor
	 * @throws TeamException
	 */
	protected abstract void runUpdateDeletions(SyncInfo[] nodes, IProgressMonitor monitor) throws TeamException;
	
	/**
	 * This method is invoked for all resources in the sync set that are incoming changes
	 * (but not deletions: @see runUpdateDeletions) or conflicting changes.
	 * This method should only update those conflicting resources that are automergable.
	 * @param project the project containing the nodes
	 * @param nodes the incoming or conflicting SyncInfo nodes
	 * @param monitor
	 * @throws TeamException
	 */
	protected abstract void runSafeUpdate(IProject project, SyncInfo[] nodes, IProgressMonitor monitor) throws TeamException;
	
	protected void safeUpdate(IProject project, IResource[] resources, LocalOption[] localOptions, IProgressMonitor monitor) throws TeamException {
		try {
			UpdateOnlyMergableOperation operation = new UpdateOnlyMergableOperation(getPart(), project, resources, localOptions);
			operation.run(monitor);
			addSkippedFiles(operation.getSkippedFiles());
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (InterruptedException e) {
			Policy.cancelOperation();
		}
	}
	
	/**
	 * Notification of all resource that were updated (either safely or othrwise)
	 */
	protected abstract void updated(IResource[] resources) throws TeamException;
	
	private void addSkippedFiles(IFile[] files) {
		SyncInfoSet set = getSyncInfoSet();
		for (IFile file : files) {
			skipped.add(set.getSyncInfo(file));
		}
	}
	
	@Override
	protected String getErrorTitle() {
		return CVSUIMessages.UpdateAction_update; 
	}
	
	@Override
	protected String getJobName() {
		SyncInfoSet syncSet = getSyncInfoSet();
		return NLS.bind(CVSUIMessages.UpdateAction_jobName, new String[] { Integer.valueOf(syncSet.size()).toString() }); 
	}

	/**
	 * Confirm with the user what we are going to be doing. By default the update action doesn't 
	 * prompt because the user has usually selected resources first. But in some cases, for example
	 * when performing a toolbar action, a confirmation prompt is nice.
	 * @param set the resources to be updated
	 * @return <code>true</code> if the update operation can continue, and <code>false</code>
	 * if the update has been cancelled by the user.
	 */
	private boolean promptIfNeeded() {
		final SyncInfoSet set = getSyncInfoSet();
		final boolean[] result = new boolean[] {true};
		if(getPromptBeforeUpdate()) {
			TeamUIPlugin.getStandardDisplay().syncExec(() -> {
				String sizeString = Integer.toString(set.size());
				String message = set.size() > 1
						? NLS.bind(CVSUIMessages.UpdateAction_promptForUpdateSeveral, new String[] { sizeString })
						: NLS.bind(CVSUIMessages.UpdateAction_promptForUpdateOne, new String[] { sizeString }); //
				result[0] = MessageDialog.openQuestion(getShell(),
						NLS.bind(CVSUIMessages.UpdateAction_promptForUpdateTitle, new String[] { sizeString }),
						message);
			});
		}
		return result[0];
	}
	
	public boolean getPromptBeforeUpdate() {
		return promptBeforeUpdate;
	}
}
