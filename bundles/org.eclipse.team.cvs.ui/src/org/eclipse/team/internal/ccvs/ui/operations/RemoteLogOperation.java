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
package org.eclipse.team.internal.ccvs.ui.operations;

import java.util.*;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.ILogEntryListener;
import org.eclipse.team.internal.ccvs.core.client.listeners.LogListener;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Performs an rlog on the resources and caches the results.
 */
public class RemoteLogOperation extends RepositoryLocationOperation {
	
	private RLog rlog = new RLog();
	private CVSTag tag1;
	private CVSTag tag2;
	private LogEntryCache entryCache;
	
	/** 
	 * A log entry cache that can be shared by multiple instances of the
	 * remote log operation.
	 */
	public static class LogEntryCache implements ILogEntryListener {
		
		/*
		 * Cache of all log entries
		 */
		private Map<String, Map<String, ILogEntry>> entries = new HashMap<>(); /*
																				 * Map String:remoteFilePath->Map
																				 * (String:revision -> ILogEntry)
																				 */
		
		private Map<String, ILogEntry> internalGetLogEntries(String path) {
			return entries.get(path);
		}
		
		/**
		 * Return all the log entries at the given path
		 * @param path the file path
		 * @return the log entries for the file
		 */
		public ILogEntry[] getLogEntries(String path) {
			Map<String, ILogEntry> map = internalGetLogEntries(path);
			return map.values().toArray(new ILogEntry[map.size()]);
		}
		
		private ILogEntry internalGetLogEntry(String path, String revision) {
			Map fileEntries = internalGetLogEntries(path);
			if (fileEntries != null) {
				return (ILogEntry)fileEntries.get(revision);
			}
			return null;
		}
		
		public String[] getCachedFilePaths() {
			return entries.keySet().toArray(new String[entries.size()]);
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
					String revision = ((ICVSRemoteFile)resource).getRevision();
					return internalGetLogEntry(path, revision);
				} catch (TeamException e) {
					// Log and return null
					CVSUIPlugin.log(e);
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
			Map<String, ILogEntry> fileEntries = internalGetLogEntries(getFullPath(resource));
			if (fileEntries != null) {
				return fileEntries.values().toArray(new ILogEntry[fileEntries.size()]);
			}
			return new ILogEntry[0];
		}
		
		/*
		 * Return the full path that uniquely identifies the resource
		 * accross repositories. This path include the repository and
		 * resource path but does not include the revision so that 
		 * all log entries for a file can be retrieved.
		 */
		private String getFullPath(ICVSRemoteResource resource) {
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
		private ICVSRemoteFile findRevison(ILogEntry[] allLogs, String predecessorRevision) throws TeamException {
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
		private String getPredecessorRevision(String revision) {
			int digits[] = Util.convertToDigits(revision);
			digits[digits.length -1]--;
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
		private boolean isBrancheRevision(String revision) {
			return Util.convertToDigits(revision).length > 2;
		}
		
		/*
		 * Remove the trailing revision digits such that the
		 * returned revision is shorter than the given revision 
		 * and is an even number of digits long
		 */
		private String getBaseRevision(String revision) {
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
			Map<String, ILogEntry> fileEntries = internalGetLogEntries(fullPath);
			if (fileEntries == null) {
				fileEntries = new HashMap<>();
				entries.put(fullPath, fileEntries);
			}
			fileEntries.put(revision, entry);
		}
	}
	
	public RemoteLogOperation(IWorkbenchPart part, ICVSRemoteResource[] remoteResources, CVSTag tag1, CVSTag tag2, LogEntryCache cache) {
		super(part, remoteResources);
		this.tag1 = tag1;
		this.tag2 = tag2;
		this.entryCache = cache;
	}

	@Override
	protected void execute(ICVSRepositoryLocation location, ICVSRemoteResource[] remoteResources, IProgressMonitor monitor) throws CVSException {
		monitor.beginTask(NLS.bind(CVSUIMessages.RemoteLogOperation_0, new String[] { location.getHost() }), 100); 
		Session s = new Session(location, CVSWorkspaceRoot.getCVSFolderFor(ResourcesPlugin.getWorkspace().getRoot()), false /* do not output to console */);
		// Create a log listener that will update the cache as entries are received
		LogListener listener = new LogListener(entryCache);
		
		ICVSRemoteResource[] remotes = remoteResources;
		Command.LocalOption[] localOptions = getLocalOptions(tag1, tag2);
		if(tag1 == null || tag2 == null) {
			// Optimize the cases were we are only fetching the history for a single revision. If it is
			// already cached, don't fetch it again.
			ArrayList<ICVSRemoteResource> unCachedRemotes = new ArrayList<>();
			for (ICVSRemoteResource r : remoteResources) {
				if(entryCache.getLogEntry(r) == null) {
					unCachedRemotes.add(r);
				}
			}
			remotes = unCachedRemotes.toArray(new ICVSRemoteResource[unCachedRemotes.size()]);
		}
		if (remotes.length > 0) {
			try {
				s.open(Policy.subMonitorFor(monitor, 10));
				IStatus status = rlog.execute(s, Command.NO_GLOBAL_OPTIONS, localOptions, remotes, listener, Policy.subMonitorFor(monitor, 90));
				collectStatus(status);
			} finally {
				s.close();
			}
		}
	}

	@Override
	protected String getTaskName() {
		return CVSUIMessages.RemoteLogOperation_1; 
	}
	
	protected Command.LocalOption[] getLocalOptions(CVSTag tag1, CVSTag tag2) {
		if(tag1 != null && tag2 != null) {
			return new Command.LocalOption[] {RLog.NO_TAGS, RLog.ONLY_INCLUDE_CHANGES, RLog.makeTagOption(tag1, tag2)};
		} 
		else if (tag1 != null){
			if (tag1.getType() == CVSTag.HEAD ||
				tag1.getType() == CVSTag.VERSION)
				return new Command.LocalOption[] {RLog.NO_TAGS, RLog.ONLY_INCLUDE_CHANGES, RLog.getCurrentTag(tag1)};
			
			if (tag1.getType() == CVSTag.DATE)
				return new Command.LocalOption[] {RLog.NO_TAGS, RLog.ONLY_INCLUDE_CHANGES, RLog.REVISIONS_ON_DEFAULT_BRANCH, RLog.getCurrentTag(tag1)};
			//branch tag
			return new Command.LocalOption[] {RLog.getCurrentTag(tag1)};
		}
		else {
			return new Command.LocalOption[] {RLog.NO_TAGS, RLog.ONLY_INCLUDE_CHANGES};
		}
	}
}
