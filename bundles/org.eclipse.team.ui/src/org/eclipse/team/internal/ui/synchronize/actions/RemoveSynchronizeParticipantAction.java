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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.model.WorkbenchPartLabelProvider;

/**
 * Action to remove the given participant from the synchronize manager.
 * @since 3.0
 */
public class RemoveSynchronizeParticipantAction extends Action {

	private final ISynchronizeView view;
	private boolean removeAll;

	public RemoveSynchronizeParticipantAction(ISynchronizeView view, boolean removeAll) {
		this.view = view;
		this.removeAll = removeAll;
		if (removeAll) {
			Utils.initAction(this, "action.removeAllPage."); //$NON-NLS-1$
		} else {
			Utils.initAction(this, "action.removePage."); //$NON-NLS-1$
		}
	}

	@Override
	public void run() {
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					if (removeAll) {
						removeAll();
					} else {
						removeCurrent();
					}
				}
			});
		} catch (InvocationTargetException e) {
			Utils.handle(e);
		} catch (InterruptedException e) {
			// Cancelled. Just ignore
		}
	}

	private void removeCurrent() {
		final ISynchronizeParticipant participant = view.getParticipant();
		if (participant != null) {
			final List dirtyModels = getDirtyModels(new ISynchronizeParticipant[] { participant });
			if (participant.isPinned() || !dirtyModels.isEmpty()) {
				final boolean[] keepGoing = new boolean[] { false };
				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						if (!dirtyModels.isEmpty()) {
							keepGoing[0] = promptToSave(dirtyModels);
						} else {
							keepGoing[0] = MessageDialog.openQuestion(
									view.getSite().getShell(),
									TeamUIMessages.RemoveSynchronizeParticipantAction_0,
									TeamUIMessages.RemoveSynchronizeParticipantAction_1);
						}

					}
				});
				if (!keepGoing[0]) {
					return;
				}
			}
			TeamUI.getSynchronizeManager().removeSynchronizeParticipants(new ISynchronizeParticipant[]{participant});
		}
	}

	private void removeAll() {
		ISynchronizeManager manager = TeamUI.getSynchronizeManager();
		ISynchronizeParticipantReference[] refs = manager.getSynchronizeParticipants();
		ArrayList removals = new ArrayList();
		for (int i = 0; i < refs.length; i++) {
			ISynchronizeParticipantReference reference = refs[i];
			ISynchronizeParticipant p;
			try {
				p = reference.getParticipant();
				if (! p.isPinned())
					removals.add(p);
			} catch (TeamException e) {
				// keep going
			}
		}
		ISynchronizeParticipant[] toRemove = (ISynchronizeParticipant[]) removals.toArray(new ISynchronizeParticipant[removals.size()]);
		final List dirtyModels = getDirtyModels(toRemove);
		if (!dirtyModels.isEmpty()) {
			final boolean[] keepGoing = new boolean[] { false };
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					if (!dirtyModels.isEmpty()) {
						keepGoing[0] = promptToSave(dirtyModels);
					}
				}
			});
			if (!keepGoing[0]) {
				return;
			}
		}
		manager.removeSynchronizeParticipants(toRemove);
	}

	private boolean promptToSave(List dirtyModels) {
        if (dirtyModels.size() == 1) {
        	Saveable model = (Saveable) dirtyModels.get(0);
			String message = NLS.bind(TeamUIMessages.RemoveSynchronizeParticipantAction_2, model.getName());
			// Show a dialog.
			String[] buttons = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL };
			MessageDialog d = new MessageDialog(
					view.getSite().getShell(), TeamUIMessages.RemoveSynchronizeParticipantAction_3,
				null, message, MessageDialog.QUESTION, buttons, 0);

			int choice = d.open();

			// Branch on the user choice.
			// The choice id is based on the order of button labels
			// above.
			switch (choice) {
			case 0: // yes
				break;
			case 1: // no
				return true;
			default:
			case 2: // cancel
				return false;
			}
        } else {
            ListSelectionDialog dlg = new ListSelectionDialog(
                    view.getSite().getShell(), dirtyModels,
                    new ArrayContentProvider(),
                    new WorkbenchPartLabelProvider(), TeamUIMessages.RemoveSynchronizeParticipantAction_4);
            dlg.setInitialSelections(dirtyModels.toArray());
            dlg.setTitle(TeamUIMessages.RemoveSynchronizeParticipantAction_5);

        	int result = dlg.open();
            //Just return false to prevent the operation continuing
            if (result == IDialogConstants.CANCEL_ID)
                return false;

            dirtyModels = Arrays.asList(dlg.getResult());
        }

	    // If the editor list is empty return.
	    if (dirtyModels.isEmpty())
	        return true;

		// Create save block.
	    final List finalModels = dirtyModels;
		IRunnableWithProgress progressOp = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) {
				monitor.beginTask(null, finalModels.size());
				for (Iterator i = finalModels.iterator(); i.hasNext();) {
					Saveable model = (Saveable) i.next();
					if (model.isDirty()) {
						try {
							model.doSave(new SubProgressMonitor(monitor, 1));
						} catch (CoreException e) {
							ErrorDialog.openError(view.getSite().getShell(), null, e.getMessage(), e.getStatus());
						}
					}
					if (monitor.isCanceled())
						break;
				}
				monitor.done();
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, progressOp);
		} catch (InvocationTargetException e) {
			Utils.handleError(view.getSite().getShell(), e, null, null);
			return false;
		} catch (InterruptedException e) {
			// Ignore
		}
		// TODO: How do we handle a cancel during save?
		return true;
	}

	private List getDirtyModels(ISynchronizeParticipant[] participants) {
		List result = new ArrayList();
		for (int i = 0; i < participants.length; i++) {
			ISynchronizeParticipant participant = participants[i];
			if (participant instanceof ModelSynchronizeParticipant) {
				ModelSynchronizeParticipant msp = (ModelSynchronizeParticipant) participant;
				Saveable s = msp.getActiveSaveable();
				if (s != null && s.isDirty())
					result.add(s);
			}
		}
		return result;
	}
}
