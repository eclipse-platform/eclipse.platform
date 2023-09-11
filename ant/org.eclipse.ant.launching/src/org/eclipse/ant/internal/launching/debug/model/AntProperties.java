/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.launching.debug.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class AntProperties extends AntDebugElement implements IVariable {

	private IValue fValue;
	private final String fName;
	private boolean fValid = true;

	public AntProperties(AntDebugTarget target, String name) {
		super(target);
		fName = name;
	}

	@Override
	public synchronized IValue getValue() throws DebugException {
		int attempts = 0;
		while (!fValid && !getDebugTarget().isTerminated()) {
			try {
				wait(50);
			}
			catch (InterruptedException e) {
				// do nothing
			}
			if (attempts == 20 && !fValid && !getDebugTarget().isTerminated()) {
				throwDebugException(DebugModelMessages.AntProperties_1);
			}
			attempts++;
		}
		return fValue;
	}

	public IValue getLastValue() {
		return fValue;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public String getReferenceTypeName() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean hasValueChanged() {
		return false;
	}

	@Override
	public void setValue(String expression) {
		// do nothing
	}

	@Override
	public void setValue(IValue value) {
		fValue = value;
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

	public synchronized void setValid(boolean valid) {
		fValid = valid;
		notifyAll();
	}
}
