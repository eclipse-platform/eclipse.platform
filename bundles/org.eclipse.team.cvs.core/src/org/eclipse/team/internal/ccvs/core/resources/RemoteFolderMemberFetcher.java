/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.resources;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.*;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * Fetch the children for the given parent folder. When fetchMembers is invoked,
 * the children of the folder will be fetched from the server and assigned to
 * the children of the parent folder.
 */
public class RemoteFolderMemberFetcher implements IUpdateMessageListener, IStatusListener {
	
	private final RemoteFolder parentFolder;
	private CVSTag tag;
	
	List<RemoteFolder> folders = new ArrayList<>();
	List<RemoteFile> files = new ArrayList<>();
	boolean exists = true;
	List<CVSException> exceptions = new ArrayList<>();
	
	protected RemoteFolderMemberFetcher(RemoteFolder parentFolder, CVSTag tag) {
		this.tag = tag;
		this.parentFolder = parentFolder;
	}
	
	/**
	 * Fetch the members for a given tag and returns them.
	 * During the execution of this method, the instance variable children
	 * will be used to contain the children. However, the variable is reset
	 * and the result returned. Thus, instances of RemoteFolder do not
	 * persist the children. Subclasses (namely RemoteFolderTree) may
	 * persist the children.
	 */
	public void fetchMembers(IProgressMonitor monitor) throws CVSException {
		fetchMembers(monitor, tag);
	}
	public void fetchMembers(IProgressMonitor monitor, CVSTag tag) throws CVSException {
		final IProgressMonitor progress = Policy.monitorFor(monitor);
		progress.beginTask(CVSMessages.RemoteFolder_getMembers, 100); 
		try {
			// Update the parent folder children so there are no children
			updateParentFolderChildren();
			// Perform an update to retrieve the child files and folders
			IStatus status = performUpdate(Policy.subMonitorFor(progress, 50), tag);
			// Update the parent folder with the new children
			updateParentFolderChildren();
			Policy.checkCanceled(monitor);
			
			// Handle any errors that were identified by the listener
			performErrorCheck(status, CVSMessages.RemoteFolder_errorFetchingMembers); 
			
			// Get the revision numbers for the files
			ICVSFile[] remoteFiles = getFiles();
			if (remoteFiles.length > 0) {
				updateFileRevisions(remoteFiles, Policy.subMonitorFor(progress, 50));
			} else {
				progress.worked(50);
			}
		} catch (CVSServerException e) {
			if ( ! e.isNoTagException() && e.containsErrors())
				throw e;
			if (tag == null)
				throw e;
			// we now know that this is an exception caused by a cvs bug.
			// if the folder has no files in it (just subfolders) cvs does not respond with the subfolders...
			// workaround: retry the request with no tag to get the directory names (if any)
			Policy.checkCanceled(progress);
			fetchMembers(Policy.subMonitorFor(progress, 50), null);
		} finally {
			progress.done();
		}
	}

	protected IStatus performUpdate(IProgressMonitor progress, CVSTag tag) throws CVSException {
		progress.beginTask(null, 100);
		Session session = new Session(parentFolder.getRepository(), parentFolder, false /* output to console */);
		session.open(Policy.subMonitorFor(progress, 10), false /* read-only */);
		try {
			// Build the local options
			final List<LocalOption> localOptions = new ArrayList<>();
			localOptions.add(Update.RETRIEVE_ABSENT_DIRECTORIES);
			if (tag != null) localOptions.add(Update.makeTagOption(tag));
			
			return Command.UPDATE.execute(
				session,
				new GlobalOption[] { Command.DO_NOT_CHANGE },
				localOptions.toArray(new LocalOption[localOptions.size()]),
				new ICVSResource[] { parentFolder },
				new UpdateListener(this),
			Policy.subMonitorFor(progress, 90));
		} finally {
			session.close();
		}
	}
	
	protected void updateFileRevisions(final ICVSFile[] files, IProgressMonitor monitor) throws CVSException {
			
		// Perform a "cvs status..." with a listener
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		QuietOption quietness = CVSProviderPlugin.getPlugin().getQuietness();
		try {
			CVSProviderPlugin.getPlugin().setQuietness(Command.VERBOSE);
			Session session = new Session(parentFolder.getRepository(), parentFolder, false /* output to console */);
			session.open(Policy.subMonitorFor(monitor, 10), false /* read-only */);
			try {
				IStatus status = Command.STATUS.execute(
					session,
					Command.NO_GLOBAL_OPTIONS,
					Command.NO_LOCAL_OPTIONS,
					files,
					new StatusListener(this),
					Policy.subMonitorFor(monitor, 90));
				performErrorCheck(status, CVSMessages.RemoteFolder_errorFetchingRevisions); 
				// TODO: Ensure all files have a revision?
			} finally {
				session.close();
			}
		} finally {
			CVSProviderPlugin.getPlugin().setQuietness(quietness);
		}
	}

