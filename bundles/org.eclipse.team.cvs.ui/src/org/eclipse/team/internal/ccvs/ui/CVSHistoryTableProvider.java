/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
 *     Brock Janiczak (brockj@tpg.com.au) - Bug 180436 Use table sort indicators on CVS
 *     Brock Janiczak (brockj@tpg.com.au) - Bug 181899 CVS History wrongly ordered
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 182442 Display full comment in tooltip
 *     Olexiy Buyanskyy <olexiyb@gmail.com> - Bug 76386 - [History View] CVS Resource History shows revisions from all branches
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.history.*;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.filehistory.CVSFileRevision;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.core.history.LocalFileRevision;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.history.AbstractHistoryCategory;
import org.eclipse.team.internal.ui.history.DateHistoryCategory;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;

public class CVSHistoryTableProvider {

	public static final String CATEGORIES_COLOR = "org.eclipse.team.cvs.ui.fontsandcolors.cvshistorypagecategories";  //$NON-NLS-1$
	
	private IFileHistory currentFileHistory;
	private IFile workspaceFile;
	
	/* private */TreeViewer viewer;
	/* private */Font currentRevisionFont;

	private boolean baseModified;

	private IDialogSettings settings;

	//column constants
	private static final int COL_REVISIONID = 0;
	private static final int COL_BRANCHES = 1;
	private static final int COL_TAGS = 2;
	private static final int COL_DATE = 3;
	private static final int COL_AUTHOR = 4;
	private static final int COL_COMMENT = 5;
	
	// cvs history table provider settings section name
	private static final String CVS_HISTORY_TABLE_PROVIDER_SECTION = CVSHistoryTableProvider.class.getName();

	// cvs history table provider settings keys
	private static final String COL_REVISIONID_NAME = "COL_REVISIONID"; //$NON-NLS-1$
	private static final String COL_BRANCHES_NAME = "COL_BRANCHES"; //$NON-NLS-1$
	private static final String COL_TAGS_NAME = "COL_TAGS"; //$NON-NLS-1$
	private static final String COL_DATE_NAME = "COL_DATE"; //$NON-NLS-1$
	private static final String COL_AUTHOR_NAME = "COL_AUTHOR"; //$NON-NLS-1$
	private static final String COL_COMMENT_NAME = "COL_COMMENT"; //$NON-NLS-1$

	private static final String COL_NAME = "COLUMN_NAME"; //$NON-NLS-1$
	private static final String SORT_COL_NAME = "SORT_COL_NAME"; //$NON-NLS-1$
	private static final String SORT_COL_DIRECTION = "SORT_COL_DIRECTION"; //$NON-NLS-1$

	public CVSHistoryTableProvider() {
		IDialogSettings viewsSettings = CVSUIPlugin.getPlugin()
				.getDialogSettings();
		settings = viewsSettings.getSection(CVS_HISTORY_TABLE_PROVIDER_SECTION);
		if (settings == null) {
			settings = viewsSettings
					.addNewSection(CVS_HISTORY_TABLE_PROVIDER_SECTION);
		}
	}

	/**
	 * The history label provider.
	 */
	class HistoryLabelProvider extends ColumnLabelProvider {
		
		Image dateImage = null;
		ImageDescriptor dateDesc = null;
		
		Image localRevImage = null;
		ImageDescriptor localRevDesc = null;
		
		Image remoteRevImage = null;
		ImageDescriptor remoteRevDesc = null;
		
		ThemeListener themeListener;
		private DateFormat dateFormat;
		private final int column;
		
		public HistoryLabelProvider(int column, CVSHistoryTableProvider provider){
				this.column = column;
				PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(themeListener= new ThemeListener(provider));
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
			
			if (remoteRevImage != null) {
				remoteRevImage.dispose();
				remoteRevImage = null;
			}
			
			if (themeListener != null){
				PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeListener);
			}
		}
		
		@Override
		public String getToolTipText(Object element) {
			if (column == COL_COMMENT && !isSingleLine(element)) {
				IFileRevision entry = adaptToFileRevision(element);
				if (entry != null)
					return entry.getComment();
			}
			return null;
		}
		
		@Override
		public boolean useNativeToolTip(Object object) {
			return column != COL_COMMENT || isSingleLine(object);
		}
		
		private boolean isSingleLine(Object object) {
			IFileRevision entry = adaptToFileRevision(object);
			if (entry != null)
				return entry.getComment() == null || entry.getComment().indexOf('\n') == -1;
			return true;
		}

