/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.ConfigureRefreshScheduleDialog;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;
import org.eclipse.ui.IActionBars;

/**
 * Provides the actions to be associated with a synchronize page
 */
public final class SubscriberActionContribution extends SynchronizePageActionGroup {

	// the changes viewer are contributed via the viewer and not the page.
	private Action configureSchedule;
	private Action refreshSelectionAction;
	private RemoveFromViewAction removeFromViewAction;
	private RestoreRemovedItemsAction restoreRemovedItemsAction;

	@Override
	public void initialize(final ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);
		final SubscriberParticipant participant = (SubscriberParticipant)configuration.getParticipant();
		final ISynchronizePageSite site = configuration.getSite();
		// toolbar
		if(participant.doesSupportSynchronize()) {

			refreshSelectionAction = new Action() {
				@Override
				public void run() {
					IStructuredSelection selection = (IStructuredSelection)site.getSelectionProvider().getSelection();
					IResource[] resources = Utils.getResources(selection.toArray());
					if (resources.length == 0) {
						// Refresh all participant resources
						resources = participant.getResources();
					}
					participant.refresh(resources, null, null, site.getWorkbenchSite()); //
				}
			};
			Utils.initAction(refreshSelectionAction, "action.refreshWithRemote."); //$NON-NLS-1$
			refreshSelectionAction.setActionDefinitionId("org.eclipse.team.ui.synchronizeLast"); //$NON-NLS-1$
			refreshSelectionAction.setId("org.eclipse.team.ui.synchronizeLast"); //$NON-NLS-1$

			configureSchedule = new Action() {
				@Override
				public void run() {
					ConfigureRefreshScheduleDialog d = new ConfigureRefreshScheduleDialog(
							site.getShell(), participant.getRefreshSchedule());
					d.setBlockOnOpen(false);
					d.open();
				}
			};
			Utils.initAction(configureSchedule, "action.configureSchedulel."); //$NON-NLS-1$
		}

		removeFromViewAction = new RemoveFromViewAction(configuration);
		restoreRemovedItemsAction = new RestoreRemovedItemsAction(configuration);
		appendToGroup(ISynchronizePageConfiguration.P_VIEW_MENU, ISynchronizePageConfiguration.SYNCHRONIZE_GROUP, restoreRemovedItemsAction);
	}

	@Override
	public void fillContextMenu(IMenuManager manager) {
		if (findGroup(manager, ISynchronizePageConfiguration.SYNCHRONIZE_GROUP) != null
			&& findGroup(manager, ISynchronizePageConfiguration.NAVIGATE_GROUP) != null) {
			// Place synchronize with navigate to save space
			appendToGroup(manager, ISynchronizePageConfiguration.NAVIGATE_GROUP, refreshSelectionAction);
			appendToGroup(manager, ISynchronizePageConfiguration.NAVIGATE_GROUP, removeFromViewAction);
		} else {
			appendToGroup(manager, ISynchronizePageConfiguration.SYNCHRONIZE_GROUP, refreshSelectionAction);
			appendToGroup(manager, ISynchronizePageConfiguration.SYNCHRONIZE_GROUP, removeFromViewAction);
		}
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		if(actionBars != null) {
			// view menu
			IMenuManager menu = actionBars.getMenuManager();
			if (findGroup(menu, ISynchronizePageConfiguration.SYNCHRONIZE_GROUP) != null
					&& findGroup(menu, ISynchronizePageConfiguration.PREFERENCES_GROUP) != null) {
				appendToGroup(menu, ISynchronizePageConfiguration.PREFERENCES_GROUP, configureSchedule);
			} else {
				appendToGroup(menu, ISynchronizePageConfiguration.SYNCHRONIZE_GROUP, configureSchedule);
			}
		}
	}
}
