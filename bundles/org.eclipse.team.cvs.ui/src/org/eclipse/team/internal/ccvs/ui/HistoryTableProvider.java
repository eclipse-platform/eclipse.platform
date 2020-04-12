/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *     Olexiy Buyanskyy <olexiyb@gmail.com> - Bug 76386 - [History View] CVS Resource History shows revisions from all branches
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * This class provides the table and it's required components for a file's revision
 * history
 */
public class HistoryTableProvider {

	private ICVSFile currentFile;
	private String currentRevision;
	private TableViewer viewer;
	private Font currentRevisionFont;
	
	/**
	 * Constructor for HistoryTableProvider.
	 */
	public HistoryTableProvider() {
		super();
	}

	//column constants
	private static final int COL_REVISION = 0;
	private static final int COL_BRANCHES = 1;
	private static final int COL_TAGS = 2;
	private static final int COL_DATE = 3;
	private static final int COL_AUTHOR = 4;
	private static final int COL_COMMENT = 5;

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
			ILogEntry entry = adaptToLogEntry(element);
			if (entry == null) return ""; //$NON-NLS-1$
			switch (columnIndex) {
				case COL_REVISION:
					String revision = entry.getRevision();
					String currentRevision = getCurrentRevision();
					if (currentRevision != null && currentRevision.equals(revision)) {
						revision = NLS.bind(CVSUIMessages.currentRevision, new String[] { revision }); 
					}
					return revision;
				case COL_BRANCHES:
					CVSTag[] branches = entry.getBranches();
					StringBuilder result = new StringBuilder();
					for (int i = 0; i < branches.length; i++) {
						result.append(branches[i].getName());
						if (i < branches.length - 1) {
							result.append(", "); //$NON-NLS-1$
						}
					}
					return result.toString();

				case COL_TAGS:
					CVSTag[] tags = entry.getTags();
					result = new StringBuilder();
					for (int i = 0; i < tags.length; i++) {
						result.append(tags[i].getName());
						if (i < tags.length - 1) {
							result.append(", "); //$NON-NLS-1$
						}
					}
					return result.toString();
				case COL_DATE:
					Date date = entry.getDate();
					if (date == null) return CVSUIMessages.notAvailable; 
					return getDateFormat().format(date);
				case COL_AUTHOR:
					return entry.getAuthor();
				case COL_COMMENT:
					String comment = entry.getComment();
					int index = comment.indexOf("\n"); //$NON-NLS-1$
					switch (index) {
						case -1:
							return comment;
						case 0:
							return CVSUIMessages.HistoryView_______4; 
						default:
							return NLS.bind(CVSUIMessages.CVSCompareRevisionsInput_truncate, new String[] { comment.substring(0, index) }); 
					}
			}
			return ""; //$NON-NLS-1$
		}
		
		private synchronized DateFormat getDateFormat() {
			if (dateFormat == null)
				dateFormat = DateFormat.getInstance();
			return dateFormat;
		}
		
		@Override
		public Color getForeground(Object element) {
			ILogEntry entry = adaptToLogEntry(element);
			if (entry.isDeletion())  {
				return Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
			} else  {
				return null;
			}
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}

		@Override
		public Font getFont(Object element) {
			ILogEntry entry = adaptToLogEntry(element);
			if (entry == null)
				return null;
			String revision = entry.getRevision();
			String currentRevision = getCurrentRevision();
			if (currentRevision != null && currentRevision.equals(revision)) {
				if (currentRevisionFont == null) {
					Font defaultFont = JFaceResources.getDefaultFont();
					FontData[] data = defaultFont.getFontData();
					for (FontData d : data) {
						d.setStyle(SWT.BOLD);
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
		
		private VersionCollator versionCollator = new VersionCollator();
		
		// column headings:	"Revision" "Branches" "Tags" "Date" "Author" "Comment"
		private int[][] SORT_ORDERS_BY_COLUMN = {
			{COL_REVISION, COL_DATE, COL_AUTHOR, COL_COMMENT, COL_TAGS, COL_BRANCHES},	/* revision */ 
			{COL_BRANCHES, COL_REVISION, COL_DATE, COL_AUTHOR, COL_COMMENT, COL_TAGS},	/* tags */
			{COL_TAGS, COL_REVISION, COL_DATE, COL_AUTHOR, COL_COMMENT, COL_BRANCHES},	/* tags */
			{COL_DATE, COL_REVISION, COL_AUTHOR, COL_COMMENT, COL_TAGS, COL_BRANCHES},	/* date */
			{COL_AUTHOR, COL_REVISION, COL_DATE, COL_COMMENT, COL_TAGS, COL_BRANCHES},	/* author */
			{COL_COMMENT, COL_REVISION, COL_DATE, COL_AUTHOR, COL_TAGS, COL_BRANCHES}		/* comment */
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
			ILogEntry e1 = adaptToLogEntry(o1);
			ILogEntry e2 = adaptToLogEntry(o2);
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
		int compareColumnValue(int columnNumber, ILogEntry e1, ILogEntry e2) {
			switch (columnNumber) {
				case COL_REVISION: /* revision */
					return versionCollator.compare(e1.getRevision(), e2.getRevision());
				case COL_BRANCHES: /* branches */
					CVSTag[] branches1 = e1.getBranches();
					CVSTag[] branches2 = e2.getBranches();
					if (branches2.length == 0) {
						return -1;
					}
					if (branches1.length == 0) {
						return 1;
					}
					return getComparator().compare(branches1[0].getName(), branches2[0].getName());
				case COL_TAGS: /* tags */
					CVSTag[] tags1 = e1.getTags();
					CVSTag[] tags2 = e2.getTags();
					if (tags2.length == 0) {
						return -1;
					}
					if (tags1.length == 0) {
						return 1;
					}
					return getComparator().compare(tags1[0].getName(), tags2[0].getName());
				case COL_DATE: /* date */
					Date date1 = e1.getDate();
					Date date2 = e2.getDate();
					return date1.compareTo(date2);
				case COL_AUTHOR: /* author */
					return getComparator().compare(e1.getAuthor(), e2.getAuthor());
				case COL_COMMENT: /* comment */
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

	protected ILogEntry adaptToLogEntry(Object element) {
		// Get the log entry for the provided object
		ILogEntry entry = null;
		if (element instanceof ILogEntry) {
			entry = (ILogEntry) element;
		} else if (element instanceof IAdaptable) {
			entry = ((IAdaptable)element).getAdapter(ILogEntry.class);
		}
		return entry;
	}
	
	/**
	 * Create a TableViewer that can be used to display a list of ILogEntry instances.
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
		HistoryComparator sorter = new HistoryComparator(COL_REVISION);
		sorter.setReversed(true);
		viewer.setComparator(sorter);
		
		table.addDisposeListener(e -> {
			if (currentRevisionFont != null) {
				currentRevisionFont.dispose();
			}
		});
		
		this.viewer = viewer;
		return viewer;
	}

	/**
	 * Create a CheckBoxTableViewer that can be used to display a list of ILogEntry instances.
	 * Ths method provides the labels and sorter but does not provide a content provider
	 * 
	 * @param parent
	 * @return TableViewer
	 */
	public CheckboxTableViewer createCheckBoxTable(Composite parent) {
		Table table = new Table(parent, SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridData data = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(data);
	
		TableLayout layout = new TableLayout();
		table.setLayout(layout);
		
		CheckboxTableViewer viewer = new CheckboxTableViewer(table);
		
		createColumns(table, layout, viewer);

		viewer.setLabelProvider(new HistoryLabelProvider());
		
		// By default, reverse sort by revision.
		HistoryComparator sorter = new HistoryComparator(COL_REVISION);
		sorter.setReversed(true);
		viewer.setComparator(sorter);
		
		table.addDisposeListener(e -> {
			if (currentRevisionFont != null) {
				currentRevisionFont.dispose();
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
		col.setText(CVSUIMessages.HistoryView_revision); 
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(20, true));
	
		// branches
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(CVSUIMessages.HistoryView_branches); 
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(20, true));

		// tags
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(CVSUIMessages.HistoryView_tags); 
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(20, true));
	
		// creation date
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(CVSUIMessages.HistoryView_date); 
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(20, true));
	
		// author
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(CVSUIMessages.HistoryView_author); 
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(20, true));
	
		//comment
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(CVSUIMessages.HistoryView_comment); 
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
	
	public String getCurrentRevision() {
		return currentRevision;
	}
		
	/**
	 * Method getRevision.
	 * @param currentEdition
	 */
	private String getRevision(ICVSFile currentEdition) throws CVSException {
		if (currentEdition == null) return ""; //$NON-NLS-1$
		ResourceSyncInfo info = currentEdition.getSyncInfo();
		if (info == null) return ""; //$NON-NLS-1$
		return info.getRevision();
	}
	
	public void setFile(ICVSFile file) throws CVSException {
		this.currentFile = file;
		this.currentRevision = getRevision(this.currentFile);
	}

	public ICVSFile getICVSFile() {
		return this.currentFile;
	}
}
