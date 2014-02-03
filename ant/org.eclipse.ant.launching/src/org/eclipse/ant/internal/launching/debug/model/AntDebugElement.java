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
package org.eclipse.ant.internal.launching.debug.model;


import org.eclipse.ant.internal.launching.AntLaunching;
import org.eclipse.ant.internal.launching.debug.IAntDebugConstants;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.DebugElement;

/**
 * Common function of Ant debug model elements
 */
public abstract class AntDebugElement extends DebugElement {
	
	/**
	 * Constructs a new debug element contained in the given
	 * debug target.
	 * 
	 * @param target debug target
	 */
	public AntDebugElement(AntDebugTarget target) {
		super(target);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugElement#getModelIdentifier()
	 */
	public String getModelIdentifier() {
		return IAntDebugConstants.ID_ANT_DEBUG_MODEL;
	}
    
	/**
     * Throws a debug exception with the given message, error code, and underlying
     * exception.
     */
    protected void throwDebugException(String message) throws DebugException {
        throw new DebugException(new Status(IStatus.ERROR, AntLaunching.getUniqueIdentifier(),
            DebugException.TARGET_REQUEST_FAILED, message, null));
    }
    
    protected AntDebugTarget getAntDebugTarget() {
        return (AntDebugTarget)super.getDebugTarget();
    }
}
