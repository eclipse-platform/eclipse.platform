/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.IHandler;
import org.eclipse.core.expressions.Expression;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.LegacyHandlerSubmissionExpression;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.services.IServiceLocator;

/**
 * A synchronize page site for use in dialogs.
 */
public class DialogSynchronizePageSite implements ISynchronizePageSite {

	private final Shell shell;
	private ISelectionProvider selectionProvider;
	private IActionBars actionBars;
	private final boolean isModal;
	// Keybindings enabled in the dialog, these should be removed
	// when the dialog is closed.
	private List<IHandlerActivation> actionHandlers = new ArrayList<>(2);

	/**
	 * Create a site for use in a dialog
	 * @param shell the shell
	 * @param isModal whether the dialog is model
	 */
	public DialogSynchronizePageSite(Shell shell, boolean isModal) {
		this.shell = shell;
		this.isModal = isModal;
	}

	@Override
	public ISelectionProvider getSelectionProvider() {
		return selectionProvider;
	}

	@Override
	public Shell getShell() {
		return shell;
	}

	@Override
	public void setSelectionProvider(ISelectionProvider provider) {
		selectionProvider = provider;
	}

	@Override
	public IWorkbenchSite getWorkbenchSite() {
		return null;
	}

	@Override
	public IWorkbenchPart getPart() {
		return null;
	}

	@Override
	public IKeyBindingService getKeyBindingService() {
		return null;
	}

	@Override
	public void setFocus() {
	}

	@Override
	public IDialogSettings getPageSettings() {
		return null;
	}

	@Override
	public IActionBars getActionBars() {
		return actionBars;
	}

	@Override
	public boolean isModal() {
		return isModal;
	}

	/**
	 * Create the action-bars for this site.
	 * @param toolbar the toolbar for the action bar
	 */
	public void createActionBars(final IToolBarManager toolbar) {
		if (actionBars == null) {
			actionBars = new IActionBars() {
				@Override
				public void clearGlobalActionHandlers() {
				}
				@Override
				public IAction getGlobalActionHandler(String actionId) {
					return null;
				}
				@Override
				public IMenuManager getMenuManager() {
					return null;
				}
				@Override
				public IStatusLineManager getStatusLineManager() {
					return null;
				}
				@Override
				public IToolBarManager getToolBarManager() {
					return toolbar;
				}
				@Override
				public void setGlobalActionHandler(String actionId, IAction action) {
					if (actionId != null && !"".equals(actionId)) { //$NON-NLS-1$
						IHandler handler = new ActionHandler(action);
						Expression expression = new LegacyHandlerSubmissionExpression(null, shell, null);
						IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
						actionHandlers.add(handlerService.activateHandler(actionId, handler, expression));
					}
				}

				@Override
				public void updateActionBars() {
				}
				@Override
				public IServiceLocator getServiceLocator() {
					return null;
				}
			};
		}
	}

	/**
	 * Cleanup when the dialog is closed
	 */
	public void dispose() {
		IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
		actionHandlers.forEach(handlerService::deactivateHandler);
	}
}