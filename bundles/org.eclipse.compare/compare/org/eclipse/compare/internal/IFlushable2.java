/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.compare.contentmergeviewer.IFlushable;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface which provides the ability to flush the contents from the specified
 * side of the viewer.
 * 
 * @see IFlushable
 * 
 * @since 3.7
 */
public interface IFlushable2 {
	void flushLeft(IProgressMonitor monitor);

	void flushRight(IProgressMonitor monitor);
}
