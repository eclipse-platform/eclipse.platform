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
 *******************************************************************************/
package org.eclipse.team.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.xml.sax.SAXException;

public class ProjectSetImportWizard extends Wizard implements IImportWizard {
	ImportProjectSetMainPage mainPage;

	public ProjectSetImportWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle(TeamUIMessages.ProjectSetImportWizard_Project_Set_1);
	}

	@Override
	public void addPages() {
		mainPage = new ImportProjectSetMainPage("projectSetMainPage", TeamUIMessages.ProjectSetImportWizard_Import_a_Project_Set_3, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_PROJECTSET_IMPORT_BANNER)); //$NON-NLS-1$
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		final boolean[] result = new boolean[] {false};
		try {
			ImportProjectSetOperation op;
			if (mainPage.getInputType() == ImportProjectSetMainPage.InputType_URL) {
				String psfContent = mainPage.getURLContents();
				if(psfContent==null){
					return false;
				}
				op = new ImportProjectSetOperation(
						mainPage.isRunInBackgroundOn() ? null : getContainer(),
						psfContent, mainPage.getUrl(), mainPage.getWorkingSets());
			} else {
				op = new ImportProjectSetOperation(
						mainPage.isRunInBackgroundOn() ? null : getContainer(),
						mainPage.getFileName(), mainPage.getWorkingSets());
			}
			op.run();
			result[0] = true;
		} catch (InterruptedException e) {
			return true;
		} catch (InvocationTargetException e) {
			Throwable target = e.getTargetException();
			if (target instanceof TeamException) {
				ErrorDialog.openError(getShell(), null, null, ((TeamException)target).getStatus());
				return false;
			}
			if (target instanceof RuntimeException) {
				throw (RuntimeException)target;
			}
			if (target instanceof Error) {
				throw (Error)target;
			}
			if (target instanceof SAXException) {
				ErrorDialog.openError(getShell(), null, null, new Status(IStatus.ERROR, TeamUIPlugin.ID, 0, NLS.bind(TeamUIMessages.ProjectSetImportWizard_2, new String[] { target.getMessage() }), target));
				return false;
			}
			ErrorDialog.openError(getShell(), null, null, new Status(IStatus.ERROR, TeamUIPlugin.ID, 0, NLS.bind(TeamUIMessages.ProjectSetImportWizard_3, new String[] { target.getMessage() }), target));
		}
		return result[0];
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// The code that finds "selection" is broken (it is always empty), so we
		// must dig for the selection in the workbench.
		PsfFilenameStore.getInstance().setDefaultFromSelection(workbench);
	}
}
