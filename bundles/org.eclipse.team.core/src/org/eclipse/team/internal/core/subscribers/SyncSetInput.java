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
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoFilter;
import org.eclipse.team.internal.core.Policy;

/**
 * This is the superclass for all SyncSet input providers
 */
public abstract class SyncSetInput {

	private SubscriberSyncInfoSet syncSet;
	private SyncInfoFilter filter = new FastSyncInfoFilter();

	public SyncSetInput(SubscriberEventHandler handler) {
		syncSet = new SubscriberSyncInfoSet(handler);
	}

	public SubscriberSyncInfoSet getSyncSet() {
		return syncSet;
	}

	/**
	 * This method is invoked from reset to get all the sync information from
	 * the input source.
	 */
	protected abstract void fetchInput(IProgressMonitor monitor) throws TeamException;

	/**
	 * The input is no longer being used. Disconnect it from its source.
	 */
	public abstract void disconnect();

	/**
	 * Reset the input. This will clear the current contents of the sync set and
	 * obtain the contents from the input source.
	 */
	public void reset(IProgressMonitor monitor) throws TeamException {

		try {
			syncSet.beginInput();
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(null, 100);
			syncSet.clear();
			fetchInput(Policy.subMonitorFor(monitor, 90));
		} finally {
			syncSet.endInput(Policy.subMonitorFor(monitor, 10));
			monitor.done();
		}
	}

	/**
	 * Collect the change in the provided sync info.
	 */
	protected void collect(SyncInfo info, IProgressMonitor monitor) {
		boolean isOutOfSync = filter.select(info, monitor);
		SyncInfo oldInfo = syncSet.getSyncInfo(info.getLocal());
		boolean wasOutOfSync = oldInfo != null;
		if (isOutOfSync) {
			syncSet.add(info);
		} else if (wasOutOfSync) {
			syncSet.remove(info.getLocal());
		}
	}

	protected void remove(IResource resource)  {
		SyncInfo oldInfo = syncSet.getSyncInfo(resource);
		if (oldInfo != null) {
			syncSet.remove(resource);
		}
	}

	public SyncInfoFilter getFilter() {
		return filter;
	}

	public void setFilter(SyncInfoFilter filter) {
		this.filter = filter;
	}

}
