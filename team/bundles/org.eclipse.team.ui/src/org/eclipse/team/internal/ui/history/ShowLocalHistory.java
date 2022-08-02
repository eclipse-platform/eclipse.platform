/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.history;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;

public class ShowLocalHistory extends ActionDelegate implements IObjectActionDelegate {

	private IStructuredSelection fSelection;
	private IWorkbenchPart targetPart;

	@Override
	public void run(IAction action) {
		IFileState states[]= getLocalHistory();
		if (states == null || states.length == 0)
			return;
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
				final IResource resource = (IResource) fSelection.getFirstElement();
				Runnable r = () -> {
					IHistoryView view = TeamUI.showHistoryFor(TeamUIPlugin.getActivePage(), resource,  LocalHistoryPageSource.getInstance());
					IHistoryPage page = view.getHistoryPage();
					if (page instanceof LocalHistoryPage){
						LocalHistoryPage historyPage = (LocalHistoryPage) page;
						historyPage.setClickAction(isCompare());
					}
				};
				TeamUIPlugin.getStandardDisplay().asyncExec(r);
			});
		} catch (InvocationTargetException exception) {
			ErrorDialog.openError(getShell(), null, null, new Status(IStatus.ERROR, TeamUIPlugin.PLUGIN_ID, IStatus.ERROR, TeamUIMessages.ShowLocalHistory_1, exception.getTargetException()));
		} catch (InterruptedException exception) {
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			fSelection= (IStructuredSelection) sel;
		}
	}
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

	protected Shell getShell() {
		if (targetPart != null)
			return targetPart.getSite().getShell();
		return TeamUIPlugin.getActivePage().getActivePart().getSite().getShell();
	}

	protected boolean isCompare() {
		return false;
	}

	public IStructuredSelection getSelection() {
		return fSelection;
	}

	protected IFileState[] getLocalHistory() {
		final IFile file = (IFile) getSelection().getFirstElement();
		IFileState states[];
		try {
			states= file.getHistory(null);
		} catch (CoreException ex) {
			MessageDialog.openError(getShell(), getPromptTitle(), ex.getMessage());
			return null;
		}

		if (states == null || states.length <= 0) {
			MessageDialog.openInformation(getShell(), getPromptTitle(), TeamUIMessages.ShowLocalHistory_0);
			return states;
		}
		return states;
	}

	protected String getPromptTitle() {
		return TeamUIMessages.ShowLocalHistory_2;
	}

}
