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
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionContext;

/**
 * General synchronize page actions
 */
public class DefaultSynchronizePageActions extends SynchronizePageActionGroup {

	// Actions
	private OpenWithActionGroup openWithActions;
	private RefactorActionGroup refactorActions;
	private SyncViewerShowPreferencesAction showPreferences;

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#initialize(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	@Override
	public void initialize(ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);
		final ISynchronizePageSite site = configuration.getSite();
		IWorkbenchSite ws = site.getWorkbenchSite();
		if (ws instanceof IViewSite) {
			openWithActions = new OpenWithActionGroup(configuration, true);
			refactorActions = new RefactorActionGroup(site);
			configuration.setProperty(SynchronizePageConfiguration.P_OPEN_ACTION, new Action() {
				@Override
				public void run() {
					openWithActions.openInCompareEditor();
				}
			});
			showPreferences = new SyncViewerShowPreferencesAction(configuration);
		} else {
			// TODO: Add open menu action which opens in compare editor input
		}
	}

	/* (non-Javadoc)
     * @see org.eclipse.team.ui.synchronize.SynchronizePageActionGroup#fillActionBars(org.eclipse.ui.IActionBars)
     */
    @Override
	public void fillActionBars(IActionBars actionBars) {
        if (openWithActions != null) openWithActions.fillActionBars(actionBars);
        if (refactorActions != null) refactorActions.fillActionBars(actionBars);
        if (actionBars != null && showPreferences != null) {
        	IMenuManager menu = actionBars.getMenuManager();
        	appendToGroup(menu, ISynchronizePageConfiguration.PREFERENCES_GROUP, showPreferences);
        }
    }

	/* (non-Javadoc)
     * @see org.eclipse.ui.actions.ActionGroup#updateActionBars()
     */
    @Override
	public void updateActionBars() {
        if (openWithActions != null) openWithActions.updateActionBars();
        if (refactorActions != null) refactorActions.updateActionBars();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	public void fillContextMenu(IMenuManager manager) {

        final IContributionItem fileGroup = findGroup(manager, ISynchronizePageConfiguration.FILE_GROUP);
		if (openWithActions != null && fileGroup != null) {
			openWithActions.fillContextMenu(manager, fileGroup.getId());
		}

		final IContributionItem editGroup = findGroup(manager, ISynchronizePageConfiguration.EDIT_GROUP);
		if (refactorActions != null && editGroup != null) {
			refactorActions.fillContextMenu(manager, editGroup.getId());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizePageActionGroup#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (refactorActions != null) refactorActions.dispose();
		if (openWithActions != null) openWithActions.dispose();
	}

	/* (non-Javadoc)
     * @see org.eclipse.ui.actions.ActionGroup#setContext(org.eclipse.ui.actions.ActionContext)
     */
    @Override
	public void setContext(ActionContext context) {
        if (openWithActions != null) openWithActions.setContext(context);
        if (refactorActions != null) refactorActions.setContext(context);
    }
}
