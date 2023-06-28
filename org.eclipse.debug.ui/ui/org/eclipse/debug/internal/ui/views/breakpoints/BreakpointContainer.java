/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Patrick Chuong (Texas Instruments) - Improve usability of the breakpoint view (Bug 238956)
 *****************************************************************/
package org.eclipse.debug.internal.ui.views.breakpoints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointOrganizer;
import org.eclipse.debug.internal.ui.breakpoints.provisional.OtherBreakpointCategory;
import org.eclipse.debug.internal.ui.model.elements.ElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * This class contains the list of container or a list of breakpoint, elements are sorted according to rules
 * in the comparator.
 */
public class BreakpointContainer extends ElementContentProvider implements IAdaptable, IBreakpointContainer {
	/**
	 * Child breakpoints - inserting new element into this collection should use the insertBreakpoint method
	 */
	final private List<IBreakpoint> fBreakpoints = new ArrayList<>();

	/**
	 * Child containers - inserting new element into this container should use the insertChildContainer method
	 */
	final private List<BreakpointContainer> fChildContainers = new ArrayList<>();

	/**
	 * The category for this container
	 */
	private IAdaptable fCategory;

	/**
	 * The breakpoint organizer for this container
	 */
	private IBreakpointOrganizer fOrganizer;

	/**
	 * The nested breakpoint organizer
	 */
	private IBreakpointOrganizer[] fNesting;

	/**
	 * A flag to indicate this is the default container
	 */
	private boolean fDefaultContainer;

	/**
	 * Parent container
	 */
	private BreakpointContainer fParent;

	/**
	 * The comparator, will be use to compare the order for inserting new element into this container
	 */
	private ElementComparator fComparator;

	/**
	 * Constructor, intended to be call when creating the root container.
	 *
	 * @param organizers the breakpoint organizer for this container
	 * @param comparator the element comparator, can be <code>null</code>. If <code>null</code> than new element
	 * will be added to the end of the list.
	 */
	public BreakpointContainer(IBreakpointOrganizer[] organizers, ElementComparator comparator) {
		fNesting = organizers;
		fComparator = comparator;
	}

	/**
	 * Constructor, intended to be call within this class only.
	 *
	 * @param parent the parent breakpoint container
	 * @param category the category for this container
	 * @param organizer the organizer for this container
	 * @param comparator the element comparator
	 * @param nesting the nested breakpoint organizer
	 */
	private BreakpointContainer(BreakpointContainer parent, IAdaptable category, IBreakpointOrganizer organizer,
			ElementComparator comparator, IBreakpointOrganizer[] nesting) {
		this(category, organizer, nesting);
		fParent = parent;
		fComparator = comparator;
	}

	/**
	 * Constructor, intended to be call when reorganizing the content.
	 *
	 * @param category the breakpoint category
	 * @param organizer the breakpoint organizer
	 * @param nesting the nested breakpoint organizer
	 */
	BreakpointContainer(IAdaptable category, IBreakpointOrganizer organizer, IBreakpointOrganizer[] nesting) {
		fCategory = category;
		fOrganizer = organizer;
		fNesting = nesting;
	}

	/**
	 * Initialize the default containers.
	 *
	 * @param parentDelta the parent delta, addition child delta will be added to the parent
	 */
	public void initDefaultContainers(ModelDelta parentDelta) {
		// seed with all nested categories
		if (fNesting != null && fNesting.length > 0) {
			IAdaptable[] emptyCategories = fNesting[0].getCategories();
			if (emptyCategories != null) {
				for (IAdaptable category : emptyCategories) {
					BreakpointContainer container = findExistingContainer(fChildContainers, category);
					if (container == null) {
						IBreakpointOrganizer[] siblings = new IBreakpointOrganizer[fNesting.length - 1];
						System.arraycopy(fNesting, 1, siblings, 0, siblings.length);
						container = new BreakpointContainer(this, category, fNesting[0], fComparator, siblings);
						insertChildContainer(container);
						container.fDefaultContainer = true;

						int size = container.getChildren().length;
						parentDelta.addNode(container, fChildContainers.indexOf(container), IModelDelta.INSTALL|IModelDelta.ADDED|IModelDelta.EXPAND, size);

					}
				}
			}
		}
	}

