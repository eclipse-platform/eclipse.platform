/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.compare.ICompareNavigator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.actions.ExpandAllAction;
import org.eclipse.team.internal.ui.synchronize.actions.NavigateAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;
import org.eclipse.ui.IActionBars;

/**
 * Action group that provide expand, collapse and navigation actions.
 */
public class NavigationActionGroup extends SynchronizePageActionGroup {

	private ExpandAllAction expandAllAction;
	private Action collapseAll;
	private NavigateAction gotoNext;
	private NavigateAction gotoPrevious;

	@Override
	public void initialize(ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);
		final Viewer viewer = configuration.getPage().getViewer();
		if (viewer instanceof AbstractTreeViewer) {

			expandAllAction = new ExpandAllAction((AbstractTreeViewer) viewer);
			Utils.initAction(expandAllAction, "action.expandAll."); //$NON-NLS-1$

			collapseAll = new Action() {
				@Override
				public void run() {
					if (viewer.getControl().isDisposed() || !(viewer instanceof AbstractTreeViewer))
						return;
					viewer.getControl().setRedraw(false);
					((AbstractTreeViewer)viewer).collapseToLevel(viewer.getInput(), AbstractTreeViewer.ALL_LEVELS);
					viewer.getControl().setRedraw(true);
				}
			};
			Utils.initAction(collapseAll, "action.collapseAll."); //$NON-NLS-1$

			ICompareNavigator nav = (ICompareNavigator)configuration.getProperty(SynchronizePageConfiguration.P_NAVIGATOR);
			if (nav != null) {
				gotoNext = new NavigateAction(configuration, true /*next*/);
				gotoPrevious = new NavigateAction(configuration, false /*previous*/);
			}
		}
	}
	@Override
	public void fillContextMenu(IMenuManager manager) {
		if (manager == null || expandAllAction == null)
			return;

		if (manager.find(TeamUIPlugin.REMOVE_FROM_VIEW_ACTION_ID) != null)
			manager.insertBefore(TeamUIPlugin.REMOVE_FROM_VIEW_ACTION_ID, expandAllAction);
		else
			appendToGroup(manager, ISynchronizePageConfiguration.NAVIGATE_GROUP, expandAllAction);
	}
	@Override
	public void fillActionBars(IActionBars actionBars) {
		IToolBarManager manager = actionBars.getToolBarManager();
		appendToGroup(manager, ISynchronizePageConfiguration.NAVIGATE_GROUP, collapseAll);
		if (gotoNext != null)
			appendToGroup(manager, ISynchronizePageConfiguration.NAVIGATE_GROUP, gotoNext);
		if (gotoPrevious != null)
			appendToGroup(manager, ISynchronizePageConfiguration.NAVIGATE_GROUP, gotoPrevious);
	}
}
