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

import java.io.*;
import java.util.*;
import org.eclipse.core.internal.localstore.SafeChunkyInputStream;
import org.eclipse.core.internal.localstore.SafeFileInputStream;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.internal.watson.IPathRequestor;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

//
public class Synchronizer implements ISynchronizer {
	protected Workspace workspace;
	protected SyncInfoWriter writer;

	// Registry of sync partners. Set of qualified names.
	protected Set<QualifiedName> registry = new HashSet<>(5);

	public Synchronizer(Workspace workspace) {
		super();
		this.workspace = workspace;
		this.writer = new SyncInfoWriter(workspace, this);
	}

	/**
	 * @see ISynchronizer#accept(QualifiedName, IResource, IResourceVisitor, int)
	 */
	@Override
	public void accept(QualifiedName partner, IResource resource, IResourceVisitor visitor, int depth)
			throws CoreException {
		Assert.isLegal(partner != null);
		Assert.isLegal(resource != null);
		Assert.isLegal(visitor != null);

		// if we don't have sync info for the given identifier, then skip it
		if (getSyncInfo(partner, resource) != null) {
			// visit the resource and if the visitor says to stop the recursion then return
			if (!visitor.visit(resource))
				return;
		}

		// adjust depth if necessary
		if (depth == IResource.DEPTH_ZERO || resource.getType() == IResource.FILE)
			return;
		if (depth == IResource.DEPTH_ONE)
			depth = IResource.DEPTH_ZERO;

		// otherwise recurse over the children
		IResource[] children = ((IContainer) resource).members();
		for (IResource element : children)
			accept(partner, element, visitor, depth);
	}

	/**
	 * @see ISynchronizer#add(QualifiedName)
	 */
	@Override
	public void add(QualifiedName partner) {
		Assert.isLegal(partner != null);
		registry.add(partner);
	}

	/**
	 * @see ISynchronizer#flushSyncInfo(QualifiedName, IResource, int)
	 */
	@Override
	public void flushSyncInfo(final QualifiedName partner, final IResource root, final int depth) throws CoreException {
		Assert.isLegal(partner != null);
		Assert.isLegal(root != null);

		ICoreRunnable body = monitor -> {
			IResourceVisitor visitor = resource -> {
				// only need to flush sync info if there is sync info
				if (getSyncInfo(partner, resource) != null)
					setSyncInfo(partner, resource, null);
				return true;
			};
			root.accept(visitor, depth, true);
		};
		workspace.run(body, root, IResource.NONE, null);
	}

	/**
	 * @see ISynchronizer#getPartners()
	 */
	@Override
	public QualifiedName[] getPartners() {
		return registry.toArray(new QualifiedName[registry.size()]);
	}

	/**
	 * For use by the serialization code.
	 */
	protected Set<QualifiedName> getRegistry() {
		return registry;
	}

	/**
	 * @see ISynchronizer#getSyncInfo(QualifiedName, IResource)
	 */
	@Override
	public byte[] getSyncInfo(QualifiedName partner, IResource resource) throws CoreException {
		Assert.isLegal(partner != null);
		Assert.isLegal(resource != null);

		if (!isRegistered(partner)) {
			String message = NLS.bind(Messages.synchronizer_partnerNotRegistered, partner);
			throw new ResourceException(new ResourceStatus(IResourceStatus.PARTNER_NOT_REGISTERED, message));
		}

		// namespace check, if the resource doesn't exist then return null
		ResourceInfo info = workspace.getResourceInfo(resource.getFullPath(), true, false);
		return (info == null) ? null : info.getSyncInfo(partner, true);
	}

	protected boolean isRegistered(QualifiedName partner) {
		Assert.isLegal(partner != null);
		return registry.contains(partner);
	}

	/**
	 * @see #savePartners(DataOutputStream)
	 */
	public void readPartners(DataInputStream input) throws CoreException {
		SyncInfoReader reader = new SyncInfoReader(workspace, this);
		reader.readPartners(input);
	}

	public void restore(IResource resource, IProgressMonitor monitor) throws CoreException {
		// first restore from the last save and then apply any snapshots
		restoreFromSave(resource);
		restoreFromSnap(resource);
	}

