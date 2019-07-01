/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.ccvs.core.mapping.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.subscriber.CVSChangeSetCollector;
import org.eclipse.team.internal.core.subscribers.*;
import org.eclipse.team.internal.core.subscribers.BatchingChangeSetManager.CollectorChangeEvent;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.ResourceModelContentProvider;
import org.eclipse.team.internal.ui.mapping.ResourceModelLabelProvider;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.internal.ui.synchronize.IChangeSetProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.navigator.*;

public class ChangeSetContentProvider extends ResourceModelContentProvider implements ITreePathContentProvider {

	private final class CollectorListener implements IChangeSetChangeListener, BatchingChangeSetManager.IChangeSetCollectorChangeListener {
		@Override
		public void setAdded(final ChangeSet set) {
			// We only react here for active change sets.
			// Checked-in change set changes are batched
			if (set instanceof ActiveChangeSet) {
				if (isVisibleInMode(set)) {
					Utils.syncExec((Runnable) () -> {
						Object input = getViewer().getInput();
						((AbstractTreeViewer) getViewer()).add(input, set);
					}, (StructuredViewer)getViewer());
				}
				handleSetAddition(set);
			}
		}

		private void handleSetAddition(final ChangeSet set) {
			getUnassignedSet().remove(set.getResources());
		}

		@Override
		public void defaultSetChanged(final ChangeSet previousDefault, final ChangeSet set) {
			if (isVisibleInMode(set) || isVisibleInMode(previousDefault)) {
				Utils.asyncExec((Runnable) () -> {
					if (set == null) {
						// unset default changeset
						((AbstractTreeViewer) getViewer()).update(previousDefault, null);
					} else if (previousDefault != null) {
						((AbstractTreeViewer) getViewer()).update(new Object[] { previousDefault, set }, null);
					} else {
						// when called for the first time previous default change set is null
						((AbstractTreeViewer) getViewer()).update(set, null);
					}
				}, (StructuredViewer)getViewer());
			}
		}

		@Override
		public void setRemoved(final ChangeSet set) {
			// We only react here for active change sets.
			// Checked-in change set changes are batched
			if (set instanceof ActiveChangeSet) {
				if (isVisibleInMode(set)) {
					Utils.syncExec((Runnable) () -> ((AbstractTreeViewer) getViewer())
							.remove(TreePath.EMPTY.createChildPath(set)), (StructuredViewer) getViewer());
				}
				handleSetRemoval(set);
			}
		}

		private void handleSetRemoval(final ChangeSet set) {
			IResource[] resources = set.getResources();
			List toAdd = new ArrayList();
			for (IResource resource : resources) {
				IDiff diff = getContext().getDiffTree().getDiff(resource);
				if (diff != null && !isContainedInSet(diff))
					toAdd.add(diff);
			}
			getUnassignedSet().add((IDiff[]) toAdd.toArray(new IDiff[toAdd.size()]));
		}

		@Override
		public void nameChanged(final ChangeSet set) {
			if (isVisibleInMode(set)) {
				Utils.asyncExec((Runnable) () -> ((AbstractTreeViewer) getViewer()).update(set, null),
						(StructuredViewer) getViewer());
			}
		}

		@Override
		public void resourcesChanged(final ChangeSet set, final IPath[] paths) {
			// We only react here for active change sets.
			// Checked-in change set changes are batched
			if (set instanceof ActiveChangeSet) {
				if (isVisibleInMode(set)) {
					Utils.syncExec((Runnable) () -> {
						if (hasChildrenInContext(set))
							if (getVisibleSetsInViewer().contains(set))
								((AbstractTreeViewer) getViewer()).refresh(set, true);
							else
								((AbstractTreeViewer) getViewer()).add(getViewer().getInput(), set);
						else
							((AbstractTreeViewer) getViewer()).remove(set);
					}, (StructuredViewer)getViewer());
				}
				handleSetChange(set, paths);
			}
		}

		private void handleSetChange(final ChangeSet set, final IPath[] paths) {
			try {
				getTheRest().beginInput();
				for (IPath path : paths) {
					boolean isContained = ((DiffChangeSet)set).contains(path);
					if (isContained) {
						IDiff diff = ((DiffChangeSet)set).getDiffTree().getDiff(path);
						if (diff != null) {
							getTheRest().remove(ResourceDiffTree.getResourceFor(diff));
						}
					} else {
						IDiff diff = getContext().getDiffTree().getDiff(path);
						if (diff != null && !isContainedInSet(diff)) {
							getTheRest().add(diff);
						}
					}   
				}
			} finally {
				getTheRest().endInput(null);
			}
		}

