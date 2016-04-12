/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.core.subscribers.WorkingSetFilteredSyncInfoCollector;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.SubscriberParticipantPage;
import org.eclipse.team.ui.synchronize.*;

/**
 * Remove the selected elements from the page
 */
public class RemoveFromViewAction extends SynchronizeModelAction {

	public RemoveFromViewAction(ISynchronizePageConfiguration configuration) {
		super(null, configuration);
		Utils.initAction(this, "action.removeFromView."); //$NON-NLS-1$
		setId(TeamUIPlugin.REMOVE_FROM_VIEW_ACTION_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#run()
	 */
	@Override
	public void run() {
		if (confirmRemove()) {
			super.run();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSubscriberOperation(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration, org.eclipse.compare.structuremergeviewer.IDiffElement[])
	 */
	@Override
	protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return new SynchronizeModelOperation(configuration, elements) {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				SyncInfoSet set = getSyncInfoSet();
				removeFromView(set);
			}
			@Override
			protected boolean canRunAsJob() {
				return false;
			}
			/**
			 * Remove the sync info contained in the given set from the view.
			 * @param set the sync info set
			 */
			private void removeFromView(final SyncInfoSet set) {
				ISynchronizePage page = getConfiguration().getPage();
				if (page instanceof SubscriberParticipantPage) {
					final WorkingSetFilteredSyncInfoCollector collector = ((SubscriberParticipantPage)page).getCollector();
					collector.run(new IWorkspaceRunnable() {
						@Override
						public void run(IProgressMonitor monitor) throws CoreException {
							collector.getWorkingSetSyncInfoSet().removeAll(set.getResources());
						}
					});
				}
			}
		};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#needsToSaveDirtyEditors()
	 */
	@Override
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	private boolean confirmRemove() {
		IPreferenceStore store = TeamUIPlugin.getPlugin().getPreferenceStore();
		if (store.getBoolean(IPreferenceIds.SYNCVIEW_REMOVE_FROM_VIEW_NO_PROMPT)) {
			return true;
		} else {
			MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(
					getConfiguration().getSite().getShell(),
					TeamUIMessages.RemoveFromView_warningTitle,
					TeamUIMessages.RemoveFromView_warningMessage,
					TeamUIMessages.RemoveFromView_warningDontShow,
					false,
					null,
					null);
			store.setValue(IPreferenceIds.SYNCVIEW_REMOVE_FROM_VIEW_NO_PROMPT, dialog.getToggleState());
			return dialog.getReturnCode() == Window.OK;
		}
	}
}
