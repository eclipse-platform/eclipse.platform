/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.internal.ui.history;

import com.ibm.icu.text.DateFormat;
import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.TeamUIMessages;

public class GenericHistoryTableProvider {

	private IFileHistory currentFileHistory;
	private IFile currentFile;
	private String currentRevision;
	private TableViewer viewer;
	private Font currentRevisionFont;

	//column constants
	private static final int COL_REVISIONID = 0;
	private static final int COL_DATE= 1;
	private static final int COL_AUTHOR = 2;
	private static final int COL_COMMENT = 3;

	/**
	 * The history label provider.
	 */
	class HistoryLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider, IFontProvider {
		private DateFormat dateFormat;
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		@Override
		public String getColumnText(Object element, int columnIndex) {
			IFileRevision entry = adaptToFileRevision(element);
			if (entry == null) return ""; //$NON-NLS-1$
			switch (columnIndex) {
				case COL_REVISIONID:
					String revision = entry.getContentIdentifier();
					return revision;
				case COL_DATE:
					long date = entry.getTimestamp();
					Date dateFromLong = new Date(date);
					return getDateFormat().format(dateFromLong);
				case COL_AUTHOR:
					return entry.getAuthor();
				case COL_COMMENT:
					String comment = entry.getComment();
					return comment;
			}
			return ""; //$NON-NLS-1$
		}

		private synchronized DateFormat getDateFormat() {
			if (dateFormat == null)
				dateFormat = DateFormat.getInstance();
			return dateFormat;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
		 */
		@Override
		public Color getForeground(Object element) {
			IFileRevision entry = adaptToFileRevision(element);
			if (!entry.exists())  {
				return Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
			}

			return null;
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
		 */
		@Override
		public Color getBackground(Object element) {
			return null;
		}
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
		 */
		@Override
		public Font getFont(Object element) {
			IFileRevision entry = adaptToFileRevision(element);
			if (entry == null)
				return null;
			String revision = entry.getContentIdentifier();
			String tempCurrentRevision = getCurrentRevision();
			if (tempCurrentRevision != null && tempCurrentRevision.equals(revision)) {
				if (currentRevisionFont == null) {
					Font defaultFont = JFaceResources.getDefaultFont();
					FontData[] data = defaultFont.getFontData();
					for (int i = 0; i < data.length; i++) {
						data[i].setStyle(SWT.BOLD);
					}
					currentRevisionFont = new Font(viewer.getTable().getDisplay(), data);
				}
				return currentRevisionFont;
			}
			return null;
		}
	}

	/**
	 * The history sorter
	 */
	class HistoryComparator extends ViewerComparator {
		private boolean reversed = false;
		private int columnNumber;

		// column headings:	"Revision" "Tags" "Date" "Author" "Comment"
		private int[][] SORT_ORDERS_BY_COLUMN = {
			{COL_REVISIONID, COL_DATE, COL_AUTHOR, COL_COMMENT},	/* revision */
			{COL_REVISIONID, COL_DATE, COL_AUTHOR, COL_COMMENT},	/* tags */
			{COL_DATE, COL_REVISIONID, COL_AUTHOR, COL_COMMENT},	/* date */
			{COL_AUTHOR, COL_REVISIONID, COL_DATE, COL_COMMENT},	/* author */
			{COL_COMMENT, COL_REVISIONID, COL_DATE, COL_AUTHOR}		/* comment */
		};

		/**
		 * The constructor.
		 */
		public HistoryComparator(int columnNumber) {
			this.columnNumber = columnNumber;
		}
		/**
		 * Compares two log entries, sorting first by the main column of this sorter,
		 * then by subsequent columns, depending on the column sort order.
		 */
		@Override
		public int compare(Viewer viewer, Object o1, Object o2) {
			IFileRevision e1 = adaptToFileRevision(o1);
			IFileRevision e2 = adaptToFileRevision(o2);
			int result = 0;
			if (e1 == null || e2 == null) {
				result = super.compare(viewer, o1, o2);
			} else {
				int[] columnSortOrder = SORT_ORDERS_BY_COLUMN[columnNumber];
				for (int i = 0; i < columnSortOrder.length; ++i) {
					result = compareColumnValue(columnSortOrder[i], e1, e2);
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
				case 0: /* revision */
					return e1.getContentIdentifier().compareTo(e2.getContentIdentifier());
				case 1: /* date */
					long date1 = e1.getTimestamp();
					long date2 = e2.getTimestamp();
					if (date1 == date2)
						return 0;

					return date1>date2 ? -1 : 1;

				case 2: /* author */
					return getComparator().compare(e1.getAuthor(), e2.getAuthor());
				case 3: /* comment */
					return getComparator().compare(e1.getComment(), e2.getComment());
				default:
					return 0;
			}
		}
		/**
		 * Returns the number of the column by which this is sorting.
		 */
		public int getColumnNumber() {
			return columnNumber;
		}
		/**
		 * Returns true for descending, or false
		 * for ascending sorting order.
		 */
		public boolean isReversed() {
			return reversed;
		}
		/**
		 * Sets the sorting order.
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
			entry = (IFileRevision)((IAdaptable)element).getAdapter(IFileRevision.class);
		}
		return entry;
	}

	/**
	 * Create a TableViewer that can be used to display a list of IFileRevision instances.
	 * Ths method provides the labels and sorter but does not provide a content provider
	 *
	 * @param parent
	 * @return TableViewer
	 */
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
		HistoryComparator sorter = new HistoryComparator(COL_REVISIONID);
		sorter.setReversed(true);
		viewer.setComparator(sorter);

		table.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if(currentRevisionFont != null) {
					currentRevisionFont.dispose();
				}
			}
		});

		this.viewer = viewer;
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
		col.setText(TeamUIMessages.GenericHistoryTableProvider_Revision);
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(20, true));

		// creation date
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(TeamUIMessages.GenericHistoryTableProvider_RevisionTime);
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(20, true));

		// author
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(TeamUIMessages.GenericHistoryTableProvider_Author);
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(20, true));

		//comment
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(TeamUIMessages.GenericHistoryTableProvider_Comment);
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(50, true));
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
				HistoryComparator oldSorter = (HistoryComparator)tableViewer.getComparator();
				if (oldSorter != null && column == oldSorter.getColumnNumber()) {
					oldSorter.setReversed(!oldSorter.isReversed());
					tableViewer.refresh();
				} else {
					tableViewer.setComparator(new HistoryComparator(column));
				}
			}
		};
	}

	public void setFile(IFileHistory fileHistory, IFile file)  {
		this.currentFileHistory = fileHistory;
		this.currentFile= file;
		this.currentRevision = findCurrentRevision();
	}

	private String findCurrentRevision() {

		RepositoryProvider teamProvider = RepositoryProvider.getProvider(currentFile.getProject());
		IFileRevision fileRevision = teamProvider.getFileHistoryProvider().getWorkspaceFileRevision(currentFile);

		if (fileRevision != null )
			return fileRevision.getContentIdentifier();

		return null;
	}

	public IFileHistory getIFileHistory() {
		return this.currentFileHistory;
	}

	public String getCurrentRevision() {
		return currentRevision;
	}
}
