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
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.diff.IDiffChangeListener;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.actions.StatusLineContributionGroup;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class DiffTreeStatusLineContributionGroup extends
		StatusLineContributionGroup implements IDiffChangeListener {

	public DiffTreeStatusLineContributionGroup(Shell shell, ISynchronizePageConfiguration configuration) {
		super(shell, configuration);
		getSynchronizationContext().getDiffTree().addDiffChangeListener(this);
	}

	@Override
	public void dispose() {
		getSynchronizationContext().getDiffTree().removeDiffChangeListener(this);
		super.dispose();
	}

	@Override
	protected int getChangeCount() {
		return getSynchronizationContext().getDiffTree().size();
	}

	private ISynchronizationContext getSynchronizationContext() {
		return (ISynchronizationContext)getConfiguration().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT);
	}

	@Override
	protected int countFor(int state) {
		switch (state) {
		case SyncInfo.OUTGOING:
			state = IThreeWayDiff.OUTGOING;
			break;
		case SyncInfo.INCOMING:
			state = IThreeWayDiff.INCOMING;
			break;
		case SyncInfo.CONFLICTING:
			state = IThreeWayDiff.CONFLICTING;
			break;
		}
		return (int)getSynchronizationContext().getDiffTree().countFor(state, IThreeWayDiff.DIRECTION_MASK);
	}

	@Override
	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		updateCounts();
	}

	@Override
	public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
		// Do nothing
	}

}
