/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.importexport.breakpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.DelegatingModelPresentation;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointOrganizer;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointContainer;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsComparator;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsContentProvider;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsLabelProvider;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsView;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsViewer;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IViewPart;

/**
 * This class creates a simplified debug view that can be used in wizards etc., to emulate the current debug view
 *
 * @see WizardExportBreakpointsPage
 * @see WizardImportBreakpointsPage
 *
 * @since 3.2
 */
public class EmbeddedBreakpointsViewer {

	//widgets
	private IStructuredSelection fSelection = null;
	private BreakpointsContentProvider fProvider = null;
	private Tree fTree = null;
	private BreakpointsViewer fViewer = null;
	private ICheckStateListener fCheckListener = event -> updateCheckedState(event.getElement(), event.getChecked());

	/**
	 * This constructor allows a specific selection to be used in stead of the default
	 *
	 * @param parent the parent composite to add this one to
	 * @param input the input to the viewer
	 * @param selection the selection to set on the viewer
	 */
	public EmbeddedBreakpointsViewer(Composite parent, Object input, IStructuredSelection selection) {
		Assert.isNotNull(parent);
		Assert.isNotNull(input);
		createControl(parent, input, selection);
	}

	/**
	 * Creates the control initialized to the current view, selection, and organization of the breakpoints view
	 * @param parent the parent composite to add this one to.
	 *
	 * @param parent the parent composite to add this one to
	 * @param input the input for the viewer
	 * @param selection the selection for the viewer to be initialized to. If null the selection from the breakpoints view is used
	 */
	private void createControl(Composite parent, Object input, IStructuredSelection selection) {
		fSelection = selection;
		if(fSelection == null) {
			IViewPart fViewpart = DebugUIPlugin.getActiveWorkbenchWindow().getActivePage().findView(IDebugUIConstants.ID_BREAKPOINT_VIEW);
			if(fViewpart != null) {
				fSelection = (IStructuredSelection)fViewpart.getViewSite().getSelectionProvider().getSelection();
			}
			else {
				fSelection = new StructuredSelection();
			}
		}
		Composite composite = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH, 0, 0);

