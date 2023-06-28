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
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.breakpoints;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;

/**
 * A run to line breakpoint.
 */
public class PDARunToLineBreakpoint extends PDALineBreakpoint {

	private IFile fSourceFile;

	/**
	 * Constructs a run-to-line breakpoint in the given PDA program.
	 *
	 * @param resource PDA source file
	 * @param lineNumber line to run to
	 * @exception DebugException if unable to create the breakpoint
	 */
	public PDARunToLineBreakpoint(final IFile resource, final int lineNumber) throws DebugException {
		IWorkspaceRunnable runnable = monitor -> {
			// associate with workspace root to avoid drawing in editor ruler
			IMarker marker = ResourcesPlugin.getWorkspace().getRoot().createMarker("org.eclipse.debug.examples.core.pda.markerType.lineBreakpoint"); //$NON-NLS-1$
			setMarker(marker);
			marker.setAttribute(IBreakpoint.ENABLED, Boolean.TRUE);
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
			marker.setAttribute(IBreakpoint.ID, getModelIdentifier());
			setRegistered(false);
			fSourceFile = resource;
		};
		run(getMarkerRule(resource), runnable);
	}

	/**
	 * Returns whether this breakpoint is a run-to-line breakpoint
	 *
	 * @return whether this breakpoint is a run-to-line breakpoint
	 */
	@Override
	public boolean isRunToLineBreakpoint() {
		return true;
	}

	/**
	 * Returns the source file this breakpoint is contained in.
	 *
	 * @return the source file this breakpoint is contained in
	 */
	public IFile getSourceFile() {
		return fSourceFile;
	}
}
