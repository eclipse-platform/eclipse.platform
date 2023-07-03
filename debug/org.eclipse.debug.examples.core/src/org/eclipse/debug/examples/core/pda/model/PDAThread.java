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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.examples.core.pda.protocol.PDADataCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDADropFrameCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDAEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDAListResult;
import org.eclipse.debug.examples.core.pda.protocol.PDANoSuchLabelEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDAPopDataCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDAPushDataCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDAResumeCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDAResumedEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDARunControlEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDAStackCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDAStackCommandResult;
import org.eclipse.debug.examples.core.pda.protocol.PDAStepCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDASuspendCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDASuspendedEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDAUnimplementedInstructionEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDAVMResumedEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDAVMSuspendedEvent;

/**
 * A PDA thread. A PDA VM is single threaded.
 */
public class PDAThread extends PDADebugElement implements IThread, IPDAEventListener {

	/**
	 * ID of this thread as reported by PDA.
	 */
	private final int fThreadId;

	/**
	 * Breakpoint this thread is suspended at or <code>null</code>
	 * if none.
	 */
	private IBreakpoint fBreakpoint;

	/**
	 * Whether this thread is stepping
	 */
	private volatile boolean fStepping = false;

	/**
	 * Whether this thread is suspended
	 */
	private boolean fSuspended = false;

	/**
	 * Most recent error event or <code>null</code>
	 */
	private String fErrorEvent;

	/**
	 * Table mapping stack frames to current variables
	 */
	private Map<IStackFrame, IVariable[]> fVariables = Collections.synchronizedMap(new HashMap<IStackFrame, IVariable[]>());

	/**
	 * Constructs a new thread for the given target
	 *
	 * @param target VM
	 */
	public PDAThread(PDADebugTarget target, int threadId) {
		super(target);
		fThreadId = threadId;
	}

	/**
	 * Called by the debug target after the thread is created.
	 *
	 * @since 3.5
	 */
	void start() {
		fireCreationEvent();
		getPDADebugTarget().addEventListener(this);
	}

	/**
	 * Called by the debug target before the thread is removed.
	 *
	 * @since 3.5
	 */
	void exit() {
		getPDADebugTarget().removeEventListener(this);
		fireTerminateEvent();
	}

	@Override
	public IStackFrame[] getStackFrames() throws DebugException {
		if (isSuspended()) {
			PDAStackCommandResult result = (PDAStackCommandResult)sendCommand(new PDAStackCommand(fThreadId));
			IStackFrame[] frames = new IStackFrame[result.fFrames.length];
			for (int i = 0; i < result.fFrames.length; i++) {
				frames[frames.length - i - 1] = new PDAStackFrame(this, result.fFrames[i], i);
			}
			return frames;
		}
		return new IStackFrame[0];
	}

	@Override
	public boolean hasStackFrames() throws DebugException {
		return isSuspended();
	}

	@Override
	public int getPriority() throws DebugException {
		return 0;
	}

	@Override
	public IStackFrame getTopStackFrame() throws DebugException {
		IStackFrame[] frames = getStackFrames();
		if (frames.length > 0) {
			return frames[0];
		}
		return null;
	}

	@Override
	public String getName() {
		return "Main thread"; //$NON-NLS-1$
	}

	@Override
	public synchronized IBreakpoint[] getBreakpoints() {
		if (fBreakpoint == null) {
			return new IBreakpoint[0];
		}
		return new IBreakpoint[]{fBreakpoint};
	}

	/**
	 * Notifies this thread it has been suspended by the given breakpoint.
	 *
	 * @param breakpoint breakpoint
	 */
	public synchronized void suspendedBy(IBreakpoint breakpoint) {
		fBreakpoint = breakpoint;
		suspended(DebugEvent.BREAKPOINT);
	}

	@Override
	public boolean canResume() {
		return isSuspended() && !getDebugTarget().isSuspended();
	}

	@Override
	public boolean canSuspend() {
		return !isSuspended();
	}

	@Override
	public boolean isSuspended() {
		if (getDebugTarget().isTerminated()) {
			return false;
		}
		if (getDebugTarget().isSuspended()) {
			return true;
		}
		synchronized (this) {
			return fSuspended;
		}
	}

	@Override
	public void resume() throws DebugException {
		//#ifdef ex2
//#		// TODO: Exercise 2 - send resume request to interpreter
		//#else
		sendCommand(new PDAResumeCommand(fThreadId));
		//#endif
	}

