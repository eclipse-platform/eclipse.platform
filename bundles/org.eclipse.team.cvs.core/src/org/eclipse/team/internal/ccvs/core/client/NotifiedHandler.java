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
import org.eclipse.core.runtime.Path;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;

/**
 * @author Administrator
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window&gt;Preferences&gt;Java&gt;Templates.
 * To enable and disable the creation of type comments go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation.
 */
public class NotifiedHandler extends ResponseHandler {

	@Override
	public String getResponseID() {
		return "Notified"; //$NON-NLS-1$
	}

	@Override
	public void handle(
		Session session,
		String localDir,
		IProgressMonitor monitor)
		throws CVSException {
			
		// read additional data for the response 
		// (which is the full repository path of the file)
		String repositoryFilePath = session.readLine();

		// clear the notify info for the file
		ICVSFolder folder = session.getLocalRoot().getFolder(localDir);
		ICVSFile file = folder.getFile(new Path(null, repositoryFilePath).lastSegment());
		file.notificationCompleted();
	}

}
