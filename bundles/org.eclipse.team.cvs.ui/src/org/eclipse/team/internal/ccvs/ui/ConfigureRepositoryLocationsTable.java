/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.internal.ccvs.ui;

import java.util.*;
import java.util.List;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.CVSRepositoryLocationMatcher;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;

public class ConfigureRepositoryLocationsTable implements ICellModifier,
		IStructuredContentProvider, ITableLabelProvider {

	private static final class AlternativeRepositoryComparator extends
			ViewerComparator {

		public AlternativeRepositoryComparator() {
		}

		private int getCategory(Object element) {
			if (element instanceof RepositoryLocationItem) {
				return 0;
			}
			return 2;
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			final int compare = getCategory(e1) - getCategory(e2);
			if (compare != 0)
				return compare;
			return super.compare(viewer, ((Item) e1).location,
					((Item) e2).location);
		}
	}

	public abstract static class Item implements Comparable {
		public final ICVSRepositoryLocation location;
		public List alternativeList;
		public int selected;

		public Item(ICVSRepositoryLocation name, List alternative) {
			this.location = name;
			this.alternativeList = alternative;
			this.selected = 0;
		}

		@Override
		public int compareTo(Object o) {
			return location.getLocation(false).compareTo(
					((Item) o).location.getLocation(false));
		}
	}

	public static class RepositoryLocationItem extends Item {
		public RepositoryLocationItem(
				ICVSRepositoryLocation projectSetRepositoryLocation,
				List suggestedRepositoryLocations) {
			super(projectSetRepositoryLocation, suggestedRepositoryLocations);
		}
	}

	protected static final String ITEM = "item"; //$NON-NLS-1$
	protected static final String PROPERTY_ALTERNATIVE_LIST = "alternativeList"; //$NON-NLS-1$

	private TableViewer fTableViewer;

	private CellEditor[] cellEditors;

	private TextCellEditor dummyAlternativeRepositoryEditor;

	private Table table;

	/**
	 * List of <code>AlternativeRepositoryTable.RepositoryLocationItem</code>
	 * used as an input to the table.
	 */
	private List fAlternatives;

	private boolean fShowOnlyCompatibeLocations = true;

	public ConfigureRepositoryLocationsTable(Map alternativesMap) {
		fAlternatives = new ArrayList();
		for (Iterator iterator = alternativesMap.entrySet().iterator(); iterator
				.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			fAlternatives
					.add(new ConfigureRepositoryLocationsTable.RepositoryLocationItem(
							(ICVSRepositoryLocation) entry.getKey(),
							(List) entry.getValue()));
		}
	}

	public Composite createControl(final Composite composite) {
		/**
		 * Create a table.
		 */
		table = new Table(composite, SWT.V_SCROLL | SWT.BORDER | SWT.MULTI
				| SWT.FULL_SELECTION);
		// table.setLayoutData(SWTUtils.createHVFillGridData());
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.addListener(SWT.MeasureItem, event -> {
			// int clientWidth = table.getClientArea().width;
			event.height = event.gc.getFontMetrics().getHeight() + 5;
			// event.width = clientWidth * 2;
		});

		/**
		 * The 'Project set repository location' column
		 */
		final TableColumn projectSetRepositoryColumn = new TableColumn(table,
				SWT.NONE, 0);
		projectSetRepositoryColumn
				.setText(CVSUIMessages.ConfigureRepositoryLocationsWizard_column0);

		/**
		 * The 'Repository location' column
		 */
		final TableColumn alternativeRepositoryColums = new TableColumn(table,
				SWT.NONE, 1);
		alternativeRepositoryColums
				.setText(CVSUIMessages.ConfigureRepositoryLocationsWizard_column1);

		composite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				Rectangle area = composite.getClientArea();
				Point size = table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				ScrollBar vBar = table.getVerticalBar();
				int width = area.width - table.computeTrim(0, 0, 0, 0).width
						- vBar.getSize().x;
				if (size.y > area.height + table.getHeaderHeight()) {
					// Subtract the scrollbar width from the total column width
					// if a vertical scrollbar will be required
					Point vBarSize = vBar.getSize();
					width -= vBarSize.x;
				}
				Point oldSize = table.getSize();
				if (oldSize.x > area.width) {
					// table is getting smaller so make the columns
					// smaller first and then resize the table to
					// match the client area width
					projectSetRepositoryColumn.setWidth(width / 2);
					alternativeRepositoryColums.setWidth(width
							- projectSetRepositoryColumn.getWidth());
					table.setSize(area.width, area.height);
				} else {
					// table is getting bigger so make the table
					// bigger first and then make the columns wider
					// to match the client area width
					table.setSize(area.width, area.height);
					projectSetRepositoryColumn.setWidth(width / 2);
					alternativeRepositoryColums.setWidth(width
							- projectSetRepositoryColumn.getWidth());
				}
			}
		});

		/**
		 * Create a viewer for the table.
		 */
		fTableViewer = new TableViewer(table);
		fTableViewer.setContentProvider(this);
		fTableViewer.setLabelProvider(this);
		fTableViewer.setComparator(new AlternativeRepositoryComparator());
		
		/**
		 * Add a cell editor in the 'Repository location' column
		 */
		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(
				fTableViewer, new FocusCellOwnerDrawHighlighter(fTableViewer));
		ColumnViewerEditorActivationStrategy editorActivationStrategy = new ColumnViewerEditorActivationStrategy(
				fTableViewer) {
			@Override
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.F2);
			}
		};
		TableViewerEditor.create(fTableViewer, focusCellManager,
				editorActivationStrategy, ColumnViewerEditor.DEFAULT
						| ColumnViewerEditor.KEYBOARD_ACTIVATION);

		cellEditors = new CellEditor[2];
		cellEditors[0] = null;
		// to enable cell editing, create a dummy cell editor
		cellEditors[1] = dummyAlternativeRepositoryEditor = new TextCellEditor(
				table, SWT.READ_ONLY);

		fTableViewer.setCellEditors(cellEditors);
		fTableViewer.setColumnProperties(new String[] { ITEM,
				PROPERTY_ALTERNATIVE_LIST });
		fTableViewer.setCellModifier(this);
		fTableViewer.setInput(fAlternatives);
		
		// set initial selection
		for (int i = 0; i < fTableViewer.getTable().getItemCount(); i++) {
			Object element = fTableViewer.getElementAt(i);
			RepositoryLocationItem locationItem = (RepositoryLocationItem) element;
			// select second entry only when it's compatible and the first is
			// unknown (from project set file)
			if (locationItem.alternativeList.size() > 1
					&& !KnownRepositories
							.getInstance()
							.isKnownRepository(
									((ICVSRepositoryLocation) locationItem.alternativeList
											.get(0)).getLocation(false))
					&& CVSRepositoryLocationMatcher
							.isCompatible(
									locationItem.location,
									(ICVSRepositoryLocation) locationItem.alternativeList
											.get(1), false)) {
				locationItem.selected = 1;
			}
		}
		fTableViewer.refresh();
		return table;
	}

	@Override
	public Object getValue(Object element, String property) {

		final Item item = (Item) element;

		if (PROPERTY_ALTERNATIVE_LIST.equals(property)) {
			return Integer.valueOf(item.selected);
		}
		return null;
	}

	@Override
	public boolean canModify(Object element, String property) {
		// set the correct cell editor for this element
		cellEditors[1] = getCellEditor(element);
		// only allow modification for editable elements
		return PROPERTY_ALTERNATIVE_LIST.equals(property);
	}

	private CellEditor getCellEditor(Object element) {

		if (element instanceof RepositoryLocationItem) {
			// create combo-box list of alternative repositories
			RepositoryLocationItem item = (RepositoryLocationItem) element;
			
			return new ComboBoxCellEditor(table, getFilteredAlternativeRepositoriesForDisplay(item),
					SWT.READ_ONLY);
		}
		return dummyAlternativeRepositoryEditor;
	}

	@Override
	public void modify(Object element, String property, Object value) {

		final IStructuredSelection selection = fTableViewer.getStructuredSelection();
		final Item item = (Item) selection.getFirstElement();
		if (item == null)
			return;

		final int comboIndex = ((Integer) value).intValue();

		if (PROPERTY_ALTERNATIVE_LIST.equals(property)) {
			item.selected = comboIndex;
		}
		fTableViewer.refresh(item);
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		final Item item = (Item) element;

		switch (columnIndex) {
		case 0:
			return item.location.getLocation(false);
		case 1: 
			return getFilteredAlternativeRepositoriesForDisplay(item)[item.selected];
		default:
			return null;
		}
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

	@Override
	public Object[] getElements(Object inputElement) {
		return ((Collection) inputElement).toArray();
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public IStructuredSelection getSelection() {
		return fTableViewer.getStructuredSelection();
	}

	public TableViewer getViewer() {
		return fTableViewer;
	}

	/**
	 * @return a selected (in a combo box) alternative repository location for
	 *         the first selected element
	 */
	public CVSRepositoryLocation getSelectedAlternativeRepository() {
		RepositoryLocationItem firstElement = (RepositoryLocationItem) getSelection()
				.getFirstElement();
		return (CVSRepositoryLocation) getFilteredAlternativeRepositories(firstElement)
				.get(firstElement.selected);
	}

	/**
	 * Add newly created repository location to all RepositoryLocationItems. The
	 * location is added at the end of a list. For selected and compatible rows
	 * it will be automatically selected.
	 * 
	 * @param location
	 *            Location to add.
	 */
	void addAlternativeRepository(ICVSRepositoryLocation location) {
		for (int i = 0; i < fTableViewer.getTable().getItemCount(); i++) {
			Object element = fTableViewer.getElementAt(i);
			RepositoryLocationItem rli = (RepositoryLocationItem) element;
			// TODO: at this moment a newly created repository location
			// is added at the end of every locationItem, we could
			// consider sorting the list again
			rli.alternativeList.add(location);
			if (getSelection().toList().contains(element)
					&& CVSRepositoryLocationMatcher.isCompatible(location,
							rli.location, false)) {
				// at the end
				rli.selected = getFilteredAlternativeRepositories(rli).size() - 1;
			}
		}
		fTableViewer.refresh(true);
	}

	/**
	 * @return A map with repository location from the project set as a key and
	 *         selected repository location from a combo box as value.
	 */
	public Map getSelected() {
		Map map = new HashMap();
		for (Iterator iterator = fAlternatives.iterator(); iterator.hasNext();) {
			RepositoryLocationItem rli = (RepositoryLocationItem) iterator
					.next();
			map.put(rli.location, getFilteredAlternativeRepositories(rli).get(rli.selected));
		}
		return map;
	}

	/**
	 * Change the fShowOnlyCompatibeLocations flag. If set to <code>true</code>
	 * only compatible repository locations are shown, current selection will be
	 * updated when a non-compatible entry is selected. If set to
	 * <code>false</code> all repository locations are shown, current
	 * selection will be updated if necessary.
	 * 
	 * @param show
	 *            The flag
	 */
	public void setShowOnlyCompatibleLocations(boolean show) {
		fShowOnlyCompatibeLocations = show;
		for (int i = 0; i < fTableViewer.getTable().getItemCount(); i++) {
			Object element = fTableViewer.getElementAt(i);
			RepositoryLocationItem rli = (RepositoryLocationItem) element;
			updateSelection(rli); 
		}
		fTableViewer.refresh(true);
	}
	
	private List getFilteredAlternativeRepositories(Item item) {
		return getFilteredAlternativeRepositories(item, fShowOnlyCompatibeLocations);
	}
	
	private List getFilteredAlternativeRepositories(Item item, boolean showOnlyCompatible) {
		List alternativeList = item.alternativeList;
		if (!showOnlyCompatible) {
			return alternativeList;
		} else {
			List alternativeFiltered = new ArrayList();
			for (int i = 0; i < alternativeList.size(); i++) {
				CVSRepositoryLocation repo = (CVSRepositoryLocation) alternativeList.get(i);
				// If "Show only compatible..." option is on add only compatible
				// locations or the location itself
				if (!CVSRepositoryLocationMatcher.isCompatible(item.location,
						repo, true)){
					continue; // skip this repo location
				}
				alternativeFiltered.add(repo);
			}
			return alternativeFiltered;
		}
	}
	
	private void updateSelection(Item item) {
		if (fShowOnlyCompatibeLocations) {
			int shift = 0;
			for (int j = 0; j <= item.selected; j++) {
				ICVSRepositoryLocation rl = (ICVSRepositoryLocation) item.alternativeList
						.get(j);
				if (!CVSRepositoryLocationMatcher.isCompatible(item.location,
						rl, true)) {
					shift++;
				}
			}
			item.selected -= shift;

			// the selected location is neither compatible nor equal to the
			// one from the project set
			ICVSRepositoryLocation selected = (ICVSRepositoryLocation) getFilteredAlternativeRepositories(
					item).get(item.selected);
			if (!CVSRepositoryLocationMatcher.isCompatible(item.location,
					selected, true)) {
				item.selected = 0; // default
				// find compatible
				for (int j = 0; j < item.alternativeList.size(); j++) {
					ICVSRepositoryLocation l = (ICVSRepositoryLocation) item.alternativeList
							.get(j);
					if (CVSRepositoryLocationMatcher.isCompatible(l,
							item.location, true)) {
						item.selected = j;
						break;
					}
				}
			}
		} else {
			// show all
			int shift = 0;
			// index of (item.)selected object from the full list
			for (int j = 0; j <= getFilteredAlternativeRepositories(item)
					.indexOf(
							getFilteredAlternativeRepositories(item, true).get(
									item.selected)); j++) {
				ICVSRepositoryLocation rl = (ICVSRepositoryLocation) item.alternativeList
						.get(j);
				if (!CVSRepositoryLocationMatcher.isCompatible(item.location, rl,
						true)) {
					shift++;
				}
			}
			item.selected += shift;
		}
	}
	
	private String[] getFilteredAlternativeRepositoriesForDisplay(Item item) {
		List filteredAlternativeList = getFilteredAlternativeRepositories(item);
		List repositoriesForDisplay = new ArrayList();
		for (int i = 0; i < filteredAlternativeList.size(); i++) {
			CVSRepositoryLocation rl = (CVSRepositoryLocation) filteredAlternativeList
					.get(i);
			repositoriesForDisplay.add(rl.getLocation());
		}
		return (String[]) repositoriesForDisplay.toArray(new String[0]);
	}

}
