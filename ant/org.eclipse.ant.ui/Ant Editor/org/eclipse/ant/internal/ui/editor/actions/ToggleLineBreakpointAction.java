/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.editor.actions;

import org.eclipse.ant.internal.launching.debug.IAntDebugConstants;
import org.eclipse.ant.internal.launching.debug.model.AntLineBreakpoint;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;

public class ToggleLineBreakpointAction implements IToggleBreakpointsTarget {

	@Override
	public void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
		IEditorPart editorPart = (IEditorPart) part;
		IEditorInput editorInput = editorPart.getEditorInput();
		IResource resource = null;
		if (editorInput instanceof IFileEditorInput) {
			resource = ((IFileEditorInput) editorInput).getFile();
		}
		if (resource == null) {
			return;
		}

		ITextSelection textSelection = (ITextSelection) selection;
		int lineNumber = textSelection.getStartLine();
		for (IBreakpoint breakpoint : DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(IAntDebugConstants.ID_ANT_DEBUG_MODEL)) {
			if (resource.equals(breakpoint.getMarker().getResource())) {
				if (((ILineBreakpoint) breakpoint).getLineNumber() == (lineNumber + 1)) {
					DebugUITools.deleteBreakpoints(new IBreakpoint[] { breakpoint }, part.getSite().getShell(), null);
					return;
				}
			}
		}
		// create line breakpoint (doc line numbers start at 0)
		new AntLineBreakpoint(resource, lineNumber + 1);
	}

	@Override
	public boolean canToggleLineBreakpoints(IWorkbenchPart part, ISelection selection) {
		return selection instanceof ITextSelection;
	}

	@Override
	public void toggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
		// do nothing
	}

	@Override
	public boolean canToggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) {
		return false;
	}

	@Override
	public void toggleWatchpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
		// do nothing
	}

	@Override
	public boolean canToggleWatchpoints(IWorkbenchPart part, ISelection selection) {
		return false;
	}
}
