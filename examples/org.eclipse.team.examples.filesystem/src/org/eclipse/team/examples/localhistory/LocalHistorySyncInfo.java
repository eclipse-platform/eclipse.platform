/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.examples.localhistory;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

public class LocalHistorySyncInfo extends SyncInfo {

	public LocalHistorySyncInfo(IResource local, IResourceVariant remote, IResourceVariantComparator comparator) {
		super(local, null, remote, comparator);
	}

	@Override
	protected int calculateKind() throws TeamException {
		if (getRemote() == null)
			return IN_SYNC;
		return super.calculateKind();
	}
}
