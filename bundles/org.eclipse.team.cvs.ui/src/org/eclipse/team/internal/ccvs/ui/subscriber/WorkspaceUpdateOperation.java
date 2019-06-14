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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.ReplaceOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * This action performs an update for the CVSWorkspaceSubscriber.
 */
public class WorkspaceUpdateOperation extends SafeUpdateOperation {

	protected WorkspaceUpdateOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, boolean promptBeforeUpdate) {
		super(configuration, elements, promptBeforeUpdate);
	}

	@Override
	protected void runUpdateDeletions(SyncInfo[] nodes, IProgressMonitor monitor) throws TeamException {
		monitor.beginTask(null, nodes.length * 100);
		for (SyncInfo node : nodes) {
			unmanage(node, Policy.subMonitorFor(monitor, 50));
			deleteAndKeepHistory(node.getLocal(), Policy.subMonitorFor(monitor, 50));
		}
		pruneEmptyParents(nodes);
		monitor.done();
	}

	@Override
	protected void runSafeUpdate(IProject project, SyncInfo[] nodes, IProgressMonitor monitor) throws TeamException {
		safeUpdate(project, getIResourcesFrom(nodes), new LocalOption[] { Command.DO_NOT_RECURSE }, monitor);
	}

	@Override
	protected void overwriteUpdate(SyncInfoSet syncSet, IProgressMonitor monitor) throws TeamException {
		try {
			new ReplaceOperation(getPart(), syncSet.getResources(), null /* tag */, false /* recurse */)
				.run(monitor);
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (InterruptedException e) {
			Policy.cancelOperation();
		}
		
	}
	

	@Override
	protected void updated(IResource[] resources) throws TeamException {
		// Do nothing
	}
	
	private void unmanage(SyncInfo element, IProgressMonitor monitor) throws CVSException {
		CVSWorkspaceRoot.getCVSResourceFor(element.getLocal()).unmanage(monitor);
	}

	private void deleteAndKeepHistory(IResource resource, IProgressMonitor monitor) throws CVSException {
		try {
			if (!resource.exists()) return;
			if (resource.getType() == IResource.FILE)
				((IFile)resource).delete(false /* force */, true /* keep history */, monitor);
			else if (resource.getType() == IResource.FOLDER)
				((IFolder)resource).delete(false /* force */, true /* keep history */, monitor);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

}
