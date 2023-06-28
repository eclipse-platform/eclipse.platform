/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *     Pawel Piech (Wind River) - ported PDA Virtual Machine to Java (Bug 261400)
 *******************************************************************************/
package org.eclipse.debug.examples.ui.pda.editor;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.examples.core.pda.model.PDAStackFrame;
import org.eclipse.debug.examples.core.pda.model.PDAThread;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;


public class PopFrameActionDelegate implements IObjectActionDelegate, IActionDelegate2 {

	private PDAThread fThread = null;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	@Override
	public void run(IAction action) {
		//#ifdef ex5
//#		// TODO: Exercise 5 - pop the top frame
		//#else
		try {
			fThread.popFrame();
		} catch (DebugException e) {
		}
		//#endif
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			Object element = ss.getFirstElement();
			if (element instanceof PDAStackFrame) {
				PDAStackFrame frame = (PDAStackFrame) element;
				//#ifdef ex5
//#				// TODO: Exercise 5 - enable the action if the frame's thread supports it
				//#else
				fThread = (PDAThread) frame.getThread();
				try {
					action.setEnabled(fThread.canPopFrame() && fThread.getTopStackFrame().equals(frame));
				} catch (DebugException e) {
				}
				return;
				//#endif
			}

		}
		action.setEnabled(false);
	}

	@Override
	public void init(IAction action) {
	}

	@Override
	public void dispose() {
		fThread = null;
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

}
