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


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.ILaunchHistoryChangedListener;
import org.eclipse.debug.internal.ui.ILaunchLabelChangedListener;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.contextlaunching.LaunchingResourceManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationSelectionDialog;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsMessages;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchHistory;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutSelectionDialog;
import org.eclipse.debug.internal.ui.stringsubstitution.SelectedResourceManager;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;

/**
 * Abstract implementation of an action that displays a drop-down launch
 * history for a specific launch group.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @see LaunchingResourceManager
 * @see ILaunchLabelChangedListener
 * @since 2.1
 */
public abstract class AbstractLaunchHistoryAction implements IActionDelegate2, IWorkbenchWindowPulldownDelegate2, ILaunchHistoryChangedListener {

	/**
	 * The menu created by this action
	 */
	private Menu fMenu;

	/**
	 * The action used to render this delegate.
	 */
	private IAction fAction;

	/**
	 * The associated <code>ILaunchGroup</code>
	 * @since 3.3
	 */
	private ILaunchGroup fLaunchGroup = null;

	/**
	 * Indicates whether the launch history has changed and
	 * the sub menu needs to be recreated.
	 */
	protected boolean fRecreateMenu = false;

	/**
	 * Constructs a launch history action.
	 *
	 * @param launchGroupIdentifier unique identifier of the launch group
	 * extension that this action displays a launch history for.
	 */
	public AbstractLaunchHistoryAction(String launchGroupIdentifier) {
		fLaunchGroup = getLaunchConfigurationManager().getLaunchGroup(launchGroupIdentifier);
	}

	/**
	 * A listener to be notified of launch label updates
	 * @since 3.3
	 */
	private ILaunchLabelChangedListener fLabelListener = new ILaunchLabelChangedListener() {
		@Override
		public ILaunchGroup getLaunchGroup() {
			return fLaunchGroup;
		}
		@Override
		public void labelChanged() {
			updateTooltip();
		}
	};

	/**
	 * Sets the action used to render this delegate.
	 *
	 * @param action the action used to render this delegate
	 */
	private void setAction(IAction action) {
		fAction = action;
	}

	/**
	 * Returns the action used to render this delegate.
	 *
	 * @return the action used to render this delegate
	 */
	protected IAction getAction() {
		return fAction;
	}

