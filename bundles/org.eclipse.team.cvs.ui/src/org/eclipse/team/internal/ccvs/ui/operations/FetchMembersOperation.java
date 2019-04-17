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
package org.eclipse.team.internal.ccvs.ui.operations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolder;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderMemberFetcher;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.progress.IElementCollector;

/**
 * Fetch the members of a remote folder in the background, passing incremental
 * results through an IElementCollector.
 */
public class FetchMembersOperation extends RemoteOperation {

	/* internal uzse only */ IElementCollector collector;
	/* internal uzse only */ RemoteFolderFilter filter = new RemoteFolderFilter();
	
	public static class RemoteFolderFilter {
		public ICVSRemoteResource[] filter(ICVSRemoteResource[] resource) {
			return resource;
		}
	}
	
	public class InternalRemoteFolderMemberFetcher extends RemoteFolderMemberFetcher {
		long sendIncrement = 100;
		List<RemoteFolder> unsent = new ArrayList<>();
		long intervalStart;
		protected InternalRemoteFolderMemberFetcher(RemoteFolder parentFolder, CVSTag tag) {
			super(parentFolder, tag);
		}
		@Override
		protected void parentDoesNotExist() {
			super.parentDoesNotExist();
			// Indicate that there are no children
			collector.add(new Object[0], getProgressMonitor());
		}
		@Override
		protected RemoteFolder recordFolder(String name) {
			RemoteFolder folder = super.recordFolder(name);
			unsent.add(folder);
			if (isTimeToSend()) {
				sendFolders();
			}
			return folder;
		}
		private boolean isTimeToSend() {
			long currentTime = System.currentTimeMillis();
			return ((currentTime - intervalStart) >  sendIncrement) || unsent.size() > sendIncrement;
		}
		@Override
		protected IStatus performUpdate(IProgressMonitor progress, CVSTag tag) throws CVSException {
			intervalStart = System.currentTimeMillis();
			IStatus status = super.performUpdate(progress, tag);
			sendFolders();
			return status;
		}
		@Override
		protected void updateFileRevisions(ICVSFile[] files, IProgressMonitor monitor) throws CVSException {
			super.updateFileRevisions(files, monitor);
			sendFiles();
		}
		private void sendFolders() {
			updateParentFolderChildren();
			collector.add(filter.filter(unsent.toArray(new ICVSRemoteFolder[unsent.size()])), getProgressMonitor());
			unsent.clear();
			intervalStart = System.currentTimeMillis();
		}
		private void sendFiles() {
			collector.add(getFiles(), getProgressMonitor());
			unsent.clear();
		}
		private IProgressMonitor getProgressMonitor() {
			return null;
		}
	}

	public FetchMembersOperation(IWorkbenchPart part, ICVSRemoteFolder folder, IElementCollector collector) {
		super(part, new ICVSRemoteResource[] { folder });
		this.collector = collector;
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		ICVSRemoteFolder remote = getRemoteFolder();
		if (remote.getClass().equals(RemoteFolder.class)) {
			monitor = Policy.monitorFor(monitor);
			boolean isRoot = remote.getName().equals(ICVSRemoteFolder.REPOSITORY_ROOT_FOLDER_NAME);
			monitor.beginTask(null, 100 + (isRoot ? 30 : 0));
			RemoteFolderMemberFetcher fetcher = new InternalRemoteFolderMemberFetcher((RemoteFolder)remote, remote.getTag());
			fetcher.fetchMembers(Policy.subMonitorFor(monitor, 100));
			if (isRoot) {
				ICVSRemoteResource[] modules = CVSUIPlugin.getPlugin()
					.getRepositoryManager()
					.getRepositoryRootFor(remote.getRepository())
					.getDefinedModules(remote.getTag(), Policy.subMonitorFor(monitor, 25));
				collector.add(filter.filter(modules), Policy.subMonitorFor(monitor, 5));
			}
		} else {
			monitor = Policy.monitorFor(monitor);
			try {
				monitor.beginTask(null, 100);
				ICVSRemoteResource[] children = remote.members(Policy.subMonitorFor(monitor, 95));
				collector.add(children, Policy.subMonitorFor(monitor, 5));
			} catch (TeamException e) {
				throw CVSException.wrapException(e);
			} finally {
				monitor.done();
			}
		}

	}

	@Override
	protected String getTaskName() {
		return NLS.bind(CVSUIMessages.FetchMembersOperation_0, new String[] { getRemoteFolder().getName() }); 
	}

	private ICVSRemoteFolder getRemoteFolder() {
		return (ICVSRemoteFolder)getRemoteResources()[0];
	}

	public void setFilter(RemoteFolderFilter filter) {
		this.filter = filter;
	}

}
