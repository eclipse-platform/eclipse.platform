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
package org.eclipse.team.internal.ccvs.ui.mappings;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.actions.CommitAction;
import org.eclipse.team.internal.ccvs.ui.subscriber.WorkspaceSynchronizeParticipant;
import org.eclipse.team.internal.ccvs.ui.wizards.CheckoutWizard;
import org.eclipse.team.internal.ui.mapping.ModelElementSelectionPage;
import org.eclipse.team.internal.ui.synchronize.GlobalRefreshElementSelectionPage;
import org.eclipse.team.internal.ui.synchronize.GlobalRefreshResourceSelectionPage;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;

public class ModelSynchronizeWizard extends ParticipantSynchronizeWizard {

	private GlobalRefreshElementSelectionPage selectionPage;
	
	private boolean isShowModelSync() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_ENABLE_MODEL_SYNC);
	}
	
	@Override
	protected void createParticipant() {
		if (isShowModelSync()) {
			ISynchronizeParticipant participant = createParticipant(
					((ModelElementSelectionPage)selectionPage).getSelectedMappings());
			TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[]{participant});
			// We don't know in which site to show progress because a participant could actually be shown in multiple sites.
			participant.run(null /* no site */);
		} else {
			IResource[] resources = ((GlobalRefreshResourceSelectionPage)selectionPage).getRootResources();
			if (resources != null && resources.length > 0) {
				SubscriberParticipant participant = createParticipant(((GlobalRefreshResourceSelectionPage)selectionPage).getSynchronizeScope());
				TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[]{participant});
				// We don't know in which site to show progress because a participant could actually be shown in multiple sites.
				participant.run(null /* no site */);
			}
		}
	}
	
	@Override
	protected final WizardPage createScopeSelectionPage() {
		if (isShowModelSync())
			selectionPage = new ModelElementSelectionPage(getRootResources());
		else 
			selectionPage = new GlobalRefreshResourceSelectionPage(getRootResources());
		return selectionPage;
	}
	
	public static ISynchronizeParticipant createWorkspaceParticipant(ResourceMapping[] selectedMappings, Shell shell) {
		ISynchronizationScopeManager manager = WorkspaceSubscriberContext.createWorkspaceScopeManager(selectedMappings, true, CommitAction.isIncludeChangeSets(shell, CVSUIMessages.SyncAction_1));
		WorkspaceModelParticipant p =  new WorkspaceModelParticipant( 
				WorkspaceSubscriberContext.createContext(manager, ISynchronizationContext.THREE_WAY));
		return p;
	}
	
	public ModelSynchronizeWizard() {
		super();
		setNeedsProgressMonitor(isShowModelSync());
	}

	protected ISynchronizeParticipant createParticipant(ResourceMapping[] selectedMappings) {
		return createWorkspaceParticipant(selectedMappings, getShell());
	}

	protected SubscriberParticipant createParticipant(ISynchronizeScope scope) {
		// First check if there is an existing matching participant
		IResource[] roots = scope.getRoots();
		if (roots == null) {
			roots = CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().roots();
		}
		WorkspaceSynchronizeParticipant participant = (WorkspaceSynchronizeParticipant)SubscriberParticipant.getMatchingParticipant(WorkspaceSynchronizeParticipant.ID, roots);	
		// If there isn't, create one and add to the manager
		if (participant == null) {
			return new WorkspaceSynchronizeParticipant(scope);
		} else {
			return participant;
		}
	}
	
	@Override
	protected String getPageTitle() {
		ISynchronizeParticipantDescriptor desc = TeamUI.getSynchronizeManager().getParticipantDescriptor(WorkspaceModelParticipant.ID);
		if(desc != null) {
			return desc.getName();
		} else {
			return CVSUIMessages.CVSSynchronizeWizard_0; 
		}
	}

	@Override
	protected IWizard getImportWizard() {
		return new CheckoutWizard();
	}

	@Override
	protected IResource[] getRootResources() {
		return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().roots();
	}

}
