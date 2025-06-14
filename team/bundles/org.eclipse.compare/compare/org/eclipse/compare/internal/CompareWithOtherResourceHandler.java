/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
 *     Alex Blewitt <alex.blewitt@gmail.com> - replace new Boolean with Boolean.valueOf - https://bugs.eclipse.org/470344
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * This is a temporary replacement for CompareWithOtherResourceAction which was
 * available from "Compare With &gt; Other Resource...". See bug 264498.
 */
public class CompareWithOtherResourceHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		IWorkbenchPage workbenchPage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();

		// CompareAction#isEnabled(ISelection)
		CompareConfiguration cc = new CompareConfiguration();
		cc.setProperty(CompareEditor.CONFIRM_SAVE_PROPERTY, Boolean.FALSE);
		ResourceCompareInput input = new ResourceCompareInput(cc);

		int selectionSize = 0;
		if (selection instanceof IStructuredSelection) {
			selectionSize = ((IStructuredSelection) selection).toArray().length;
		}
		if (input.isEnabled(selection) || selectionSize == 1) {

			// CompareAction#run(ISelection)
			if (!input.setSelection(selection, workbenchPage.getWorkbenchWindow().getShell(), false)) {
				return null;
			}
			input.initializeCompareConfiguration();
			CompareUI.openCompareEditorOnPage(input, workbenchPage);
		}
		return null;
	}
}
