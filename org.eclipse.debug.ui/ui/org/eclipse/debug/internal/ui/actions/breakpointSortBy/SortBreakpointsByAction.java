/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 *     Patrick Chuong (Texas Instruments) - Improve usability of the breakpoint view (Bug 238956)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.breakpointSortBy;

import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.breakpointGroups.AbstractBreakpointsViewAction;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 *
 */
public class SortBreakpointsByAction extends AbstractBreakpointsViewAction implements IMenuCreator {

	private IAction fAction= null;

	public SortBreakpointsByAction() {
	}

	@Override
	public void run(IAction action) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public Menu getMenu(Control parent) {
		// Never called
		return null;
	}

	@Override
	public Menu getMenu(Menu parent) {
		Menu menu = new Menu(parent);
		menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				Menu m = (Menu)e.widget;
				MenuItem[] items = m.getItems();
				for (MenuItem item : items) {
					item.dispose();
				}
				fillMenu(m);
			}
		});
		return menu;
	}

	/**
	 * Fill pull down menu with the "group by" options
	 */
	private void fillMenu(Menu menu) {

		// Add hard-coded sorting order by Name
		IAction action = new SortBreakpointsAction(fView, Messages.GroupBreakpointsSortByAction_Name, IInternalDebugUIConstants.BREAKPOINT_SORTING_ORDER_NAME);
		ActionContributionItem item = new ActionContributionItem(action);
		item.fill(menu, 0);

		// Add hard-coded sorting order by Creation Time
		action = new SortBreakpointsAction(fView, Messages.GroupBreakpointsSortByAction_CreationTime, IInternalDebugUIConstants.BREAKPOINT_SORTING_ORDER_CREATION_TIME);
		item = new ActionContributionItem(action);
		item.fill(menu, 1);

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (action != fAction) {
			action.setMenuCreator(this);
			fAction= action;
		}
	}
}
