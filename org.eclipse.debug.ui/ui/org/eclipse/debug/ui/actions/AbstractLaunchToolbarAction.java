/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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


import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.contextlaunching.ContextRunner;
import org.eclipse.debug.internal.ui.contextlaunching.LaunchingResourceManager;
import org.eclipse.debug.internal.ui.launchConfigurations.OrganizeFavoritesAction;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Menu;

/**
 * A launch history action that also includes launch shortcut actions (run/debug
 * as), and an action to open the launch configuration dialog.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 2.1
 */
public class AbstractLaunchToolbarAction extends AbstractLaunchHistoryAction {


	/**
	 * Constructs a launch toolbar action.
	 *
	 * @param launchGroupIdentifier unique identifier of the launch group
	 * extension that this action displays a launch history, shortcuts, and
	 * launch configuration dialog for.
	 */
	public AbstractLaunchToolbarAction(String launchGroupIdentifier) {
		super(launchGroupIdentifier);
	}

	/**
	 * Fills the drop-down menu with favorites and launch history,
	 * launch shortcuts, and an action to open the launch configuration dialog.
	 *
	 * @param menu the menu to fill
	 */
	@Override
	protected void fillMenu(Menu menu) {
		super.fillMenu(menu);
		// Separator between history and common actions
		if (menu.getItemCount() > 0) {
			addSeparator(menu);
		}
		addToMenu(menu, new LaunchShortcutsAction(getLaunchGroupIdentifier()), -1);
		addToMenu(menu, getOpenDialogAction(), -1);
		addToMenu(menu, new OrganizeFavoritesAction(getLaunchGroupIdentifier()), -1);
	}

	/**
	 * Returns an action to open the launch dialog
	 * @return the new {@link OpenLaunchDialogAction}
	 * @since 3.1
	 */
	protected IAction getOpenDialogAction() {
		return new OpenLaunchDialogAction(getLaunchGroupIdentifier());
	}

	/**
	 * Launch the last launch, or open the launch config dialog if none.
	 *
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		runInternal(action, false);
	}

	@Override
	protected void runInternal(IAction action, boolean isShift) {
		//always ignore external tools during context launching
		if(LaunchingResourceManager.isContextLaunchEnabled(getLaunchGroupIdentifier())) {
			ContextRunner.getDefault().launch(DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(getLaunchGroupIdentifier()), isShift);
		}
		else {
			ILaunchConfiguration configuration = getLastLaunch();
			if (configuration == null) {
				DebugUITools.openLaunchConfigurationDialogOnGroup(DebugUIPlugin.getShell(), new StructuredSelection(), getLaunchGroupIdentifier());
			} else {
				DebugUITools.launch(configuration, getMode(), isShift);
			}
		}
	}
}
