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
package org.eclipse.team.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.ui.TeamImages;

/**
 * This is a convenience class for creating wizards for use with the
 * <code>org.eclipse.team.ui.synchronizeWizard</code> extension point.
 *
 * @since 3.2
 */
public abstract class ParticipantSynchronizeWizard extends Wizard {

	private WizardPage selectionPage;
	private IWizard importWizard;

	/**
	 * Create the wizard.
	 */
	protected ParticipantSynchronizeWizard() {
		setDefaultPageImageDescriptor(TeamImages.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE));
		setNeedsProgressMonitor(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getWindowTitle()
	 */
	@Override
	public String getWindowTitle() {
		return TeamUIMessages.GlobalRefreshSubscriberPage_0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		if (getRootResources().length == 0) {
			importWizard = getImportWizard();
			if (importWizard != null){
				importWizard.setContainer(getContainer());
				importWizard.addPages();
				IWizardPage startingPage = importWizard.getStartingPage();
				if (startingPage != null) {
					startingPage.setTitle(NLS.bind(TeamUIMessages.SubscriberParticipantWizard_0, new String[] { getPageTitle() }));
					startingPage.setDescription(NLS.bind(TeamUIMessages.SubscriberParticipantWizard_1, new String[] { importWizard.getWindowTitle() }));
				}
			}
		} else {
			selectionPage = createScopeSelectionPage();
			selectionPage.setTitle(NLS.bind(TeamUIMessages.GlobalRefreshSubscriberPage_1, new String[] { getPageTitle() }));
			selectionPage.setMessage(TeamUIMessages.GlobalRefreshSubscriberPage_2);
			addPage(selectionPage);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		if (importWizard != null) {
			return importWizard.performFinish();
		} else {
			createParticipant();
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if(importWizard != null ) {
			return importWizard.getNextPage(page);
		}
		return super.getNextPage(page);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performCancel()
	 */
	@Override
	public boolean performCancel() {
		if(importWizard != null) {
			return importWizard.performCancel();
		}
		return super.performCancel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	@Override
	public boolean canFinish() {
		if(importWizard != null) {
			return importWizard.canFinish();
		}
		return super.canFinish();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getStartingPage()
	 */
	@Override
	public IWizardPage getStartingPage() {
		if(importWizard != null) {
			return importWizard.getStartingPage();
		}
		return super.getStartingPage();
	}

	/**
	 * Return the page title for the page used by this wizard.
	 * @return the page title for the page used by this wizard
	 */
	protected abstract String getPageTitle();

	/**
	 * Return a wizard that can be used to populate the workspace
	 * if there are no resources returned from {@link #getRootResources()}.
	 * @return a wizard that can be used to populate the workspace
	 */
	protected abstract IWizard getImportWizard();

	/**
	 * Return the resources that are the roots of the resource
	 * trees that can be considered for inclusion.
	 * @return the resources that are the roots of the resource
	 * trees that can be considered for inclusion
	 */
	protected abstract IResource[] getRootResources();

	/**
	 * Create the page which allows the user to select the scope
	 * for the operation.
	 * @return the page which allows the user to select the scope
	 * for the operation
	 */
	protected abstract WizardPage createScopeSelectionPage();

	/**
	 * Method called from {@link #performFinish()} to create
	 * a participant. This participant will be added to the
	 * Synchronize view.
	 */
	protected abstract void createParticipant();

}
