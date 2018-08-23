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
package org.eclipse.team.internal.ccvs.core.client.listeners;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;

/**
 * Instances of this interface can be passed to the <code>Command#execute</code> methods
 * and will receive notification when M or E messages are received from the server.
 */
public interface ICommandOutputListener {
	
	/*** Status to be returned when no error or warning occured ***/
	public static final IStatus OK = new CVSStatus(IStatus.OK, CVSMessages.ok); 
	
	public static final String SERVER_PREFIX = "server: "; //$NON-NLS-1$
	public static final String SERVER_ABORTED_PREFIX = "[server aborted]: "; //$NON-NLS-1$
	public static final String RTAG_PREFIX = "rtag: "; //$NON-NLS-1$
	
	/**
	 * Invoked when a message line is received from the server.
	 * <p>
	 * Any status other than ICommandOutputListener.OK will be accumulated
	 * by the command and returned. The severity of the status matches those of
	 * IStatus and must indicate whether this is a warning, error, or informational 
	 * text.while the code should be one of the codes provided by CVSStatus.
	 * The status code must not be CVSStatus.SERVER_ERROR.
	 * </p>
	 * 
	 * @param line the line of message text sent by the server
	 * @param commandRoot the root directory of the command
	 * @param monitor the progress monitor
	 * @return a status indicating success or failure based on the text
	 */
	public IStatus messageLine(String line,
		ICVSRepositoryLocation location,
		ICVSFolder commandRoot,
		IProgressMonitor monitor);

	/**
	 * Invoked when an error line is received from the server.
	 * <p>
	 * Any status other than ICommandOutputListener.OK will be accumulated
	 * by the command and returned. The severity of the status matches those of
	 * IStatus and must indicate whether this is a warning, error, or informational 
	 * text.while the code should be one of the codes provided by CVSStatus.
	 * The status code must not be CVSStatus.SERVER_ERROR.
	 * </p>
	 * 
	 * @param line the line of error text sent by the server
	 * @param commandRoot the root directory of the command
	 * @param monitor the progress monitor
	 * @return a status indicating success or failure based on the text
	 */
	public IStatus errorLine(String line,
		ICVSRepositoryLocation location,
		ICVSFolder commandRoot,
		IProgressMonitor monitor);
}
