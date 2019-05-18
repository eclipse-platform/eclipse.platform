/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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


import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;

/**
 * Handles a "Removed" response from the CVS server.
 * <p>
 * Suppose as a result of performing a command the CVS server responds
 * as follows:<br>
 * <pre>
 *   [...]
 *   Removed ??? \n
 *   [...]
 * </pre>
 * Then 
 * </p>
 */

/**
 * It removes the file from both the entries of the parent-folder
 * and from the local filesystem.
 */
class RemovedHandler extends ResponseHandler {
	public String getResponseID() {
		return "Removed"; //$NON-NLS-1$
	}

	public void handle(Session session, String localDir, IProgressMonitor monitor) throws CVSException {
		
		// read additional data for the response
		String repositoryFile = session.readLine();

		// Get the local file		
		String fileName = repositoryFile.substring(repositoryFile.lastIndexOf("/") + 1); //$NON-NLS-1$
		ICVSFolder mParent = session.getLocalRoot().getFolder(localDir);
		ICVSFile mFile = mParent.getFile(fileName);
		
		if ( ! mFile.isManaged()) {
			IStatus status = new CVSStatus(IStatus.ERROR,CVSStatus.ERROR,NLS.bind(CVSMessages.RemovedHandler_invalid, new String[] { new Path(null, localDir).append(fileName).toString() }),session.getLocalRoot());
			throw new CVSException(status); 
		}
		
		// delete then unmanage the file
		try {
			if (mFile.isReadOnly()) mFile.setReadOnly(false);
			mFile.delete();
			mFile.unmanage(null);
		} catch (CVSException e) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.RESPONSE_HANDLING_FAILURE, NLS.bind(CVSMessages.RemovedHandler_0, new String[] { getPath(mFile) }), e, session.getLocalRoot());
			session.handleResponseError(status); 
		}
	}

	private String getPath(ICVSFile file) {
		IResource resource = file.getIResource();
		if (resource != null) {
			return resource.getFullPath().toString();
		}
		try {
			return file.getRepositoryRelativePath();
		} catch (CVSException e1) {
			return file.getName();
		}
	}
}

