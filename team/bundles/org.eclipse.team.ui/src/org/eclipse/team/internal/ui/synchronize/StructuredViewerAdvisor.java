/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.mapping.DiffTreeStatusLineContributionGroup;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

/**
 * A <code>StructuredViewerAdvisor</code> controls various UI
 * aspects of viewers that show {@link SyncInfoSet} like the context menu, toolbar,
 * content provider, label provider, navigation, and model provider. The
 * advisor allows decoupling viewer behavior from the viewers presentation. This
 * allows viewers that aren't in the same class hierarchy to re-use basic
 * behavior.
 * <p>
 * This advisor allows viewer contributions made in a plug-in manifest to
 * be scoped to a particular unique id. As a result the context menu for the
 * viewer can be configured to show object contributions for random id schemes.
 * To enable declarative action contributions for a configuration there are two
 * steps required:
 * </p>
 * <ul>
 * <li>Create a viewer contribution with a <code>targetID</code> that groups
 * sets of actions that are related. A common practice for synchronize view
 * configurations is to use the participant id as the targetID.
 *
 * <pre>
 *  &lt;viewerContribution
 *  id=&quot;org.eclipse.team.ccvs.ui.CVSCompareSubscriberContributions&quot;
 *  targetID=&quot;org.eclipse.team.cvs.ui.compare-participant&quot;&gt;
 *  ...
 * </pre>
 *
 * <li>Create a configuration instance with a <code>menuID</code> that
 * matches the targetID in the viewer contribution.
 * </ul>
 * <p>
 * Clients may subclass to add behavior for concrete structured viewers.
 * </p>
 *
 * @see TreeViewerAdvisor
 * @since 3.0
 */
public abstract class StructuredViewerAdvisor extends AbstractViewerAdvisor {

	// Property change listener which responds to:
	//    - working set selection by the user
	//    - decorator format change selected by the user
	private IPropertyChangeListener propertyListener = event -> {
		// Change to showing of sync state in text labels preference
		if(event.getProperty().equals(IPreferenceIds.SYNCVIEW_VIEW_SYNCINFO_IN_LABEL)) {
			StructuredViewer viewer = getViewer();
			if(viewer != null && !viewer.getControl().isDisposed()) {
				viewer.refresh(true /* update labels */);
			}
		}
	};
	private DiffTreeStatusLineContributionGroup statusLine;

	/**
	 * Create an advisor that will allow viewer contributions with the given <code>targetID</code>. This
	 * advisor will provide a presentation model based on the given sync info set. The model is disposed
	 * when the viewer is disposed.
	 *
	 * @param configuration
	 */
	public StructuredViewerAdvisor(ISynchronizePageConfiguration configuration) {
		super(configuration);
	}

	/**
	 * Install a viewer to be configured with this advisor. An advisor can only be installed with
	 * one viewer at a time. When this method completes the viewer is considered initialized and
	 * can be shown to the user.
	 * @param viewer the viewer being installed
	 */
	@Override
	public void initializeViewer(final StructuredViewer viewer) {
		super.initializeViewer(viewer);
		initializeListeners(viewer);
		hookContextMenu(viewer);
	}

	/**
	 * Must be called when an advisor is no longer needed.
	 */
	public void dispose() {
		if (getActionGroup() != null) {
			getActionGroup().dispose();
		}
		if (statusLine != null)
			statusLine.dispose();
		TeamUIPlugin.getPlugin().getPreferenceStore().removePropertyChangeListener(propertyListener);
	}

	/**
	 * Method invoked from <code>initializeViewer(Composite, StructuredViewer)</code>
	 * in order to initialize any listeners for the viewer.
	 *
	 * @param viewer the viewer being initialize
	 */
	protected void initializeListeners(final StructuredViewer viewer) {
		viewer.getControl().addDisposeListener(e -> StructuredViewerAdvisor.this.dispose());

		new OpenAndLinkWithEditorHelper(viewer) {

			@Override
			protected void activate(ISelection selection) {
				final int currentMode= OpenStrategy.getOpenMethod();
				try {
					OpenStrategy.setOpenMethod(OpenStrategy.DOUBLE_CLICK);
					handleOpen();
				} finally {
					OpenStrategy.setOpenMethod(currentMode);
				}
			}

			@Override
			protected void linkToEditor(ISelection selection) {
				// not supported by this part
			}

			@Override
			protected void open(ISelection selection, boolean activate) {
				handleOpen();
			}

		};

		viewer.addDoubleClickListener(event -> handleDoubleClick(viewer, event));

		viewer.addSelectionChangedListener(event -> updateActionBars(viewer.getStructuredSelection()));
		TeamUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(propertyListener);
	}

