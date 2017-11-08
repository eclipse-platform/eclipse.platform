/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;


public class CompareRevertOperation extends CVSSubscriberOperation {
	protected CompareRevertOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		super(configuration, elements);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSSubscriberAction#getJobName()
	 */
	protected String getJobName() {
		SyncInfoSet syncSet = getSyncInfoSet();
		return NLS.bind(CVSUIMessages.CompareRevertAction_0, new String[] { Integer.valueOf(syncSet.size()).toString() }); 

	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSSubscriberAction#run(org.eclipse.team.core.subscribers.MutableSyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void runWithProjectRule(IProject project, SyncInfoSet syncSet, IProgressMonitor monitor) throws TeamException {
		SyncInfo[] changed = syncSet.getSyncInfos();
		if (changed.length == 0) return;
		
		if(! promptForOverwrite(syncSet)) return;
		
		// The list of sync resources to be updated using "cvs update"
		List updateShallow = new ArrayList();
		// A list of sync resource folders which need to be created locally 
		// (incoming addition or previously pruned)
		Set parentCreationElements = new HashSet();
	
		for (int i = 0; i < changed.length; i++) {
			SyncInfo changedNode = changed[i];
			
			// Make sure that parent folders exist
			SyncInfo parent = getParent(changedNode);
			if (parent != null && isOutOfSync(parent)) {
				// We need to ensure that parents that are either incoming folder additions
				// or previously pruned folders are recreated.
				parentCreationElements.add(parent);
			}
			
			IResource resource = changedNode.getLocal();
			if (resource.getType() == IResource.FILE) {	
				if (changedNode.getLocal().exists()) {
					updateShallow.add(changedNode);
				} else if (changedNode.getRemote() != null) {
					updateShallow.add(changedNode);
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

			if (parentCreationElements.size() > 0) {
				makeInSync((SyncInfo[]) parentCreationElements.toArray(new SyncInfo[parentCreationElements.size()]), Policy.subMonitorFor(monitor, 25));				
			}		
			if (updateShallow.size() > 0) {
				runUpdate((SyncInfo[])updateShallow.toArray(new SyncInfo[updateShallow.size()]), Policy.subMonitorFor(monitor, 75));
			}
		} finally {
			monitor.done();
		}
		return;
	}
	
	private void runUpdate(SyncInfo[] infos, IProgressMonitor monitor) throws TeamException {
		monitor.beginTask(null, 100 * infos.length);
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			makeRemoteLocal(info, Policy.subMonitorFor(monitor, 100));
		}
		monitor.done();
	}
}
