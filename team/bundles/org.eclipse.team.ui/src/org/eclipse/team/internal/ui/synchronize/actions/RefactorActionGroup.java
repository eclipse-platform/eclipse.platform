/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SynchronizeView;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.DeleteResourceAction;
import org.eclipse.ui.actions.MoveResourceAction;
import org.eclipse.ui.actions.RenameResourceAction;
import org.eclipse.ui.navigator.INavigatorContentService;

/**
 * This action group is modeled after the class of the same name in
 * the org.eclipse.ui.workbench plugin. We couldn't reuse that class
 * because of a hard dependency on the navigator.
 */
public class RefactorActionGroup extends ActionGroup {

	private CopyToClipboardAction copyAction;
	private MoveResourceAction moveAction;
	private RenameResourceAction renameAction;
	private final ISynchronizePageSite site;
	private DeleteResourceAction deleteAction;
	private final INavigatorContentService navigatorContentService;

	public RefactorActionGroup(ISynchronizePageSite site) {
		this(site, null);
	}

	public RefactorActionGroup(ISynchronizePageSite site, INavigatorContentService navigatorContentService) {
		this.site = site;
		this.navigatorContentService = navigatorContentService;
		makeActions();
	}

	public void fillContextMenu(IMenuManager parentMenu, String groupId) {
		parentMenu.appendToGroup(groupId, copyAction);
		// the paste action has been already created in the Sync view
		IWorkbenchPart part = site.getPart();
		if (part instanceof SynchronizeView) {
			SynchronizeView sv = (SynchronizeView) part;
			IAction pasteAction = sv.getPastePatchAction();
			parentMenu.appendToGroup(groupId, pasteAction);
		}
		parentMenu.appendToGroup(groupId, deleteAction);
		parentMenu.appendToGroup(groupId, moveAction);
		parentMenu.appendToGroup(groupId, renameAction);
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
		actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteAction);
		actionBars.setGlobalActionHandler(ActionFactory.RENAME.getId(), renameAction);
		actionBars.setGlobalActionHandler(ActionFactory.MOVE.getId(), moveAction);
	}

	@Override
	public void updateActionBars() {
		copyAction.selectionChanged(getObjectSelection());
		deleteAction.selectionChanged(getObjectSelection());
		moveAction.selectionChanged(getObjectSelection());
		renameAction.selectionChanged(getObjectSelection());
	}

	protected void makeActions() {
		final IShellProvider shellProvider = site.getWorkbenchSite();
		final ISharedImages images = PlatformUI.getWorkbench()
				.getSharedImages();

		copyAction = new CopyToClipboardAction(site.getShell(), navigatorContentService);
		copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
		copyAction.setImageDescriptor(images
				.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		copyAction.setDisabledImageDescriptor(images
				.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));

		deleteAction = new DeleteResourceAction(shellProvider) {
			@Override
			protected List getSelectedResources() {
				return getSelection().toList();// Arrays.asList(Utils.getResources(getSelection().toArray()));
			}

			@Override
			protected boolean updateSelection(IStructuredSelection selection) {
				return super.updateSelection(selection)
						&& allResourcesAreOfType(selection, IResource.PROJECT
								| IResource.FOLDER | IResource.FILE);
			}
		};
		deleteAction
				.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_DELETE);
		deleteAction.setImageDescriptor(images
				.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		deleteAction.setDisabledImageDescriptor(images
				.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE_DISABLED));

		moveAction = new MoveResourceAction(shellProvider) {
			@Override
			protected boolean updateSelection(IStructuredSelection selection) {
				return super.updateSelection(selection)
						&& allResourcesAreOfType(selection, IResource.PROJECT
								| IResource.FOLDER | IResource.FILE);
			}
		};
		moveAction.setActionDefinitionId(IWorkbenchCommandConstants.FILE_MOVE);

		renameAction = new RenameResourceAction(shellProvider) {
			@Override
			protected boolean updateSelection(IStructuredSelection selection) {
				return super.updateSelection(selection)
						&& allResourcesAreOfType(selection, IResource.PROJECT
								| IResource.FOLDER | IResource.FILE);
			}
		};
		renameAction
				.setActionDefinitionId(IWorkbenchCommandConstants.FILE_RENAME);
	}

	private IStructuredSelection getSelection() {
		final ISelection selection= getContext().getSelection();

		if (!(selection instanceof IStructuredSelection))
			return new StructuredSelection();

		return new StructuredSelection(Utils.getResources(((IStructuredSelection)selection).toArray()));
	}

	private IStructuredSelection getObjectSelection() {
		final ISelection selection= getContext().getSelection();

		if (!(selection instanceof IStructuredSelection))
			return new StructuredSelection();

		return (IStructuredSelection)selection;
	}

	private boolean allResourcesAreOfType(IStructuredSelection selection, int resourceMask) {
		Iterator resources = selection.iterator();
		while (resources.hasNext()) {
			Object next = resources.next();
			IResource resource = null;
			if (next instanceof IResource) {
				resource = (IResource)next;
			} else if (next instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable)next;
				resource = adaptable.getAdapter(IResource.class);
			}
			if(resource == null) {
				IResource[] r = Utils.getResources(new Object[] {next});
				if(r.length == 1) {
					resource = r[0];
				}
			}
			if (resource == null || (resource.getType() & resourceMask) == 0) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void dispose() {
		super.dispose();
		copyAction.dispose();
	}
}
