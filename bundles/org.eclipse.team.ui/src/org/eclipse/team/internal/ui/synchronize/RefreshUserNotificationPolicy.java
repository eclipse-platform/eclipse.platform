/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.actions.OpenInCompareAction;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.ui.actions.ActionFactory;

/**
 * This class manages the notification and setup that occurs after a refresh is completed.
 */
public class RefreshUserNotificationPolicy implements IRefreshSubscriberListener {

	private ISynchronizeParticipant participant;

	public RefreshUserNotificationPolicy(ISynchronizeParticipant participant) {
		this.participant = participant;
	}

	@Override
	public void refreshStarted(final IRefreshEvent event) {
		TeamUIPlugin.getStandardDisplay().asyncExec(() -> {
			if (event.getRefreshType() == IRefreshEvent.USER_REFRESH && event.getParticipant() == participant) {
				ISynchronizeView view = TeamUI.getSynchronizeManager().showSynchronizeViewInActivePage();
				if (view != null) {
					view.display(participant);
				}
			}
		});
	}

	@Override
	public ActionFactory.IWorkbenchAction refreshDone(final IRefreshEvent event) {
		// Ensure that this event was generated for this participant
		if (event.getParticipant() != participant) return null;
		// If the event is for a canceled operation, there's nothing to do
		int severity = event.getStatus().getSeverity();
		if(severity == IStatus.CANCEL || severity == IStatus.ERROR) return null;
		// Decide on what action to take after the refresh is completed
		return new WorkbenchAction() {
			@Override
			public void run() {
				boolean prompt = (event.getStatus().getCode() == IRefreshEvent.STATUS_NO_CHANGES);

				prompt = handleRefreshDone(event, prompt);

				// Prompt user if preferences are set for this type of refresh.
				if (prompt) {
					notifyIfNeededModal(event);
				}
				setToolTipText(getToolTipText());
				if (event.isLink()) {
					// Go to the sync view
					ISynchronizeView view = TeamUI.getSynchronizeManager().showSynchronizeViewInActivePage();
					if (view != null) {
						view.display(participant);
					}
				}
			}

			@Override
			public String getToolTipText() {
				boolean prompt = (event.getStatus().getCode() == IRefreshEvent.STATUS_NO_CHANGES);
				if(prompt) {
					return TeamUIMessages.RefreshSubscriberJob_2a;
				} else {
					return NLS.bind(TeamUIMessages.RefreshSubscriberJob_2b, new String[] { Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, participant.getName()) });
				}
			}
		};
	}

	private void notifyIfNeededModal(final IRefreshEvent event) {
		TeamUIPlugin.getStandardDisplay().asyncExec(() -> {
			String title = (event.getRefreshType() == IRefreshEvent.SCHEDULED_REFRESH
					? NLS.bind(TeamUIMessages.RefreshCompleteDialog_4a, new String[] { Utils.getTypeName(participant) })
					: NLS.bind(TeamUIMessages.RefreshCompleteDialog_4,
							new String[] { Utils.getTypeName(participant) }));
			MessageDialog.openInformation(Utils.getShell(null), title, event.getStatus().getMessage());
		});
	}

	protected boolean handleRefreshDone(final IRefreshEvent event, boolean prompt) {
		if (participant instanceof SubscriberParticipant) {
			SubscriberParticipant sp = (SubscriberParticipant) participant;
			SyncInfo[] infos = ((RefreshChangeListener)event.getChangeDescription()).getChanges();
			List<IResource> selectedResources = new ArrayList<>();
			selectedResources.addAll(Arrays.asList(((RefreshChangeListener)event.getChangeDescription()).getResources()));
			for (SyncInfo info : infos) {
				selectedResources.add(info.getLocal());
			}
			IResource[] resources = selectedResources.toArray(new IResource[selectedResources.size()]);

			// If it's a file, simply show the compare editor
			if (resources.length == 1 && resources[0].getType() == IResource.FILE) {
				IResource file = resources[0];
				SyncInfo info = sp.getSubscriberSyncInfoCollector().getSyncInfoSet().getSyncInfo(file);
				if(info != null) {
					OpenInCompareAction.openCompareEditor(participant, info, null);
					prompt = false;
				}
			}
		}
		// TODO: Implement one change case for model participant
		return prompt;
	}
}
