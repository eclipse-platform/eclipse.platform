/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.jface.action.*;
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

public class SynchronizePageDropDownAction extends Action implements IMenuCreator, ISynchronizeParticipantListener {

	private ISynchronizeView fView;
	private Action synchronizeAction;
	private MenuManager menuManager;

	public SynchronizePageDropDownAction(ISynchronizeView view) {
		fView= view;
		Utils.initAction(this, "action.refreshSubscriber."); //$NON-NLS-1$
		
		synchronizeAction = new Action(TeamUIMessages.GlobalRefreshAction_4) { 
			@Override
			public void run() {
				IWizard wizard = new GlobalSynchronizeWizard();
				WizardDialog dialog = new WizardDialog(fView.getViewSite().getShell(), wizard);
				dialog.open();
			}
		};
		synchronizeAction.setImageDescriptor(TeamImages.getImageDescriptor(ITeamUIImages.IMG_SYNC_VIEW));
		synchronizeAction.setActionDefinitionId("org.eclipse.team.ui.synchronizeAll"); //$NON-NLS-1$
		setMenuCreator(this);		
		TeamUI.getSynchronizeManager().addSynchronizeParticipantListener(this);
		update();			
		fView.getSite().getKeyBindingService().registerAction(synchronizeAction);
		setActionDefinitionId("org.eclipse.team.ui.synchronizeLast"); //$NON-NLS-1$
		fView.getSite().getKeyBindingService().registerAction(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#dispose()
	 */
	@Override
	public void dispose() {
		if(menuManager != null) {
			menuManager.dispose();
			menuManager = null;
		}
		TeamUI.getSynchronizeManager().removeSynchronizeParticipantListener(this);	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Menu)
	 */
	@Override
	public Menu getMenu(Menu parent) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public Menu getMenu(Control parent) {
		Menu fMenu = null;
		if (menuManager == null) {
			menuManager = new MenuManager();
			fMenu = menuManager.createContextMenu(parent);
			final ISynchronizeParticipantReference[] participants = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
			addParticipantsToMenu(participants);
			if (participants.length > 0)
				menuManager.add(new Separator());
			menuManager.add(synchronizeAction);
			menuManager.update(true);
		} else {
			fMenu = menuManager.getMenu();
		}
		return fMenu;
	}

	protected void addParticipantsToMenu(ISynchronizeParticipantReference[] refs) {
		ISynchronizeParticipant current = fView.getParticipant();
		for (int i = 0; i < refs.length; i++) {
			ISynchronizeParticipantReference page = refs[i];
			Action action = new ShowSynchronizeParticipantAction(fView, page);  
			try {
				boolean isCurrent = page.getParticipant().equals(current);
				action.setChecked(isCurrent);
			} catch (TeamException e) {
				continue;
			}
			menuManager.add(action);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		ISynchronizeParticipant current = fView.getParticipant();
		if(current != null) {
			current.run(fView);
		} else {
			synchronizeAction.run();
		}
		update();
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
				update();
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
				update();
			}
		});
	}
	
	public void update() {
		ISynchronizeParticipant current = fView.getParticipant();
		ISynchronizeParticipantReference[] refs = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
		String text = null;
		if(current != null && refs.length > 0) {
			text = NLS.bind(TeamUIMessages.GlobalRefreshAction_5, new String[] { Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, current.getName()) }); 
			setToolTipText(text);
			setText(text);
		} else {
			text = TeamUIMessages.GlobalRefreshAction_4; 
			setToolTipText(text);
			setText(text);
		}
	}
}
