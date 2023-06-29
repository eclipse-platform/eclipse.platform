/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.importexport.breakpoints;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * <p>
 * This class provides a wizard for exporting breakpoints.
 * It serves dual purpose, in that it is used by the platform import/export wizard,
 * but it can also be used as a standalone wizard.
 * </p>
 * <p>
 * Example:
 * </p>
 * <pre>
 * IWizard wiz = new WizardExportBreakpoints();
 * wiz.init(workbench, selection);
 * WizardDialog wizdialog = new WizardDialog(shell, wiz);
 * wizdialog.open();
 * </pre>
 *
 * This class uses <code>WizardExportBreakpointsPage</code>
 *
 * @since 3.2
 *
 */
public class WizardExportBreakpoints extends Wizard implements IExportWizard {

	/*
	 * The main page
	 */
	private WizardExportBreakpointsPage fMainPage = null;

	/**
	 * The existing selection
	 */
	private IStructuredSelection fSelection = null;

	/**
	 * Identifier for dialog settings section for the export wizard.
	 */
	private static final String EXPORT_DIALOG_SETTINGS = "BreakpointExportSettings"; //$NON-NLS-1$

	/**
	 * This is the default constructor
	 */
	public WizardExportBreakpoints() {
		super();
		IDialogSettings workbenchSettings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(WizardExportBreakpoints.class)).getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection(EXPORT_DIALOG_SETTINGS);
		if (section == null) {
			section = workbenchSettings.addNewSection(EXPORT_DIALOG_SETTINGS);
		}
		setDialogSettings(section);
	}

	@Override
	public void addPages() {
		super.addPages();
		fMainPage = new WizardExportBreakpointsPage(ImportExportMessages.WizardExportBreakpoints_0, fSelection);
		addPage(fMainPage);
	}

	@Override
	public void dispose() {
		super.dispose();
		fMainPage = null;
		fSelection = null;
	}

	@Override
	public boolean performFinish() {
		return fMainPage.finish();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		fSelection = selection;
		setWindowTitle(ImportExportMessages.WizardExportBreakpoints_0);
		setNeedsProgressMonitor(true);
	}
}
