/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.core.mapping.provider;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.team.core.ICache;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.internal.core.Cache;
import org.eclipse.team.internal.core.Policy;

/**
 * Abstract implementation of the {@link ISynchronizationContext} interface.
 * This class can be subclassed by clients.
 *
 * @see ISynchronizationContext
 * @since 3.2
 */
public abstract class SynchronizationContext extends PlatformObject implements ISynchronizationContext {

	private final int type;
	private final IResourceDiffTree diffTree;
	private Cache cache;
	private final ISynchronizationScopeManager manager;

	/**
	 * Create a synchronization context.
	 * @param manager the manager that defines the scope of the synchronization
	 * @param type the type of synchronization (ONE_WAY or TWO_WAY)
	 * @param diffTree the sync info tree that contains all out-of-sync resources
	 */
	protected SynchronizationContext(ISynchronizationScopeManager manager, int type, IResourceDiffTree diffTree) {
		this.manager = manager;
		this.type = type;
		this.diffTree = diffTree;
	}

	@Override
	public ISynchronizationScope getScope() {
		return getScopeManager().getScope();
	}

	/**
	 * Return the scope manager for the scope of this context.
	 * @return the scope manager for the scope of this context
	 */
	public ISynchronizationScopeManager getScopeManager() {
		return manager;
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public void dispose() {
		if (cache != null) {
			cache.dispose();
		}
		manager.dispose();
	}

	@Override
	public synchronized ICache getCache() {
		if (cache == null) {
			cache = new Cache();
		}
		return cache;
	}

	@Override
	public IResourceDiffTree getDiffTree() {
		return diffTree;
	}

	@Override
	public void refresh(ResourceMapping[] mappings, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(null, 100);
		ISynchronizationScopeManager manager = getScopeManager();
		if (manager == null) {
			// The scope manager is missing so just refresh everything
			refresh(getScope().getTraversals(), IResource.NONE, Policy.subMonitorFor(monitor, 50));
		} else {
			ResourceTraversal[] traversals = manager.refresh(mappings, Policy.subMonitorFor(monitor, 50));
			if (traversals.length > 0)
				refresh(traversals, IResource.NONE, Policy.subMonitorFor(monitor, 50));
		}
		monitor.done();
	}

}
