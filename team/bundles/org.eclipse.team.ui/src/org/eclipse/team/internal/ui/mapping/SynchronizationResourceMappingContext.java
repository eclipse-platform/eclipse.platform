/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.ui.mapping.SynchronizationContentProvider;

/**
 * A remote resource mapping context that wraps a synchronization context.
 * This is used by the {@link SynchronizationContentProvider} to get the traversals
 * for resource mappings. Since it is used to provide content, it avoids long running
 * operations if possible.
 *
 * @since 3.2
 */
public final class SynchronizationResourceMappingContext extends
		RemoteResourceMappingContext {

	private final ISynchronizationContext context;

	/**
	 * Create a resource mapping context for the given synchronization context
	 * @param context the synchronization context
	 */
	public SynchronizationResourceMappingContext(ISynchronizationContext context) {
		this.context = context;
	}

	@Override
	public boolean isThreeWay() {
		return context.getType() == ISynchronizationContext.THREE_WAY;
	}

	@Override
	public boolean hasRemoteChange(IResource resource, IProgressMonitor monitor) throws CoreException {
		IDiff diff = context.getDiffTree().getDiff(resource);
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IDiff remote = twd.getRemoteChange();
			return remote != null && remote.getKind() != IDiff.NO_CHANGE;
		}
		return diff != null && diff.getKind() != IDiff.NO_CHANGE;
	}

	@Override
	public boolean hasLocalChange(IResource resource, IProgressMonitor monitor) throws CoreException {
		IDiff diff = context.getDiffTree().getDiff(resource);
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IDiff local = twd.getLocalChange();
			return local != null && local.getKind() != IDiff.NO_CHANGE;
		}
		return false;
	}

	@Override
	public IStorage fetchRemoteContents(IFile file, IProgressMonitor monitor) throws CoreException {
		IDiff diff = context.getDiffTree().getDiff(file);
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IDiff remote = twd.getRemoteChange();
			if (remote instanceof IResourceDiff) {
				IResourceDiff rd = (IResourceDiff) remote;
				return rd.getAfterState().getStorage(monitor);
			}
		} else if (diff instanceof IResourceDiff) {
			IResourceDiff rd = (IResourceDiff) diff;
			return rd.getAfterState().getStorage(monitor);
		}
		return file;
	}

	@Override
	public IStorage fetchBaseContents(IFile file, IProgressMonitor monitor) throws CoreException {
		IDiff diff = context.getDiffTree().getDiff(file);
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IDiff remote = twd.getRemoteChange();
			if (remote instanceof IResourceDiff) {
				IResourceDiff rd = (IResourceDiff) remote;
				return rd.getBeforeState().getStorage(monitor);
			}
			IDiff local = twd.getLocalChange();
			if (local instanceof IResourceDiff) {
				IResourceDiff rd = (IResourceDiff) local;
				return rd.getBeforeState().getStorage(monitor);
			}
		}
		return null;
	}

	@Override
	public IResource[] fetchMembers(IContainer container, IProgressMonitor monitor) throws CoreException {
		Set<IResource> result = new HashSet<>();
		IResource[] children = container.members();
		Collections.addAll(result, children);
		IPath[] childPaths = context.getDiffTree().getChildren(container.getFullPath());
		for (IPath path : childPaths) {
			IDiff delta = context.getDiffTree().getDiff(path);
			IResource child;
			if (delta == null) {
				// the path has descendent deltas so it must be a folder
				if (path.segmentCount() == 1) {
					child = ((IWorkspaceRoot)container).getProject(path.lastSegment());
				} else {
					child = container.getFolder(IPath.fromOSString(path.lastSegment()));
				}
			} else {
				child = context.getDiffTree().getResource(delta);
			}
			result.add(child);
		}
		return result.toArray(new IResource[result.size()]);
	}

	@Override
	public void refresh(ResourceTraversal[] traversals, int flags, IProgressMonitor monitor) throws CoreException {
		//context.refresh(traversals, flags, monitor);
	}

	public ISynchronizationContext getSynchronizationContext() {
		return context;
	}

	@Override
	public IProject[] getProjects() {
		Set<IProject> projects = new HashSet<>();
		IResource[] roots = context.getScope().getRoots();
		for (IResource resource : roots) {
			projects.add(resource.getProject());
		}
		return projects.toArray(new IProject[projects.size()]);
	}

}
