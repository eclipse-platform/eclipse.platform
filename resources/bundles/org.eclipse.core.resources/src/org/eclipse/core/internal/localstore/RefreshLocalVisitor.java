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
 *     Sergey Prigogin (Google) - [482064] Incorrect SubMonitor usage in RefreshLocalVisitor.visit
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import org.eclipse.core.internal.resources.Container;
import org.eclipse.core.internal.resources.File;
import org.eclipse.core.internal.resources.Folder;
import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.internal.resources.ResourceStatus;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;

/**
 * Visits a unified tree, and synchronizes the file system with the
 * resource tree.  After the visit is complete, the file system will
 * be synchronized with the workspace tree with respect to
 * resource existence, gender, and timestamp.
 */
public class RefreshLocalVisitor implements IUnifiedTreeVisitor, ILocalStoreConstants {
	/** control constants */
	protected static final int RL_UNKNOWN = 0;
	protected static final int RL_IN_SYNC = 1;
	protected static final int RL_NOT_IN_SYNC = 2;

	// Progress monitor will initially move by 1. / TOTAL_WORK per resource but will gradually slow down
	// as more resources are discovered.
	public static final int TOTAL_WORK = 1000;

	protected MultiStatus errors;
	protected SubMonitor monitor;
	protected boolean resourceChanged;
	protected Workspace workspace;

