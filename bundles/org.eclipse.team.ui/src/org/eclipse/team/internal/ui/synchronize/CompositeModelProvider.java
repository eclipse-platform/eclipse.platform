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
package org.eclipse.team.internal.ui.synchronize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * This class provides functionality for defining composite synchronize model
 * providers. A composite provider is one that breaks up the displayed
 * {@link SyncInfoSet} into subsets that may be displayed using one
 * or more synchronize model providers.
 *
 */
public abstract class CompositeModelProvider extends AbstractSynchronizeModelProvider {

	private final List<ISynchronizeModelProvider> providers = new ArrayList<>();
	private final Map<IResource, List <ISynchronizeModelElement>> resourceToElements = new HashMap<>();
	private final Map<ISynchronizeModelElement, AbstractSynchronizeModelProvider> elementToProvider = new HashMap<>();

	protected CompositeModelProvider(ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		super(configuration, set);
	}

	/**
	 * Add the provider to the list of providers.
	 * @param provider the provider to be added
	 */
	protected void addProvider(ISynchronizeModelProvider provider) {
		providers.add(provider);
	}

	/**
	 * Remove the provider from the list of providers.
	 * @param provider the provider to be removed
	 */
	protected void removeProvider(ISynchronizeModelProvider provider) {
		providers.remove(provider);
		provider.dispose();
	}

	@Override
	protected ISynchronizeModelProvider getProvider(ISynchronizeModelElement element) {
		return elementToProvider.get(element);
	}

	@Override
	public ISynchronizeModelElement[] getClosestExistingParents(IResource resource) {
		ISynchronizeModelProvider[] providers = getProviders();
		if (providers.length == 0) {
			return new ISynchronizeModelElement[0];
		}
		if (providers.length == 1 && providers[0] instanceof AbstractSynchronizeModelProvider) {
			return ((AbstractSynchronizeModelProvider)providers[0]).getClosestExistingParents(resource);
		}
		List<ISynchronizeModelElement> result = new ArrayList<>();
		for (ISynchronizeModelProvider provider : providers) {
			if (provider instanceof AbstractSynchronizeModelProvider) {
				ISynchronizeModelElement[] elements = ((AbstractSynchronizeModelProvider)provider).getClosestExistingParents(resource);
				Collections.addAll(result, elements);
			}
		}
		return result.toArray(new ISynchronizeModelElement[result.size()]);
	}

	/**
	 * Return all the sub-providers of this composite.
	 * @return the sub-providers of this composite
	 */
	protected ISynchronizeModelProvider[] getProviders() {
		return providers.toArray(new ISynchronizeModelProvider[providers.size()]);
	}

	/**
	 * Return the providers that are displaying the given resource.
	 * @param resource the resource
	 * @return the providers displaying the resource
	 */
	protected ISynchronizeModelProvider[] getProvidersContaining(IResource resource) {
		List<ISynchronizeModelElement> elements = resourceToElements.get(resource);
		if (elements == null || elements.isEmpty()) {
			return new ISynchronizeModelProvider[0];
		}
		List<ISynchronizeModelProvider> result = new ArrayList<>();
		for (ISynchronizeModelElement element : elements) {
			result.add(getProvider(element));
		}
		return result.toArray(new ISynchronizeModelProvider[result.size()]);
	}

	@Override
	protected final void handleResourceAdditions(ISyncInfoTreeChangeEvent event) {
		handleAdditions(event.getAddedResources());
	}

	/**
	 * Handle the resource additions by adding them to any existing
	 * sub-providers or by creating addition sub-providers as needed.
	 * @param resources
	 */
	protected void handleAdditions(SyncInfo[] resources) {
		for (SyncInfo info : resources) {
			handleAddition(info);
		}
	}

	/**
	 * Handle the addition of the given sync info to this provider
	 * @param info the added sync info
	 */
	protected abstract void handleAddition(SyncInfo info);

	@Override
	protected final void handleResourceChanges(ISyncInfoTreeChangeEvent event) {
		SyncInfo[] infos = event.getChangedResources();
		for (SyncInfo info : infos) {
			handleChange(info);
		}
	}

