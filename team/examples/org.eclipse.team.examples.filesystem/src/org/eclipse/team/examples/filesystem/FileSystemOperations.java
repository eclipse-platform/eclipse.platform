/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 * 	   Andreas Voss <av@tonbeller.com> - Bug 181141 [Examples] Team: filesystem provider example can not handle deletions
 *******************************************************************************/
package org.eclipse.team.examples.filesystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.core.variants.ThreeWaySynchronizer;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemResourceVariant;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemSubscriber;

/**
 * The get and put operations for the file system provider.
 */
public class FileSystemOperations {

	// A reference to the provider
	private FileSystemProvider provider;

	FileSystemOperations(FileSystemProvider provider) {
		this.provider = provider;
	}

	/**
	 * Make the local state of the project match the remote state by getting any out-of-sync
	 * resources. The overrideOutgoing flag is used to indicate whether locally modified
	 * files should also be replaced or left alone.
	 * @param resources the resources to get
	 * @param depth the depth of the operation
	 * @param overrideOutgoing whether locally modified resources should be replaced
	 * @param progress a progress monitor
	 * @throws TeamException
	 */
	public void get(IResource[] resources, int depth, boolean overrideOutgoing, IProgressMonitor progress) throws TeamException {
		try {
			// ensure the progress monitor is not null
			progress = Policy.monitorFor(progress);
			progress.beginTask(Policy.bind("GetAction.working"), 100); //$NON-NLS-1$
			// Refresh the subscriber so we have the latest remote state
			FileSystemSubscriber.getInstance().refresh(resources, depth, SubMonitor.convert(progress, 30));
			internalGet(resources, depth, overrideOutgoing, SubMonitor.convert(progress, 70));
		} finally {
			progress.done();
		}
	}

