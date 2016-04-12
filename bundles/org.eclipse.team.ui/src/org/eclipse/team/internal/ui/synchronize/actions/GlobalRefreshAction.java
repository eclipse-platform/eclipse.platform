/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.core.commands.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.SynchronizeView;
import org.eclipse.team.internal.ui.wizards.GlobalSynchronizeWizard;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * A global refresh action that allows the user to select the participant to refresh 
 * or the default action is to refresh the last selected participant. Participants are 
 * only listed if they support
 * <p>
 * This action is normally associated with the Team action set and is enabled by default
 * in the Team Synchronizing perspective.
 * </p>
 * @since 3.0
 */
public class GlobalRefreshAction extends Action implements IMenuCreator, IWorkbenchWindowPulldownDelegate, ISynchronizeParticipantListener {

	public final static String NO_DEFAULT_PARTICPANT = "none"; //$NON-NLS-1$
	
	private MenuManager menuManager;
	private Action synchronizeAction;
	private IWorkbenchWindow window;
	private IAction actionProxy;
	private IHandlerActivation syncAll;
	private IHandlerActivation syncLatest;

	class RefreshParticipantAction extends Action {
		private ISynchronizeParticipantReference participant;

		@Override
		public void run() {
			TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCHRONIZING_DEFAULT_PARTICIPANT, participant.getId());
			TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCHRONIZING_DEFAULT_PARTICIPANT_SEC_ID, participant.getSecondaryId());
			GlobalRefreshAction.this.run(participant);
		}

		public RefreshParticipantAction(int prefix, ISynchronizeParticipantReference participant) {
			super("&" + prefix + " " + Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, participant.getDisplayName())); //$NON-NLS-1$ //$NON-NLS-2$
			this.participant = participant;
			setImageDescriptor(participant.getDescriptor().getImageDescriptor());
		}
	}

	public GlobalRefreshAction() {
		// Nothing to do
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#dispose()
	 */
	@Override
	public void dispose() {
		if(menuManager != null) {
			menuManager.dispose();
		}
		
		// participant listener
		TeamUI.getSynchronizeManager().removeSynchronizeParticipantListener(this);
		
		// handlers
		if (window != null) {
			IHandlerService hs = (IHandlerService)window.getService(IHandlerService.class);
			if (hs != null) {
				if (syncAll != null)
					hs.deactivateHandler(syncAll);
				if (syncLatest != null)
					hs.deactivateHandler(syncLatest);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Menu)
	 */
	@Override
	public Menu getMenu(Menu parent) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public Menu getMenu(Control parent) {
		Menu fMenu = null;
		if (menuManager == null) {
			menuManager = new MenuManager();
			fMenu = menuManager.createContextMenu(parent);
			menuManager.removeAll();
			ISynchronizeParticipantReference[] participants = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
			for (int i = 0; i < participants.length; i++) {
				ISynchronizeParticipantReference description = participants[i];
				Action action = new RefreshParticipantAction(i + 1, description);
				menuManager.add(action);
			}
			if (participants.length > 0)
				menuManager.add(new Separator());
			menuManager.add(synchronizeAction);
		} else {
			fMenu = menuManager.getMenu();
		}
		return fMenu;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;

		synchronizeAction = new Action(TeamUIMessages.GlobalRefreshAction_4) { 
			@Override
			public void run() {
				IWizard wizard = new GlobalSynchronizeWizard();
				WizardDialog dialog = new WizardDialog(GlobalRefreshAction.this.window.getShell(), wizard);
				dialog.open();
			}
		};
		synchronizeAction.setImageDescriptor(TeamImages.getImageDescriptor(ITeamUIImages.IMG_SYNC_VIEW));
		synchronizeAction.setActionDefinitionId("org.eclipse.team.ui.synchronizeAll"); //$NON-NLS-1$
		
		IHandlerService hs = (IHandlerService)window.getService(IHandlerService.class);
		if (hs != null) {
			// hook up actions to the commands
			IHandler handler = new AbstractHandler() {
				@Override
				public Object execute(ExecutionEvent event)
						throws ExecutionException {
					synchronizeAction.run();
					return null;
				}
			};
			syncAll = hs.activateHandler("org.eclipse.team.ui.synchronizeAll", handler); //$NON-NLS-1$
					
			handler = new AbstractHandler() {
				@Override
				public Object execute(ExecutionEvent event)
						throws ExecutionException {
					run();
					return null;
				}
			};
	        syncLatest = hs.activateHandler("org.eclipse.team.ui.synchronizeLast", handler);	 //$NON-NLS-1$
		}
		setMenuCreator(this);
		TeamUI.getSynchronizeManager().addSynchronizeParticipantListener(this);
	}

	@Override
	public void run() {
		String id = TeamUIPlugin.getPlugin().getPreferenceStore().getString(IPreferenceIds.SYNCHRONIZING_DEFAULT_PARTICIPANT);
		String secondaryId = TeamUIPlugin.getPlugin().getPreferenceStore().getString(IPreferenceIds.SYNCHRONIZING_DEFAULT_PARTICIPANT_SEC_ID);
		ISynchronizeParticipantReference participant = TeamUI.getSynchronizeManager().get(id, secondaryId);
		if (participant != null) {
			run(participant);
		} else {
			synchronizeAction.run();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		run();
		actionProxy = action;
		updateTooltipText();
	}
		
	private void run(ISynchronizeParticipantReference participant) {
		ISynchronizeParticipant p;
		try {
			p = participant.getParticipant();
			p.run(null /* no workbench part */);
			updateTooltipText();
		} catch (TeamException e) {
			Utils.handle(e);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeParticipantListener#participantsAdded(org.eclipse.team.ui.sync.ISynchronizeParticipant[])
	 */
	@Override
	public void participantsAdded(ISynchronizeParticipant[] consoles) {
		Display display = TeamUIPlugin.getStandardDisplay();
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if(menuManager != null) {
					menuManager.dispose();
					menuManager = null;
				}
				updateTooltipText();
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeParticipantListener#participantsRemoved(org.eclipse.team.ui.sync.ISynchronizeParticipant[])
	 */
	@Override
	public void participantsRemoved(ISynchronizeParticipant[] consoles) {
		Display display = TeamUIPlugin.getStandardDisplay();
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if(menuManager != null) {
					menuManager.dispose();
					menuManager = null;
				}
				updateTooltipText();
			}
		});
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
	 *           org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		actionProxy = action;
	}
	
	protected void updateTooltipText() {
		if (actionProxy != null) {
			String id = TeamUIPlugin.getPlugin().getPreferenceStore().getString(IPreferenceIds.SYNCHRONIZING_DEFAULT_PARTICIPANT);
			String secondaryId = TeamUIPlugin.getPlugin().getPreferenceStore().getString(IPreferenceIds.SYNCHRONIZING_DEFAULT_PARTICIPANT_SEC_ID);
			if (!id.equals(NO_DEFAULT_PARTICPANT)) {
				ISynchronizeParticipantReference ref = TeamUI.getSynchronizeManager().get(id, secondaryId);
				if (ref != null) {
					actionProxy.setToolTipText(NLS.bind(TeamUIMessages.GlobalRefreshAction_5, new String[] { ref.getDisplayName() })); 
					return;
				}
			}
			actionProxy.setToolTipText(TeamUIMessages.GlobalRefreshAction_4); 
		}
	}
}
