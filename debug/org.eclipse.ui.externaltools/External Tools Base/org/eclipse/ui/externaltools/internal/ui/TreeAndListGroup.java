/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.ui.externaltools.internal.ui;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * This class was derived from org.eclipse.ui.internal.misc.CheckboxTreeAndListGroup
 *
 */
public class TreeAndListGroup implements ISelectionChangedListener {
	private Object root;
	private Object currentTreeSelection;
	private List<ISelectionChangedListener> selectionChangedListeners = new ArrayList<>();
	private List<IDoubleClickListener> doubleClickListeners = new ArrayList<>();

	private ITreeContentProvider treeContentProvider;
	private IStructuredContentProvider listContentProvider;
	private ILabelProvider treeLabelProvider;
	private ILabelProvider listLabelProvider;

	// widgets
	private TreeViewer treeViewer;
	private TableViewer listViewer;
	private boolean allowMultiselection= false;

	/**
	 *	Create an instance of this class.  Use this constructor if you wish to specify
	 *	the width and/or height of the combined widget (to only hardcode one of the
	 *	sizing dimensions, specify the other dimension's value as -1)
	 *
	 *	@param parent org.eclipse.swt.widgets.Composite
	 *	@param style int
	 *  @param rootObject java.lang.Object
	 *	@param width int
	 *	@param height int
	 *  @param allowMultiselection Whether to allow multi-selection in the list viewer.
	 */
	public TreeAndListGroup(Composite parent, Object rootObject, ITreeContentProvider treeContentProvider, ILabelProvider treeLabelProvider, IStructuredContentProvider listContentProvider, ILabelProvider listLabelProvider, int style, int width, int height, boolean allowMultiselection) {

		root = rootObject;
		this.treeContentProvider = treeContentProvider;
		this.listContentProvider = listContentProvider;
		this.treeLabelProvider = treeLabelProvider;
		this.listLabelProvider = listLabelProvider;
		this.allowMultiselection= allowMultiselection;
		createContents(parent, width, height, style);
	}
	/**
	 * This method must be called just before this window becomes visible.
	 */
	public void aboutToOpen() {
		currentTreeSelection = null;

		//select the first element in the list
		Object[] elements = treeContentProvider.getElements(root);
		Object primary = elements.length > 0 ? elements[0] : null;
		if (primary != null) {
			treeViewer.setSelection(new StructuredSelection(primary));
		}
		treeViewer.getControl().setFocus();
	}
	/**
	 *	Add the passed listener to collection of clients
	 *	that listen for changes to list viewer selection state
	 *
	 *	@param listener ISelectionChangedListener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.add(listener);
	}

	/**
	 * Add the given listener to the collection of clients that listen to
	 * double-click events in the list viewer
	 *
	 * @param listener IDoubleClickListener
	 */
	public void addDoubleClickListener(IDoubleClickListener listener) {
		doubleClickListeners.add(listener);
	}

	/**
	 * Notify all selection listeners that a selection has occurred in the list
	 * viewer
	 */
	protected void notifySelectionListeners(SelectionChangedEvent event) {
		for (ISelectionChangedListener listener : selectionChangedListeners) {
			listener.selectionChanged(event);
		}
	}

	/**
	 * Notify all double click listeners that a double click event has occurred
	 * in the list viewer
	 */
	protected void notifyDoubleClickListeners(DoubleClickEvent event) {
		for (IDoubleClickListener listener : doubleClickListeners) {
			listener.doubleClick(event);
		}
	}

