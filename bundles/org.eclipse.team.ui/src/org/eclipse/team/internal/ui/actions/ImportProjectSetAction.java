/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - suppress frequent notifications during import, bug 311526
 *******************************************************************************/
package org.eclipse.team.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.ProjectSetImporter;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.wizards.ImportProjectSetOperation;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.statushandlers.StatusManager;

public class ImportProjectSetAction extends ActionDelegate implements IObjectActionDelegate {

	private IStructuredSelection fSelection;

	@Override
	public void run(IAction action) {
		final Shell shell= Display.getDefault().getActiveShell();
		try {
			new ProgressMonitorDialog(shell).run(true, true, new WorkspaceModifyOperation(null) {
				@Override
				protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
					Iterator iterator= fSelection.iterator();
					while (iterator.hasNext()) {
						IFile file = (IFile) iterator.next();
						if (isRunInBackgroundPreferenceOn()) {
							ImportProjectSetOperation op = new ImportProjectSetOperation(null, file.getLocation().toString(), new IWorkingSet[0]);
							op.run();
						} else {
							ProjectSetImporter.importProjectSet(file.getLocation().toString(), shell, monitor);
						}
					}
				}
			});
		} catch (InvocationTargetException exception) {
			StatusManager.getManager().handle(
					new Status(IStatus.ERROR, TeamUIPlugin.PLUGIN_ID,
							IStatus.ERROR,
							TeamUIMessages.ImportProjectSetAction_0,
							exception.getTargetException()),
					StatusManager.LOG | StatusManager.SHOW);
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
	}

	private static boolean isRunInBackgroundPreferenceOn() {
		return TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(
				IPreferenceIds.RUN_IMPORT_IN_BACKGROUND);
	}

}
