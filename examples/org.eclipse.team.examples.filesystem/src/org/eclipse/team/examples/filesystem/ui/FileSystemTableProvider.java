/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.ui;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.examples.filesystem.history.FileSystemFileRevision;

public class FileSystemTableProvider {

	//column constants
	private static final int COL_TYPE = 0;
	private static final int COL_DATE = 1;

	public TableViewer createTable(Composite parent) {
		Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridData data = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(data);

		TableLayout layout = new TableLayout();
		table.setLayout(layout);

		TableViewer viewer = new TableViewer(table);

		createColumns(table, layout, viewer);

		viewer.setLabelProvider(new HistoryLabelProvider());

		// By default, reverse sort by revision.
		HistoryComparator sorter = new HistoryComparator(COL_DATE);
		sorter.setReversed(true);
		viewer.setComparator(sorter);

		return viewer;
	}

	/**
	 * Creates the columns for the history table.
	 */
	private void createColumns(Table table, TableLayout layout, TableViewer viewer) {
		SelectionListener headerListener = getColumnListener(viewer);
		// revision
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText("Revision"); //$NON-NLS-1$
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(20, true));

		// creation date
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText("Date"); //$NON-NLS-1$
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(20, true));

	}

	class HistoryLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider, IFontProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			IFileRevision entry = adaptToFileRevision(element);
			if (entry == null)
				return ""; //$NON-NLS-1$
			switch (columnIndex) {
			case COL_TYPE :
				if (entry instanceof FileSystemFileRevision)
					return "FileSystem revision";  //$NON-NLS-1$

				return "Local history revision";  //$NON-NLS-1$
			case COL_DATE :
				long date = entry.getTimestamp();
				Date dateFromLong = new Date(date);
				return DateFormat.getInstance().format(dateFromLong);
			}
			return ""; //$NON-NLS-1$
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}

		@Override
		public Color getForeground(Object element) {
			return null;
		}

		@Override
		public Font getFont(Object element) {
			return null;
		}

	}

	/**
	 * The history sorter
	 */
	class HistoryComparator extends ViewerComparator {
		private boolean reversed = false;
		private int columnNumber;

		// column headings:	"Type" "Date"
		private int[][] SORT_ORDERS_BY_COLUMN = { {COL_DATE, COL_TYPE} /* date */, {COL_TYPE, COL_DATE} /* type */
		};

		/**
		 * The constructor.
		 * @param columnNumber
		 */
		public HistoryComparator(int columnNumber) {
			this.columnNumber = columnNumber;
		}

		/**
		 * Compares two log entries, sorting first by the main column of this sorter,
		 * then by subsequent columns, depending on the column sort order.
		 */
		@Override
		public int compare(Viewer compareViewer, Object o1, Object o2) {
			/*if (o1 instanceof AbstractCVSHistoryCategory || o2 instanceof AbstractCVSHistoryCategory)
			return 0;*/

			IFileRevision e1 = adaptToFileRevision(o1);
			IFileRevision e2 = adaptToFileRevision(o2);
			int result = 0;
			if (e1 == null || e2 == null) {
				result = super.compare(compareViewer, o1, o2);
			} else {
				int[] columnSortOrder = SORT_ORDERS_BY_COLUMN[columnNumber];
				for (int element : columnSortOrder) {
					result = compareColumnValue(element, e1, e2);
					if (result != 0)
						break;
				}
			}
			if (reversed)
				result = -result;
			return result;
		}

		/**
		 * Compares two markers, based only on the value of the specified column.
		 */
		int compareColumnValue(int columnNumber, IFileRevision e1, IFileRevision e2) {
			switch (columnNumber) {
			case 0 : /* date */
				long date1 = e1.getTimestamp();
				long date2 = e2.getTimestamp();
				if (date1 == date2)
					return 0;

				return date1 > date2 ? -1 : 1;

			default :
				return 0;
			}
		}

		/**
		 * Returns the number of the column by which this is sorting.
		 * @return the column number
		 */
		public int getColumnNumber() {
			return columnNumber;
		}

		/**
		 * Returns true for descending, or false
		 * for ascending sorting order.
		 * @return returns true if reversed
		 */
		public boolean isReversed() {
			return reversed;
		}

		/**
		 * Sets the sorting order.
		 * @param newReversed
		 */
		public void setReversed(boolean newReversed) {
			reversed = newReversed;
		}
	}

	protected IFileRevision adaptToFileRevision(Object element) {
		// Get the log entry for the provided object
		IFileRevision entry = null;
		if (element instanceof IFileRevision) {
			entry = (IFileRevision) element;
		} else if (element instanceof IAdaptable) {
			entry = ((IAdaptable) element).getAdapter(IFileRevision.class);
		}
		return entry;
	}

	/**
	 * Adds the listener that sets the sorter.
	 */
	private SelectionListener getColumnListener(final TableViewer tableViewer) {
		/**
		 * This class handles selections of the column headers.
		 * Selection of the column header will cause resorting
		 * of the shown tasks using that column's sorter.
		 * Repeated selection of the header will toggle
		 * sorting order (ascending versus descending).
		 */
		return new SelectionAdapter() {
			/**
			 * Handles the case of user selecting the
			 * header area.
			 * <p>If the column has not been selected previously,
			 * it will set the sorter of that column to be
			 * the current tasklist sorter. Repeated
			 * presses on the same column header will
			 * toggle sorting order (ascending/descending).
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				// column selected - need to sort
				int column = tableViewer.getTable().indexOf((TableColumn) e.widget);
				HistoryComparator oldSorter = (HistoryComparator) tableViewer.getComparator();
				if (oldSorter != null && column == oldSorter.getColumnNumber()) {
					oldSorter.setReversed(!oldSorter.isReversed());
					tableViewer.refresh();
				} else {
					tableViewer.setComparator(new HistoryComparator(column));
				}
			}
		};
	}

}
