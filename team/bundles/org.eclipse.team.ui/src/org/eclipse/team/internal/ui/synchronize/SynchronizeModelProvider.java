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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener;
import org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * This class is reponsible for creating and maintaining a presentation model of
 * {@link SynchronizeModelElement} elements that can be shown in a viewer. The model
 * is based on the synchronization information contained in the provided {@link SyncInfoSet}.
 * <p>
 * label updates (property propagation to parent nodes)
 * sync change listener (changes, additions, removals, reset)
 * batching busy updates
 * </p>
 *
 * @see HierarchicalModelProvider
 * @see CompressedFoldersModelProvider
 * @since 3.0
 */
public abstract class SynchronizeModelProvider extends AbstractSynchronizeModelProvider implements ISyncInfoSetChangeListener {

	protected final Map<IResource, ISynchronizeModelElement> resourceMap = Collections.synchronizedMap(new HashMap<>());

	protected static final boolean DEBUG = false;

	public SynchronizeModelProvider(ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		super(configuration, set);
	}

	public SynchronizeModelProvider(AbstractSynchronizeModelProvider parentProvider, ISynchronizeModelElement modelRoot, ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		super(parentProvider, modelRoot, configuration, set);
		associateRoot(modelRoot);
	}

	private void associateRoot(ISynchronizeModelElement modelRoot) {
		// associate the root resource with the provider's root element
		resourceMap.put(ResourcesPlugin.getWorkspace().getRoot(), modelRoot);
	}

	/**
	 * Dispose of the builder
	 */
	@Override
	public void dispose() {
		resourceMap.clear();
		super.dispose();
	}

	/**
	 * Returns the sorter for this model provider.
	 *
	 * @return the sorter for this model provider.
	 */
	@Override
	public abstract ViewerSorter getViewerSorter();

	/**
	 * Return the model object (i.e. an instance of <code>SyncInfoModelElement</code>
	 * or one of its subclasses) for the given IResource.
	 * @param resource
	 *            the resource
	 * @return the <code>SyncInfoModelElement</code> for the given resource
	 */
	protected ISynchronizeModelElement getModelObject(IResource resource) {
		return resourceMap.get(resource);
	}

	@Override
	protected ISynchronizeModelElement[] getModelObjects(IResource resource) {
		ISynchronizeModelElement element = getModelObject(resource);
		if (element == null) {
			return new ISynchronizeModelElement[0];
		}
		return new ISynchronizeModelElement[] { element };
	}

	protected void associateDiffNode(ISynchronizeModelElement node) {
		IResource resource = node.getResource();
		if(resource != null) {
			resourceMap.put(resource, node);
		}
	}

	protected void unassociateDiffNode(IResource resource) {
		resourceMap.remove(resource);
	}

	/**
	 * Helper method to remove a resource from the viewer. If the resource
	 * is not mapped to a model element, this is a no-op.
	 * @param resource the resource to remove
	 */
	protected void removeFromViewer(IResource resource) {
		ISynchronizeModelElement element = getModelObject(resource);
		if(element != null) {
			removeFromViewer(new ISynchronizeModelElement[] { element });
		}
	}

	/**
	 * Helper method to remove a set of resources from the viewer. If a resource
	 * is not mapped to a model element, it is ignored.
	 * @param resources the resources to remove
	 */
	protected void removeFromViewer(IResource[] resources) {
		List<ISynchronizeModelElement> elements = new ArrayList<>();
		for (IResource resource : resources) {
			ISynchronizeModelElement element = getModelObject(resource);
			if(element != null) {
				elements.add(element);
			}
		}
		if (!elements.isEmpty()) {
			removeFromViewer(elements.toArray(new ISynchronizeModelElement[elements.size()]));
		}
	}

	@Override
	protected void recursiveClearModelObjects(ISynchronizeModelElement node) {
		super.recursiveClearModelObjects(node);
		if (node == getModelRoot()) {
			// If we are clearing everything under the root
			// than just purge the resource map
			resourceMap.clear();
			// Reassociate the root node to allow the children to be readded
			associateRoot(getModelRoot());
		} else {
			IResource resource = node.getResource();
			if (resource != null) {
				unassociateDiffNode(resource);
			}
		}
	}

	@Override
	protected void addToViewer(ISynchronizeModelElement node) {
		associateDiffNode(node);
		super.addToViewer(node);
	}

	@Override
	protected boolean hasViewerState() {
		return ! resourceMap.isEmpty();
	}

	@Override
	public ISynchronizeModelElement[] getClosestExistingParents(IResource resource) {
		ISynchronizeModelElement element = getModelObject(resource);
		if(element == null) {
			do {
				resource = resource.getParent();
				element = getModelObject(resource);
			} while(element == null && resource != null);
		}
		if (element == null) {
			return new ISynchronizeModelElement[0];
		}
		return new ISynchronizeModelElement[] { element };
	}

	@Override
	protected final void handleChanges(ISyncInfoTreeChangeEvent event, IProgressMonitor monitor) {
		super.handleChanges(event, monitor);
	}

	@Override
	protected void handleResourceChanges(ISyncInfoTreeChangeEvent event) {
		// Refresh the viewer for each changed resource
		SyncInfo[] infos = event.getChangedResources();
		for (SyncInfo info : infos) {
			IResource local = info.getLocal();
			ISynchronizeModelElement diffNode = getModelObject(local);
			if (diffNode != null) {
				handleChange(diffNode, info);
			}
		}
	}

	/**
	 * The sync state for the existing diff node has changed and the new state
	 * is provided by the given sync info.
	 * @param diffNode the changed diff node
	 * @param info the new sync state
	 */
	protected void handleChange(ISynchronizeModelElement diffNode, SyncInfo info) {
		IResource local = info.getLocal();

		if(diffNode instanceof SyncInfoModelElement) {
			((SyncInfoModelElement)diffNode).update(info);
			propogateConflictState(diffNode, false);
			queueForLabelUpdate(diffNode);
		} else {
			removeFromViewer(local);
			addResource(info);
			ISynchronizeModelElement node = getModelObject(info.getLocal());
			buildModelObjects(node);

		}
	}

	/**
	 * Add the give sync infos to the provider, creating
	 * any intermediate nodes a required.
	 * @param added the added infos
	 */
	protected void addResources(SyncInfo[] added) {
		for (SyncInfo info : added) {
			addResource(info);
		}
	}

	/**
	 * Add the give sync info to the provider, creating
	 * any intermediate nodes a required and adding any children as well
	 * @param info the added infos
	 */
	protected abstract void addResource(SyncInfo info);

	/**
	 * Create the model object for the given sync info as a child of the given parent node.
	 * @param parent the parent
	 * @param info the info to be used for the new node
	 * @return the created node
	 */
	protected abstract ISynchronizeModelElement createModelObject(ISynchronizeModelElement parent, SyncInfo info);
}