	/**
	 * The state of the sync info for a resource has changed. Propagate the
	 * change to any sub-providers that contain the resource.
	 * @param info the sync info for the resource whose sync state has changed
	 */
	protected void handleChange(SyncInfo info) {
		handleRemoval(info.getLocal());
		handleAddition(info);
	}

	@Override
	protected final void handleResourceRemovals(ISyncInfoTreeChangeEvent event) {
		IResource[] resources = event.getRemovedResources();
		for (IResource resource : resources) {
			handleRemoval(resource);
		}
	}

	/**
	 * Remove the resource from all providers that are displaying it
	 * @param resource the resource to be removed
	 */
	protected void handleRemoval(IResource resource) {
		ISynchronizeModelProvider[] providers = getProvidersContaining(resource);
		for (ISynchronizeModelProvider provider : providers) {
			removeFromProvider(resource, provider);
		}
	}

	/**
	 * Remove the resource from the sync set of the given provider
	 * unless the provider is this composite. Subclasses can
	 * override if they show resources directly.
	 * @param resource the resource to be removed
	 * @param provider the provider from which to remove the resource
	 */
	protected void removeFromProvider(IResource resource, ISynchronizeModelProvider provider) {
		if (provider != this) {
			provider.getSyncInfoSet().remove(resource);
		}
	}

	@Override
	protected void nodeAdded(ISynchronizeModelElement node, AbstractSynchronizeModelProvider provider) {
		// Update the resource-to-element map and the element-to-provider map
		IResource r = node.getResource();
		if(r != null) {
			List<ISynchronizeModelElement> elements = resourceToElements.get(r);
			if(elements == null) {
				elements = new ArrayList<>(2);
				resourceToElements.put(r, elements);
			}
			elements.add(node);
		}
		elementToProvider.put(node, provider);
		super.nodeAdded(node, provider);
	}

	@Override
	public void modelObjectCleared(ISynchronizeModelElement node) {
		super.modelObjectCleared(node);
		IResource r = node.getResource();
		if(r != null) {
			List elements = resourceToElements.get(r);
			if(elements != null) {
				elements.remove(node);
				if (elements.isEmpty()) {
					resourceToElements.remove(r);
				}
			}
		}
		elementToProvider.remove(node);
	}

	@Override
	protected void recursiveClearModelObjects(ISynchronizeModelElement node) {
		super.recursiveClearModelObjects(node);
		if (node == getModelRoot()) {
			clearProviders();
		}
	}

	private void clearProviders() {
		for (ISynchronizeModelProvider provider : providers) {
			provider.dispose();
		}
		providers.clear();
		resourceToElements.clear();
		elementToProvider.clear();
	}

	/**
	 * Helper method for creating a provider for the given id.
	 * @param parent the root node for the new provider
	 * @param id the id of the providers descriptor
	 * @return the new provider
	 */
	protected ISynchronizeModelProvider createModelProvider(ISynchronizeModelElement parent, String id, SyncInfoTree syncInfoTree) {
		if (id != null && id.endsWith(FlatModelProvider.FlatModelProviderDescriptor.ID)) {
			return new FlatModelProvider(this, parent, getConfiguration(), syncInfoTree);
		} else if (id != null && id.endsWith(CompressedFoldersModelProvider.CompressedFolderModelProviderDescriptor.ID)) {
			return new CompressedFoldersModelProvider(this, parent, getConfiguration(), syncInfoTree);
		} else {
			return new HierarchicalModelProvider(this, parent, getConfiguration(), syncInfoTree);
		}
	}

	@Override
	public void dispose() {
		clearProviders();
		super.dispose();
	}

	@Override
	protected boolean hasViewerState() {
		return resourceToElements != null && !resourceToElements.isEmpty();
	}

	@Override
	protected ISynchronizeModelElement[] getModelObjects(IResource resource) {
		List<ISynchronizeModelElement> elements = resourceToElements.get(resource);
		if (elements == null) {
			return new ISynchronizeModelElement[0];
		}
		return elements.toArray(new ISynchronizeModelElement[elements.size()]);
	}
}