	/**
	 * Handles a double-click event. If <code>false</code> is returned,
	 * subclasses may handle the double click.
	 */
	protected boolean handleDoubleClick(StructuredViewer viewer, DoubleClickEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		Object node = selection.getFirstElement();
		if (node != null && node instanceof SyncInfoModelElement) {
			SyncInfoModelElement syncNode = (SyncInfoModelElement) node;
			IResource resource = syncNode.getResource();
			if (syncNode != null && resource != null && resource.getType() == IResource.FILE) {
				// The open is handled by the open strategy but say we handled
				// it so that overriding methods will not do anything
				return true;
			}
		}
		return false;
	}

	private void handleOpen() {
		Object o = getConfiguration().getProperty(SynchronizePageConfiguration.P_OPEN_ACTION);
		if (o instanceof IAction) {
			IAction action = (IAction)o;
			action.run();
		}
	}

	/**
	 * Method invoked from the synchronize page when the action
	 * bars are set. The advisor uses the configuration to determine
	 * which groups appear in the action bar menus and allows all
	 * action groups registered with the configuration to fill the action bars.
	 * @param actionBars the Action bars for the page
	 */
	@Override
	public final void setActionBars(IActionBars actionBars) {
		if(actionBars != null) {
			IToolBarManager manager = actionBars.getToolBarManager();

			// Populate the toolbar menu with the configured groups
			Object o = getConfiguration().getProperty(ISynchronizePageConfiguration.P_TOOLBAR_MENU);
			if (!(o instanceof String[])) {
				o = ISynchronizePageConfiguration.DEFAULT_TOOLBAR_MENU;
			}
			String[] groups = (String[])o;
			for (String group : groups) {
				// The groupIds must be converted to be unique since the toolbar is shared
				manager.add(new Separator(getGroupId(group)));
			}

			// view menu
			IMenuManager menu = actionBars.getMenuManager();
			if (menu != null) {
				// Populate the view drop-down menu with the configured groups
				o = getConfiguration().getProperty(ISynchronizePageConfiguration.P_VIEW_MENU);
				if (!(o instanceof String[])) {
					o = ISynchronizePageConfiguration.DEFAULT_VIEW_MENU;
				}
				groups = (String[]) o;
				initializeStatusLine(actionBars);
				for (String group : groups) {
					// The groupIds must be converted to be unique since the
					// view menu is shared
					menu.add(new Separator(getGroupId(group)));
				}
			}

			fillActionBars(actionBars);
		}
	}

	/**
	 * Initialize the status line
	 * @param actionBars the action bars
	 */
	protected void initializeStatusLine(IActionBars actionBars) {
		statusLine = new DiffTreeStatusLineContributionGroup(
				getConfiguration().getSite().getShell(),
				getConfiguration());
		IStatusLineManager statusLineMgr = actionBars.getStatusLineManager();
		if (statusLineMgr != null && statusLine != null) {
			statusLine.fillActionBars(actionBars);
		}
	}

