/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
