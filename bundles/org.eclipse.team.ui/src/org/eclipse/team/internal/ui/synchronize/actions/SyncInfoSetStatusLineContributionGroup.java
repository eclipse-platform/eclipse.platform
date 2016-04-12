/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class SyncInfoSetStatusLineContributionGroup extends
		StatusLineContributionGroup implements ISyncInfoSetChangeListener {

	public SyncInfoSetStatusLineContributionGroup(Shell shell, ISynchronizePageConfiguration configuration) {
		super(shell, configuration);
		// Listen to changes to update the counts
		SyncInfoSet set = getSyncInfoSet();
		set.addSyncSetChangedListener(this);
	}
	
	@Override
	public void dispose() {
		getSyncInfoSet().removeSyncSetChangedListener(this);
		super.dispose();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.internal.ui.sync.sets.ISyncSetChangedListener#syncSetChanged(org.eclipse.team.internal.ui.sync.sets.SyncSetChangedEvent)
	 */
	@Override
	public void syncInfoChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
		updateCounts();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ISyncInfoSetChangeListener#syncInfoSetReset(org.eclipse.team.core.subscribers.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
		updateCounts();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ISyncInfoSetChangeListener#syncInfoSetError(org.eclipse.team.core.subscribers.SyncInfoSet, org.eclipse.team.core.ITeamStatus[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
		// Nothing to do for errors
	}
	
	private SyncInfoSet getSyncInfoSet() {
		return (SyncInfoSet)getConfiguration().getProperty(SynchronizePageConfiguration.P_WORKING_SET_SYNC_INFO_SET);
	}

	@Override
	protected int getChangeCount() {
		return getSyncInfoSet().size();
	}

	@Override
	protected int countFor(int state) {
		return (int)getSyncInfoSet().countFor(state, SyncInfo.DIRECTION_MASK);
	}

}
