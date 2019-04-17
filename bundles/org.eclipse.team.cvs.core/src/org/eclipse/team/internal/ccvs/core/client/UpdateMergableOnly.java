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
 *     Red Hat Incorporated - is/setExecutable() code
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.*;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * This custom update command will only update files that 
 * are either incoming changes (Update-existing) or auto-mergable
 * (Merged with no "+=" in entry line).
 */
public class UpdateMergableOnly extends Update {
	
	private static final String LOCAL_FILE_PATH_VARIABLE_NAME = "localFilePath"; //$NON-NLS-1$
	private static ServerMessageLineMatcher MERGE_UPDATE_CONFLICTING_ADDITION_MATCHER;
	static {
		// TODO: temprary until proper lifecycle is defined
		initializePatterns();
	}
	public static void initializePatterns() {
		try {
			MERGE_UPDATE_CONFLICTING_ADDITION_MATCHER = new ServerMessageLineMatcher(
				IMessagePatterns.MERGE_UPDATE_CONFLICTING_ADDITION, new String[] {LOCAL_FILE_PATH_VARIABLE_NAME});
		} catch (CVSException e) {
			// This is serious as the listener will not function properly
			CVSProviderPlugin.log(e);
		}
	}
	
	List skippedFiles = new ArrayList();
	
	public class MergableOnlyUpdatedHandler extends UpdatedHandler {
		
		public MergableOnlyUpdatedHandler() {
			// handle "Merged" responses
			super(UpdatedHandler.HANDLE_MERGED);
		}
		
		@Override
		protected ICVSFile getTargetFile(ICVSFolder mParent, String fileName, byte[] entryBytes) throws CVSException {
			String adjustedFileName = fileName;
			if (ResourceSyncInfo.isMergedWithConflicts(entryBytes)) {
				// for merged-with-conflict, return a temp file
				adjustedFileName = ".##" + adjustedFileName + " " + ResourceSyncInfo.getRevision(entryBytes); //$NON-NLS-1$ //$NON-NLS-2$
				skippedFiles.add(((IContainer)mParent.getIResource()).getFile(new Path(null, fileName)));
			}
			return super.getTargetFile(mParent, adjustedFileName, entryBytes);
		}
		
		@Override
		protected void receiveTargetFile(
			Session session,
			ICVSFile mFile,
			String entryLine,
			Date modTime,
			boolean binary,
			boolean readOnly,
			boolean executable,
			IProgressMonitor monitor)
			throws CVSException {
			
			if (ResourceSyncInfo.isMergedWithConflicts(entryLine.getBytes())) {
				// For merged-with-conflict, just recieve the file contents.
				// Use the Updated handler type so that the file will be created or
				// updated.
				session.receiveFile(mFile, binary, UpdatedHandler.HANDLE_UPDATED, monitor);
				// Now delete the file since it is not used
				mFile.delete();
			} else {
				super.receiveTargetFile(session, mFile, entryLine, modTime, binary, readOnly, executable, monitor);
			}
		}
	}
	
	/**
	 * Override the general update listener to handle the following
	 * message:
	 *   cvs server: file folder/file.ext exists, but has been added in revision TAG_NAME
	 * This is required because MergeSubscriber adjusts the base when an update 
	 * occurs and we can end up in a situation where the update faile with the
	 * above message (see buh 58654).
	 */
	public class MergeUpdateListener extends UpdateListener {
		public MergeUpdateListener(IUpdateMessageListener updateMessageListener) {
			super(updateMessageListener);
		}
		public IStatus errorLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot, IProgressMonitor monitor) {
			Map variables = MERGE_UPDATE_CONFLICTING_ADDITION_MATCHER.processServerMessage(line);
			if (variables != null) {
				String filePath = (String)variables.get(LOCAL_FILE_PATH_VARIABLE_NAME);
				try {
					ICVSResource cvsResource = commandRoot.getChild(filePath);
					IResource resource = cvsResource.getIResource();
					if (resource != null && resource.getType() == IResource.FILE) {
						skippedFiles.add(resource);
						return OK;
					}
				} catch (CVSException e) {
					CVSProviderPlugin.log(e);
					// Fall through to let the superclass process the error line
				}
			}
			return super.errorLine(line, location, commandRoot, monitor);
		}
	}
	
	@Override
	protected IStatus doExecute(
		Session session,
		GlobalOption[] globalOptions,
		LocalOption[] localOptions,
		String[] arguments,
		ICommandOutputListener listener,
		IProgressMonitor monitor)
		throws CVSException {
		
		MergableOnlyUpdatedHandler newHandler = new MergableOnlyUpdatedHandler();
		ResponseHandler oldHandler = session.getResponseHandler(newHandler.getResponseID());
		skippedFiles.clear();
		try {
			session.registerResponseHandler(newHandler);
			// Don't create backup files since merges won't be overridden
			session.setCreateBackups(false);
			return super.doExecute(
				session,
				globalOptions,
				localOptions,
				arguments,
				new MergeUpdateListener(null),
				monitor);
		} finally {
			session.registerResponseHandler(oldHandler);
			session.setCreateBackups(true);
		}
	}

	public IFile[] getSkippedFiles() {
		return (IFile[]) skippedFiles.toArray(new IFile[skippedFiles.size()]);
	}
	
}