		@Override
		public void changeSetChanges(final CollectorChangeEvent event, IProgressMonitor monitor) {
			ChangeSet[] addedSets = event.getAddedSets();
			final Object[] visibleAddedSets = getVisibleSets(addedSets);
			ChangeSet[] removedSets = event.getRemovedSets();
			final Object[] visibleRemovedSets = getVisibleSets(removedSets);
			ChangeSet[] changedSets = event.getChangedSets();
			final ChangeSet[] visibleChangedSets = getVisibleSets(changedSets);
			if (visibleAddedSets.length > 0 || visibleRemovedSets.length > 0 || visibleChangedSets.length > 0) {
				Utils.syncExec((Runnable) () -> {
					try {
						getViewer().getControl().setRedraw(false);
						if (visibleAddedSets.length > 0) {
							Object input = getViewer().getInput();
							((AbstractTreeViewer) getViewer()).add(input, visibleAddedSets);
						}
						if (visibleRemovedSets.length > 0)
							((AbstractTreeViewer) getViewer()).remove(visibleRemovedSets);
						for (ChangeSet set : visibleChangedSets) {
							((AbstractTreeViewer) getViewer()).refresh(set, true);
						}
					} finally {
						getViewer().getControl().setRedraw(true);
					}
				}, (StructuredViewer)getViewer());
			}
			try {
				getTheRest().beginInput();
				for (ChangeSet set : addedSets) {
					handleSetAddition(set);
				}
				if (removedSets.length > 0) {
					// If sets were removed, we reset the unassigned set.
					// We need to do this because it is possible that diffs were 
					// removed from the set before the set itself was removed.
					// See bug 173138
					addAllUnassignedToUnassignedSet();
				}
				for (ChangeSet set : changedSets) {
					IPath[] paths = event.getChangesFor(set);
					if (event.getSource().contains(set)) {
						handleSetChange(set, paths);
					} else {
						try {
							getTheRest().beginInput();
							for (IPath path : paths) {
								IDiff diff = getContext().getDiffTree().getDiff(path);
								if (diff != null && !isContainedInSet(diff))
									getTheRest().add(diff);
							}
						} finally {
							getTheRest().endInput(null);
						}
					}
				}
			} finally {
				getTheRest().endInput(monitor);
			}
		}

		private ChangeSet[] getVisibleSets(ChangeSet[] sets) {
			List result = new ArrayList(sets.length);
			for (ChangeSet set : sets) {
				if (isVisibleInMode(set)) {
					result.add(set);
				}
			}
			return (ChangeSet[]) result.toArray(new ChangeSet[result.size()]);
		}
	}

	private DiffChangeSet unassignedDiffs;
	private boolean firstDiffChange = true;
	
	/*
	 * Listener that reacts to changes made to the active change set collector
	 */
	private IChangeSetChangeListener collectorListener = new CollectorListener();
	
	private IDiffChangeListener diffTreeListener = new IDiffChangeListener() {
	
		@Override
		public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
			// Ignore
		}
	
		boolean isSetVisible(DiffChangeSet set) {
			return getVisibleSetsInViewer().contains(set);
		}
		
