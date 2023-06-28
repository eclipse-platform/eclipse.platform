/*****************************************************************
 * Copyright (c) 2009, 2013 Texas Instruments and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Chuong (Texas Instruments) - Initial API and implementation (Bug 238956)
 *     IBM Corporation - ongoing enhancements and bug fixing
 *     Wind River Systems - ongoing enhancements and bug fixing
 *     Wind River System (Randy Rohrbach) - non standard model breakpoint filtering (Bug 333517)
 *****************************************************************/
package org.eclipse.debug.internal.ui.model.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointsListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointOrganizer;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointUIConstants;
import org.eclipse.debug.internal.ui.elements.adapters.DefaultBreakpointsViewInput;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.update.BreakpointManagerProxy;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointContainer;
import org.eclipse.debug.internal.ui.views.breakpoints.ElementComparator;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * This class provides breakpoint content for the breakpoint manager.
 *
 * @since 3.6
 */
public class BreakpointManagerContentProvider extends ElementContentProvider
		implements IBreakpointsListener {

	/**
	 * Breakpoint input data. Contains all input specific data.
	 *
	 * @since 3.6
	 */
	private class InputData {
		/**
		 * Breakpoint manager input
		 */
		final private DefaultBreakpointsViewInput fInput;

		/**
		 * Model proxy of the input
		 */
		final private List<BreakpointManagerProxy> fProxies = new ArrayList<>(1);

		/**
		 * Element comparator, use to compare the ordering of elements for the model
		 * <br/> Note: We assume that the comparator does not change.
		 */
		private ElementComparator fComparator;

		/**
		 * The breakpoint root container.<br/>
		 * Note: The final qualifier guarantees that fContainer will be
		 * initialized before the class is accessed on other threads.
		 */
		final private BreakpointContainer fContainer;

		/**
		 * Known current breakpoint organizers.
		 */
		private IBreakpointOrganizer[] fOrganizers;

		private IStructuredSelection fDebugContext = StructuredSelection.EMPTY;

		private IPropertyChangeListener fOrganizersListener = event -> updateContainers();

		private IPropertyChangeListener fPresentationContextListener = this::presentationPropertyChanged;

		private IDebugContextListener fDebugContextListener = InputData.this::debugContextChanged;

		/**
		 * Constructor
		 *
		 * @param input the breakpoint manager input
		 */
		InputData(DefaultBreakpointsViewInput input) {
			fInput = input;
			fComparator = (ElementComparator)
				input.getContext().getProperty(IBreakpointUIConstants.PROP_BREAKPOINTS_ELEMENT_COMPARATOR);

			fOrganizers = (IBreakpointOrganizer[])
				input.getContext().getProperty(IBreakpointUIConstants.PROP_BREAKPOINTS_ORGANIZERS);

			// Create the initial container.
			ModelDelta initialDelta = new ModelDelta(fInput, 0, IModelDelta.NO_CHANGE, -1);
			IBreakpoint[] breakpoints = filterBreakpoints(
				fInput, getSelectionFilter(fInput, getDebugContext()), fBpManager.getBreakpoints());
			fContainer = createRootContainer(initialDelta, fInput, fOrganizers, breakpoints);

			registerOrganizersListener(null, fOrganizers);
			input.getContext().addPropertyChangeListener(fPresentationContextListener);

			IWorkbenchWindow window = fInput.getContext().getWindow();
			if (window != null) {
				IDebugContextService debugContextService = DebugUITools.getDebugContextManager().getContextService(window);
				ISelection debugContext = debugContextService.getActiveContext();
				if (debugContext instanceof IStructuredSelection) {
					synchronized(this) {
						fDebugContext = (IStructuredSelection)debugContext;
					}
				}
				debugContextService.addDebugContextListener(fDebugContextListener);
			}
		}

		void dispose() {
			// Unregister listener to breakpoint organizers.
			IBreakpointOrganizer[] organizers;
			synchronized(this) {
				organizers = fOrganizers;
				fOrganizers = null;
			}
			registerOrganizersListener(organizers, null);

			// Unregister listener to presentation context.
			fInput.getContext().removePropertyChangeListener(fPresentationContextListener);

			// Unregister listener to debug context in window.
			IWorkbenchWindow window = fInput.getContext().getWindow();
			if (window != null) {
				IDebugContextService debugContextService = DebugUITools.getDebugContextManager().getContextService(window);
				debugContextService.removeDebugContextListener(fDebugContextListener);
			}

		}

		void proxyInstalled(BreakpointManagerProxy proxy) {
			ModelDelta rootDelta = null;
			synchronized(this) {
				fProxies.add(proxy);

				// Generate an install delta

				rootDelta = new ModelDelta(fInput, 0, IModelDelta.NO_CHANGE, -1);
				buildInstallDelta(rootDelta, fContainer);

				if (DebugUIPlugin.DEBUG_BREAKPOINT_DELTAS) {
					DebugUIPlugin.trace("PROXY INSTALLED (" + proxy + ")\n"); //$NON-NLS-1$ //$NON-NLS-2$
				}

				proxy.postModelChanged(rootDelta, false);
			}
		}

		synchronized void proxyDisposed(BreakpointManagerProxy proxy) {
			fProxies.remove(proxy);
			if (DebugUIPlugin.DEBUG_BREAKPOINT_DELTAS) {
				DebugUIPlugin.trace("PROXY DISPOSED (" + proxy + ")\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		synchronized BreakpointManagerProxy[] getProxies() {
			return fProxies.toArray(new BreakpointManagerProxy[fProxies.size()]);
		}

		/**
		 * Change the breakpoint organizers for the root container.
		 *
		 * @param organizers the new organizers.
		 */
		void setOrganizers(IBreakpointOrganizer[] organizers) {
			IBreakpointOrganizer[] oldOrganizers = null;
			synchronized(this) {
				oldOrganizers = fOrganizers;
				fOrganizers = organizers;
			}
			registerOrganizersListener(oldOrganizers, organizers);
			updateContainers();
		}

		private void registerOrganizersListener(IBreakpointOrganizer[] oldOrganizers, IBreakpointOrganizer[] newOrganizers) {
			if (oldOrganizers != null) {
				for (IBreakpointOrganizer oldOrganizer : oldOrganizers) {
					oldOrganizer.removePropertyChangeListener(fOrganizersListener);
				}
			}
			if (newOrganizers != null) {
				for (IBreakpointOrganizer newOrganizer : newOrganizers) {
					newOrganizer.addPropertyChangeListener(fOrganizersListener);
				}
			}
		}

		void updateContainers() {
			IBreakpoint[] breakpoints = filterBreakpoints(
				fInput, getSelectionFilter(fInput, getDebugContext()), fBpManager.getBreakpoints());

			synchronized(this) {
				ModelDelta delta = new ModelDelta(fInput, IModelDelta.NO_CHANGE);
				// create a reference container, use for deleting elements and adding elements
				ModelDelta dummyDelta = new ModelDelta(null, IModelDelta.NO_CHANGE);
				BreakpointContainer refContainer = createRootContainer(dummyDelta, fInput, fOrganizers, breakpoints);

				// delete the removed elements
				deleteRemovedElements(fContainer, refContainer, delta);

				// adjust the old organizer with the reference organizer
				BreakpointContainer.copyOrganizers(fContainer, refContainer);

				// insert the added elements
				IBreakpoint newBreakpoint = insertAddedElements(fContainer, refContainer, delta);
				delta.setChildCount(fContainer.getChildren().length);

				// select the new breakpoint
				if (newBreakpoint != null) {
					appendModelDeltaToElement(delta, newBreakpoint, IModelDelta.SELECT);
				}
				if (DebugUIPlugin.DEBUG_BREAKPOINT_DELTAS) {
					DebugUIPlugin.trace("POST BREAKPOINT DELTA (setOrganizers)\n"); //$NON-NLS-1$
				}
				postModelChanged(delta, false);
			}
		}

		void sortContainers() {
			IBreakpoint[] breakpoints = filterBreakpoints(fInput, getSelectionFilter(fInput, getDebugContext()), fBpManager.getBreakpoints());

			synchronized (this) {
				ModelDelta delta = new ModelDelta(fInput, IModelDelta.NO_CHANGE);
				// create a reference container, use for deleting elements and
				// adding elements
				ModelDelta dummyDelta = new ModelDelta(null, IModelDelta.NO_CHANGE);
				BreakpointContainer refContainer = createRootContainer(dummyDelta, fInput, fOrganizers, breakpoints);

				// delete all elements
				deleteAllElements(fContainer, delta);

				// adjust the old organizer with the reference organizer
				BreakpointContainer.copyOrganizers(fContainer, refContainer);

				// insert all elements
				IBreakpoint newBreakpoint = insertAddedElements(fContainer, refContainer, delta);
				delta.setChildCount(fContainer.getChildren().length);

				// select the new breakpoint
				if (newBreakpoint != null) {
					appendModelDeltaToElement(delta, newBreakpoint, IModelDelta.SELECT);
				}
				if (DebugUIPlugin.DEBUG_BREAKPOINT_DELTAS) {
					DebugUIPlugin.trace("POST BREAKPOINT DELTA (setOrganizers)\n"); //$NON-NLS-1$
				}
				postModelChanged(delta, false);
			}
		}

		private synchronized IStructuredSelection getDebugContext() {
			return fDebugContext;
		}

		/**
		 * Handles the property changed events in presentation contexts.
		 * Sub-classes may override to perform additional handling.
		 * @param event the event
		 */
		private void presentationPropertyChanged(PropertyChangeEvent event) {
			if (IBreakpointUIConstants.PROP_BREAKPOINTS_ELEMENT_COMPARATOR_SORT.equals(event.getProperty())) {
				sortContainers();
			} else if (IPresentationContext.PROPERTY_DISPOSED.equals(event.getProperty())) {
				contextDisposed(fInput.getContext());
			}
			if (IBreakpointUIConstants.PROP_BREAKPOINTS_ORGANIZERS.equals(event.getProperty())) {
				IBreakpointOrganizer[] organizers = (IBreakpointOrganizer[])event.getNewValue();
				setOrganizers(organizers);
			}
			else if ( IBreakpointUIConstants.PROP_BREAKPOINTS_FILTER_SELECTION.equals(event.getProperty()) )
			{
				IStructuredSelection selection = null;

				if (Boolean.TRUE.equals(event.getNewValue()) ) {
					selection = getDebugContext();
				}
				setFilterSelection(selection);
			}
			else if ( IBreakpointUIConstants.PROP_BREAKPOINTS_TRACK_SELECTION.equals(event.getProperty()) )
			{
				IStructuredSelection selection = null;

				if (Boolean.TRUE.equals(event.getNewValue()) ) {
					selection = getDebugContext();
				}
				trackSelection(selection);
			}
		}

		private void debugContextChanged(DebugContextEvent event) {
			IStructuredSelection newContext;
			if (event.getContext() instanceof IStructuredSelection) {
				newContext = (IStructuredSelection)event.getContext();
			} else {
				newContext = StructuredSelection.EMPTY;
			}

			synchronized(this) {
				fDebugContext = newContext;
			}

			if (Boolean.TRUE.equals(fInput.getContext().getProperty(IBreakpointUIConstants.PROP_BREAKPOINTS_FILTER_SELECTION)) ) {
				setFilterSelection(newContext);
			}

			if (Boolean.TRUE.equals(fInput.getContext().getProperty(IBreakpointUIConstants.PROP_BREAKPOINTS_TRACK_SELECTION)) ) {
				trackSelection(newContext);
			}
		}


		private void setFilterSelection(IStructuredSelection ss) {
			ModelDelta delta = new ModelDelta(fInput, IModelDelta.NO_CHANGE);
			boolean changed = false;

			// calculate supported breakpoints outside of the synchronized section.
			IBreakpoint[] allBreakpoints = fBpManager.getBreakpoints();
			boolean[] supportedBreakpoints = new boolean[allBreakpoints.length];
			for (int i = 0; i < allBreakpoints.length; ++i) {
				supportedBreakpoints[i] = supportsBreakpoint(ss, allBreakpoints[i]);
			}

			synchronized(this) {
				Set<IBreakpoint> existingBreakpoints = new HashSet<>(Arrays.asList(fContainer.getBreakpoints()));

				// Bug 310879
				// Process breakpoints in two passes: first remove breakpoints, then add new ones.
				// This way the breakpoint counts and indexes will be consistent in the delta.
				for (int i = 0; i < allBreakpoints.length; ++i) {
					if (!supportedBreakpoints[i] && existingBreakpoints.contains(allBreakpoints[i])) {
						fContainer.removeBreakpoint(allBreakpoints[i], delta);
						changed = true;
					}
				}
				for (int i = 0; i < allBreakpoints.length; ++i) {
					if (supportedBreakpoints[i] && !existingBreakpoints.contains(allBreakpoints[i])) {
						fContainer.addBreakpoint(allBreakpoints[i], delta);
						changed = true;
					}
				}

				if (changed) {
					if (DebugUIPlugin.DEBUG_BREAKPOINT_DELTAS) {
						DebugUIPlugin.trace("POST BREAKPOINT DELTA (setFilterSelection)\n"); //$NON-NLS-1$
					}
					postModelChanged(delta, false);
				}
			}
		}


		private void trackSelection(IStructuredSelection selection) {
			if (selection == null || selection.size() != 1) {
				return;
			}

			Iterator<?> iter = selection.iterator();
			Object firstElement = iter.next();
			if (firstElement == null || iter.hasNext()) {
				return;
			}
			IThread thread = null;
			if (firstElement instanceof IStackFrame) {
				thread = ((IStackFrame) firstElement).getThread();
			} else if (firstElement instanceof IThread) {
				thread = (IThread) firstElement;
			} else {
				return;
			}

			IBreakpoint[] breakpoints = thread.getBreakpoints();
			Set<IBreakpoint> bpsSet = new HashSet<>(breakpoints.length * 4 / 3);
			Collections.addAll(bpsSet, breakpoints);

			ModelDelta delta = new ModelDelta(fInput, IModelDelta.NO_CHANGE);
			synchronized (this) {
				if (buildTrackSelectionDelta(delta, fContainer, bpsSet)) {
					if (DebugUIPlugin.DEBUG_BREAKPOINT_DELTAS) {
						DebugUIPlugin.trace("POST BREAKPOINT DELTA (trackSelection)\n"); //$NON-NLS-1$
					}
					for (BreakpointManagerProxy proxy : getProxies()) {
						proxy.postModelChanged(delta, true);
					}
				}
			}

		}

		/**
		 * Recursive function to build the model delta to select a breakpoint
		 * corresponding to the active debug context selection.
		 *
		 * @param delta Delta node to build on
		 * @param container Container element to build delta for.
		 * @param breakpoints Breakpoint set to be selected.
		 * @return whether we found a breakpoint to select
		 */
		private boolean buildTrackSelectionDelta(ModelDelta delta, BreakpointContainer container, Set<IBreakpoint> breakpoints) {
			Object[] children = container.getChildren();
			delta.setChildCount(children.length);
			for (int i = 0; i < children.length; i++) {
				ModelDelta childDelta = delta.addNode(children[i], i, IModelDelta.NO_CHANGE);
				if (children[i] instanceof BreakpointContainer) {
					BreakpointContainer childContainer = (BreakpointContainer)children[i];
					boolean containsBP = false;
					for (IBreakpoint containerBP : childContainer.getBreakpoints()) {
						if (breakpoints.contains(containerBP)) {
							containsBP = true;
							break;
						}
					}
					if (containsBP && buildTrackSelectionDelta(childDelta, childContainer, breakpoints) ) {
						return true;
					}
				} else if (children[i] instanceof IBreakpoint &&
					breakpoints.contains(children[i]))
				{
					childDelta.setFlags(IModelDelta.SELECT | IModelDelta.EXPAND);
					return true;
				}
			}
			return false;
		}

		/**
		 * Helper method to add breakpoints to the given input.
		 *
		 * @param breakpoints the breakpoints
		 */
		void breakpointsAdded(IBreakpoint[] breakpoints) {
			IBreakpoint[] filteredBreakpoints = filterBreakpoints(
				fInput, getSelectionFilter(fInput, getDebugContext()), breakpoints);

			if (filteredBreakpoints.length > 0) {
				synchronized (this) {
					ModelDelta delta = new ModelDelta(fInput, 0, IModelDelta.NO_CHANGE, -1);
					for (IBreakpoint filteredBreakpoint : filteredBreakpoints) {
						// Avoid adding breakpoints which were already removed.  If breakpoints
						// are added and removed very fast, the Breakpoint manager can issue
						// breakpoint added events after breakpoint removed events!  This means
						// that such breakpoints would never be removed from the view.
						// (Bug 289526)
						if (DebugPlugin.getDefault().getBreakpointManager().getBreakpoint(filteredBreakpoint.getMarker()) != null) {
							fContainer.addBreakpoint(filteredBreakpoint, delta);
						}
					}
					delta.setChildCount(fContainer.getChildren().length);

					// select the breakpoint
					if (filteredBreakpoints.length > 0) {
						appendModelDeltaToElement(delta, filteredBreakpoints[0], IModelDelta.SELECT);
					}

					if (DebugUIPlugin.DEBUG_BREAKPOINT_DELTAS) {
						DebugUIPlugin.trace("POST BREAKPOINT DELTA (breakpointsAddedInput)\n"); //$NON-NLS-1$
					}
					postModelChanged(delta, false);
				}
			}
		}

		/**
		 * Helper method to remove breakpoints from a given input.
		 *
		 * @param breakpoints the breakpoints
		 */
		void breakpointsRemoved(IBreakpoint[] breakpoints) {
			synchronized (this) {
				boolean removed = false;
				ModelDelta delta = new ModelDelta(fInput, IModelDelta.NO_CHANGE);
				for (IBreakpoint breakpoint : breakpoints) {
					removed = fContainer.removeBreakpoint(breakpoint, delta) || removed;
				}

				if (removed) {
					if (DebugUIPlugin.DEBUG_BREAKPOINT_DELTAS) {
						DebugUIPlugin.trace("POST BREAKPOINT DELTA (breakpointsRemovedInput)\n"); //$NON-NLS-1$
					}
					postModelChanged(delta, false);
				}
			}
		}

		void breakpointsChanged(IBreakpoint[] breakpoints) {


			IBreakpoint[] filteredBreakpoints = filterBreakpoints(
				fInput, getSelectionFilter(fInput, getDebugContext()), breakpoints);

			synchronized (this) {
				ModelDelta delta = new ModelDelta(fInput, IModelDelta.NO_CHANGE);

				// If the change caused a breakpoint to be added (installed) or remove (un-installed) update accordingly.
				List<IBreakpoint> removed = new ArrayList<>();
				List<IBreakpoint> added = new ArrayList<>();
				List<IBreakpoint> filteredAsList = Arrays.asList(filteredBreakpoints);
				for (IBreakpoint bp : breakpoints) {
					boolean oldContainedBp = fContainer.contains(bp);
					boolean newContained = filteredAsList.contains(bp);
					if (oldContainedBp && !newContained) {
						removed.add(bp);
					} else if (!oldContainedBp && newContained) {
						added.add(bp);
					}
				}
				if (!added.isEmpty()) {
					breakpointsAdded(added.toArray(new IBreakpoint[added.size()]));
				}
				if (!removed.isEmpty()) {
					breakpointsRemoved(removed.toArray(new IBreakpoint[removed.size()]));
				}
				for (IBreakpoint filteredBreakpoint : filteredBreakpoints) {
					appendModelDelta(fContainer, delta, IModelDelta.STATE | IModelDelta.CONTENT, filteredBreakpoint); // content flag triggers detail refresh
				}

				if (DebugUIPlugin.DEBUG_BREAKPOINT_DELTAS) {
					DebugUIPlugin.trace("POST BREAKPOINT DELTA (breakpointsChanged)\n"); //$NON-NLS-1$
				}
				postModelChanged(delta, false);
			}
		}


		/**
		 * Recursive function to build the model delta to install breakpoint
		 * model proxies for all breakpoints and breakpoint containers.
		 *
		 * @param delta Delta node to build on
		 * @param container Container element to build delta for.
		 */
		private void buildInstallDelta(ModelDelta delta, BreakpointContainer container) {
			Object[] children = container.getChildren();
			delta.setChildCount(children.length);
			for (int i = 0; i < children.length; i++) {
				ModelDelta childDelta = delta.addNode(children[i], i, IModelDelta.NO_CHANGE);
				if (children[i] instanceof BreakpointContainer) {
					childDelta.setFlags(IModelDelta.INSTALL);
					buildInstallDelta(childDelta, (BreakpointContainer)children[i]);
				} else if (children[i] instanceof IBreakpoint) {
					childDelta.setFlags(IModelDelta.INSTALL);
				}
			}
		}


		/**
		 * Insert elements from the reference container to an existing container.
		 *
		 * @param container the existing  container to insert the new elements.
		 * @param refContainer the reference container to compare elements that are added.
		 * @param containerDelta the delta of the existing container.
		 * @return the breakpoint that was inserted
		 */
		private IBreakpoint insertAddedElements(BreakpointContainer container, BreakpointContainer refContainer, ModelDelta containerDelta) {
			IBreakpoint newBreakpoint = null;

			Object[] children = container.getChildren();

			for (Object refChildElement : refContainer.getChildren()) {
				Object element = getElement(children, refChildElement);

				// if a child of refContainer doesn't exist in container, than insert it to container
				//      - if the reference child is a container, than copy the reference child container to container
				//      - otherwise (Breakpoint), add the breakpoint to container
				if (element == null) {
					if (refChildElement instanceof BreakpointContainer) {
						BreakpointContainer.addChildContainer(container, (BreakpointContainer) refChildElement, containerDelta);
					} else if(refChildElement instanceof IBreakpoint) {
						BreakpointContainer.addBreakpoint(container, (IBreakpoint) refChildElement, containerDelta);
						if (newBreakpoint == null) {
							newBreakpoint = (IBreakpoint) refChildElement;
						}
					}

				// if a child exist in container, than recursively search into container. And also update the organizer of
				// of container to the one in the refContainer's child.
				} else if (element instanceof BreakpointContainer) {
					ModelDelta childDelta = containerDelta.addNode(element, container.getChildIndex(element), IModelDelta.INSTALL, -1);
					BreakpointContainer.copyOrganizers((BreakpointContainer) element, (BreakpointContainer) refChildElement);
					newBreakpoint = insertAddedElements((BreakpointContainer) element, (BreakpointContainer) refChildElement, childDelta);
					childDelta.setChildCount(((BreakpointContainer) element).getChildren().length);
				}
			}

			return newBreakpoint;
		}



		/**
		 * Delete elements from existing container that doesn't exist in the reference container.
		 *
		 * @param container the existing container to delete the removed elements.
		 * @param refContainer the reference container to compare elements that are removed.
		 * @param containerDelta the delta of the existing container.
		 */
		private void deleteRemovedElements(BreakpointContainer container, BreakpointContainer refContainer, ModelDelta containerDelta) {
			Object[] refChildren = refContainer.getChildren();

			// if a child of container doesn't exist in refContainer, than remove it from container
			for (Object childElement : container.getChildren()) {
				Object element = getElement(refChildren, childElement);

				if (element == null) {
					if (childElement instanceof BreakpointContainer) {
						BreakpointContainer.removeAll((BreakpointContainer) childElement, containerDelta);
					} else {
						BreakpointContainer.removeBreakpoint(container, (IBreakpoint) childElement, containerDelta);
					}
				} else if (element instanceof BreakpointContainer){

					ModelDelta childDelta = containerDelta.addNode(childElement, IModelDelta.STATE);
					deleteRemovedElements((BreakpointContainer) childElement, (BreakpointContainer) element, childDelta);
				}
			}
		}

		private void deleteAllElements(BreakpointContainer container, ModelDelta containerDelta) {
			// Object[] refChildren = refContainer.getChildren();

			// if a child of container doesn't exist in refContainer, than
			// remove it from container
			for (Object childElement :  container.getChildren()) {
				if (childElement instanceof BreakpointContainer) {
						BreakpointContainer.removeAll((BreakpointContainer) childElement, containerDelta);
					} else {
						BreakpointContainer.removeBreakpoint(container, (IBreakpoint) childElement, containerDelta);
					}
			}
		}

		/**
		 * Get the element that is in the collection.
		 *
		 * @param collection the collection of elements.
		 * @param element the element to search.
		 * @return if element exist in collection, than it is returned, otherwise <code>null</code> is returned.
		 * @see #insertAddedElements
		 * @see #deleteRemovedElements
		 */
		private Object getElement(Object[] collection, Object element) {
			for (Object element2 : collection) {
				if (element2 instanceof BreakpointContainer && element instanceof BreakpointContainer) {
					if (element2.equals(element)) {
						return element2;
					}
				} else {
					if (element2.equals(element)) {
						return element2;
					}
				}
			}
			return null;
		}

		/**
		 * Create a root container.
		 *
		 * @param rootDelta the root delta.
		 * @param input the view input.
		 * @param organizers the breakpoint organizers.
		 * @param breakpoints the breakpoints to add to the container
		 * @return the new root container
		 */
		private BreakpointContainer createRootContainer(
			ModelDelta rootDelta, DefaultBreakpointsViewInput input,
			IBreakpointOrganizer[] organizers, IBreakpoint[] breakpoints)
		{

			BreakpointContainer container = new BreakpointContainer(organizers, fComparator);
			container.initDefaultContainers(rootDelta);

			for (IBreakpoint breakpoint : breakpoints) {
				container.addBreakpoint(breakpoint, rootDelta);
			}

			return container;
		}

		/**
		 * Fire model change event for the input.
		 *
		 * @param delta the model delta.
		 * @param select if the viewer selection should change
		 */
		synchronized private void postModelChanged(final IModelDelta delta, boolean select) {
			for (int i = 0; fProxies != null && i < fProxies.size(); i++) {
				fProxies.get(i).postModelChanged(delta, select);
			}
		}


	}

	private class InputDataMap<K, V> extends LinkedHashMap<K, V> {
		private static final long serialVersionUID = 1L;

		public InputDataMap() {
			super(1, (float)0.75, true);
		}

		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<K, V> arg0) {
			InputData data = (InputData)arg0.getValue();
			if (size() > getMaxInputsCache() && data.fProxies.isEmpty()) {
				data.dispose();
				return true;
			}
			return false;
		}
	}

	/**
	 * A map of input to info data cache
	 */
	final private Map<DefaultBreakpointsViewInput, InputData> fInputToData = Collections.synchronizedMap(new InputDataMap<>());

	/**
	 * Flag indicating whether the content provider is currently a breakpoints listener.
	 */
	private boolean fIsBreakpointListener = false;

	/**
	 * The breakpoint manager.
	 */
	final private IBreakpointManager fBpManager = DebugPlugin.getDefault().getBreakpointManager();

	/**
	 * Sub-classes may override this method to filter the breakpoints.
	 *
	 * @param input the breakpoint manager input.
	 * @param selectionFilter the selection to use as filter
	 * @param breakpoints the list of breakpoint to filter.
	 * @return the filtered list of breakpoint based on the input.
	 */
	protected IBreakpoint[] filterBreakpoints(DefaultBreakpointsViewInput input, IStructuredSelection selectionFilter, IBreakpoint[] breakpoints) {
		if (selectionFilter != null && !selectionFilter.isEmpty()) {
			List<IDebugTarget> targets = getDebugTargets(selectionFilter);
			ArrayList<IBreakpoint> retVal = new ArrayList<>();
			if (targets != null) {
				for (IBreakpoint breakpoint : breakpoints) {
					if (supportsBreakpoint(targets, breakpoint)) {
						retVal.add(breakpoint);
					}
				}
			}
			return retVal.toArray(new IBreakpoint[retVal.size()]);
		} else {
			return breakpoints;
		}
	}

	/**
	 * Sub-classes may override this to determine whether the breakpoint is supported by the selection.
	 *
	 * @param ss the selection of the debug elements.
	 * @param breakpoint the breakpoint.
	 * @return true if supported.
	 */
	protected boolean supportsBreakpoint(IStructuredSelection ss, IBreakpoint breakpoint) {
		return supportsBreakpoint(getDebugTargets(ss), breakpoint);
	}

	/**
	 * Returns true if the breakpoint contains in one of the targets.
	 *
	 * @param targets a list of <code>IDebugTarget</code> objects.
	 * @param breakpoint the breakpoint.
	 * @return true if breakpoint contains in the list of targets.
	 */
	protected boolean supportsBreakpoint(List<IDebugTarget> targets, IBreakpoint breakpoint) {
		boolean exist = targets.isEmpty() ? true : false;
		for (int i = 0; !exist && i < targets.size(); ++i) {
			IDebugTarget target = targets.get(i);
			exist |= target.supportsBreakpoint(breakpoint);
		}
		return exist;
	}

	/**
	 * Returns the list of IDebugTarget for the selection.
	 *
	 * @param ss the selection.
	 * @return list of IDebugTarget object.
	 */
	protected List<IDebugTarget> getDebugTargets(IStructuredSelection ss) {
		List<IDebugTarget> debugTargets = new ArrayList<>(2);
		if (ss != null) {
			Iterator<?> i = ss.iterator();
			while (i.hasNext()) {
				Object next = i.next();
				if (next instanceof IDebugElement) {
					debugTargets.add(((IDebugElement)next).getDebugTarget());
				} else if (next instanceof ILaunch) {
					IDebugTarget[] targets = ((ILaunch)next).getDebugTargets();
					Collections.addAll(debugTargets, targets);
				} else if (next instanceof IProcess) {
					IDebugTarget target = ((IProcess)next).getAdapter(IDebugTarget.class);
					if (target != null) {
						debugTargets.add(target);
					}
				} else if (next instanceof IAdaptable) {
					// Allow non-standard debug model element return an IDebugTarget
					// element that could be used for implementing breakpoint filtering.
					// Bug 333517.
					IDebugTarget target = ((IAdaptable)next).getAdapter(IDebugTarget.class);
					if (target != null) {
						debugTargets.add(target);
					}
				}
			}
		}
		return debugTargets;
	}

	/**
	 * Maximum number of breakpoint manager input objects that this provider
	 * will cache data for.  This method is called once upon class creation
	 * when setting up the data cache.  Sub-classes may override to provide
	 * a custom setting.
	 *
	 * @return Maximum data cache size
	 */
	protected int getMaxInputsCache() {
		return 2;
	}

	/**
	 * Handles the event when a presentation context is disposed.
	 * Sub-classes may override to perform additional cleanup.
	 *
	 * @param context Presentation context that was disposed.
	 */
	protected void contextDisposed(IPresentationContext context) {
		List<InputData> removed = new ArrayList<>(1);
		synchronized (fInputToData) {
			for (Iterator<Entry<DefaultBreakpointsViewInput, InputData>> itr = fInputToData.entrySet().iterator(); itr.hasNext();) {
				Map.Entry<DefaultBreakpointsViewInput, InputData> entry = itr.next();
				IPresentationContext entryContext = entry.getKey().getContext();
				if (context.equals(entryContext)) {
					removed.add(entry.getValue());
					itr.remove();
				}
			}
		}

		// Dispose the removed input data
		for (InputData inputData : removed) {
			inputData.dispose();
		}
	}

	/**
	 * Register the breakpoint manager input with this content provider.
	 *
	 * @param input the breakpoint manager input to register.
	 * @param proxy the model proxy of the input.
	 */
	public void registerModelProxy(DefaultBreakpointsViewInput input, BreakpointManagerProxy proxy) {
		synchronized(this) {
			if (!fIsBreakpointListener) {
				fBpManager.addBreakpointListener(this);
				fIsBreakpointListener = true;
			}
		}
		InputData inputData = getInputData(input);
		if (inputData != null) {
			inputData.proxyInstalled(proxy);
		}
	}

	/**
	 * Unregister the breakpoint manager input with this content provider.
	 *
	 * @param input the breakpoint manager input to unregister.
	 * @param proxy the manager proxy
	 */
	public void unregisterModelProxy(DefaultBreakpointsViewInput input, BreakpointManagerProxy proxy) {
		InputData inputData = fInputToData.get(input);
		if (inputData != null) {
			inputData.proxyDisposed(proxy);

			if (fInputToData.isEmpty()) {
				synchronized(this) {
					if (fIsBreakpointListener) {
						fBpManager.removeBreakpointListener(this);
						fIsBreakpointListener = false;
					}
				}
			}
		}
	}

	private InputData getInputData(DefaultBreakpointsViewInput input) {
		if (Boolean.TRUE.equals(input.getContext().getProperty(IPresentationContext.PROPERTY_DISPOSED)) ) {
			return null;
		}

		InputData data = null;
		synchronized (fInputToData) {
			data = fInputToData.get(input);
			if (data == null) {
				data = new InputData(input);
				fInputToData.put(input, data);
			}
		}
		return data;
	}

	/**
	 * Returns the selection filter for the input.
	 *
	 * @param input the selection.
	 * @param debugContext the current context
	 * @return the filtered selection or <code>null</code>
	 */
	protected IStructuredSelection getSelectionFilter(Object input, IStructuredSelection debugContext) {
		if (input instanceof DefaultBreakpointsViewInput) {
			IPresentationContext presentation = ((DefaultBreakpointsViewInput)input).getContext();
			if ( Boolean.TRUE.equals(presentation.getProperty(IBreakpointUIConstants.PROP_BREAKPOINTS_FILTER_SELECTION)) ) {
				return debugContext;
			}
		}
		return null;
	}

	@Override
	protected boolean supportsContextId(String id) {
		return id.equals(IDebugUIConstants.ID_BREAKPOINT_VIEW);
	}

	@Override
	protected int getChildCount(Object element, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		Object input = monitor.getViewerInput();
		if (input instanceof DefaultBreakpointsViewInput) {
			DefaultBreakpointsViewInput bpManagerInput = (DefaultBreakpointsViewInput)input;
			InputData inputData = getInputData(bpManagerInput);
			if (inputData != null) {
				return inputData.fContainer.getChildren().length;
			}
		}
		return 0;
	}

	@Override
	protected Object[] getChildren(Object parent, int index, int length, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		Object input = monitor.getViewerInput();
		if (input instanceof DefaultBreakpointsViewInput) {
			DefaultBreakpointsViewInput bpManagerInput = (DefaultBreakpointsViewInput)input;
			InputData inputData = getInputData(bpManagerInput);
			if (inputData != null) {
				Object[] children =  inputData.fContainer.getChildren();
				return getElements(children, index, length);
			}
		}

		return EMPTY;
	}

	final SerialExecutor breakpointUpdater = new SerialExecutor("Breakpoints View Update Job", this); //$NON-NLS-1$

	void updateBreakpointView(Consumer<InputData> action) {
		breakpointUpdater.schedule(() -> fInputToData.values().forEach(action::accept));
	}

	@Override
	public void breakpointsAdded(final IBreakpoint[] breakpoints) {
		updateBreakpointView(data -> data.breakpointsAdded(breakpoints));
	}

	@Override
	public void breakpointsRemoved(final IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
		updateBreakpointView(data -> data.breakpointsRemoved(breakpoints));
	}

	@Override
	public void breakpointsChanged(final IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
		updateBreakpointView(data -> data.breakpointsChanged(breakpoints));
	}
	/**
	 * Appends the model delta flags to child containers that contains the breakpoint.
	 *
	 * @param parent the parent container.
	 * @param parentDelta the parent model delta.
	 * @param flags the model delta flags.
	 * @param breakpoint the breakpoint to search in the children containers.
	 */
	private void appendModelDelta(BreakpointContainer parent, ModelDelta parentDelta, int flags, IBreakpoint breakpoint) {
		BreakpointContainer[] containers = parent.getContainers();

		if (parent.contains(breakpoint)) {
			if ((containers.length != 0)) {
				for (BreakpointContainer container : containers) {
					ModelDelta nodeDelta = parentDelta.addNode(container, IModelDelta.STATE);
					appendModelDelta(container, nodeDelta, flags, breakpoint);
				}
			} else {
				parentDelta.addNode(breakpoint, flags);
			}
		}
	}

	/**
	 * Appends the model delta to the first found element in the model delta tree.
	 *
	 * @param parentDelta the parent delta
	 * @param element the element to search
	 * @param flags the delta flags
	 */
	private void appendModelDeltaToElement(IModelDelta parentDelta, Object element, int flags) {
		if (element.equals(parentDelta.getElement())) {
			((ModelDelta) parentDelta).setFlags(parentDelta.getFlags() | flags);
			return;
		}

		for (IModelDelta childDelta : parentDelta.getChildDeltas()) {
			if (element.equals(childDelta.getElement())) {
				((ModelDelta) childDelta).setFlags(childDelta.getFlags() | flags);
				return;
			}

			appendModelDeltaToElement(childDelta, element, flags);
		}
	}
}
