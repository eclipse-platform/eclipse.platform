/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.ui.actions;


import java.util.List;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.LaunchShortcutAction;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupExtension;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.WorkbenchActivityHelper;

/**
 * A cascading sub-menu that shows all launch shortcuts pertinent to a
 * launch group.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 2.1
 * @deprecated The use of perspective based launch shortcuts has been deprecated
 *  in the 3.1 release. Instead, selection sensitive launch is supported in the top level
 *  menus. Use <code>LaunchShorcutsAction</code> instead.
 * @noextend This class is not intended to be subclassed by clients.
 */
@Deprecated
public class LaunchAsAction extends Action implements IMenuCreator, IWorkbenchWindowPulldownDelegate2 {

	/**
	 * Cascading menu
	 */
	private Menu fCreatedMenu;

	/**
	 * Launch group identifier
	 */
	private String fLaunchGroupIdentifier;

	/**
	 * Presentation wrapper for this action
	 */
	private IAction fAction;

	/**
	 * Creates a cascading menu action to populate with shortcuts in the given
	 * launch group.
	 *
	 * @param launchGroupIdentifier launch group identifier
	 */
	public LaunchAsAction(String launchGroupIdentifier) {
		super();
		fLaunchGroupIdentifier = launchGroupIdentifier;
		ILaunchMode launchMode = DebugPlugin.getDefault().getLaunchManager().getLaunchMode(getMode());
		setText(launchMode.getLaunchAsLabel());
		setMenuCreator(this);
	}

	/**
	 * Returns the launch group associated with this action.
	 *
	 * @return the launch group associated with this action
	 */
	private LaunchGroupExtension getLaunchGroup() {
		return getLaunchConfigurationManager().getLaunchGroup(fLaunchGroupIdentifier);
	}

	/**
	 * @see IAction#run()
	 */
	@Override
	public void run() {
		//do nothing, this action just creates a cascading menu.
	}

	private void createAction(Menu parent, IAction action, int count) {
		StringBuilder label= new StringBuilder();
		//add the numerical accelerator
		if (count < 10) {
			label.append('&');
			label.append(count);
			label.append(' ');
		}
		label.append(action.getText());
		action.setText(label.toString());
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(parent, -1);
	}

	/**
	 * @see IMenuCreator#dispose()
	 */
	@Override
	public void dispose() {
		if (getCreatedMenu() != null) {
			getCreatedMenu().dispose();
		}
	}

	/**
	 * @see IMenuCreator#getMenu(Control)
	 */
	@Override
	public Menu getMenu(Control parent) {
		return null;
	}

	/**
	 * @see IMenuCreator#getMenu(Menu)
	 */
	@Override
	public Menu getMenu(Menu parent) {
		if (getCreatedMenu() != null) {
			 getCreatedMenu().dispose();
		 }
		setCreatedMenu(new Menu(parent));
		fillMenu();
		initMenu();
		return getCreatedMenu();
	}

	private void fillMenu() {
		//Retrieve the current perspective and the registered shortcuts
		List<LaunchShortcutExtension> shortcuts = null;
		 String activePerspID = getActivePerspectiveID();
		 if (activePerspID != null) {
			 shortcuts = getLaunchConfigurationManager().getLaunchShortcuts(activePerspID, getCategory());
		 }

		 // If NO shortcuts are listed in the current perspective, add ALL shortcuts
		 // to avoid an empty cascading menu
		 if (shortcuts == null || shortcuts.isEmpty()) {
			 shortcuts = getLaunchConfigurationManager().getLaunchShortcuts(getCategory());
		 }

		 int menuCount = 1;
		 String mode = getMode();
		for (LaunchShortcutExtension ext : shortcuts) {
			 if (ext.getModes().contains(mode) && !WorkbenchActivityHelper.filterItem(ext)) {
				populateMenu(mode, ext, getCreatedMenu(), menuCount);
				menuCount++;
			 }
		 }
	}

	/**
	 * Creates the menu for the action
	 */
	private void initMenu() {
		// Add listener to re-populate the menu each time
		// it is shown to reflect changes in selection or active perspective
		fCreatedMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				Menu m = (Menu)e.widget;
				MenuItem[] items = m.getItems();
				for (MenuItem item : items) {
					item.dispose();
				}
				fillMenu();
			}
		});
	}

	/**
	 * Add the shortcut to the menu.
	 * @param mode the launch mode identifier
	 * @param ext the shortcut extension to get label and help information from
	 * @param menu the menu to add to
	 * @param menuCount the number to use as the accelerator for the new action
	 */
	private void populateMenu(String mode, LaunchShortcutExtension ext, Menu menu, int menuCount) {
		LaunchShortcutAction action = new LaunchShortcutAction(mode, ext);
		action.setActionDefinitionId(ext.getId() + "." + mode); //$NON-NLS-1$
		String helpContextId = ext.getHelpContextId();
		if (helpContextId != null) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(action, helpContextId);
		}
		/*if (fKeyBindingService != null) {
			fKeyBindingService.registerGlobalAction(action);
		}*/
		createAction(menu, action, menuCount);
	}

	/**
	 * Return the ID of the currently active perspective, or <code>null</code>
	 * if there is none.
	 * @return the identifier of the currently active perspective
	 */
	private String getActivePerspectiveID() {
		IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IPerspectiveDescriptor persp = page.getPerspective();
				if (persp != null) {
					return persp.getId();
				}
			}
		}
		return null;
	}

	/**
	 * Returns the mode of this action - run or debug
	 *
	 * @return the mode of this action - run or debug
	 */
	private String getMode() {
		return getLaunchGroup().getMode();
	}

	/**
	 * Returns the category of this action - possibly <code>null</code>
	 *
	 * @return the category of this action - possibly <code>null</code>
	 */
	private String getCategory() {
		return getLaunchGroup().getCategory();
	}

	private Menu getCreatedMenu() {
		return fCreatedMenu;
	}

	private void setCreatedMenu(Menu createdMenu) {
		fCreatedMenu = createdMenu;
	}

	/**
	 * Returns the launch configuration manager.
	 *
	 * @return launch configuration manager
	 */
	private LaunchConfigurationManager getLaunchConfigurationManager() {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager();
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void init(IWorkbenchWindow window) {
//		if (window instanceof WorkbenchWindow) {
//			fKeyBindingService= ((WorkbenchWindow)window).getKeyBindingService();
//		}
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		// do nothing - this is just a menu
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (fAction == null) {
			initialize(action);
		}
	}

	/**
	 * Set the enabled state of the underlying action based on whether there are any
	 * registered launch shortcuts for this launch mode.
	 * @param action the action to initialize
	 */
	private void initialize(IAction action) {
		fAction = action;
		action.setEnabled(existsShortcutsForMode());
	}

	/**
	 * Return whether there are any registered launch shortcuts for
	 * the mode of this action.
	 *
	 * @return whether there are any registered launch shortcuts for
	 * the mode of this action
	 */
	private boolean existsShortcutsForMode() {
		List<LaunchShortcutExtension> shortcuts = getLaunchConfigurationManager().getLaunchShortcuts(getCategory());
		for (LaunchShortcutExtension ext : shortcuts) {
			if (ext.getModes().contains(getMode())) {
				return true;
			}
		}
		return false;
	}
}

