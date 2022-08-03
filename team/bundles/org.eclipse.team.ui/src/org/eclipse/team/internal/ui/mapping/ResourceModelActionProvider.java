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
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.internal.ui.synchronize.actions.RefactorActionGroup;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.mapping.SynchronizationActionProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.ui.navigator.INavigatorContentService;

/**
 * This is the synchronization action handler for the resources model
 */
public class ResourceModelActionProvider extends SynchronizationActionProvider {

	private RefactorActionGroup refactorActions;

	public ResourceModelActionProvider() {
	}

	@Override
	protected void initialize() {
		super.initialize();
		// Register the merge, overwrite and mark-as-merged handlers
		ResourceMergeHandler mergeHandler = new ResourceMergeHandler(
				(ISynchronizePageConfiguration)getExtensionStateModel().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_PAGE_CONFIGURATION),
				false /* overwrite */);
		registerHandler(MERGE_ACTION_ID, mergeHandler);
		ResourceMergeHandler overwriteHandler = new ResourceMergeHandler(
				(ISynchronizePageConfiguration)getExtensionStateModel().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_PAGE_CONFIGURATION),
				true /* overwrite */);
		registerHandler(OVERWRITE_ACTION_ID, overwriteHandler);
		ResourceMarkAsMergedHandler markAsMergedHandler = new ResourceMarkAsMergedHandler(
				(ISynchronizePageConfiguration)getExtensionStateModel().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_PAGE_CONFIGURATION));
		registerHandler(MARK_AS_MERGE_ACTION_ID, markAsMergedHandler);

		ICommonViewerSite cvs = getActionSite().getViewSite();
		ISynchronizePageConfiguration configuration = getSynchronizePageConfiguration();
		if (cvs instanceof ICommonViewerWorkbenchSite && configuration != null) {
			ICommonViewerWorkbenchSite cvws = (ICommonViewerWorkbenchSite) cvs;
			final IWorkbenchPartSite wps = cvws.getSite();
			if (wps instanceof IViewSite) {
				refactorActions = new RefactorActionGroup(configuration.getSite(), getNavigatorContentService(configuration));
			}
		}
	}

	private INavigatorContentService getNavigatorContentService(ISynchronizePageConfiguration configuration) {
		Viewer v = configuration.getPage().getViewer();
		if (v instanceof CommonViewer) {
			CommonViewer cv = (CommonViewer) v;
			return cv.getNavigatorContentService();
		}
		return null;
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		if (refactorActions != null) refactorActions.fillActionBars(actionBars);
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		IContributionItem editGroup = menu.find(ISynchronizePageConfiguration.EDIT_GROUP);
		if (refactorActions != null && editGroup != null) {
			refactorActions.fillContextMenu(menu, editGroup.getId());
		}
	}

	@Override
	public void updateActionBars() {
		super.updateActionBars();
		if (refactorActions != null) refactorActions.updateActionBars();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (refactorActions != null) refactorActions.dispose();
	}

	@Override
	public void setContext(ActionContext context) {
		super.setContext(context);
		if (refactorActions != null) refactorActions.setContext(context);
	}
}
