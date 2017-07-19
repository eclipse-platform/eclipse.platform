/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.AbstractHistoryCategory;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.progress.IProgressService;

public class OpenRevisionAction extends BaseSelectionListenerAction {

	private IStructuredSelection selection;
	private HistoryPage page;

	public OpenRevisionAction(String text, HistoryPage page) {
		super(text);
		this.page = page;
	}

	@Override
	public void run() {
		IStructuredSelection structSel = selection;

		if (structSel == null)
			return;

		Object[] objArray = structSel.toArray();

		for (int i = 0; i < objArray.length; i++) {
			Object tempRevision = objArray[i];
			//If not a revision, don't try opening
			if (tempRevision instanceof AbstractHistoryCategory)
				continue;

			final IFileRevision revision = (IFileRevision) tempRevision;
			if (revision == null || !revision.exists()) {
				MessageDialog.openError(page.getSite().getShell(), TeamUIMessages.OpenRevisionAction_DeletedRevTitle, TeamUIMessages.OpenRevisionAction_DeletedRevMessage);
			} else {
				IRunnableWithProgress runnable = monitor -> {
					try {
						Utils.openEditor(page.getSite().getPage(), revision, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}

				};

				IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
				try {
					progressService.run(false, false, runnable);
				} catch (InvocationTargetException e) {
					Utils.handleError(page.getSite().getShell(), e, TeamUIMessages.OpenRevisionAction_ErrorTitle, TeamUIMessages.OpenRevisionAction_ErrorMessage);
				} catch (InterruptedException e) {
				}
			}

		}
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		this.selection = selection;
		return shouldShow();
	}

	private boolean shouldShow() {
		IStructuredSelection structSel = selection;
		Object[] objArray = structSel.toArray();

		if (objArray.length == 0)
			return false;

		for (int i = 0; i < objArray.length; i++) {
			//Don't bother showing if this a category
			if (objArray[i] instanceof AbstractHistoryCategory)
				return false;

			IFileRevision revision = (IFileRevision) objArray[i];
			//check to see if any of the selected revisions are deleted revisions
			if (revision != null && !revision.exists())
				return false;
		}

		return true;
	}

}