		// create the treeview
		fTree = new Tree(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CHECK);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		fTree.setLayoutData(gd);
		fProvider = new BreakpointsContentProvider();
		BreakpointsView view = ((BreakpointsView)DebugUIPlugin.getActiveWorkbenchWindow().getActivePage().findView(IDebugUIConstants.ID_BREAKPOINT_VIEW));
		fViewer = new BreakpointsViewer(fTree);
		BreakpointsLabelProvider labelprovider = new BreakpointsLabelProvider();
		if(view != null) {
			//if we have handle to the view try get the current attributes, that way the
			//presentation of the embedded viewer matches the current view
			Map<String, Object> map = null;
			IDebugModelPresentation current = view.getAdapter(IDebugModelPresentation.class);
			if (current instanceof DelegatingModelPresentation) {
				map = ((DelegatingModelPresentation) current).getAttributes();
			}
			if(map != null) {
				IDebugModelPresentation newpres = labelprovider.getPresentation();
				for (Map.Entry<String, Object> entry : map.entrySet()) {
					String key = entry.getKey();
					newpres.setAttribute(key, entry.getValue());
				}
			}
		}
		fViewer.setComparator(new BreakpointsComparator());
		fViewer.setLabelProvider(labelprovider);
		fViewer.addCheckStateListener(fCheckListener);
		IBreakpointOrganizer[] orgs = null;
		if(view != null) {
			 orgs = view.getBreakpointOrganizers();
		}
		fViewer.setContentProvider(fProvider);
		fViewer.setInput(input);
		fProvider.setOrganizers(orgs);
		initViewerState();
	}

	/**
	 * Performs the initialization of the viewer from a selection
	 */
	private void initViewerState() {
		fViewer.setGrayedElements(new Object[] {});
		fViewer.setCheckedElements(new Object[] {});
		ArrayList<IBreakpoint> list = new ArrayList<>();
		for (Object item : fSelection.toArray()) {
			IBreakpoint breakpoint = (IBreakpoint)DebugPlugin.getAdapter(item, IBreakpoint.class);
			if(breakpoint != null) {
				list.add(breakpoint);
			}
			else if (item instanceof IBreakpointContainer) {
				getBreakpointsFromContainers((IBreakpointContainer)item, list);
			}
		}
		for (IBreakpoint element : list) {
			updateCheckedState(element, true);
		}
	}

	/**
	 * FInds the breakpoints of a given container
	 * @param container the container to get breakpoints from
	 * @param list the list of breakpoints to update state for
	 */
	private void getBreakpointsFromContainers(IBreakpointContainer container, ArrayList<IBreakpoint> list) {
		IBreakpoint[] bps = container.getBreakpoints();
		list.ensureCapacity(list.size() + bps.length);
		Collections.addAll(list, bps);
	}

	/**
	 * Returns the selection from the viewer with no duplicates
	 * @return the selection from the viewer with no duplicates
	 */
	public IStructuredSelection getCheckedElements() {
		Vector<Object> selected = new Vector<>();
		for (Object element : fViewer.getCheckedElements()) {
			if(!selected.contains(element)) {
				selected.addElement(element);
			}
		}
		return new StructuredSelection(selected);
	}

	/**
	 * Allows access to the viewer
	 * @return the viewer
	 */
	public BreakpointsViewer getViewer() {
		return fViewer;
	}

	/**
	 * finds all occurrences of a widget to update
	 * @param element the element to search for when finding occurrences
	 * @return a list of widget occurrences to update or an empty list
	 */
	private Widget[] searchItems(Object element) {
		ArrayList<TreeItem> list = new ArrayList<>();
		for (TreeItem item : fTree.getItems()) {
			findAllOccurrences(item, element, list);
		}
		return list.toArray(new Widget[0]);
	}

	/**
	 * performs the actual search for items in the tree
	 * @param list the list to add matches to
	 * @param item the item in the tree
	 * @param element the element to compare
	 */
	private void findAllOccurrences(TreeItem item, Object element, ArrayList<TreeItem> list) {
		if (element.equals(item.getData())) {
				list.add(item);
		}
		for (TreeItem childItem : item.getItems()) {
			findAllOccurrences(childItem, element, list);
		}
	}

	 /**
	 * Update the checked state of the given element and all of its children.
	 *
	 * @param obj the object that has been changed
	 * @param enable the checked status of the obj
	 */
	private void updateCheckedState(Object obj, boolean enable) {
		IBreakpoint breakpoint = (IBreakpoint)DebugPlugin.getAdapter(obj, IBreakpoint.class);
		if (breakpoint != null) {
			TreeItem item = null;
			for (Widget element : searchItems(obj)) {
				item = (TreeItem)element;
				item.setChecked(enable);
				refreshParents(item);
			}
		}
		else if (obj instanceof BreakpointContainer) {
			ArrayList<IBreakpoint> bps = new ArrayList<>();
			getBreakpointsFromContainers((BreakpointContainer)obj, bps);
			for (IBreakpoint bp : bps) {
				updateCheckedState(bp, enable);
			}
		}
	 }

	/**
	 * refreshes the grayed/checked state of the parents of item
	 * @param item the item to refresh parents of
	 */
	private void refreshParents(TreeItem item) {
		TreeItem parent = item.getParentItem();
		while (parent != null) {
			int checked = getNumberChildrenChecked(parent);
			if(checked == 0) {
				parent.setGrayed(false);
				parent.setChecked(false);
			}
			else if(checked == parent.getItemCount()) {
				if(getNumberChildrenGrayed(parent) > 0) {
					parent.setGrayed(true);
				}
				else {
					parent.setGrayed(false);
				}
		 		parent.setChecked(true);
			}
			else {
				parent.setGrayed(true);
				parent.setChecked(true);
			}
			parent = parent.getParentItem();
		}
	}

	/**
	 * Gets the number of grayed children for this parent
	 * @param parent the parent to inspect
	 * @return treu is any one or more children is grayed, false otherwise
	 */
	private int getNumberChildrenGrayed(TreeItem parent) {
		int count = 0;
		for (TreeItem element : parent.getItems()) {
			if(element.getGrayed()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Checks to see if all of the children under an given parent are checked or not
	 * @param children the children to check
	 * @return true if all children are checked, false otherwise
	 */
	private int getNumberChildrenChecked(TreeItem parent) {
		int count = 0;
		for (TreeItem element : parent.getItems()) {
			if(element.getChecked()) {
				count++;
			}
		}
		return count;
	}
}
