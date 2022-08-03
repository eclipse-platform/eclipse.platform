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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ParticipantPageDialog;
import org.eclipse.team.ui.synchronize.ParticipantPageSaveablePart;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.ui.actions.ActionFactory;

public class RefreshUserNotificationPolicyInModalDialog implements IRefreshSubscriberListener {

	private SubscriberParticipant participant;
	private ISynchronizePageConfiguration configuration;
	private Shell shell;
	private String title;

	public RefreshUserNotificationPolicyInModalDialog(Shell shell, String title, ISynchronizePageConfiguration configuration, SubscriberParticipant participant) {
		this.title = title;
		this.configuration  = configuration;
		this.participant = participant;
		this.shell = shell;
	}

	@Override
	public void refreshStarted(IRefreshEvent event) {
	}

	@Override
	public ActionFactory.IWorkbenchAction refreshDone(final IRefreshEvent event) {
		//	Ensure that this event was generated for this participant
		if (event.getParticipant() != participant)
			return null;
		//	 If the event is for a cancelled operation, there's nothing to do
		int severity = event.getStatus().getSeverity();
		if(severity == IStatus.CANCEL || severity == IStatus.ERROR)
			return null;

		return new WorkbenchAction() {
			@Override
			public void run() {
					// If there are no changes
					if (event.getStatus().getCode() == IRefreshEvent.STATUS_NO_CHANGES) {
						MessageDialog.openInformation(shell, TeamUIMessages.OpenComparedDialog_noChangeTitle, TeamUIMessages.OpenComparedDialog_noChangesMessage); //
						return;
					}
					compareAndOpenDialog(event, participant);
					setEnabled(false);
			}
			@Override
			public void dispose() {
				if (TeamUI.getSynchronizeManager().get(participant.getId(), participant.getSecondaryId()) == null) {
					participant.dispose();
				}
			}
		};
	}

	protected void compareAndOpenDialog(final IRefreshEvent event, final SubscriberParticipant participant) {
		CompareConfiguration cc = new CompareConfiguration();
		ParticipantPageSaveablePart input = new ParticipantPageSaveablePart(Utils.getShell(null), cc, configuration, participant) {
			@Override
			public String getTitle() {
				return RefreshUserNotificationPolicyInModalDialog.this.title;
			}
		};
		try {
			ParticipantPageDialog dialog = new ParticipantPageDialog(shell, input, participant);
			dialog.setBlockOnOpen(true);
			dialog.open();
		} finally {
			input.dispose();
		}
	}
}
