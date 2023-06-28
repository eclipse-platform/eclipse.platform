/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.elements.adapters;

import java.math.BigInteger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.memory.provisional.AbstractAsyncTableRendering;
import org.eclipse.debug.internal.ui.memory.provisional.MemoryViewPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.provisional.AsynchronousLabelAdapter;
import org.eclipse.debug.internal.ui.views.launch.DebugElementHelper;
import org.eclipse.debug.internal.ui.views.memory.renderings.AbstractBaseTableRendering;
import org.eclipse.debug.internal.ui.views.memory.renderings.MemorySegment;
import org.eclipse.debug.internal.ui.views.memory.renderings.TableRenderingContentDescriptor;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.memory.IMemoryBlockTablePresentation;
import org.eclipse.debug.ui.memory.MemoryRenderingElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

public class MemorySegmentLabelAdapter extends AsynchronousLabelAdapter {

	@Override
	protected String[] getLabels(Object element, IPresentationContext context)
			throws CoreException {

		if (context instanceof MemoryViewPresentationContext)
		{
			MemoryViewPresentationContext tableRenderingContext = (MemoryViewPresentationContext)context;
			if (tableRenderingContext.getRendering() != null && tableRenderingContext.getRendering() instanceof AbstractAsyncTableRendering)
			{
				AbstractAsyncTableRendering tableRendering = (AbstractAsyncTableRendering)tableRenderingContext.getRendering();
				TableRenderingContentDescriptor descriptor = tableRendering.getAdapter(TableRenderingContentDescriptor.class);
				if (descriptor != null)
				{
					String addressStr = getColumnText(element, 0, tableRendering, descriptor);
					int numColumns = tableRendering.getAddressableUnitPerLine() / tableRendering.getAddressableUnitPerColumn();

					String[] labels = new String[numColumns+2];
					labels[0] = addressStr;

					for (int i=0; i<=numColumns; i++)
					{
						labels[i+1] = getColumnText(element, i+1, tableRendering, tableRendering.getAdapter(TableRenderingContentDescriptor.class));
					}

					labels[labels.length - 1 ] = IInternalDebugCoreConstants.EMPTY_STRING;
					return labels;
				}
			}
		}
		return new String[0];
	}

	private String getColumnText(Object element, int columnIndex, AbstractAsyncTableRendering tableRendering, TableRenderingContentDescriptor descriptor) {
		String columnLabel = null;

		if (columnIndex == 0)
		{
			IMemoryBlockTablePresentation presentation = tableRendering.getMemoryBlock().getAdapter(IMemoryBlockTablePresentation.class);
			if (presentation != null)
			{
				String rowLabel = presentation.getRowLabel(tableRendering.getMemoryBlock(), ((MemorySegment)element).getAddress());
				if (rowLabel != null)
					return rowLabel;
			}

			columnLabel = ((MemorySegment)element).getAddress().toString(16).toUpperCase();

			int addressSize = descriptor.getAddressSize();
			int prefillLength = addressSize * 2 - columnLabel.length();
			StringBuilder buf = new StringBuilder();
			if (prefillLength > 0)
			{
				for (int i=0; i<prefillLength; i++)
				{
					buf.append("0"); //$NON-NLS-1$
				}
			}
			buf.append(columnLabel);
			return buf.toString();

		}
		else if (columnIndex > (tableRendering.getBytesPerLine()/tableRendering.getBytesPerColumn()))
		{
			columnLabel = " "; //$NON-NLS-1$
		}
		else
		{
			if (element instanceof MemorySegment)
			{
				MemorySegment segment = (MemorySegment)element;
				if (segment.getBytes().length != tableRendering.getBytesPerLine())
					return IInternalDebugCoreConstants.EMPTY_STRING;
			}

			ILabelProvider labelProvider = tableRendering.getAdapter(ILabelProvider.class);
			if (labelProvider != null && columnIndex > 0)
			{
				MemoryRenderingElement renderingElement = getMemoryRenderingElement(element, columnIndex, tableRendering);
				if (renderingElement != null) {
					String label = labelProvider.getText(renderingElement);
					if (label != null)
						return label;
				}
			}

			int start = (columnIndex-1)*tableRendering.getBytesPerColumn();
			MemoryByte[] bytes = ((MemorySegment)element).getBytes(start, tableRendering.getBytesPerColumn());
			BigInteger address = ((MemorySegment)element).getAddress();
			address = address.add(BigInteger.valueOf(start));

			columnLabel = tableRendering.getString(tableRendering.getRenderingId(), address, bytes);
		}
		return columnLabel;
	}

