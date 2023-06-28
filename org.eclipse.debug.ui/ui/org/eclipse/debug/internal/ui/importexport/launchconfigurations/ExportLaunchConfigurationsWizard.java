/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *     Ian Pun (Red Hat Inc.) - Bug 518652
 *******************************************************************************/
package org.eclipse.debug.internal.ui.importexport.launchconfigurations;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * This class provides a wizard for exporting launch configurations to the local
 * file system
 *
 * @since 3.4.0
 */
public class ExportLaunchConfigurationsWizard extends Wizard implements IExportWizard {

	private String EXPORT_DIALOG_SETTINGS = "ExportLaunchConfigurations"; //$NON-NLS-1$
	private IStructuredSelection selectedElements;

	/**
	 * Constructor
	 */
	public ExportLaunchConfigurationsWizard() {
		super();
		IDialogSettings workbenchSettings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(ExportLaunchConfigurationsWizard.class))
				.getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection(EXPORT_DIALOG_SETTINGS);
		if (section == null) {
			section = workbenchSettings.addNewSection(EXPORT_DIALOG_SETTINGS);
		}
		setDialogSettings(section);
	}

	public ExportLaunchConfigurationsWizard(IStructuredSelection selection) {
		this();
		selectedElements = selection;
	}

	@Override
	public void addPages() {
		IWizardPage page;
		if (selectedElements == null) {
			page = new ExportLaunchConfigurationsWizardPage();
		} else {
			page = new ExportLaunchConfigurationsWizardPage(selectedElements);
		}
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		return ((ExportLaunchConfigurationsWizardPage)getStartingPage()).finish();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(WizardMessages.ExportLaunchConfigurationsWizard_0);
		setNeedsProgressMonitor(true);
	}
}
