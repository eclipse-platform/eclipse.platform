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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.examples.core.pda.model.PDAThread;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;

/**
 * Pushes a value onto the data stack.
 */
public class PushHandler extends AbstractDataStackViewHandler {

	@Override
protected void doExecute(DataStackView view, PDAThread thread, ISelection selection) throws ExecutionException {
		InputDialog dialog = new InputDialog(view.getSite().getShell(), "Specify Value", "Enter value to push", null, null); //$NON-NLS-1$ //$NON-NLS-2$
		if (dialog.open() == Window.OK) {
			try {
				thread.pushData(dialog.getValue());
			} catch (DebugException e) {
				throw new ExecutionException("Failed to execute push command", e); //$NON-NLS-1$
			}
		}
		view.getViewer().refresh();
	}

}
