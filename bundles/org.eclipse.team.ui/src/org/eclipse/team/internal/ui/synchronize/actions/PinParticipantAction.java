/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
				PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException, InterruptedException {
						participant.setPinned(!participant.isPinned());
						updateState();
					}
				});
			} catch (InvocationTargetException e) {
				Utils.handle(e);
			} catch (InterruptedException e) {
				// Cancelled. Just ignore
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
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
