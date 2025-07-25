/*******************************************************************************
 * Copyright (c) 2006, 2023 IBM Corporation and others.
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
 *     Red Hat Inc - Adapted from classes in org.eclipse.ui.ide.undo and org.eclipse.ui.internal.ide.undo
 *******************************************************************************/

package org.eclipse.core.internal.resources.undo.snapshot;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.undo.snapshot.IContainerSnapshot;
import org.eclipse.core.resources.undo.snapshot.IResourceSnapshot;
import org.eclipse.core.resources.undo.snapshot.ResourceSnapshotFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

/**
 * ContainerDescription is a lightweight description that describes a container
 * to be created.
 *
 * This class is not intended to be instantiated or used by clients.
 *
 * @since 3.20
 */
public abstract class ContainerSnapshot<T extends IContainer> extends AbstractResourceSnapshot<T> implements IContainerSnapshot<T> {

	String name;

	URI location;

	IResourceFilterDescription[] filters;

	String defaultCharSet;

	final List<IResourceSnapshot<? extends IResource>> members = new ArrayList<>();

	/**
	 * Create a container snapshot from the specified container handle that can be
	 * used to create the container. The returned ContainerSnapshot should represent
	 * any non-existing parents in addition to the specified container.
	 *
	 * @param container the handle of the container to be described
	 * @return a container snapshot describing the container and any non-existing
	 *         parents.
	 */

	public static <R extends IContainer> ContainerSnapshot<? extends R> fromContainer(R container) {
		return fromContainer(container, false);
	}

	/**
	 * Create a group container snapshot from the specified container handle that
	 * can be used to create the container. The returned ContainerSnapshot should
	 * represent any non-existing parents in addition to the specified container.
	 *
	 * @param container the handle of the container to be described
	 * @return a container description snapshot the container and any non-existing
	 *         parents.
	 */

	public static <R extends IContainer> ContainerSnapshot<? extends R> fromVirtualFolderContainer(R container) {
		return fromContainer(container, true);
	}

	@SuppressWarnings("unchecked")
	public static <R extends IContainer> ContainerSnapshot<? extends R> fromContainer(R container, boolean usingVirtualFolder) {
		IPath fullPath = container.getFullPath();
		ContainerSnapshot<? extends IContainer> firstCreatedParent = null;
		ContainerSnapshot<? extends IContainer> currentContainerDescription = null;

		// Does the container exist already? If so, then the parent exists and
		// we use the normal creation constructor.
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IContainer currentContainer = (IContainer) root.findMember(fullPath);
		if (currentContainer != null) {
			return (ContainerSnapshot<R>) ResourceSnapshotFactory.fromResource(container);
		}

		// Create container descriptions for any uncreated parents in the given
		// path.
		currentContainer = root;
		for (int i = 0; i < fullPath.segmentCount(); i++) {
			String currentSegment = fullPath.segment(i);
			IResource resource = currentContainer.findMember(currentSegment);
			if (resource != null) {
				// parent already exists, no need to create a description for it
				currentContainer = (IContainer) resource;
			} else if (i == 0) {
				// parent does not exist and it is a project
				firstCreatedParent = new ProjectSnapshot(root.getProject(currentSegment));
				currentContainerDescription = firstCreatedParent;
			} else {
				IFolder folderHandle = currentContainer.getFolder(IPath.fromOSString(currentSegment));
				FolderSnapshot currentFolder;
				currentFolder = new FolderSnapshot(folderHandle, usingVirtualFolder);
				currentContainer = folderHandle;
				if (currentContainerDescription != null) {
					currentContainerDescription.addMember(currentFolder);
				}
				currentContainerDescription = currentFolder;
				if (firstCreatedParent == null) {
					firstCreatedParent = currentFolder;
				}
			}
		}
		return (ContainerSnapshot<? extends R>)firstCreatedParent;
	}

	/**
	 * Create a ContainerDescription with no state.
	 */
	public ContainerSnapshot() {

	}

