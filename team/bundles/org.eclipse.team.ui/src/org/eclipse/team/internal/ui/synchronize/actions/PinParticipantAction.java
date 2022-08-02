/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.PlatformUI;

/**
 * Action that toggles pinned state of a participant
 */
public class PinParticipantAction extends Action implements IPropertyChangeListener {

	private ISynchronizeParticipant participant;

	public PinParticipantAction() {
		super();
		Utils.initAction(this, "action.pinParticipant."); //$NON-NLS-1$
	}

	public void setParticipant(ISynchronizeParticipant participant) {
		if (this.participant != null) {
			this.participant.removePropertyChangeListener(this);
		}
		this.participant = participant;
		setEnabled(participant != null);
		if (participant != null) {
			participant.addPropertyChangeListener(this);
		}
		updateState();
	}

	private void updateState() {
		setChecked(participant != null && participant.isPinned());
	}

	@Override
	public void run() {
		if (participant != null) {
			try {
				PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
					participant.setPinned(!participant.isPinned());
					updateState();
				});
			} catch (InvocationTargetException e) {
				Utils.handle(e);
			} catch (InterruptedException e) {
				// Cancelled. Just ignore
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getSource() == participant) {
			updateState();
		}
	}

	public void dispose() {
		if (participant != null) {
			participant.removePropertyChangeListener(this);
		}
	}
}
