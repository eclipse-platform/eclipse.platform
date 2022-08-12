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
 *     Serge Beauchamp (Freescale Semiconductor) - [252996] add resource filtering
*******************************************************************************/
package org.eclipse.core.internal.localstore;

import java.net.URI;
import java.util.LinkedList;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

//
public class CopyVisitor implements IUnifiedTreeVisitor {

	/** root destination */
	protected IResource rootDestination;

	/** reports progress */
	protected SubMonitor monitor;

	/** update flags */
	protected int updateFlags;

	/** force flag */
	protected boolean force;

	/** deep copy flag */
	protected boolean isDeep;

	/** segments to drop from the source name */
	protected int segmentsToDrop;

	/** stores problems encountered while copying */
	protected MultiStatus status;

	/** visitor to refresh unsynchronized nodes */
	protected RefreshLocalVisitor refreshLocalVisitor;

	private FileSystemResourceManager localManager;

	public CopyVisitor(IResource rootSource, IResource destination, int updateFlags, IProgressMonitor monitor) {
		this.localManager = ((Resource) rootSource).getLocalManager();
		this.rootDestination = destination;
		this.updateFlags = updateFlags;
		this.isDeep = (updateFlags & IResource.SHALLOW) == 0;
		this.force = (updateFlags & IResource.FORCE) != 0;
		this.monitor = SubMonitor.convert(monitor);
		this.segmentsToDrop = rootSource.getFullPath().segmentCount();
		this.status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IStatus.INFO, Messages.localstore_copyProblem, null);
	}

	protected boolean copy(UnifiedTreeNode node) {
		Resource source = (Resource) node.getResource();
		IPath sufix = source.getFullPath().removeFirstSegments(segmentsToDrop);
		Resource destination = getDestinationResource(source, sufix);
		if (!copyProperties(source, destination))
			return false;
		return copyContents(node, source, destination);
	}

	protected boolean copyContents(UnifiedTreeNode node, Resource source, Resource destination) {
		try {
			if (source.isVirtual()) {
				((Folder) destination).create(IResource.VIRTUAL, true, null);
				return true;
			}
			if ((!isDeep || source.isUnderVirtual()) && source.isLinked()) {
				URI sourceLocationURI = getWorkspace().transferVariableDefinition(source, destination, source.getRawLocationURI());
				destination.createLink(sourceLocationURI, updateFlags & IResource.ALLOW_MISSING_LOCAL, null);
				return false;
			}
			// update filters in project descriptions
			if (source instanceof Container && ((Container) source).hasFilters()) {
				Project sourceProject = (Project) source.getProject();
				LinkedList<FilterDescription> originalDescriptions = sourceProject.internalGetDescription().getFilter(source.getProjectRelativePath());
				LinkedList<FilterDescription> filterDescriptions = FilterDescription.copy(originalDescriptions, destination);
				Project project = (Project) destination.getProject();
				project.internalGetDescription().setFilters(destination.getProjectRelativePath(), filterDescriptions);
				project.writeDescription(updateFlags);
			}

			IFileStore sourceStore = node.getStore();
			IFileStore destinationStore = destination.getStore();
			//ensure the parent of the root destination exists (bug 126104)
			if (destination == rootDestination)
				destinationStore.getParent().mkdir(EFS.NONE, monitor.newChild(0));
			sourceStore.copy(destinationStore, EFS.SHALLOW, monitor.newChild(0));
			//create the destination in the workspace
			ResourceInfo info = localManager.getWorkspace().createResource(destination, updateFlags);
			localManager.updateLocalSync(info, destinationStore.fetchInfo().getLastModified());
			//update timestamps on aliases
			getWorkspace().getAliasManager().updateAliases(destination, destinationStore, IResource.DEPTH_ZERO, monitor);
			if (destination.getType() == IResource.FILE)
				((File) destination).updateMetadataFiles();
		} catch (CoreException e) {
			status.add(e.getStatus());
		}
		return true;
	}

	protected boolean copyProperties(Resource target, Resource destination) {
		try {
			target.getPropertyManager().copy(target, destination, IResource.DEPTH_ZERO);
			return true;
		} catch (CoreException e) {
			status.add(e.getStatus());
			return false;
		}
	}

	protected Resource getDestinationResource(Resource source, IPath suffix) {
		if (suffix.segmentCount() == 0)
			return (Resource) rootDestination;
		IPath destinationPath = rootDestination.getFullPath().append(suffix);
		return getWorkspace().newResource(destinationPath, source.getType());
	}

	/**
	 * This is done in order to generate less garbage.
	 */
	protected RefreshLocalVisitor getRefreshLocalVisitor() {
		if (refreshLocalVisitor == null)
			refreshLocalVisitor = new RefreshLocalVisitor(SubMonitor.convert(null));
		return refreshLocalVisitor;
	}

	public IStatus getStatus() {
		return status;
	}

	protected Workspace getWorkspace() {
		return (Workspace) rootDestination.getWorkspace();
	}

	protected boolean isSynchronized(UnifiedTreeNode node) {
		/* virtual resources are always deemed as being synchronized */
		if (node.getResource().isVirtual())
			return true;
		if (node.isErrorInFileSystem())
			return true; // Assume synchronized unless proven otherwise
		/* does the resource exist in workspace and file system? */
		if (!node.existsInWorkspace() || !node.existsInFileSystem())
			return false;
		/* we don't care about folder last modified */
		if (node.isFolder() && node.getResource().getType() == IResource.FOLDER)
			return true;
		/* is lastModified different? */
		Resource target = (Resource) node.getResource();
		long lastModifed = target.getResourceInfo(false, false).getLocalSyncInfo();
		if (lastModifed != node.getLastModified())
			return false;
		return true;
	}

	protected void synchronize(UnifiedTreeNode node) throws CoreException {
		getRefreshLocalVisitor().visit(node);
	}

	@Override
	public boolean visit(UnifiedTreeNode node) throws CoreException {
		monitor.checkCanceled();
		int work = 1;
		try {
			//location can be null if based on an undefined variable
			if (node.getStore() == null) {
				//should still be a best effort copy
				IPath path = node.getResource().getFullPath();
				String message = NLS.bind(Messages.localstore_locationUndefined, path);
				status.add(new ResourceStatus(IResourceStatus.FAILED_READ_LOCAL, path, message, null));
				return false;
			}
			boolean wasSynchronized = isSynchronized(node);
			if (force && !wasSynchronized) {
				synchronize(node);
				// If not synchronized, the monitor did not take this resource into account.
				// So, do not report work on it.
				work = 0;
				//if source still doesn't exist, then fail because we can't copy a missing resource
				if (!node.existsInFileSystem()) {
					IPath path = node.getResource().getFullPath();
					String message = NLS.bind(Messages.resources_mustExist, path);
					status.add(new ResourceStatus(IResourceStatus.RESOURCE_NOT_FOUND, path, message, null));
					return false;
				}
			}
			if (!force && !wasSynchronized) {
				IPath path = node.getResource().getFullPath();
				String message = NLS.bind(Messages.localstore_resourceIsOutOfSync, path);
				status.add(new ResourceStatus(IResourceStatus.OUT_OF_SYNC_LOCAL, path, message, null));
				return true;
			}
			return copy(node);
		} finally {
			monitor.worked(work);
		}
	}

}
