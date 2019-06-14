/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.SyncInfoDirectionFilter;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.wizards.CommitWizard;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.eclipse.ui.PlatformUI;

public class WorkspaceCommitAction extends CVSParticipantAction {

	public WorkspaceCommitAction(ISynchronizePageConfiguration configuration) {
		super(configuration);
		setId(ICVSUIConstants.CMD_COMMIT);
		setActionDefinitionId(ICVSUIConstants.CMD_COMMIT);
	}

	public WorkspaceCommitAction(ISynchronizePageConfiguration configuration, ISelectionProvider provider, String bundleKey) {
		super(configuration, provider, bundleKey);
		setId(ICVSUIConstants.CMD_COMMIT_ALL);
		setActionDefinitionId(ICVSUIConstants.CMD_COMMIT_ALL);
	}
	
	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new SyncInfoDirectionFilter(new int[] { SyncInfo.OUTGOING });
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return new WorkspaceCommitOperation(configuration, elements, false /* override */);
	}
	
	@Override
	public void runOperation() {
		final SyncInfoSet set = getSyncInfoSet();
		final Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		try {
			// Include the subscriber operation as a job listener so that the busy feedback for the 
			// commit will appear in the synchronize view
			CommitWizard.run(shell, set, getSubscriberOperation(getConfiguration(), getFilteredDiffElements()));
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
		}
	}

	/*
	 * Return the selected SyncInfo for which this action is enabled.
	 * 
	 * @return the selected SyncInfo for which this action is enabled.
	 */
	private SyncInfoSet getSyncInfoSet() {
		IDiffElement [] elements= getFilteredDiffElements();
		SyncInfoSet filtered = new SyncInfoSet();
		for (IDiffElement e : elements) {
			if (e instanceof SyncInfoModelElement) {
				filtered.add(((SyncInfoModelElement)e).getSyncInfo());
			}
		}
		return filtered;
	}
}
