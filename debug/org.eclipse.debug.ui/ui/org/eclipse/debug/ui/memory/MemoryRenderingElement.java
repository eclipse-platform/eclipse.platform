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

package org.eclipse.debug.ui.memory;

import java.math.BigInteger;

import org.eclipse.debug.core.model.MemoryByte;

/**
 * A memory rendering element represents a set of memory bytes being
 * rendered in a memory rendering. Instances of this class are passed
 * to a rendering's label provider, color provider, and font provider
 * to be rendered.
 * <p>
 * Clients may instantiate this class. Clients may subclass this class to add
 * other members / settings as required by a rendering.
 * </p>
 * @since 3.1
 */
public class MemoryRenderingElement {
	private IMemoryRendering fRendering;
	private BigInteger fAddress;
	private MemoryByte[] fBytes;

	/**
	 * Constructs a new memory rendering element for the given rendering
	 * and specified bytes.
	 *
	 * @param rendering the rendering containing the memory block being rendered
	 * @param address the address at which the rendering is taking place
	 * @param bytes the memory bytes being rendered
	 */
	public MemoryRenderingElement(IMemoryRendering rendering, BigInteger address, MemoryByte[] bytes)
	{
		fRendering = rendering;
		fAddress = address;
		fBytes = bytes;
	}

	/**
	 * Returns the memory rendering in which bytes are being rendered.
	 *
	 * @return the memory rendering in which bytes are being rendered
	 */
	public IMemoryRendering getRendering()
	{
		return fRendering;
	}

	/**
	 * Returns the address at which bytes are being rendered.
	 *
	 * @return the address at which bytes are being rendered
	 */
	public BigInteger getAddress() {
		return fAddress;
	}


	/**
	 * Returns the memory bytes being rendered.
	 *
	 * @return the memory bytes being rendered
	 */
	public MemoryByte[] getBytes() {
		return fBytes;
	}
}
