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
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.SyncInfoDirectionFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

public class WorkspaceUpdateAction extends CVSParticipantAction {
	
	private boolean promptBeforeUpdate;

	public WorkspaceUpdateAction(ISynchronizePageConfiguration configuration) {
		super(configuration);
		setId(ICVSUIConstants.CMD_UPDATE);
		setActionDefinitionId(ICVSUIConstants.CMD_UPDATE);
	}

	public WorkspaceUpdateAction(ISynchronizePageConfiguration configuration, ISelectionProvider provider, String bundleKey) {
		super(configuration, provider, bundleKey);
		setId(ICVSUIConstants.CMD_UPDATE_ALL);
		setActionDefinitionId(ICVSUIConstants.CMD_UPDATE_ALL);
	}

	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new SyncInfoDirectionFilter(new int[] {SyncInfo.INCOMING, SyncInfo.CONFLICTING});
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return new WorkspaceUpdateOperation(configuration, elements, promptBeforeUpdate);
	}

	public void setPromptBeforeUpdate(boolean prompt) {
		promptBeforeUpdate = prompt;
	}
}
