/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.*;
import org.eclipse.core.internal.events.LifecycleEvent;
import org.eclipse.core.internal.localstore.IHistoryStore;
import org.eclipse.core.internal.utils.*;
import org.eclipse.core.internal.watson.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.osgi.util.NLS;

public abstract class Container extends Resource implements IContainer {
	protected Container(IPath path, Workspace container) {
		super(path, container);
	}

	/**
	 * Converts this resource and all its children into phantoms by modifying
	 * their resource infos in-place.
	 */
	@Override
	public void convertToPhantom() throws CoreException {
		if (isPhantom())
			return;
		super.convertToPhantom();
		IResource[] members = members(IContainer.INCLUDE_PHANTOMS | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS | IContainer.INCLUDE_HIDDEN);
		for (IResource member : members)
			((Resource) member).convertToPhantom();
	}

	@Override
	public IResourceFilterDescription createFilter(int type, FileInfoMatcherDescription matcherDescription, int updateFlags, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(getProject());
		monitor = Policy.monitorFor(monitor);
		FilterDescription filter = null;
		try {
			String message = NLS.bind(Messages.links_creating, getFullPath());
			monitor.beginTask(message, Policy.totalWork);
			Policy.checkCanceled(monitor);
			checkValidPath(path, FOLDER | PROJECT, true);
			final ISchedulingRule rule = workspace.getRuleFactory().createRule(this);
			try {
				workspace.prepareOperation(rule, monitor);
				workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_FILTER_ADD, this));
				workspace.beginOperation(true);
				monitor.worked(Policy.opWork * 5 / 100);
				//save the filter in the project description
				filter = new FilterDescription(this, type, matcherDescription);
				filter.setId(System.currentTimeMillis());

				Project project = (Project) getProject();
				project.internalGetDescription().addFilter(getProjectRelativePath(), filter);
				project.writeDescription(IResource.NONE);
				monitor.worked(Policy.opWork * 5 / 100);

				//refresh to discover any new resources below this folder
				if (getType() != IResource.FILE) {
					//refresh either in background or foreground
					if ((updateFlags & IResource.BACKGROUND_REFRESH) != 0) {
						workspace.refreshManager.refresh(this);
						monitor.worked(Policy.opWork * 90 / 100);
					} else {
						refreshLocal(DEPTH_INFINITE, Policy.subMonitorFor(monitor, Policy.opWork * 90 / 100));
					}
				} else
					monitor.worked(Policy.opWork * 90 / 100);
			} catch (OperationCanceledException e) {
				workspace.getWorkManager().operationCanceled();
				throw e;
			} finally {
				workspace.endOperation(rule, true);
			}
		} finally {
			monitor.done();
		}
		return filter;
	}

	@Override
	public boolean exists(IPath childPath) {
		return workspace.getResourceInfo(getFullPath().append(childPath), false, false) != null;
	}

	@Override
	public IResource findMember(String memberPath) {
		return findMember(memberPath, false);
	}

	@Override
	public IResource findMember(String memberPath, boolean phantom) {
		IPath childPath = getFullPath().append(memberPath);
		ResourceInfo info = workspace.getResourceInfo(childPath, phantom, false);
		return info == null ? null : workspace.newResource(childPath, info.getType());
	}

	@Override
	public IResource findMember(IPath childPath) {
		return findMember(childPath, false);
	}

	@Override
	public IResource findMember(IPath childPath, boolean phantom) {
		childPath = getFullPath().append(childPath);
		ResourceInfo info = workspace.getResourceInfo(childPath, phantom, false);
		return (info == null) ? null : workspace.newResource(childPath, info.getType());
	}

	@Override
	protected void fixupAfterMoveSource() throws CoreException {
		super.fixupAfterMoveSource();
		if (!synchronizing(getResourceInfo(true, false)))
			return;
		IResource[] members = members(IContainer.INCLUDE_PHANTOMS | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS | IContainer.INCLUDE_HIDDEN);
		for (IResource member : members)
			((Resource) member).fixupAfterMoveSource();
	}

	protected IResource[] getChildren(int memberFlags) {
		IPath[] children = null;
		try {
			children = workspace.tree.getChildren(path);
		} catch (IllegalArgumentException e) {
			//concurrency problem: the container has been deleted by another
			//thread during this call.  Just return empty children set
		}
		if (children == null || children.length == 0)
			return ICoreConstants.EMPTY_RESOURCE_ARRAY;
		Resource[] result = new Resource[children.length];
		int found = 0;
		for (IPath child : children) {
			ResourceInfo info = workspace.getResourceInfo(child, true, false);
			if (info != null && isMember(info.getFlags(), memberFlags))
				result[found++] = workspace.newResource(child, info.getType());
		}
		if (found == result.length)
			return result;
		Resource[] trimmedResult = new Resource[found];
		System.arraycopy(result, 0, trimmedResult, 0, found);
		return trimmedResult;
	}

	public IFile getFile(String name) {
		return (IFile) workspace.newResource(getFullPath().append(name), FILE);
	}

	@Override
	public IResourceFilterDescription[] getFilters() throws CoreException {
		IResourceFilterDescription[] results = null;
		checkValidPath(path, FOLDER | PROJECT, true);
		Project project = (Project) getProject();
		ProjectDescription desc = project.internalGetDescription();
		if (desc != null) {
			LinkedList<FilterDescription> list = desc.getFilter(getProjectRelativePath());
			if (list != null) {
				results = new IResourceFilterDescription[list.size()];
				for (int i = 0; i < list.size(); i++) {
					results[i] = list.get(i);
				}
				return results;
			}
		}
		return new IResourceFilterDescription[0];
	}

	public boolean hasFilters() {
		IProject project = getProject();
		if (project == null)
			return false;
		ProjectDescription desc = ((Project) project).internalGetDescription();
		if (desc == null)
			return false;
		LinkedList<FilterDescription> filters = desc.getFilter(getProjectRelativePath());
		if ((filters != null) && (filters.size() > 0))
			return true;
		return false;
	}

	@Override
	public IFile getFile(IPath childPath) {
		return (IFile) workspace.newResource(getFullPath().append(childPath), FILE);
	}

	public IFolder getFolder(String name) {
		return (IFolder) workspace.newResource(getFullPath().append(name), FOLDER);
	}

	@Override
	public IFolder getFolder(IPath childPath) {
		return (IFolder) workspace.newResource(getFullPath().append(childPath), FOLDER);
	}

	@Deprecated
	@Override
	public boolean isLocal(int flags, int depth) {
		if (!super.isLocal(flags, depth))
			return false;
		if (depth == DEPTH_ZERO)
			return true;
		if (depth == DEPTH_ONE)
			depth = DEPTH_ZERO;
		// get the children via the workspace since we know that this
		// resource exists (it is local).
		IResource[] children = getChildren(IResource.NONE);
		for (IResource c : children) {
			if (!c.isLocal(depth)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public IResource[] members() throws CoreException {
		// forward to central method
		return members(IResource.NONE);
	}

	@Override
	public IResource[] members(boolean phantom) throws CoreException {
		// forward to central method
		return members(phantom ? INCLUDE_PHANTOMS : IResource.NONE);
	}

	@Override
	public IResource[] members(int memberFlags) throws CoreException {
		final boolean phantom = (memberFlags & INCLUDE_PHANTOMS) != 0;
		ResourceInfo info = getResourceInfo(phantom, false);
		checkAccessible(getFlags(info));
		//if children are currently unknown, ask for immediate refresh
		if (info.isSet(ICoreConstants.M_CHILDREN_UNKNOWN))
			workspace.refreshManager.refresh(this);
		return getChildren(memberFlags);
	}

	public void removeFilter(IResourceFilterDescription filterDescription, int updateFlags, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		try {
			String message = NLS.bind(Messages.links_creating, getFullPath());
			monitor.beginTask(message, Policy.totalWork);
			Policy.checkCanceled(monitor);
			checkValidPath(path, FOLDER | PROJECT, true);
			final ISchedulingRule rule = workspace.getRuleFactory().createRule(this);
			try {
				workspace.prepareOperation(rule, monitor);
				workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_FILTER_REMOVE, this));
				workspace.beginOperation(true);
				monitor.worked(Policy.opWork * 5 / 100);
				//save the filter in the project description
				Project project = (Project) getProject();
				project.internalGetDescription().removeFilter(getProjectRelativePath(), (FilterDescription) filterDescription);
				project.writeDescription(IResource.NONE);
				monitor.worked(Policy.opWork * 5 / 100);

				//refresh to discover any new resources below this linked location
				if (getType() != IResource.FILE) {
					//refresh either in background or foreground
					if ((updateFlags & IResource.BACKGROUND_REFRESH) != 0) {
						workspace.refreshManager.refresh(this);
						monitor.worked(Policy.opWork * 90 / 100);
					} else {
						refreshLocal(DEPTH_INFINITE, Policy.subMonitorFor(monitor, Policy.opWork * 90 / 100));
					}
				} else
					monitor.worked(Policy.opWork * 90 / 100);
			} catch (OperationCanceledException e) {
				workspace.getWorkManager().operationCanceled();
				throw e;
			} finally {
				workspace.endOperation(rule, true);
			}
		} finally {
			monitor.done();
		}
	}

	@Override
	public String getDefaultCharset() throws CoreException {
		return getDefaultCharset(true);
	}

	@Override
	public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor) {
		IHistoryStore historyStore = getLocalManager().getHistoryStore();
		IPath basePath = getFullPath();
		IWorkspaceRoot root = getWorkspace().getRoot();
		Set<IFile> deletedFiles = new HashSet<>();

		if (depth == IResource.DEPTH_ZERO) {
			// this folder might have been a file in a past life
			if (historyStore.getStates(basePath, monitor).length > 0) {
				IFile file = root.getFile(basePath);
				if (!file.exists()) {
					deletedFiles.add(file);
				}
			}
		} else {
			// convert IPaths to IFiles keeping only files that no longer exist
			for (IPath filePath : historyStore.allFiles(basePath, depth, monitor)) {
				IFile file = root.getFile(filePath);
				if (!file.exists()) {
					deletedFiles.add(file);
				}
			}
		}
		return deletedFiles.toArray(new IFile[deletedFiles.size()]);
	}

	@Deprecated
	@Override
	public void setDefaultCharset(String charset) throws CoreException {
		ResourceInfo info = getResourceInfo(false, false);
		checkAccessible(getFlags(info));
		workspace.getCharsetManager().setCharsetFor(getFullPath(), charset);
	}

	@Override
	public void setDefaultCharset(String newCharset, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		try {
			String message = NLS.bind(Messages.resources_settingDefaultCharsetContainer, getFullPath());
			monitor.beginTask(message, Policy.totalWork);
			// need to get the project as a scheduling rule because we might be
			// creating a new folder/file to hold the project settings
			final ISchedulingRule rule = workspace.getRuleFactory().charsetRule(this);
			try {
				workspace.prepareOperation(rule, monitor);
				checkAccessible(getFlags(getResourceInfo(false, false)));
				workspace.beginOperation(true);
				workspace.getCharsetManager().setCharsetFor(getFullPath(), newCharset);
				// now propagate the changes to all children inheriting their setting from this container
				IElementContentVisitor visitor = new IElementContentVisitor() {
					boolean visitedRoot = false;

					@Override
					public boolean visitElement(ElementTree tree, IPathRequestor requestor, Object elementContents) {
						if (elementContents == null)
							return false;
						IPath nodePath = requestor.requestPath();
						// we will always generate an event at least for the root of the sub tree
						// (skip visiting the root because we already have set the charset above and
						// that is the condition we are checking later)
						if (!visitedRoot) {
							visitedRoot = true;
							ResourceInfo info = workspace.getResourceInfo(nodePath, false, true);
							if (info == null)
								return false;
							info.incrementCharsetGenerationCount();
							return true;
						}
						// does it already have an encoding explicitly set?
						if (workspace.getCharsetManager().getCharsetFor(nodePath, false) != null)
							return false;
						ResourceInfo info = workspace.getResourceInfo(nodePath, false, true);
						if (info == null)
							return false;
						info.incrementCharsetGenerationCount();
						return true;
					}
				};
				try {
					new ElementTreeIterator(workspace.getElementTree(), getFullPath()).iterate(visitor);
				} catch (WrappedRuntimeException e) {
					throw (CoreException) e.getTargetException();
				}
				monitor.worked(Policy.opWork);
			} catch (OperationCanceledException e) {
				workspace.getWorkManager().operationCanceled();
				throw e;
			} finally {
				workspace.endOperation(rule, true);
			}
		} finally {
			monitor.done();
		}
	}
}
