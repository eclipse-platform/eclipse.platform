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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * This action performs a "cvs update -j start -j end ..." to merge changes
 * into the local workspace.
 */
public class MergeUpdateOperation extends SafeUpdateOperation {
	
	Subscriber currentSubcriber = null;
	
	protected MergeUpdateOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, boolean promptBeforeUpdate) {
		super(configuration, elements, promptBeforeUpdate);
	}
	
	@Override
	protected String getJobName() {
		SyncInfoSet syncSet = getSyncInfoSet();
		return NLS.bind(CVSUIMessages.MergeUpdateAction_jobName, new String[] { Integer.valueOf(syncSet.size()).toString() }); 
	}
	
	@Override
	protected boolean getOverwriteLocalChanges() {
		return true;
	}
	
	@Override
	protected void updated(IResource[] resources) throws TeamException {
		// Mark all succesfully updated resources as merged
		if(resources.length > 0 && currentSubcriber != null) {
			((CVSMergeSubscriber)currentSubcriber).merged(resources);
		}
	}
	
	@Override
	protected void runUpdateDeletions(SyncInfo[] nodes, IProgressMonitor monitor) throws TeamException {
		// When merging, update deletions become outgoing deletions so just delete
		// the files locally without unmanaging (so the sync info is kept to 
		// indicate an outgoing deletion
		try {
			monitor.beginTask(null, 100 * nodes.length);
			for (SyncInfo node : nodes) {
				IResource resource = node.getLocal();
				if (resource.getType() == IResource.FILE) {
					((IFile)resource).delete(false /* force */, true /* keep local history */, Policy.subMonitorFor(monitor, 100));
				}
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		} finally {
			monitor.done();
		}
	}
	
	@Override
	protected void runSafeUpdate(IProject project, SyncInfo[] nodes, IProgressMonitor monitor) throws TeamException {
		if(nodes.length > 0) {
			setSubscriber(nodes[0]);
			CVSTag startTag = ((CVSMergeSubscriber)currentSubcriber).getStartTag();
			CVSTag endTag = ((CVSMergeSubscriber)currentSubcriber).getEndTag();

			// Incoming additions require different handling then incoming changes and deletions
			List<SyncInfo> additions = new ArrayList<>();
			List<SyncInfo> changes = new ArrayList<>();
			for (SyncInfo resource : nodes) {
				int kind = resource.getKind();
				if ((kind & SyncInfo.CHANGE_MASK) == SyncInfo.ADDITION) {
					additions.add(resource);
				} else {
					changes.add(resource);
				}
			}
			
			try {
				monitor.beginTask(null, (additions.size() + changes.size()) * 100);
				if (!additions.isEmpty()) {
					safeUpdate(
						project, 
						getIResourcesFrom(additions.toArray(new SyncInfo[additions.size()])), 
						new Command.LocalOption[] {
							Command.DO_NOT_RECURSE,
							Command.makeArgumentOption(Update.JOIN, endTag.getName()) 
						},
						Policy.subMonitorFor(monitor, additions.size() * 100));
				}
				if (!changes.isEmpty()) {
					safeUpdate(
						project, 
						getIResourcesFrom(changes.toArray(new SyncInfo[changes.size()])), 
						new Command.LocalOption[] {
							Command.DO_NOT_RECURSE,
							Command.makeArgumentOption(Update.JOIN, startTag.getName()),
							Command.makeArgumentOption(Update.JOIN, endTag.getName()) 
						},
						Policy.subMonitorFor(monitor, changes.size() * 100));
				}
			} finally {
				monitor.done();
			}
		}
	}
	
	/**
	 * @param nodes
	 * @throws CVSException
	 */
	private void setSubscriber(SyncInfo node) throws CVSException {
		// Assumption that all nodes are from the same subscriber.
		currentSubcriber = ((CVSSyncInfo)node).getSubscriber();
		if (!(currentSubcriber instanceof CVSMergeSubscriber)) {
			throw new CVSException(NLS.bind(CVSUIMessages.MergeUpdateAction_invalidSubscriber, new String[] { currentSubcriber.toString() })); 
		}
	}

	@Override
	protected void overwriteUpdate(SyncInfoSet set, IProgressMonitor monitor) throws TeamException {
		SyncInfo[] nodes = set.getSyncInfos();
		if (nodes.length == 0) return;
		setSubscriber(nodes[0]);
		monitor.beginTask(null, 1000 * nodes.length);
		try {
			for (SyncInfo node : nodes) {
				makeRemoteLocal(node, Policy.subMonitorFor(monitor, 1000));
			}
		} finally {
			monitor.done();
		}
	}
}