		@Override
		public Image getImage(Object element) {
			return getColumnImage(element, column);
		}
		
		public Image getColumnImage(Object element, int columnIndex) {
			if (element instanceof DateHistoryCategory &&
				columnIndex == COL_REVISIONID){
				if (dateImage == null){
					dateDesc = CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_DATES_CATEGORY);
					dateImage = dateDesc.createImage();
				} 
				return dateImage;
			}
			
			if (element instanceof LocalFileRevision &&
					columnIndex == COL_REVISIONID){
				if (localRevImage == null){
					localRevDesc = CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_LOCALREVISION_TABLE);
					localRevImage = localRevDesc.createImage();
				}
				return localRevImage;
			}
			
			if (element instanceof CVSFileRevision &&
					columnIndex == COL_REVISIONID){
				if (remoteRevImage == null){
					remoteRevDesc = CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_REMOTEREVISION_TABLE);
					remoteRevImage = remoteRevDesc.createImage();
				}
				return remoteRevImage;
			}
			return null;
		}

		
		@Override
		public String getText(Object element) {
			return getColumnText(element, column);
		}
		
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof AbstractHistoryCategory){
				if (columnIndex != COL_REVISIONID)
					return ""; //$NON-NLS-1$
				
				return ((AbstractHistoryCategory) element).getName();
			}
			
			IFileRevision entry = adaptToFileRevision(element);
			if (entry == null)
				return ""; //$NON-NLS-1$
			switch (columnIndex) {
				case COL_REVISIONID :
					String revision = entry.getContentIdentifier();
					String currentRevision = getCurrentRevision();
					if (currentRevision != null && currentRevision.equals(revision)) {
						if (baseModified)
							revision = NLS.bind(CVSUIMessages.nameAndRevision, new String[] { revision, CVSUIMessages.CVSHistoryTableProvider_base}); //NLS.bind(CVSUIMessages.currentRevision, new String[] { revision }); 
						else
							revision = NLS.bind(CVSUIMessages.currentRevision, new String[] {revision}); //NLS.bind(CVSUIMessages.currentRevision, new String[] { revision });
					}
					return revision;
				case COL_BRANCHES:
					ITag[] branches = entry.getBranches();
					StringBuilder resultBranches = new StringBuilder();
					for (int i = 0; i < branches.length; i++) {
						resultBranches.append(branches[i].getName());
						if (i < branches.length - 1) {
							resultBranches.append(", "); //$NON-NLS-1$
						}
					}
					return resultBranches.toString();

				case COL_TAGS:
					ITag[] tags = entry.getTags();
					StringBuilder result = new StringBuilder();
					for (int i = 0; i < tags.length; i++) {
						result.append(tags[i].getName());
						if (i < tags.length - 1) {
							result.append(", "); //$NON-NLS-1$
						}
					}
					return result.toString();

				case COL_DATE :
					long date = entry.getTimestamp();
					Date dateFromLong = new Date(date);
					return getDateFormat().format(dateFromLong);
				case COL_AUTHOR :
					return entry.getAuthor();
				case COL_COMMENT :
					return getCommentAsSingleLine(entry);
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
			if (element instanceof AbstractHistoryCategory){
				ITheme current = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
				return current.getColorRegistry().get(CVSHistoryTableProvider.CATEGORIES_COLOR);
			}
			
			IFileRevision entry = adaptToFileRevision(element);
			if (!entry.exists()) {
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
			
			IFileRevision entry = adaptToFileRevision(element);
			if (entry == null)
				return null;
			String revision = entry.getContentIdentifier();
			//String comment = entry.getComment();
			String tempCurrentRevision = getCurrentRevision();
			Font returnFont = null;
			
			if (tempCurrentRevision != null && tempCurrentRevision.equals(revision)) {
				returnFont = getCurrentRevisionFont();
			}
			//Check to see if this is the local workspace file
			if (workspaceFile != null && entry instanceof LocalFileRevision) {
				LocalFileRevision localRevision = (LocalFileRevision) entry;
				if (localRevision.isCurrentState())
					return getCurrentRevisionFont();	
			}
			
			return returnFont;
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
	class HistoryComparator extends ViewerComparator {
		private boolean reversed = false;
		private int columnNumber;
		
		private VersionCollator versionCollator = new VersionCollator();
		
		// column headings:	"Revision" "Branches" "Tags" "Date" "Author" "Comment"
		private int[][] SORT_ORDERS_BY_COLUMN = {
		{COL_REVISIONID, COL_DATE, COL_AUTHOR, COL_COMMENT, COL_TAGS, COL_BRANCHES}, /* revision */
		{COL_BRANCHES, COL_DATE, COL_REVISIONID, COL_AUTHOR, COL_COMMENT, COL_TAGS}, /* tags */
		{COL_TAGS, COL_DATE, COL_REVISIONID, COL_AUTHOR, COL_COMMENT, COL_BRANCHES}, /* tags */
		{COL_DATE, COL_REVISIONID, COL_AUTHOR, COL_COMMENT, COL_TAGS, COL_BRANCHES}, /* date */
		{COL_AUTHOR, COL_REVISIONID, COL_DATE, COL_COMMENT, COL_TAGS, COL_BRANCHES}, /* author */
		{COL_COMMENT, COL_REVISIONID, COL_DATE, COL_AUTHOR, COL_TAGS, COL_BRANCHES} /* comment */
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
		public int compare(Viewer compareViewer, Object o1, Object o2) {
			if (o1 instanceof AbstractHistoryCategory || o2 instanceof AbstractHistoryCategory)
				return 0;
			
			IFileRevision e1 = adaptToFileRevision(o1);
			IFileRevision e2 = adaptToFileRevision(o2);
			int result = 0;
			if (e1 == null || e2 == null) {
				result = super.compare(compareViewer, o1, o2);
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
				case COL_REVISIONID : /* revision */
					if (e1 instanceof LocalFileRevision ||
						e2 instanceof LocalFileRevision) {
						//compare based on dates
						long date1 = e1.getTimestamp();
						long date2 = e2.getTimestamp();
						if (date1 == date2)
							return 0;

						return date1 > date2 ? 1 : -1;
					}
					return versionCollator.compare(e1.getContentIdentifier(), e2.getContentIdentifier());
				case COL_BRANCHES: /* branches */
					ITag[] branches1 = e1.getBranches();
					ITag[] branches2 = e2.getBranches();
					if (branches2.length == 0) {
						return -1;
					}
					if (branches1.length == 0) {
						return 1;
					}
					return getComparator().compare(branches1[0].getName(), branches2[0].getName());
				case COL_TAGS: /* tags */
					ITag[] tags1 = e1.getTags();
					ITag[] tags2 = e2.getTags();
					if (tags2.length == 0) {
						return -1;
					}
					if (tags1.length == 0) {
						return 1;
					}
					return getComparator().compare(tags1[0].getName(), tags2[0].getName());
				case COL_DATE : /* date */
					long date1 = e1.getTimestamp();
					long date2 = e2.getTimestamp();
					if (date1 == date2)
						return 0;

					return date1 > date2 ? 1 : -1;

				case COL_AUTHOR : /* author */
					String author1 = e1.getAuthor();
					String author2 = e2.getAuthor();
					if (author2 == null)
						return -1;
					
					if (author1 == null)
						return 1;
					
					return getComparator().compare(author1, author2);
				case COL_COMMENT : /* comment */
					String comment1 = e1.getComment();
					String comment2 = e2.getComment();
					if (comment2 == null)
						return -1;
					
					if (comment1 == null)
						return 1;
					
					return getComparator().compare(comment1, comment2);
				default :
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
			entry = ((IAdaptable) element).getAdapter(IFileRevision.class);
		} else if (element instanceof AbstractHistoryCategory){
			if (((AbstractHistoryCategory) element).hasRevisions())
				entry = ((AbstractHistoryCategory) element).getRevisions()[0];
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
	public TreeViewer createTree(Composite parent) {
		Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		
		GridData data = new GridData(GridData.FILL_BOTH);
		tree.setLayoutData(data);

		TableLayout layout= new TableLayout(true);
		tree.setLayout(layout);

		this.viewer = new TreeViewer(tree);
		createColumns(viewer, layout);
		
		// Initialize the sorting
		ColumnViewerToolTipSupport.enableFor(viewer);
		viewer.refresh();

		tree.addDisposeListener(e -> {
			if (currentRevisionFont != null) {
				currentRevisionFont.dispose();
			}
		});
		
		return viewer;
	}

	/**
	 * Creates the columns for the history table.
	 */
	private void createColumns(TreeViewer tree, TableLayout layout) {
		SelectionListener headerListener = getColumnListener(viewer);
		
		// revision
		TreeViewerColumn viewerCol = new TreeViewerColumn(tree, SWT.NONE);
		viewerCol.setLabelProvider(new HistoryLabelProvider(COL_REVISIONID, this));
		TreeColumn col = viewerCol.getColumn();
		col.setData(COL_NAME, COL_REVISIONID_NAME);
		col.setResizable(true);
		col.setText(TeamUIMessages.GenericHistoryTableProvider_Revision);
		col.addSelectionListener(headerListener);

		// branches
		viewerCol = new TreeViewerColumn(tree, SWT.NONE);
		viewerCol.setLabelProvider(new HistoryLabelProvider(COL_BRANCHES, this));
		col = viewerCol.getColumn();
		col.setData(COL_NAME, COL_BRANCHES_NAME);
		col.setResizable(true);
		col.setText(CVSUIMessages.HistoryView_branches); 
		col.addSelectionListener(headerListener);

		// tags
		viewerCol = new TreeViewerColumn(tree, SWT.NONE);
		viewerCol.setLabelProvider(new HistoryLabelProvider(COL_TAGS, this));
		col = viewerCol.getColumn();
		col.setData(COL_NAME, COL_TAGS_NAME);
		col.setResizable(true);
		col.setText(CVSUIMessages.HistoryView_tags); 
		col.addSelectionListener(headerListener);

		// creation date
		viewerCol = new TreeViewerColumn(tree, SWT.NONE);
		viewerCol.setLabelProvider(new HistoryLabelProvider(COL_DATE, this));
		col = viewerCol.getColumn();
		col.setData(COL_NAME, COL_DATE_NAME);
		col.setResizable(true);
		col.setText(TeamUIMessages.GenericHistoryTableProvider_RevisionTime);
		col.addSelectionListener(headerListener);

		// author
		viewerCol = new TreeViewerColumn(tree, SWT.NONE);
		viewerCol.setLabelProvider(new HistoryLabelProvider(COL_AUTHOR, this));
		col = viewerCol.getColumn();
		col.setData(COL_NAME, COL_AUTHOR_NAME);
		col.setResizable(true);
		col.setText(TeamUIMessages.GenericHistoryTableProvider_Author);
		col.addSelectionListener(headerListener);

		//comment
		viewerCol = new TreeViewerColumn(tree, SWT.NONE);
		viewerCol.setLabelProvider(new HistoryLabelProvider(COL_COMMENT, this));
		col = viewerCol.getColumn();
		col.setData(COL_NAME, COL_COMMENT_NAME);
		col.setResizable(true);
		col.setText(TeamUIMessages.GenericHistoryTableProvider_Comment);
		col.addSelectionListener(headerListener);

		loadColumnLayout(layout);
	}

	public void loadColumnLayout(TableLayout layout) {
		int widths[] = new int[] {
				getSettingsInt(COL_REVISIONID_NAME),
				getSettingsInt(COL_BRANCHES_NAME),
				getSettingsInt(COL_TAGS_NAME),
				getSettingsInt(COL_DATE_NAME),
				getSettingsInt(COL_AUTHOR_NAME),
				getSettingsInt(COL_COMMENT_NAME) };
		ColumnLayoutData weightData[] = getWeightData(widths);
		for (ColumnLayoutData w : weightData) {
			layout.addColumnData(w);
		}

		String sortName = settings.get(SORT_COL_NAME);
		if (sortName == null) {
			sortName = COL_DATE_NAME;
		}
		int sortDirection = SWT.DOWN;
		try {
			sortDirection = settings.getInt(SORT_COL_DIRECTION);
		} catch (NumberFormatException e) {
			// Silently ignored
		}
		TreeColumn sortColumn = null;
		int columnNumber = 0;
		TreeColumn columns[] = viewer.getTree().getColumns();
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].getData(COL_NAME).equals(sortName)) {
				sortColumn = columns[i];
				columnNumber = i;
			}
		}
		viewer.getTree().setSortColumn(sortColumn);
		viewer.getTree().setSortDirection(sortDirection);
		HistoryComparator sorter = new HistoryComparator(columnNumber);
		sorter.setReversed(sortDirection == SWT.DOWN);
		viewer.setComparator(sorter);
	}
	
	private int getSettingsInt(String key) {
		String value = settings.get(key);
		int ret = 0;
		try {
			ret = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			ret = -1;
		}
		return ret;
	}

	private ColumnLayoutData[] getWeightData(int[] widths) {
		boolean reset = true;
		for (int i = 0; i < widths.length; i++) {
			if (widths[i] > 0) {
				reset = false;
				break;
			}
		}
		ColumnLayoutData[] ret = new ColumnLayoutData[widths.length];
		for (int i = 0; i < widths.length; i++) {
			if (reset) {
				// use same weight for all columns
				ret[i] = new ColumnWeightData(10, true);
			} else {
				if (widths[i] < 0)
					ret[i] = new ColumnPixelData(getWidthForColumn(i));
				else
					ret[i] = new ColumnPixelData(widths[i]);
			}
		}
		return ret;
	}

	private int getWidthForColumn(int i) {
		// see #createColumns
		int chars = 4;
		switch (i) {
		case COL_REVISIONID:
			chars += TeamUIMessages.GenericHistoryTableProvider_Revision.length();
			break;
		case COL_BRANCHES:
			chars += CVSUIMessages.HistoryView_branches.length();
			break;
		case  COL_TAGS:
			chars += CVSUIMessages.HistoryView_tags.length();
			break;
		case COL_DATE:
			chars += TeamUIMessages.GenericHistoryTableProvider_RevisionTime.length();
			break;
		case COL_AUTHOR:
			chars += TeamUIMessages.GenericHistoryTableProvider_Author.length();
			break;
		case COL_COMMENT:
			chars += TeamUIMessages.GenericHistoryTableProvider_Comment.length();
			break;
		} 
		return new PixelConverter(viewer.getTree()).convertWidthInCharsToPixels(chars);
	}

	public void saveColumnLayout() {
		TreeColumn columns[] = viewer.getTree().getColumns();
		for (TreeColumn column : columns) {
			settings.put((String) column.getData(COL_NAME), column.getWidth());
		}
		settings.put(SORT_COL_NAME, (String) viewer.getTree().getSortColumn()
				.getData(COL_NAME));
		settings.put(SORT_COL_DIRECTION, viewer.getTree().getSortDirection());
	}

	/**
	 * Adds the listener that sets the sorter.
	 */
	private SelectionListener getColumnListener(final TreeViewer treeViewer) {
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
				int column = treeViewer.getTree().indexOf((TreeColumn) e.widget);
				HistoryComparator oldSorter = (HistoryComparator) treeViewer.getComparator();
				TreeColumn treeColumn = ((TreeColumn)e.widget);
				if (oldSorter != null && column == oldSorter.getColumnNumber()) {
					oldSorter.setReversed(!oldSorter.isReversed());
					
					treeViewer.getTree().setSortColumn(treeColumn);
					treeViewer.getTree().setSortDirection(oldSorter.isReversed() ? SWT.DOWN : SWT.UP);
					treeViewer.refresh();
				} else {
					treeViewer.getTree().setSortColumn(treeColumn);
					treeViewer.getTree().setSortDirection(SWT.UP);
					treeViewer.setComparator(new HistoryComparator(column));
				}
			}
		};
	}

	public void setFile(IFileHistory fileHistory, IFile workspaceFile) {
		this.currentFileHistory = fileHistory;
		this.workspaceFile = workspaceFile;
	}


	public IFileHistory getIFileHistory() {
		return this.currentFileHistory;
	}

	public String getCurrentRevision() {
		
		try {
			if (workspaceFile != null) {
				ICVSFile cvsWorkspaceFile = CVSWorkspaceRoot.getCVSFileFor(workspaceFile);
				byte[] syncBytes = cvsWorkspaceFile.getSyncBytes();
				if (syncBytes != null) {
					String workspaceRevision = ResourceSyncInfo.getRevision(syncBytes);
					return workspaceRevision;
				}
			}
		
		} catch (CVSException e) {
		}

		return null;
	}

	public void setBaseModified(boolean modified) {
		this.baseModified=modified;
	}
	
	private static class ThemeListener implements IPropertyChangeListener {

		private final CVSHistoryTableProvider provider;
		
		ThemeListener(CVSHistoryTableProvider provider) {
			this.provider= provider;
		}
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			provider.viewer.refresh();
		}
	}

	public void setWorkspaceFile(IFile workspaceFile) {
		this.workspaceFile = workspaceFile;
	}
	
	public static String getCommentAsSingleLine(IFileRevision entry) {
		String comment = entry.getComment();
		if (comment != null){
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
}
