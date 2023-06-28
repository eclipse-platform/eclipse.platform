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
package org.eclipse.debug.internal.ui.launchConfigurations;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Creates the drop down menu for the filtering button in the launch configuration dialog
 * @since 3.2
 */
public class FilterDropDownMenuCreator implements IMenuCreator {

	/**
	 * Provides wrapper action for filtering actions on the launch configuration dialog
	 * @since 3.2
	 */
	class FilterAction extends Action {

		/** The preference store. */
		private IPreferenceStore fStore = null;

		/** The preference key for the value in the store. */
		private String fKey = null;

		/**
		 * Constructor for check style menu items
		 * @param store the pref store
		 * @param name the name of the action
		 * @param key the pref key it is tied to
		 */
		public FilterAction(Menu menu, IPreferenceStore store, String name, String key) {
			super(name, IAction.AS_CHECK_BOX);
			fStore = store;
			fKey = key;
			setChecked(fStore.getBoolean(fKey));
			fillIntoMenu(menu, this);
		}

		/**
		 * Constructor for flyout menu style actions
		 * @param menu the parent menu
		 * @param name the text of the action
		 * @param creator the menu creator for this action
		 */
		public FilterAction(Menu menu, String name, IMenuCreator creator) {
			super(name, IAction.AS_DROP_DOWN_MENU);
			setMenuCreator(creator);
			fillIntoMenu(menu, this);
		}

		/**
		 * fills the new action into the specified menu
		 * @param menu the parent menu
		 * @param action the new aciton to fill in to the parent
		 */
		private void fillIntoMenu(Menu menu, IAction action) {
			ActionContributionItem item = new ActionContributionItem(action);
			item.fill(menu, -1);
		}

		@Override
		public void run() {
			if(fStore != null) {
				fStore.setValue(fKey, isChecked());
			}
		}
	}

	/**
	 * the menu created via this class
	 */
	private Menu fCreatedMenu = null;

	/**
	 * gets the DebugUIPlugin preference store
	 * @return the pref store
	 */
	private IPreferenceStore getDebugPrefStore() {
		return DebugUIPlugin.getDefault().getPreferenceStore();
	}

	@Override
	public void dispose() {
		fCreatedMenu = null;
	}

	@Override
	public Menu getMenu(Control parent) {
		if(fCreatedMenu != null) {
			fCreatedMenu.dispose();
		}
		//create the menu & items
		fCreatedMenu = new Menu(parent);
		new FilterAction(fCreatedMenu, getDebugPrefStore(), LaunchConfigurationsMessages.FilterDropDownMenuCreator_0, IInternalDebugUIConstants.PREF_FILTER_LAUNCH_CLOSED);
		new FilterAction(fCreatedMenu, getDebugPrefStore(), LaunchConfigurationsMessages.FilterDropDownMenuCreator_1, IInternalDebugUIConstants.PREF_FILTER_LAUNCH_DELETED);
		new FilterAction(fCreatedMenu, getDebugPrefStore(), LaunchConfigurationsMessages.FilterDropDownMenuCreator_2, IInternalDebugUIConstants.PREF_FILTER_LAUNCH_TYPES);
		new FilterAction(fCreatedMenu, getDebugPrefStore(), LaunchConfigurationsMessages.FilterDropDownMenuCreator_4, IInternalDebugUIConstants.PREF_FILTER_WORKING_SETS);

		//add separator
		new MenuItem(fCreatedMenu, SWT.SEPARATOR);

		//add pref action
		IAction action = new Action(LaunchConfigurationsMessages.FilterDropDownMenuCreator_3) {
			@Override
			public void run() {
				SWTFactory.showPreferencePage("org.eclipse.debug.ui.LaunchConfigurations"); //$NON-NLS-1$
			}
		};
		new ActionContributionItem(action).fill(fCreatedMenu, -1);
		return fCreatedMenu;
	}

	@Override
	public Menu getMenu(Menu parent) {
		return fCreatedMenu;
	}
}
