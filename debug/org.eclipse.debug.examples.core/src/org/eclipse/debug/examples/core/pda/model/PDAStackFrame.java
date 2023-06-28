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
 *     Pawel Piech (Wind River) - ported PDA Virtual Machine to Java (Bug 261400)
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.model;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.examples.core.pda.protocol.PDAFrameData;

/**
 * PDA stack frame.
 */
public class PDAStackFrame extends PDADebugElement implements IStackFrame {

	private PDAThread fThread;
	private String fName;
	private int fPC;
	private IPath fFilePath;
	private int fId;

	/**
	 * Constructs a stack frame in the given thread with the given
	 * frame data.
	 *
	 * @param thread
	 * @param data frame data
	 * @param id stack frame id (0 is the bottom of the stack)
	 */
	public PDAStackFrame(PDAThread thread, PDAFrameData data, int id) {
		super(thread.getPDADebugTarget());
		fId = id;
		fThread = thread;
		init(data);
	}

	/**
	 * Initializes this frame based on its data
	 *
	 * @param data
	 */
	private void init(PDAFrameData data) {
		fFilePath = data.fFilePath;
		fPC = data.fPC + 1;
		fName = data.fFunction;
		IVariable[] vars = new IVariable[data.fVariables.length];
		for (int i = 0; i < data.fVariables.length; i++) {
			vars[i] = new PDAVariable(this, data.fVariables[i]);
		}
		fThread.setVariables(this, vars);
	}

	@Override
	public IThread getThread() {
		return fThread;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		return fThread.getVariables(this);
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return getVariables().length > 0;
	}

	@Override
	public int getLineNumber() throws DebugException {
		return fPC;
	}

	@Override
	public int getCharStart() throws DebugException {
		return -1;
	}

	@Override
	public int getCharEnd() throws DebugException {
		return -1;
	}

	@Override
	public String getName() throws DebugException {
		return fName;
	}

	@Override
	public IRegisterGroup[] getRegisterGroups() throws DebugException {
		return null;
	}

	@Override
	public boolean hasRegisterGroups() throws DebugException {
		return false;
	}

	@Override
	public boolean canStepInto() {
		return getThread().canStepInto();
	}

	@Override
	public boolean canStepOver() {
		return getThread().canStepOver();
	}

	@Override
	public boolean canStepReturn() {
		return getThread().canStepReturn();
	}

	@Override
	public boolean isStepping() {
		return getThread().isStepping();
	}

	@Override
	public void stepInto() throws DebugException {
		getThread().stepInto();
	}

	@Override
	public void stepOver() throws DebugException {
		getThread().stepOver();
	}

	@Override
	public void stepReturn() throws DebugException {
		getThread().stepReturn();
	}

	@Override
	public boolean canResume() {
		return getThread().canResume();
	}

	@Override
	public boolean canSuspend() {
		return getThread().canSuspend();
	}

	@Override
	public boolean isSuspended() {
		return getThread().isSuspended();
	}

	@Override
	public void resume() throws DebugException {
		getThread().resume();
	}

	@Override
	public void suspend() throws DebugException {
		getThread().suspend();
	}

	@Override
	public boolean canTerminate() {
		return getThread().canTerminate();
	}

	@Override
	public boolean isTerminated() {
		return getThread().isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		getThread().terminate();
	}

	/**
	 * Returns the name of the source file this stack frame is associated
	 * with.
	 *
	 * @return the name of the source file this stack frame is associated
	 * with
	 */
	public String getSourceName() {
		return fFilePath.lastSegment();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PDAStackFrame) {
			PDAStackFrame sf = (PDAStackFrame)obj;
			return sf.getThread().equals(getThread()) &&
				sf.getSourceName().equals(getSourceName()) &&
				sf.fId == fId;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getSourceName().hashCode() + fId;
	}

	/**
	 * Returns this stack frame's unique identifier within its thread
	 *
	 * @return this stack frame's unique identifier within its thread
	 */
	protected int getIdentifier() {
		return fId;
	}

	/**
	 * Returns the stack frame's thread's unique identifier
	 *
	 * @return this stack frame's thread's unique identifier
	 *
	 * @since 3.5
	 */
	protected int getThreadIdentifier() {
		return fThread.getIdentifier();
	}

}
