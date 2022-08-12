/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
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

	@Override
	public void syncInfoChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
		updateCounts();
	}

	@Override
	public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
		updateCounts();
	}

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