	/**
	 *	Lay out and initialize self's visual components.
	 *
	 *	@param parent org.eclipse.swt.widgets.Composite
	 *	@param width int
	 *	@param height int
	 */
	protected void createContents(Composite parent, int width, int height, int style) {
		// group pane
		Composite composite = new Composite(parent, style);
		composite.setFont(parent.getFont());
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		createTreeViewer(composite, width / 2, height);
		createListViewer(composite, width / 2, height);

		initialize();
	}
	/**
	 *	Create this group's list viewer.
	 */
	protected void createListViewer(Composite parent, int width, int height) {
		int style;
		if (allowMultiselection) {
			style= SWT.MULTI;
		} else {
			style= SWT.SINGLE;
		}
		listViewer = new TableViewer(parent, SWT.BORDER | style);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = width;
		data.heightHint = height;
		listViewer.getTable().setLayoutData(data);
		listViewer.getTable().setFont(parent.getFont());
		listViewer.setContentProvider(listContentProvider);
		listViewer.setLabelProvider(listLabelProvider);
		listViewer.setComparator(new ResourceComparator(ResourceComparator.NAME));
		listViewer.addSelectionChangedListener(this::notifySelectionListeners);
		listViewer.addDoubleClickListener(event -> {
			if (!event.getSelection().isEmpty()) {
				notifyDoubleClickListeners(event);
			}
		});
	}
	/**
	 *	Create this group's tree viewer.
	 */
	protected void createTreeViewer(Composite parent, int width, int height) {
		Tree tree = new Tree(parent, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = width;
		data.heightHint = height;
		tree.setLayoutData(data);
		tree.setFont(parent.getFont());

		treeViewer = new TreeViewer(tree);
		treeViewer.setContentProvider(treeContentProvider);
		treeViewer.setLabelProvider(treeLabelProvider);
		treeViewer.setComparator(new ResourceComparator(ResourceComparator.NAME));
		treeViewer.addSelectionChangedListener(this);
	}

	public Table getListTable() {
		return listViewer.getTable();
	}

	public IStructuredSelection getListTableSelection() {
		ISelection selection=  this.listViewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			return (IStructuredSelection)selection;
		}
		return StructuredSelection.EMPTY;
	}

	protected void initialListItem(Object element) {
		Object parent = treeContentProvider.getParent(element);
		selectAndRevealFolder(parent);
	}

	public void selectAndRevealFolder(Object treeElement) {
		treeViewer.reveal(treeElement);
		IStructuredSelection selection = new StructuredSelection(treeElement);
		treeViewer.setSelection(selection);
	}

	public void selectAndRevealFile(Object treeElement) {
		listViewer.reveal(treeElement);
		IStructuredSelection selection = new StructuredSelection(treeElement);
		listViewer.setSelection(selection);
	}

	/**
	 *	Initialize this group's viewers after they have been laid out.
	 */
	protected void initialize() {
		treeViewer.setInput(root);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		IStructuredSelection selection = event.getStructuredSelection();
		Object selectedElement = selection.getFirstElement();
		if (selectedElement == null) {
			currentTreeSelection = null;
			listViewer.setInput(currentTreeSelection);
			return;
		}

		// ie.- if not an item deselection
		if (selectedElement != currentTreeSelection) {
			listViewer.setInput(selectedElement);
		}

		currentTreeSelection = selectedElement;
	}
	/**
	 *	Set the list viewer's providers to those passed
	 *
	 *	@param contentProvider ITreeContentProvider
	 *	@param labelProvider ILabelProvider
	 */
	public void setListProviders(IStructuredContentProvider contentProvider, ILabelProvider labelProvider) {
		listViewer.setContentProvider(contentProvider);
		listViewer.setLabelProvider(labelProvider);
	}
	/**
	 *	Set the sorter that is to be applied to self's list viewer
	 */
	public void setListSorter(ViewerComparator comparator) {
		listViewer.setComparator(comparator);
	}
	/**
	 * Set the root of the widget to be new Root. Regenerate all of the tables and lists from this
	 * value.
	 * @param newRoot
	 */
	public void setRoot(Object newRoot) {
		root = newRoot;
		initialize();
	}

	/**
	 *	Set the tree viewer's providers to those passed
	 *
	 *	@param contentProvider ITreeContentProvider
	 *	@param labelProvider ILabelProvider
	 */
	public void setTreeProviders(ITreeContentProvider contentProvider, ILabelProvider labelProvider) {
		treeViewer.setContentProvider(contentProvider);
		treeViewer.setLabelProvider(labelProvider);
	}
	/**
	 *	Set the comparator that is to be applied to self's tree viewer
	 */
	public void setTreeComparator(ViewerComparator comparator) {
		treeViewer.setComparator(comparator);
	}

	/**
	 * Set the focus on to the list widget.
	 */
	public void setFocus() {
		treeViewer.getTree().setFocus();
	}
}
