/*******************************************************************************
 * Copyright (c) 2014, 2026 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * IBM Corporation - Update label for window state
 *******************************************************************************/
package org.eclipse.terminal.view.ui.internal.handler;

import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.terminal.view.ui.internal.Messages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

/**
 * Maximize view handler implementation.
 */
public class MaximizeViewHandler extends AbstractTriggerCommandHandler implements IElementUpdater {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		triggerCommand("org.eclipse.ui.window.maximizePart", null); //$NON-NLS-1$
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		if (isActivePartMaximized()) {
			element.setText(Messages.MaximizeViewHandler_minimize);
			element.setTooltip(Messages.MaximizeViewHandler_minimizeTooltip);
		} else {
			element.setText(Messages.MaximizeViewHandler_maximize);
			element.setTooltip(Messages.MaximizeViewHandler_maximizeTooltip);
		}
	}

	/**
	 * Returns {@code true} when the currently active part in the active workbench
	 * page is in the {@link IWorkbenchPage#STATE_MAXIMIZED} state.
	 */
	private static boolean isActivePartMaximized() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			return false;
		}
		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			return false;
		}
		IWorkbenchPartReference partRef = page.getActivePartReference();
		if (partRef == null) {
			return false;
		}
		return page.getPartState(partRef) == IWorkbenchPage.STATE_MAXIMIZED;
	}
}
