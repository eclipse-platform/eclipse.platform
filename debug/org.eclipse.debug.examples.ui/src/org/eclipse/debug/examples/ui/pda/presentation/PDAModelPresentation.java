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
package org.eclipse.debug.examples.ui.pda.presentation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.examples.core.pda.DebugCorePlugin;
import org.eclipse.debug.examples.core.pda.breakpoints.PDALineBreakpoint;
import org.eclipse.debug.examples.core.pda.breakpoints.PDAWatchpoint;
import org.eclipse.debug.examples.core.pda.model.PDADebugTarget;
import org.eclipse.debug.examples.core.pda.model.PDAStackFrame;
import org.eclipse.debug.examples.core.pda.model.PDAThread;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;


/**
 * Renders PDA debug elements
 */
public class PDAModelPresentation extends LabelProvider implements IDebugModelPresentation {
	@Override
	public void setAttribute(String attribute, Object value) {
	}

	@Override
	public String getText(Object element) {
		if (element instanceof PDADebugTarget) {
			return getTargetText((PDADebugTarget)element);
		} else if (element instanceof PDAThread) {
			return getThreadText((PDAThread)element);
		} else if (element instanceof PDAStackFrame) {
			return getStackFrameText((PDAStackFrame)element);
		} else if (element instanceof PDAWatchpoint) {
			return getWatchpointText((PDAWatchpoint)element);
		}
		return null;
	}

	/**
	 * Returns a label for the given watchpoint.
	 *
	 * @param watchpoint
	 * @return a label for the given watchpoint
	 */
	private String getWatchpointText(PDAWatchpoint watchpoint) {
		try {
			String label = watchpoint.getVariableName() + " (" + watchpoint.getFunctionName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			if (watchpoint.isAccess()) {
				label += " [read]"; //$NON-NLS-1$
			}
			if (watchpoint.isModification()) {
				label += " [write]"; //$NON-NLS-1$
			}
			return label;
		} catch (CoreException e) {
			return null;
		}
	}
	/**
	 * Returns a label for the given debug target
	 *
	 * @param target debug target
	 * @return a label for the given debug target
	 */
	private String getTargetText(PDADebugTarget target) {
		try {
			String pgmPath = target.getLaunch().getLaunchConfiguration().getAttribute(DebugCorePlugin.ATTR_PDA_PROGRAM, (String)null);
			if (pgmPath != null) {
				IPath path = IPath.fromOSString(pgmPath);
				String label = ""; //$NON-NLS-1$
				if (target.isTerminated()) {
					label = "<terminated>"; //$NON-NLS-1$
				}
				return label + "PDA [" + path.lastSegment() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (CoreException e) {
		}
		return "PDA"; //$NON-NLS-1$

	}

	/**
	 * Returns a label for the given stack frame
	 *
	 * @param frame a stack frame
	 * @return a label for the given stack frame
	 */
	private String getStackFrameText(PDAStackFrame frame) {
		try {
			return frame.getName() + " (line: " + frame.getLineNumber() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		} catch (DebugException e) {
		}
		return null;

	}

	/**
	 * Returns a label for the given thread
	 *
	 * @param thread a thread
	 * @return a label for the given thread
	 */
	private String getThreadText(PDAThread thread) {
		String label = thread.getName();
		if (thread.isStepping()) {
			label += " (stepping)"; //$NON-NLS-1$
		} else if (thread.isSuspended()) {
			IBreakpoint[] breakpoints = thread.getBreakpoints();
			if (breakpoints.length == 0) {
				if (thread.getError() == null) {
					label += " (suspended)"; //$NON-NLS-1$
				} else {
					label += " (" + thread.getError() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			} else {
				IBreakpoint breakpoint = breakpoints[0]; // there can only be one in PDA
				if (breakpoint instanceof PDALineBreakpoint) {
					PDALineBreakpoint pdaBreakpoint = (PDALineBreakpoint) breakpoint;
					if (pdaBreakpoint instanceof PDAWatchpoint) {
						try {
							PDAWatchpoint watchpoint = (PDAWatchpoint)pdaBreakpoint;
							label += " (watchpoint: " + watchpoint.getSuspendType() + " " + watchpoint.getVariableName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						} catch (CoreException e) {
						}
					} else if (pdaBreakpoint.isRunToLineBreakpoint()) {
						label += " (run to line)"; //$NON-NLS-1$
					} else {
						label += " (suspended at line breakpoint)"; //$NON-NLS-1$
					}
				}
			}
		} else if (thread.isTerminated()) {
			label = "<terminated> " + label; //$NON-NLS-1$
		}
		return label;
	}

	@Override
	public void computeDetail(IValue value, IValueDetailListener listener) {
		String detail = ""; //$NON-NLS-1$
		try {
			detail = value.getValueString();
		} catch (DebugException e) {
		}
		listener.detailComputed(value, detail);
	}

	@Override
	public IEditorInput getEditorInput(Object element) {
		if (element instanceof IFile) {
			return new FileEditorInput((IFile)element);
		}
		if (element instanceof ILineBreakpoint) {
			return new FileEditorInput((IFile)((ILineBreakpoint)element).getMarker().getResource());
		}
		return null;
	}

	@Override
	public String getEditorId(IEditorInput input, Object element) {
		if (element instanceof IFile || element instanceof ILineBreakpoint) {
			return "pda.editor"; //$NON-NLS-1$
		}
		return null;
	}
}
