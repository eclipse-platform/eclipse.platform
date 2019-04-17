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
package org.eclipse.team.internal.ccvs.ui.operations;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Operation that ensures that the contents for remote
 * of each local resource is cached.
 */
public class CacheRemoteContentsOperation extends CacheTreeContentsOperation {

	public CacheRemoteContentsOperation(IWorkbenchPart part, ResourceMapping[] mappers, IResourceDiffTree tree) {
		super(part, mappers, tree);
	}
	
	@Override
	protected IFileRevision getRemoteFileState(IThreeWayDiff twd) {
		IResourceDiff diff = (IResourceDiff)twd.getRemoteChange();
		if (diff == null)
			return null;
		return diff.getAfterState();
	}

	@Override
	protected boolean isEnabledForDirection(int direction) {
		return direction == IThreeWayDiff.CONFLICTING || 
			direction == IThreeWayDiff.INCOMING;
	}

	@Override
	protected ICVSRemoteResource buildTree(CVSTeamProvider provider) throws TeamException {
		return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().buildRemoteTree(provider.getProject(), true, new NullProgressMonitor());
	}

}
