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
package org.eclipse.team.examples.filesystem.subscriber;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

/**
 * Provide a custom sync info that will report files that exist both
 * locally and remotely as in-sync and will return a null base if there
 * is an incoming change.
 */
public class FileSystemSyncInfo extends SyncInfo {

	public FileSystemSyncInfo(IResource local, IResourceVariant base, IResourceVariant remote, IResourceVariantComparator comparator) {
		super(local, base, remote, comparator);
	}

	@Override
	protected int calculateKind() throws TeamException {
		if (getLocal().getType() != IResource.FILE) {
			if (getLocal().exists() && getRemote() != null) {
				return IN_SYNC;
			}
		}
		return super.calculateKind();
	}

	@Override
	public IResourceVariant getBase() {
		// If the kind has been set and there is an incoming change
		// return null as the base since the server does not keep the
		// base contents
//		if ((getKind() & INCOMING) > 0) {
//			return null;
//		}
		return super.getBase();
	}
}
