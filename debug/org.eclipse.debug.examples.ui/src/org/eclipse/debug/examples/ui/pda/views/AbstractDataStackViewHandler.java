/*******************************************************************************
 * Copyright (c) 2008, 2013 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.examples.ui.pda.views;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.examples.core.pda.model.PDAStackFrame;
import org.eclipse.debug.examples.core.pda.model.PDAThread;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Base class for command handlers for data stack view.
 */
abstract public class AbstractDataStackViewHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePartChecked(event);
		if (part instanceof DataStackView) {
			DataStackView view = (DataStackView)part;

			ISelection selection = DebugUITools.getDebugContextForEventChecked(event);
			if (selection instanceof IStructuredSelection) {
				Object element = ((IStructuredSelection)selection).getFirstElement();

				PDAThread thread = null;
				if (element instanceof PDAThread) {
					thread = (PDAThread)element;
				} else if (element instanceof PDAStackFrame) {
					thread = (PDAThread)((PDAStackFrame)element).getThread();
				}

				if (element != null) {
					doExecute(
						view,
						thread,
						HandlerUtil.getCurrentSelectionChecked(event));
				}
			}
		} else {
			throw new ExecutionException("Handler must be with DataStackView only"); //$NON-NLS-1$
		}
		return null;
	}

	/**
	 * Performs the actual handler operation.
	 *
	 * @param view The view that the handler was invoked in.
	 * @param target The current active debug target.
	 * @param selection The current selection in view.
	 */
	abstract protected void doExecute(DataStackView view, PDAThread target, ISelection selection) throws ExecutionException;
}