	/**
	 * Insert the breakpoint to this container.
	 *
	 * @param breakpoint the new breakpoint
	 * @return the index of the breakpoint in the cache, -1 if the breakpoint already exist
	 */
	private int insertBreakpoint(IBreakpoint breakpoint) {
		if (fBreakpoints.contains(breakpoint) || breakpoint == null) {
			return -1;
		}
		int index = fBreakpoints.size();
		for (; fComparator != null && index > 0; index--) {
			if (fComparator.compare(fBreakpoints.get(index-1), breakpoint) < 0) {
				break;
			}
		}
		if (index < 0) {
			index = 0;
		}
		fBreakpoints.add(index, breakpoint);
		return index;
	}

	/**
	 * Insert the child container this container.
	 *
	 * @param container the child container
	 * @return the index of the container in the cache, -1 if the child container already exist
	 */
	private int insertChildContainer(BreakpointContainer container) {
		int index = fChildContainers.size();
		for (; fComparator != null && index > 0; index--) {
			if (fComparator.compare(fChildContainers.get(index-1), container) < 0) {
				break;
			}
		}

		if (index < 0) {
			index = 0;
		}
		fChildContainers.add(index, container);

		return index;
	}


	/**
	 * Returns the element comparator.
	 *
	 * @return the element comparator
	 */
	public ElementComparator getElementComparator() {
		return fComparator;
	}

	/**
	 * Returns the parent container, can be <code>null</code>.
	 *
	 * @return the parent container
	 */
	public BreakpointContainer getParent() {
		return fParent;
	}

	/**
	 * Determine whether there is any nested container.
	 *
	 * @return true if has nested container
	 */
	private boolean hasNesting() {
		return fNesting != null && fNesting.length > 0;
	}

	/**
	 * Get the categories for the breakpoint with the given organizer.
	 *
	 * @param breakpoint the breakpoint
	 * @param organizer the organizer
	 * @return the categories
	 */
	private static IAdaptable[] getCategories(IBreakpoint breakpoint, IBreakpointOrganizer organizer) {
		IAdaptable[] categories = organizer.getCategories(breakpoint);
		if (categories == null || categories.length == 0) {
			categories = OtherBreakpointCategory.getCategories(organizer);
		}
		return categories;
	}

	/**
	 * Find existing breakpoint container in the container array the given category.
	 *
	 * @param containers the container array
	 * @param category the category
	 * @return the breakpoint container, can be <code>null</code>.
	 */
	private static BreakpointContainer findExistingContainer(List<BreakpointContainer> containers, IAdaptable category) {
		for (BreakpointContainer c : containers) {
			if (category.equals(c.getCategory())) {
				return c;
			}
		}
		return null;
	}

	// TODO [pchuong]: can be remove if BreakpointsContentProvider no longer uses this class
	void addBreakpoint(IBreakpoint breakpoint) {
		addBreakpoint(breakpoint, new ModelDelta(null, IModelDelta.NO_CHANGE));
	}

	/**
	 * Add a breakpoint to the container, additional delta will be added to the root delta.
	 *
	 * @param breakpoint the breakpoint to added
	 * @param rootDelta the root delta of this container
	 * @see #removeBreakpoint
	 */
	public void addBreakpoint(IBreakpoint breakpoint, ModelDelta rootDelta) {
		final int bpIndex = insertBreakpoint(breakpoint);
		if (bpIndex < 0) {
			return;
		}

		if (hasNesting()) {
			IBreakpointOrganizer organizer = fNesting[0];

			// get the breakpoint categories from the organizer
			for (IAdaptable category : getCategories(breakpoint, organizer)) {
				ModelDelta childDelta = null;
				BreakpointContainer container = findExistingContainer(fChildContainers, category);

				// create a new container if it doesn't exist
				if (container == null) {
					IBreakpointOrganizer[] nesting = null;
					if (fNesting.length > 1) {
						nesting = new IBreakpointOrganizer[fNesting.length - 1];
						System.arraycopy(fNesting, 1, nesting, 0, nesting.length);
					}
					container = new BreakpointContainer(this, category, organizer, fComparator, nesting);
					insertChildContainer(container);
					childDelta = rootDelta.addNode(container, fChildContainers.indexOf(container), IModelDelta.INSERTED|IModelDelta.INSTALL, -1);

				} else {
					childDelta = rootDelta.addNode(container, fChildContainers.indexOf(container), IModelDelta.STATE, -1);
				}

				container.addBreakpoint(breakpoint, childDelta);
				childDelta.setChildCount(container.getChildren().length);
			}

		} else {
			// TODO [pchuong]: There seems to be some kind of problem when the INSERTED flag is used,
			//				   there is a additional checkbox added to the end of the tree.
			//				   Also the tree seems to have a strange visual effect when using the INSERTED
			//				   flag for the child node instead of ADDED flag. Note: all breakpoint delta
			//				   is using the ADDED flag in this class.
			rootDelta.addNode(breakpoint, bpIndex, IModelDelta.ADDED|IModelDelta.INSTALL, 0);
			// rootDelta.addNode(breakpoint, bpIndex, IModelDelta.INSERTED|IModelDelta.INSTALL, 0);

			rootDelta.setFlags(rootDelta.getFlags() | IModelDelta.EXPAND);
		}
	}

