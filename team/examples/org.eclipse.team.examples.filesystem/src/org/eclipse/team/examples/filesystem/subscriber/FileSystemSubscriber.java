/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.subscriber;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ThreeWayRemoteTree;
import org.eclipse.team.core.variants.ThreeWaySubscriber;
import org.eclipse.team.core.variants.ThreeWaySynchronizer;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.filesystem.FileSystemProvider;

/**
 * This is an example file system subscriber that overrides
 * ThreeWaySubscriber. It uses a repository
 * provider (<code>FileSystemProvider</code>) to determine and
 * manage the roots and to create resource variants. It also makes
 * use of a file system specific remote tree (<code>FileSystemRemoteTree</code>)
 * for provided the remote tree access and refresh.
 *
 * @see ThreeWaySubscriber
 * @see ThreeWaySynchronizer
 * @see FileSystemProvider
 * @see FileSystemRemoteTree
 */
public class FileSystemSubscriber extends ThreeWaySubscriber {

	private static FileSystemSubscriber instance;

	/**
	 * Return the file system subscriber singleton.
	 * @return the file system subscriber singleton.
	 */
	public static synchronized FileSystemSubscriber getInstance() {
		if (instance == null) {
			instance = new FileSystemSubscriber();
		}
		return instance;
	}

	/**
	 * Create the file system subscriber.
	 */
	private FileSystemSubscriber() {
		super(new ThreeWaySynchronizer(new QualifiedName(FileSystemPlugin.ID, "workpsace-sync"))); //$NON-NLS-1$
	}

	@Override
	public IResourceVariant getResourceVariant(IResource resource, byte[] bytes) {
		RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), FileSystemPlugin.PROVIDER_ID);
		if (provider != null) {
			return ((FileSystemProvider)provider).getResourceVariant(resource, bytes);
		}
		return null;
	}

	@Override
	protected ThreeWayRemoteTree createRemoteTree() {
		return new FileSystemRemoteTree(this);
	}

	@Override
	public String getName() {
		return "File System Example"; //$NON-NLS-1$
	}

	@Override
	public IResource[] roots() {
		List<IProject> result = new ArrayList<>();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			if(project.isAccessible()) {
				RepositoryProvider provider = RepositoryProvider.getProvider(project, FileSystemPlugin.PROVIDER_ID);
				if(provider != null) {
					result.add(project);
				}
			}
		}
		return result.toArray(new IProject[result.size()]);
	}

	@Override
	public void handleRootChanged(IResource resource, boolean added) {
		// Override to allow FileSystemProvider to signal the addition and removal of roots
		super.handleRootChanged(resource, added);
	}

	@Override
	protected SyncInfo getSyncInfo(IResource local, IResourceVariant base, IResourceVariant remote) throws TeamException {
		// Override to use a custom sync info
		FileSystemSyncInfo info = new FileSystemSyncInfo(local, base, remote, this.getResourceComparator());
		info.init();
		return info;
	}

	/**
	 * Make the resource in-sync.
	 * @param resource the resource
	 * @throws TeamException
	 */
	public void makeInSync(IResource resource) throws TeamException {
		ThreeWaySynchronizer synchronizer = getSynchronizer();
		byte[] remoteBytes = synchronizer.getRemoteBytes(resource);
		if (remoteBytes == null) {
			if (!resource.exists())
				synchronizer.flush(resource, IResource.DEPTH_ZERO);
		} else {
			synchronizer.setBaseBytes(resource, remoteBytes);
		}
	}

	/**
	 * Make the change an outgoing change
	 * @param resource
	 * @throws TeamException
	 */
	public void markAsMerged(IResource resource, IProgressMonitor monitor) throws TeamException {
		makeInSync(resource);
		try {
			resource.touch(monitor);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

}