	@Override
	public void suspend() throws DebugException {
		//#ifdef ex2
//#		// TODO: Exercise 2 - send suspend request to interpreter
		//#else
		sendCommand(new PDASuspendCommand(fThreadId));
		//#endif
	}

	@Override
	public boolean canStepInto() {
		return false;
	}

	@Override
	public boolean canStepOver() {
		return isSuspended();
	}

	@Override
	public boolean canStepReturn() {
		return false;
	}

	@Override
	public boolean isStepping() {
		return fStepping;
	}

	@Override
	public void stepInto() throws DebugException {
	}

	@Override
	public void stepOver() throws DebugException {
		sendCommand(new PDAStepCommand(fThreadId));
	}

	@Override
	public void stepReturn() throws DebugException {
	}

	@Override
	public boolean canTerminate() {
		return !isTerminated();
	}

	@Override
	public boolean isTerminated() {
		return getDebugTarget().isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		getDebugTarget().terminate();
	}

	/**
	 * Sets whether this thread is stepping
	 *
	 * @param stepping whether stepping
	 */
	private synchronized void setStepping(boolean stepping) {
		fStepping = stepping;
	}

	/**
	 * Sets whether this thread is suspended
	 *
	 * @param suspended whether suspended
	 */
	private synchronized  void setSuspended(boolean suspended) {
		fSuspended = suspended;
	}

	/**
	 * Sets the most recent error event encountered, or <code>null</code>
	 * to clear the most recent error
	 *
	 * @param event one of 'unimpinstr' or 'nosuchlabel' or <code>null</code>
	 */
	private synchronized void setError(String event) {
		fErrorEvent = event;
	}

	/**
	 * Returns the most recent error event encountered since the last
	 * suspend, or <code>null</code> if none.
	 *
	 * @return the most recent error event encountered since the last
	 * suspend, or <code>null</code> if none
	 */
	public Object getError() {
		return fErrorEvent;
	}

	@Override
	public void handleEvent(PDAEvent _event) {
		if (_event instanceof PDARunControlEvent && fThreadId == ((PDARunControlEvent)_event).fThreadId) {
			PDARunControlEvent event = (PDARunControlEvent)_event;
			// clear previous state
			fBreakpoint = null;
			setStepping(false);

			// handle events
			if (event instanceof PDAResumedEvent || event instanceof PDAVMResumedEvent) {
				setSuspended(false);
				if ("step".equals(event.fReason)) { //$NON-NLS-1$
					setStepping(true);
					resumed(DebugEvent.STEP_OVER);
				//#ifdef ex2
	//#			}
	//#			// TODO: Exercise 2 - handle/fire "client" resume event
				//#else
				} else if ("client".equals(event.fReason)) { //$NON-NLS-1$
					resumed(DebugEvent.CLIENT_REQUEST);
				}
				//#endif
				//#ifdef ex5
	//#			// TODO: Exercise 5 - handle start of drop event
				//#else
				else if ("drop".equals(event.fReason)) { //$NON-NLS-1$
					resumed(DebugEvent.STEP_RETURN);
				}
				//#endif
			} else if (event instanceof PDASuspendedEvent || event instanceof PDAVMSuspendedEvent) {
				setSuspended(true);
				//#ifdef ex2
	//#			// TODO: Exercise 2 - handle/fire "client" suspend event
	//#			if (event.endsWith("step")) {
	//#				suspended(DebugEvent.STEP_END);
	//#			} else if (event.startsWith("suspended event") && getError() != null) {
	//#				exceptionHit();
	//#			}
				//#else
				if ("client".equals(event.fReason)) { //$NON-NLS-1$
					suspended(DebugEvent.CLIENT_REQUEST);
				} else if ("step".equals(event.fReason)) { //$NON-NLS-1$
					suspended(DebugEvent.STEP_END);
				} else if ("event".equals(event.fReason) && getError() != null) { //$NON-NLS-1$
					exceptionHit();
				}
				//#endif
				//#ifdef ex5
	//#			// TODO: Exercise 5 - handle end of drop event
				//#else
				else if ("drop".equals(event.fReason)) { //$NON-NLS-1$
					suspended(DebugEvent.STEP_END);
				}
				//#endif
			} else if (_event instanceof PDANoSuchLabelEvent ||
					   _event instanceof PDAUnimplementedInstructionEvent)
			{
				setError(event.fMessage);
			}
		}
	}

