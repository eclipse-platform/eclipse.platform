/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.subscribers;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeChangeListener;

/**
 * An abstract implementation of an {@link ISynchronizationScope}.
 *
 * @since 3.2
 */
public abstract class AbstractSynchronizationScope implements ISynchronizationScope {

	private ListenerList listeners = new ListenerList(ListenerList.IDENTITY);

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.ISynchronizationScope#getRoots()
	 */
	public IResource[] getRoots() {
		List result = new ArrayList();
		ResourceTraversal[] traversals = getTraversals();
		for (int i = 0; i < traversals.length; i++) {
			ResourceTraversal traversal = traversals[i];
			IResource[] resources = traversal.getResources();
			for (int j = 0; j < resources.length; j++) {
				IResource resource = resources[j];
				accumulateRoots(result, resource);
			}
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.ISynchronizationScope#contains(org.eclipse.core.resources.IResource)
	 */
	public boolean contains(IResource resource) {
		ResourceTraversal[] traversals = getTraversals();
		for (int i = 0; i < traversals.length; i++) {
			ResourceTraversal traversal = traversals[i];
			if (traversal.contains(resource))
				return true;
		}
		return false;
	}

	/*
	 * Add the resource to the list if it isn't there already
	 * or is not a child of an existing resource.
	 */
	private void accumulateRoots(List roots, IResource resource) {
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
		for (int i = 0; i < allListeners.length; i++) {
			final Object listener = allListeners[i];
			SafeRunner.run(new ISafeRunnable() {
				public void run() throws Exception {
					((ISynchronizationScopeChangeListener)listener).scopeChanged(AbstractSynchronizationScope.this, newMappings, newTraversals);
				}
				public void handleException(Throwable exception) {
					// Logged by Platform
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.ISynchronizationScope#addPropertyChangeListener(org.eclipse.core.runtime.Preferences.IPropertyChangeListener)
	 */
	public void addScopeChangeListener(ISynchronizationScopeChangeListener listener) {
		listeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.ISynchronizationScope#removePropertyChangeListener(org.eclipse.core.runtime.Preferences.IPropertyChangeListener)
	 */
	public void removeScopeChangeListener(ISynchronizationScopeChangeListener listener) {
		listeners.remove(listener);
	}

}
