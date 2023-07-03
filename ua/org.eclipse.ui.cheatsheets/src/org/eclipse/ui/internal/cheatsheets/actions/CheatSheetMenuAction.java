/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;

/**
 * This is the action used to contribute the CheatSheets menu to the workbench's
 * help menu.
 */
public class CheatSheetMenuAction implements IWorkbenchWindowPulldownDelegate2, IPropertyListener {
	/**
	 * The menu created by this action
	 */
	private Menu fMenu;

	/**
	 * Indicates whether the cheat sheet history has changed and
	 * the sub menu needs to be recreated.
	 */
	protected boolean fRecreateMenu = false;

	/**
	 * The constructor.
	 */
	public CheatSheetMenuAction() {
		CheatSheetPlugin.getPlugin().getCheatSheetHistory().addListener(this);
	}

	@Override
	public void dispose() {
		setMenu(null);
		CheatSheetPlugin.getPlugin().getCheatSheetHistory().removeListener(this);
	}

	/**
	 * Fills the drop-down menu with cheat sheets history
	 *
	 * @param menu the menu to fill
	 */
	protected void fillMenu(Menu menu) {
		CheatSheetMenu cheatsheetMenuMenuItem = new CheatSheetMenu();
		cheatsheetMenuMenuItem.fill(menu, 0);
	}

	@Override
	public Menu getMenu(Control parent) {
		return null;
	}

	@Override
	public Menu getMenu(Menu parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		initMenu();
		return fMenu;
	}

	@Override
	public void init(IWorkbenchWindow window) {
	}

	/**
	 * Creates the menu for the action
	 */
	private void initMenu() {
		// Add listener to repopulate the menu each time
		// it is shown because of dynamic history list
		fMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				if (fRecreateMenu) {
					Menu m = (Menu)e.widget;
					MenuItem[] items = m.getItems();
					for (MenuItem item : items) {
						item.dispose();
					}
					fillMenu(m);
					fRecreateMenu= false;
				}
			}
		});
	}

	@Override
	public void propertyChanged(Object source, int propId) {
		fRecreateMenu = true;
	}

	@Override
	public void run(IAction action) {
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * Sets this action's drop-down menu, disposing the previous menu.
	 *
	 * @param menu the new menu
	 */
	private void setMenu(Menu menu) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = menu;
	}
}
