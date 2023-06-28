/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
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
package org.eclipse.debug.examples.core.pda.model;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IMemoryBlock;

/**
 * Example memory block
 */
public class PDAMemoryBlock extends PDADebugElement implements IMemoryBlock {

	/**
	 * The bytes
	 */
	private byte[] fBytes = null;
	private long fStart, fLength;

	/**
	 * Constructs a new memory block
	 */
	public PDAMemoryBlock(PDADebugTarget target, long start, long length) {
		super(target);
		fBytes = new byte[(int)length];
		fStart = start;
		fLength = length;
		byte b = 0;
		for (int i = 0; i < fBytes.length; i++) {
			fBytes[i] = b++;
		}
	}

	@Override
	public long getStartAddress() {
		return fStart;
	}

	@Override
	public long getLength() {
		return fLength;
	}

	@Override
	public byte[] getBytes() throws DebugException {
		return fBytes;
	}

	@Override
	public boolean supportsValueModification() {
		return true;
	}

	@Override
	public void setValue(long offset, byte[] bytes) throws DebugException {
		int i = 0;
		long off = offset;
		while (off < fBytes.length && i < bytes.length) {
			fBytes[(int)off++] = bytes[i++];
		}
		fireChangeEvent(DebugEvent.CONTENT);
	}

}
