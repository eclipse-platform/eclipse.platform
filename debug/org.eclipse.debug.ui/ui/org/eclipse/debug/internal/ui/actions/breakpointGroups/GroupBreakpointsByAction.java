/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.breakpointGroups;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointOrganizer;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointOrganizerManager;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class GroupBreakpointsByAction extends AbstractBreakpointsViewAction implements IMenuCreator {

	private IAction fAction= null;

	public GroupBreakpointsByAction() {
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
				for (MenuItem item : m.getItems()) {
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
		// determine which item should be checked
		IBreakpointOrganizer[] organizers = fView.getBreakpointOrganizers();

		boolean none = false;
		boolean advanced = false;
		IBreakpointOrganizer organizer = null;
		if (organizers == null || organizers.length == 0) {
			none = true;
		} else if (organizers.length > 1) {
			advanced = true;
		} else {
			organizer = organizers[0];
		}

		int accel = 1;
		// Add hard-coded action for flat breakpoints list
		IAction action = new GroupBreakpointsAction(null, fView);
		addAccel(accel, action, BreakpointGroupMessages.GroupBreakpointsByAction_0);
		accel++;
		action.setImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_VIEW_BREAKPOINTS));
		action.setChecked(none);
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(menu, -1);

		// Add actions for each contributed organizer
		List<IAction> actions = getActions(accel);
		accel = accel + actions.size();
		Iterator<IAction> actionIter = actions.iterator();
		while (actionIter.hasNext()) {
			GroupBreakpointsAction bpAction = (GroupBreakpointsAction) actionIter.next();
			bpAction.setChecked(bpAction.getOrganizer().equals(organizer));
			item= new ActionContributionItem(bpAction);
			item.fill(menu, -1);
		}

		// advanced action
		AdvancedGroupBreakpointsByAction advancedAction = new AdvancedGroupBreakpointsByAction(fView);
		addAccel(accel, advancedAction,BreakpointGroupMessages.GroupBreakpointsByAction_1);
		advancedAction.setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_HIERARCHICAL));
		advancedAction.setChecked(advanced);
		item= new ActionContributionItem(advancedAction);
		item.fill(menu, -1);
	}

	public List<IAction> getActions(int accel) {
		List<IAction> actions = new ArrayList<>();
		for (IBreakpointOrganizer organizer : BreakpointOrganizerManager.getDefault().getOrganizers()) {
			IAction action = new GroupBreakpointsAction(organizer, fView);
			addAccel(accel, action, organizer.getLabel());
			accel++;
			action.setImageDescriptor(organizer.getImageDescriptor());
			actions.add(action);
		}
		return actions;
	}

	private void addAccel(int accel, IAction action, String label) {
		StringBuilder actionLabel= new StringBuilder();
		if (accel != 10) {
			if (accel < 10) {
				// add the numerical accelerators 1 through 9
				actionLabel.append('&');
			}
			actionLabel.append(accel);
		} else {
			actionLabel.append("1&0"); //$NON-NLS-1$
		}
		accel++;
		actionLabel.append(' ');
		actionLabel.append(label);
		action.setText(actionLabel.toString());
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (action != fAction) {
			action.setMenuCreator(this);
			fAction= action;
		}
	}
}
