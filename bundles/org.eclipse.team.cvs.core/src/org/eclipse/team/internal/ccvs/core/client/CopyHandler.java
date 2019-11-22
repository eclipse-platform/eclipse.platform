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


import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.*;

/**
 * Handles a "Copy-file" response from the CVS server.
 * <p>
 * Suppose as a result of performing a command the CVS server responds
 * as follows:
 * </p>
 * <pre>
 *   [...]
 *   Copy-file myproject/ \n
 *   /u/cvsroot/myproject/oldfile.txt \n
 *   newfile.txt
 *   [...]
 * </pre>
 * Then we copy (or optionally rename) the local file "oldfile.txt" in
 * folder "myproject" to "newfile.txt".  This response is used to create
 * a backup copy of an existing file before merging in new changes.
 */
class CopyHandler extends ResponseHandler {
	public String getResponseID() {
		return "Copy-file"; //$NON-NLS-1$
	}

	public void handle(Session session, String localDir,
		IProgressMonitor monitor) throws CVSException {
		// read additional data for the response
		String repositoryFile = session.readLine();
		String newFile = session.readLine();
		if (session.isNoLocalChanges() || ! session.isCreateBackups()) return;

		// Get the local file		
		String fileName = repositoryFile.substring(repositoryFile.lastIndexOf("/") + 1); //$NON-NLS-1$
		ICVSFolder mParent = session.getLocalRoot().getFolder(localDir);
		ICVSFile mFile = mParent.getFile(fileName);

		Assert.isTrue(mParent.exists());
		Assert.isTrue(mFile.exists() && mFile.isManaged());
		
		// rename the file
		mFile.copyTo(newFile);
	}
}

