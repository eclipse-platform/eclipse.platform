/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.team.core.diff.FastDiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.core.subscribers.DiffChangeSet;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.ui.mapping.SynchronizationOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public abstract class ResourceModelProviderOperation extends SynchronizationOperation {

	private final IStructuredSelection selection;

	protected ResourceModelProviderOperation(ISynchronizePageConfiguration configuration, IStructuredSelection selection) {
		super(configuration, getElements(selection));
		this.selection = selection;
	}

	private static Object[] getElements(IStructuredSelection selection) {
		return selection.toArray();
	}

	/**
	 * Return the file deltas that are either contained in the selection
	 * or are children of the selection and visible given the current
	 * mode of the page configuration.
	 * @param elements the selected elements
	 * @return the file deltas contained in or descended from the selection
	 */
	private IDiff[] getFileDeltas(Object[] pathOrElements) {
		Set<IDiff> result = new HashSet<>();
		for (int i = 0; i < pathOrElements.length; i++) {
			Object pathOrElement = pathOrElements[i];
			IDiff[] diffs = getFileDeltas(pathOrElement);
			for (int j = 0; j < diffs.length; j++) {
				IDiff node = diffs[j];
				result.add(node);
			}
		}
		return result.toArray(new IDiff[result.size()]);
	}

	private IDiff[] getFileDeltas(Object pathOrElement) {
		List<IDiff> result = new ArrayList<>();
		ResourceTraversal[] traversals = getTraversals(pathOrElement);
		if (traversals.length > 0) {
			ISynchronizationContext context = getContext();
			final IResourceDiffTree diffTree = context.getDiffTree();
			IDiff[] diffs = diffTree.getDiffs(traversals);
			// Now filter the by the mode of the configuration
			for (int i = 0; i < diffs.length; i++) {
				IDiff node = diffs[i];
				if (isVisible(node) && getDiffFilter().select(node))
					result.add(node);
			}
		}
		return result.toArray(new IDiff[result.size()]);
	}

	/**
	 * Return whether the given node is visible in the page based
	 * on the mode in the configuration.
	 * @param node a diff node
	 * @return whether the given node is visible in the page
	 */
	protected boolean isVisible(IDiff node) {
		return ((SynchronizePageConfiguration)getConfiguration()).isVisible(node);
	}

	private ResourceTraversal[] getTraversals(Object pathOrElement) {
		// First check to see if the element is a tree path
		Object element;
		if (pathOrElement instanceof TreePath) {
			TreePath tp = (TreePath) pathOrElement;
			Object o = tp.getFirstSegment();
			if (o instanceof DiffChangeSet) {
				// Special handling for change sets
				DiffChangeSet dcs = (DiffChangeSet) o;
				return getTraversalCalculator().getTraversals(dcs, tp);
			}
			element = tp.getLastSegment();
		} else {
			element = pathOrElement;
		}

		// Check for resources and adjust the depth to match the provider depth
		if (isResourcePath(pathOrElement)) {
			IResource resource = (IResource) element;
			return getTraversalCalculator().getTraversals(resource, (TreePath)pathOrElement);
		}

		// Finally, just get the traversals from the mapping.
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null) {
			// First, check if we have already calculated the traversal
			ResourceTraversal[] traversals = getContext().getScope().getTraversals(mapping);
			if (traversals != null)
				return traversals;
			// We need to determine the traversals from the mapping.
			// By default, use the local context. Models will need to provide
			// custom handlers if this doesn't work for them
			try {
				return mapping.getTraversals(ResourceMappingContext.LOCAL_CONTEXT, null);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
		return new ResourceTraversal[0];
	}

	private boolean isResourcePath(Object pathOrElement) {
		if (pathOrElement instanceof TreePath) {
			TreePath tp = (TreePath) pathOrElement;
			return getTraversalCalculator().isResourcePath(tp);
		}
		return false;
	}

	/**
	 * Return the filter used to match diffs to which this action applies.
	 * @return the filter used to match diffs to which this action applies
	 */
	protected abstract FastDiffFilter getDiffFilter();

	@Override
	public boolean shouldRun() {
		Object[] elements = getElements();
		for (int i = 0; i < elements.length; i++) {
			Object object = elements[i];
			if (Utils.getResourceMapping(internalGetElement(object)) != null) {
				return true;
			}
		}
		return false;
	}

	protected IDiff[] getTargetDiffs() {
		return getFileDeltas(getTreePathsOrElements());
	}

	private Object[] getTreePathsOrElements() {
		if (selection instanceof ITreeSelection) {
			ITreeSelection ts = (ITreeSelection) selection;
			return ts.getPaths();
		}
		return getElements();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.TeamOperation#canRunAsJob()
	 */
	@Override
	protected boolean canRunAsJob() {
		return true;
	}

	protected ResourceModelTraversalCalculator getTraversalCalculator() {
		return ResourceModelTraversalCalculator.getTraversalCalculator(getConfiguration());
	}

	private Object internalGetElement(Object elementOrPath) {
		if (elementOrPath instanceof TreePath) {
			TreePath tp = (TreePath) elementOrPath;
			return tp.getLastSegment();
		}
		return elementOrPath;
	}

	@Override
	public boolean belongsTo(Object family) {
		if (family == getContext()) {
			return true;
		}
		return super.belongsTo(family);
	}

}
