/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.core.subscribers.WorkingSetFilteredSyncInfoCollector;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SubscriberParticipantPage;
import org.eclipse.team.ui.synchronize.ISynchronizePage;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

public class RestoreRemovedItemsAction extends SynchronizeModelAction {

	public RestoreRemovedItemsAction(ISynchronizePageConfiguration configuration) {
		super(null, configuration);
		Utils.initAction(this, "action.restoreRemovedFromView."); //$NON-NLS-1$
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return new SynchronizeModelOperation(configuration, elements) {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				restoreRemovedItems();
			}
			@Override
			protected boolean canRunAsJob() {
				return false;
			}
			/**
			 * Remove the sync info contained in the given set from the view.
			 * @param set the sync info set
			 */
			private void restoreRemovedItems() {
				ISynchronizePage page = getConfiguration().getPage();
				if (page instanceof SubscriberParticipantPage) {
					WorkingSetFilteredSyncInfoCollector collector = ((SubscriberParticipantPage)page).getCollector();
					collector.reset();
				}
			}
		};
	}

	@Override
	public boolean isEnabled(){
		return true;
	}

	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

}