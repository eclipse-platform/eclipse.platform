/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
 *     Sebastian Schmidt - bug 384460
 *******************************************************************************/

package org.eclipse.debug.internal.ui.importexport.breakpoints;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * <p>
 * Wizard for Importing breakpoints.
 * It serves a dual purpose, in that it is used by the platform import/export wizard,
 * but it can also be used as a standalone wizard.
 * </p>
 * <p>
 * Example:
 * </p>
 * <pre>
 * IWizard wiz = new WizardImportBreakpoints();
 * wiz.init(workbench, selection);
 * WizardDialog wizdialog = new WizardDialog(shell, wiz);
 * wizdialog.open();
 * </pre>
 *
 * This class uses <code>WizardImportBreakpointsPage</code> and
 * <code>WizardImportBreakpointsSelectionPage</code>.
 *
 * @since 3.2
 *
 */
public class WizardImportBreakpoints extends Wizard implements IImportWizard {

	/*
	 * The main page
	 */
	private WizardImportBreakpointsPage fMainPage;

	private WizardImportBreakpointsSelectionPage fSelectionPage;

	/**
	 * Identifier for dialog settings section for the import wizard.
	 */
	private static final String IMPORT_DIALOG_SETTINGS = "BreakpointImportSettings"; //$NON-NLS-1$

	/**
	 * This is the default constructor
	 */
	public WizardImportBreakpoints() {
		super();
		IDialogSettings workbenchSettings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(WizardImportBreakpoints.class)).getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection(IMPORT_DIALOG_SETTINGS);
		if (section == null) {
			section = workbenchSettings.addNewSection(IMPORT_DIALOG_SETTINGS);
		}
		setDialogSettings(section);
	}

	@Override
	public void addPages() {
		super.addPages();
		fMainPage = new WizardImportBreakpointsPage(ImportExportMessages.WizardImportBreakpoints_0);
		addPage(fMainPage);
		fSelectionPage = new WizardImportBreakpointsSelectionPage(ImportExportMessages.WizardImportBreakpointsSelectionPage_0);
		addPage(fSelectionPage);
	}

	@Override
	public void dispose() {
		super.dispose();
		fMainPage = null;
	}

	@Override
	public boolean performFinish() {
		List<IMarker> selectedBreakpoints = fSelectionPage.getSelectedMarkers();
		return fMainPage.finish(selectedBreakpoints);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(ImportExportMessages.WizardImportBreakpoints_0);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean needsProgressMonitor() {
		return true;
	}
}
