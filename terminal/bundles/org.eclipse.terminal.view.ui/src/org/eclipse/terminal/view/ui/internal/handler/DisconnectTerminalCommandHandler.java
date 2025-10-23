/*******************************************************************************
 * Copyright (c) 2014, 2018 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.terminal.view.ui.internal.handler;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.eclipse.terminal.view.ui.ITerminalsView;
import org.eclipse.terminal.view.ui.internal.tabs.TabFolderManager;

/**
 * Disconnect terminal connection command handler implementation.
 */
public class DisconnectTerminalCommandHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) ISelection selection,
			@Named(IServiceConstants.ACTIVE_PART) MPart activePart) {
		CTabItem item = null;

		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			if (element instanceof CTabItem && ((CTabItem) element).getData() instanceof ITerminalViewControl) {
				item = (CTabItem) element;
			}
		}

		if (item == null && activePart != null && activePart.getObject() instanceof ITerminalsView) {
			ITerminalsView view = (ITerminalsView) activePart.getObject();
			TabFolderManager mgr = view.getAdapter(TabFolderManager.class);
			if (mgr != null && mgr.getActiveTabItem() != null) {
				item = mgr.getActiveTabItem();
			}
		}

		if (item != null && item.getData() instanceof ITerminalViewControl) {
			ITerminalViewControl terminal = (ITerminalViewControl) item.getData();
			if (terminal != null && !terminal.isDisposed()) {
				terminal.disconnectTerminal();
			}
		}
	}

	@CanExecute
	public boolean canExecute(@Named(IServiceConstants.ACTIVE_PART) MPart activePart) {
		if (activePart != null && activePart.getObject() instanceof ITerminalsView) {
			ITerminalsView view = (ITerminalsView) activePart.getObject();
			TabFolderManager mgr = view.getAdapter(TabFolderManager.class);
			if (mgr != null && mgr.getActiveTabItem() != null) {
				CTabItem item = mgr.getActiveTabItem();
				if (item != null && item.getData() instanceof ITerminalViewControl) {
					ITerminalViewControl terminal = (ITerminalViewControl) item.getData();
					return terminal != null && !terminal.isDisposed();
				}
			}
		}
		return false;
	}

}
