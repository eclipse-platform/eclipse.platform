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
package org.eclipse.team.internal.ccvs.core.client.listeners;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.CommandOutputListener;
import org.eclipse.team.internal.ccvs.core.util.Util;

public class StatusListener extends CommandOutputListener {
	private static boolean isFolder = false;
	private IStatusListener statusListener;

	public StatusListener(IStatusListener statusListener) {
		this.statusListener = statusListener;
	}

	public IStatus messageLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot,
		IProgressMonitor monitor) {
		
		// We're only concerned about file revisions.
		if (line.startsWith("   Repository revision:")) { //$NON-NLS-1$
			if (!line.startsWith("   Repository revision:	No revision control file")) { //$NON-NLS-1$
				int separatingTabIndex = line.indexOf('\t', 24);
				String remoteRevision = line.substring(24, separatingTabIndex);

				// This is the full location on the server (e.g. /home/cvs/repo/project/file.txt)
				String fileLocation = line.substring(separatingTabIndex + 1, line.length() - 2);

				// Inform the listener about the file revision
				statusListener.fileStatus(commandRoot, removeAtticSegment(fileLocation), remoteRevision);
			}
		}
		return OK;
	}

	public IStatus errorLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot, IProgressMonitor monitor) {
		String serverMessage = getServerMessage(line, location);
		if (serverMessage != null) {
			if (serverMessage.startsWith("conflict:")) {//$NON-NLS-1$
				// We get this because we made up an entry line to send to the server
				// Therefore, we make this a warning!!!
				return new CVSStatus(IStatus.WARNING, CVSStatus.CONFLICT, line, commandRoot);
			}
			if (serverMessage.startsWith("Examining")) {//$NON-NLS-1$
				isFolder = true;
				return OK;
			}
		}
		if (isFolder) {
			// This used to do something but it was obviously wrong and there was no indication
			// why it was needed. Therefore, I have removed the code to see if anything is affected
			isFolder = false;
		}
		return super.errorLine(line, location, commandRoot, monitor);
	}
	
	/**
	 * If the status returns that the file is in the Attic, then remove the
	 * Attic segment. This is because files added to a branch that are not in
	 * the main trunk (HEAD) are added to the Attic but cvs does magic on
	 * update to put them in the correct location.
	 * (e.g. /project/Attic/file.txt -&gt; /project/file.txt)
	 */ 
	private String removeAtticSegment(String path) {
		return Util.removeAtticSegment(path);
	}
}
