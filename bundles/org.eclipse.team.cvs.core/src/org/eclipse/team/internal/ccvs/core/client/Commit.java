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


import java.util.Collection;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;

public class Commit extends Command {
	/*** Local options: specific to commit ***/
	// Forces a file to be committed even if it has not been modified; implies -l.
	// NOTE: This option is not fully supported -- a file will not be sent
	//       unless it is dirty.  The primary use is to resend a file that may
	//       or may not be changed (e.g. could depend on CR/LF translations, etc...)
	//       and force the server to create a new revision and reply Checked-in.
	public static final LocalOption FORCE = new LocalOption("-f"); //$NON-NLS-1$

	protected Commit() { }
	protected String getRequestId() {
		return "ci"; //$NON-NLS-1$
	}

	/**
	 * Send all files under the workingFolder as changed files to 
	 * the server.
	 */		
	protected ICVSResource[] sendLocalResourceState(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor)
		throws CVSException {			

		// Get the folders we want to work on
		checkResourcesManaged(session, resources);
		
		// Send all changed files to the server	
		ModifiedFileSender visitor = new ModifiedFileSender(session, localOptions);
		visitor.visit(session, resources, monitor);
		
		// Send the changed files as arguments (because this is what other cvs clients do)
		ICVSFile[] changedFiles = visitor.getModifiedFiles();
		for (ICVSFile changedFile : changedFiles) {
			// escape file names, see bug 149683
			String fileName = changedFile.getRelativePath(session.getLocalRoot());
			if(fileName.startsWith("-")){ //$NON-NLS-1$
				fileName = "./" + fileName; //$NON-NLS-1$
			}
			session.sendArgument(fileName);
		}
		return changedFiles;
	}
	
	/**
	 * On successful finish, prune empty directories if the -P or -D option was specified.
	 */
	protected IStatus commandFinished(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor,
		IStatus status) throws CVSException {
		// If we didn't succeed, don't do any post processing
		if (status.getCode() == CVSStatus.SERVER_ERROR) {
			return status;
		}

		// If pruning is enable, prune empty directories after a commit
		if (CVSProviderPlugin.getPlugin().getPruneEmptyDirectories()) { 
			new PruneFolderVisitor().visit(session, resources);
		}
		
		// Reset the timestamps of any committed files that are still dirty.
		// Only do so if there were no E messages from the server
		if (status.isOK()) {
			for (ICVSResource resource : resources) {
				if (!resource.isFolder()) {
					ICVSFile cvsFile = (ICVSFile) resource;
					if (cvsFile.exists() && cvsFile.isModified(null)) {
						status = mergeStatus(status, clearModifiedState(cvsFile));
					}
				}
			}
		}
		return status;
	}
	
	protected IStatus clearModifiedState(ICVSFile cvsFile) throws CVSException {
		byte[] info = cvsFile.getSyncBytes();
		IResource resource = cvsFile.getIResource();
		String filePath;
		if (resource == null) {
			filePath = cvsFile.getRepositoryRelativePath();
		} else {
			filePath = resource.getFullPath().toString();
		}
		if (info == null) {
			// There should be sync info. Log the problem
			return new Status(IStatus.WARNING, CVSProviderPlugin.ID, 0, NLS.bind(CVSMessages.Commit_syncInfoMissing, new String[] { filePath }), null); 
		}
		cvsFile.checkedIn(null, true /* commit in progress */);
		return new Status(IStatus.INFO, CVSProviderPlugin.ID, 0, NLS.bind(CVSMessages.Commit_timestampReset, new String[] { filePath }), null); //;
	}
	
	/**
	 * We do not want to send the arguments here, because we send
	 * them in sendRequestsToServer (special handling).
	 */
	protected void sendArguments(Session session, String[] arguments) throws CVSException {
	}
	
	public final IStatus execute(Session session, GlobalOption[] globalOptions, LocalOption[] localOptions, 
		ICVSResource[] arguments, Collection filesToCommitAsText,
		ICommandOutputListener listener, IProgressMonitor pm) throws CVSException {
		
		session.setTextTransferOverride(filesToCommitAsText);
		try {
			return super.execute(session, globalOptions, localOptions, arguments, listener, pm);
		} finally {
			session.setTextTransferOverride(null);
		}
	}
	
	protected String getDisplayText() {
		return "commit"; //$NON-NLS-1$
	}
}	
