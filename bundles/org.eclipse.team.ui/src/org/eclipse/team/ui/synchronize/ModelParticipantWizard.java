/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.synchronize;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.team.internal.ui.mapping.ModelElementSelectionPage;
import org.eclipse.team.ui.TeamUI;

/**
 * This is a convenience class for creating wizards for use with the
 * <code>org.eclipse.team.ui.synchronizeWizard</code> extension point
 * that create a {@link ModelSynchronizeParticipant}.
 *
 * @since 3.2
 */
public abstract class ModelParticipantWizard extends ParticipantSynchronizeWizard {


	private ModelElementSelectionPage selectionPage;

	public ModelParticipantWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	@Override
	protected final void createParticipant() {
		ISynchronizeParticipant participant = createParticipant(selectionPage.getSelectedMappings());
		TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[]{participant});
		// We don't know in which site to show progress because a participant could actually be shown in multiple sites.
		participant.run(null /* no site */);
	}

	@Override
	protected final WizardPage createScopeSelectionPage() {
		selectionPage = new ModelElementSelectionPage(getRootResources());
		return selectionPage;
	}

	/**
	 * Method called from {@link #createParticipant()} to create a
	 * {@link ModelSynchronizeParticipant} for the given resource mappings.
	 *
	 * @param selectedMappings
	 *            the selected mappings that define the scope
	 * @return a synchronize participant that will be added to the Synchronize
	 *         view
	 */
	protected abstract ISynchronizeParticipant createParticipant(ResourceMapping[] selectedMappings);

}
