/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.*;
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

    private final List providers = new ArrayList();
    private final Map resourceToElements = new HashMap(); // Map IResource to List of ISynchronizeModelElement
    private final Map elementToProvider = new HashMap(); // Map ISynchronizeModelElement -> AbstractSynchronizeModelProvider

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

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#getProvider(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
     */
    @Override
	protected ISynchronizeModelProvider getProvider(ISynchronizeModelElement element) {
        return (ISynchronizeModelProvider)elementToProvider.get(element);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#getClosestExistingParents(org.eclipse.core.resources.IResource)
     */
    @Override
	public ISynchronizeModelElement[] getClosestExistingParents(IResource resource) {
        ISynchronizeModelProvider[] providers = getProviders();
        if (providers.length == 0) {
            return new ISynchronizeModelElement[0];
        }
        if (providers.length == 1 && providers[0] instanceof AbstractSynchronizeModelProvider) {
            return ((AbstractSynchronizeModelProvider)providers[0]).getClosestExistingParents(resource);
        }
        List result = new ArrayList();
        for (int i = 0; i < providers.length; i++) {
            ISynchronizeModelProvider provider = providers[i];
            if (provider instanceof AbstractSynchronizeModelProvider) {
	            ISynchronizeModelElement[] elements = ((AbstractSynchronizeModelProvider)provider).getClosestExistingParents(resource);
	            for (int j = 0; j < elements.length; j++) {
	                ISynchronizeModelElement element = elements[j];
	                result.add(element);
	            }
            }
        }
        return (ISynchronizeModelElement[]) result.toArray(new ISynchronizeModelElement[result.size()]);
    }

    /**
     * Return all the sub-providers of this composite.
     * @return the sub-providers of this composite
     */
    protected ISynchronizeModelProvider[] getProviders() {
        return (ISynchronizeModelProvider[]) providers.toArray(new ISynchronizeModelProvider[providers.size()]);
    }

    /**
     * Return the providers that are displaying the given resource.
     * @param resource the resource
     * @return the providers displaying the resource
     */
    protected ISynchronizeModelProvider[] getProvidersContaining(IResource resource) {
        List elements = (List)resourceToElements.get(resource);
        if (elements == null || elements.isEmpty()) {
            return new ISynchronizeModelProvider[0];
        }
        List result = new ArrayList();
        for (Iterator iter = elements.iterator(); iter.hasNext();) {
            ISynchronizeModelElement element = (ISynchronizeModelElement)iter.next();
            result.add(getProvider(element));
        }
        return (ISynchronizeModelProvider[]) result.toArray(new ISynchronizeModelProvider[result.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#handleResourceAdditions(org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent)
     */
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
        for (int i = 0; i < resources.length; i++) {
            SyncInfo info = resources[i];
            handleAddition(info);
        }
    }

    /**
     * Handle the addition of the given sync info to this provider
     * @param info the added sync info
     */
    protected abstract void handleAddition(SyncInfo info);

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#handleResourceChanges(org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent)
     */
    @Override
	protected final void handleResourceChanges(ISyncInfoTreeChangeEvent event) {
        SyncInfo[] infos = event.getChangedResources();
        for (int i = 0; i < infos.length; i++) {
            SyncInfo info = infos[i];
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

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#handleResourceRemovals(org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent)
     */
    @Override
	protected final void handleResourceRemovals(ISyncInfoTreeChangeEvent event) {
        IResource[] resources = event.getRemovedResources();
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            handleRemoval(resource);
        }
    }

    /**
     * Remove the resource from all providers that are displaying it
     * @param resource the resource to be removed
     */
    protected void handleRemoval(IResource resource) {
        ISynchronizeModelProvider[] providers = getProvidersContaining(resource);
        for (int i = 0; i < providers.length; i++) {
            ISynchronizeModelProvider provider = providers[i];
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

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#nodeAdded(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
     */
	@Override
	protected void nodeAdded(ISynchronizeModelElement node, AbstractSynchronizeModelProvider provider) {
		// Update the resource-to-element map and the element-to-provider map
		IResource r = node.getResource();
		if(r != null) {
			List elements = (List)resourceToElements.get(r);
			if(elements == null) {
				elements = new ArrayList(2);
				resourceToElements.put(r, elements);
			}
			elements.add(node);
		}
		elementToProvider.put(node, provider);
		super.nodeAdded(node, provider);
	}

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#modelObjectCleared(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
     */
    @Override
	public void modelObjectCleared(ISynchronizeModelElement node) {
        super.modelObjectCleared(node);
	    IResource r = node.getResource();
		if(r != null) {
			List elements = (List)resourceToElements.get(r);
			if(elements != null) {
				elements.remove(node);
				if (elements.isEmpty()) {
				    resourceToElements.remove(r);
				}
			}
		}
		elementToProvider.remove(node);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#clearModelObjects(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
     */
    @Override
	protected void recursiveClearModelObjects(ISynchronizeModelElement node) {
        super.recursiveClearModelObjects(node);
        if (node == getModelRoot()) {
            clearProviders();
        }
    }

    private void clearProviders() {
        for (Iterator iter = providers.iterator(); iter.hasNext();) {
            ISynchronizeModelProvider provider = (ISynchronizeModelProvider) iter.next();
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

	/* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#dispose()
     */
    @Override
	public void dispose() {
        clearProviders();
        super.dispose();
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#hasViewerState()
     */
    @Override
	protected boolean hasViewerState() {
        return resourceToElements != null && !resourceToElements.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#getModelObjects(org.eclipse.core.resources.IResource)
     */
    @Override
	protected ISynchronizeModelElement[] getModelObjects(IResource resource) {
        List elements = (List)resourceToElements.get(resource);
        if (elements == null) {
            return new ISynchronizeModelElement[0];
        }
        return (ISynchronizeModelElement[]) elements.toArray(new ISynchronizeModelElement[elements.size()]);
    }
}
