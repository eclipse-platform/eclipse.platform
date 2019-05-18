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
package org.eclipse.team.tests.ccvs.core.mappings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.core.variants.IResourceVariant;

/**
 * A traversal context that traverses the local workspace but also
 * adds resources that exist in the given sync info set but do not exist
 * locally.
 */
public class SyncInfoSetTraveralContext extends RemoteResourceMappingContext {
	
	SyncInfoTree set;
	
	public SyncInfoSetTraveralContext(SyncInfoSet set) {
		this.set = new SyncInfoTree();
		this.set.addAll(set);
	}

	protected SyncInfo getSyncInfo(IFile file) {
		return set.getSyncInfo(file);
	}
	
	public boolean contentDiffers(IFile file, IProgressMonitor monitor) {
		return getSyncInfo(file) != null;
	}

	@Override
	public IStorage fetchRemoteContents(IFile file, IProgressMonitor monitor) throws CoreException {
		SyncInfo info = getSyncInfo(file);
		if (info == null)
			return null;
		IResourceVariant remote = info.getRemote();
		if (remote == null)
			return null;
		return remote.getStorage(monitor);
	}

	@Override
	public IResource[] fetchMembers(IContainer container, IProgressMonitor monitor) throws CoreException {
		Set<IResource> members = new HashSet<>();
		members.addAll(Arrays.asList(container.members(false)));
		members.addAll(Arrays.asList(set.members(container)));
		return members.toArray(new IResource[members.size()]);
	}

	@Override
	public void refresh(ResourceTraversal[] traversals, int flags, IProgressMonitor monitor) throws CoreException {
		// Do nothing
	}

	@Override
	public boolean isThreeWay() {
		for (Iterator<SyncInfo> iter = set.iterator(); iter.hasNext();) {
			SyncInfo info = iter.next();
			return info.getComparator().isThreeWay();
		}
		return true;
	}

	@Override
	public boolean hasRemoteChange(IResource resource, IProgressMonitor monitor) throws CoreException {
		SyncInfo info = set.getSyncInfo(resource);
		int direction = SyncInfo.getDirection(info.getKind());
		return direction == SyncInfo.INCOMING || direction == SyncInfo.CONFLICTING;
	}

	@Override
	public boolean hasLocalChange(IResource resource, IProgressMonitor monitor) throws CoreException {
		SyncInfo info = set.getSyncInfo(resource);
		int direction = SyncInfo.getDirection(info.getKind());
		return direction == SyncInfo.OUTGOING || direction == SyncInfo.CONFLICTING;

	}

	@Override
	public IStorage fetchBaseContents(IFile file, IProgressMonitor monitor) throws CoreException {
		SyncInfo info = getSyncInfo(file);
		if (info == null)
			return null;
		IResourceVariant base = info.getBase();
		if (base == null)
			return null;
		return base.getStorage(monitor);
	}

	@Override
	public IProject[] getProjects() {
		return ResourcesPlugin.getWorkspace().getRoot().getProjects();
	}

}
