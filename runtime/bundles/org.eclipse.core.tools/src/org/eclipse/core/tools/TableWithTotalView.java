/*******************************************************************************
 * Copyright (c) 2002, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools;

import java.util.Iterator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;

/**
 * Abstract class representing a view made of two tables.
 * The first one is used to display data and the second to display totals of the
 * data showed in the first table.
 */
public abstract class TableWithTotalView extends ViewPart implements ISelectionProvider {
	protected TableViewer viewer; // Table viewer used to contain all the data
									// but the total
	protected Table tableTree; // The table tree that will populate the viewer
	protected Table totalTable; // The table used to display the totals
	protected boolean flat; // Flag indicating the view mode
	protected Clipboard clipboard;
	protected Action copyAction;
	protected Action selectAllAction;

	abstract protected String[] getColumnHeaders();

	abstract protected ColumnLayoutData[] getColumnLayout();

	abstract protected void createActions();

	abstract protected void createToolbar();

	abstract protected void createContextMenu();

	private SelectionListener getColumnListener() {
		return new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				// do nothing
			}

			@Override
			public void widgetSelected(SelectionEvent event) {
				// column selected - need to sort
				int column = viewer.getTable().indexOf((TableColumn) event.widget);

				ISorter oldSorter = (ISorter) viewer.getSorter();
				boolean threeState = oldSorter.states() == 3;
				ISelection selection = viewer.getSelection();

				// first check to see if we are changing sort columns.
				// If so, set flatness and get a new sorter
				if (oldSorter == null || !threeState || column != oldSorter.getColumnNumber()) {
					flat = column != 0; // default for column 0 is NOT flat
					viewer.setComparator(getSorter(column));
				} else {
					// Not changing sorters so we have to cycle through states for the columns
					// Three state sort for column 0.  !flat/!reverse -> flat/!reverse -> flat/reverse
					if (column == 0) {
						if (flat) {
							if (oldSorter.isReversed())
								flat = false;
							oldSorter.setReversed(!oldSorter.isReversed());
						} else {
							flat = true;
							oldSorter.setReversed(false);
						}
					} else { // for all other columns flatten and simply reverse
						flat = true;
						oldSorter.setReversed(!oldSorter.isReversed());
					}
				}
				if (viewer.getContentProvider() instanceof IFlattable)
					((IFlattable) viewer.getContentProvider()).setFlat(flat);
				viewer.refresh();
				viewer.setSelection(selection);
			}
		};
	}

	protected ControlListener getColumnResizeListener() {
		return new ControlListener() {
			@Override
			public void controlMoved(ControlEvent event) {
				// do nothing
			}

			@Override
			public void controlResized(ControlEvent event) {
				TableColumn column = (TableColumn) event.widget;
				int columnNumber = viewer.getTable().indexOf(column);
				totalTable.getColumn(columnNumber).setWidth(column.getWidth());
			}
		};
	}

	private void createTables(Composite parent) {
		// create a first table, that will display all the data
		tableTree = new Table(parent, SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
		tableTree.setLayoutData(new GridData(GridData.FILL_BOTH));
		Table table = tableTree;
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		TableLayout tableLayout = new TableLayout();
		table.setLayout(tableLayout);

		SelectionListener headerListener = getColumnListener();

		// create a second table for totals
		totalTable = new Table(parent, 0);
		GridData gridInfo = new GridData(GridData.FILL_HORIZONTAL);
		gridInfo.heightHint = totalTable.getItemHeight();
		totalTable.setLayoutData(gridInfo);
		totalTable.setHeaderVisible(false);
		totalTable.setLinesVisible(true);
		TableLayout totalLayout = new TableLayout();
		totalTable.setLayout(totalLayout);

		ControlListener columnResizeListener = getColumnResizeListener();
		//create the columns for the two tables
		ColumnLayoutData[] columnLayout = getColumnLayout();
		String[] headers = getColumnHeaders();
		for (int i = 0; i < getColumnHeaders().length; i++) {
			// column for the first table
			tableLayout.addColumnData(columnLayout[i]);
			TableColumn column = new TableColumn(table, SWT.NONE, i);
			column.setResizable(true);
			column.setText(headers[i]);
			column.addSelectionListener(headerListener);
			// "connect" the two tables so the width of their column evolve simultaneously
			// more precisely here, only the resize of the first table will trigger a resize of the second one
			column.addControlListener(columnResizeListener);

			// column for the second table
			totalLayout.addColumnData(columnLayout[i]);
			column = new TableColumn(totalTable, SWT.NONE, i);
			column.setResizable(true);
		}
	}

	protected abstract ITreeContentProvider getContentProvider();

	protected abstract ITableLabelProvider getLabelProvider();

	protected abstract ViewerComparator getSorter(int column);

	protected abstract String getStatusLineMessage(Object element);

	@Override
	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, 0);
		// crete a grid layout of one column
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);

		createTables(composite);

		clipboard = new Clipboard(parent.getDisplay());
		//create the viewer
		viewer = new TableViewer(tableTree);
		viewer.setContentProvider(getContentProvider());
		viewer.setLabelProvider(getLabelProvider());
		viewer.setComparator(getSorter(0));
		viewer.addSelectionChangedListener(getTableListener());

		createCommonActions();
		createActions();
		createToolbar();
		createContextMenu();
	}

	abstract protected String[] computeTotalLine(Iterator elements);

	private void createCommonActions() {
		copyAction = new Action() {
			@Override
			public void run() {
				IStructuredSelection selection = viewer.getStructuredSelection();
				String result = ""; //$NON-NLS-1$
				String[] columnHeaders = getColumnHeaders();
				for (String columnHeader : columnHeaders)
					result += columnHeader + ","; //$NON-NLS-1$
				result += "\n\n"; //$NON-NLS-1$

				ITableLabelProvider labelProvider = (ITableLabelProvider) viewer.getLabelProvider();
				for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
					Object selectedItem = iterator.next();
					for (int i = 0; i < columnHeaders.length; i++)
						result += labelProvider.getColumnText(selectedItem, i) + ","; //$NON-NLS-1$
					result += "\n"; //$NON-NLS-1$
				}
				clipboard.setContents(new Object[] {result}, new Transfer[] {TextTransfer.getInstance()});
			}
		};
		IActionBars actionBars = getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);

		selectAllAction = new Action() {
			@Override
			public void run() {
				tableTree.selectAll();
				// force viewer selection change
				viewer.setSelection(viewer.getSelection());
			}
		};
		actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), selectAllAction);
	}

	protected ISelectionChangedListener getTableListener() {
		return e -> {
			IStructuredSelection selection = (IStructuredSelection) e.getSelection();
			copyAction.setEnabled(!selection.isEmpty());
			if (selection.size() == 1) {
				String message = getStatusLineMessage(selection.getFirstElement());
				getViewSite().getActionBars().getStatusLineManager().setMessage(message);
			}
			totalTable.removeAll();
			updateTotals();
		};
	}

	public void updateTotals() {
		IStructuredSelection selection = (IStructuredSelection) getSelection();
		TableItem item = new TableItem(totalTable, 0);
		item.setText(computeTotalLine(selection.iterator()));
	}

	@Override
	public ISelection getSelection() {
		return viewer.getSelection();
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		// do nothing
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		// do nothing
	}

	@Override
	public void setSelection(ISelection selection) {
		// do nothing
	}

	@Override
	public void setFocus() {
		if (tableTree != null)
			tableTree.setFocus();
	}
}