	/*
	 * Method invoked from <code>initializeViewer(StructuredViewer)</code>
	 * in order to configure the viewer to call <code>fillContextMenu(StructuredViewer, IMenuManager)</code>
	 * when a context menu is being displayed in viewer.
	 *
	 * @param viewer the viewer being initialized
	 * @see fillContextMenu(StructuredViewer, IMenuManager)
	 */
	private void hookContextMenu(final StructuredViewer viewer) {
		String targetID = getContextMenuId(viewer);
		final MenuManager menuMgr = createContextMenuManager(targetID);

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(viewer, manager));
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		menu.addMenuListener(new MenuListener() {

			@Override
			public void menuHidden(MenuEvent e) {
			}

			// Hack to allow action contributions to update their
			// state before the menu is shown. This is required when
			// the state of the selection changes and the contributions
			// need to update enablement based on this.
			// TODO: Is this hack still needed
			@Override
			public void menuShown(MenuEvent e) {
				IContributionItem[] items = menuMgr.getItems();
				for (IContributionItem item : items) {
					if (item instanceof ActionContributionItem) {
						IAction actionItem = ((ActionContributionItem) item).getAction();
						if (actionItem instanceof SynchronizeModelAction) {
							((SynchronizeModelAction) actionItem).selectionChanged(viewer.getSelection());
						}
					}
				}
			}
		});
		viewer.getControl().setMenu(menu);
		registerContextMenu(viewer, menuMgr);
	}

	/**
	 * Create the menu manager to be used to manage the context
	 * menu of the viewer.
	 * @param targetID the context menu id
	 * @return the menu manager to be used to manage the context
	 * menu of the viewer
	 */
	protected MenuManager createContextMenuManager(String targetID) {
		return new MenuManager(targetID);
	}

	/**
	 * Register the context menu with the platform if appropriate.
	 * @param viewer the viewer
	 * @param menuMgr the context menu manager
	 */
	protected void registerContextMenu(final StructuredViewer viewer, MenuManager menuMgr) {
		String targetID = getContextMenuId(viewer);
		if (targetID != null) {
			IWorkbenchSite workbenchSite = getConfiguration().getSite().getWorkbenchSite();
			IWorkbenchPartSite ws = null;
			if (workbenchSite instanceof IWorkbenchPartSite)
				ws = (IWorkbenchPartSite)workbenchSite;
			if (ws != null) {
				ws.registerContextMenu(targetID, menuMgr, viewer);
			}
		}
	}

	/**
	 * Return the context menu id.
	 * @param viewer the viewer
	 * @return the context menu id
	 */
	protected String getContextMenuId(StructuredViewer viewer) {
		String targetID;
		Object o = getConfiguration().getProperty(ISynchronizePageConfiguration.P_OBJECT_CONTRIBUTION_ID);
		if (o instanceof String) {
			targetID = (String)o;
		} else {
			targetID = null;
		}
		return targetID;
	}

	/**
	 * Callback that is invoked when a context menu is about to be shown in the
	 * viewer. Subclasses must implement to contribute menus. Also, menus can
	 * contributed by creating a viewer contribution with a <code>targetID</code>
	 * that groups sets of actions that are related.
	 *
	 * @param viewer the viewer in which the context menu is being shown.
	 * @param manager the menu manager to which actions can be added.
	 */
	protected void fillContextMenu(StructuredViewer viewer, final IMenuManager manager) {
		addContextMenuGroups(manager);
		getActionGroup().setContext(new ActionContext(viewer.getSelection()));
		getActionGroup().fillContextMenu(manager);
	}

	/**
	 * Add the context menu groups to the context menu.
	 * @param manager the menu manager
	 */
	protected void addContextMenuGroups(final IMenuManager manager) {
		// Populate the menu with the configured groups
		Object o = getConfiguration().getProperty(ISynchronizePageConfiguration.P_CONTEXT_MENU);
		if (!(o instanceof String[])) {
			o = ISynchronizePageConfiguration.DEFAULT_CONTEXT_MENU;
		}
		String[] groups = (String[])o;
		for (String group : groups) {
			// There is no need to adjust the group ids in a context menu (see setActionBars)
			manager.add(new Separator(group));
		}
	}

	/**
	 * Invoked once when the action bars are set.
	 * @param actionBars the action bars
	 */
	protected void fillActionBars(IActionBars actionBars) {
		getActionGroup().fillActionBars(actionBars);
		updateActionBars(getViewer().getStructuredSelection());
		Object input = getViewer().getInput();
		if (input instanceof ISynchronizeModelElement) {
			getActionGroup().modelChanged((ISynchronizeModelElement) input);
		}
	}

	/**
	 * Invoked each time the selection in the view changes in order
	 * to update the action bars.
	 * @param selection the selection from the viewer
	 */
	protected void updateActionBars(IStructuredSelection selection) {
		ActionGroup group = getActionGroup();
		if (group != null) {
			group.setContext(new ActionContext(selection));
			group.updateActionBars();
		}
	}

	protected SynchronizePageActionGroup getActionGroup() {
		return (SynchronizePageActionGroup)getConfiguration();
	}

	private String getGroupId(String group) {
		return ((SynchronizePageConfiguration)getConfiguration()).getGroupId(group);
	}
}
