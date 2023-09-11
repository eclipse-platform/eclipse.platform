/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.internal.launching.debug.model.AntLineBreakpoint;
import org.eclipse.ant.internal.launching.debug.model.DebugModelMessages;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointsListener;
import org.eclipse.debug.core.model.IBreakpoint;

public class AntModelCore implements IBreakpointsListener {

	private static AntModelCore inst;

	public static AntModelCore getDefault() {
		if (inst == null) {
			inst = new AntModelCore();
		}

		return inst;
	}

	private final List<IAntModelListener> fModelChangeListeners = new ArrayList<>();

	private AntModelCore() {
	}

	public void addAntModelListener(IAntModelListener listener) {
		synchronized (fModelChangeListeners) {
			fModelChangeListeners.add(listener);
		}
	}

	public void removeAntModelListener(IAntModelListener listener) {
		synchronized (fModelChangeListeners) {
			fModelChangeListeners.remove(listener);
		}
	}

	public void notifyAntModelListeners(AntModelChangeEvent event) {
		Iterator<IAntModelListener> i;
		synchronized (fModelChangeListeners) {
			i = new ArrayList<>(fModelChangeListeners).iterator();
		}
		while (i.hasNext()) {
			i.next().antModelChanged(event);
		}
	}

	/**
	 * Updates message attributes on any Ant line breakpoints if there are active Ant editors with Ant models
	 * 
	 * @see org.eclipse.debug.core.IBreakpointsListener#breakpointsAdded(org.eclipse.debug.core.model.IBreakpoint[])
	 */
	private void updateBreakpointMessages(final IBreakpoint[] breakpoints) {
		IWorkspaceRunnable runnable = monitor -> {
			for (IBreakpoint breakpoint : breakpoints) {
				if (breakpoint instanceof AntLineBreakpoint) {
					IMarker marker = breakpoint.getMarker();
					if (marker.exists()) {
						int lineNumber = marker.getAttribute(IMarker.LINE_NUMBER, 0);
						marker.setAttribute(IMarker.MESSAGE, MessageFormat.format(DebugModelMessages.AntLineBreakpoint_0, new Object[] {
								Integer.toString(lineNumber) }));
					}
				}
			}
		};
		try {
			ResourcesPlugin.getWorkspace().run(runnable, null, 0, null);
		}
		catch (CoreException e) {
			AntUIPlugin.log(e);
		}
	}

	@Override
	public void breakpointsAdded(IBreakpoint[] breakpoints) {
		// do nothing
	}

	@Override
	public void breakpointsRemoved(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
		// do nothing
	}

	@Override
	public void breakpointsChanged(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
		updateBreakpointMessages(breakpoints);
	}

	public void stopBreakpointListening() {
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
	}

	public void startBreakpointListening() {
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
	}
}
