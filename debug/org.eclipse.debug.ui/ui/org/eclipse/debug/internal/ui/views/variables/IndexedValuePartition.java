/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.variables;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IIndexedValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;

/**
 * A parition (subrange) of values of an indexed value
 */
public class IndexedValuePartition implements IIndexedValue {

	// the starting offset of this parition, into the associated collection
	private int fOffset;

	// the length of this partition
	private int fLength;

	// the indexed value
	private IIndexedValue fValue;

	/**
	 * Creates a parition for an indexed value.
	 *
	 * @param value indexed value
	 * @param offset beginning offset of this partition (into the value)
	 * @param length the length of this parition
	 */
	public IndexedValuePartition(IIndexedValue value, int offset, int length) {
		fValue = value;
		fOffset = offset;
		fLength = length;
	}

	@Override
	public int getSize() {
		return fLength;
	}

	@Override
	public IVariable getVariable(int offset) throws DebugException {
		return fValue.getVariable(offset);
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return fValue.getReferenceTypeName();
	}

	@Override
	public String getValueString() {
		return IInternalDebugCoreConstants.EMPTY_STRING;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		return getVariables(fOffset, fLength);
	}

	@Override
	public boolean hasVariables() {
		return fLength > 0;
	}

	@Override
	public boolean isAllocated() throws DebugException {
		return fValue.isAllocated();
	}

	@Override
	public IDebugTarget getDebugTarget() {
		return fValue.getDebugTarget();
	}

	@Override
	public ILaunch getLaunch() {
		return fValue.getLaunch();
	}

	@Override
	public String getModelIdentifier() {
		return fValue.getModelIdentifier();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return fValue.getAdapter(adapter);
	}

	@Override
	public IVariable[] getVariables(int offset, int length) throws DebugException {
		return fValue.getVariables(offset, length);
	}

	@Override
	public int getInitialOffset() {
		return fOffset;
	}

}
