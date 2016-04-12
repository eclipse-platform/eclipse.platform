/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.*;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.core.mapping.CompoundResourceTraversal;
import org.eclipse.team.internal.core.subscribers.DiffChangeSet;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelParticipantAction;

public abstract class ResourceModelParticipantAction extends ModelParticipantAction {

	public ResourceModelParticipantAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	/**
	 * Return the set of traversals that cover the resources in the current selection
	 * @param selection the selection
	 * @param monitor a progress monitor
	 * @return the traversals that cover the resources in the current selection
	 * @throws CoreException
	 */
	protected ResourceTraversal[] getResourceTraversals(IStructuredSelection selection, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(null, selection.size() * 100);
			CompoundResourceTraversal traversal = new CompoundResourceTraversal();
			if (selection instanceof ITreeSelection) {
				ITreeSelection ts = (ITreeSelection) selection;
				TreePath[] paths = ts.getPaths();
				for (int i = 0; i < paths.length; i++) {
					TreePath path = paths[i];
					ResourceTraversal[] traversals = getTraversals(path, Policy.subMonitorFor(monitor, 100));
					traversal.addTraversals(traversals);
				}
			} else {
				for (Iterator iter = selection.iterator(); iter.hasNext();) {
					Object element = (Object) iter.next();
					ResourceTraversal[] traversals = getTraversals(element, Policy.subMonitorFor(monitor, 100));
					traversal.addTraversals(traversals);
				}
			}
			return traversal.asTraversals();
		} finally {
			monitor.done();
		}
	}

	/**
	 * Return a traversal that includes the resources that are visible
	 * in the sync view.
	 * @param element the selected element
	 * @return a set of traversals that cover the visible resources.
	 */
	private ResourceTraversal[] getTraversals(Object element, IProgressMonitor monitor) throws CoreException {
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null)
			return mapping.getTraversals(getResourceMappingContext(), monitor);
		return null;
	}

	protected ResourceMappingContext getResourceMappingContext() {
		return new SynchronizationResourceMappingContext(getSynchronizationContext());
	}

	protected ResourceModelTraversalCalculator getTraversalCalculator() {
		return ResourceModelTraversalCalculator.getTraversalCalculator(getConfiguration());
	}

	/**
	 * Return a traversal that includes the resources that are visible
	 * in the sync view.
	 * @param element the selected element
	 * @return a set of traversals that cover the visible resources.
	 */
	private ResourceTraversal[] getTraversals(TreePath path, IProgressMonitor monitor) throws CoreException {
		if (path.getSegmentCount() > 0) {
			DiffChangeSet set = getChangeSet(path);
			Object o = path.getLastSegment();
			if (set != null) {
				if (path.getSegmentCount() == 1) {
					return new ResourceTraversal[] { new ResourceTraversal(set.getResources(), IResource.DEPTH_ZERO, IResource.NONE) };
				}
				if (o instanceof IResource) {
					IResource resource = (IResource) o;
					int depth = getTraversalCalculator().getLayoutDepth(resource, path);
					IDiff[] diffs = set.getDiffTree().getDiffs(resource, depth);
					Set resources = new HashSet();
					for (int i = 0; i < diffs.length; i++) {
						IDiff diff = diffs[i];
						IResource r = ResourceDiffTree.getResourceFor(diff);
						if (r != null)
							resources.add(r);
					}
					return new ResourceTraversal[] { new ResourceTraversal((IResource[]) resources.toArray(new IResource[resources.size()]), IResource.DEPTH_ZERO, IResource.NONE) };
				}
			}
			if (getTraversalCalculator().isResourcePath(path)) {
				IResource resource = (IResource) o;
				return getTraversalCalculator().getTraversals(resource, path);
			}
			return getTraversals(o, monitor);
		}
		return null;
	}

	private DiffChangeSet getChangeSet(TreePath path) {
		Object o = path.getFirstSegment();
		if (o instanceof DiffChangeSet) {
			return (DiffChangeSet) o;
		}
		return null;
	}

}
