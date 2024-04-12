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
 *     Alexander Gurov - bug 230853
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.HashSet;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * The job of this input is to create the logical model of the contents of the
 * sync set for displaying to the user. The created logical model must diff
 * nodes.
 * <ol>
 * <li>First, prepareInput is called to initialize the model with the given sync
 * set. Building the model occurs in the UI thread.</li>
 * <li>The input must react to changes in the sync set and adjust its diff node
 * model then update the viewer. In effect mediating between the sync set
 * changes and the model shown to the user. This happens in the ui thread.
 * </ol>
 * NOT ON DEMAND - model is created then maintained!
 *
 * @since 3.0
 */
public class HierarchicalModelProvider extends SynchronizeModelProvider {

	public static class HierarchicalModelProviderDescriptor implements ISynchronizeModelProviderDescriptor {
		public static final String ID = TeamUIPlugin.ID + ".modelprovider_hierarchical"; //$NON-NLS-1$
		@Override
		public String getId() {
			return ID;
		}
		@Override
		public String getName() {
			return TeamUIMessages.HierarchicalModelProvider_0;
		}
		@Override
		public ImageDescriptor getImageDescriptor() {
			return TeamImages.getImageDescriptor(ITeamUIImages.IMG_HIERARCHICAL);
		}
	}
	private static final HierarchicalModelProviderDescriptor hierarchicalDescriptor = new HierarchicalModelProviderDescriptor();

	/**
	 * Create an input based on the provide sync set. The input is not
	 * initialized until <code>prepareInput</code> is called.
	 *
	 * @param configuration
	 *            the synchronize page configuration
	 *
	 * @param set
	 *            the sync set used as the basis for the model created by this
	 *            input.
	 */
	public HierarchicalModelProvider(ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		super(configuration, set);
	}

	public HierarchicalModelProvider(
			AbstractSynchronizeModelProvider parentProvider,
			ISynchronizeModelElement modelRoot,
			ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		super(parentProvider, modelRoot, configuration, set);
	}

	@Override
	public ISynchronizeModelProviderDescriptor getDescriptor() {
		return hierarchicalDescriptor;
	}

	@Override
	public ViewerComparator getViewerComparator() {
		return new SynchronizeModelElementComparator();
	}

	protected SyncInfoTree getSyncInfoTree() {
		return (SyncInfoTree)getSyncInfoSet();
	}

	/**
	 * Invoked by the <code>buildModelObject</code> method to create
	 * the childen of the given node. This method can be overriden
	 * by subclasses but subclasses should inv
	 * @return the diff elements
	 */
	protected IDiffElement[] createModelObjects(ISynchronizeModelElement container) {
		IResource resource = null;
		if (container == getModelRoot()) {
			resource = ResourcesPlugin.getWorkspace().getRoot();
		} else {
			resource = container.getResource();
		}
		if(resource != null) {
			SyncInfoTree infoTree = getSyncInfoTree();
			IResource[] children = infoTree.members(resource);
			ISynchronizeModelElement[] nodes = new ISynchronizeModelElement[children.length];
			for (int i = 0; i < children.length; i++) {
				nodes[i] = createModelObject(container, children[i]);
			}
			return nodes;
		}
		return new IDiffElement[0];
	}

	protected ISynchronizeModelElement createModelObject(ISynchronizeModelElement parent, IResource resource) {
		SyncInfo info = getSyncInfoTree().getSyncInfo(resource);
		SynchronizeModelElement newNode;
		if(info != null) {
			newNode = new SyncInfoModelElement(parent, info);
		} else {
			newNode = new UnchangedResourceModelElement(parent, resource);
		}
		addToViewer(newNode);
		return newNode;
	}

	/**
	 * Invokes <code>getModelObject(Object)</code> on an array of resources.
	 * @param resources
	 *            the resources
	 * @return the model objects for the resources
	 */
	protected Object[] getModelObjects(IResource[] resources) {
		Object[] result = new Object[resources.length];
		for (int i = 0; i < resources.length; i++) {
			result[i] = getModelObject(resources[i]);
		}
		return result;
	}

	protected void addResources(IResource[] added) {
		for (IResource resource : added) {
			addResource(resource);
		}
	}

	private void addResource(IResource resource) {
		ISynchronizeModelElement node = getModelObject(resource);
		if (node != null) {
			// Somehow the node exists. Remove it and read it to ensure
			// what is shown matches the contents of the sync set
			removeFromViewer(resource);
		}
		// Build the sub-tree rooted at this node
		ISynchronizeModelElement parent = getModelObject(resource.getParent());
		if (parent != null) {
			node = createModelObject(parent, resource);
			buildModelObjects(node);
		}
	}

	@Override
	protected IDiffElement[] buildModelObjects(ISynchronizeModelElement node) {
		IDiffElement[] children = createModelObjects(node);
		for (IDiffElement element : children) {
			if (element instanceof ISynchronizeModelElement) {
				buildModelObjects((ISynchronizeModelElement) element);
			}
		}
		return children;
	}

	@Override
	protected void handleResourceAdditions(ISyncInfoTreeChangeEvent event) {
		SyncInfo[] infos = event.getAddedResources();
		HashSet<IProject> set = new HashSet<>();
		for (SyncInfo info : infos) {
			set.add(info.getLocal().getProject());
		}
		for (IProject element : set) {
			addResource(element);
		}
	}

	@Override
	protected void handleResourceRemovals(ISyncInfoTreeChangeEvent event) {
		// Remove the removed subtrees
		IResource[] removedRoots = event.getRemovedSubtreeRoots();
		removeFromViewer(removedRoots);
		// We have to look for folders that may no longer be in the set
		// (i.e. are in-sync) but still have descendants in the set
		IResource[] removedResources = event.getRemovedResources();
		for (IResource resource : removedResources) {
			if (resource.getType() != IResource.FILE) {
				ISynchronizeModelElement node = getModelObject(resource);
				if (node != null) {
					removeFromViewer(resource);
				}
			}
		}
	}

	@Override
	protected ISynchronizeModelElement createModelObject(ISynchronizeModelElement parent, SyncInfo info) {
		return createModelObject(parent, info.getLocal());
	}

	@Override
	protected void addResource(SyncInfo info) {
		addResource(info.getLocal());
	}
}
