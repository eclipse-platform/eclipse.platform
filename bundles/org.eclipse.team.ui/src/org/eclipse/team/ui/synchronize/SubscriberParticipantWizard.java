/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.team.internal.ui.synchronize.GlobalRefreshResourceSelectionPage;
import org.eclipse.team.ui.TeamUI;

/**
 * This is a convenience class for creating wizards for use with the
 * <code>org.eclipse.team.ui.synchronizeWizard</code> extension point
 * that create a {@link SubscriberParticipant}.
 *
 * @since 3.2
 */
public abstract class SubscriberParticipantWizard extends ParticipantSynchronizeWizard {

	private GlobalRefreshResourceSelectionPage selectionPage;

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ParticipantSynchronizeWizard#createScopeSelectionPage()
	 */
	@Override
	protected final WizardPage createScopeSelectionPage() {
		selectionPage = new GlobalRefreshResourceSelectionPage(getRootResources());
		return selectionPage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ParticipantSynchronizeWizard#createParticipant()
	 */
	@Override
	protected final void createParticipant() {
		IResource[] resources = selectionPage.getRootResources();
		if (resources != null && resources.length > 0) {
			SubscriberParticipant participant = createParticipant(selectionPage.getSynchronizeScope());
			TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[]{participant});
			// We don't know in which site to show progress because a participant could actually be shown in multiple sites.
			participant.run(null /* no site */);
		}
	}

	/**
	 * Method called from {@link #createParticipant()} to create a
	 * {@link SubscriberParticipant} for the given scope.
	 *
	 * @param scope the selected scope
	 * @return a synchronize participant that will be added to the Synchronize
	 *         view
	 */
	protected abstract SubscriberParticipant createParticipant(ISynchronizeScope scope);

}
