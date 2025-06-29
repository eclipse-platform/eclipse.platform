/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class AntNewJavaProjectWizard extends Wizard implements INewWizard {

	private AntNewJavaProjectPage fMainPage;

	public AntNewJavaProjectWizard() {
		super();
	}

	@Override
	public void addPages() {
		super.addPages();
		fMainPage = new AntNewJavaProjectPage();
		addPage(fMainPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		setWindowTitle(DataTransferMessages.AntNewJavaProjectWizard_0);
		setDefaultPageImageDescriptor(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_WIZARD_BANNER));

	}

	@Override
	public boolean performCancel() {
		return true;
	}

	@Override
	public boolean performFinish() {
		return (fMainPage.createProject() != null);
	}
}
