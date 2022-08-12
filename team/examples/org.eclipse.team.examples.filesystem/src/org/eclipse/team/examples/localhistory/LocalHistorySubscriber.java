/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

public class LocalHistorySubscriber extends Subscriber {

	private LocalHistoryVariantComparator comparator;

	public LocalHistorySubscriber() {
		this.comparator = new LocalHistoryVariantComparator();
	}

	@Override
	public String getName() {
		return "Local History Subscriber"; //$NON-NLS-1$
	}

	/**
	 * @param resource the resource being tested
	 */
	@Override
	public boolean isSupervised(IResource resource) {
		// all resources in the workspace can potentially have resource history
		return true;
	}

	@Override
	public IResource[] members(IResource resource) throws TeamException {
		try {
			if(resource.getType() == IResource.FILE)
				return new IResource[0];
			IContainer container = (IContainer)resource;
			List<IResource> existingChildren = new ArrayList<>(Arrays.asList(container.members()));
			existingChildren.addAll(Arrays.asList(container.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, null)));
			return existingChildren.toArray(new IResource[existingChildren.size()]);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

	@Override
	public IResource[] roots() {
		return ResourcesPlugin.getWorkspace().getRoot().getProjects();
	}

	@Override
	public SyncInfo getSyncInfo(IResource resource) throws TeamException {
		try {
			IResourceVariant variant = null;
			if(resource.getType() == IResource.FILE) {
				IFile file = (IFile)resource;
				IFileState[] states = file.getHistory(null);
				if(states.length > 0) {
					// last state only
					variant = new LocalHistoryVariant(states[0]);
				}
			}
			SyncInfo info = new LocalHistorySyncInfo(resource, variant, comparator);
			info.init();
			return info;
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

	@Override
	public IResourceVariantComparator getResourceComparator() {
		return comparator;
	}

	/**
	 * @param resources
	 *            the resources to refresh
	 * @param depth
	 *            the depth
	 * @param monitor
	 *            progress monitor, or <code>null</code> if progress reporting
	 *            and cancellation are not desired
	 */
	@Override
	public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) {
		// do nothing
	}
}
