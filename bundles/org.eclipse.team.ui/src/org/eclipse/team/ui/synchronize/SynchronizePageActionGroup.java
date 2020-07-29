/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.ui.synchronize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.actions.ActionGroup;

/**
 * Used to add one or more actions to the context menu, toolbar or view menu
 * of an {@link ISynchronizePage}. An action group is added to a synchronize
 * page by adding the group to the {@link ISynchronizePageConfiguration} after
 * configuration has been created by the page but before the page is created.
 * <p>
 * The life cycle of an action group is:
 * </p>
 * <ul>
 * <li>the <code>initialize(ISynchronizePageConfiguration}</code> method is
 * invoked before the methods to populate menus. This is done to give clients
 * a change to create and initialize the actions of the action group.
 * <li>The <code>fillActionBars(IActionBars)</code> method is invoked
 * to populate the page's action bars (view menu and toolbar). It is
 * possible for the action bars to be missing one or more components
 * so clients are expected to check for <code>null</code> when accessing
 * the menus from the action bars.
 * <li>The <code>fillContextMenu(IMenuManager)</code> method is invoked each time
 * the context menu is shown. Before this method is called, the
 * action group will be provided with an <code>ActionContext</code>
 * containing the view selection. Clients can access the context using
 * <code>getContext()</code>.
 * <li>The <code>updateActionBars()</code> method is invoked whenever the
 * page's selection changes. Before this method is called, the
 * action group will be provided with an <code>ActionContext</code>
 * containing the view selection. Clients can access the context using
 * <code>getContext()</code>.
 * <li>The <code>modelChanged(ISynchronizeModelElement)</code> method is
 * invoked whenever the model being displayed is changed. This gives clients
 * a chance to adjust the input to actions that operate on all visible elements.
 * <li>The <code>dispose()</code> method is called when the page is disposed.
 * </ul>
 * @since 3.0
 */
public abstract class SynchronizePageActionGroup extends ActionGroup {

	private ISynchronizePageConfiguration configuration;

	private Map<String, Map<String, List<Object>>> menuContributions = new HashMap<>();

	private VisibleRootsSelectionProvider visibleRootSelectionProvider;

	/*
	 * A selection provider whose selection is the root elements visible in the
	 * page. Selection changed events are sent out when the model roots change
	 * or their visible children change
	 */
	private class VisibleRootsSelectionProvider extends SynchronizePageActionGroup implements ISelectionProvider {

		private ListenerList<ISelectionChangedListener> selectionChangedListeners = new ListenerList<>(ListenerList.IDENTITY);
		private ISelection selection;

		protected VisibleRootsSelectionProvider(ISynchronizeModelElement element) {
			modelChanged(element);
		}

		@Override
		public void modelChanged(ISynchronizeModelElement root) {
			if (root == null) {
				setSelection(StructuredSelection.EMPTY);
			} else {
				setSelection(new StructuredSelection(root));
			}
		}

		@Override
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			selectionChangedListeners.add(listener);
		}

