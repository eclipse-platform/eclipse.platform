/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.model;

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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointsListener;
import org.eclipse.debug.core.model.IBreakpoint;

import com.ibm.icu.text.MessageFormat;

public class AntModelCore implements IBreakpointsListener {
	
	private static AntModelCore inst;
	
	public static AntModelCore getDefault() {
		if (inst == null) {
			inst= new AntModelCore();
		}
			
		return inst;
	}
	
	private List fModelChangeListeners= new ArrayList();
	
	private AntModelCore() { }

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
		Iterator i;
		synchronized (fModelChangeListeners) {
			i= new ArrayList(fModelChangeListeners).iterator();
		}
		while (i.hasNext()) {
			((IAntModelListener)i.next()).antModelChanged(event);
		}
	}
    
    /**
     * Updates message attributes on any Ant line breakpoints if there are active Ant editors
     * with Ant models
     * 
     * @see org.eclipse.debug.core.IBreakpointsListener#breakpointsAdded(org.eclipse.debug.core.model.IBreakpoint[])
     */
    private void updateBreakpointMessages(final IBreakpoint[] breakpoints) {
        IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
                for (int i = 0; i < breakpoints.length; i++) {
                    IBreakpoint breakpoint= breakpoints[i];
                    if (breakpoint instanceof AntLineBreakpoint) {
                        IMarker marker = breakpoint.getMarker();
                        if (marker.exists()) {
                        	int lineNumber= marker.getAttribute(IMarker.LINE_NUMBER, 0);
                        	marker.setAttribute(IMarker.MESSAGE, MessageFormat.format(DebugModelMessages.AntLineBreakpoint_0, new String[] {Integer.toString(lineNumber)}));
                        }
                    }
                }
            }
        };
        try {
            ResourcesPlugin.getWorkspace().run(runnable, null, 0, null);
        } catch (CoreException e) {
            AntUIPlugin.log(e);
        }
    }

    public void breakpointsAdded(IBreakpoint[] breakpoints) {
    }

    public void breakpointsRemoved(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
    }

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
