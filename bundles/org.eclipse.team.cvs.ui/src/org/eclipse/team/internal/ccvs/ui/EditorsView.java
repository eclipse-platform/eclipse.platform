/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     CSC - Initial implementation
 *     IBM Corporation - ongoing maintenance
 *     Brian Mauter <brianmauter@gmail.com> - bug 248333
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import java.text.*;
import java.util.Locale;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.EditorsInfo;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

/**
 * 
 * The <code>EditorsView</code> shows the result of cvs editors command
 * 
 * @author <a href="mailto:gregor.kohlwes@csc.com,kohlwes@gmx.net">Gregor Kohlwes</a>
 * @see org.eclipse.team.internal.ccvs.ui.actions.ShowEditorsAction
 */
public class EditorsView extends ViewPart {
	public static final String VIEW_ID = "org.eclipse.team.ccvs.ui.EditorsView"; //$NON-NLS-1$

	private Table table;
	private TableViewer tableViewer;

	class EditorsLabelProvider implements ITableLabelProvider {
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element == null)
				return ""; //$NON-NLS-1$
			EditorsInfo info = (EditorsInfo) element;

			String result = null;
			switch (columnIndex) {
				case 0 :
					result = info.getFileName();
					break;
				case 1 :
					result = info.getUserName();
					break;
				case 2 :
					result = info.getDateString();
					break;
				case 3 :
					result = info.getComputerName();
					break;
			}
			// This method must not return null
			if (result == null) result = ""; //$NON-NLS-1$
			return result;

		}

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

	}

	class EditorsComparator extends ViewerComparator {
		private int column;
		private boolean reversed;
		
		// we recognize the following date/time formats
		private DateFormat[] dateFormats = new DateFormat[] {
				new SimpleDateFormat("dd MMM yyyy HH:mm:ss z", Locale.US), //$NON-NLS-1$
				new SimpleDateFormat("dd MMM yyyy HH:mm:ss z"), //$NON-NLS-1$
				new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy z") //$NON-NLS-1$
		};

		public EditorsComparator(int column) {
			this.column = column;
		}
		
		public int getColumnNumber() {
			return column;
		}
		
		public boolean isReversed() {
			return reversed;
		}
		
		public void setReversed(boolean reversed) {
			this.reversed = reversed;
		}
		
		@Override
		public int compare(Viewer compareViewer, Object o1, Object o2) {
			int result = 0;
			if ((o1 instanceof EditorsInfo) && (o2 instanceof EditorsInfo)) {
				EditorsInfo ei1 = (EditorsInfo) o1;
				EditorsInfo ei2 = (EditorsInfo) o2;
				switch (column) {
				case 0 : // file name
					result = compareStrings(ei1.getFileName(), ei2.getFileName());
					break;
				case 1 : // user name
					result = compareStrings(ei1.getUserName(), ei2.getUserName());
					break;
				case 2 : // date
					result = compareDates(ei1.getDateString(), ei2.getDateString());
					break;
				case 3 : // computer name
					result = compareStrings(ei1.getComputerName(), ei2.getComputerName());
				}
				if (reversed)
					result = -result;
			}
			return result;
		}
		
		private int compareStrings(String s1, String s2) {
			if (s1 == null && s2 == null)
				return 0;
			if (s1 == null)
				return -1;
			if (s2 == null)
				return 1;
			return getComparator().compare(s1, s2);
		}
		
		private int compareDates(String s1, String s2) {
			long date1 = extractDate(s1);
			long date2 = extractDate(s2);
			if (date1 == date2)
				return 0;
			if (date1 == -1)
				return -1;
			if (date2 == -1)
				return 1;
			return date1 > date2 ? 1 : -1;
		}
		
		/**
		 * @param dateString Date to parse
		 * @return the parsed time/date in milliseconds or -1 on error
		 */
		private long extractDate(String dateString) {
			if (dateString != null) {
				for (DateFormat dateFormat : dateFormats) {
					dateFormat.setLenient(true);
					try {
						return dateFormat.parse(dateString).getTime();
					} catch (ParseException ex) {
						// silently ignored
					}
				}
			}
			return -1;
		}
	}	
	
	@Override
	public void createPartControl(Composite parent) {
		table =	new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.widthHint=500;
		gridData.heightHint=100;
		table.setLayoutData(gridData);

		TableLayout layout = new TableLayout();
		table.setLayout(layout);

		tableViewer = new TableViewer(table);
		createColumns(table, layout);

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setLabelProvider(new EditorsLabelProvider());
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(tableViewer.getControl(), IHelpContextIds.CVS_EDITORS_VIEW);
	}
	public void setInput(EditorsInfo[] infos) {
		tableViewer.setInput(infos);
	}
	/**
	 * Method createColumns.
	 * @param table
	 * @param layout
	 * @param viewer
	 */
	private void createColumns(Table table, TableLayout layout) {
		SelectionListener headerListener = getColumnListener(tableViewer);

		TableColumn col;
		// file name
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(CVSUIMessages.EditorsView_file); 
		layout.addColumnData(new ColumnWeightData(30, true));
		col.addSelectionListener(headerListener);

		// user name
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(CVSUIMessages.EditorsView_user); 
		layout.addColumnData(new ColumnWeightData(20, true));
		col.addSelectionListener(headerListener);

		// creation date
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(CVSUIMessages.EditorsView_date); 
		layout.addColumnData(new ColumnWeightData(30, true));
		col.addSelectionListener(headerListener);

		// computer name
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(CVSUIMessages.EditorsView_computer); 
		layout.addColumnData(new ColumnWeightData(20, true));
		col.addSelectionListener(headerListener);
	}

	private SelectionListener getColumnListener(final TableViewer tableViewer) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// column selected - need to sort
				TableColumn tableColumn = (TableColumn) e.widget;
				int column = tableViewer.getTable().indexOf(tableColumn);
				EditorsComparator oldSorter = (EditorsComparator) tableViewer.getComparator();
				if (oldSorter != null && column == oldSorter.getColumnNumber()) {
					oldSorter.setReversed(!oldSorter.isReversed());
					
					tableViewer.getTable().setSortColumn(tableColumn);
					tableViewer.getTable().setSortDirection(oldSorter.isReversed() ? SWT.DOWN : SWT.UP);
					tableViewer.refresh();
				} else {
					tableViewer.getTable().setSortColumn(tableColumn);
					tableViewer.getTable().setSortDirection(SWT.UP);
					tableViewer.setComparator(new EditorsComparator(column));
				}
			}
		};
	}
	
	@Override
	public void setFocus() {
		if (table != null)
			table.setFocus();
	}
	
	public Table getTable() {
		return table;
	}

}