	private void performErrorCheck(IStatus status, String errorTitle) throws CVSException {
		if (status.getCode() == CVSStatus.SERVER_ERROR) {
			// Only throw the exception if no files or folders were found
			if (folders.size() + files.size() == 0) {
				throw new CVSServerException(status);
			} else {
				CVSProviderPlugin.log(new CVSServerException(status));
			}	
		}
		if (!exists) {
			IStatus notExistStatus = new CVSStatus(IStatus.ERROR, CVSStatus.DOES_NOT_EXIST, NLS.bind(CVSMessages.RemoteFolder_doesNotExist, new String[] { this.parentFolder.getRepositoryRelativePath() }), parentFolder);
			throw new CVSException(notExistStatus); 
		}
		
		// Report any internal exceptions that occurred fetching the members
		if ( ! exceptions.isEmpty()) {
			if (exceptions.size() == 1) {
				throw exceptions.get(0);
			} else {
				MultiStatus multi = new MultiStatus(CVSProviderPlugin.ID, 0, errorTitle, null);
				for (int i = 0; i < exceptions.size(); i++) {
					multi.merge(exceptions.get(i).getStatus());
				}
				throw new CVSException(multi);
			}
		}
	}
	
	@Override
	public void directoryInformation(ICVSFolder commandRoot, String stringPath, boolean newDirectory) {
		try {
			IPath path = this.parentFolder.getRelativePathFromRootRelativePath(commandRoot, new Path(null, stringPath));
			if (path.segmentCount() == 1) {
				String pathName = path.lastSegment();
				if (!pathName.equals(".")) { //$NON-NLS-1$
					recordFolder(path.lastSegment());
				}
			}
		} catch (CVSException e) {
			exceptions.add(e);
		}
	}

	@Override
	public void directoryDoesNotExist(ICVSFolder parent, String stringPath) {
		try {
			IPath path = this.parentFolder.getRelativePathFromRootRelativePath(parent, new Path(null, stringPath));
			if (path.isEmpty()) {
				parentDoesNotExist();
			}
		} catch (CVSException e) {
			exceptions.add(e);
		}
	}

	@Override
	public void fileInformation(int type, ICVSFolder parent, String filename) {
		try {
			IPath filePath = new Path(null, filename);
			filePath = this.parentFolder.getRelativePathFromRootRelativePath(parent, filePath);	
			if( filePath.segmentCount() == 1 ) {
				String properFilename = filePath.lastSegment();
				recordFile(properFilename);
			}
		} catch (CVSException e) {
			exceptions.add(e);
		}
	}

	@Override
	public void fileDoesNotExist(ICVSFolder parent, String filename) {
	}

	@Override
	public void fileStatus(ICVSFolder commandRoot, String path, String remoteRevision) {
		if (remoteRevision == IStatusListener.FOLDER_REVISION)
			// Ignore any folders
			return;
		try {
			((RemoteFile)parentFolder.getChild(Util.getLastSegment(path))).setRevision(remoteRevision);
		} catch (CVSException e) {
			exceptions.add(e);
		}
	}
	
	/**
	 * This method is invoked for each child folder as the responses are being received from
	 * the server. Default behavior is to record the folder for later retrieval using <code>getChilren()</code>. 
	 * Subclasses may override but should invoke the inherited method to ensure the folder gets recorded.
	 * @param name the name of the child folder
	 */
	protected RemoteFolder recordFolder(String name) {
		RemoteFolder folder = new RemoteFolder(
			parentFolder, 
			parentFolder.getRepository(), 
			Util.appendPath(parentFolder.getRepositoryRelativePath(), name), 
			tag);
		folders.add(folder);
		return folder;
	}

	/**
	 * This method is invoked for each child file as the responses are being received from
	 * the server. Default behavior is to record the file for later retrieval using <code>getChildren()</code>. 
	 * Subclasses may override but should invoke the inherited method to ensure the file gets recorded.
	 * This is important because the file revisions for any files are fetched subsequent to the fetching
	 * of the children.
	 * @param name the name of the child folder
	 */
	protected RemoteFile recordFile(String name) {
		RemoteFile file = new RemoteFile(
			parentFolder, 
			Update.STATE_NONE, 
			name, 
			null, /* revision unknown */
			null, /* keyword mode unknown */
			tag);
		files.add(file);
		return file;
	}
	
	/**
	 * This method is invoked to indicate that the parent being queried for children
	 * does not exist. Subclasses may override to get early notification of this but 
	 * should still invoke the inherited method.
	 */
	protected void parentDoesNotExist() {
		exists = false;
	}

	/**
	 * Update the parent folder such that it's children are the
	 * children that have been fetched by the receiver.
	 */
	protected void updateParentFolderChildren() {
		parentFolder.setChildren(getFetchedChildren());
	}
	
	/**
	 * Return the child files fetched from the server.
	 * @return
	 */
	protected ICVSFile[] getFiles() {
		return files.toArray(new ICVSFile[files.size()]);
	}
	
	/**
	 * Return an array of all fetched children.
	 * @return
	 */
	public ICVSRemoteResource[] getFetchedChildren() {
		ICVSRemoteResource[] resources = new ICVSRemoteResource[folders.size() + files.size()];
		int count = 0;
		for (ICVSRemoteResource resource : folders) {
			resources[count++] = resource;
		}
		for (ICVSRemoteResource resource : files) {
			resources[count++] = resource;
		}
		return resources;
	}

}
