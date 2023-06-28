/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.view.memory;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;

/**
 * Dummy memory block implementation.
 *
 * @since 3.1
 */
public class MemoryBlock implements IMemoryBlock {

	/**
	 * @see org.eclipse.debug.core.model.IMemoryBlock#getStartAddress()
	 */
	@Override
	public long getStartAddress() {
		return 0;
	}

	/**
	 * @see org.eclipse.debug.core.model.IMemoryBlock#getLength()
	 */
	@Override
	public long getLength() {
		return 100;
	}

	/**
	 * @see org.eclipse.debug.core.model.IMemoryBlock#getBytes()
	 */
	@Override
	public byte[] getBytes() throws DebugException {
		byte[] bytes = new byte[(int)getLength()];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte)i;
		}
		return bytes;
	}

	/**
	 * @see org.eclipse.debug.core.model.IMemoryBlock#supportsValueModification()
	 */
	@Override
	public boolean supportsValueModification() {
		return false;
	}

	/**
	 * @see org.eclipse.debug.core.model.IMemoryBlock#setValue(long, byte[])
	 */
	@Override
	public void setValue(long offset, byte[] bytes) throws DebugException {

	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugElement#getModelIdentifier()
	 */
	@Override
	public String getModelIdentifier() {
		return "no.debugger"; //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugElement#getDebugTarget()
	 */
	@Override
	public IDebugTarget getDebugTarget() {
		return null;
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugElement#getLaunch()
	 */
	@Override
	public ILaunch getLaunch() {
		return null;
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}
}