		@Override
		public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
			Object input = getViewer().getInput();
			if (input instanceof ChangeSetModelProvider && unassignedDiffs != null && event.getTree() == unassignedDiffs.getDiffTree()) {
				Utils.asyncExec((Runnable) () -> {
					if (unassignedDiffs.isEmpty() || !hasChildren(TreePath.EMPTY.createChildPath(getUnassignedSet()))) {
						((AbstractTreeViewer) getViewer()).remove(unassignedDiffs);
					} else if (!isSetVisible(unassignedDiffs)) {
						Object input1 = getViewer().getInput();
						((AbstractTreeViewer) getViewer()).add(input1, unassignedDiffs);
					} else {
						((AbstractTreeViewer) getViewer()).refresh(unassignedDiffs);
					}
				}, (StructuredViewer)getViewer());
			}
		}
	
	};
	private CheckedInChangeSetCollector checkedInCollector;
	private boolean collectorInitialized;
	
	@Override
	protected String getModelProviderId() {
		return ChangeSetModelProvider.ID;
	}
	
	/* package */ boolean isVisibleInMode(ChangeSet set) {
		final Object input = getViewer().getInput();
		if (input instanceof ChangeSetModelProvider) {
			if (set instanceof ActiveChangeSet) {
				return getConfiguration().getMode() != ISynchronizePageConfiguration.INCOMING_MODE;
			}
			if (set instanceof DiffChangeSet) {
				return getConfiguration().getMode() != ISynchronizePageConfiguration.OUTGOING_MODE;
			}
		}
		return false;
	}

	protected boolean isEnabled() {
		final Object input = getViewer().getInput();
		return (input instanceof ChangeSetModelProvider);
	}
	
	@Override
	public Object[] getElements(Object parent) {
		if (parent instanceof ISynchronizationContext) {
			// Do not show change sets when all models are visible because
			// model providers that override the resource content may cause
			// problems for the change set content provider
			return new Object[0];
		}
		if (parent == getModelProvider()) {
			return getRootElements();
		}
		return super.getElements(parent);
	}

	private Object[] getRootElements() {
		if (!collectorInitialized) {
			initializeCheckedInChangeSetCollector(getChangeSetCapability());
			collectorInitialized = true;
		}
		List result = new ArrayList();
		ChangeSet[] sets = getAllSets();
		for (ChangeSet set : sets) {
			if (hasChildren(TreePath.EMPTY.createChildPath(set)))
				result.add(set);
		}
		if (!getUnassignedSet().isEmpty() && hasChildren(TreePath.EMPTY.createChildPath(getUnassignedSet()))) {
			result.add(getUnassignedSet());
		}
		return result.toArray();
	}

	synchronized DiffChangeSet getUnassignedSet() {
		if (unassignedDiffs == null) {
			unassignedDiffs = new UnassignedDiffChangeSet(
					CVSUIMessages.ChangeSetContentProvider_0);
			unassignedDiffs.getDiffTree().addDiffChangeListener(
					diffTreeListener);
			addAllUnassignedToUnassignedSet();
		}
		return unassignedDiffs;
	}

	private void addAllUnassignedToUnassignedSet() {
		IResourceDiffTree allChanges = getContext().getDiffTree();
		final List diffs = new ArrayList();
		allChanges.accept(ResourcesPlugin.getWorkspace().getRoot().getFullPath(), diff -> {
			if (!isContainedInSet(diff))
				diffs.add(diff);
			return true;
		}, IResource.DEPTH_INFINITE);
		unassignedDiffs.add((IDiff[]) diffs.toArray(new IDiff[diffs.size()]));
	}
	
	private ResourceDiffTree getTheRest() {
		return (ResourceDiffTree)getUnassignedSet().getDiffTree();
	}

	/**
	 * Return whether the given diff is contained in a set other than
	 * the unassigned set.
	 * @param diff the diff
	 * @return whether the given diff is contained in a set other than
	 * the unassigned set
	 */
	protected boolean isContainedInSet(IDiff diff) {
		ChangeSet[] sets = getAllSets();
		for (ChangeSet set : sets) {
			if (set.contains(ResourceDiffTree.getResourceFor(diff))) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected ResourceTraversal[] getTraversals(
			ISynchronizationContext context, Object object) {
		if (object instanceof ChangeSet) {
			ChangeSet set = (ChangeSet) object;
			IResource[] resources = set.getResources();
			return new ResourceTraversal[] { new ResourceTraversal(resources, IResource.DEPTH_ZERO, IResource.NONE) };
		}
		return super.getTraversals(context, object);
	}

	@Override
	public Object[] getChildren(TreePath parentPath) {
		if (!isEnabled())
			return new Object[0];
		if (parentPath.getSegmentCount() == 0)
			return getRootElements();
		Object first = parentPath.getFirstSegment();
		if (!isVisibleInMode(first)) {
			return new Object[0];
		}
		IResourceDiffTree diffTree;
		Object parent = parentPath.getLastSegment();
		if (first instanceof DiffChangeSet) {
			DiffChangeSet set = (DiffChangeSet) first;
			diffTree = set.getDiffTree();
			if (parent instanceof DiffChangeSet) {
				parent = getModelRoot();
			}
		} else {
			return new Object[0];
		}
		Object[] children = getChildren(parent);
		Set result = new HashSet();
		for (Object child : children) {
			if (isVisible(child, diffTree)) {
				result.add(child);
			}
		}
		return result.toArray();
	}

	private boolean isVisibleInMode(Object first) {
		if (first instanceof ChangeSet) {
			ChangeSet cs = (ChangeSet) first;
			int mode = getConfiguration().getMode();
			switch (mode) {
			case ISynchronizePageConfiguration.BOTH_MODE:
				return true;
			case ISynchronizePageConfiguration.CONFLICTING_MODE:
				return containsConflicts(cs);
			case ISynchronizePageConfiguration.INCOMING_MODE:
				return cs instanceof CVSCheckedInChangeSet || (isUnassignedSet(cs) && hasIncomingChanges(cs));
			case ISynchronizePageConfiguration.OUTGOING_MODE:
				return cs instanceof ActiveChangeSet || hasConflicts(cs) || (isUnassignedSet(cs) && hasOutgoingChanges(cs));
			default:
				break;
			}
		}
		return true;
	}

	private boolean hasIncomingChanges(ChangeSet cs) {
		if (cs instanceof DiffChangeSet) {
			DiffChangeSet dcs = (DiffChangeSet) cs;
			return dcs.getDiffTree().countFor(IThreeWayDiff.INCOMING, IThreeWayDiff.DIRECTION_MASK) > 0;
		}
		return false;
	}

	private boolean hasOutgoingChanges(ChangeSet cs) {
		if (cs instanceof DiffChangeSet) {
			DiffChangeSet dcs = (DiffChangeSet) cs;
			return dcs.getDiffTree().countFor(IThreeWayDiff.OUTGOING, IThreeWayDiff.DIRECTION_MASK) > 0;
		}
		return false;
	}

	private boolean isUnassignedSet(ChangeSet cs) {
		return cs == unassignedDiffs;
	}

	private boolean hasConflicts(ChangeSet cs) {
		if (cs instanceof DiffChangeSet) {
			DiffChangeSet dcs = (DiffChangeSet) cs;
			return dcs.getDiffTree().countFor(IThreeWayDiff.CONFLICTING, IThreeWayDiff.DIRECTION_MASK) > 0;
		}
		return false;
	}

	private boolean containsConflicts(ChangeSet cs) {
		if (cs instanceof DiffChangeSet) {
			DiffChangeSet dcs = (DiffChangeSet) cs;
			return dcs.getDiffTree().hasMatchingDiffs(ResourcesPlugin.getWorkspace().getRoot().getFullPath(), ResourceModelLabelProvider.CONFLICT_FILTER);
		}
		return false;
	}

	private boolean isVisible(Object object, IResourceDiffTree tree) {
		if (object instanceof IResource) {
			IResource resource = (IResource) object;
			IDiff diff = tree.getDiff(resource);
			if (diff != null && isVisible(diff))
				return true;
			int depth = getTraversalCalculator().getLayoutDepth(resource, null);
			IDiff[] diffs = tree.getDiffs(resource, depth);
			for (IDiff child : diffs) {
				if (isVisible(child)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean hasChildren(TreePath path) {
		if (path.getSegmentCount() == 1) {
			Object first = path.getFirstSegment();
			if (first instanceof ChangeSet) {
				return isVisibleInMode(first) && hasChildrenInContext((ChangeSet)first);
			}
		}
		return getChildren(path).length > 0;
	}

	private boolean hasChildrenInContext(ChangeSet set) {
		IResource[] resources = set.getResources();
		for (IResource resource : resources) {
			if (getContext().getDiffTree().getDiff(resource) != null)
				return true;
		}
		return false;
	}

	@Override
	public TreePath[] getParents(Object element) {
		if (element instanceof ChangeSet) {
			return new TreePath[] { TreePath.EMPTY };
		}
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			DiffChangeSet[] sets = getSetsContaining(resource);
			if (sets.length > 0) {
				List result = new ArrayList();
				for (DiffChangeSet set : sets) {
					TreePath path = getPathForElement(set, resource.getParent());
					if (path != null)
						result.add(path);
				}
				return (TreePath[]) result.toArray(new TreePath[result.size()]);
			} else {
				TreePath path = getPathForElement(getUnassignedSet(), resource.getParent());
				if (path != null)
					return new TreePath[] { path };
			}
		}
		
		return new TreePath[0];
	}

	private DiffChangeSet[] getSetsContaining(IResource resource) {
		List result = new ArrayList();
		DiffChangeSet[] allSets = getAllSets();
		for (DiffChangeSet set : allSets) {
			if (isVisible(resource, set.getDiffTree())) {
				result.add(set);
			}
		}
		return (DiffChangeSet[]) result.toArray(new DiffChangeSet[result.size()]);
	}

	/**
	 * Return all the change sets (incoming and outgoing). This 
	 * list must not include the unassigned set. 
	 * @return all the change sets (incoming and outgoing)
	 */
	private DiffChangeSet[] getAllSets() {
		List result = new ArrayList();
		ChangeSetCapability csc = getChangeSetCapability();
		if (csc.supportsActiveChangeSets()) {
			ActiveChangeSetManager collector = csc.getActiveChangeSetManager();
			ChangeSet[] sets = collector.getSets();	
			Collections.addAll(result, sets);
		}
		if (checkedInCollector != null) {
			ChangeSet[] sets = checkedInCollector.getSets();	
			Collections.addAll(result, sets);
		}
		return (DiffChangeSet[]) result.toArray(new DiffChangeSet[result.size()]);
	}

	private TreePath getPathForElement(DiffChangeSet set, IResource resource) {
		List pathList = getPath(set.getDiffTree(), resource);
		if (pathList != null) {
			pathList.add(0, set);
			TreePath path = new TreePath(pathList.toArray());
			return path;
		}
		return null;
	}
	
	private List getPath(IResourceDiffTree tree, IResource resource) {
		if (resource == null)
			return null;
		boolean hasDiff = tree.getDiff(resource) == null;
		if (hasDiff && tree.members(resource).length == 0)
			return null;
		if (resource.getType() == IResource.ROOT) {
			return null;
		}
		List result = new ArrayList();
		result.add(resource.getProject());
		if (resource.getType() != IResource.PROJECT) {
			String layout = getTraversalCalculator().getLayout();
			if (layout.equals(IPreferenceIds.FLAT_LAYOUT)) {
				result.add(resource);
			} else if (layout.equals(IPreferenceIds.COMPRESSED_LAYOUT) && resource.getType() == IResource.FOLDER) {
				result.add(resource);
			} else if (layout.equals(IPreferenceIds.COMPRESSED_LAYOUT) && resource.getType() == IResource.FILE) {
				IContainer parent = resource.getParent();
				if (parent.getType() != IResource.PROJECT)
					result.add(parent);
				result.add(resource);
			} else {
				List resourcePath = new ArrayList();
				IResource next = resource;
				while (next.getType() != IResource.PROJECT) {
					resourcePath.add(next);
					next = next.getParent();
				}
				for (int i = resourcePath.size() - 1; i >=0; i--) {
					result.add(resourcePath.get(i));
				}
			}
		}
		return result;
	}

	@Override
	public void init(ICommonContentExtensionSite site) {
		super.init(site);
		ChangeSetCapability csc = getChangeSetCapability();
		if (csc.supportsActiveChangeSets()) {
			ActiveChangeSetManager collector = csc.getActiveChangeSetManager();
			collector.addListener(collectorListener);
		}
		ChangeSetSorter sorter = getSorter();
		if (sorter != null) {
			sorter.setConfiguration(getConfiguration());
		}
	}

	private ChangeSetSorter getSorter() {
		INavigatorContentService contentService = getExtensionSite().getService();
		INavigatorSorterService sortingService = contentService.getSorterService();
		INavigatorContentExtension extension = getExtensionSite().getExtension();
		if (extension != null) {
			ViewerSorter sorter = sortingService.findSorter(extension.getDescriptor(), getModelProvider(), new DiffChangeSet(), new DiffChangeSet());
			if (sorter instanceof ChangeSetSorter) {
				return (ChangeSetSorter) sorter;
			}
		}
		return null;
	}
	
	private void initializeCheckedInChangeSetCollector(ChangeSetCapability csc) {
		if (csc.supportsCheckedInChangeSets()) {
			checkedInCollector = ((ModelParticipantChangeSetCapability)csc).createCheckedInChangeSetCollector(getConfiguration());
			getConfiguration().setProperty(CVSChangeSetCollector.CVS_CHECKED_IN_COLLECTOR, checkedInCollector);
			checkedInCollector.addListener(collectorListener);
			checkedInCollector.add(((ResourceDiffTree)getContext().getDiffTree()).getDiffs());
		}
	}
	
	@Override
	public void dispose() {
		ChangeSetCapability csc = getChangeSetCapability();
		if (csc.supportsActiveChangeSets()) {
			csc.getActiveChangeSetManager().removeListener(collectorListener);
		}
		if (checkedInCollector != null) {
			checkedInCollector.removeListener(collectorListener);
			checkedInCollector.dispose();
		}
		if (unassignedDiffs != null) {
			unassignedDiffs.getDiffTree().removeDiffChangeListener(diffTreeListener);
		}
		super.dispose();
	}
	
	@Override
	public boolean isVisible(IDiff diff) {
		return super.isVisible(diff);
	}

	public IResourceDiffTree getDiffTree(TreePath path) {
		if (path.getSegmentCount() > 0) {
			Object first = path.getFirstSegment();
			if (first instanceof DiffChangeSet) {
				DiffChangeSet set = (DiffChangeSet) first;
				return set.getDiffTree();
			}
		}
		return getTheRest();
	}
	
	@Override
	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		// Override inherited method to reconcile sub-trees
		IPath[] removed = event.getRemovals();
		IDiff[] added = event.getAdditions();
		IDiff[] changed = event.getChanges();
		// Only adjust the set of the rest. The others will be handled by the collectors
		try {
			getTheRest().beginInput();
			for (IPath path : removed) {
				getTheRest().remove(path);
			}
			for (IDiff diff : added) {
				// Only add the diff if it is not already in another set
				if (!isContainedInSet(diff)) {
					getTheRest().add(diff);
				}
			}
			for (IDiff diff : changed) {
				// Only add the diff if it is already contained in the free set
				if (getTheRest().getDiff(diff.getPath()) != null) {
					getTheRest().add(diff);
				}
			}
		} finally {
			getTheRest().endInput(monitor);
		}
		if (checkedInCollector != null)
			checkedInCollector.handleChange(event);
		if (firstDiffChange) {
			// One the first diff event, refresh the viewer to ensure outgoing change sets appear
			firstDiffChange = false;
			Utils.asyncExec((Runnable) () -> ((AbstractTreeViewer) getViewer()).refresh(),
					(StructuredViewer) getViewer());
		}
	}
	
	@Override
	protected void updateLabels(ISynchronizationContext context, IPath[] paths) {
		super.updateLabels(context, paths);
		ChangeSet[] sets = getSetsShowingPropogatedStateFrom(paths);
		if (sets.length > 0)
			((AbstractTreeViewer)getViewer()).update(sets, null);
	}
	
	
	private ChangeSet[] getSetsShowingPropogatedStateFrom(IPath[] paths) {
		Set result = new HashSet();
		for (IPath path : paths) {
			ChangeSet[] sets = getSetsShowingPropogatedStateFrom(path);
			Collections.addAll(result, sets);
		}
		return (ChangeSet[]) result.toArray(new ChangeSet[result.size()]);
	}
	
	protected DiffChangeSet[] getSetsShowingPropogatedStateFrom(IPath path) {
		List result = new ArrayList();
		DiffChangeSet[] allSets = getAllSets();
		for (DiffChangeSet set : allSets) {
			if (set.getDiffTree().getDiff(path) != null || set.getDiffTree().getChildren(path).length > 0) {
				result.add(set);
			}
		}
		return (DiffChangeSet[]) result.toArray(new DiffChangeSet[result.size()]);
	}

	public ChangeSetCapability getChangeSetCapability() {
		ISynchronizeParticipant participant = getConfiguration().getParticipant();
		if (participant instanceof IChangeSetProvider) {
			IChangeSetProvider provider = (IChangeSetProvider) participant;
			return provider.getChangeSetCapability();
		}
		return null;
	}
	
	private Set getVisibleSetsInViewer() {
		TreeViewer viewer = (TreeViewer)getViewer();
		Tree tree = viewer.getTree();
		TreeItem[] children = tree.getItems();
		Set result = new HashSet();
		for (TreeItem control : children) {
			Object data = control.getData();
			if (data instanceof ChangeSet) {
				ChangeSet set = (ChangeSet) data;
				result.add(set);
			}
		}
		return result;
	}

}