	@Override
	protected ImageDescriptor[] getImageDescriptors(Object element,
			IPresentationContext context) throws CoreException {
		if (context instanceof MemoryViewPresentationContext)
		{
			MemoryViewPresentationContext tableRenderingContext = (MemoryViewPresentationContext)context;
			if (tableRenderingContext.getRendering() != null && tableRenderingContext.getRendering() instanceof AbstractAsyncTableRendering)
			{
				AbstractAsyncTableRendering tableRendering = (AbstractAsyncTableRendering)tableRenderingContext.getRendering();
				int numColumns = tableRendering.getAddressableUnitPerLine() / tableRendering.getAddressableUnitPerColumn();

				ImageDescriptor[] images = new ImageDescriptor[numColumns+2];

				for (int i=0; i<=numColumns; i++)
				{
					images[i] = getColumnImageDescriptor(element, i, tableRendering);
				}

				images[images.length - 1 ] = null;
				return images;
			}
		}
		return new ImageDescriptor[0];
	}

	private ImageDescriptor getColumnImageDescriptor(Object element, int columnIndex, AbstractAsyncTableRendering tableRendering)
	{
		if (columnIndex == 0)
			return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_OBJECT_MEMORY);

		if (element instanceof MemorySegment)
		{
			MemorySegment segment = (MemorySegment)element;
			if (segment.getBytes().length != tableRendering.getBytesPerLine())
				return null;

			ILabelProvider labelProvider = tableRendering.getAdapter(ILabelProvider.class);
			if (labelProvider != null && columnIndex > 0)
			{
				MemoryRenderingElement renderingElement = getMemoryRenderingElement(element, columnIndex, tableRendering);
				if (renderingElement != null) {
					Image image = labelProvider.getImage(renderingElement);
					if (image != null)
					{
						return DebugElementHelper.getImageDescriptor(image);
					}
				}
			}

			int start = (columnIndex-1)*tableRendering.getBytesPerColumn();

			MemoryByte[] bytes = ((MemorySegment)element).getBytes(start, tableRendering.getBytesPerColumn());
			boolean allKnown = true;
			boolean unchanged = true;
			for (MemoryByte b : bytes) {
				if (!b.isHistoryKnown()) {
					allKnown = false;
				}
				if (b.isChanged()) {
					unchanged = false;
				}
			}

			if (allKnown)
			{
				// mark changed elements with changed icon
				if (!unchanged)
					return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_OBJECT_MEMORY_CHANGED);

			}
		}
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_OBJECT_MEMORY);
	}

	@Override
	protected FontData[] getFontDatas(Object element,
			IPresentationContext context) throws CoreException {
		if (context instanceof MemoryViewPresentationContext)
		{
			MemoryViewPresentationContext tableRenderingContext = (MemoryViewPresentationContext)context;
			if (tableRenderingContext.getRendering() != null && tableRenderingContext.getRendering() instanceof AbstractAsyncTableRendering)
			{
				AbstractAsyncTableRendering tableRendering = (AbstractAsyncTableRendering)tableRenderingContext.getRendering();
				int numColumns = tableRendering.getAddressableUnitPerLine() / tableRendering.getAddressableUnitPerColumn();

				FontData[] fontData = new FontData[numColumns+2];

				for (int i=0; i<fontData.length-1; i++)
				{
					fontData[i] = getColumnFontData(element, i, tableRendering);
				}
				return fontData;
			}
		}

		return new FontData[0];
	}

	private FontData getColumnFontData(Object element, int columnIndex, AbstractAsyncTableRendering tableRendering)
	{
		if (element instanceof MemorySegment)
		{
			MemorySegment segment = (MemorySegment)element;
			if (segment.getBytes().length != tableRendering.getBytesPerLine())
				return null;

			IFontProvider fontProvider = tableRendering.getAdapter(IFontProvider.class);
			if (fontProvider != null && columnIndex > 0)
			{
				MemoryRenderingElement renderingElement = getMemoryRenderingElement(element, columnIndex, tableRendering);
				if (renderingElement != null) {
					Font font = fontProvider.getFont(renderingElement);
					if (font != null)
						return font.getFontData()[0];
				}
			}
		}
		return null;
	}

	@Override
	protected RGB[] getForegrounds(Object element, IPresentationContext context)
			throws CoreException {

		if (context instanceof MemoryViewPresentationContext)
		{
			MemoryViewPresentationContext tableRenderingContext = (MemoryViewPresentationContext)context;
			if (tableRenderingContext.getRendering() != null && tableRenderingContext.getRendering() instanceof AbstractAsyncTableRendering)
			{
				AbstractAsyncTableRendering tableRendering = (AbstractAsyncTableRendering)tableRenderingContext.getRendering();
				int numColumns = tableRendering.getAddressableUnitPerLine() / tableRendering.getAddressableUnitPerColumn();

				RGB[] colors = new RGB[numColumns+2];

				for (int i=0; i<colors.length-1; i++)
				{
					colors[i] = getColumnForeground(element, i, tableRendering);
				}

				colors[colors.length-1] = null;

				return colors;
			}
		}

		return new RGB[0];
	}

	private RGB getColumnBackground(Object element, int columnIndex, AbstractAsyncTableRendering tableRendering)
	{
		if (columnIndex == 0)
			return null;

		if (element instanceof MemorySegment)
		{
			MemorySegment segment = (MemorySegment)element;
			if (segment.getBytes().length != tableRendering.getBytesPerLine())
				return null;

			IColorProvider colorProvider = tableRendering.getAdapter(IColorProvider.class);
			if (colorProvider != null && columnIndex > 0)
			{
				MemoryRenderingElement renderingElement = getMemoryRenderingElement(element, columnIndex, tableRendering);
				if (renderingElement != null) {
					Color color = colorProvider.getBackground(renderingElement);
					if (color != null)
						return color.getRGB();
				}
			}
		}
		return null;
	}

	private RGB getColumnForeground(Object element, int columnIndex, AbstractAsyncTableRendering tableRendering)
	{
		if (columnIndex == 0)
			return null;

		if (element instanceof MemorySegment)
		{
			MemorySegment segment = (MemorySegment)element;
			if (segment.getBytes().length != tableRendering.getBytesPerLine())
				return null;

			IColorProvider colorProvider = tableRendering.getAdapter(IColorProvider.class);
			if (colorProvider != null && columnIndex > 0)
			{
				MemoryRenderingElement renderingElement = getMemoryRenderingElement(element, columnIndex, tableRendering);
				if (renderingElement != null) {
					Color color = colorProvider.getForeground(renderingElement);
					if (color != null)
						return color.getRGB();
				}
			}

			int start = (columnIndex-1)*tableRendering.getBytesPerColumn();
			MemoryByte[] bytes = segment.getBytes(start, tableRendering.getBytesPerColumn());
			boolean allKnown = true;
			boolean unchanged = true;
			for (MemoryByte b : bytes) {
				if (!b.isHistoryKnown()) {
					allKnown = false;
				}
				if (b.isChanged()) {
					unchanged = false;
				}
			}

			if (allKnown)
			{
				// mark changed elements in changed color
				if (!unchanged)
					return DebugUIPlugin.getPreferenceColor(IDebugUIConstants.PREF_CHANGED_DEBUG_ELEMENT_COLOR).getRGB();

				return DebugUIPlugin.getPreferenceColor(IDebugUIConstants.PREF_MEMORY_HISTORY_KNOWN_COLOR).getRGB();
			}

			return DebugUIPlugin.getPreferenceColor(IDebugUIConstants.PREF_MEMORY_HISTORY_UNKNOWN_COLOR).getRGB();

		}
		return null;
	}

	@Override
	protected RGB[] getBackgrounds(Object element, IPresentationContext context)
			throws CoreException {

		if (context instanceof MemoryViewPresentationContext)
		{
			MemoryViewPresentationContext tableRenderingContext = (MemoryViewPresentationContext)context;
			if (tableRenderingContext.getRendering() != null && tableRenderingContext.getRendering() instanceof AbstractAsyncTableRendering)
			{
				AbstractAsyncTableRendering tableRendering = (AbstractAsyncTableRendering)tableRenderingContext.getRendering();
				int numColumns = tableRendering.getAddressableUnitPerLine() / tableRendering.getAddressableUnitPerColumn();

				RGB[] colors = new RGB[numColumns+2];

				for (int i=0; i<colors.length-1; i++)
				{
					colors[i] = getColumnBackground(element, i, tableRendering);
				}

				colors[colors.length-1] = null;

				return colors;
			}
		}

		return new RGB[0];
	}

	/**
	 * Returns a memory rendering element corresponding to the given element
	 * or <code>null</code> if none.
	 *
	 * @param element element to be rendered
	 * @param columnIndex column index at which to render
	 * @return memory rendering element or <code>null</code>
	 */
	private MemoryRenderingElement getMemoryRenderingElement(Object element, int columnIndex, AbstractBaseTableRendering rendering) {
		if (element instanceof MemorySegment) {
			MemorySegment line = (MemorySegment) element;
			BigInteger address = line.getAddress();
			int offset = (columnIndex - 1) * rendering.getBytesPerColumn();
			if (offset < rendering.getBytesPerLine() && (offset + rendering.getBytesPerColumn()) <= rendering.getBytesPerLine()) {
				return getMemoryRenderingElement(line, address, offset, rendering);
			}
		}
		return null;
	}

	private MemoryRenderingElement getMemoryRenderingElement(MemorySegment line, BigInteger lineAddress, int offset, AbstractBaseTableRendering rendering) {
		BigInteger cellAddress = lineAddress.add(BigInteger.valueOf(offset));
		MemoryByte[] bytes = line.getBytes(offset, rendering.getBytesPerColumn());
		// make a copy to ensure that the memory bytes are not overwritten
		// by clients
		MemoryByte[] copy = new MemoryByte[bytes.length];
		System.arraycopy(bytes, 0, copy, 0, bytes.length);
		MemoryRenderingElement renderingElement = new MemoryRenderingElement(
				rendering, cellAddress, copy);
		return renderingElement;
	}

}
