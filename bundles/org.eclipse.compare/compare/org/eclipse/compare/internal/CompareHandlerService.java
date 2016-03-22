/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.ICompareContainer;
import org.eclipse.core.expressions.Expression;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.services.IServiceLocator;

public class CompareHandlerService {
	
	private final List fActivations = new ArrayList();
	private final Expression fExpression;
	private ICompareContainer fContainer;
	private boolean fDisposed;
	private List fPaneActivations = new ArrayList();
	private IHandlerService fHandlerService;

	public static CompareHandlerService createFor(ICompareContainer container, Shell shell) {
		IServiceLocator serviceLocator = container.getServiceLocator();
		if (serviceLocator != null) {
			IHandlerService service = (IHandlerService)serviceLocator.getService(IHandlerService.class);
			if (service != null)
				return new CompareHandlerService(container, null);
		}
		if (container.getWorkbenchPart() == null && shell != null) {
			// We're in a dialog so we can use an active shell expression
			IHandlerService service = (IHandlerService)PlatformUI.getWorkbench().getService(IHandlerService.class);
			if (service != null) {
				Expression e = new ActiveShellExpression(shell);
				return new CompareHandlerService(container, e);
			}
		}
		return new CompareHandlerService(null, null);
	}
	
	private CompareHandlerService(ICompareContainer container,
			Expression expression) {
		fContainer = container;
		fExpression = expression;
		initialize();
	}
	
	public void registerAction(IAction action, String commandId) {
		IHandlerService handlerService = getHandlerService();
		if (handlerService == null)
			return;
		action.setActionDefinitionId(commandId);
		IHandlerActivation activation;
		if (fExpression == null) {
			activation = handlerService.activateHandler(commandId, new ActionHandler(action));
		} else {
			activation = handlerService.activateHandler(commandId, new ActionHandler(action), fExpression);
		}
		if (activation != null) {
			fActivations .add(activation);
		}
	}
	
	private IHandlerService getHandlerService() {
		if (fDisposed)
			return null;
		return fHandlerService;
	}

	private void initialize() {
		if (fHandlerService == null) {
			IServiceLocator serviceLocator = fContainer.getServiceLocator();
			if (serviceLocator != null) {
				IHandlerService service = (IHandlerService)serviceLocator.getService(IHandlerService.class);
				if (service != null)
					fHandlerService = service;
			}
			if (fHandlerService == null && fContainer.getWorkbenchPart() == null && fExpression != null) {
				// We're in a dialog so we can use an active shell expression
				IHandlerService service = (IHandlerService)PlatformUI.getWorkbench().getService(IHandlerService.class);
				if (service != null) {
					fHandlerService = service;
				}
			}
		}
	}

	public void setGlobalActionHandler(String actionId, IAction actionHandler) {
		IActionBars bars = getActionBars();
		if (bars != null) {
			bars.setGlobalActionHandler(actionId, actionHandler);
			return;
		} else if (fExpression != null && actionHandler != null && actionHandler.getActionDefinitionId() != null) {
			IHandlerService service = getHandlerService();
			if (service != null) {
				IHandlerActivation activation = service.activateHandler(actionHandler.getActionDefinitionId(), new ActionHandler(actionHandler), fExpression);
				fPaneActivations.add(activation);
				return;
			}
		}
		// Remove the action definition id since we won't get key bindings
		if (actionHandler != null)
			actionHandler.setActionDefinitionId(null);
	}
	
	private void updateActionBars() {
		IActionBars bars = getActionBars();
		if (bars != null)
			bars.updateActionBars();
	}

	private void clearPaneActionHandlers() {
		if (!fPaneActivations.isEmpty()) {
			IHandlerService service = getHandlerService();
			if (service != null) {
				service.deactivateHandlers(fPaneActivations);
				fPaneActivations.clear();
			}
		}
	}
	
	private IActionBars getActionBars() {
		return fContainer.getActionBars();
	}

	public void dispose() {
		clearPaneActionHandlers();
		IHandlerService service = getHandlerService();
		if (service == null)
			return;
		service.deactivateHandlers(fActivations);
		fActivations.clear();
		fDisposed = true;
	}

	public void updatePaneActionHandlers(Runnable runnable) {
		clearPaneActionHandlers();
		runnable.run();
		updateActionBars();
	}
}
