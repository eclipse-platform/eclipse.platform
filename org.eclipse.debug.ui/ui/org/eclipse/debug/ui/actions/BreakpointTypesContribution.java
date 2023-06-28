/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.ui.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.debug.internal.ui.actions.ToggleBreakpointsTargetManager;
import org.eclipse.debug.internal.ui.actions.breakpoints.Messages;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Breakpoint ruler pop-up action that creates a sub-menu to select the
 * currently active breakpoint type. This menu contribution can be added to an
 * editor with the <code>org.eclipse.ui.menus</code> extension point. The
 * breakpoint types are calculated based on the toggle breakpoint target
 * factories contributed through the
 * <code>toggleBreakpointsTargetFactories</code> extension point.
 * <p>
 * Following is example plug-in XML used to contribute this action to an
 * editor's vertical ruler context menu.
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.ui.menus"&gt;
 *   &lt;menuContribution
 *     locationURI="popup:#CEditorRulerContext?after=additions"
 *     id="example.RulerPopupActions"&gt;
 *       &lt;menu\
 *         id="breakpointTypes"
 *         label="Toggle Breakpoint"&gt;
 *         &lt;dynamic\
 *           id="example.rulerContextMenu.breakpointTypesAction"&gt;
 *           class="org.eclipse.debug.ui.actions.BreakpointTypesContribution"
 *         menubarPath="additions"&gt;/
 *       &lt;/menu&gt;
 *   &lt;/menuContribution&gt;
 * </pre>
 *
 * <p>
 * Clients may refer to this class in plug-in XML. This class is not intended to
 * be sub-classed.
 * </p>
 *
 * @since 3.8
 * @noextend This class is not intended to be sub-classed by clients.
 */
public class BreakpointTypesContribution extends CompoundContributionItem implements IWorkbenchContribution {

	private class SelectTargetAction extends Action {
		private final Set<String> fPossibleIDs;
		private final String fID;

		SelectTargetAction(String name, Set<String> possibleIDs, String ID) {
			super(name, AS_RADIO_BUTTON);
			fID = ID;
			fPossibleIDs = possibleIDs;
		}

		@Override
		public void run() {
			if (isChecked()) {
				// Note: setPreferredTarget is not declared on the
				// IToggleBreakpontsTargetManager interface.
				ToggleBreakpointsTargetManager.getDefault().setPreferredTarget(fPossibleIDs, fID);
			}
		}
	}

	private IServiceLocator fServiceLocator;

	private static IContributionItem[] NO_BREAKPOINT_TYPES_CONTRIBUTION_ITEMS = new IContributionItem[] {
		new ContributionItem() {
			@Override
			public void fill(Menu menu, int index) {
				MenuItem item = new MenuItem(menu, SWT.NONE);
				item.setEnabled(false);
				item.setText(Messages.BreakpointTypesContribution_0);
			}

			@Override
			public boolean isEnabled() {
				return false;
			}
		}
	};

	@Override
	protected IContributionItem[] getContributionItems() {
		IWorkbenchPart part = null;
		ISelection selection = null;

		ISelectionService selectionService =
			fServiceLocator.getService(ISelectionService.class);
		if (selectionService != null) {
			selection = selectionService.getSelection();
		}
		IPartService partService = fServiceLocator.getService(IPartService.class);
		if (partService != null) {
			part = partService.getActivePart();
		}

		// If no part or selection, disable all.
		if (part == null || selection == null) {
			return NO_BREAKPOINT_TYPES_CONTRIBUTION_ITEMS;
		}

		// Get breakpoint toggle target IDs.
		IToggleBreakpointsTargetManager manager = DebugUITools.getToggleBreakpointsTargetManager();
		Set<String> enabledIDs = manager.getEnabledToggleBreakpointsTargetIDs(part, selection);
		String preferredId = manager.getPreferredToggleBreakpointsTargetID(part, selection);

		List<Action> actions = new ArrayList<>(enabledIDs.size());
		for (String id : enabledIDs) {
			Action action = new SelectTargetAction(manager.getToggleBreakpointsTargetName(id), enabledIDs, id);
			if (id.equals(preferredId)) {
				action.setChecked(true);
			}
			actions.add(action);
		}

		if ( enabledIDs.isEmpty() ) {
			return NO_BREAKPOINT_TYPES_CONTRIBUTION_ITEMS;
		}

		IContributionItem[] items = new IContributionItem[enabledIDs.size()];
		for (int i = 0; i < actions.size(); i++) {
			items[i] = new ActionContributionItem(actions.get(i));
		}
		return items;
	}

	@Override
	public void initialize(IServiceLocator serviceLocator) {
		fServiceLocator = serviceLocator;
	}
}
