/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.variables.details;


import java.util.Set;

import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;

/**
 * Drop down action that displays the available detail panes for a selection.
 *
 * @since 3.3
 * @see IDetailPaneContainer
 */
public class AvailableDetailPanesAction extends Action implements IMenuCreator {

	private Menu fMenu;
	private Set<String> fAvailableIDs;
	private IDetailPaneContainer fDetailPaneContainer;

	/**
	 * Each entry in the menu will be of this type.  It represents one possible detail pane
	 * that the user can select.  If the user selects it, the display is changed to use that
	 * detail pane and the preferred detail pane map in the pane manager is updated.
	 *
	 * @see DetailPaneManager
	 * @since 3.3
	 */
	private class SetDetailPaneAction extends Action {

		private String fPaneID;
		private Set<String> fPossiblePaneIDs;

		public SetDetailPaneAction(String name, String paneID, Set<String> possiblePaneIDs) {
			super(name,AS_RADIO_BUTTON);
			fPaneID = paneID;
			fPossiblePaneIDs = possiblePaneIDs;
		}

		@Override
		public void run() {
			// Don't change panes unless the user is selecting a different pane than the one currently displayed
			if (isChecked() && !fDetailPaneContainer.getCurrentPaneID().equals(fPaneID)){
				DetailPaneManager.getDefault().setPreferredDetailPane(fPossiblePaneIDs, fPaneID);
				fDetailPaneContainer.refreshDetailPaneContents();
			}
		}

	}

	public AvailableDetailPanesAction(IDetailPaneContainer detailPaneContainer) {
		fDetailPaneContainer = detailPaneContainer;
		setText(DetailMessages.AvailableDetailPanesAction_0);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.VARIABLES_SELECT_DETAIL_PANE);

		setEnabled(false);
		setMenuCreator(this);
		init();
	}

	@Override
	public void run() {
	}

	@Override
	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fAvailableIDs.clear();
	}

	@Override
	public Menu getMenu(Control parent) {
		return null;
	}

	protected void addActionToMenu(Menu parent, IAction action) {
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(parent, -1);
	}

	@Override
	public Menu getMenu(Menu parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu= new Menu(parent);
		int i = 0;
		for (String currentID : fAvailableIDs) {
			StringBuilder name = new StringBuilder();
			//add the numerical accelerator
			i++;
			if (i < 9) {
				name.append('&');
				name.append(i);
				name.append(' ');
			}

			String typeName = DetailPaneManager.getDefault().getNameFromID(currentID);
			if (typeName != null && typeName.length() > 0){
				name.append(typeName);
			} else {
				name.append(currentID);
			}

			IAction action = new SetDetailPaneAction(name.toString(),currentID,fAvailableIDs);

			if (currentID.equals(fDetailPaneContainer.getCurrentPaneID())){
				action.setChecked(true);
			}

			addActionToMenu(fMenu, action);
		}

		return fMenu;
	}

	public void init() {
		fAvailableIDs = DetailPaneManager.getDefault().getAvailablePaneIDs(fDetailPaneContainer.getCurrentSelection());
		if (fAvailableIDs.size() > 1){
			setEnabled(true);
		}
	}
}
