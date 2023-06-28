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

import java.math.BigInteger;

import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.memory.AbstractTableRendering;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Abstract label provider for an ITableMemoryViewTab
 *
 * @since 3.0
 */
abstract public class AbstractTableRenderingLabelProvider extends LabelProvider implements ITableLabelProvider{

	protected AbstractTableRendering fRendering;

	/**
	 *
	 * Constructor for MemoryViewLabelProvider
	 */
	public AbstractTableRenderingLabelProvider() {
		super();
	}

	public AbstractTableRenderingLabelProvider(AbstractTableRendering rendering){
		fRendering = rendering;
	}

	@Override
	public void dispose() {
		fRendering = null;
		super.dispose();
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {

		if (columnIndex == 0)
		{
			return DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_OBJECT_MEMORY);
		}
		else if (columnIndex > (fRendering.getBytesPerLine()/fRendering.getBytesPerColumn()))
		{
			return DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_OBJECT_MEMORY);
		}
		else
		{
			// if memory in the range has changed, return delta icon
			int startOffset = (columnIndex-1)*fRendering.getBytesPerColumn();
			int endOffset = startOffset + fRendering.getBytesPerColumn() - 1;
			if (((TableRenderingLine)element).isRangeChange(startOffset, endOffset)) {
				return DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_OBJECT_MEMORY_CHANGED);
			}
			return DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_OBJECT_MEMORY);
		}
	}


	@Override
	public String getColumnText(Object element, int columnIndex) {
		String columnLabel = null;

		if (columnIndex == 0)
		{
			columnLabel = ((TableRenderingLine)element).getAddress();

			// consult model presentation for address presentation
		}
		else if (columnIndex > (fRendering.getBytesPerLine()/fRendering.getBytesPerColumn()))
		{
			columnLabel = " "; //$NON-NLS-1$
		}
		else
		{
			int start = (columnIndex-1)*fRendering.getBytesPerColumn();
			int end = start + fRendering.getBytesPerColumn();

			MemoryByte[] bytes = ((TableRenderingLine)element).getBytes(start, end);
			BigInteger address = new BigInteger(((TableRenderingLine)element).getAddress(), 16);
			address = address.add(BigInteger.valueOf(start));

			columnLabel = fRendering.getString(fRendering.getRenderingId(), address, bytes);
		}
		return columnLabel;
	}
}