	/**
	 * Remove a breakpoint from the container, additional delta will be added to the root delta.
	 *
	 * @param breakpoint the breakpoint to remove
	 * @param rootDelta the root delta of this container
	 * @return if the breakpoint was successfully removed
	 * @see #addBreakpoint
	 */
	public boolean removeBreakpoint(IBreakpoint breakpoint, ModelDelta rootDelta) {
		boolean removed = fBreakpoints.remove(breakpoint);

		if (removed) {
			boolean addRemoveBpDelta = getContainers().length == 0;

			Iterator<BreakpointContainer> it = fChildContainers.iterator();
			while (it.hasNext()) {
				BreakpointContainer container = it.next();
				// if the breakpoint contains in the container and it is the only breakpoint,
				// than remove the container from the collection
				if (container.contains(breakpoint)) {
					ModelDelta childDelta = null;
					if ((!container.isDefaultContainer()) && (container.getBreakpoints().length <= 1)) {
						it.remove();
						childDelta = rootDelta.addNode(container, IModelDelta.REMOVED|IModelDelta.UNINSTALL);

					} else {
						childDelta = rootDelta.addNode(container, IModelDelta.STATE);
					}
					// remove the breakpoint from the nested containers
					container.removeBreakpoint(breakpoint, childDelta);
				}
			}

			if (addRemoveBpDelta) {
				rootDelta.addNode(breakpoint, IModelDelta.REMOVED|IModelDelta.UNINSTALL);
			}
		}
		return removed;
	}

	/**
	 * A helper method to copy the organizers between two containers.
	 *
	 * @param destContainer the destination container
	 * @param sourceContainer the source container
	 */
	public static void copyOrganizers(BreakpointContainer destContainer, BreakpointContainer sourceContainer) {
		destContainer.fNesting = sourceContainer.fNesting;
		destContainer.fOrganizer = sourceContainer.fOrganizer;
		destContainer.fCategory = sourceContainer.fCategory;
	}

	/**
	 * A helper method to update the breakpoint cache of the container and it's ancestors.
	 *
	 * @param container the breakpoint container
	 * @param breakpoints the breakpoint to update
	 * @param add true if breakpoint should be added to the cache, otherwise remove the breakpoint from the cache
	 */
	private static void updateSelfAndAncestorsBreakpointCache(BreakpointContainer container, List<IBreakpoint> breakpoints, boolean add) {
		if (container != null) {
			container.fBreakpoints.removeAll(breakpoints);
			if (add) {
				container.fBreakpoints.addAll(breakpoints);
			}
			updateSelfAndAncestorsBreakpointCache(container.getParent(), breakpoints, add);
		}
	}

	/**
	 * A helper method to add a breakpoint to an existing container.
	 *
	 * @param destContainer the destination container
	 * @param breakpoint the breakpoint to add
	 * @param destContainerDelta the destination container delta, additional delta will be added to this delta
	 */
	static public void addBreakpoint(BreakpointContainer destContainer, IBreakpoint breakpoint, ModelDelta destContainerDelta) {
		int index = destContainer.insertBreakpoint(breakpoint);
		Assert.isTrue(index >= 0);

		List<IBreakpoint> breakpoints = destContainer.fBreakpoints;
		destContainerDelta.addNode(breakpoint, index/*breakpoints.indexOf(breakpoint)*/, IModelDelta.ADDED|IModelDelta.INSTALL, 0);
		destContainerDelta.setFlags(destContainerDelta.getFlags() | IModelDelta.EXPAND);

		// add the breakpoints to the parent containers.
		updateSelfAndAncestorsBreakpointCache(destContainer.getParent(), breakpoints, true);
	}

