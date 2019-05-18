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
package org.eclipse.team.internal.ccvs.core.client.listeners;


import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.CommandOutputListener;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * Used with 'admin -ksubst' to capture lines of text that are issued
 * as confirmation that the remote keyword substitution mode has been
 * changed.  When encountered, updates the local ResourceSyncInfo for
 * the file in question to reflect
 * 
 * e.g.
 *   RCS file: path/filename,v
 *   done
 * 
 * We don't expect to see anything special on stderr if the command succeeds.
 */
public class AdminKSubstListener extends CommandOutputListener {
	private KSubstOption ksubstMode;
	
	public AdminKSubstListener(KSubstOption ksubstMode) {
		this.ksubstMode = ksubstMode;
	}
	
	public IStatus messageLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot,
		IProgressMonitor monitor) {
		if (line.startsWith("RCS file:")) { //$NON-NLS-1$
			String rcsFile = line.substring(10).trim();
			if (! rcsFile.endsWith(",v")) { //$NON-NLS-1$
				return new CVSStatus(IStatus.ERROR,
					NLS.bind(CVSMessages.AdminKSubstListener_expectedRCSFile, new String[] { rcsFile })); 
			}
			IPath rcsFilePath = new Path(null, Util.removeAtticSegment(rcsFile.substring(0, rcsFile.length() - 2)));
			try {
				ICVSFile file = findLocalFileFor(commandRoot, rcsFilePath);
				//ResourceSyncInfo info = file.getSyncInfo();
				byte[] syncBytes = file.getSyncBytes();
				if (syncBytes != null) {
					// only update sync info if we have it locally
					file.setSyncBytes(ResourceSyncInfo.setKeywordMode(syncBytes, ksubstMode), ICVSFile.UNKNOWN);
				}
			} catch (CVSException e) {
				return e.getStatus();
			}
		}
		return OK;
	}
	
	private ICVSFile findLocalFileFor(ICVSFolder commandRoot, IPath rcsFilePath) throws CVSException {
		
		// First, look for the local file by following the remote path
		FolderSyncInfo info = commandRoot.getFolderSyncInfo();
		String remoteRootLocation = info.getRemoteLocation();
		if (remoteRootLocation == null) {
			throw new CVSException(new CVSStatus(IStatus.ERROR,
				CVSMessages.AdminKSubstListener_commandRootNotManaged)); 
		}
		IPath remoteRootPath = new Path(null, remoteRootLocation);
		if (remoteRootPath.isPrefixOf(rcsFilePath)) {
			IPath relativeFilePath = rcsFilePath.removeFirstSegments(remoteRootPath.segmentCount());
			ICVSFile file = commandRoot.getFile(relativeFilePath.toString());
			if (file.isManaged() && isMatchingPath(file, rcsFilePath)) {
				return file;
			}
		}
		
		// We couldn't find the file that way which means we're working in a defined module.
		// Scan all folders looking for a match
		ICVSFolder parent = findFolder(commandRoot, rcsFilePath.removeLastSegments(1));
		if (parent != null) {
			ICVSFile file = parent.getFile(rcsFilePath.lastSegment());
			if (file.isManaged()) {
				return file;
			}
		}
		
		// No file was found so return null;
		throw new CVSException(new CVSStatus(IStatus.ERROR,
				NLS.bind(CVSMessages.AdminKSubstListener_expectedChildOfCommandRoot, new String[] { rcsFilePath.toString(), remoteRootPath.toString() })));
	}

	private ICVSFolder findFolder(ICVSFolder commandRoot, IPath path) throws CVSException {
		final String remotePath = path.toString();
		final ICVSFolder[] result = new ICVSFolder[] { null };
		commandRoot.accept(new ICVSResourceVisitor() {
			public void visitFile(ICVSFile file) throws CVSException {
				// Nothing to do for files
			}
			public void visitFolder(ICVSFolder folder) throws CVSException {
				FolderSyncInfo info = folder.getFolderSyncInfo();
				if (info != null && info.getRemoteLocation().equals(remotePath)) {
					// We found the folder we're looking for
					result[0] = folder;
				}
				if (result[0] == null) {
					folder.acceptChildren(this);
				}
			}
		});
		return result[0];
	}

	private boolean isMatchingPath(ICVSFile file, IPath rcsFilePath) throws CVSException {
		FolderSyncInfo info = file.getParent().getFolderSyncInfo();
		return info != null 
			&& info.getRemoteLocation().equals(rcsFilePath.removeLastSegments(1).toString());
	}
}
