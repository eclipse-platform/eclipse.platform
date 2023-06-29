/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class PDAArrayEntry extends PDADebugElement implements IVariable {

	private IValue fValue;
	private int fIndex;

	/**
	 * Constructs a new array entry
	 *
	 * @param target debug target
	 * @param index index in the array
	 * @param value value of the entry
	 */
	public PDAArrayEntry(IDebugTarget target, int index, IValue value) {
		super(target);
		fValue = value;
		fIndex = index;
	}

	@Override
	public IValue getValue() throws DebugException {
		return fValue;
	}

	@Override
	public String getName() throws DebugException {
		return "[" + fIndex + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return "String"; //$NON-NLS-1$
	}

	@Override
	public boolean hasValueChanged() throws DebugException {
		return false;
	}

	@Override
	public void setValue(String expression) throws DebugException {
	}

	@Override
	public void setValue(IValue value) throws DebugException {
	}

	@Override
	public boolean supportsValueModification() {
		return false;
	}

	@Override
	public boolean verifyValue(String expression) throws DebugException {
		return false;
	}

	@Override
	public boolean verifyValue(IValue value) throws DebugException {
		return false;
	}

}
