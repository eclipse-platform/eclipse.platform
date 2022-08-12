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
 *     Serge Beauchamp (Freescale Semiconductor) - [229633] Group and Project Path Variable Support
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.net.URI;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.osgi.util.NLS;

public class Folder extends Container implements IFolder {
	protected Folder(IPath path, Workspace container) {
		super(path, container);
	}

	protected void assertCreateRequirements(IFileStore store, IFileInfo localInfo, int updateFlags) throws CoreException {
		checkDoesNotExist();
		Container parent = (Container) getParent();
		ResourceInfo info = parent.getResourceInfo(false, false);
		parent.checkAccessible(getFlags(info));
		checkValidGroupContainer(parent, false, false);

		final boolean force = (updateFlags & IResource.FORCE) != 0;
		if (!force && localInfo.exists()) {
			//return an appropriate error message for case variant collisions
			if (!Workspace.caseSensitive) {
				String name = getLocalManager().getLocalName(store);
				if (name != null && !store.getName().equals(name)) {
					String msg = NLS.bind(Messages.resources_existsLocalDifferentCase, new Path(store.toString()).removeLastSegments(1).append(name).toOSString());
					throw new ResourceException(IResourceStatus.CASE_VARIANT_EXISTS, getFullPath(), msg, null);
				}
			}
			String msg = NLS.bind(Messages.resources_fileExists, store.toString());
			throw new ResourceException(IResourceStatus.FAILED_WRITE_LOCAL, getFullPath(), msg, null);
		}
	}

	/* (non-Javadoc)
	 * Changes this folder to be a file in the resource tree and returns the newly
	 * created file.  All related properties are deleted.  It is assumed that on
	 * disk the resource is already a file so no action is taken to delete the disk
	 * contents.
	 * <p>
	 * <b>This method is for the exclusive use of the local refresh mechanism</b>
	 *
	 * @see org.eclipse.core.internal.localstore.RefreshLocalVisitor#folderToFile(UnifiedTreeNode, Resource)
	 */
	public IFile changeToFile() throws CoreException {
		getPropertyManager().deleteProperties(this, IResource.DEPTH_INFINITE);
		IFile result = workspace.getRoot().getFile(path);
		if (isLinked()) {
			URI location = getRawLocationURI();
			delete(IResource.NONE, null);
			result.createLink(location, IResource.ALLOW_MISSING_LOCAL, null);
		} else {
			workspace.deleteResource(this);
			workspace.createResource(result, false);
		}
		return result;
	}

	@Override
	public void create(int updateFlags, boolean local, IProgressMonitor monitor) throws CoreException {
		if ((updateFlags & IResource.VIRTUAL) == IResource.VIRTUAL) {
			createLink(LinkDescription.VIRTUAL_LOCATION, updateFlags, monitor);
			return;
		}
		final boolean force = (updateFlags & IResource.FORCE) != 0;
		String message = NLS.bind(Messages.resources_creating, getFullPath());
		SubMonitor subMonitor = SubMonitor.convert(monitor, message, 100);
		checkValidPath(path, FOLDER, true);
		final ISchedulingRule rule = workspace.getRuleFactory().createRule(this);
		SubMonitor newChild = subMonitor.newChild(1);
		try {
			workspace.prepareOperation(rule, newChild);
			IFileStore store = getStore();
			IFileInfo localInfo = store.fetchInfo();
			assertCreateRequirements(store, localInfo, updateFlags);
			workspace.beginOperation(true);
			if (force && !Workspace.caseSensitive && localInfo.exists()) {
				String name = getLocalManager().getLocalName(store);
				if (name == null || localInfo.getName().equals(name)) {
					delete(true, null);
				} else {
					// The file system is not case sensitive and a case variant exists at this
					// location
					String msg = NLS.bind(Messages.resources_existsLocalDifferentCase,
							new Path(store.toString()).removeLastSegments(1).append(name).toOSString());
					throw new ResourceException(IResourceStatus.CASE_VARIANT_EXISTS, getFullPath(), msg, null);
				}
			}
			internalCreate(updateFlags, local, subMonitor.newChild(98));
			workspace.getAliasManager().updateAliases(this, getStore(), IResource.DEPTH_ZERO, subMonitor.newChild(1));
		} catch (OperationCanceledException e) {
			workspace.getWorkManager().operationCanceled();
			throw e;
		} finally {
			subMonitor.done();
			workspace.endOperation(rule, true);
		}
	}

	@Override
	public void create(boolean force, boolean local, IProgressMonitor monitor) throws CoreException {
		// funnel all operations to central method
		create((force ? IResource.FORCE : IResource.NONE), local, monitor);
	}

	/**
	 * Ensures that this folder exists in the workspace. This is similar in
	 * concept to mkdirs but it does not work on projects.
	 * If this folder is created, it will be marked as being local.
	 */
	public void ensureExists(IProgressMonitor monitor) throws CoreException {
		ResourceInfo info = getResourceInfo(false, false);
		int flags = getFlags(info);
		if (exists(flags, true))
			return;
		if (exists(flags, false)) {
			String message = NLS.bind(Messages.resources_folderOverFile, getFullPath());
			throw new ResourceException(IResourceStatus.RESOURCE_WRONG_TYPE, getFullPath(), message, null);
		}
		Container parent = (Container) getParent();
		if (parent.getType() == PROJECT) {
			info = parent.getResourceInfo(false, false);
			parent.checkExists(getFlags(info), true);
		} else
			((Folder) parent).ensureExists(monitor);
		if (getType() == FOLDER && isUnderVirtual())
			create(IResource.VIRTUAL | IResource.FORCE, true, monitor);
		else
			internalCreate(IResource.FORCE, true, monitor);
	}

	@Override
	public String getDefaultCharset(boolean checkImplicit) {
		// non-existing resources default to parent's charset
		if (!exists())
			return checkImplicit ? workspace.getCharsetManager().getCharsetFor(getFullPath().removeLastSegments(1), true) : null;
		return workspace.getCharsetManager().getCharsetFor(getFullPath(), checkImplicit);
	}

	@Override
	public int getType() {
		return FOLDER;
	}

	public void internalCreate(int updateFlags, boolean local, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		try {
			String message = NLS.bind(Messages.resources_creating, getFullPath());
			monitor.beginTask(message, Policy.totalWork);
			workspace.createResource(this, updateFlags);
			if (local) {
				try {
					final boolean force = (updateFlags & IResource.FORCE) != 0;
					getLocalManager().write(this, force, Policy.subMonitorFor(monitor, Policy.totalWork));
				} catch (CoreException e) {
					// a problem happened creating the folder on disk, so delete from the workspace
					workspace.deleteResource(this);
					throw e; // rethrow
				}
			}
			internalSetLocal(local, DEPTH_ZERO);
			if (!local)
				getResourceInfo(true, true).clearModificationStamp();
		} finally {
			monitor.done();
		}
	}
}
