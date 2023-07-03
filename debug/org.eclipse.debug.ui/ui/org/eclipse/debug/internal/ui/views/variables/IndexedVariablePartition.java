/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IIndexedValue;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * A variable containing a subset/range of values from an indexed value
 * (<code>IIndexedValue</code>).
 */
public class IndexedVariablePartition extends PlatformObject implements IVariable  {

	// the starting offset of this partition, into the associated collection
	private int fOffset;

	// the length of this partition
	private int fLength;

	// the root variable or expression containing the indexed value
	private IDebugElement fOriginalVariable;

	// the indexed value
	private IIndexedValue fOriginalValue;

	// sub-range of values
	private IIndexedValue fValuePartition;

	private String fName = null;

	/**
	 * Creates a partition for an indexed value.
	 *
	 * @param variable variable or expression containing the indexed value
	 * @param value indexed value
	 * @param offset beginning offset of this partition (into the value)
	 * @param length the length of this partition
	 */
	public IndexedVariablePartition(IDebugElement variable, IIndexedValue value, int offset, int length) {
		fOriginalVariable = variable;
		fOriginalValue = value;
		fOffset = offset;
		fLength = length;
		fValuePartition = new IndexedValuePartition(value, offset, length);
	}

	@Override
	public IValue getValue() {
		return fValuePartition;
	}

	@Override
	public String getName() {
		if (fName == null) {
			StringBuilder buf = new StringBuilder();
			buf.append("["); //$NON-NLS-1$
			buf.append(fOffset);
			buf.append("..."); //$NON-NLS-1$
			buf.append(fOffset + fLength - 1);
			buf.append("]"); //$NON-NLS-1$
			fName = buf.toString();
		}
		return fName;
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		if (fOriginalVariable instanceof IVariable) {
			IVariable variable = (IVariable) fOriginalVariable;
			return variable.getReferenceTypeName();
		}
		return IInternalDebugCoreConstants.EMPTY_STRING;
	}

	@Override
	public boolean hasValueChanged() {
		return false;
	}

	@Override
	public String getModelIdentifier() {
		return fOriginalValue.getModelIdentifier();
	}

	@Override
	public IDebugTarget getDebugTarget() {
		return fOriginalValue.getDebugTarget();
	}

	@Override
	public ILaunch getLaunch() {
		return fOriginalValue.getLaunch();
	}

	@Override
	public void setValue(String expression) throws DebugException {
		throw new DebugException(new Status(IStatus.ERROR, IDebugUIConstants.PLUGIN_ID, IDebugUIConstants.INTERNAL_ERROR, "Value modification not supported for indexed partitions.", null));  //$NON-NLS-1$
	}

	@Override
	public void setValue(IValue value) throws DebugException {
		throw new DebugException(new Status(IStatus.ERROR, IDebugUIConstants.PLUGIN_ID, IDebugUIConstants.INTERNAL_ERROR, "Value modification not supported for indexed partitions.", null));  //$NON-NLS-1$
	}

	@Override
	public boolean supportsValueModification() {
		return false;
	}

	@Override
	public boolean verifyValue(String expression) {
		return false;
	}

	@Override
	public boolean verifyValue(IValue value) {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IndexedVariablePartition) {
			IndexedVariablePartition partition = (IndexedVariablePartition)obj;
			return fOriginalVariable.equals(partition.fOriginalVariable) &&
				fOffset == partition.fOffset && fLength == partition.fLength;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return fOriginalVariable.hashCode() + fOffset;
	}

}
