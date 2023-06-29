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
import org.eclipse.debug.core.model.IWatchpoint;
import org.eclipse.debug.examples.core.pda.model.PDADebugTarget;
import org.eclipse.debug.examples.core.pda.protocol.PDAEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDARunControlEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDASuspendedEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDAVMSuspendedEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDAWatchCommand;


/**
 * A watchpoint.
 */
public class PDAWatchpoint extends PDALineBreakpoint implements IWatchpoint {

	// 'read' or 'write' depending on what caused the last suspend for this watchpoint
	private String fLastSuspendType;

	// marker attributes
	public static final String ACCESS = "ACCESS"; //$NON-NLS-1$
	public static final String MODIFICATION = "MODIFICATION"; //$NON-NLS-1$
	public static final String FUNCTION_NAME = "FUNCTION_NAME"; //$NON-NLS-1$
	public static final String VAR_NAME = "VAR_NAME"; //$NON-NLS-1$

	/**
	 * Default constructor is required for the breakpoint manager
	 * to re-create persisted breakpoints. After instantiating a breakpoint,
	 * the <code>setMarker(...)</code> method is called to restore
	 * this breakpoint's attributes.
	 */
	public PDAWatchpoint() {
	}
	/**
	 * Constructs a line breakpoint on the given resource at the given
	 * line number. The line number is 1-based (i.e. the first line of a
	 * file is line number 1). The PDA VM uses 0-based line numbers,
	 * so this line number translation is done at breakpoint install time.
	 *
	 * @param resource file on which to set the breakpoint
	 * @param lineNumber 1-based line number of the breakpoint
	 * @param functionName function name the variable is defined in
	 * @param varName variable name that watchpoint is set on
	 * @param access whether this is an access watchpoint
	 * @param modification whether this in a modification watchpoint
	 * @throws CoreException if unable to create the watchpoint
	 */
	public PDAWatchpoint(final IResource resource, final int lineNumber, final String functionName, final String varName, final boolean access, final boolean modification) throws CoreException {
		IWorkspaceRunnable runnable = monitor -> {
			IMarker marker = resource.createMarker("org.eclipse.debug.examples.core.pda.markerType.watchpoint"); //$NON-NLS-1$
			setMarker(marker);
			setEnabled(true);
			ensureMarker().setAttribute(IMarker.LINE_NUMBER, lineNumber);
			ensureMarker().setAttribute(IBreakpoint.ID, getModelIdentifier());
			setAccess(access);
			setModification(modification);
			setVariable(functionName, varName);
			marker.setAttribute(IMarker.MESSAGE, "Watchpoint: " + resource.getName() + " [line: " + lineNumber + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		};
		run(getMarkerRule(resource), runnable);
	}

	@Override
	public boolean isAccess() throws CoreException {
		return getMarker().getAttribute(ACCESS, true);
	}

	@Override
	public void setAccess(boolean access) throws CoreException {
		setAttribute(ACCESS, access);
	}

	@Override
	public boolean isModification() throws CoreException {
		return getMarker().getAttribute(MODIFICATION, true);
	}

	@Override
	public void setModification(boolean modification) throws CoreException {
		setAttribute(MODIFICATION, modification);
	}

	@Override
	public boolean supportsAccess() {
		return true;
	}

	@Override
	public boolean supportsModification() {
		return true;
	}

	/**
	 * Sets the variable and function names the watchpoint is set on.
	 *
	 * @param functionName function name
	 * @param variableName variable name
	 * @throws CoreException if an exception occurrs setting marker attribtues
	 */
	protected void setVariable(String functionName, String variableName) throws CoreException {
		setAttribute(VAR_NAME, variableName);
		setAttribute(FUNCTION_NAME, functionName);
	}

	/**
	 * Returns the name of the variable this watchpoint is set on.
	 *
	 * @return the name of the variable this watchpoint is set on
	 * @throws CoreException if unable to access the attribute
	 */
	public String getVariableName() throws CoreException {
		return getMarker().getAttribute(VAR_NAME, (String)null);
	}

	/**
	 * Returns the name of the function the variable associted with this watchpoint is defined in.
	 *
	 * @return the name of the function the variable associted with this watchpoint is defined in
	 * @throws CoreException if unable to access the attribute
	 */
	public String getFunctionName() throws CoreException {
		return getMarker().getAttribute(FUNCTION_NAME, (String)null);
	}

	/**
	 * Sets the type of event that causes the last suspend event.
	 *
	 * @param description one of 'read' or 'write'
	 */
	public void setSuspendType(String description) {
		fLastSuspendType = description;
	}

	/**
	 * Returns the type of event that caused the last suspend.
	 *
	 * @return 'read', 'write', or <code>null</code> if undefined
	 */
	public String getSuspendType() {
		return fLastSuspendType;
	}

	@Override
	protected void createRequest(PDADebugTarget target) throws CoreException {
		int flag = 0;
		if (isAccess()) {
			flag = flag | 1;
		}
		if (isModification()) {
			flag = flag | 2;
		}
		target.sendCommand(new PDAWatchCommand(getFunctionName(), getVariableName(), flag));
	}

	@Override
	protected void clearRequest(PDADebugTarget target) throws CoreException {
		target.sendCommand(new PDAWatchCommand(getFunctionName(), getVariableName(), 0));
	}

	@Override
	public void handleEvent(PDAEvent event) {
		if (event instanceof PDASuspendedEvent || event instanceof PDAVMSuspendedEvent) {
			PDARunControlEvent rcEvent = (PDARunControlEvent)event;
			if (rcEvent.fReason.equals("watch")) { //$NON-NLS-1$
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
		String[] strings = event.fMessage.split(" "); //$NON-NLS-1$
		if (strings.length == 4) {
			String fv = strings[3];
			int j = fv.indexOf("::"); //$NON-NLS-1$
			if (j > 0) {
				String fcn = fv.substring(0, j);
				String var = fv.substring(j + 2);
				try {
					if (getVariableName().equals(var) && getFunctionName().equals(fcn)) {
						setSuspendType(strings[2]);
						notifyThread(event.fThreadId);
					}
				} catch (CoreException e) {
				}
			}
		}
	}
}
