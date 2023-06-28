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
 *     Wind River Systems - added support for IToggleBreakpointsTargetFactory
 *     Pawel Piech (Wind River) - ported PDA Virtual Machine to Java (Bug 261400)
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.model;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.examples.core.pda.protocol.PDACommandResult;
import org.eclipse.debug.examples.core.pda.protocol.PDASetVarCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDAVarCommand;

/**
 * A variable in a PDA stack frame
 */
public class PDAVariable extends PDADebugElement implements IVariable {

	// name & stack frmae
	private String fName;
	private PDAStackFrame fFrame;

	/**
	 * Constructs a variable contained in the given stack frame
	 * with the given name.
	 *
	 * @param frame owning stack frame
	 * @param name variable name
	 */
	public PDAVariable(PDAStackFrame frame, String name) {
		super(frame.getPDADebugTarget());
		fFrame = frame;
		fName = name;
	}

	@Override
	public IValue getValue() throws DebugException {
		PDACommandResult result = sendCommand(new PDAVarCommand(
			fFrame.getThreadIdentifier(), getStackFrame().getIdentifier(), getName()));
		return new PDAValue(this, result.fResponseText);
	}

	@Override
	public String getName() throws DebugException {
		return fName;
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return "Thing"; //$NON-NLS-1$
	}

	@Override
	public boolean hasValueChanged() throws DebugException {
		return false;
	}

	@Override
	public void setValue(String expression) throws DebugException {
		sendCommand(new PDASetVarCommand(
			fFrame.getThreadIdentifier(), getStackFrame().getIdentifier(), getName(), expression));
		fireChangeEvent(DebugEvent.CONTENT);
	}

	@Override
	public void setValue(IValue value) throws DebugException {
	}

	@Override
	public boolean supportsValueModification() {
		return true;
	}

	@Override
	public boolean verifyValue(String expression) throws DebugException {
		return true;
	}

	@Override
	public boolean verifyValue(IValue value) throws DebugException {
		return false;
	}

	/**
	 * Returns the stack frame owning this variable.
	 *
	 * @return the stack frame owning this variable
	 */
	public PDAStackFrame getStackFrame() {
		return fFrame;
	}

}
