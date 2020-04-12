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
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.mapping.CVSCheckedInChangeSet;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteCompareOperation.CompareTreeBuilder;
import org.eclipse.team.internal.ccvs.ui.subscriber.CVSChangeSetCollector;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.core.subscribers.DiffChangeSet;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.ResourceModelParticipantAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.PlatformUI;

class OpenChangeSetAction extends ResourceModelParticipantAction {

	protected OpenChangeSetAction(ISynchronizePageConfiguration configuration) {
		super(CVSUIMessages.OpenCommitSetAction_20, configuration);
		ISelection selection = configuration.getSite().getSelectionProvider().getSelection();
		if (selection != null)
			selectionChanged(selection);
	}
	
	private ChangeSet getChangeSet(IStructuredSelection selection) {
		// First, check to see if a change set is selected directly
		if (selection.size() == 1) {
			Object o = selection.getFirstElement();
			if (o instanceof IAdaptable) {
				ChangeSet set = ((IAdaptable)o).getAdapter(ChangeSet.class);
				if (set != null)
					return set;
			}
		}
		// Failing that, check to see if all the selected elements and their children are in the same change set
		if (selection instanceof TreeSelection) {
			TreeSelection ts = (TreeSelection) selection;
			TreePath[] paths = ts.getPaths();
			if (paths.length > 0) {
				ChangeSet set = getChangeSet(paths[0]);
				if (set != null) {
					for (int i = 1; i < paths.length; i++) {
						TreePath path = paths[i];
						ChangeSet otherSet = getChangeSet(path);
						if (set != otherSet)
							return null;
					}
				}
				return set;
			}
		}
		return null;
	}

	private ChangeSet getChangeSet(TreePath treePath) {
		Object test = treePath.getFirstSegment();
		if (test instanceof ChangeSet) {
			ChangeSet cs = (ChangeSet) test;
			return cs;
		}
		return null;
	}
	
	@Override
	protected boolean isEnabledForSelection(IStructuredSelection selection) {
		// The selection only contains appropriate files so
		// only enable if the selection is contained within a single change set
		ChangeSet set = getChangeSet(selection);
		return set != null;
	}

	public void openEditor(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			IDiff[] diffs = getSelectedDiffs(monitor);
			if (diffs.length > 0) {
				ICVSRepositoryLocation location = getLocation(diffs[0]);
				if (location == null) {
					throw new CVSException(CVSUIMessages.OpenCommitSetAction_21); 
				}
				CompareTreeBuilder builder = new CompareTreeBuilder(location, null, null);
				if (buildTrees(builder, diffs)) {
					builder.cacheContents(monitor);
					builder.openCompareEditor(getConfiguration().getSite().getPart().getSite().getPage(), getCompareTitle(), getCompareToolTip());
				}
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}
	
	private IDiff[] getSelectedDiffs(IProgressMonitor monitor) throws CoreException {
		ResourceTraversal[] traversals = getResourceTraversals(getStructuredSelection(), monitor);
		DiffChangeSet set = (DiffChangeSet)getChangeSet(getStructuredSelection());
		return set.getDiffTree().getDiffs(traversals);
	}

	/*
	 * Build the trees that will be compared
	 */
	private boolean buildTrees(CompareTreeBuilder builder, IDiff[] diffs) {
		for (IDiff diff : diffs) {
			if (isFileChange(diff)) {
				IFileRevision remoteRevision = Utils.getRemote(diff);
				IResourceVariant remote = SyncInfoToDiffConverter.asResourceVariant(remoteRevision);
				if (remote == null) {
					IFileRevision predecessorRevision = Utils.getBase(diff);
					IResourceVariant predecessor = SyncInfoToDiffConverter.asResourceVariant(predecessorRevision);
					if (predecessor instanceof ICVSRemoteFile) {
						builder.addToTrees((ICVSRemoteFile)predecessor, null);
					}
				} else if (remote instanceof ICVSRemoteFile) {
					try {
						ICVSRemoteFile predecessor = getImmediatePredecessor(remote);
						builder.addToTrees(predecessor, (ICVSRemoteFile)remote);
					} catch (TeamException e) {
						Utils.handle(e);
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private boolean isFileChange(IDiff diff) {
		IResource resource = ResourceDiffTree.getResourceFor(diff);
		if (resource.getType() == IResource.FILE) {
			if (diff instanceof IThreeWayDiff) {
				IThreeWayDiff twd = (IThreeWayDiff) diff;
				return twd.getDirection() == IThreeWayDiff.INCOMING || twd.getDirection() == IThreeWayDiff.CONFLICTING;
			}
			return true;
		}
		return false;
	}

	private ICVSRepositoryLocation getLocation(IDiff diff) {
		IResource resource = ResourceDiffTree.getResourceFor(diff);
		RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject());
		if (provider instanceof CVSTeamProvider) {
			CVSTeamProvider ctp = (CVSTeamProvider) provider;
			try {
				return ctp.getCVSWorkspaceRoot().getRemoteLocation();
			} catch (CVSException e) {
				CVSUIPlugin.log(e);
			}
		}
		return null;
	}
	
	private String getCompareTitle() {
		ChangeSet set = getChangeSet(getStructuredSelection());
		if (set instanceof CVSCheckedInChangeSet) {
			CVSCheckedInChangeSet cics = (CVSCheckedInChangeSet)set;
			String date = DateFormat.getDateTimeInstance().format(cics.getDate());
			return NLS.bind(CVSUIMessages.OpenChangeSetAction_0, new String[] {cics.getAuthor(), date});
		}
		return CVSUIMessages.OpenChangeSetAction_1;
	}
	
	private String getCompareToolTip() {
		ChangeSet set = getChangeSet(getStructuredSelection());
		if (set != null)
			return set.getName();
		return null;
	}
	
	private ICVSRemoteFile getImmediatePredecessor(IResourceVariant remote) throws TeamException {
		CheckedInChangeSetCollector changeSetCollector = getChangeSetCollector();
		if (changeSetCollector != null) {
			return changeSetCollector.getImmediatePredecessor((ICVSRemoteFile)remote);
		}
		return null;
	}

	private CheckedInChangeSetCollector getChangeSetCollector() {
		return (CheckedInChangeSetCollector)getConfiguration().getProperty(CVSChangeSetCollector.CVS_CHECKED_IN_COLLECTOR);
	}
	
	@Override
	public void run() {
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, monitor -> openEditor(monitor));
		} catch (InvocationTargetException e) {
			Utils.handle(e);
		} catch (InterruptedException e) {
			// Ignore
		}
	}
}
