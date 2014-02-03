/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.launching.debug.model;

import com.ibm.icu.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.ant.internal.launching.debug.IAntDebugConstants;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.LineBreakpoint;

/**
 * Ant line breakpoint
 */
public class AntLineBreakpoint extends LineBreakpoint {
	
	/**
	 * Default constructor is required for the breakpoint manager
	 * to re-create persisted breakpoints. After instantiating a breakpoint,
	 * the <code>setMarker(...)</code> method is called to restore
	 * this breakpoint's attributes.
	 */
	public AntLineBreakpoint() {
	}
	
	/**
	 * Constructs a line breakpoint on the given resource at the given
	 * line number. The line number is 1-based (i.e. the first line of a
	 * file is line number 1).
	 * 
	 * @param resource file on which to set the breakpoint
	 * @param lineNumber 1-based line number of the breakpoint
	 * @throws CoreException if unable to create the breakpoint
	 */
	public AntLineBreakpoint(IResource resource, int lineNumber) throws CoreException {
	    this(resource, lineNumber, new HashMap(), true);
	}
	
	/**
	 * Constructs a line breakpoint on the given resource at the given
	 * line number. The line number is 1-based (i.e. the first line of a
	 * file is line number 1).
	 * 
	 * @param resource file on which to set the breakpoint
	 * @param lineNumber 1-based line number of the breakpoint
	 * @param attributes the marker attributes to set
	 * @param register whether to add this breakpoint to the breakpoint manager
	 * @throws CoreException if unable to create the breakpoint
	 */
	public AntLineBreakpoint(final IResource resource, final int lineNumber, final Map attributes, final boolean register) throws CoreException {
	    IWorkspaceRunnable wr= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
			    IMarker marker = resource.createMarker(IAntDebugConstants.ID_ANT_LINE_BREAKPOINT_MARKER);
			    setMarker(marker);
			    attributes.put(IBreakpoint.ENABLED, Boolean.TRUE);
			    attributes.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
			    attributes.put(IBreakpoint.ID, IAntDebugConstants.ID_ANT_DEBUG_MODEL);
                attributes.put(IMarker.MESSAGE, MessageFormat.format(DebugModelMessages.AntLineBreakpoint_0, new String[] {Integer.toString(lineNumber)}));
			    ensureMarker().setAttributes(attributes);
                
                register(register);
			}
	    };
	    run(getMarkerRule(resource), wr);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IBreakpoint#getModelIdentifier()
	 */
	public String getModelIdentifier() {
		return IAntDebugConstants.ID_ANT_DEBUG_MODEL;
	}

    /**
     * @return whether this breakpoint is a run to line breakpoint
     */
    public boolean isRunToLine() {
        try {
            return ensureMarker().getAttribute(IAntDebugConstants.ANT_RUN_TO_LINE, false);
        } catch (DebugException e) {
           return false;
        }
    }
    
    /**
     * Add this breakpoint to the breakpoint manager,
     * or sets it as unregistered.
     */
    private void register(boolean register) throws CoreException {
        if (register) {
            DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(this);
        } else {
            setRegistered(false);
        }
    }
}
