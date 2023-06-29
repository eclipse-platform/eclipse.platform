/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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

import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.internal.ui.memory.provisional.AbstractAsyncTableRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingContainer;

/**
 * Abstract implementation to an integer rendering.
 * @since 3.1
 *
 */
public abstract class AbstractIntegerRendering extends AbstractAsyncTableRendering {

	private int fDisplayEndianess = RenderingsUtil.ENDIANESS_UNKNOWN;

	public AbstractIntegerRendering(String renderingId){
		super(renderingId);
	}

	@Override
	public void init(IMemoryRenderingContainer container, IMemoryBlock block) {
		super.init(container, block);

		// default to big endian for simple memory block
		if (!(block instanceof IMemoryBlockExtension))
			fDisplayEndianess = RenderingsUtil.BIG_ENDIAN;
	}

	/**
	 * @return Returns the currentEndianess.
	 */
	public int getDisplayEndianess() {
		return fDisplayEndianess;
	}

	/**
	 * @param currentEndianess The currentEndianess to set.
	 */
	public void setDisplayEndianess(int currentEndianess) {
		fDisplayEndianess = currentEndianess;
	}

	protected int getBytesEndianess(MemoryByte[] data) {
		int endianess = RenderingsUtil.ENDIANESS_UNKNOWN;

		if (!data[0].isEndianessKnown())
			return endianess;

		if (data[0].isBigEndian())
			endianess = RenderingsUtil.BIG_ENDIAN;
		else
			endianess = RenderingsUtil.LITTLE_ENDIAN;
		for (int i=1; i<data.length; i++)
		{
			// if endianess is not known for a byte, return unknown
			if (!data[i].isEndianessKnown())
				return RenderingsUtil.ENDIANESS_UNKNOWN;

			int byteEndianess = data[i].isBigEndian()?RenderingsUtil.BIG_ENDIAN:RenderingsUtil.LITTLE_ENDIAN;
			if (byteEndianess != endianess)
			{
				endianess = RenderingsUtil.ENDIANESS_UNKNOWN;
				break;
			}
		}
		return endianess;
	}
}
