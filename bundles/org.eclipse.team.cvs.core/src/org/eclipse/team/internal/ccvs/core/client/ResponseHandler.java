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


import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * Handles server responses that arise as a result of issuing a request
 * (usually a command) to a CVS server.  The processing of each such
 * response is deferred to subclasses.
 */
public abstract class ResponseHandler {
	/**
	 * Returns the text string of the server response handled by this object.
	 * @return the id
	 */
	public abstract String getResponseID();

	/**
	 * Handles a server response.
	 * <p>
	 * Suppose as a result of performing a command the CVS server responds
	 * as follows:
	 * </p>
	 * <pre>
	 *   [...]
	 *   Clear-sticky myDirectory \n
	 *   /u/cvsroot/myDirectory \n
	 *   [...]
	 * </pre>
	 * Then the <code>handle</code> method of the <code>ResponseHandler</code>
	 * for <em>Clear-sticky</em> will be invoked with <code>argument</code>
	 * set to <em>"myDirectory"</em>.  It must then read the remaining
	 * response data from the connection (<em>"/u/cvsroot/myDirectory"</em>
	 * including the newline) and take any necessary action.<p>
	 * Note:  The type and quantity of additional data that must be read
	 * from the connection varies on a per-response basis.
	 * </p>
	 * @param session the Session used for CVS communication
	 * @param argument the argument supplied with the response
	 * @param monitor the progress monitor for the current CVS command
	 */
	public abstract void handle(Session session, String argument,
		IProgressMonitor monitor) throws CVSException;
	
	/**
	 * Creates a new CVS folder.
	 * @param localDir the local path of the folder relative to root
	 * @param repositoryDir the remote path of the folder relative to the repository
	 * @return the new folder
	 */
	protected static ICVSFolder createFolder(
			Session session,
			String localDir, 
			String repositoryDir) throws CVSException {
		
		ICVSFolder folder = session.getLocalRoot().getFolder(localDir);
		if (!folder.exists() 
				&&  (!CVSProviderPlugin.getPlugin().getPruneEmptyDirectories() 
						|| !folder.getParent().isCVSFolder())) {
			// Only create the folder if pruning is disabled or the
			// folder's parent is not a CVS folder (which occurs on checkout).
			// When pruning is enabled, the folder will be lazily created
			// when it contains a file (see getExistingFolder)
			folder.mkdir();
		}
		if (! folder.isCVSFolder()) {
			String repositoryRoot = session.getRepositoryRoot();
			String relativePath;
			if (repositoryDir.startsWith(repositoryRoot)) {
				// The repositoryDir is an absolute path
				relativePath = Util.getRelativePath(repositoryRoot, repositoryDir);
			} else {
				// The repositoryDir is already a relative path
				relativePath = repositoryDir;
			}
			IResource resource = folder.getIResource();
			if (resource != null) {
				IProject project = resource.getProject();
				if (project != null && project.isAccessible() && !CVSTeamProvider.isSharedWithCVS(project)) {
					// The project isn't shared but we are about to perform an operation on it.
					// we need to flag the project as shared so that the sync info management works
					CVSTeamProvider.markAsTempShare(project);
				}
			}
			try{
			folder.setFolderSyncInfo(new FolderSyncInfo(
				relativePath,
				session.getCVSRepositoryLocation().getLocation(false),
				null, false));
			} catch (CVSException ex){
				IStatus status = ex.getStatus();
				if (status != null){
					if (status.getCode() == IResourceStatus.INVALID_VALUE){
						//if it's an invalid value, just ignore the exception (see Bug# 152053),
						//else throw it again
					} else {
						throw ex;
					}
				}
			}
		}
		return folder;
	}

	protected ICVSFolder getExistingFolder(Session session, String localDir) throws CVSException {
			ICVSFolder mParent = session.getLocalRoot().getFolder(localDir);
			if (! mParent.exists()) {
				// First, check if the parent is a phantom
				IContainer container = (IContainer)mParent.getIResource();
				if (container != null) {
					try {
						// Create all the parents as need
						recreatePhantomFolders(mParent);
					} catch (CVSException e) {
						if (!handleInvalidResourceName(session, mParent, e)) {
							throw e;
						}
					}
				}
			}
			return mParent;
		}

	/**
	 * Method recreatePhantomFolders.
	 * @param mParent
	 */
	private void recreatePhantomFolders(ICVSFolder folder) throws CVSException {
		ICVSFolder parent = folder.getParent();
		if (!parent.exists()) {
			recreatePhantomFolders(parent);
		}
		folder.mkdir();
	}

	/**
	 * Return as instance that can be used by an open session. Subclasses that contain
	 * session related state must override this message to return a copy of themselves.
	 */
	/* package */ ResponseHandler getInstance() {
		return this;
	}
	
	protected boolean handleInvalidResourceName(Session session, ICVSResource resource, CVSException e) {
		int code = e.getStatus().getCode();
		if (code == IResourceStatus.INVALID_VALUE
				|| code == IResourceStatus.INVALID_RESOURCE_NAME
				|| code == IResourceStatus.RESOURCE_NOT_FOUND
				|| code == IResourceStatus.RESOURCE_EXISTS
				|| code == IResourceStatus.RESOURCE_WRONG_TYPE
				|| code == IResourceStatus.CASE_VARIANT_EXISTS
				|| code == IResourceStatus.PATH_OCCUPIED) {
			
			try {
				IResource local = resource.getIResource();
				String path;
				if (local == null) {
					path = resource.getRepositoryRelativePath();
				} else {
					path = local.getFullPath().toString();
				}
				IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.INVALID_LOCAL_RESOURCE_PATH, NLS.bind(CVSMessages.ResponseHandler_0, new String[] { path, e.getMessage() }), e, session.getLocalRoot()); 
				session.handleResponseError(status);
			} catch (CVSException e1) {
				CVSProviderPlugin.log(e1);
			}
			return true;
		}
		return false;
	}
}

