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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.ResourceVariantByteStore;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;


public class CVSBaseResourceVariantTree extends ResourceVariantByteStore {
	public void dispose() {
		// Do nothing
	}
	public byte[] getBytes(IResource resource) throws TeamException {
		if (resource.getType() == IResource.FILE) {
			// For a file, return the entry line
			byte[] bytes =  EclipseSynchronizer.getInstance().getSyncBytes(resource);
			if (bytes != null) {
				// Use the base sync info (i.e. no deletion or addition)
				if (ResourceSyncInfo.isDeletion(bytes)) {
					bytes = ResourceSyncInfo.convertFromDeletion(bytes);
				} else if (ResourceSyncInfo.isAddition(bytes)) {
					bytes = null;
				}
			}
			return bytes;
		} else {
			// For a folder, return the folder sync info bytes
			FolderSyncInfo info = EclipseSynchronizer.getInstance().getFolderSync((IContainer)resource);
			if (info == null) return null;
			return info.getBytes();
		}
	}
	public boolean isVariantKnown(IResource resource) throws TeamException {
		return getBytes(resource) != null;
	}
	public boolean flushBytes(IResource resource, int depth) throws TeamException {
		throw new UnsupportedOperationException();
	}
	public boolean setBytes(IResource resource, byte[] bytes) throws TeamException {
		throw new UnsupportedOperationException();
	}
	public boolean deleteBytes(IResource resource) throws TeamException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public IResource[] members(IResource resource) throws TeamException {
		if(resource.getType() == IResource.FILE) {
			return new IResource[0];
		}	
		return EclipseSynchronizer.getInstance().members((IContainer)resource);
	}
}
