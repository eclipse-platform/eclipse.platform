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

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.*;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

/**
 * Resets the dirty state of files whose contents match their base.
 */
public class RefreshDirtyStateAction extends CVSParticipantAction {
	
	public RefreshDirtyStateAction(ISynchronizePageConfiguration configuration) {
		super(configuration);
	}

	@Override
	protected FastSyncInfoFilter getSyncInfoFilter() {
		// Only interested in outgoing changes
		return new AndSyncInfoFilter(new FastSyncInfoFilter[] {
			new SyncInfoDirectionFilter(new int[] {SyncInfo.OUTGOING, SyncInfo.CONFLICTING }),
			new SyncInfoChangeTypeFilter(SyncInfo.CHANGE)
		});
	}
	
	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return new RefreshDirtyStateOperation(configuration, elements);
	}
}