	/**
	 * Adds the given action to the specified menu with an accelerator specified
	 * by the given number.
	 *
	 * @param menu the menu to add the action to
	 * @param action the action to add
	 * @param accelerator the number that should appear as an accelerator
	 */
	protected void addToMenu(Menu menu, IAction action, int accelerator) {
		StringBuilder label= new StringBuilder();
		if (accelerator >= 0 && accelerator < 10) {
			//add the numerical accelerator
			label.append('&');
			label.append(accelerator);
			label.append(' ');
		}
		label.append(action.getText());
		action.setText(label.toString());
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	/**
	 * Initialize this action so that it can dynamically set its tool-tip.  Also set the enabled state
	 * of the underlying action based on whether there are any registered launch configuration types that
	 * understand how to launch in the mode of this action.
	 * @param action the {@link IAction} to initialize
	 */
	private void initialize(IAction action) {
		getLaunchConfigurationManager().addLaunchHistoryListener(this);
		setAction(action);
		updateTooltip();
		action.setEnabled(existsConfigTypesForMode());
	}

	/**
	 * Return whether there are any registered launch configuration types for
	 * the mode of this action.
	 *
	 * @return whether there are any registered launch configuration types for
	 * the mode of this action
	 */
	private boolean existsConfigTypesForMode() {
		ILaunchConfigurationType[] configTypes = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationTypes();
		for (ILaunchConfigurationType configType : configTypes) {
			if (configType.supportsMode(getMode())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Updates this action's tool-tip. The tooltip is based on user preference settings
	 * for launching - either the previous launch, or based on the selection and which
	 * configuration will be launched.
	 * <p>
	 * Subclasses may override as required.
	 * </p>
	 */
	protected void updateTooltip() {
		CompletableFuture.supplyAsync(this::getToolTip).thenAcceptAsync(getAction()::setToolTipText,
				Display.getDefault());
	}

	/**
	 * Returns the tooltip specific to a configuration.
	 *
	 * @param configuration a <code>ILauncConfiguration</code>
	 * @return the string for the tool tip
	 */
	protected String getToolTip(ILaunchConfiguration configuration) {
		String launchName= configuration.getName();
		String mode= getMode();
		String label;
		if (mode.equals(ILaunchManager.RUN_MODE)) {
			label= ActionMessages.AbstractLaunchHistoryAction_1;
		} else if (mode.equals(ILaunchManager.DEBUG_MODE)){
			label= ActionMessages.AbstractLaunchHistoryAction_2;
		} else if (mode.equals(ILaunchManager.PROFILE_MODE)){
			label= ActionMessages.AbstractLaunchHistoryAction_3;
		} else {
			label= ActionMessages.AbstractLaunchHistoryAction_4;
		}
		return MessageFormat.format(ActionMessages.AbstractLaunchHistoryAction_0, new Object[] {
				label, launchName });
	}

	/**
	 * Returns this action's tooltip. The tooltip is retrieved from the launch resource manager
	 * which builds tool tips asynchronously for context launching support.
	 *
	 * @return the string for the tool tip
	 */
	private String getToolTip() {
		String launchName = DebugUIPlugin.getDefault().getLaunchingResourceManager().getLaunchLabel(fLaunchGroup);
		if(launchName == null) {
			return DebugUIPlugin.removeAccelerators(internalGetHistory().getLaunchGroup().getLabel());
		}
		String label = null;
		String mode = getMode();
		if (mode.equals(ILaunchManager.RUN_MODE)) {
			label = ActionMessages.AbstractLaunchHistoryAction_1;
		} else if (mode.equals(ILaunchManager.DEBUG_MODE)){
			label = ActionMessages.AbstractLaunchHistoryAction_2;
		} else if (mode.equals(ILaunchManager.PROFILE_MODE)){
			label = ActionMessages.AbstractLaunchHistoryAction_3;
		} else {
			label = ActionMessages.AbstractLaunchHistoryAction_4;
		}
		if(IInternalDebugCoreConstants.EMPTY_STRING.equals(launchName)) {
			return MessageFormat.format(ActionMessages.AbstractLaunchHistoryAction_5, new Object[] { label });
		}
		else {
			return MessageFormat.format(ActionMessages.AbstractLaunchHistoryAction_0, new Object[] {
					label, launchName });
		}
	}

	/**
	 * @see ILaunchHistoryChangedListener#launchHistoryChanged()
	 */
	@Override
	public void launchHistoryChanged() {
		fRecreateMenu = true;
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	@Override
	public void dispose() {
		setMenu(null);
		getLaunchConfigurationManager().removeLaunchHistoryListener(this);
		DebugUIPlugin.getDefault().getLaunchingResourceManager().removeLaunchLabelChangedListener(fLabelListener);
	}

	/**
	 * Return the last launch in this action's launch history.
	 *
	 * @return the most recent configuration that was launched from this
	 *  action's launch history that is not filtered from the menu
	 */
	protected ILaunchConfiguration getLastLaunch() {
		return getLaunchConfigurationManager().getFilteredLastLaunch(getLaunchGroupIdentifier());
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchWindowPulldownDelegate#getMenu(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public Menu getMenu(Control parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		initMenu();
		return fMenu;
	}

	@Override
	public Menu getMenu(Menu parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		initMenu();
		return fMenu;
	}

	/**
	 * Creates the menu for the action
	 */
	private void initMenu() {
		// Add listener to re-populate the menu each time
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

	/**
	 * Fills the drop-down menu with favorites and launch history
	 *
	 * @param menu the menu to fill
	 */
	protected void fillMenu(Menu menu) {
		ILaunchConfiguration[] historyList= getHistory();
		ILaunchConfiguration[] favoriteList = getFavorites();

		// Add favorites
		int accelerator = 1;
		for (ILaunchConfiguration launch : favoriteList) {
			LaunchAction action= new LaunchAction(launch, getMode());
			addToMenu(menu, action, accelerator);
			accelerator++;
		}

		// Separator between favorites and history
		if (favoriteList.length > 0 && historyList.length > 0) {
			addSeparator(menu);
		}

		// Add history launches next
		for (ILaunchConfiguration launch : historyList) {
			LaunchAction action= new LaunchAction(launch, getMode());
			addToMenu(menu, action, accelerator);
			accelerator++;
		}

		if(accelerator == 1) {
			IAction action = new Action(ActionMessages.AbstractLaunchHistoryAction_6) {};
			action.setEnabled(false);
			ActionContributionItem item= new ActionContributionItem(action);
			item.fill(menu, -1);
		}
	}

	/**
	 * Adds a separator to the given menu
	 *
	 * @param menu the menu to add the separator to
	 */
	protected void addSeparator(Menu menu) {
		new MenuItem(menu, SWT.SEPARATOR);
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		// do nothing - this is just a menu
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate2#runWithEvent(org.eclipse.jface.action.IAction, org.eclipse.swt.widgets.Event)
	 * @since 3.6
	 */
	@Override
	public void runWithEvent(IAction action, Event event) {
		if(((event.stateMask & SWT.MOD1) > 0) && (event.type != SWT.KeyDown)) {
			ILaunchConfiguration configuration = null;
			String groupid = getLaunchGroupIdentifier();
			if(LaunchingResourceManager.isContextLaunchEnabled(groupid)) {
				configuration = resolveContextConfiguration();
			} else {
				configuration = getLaunchConfigurationManager().getFilteredLastLaunch(groupid);
			}
			ArrayList<ILaunchConfiguration> configs = new ArrayList<>(1);
			if (configuration != null){
				configs.add(configuration);
			}
			DebugUIPlugin.openLaunchConfigurationsDialog(
					DebugUIPlugin.getShell(),
					new StructuredSelection(configs),
					groupid,
					true);
			return;
		}
		runInternal(action, ((event.stateMask & SWT.SHIFT) > 0) ? true : false);
	}

	/**
	 * @since 3.12
	 */
	protected void runInternal(IAction action, @SuppressWarnings("unused") boolean isShift) {
		run(action);
	}

	/**
	 * Resolves the configuration to show in the dialog when opened via the Ctrl+Click.
	 * If no configuration exists a new one is created using its respective {@link ILaunchShortcut}
	 * @return the configuration to show in the launch dialog
	 * @since 3.6
	 */
	private ILaunchConfiguration resolveContextConfiguration() {
		SelectedResourceManager srm = SelectedResourceManager.getDefault();
		IStructuredSelection selection = srm.getCurrentSelection();
		List<LaunchShortcutExtension> shortcuts = null;
		IResource resource = srm.getSelectedResource();
		LaunchingResourceManager lrm = DebugUIPlugin.getDefault().getLaunchingResourceManager();
		shortcuts = lrm.getShortcutsForSelection(selection, getMode());
		if(resource == null) {
			resource = lrm.getLaunchableResource(shortcuts, selection);
		}
		List<ILaunchConfiguration> configs = lrm.getParticipatingLaunchConfigurations(selection, resource, shortcuts, getMode());
		if(configs.size() == 1) {
			return configs.get(0);
		} else if(configs.size() > 1) {
			// launch most recently launched config
			ILaunchConfiguration config = getLaunchConfigurationManager().getMRUConfiguration(configs, fLaunchGroup, resource);
			if(config != null) {
				return config;
			} else {
				// Let the use select which config to open
				LaunchConfigurationSelectionDialog dialog = new LaunchConfigurationSelectionDialog(DebugUIPlugin.getShell(), configs);
				if(dialog.open() == IDialogConstants.OK_ID) {
					return (ILaunchConfiguration) dialog.getResult()[0];
				}
			}
			return null;
		} else if(shortcuts.size() > 1) {
			//no configs, choose shortcut to create a new one
			LaunchShortcutSelectionDialog dialog = new LaunchShortcutSelectionDialog(shortcuts, resource, getMode());
			if(dialog.open() == IDialogConstants.OK_ID) {
				LaunchShortcutExtension ext = (LaunchShortcutExtension) dialog.getResult()[0];
				return createConfigurationFromTypes(ext.getAssociatedConfigurationTypes());
			}
			return null;
		} else if(shortcuts.size() == 1) {
			LaunchShortcutExtension ext = shortcuts.get(0);
			return createConfigurationFromTypes(ext.getAssociatedConfigurationTypes());
		}
		return getLaunchConfigurationManager().getFilteredLastLaunch(getLaunchGroupIdentifier());
	}

	/**
	 * Creates an {@link ILaunchConfiguration} from the given set of {@link ILaunchConfigurationType}s
	 * @param types the set of {@link String} {@link ILaunchConfigurationType} identifiers
	 * @return a new {@link ILaunchConfiguration}
	 * @since 3.6
	 */
	private ILaunchConfiguration createConfigurationFromTypes(Set<String> types) {
		//context launching always takes the first type, so we do that here as well
		if(types != null && types.size() > 0) {
			try {
				ILaunchConfigurationType type = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType((String) types.toArray()[0]);
				ILaunchConfigurationWorkingCopy copy = type.newInstance(null, DebugPlugin.getDefault().getLaunchManager().generateLaunchConfigurationName(LaunchConfigurationsMessages.CreateLaunchConfigurationAction_New_configuration_2));
				return copy;
			}
			catch(CoreException ce) {
				//do nothing return null
			}
		}
		return null;
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate2#init(org.eclipse.jface.action.IAction)
	 * @since 3.6
	 */
	@Override
	public void init(IAction action) {
		// do nothing by default
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection){
		if (fAction == null) {
			initialize(action);
		}
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void init(IWorkbenchWindow window) {
		if (this instanceof AbstractLaunchToolbarAction) {
			DebugUIPlugin.getDefault().getLaunchingResourceManager().addLaunchLabelUpdateListener(fLabelListener);
		}
	}

	/**
	 * Returns the launch history associated with this action's launch group.
	 *
	 * @return the launch history associated with this action's launch group
	 * @deprecated this method returns a class that is not API and is not intended
	 *  for clients of the debug platform. Instead, use <code>getHistory()</code>,
	 *  <code>getFavorites()</code>, and <code>getLastLaunch()</code>.
	 */
	@Deprecated
	protected LaunchHistory getLaunchHistory() {
		return getLaunchConfigurationManager().getLaunchHistory(getLaunchGroupIdentifier());
	}

	/**
	 * Returns the launch history associated with this action's launch group.
	 *
	 * @return the launch history associated with this action's launch group
	 * @since 3.3
	 */
	private LaunchHistory internalGetHistory() {
		return getLaunchConfigurationManager().getLaunchHistory(getLaunchGroupIdentifier());
	}

	/**
	 * Returns the launch history associated with this action's launch mode and group in most
	 * recently launched order. Configurations associated with disabled activities are not included
	 * in the list. As well, configurations are filtered based on workspace preference settings
	 * to filter configurations from closed projects, deleted projects, working sets and to filter
	 * specific launch configuration types.
	 *
	 * @return launch history
	 * @since 3.3
	 */
	protected ILaunchConfiguration[] getHistory() {
		return LaunchConfigurationManager.filterConfigs(internalGetHistory().getHistory());
	}

	/**
	 * Returns the launch favorites associated with this action's launch mode and group in user
	 * preference order. Configurations associated with disabled activities are not included
	 * in the list. As well, configurations are filtered based on workspace preference settings
	 * to filter configurations from closed projects, deleted projects, working sets and to filter
	 * specific launch configuration types.
	 *
	 * @return favorite launch configurations
	 * @since 3.3
	 */
	protected ILaunchConfiguration[] getFavorites() {
		return LaunchConfigurationManager.filterConfigs(internalGetHistory().getFavorites());
	}

	/**
	 * Returns the mode (e.g., 'run' or 'debug') of this drop down.
	 *
	 * @return the mode of this action
	 */
	protected String getMode() {
		return internalGetHistory().getLaunchGroup().getMode();
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
	 * Returns the identifier of the launch group this action is associated
	 * with.
	 *
	 * @return the identifier of the launch group this action is associated
	 * with
	 */
	protected String getLaunchGroupIdentifier() {
		return fLaunchGroup.getIdentifier();
	}
}
