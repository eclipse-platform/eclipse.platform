/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core;


import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A runnable which executes as a batch operation within a specific cvs local
 * workspace.
 * The <code>ICVSRunnable</code> interface should be implemented by any class whose 
 * instances are intended to be run by <code>IWorkspace.run</code>.
 * <p>
 * Clients may implement this interface.
 * </p>
 */
public interface ICVSRunnable {
	/**
	 * Runs the operation reporting progress to and accepting
	 * cancellation requests from the given progress monitor.
	 * <p>
	 * Implementors of this method should check the progress monitor
	 * for cancellation when it is safe and appropriate to do so.  The cancellation
	 * request should be propagated to the caller by throwing 
	 * <code>OperationCanceledException</code>.
	 * </p>
	 * 
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @exception CoreException if this operation fails.
	 */
	public void run(IProgressMonitor monitor) throws CVSException;
}