	protected void restoreFromSave(IResource resource) throws CoreException {
		IPath sourceLocation = workspace.getMetaArea().getSyncInfoLocationFor(resource);
		IPath tempLocation = workspace.getMetaArea().getBackupLocationFor(sourceLocation);
		if (!sourceLocation.toFile().exists() && !tempLocation.toFile().exists())
			return;
		try (DataInputStream input = new DataInputStream(
				new SafeFileInputStream(sourceLocation.toOSString(), tempLocation.toOSString()))) {
			SyncInfoReader reader = new SyncInfoReader(workspace, this);
			reader.readSyncInfo(input);
		} catch (Exception e) {
			// don't let runtime exceptions such as ArrayIndexOutOfBounds prevent startup
			String msg = NLS.bind(Messages.resources_readMeta, sourceLocation);
			throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, sourceLocation, msg, e);
		}
	}

	protected void restoreFromSnap(IResource resource) {
		IPath sourceLocation = workspace.getMetaArea().getSyncInfoSnapshotLocationFor(resource);
		if (!sourceLocation.toFile().exists())
			return;
		try (DataInputStream input = new DataInputStream(new SafeChunkyInputStream(sourceLocation.toFile()))) {
			SyncInfoSnapReader reader = new SyncInfoSnapReader(workspace, this);
			while (true)
				reader.readSyncInfo(input);
		} catch (EOFException eof) {
			// ignore end of file -- proceed with what we successfully read
		} catch (Exception e) {
			// only log the exception, we should not fail restoring the snapshot
			String msg = NLS.bind(Messages.resources_readMeta, sourceLocation);
			Policy.log(new ResourceStatus(IResourceStatus.FAILED_READ_METADATA, sourceLocation, msg, e));
		}
	}

	/**
	 * @see ISynchronizer#remove(QualifiedName)
	 */
	@Override
	public void remove(QualifiedName partner) {
		Assert.isLegal(partner != null);
		if (isRegistered(partner)) {
			// remove all sync info for this partner
			try {
				flushSyncInfo(partner, workspace.getRoot(), IResource.DEPTH_INFINITE);
				registry.remove(partner);
			} catch (CoreException e) {
				// XXX: flush needs to be more resilient and not throw exceptions all the time
				Policy.log(e);
			}
		}
	}

	public void savePartners(DataOutputStream output) throws IOException {
		writer.savePartners(output);
	}

	public void saveSyncInfo(ResourceInfo info, IPathRequestor requestor, DataOutputStream output,
			List<QualifiedName> writtenPartners) throws IOException {
		writer.saveSyncInfo(info, requestor, output, writtenPartners);
	}

	protected void setRegistry(Set<QualifiedName> registry) {
		this.registry = registry;
	}

	/**
	 * @see ISynchronizer#setSyncInfo(QualifiedName, IResource, byte[])
	 */
	@Override
	public void setSyncInfo(QualifiedName partner, IResource resource, byte[] info) throws CoreException {
		Assert.isLegal(partner != null);
		Assert.isLegal(resource != null);
		try {
			workspace.prepareOperation(resource, null);
			workspace.beginOperation(true);
			if (!isRegistered(partner)) {
				String message = NLS.bind(Messages.synchronizer_partnerNotRegistered, partner);
				throw new ResourceException(new ResourceStatus(IResourceStatus.PARTNER_NOT_REGISTERED, message));
			}
			// we do not store sync info on the workspace root
			if (resource.getType() == IResource.ROOT)
				return;
			// if the resource doesn't yet exist then create a phantom so we can set the
			// sync info on it
			Resource target = (Resource) resource;
			ResourceInfo resourceInfo = workspace.getResourceInfo(target.getFullPath(), true, false);
			int flags = target.getFlags(resourceInfo);
			if (!target.exists(flags, false)) {
				if (info == null)
					return;
				// ensure it is possible to create this resource
				target.checkValidPath(target.getFullPath(), target.getType(), false);
				Container parent = (Container) target.getParent();
				parent.checkAccessible(parent.getFlags(parent.getResourceInfo(true, false)));
				workspace.createResource(target, true);
			}
			resourceInfo = target.getResourceInfo(true, true);
			resourceInfo.setSyncInfo(partner, info);
			resourceInfo.incrementSyncInfoGenerationCount();
			resourceInfo.set(ICoreConstants.M_SYNCINFO_SNAP_DIRTY);
			flags = target.getFlags(resourceInfo);
			if (target.isPhantom(flags) && resourceInfo.getSyncInfo(false) == null) {
				MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.INTERNAL_ERROR,
						Messages.resources_deleteProblem, null);
				((Resource) resource).deleteResource(false, status);
				if (!status.isOK())
					throw new ResourceException(status);
			}
		} finally {
			workspace.endOperation(resource, false);
		}
	}

	public void snapSyncInfo(ResourceInfo info, IPathRequestor requestor, DataOutputStream output) throws IOException {
		writer.snapSyncInfo(info, requestor, output);
	}
}
