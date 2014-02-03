/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.launching.debug;

public interface IAntDebugConstants {
	
	/**
	 * Unique identifier for the Ant debug model (value 
	 * <code>org.eclipse.ant.ui.debug</code>).
	 */
	public static final String ID_ANT_DEBUG_MODEL = "org.eclipse.ant.ui.debug"; //$NON-NLS-1$
	
	/**
	 * Unique identifier for the Ant line breakpoint markers 
	 * (value <code>org.eclipse.ant.ui.antLineBreakpointMarker</code>).
	 */
	public static final String ID_ANT_LINE_BREAKPOINT_MARKER= "org.eclipse.ant.ui.antLineBreakpointMarker"; //$NON-NLS-1$
    
    /**
     * Unique identifier for the Ant run to line breakpoints 
     * (value <code>org.eclipse.ant.ui.runToLineBreakpoint</code>).
     */
    public static final String ANT_RUN_TO_LINE= "org.eclipse.ant.ui.runToLineBreakpoint"; //$NON-NLS-1$
}
