/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.*;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.ui.actions.RefreshAction;


/**
 * Action which synchronizes the current synchronization participant and locally refreshes the
 * selected resources.
 *
 * @since 3.7
 */
public class SynchronizeAndRefreshAction extends Action {

	private ISynchronizeView fView;

	public SynchronizeAndRefreshAction(ISynchronizeView view) {
		fView= view;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		ISynchronizeParticipant current = fView.getParticipant();
		if(current != null) {
			refreshLocal();
			current.run(fView);
		}
	}

	/**
	 * Refreshes the local resources that are selected in the view.
	 */
	private void refreshLocal() {
		final ISelectionProvider selectionProvider= fView.getSite().getSelectionProvider();
		if (selectionProvider == null)
			return;

		ISelection selection= selectionProvider.getSelection();
		if (!(selection instanceof IStructuredSelection))
			return;

		RefreshAction refreshAction= new RefreshAction(fView.getSite());
		if (selection.isEmpty())
			refreshAction.refreshAll();
		else {
			refreshAction.selectionChanged((IStructuredSelection)selection);
			refreshAction.run();
		}
	}
}
