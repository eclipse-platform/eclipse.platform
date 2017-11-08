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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.OverrideAndUpdateOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class OverrideAndUpdateSubscriberOperation extends CVSSubscriberOperation {
	protected OverrideAndUpdateSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		super(configuration, elements);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.TeamOperation#shouldRun()
	 */
	public boolean shouldRun() {
		SyncInfoSet syncSet = getSyncInfoSet();
		return(promptForOverwrite(syncSet));
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSSubscriberOperation#run(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void runWithProjectRule(IProject project, SyncInfoSet set, IProgressMonitor monitor) throws TeamException {
		try {
			SyncInfo[] conflicts = set.getNodes(getConflictingAdditionFilter());
			List conflictingResources = new ArrayList();
			for (int i = 0; i < conflicts.length; i++) {
				SyncInfo info = conflicts[i];
				conflictingResources.add(info.getLocal());
			}
			new OverrideAndUpdateOperation(getPart(), project, set.getResources(), (IResource[]) conflictingResources.toArray(new IResource[conflictingResources.size()]), null /* tag */, false /* recurse */).run(monitor);
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (InterruptedException e) {
			Policy.cancelOperation();
		}
	}
	private FastSyncInfoFilter getConflictingAdditionFilter() {
		return new FastSyncInfoFilter.AndSyncInfoFilter(
			new FastSyncInfoFilter[] {
				new FastSyncInfoFilter.SyncInfoDirectionFilter(new int[] {SyncInfo.CONFLICTING}), 
				new FastSyncInfoFilter.SyncInfoChangeTypeFilter(new int[] {SyncInfo.ADDITION})
			});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSSubscriberAction#getJobName(org.eclipse.team.ui.sync.SyncInfoSet)
	 */
	protected String getJobName() {
		SyncInfoSet syncSet = getSyncInfoSet();
		return NLS.bind(CVSUIMessages.UpdateAction_jobName, new String[] { Integer.valueOf(syncSet.size()).toString() }); 
	}
}
