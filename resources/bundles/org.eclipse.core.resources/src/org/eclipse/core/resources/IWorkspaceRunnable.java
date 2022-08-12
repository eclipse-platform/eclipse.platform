/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.resources;

import org.eclipse.core.runtime.*;

/**
 * This interface is structurally equivalent to {@link ICoreRunnable}. New code should use
 * {@link ICoreRunnable} instead of {@code IWorkspaceRunnable}.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IWorkspace#run(ICoreRunnable, IProgressMonitor)
 */
public interface IWorkspaceRunnable extends ICoreRunnable {
	/**
	 * @param monitor a progress monitor, or {@code null} if progress reporting and
	 *     cancellation are not desired.  The monitor is only valid for the duration
	 *     of the invocation of this method.  Callers may call {@link IProgressMonitor#done()}
	 *     after this method returns or throws an exception, but this is not strictly
	 *     required.
	 * @exception CoreException if this operation fails
	 * @exception OperationCanceledException if this operation is canceled
	 */
	@Override void run(IProgressMonitor monitor) throws CoreException;
}
