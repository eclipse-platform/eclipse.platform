/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Show integers in Big Endian
 */
public class BigEndianAction implements IObjectActionDelegate {

	AbstractIntegerRendering fRendering;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	@Override
	public void run(IAction action) {

		if (fRendering == null) {
			return;
		}

		if (fRendering.getDisplayEndianess() != RenderingsUtil.BIG_ENDIAN){
			fRendering.setDisplayEndianess(RenderingsUtil.BIG_ENDIAN);
			fRendering.refresh();
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {

		if (selection == null) {
			return;
		}

		if (selection instanceof IStructuredSelection)
		{
			Object obj = ((IStructuredSelection)selection).getFirstElement();
			if (obj == null) {
				return;
			}

			if (obj instanceof AbstractIntegerRendering)
			{
				fRendering = (AbstractIntegerRendering)obj;
			}

			int endianess = RenderingsUtil.ENDIANESS_UNKNOWN;
			if (fRendering.getDisplayEndianess() == RenderingsUtil.ENDIANESS_UNKNOWN)
			{
				MemoryByte[] selectedBytes = fRendering.getSelectedAsBytes();
				for (int i=0; i<selectedBytes.length; i++)
				{
					if (!selectedBytes[i].isEndianessKnown())
					{
						endianess = RenderingsUtil.ENDIANESS_UNKNOWN;
						break;
					}
					if (i==0)
					{
						endianess = selectedBytes[i].isBigEndian()?RenderingsUtil.BIG_ENDIAN:RenderingsUtil.LITTLE_ENDIAN;
					}
					else
					{
						int byteEndianess = selectedBytes[i].isBigEndian()?RenderingsUtil.BIG_ENDIAN:RenderingsUtil.LITTLE_ENDIAN;
						if (endianess != byteEndianess)
						{
							endianess = RenderingsUtil.ENDIANESS_UNKNOWN;
							break;
						}
					}
				}
			} else {
				endianess = fRendering.getDisplayEndianess();
			}

			if (endianess == RenderingsUtil.BIG_ENDIAN) {
				action.setChecked(true);
			} else {
				action.setChecked(false);
			}
		}
	}
}