		@Override
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			selectionChangedListeners.remove(listener);
		}

		@Override
		public ISelection getSelection() {
			return selection;
		}

		@Override
		public void setSelection(ISelection selection) {
			this.selection = selection;
			selectionChanged(new SelectionChangedEvent(this, getSelection()));
		}

		private void selectionChanged(final SelectionChangedEvent event) {
			// pass on the notification to listeners
			for (final ISelectionChangedListener l: selectionChangedListeners) {
				SafeRunner.run(new SafeRunnable() {
					@Override
					public void run() {
						l.selectionChanged(event);
					}
				});
			}
		}
	}

	/**
	 * Initialize the actions of this contribution. This method will be invoked
	 * once before any calls are made to <code>filleContextMenu</code> or
	 * <code>setActionBars</code> but after the control for the page has been
	 * created. As a result of this, the site of the configuration can be
	 * accessed. Subclasses may override this method but must invoke the
	 * overridden method.
	 *
	 * @param configuration the configuration for the part to which the
	 * contribution is associated
	 */
	public void initialize(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		if (visibleRootSelectionProvider != null) {
			configuration.addActionContribution(visibleRootSelectionProvider);
		}
	}

	/**
	 * This method is invoked whenever the model being displayed in the view
	 * changes. This includes when the input to the view changes and when the
	 * children of the input change. The default implementation of this method
	 * does nothing. Subclasses may override.
	 *
	 * @param root the root of the model being viewed
	 */
	public void modelChanged(ISynchronizeModelElement root) {
		// Do nothing by default
	}

	/**
	 * Dispose of the action group. Subclasses may override but must
	 * invoke the overridden method.
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (configuration != null) {
			configuration.removeActionContribution(this);
		}
	}

	/**
	 * Helper method to find the group of the given id for the page associated
	 * with the configuration of this action group. The id of the returned group
	 * will not match that of the provided id since the group must be modified
	 * to ensure that groups are unique across pages.
	 *
	 * @param menu the menu
	 * @param groupId the id of the group being searched for
	 * @return the group for the given id or <code>null</code>
	 */
	protected IContributionItem findGroup(IContributionManager menu, String groupId) {
		if(menu == null) return null;
		IContributionItem item = menu.find(((SynchronizePageConfiguration)configuration).getGroupId(groupId));
		if (item == null) {
			// Context menus do not change the id
			item = menu.find(groupId);
		}
		return item;
	}

	/**
	 * Helper method to add an action to a group in a menu. The action is only
	 * added to the menu if the group exists in the menu. Calling this method
	 * also has no effect if either the menu or action are <code>null</code>.
	 *
	 * @param manager the menu manager
	 * @param groupId the group to append the action to
	 * @param action the action to add
	 * @return <code>true</code> if the group exists and the action was added
	 * and <code>false</code> if the action was not added
	 */
	protected boolean appendToGroup(IContributionManager manager, String groupId, IAction action) {
		if (internalAppendToGroup(manager, groupId, action)) {
			registerActionWithWorkbench(action);
			return true;
		}
		return false;
	}

	private boolean internalAppendToGroup(IContributionManager manager, String groupId, IAction action) {
		if (manager == null || action == null) return false;
		IContributionItem group = findGroup(manager, groupId);
		if (group != null) {
			manager.appendToGroup(group.getId(), action);
			return true;
		}
		return false;
	}

	/**
	 * Helper method to add a contribution item to a group in a menu. The item
	 * is only added to the menu if the group exists in the menu. Calling this
	 * method also has no effect if either the menu or item are
	 * <code>null</code>.
	 *
	 * @param manager the menu manager
	 * @param groupId the group to append the action to
	 * @param item the item to add
	 * @return <code>true</code> if the group exists and the action was added
	 * and <code>false</code> if the action was not added
	 */
	protected boolean appendToGroup(IContributionManager manager, String groupId, IContributionItem item) {
		if (manager == null || item == null) return false;
		IContributionItem group = findGroup(manager, groupId);
		if (group != null) {
			manager.appendToGroup(group.getId(), item);
			return true;
		}
		return false;
	}

	/**
	 * Helper method that can be invoked during initialization to add an action
	 * to a particular menu (one of P_TOOLBAR_MENU, P_VIEW_MENU, P_CONTEXT_MENU
	 * from ISynchronizePageConfiguration). The action is added to the given
	 * group if it is present. Otherwise the action is not added to the menu.
	 *
	 * @param menuId the menu id (one of P_TOOLBAR_MENU, P_VIEW_MENU,
	 * P_CONTEXT_MENU from ISynchronizePageConfiguration)
	 * @param groupId the group id in the menu to which the action is to be
	 * added
	 * @param action the action to be added
	 */
	protected void appendToGroup(String menuId, String groupId, IAction action) {
		registerActionWithWorkbench(action);
		internalAppendToGroup(menuId, groupId, action);
	}

	/**
	 * Register this action with the workbench so that it can participate in keybindings and
	 * retargetable actions.
	 *
	 * @param action the action to register
	 */
	private void registerActionWithWorkbench(IAction action) {
		ISynchronizePageSite site = configuration.getSite();
		String id = action.getId();
		if (id != null) {
			site.getActionBars().setGlobalActionHandler(id, action);
			IKeyBindingService keyBindingService = site.getKeyBindingService();
			if(keyBindingService != null)
				keyBindingService.registerAction(action);
		}
	}

	/**
	 * Helper method that can be invoked during initialization to add an item to
	 * a particular menu (one of P_TOOLBAR_MENU, P_VIEW_MENU, P_CONTEXT_MENU
	 * from ISynchronizePageConfiguration). The item is added to the given group
	 * if it is present. Otherwise the item is not added to the menu.
	 *
	 * @param menuId the menu id (one of P_TOOLBAR_MENU, P_VIEW_MENU,
	 * P_CONTEXT_MENU from ISynchronizePageConfiguration)
	 * @param groupId the group id in the menu to which the item is to be added
	 * @param item the item to be added
	 */
	protected void appendToGroup(String menuId, String groupId, IContributionItem item) {
		internalAppendToGroup(menuId, groupId, item);
	}

	/**
	 * Return a selection provider whose selection includes all roots of the
	 * elements visible in the page. Selection change events are fired when the
	 * elements visible in the view change.
	 *
	 * @return a selection provider whose selection is the roots of all
	 * elements visible in the page
	 */
	protected ISelectionProvider getVisibleRootsSelectionProvider() {
		if (visibleRootSelectionProvider == null) {
			ISynchronizeModelElement root = null;
			if (configuration != null) {
				root = (ISynchronizeModelElement)configuration.getProperty(SynchronizePageConfiguration.P_MODEL);
			}
			visibleRootSelectionProvider = new VisibleRootsSelectionProvider(root);
			if (configuration != null) {
				configuration.addActionContribution(visibleRootSelectionProvider);
			}
		}
		return visibleRootSelectionProvider;
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		fillMenu(menu, ISynchronizePageConfiguration.P_CONTEXT_MENU);
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		if (actionBars != null) {
			fillMenu(actionBars.getMenuManager(), ISynchronizePageConfiguration.P_VIEW_MENU);
			fillMenu(actionBars.getToolBarManager(), ISynchronizePageConfiguration.P_TOOLBAR_MENU);
		}
	}

	private void fillMenu(IContributionManager menu, String menuId) {
		Map<String, List<Object>> groups = menuContributions.get(menuId);
		if (menu != null && groups != null) {
			for (String groupId : groups.keySet()) {
				List actions = groups.get(groupId);
				if (actions != null) {
					for (Object element : actions) {
						if (element instanceof IAction) {
							// Call the internal method to avoid registering the action
							// as a global handler since it would have been registered
							// when the action was added to the menuContributions
							internalAppendToGroup(menu, groupId, (IAction)element);
						} else if (element instanceof IContributionItem) {
							appendToGroup(menu, groupId, (IContributionItem)element);
						}
					}
				}
			}
		}
	}

	private void internalAppendToGroup(String menuId, String groupId, Object action) {
		Map<String, List<Object>> groups = menuContributions.get(menuId);
		if (groups == null) {
			groups = new HashMap<>();
			menuContributions.put(menuId, groups);
		}
		List<Object> actions = groups.get(groupId);
		if (actions == null) {
			actions = new ArrayList<>();
			groups.put(groupId, actions);
		}
		actions.add(action);
	}

	/**
	 * Return the configuration for the page to which the action group
	 * is associated.
	 * @return the configuration for the page to which the action group
	 * is associated
	 *
	 * @since 3.1
	 */
	public ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}
}
