/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
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
package org.eclipse.debug.examples.internal.memory.core;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.osgi.util.NLS;

/**
 *
 *
 */
public class SampleStackFrame extends DebugElement implements IStackFrame {

	private SampleThread fThread;
	private SampleRegisterGroup fRegisterGroup;
	private long timeStamp;
	private String fName;

	/**
	 * Constructs a SampleStackFrame
	 *
	 * @param thread
	 * @param name
	 */
	public SampleStackFrame(SampleThread thread, String name) {
		super(thread.getDebugTarget());
		fThread = thread;
		fName = name;
		timeStamp = System.currentTimeMillis();
	}

	@Override
	public IThread getThread() {
		return fThread;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {

		return new IVariable[] { new SampleVariable(this, "sampleVariable") }; //$NON-NLS-1$
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ILaunch.class) {
			return (T) getLaunch();
		}
		return super.getAdapter(adapter);
	}

	@Override
	public int getLineNumber() throws DebugException {
		return 0;
	}

	@Override
	public int getCharStart() throws DebugException {
		return 0;
	}

	@Override
	public int getCharEnd() throws DebugException {
		return 0;
	}

	@Override
	public String getName() throws DebugException {
		return NLS.bind(Messages.SampleStackFrame_0, new Object[] {
				fName, timeStamp });
	}

	@Override
	public IRegisterGroup[] getRegisterGroups() throws DebugException {
		if (fRegisterGroup == null) {
			fRegisterGroup = new SampleRegisterGroup(this);
		}
		return new IRegisterGroup[] { fRegisterGroup };
	}

	@Override
	public boolean hasRegisterGroups() throws DebugException {
		return true;
	}

	@Override
	public String getModelIdentifier() {
		return fThread.getModelIdentifier();
	}

	@Override
	public IDebugTarget getDebugTarget() {
		return fThread.getDebugTarget();
	}

	@Override
	public ILaunch getLaunch() {
		return fThread.getDebugTarget().getLaunch();
	}

	@Override
	public boolean canStepInto() {
		return false;
	}

	@Override
	public boolean canStepOver() {
		return fThread.canStepOver();
	}

	@Override
	public boolean canStepReturn() {
		return false;
	}

	@Override
	public boolean isStepping() {
		return false;
	}

	@Override
	public void stepInto() throws DebugException {

	}

	@Override
	public void stepOver() throws DebugException {
		fThread.stepOver();
	}

	@Override
	public void stepReturn() throws DebugException {

	}

	@Override
	public boolean canResume() {
		return fThread.canResume();
	}

	@Override
	public boolean canSuspend() {
		return fThread.canSuspend();
	}

	@Override
	public boolean isSuspended() {
		return fThread.isSuspended();
	}

	@Override
	public void resume() throws DebugException {
		fThread.resume();

	}

	@Override
	public void suspend() throws DebugException {
		fThread.suspend();
	}

	@Override
	public boolean canTerminate() {
		return fThread.canTerminate();
	}

	@Override
	public boolean isTerminated() {
		return fThread.isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		fThread.terminate();

	}
}
