/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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


import java.io.PrintStream;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.CommandOutputListener;
import org.eclipse.team.internal.ccvs.core.client.Session;

public class DiffListener extends CommandOutputListener {

	//Error Messages returned by CVS
	static final String ERR_NOSUCHDIRECTORY = "cvs [diff aborted]: no such directory"; //$NON-NLS-1$

	// Expressions for examining response lines 
	static final String INDEX = "Index: "; //$NON-NLS-1$
	static final String BINARYFILESDIFFER = "Binary files .* and .* differ"; //$NON-NLS-1$
	
	PrintStream patchStream;
	boolean wroteToStream;
	
	private String index = ""; //$NON-NLS-1$

	public DiffListener(PrintStream patchStream) {
		this.patchStream = patchStream;
		wroteToStream=false;
	}
	
	public IStatus messageLine(
			String line, 
			ICVSRepositoryLocation location, 
			ICVSFolder commandRoot,
			IProgressMonitor monitor) {
		
		// Special handling to avoid getting duplicate CRs when generating a patch on windows.  
		// If the remote file has CR/LF in it, then the line will have a CR at the end.
		// We need to remove it so we don't end up with two CRs (since the printStream will also add one).
		// On *nix, we want to include the CR since it will not be added by the printStream (see bug 92162).
		if (Session.IS_CRLF_PLATFORM && line.length() > 0 && line.charAt(line.length() - 1) == '\r') {
			line = line.substring(0, line.length() - 1);
		}
		patchStream.println(line);
		wroteToStream = true;

		if (line.startsWith(INDEX)) {
			index = line.substring(INDEX.length());
		} else if (Pattern.matches(BINARYFILESDIFFER, line)) {
			String message = NLS.bind(
					CVSMessages.ThePatchDoesNotContainChangesFor_0,
					new String[] { index });
			return new CVSStatus(IStatus.WARNING,
					CVSStatus.BINARY_FILES_DIFFER, message, (Throwable) null);
		}

		return OK;
	}

	public IStatus errorLine(
			String line, 
			ICVSRepositoryLocation location, 
			ICVSFolder commandRoot,
			IProgressMonitor monitor) {
		// return all the server errors as CVSStatus.ERROR_LINE or
		// CVSStatus.PROTOCOL_ERROR
		if (getServerMessage(line, location) != null) {
			return super.errorLine(line, location, commandRoot, monitor);
		}
		
		//Check to see if this is a no such directory message
		if (line.contains(ERR_NOSUCHDIRECTORY)){
			return OK;
		}
		return super.errorLine(line, location, commandRoot, monitor);
	}

	public boolean wroteToStream() {
		return wroteToStream;
	}
}
