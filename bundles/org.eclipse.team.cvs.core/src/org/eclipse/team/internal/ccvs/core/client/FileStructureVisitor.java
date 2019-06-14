/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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


import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;

/**
 * Visitor to send the local file structure to the CVS Server.
 * 
 * Files are sent as Unchanged or Modified.
 * Folders are sent if they contain files unless sendEmptyFolders is true
 * in which case all folders are sent.
 *
 * @param sendEmptyFolders sends the folder-entrie even if there is no file 
 		  to send in it
 */

class FileStructureVisitor extends AbstractStructureVisitor {

	private boolean sendEmptyFolders;

	public FileStructureVisitor(Session session, LocalOption[] localOptions, boolean sendEmptyFolders, boolean sendModifiedContents) {
		this(session, localOptions, sendEmptyFolders, sendModifiedContents, true);
	}

	public FileStructureVisitor(Session session, LocalOption[] localOptions, boolean sendEmptyFolders, boolean sendModifiedContents, boolean sendBinary) {
		super(session, localOptions, true, sendModifiedContents, sendBinary);
		this.sendEmptyFolders = sendEmptyFolders;
	}

	@Override
	public void visitFile(ICVSFile mFile) throws CVSException {
		sendFile(mFile);
	}

	@Override
	public void visitFolder(ICVSFolder mFolder) throws CVSException {

		if (sendEmptyFolders) {
			// If we want to send empty folder, that just send it when
			// we come to it
			sendFolder(mFolder);
		}

		boolean exists = mFolder.exists();
		boolean isCVSFolder = mFolder.isCVSFolder();
		
		// We are only interested in CVS folders
		// A folder could be a non-existant CVS folder if it is a holder for outgoing file deletions
		if ( ! isCVSFolder) return;
		
		if (exists &&  isOrphanedSubtree(mFolder)) {
			return;
		}

		// Send files, then the questionable folders, then the managed folders
		ICVSResource[] children = mFolder.members(ICVSFolder.ALL_UNIGNORED_MEMBERS);
		sendFiles(children);
		sendQuestionableFolders(children);
		if (isRecurse()) {
			sendManagedFolders(children);
		}
	}

	/**
	 * Method sendManagedFolders.
	 * @param children
	 */
	private void sendManagedFolders(ICVSResource[] children) throws CVSException {
		for (ICVSResource resource : children) {
			if (resource.isFolder() && resource.isManaged()) {
				resource.accept(this);
			}
		}
	}

	/**
	 * Method sendQuestionableFolders.
	 * @param children
	 */
	private void sendQuestionableFolders(ICVSResource[] children) throws CVSException {
		for (ICVSResource resource : children) {
			if (resource.isFolder() && ! resource.isManaged()) {
				resource.accept(this);
			}
		}
	}

	/**
	 * Method sendFiles.
	 * @param children
	 */
	private void sendFiles(ICVSResource[] children) throws CVSException {
		for (ICVSResource resource : children) {
			if (!resource.isFolder()) {
				resource.accept(this);
			}
		}
	}

}
