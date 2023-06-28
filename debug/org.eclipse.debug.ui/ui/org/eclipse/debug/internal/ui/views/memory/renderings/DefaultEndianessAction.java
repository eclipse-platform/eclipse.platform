/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.memory.renderings;

import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Restore default endianess.
 * For IMemoryBlockExtension, default is governed by the attributes set
 * in its memory bytes.
 * For IMemoryBlock, default is big endian.
 *
 */
public class DefaultEndianessAction implements IObjectActionDelegate {

	AbstractIntegerRendering fRendering;
	public DefaultEndianessAction() {
		super();
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	@Override
	public void run(IAction action) {
		if (fRendering != null)
		{
			if (fRendering.getMemoryBlock() instanceof IMemoryBlockExtension)
			{
				fRendering.setDisplayEndianess(RenderingsUtil.ENDIANESS_UNKNOWN);
			}
			else
			{
				fRendering.setDisplayEndianess(RenderingsUtil.BIG_ENDIAN);
			}
			fRendering.refresh();
		}

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection == null)
			return;

		if (selection instanceof IStructuredSelection)
		{
			Object obj = ((IStructuredSelection)selection).getFirstElement();
			if (obj == null)
				return;

			if (obj instanceof AbstractIntegerRendering)
			{
				fRendering = (AbstractIntegerRendering)obj;
			}
		}
	}

}
