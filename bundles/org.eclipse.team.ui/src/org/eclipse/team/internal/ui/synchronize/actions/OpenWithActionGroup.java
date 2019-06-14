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
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SaveablesCompareEditorInput;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.keys.IBindingService;

/**
 * This is the action group for the open actions. It contains open
 * actions for
 */
public class OpenWithActionGroup extends ActionGroup {

	private OpenFileInSystemEditorAction openFileAction;
	private OpenInCompareAction openInCompareAction;
	private final boolean includeOpenInCompare;
	private final ISynchronizePageConfiguration configuration;

	public OpenWithActionGroup(ISynchronizePageConfiguration configuration, boolean includeOpenInCompare) {
		this.configuration = configuration;
		this.includeOpenInCompare = includeOpenInCompare;
		makeActions();
	}

	protected void makeActions() {
		IWorkbenchSite ws = getSite().getWorkbenchSite();
		if (ws != null) {
			openFileAction = new OpenFileInSystemEditorAction(ws.getPage());
			if (includeOpenInCompare)
				openInCompareAction = new OpenInCompareAction(configuration);
		}
	}

	private ISynchronizeParticipant getParticipant() {
		return configuration.getParticipant();
	}

	private ISynchronizePageSite getSite() {
		return configuration.getSite();
	}

	public void fillContextMenu(IMenuManager menu, String groupId) {
		ISelection selection = getSite().getSelectionProvider().getSelection();
		if (selection instanceof IStructuredSelection && !hasFileMenu(menu)) {
			fillOpenWithMenu(menu, groupId, (IStructuredSelection)selection);
		}
	}

	private boolean hasFileMenu(IMenuManager menu) {
		return menu.find(openFileAction.getId()) != null
				|| menu.find(IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_QUICK_MENU) != null;
	}

	/**
	 * Adds the OpenWith submenu to the context menu.
	 *
	 * @param menu the context menu
	 * @param selection the current selection
	 */
	private void fillOpenWithMenu(IMenuManager menu, String groupId, IStructuredSelection selection) {

		// Only supported if at least one file is selected.
		if (selection == null || selection.size() < 1)
			return;
		Object[] elements = selection.toArray();
		IResource resources[] = Utils.getResources(elements);
		if (resources.length == 0) {
			if (openInCompareAction != null) {
				// We can still show the compare editor open if the element has
				// a compare input
				if (elements.length > 0) {
					ISynchronizeParticipant participant = getParticipant();
					if (participant instanceof ModelSynchronizeParticipant) {
						ModelSynchronizeParticipant msp = (ModelSynchronizeParticipant) participant;
						boolean allElementsHaveCompareInput = true;
						for (Object element : elements) {
							if (!msp.hasCompareInputFor(element)) {
								allElementsHaveCompareInput = false;
								break;
							}
						}
						if (allElementsHaveCompareInput) {
							menu.appendToGroup(groupId, openInCompareAction);
						}
					}
				}
			}
			return;
		}

		if (elements.length != resources.length){
			// Only supported if all the items are resources.
			return;
		}

		boolean allFiles = true;
		for (IResource resource : resources) {
			if (resource.getType() != IResource.FILE) {
				// Open actions are only supported if all the items are files.
				allFiles = false;
				break;
			}
		}

		if (allFiles) {
			if (openInCompareAction != null) {
				menu.appendToGroup(groupId, openInCompareAction);
			}
		}

		for (IResource resource : resources) {
			if (!resource.exists()) {
				// Only support non-compare actions if all resources exist.
				return;
			}
		}

		if (allFiles) {
			if (openFileAction != null) {
				openFileAction.selectionChanged(selection);
				menu.appendToGroup(groupId, openFileAction);
			}

			if (resources.length == 1) {
				// Only support the "Open With..." submenu if exactly one file is selected.
				IWorkbenchSite ws = getSite().getWorkbenchSite();
				if (ws != null) {
					MenuManager openWithSubmenu =
						new MenuManager(TeamUIMessages.OpenWithActionGroup_0);
					openWithSubmenu.add(new OpenWithMenu(ws.getPage(), resources[0]));
					menu.appendToGroup(groupId, openWithSubmenu);
				}
			}
		}

		// Add "Show In" submenu, available for any number of resources
		IWorkbenchSite ws = getSite().getWorkbenchSite();
		if (ws != null) {
			MenuManager showInSubmenu = new MenuManager(getShowInMenuLabel(),
					IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_QUICK_MENU);
			IContributionItem showInMenu = ContributionItemFactory.VIEWS_SHOW_IN
					.create(ws.getWorkbenchWindow());
			showInSubmenu.add(showInMenu);
			menu.appendToGroup(groupId, showInSubmenu);
		}
	}

	/**
	 * {@link SaveablesCompareEditorInput#getShowInMenuLabel}
	 *
	 * @return label for "Show In" menu
	 */
	private static String getShowInMenuLabel() {
		String keyBinding = null;

		IBindingService bindingService = PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		if (bindingService != null)
			keyBinding = bindingService
					.getBestActiveBindingFormattedFor(IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_QUICK_MENU);

		if (keyBinding == null)
			keyBinding = ""; //$NON-NLS-1$

		return NLS
				.bind(TeamUIMessages.SaveableCompareEditorInput_0, keyBinding);
	}

	public void openInCompareEditor() {
		if (openInCompareAction != null)
			openInCompareAction.run();
	}
}
