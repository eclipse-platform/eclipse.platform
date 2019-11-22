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
package org.eclipse.team.internal.ccvs.core.client;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * Handles a "Checked-in" response from the CVS server.
 * <p>
 * Suppose as a result of performing a command the CVS server responds
 * as follows:
 * </p>
 * <pre>
 *   [...]
 *   Checked-in ??? \n
 *   [...]
 * </pre>
 * Then
 */
class CheckedInHandler extends ResponseHandler {
	public String getResponseID() {
		return "Checked-in"; //$NON-NLS-1$
	}

	public void handle(Session session, String localDir, IProgressMonitor monitor) throws CVSException {
		// read additional data for the response
		String repositoryFile = session.readLine();
		String entryLine = session.readLine();
		
		// clear file update modifiers
		session.setModTime(null);
		
		// Get the local file		
		String fileName = repositoryFile.substring(repositoryFile.lastIndexOf("/") + 1); //$NON-NLS-1$
		ICVSFolder mParent = session.getLocalRoot().getFolder(localDir);
		ICVSFile mFile = mParent.getFile(fileName);
		
		// Marked the local file as checked-in
		monitor.subTask(NLS.bind(CVSMessages.CheckInHandler_checkedIn, new String[] { Util.toTruncatedPath((ICVSResource)mFile, session.getLocalRoot(), 3) })); 
		mFile.checkedIn(entryLine, session.getCurrentCommand() instanceof Commit);
	}
}

