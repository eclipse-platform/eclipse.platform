/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.memory.AbstractTableRendering;
import org.eclipse.debug.ui.memory.IMemoryBlockTablePresentation;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.swt.graphics.Color;

/**
 * @since 3.0
 */
public class TableRenderingLabelProvider extends AbstractTableRenderingLabelProvider implements IColorProvider {

	private IMemoryBlockTablePresentation fTablePresentation;

	/**
	 * Constructor for MemoryViewLabelProvider
	 */
	public TableRenderingLabelProvider() {
		super();
	}

	public TableRenderingLabelProvider(AbstractTableRendering rendering){
		super(rendering);
		fTablePresentation = rendering.getAdapter(IMemoryBlockTablePresentation.class);
	}

	@Override
	public void dispose() {
		if (fTablePresentation != null) {
			fTablePresentation.dispose();
			fTablePresentation = null;
		}
		super.dispose();
	}

	@Override
	public Color getForeground(Object element) {
		if (element instanceof TableRenderingLine)
		{
			TableRenderingLine line = (TableRenderingLine)element;

			if (line.isMonitored) {
				return DebugUIPlugin.getPreferenceColor(IDebugUIConstants.PREF_MEMORY_HISTORY_KNOWN_COLOR);
			}
			return DebugUIPlugin.getPreferenceColor(IDebugUIConstants.PREF_MEMORY_HISTORY_UNKNOWN_COLOR);
		}
		return DebugUIPlugin.getPreferenceColor(IDebugUIConstants.PREF_MEMORY_HISTORY_KNOWN_COLOR);
	}

	@Override
	public Color getBackground(Object element) {

		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {

		String label = super.getColumnText(element, columnIndex);

		// consult model presentation for address presentation
		if (columnIndex == 0)
		{
			if (fTablePresentation != null)
			{
				String address = ((TableRenderingLine)element).getAddress();

				// get address presentation
				String tempLabel = fTablePresentation.getRowLabel(fRendering.getMemoryBlock(), new BigInteger(address, 16));

				if (tempLabel != null) {
					return tempLabel;
				}
			}
			return label;
		}
		return label;
	}
}
