/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.datatransfer;

import java.util.List;

import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Exports currently selected Eclipse Java project as an Ant buildfile.
 */
public class AntBuildfileExportWizard extends Wizard implements IExportWizard {
	private IStructuredSelection fSelection;
	private AntBuildfileExportPage fMainPage;

	/**
	 * Creates buildfile.
	 */
	@Override
	public boolean performFinish() {
		return fMainPage.generateBuildfiles();
	}

	@Override
	public void addPages() {
		fMainPage = new AntBuildfileExportPage();
		List<IJavaProject> projects = fSelection.toList();
		fMainPage.setSelectedProjects(projects);
		addPage(fMainPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(DataTransferMessages.AntBuildfileExportWizard_0);
		setDefaultPageImageDescriptor(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_EXPORT_WIZARD_BANNER));
		setNeedsProgressMonitor(true);
		fSelection = selection;
	}
}
