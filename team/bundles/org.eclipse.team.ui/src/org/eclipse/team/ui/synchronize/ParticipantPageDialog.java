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
package org.eclipse.team.ui.synchronize;

import org.eclipse.compare.CompareUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.ui.SaveablePartAdapter;
import org.eclipse.team.ui.SaveablePartDialog;
import org.eclipse.team.ui.TeamUI;

/**
 * A dialog that displays the option of adding the participant to the {@link org.eclipse.team.ui.synchronize.ISynchronizeManager}
 * when the dialog is closed. This can be useful for showing changes for a participant modally and allowing the
 * user to decide if the participant shown should be made available non-modally.
 *
 * @see SaveablePartAdapter
 * @see ISynchronizeParticipant
 * @since 3.0
 * @deprecated Clients should use {@link ParticipantPageCompareEditorInput}
 *      and {@link CompareUI#openCompareDialog(org.eclipse.compare.CompareEditorInput)}
 */
@Deprecated
public class ParticipantPageDialog extends SaveablePartDialog {

	private ISynchronizeParticipant participant;
	private Button rememberParticipantButton;

	/**
	 * Creates a dialog with the given participant and input. The input is not created until the dialog
	 * is opened.
	 *
	 * @param shell the parent shell or <code>null</code> to create a top level shell.
	 * @param input the compare input to show in the dialog
	 * @param participant the given participant
	 */
	public ParticipantPageDialog(Shell shell, SaveablePartAdapter input, ISynchronizeParticipant participant) {
		super(shell, input);
		this.participant = participant;
	}

	@Override
	protected Control createDialogArea(Composite parent2) {
		Composite parent = (Composite) super.createDialogArea(parent2);
		if (isOfferToRememberParticipant() && participant != null && ! particantRegisteredWithSynchronizeManager(participant)) {
			rememberParticipantButton = new Button(parent, SWT.CHECK);
			rememberParticipantButton.setText(TeamUIMessages.ParticipantCompareDialog_1);
		}
		Dialog.applyDialogFont(parent2);
		return parent;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if(buttonId == IDialogConstants.OK_ID && isRememberParticipant()) {
			rememberParticipant();
		}
		super.buttonPressed(buttonId);
	}

	private boolean isRememberParticipant() {
		return getParticipant() != null && rememberParticipantButton != null && rememberParticipantButton.getSelection();
	}

	private boolean particantRegisteredWithSynchronizeManager(ISynchronizeParticipant participant) {
		return TeamUI.getSynchronizeManager().get(participant.getId(), participant.getSecondaryId()) != null;
	}

	private void rememberParticipant() {
		if(getParticipant() != null) {
			ISynchronizeManager mgr = TeamUI.getSynchronizeManager();
			ISynchronizeView view = mgr.showSynchronizeViewInActivePage();
			mgr.addSynchronizeParticipants(new ISynchronizeParticipant[] {getParticipant()});
			view.display(participant);
		}
	}

	/**
	 * Returns the participant showing in this dialog.
	 *
	 * @return the participant showing in this dialog.
	 */
	protected ISynchronizeParticipant getParticipant() {
		return participant;
	}

	/**
	 * Return whether the ability to remember the participant in the synchronize
	 * view should be presented to the user. By default, <code>true</code> is
	 * returned. Subclasses may override.
	 * @return whether the ability to remember the participant in the synchronize
	 * view should be presented to the user
	 * @since 3.2
	 */
	protected boolean isOfferToRememberParticipant() {
		return true;
	}
}
