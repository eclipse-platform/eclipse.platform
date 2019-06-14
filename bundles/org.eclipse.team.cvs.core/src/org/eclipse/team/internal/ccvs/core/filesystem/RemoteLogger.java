/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.filesystem;

import java.util.HashMap;

import org.eclipse.core.runtime.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.LogEntry;
import org.eclipse.team.internal.ccvs.core.client.listeners.LogListener;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTree;

public class RemoteLogger {

	static final String DEAD_STATE = "dead"; //$NON-NLS-1$
	
	private ICVSRemoteFolder remoteFolder;
	private LogEntryCache cache;
	
	private RLogTreeBuilder treeBuilder;

	public RemoteLogger(ICVSRemoteFolder folder) {
		this.remoteFolder = folder;
	}

	private void getRemoteChildren(CVSTag tag, IProgressMonitor monitor) {
		Session session = new Session(this.remoteFolder.getRepository(), this.remoteFolder, false /* output to console */);
		try {
			// Build the local options
			this.cache = new LogEntryCache();
			LogListener listener = new LogListener(cache);

			Command.LocalOption[] localOptions = getLocalOptions(tag, null);
			try {
				session.open(Policy.subMonitorFor(monitor, 10));
				RLog rlog = new RLog();
				rlog.execute(session, Command.NO_GLOBAL_OPTIONS, localOptions, new ICVSRemoteResource[] {this.remoteFolder}, listener, Policy.subMonitorFor(monitor, 90));
			} catch (CVSException e) {
			}
		} finally {
			session.close();
		}
	}

	public ICVSResource[] fetchChildren(IProgressMonitor monitor) throws CVSException, TeamException {
		return fetchTree(monitor).getChildren();
	}

	public HashMap getFolderMap() {
		return treeBuilder.getFolderMap();
	}

	public RemoteFolderTree fetchTree(IProgressMonitor monitor) throws CVSException, TeamException {
		try{
		monitor.beginTask(null, 100);
		CVSTag tag = this.remoteFolder.getTag();
		if (tag == null)
			tag = CVSTag.DEFAULT;

		getRemoteChildren(tag, SubMonitor.convert(monitor,70));

		final ICVSRemoteFolder project = this.remoteFolder;
		//Get the entry paths
		String[] entry = this.cache.getCachedFilePaths();

		treeBuilder = new RLogTreeBuilder(project.getRepository(), tag, cache);
		for (String e : entry) {
			ILogEntry[] logEntry = this.cache.getLogEntries(e);
			//might not have state if this a branch entry
			if (logEntry[0].getState() != null && logEntry[0].getState().equals(DEAD_STATE))
				continue;

			ICVSRemoteFile remoteFile = logEntry[0].getRemoteFile();
			//if the current folder tag is a branch tag, we need to take the extra step
			//of making sure that the file's revision number has been set appropriately
			if (tag.getType() == CVSTag.BRANCH && remoteFile.getRevision().equals(LogListener.BRANCH_REVISION))
				verifyRevision(tag, logEntry[0], remoteFile);

			IPath logPath = new Path(null, remoteFile.getRepositoryRelativePath());
			if (logPath.segmentCount() > 0) {
				//trim everything up to the project segment
				String[] pathSegments = logPath.segments();
				int index;
				String projectName = project.getName();
				for (index = 0; index < pathSegments.length; index++) {
					if (pathSegments[index].equals(projectName))
						break;
				}
				logPath = logPath.removeFirstSegments(index + 1);
			}
			treeBuilder.newFile(logPath, remoteFile);
		}

		return treeBuilder.getTree();
		}
		finally{
			monitor.done();
		}
	}

	protected Command.LocalOption[] getLocalOptions(CVSTag tag1, CVSTag tag2) {
		if (tag1 != null && tag2 != null) {
			return new Command.LocalOption[] {RLog.NO_TAGS, RLog.ONLY_INCLUDE_CHANGES, RLog.makeTagOption(tag1, tag2)};
		} else if (tag1 != null) {
			if (tag1.getType() == CVSTag.HEAD || tag1.getType() == CVSTag.VERSION)
				return new Command.LocalOption[] {RLog.NO_TAGS, RLog.ONLY_INCLUDE_CHANGES, RLog.getCurrentTag(tag1)};

			if (tag1.getType() == CVSTag.DATE)
				return new Command.LocalOption[] {RLog.NO_TAGS, RLog.ONLY_INCLUDE_CHANGES, RLog.REVISIONS_ON_DEFAULT_BRANCH, RLog.getCurrentTag(tag1)};
			//branch tag
			return new Command.LocalOption[] {RLog.getCurrentTag(tag1)};
		} else {
			return new Command.LocalOption[] {RLog.NO_TAGS, RLog.ONLY_INCLUDE_CHANGES};
		}
	}

	private void verifyRevision(CVSTag tag, ILogEntry entry, ICVSRemoteFile remoteFile) throws CVSException {
		if (entry instanceof LogEntry) {
			LogEntry logEntry = (LogEntry) entry;
			String[] allBranchRevisions = logEntry.getBranchRevisions();
			CVSTag[] allCVSTags = entry.getTags();
			for (int i = 0; i < allCVSTags.length; i++) {
				if (allCVSTags[i].equals(tag)) {
					//get the revision number stored for this tag
					((RemoteFile) remoteFile).setRevision(allBranchRevisions[i]);
					break;
				}
			}
		}
	}

	public HashMap getLogMap() {
		return treeBuilder.getLogMap();
	}

}