	/**
	 * Make the local state of the traversals match the remote state by getting any out-of-sync
	 * resources. The overrideOutgoing flag is used to indicate whether locally modified
	 * files should also be replaced or left alone.
	 * @param traversals the traversals that cover the resources to get
	 * @param overrideOutgoing whether locally modified resources should be replaced
	 * @param progress a progress monitor
	 * @throws TeamException
	 */
	public void get(ResourceTraversal[] traversals, boolean overrideOutgoing, IProgressMonitor monitor) throws TeamException {
		try {
			// ensure the progress monitor is not null
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(null, 100* traversals.length);
			for (ResourceTraversal traversal : traversals) {
				get(traversal.getResources(), traversal.getDepth(), overrideOutgoing, SubMonitor.convert(monitor, 100));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Checkout the given resources to the given depth by setting any files
	 * to writable (i.e set read-only to <code>false</code>.
	 * @param resources the resources to be checked out
	 * @param depth the depth of the checkout
	 * @param progress a progress monitor
	 * @throws TeamException
	 */
	public void checkout(IResource[] resources, int depth, IProgressMonitor progress) throws TeamException {
		try {
			progress = Policy.monitorFor(progress);
			progress.beginTask(Policy.bind("FileSystemSimpleAccessOperations.1"), resources.length); //$NON-NLS-1$
			for (IResource resource2 : resources) {
				Policy.checkCanceled(progress);
				resource2.accept((IResourceVisitor) resource -> {
					if (resource.getType() == IResource.FILE) {
						//TODO: lock the file on the 'server'.
						resource.setReadOnly(false);
					}
					return true;
				}, depth, false /* include phantoms */);
				progress.worked(1);
			}
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		} finally {
			progress.done();
		}
	}

	/**
	 * Check-in the given resources to the given depth by replacing the remote (i.e. file system)
	 * contents with the local workspace contents.
	 * @param resources the resources
	 * @param depth the depth of the operation
	 * @param overrideIncoming indicate whether incoming remote changes should be replaced
	 * @param progress a progress monitor
	 * @throws TeamException
	 */
	public void checkin(IResource[] resources, int depth, boolean overrideIncoming, IProgressMonitor progress) throws TeamException {
		try {
			// ensure the progress monitor is not null
			progress = Policy.monitorFor(progress);
			progress.beginTask(Policy.bind("PutAction.working"), 100); //$NON-NLS-1$
			// Refresh the subscriber so we have the latest remote state
			FileSystemSubscriber.getInstance().refresh(resources, depth, SubMonitor.convert(progress, 30));
			internalPut(resources, depth, overrideIncoming, SubMonitor.convert(progress, 70));
		} finally {
			progress.done();
		}
	}

	/**
	 * Check-in the given resources to the given depth by replacing the remote (i.e. file system)
	 * contents with the local workspace contents.
	 * @param traversals the traversals that cover the resources to check in
	 * @param overrideIncoming indicate whether incoming remote changes should be replaced
	 * @param progress a progress monitor
	 * @throws TeamException
	 */
	public void checkin(ResourceTraversal[] traversals, boolean overrideIncoming, IProgressMonitor monitor) throws TeamException {
		try {
			// ensure the progress monitor is not null
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(null, 100* traversals.length);
			for (ResourceTraversal traversal : traversals) {
				checkin(traversal.getResources(), traversal.getDepth(), overrideIncoming, SubMonitor.convert(monitor, 100));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Return whether the local resource is checked out. A resource
	 * is checked out if it is a file that is not read-only. Folders
	 * are always checked out.
	 * @param resource the resource
	 * @return whether the resource is checked out and can be modified
	 */
	public boolean isCheckedOut(IResource resource) {
		if (resource.getType() != IResource.FILE) return true;
		return !resource.isReadOnly();
	}

	/*
	 * Get the resource variant for the given resource.
	 */
	private FileSystemResourceVariant getResourceVariant(IResource resource) {
		return (FileSystemResourceVariant)provider.getResourceVariant(resource);
	}

	private void internalGet(IResource[] resources, int depth, boolean overrideOutgoing, IProgressMonitor progress) throws TeamException {
		// Traverse the resources and get any that are out-of-sync
		progress.beginTask(Policy.bind("GetAction.working"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
		for (IResource resource : resources) {
			Policy.checkCanceled(progress);
			if (resource.getType() == IResource.FILE) {
				internalGet((IFile) resource, overrideOutgoing, progress);
			} else if (depth != IResource.DEPTH_ZERO) {
				internalGet((IContainer)resource, depth, overrideOutgoing, progress);
			}
			progress.worked(1);
		}
	}

	/*
	 * Get the folder and its children to the depth specified.
	 */
	private void internalGet(IContainer container, int depth, boolean overrideOutgoing, IProgressMonitor progress) throws TeamException {
		try {
			ThreeWaySynchronizer synchronizer = FileSystemSubscriber.getInstance().getSynchronizer();
			// Make the local folder state match the remote folder state
			List<IFolder> toDelete = new ArrayList<>();
			if (container.getType() == IResource.FOLDER) {
				IFolder folder = (IFolder)container;
				FileSystemResourceVariant remote = getResourceVariant(container);
				if (!folder.exists() && remote != null) {
					// Create the local folder
					folder.create(false, true, progress);
					synchronizer.setBaseBytes(folder, remote.asBytes());
				} else if (folder.exists() && remote == null) {
					// Schedule the folder for removal but delay in
					// case the folder contains outgoing changes
					toDelete.add(folder);
				}
			}

			// Get the children
			IResource[] children = synchronizer.members(container);
			if (children.length > 0) {
				internalGet(children, depth == IResource.DEPTH_INFINITE ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO, overrideOutgoing, progress);
			}

			// Remove any empty folders
			for (IFolder folder : toDelete) {
				if (folder.members().length == 0) {
					folder.delete(false, true, progress);
					synchronizer.flush(folder, IResource.DEPTH_INFINITE);
				}
			}
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

	/*
	 * Get the file if it is out-of-sync.
	 */
	private void internalGet(IFile localFile, boolean overrideOutgoing, IProgressMonitor progress) throws TeamException {
		ThreeWaySynchronizer synchronizer = FileSystemSubscriber.getInstance().getSynchronizer();
		IResourceVariantComparator comparator = FileSystemSubscriber.getInstance().getResourceComparator();
		FileSystemResourceVariant remote = getResourceVariant(localFile);
		byte[] baseBytes = synchronizer.getBaseBytes(localFile);
		IResourceVariant base = provider.getResourceVariant(localFile, baseBytes);
		if (!synchronizer.hasSyncBytes(localFile)
				|| (isLocallyModified(localFile) && !overrideOutgoing)) {
			// Do not overwrite the local modification
			return;
		}
		if (base != null && remote == null) {
			// The remote no longer exists so remove the local
			try {
				localFile.delete(false, true, progress);
				synchronizer.flush(localFile, IResource.DEPTH_ZERO);
				return;
			} catch (CoreException e) {
				throw TeamException.asTeamException(e);
			}
		}
		if (!synchronizer.isLocallyModified(localFile)
				&& base != null
				&& remote != null
				&& comparator.compare(base, remote)) {
			// The base and remote are the same and there's no local changes
			// so nothing needs to be done
			return;
		}
		// Copy from the local file to the remote file:
		// Get the remote file content.
		try (InputStream source = remote.getContents()) {
			// Set the local file content to be the same as the remote file.
			if (localFile.exists())
				localFile.setContents(source, false, false, progress);
			else
				localFile.create(source, false, progress);

			// Mark as read-only to force a checkout before editing
			localFile.setReadOnly(true);
			synchronizer.setBaseBytes(localFile, remote.asBytes());
		} catch (IOException e) {
			throw FileSystemPlugin.wrapException(e);
		} catch (CoreException e) {
			throw FileSystemPlugin.wrapException(e);
		}
	}

	private void internalPut(IResource[] resources, int depth, boolean overrideIncoming, IProgressMonitor progress) throws TeamException {
		// ensure the progress monitor is not null
		progress = Policy.monitorFor(progress);
		progress.beginTask(Policy.bind("PutAction.working"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
		for (IResource resource : resources) {
			Policy.checkCanceled(progress);
			if (resource.getType() == IResource.FILE) {
				internalPut((IFile)resource, overrideIncoming, progress);
			} else if (depth > 0) { //Assume that resources are either files or containers.
				internalPut((IContainer)resource, depth, overrideIncoming, progress);
			}
			progress.worked(1);
		}
		progress.done();
	}

	/**
	 * Put the file if the sync state allows it.
	 * @param localFile the local file
	 * @param overrideIncoming whether incoming changes should be overwritten
	 * @param progress a progress monitor
	 * @return whether the put succeeded (i.e. the local matches the remote)
	 * @throws TeamException
	 */
	private boolean internalPut(IFile localFile, boolean overrideIncoming, IProgressMonitor progress) throws TeamException {
		ThreeWaySynchronizer synchronizer = FileSystemSubscriber.getInstance().getSynchronizer();
		IResourceVariantComparator comparator = FileSystemSubscriber.getInstance().getResourceComparator();
		FileSystemResourceVariant remote = getResourceVariant(localFile);
		byte[] baseBytes = synchronizer.getBaseBytes(localFile);
		IResourceVariant base = provider.getResourceVariant(localFile, baseBytes);

		// Check whether we are overriding a remote change
		if (base == null && remote != null && !overrideIncoming) {
			// The remote is an incoming (or conflicting) addition.
			// Do not replace unless we are overriding
			return false;
		} else  if (base != null && remote == null) {
			// The remote is an incoming deletion
			if (!localFile.exists()) {
				// Conflicting deletion. Clear the synchronizer.
				synchronizer.flush(localFile, IResource.DEPTH_ZERO);
			} else if (!overrideIncoming) {
				// Do not override the incoming deletion
				return false;
			}
		} else if (base != null && remote != null) {
			boolean same = comparator.compare(base, remote);
			if (!isLocallyModified(localFile) && same) {
				// The base and remote are the same and there's no local changes
				// so nothing needs to be done
				return true;
			}
			if (!same && !overrideIncoming) {
				// The remote has changed. Only override if specified
				return false;
			}
		}

		// Handle an outgoing deletion
		File diskFile = provider.getFile(localFile);
		if (!localFile.exists()) {
			diskFile.delete();
			synchronizer.flush(localFile, IResource.DEPTH_ZERO);
		} else {
			// Otherwise, upload the contents
			try (InputStream in = localFile.getContents()) {
				if (!diskFile.getParentFile().exists()) {
					diskFile.getParentFile().mkdirs();
				}
				try (FileOutputStream out = new FileOutputStream(diskFile)) {
					//Copy the contents of the local file to the remote file:
					StreamUtil.pipe(in, out, diskFile.length(), progress, diskFile.getName());
					// Mark the file as read-only to require another checkout
					localFile.setReadOnly(true);
				}
				// Update the synchronizer base bytes
				remote = getResourceVariant(localFile);
				synchronizer.setBaseBytes(localFile, remote.asBytes());
			} catch (IOException e) {
				throw FileSystemPlugin.wrapException(e);
			} catch (CoreException e) {
				throw FileSystemPlugin.wrapException(e);
			}
		}
		return true;
	}

	private boolean isLocallyModified(IFile localFile) throws TeamException {
		ThreeWaySynchronizer synchronizer = FileSystemSubscriber.getInstance().getSynchronizer();
		if (!localFile.exists()) {
			// Extra check for bug 141415
			return synchronizer.getBaseBytes(localFile) != null;
		}
		return synchronizer.isLocallyModified(localFile);
	}

	/*
	 * Get the folder and its children to the depth specified.
	 */
	private void internalPut(IContainer container, int depth, boolean overrideIncoming, IProgressMonitor progress) throws TeamException {
		try {
			ThreeWaySynchronizer synchronizer = FileSystemSubscriber.getInstance().getSynchronizer();
			// Make the local folder state match the remote folder state
			List<File> toDelete = new ArrayList<>();
			if (container.getType() == IResource.FOLDER) {
				IFolder folder = (IFolder)container;
				File diskFile = provider.getFile(container);
				FileSystemResourceVariant remote = getResourceVariant(container);
				if (!folder.exists() && remote != null) {
					// Schedule the folder for removal but delay in
					// case the folder contains incoming changes
					toDelete.add(diskFile);
				} else if (folder.exists() && remote == null) {
					// Create the remote directory and sync up the local
					diskFile.mkdir();
					synchronizer.setBaseBytes(folder, provider.getResourceVariant(folder).asBytes());
				}
			}

			// Get the children
			IResource[] children = synchronizer.members(container);
			if (children.length > 0) {
				internalPut(children, depth == IResource.DEPTH_INFINITE ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO, overrideIncoming, progress);
			}

			// Remove any empty folders
			for (File diskFile : toDelete) {
				File[] fileList = diskFile.listFiles();
				if(fileList == null) {
					throw new TeamException("Content from directory '" + diskFile.getAbsolutePath() + "' can not be listed."); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (fileList.length == 0) {
					diskFile.delete();
					synchronizer.flush(container, IResource.DEPTH_INFINITE);
				}
			}
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}
}
