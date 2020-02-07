/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
/**
 * 
 */
package org.eclipse.team.internal.ccvs.core.filesystem;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ILogEntry;
import org.eclipse.team.internal.ccvs.core.client.listeners.ILogEntryListener;
import org.eclipse.team.internal.ccvs.core.util.Util;

class LogEntryCache implements ILogEntryListener {

	/*
	 * Cache of all log entries
	 */
	private Map entries = new HashMap(); /* Map String:remoteFilePath->Map (String:revision -> ILogEntry) */

	Map internalGetLogEntries(String path) {
		return (Map) entries.get(path);
	}

	/**
	 * Return all the log entries at the given path
	 * @param path the file path
	 * @return the log entries for the file
	 */
	public ILogEntry[] getLogEntries(String path) {
		Map map = internalGetLogEntries(path);
		return (ILogEntry[]) map.values().toArray(new ILogEntry[map.size()]);
	}

	ILogEntry internalGetLogEntry(String path, String revision) {
		Map fileEntries = internalGetLogEntries(path);
		if (fileEntries != null) {
			return (ILogEntry) fileEntries.get(revision);
		}
		return null;
	}

	public String[] getCachedFilePaths() {
		return (String[]) entries.keySet().toArray(new String[entries.size()]);
	}

	/**
	 * Return the log entry that for the given resource
	 * or <code>null</code> if no entry was fetched or the
	 * resource is not a file.
	 * @param getFullPath(resource) the resource
	 * @return the log entry or <code>null</code>
	 */
	public synchronized ILogEntry getLogEntry(ICVSRemoteResource resource) {
		if (resource instanceof ICVSRemoteFile) {
			try {
				String path = getFullPath(resource);
				String revision = ((ICVSRemoteFile) resource).getRevision();
				return internalGetLogEntry(path, revision);
			} catch (TeamException e) {
				// Log and return null
			}
		}
		return null;
	}

	/**
	 * Return the log entries that were fetched for the given resource
	 * or an empty list if no entry was fetched.
	 * @param getFullPath(resource) the resource
	 * @return the fetched log entries or an empty list is none were found
	 */
	public synchronized ILogEntry[] getLogEntries(ICVSRemoteResource resource) {
		Map fileEntries = internalGetLogEntries(getFullPath(resource));
		if (fileEntries != null) {
			return (ILogEntry[]) fileEntries.values().toArray(new ILogEntry[fileEntries.size()]);
		}
		return new ILogEntry[0];
	}

	/*
	 * Return the full path that uniquely identifies the resource
	 * accross repositories. This path include the repository and
	 * resource path but does not include the revision so that 
	 * all log entries for a file can be retrieved.
	 */
	String getFullPath(ICVSRemoteResource resource) {
		return Util.appendPath(resource.getRepository().getLocation(false), resource.getRepositoryRelativePath());
	}

	public synchronized void clearEntries() {
		entries.clear();
	}

	public synchronized ICVSRemoteFile getImmediatePredecessor(ICVSRemoteFile file) throws TeamException {
		ILogEntry[] allLogs = getLogEntries(file);
		String revision = file.getRevision();
		// First decrement the last digit and see if that revision exists
		String predecessorRevision = getPredecessorRevision(revision);
		ICVSRemoteFile predecessor = findRevison(allLogs, predecessorRevision);
		// If nothing was found, try to fond the base of a branch
		if (predecessor == null && isBrancheRevision(revision)) {
			predecessorRevision = getBaseRevision(revision);
			predecessor = findRevison(allLogs, predecessorRevision);
		}
		// If that fails, it is still possible that there is a revision.
		// This can happen if the revision has been manually set.
		if (predecessor == null) {
			// We don't search in this case since this is costly and would be done
			// for any file that is new as well.
		}
		return predecessor;
	}

	/*
	 * Find the given revision in the list of log entries.
	 * Return null if the revision wasn't found.
	 */
	ICVSRemoteFile findRevison(ILogEntry[] allLogs, String predecessorRevision) throws TeamException {
		for (ILogEntry entry : allLogs) {
			ICVSRemoteFile file = entry.getRemoteFile();
			if (file.getRevision().equals(predecessorRevision)) {
				return file;
			}
		}
		return null;
	}

	/*
	 * Decrement the trailing digit by one.
	 */
	String getPredecessorRevision(String revision) {
		int digits[] = Util.convertToDigits(revision);
		digits[digits.length - 1]--;
		StringBuilder buffer = new StringBuilder(revision.length());
		for (int i = 0; i < digits.length; i++) {
			buffer.append(Integer.toString(digits[i]));
			if (i < digits.length - 1) {
				buffer.append('.');
			}
		}
		return buffer.toString();
	}

	/*
	 * Return true if there are more than 2 digits in the revision number
	 * (i.e. the revision is on a branch)
	 */
	boolean isBrancheRevision(String revision) {
		return Util.convertToDigits(revision).length > 2;
	}

	/*
	 * Remove the trailing revision digits such that the
	 * returned revision is shorter than the given revision 
	 * and is an even number of digits long
	 */
	String getBaseRevision(String revision) {
		int digits[] = Util.convertToDigits(revision);
		int length = digits.length - 1;
		if (length % 2 == 1) {
			length--;
		}
		StringBuilder buffer = new StringBuilder(revision.length());
		for (int i = 0; i < length; i++) {
			buffer.append(Integer.toString(digits[i]));
			if (i < length - 1) {
				buffer.append('.');
			}
		}
		return buffer.toString();
	}

	/**
	 * Remove any entries for the remote resources
	 * @param resource the remote resource
	 */
	public synchronized void clearEntries(ICVSRemoteResource resource) {
		String remotePath = getFullPath(resource);
		entries.remove(remotePath);
	}

	@Override
	public void handleLogEntryReceived(ILogEntry entry) {
		ICVSRemoteFile file = entry.getRemoteFile();
		String fullPath = getFullPath(file);
		String revision = entry.getRevision();
		Map fileEntries = internalGetLogEntries(fullPath);
		if (fileEntries == null) {
			fileEntries = new HashMap();
			entries.put(fullPath, fileEntries);
		}
		fileEntries.put(revision, entry);
	}
}
