/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.client;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;

public class CommandOutputListener implements ICommandOutputListener {
	
	/*
	 * Failure string that is returned from the server when pserver is used and the root directory
	 * is not readable. The problem can be fixed by making the directory readable or by using -f in 
	 * the pserver configuration file. We will ignore the error since it does not affect the command.
	 */
	public static final String ROOT_CVSIGNORE_READ_FAILURE = "cvs server: cannot open /root/.cvsignore: Permission denied"; //$NON-NLS-1$
	
	public IStatus messageLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot, IProgressMonitor monitor) {
		return OK;
	}
	public IStatus errorLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot, IProgressMonitor monitor) {
		String protocolError = getProtocolError(line, location);
		if (protocolError != null) {
			return new CVSStatus(IStatus.ERROR, CVSStatus.PROTOCOL_ERROR, protocolError, commandRoot);
		}
		if (line.equals(ROOT_CVSIGNORE_READ_FAILURE) || getServerMessage(ROOT_CVSIGNORE_READ_FAILURE, location).equals(getServerMessage(line, location))) {
			// Don't report this as an error since it does not affect the command
			return new CVSStatus(IStatus.WARNING, CVSStatus.ERROR_LINE, line, commandRoot);
		}
		return new CVSStatus(IStatus.ERROR, CVSStatus.ERROR_LINE, line, commandRoot);
	}
	
	/**
	 * Return the portion of the line that describes the error if the error line
	 * is a protocol error or null if the line is not a protocol error.
	 * 
	 * @param line the error line received from the server
	 * @param location the repository location
	 * @return String the protocol error or null
	 */
	protected String getProtocolError(String line, ICVSRepositoryLocation location) {
		if (line.startsWith("Protocol error:")) { //$NON-NLS-1$
			return line;
		}
		return null;
	}

	public String getServerMessage(String line, ICVSRepositoryLocation location) {
		return ((CVSRepositoryLocation)location).getServerMessageWithoutPrefix(line, SERVER_PREFIX);
	}

	public String getServerAbortedMessage(String line, ICVSRepositoryLocation location) {
		return ((CVSRepositoryLocation)location).getServerMessageWithoutPrefix(line, SERVER_ABORTED_PREFIX);
	}

	public String getServerRTagMessage(String line, ICVSRepositoryLocation location) {
		return ((CVSRepositoryLocation)location).getServerMessageWithoutPrefix(line, RTAG_PREFIX);
	}
}
