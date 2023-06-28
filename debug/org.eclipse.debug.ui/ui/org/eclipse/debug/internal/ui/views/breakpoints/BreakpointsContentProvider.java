/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.breakpoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointOrganizer;
import org.eclipse.debug.internal.ui.breakpoints.provisional.OtherBreakpointCategory;
import org.eclipse.debug.ui.IBreakpointOrganizerDelegate;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider for the breakpoints view
 */
public class BreakpointsContentProvider implements ITreeContentProvider, IPropertyChangeListener {

	private IBreakpointOrganizer[] fOrganizers = null;
	private BreakpointsViewer fViewer;
	private Object[] fElements;
	private boolean fDisposed = false;

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement.equals(DebugPlugin.getDefault().getBreakpointManager())) {
			return fElements;
		} else if (parentElement instanceof BreakpointContainer) {
			return ((BreakpointContainer)parentElement).getChildren();
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public void dispose() {
		fDisposed = true;
		fElements = null;
		setOrganizers(null);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fViewer = (BreakpointsViewer)viewer;
		if (newInput != null) {
			reorganize();
		}
	}

	/**
	 * Sets the nested order of breakpoint organizers, or <code>null</code>
	 * if none.
	 *
	 * @param organizers the nested order of breakpoint organizers, or <code>null</code>
	 * if none
	 */
	public void setOrganizers(IBreakpointOrganizer[] organizers) {
		// remove previous listeners
		if (fOrganizers != null) {
			for (IBreakpointOrganizer organizer : fOrganizers) {
				organizer.removePropertyChangeListener(this);
			}
		}
		fOrganizers = organizers;
		if (organizers != null && organizers.length == 0) {
			fOrganizers = null;
		}
		// add listeners
		if (fOrganizers != null) {
			for (IBreakpointOrganizer organizer : fOrganizers) {
				organizer.addPropertyChangeListener(this);
			}
		}
		if (!fDisposed) {
			fViewer.getControl().setRedraw(false);
			// maintain expansion based on visible breakpoints
			IBreakpoint[] breakpoints = null;
			if (isShowingGroups()) {
				breakpoints = fViewer.getVisibleBreakpoints();
			}
			reorganize();
			if (isShowingGroups() && breakpoints != null) {
				// restore expansion
				for (Object element : fElements) {
					BreakpointContainer container = (BreakpointContainer) element;
					for (IBreakpoint breakpoint : breakpoints) {
						if (container.contains(breakpoint)) {
							fViewer.expandToLevel(container, AbstractTreeViewer.ALL_LEVELS);
							fViewer.updateCheckedState(container);
							break;
						}
					}

				}
			}
			fViewer.getControl().setRedraw(true);
		}
	}

	/**
	 * Returns the root containers containing the given breakpoint, or <code>null</code>
	 * if none
	 *
	 * @param breakpoint the breakpoint to get containers for
	 * @return root containers containing the given breakpoint or <code>null</code>
	 */
	public BreakpointContainer[] getRoots(IBreakpoint breakpoint) {
		if (isShowingGroups()) {
			List<BreakpointContainer> list = new ArrayList<>();
			for (Object element : fElements) {
				BreakpointContainer container = (BreakpointContainer) element;
				if (container.contains(breakpoint)) {
					list.add(container);
				}
			}
			return list.toArray(new BreakpointContainer[list.size()]);
		}
		return null;
	}

	/**
	 * Returns the nested order of breakpoint organizers being used, or <code>null</code>
	 * if none.
	 *
	 * @return the nested order of breakpoint organizers being used, or <code>null</code>
	 * if none
	 */
	IBreakpointOrganizer[] getOrganizers() {
		return fOrganizers;
	}

	/**
	 * Organizes the breakpoints based on nested categories, if any.
	 */
	protected void reorganize() {
		IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints();
		if (fOrganizers == null) {
			fElements = breakpoints;
		} else {
			IBreakpointOrganizer organizer = fOrganizers[0];
			Map<IAdaptable, BreakpointContainer> categoriesToContainers = new HashMap<>();
			for (IBreakpoint breakpoint : breakpoints) {
				IAdaptable[] categories = organizer.getCategories(breakpoint);
				if (categories == null || categories.length == 0) {
					categories = OtherBreakpointCategory.getCategories(organizer);
				}
				for (IAdaptable category : categories) {
					BreakpointContainer container = categoriesToContainers.get(category);
					if (container == null) {
						IBreakpointOrganizer[] nesting = null;
						if (fOrganizers.length > 1) {
							nesting = new IBreakpointOrganizer[fOrganizers.length - 1];
							System.arraycopy(fOrganizers, 1, nesting, 0, nesting.length);
						}
						container = new BreakpointContainer(category, organizer, nesting);
						categoriesToContainers.put(category, container);
					}
					container.addBreakpoint(breakpoint);
				}
			}
			// add empty categories
			IAdaptable[] emptyCategories = organizer.getCategories();
			if (emptyCategories != null) {
				for (IAdaptable emptyCategory : emptyCategories) {
					BreakpointContainer container = categoriesToContainers.get(emptyCategory);
					if (container == null) {
						container = new BreakpointContainer(emptyCategory, organizer, null);
						categoriesToContainers.put(emptyCategory, container);
					}
				}
			}
			fElements = categoriesToContainers.values().toArray();
		}
		fViewer.getControl().setRedraw(false);
		fViewer.refresh();
		fViewer.getControl().setRedraw(true);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(IBreakpointOrganizerDelegate.P_CATEGORY_CHANGED)) {
			// TODO: only re-organize if showing the changed category
			reorganize();
		}
	}

	/**
	 * Returns the existing containers the given breakpoint is contained in, or <code>null</code>.
	 *
	 * @param breakpoint the breakpoint to get containers for
	 * @return the existing containers the given breakpoint is contained in, or <code>null</code>
	 */
	protected BreakpointContainer[] getContainers(IBreakpoint breakpoint) {
		if (isShowingGroups()) {
			IAdaptable[] categories = fOrganizers[0].getCategories(breakpoint);
			if (categories == null || categories.length == 0) {
				categories = OtherBreakpointCategory.getCategories(fOrganizers[0]);
			}
			BreakpointContainer[] containers = new BreakpointContainer[categories.length];
			int index = 0;
			for (Object element : fElements) {
				BreakpointContainer container = (BreakpointContainer)element;
				for (IAdaptable category : categories) {
					if (container.getCategory().equals(category)) {
						containers[index] = container;
						index++;
					}
				}
			}
			return containers;
		}
		return null;
	}

	/**
	 * Returns whether content is grouped by categories.
	 *
	 * @return whether content is grouped by categories
	 */
	protected boolean isShowingGroups() {
		return fOrganizers != null;
	}
}
