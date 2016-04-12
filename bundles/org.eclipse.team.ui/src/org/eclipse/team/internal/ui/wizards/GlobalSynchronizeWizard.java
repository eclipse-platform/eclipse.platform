/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.wizards;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantReference;
import org.eclipse.ui.IWorkbench;

/**
 * The wizard for synchronizing a synchronize participant.
 *
 * @since 3.0
 */
public class GlobalSynchronizeWizard extends Wizard {

    private final static String DIALOG_SETTINGS_SECTION= "SynchronizeWizard"; //$NON-NLS-1$

	protected IWorkbench workbench;
	protected GlobalRefreshWizardSelectionPage mainPage;
	protected ISynchronizeParticipantReference participant;

	public GlobalSynchronizeWizard() {
		setWindowTitle(TeamUIMessages.GlobalSynchronizeWizard_11);
		setDefaultPageImageDescriptor(TeamImages.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE));
		setForcePreviousAndNextButtons(true);
		setNeedsProgressMonitor(false);

		final IDialogSettings pluginSettings= TeamUIPlugin.getPlugin().getDialogSettings();
		IDialogSettings wizardSettings= pluginSettings.getSection(DIALOG_SETTINGS_SECTION);
		if (wizardSettings == null) {
		    pluginSettings.addNewSection(DIALOG_SETTINGS_SECTION);
		    wizardSettings= pluginSettings.getSection(DIALOG_SETTINGS_SECTION);
		}
		setDialogSettings(wizardSettings);
	}

	/*
	 * @see Wizard#addPages
	 */
	@Override
	public void addPages() {
		mainPage = new GlobalRefreshWizardSelectionPage();
		addPage(mainPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#canFinish()
	 */
	@Override
	public boolean canFinish() {
		// If we are on the first page, never allow finish unless the selected
		// wizard has no pages.
		if (getContainer().getCurrentPage() == mainPage) {
			if (mainPage.getSelectedWizard() != null && mainPage.getNextPage() == null) {
				return true;
			}
			return false;
		}
		return super.canFinish();
	}

	/*
	 * @see Wizard#performFinish
	 */
	@Override
	public boolean performFinish() {
		// If we are on the first page and the selected wizard has no pages then allow it to finish.
		if (getContainer().getCurrentPage() == mainPage) {
			IWizard noPageWizard = mainPage.getSelectedWizard();
			if (noPageWizard != null) {
				if (noPageWizard.canFinish()) {
				    mainPage.savePageSettings();
					return noPageWizard.performFinish();
				}
			}
		}
		mainPage.savePageSettings();
		return true;
	}
}
