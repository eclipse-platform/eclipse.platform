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
package org.eclipse.team.internal.core.subscribers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeChangeListener;

/**
 * An abstract implementation of an {@link ISynchronizationScope}.
 *
 * @since 3.2
 */
public abstract class AbstractSynchronizationScope implements ISynchronizationScope {

	private ListenerList<ISynchronizationScopeChangeListener> listeners = new ListenerList<>(ListenerList.IDENTITY);

	@Override
	public IResource[] getRoots() {
		List<IResource> result = new ArrayList<>();
		ResourceTraversal[] traversals = getTraversals();
		for (ResourceTraversal traversal : traversals) {
			IResource[] resources = traversal.getResources();
			for (IResource resource : resources) {
				accumulateRoots(result, resource);
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	@Override
	public boolean contains(IResource resource) {
		ResourceTraversal[] traversals = getTraversals();
		for (ResourceTraversal traversal : traversals) {
			if (traversal.contains(resource))
				return true;
		}
		return false;
	}

	/*
	 * Add the resource to the list if it isn't there already
	 * or is not a child of an existing resource.
	 */
	private void accumulateRoots(List<IResource> roots, IResource resource) {
		IPath resourcePath = resource.getFullPath();
		for (Iterator iter = roots.iterator(); iter.hasNext();) {
			IResource root = (IResource) iter.next();
			IPath rootPath = root.getFullPath();
			// If there is a higher resource in the collection, skip this one
			if (rootPath.isPrefixOf(resourcePath))
				return;
			// If there are lower resources, remove them
			if (resourcePath.isPrefixOf(rootPath))
				iter.remove();
		}
		// There were no higher resources, so add this one
		roots.add(resource);
	}

	/**
	 * Fire the scope change event
	 * @param newTraversals the new traversals (may be empty)
	 * @param newMappings the new mappings (may be empty)
	 */
	public void fireTraversalsChangedEvent(final ResourceTraversal[] newTraversals, final ResourceMapping[] newMappings) {
		Object[] allListeners = listeners.getListeners();
		for (Object listener : allListeners) {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					((ISynchronizationScopeChangeListener)listener).scopeChanged(AbstractSynchronizationScope.this, newMappings, newTraversals);
				}
				@Override
				public void handleException(Throwable exception) {
					// Logged by Platform
				}
			});
		}
	}

	@Override
	public void addScopeChangeListener(ISynchronizationScopeChangeListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeScopeChangeListener(ISynchronizationScopeChangeListener listener) {
		listeners.remove(listener);
	}

}