	/**
	 * A helper method to add a child container to an existing container.
	 *
	 * @param destContainer the destination container
	 * @param sourceContainer the source container
	 * @param destContainerDelta the delta of the destination container, additional delta will be added to this delta
	 */
	static public void addChildContainer(BreakpointContainer destContainer, BreakpointContainer sourceContainer, ModelDelta destContainerDelta) {
		destContainer.insertChildContainer(sourceContainer);
		sourceContainer.fParent = destContainer;

		// add the breakpoints to the parent containers.
		List<IBreakpoint> breakpoints = Arrays.asList(sourceContainer.getBreakpoints());
		updateSelfAndAncestorsBreakpointCache(destContainer, breakpoints, true);

		int index = destContainer.fChildContainers.indexOf(sourceContainer);
		int size = sourceContainer.getChildren().length;
		ModelDelta childDelta  = destContainerDelta.addNode(sourceContainer, index, IModelDelta.INSERTED|IModelDelta.INSTALL|IModelDelta.EXPAND, size);

		appendContainerDelta(sourceContainer, childDelta);
	}

	/**
	 * A helper method to append delta to the breakpoint container. This method is used by addContainer only.
	 *
	 * @param container the container to append child delta
	 * @param containerDelta the delta of the breakpoint container, additional delta will be added to this delta
	 */
	static private void appendContainerDelta(BreakpointContainer container, ModelDelta containerDelta) {
		Object[] children = container.getChildren();
		for (int i = 0; i < children.length; ++i) {
			boolean isBreakpoint = children[0] instanceof IBreakpoint;
			int numChild =  isBreakpoint ? 0 : children.length;
			int flag = isBreakpoint ? IModelDelta.ADDED|IModelDelta.INSTALL
					: IModelDelta.INSERTED|IModelDelta.INSTALL|IModelDelta.EXPAND;
			ModelDelta childDelta = containerDelta.addNode(children[i], i, flag, numChild);

			if (children[i] instanceof BreakpointContainer) {
				BreakpointContainer childContainer = (BreakpointContainer) children[i];
				appendContainerDelta(childContainer, childDelta);
			}
		}
	}

	/**
	 * A helper method to remove the breakpoint from the container.
	 *
	 * @param container the container to remove the breakpoint
	 * @param breakpoint the breakpoint to remove
	 * @param containerDelta the delta of the breakpoint container, additional delta will be added to this delta
	 */
	static public void removeBreakpoint(BreakpointContainer container, IBreakpoint breakpoint, ModelDelta containerDelta) {
		container.removeBreakpoint(breakpoint, containerDelta);
		List<IBreakpoint> breakpoints = new ArrayList<>();
		breakpoints.add(breakpoint);
		updateSelfAndAncestorsBreakpointCache(container.getParent(), breakpoints, false);
	}

	/**
	 * Remove all child elements including the given container itself.
	 *
	 * @param container the breakpoint container
	 * @param delta the parent delta
	 */
	static public void removeAll(BreakpointContainer container, ModelDelta delta) {
		BreakpointContainer parent = container.getParent();
		if (parent != null) {
			parent.fChildContainers.remove(container);
			delta = delta.addNode(container, IModelDelta.UNINSTALL|IModelDelta.REMOVED);
		}

		if (container.fChildContainers.isEmpty()) {
			List<IBreakpoint> breakpoints = new ArrayList<>();
			Iterator<IBreakpoint> iterator = container.fBreakpoints.iterator();
			while (iterator.hasNext()) {
				IBreakpoint obj = iterator.next();
				breakpoints.add(obj);
				delta.addNode(obj, IModelDelta.UNINSTALL|IModelDelta.REMOVED);
				iterator.remove();
			}

			// remove the breakpoints from the parent containers.
			updateSelfAndAncestorsBreakpointCache(container.getParent(), breakpoints, false);
			return;
		}

		Iterator<BreakpointContainer> iterator = container.fChildContainers.iterator();
		while (iterator.hasNext()) {
			BreakpointContainer childContainer = iterator.next();
			ModelDelta childDelta = delta.addNode(childContainer, IModelDelta.REMOVED|IModelDelta.UNINSTALL);
			iterator.remove();
			removeAll(childContainer, childDelta);
		}
	}

