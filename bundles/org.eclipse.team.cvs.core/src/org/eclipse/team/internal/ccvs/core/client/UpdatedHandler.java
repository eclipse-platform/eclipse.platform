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
 *     Red Hat Incorporated - is/setExecutable() code
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.client;

import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.syncinfo.MutableResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * Handles any "Updated" and "Merged" responses
 * from the CVS server.
 * <p>
 * Suppose as a result of performing a command the CVS server responds
 * as follows:<br>
 * <pre>
 *   [...]
 *   Updated ???\n
 *   [...]
 * </pre>
 * Then 
 * </p>
 */

/**
 * Does get information about the file that is updated
 * and the file-content itself and puts it on the fileSystem.
 * 
 * The difference beetween the "Updated" and the "Merged" is, that
 * an "Merged" file is not going to be up-to-date after the operation.
 * 
 * Requiers a exisiting parent-folder.
 */
public class UpdatedHandler extends ResponseHandler {
	
	private int handlerType;
	
	public static final int HANDLE_UPDATED = ICVSFile.UPDATED;
	public static final int HANDLE_MERGED = ICVSFile.MERGED;
	public static final int HANDLE_UPDATE_EXISTING = ICVSFile.UPDATE_EXISTING;
	public static final int HANDLE_CREATED = ICVSFile.CREATED;
	
	private static final String READ_ONLY_FLAG = "u=rw"; //$NON-NLS-1$
	private static final String EXECUTE_FLAG = "x"; //$NON-NLS-1$
	
	public UpdatedHandler(int handlerType) {
		this.handlerType = handlerType;
	}
	
	public String getResponseID() {
		switch (handlerType) {
			case HANDLE_UPDATED: return "Updated"; //$NON-NLS-1$
			case HANDLE_MERGED: return "Merged"; //$NON-NLS-1$
			case HANDLE_UPDATE_EXISTING: return "Update-existing"; //$NON-NLS-1$
			case HANDLE_CREATED: return "Created"; //$NON-NLS-1$
		}
		return null;
	}

	public void handle(Session session, String localDir, IProgressMonitor monitor) throws CVSException {
		// read additional data for the response
		String repositoryFile = session.readLine();
		String entryLine = session.readLine();
		byte[] entryBytes = entryLine.getBytes();
		String permissionsLine = session.readLine();

		// clear file update modifiers
		Date modTime = session.getModTime();
		session.setModTime(null);
		
		// Get the local file
		String fileName = repositoryFile.substring(repositoryFile.lastIndexOf("/") + 1); //$NON-NLS-1$
		ICVSFolder mParent = getExistingFolder(session, localDir);
		ICVSFile mFile = getTargetFile(mParent, fileName, entryBytes);
		
		boolean binary = ResourceSyncInfo.isBinary(entryBytes);
		boolean readOnly = !permissionsLine.contains(READ_ONLY_FLAG);
		boolean executable = permissionsLine.contains(EXECUTE_FLAG);
		
		try {
			// The file may have been set as read-only by a previous checkout/update
			if (mFile.isReadOnly()) mFile.setReadOnly(false);
		} catch (CVSException e) {
			// Just log and keep going
			CVSProviderPlugin.log(e);
		}
		
		try {
			receiveTargetFile(session, mFile, entryLine, modTime, binary, readOnly, executable, monitor);
		} catch (CVSException e) {
			// An error occurred while recieving the file.
			// If it is due to an invalid file name,
			// accumulate the error and continue.
			// Otherwise, exit
			if (!handleInvalidResourceName(session, mFile, e)) {
				throw e;
			}
		}
	}

	protected ICVSFile getTargetFile(ICVSFolder mParent, String fileName, byte[] entryBytes) throws CVSException {
		return mParent.getFile(fileName);
	}
	
	protected void receiveTargetFile(Session session, ICVSFile mFile, String entryLine, Date modTime, boolean binary, boolean readOnly, boolean executable, IProgressMonitor monitor) throws CVSException {
		
		// receive the file contents from the server
		session.receiveFile(mFile, binary, handlerType, monitor);
		
		// Set the timestamp in the file and get it again so that we use the *real* timestamp
		// in the sync info. The os may not actually set the time we provided :)
		mFile.setTimeStamp(modTime);
		modTime = mFile.getTimeStamp();
		ResourceSyncInfo info = new ResourceSyncInfo(entryLine, null);
		MutableResourceSyncInfo newInfoWithTimestamp = info.cloneMutable();
		newInfoWithTimestamp.setTimeStamp(modTime);
		
		//see bug 106876
		CVSTag tag = newInfoWithTimestamp.getTag();
		if(tag != null && CVSTag.BASE.getName().equals(tag.getName())){
			newInfoWithTimestamp.setTag(mFile.getSyncInfo().getTag());
		}
		
		int modificationState = ICVSFile.UNKNOWN;
		if(handlerType==HANDLE_MERGED) {
			newInfoWithTimestamp.setMerged();
		} else if (!session.isIgnoringLocalChanges()
			&& !info.isAdded() /* could be an added entry during a merge in which case it is dirty */
			&& (handlerType==HANDLE_UPDATE_EXISTING || handlerType==HANDLE_CREATED)) {
			// both these cases result in an unmodified file.
			// reporting is handled by the FileModificationManager
			modificationState = ICVSFile.CLEAN;
			CVSProviderPlugin.getPlugin().getFileModificationManager().updated(mFile);
		}
		mFile.setSyncInfo(newInfoWithTimestamp, modificationState);
		try {
			if (readOnly) mFile.setReadOnly(true);
			if (executable) mFile.setExecutable(true);
		} catch (CVSException e) {
			// Just log and keep going
			CVSProviderPlugin.log(e);
		}
	}

	public int getHandlerType() {
		return handlerType;
	}
	
}
