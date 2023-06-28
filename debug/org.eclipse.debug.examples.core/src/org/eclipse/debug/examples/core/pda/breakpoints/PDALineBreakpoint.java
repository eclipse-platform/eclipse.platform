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
package org.eclipse.debug.examples.core.pda.breakpoints;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.LineBreakpoint;
import org.eclipse.debug.examples.core.pda.DebugCorePlugin;
import org.eclipse.debug.examples.core.pda.model.IPDAEventListener;
import org.eclipse.debug.examples.core.pda.model.PDADebugTarget;
import org.eclipse.debug.examples.core.pda.model.PDAThread;
import org.eclipse.debug.examples.core.pda.protocol.PDAClearBreakpointCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDAEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDARunControlEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDASetBreakpointCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDASuspendedEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDAVMSuspendedEvent;


/**
 * PDA line breakpoint
 */
public class PDALineBreakpoint extends LineBreakpoint implements IPDAEventListener {

	// target currently installed in
	private PDADebugTarget fTarget;

	/**
	 * Default constructor is required for the breakpoint manager
	 * to re-create persisted breakpoints. After instantiating a breakpoint,
	 * the <code>setMarker(...)</code> method is called to restore
	 * this breakpoint's attributes.
	 */
	public PDALineBreakpoint() {
	}

	/**
	 * Constructs a line breakpoint on the given resource at the given
	 * line number. The line number is 1-based (i.e. the first line of a
	 * file is line number 1). The PDA VM uses 0-based line numbers,
	 * so this line number translation is done at breakpoint install time.
	 *
	 * @param resource file on which to set the breakpoint
	 * @param lineNumber 1-based line number of the breakpoint
	 * @throws CoreException if unable to create the breakpoint
	 */
	public PDALineBreakpoint(final IResource resource, final int lineNumber) throws CoreException {
		IWorkspaceRunnable runnable = monitor -> {
			IMarker marker = resource.createMarker("org.eclipse.debug.examples.core.pda.markerType.lineBreakpoint"); //$NON-NLS-1$
			setMarker(marker);
			marker.setAttribute(IBreakpoint.ENABLED, Boolean.TRUE);
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
			marker.setAttribute(IBreakpoint.ID, getModelIdentifier());
			marker.setAttribute(IMarker.MESSAGE, "Line Breakpoint: " + resource.getName() + " [line: " + lineNumber + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		};
		run(getMarkerRule(resource), runnable);
	}

	@Override
	public String getModelIdentifier() {
		return DebugCorePlugin.ID_PDA_DEBUG_MODEL;
	}

	/**
	 * Returns whether this breakpoint is a run-to-line breakpoint
	 *
	 * @return whether this breakpoint is a run-to-line breakpoint
	 */
	public boolean isRunToLineBreakpoint() {
		return false;
	}

	/**
	 * Installs this breakpoint in the given interprettor.
	 * Registeres this breakpoint as an event listener in the
	 * given target and creates the breakpoint specific request.
	 *
	 * @param target PDA interprettor
	 * @throws CoreException if installation fails
	 */
	public void install(PDADebugTarget target) throws CoreException {
		fTarget = target;
		target.addEventListener(this);
		createRequest(target);
	}

	/**
	 * Create the breakpoint specific request in the target. Subclasses
	 * should override.
	 *
	 * @param target PDA interprettor
	 * @throws CoreException if request creation fails
	 */
	protected void createRequest(PDADebugTarget target) throws CoreException {
		//#ifdef ex3
//#		// TODO: Exercise 3 - create breakpoint request in interpreter
		//#else
		target.sendCommand(new PDASetBreakpointCommand((getLineNumber() - 1), false));
		//#endif
	}

	/**
	 * Removes this breakpoint's event request from the target. Subclasses
	 * should override.
	 *
	 * @param target PDA interprettor
	 * @throws CoreException if clearing the request fails
	 */
	protected void clearRequest(PDADebugTarget target) throws CoreException {
		//#ifdef ex3
//#		// TODO: Exercise 3 - clear breakpoint request in interpreter
		//#else
		target.sendCommand(new PDAClearBreakpointCommand((getLineNumber() - 1)));
		//#endif
	}

	/**
	 * Removes this breakpoint from the given interprettor.
	 * Removes this breakpoint as an event listener and clears
	 * the request for the interprettor.
	 *
	 * @param target PDA interprettor
	 * @throws CoreException if removal fails
	 */
	public void remove(PDADebugTarget target) throws CoreException {
		target.removeEventListener(this);
		clearRequest(target);
		fTarget = null;

	}

	/**
	 * Returns the target this breakpoint is installed in or <code>null</code>.
	 *
	 * @return the target this breakpoint is installed in or <code>null</code>
	 */
	protected PDADebugTarget getDebugTarget() {
		return fTarget;
	}

	/**
	 * Notify's the PDA interprettor that this breakpoint has been hit.
	 */
	protected void notifyThread(int threadId) {
		if (fTarget != null) {
			PDAThread thread = fTarget.getThread(threadId);
			if (thread != null) {
				thread.suspendedBy(this);
			}
		}
	}

	/*
	 * Subclasses should override to handle their breakpoint specific event.
	 */
	@Override
	public void handleEvent(PDAEvent event) {
		if (event instanceof PDASuspendedEvent || event instanceof PDAVMSuspendedEvent) {
			PDARunControlEvent rcEvent = (PDARunControlEvent)event;
			if (rcEvent.fReason.equals("breakpoint")) { //$NON-NLS-1$
				handleHit(rcEvent);
			}
		}
	}

	/**
	 * Determines if this breakpoint was hit and notifies the thread.
	 *
	 * @param event breakpoint event
	 */
	private void handleHit(PDARunControlEvent event) {
		int lastSpace = event.fMessage.lastIndexOf(' ');
		if (lastSpace > 0) {
			String line = event.fMessage.substring(lastSpace + 1);
			int lineNumber = Integer.parseInt(line);
			// breakpoints event line numbers are 0 based, model objects are 1 based
			lineNumber++;
			try {
				if (getLineNumber() == lineNumber) {
					notifyThread(event.fThreadId);
				}
			} catch (CoreException e) {
			}
		}
	}
}
