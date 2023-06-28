/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package org.eclipse.debug.examples.ui.pda.adapters;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.examples.core.pda.model.PDADebugTarget;
import org.eclipse.debug.examples.ui.pda.DebugUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Action to add a memory block when a PDA debug target is selected
 */
public class AddPDAMemoryBlockAction implements IActionDelegate2{

	public AddPDAMemoryBlockAction() {
	}

	@Override
	public void run(IAction action) {
		IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
		if (window != null) {
			ISelectionService service = window.getSelectionService();
			ISelection selection = service.getSelection();
			PDADebugTarget target = getTarget(selection);
			if (target != null) {
				try {
					IMemoryBlock block = target.getMemoryBlock(0, 1024);
					DebugPlugin.getDefault().getMemoryBlockManager().addMemoryBlocks(new IMemoryBlock[]{block});
				} catch (DebugException e) {
				}
			}
		}

	}

	/**
	 * Returns the selected debug target or <code>null</code>.
	 *
	 * @param selection selection
	 * @return debug target from the selection or <code>null</code>
	 */
	private PDADebugTarget getTarget(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			if (ss.size() == 1) {
				Object element = ss.getFirstElement();
				if (element instanceof PDADebugTarget) {
					return (PDADebugTarget) element;
				}
			}
		}
		return null;
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		PDADebugTarget target = getTarget(selection);
		action.setEnabled(target != null && !target.isTerminated());
	}

	@Override
	public void init(IAction action) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}


}