	public RefreshLocalVisitor(IProgressMonitor monitor) {
		this.monitor = SubMonitor.convert(monitor);
		workspace = (Workspace) ResourcesPlugin.getWorkspace();
		resourceChanged = false;
		String msg = Messages.resources_errorMultiRefresh;
		errors = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_READ_LOCAL, msg, null);
	}

	/**
	 * This method has the same implementation as resourceChanged but as they are different
	 * cases, we prefer to use different methods.
	 */
	protected void contentAdded(UnifiedTreeNode node, Resource target) {
		resourceChanged(node, target);
	}

	protected void createResource(UnifiedTreeNode node, Resource target) throws CoreException {
		ResourceInfo info = target.getResourceInfo(false, false);
		int flags = target.getFlags(info);
		if (target.exists(flags, false))
			return;
		/* make sure target's parent exists */
		IContainer parent = target.getParent();
		if (parent.getType() == IResource.FOLDER)
			((Folder) target.getParent()).ensureExists(monitor);
		/* Use the basic file creation protocol since we don't want to create any content on disk. */
		info = workspace.createResource(target, false);
		/* Mark this resource as having unknown children */
		info.set(ICoreConstants.M_CHILDREN_UNKNOWN);
		target.getLocalManager().updateLocalSync(info, node.getLastModified());
	}

	protected void deleteResource(UnifiedTreeNode node, Resource target) throws CoreException {
		ResourceInfo info = target.getResourceInfo(false, false);
		int flags = target.getFlags(info);
		//don't delete linked resources
		if (ResourceInfo.isSet(flags, ICoreConstants.M_LINK)) {
			//just clear local sync info
			info = target.getResourceInfo(false, true);
			//handle concurrent deletion
			if (info != null) {
				info.clearModificationStamp();
				target.getLocalManager().updateLocalSync(info, node.getLastModified());
			}
			return;
		}
		if (target.exists(flags, false))
			target.deleteResource(true, errors);
		node.setExistsWorkspace(false);
	}

	protected void fileToFolder(UnifiedTreeNode node, Resource target) throws CoreException {
		ResourceInfo info = target.getResourceInfo(false, false);
		int flags = target.getFlags(info);
		if (target.exists(flags, true)) {
			target = (Folder) ((File) target).changeToFolder();
		} else {
			if (!target.exists(flags, false)) {
				target = (Resource) workspace.getRoot().getFolder(target.getFullPath());
				// Use the basic file creation protocol since we don't want to create any content on disk.
				workspace.createResource(target, false);
			}
		}
		node.setResource(target);
		info = target.getResourceInfo(false, true);
		target.getLocalManager().updateLocalSync(info, node.getLastModified());
	}

	protected void folderToFile(UnifiedTreeNode node, Resource target) throws CoreException {
		ResourceInfo info = target.getResourceInfo(false, false);
		int flags = target.getFlags(info);
		if (target.exists(flags, true))
			target = (File) ((Folder) target).changeToFile();
		else {
			if (!target.exists(flags, false)) {
				target = (Resource) workspace.getRoot().getFile(target.getFullPath());
				// Use the basic file creation protocol since we don't want to
				// create any content on disk.
				workspace.createResource(target, false);
			}
		}
		node.setResource(target);
		info = target.getResourceInfo(false, true);
		target.getLocalManager().updateLocalSync(info, node.getLastModified());
	}

	/**
	 * Returns the status of the nodes visited so far.  This will be a multi-status
	 * that describes all problems that have occurred, or an OK status if everything
	 * went smoothly.
	 */
	public IStatus getErrorStatus() {
		return errors;
	}

	protected void makeLocal(UnifiedTreeNode node, Resource target) {
		ResourceInfo info = target.getResourceInfo(false, true);
		if (info != null)
			target.getLocalManager().updateLocalSync(info, node.getLastModified());
	}

	/**
	 * Refreshes the parent of a resource currently being synchronized.
	 */
	protected void refresh(Container parent) throws CoreException {
		parent.getLocalManager().refresh(parent, IResource.DEPTH_ZERO, false, null);
	}

	protected void resourceChanged(UnifiedTreeNode node, Resource target) {
		ResourceInfo info = target.getResourceInfo(false, true);
		if (info == null)
			return;
		target.getLocalManager().updateLocalSync(info, node.getLastModified());
		info.incrementContentId();
		// forget content-related caching flags
		info.clear(ICoreConstants.M_CONTENT_CACHE);
		workspace.updateModificationStamp(info);
	}

	public boolean resourcesChanged() {
		return resourceChanged;
	}

	/**
	 * deletion or creation -- Returns:
	 * 	- RL_IN_SYNC - the resource is in-sync with the file system
	 * 	- RL_NOT_IN_SYNC - the resource is not in-sync with file system
	 * 	- RL_UNKNOWN - couldn't determine the sync status for this resource
	 */
	protected int synchronizeExistence(UnifiedTreeNode node, Resource target) throws CoreException {
		if (node.existsInWorkspace()) {
			if (!node.existsInFileSystem()) {
				// 1. non-local files are always in sync
				// 2. links to non-existent locations with the modification stamp of IResource.NULL_STAMP are in sync
				if (target.isLocal(IResource.DEPTH_ZERO) && target.getModificationStamp() != IResource.NULL_STAMP) {
					deleteResource(node, target);
					resourceChanged = true;
					return RL_NOT_IN_SYNC;
				}
				return RL_IN_SYNC;
			}
		} else {
			// do we have a gender variant in the workspace?
			IResource genderVariant = workspace.getRoot().findMember(target.getFullPath());
			if (genderVariant != null)
				return RL_UNKNOWN;
			if (node.existsInFileSystem()) {
				Container parent = (Container) target.getParent();
				if (!parent.exists()) {
					refresh(parent);
					if (!parent.exists())
						return RL_NOT_IN_SYNC;
				}
				if (!target.getName().equals(node.getLocalName()))
					return RL_IN_SYNC;
				if (!Workspace.caseSensitive && node.getLevel() == 0) {
					// do we have any alphabetic variants in the workspace?
					IResource variant = target.findExistingResourceVariant(target.getFullPath());
					if (variant != null) {
						deleteResource(node, ((Resource) variant));
						createResource(node, target);
						resourceChanged = true;
						return RL_NOT_IN_SYNC;
					}
				}
				createResource(node, target);
				resourceChanged = true;
				return RL_NOT_IN_SYNC;
			}
		}
		return RL_UNKNOWN;
	}

	/**
	 * gender change -- Returns true if gender was in sync.
	 */
	protected boolean synchronizeGender(UnifiedTreeNode node, Resource target) throws CoreException {
		if (!node.existsInWorkspace()) {
			//may be an existing resource in the workspace of different gender
			IResource genderVariant = workspace.getRoot().findMember(target.getFullPath());
			if (genderVariant != null)
				target = (Resource) genderVariant;
		}
		if (target.getType() == IResource.FILE) {
			if (node.isFolder()) {
				fileToFolder(node, target);
				resourceChanged = true;
				return false;
			}
		} else {
			if (!node.isFolder()) {
				folderToFile(node, target);
				resourceChanged = true;
				return false;
			}
		}
		return true;
	}

	/**
	 * lastModified
	 */
	protected void synchronizeLastModified(UnifiedTreeNode node, Resource target) {
		if (target.isLocal(IResource.DEPTH_ZERO))
			resourceChanged(node, target);
		else
			contentAdded(node, target);
		resourceChanged = true;
	}

	@Override
	public boolean visit(UnifiedTreeNode node) throws CoreException {
		Policy.checkCanceled(monitor);
		try {
			if (node.isErrorInFileSystem())
				return false; // Don't visit children if we encountered an I/O error
			Resource target = (Resource) node.getResource();
			int targetType = target.getType();
			if (targetType == IResource.PROJECT)
				return true;
			if (node.existsInWorkspace() && node.existsInFileSystem()) {
				/* for folders we only care about updating local status */
				if (targetType == IResource.FOLDER && node.isFolder()) {
					// if not local, mark as local
					if (!target.isLocal(IResource.DEPTH_ZERO))
						makeLocal(node, target);
					ResourceInfo info = target.getResourceInfo(false, false);
					if (info != null && info.getModificationStamp() != IResource.NULL_STAMP)
						return true;
				}
				/* compare file last modified */
				if (targetType == IResource.FILE && !node.isFolder()) {
					ResourceInfo info = target.getResourceInfo(false, false);
					if (info != null && info.getModificationStamp() != IResource.NULL_STAMP && info.getLocalSyncInfo() == node.getLastModified())
						return true;
				}
			} else {
				if (node.existsInFileSystem() && !IPath.EMPTY.isValidSegment(node.getLocalName())) {
					String message = NLS.bind(Messages.resources_invalidResourceName, node.getLocalName());
					errors.merge(new ResourceStatus(IResourceStatus.INVALID_RESOURCE_NAME, message));
					return false;
				}
				int state = synchronizeExistence(node, target);
				if (state == RL_IN_SYNC || state == RL_NOT_IN_SYNC) {
					if (targetType == IResource.FILE) {
						try {
							((File) target).updateMetadataFiles();
						} catch (CoreException e) {
							errors.merge(e.getStatus());
						}
					}
					return true;
				}
			}
			if (node.isSymbolicLink() && !node.existsInFileSystem())
				return true; // Dangling symbolic links are considered to be synchronized.

			if (synchronizeGender(node, target))
				synchronizeLastModified(node, target);
			if (targetType == IResource.FILE) {
				try {
					((File) target).updateMetadataFiles();
				} catch (CoreException e) {
					errors.merge(e.getStatus());
				}
			}
			return true;
		} finally {
			// The monitor will asymptotically approach 100% as the number of processed resources increases.
			monitor.setWorkRemaining(TOTAL_WORK).worked(1);
		}
	}
}
