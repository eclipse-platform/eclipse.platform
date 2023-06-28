/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.actions.LaunchConfigurationAction;
import org.eclipse.debug.internal.ui.actions.LaunchShortcutAction;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;
import org.eclipse.debug.internal.ui.stringsubstitution.SelectedResourceManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.WorkbenchActivityHelper;

/**
 * A cascading sub-menu that shows all launch shortcuts pertinent to a
 * selection. This action is similar to <code>ContextualLaunchAction</code>
 * except this action is an <code>IAction</code> rather than an action
 * delegate.
 * <p>
 * This action appears in the main Run menu
 * </p>
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.1
 */
public class LaunchShortcutsAction extends Action implements IMenuCreator, IWorkbenchWindowPulldownDelegate2 {

	/**
	 * Cascading menu
	 */
	private Menu fCreatedMenu;

	/**
	 * Launch group
	 */
	private ILaunchGroup fGroup;

	/**
	 * Whether this actions enablement has been initialized
	 */
	private boolean fInitialized = false;

	/**
	 * Creates a cascading menu action to populate with shortcuts in the given
	 * launch group.
	 *
	 * @param launchGroupIdentifier launch group identifier
	 */
	public LaunchShortcutsAction(String launchGroupIdentifier) {
		super();
		fGroup = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(launchGroupIdentifier);
		ILaunchMode mode = DebugPlugin.getDefault().getLaunchManager().getLaunchMode(fGroup.getMode());
		setText(mode.getLaunchAsLabel());
		setMenuCreator(this);
		setEnabled(existsConfigTypesForMode());
	}

	/**
	 * @see IAction#run()
	 */
	@Override
	public void run() {
		//do nothing, this action just creates a cascading menu.
	}

	/**
	 * @see IMenuCreator#dispose()
	 */
	@Override
	public void dispose() {
		if (fCreatedMenu != null) {
			fCreatedMenu.dispose();
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
		if (fCreatedMenu != null) {
			 fCreatedMenu.dispose();
		 }
		fCreatedMenu = new Menu(parent);
		initMenu();
		return fCreatedMenu;
	}

	/**
	 * @return an Evaluation context with default variable = selection
	 */
	private IEvaluationContext createContext() {
		IStructuredSelection ss = SelectedResourceManager.getDefault().getCurrentSelection();
		Object o = ss.getFirstElement();
		List<IEditorInput> list = new ArrayList<>(0);
		if(o instanceof IEditorPart) {
			list.add(((IEditorPart)o).getEditorInput());
		}
		else {
			list.addAll(ss.toList());
		}
		IEvaluationContext context = DebugUIPlugin.createEvaluationContext(list);
		context.setAllowPluginActivation(true);
		context.addVariable("selection", list); //$NON-NLS-1$
		return context;
	}

	/**
	 * Fills the fly-out menu
	 */
	private void fillMenu() {
		IEvaluationContext context = createContext();
		int accelerator = 1;
		List<LaunchShortcutExtension> allShortCuts = getLaunchConfigurationManager().getLaunchShortcuts(fGroup.getCategory());
		Iterator<LaunchShortcutExtension> iter = allShortCuts.iterator();
		List<LaunchShortcutExtension> filteredShortCuts = new ArrayList<>(10);
		while (iter.hasNext()) {
			LaunchShortcutExtension ext = iter.next();
			try {
				if (!WorkbenchActivityHelper.filterItem(ext) && isApplicable(ext, context)) {
					filteredShortCuts.add(ext);
				}
			} catch (CoreException e) {
				IStatus status = new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), "Launch shortcut '" + ext.getId() + "' enablement expression caused exception. Shortcut was removed.", e); //$NON-NLS-1$ //$NON-NLS-2$
				DebugUIPlugin.log(status);
				iter.remove();
			}
		}
		//first add the launch config if it is one
		String mode = getMode();
		try {
			ILaunchConfiguration config = getLaunchConfigurationManager().isSharedConfig(getSelection(context));
			if(config != null && config.exists() && config.supportsMode(mode)) {
				IAction action = new LaunchConfigurationAction(config, mode, config.getName(), DebugUITools.getDefaultImageDescriptor(config), accelerator++);
				ActionContributionItem item = new ActionContributionItem(action);
				item.fill(fCreatedMenu, -1);
				if(!filteredShortCuts.isEmpty()) {
					new MenuItem(fCreatedMenu, SWT.SEPARATOR);
				}
			}
		}
		catch(CoreException ce) {DebugUIPlugin.log(ce);}
		//second add the launch shortcuts if any
		for (LaunchShortcutExtension ext : filteredShortCuts) {
			for (String modee : ext.getModes()) {
				if (modee.equals(mode)) {
					populateMenuItem(modee, ext, fCreatedMenu, accelerator++);
				}
			}
		}
		if (accelerator == 1) {
			// No shortcuts added. Add "none available" action.
			IAction action= new Action(ActionMessages.LaunchShortcutsAction_1) {};
			action.setEnabled(false);
			ActionContributionItem item= new ActionContributionItem(action);
			item.fill(fCreatedMenu, -1);
		}
	}

	/**
	 * Returns the first element of the current selection
	 * @param context the current evaluation context
	 * @return the first item in the selection, or <code>null</code> if none
	 * @since 3.3
	 */
	private Object getSelection(IEvaluationContext context) {
		List<?> list = (List<?>) context.getVariable("selection"); //$NON-NLS-1$
		return (list.isEmpty() ? null : list.get(0));
	}

	/**
	 * Add the shortcut to the context menu's launch sub-menu.
	 * @param mode the launch mode identifier
	 * @param ext the shortcut extension to get label and help information from
	 * @param menu the menu to add to
	 * @param accelerator the accelerator to use
	 */
	private void populateMenuItem(String mode, LaunchShortcutExtension ext, Menu menu, int accelerator) {
		LaunchShortcutAction action = new LaunchShortcutAction(mode, ext);
		action.setActionDefinitionId(ext.getId() + "." + mode); //$NON-NLS-1$
		String helpContextId = ext.getHelpContextId();
		if (helpContextId != null) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(action, helpContextId);
		}
		StringBuilder label= new StringBuilder();
		if (accelerator >= 0 && accelerator < 10) {
			//add the numerical accelerator
			label.append('&');
			label.append(accelerator);
			label.append(' ');
		}
		String contextLabel= ext.getContextLabel(mode);
		// replace default action label with context label if specified.
		label.append((contextLabel != null) ? contextLabel : action.getText());
		action.setText(label.toString());
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	/**
	 * Evaluate the enablement logic in the contextualLaunch
	 * element description. A true result means that we should
	 * include this shortcut in the context menu.
	 * @param ext the launch shortcut extension to get the enablement expression from
	 * @param context the context to use for enablement evaluation
	 * @return true iff shortcut should appear in context menu
	 * @throws CoreException if an exception occurs
	 */
	private boolean isApplicable(LaunchShortcutExtension ext, IEvaluationContext context) throws CoreException {
		Expression expr = ext.getContextualLaunchEnablementExpression();
		return ext.evalEnablementExpression(context, expr);
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
	 * Returns the mode of this action - run or debug
	 *
	 * @return the mode of this action - run or debug
	 */
	protected String getMode() {
		return fGroup.getMode();
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
	public void init(IWorkbenchWindow window) {}

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
		if (!fInitialized) {
			action.setEnabled(existsConfigTypesForMode());
			fInitialized = true;
		}
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
}

