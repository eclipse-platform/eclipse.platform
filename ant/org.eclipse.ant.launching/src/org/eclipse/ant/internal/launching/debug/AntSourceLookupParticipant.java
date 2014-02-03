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
package org.eclipse.ant.internal.launching.debug;

import org.eclipse.ant.internal.launching.debug.model.AntStackFrame;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;

/**
 * The Ant source lookup participant knows how to translate a 
 * Ant stack frame into a source file name 
 */
public class AntSourceLookupParticipant extends AbstractSourceLookupParticipant {
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant#getSourceName(java.lang.Object)
	 */
	public String getSourceName(Object object) throws CoreException {
		if (object instanceof AntStackFrame) {
			return ((AntStackFrame)object).getFilePath();
		}
        if (object instanceof String) {
            // assume it's a file name
            return (String)object;
        }
		return null;
	}
}
