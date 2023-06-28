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

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

/**
 * Abstract Sample Thread
 *
 */
public class SampleThread extends DebugElement implements IThread {

	private SampleDebugTarget fTarget;

	/**
	 * Constructs SampleThread
	 *
	 * @param target
	 */
	public SampleThread(SampleDebugTarget target) {
		super(target);
		fTarget = target;
		try {
			getStackFrames();
		} catch (DebugException e) {
			e.printStackTrace();
		}
		fireEvent(new DebugEvent(this, DebugEvent.SUSPEND));
	}

	@Override
	public IStackFrame[] getStackFrames() throws DebugException {
		return ((SampleDebugTarget) getDebugTarget()).getEngine().getStackframes(this);
	}

	@Override
	public boolean hasStackFrames() throws DebugException {

		return true;
	}

	@Override
	public int getPriority() throws DebugException {

		return 0;
	}

	@Override
	public IStackFrame getTopStackFrame() throws DebugException {
		return ((SampleDebugTarget) getDebugTarget()).getEngine().getStackframes(this)[0];
	}

	@Override
	public String getName() throws DebugException {
		return Messages.SampleThread_0;
	}

	@Override
	public IBreakpoint[] getBreakpoints() {

		return new IBreakpoint[0];
	}

	@Override
	public String getModelIdentifier() {
		return fTarget.getModelIdentifier();
	}

	@Override
	public IDebugTarget getDebugTarget() {
		return fTarget;
	}

	@Override
	public ILaunch getLaunch() {
		return fTarget.getLaunch();
	}

	@Override
	public boolean canResume() {
		return fTarget.canResume();
	}

	@Override
	public boolean canSuspend() {
		return fTarget.canSuspend();
	}

	@Override
	public boolean isSuspended() {
		return fTarget.isSuspended();
	}

	@Override
	public void resume() throws DebugException {
		fTarget.resume();

	}

	@Override
	public void suspend() throws DebugException {
		fTarget.suspend();

	}

	@Override
	public boolean canStepInto() {

		return false;
	}

	@Override
	public boolean canStepOver() {
		if (!fTarget.isTerminated()) {
			return true;
		}

		return fTarget.isSuspended();
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
		fTarget.resume();
		fTarget.suspend();
	}

	@Override
	public void stepReturn() throws DebugException {

	}

	@Override
	public boolean canTerminate() {
		return fTarget.canTerminate();
	}

	@Override
	public boolean isTerminated() {
		return fTarget.isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		fTarget.terminate();

	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ILaunch.class) {
			return (T) getLaunch();
		}
		return super.getAdapter(adapter);
	}
}