	/**
	 * Returns whether this is the default container.
	 *
	 * @return true if it is a default container
	 */
	boolean isDefaultContainer() {
		return fDefaultContainer;
	}

	/**
	 * Returns the breakpoints in this container
	 *
	 * @return the breakpoints in this container
	 */
	@Override
	public IBreakpoint[] getBreakpoints() {
		return fBreakpoints.toArray(new IBreakpoint[fBreakpoints.size()]);
	}

	/**
	 * Returns this container's category.
	 *
	 * @return container category
	 */
	@Override
	public IAdaptable getCategory() {
		return fCategory;
	}

	/**
	 * Returns children as breakpoints or nested containers.
	 *
	 * @return children as breakpoints or nested containers
	 */
	public Object[] getChildren() {
		if (fChildContainers.isEmpty()) {
			return getBreakpoints();
		}
		return getContainers();
	}

	/**
	 * Returns the index of the given child element (breakpoint or container.
	 *
	 * @param child Child to calculate index of.
	 * @return index of child
	 */
	public int getChildIndex(Object child) {
		if (fChildContainers.isEmpty()) {
			return fBreakpoints.indexOf(child);
		}
		return fChildContainers.indexOf(child);
	}

	/**
	 * Returns the containers nested in this container, possibly empty.
	 *
	 * @return the containers nested in this container, can be empty.
	 */
	public BreakpointContainer[] getContainers() {
		return fChildContainers.toArray(new BreakpointContainer[fChildContainers.size()]);
	}

	/**
	 * Returns this container's organizer.
	 *
	 * @return this container's organizer
	 */
	@Override
	public IBreakpointOrganizer getOrganizer() {
		return fOrganizer;
	}

	/**
	 * Returns whether this container contains the given breakpoint.
	 *
	 * @param breakpoint the breakpoint to check
	 * @return true if this container contains the given breakpoint
	 */
	@Override
	public boolean contains(IBreakpoint breakpoint) {
		return fBreakpoints.contains(breakpoint);
	}

	/**
	 * Returns the child containers for the given breakpoint.
	 *
	 * @param breakpoint the breakpoint to get containers for
	 * @return child containers
	 */
	public BreakpointContainer[] getContainers(IBreakpoint breakpoint) {
		if (contains(breakpoint)) {
			BreakpointContainer[] containers = getContainers();
			if (containers.length == 0) {
				return new BreakpointContainer[]{this};
			}
			ArrayList<BreakpointContainer> list = new ArrayList<>();
			for (BreakpointContainer container : containers) {
				BreakpointContainer[] subcontainers = container.getContainers(breakpoint);
				if (subcontainers != null) {
					Collections.addAll(list, subcontainers);
				}
			}
			return list.toArray(new BreakpointContainer[list.size()]);
		}
		return new BreakpointContainer[0];
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BreakpointContainer) {
			BreakpointContainer container = (BreakpointContainer) obj;
			// With Group by "Advanced" the same category can contain a different subset of breakpoints,
			// therefore to have the same category is not enough to be equal.
			if (! (fParent != null && container.fParent != null && fParent.equals(container.fParent) ||
					fParent == null && container.fParent == null) ) {
				return false;
			}
			if (getCategory() != null && container.getCategory() != null) {
				return getCategory().equals(container.getCategory());
			} else {
				return true;
			}
		}
		return super.equals(obj);
	}

	@Override
	protected int getChildCount(Object element, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		return getChildren().length;
	}

	@Override
	protected Object[] getChildren(Object parent, int index, int length, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		return getElements(getChildren(), index, length);
	}

	@Override
	protected boolean supportsContextId(String id) {
		return id.equals(IDebugUIConstants.ID_BREAKPOINT_VIEW);
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
}
