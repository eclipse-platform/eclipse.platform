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

import java.io.*;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.util.SyncFileWriter;

/**
 * @author Administrator
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window&gt;Preferences&gt;Java&gt;Templates.
 * To enable and disable the creation of type comments go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation.
 */
public class TemplateHandler extends ResponseHandler {

	@Override
	public String getResponseID() {
		return "Template"; //$NON-NLS-1$
	}

	@Override
	public void handle(Session session, String localDir, IProgressMonitor monitor) throws CVSException {
		session.readLine(); /* read the remote dir which is not needed */
		// Only read the template file if the container exists.
		// This is OK as we only use the template from the project folder which must exist
		ICVSFolder localFolder = session.getLocalRoot().getFolder(localDir);
		IContainer container = (IContainer)localFolder.getIResource();
		ICVSStorage templateFile = null;
		if (container != null && container.exists()) {
			try {
				templateFile = CVSWorkspaceRoot.getCVSFileFor(SyncFileWriter.getTemplateFile(container));
			} catch (CVSException e) {
				// Log the inability to create the template file
				CVSProviderPlugin.log(new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, "Could not write template file in " + container.getFullPath() + ": " + e.getMessage(), e, session.getLocalRoot())); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		if (container == null || templateFile == null) {
			// Create a dummy storage handle to recieve the contents from the server
			templateFile = new ICVSStorage() {
				public String getName() {
					return "Template"; //$NON-NLS-1$
				}
				public void setContents(
					InputStream stream,
					int responseType,
					boolean keepLocalHistory,
					IProgressMonitor monitor)
					throws CVSException {

					try ( // Transfer the contents
							OutputStream out = new ByteArrayOutputStream()) {
						byte[] buffer = new byte[1024];
						int read;
						while ((read = stream.read(buffer)) >= 0) {
							Policy.checkCanceled(monitor);
							out.write(buffer, 0, read);
						}
					} catch (IOException e) {
						throw CVSException.wrapException(e); 
					} finally {
						try {
							stream.close();
						} catch (IOException e1) {
							// Ignore close errors
						}
					}
				}
				public long getSize() {
					return 0;
				}
				public InputStream getContents() throws CVSException {
					return new ByteArrayInputStream(new byte[0]);
				}
			};
		}
		try {
			session.receiveFile(templateFile, false, UpdatedHandler.HANDLE_UPDATED, monitor);
		} catch (CVSException e) {
			if (!(templateFile instanceof ICVSFile && handleInvalidResourceName(session, (ICVSFile)templateFile, e))) {
				throw e;
			}
		}
	}

}
