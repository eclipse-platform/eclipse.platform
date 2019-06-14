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
package org.eclipse.team.internal.ccvs.core.syncinfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.PersistantResourceVariantByteStore;
import org.eclipse.team.core.variants.ResourceVariantByteStore;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.core.subscribers.DescendantResourceVariantByteStore;

/**
 * CVS sycnrhonization cache that ignores stale remote bytes
 */
public class CVSDescendantResourceVariantByteStore extends DescendantResourceVariantByteStore {

	public CVSDescendantResourceVariantByteStore(ResourceVariantByteStore baseCache, PersistantResourceVariantByteStore remoteCache) {
		super(baseCache, remoteCache);
	}

	@Override
	protected boolean isDescendant(IResource resource, byte[] baseBytes, byte[] remoteBytes) throws TeamException {
		if (resource.getType() != IResource.FILE) return true;
		try {
			return ResourceSyncInfo.isLaterRevisionOnSameBranch(remoteBytes, baseBytes);
		} catch (CVSException e) {
			throw TeamException.asTeamException(e);
		}
	}
	
	@Override
	public boolean setBytes(IResource resource, byte[] bytes) throws TeamException {
		boolean changed = super.setBytes(resource, bytes);
		if (resource.getType() == IResource.FILE && getBytes(resource) != null && !parentHasSyncBytes(resource)) {
			// Log a warning if there is no sync bytes available for the resource's
			// parent but there is valid sync bytes for the child
			CVSProviderPlugin.log(new TeamException(NLS.bind(CVSMessages.ResourceSynchronizer_missingParentBytesOnSet, new String[] { ((PersistantResourceVariantByteStore)getRemoteStore()).getSyncName().toString(), resource.getFullPath().toString() }))); 
		}
		return changed;
	}

	/**
	 * Indicates whether the parent of the given local resource has sync bytes for its
	 * corresponding remote resource. The parent bytes of a remote resource are required
	 * (by CVS) to create a handle to the remote resource.
	 */
	protected boolean parentHasSyncBytes(IResource resource) throws TeamException {
		if (resource.getType() == IResource.PROJECT) return true;
		return (getBytes(resource.getParent()) != null);
	}

	@Override
	public boolean isVariantKnown(IResource resource) throws TeamException {
		return ((PersistantResourceVariantByteStore)getRemoteStore()).isVariantKnown(resource);
	}

	/*
	 * TODO: Could possibly be generalized and moved up
	 */
	public IStatus handleResourceChanges(IResource[] changedResources, boolean canModifyWorkspace) {
		// IMPORTANT NOTE: This will throw exceptions if performed during the POST_CHANGE delta phase!!!
		List errors = new ArrayList();
		for (IResource resource : changedResources) {
			try {
				if (!isInCVSProject(resource)) continue;
				if (resource.getType() == IResource.FILE
						&& (resource.exists() || resource.isPhantom())) {
					byte[] remoteBytes = getBytes(resource);
					if (remoteBytes == null) {
						if (isVariantKnown(resource)) {
							// The remote is known not to exist. If the local resource is
							// managed then this information is stale
							if (getBaseStore().getBytes(resource) != null) {
								if (canModifyWorkspace) {
									flushBytes(resource, IResource.DEPTH_ZERO);
								} else {
									// The revision  comparison will handle the stale sync bytes
									// TODO: Unless the remote is known not to exist (see bug 52936)
								}
							}
						}
					} else {
						byte[] localBytes = getBaseStore().getBytes(resource);
						if (localBytes == null || !isDescendant(resource, localBytes, remoteBytes)) {
							if (canModifyWorkspace) {
								flushBytes(resource, IResource.DEPTH_ZERO);
							} else {
								// The remote byte store handles the stale sync bytes
							}
						}
					}
				} else if (resource.getType() == IResource.FOLDER) {
					// If the base has sync info for the folder, purge the remote bytes
					if (getBaseStore().getBytes(resource) != null && canModifyWorkspace) {
						flushBytes(resource, IResource.DEPTH_ZERO);
					}
				}
			} catch (TeamException e) {
				errors.add(e);
			}
		}
		for (Iterator iter = errors.iterator(); iter.hasNext();) {
			TeamException e = (TeamException) iter.next();
			CVSProviderPlugin.log(e);
		}
		return Status.OK_STATUS; // TODO
	}

	private boolean isInCVSProject(IResource resource) {
		return RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId()) != null;
	}
}
