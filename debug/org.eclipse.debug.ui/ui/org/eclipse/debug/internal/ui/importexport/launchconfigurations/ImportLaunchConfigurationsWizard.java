/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.importexport.launchconfigurations;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * This class provides a wizard for importing launch configurations from the local
 * file system into the current workspace.
 *
 * It allows the suer to specify if the configuration should be shared or local
 * once imported
 *
 * @since 3.4.0
 */
public class ImportLaunchConfigurationsWizard extends Wizard implements IImportWizard {

	private String EXPORT_DIALOG_SETTINGS = "ExportLaunchConfigurations"; //$NON-NLS-1$

	/**
	 * Constructor
	 */
	public ImportLaunchConfigurationsWizard() {
		super();
		IDialogSettings workbenchSettings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(ImportLaunchConfigurationsWizard.class))
				.getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection(EXPORT_DIALOG_SETTINGS);
		if (section == null) {
			section = workbenchSettings.addNewSection(EXPORT_DIALOG_SETTINGS);
		}
		setDialogSettings(section);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		return ((ImportLaunchConfigurationsWizardPage)getStartingPage()).finish();
	}

	@Override
	public void addPages() {
		addPage(new ImportLaunchConfigurationsWizardPage());
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(WizardMessages.ImportLaunchConfigurationsWizard_0);
		setNeedsProgressMonitor(true);
	}

}