	/**
	 * Create a ContainerSnapshot from the specified container handle. Typically
	 * used when the container handle represents a resource that actually exists,
	 * although it will not fail if the resource is non-existent.
	 *
	 * @param container the container to be described
	 */
	public ContainerSnapshot(T container) {
		super(container);
		this.name = container.getName();
		if (container.isLinked()) {
			this.location = container.getLocationURI();
		}
		try {
			if (container.isAccessible()) {
				defaultCharSet = container.getDefaultCharset(false);
				IResource[] resourceMembers = container.members();
				for (IResource resourceMember : resourceMembers) {
					members.add(ResourceSnapshotFactory.fromResource(resourceMember));
				}
			}
		} catch (CoreException e) {
			// Eat this exception because it only occurs when the resource
			// does not exist and we have already checked this.
			// We do not want to throw exceptions on the simple constructor, as
			// no one has actually tried to do anything yet.
		}
	}

	/**
	 * Create any child resources known by this container snapshot.
	 *
	 * @param parentHandle the handle of the created parent
	 * @param monitor      the progress monitor to be used
	 */
	protected final void createChildResources(IContainer parentHandle,
			IProgressMonitor monitor) throws CoreException {
		// restore any children
		SubMonitor subMonitor = SubMonitor.convert(monitor, members.size());
		for (IResourceSnapshot<? extends IResource> member : members) {
			if (member instanceof AbstractResourceSnapshot) {
				((AbstractResourceSnapshot<? extends IResource>) member).parent = parentHandle;
			}
			member.createResource(subMonitor.split(1));
		}
	}

	@Override
	public void recordStateFromHistory(IProgressMonitor mon) throws CoreException {
		if (members != null) {
			SubMonitor subMonitor = SubMonitor.convert(mon, ResourceSnapshotMessages.FolderDescription_SavingUndoInfoProgress,
					members.size());
			for (IResourceSnapshot<? extends IResource> member : members) {
				SubMonitor iterationMonitor = subMonitor.split(1);
				if (member instanceof FileSnapshot fileSnapshot) {
					fileSnapshot.recordStateFromHistory(iterationMonitor);
				} else if (member instanceof FolderSnapshot folderSnapshot) {
					folderSnapshot.recordStateFromHistory(iterationMonitor);
				}
			}
		}
	}

	/**
	 * Return the name of the container described by this ContainerSnapshot.
	 *
	 * @return the name of the container.
	 */
	@Override
	public String getName() {
		return name;
	}

	@SuppressWarnings("unchecked")
	@Override
	public IResourceSnapshot<? extends IResource>[] getMembers() {
		return members.toArray((IResourceSnapshot<? extends IResource>[]) Array.newInstance(IResourceSnapshot.class, members.size()));
	}

	/**
	 * Add the specified resource snapshot as a member of this resource description
	 *
	 * @param member the resource snapshot considered a member of this container.
	 */
	@Override
	public void addMember(IResourceSnapshot<? extends IResource> member) {
		members.add(member);
	}

	@Override
	protected void restoreResourceAttributes(IResource resource)
			throws CoreException {
		super.restoreResourceAttributes(resource);
		Assert.isLegal(resource instanceof IContainer);
		IContainer container = (IContainer) resource;
		if (defaultCharSet != null) {
			container.setDefaultCharset(defaultCharSet, null);
		}
	}

	/**
	 * Set the location to which this container is linked.
	 *
	 * @param location the location URI, or <code>null</code> if there is no link
	 */
	@Override
	public void setLocation(URI location) {
		this.location = location;
	}

	/**
	 * Set the filters to which should be created on this container.
	 *
	 * @param filters the filters
	 */
	@Override
	public void setFilters(IResourceFilterDescription[] filters) {
		this.filters = filters;
	}

	@Override
	public boolean verifyExistence(boolean checkMembers) {
		boolean existence = super.verifyExistence(checkMembers);
		if (existence) {
			if (checkMembers) {
				// restore any children
				for (IResourceSnapshot<? extends IResource> member : members) {
					if (!member.verifyExistence(checkMembers)) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}
}