	/**
	 * Notification the target has resumed for the given reason.
	 * Clears any error condition that was last encountered and
	 * fires a resume event, and clears all cached variables
	 * for stack frames.
	 *
	 * @param detail reason for the resume
	 */
	private void resumed(int detail) {
		setError(null);
		fVariables.clear();
		fireResumeEvent(detail);
	}

	/**
	 * Notification the target has suspended for the given reason
	 *
	 * @param detail reason for the suspend
	 */
	private void suspended(int detail) {
		fireSuspendEvent(detail);
	}

	/**
	 * Notification an error was encountered. Fires a breakpoint
	 * suspend event.
	 */
	private void exceptionHit() {
		suspended(DebugEvent.BREAKPOINT);
	}

	/**
	 * Sets the current variables for the given stack frame. Called
	 * by PDA stack frame when it is created.
	 *
	 * @param frame
	 * @param variables
	 */
	protected void setVariables(IStackFrame frame, IVariable[] variables) {
		synchronized (fVariables) {
			fVariables.put(frame, variables);
		}
	}

	/**
	 * Returns the current variables for the given stack frame, or
	 * <code>null</code> if none.
	 *
	 * @param frame stack frame
	 * @return variables or <code>null</code>
	 */
	protected IVariable[] getVariables(IStackFrame frame) {
		synchronized (fVariables) {
			IVariable[] variables = fVariables.get(frame);
			if (variables == null) {
				return new IVariable[0];
			}
			return variables;
		}
	}

	/**
	 * Pops the top frame off the callstack.
	 *
	 * @throws DebugException
	 *
	 * @since 3.5
	 */
	public void popFrame() throws DebugException {
		//#ifdef ex5
//#		// TODO: Exercise 5 - send drop request
		//#else
		sendCommand(new PDADropFrameCommand(fThreadId));
		//#endif
	}

	/**
	 * Returns whether this thread can pop the top stack frame.
	 *
	 * @return whether this thread can pop the top stack frame
	 *
	 * @since 3.5
	 */
	public boolean canPopFrame() {
		//#ifdef ex5
//#		// TODO: Exercise 5 - allow pop if there is more than 1 frame on the stack
		//#else
		try {
			return getStackFrames().length > 1;
		} catch (DebugException e) {
		}
		//#endif
		return false;
	}

	/**
	 * Returns the values on the data stack (top down)
	 *
	 * @return the values on the data stack (top down)
	 *
	 * @since 3.5
	 */
	public IValue[] getDataStack() throws DebugException {
		PDAListResult result = (PDAListResult)sendCommand(new PDADataCommand(fThreadId));
		if (result.fValues.length > 0) {
			IValue[] values = new IValue[result.fValues.length];
			for (int i = 0; i < result.fValues.length; i++) {
				values[values.length - i - 1] = new PDAStackValue(this, result.fValues[i], i);
			}
			return values;
		}
		return new IValue[0];
	}

	/**
	 * Returns whether popping the data stack is currently permitted
	 *
	 * @return whether popping the data stack is currently permitted
	 *
	 * @since 3.5
	 */
	public boolean canPopData() {
		try {
			return !isTerminated() && isSuspended() && getDataStack().length > 0;
		} catch (DebugException e) {
		}
		return false;
	}

	/**
	 * Pops and returns the top of the data stack
	 *
	 * @return the top value on the stack
	 * @throws DebugException if the stack is empty or the request fails
	 *
	 * @since 3.5
	 */
	public IValue popData() throws DebugException {
		IValue[] dataStack = getDataStack();
		if (dataStack.length > 0) {
			sendCommand(new PDAPopDataCommand(fThreadId));
			return dataStack[0];
		}
		requestFailed("Empty stack", null); //$NON-NLS-1$
		return null;
	}

	/**
	 * Returns whether pushing a value is currently supported.
	 *
	 * @return whether pushing a value is currently supported
	 *
	 * @since 3.5
	 */
	public boolean canPushData() {
		return !isTerminated() && isSuspended();
	}

	/**
	 * Pushes a value onto the stack.
	 *
	 * @param value value to push
	 * @throws DebugException on failure
	 *
	 * @since 3.5
	 */
	public void pushData(String value) throws DebugException {
		sendCommand(new PDAPushDataCommand(fThreadId, value));
	}

	/**
	 * Returns this thread's unique identifier
	 *
	 * @return this thread's unique identifier
	 *
	 * @since 3.5
	 */
	public int getIdentifier() {
		return fThreadId;
	}
}
