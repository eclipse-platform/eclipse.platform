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
package org.eclipse.team.internal.ccvs.core.resources;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.Policy;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;

/**
 * This class can be used to fetch and cache file contents for remote files.
 */
public class FileContentCachingService {

	String[] fileDiffs;
	private CVSRepositoryLocation repository;
	private ICVSFolder remoteRoot;

	public static RemoteFolderTree buildRemoteTree(CVSRepositoryLocation repository, ICVSFolder root, CVSTag tag, IProgressMonitor monitor) throws CVSException {
		monitor.beginTask(null, 100);
		try {
			RemoteFolderTreeBuilder builder = new RemoteFolderTreeBuilder(repository, root, tag);
			RemoteFolderTree tree =  builder.buildTree(new ICVSResource[] { root }, Policy.subMonitorFor(monitor, 50));
			FileContentCachingService service = new FileContentCachingService(repository, tree, builder.getFileDiffs());
			service.cacheFileContents(Policy.subMonitorFor(monitor, 50));
			return tree;
		} finally {
			monitor.done();
		}
	}
	
	/**
	 * Fetch and cache the file contents for the specified files.
	 * @param root the root folder for the files being fetched
	 * @param filePaths the root relative file paths
	 * @param monitor
	 * @throws CVSException
	 */
	public static void fetchFileContents(RemoteFolderTree root, String[] filePaths, IProgressMonitor monitor) throws CVSException {
		FileContentCachingService service = new FileContentCachingService((CVSRepositoryLocation)root.getRepository(), root, filePaths);
		service.cacheFileContents(monitor);
	}
	
	public static RemoteFile buildRemoteTree(CVSRepositoryLocation repository, ICVSFile file, CVSTag tag, IProgressMonitor monitor) throws CVSException {
		monitor.beginTask(null, 100);
		try {
			RemoteFolderTreeBuilder builder = new RemoteFolderTreeBuilder(repository, file.getParent(), tag);
			RemoteFile remote =  builder.buildTree(file, monitor);
			if (builder.getFileDiffs().length > 0) {
				// Getting the storage of the file will cache the contents
				remote.getStorage(Policy.subMonitorFor(monitor, 50));
			}
			return remote;
		} catch (TeamException e) {
			throw CVSException.wrapException(e);
		} finally {
			monitor.done();
		}
	}
	
	public FileContentCachingService(CVSRepositoryLocation repository, RemoteFolderTree tree, String[] fileDiffs) {
		this.repository = repository;
		this.remoteRoot = tree;
		this.fileDiffs = fileDiffs;
	}
	
	private void cacheFileContents(IProgressMonitor monitor) throws CVSException {
		String[] files = getUncachedFiles();
		if (files.length == 0) return;
		// Fetch the file contents for all out-of-sync files by running an update
		// on the remote tree passing the known changed files as arguments
		monitor.beginTask(null, 10 + files.length * 100);
		Policy.checkCanceled(monitor);
		Session session = new Session(repository, remoteRoot, false);
		session.open(Policy.subMonitorFor(monitor, 10), false /* read-only */);
		try {
			Policy.checkCanceled(monitor);
			IStatus status = Command.UPDATE.execute(session,
				Command.NO_GLOBAL_OPTIONS,
				new LocalOption[] { Update.IGNORE_LOCAL_CHANGES },
				files,
				null,
				Policy.subMonitorFor(monitor, files.length * 100));
			if (!status.isOK()) {
				// No big deal but log the problem anyway
				CVSProviderPlugin.log (new CVSException(status));
			}
		} finally {
			session.close();
			monitor.done();
		}
	}

	/*
	 * Only return those file in the diff list that exist remotely and whose contents are not already cached
	 */
	private String[] getUncachedFiles() {
		if (fileDiffs.length == 0) return fileDiffs;
		List<String> existing = new ArrayList<>();
		for (String filePath : fileDiffs) {
			try {
				ICVSFile file = remoteRoot.getFile(filePath);
				if (file instanceof RemoteFile) {
					if (!((RemoteFile)file).isContentsCached()) {
						existing.add(filePath);
					}
				}
			} catch (CVSException e) {
				// The child does not exists so exclude it
			}
		}
		return existing.toArray(new String[existing.size()]);
	}
}
