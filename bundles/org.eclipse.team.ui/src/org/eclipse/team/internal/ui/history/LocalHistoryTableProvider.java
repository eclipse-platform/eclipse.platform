/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.history;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.compare.IModificationDate;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;

public class LocalHistoryTableProvider {
	/* private */ static final int COL_DATE = 0;

	/* private */ TreeViewer viewer;

	private Image localRevImage;
	private DateFormat dateFormat;

	/**
	 * The Local history label provider.
	 */
	private class LocalHistoryLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider, IFontProvider {
		private Image dateImage;
		private Font currentRevisionFont;

		private IPropertyChangeListener themeListener = event -> LocalHistoryTableProvider.this.viewer.refresh();

		public LocalHistoryLabelProvider() {
			PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(themeListener);
		}

		@Override
		public void dispose() {
			if (dateImage != null){
				dateImage.dispose();
				dateImage = null;
			}

			if (localRevImage != null) {
				localRevImage.dispose();
				localRevImage = null;
			}

			if (themeListener != null){
				PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeListener);
			}

			if (currentRevisionFont != null) {
				currentRevisionFont.dispose();
			}
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == COL_DATE) {
				if (element instanceof DateHistoryCategory) {
					if (dateImage == null) {
						ImageDescriptor dateDesc = TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_DATES_CATEGORY);
						dateImage = dateDesc.createImage();
					}
					return dateImage;
				}

				if (getModificationDate(element) != -1) {
					return getRevisionImage();
				}
			}

			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (columnIndex == COL_DATE) {
				if (element instanceof AbstractHistoryCategory){
					return ((AbstractHistoryCategory) element).getName();
				}

				long date = getModificationDate(element);
				if (date != -1) {
					Date dateFromLong = new Date(date);
					return getDateFormat().format(dateFromLong);
				}
			}
			return ""; //$NON-NLS-1$
		}

		@Override
		public Color getForeground(Object element) {
			if (element instanceof AbstractHistoryCategory){
				// TODO: We should have a Team theme for this
				ITheme current = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
				return current.getColorRegistry().get("org.eclipse.team.cvs.ui.fontsandcolors.cvshistorypagecategories"); //$NON-NLS-1$
			}

			if (isDeletedEdition(element)) {
				return Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
			}

			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}

		@Override
		public Font getFont(Object element) {
			if (element instanceof AbstractHistoryCategory) {
				return getCurrentRevisionFont();
			}
			if (isCurrentEdition(element)) {
				return getCurrentRevisionFont();
			}
			return null;
		}

		private Font getCurrentRevisionFont() {
			if (currentRevisionFont == null) {
				Font defaultFont = JFaceResources.getDefaultFont();
				FontData[] data = defaultFont.getFontData();
				for (FontData d : data) {
					d.setStyle(SWT.BOLD);
				}
				currentRevisionFont = new Font(viewer.getTree().getDisplay(), data);
			}
			return currentRevisionFont;
		}
	}

	/**
	 * The history sorter
	 */
	private class HistoryComparator extends ViewerComparator {
		private boolean reversed = false;
		private int columnNumber;

		// column headings:	"Revision" "Tags" "Date" "Author" "Comment"
		private int[][] SORT_ORDERS_BY_COLUMN = {
				{COL_DATE}, /* date */
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

			long date1 = getModificationDate(o1);
			long date2 = getModificationDate(o2);
			int result = 0;
			if (date1 == -1 || date2 == -1) {
				result = super.compare(compareViewer, o1, o2);
			} else {
				for (int columnSortOrder : SORT_ORDERS_BY_COLUMN[columnNumber]) {
					result = compareColumnValue(columnSortOrder, date1, date2);
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
		int compareColumnValue(int columnNumber, long date1, long date2) {
			switch (columnNumber) {
				case 0 : /* date */
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

	/**
	 * Creates the columns for the history table.
	 */
	private void createColumns(Tree tree, TableLayout layout) {
		SelectionListener headerListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// column selected - need to sort
				int column = viewer.getTree().indexOf((TreeColumn) e.widget);
				HistoryComparator oldSorter = (HistoryComparator) viewer.getComparator();
				if (oldSorter != null && column == oldSorter.getColumnNumber()) {
					oldSorter.setReversed(!oldSorter.isReversed());
					viewer.refresh();
				} else {
					viewer.setComparator(new HistoryComparator(column));
				}
			}
		};
		// creation date
		TreeColumn col = new TreeColumn(tree, SWT.NONE);
		col.setResizable(true);
		col.setText(TeamUIMessages.GenericHistoryTableProvider_RevisionTime);
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(20, true));
	}

	/**
	 * Create a TreeViewer that can be used to display a list of IFile instances.
	 * This method provides the labels and sorter but does not provide a content provider
	 *
	 * @param parent
	 * @return TableViewer
	 */
	public TreeViewer createTree(Composite parent) {
		Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(false);

		GridData data = new GridData(GridData.FILL_BOTH);
		tree.setLayoutData(data);

		TableLayout layout = new TableLayout();
		tree.setLayout(layout);

		this.viewer = new TreeViewer(tree);

		createColumns(tree, layout);

		viewer.setLabelProvider(new LocalHistoryLabelProvider());

		// By default, reverse sort by revision.
		// If local filter is on sort by date
		HistoryComparator sorter = new HistoryComparator(COL_DATE);
		sorter.setReversed(false);
		viewer.setComparator(sorter);

		return viewer;
	}

	protected long getModificationDate(Object element) {
		IModificationDate md = Adapters.adapt(element, IModificationDate.class);
		if (md != null)
			return md.getModificationDate();
		if (element instanceof IFileState) {
			IFileState fs = (IFileState) element;
			return fs.getModificationTime();
		}
		if (element instanceof IFile) {
			IFile f = (IFile) element;
			return f.getLocalTimeStamp();
		}
		return -1;
	}

	protected boolean isCurrentEdition(Object element) {
		if (element instanceof IFile) {
			return true;
		}
		if (element instanceof IFileState) {
			return false;
		}
		return false;
	}

	protected boolean isDeletedEdition(Object element) {
		if (element instanceof IFile) {
			IFile f = (IFile) element;
			return !f.exists();
		}
		return false;
	}

	public Image getRevisionImage() {
		if (localRevImage == null) {
			ImageDescriptor localRevDesc = TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_LOCALREVISION_TABLE);
			localRevImage = localRevDesc.createImage();
		}
		return localRevImage;
	}

	public synchronized DateFormat getDateFormat() {
		if (dateFormat == null)
			dateFormat = DateFormat.getInstance();
		return dateFormat;
	}
}
