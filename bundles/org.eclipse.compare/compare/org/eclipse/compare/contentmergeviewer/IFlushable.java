/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.contentmergeviewer;

import org.eclipse.compare.IEditableContent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;

/**
 * Interface which provides the ability to flush the contents from the viewer
 * model (for example, an {@link IDocument} for text based content) into the
 * underlying compare model ( most likely an instance of {@link IEditableContent}).
 * <p>
 * This interface may be implemented by clients.
 * </p>
 * 
 * @since 3.3
 */
public interface IFlushable {
	
	/**
	 * Request that the view contents be flushed to the underlying compare input.
	 * Depending on the type of input, this may result in the contents being written
	 * into the underlying model (e.g. file) as well. 
	 * @param monitor a progress monitor or <code>null</code> if progress reporting is not desired
	 */
	void flush(IProgressMonitor monitor);
}
