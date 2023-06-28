/*******************************************************************************
 * Copyright (c) 2006, 2022 IBM Corporation and others.
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
 *     Pawel Piech (Wind River) - added support for a virtual tree model viewer (Bug 242489)
 *     Patrick Chuong (Texas Instruments) - added support for checkbox (Bug 286310)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ICheckUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentation;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentation2;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentationFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementEditor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelChangedListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelSelectionPolicy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IStateUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ITreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILazyTreePathContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IMemento;

/**
 * A tree viewer that displays a model.
 *
 * @since 3.3
 */
@SuppressWarnings("deprecation")
public class InternalTreeModelViewer extends TreeViewer implements IInternalTreeModelViewer, org.eclipse.debug.internal.ui.viewers.model.ITreeModelViewer
{

	private final IPresentationContext fContext;

	/**
	 * Current column presentation or <code>null</code>
	 */
	private IColumnPresentation fColumnPresentation = null;

	/**
	 * Map of columns presentation id to its visible columns id's (String[])
	 * When a columns presentation is not in the map, default settings are used.
	 */
	private final Map<String, String[]> fVisibleColumns = new HashMap<>();

	/**
	 * Map of column id's to persisted sizes
	 */
	private final Map<Object, Integer> fColumnSizes = new HashMap<>();

	/**
	 * Map of column presentation id's to an array of integers representing the column order
	 * for that presentation, or <code>null</code> if default.
	 */
	private final Map<String, int[]> fColumnOrder = new HashMap<>();

	/**
	 * Map of column presentation id to whether columns should be displayed
	 * for that presentation (the user can toggle columns on/off when a
	 * presentation is optional.
	 */
	private final Map<String, Boolean> fShowColumns = new HashMap<>();

	/**
	 * Item's tree path cache
	 */
	private static final String TREE_PATH_KEY = "TREE_PATH_KEY"; //$NON-NLS-1$

	/**
	 * Memento type for column sizes. Sizes are keyed by column presentation id
	 */
	private static final String COLUMN_SIZES = "COLUMN_SIZES"; //$NON-NLS-1$
	/**
	 * Memento type for the column order for a presentation context.
	 * A memento is created for each column presentation
	 */
	private static final String COLUMN_ORDER = "COLUMN_ORDER";     //$NON-NLS-1$
	/**
	 * Memento type for the visible columns for a presentation context.
	 * A memento is created for each column presentation keyed by column number
	 */
	private static final String VISIBLE_COLUMNS = "VISIBLE_COLUMNS";     //$NON-NLS-1$
	/**
	 * Memento type for whether columns are visible for a presentation context.
	 * Booleans are keyed by column presentation id
	 */
	private static final String SHOW_COLUMNS = "SHOW_COLUMNS";     //$NON-NLS-1$
	/**
	 * Memento key for the number of visible columns in a VISIBLE_COLUMNS memento
	 * or for the width of a column
	 */
	private static final String SIZE = "SIZE";	 //$NON-NLS-1$
	/**
	 * Memento key prefix a visible column
	 */
	private static final String COLUMN = "COLUMN";	 //$NON-NLS-1$

	/**
	 * True while performing an insert... we allow insert with filters
	 */
	private boolean fInserting = false;

	/**
	 * Whether to notify the content provider when an element is unmapped
	 */
	private boolean fNotifyUnmap = true;

	/**
	 * Persist column sizes when they change.
	 *
	 * @since 3.2
	 */
	class ColumnListener implements ControlListener {
		@Override
		public void controlMoved(ControlEvent e) {
			persistColumnOrder();
		}

		@Override
		public void controlResized(ControlEvent e) {
			persistColumnSizes();
		}
	}

	private final ColumnListener fListener = new ColumnListener();

	/**
	 * Proxy to cell modifier/editor support
	 */
	class CellModifierProxy implements ICellModifier {

		private ICellModifier fModifier;

		@Override
		public boolean canModify(Object element, String property) {
			IElementEditor editor = ViewerAdapterService.getElementEditor(element);
			if (editor != null) {
				fModifier = editor.getCellModifier(getPresentationContext(), element);
				if (fModifier != null) {
					if (fModifier.canModify(element, property)) {
						// install cell editor
						CellEditor cellEditor = editor.getCellEditor(getPresentationContext(), property, element, (Composite)getControl());
						if (cellEditor != null) {
							disposeCellEditors();
							CellEditor[] newEditors = new CellEditor[getVisibleColumns().length];
							for (int i = 0; i < newEditors.length; i++) {
								newEditors[i] = cellEditor;
							}
							setCellEditors(newEditors);
							return true;
						}
					}
				}
			}
			return false;
		}

		@Override
		public Object getValue(Object element, String property) {
			if (fModifier != null) {
				return fModifier.getValue(element, property);
			}
			return null;
		}

		@Override
		public void modify(Object element, String property, Object value) {
			if (fModifier != null) {
				if (element instanceof Item) {
					element = ((Item)element).getData();
				}
				fModifier.modify(element, property, value);
			}
		}

		/**
		 * Clears client's column editor and cell editors
		 */
		protected void clear() {
			fModifier = null;
			disposeCellEditors();
			setCellEditors(null);
		}

		/**
		 * Disposes current cell editors
		 */
		protected void disposeCellEditors() {
			CellEditor[] cellEditors = getCellEditors();
			if (cellEditors != null) {
				for (CellEditor editor : cellEditors) {
					if (editor != null) {
						editor.dispose();
					}
				}
			}
		}

	}

	private final CellModifierProxy fCellModifier;


	/**
	 * @param parent the parent composite
	 * @param style the widget style bits
	 * @param context the presentation context
	 */
	public InternalTreeModelViewer(Composite parent, int style, IPresentationContext context) {
		super(parent, style);
		if ((style & SWT.VIRTUAL) == 0) {
			throw new IllegalArgumentException("style must include SWT.VIRTUAL"); //$NON-NLS-1$
		}
		setUseHashlookup(true);
		fCellModifier = new CellModifierProxy();
		fContext = context;
		setContentProvider(createContentProvider());
		setLabelProvider(createLabelProvider());

		// A pop-up viewer is transient and does not automatically expand
		// and select elements up when requested by the model
		if ((style & SWT.POP_UP) != 0) {
			((ITreeModelContentProvider)getContentProvider()).setModelDeltaMask(
				~ITreeModelContentProvider.CONTROL_MODEL_DELTA_FLAGS);
		}
		if ((style & SWT.CHECK) != 0) {
			context.setProperty(ICheckUpdate.PROP_CHECK, Boolean.TRUE);
		}
	}

	/**
	 * @return content provider for this tree viewer
	 */
	protected ITreeModelContentProvider createContentProvider()
	{
		return new TreeModelContentProvider();
	}

	/**
	 * @return label provider for this tree viewer
	 */
	protected ITreeModelLabelProvider createLabelProvider()
	{
		return new TreeModelLabelProvider(this);
	}

	/*
	 * Workaround for bug 159461: when an item is cleared it's label is cleared. To
	 * avoid flashing, restore its label to its previous value.
	 */
	@Override
	protected void hookControl(Control control) {
		Tree treeControl = (Tree) control;
		treeControl.addListener(SWT.SetData, event -> {
			// to avoid flash, reset previous label data
			TreeItem item = (TreeItem) event.item;
			preserveItem(item);
		});
		super.hookControl(control);
	}

	/**
	 * @param item the item
	 */
	private void preserveItem(TreeItem item) {
		Object[] labels = (Object[]) item.getData(PREV_LABEL_KEY);
		if (labels != null) {
			for (int i = 0; i < labels.length; i++) {
				if (labels[i] != null) {
					item.setText(i, (String)labels[i]);
				}
			}
		}
		Object[] images = (Object[]) item.getData(PREV_IMAGE_KEY);
		if (images != null) {
			for (int i = 0; i < images.length; i++) {
				item.setImage(i, (Image) images[i]);
			}
		}
		Object[] fonts = (Object[]) item.getData(PREV_FONT_KEY);
		if (fonts != null) {
			for (int i = 0; i < fonts.length; i++) {
				item.setFont(i, (Font) fonts[i]);
			}
		}
		Object[] foregrounds = (Object[]) item.getData(PREV_FOREGROUND_KEY);
		if (foregrounds != null) {
			for (int i = 0; i < foregrounds.length; i++) {
				item.setForeground(i, (Color) foregrounds[i]);
			}
		}
		Object[] backgrounds = (Object[]) item.getData(PREV_BACKGROUND_KEY);
		if (backgrounds != null) {
			for (int i = 0; i < backgrounds.length; i++) {
				item.setBackground(i, (Color) backgrounds[i]);
			}
		}
		Boolean checked = (Boolean) item.getData(PREV_CHECKED_KEY);
		if (checked != null) {
			item.setChecked(checked.booleanValue());
		}
		Boolean grayed = (Boolean) item.getData(PREV_GRAYED_KEY);
		if (grayed != null) {
			item.setGrayed(grayed.booleanValue());
		}
	}

	/*
	 * Override the default handler for invalid selection to allow model selection
	 * policy to select the new selection.
	 */
	@Override
	protected void handleInvalidSelection(ISelection selection, ISelection newSelection) {
		IModelSelectionPolicy selectionPolicy = ViewerAdapterService.getSelectionPolicy(selection, getPresentationContext());
		if (selectionPolicy != null) {
			while (!selection.equals(newSelection)) {
				ISelection temp = newSelection;
				selection = selectionPolicy.replaceInvalidSelection(selection, newSelection);
				if (selection == null) {
					selection = TreeSelection.EMPTY;
				}
				if (!temp.equals(selection)) {
					setSelectionToWidget(selection, false);
					newSelection = getSelection();
				} else {
					break;
				}
			}
		}
		super.handleInvalidSelection(selection, newSelection);
	}

	@Override
	protected void handleDispose(DisposeEvent event) {
		if (fColumnPresentation != null) {
			fColumnPresentation.dispose();
		}
		fCellModifier.clear();

		super.handleDispose(event);
	}

	/**
	 * Returns this viewer's presentation context.
	 *
	 * @return presentation context
	 */
	@Override
	public IPresentationContext getPresentationContext() {
		return fContext;
	}

	@Override
	protected void unmapElement(Object element, Widget widget) {
		if (fNotifyUnmap) {
			// TODO: should we update the filter with the "new non-identical element"?
			((ITreeModelContentProvider) getContentProvider()).unmapPath((TreePath) widget.getData(TREE_PATH_KEY));
		}
		super.unmapElement(element, widget);
	}

	@Override
	protected void associate(Object element, Item item) {
		// see AbstractTreeViewer.associate(...)
		Object data = item.getData();
		if (data != null && data != element && equals(data, element)) {
			// elements are equal but not identical
			// -> being removed from map, but should not change filters
			try {
				fNotifyUnmap = false;
				super.associate(element, item);
			} finally {
				fNotifyUnmap = true;
			}
		} else {
			super.associate(element, item);
		}
	}

	/*
	 * We need tree paths when disposed/unmapped in any order so cache the tree
	 * path.
	 */
	@Override
	protected void mapElement(Object element, Widget widget) {
		super.mapElement(element, widget);
		if (widget instanceof Item) {
			widget.setData(TREE_PATH_KEY, getTreePathFromItem((Item)widget));
		} else {
			widget.setData(TREE_PATH_KEY, TreeModelContentProvider.EMPTY_TREE_PATH);
		}
	}

	/*
	 * Override because we allow inserting with filters present.
	 */
	@Override
	public void insert(Object parentElementOrTreePath, Object element, int position) {
		try {
			fInserting = true;
			super.insert(parentElementOrTreePath, element, position);
		} finally {
			fInserting = false;
		}
	}

	/*
	 * Override because we allow inserting with filters present.
	 */
	@Override
	protected boolean hasFilters() {
		if (fInserting) {
			return false;
		}
		return super.hasFilters();
	}

	@Override
	protected void unmapAllElements() {
		// Do nothing when called from StructuredViewer.setInput(), to avoid
		// clearing elements before viewer state is saved.
		// Bug 326917
		if (getControl().isDisposed()) {
			unmapAllElements();
		}
	}

	@Override
	protected void inputChanged(Object input, Object oldInput) {
		fCellModifier.clear();
		// Clear items map now that ITreeModelContentProvider.inputChanged() was already called.
		// Bug 326917
		super.unmapAllElements();
		((ITreeModelContentProvider)getContentProvider()).postInputChanged(this, oldInput, input);
		super.inputChanged(input, oldInput);

		resetColumns(input);
	}

	/**
	 * Configures the columns for the given viewer input.
	 *
	 * @param input the viewer input
	 */
	protected void resetColumns(Object input) {
		if (input != null) {
			// only change columns if the input is non-null (persist when empty)
			IColumnPresentationFactory factory = ViewerAdapterService.getColumnPresentationFactory(input);
			PresentationContext context = (PresentationContext) getPresentationContext();
			String type = null;
			if (factory != null) {
				type = factory.getColumnPresentationId(context, input);
			}
			if (type != null && factory != null) {
				if (fColumnPresentation != null) {
					if (!fColumnPresentation.getId().equals(type)) {
						// dispose old, create new
						fColumnPresentation.dispose();
						fColumnPresentation = null;
					}
				}
				if (fColumnPresentation == null) {
					fColumnPresentation = factory.createColumnPresentation(context, input);
					if (fColumnPresentation != null) {
						fColumnPresentation.init(context);
						configureColumns();
					}
				}
			} else {
				if (fColumnPresentation != null) {
					fColumnPresentation.dispose();
					fColumnPresentation = null;
					configureColumns();
				}
			}
		}
	}

	/**
	 * Configures the columns based on the current settings.
	 */
	protected void configureColumns() {
		if (fColumnPresentation != null) {
			IColumnPresentation build = null;
			if (isShowColumns(fColumnPresentation.getId())) {
				build = fColumnPresentation;
			}
			buildColumns(build);
		} else {
			// get rid of columns
			buildColumns(null);
		}
	}

	/**
	 * Toggles columns on/off for the current column presentation, if any.
	 *
	 * @param show whether to show columns if the current input supports
	 * 	columns
	 */
	public void setShowColumns(boolean show) {
		if (show) {
			if (!isShowColumns()) {
				fShowColumns.remove(fColumnPresentation.getId());
			}
		} else {
			if (isShowColumns()){
				fShowColumns.put(fColumnPresentation.getId(), Boolean.FALSE);
			}
		}
		refreshColumns();
	}

	/**
	 * Resets any persisted column size for the given columns
	 * @param columnIds the identifiers of the columns to reset
	 */
	public void resetColumnSizes(String[] columnIds) {
		for (String columnId : columnIds) {
			fColumnSizes.remove(columnId);
		}
	}

	/**
	 * Sets the id's of visible columns, or <code>null</code> to set default columns.
	 * Only affects the current column presentation.
	 *
	 * @param ids visible columns
	 */
	public void setVisibleColumns(String[] ids) {
		if (ids != null && ids.length == 0) {
			ids = null;
		}
		IColumnPresentation presentation = getColumnPresentation();
		if (presentation != null) {
			fColumnOrder.remove(presentation.getId());
			fVisibleColumns.remove(presentation.getId());
			if (ids != null) {
				// put back in table if not default
				String[] columns = presentation.getInitialColumns();
				if (columns.length == ids.length) {
					for (int i = 0; i < columns.length; i++) {
						if (!ids[i].equals(columns[i])) {
							fVisibleColumns.put(presentation.getId(), ids);
							break;
						}
					}
				} else {
					fVisibleColumns.put(presentation.getId(), ids);
				}
			}
			PresentationContext presentationContext = (PresentationContext) getPresentationContext();
			presentationContext.setColumns(getVisibleColumns());
			refreshColumns();
		}
	}

	@Override
	protected void internalRefresh(Object element, boolean updateLabels) {
		ITreeModelContentProvider contentProvider = (ITreeModelContentProvider)getContentProvider();

		if (element == null) {
			internalRefresh(getControl(), getRoot(), true, updateLabels);
			contentProvider.preserveState(TreePath.EMPTY);
		} else {
			Widget[] items = findItems(element);
			if (items.length != 0) {
				for (Widget item : items) {
					if (item instanceof TreeItem) {
						contentProvider.preserveState(getTreePathFromItem((TreeItem)item));
					} else {
						contentProvider.preserveState(TreePath.EMPTY);
					}
				}
			}
		}
		super.internalRefresh(element, updateLabels);
	}

	/**
	 * Refreshes the columns in the view, based on the viewer input.
	 */
	protected void refreshColumns() {
		configureColumns();
		refresh();
	}

	/**
	 * Returns whether columns are being displayed currently.
	 *
	 * @return if columns are being shown
	 */
	public boolean isShowColumns() {
		if (fColumnPresentation != null) {
			return isShowColumns(fColumnPresentation.getId());
		}
		return false;
	}

	/**
	 * Returns whether columns can be toggled on/off for the current input.
	 *
	 * @return whether columns can be toggled on/off for the current input
	 */
	public boolean canToggleColumns() {
		return fColumnPresentation != null && fColumnPresentation.isOptional();
	}

	protected boolean isShowColumns(String columnPresentationId) {
		Boolean bool = fShowColumns.get(columnPresentationId);
		if (bool == null) {
			return true;
		}
		return bool.booleanValue();
	}

	/**
	 * Creates new columns for the given presentation.
	 *
	 * TODO: does this need to be asynchronous?
	 *
	 * @param presentation the column presentation to build from
	 */
	protected void buildColumns(IColumnPresentation presentation) {
		// dispose current columns, persisting their weights
		Tree tree = getTree();
		final TreeColumn[] columns = tree.getColumns();
		String[] visibleColumnIds = getVisibleColumns();
		// remove all listeners before disposing - see bug 223233
		for (TreeColumn column : columns) {
			column.removeControlListener(fListener);
		}
		for (TreeColumn column : columns) {
			column.dispose();
		}
		PresentationContext presentationContext = (PresentationContext) getPresentationContext();
		if (presentation != null) {
			for (int i = 0; i < visibleColumnIds.length; i++) {
				String id = visibleColumnIds[i];
				String header = presentation.getHeader(id);
				// TODO: allow client to specify style
				TreeColumn column = new TreeColumn(tree, SWT.LEFT, i);
				column.setMoveable(true);
				column.setText(header);
				column.setWidth(1);
				ImageDescriptor image = presentation.getImageDescriptor(id);
				if (image != null) {
					column.setImage(((ITreeModelLabelProvider)getLabelProvider()).getImage(image));
				}
				column.setData(id);
			}
			int[] order = fColumnOrder.get(presentation.getId());
			if (order != null) {
				tree.setColumnOrder(order);
			}
			tree.setHeaderVisible(true);
			tree.setLinesVisible(true);
			presentationContext.setColumns(visibleColumnIds);
			setColumnProperties(visibleColumnIds);
			setCellModifier(fCellModifier);
		} else {
			tree.setHeaderVisible(false);
			tree.setLinesVisible(false);
			presentationContext.setColumns(null);
			setCellModifier(null);
			setColumnProperties(null);
		}

		int treeWidgetWidth = tree.getSize().x;
		int avg = treeWidgetWidth;
		if (visibleColumnIds != null) {
			avg /= visibleColumnIds.length;
		}

		if (avg == 0) {
			tree.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent e) {
					Tree tree2 = getTree();
					String[] visibleColumns = getVisibleColumns();
					if (visibleColumns != null) {
						int treeWidgetWidth1 = tree2.getSize().x;
						int avg1 = treeWidgetWidth1 / visibleColumns.length;
						initColumns(avg1, treeWidgetWidth1, visibleColumns);
					}
					tree2.removePaintListener(this);
				}
			});
		} else {
			initColumns(avg, treeWidgetWidth, visibleColumnIds);
		}
	}

	private void initColumns(int widthHint, int treeWidgetWidth, String[] visibleColumnIds) {
		for (TreeColumn column : getTree().getColumns()) {
			Object colData = column.getData();
			String columnId = colData instanceof String ? (String) colData : null;
			Integer width = fColumnSizes.get(colData);
			if (width == null) {
				int ans = getInitialColumnWidth(columnId, treeWidgetWidth, visibleColumnIds);
				if (ans == -1) {
					column.setWidth(widthHint);
				} else {
					column.setWidth(ans);
				}
			} else {
				column.setWidth(width.intValue());
			}
			column.addControlListener(fListener);
		}
	}

	/**
	 * Returns the current column presentation for this viewer, or <code>null</code>
	 * if none.
	 *
	 * @return column presentation or <code>null</code>
	 */
	public IColumnPresentation getColumnPresentation() {
		return fColumnPresentation;
	}

	/**
	 * Returns identifiers of the visible columns in this viewer, or <code>null</code>
	 * if there is currently no column presentation.
	 *
	 * @return visible columns or <code>null</code>
	 */
	@Override
	public String[] getVisibleColumns() {
		if (isShowColumns()) {
			IColumnPresentation presentation = getColumnPresentation();
			if (presentation != null) {
				String[] columns = fVisibleColumns.get(presentation.getId());
				if (columns == null) {
					return presentation.getInitialColumns();
				} else {
					String[] available = presentation.getAvailableColumns();
					for (String column : columns) {
						boolean columnAvailable = false;
						for (String element : available) {
							if (column.equals(element)) {
								columnAvailable = true;
							}
						}

						if (!columnAvailable || presentation.getHeader(column) == null) {
							// We found a column ID which is not in current list of available column IDs.
							// Or the presentation cannot return a header title for the given column.
							// Clear out saved column data for given column presentation.
							fVisibleColumns.remove(presentation.getId());
							fColumnOrder.remove(presentation.getId());
							fColumnSizes.remove(presentation.getId());
							return presentation.getInitialColumns();
						}
					}
				}
				return columns;
			}
		}
		return null;
	}

	/**
	 * Returns initial column width of a given column, or -1
	 * @param columnId column Id
	 * @param treeWidgetWidth tree widget width
	 * @param visibleColumnIds visible columns
	 *
	 * @return column width
	 */
	public int getInitialColumnWidth(String columnId, int treeWidgetWidth, String[] visibleColumnIds) {
		if (isShowColumns()) {
			IColumnPresentation presentation = getColumnPresentation();
			if (presentation instanceof IColumnPresentation2) {
				int ans = ((IColumnPresentation2) presentation).getInitialColumnWidth(columnId, treeWidgetWidth, visibleColumnIds);
				return ans;
			}
		}
		return -1;
	}

	/**
	 * Persists column sizes in cache
	 */
	protected void persistColumnSizes() {
		Tree tree = getTree();
		for (TreeColumn column :  tree.getColumns()) {
			Object id = column.getData();
			fColumnSizes.put(id, Integer.valueOf(column.getWidth()));
		}
	}

	/**
	 * Persists column ordering
	 */
	protected void persistColumnOrder() {
		IColumnPresentation presentation = getColumnPresentation();
		if (presentation != null) {
			Tree tree = getTree();
			int[] order = tree.getColumnOrder();
			if (order.length > 0) {
				for (int i = 0; i < order.length; i++) {
					if (i != order[i]) {
						// non default order
						fColumnOrder.put(presentation.getId(), order);
						return;
					}
				}
			}
			// default order
			fColumnOrder.remove(presentation.getId());
		}
	}

	/**
	 * Save viewer state into the given memento.
	 *
	 * @param memento the {@link IMemento} to save to
	 */
	public void saveState(IMemento memento) {
		if (!fColumnSizes.isEmpty()) {
			for (Entry<Object, Integer> entry : fColumnSizes.entrySet()) {
				IMemento sizes = memento.createChild(COLUMN_SIZES, (String)entry.getKey());
				sizes.putInteger(SIZE, entry.getValue().intValue());
			}
		}
		if (!fShowColumns.isEmpty()) {
			for (Entry<String, Boolean> entry : fShowColumns.entrySet()) {
				IMemento sizes = memento.createChild(SHOW_COLUMNS, entry.getKey());
				sizes.putString(SHOW_COLUMNS, entry.getValue().toString());
			}
		}
		if (!fVisibleColumns.isEmpty()) {
			for (Entry<String, String[]> entry : fVisibleColumns.entrySet()) {
				String id = entry.getKey();
				IMemento visible = memento.createChild(VISIBLE_COLUMNS, id);
				String[] columns = entry.getValue();
				visible.putInteger(SIZE, columns.length);
				for (int i = 0; i < columns.length; i++) {
					visible.putString(COLUMN+i, columns[i]);
				}
			}
		}
		if (!fColumnOrder.isEmpty()) {
			for (Entry<String, int[]> entry : fColumnOrder.entrySet()) {
				String id = entry.getKey();
				IMemento orderMemento = memento.createChild(COLUMN_ORDER, id);
				int[] order = entry.getValue();
				orderMemento.putInteger(SIZE, order.length);
				for (int i = 0; i < order.length; i++) {
					orderMemento.putInteger(COLUMN+i, order[i]);
				}
			}
		}
		// save presentation context properties
		IPresentationContext context = getPresentationContext();
		if (context instanceof PresentationContext) {
			PresentationContext pc = (PresentationContext) context;
			pc.saveProperites(memento);

		}
	}

	/**
	 * Initializes viewer state from the memento
	 *
	 * @param memento the {@link IMemento} to read from
	 */
	public void initState(IMemento memento) {
		for (IMemento child : memento.getChildren(COLUMN_SIZES)) {
			String id = child.getID();
			Integer size = child.getInteger(SIZE);
			if (size != null) {
				fColumnSizes.put(id, size);
			}
		}
		for (IMemento child : memento.getChildren(SHOW_COLUMNS)) {
			String id = child.getID();
			Boolean bool = Boolean.valueOf(child.getString(SHOW_COLUMNS));
			if (!bool.booleanValue()) {
				fShowColumns.put(id, bool);
			}
		}
		for (IMemento child : memento.getChildren(VISIBLE_COLUMNS)) {
			String id = child.getID();
			Integer integer = child.getInteger(SIZE);
			if (integer != null) {
				int length = integer.intValue();
				String[] columns = new String[length];
				for (int j = 0; j < length; j++) {
					columns[j] = child.getString(COLUMN+j);
				}
				fVisibleColumns.put(id, columns);
			}
		}
		for (IMemento child : memento.getChildren(COLUMN_ORDER)) {
			String id = child.getID();
			Integer integer = child.getInteger(SIZE);
			if (integer != null) {
				int length = integer.intValue();
				int[] order = new int[length];
				for (int j = 0; j < length; j++) {
					order[j] = child.getInteger(COLUMN+j).intValue();
				}
				fColumnOrder.put(id, order);
			}
		}
		// restore presentation context properties
		// save presentation context properties
		IPresentationContext context = getPresentationContext();
		if (context instanceof PresentationContext) {
			PresentationContext pc = (PresentationContext) context;
			pc.initProperties(memento);
		}
	}

	/**
	 * Returns whether the candidate selection should override the current
	 * selection.
	 * @param current the current selection
	 * @param candidate the candidate for the new selection
	 * @return if the current selection should be replaced with the candidate selection
	 */
	@Override
	public boolean overrideSelection(ISelection current, ISelection candidate) {
		IModelSelectionPolicy selectionPolicy = ViewerAdapterService.getSelectionPolicy(current, getPresentationContext());
		if (selectionPolicy == null) {
			return true;
		}
		if (selectionPolicy.contains(candidate, getPresentationContext())) {
			return selectionPolicy.overrides(current, candidate, getPresentationContext());
		}
		return !selectionPolicy.isSticky(current, getPresentationContext());
	}

	/*
	 * Consider selection policy
	 */
	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		if (!overrideSelection(getSelection(), selection)) {
			return;
		}
		super.setSelection(selection, reveal);
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal, boolean force) {
		trySelection(selection, reveal, force);
	}

	@Override
	public boolean trySelection(ISelection selection, boolean reveal, boolean force) {
		if (force || overrideSelection(getSelection(), selection)) {
			super.setSelection(selection, reveal);
			return true;
		}
		return false;
	}
	/**
	 * Registers the specified listener for view update notifications.
	 *
	 * @param listener listener
	 */
	@Override
	public void addViewerUpdateListener(IViewerUpdateListener listener) {
		((ITreeModelContentProvider)getContentProvider()).addViewerUpdateListener(listener);
	}

	/**
	 * Removes the specified listener from update notifications.
	 *
	 * @param listener listener
	 */
	@Override
	public void removeViewerUpdateListener(IViewerUpdateListener listener) {
		ITreeModelContentProvider cp = (ITreeModelContentProvider)getContentProvider();
		if (cp !=  null) {
			cp.removeViewerUpdateListener(listener);
		}
	}

	/**
	 * Registers the given listener for model delta notification.
	 *
	 * @param listener model delta listener
	 */
	@Override
	public void addModelChangedListener(IModelChangedListener listener) {
		((ITreeModelContentProvider)getContentProvider()).addModelChangedListener(listener);
	}

	/**
	 * Unregisters the given listener from model delta notification.
	 *
	 * @param listener model delta listener
	 */
	@Override
	public void removeModelChangedListener(IModelChangedListener listener) {
		ITreeModelContentProvider cp = (ITreeModelContentProvider)getContentProvider();
		if (cp !=  null) {
			cp.removeModelChangedListener(listener);
		}
	}

	@Override
	public void addStateUpdateListener(IStateUpdateListener listener) {
		((ITreeModelContentProvider)getContentProvider()).addStateUpdateListener(listener);
	}

	@Override
	public void removeStateUpdateListener(IStateUpdateListener listener) {
		ITreeModelContentProvider cp = (ITreeModelContentProvider)getContentProvider();
		if (cp !=  null) {
			cp.removeStateUpdateListener(listener);
		}
	}

	@Override
	protected void doUpdateItem(final Item item, Object element) {
		if (!(item instanceof TreeItem)) {
			return;
		}
		TreeItem treeItem = (TreeItem) item;
		if (treeItem.isDisposed()) {
			unmapElement(element, treeItem);
			return;
		}

		if ( !((ITreeModelLabelProvider)getLabelProvider()).update(getTreePathFromItem(item)) ) {
			if (element instanceof String) {
				item.setData(PREV_LABEL_KEY, new String[] { (String)element } );
			}
		}


		// As it is possible for user code to run the event
		// loop check here.
		if (item.isDisposed()) {
			unmapElement(element, item);
		}
	}

	@Override
	public void addLabelUpdateListener(ILabelUpdateListener listener) {
		((ITreeModelLabelProvider)getLabelProvider()).addLabelUpdateListener(listener);
	}

	@Override
	public void removeLabelUpdateListener(ILabelUpdateListener listener) {
		if (!getControl().isDisposed()) {
			((ITreeModelLabelProvider)getLabelProvider()).removeLabelUpdateListener(listener);
		}
	}

	/**
	 * Returns the item for the element at the given tree path or <code>null</code>
	 * if none.
	 *
	 * @param path tree path
	 * @return item or <code>null</code>
	 */
	public Widget findItem(TreePath path) {
		if (path.getSegmentCount() == 0) {
			return getTree();
		}
		Widget[] items = super.findItems(path.getLastSegment());
		if (items.length == 1) {
			return items[0];
		}
		for (Widget item : items) {
			if (getTreePathFromItem((Item)item).equals(path)) {
				return item;
			}
		}
		return null;
	}

	@Override
	public Item[] getChildren(Widget widget) {
		return super.getChildren(widget);
	}

	/**
	 * Returns the tree path for the given item.
	 * @param item the item to compute the {@link TreePath} for
	 * @return {@link TreePath}
	 */
	@Override
	protected TreePath getTreePathFromItem(Item item) {
		return super.getTreePathFromItem(item);
	}

//**************************************************************************
// These methods were copied from TreeViewer as a workaround for bug 183463:
// 		Expanded nodes in tree viewer flash on refresh

	/*
	 * workaround for bug 183463
	 */
	@Override
	protected void internalRefreshStruct(Widget widget, Object element,
			boolean updateLabels) {
		// clear all starting with the given widget
		if (widget instanceof Tree) {
			((Tree) widget).clearAll(true);
		} else if (widget instanceof TreeItem) {
			((TreeItem) widget).clearAll(true);
		}
		int index = 0;
		Widget parent = null;
		if (widget instanceof TreeItem) {
			TreeItem treeItem = (TreeItem) widget;
			parent = treeItem.getParentItem();
			if (parent == null) {
				parent = treeItem.getParent();
			}
			if (parent instanceof Tree) {
				index = ((Tree) parent).indexOf(treeItem);
			} else {
				index = ((TreeItem) parent).indexOf(treeItem);
			}
		}
		virtualRefreshExpandedItems(parent, widget, element, index);
	}

	/**
	 * Traverses the visible (expanded) part of the tree and updates child
	 * counts.
	 * <p>
	 * workaround for bug 183463
	 * </p>
	 * @param parent the parent of the widget, or <code>null</code> if the widget is the tree
	 * @param widget the parent widget
	 * @param element the underlying object
	 * @param index the index of the widget in the children array of its parent, or 0 if the widget is the tree
	 */
	private void virtualRefreshExpandedItems(Widget parent, Widget widget, Object element, int index) {
		if (widget instanceof Tree) {
			if (element == null) {
				((Tree) widget).setItemCount(0);
				return;
			}
			virtualLazyUpdateChildCount(widget, getChildren(widget).length);
		} else if (((TreeItem) widget).getExpanded()) {
			// prevent SetData callback
			preserveItem((TreeItem)widget);
			//((TreeItem)widget).setText(" "); //$NON-NLS-1$
			virtualLazyUpdateWidget(parent, index);
		} else {
			return;
		}
		Item[] items = getChildren(widget);
		for (int i = 0; i < items.length; i++) {
			Item item = items[i];
			Object data = item.getData();
			virtualRefreshExpandedItems(widget, item, data, i);
		}
	}

	/**
	 * workaround for bug 183463
	 *
	 * Update the child count
	 * @param widget the widget
	 * @param currentChildCount the current child count
	 */
	private void virtualLazyUpdateChildCount(Widget widget, int currentChildCount) {
		TreePath treePath;
		if (widget instanceof Item) {
			treePath = getTreePathFromItem((Item) widget);
		} else {
			treePath = TreePath.EMPTY;
		}
		((ILazyTreePathContentProvider) getContentProvider())
				.updateChildCount(treePath, currentChildCount);
	}

	/**
	 * Update the widget at index.
	 * <p>
	 * workaround for bug 183463
	 * </p>
	 * @param widget the widget
	 * @param index the index to update
	 */
	private void virtualLazyUpdateWidget(Widget widget, int index) {
		TreePath treePath;
		if (widget instanceof Item) {
			if (widget.getData() == null) {
				// we need to materialize the parent first
				// see bug 167668
				// however, that would be too risky
				// see bug 182782 and bug 182598
				// so we just ignore this call altogether
				// and don't do this: virtualMaterializeItem((TreeItem) widget);
				return;
			}
			treePath = getTreePathFromItem((Item) widget);
		} else {
			treePath = TreePath.EMPTY;
		}
		((ILazyTreePathContentProvider) getContentProvider())
				.updateElement(treePath, index);
	}

//**************************************************************************
// Another couple of methods copied from TreeViewer to workaround the UI bug 266189.
//
	@Override
	protected void createChildren(Widget widget) {
		Object element = widget.getData();
		if (element == null && widget instanceof TreeItem) {
			// parent has not been materialized
			virtualMaterializeItem((TreeItem) widget);
			// try getting the element now that updateElement was called
			element = widget.getData();
		}
		if (element ==  null) {
			// give up because the parent is still not materialized
			return;
		}
		Item[] children = getChildren(widget);
		if (children.length == 1 && children[0].getData() == null) {
			// found a dummy node
			virtualLazyUpdateChildCount(widget, children.length);
			children = getChildren(widget);
		}
		// DO NOT touch all children
		return;
	}

	private void virtualMaterializeItem(TreeItem treeItem) {
		if (treeItem.getData() != null) {
			// already materialized
			return;
		}

		int index;
		Widget parent = treeItem.getParentItem();
		if (parent == null) {
			parent = treeItem.getParent();
		}
		Object parentElement = parent.getData();
		if (parentElement != null) {
			if (parent instanceof Tree) {
				index = ((Tree) parent).indexOf(treeItem);
			} else {
				index = ((TreeItem) parent).indexOf(treeItem);
			}
			virtualLazyUpdateWidget(parent, index);
		}
	}


	/**
	 * Performs auto expand on an element at the specified path if the auto expand
	 * level dictates the element should be expanded.
	 *
	 * @param elementPath tree path to element to consider for expansion
	 */
	@Override
	public void autoExpand(TreePath elementPath) {
		int level = getAutoExpandLevel();
		if (level > 0 || level == ITreeModelViewer.ALL_LEVELS) {
			if (level == ITreeModelViewer.ALL_LEVELS || level >= elementPath.getSegmentCount()) {
				expandToLevel(elementPath, 1);
			}
		}
	}

	@Override
	public int findElementIndex(TreePath parentPath, Object element) {
		Widget parentItem = findItem(parentPath);
		if (parentItem != null) {
			Item[] children = getChildren(parentItem);
			for (int i = 0; i < children.length; i++) {
				Item item = children[i];
				Object data = item.getData();
				if ( (element != null && element.equals(data)) || (element == null && data == null) ) {
					return i;
				}
			}
		}
		return -1;
	}

	@Override
	public boolean getElementChildrenRealized(TreePath parentPath) {
		Widget parentItem = findItem(parentPath);
		if (parentItem != null) {
			for (Item element : getChildren(parentItem)) {
				if (element.getData() == null) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public Display getDisplay() {
		Control control = getControl();
		if (control != null && !control.isDisposed()) {
			return  control.getDisplay();
		}
		return null;
	}

	protected static final String[] STATE_PROPERTIES = new String[]{ IBasicPropertyConstants.P_TEXT, IBasicPropertyConstants.P_IMAGE };

	@Override
	public void update(Object element) {
		update(element, STATE_PROPERTIES);
	}

	/**
	 * Label data cache keys
	 * TODO: workaround for bug 159461
	 */
	static String PREV_LABEL_KEY = "PREV_LABEL_KEY"; //$NON-NLS-1$
	static String PREV_IMAGE_KEY = "PREV_IMAGE_KEY"; //$NON-NLS-1$
	static String PREV_FONT_KEY = "PREV_FONT_KEY"; //$NON-NLS-1$
	static String PREV_FOREGROUND_KEY = "PREV_FOREGROUND_KEY"; //$NON-NLS-1$
	static String PREV_BACKGROUND_KEY = "PREV_BACKGROUND_KEY"; //$NON-NLS-1$
	static String PREV_CHECKED_KEY = "PREV_CHECKED_KEY"; //$NON-NLS-1$
	static String PREV_GRAYED_KEY = "PREV_GRAYED_KEY"; //$NON-NLS-1$

	@Override
	public void setElementData(TreePath path, int numColumns, String[] labels, ImageDescriptor[] imageDescriptors,
		FontData[] fontDatas, RGB[] _foregrounds, RGB[] _backgrounds)
	{
		Widget widget = findItem(path);
		String[] columnIds = getVisibleColumns();

		if (widget != null && widget instanceof TreeItem && !widget.isDisposed()) {
			TreeItem item = (TreeItem)widget;
			/*Object data = item.getData();
			int itemCount = item.getItemCount();
			item.clearAll(false);
			item.setData(data);
			item.setItemCount(itemCount);*/

			for (int i=0; i<numColumns; i++){
				// text might be null if the launch has been terminated
				item.setText(i,(labels[i] == null ? IInternalDebugCoreConstants.EMPTY_STRING : labels[i]));
			}
			item.setData(PREV_LABEL_KEY, labels);

			if (imageDescriptors == null) {
				for (int i=0; i<numColumns; i++){
					item.setImage(i,null);
				}
				item.setData(PREV_IMAGE_KEY, null);
			} else {
				Image[] images = new Image[imageDescriptors.length];
				for (int i = 0; i < imageDescriptors.length; i++) {
					images[i] = ((ITreeModelLabelProvider)getLabelProvider()).getImage(imageDescriptors[i]);
				}
				if (columnIds == null) {
					item.setImage(images[0]);
				} else {
					item.setImage(images);
				}
				item.setData(PREV_IMAGE_KEY, images);
			}

			if (_foregrounds == null) {
				for (int i=0; i<numColumns; i++){
					item.setForeground(i,null);
				}
				item.setData(PREV_FOREGROUND_KEY, null);
			} else {
				Color[] foregrounds = new Color[_foregrounds.length];
				for (int i = 0; i< foregrounds.length; i++) {
					foregrounds[i] = ((ITreeModelLabelProvider)getLabelProvider()).getColor(_foregrounds[i]);
				}
				if (columnIds == null) {
					item.setForeground(0,foregrounds[0]);
				} else {
					for (int i = 0; i< foregrounds.length; i++) {
						item.setForeground(i, foregrounds[i]);
					}
				}
				item.setData(PREV_FOREGROUND_KEY, foregrounds);
			}

			if (_backgrounds == null) {
				for (int i=0; i<numColumns; i++){
					item.setBackground(i,null);
				}
				item.setData(PREV_BACKGROUND_KEY, null);
			} else {
				Color[] backgrounds = new Color[_backgrounds.length];
				for (int i = 0; i< backgrounds.length; i++) {
					backgrounds[i] = ((ITreeModelLabelProvider)getLabelProvider()).getColor(_backgrounds[i]);
				}
				if (columnIds == null) {
					item.setBackground(0,backgrounds[0]);
				} else {
					for (int i = 0; i< backgrounds.length; i++) {
						item.setBackground(i, backgrounds[i]);
					}
				}
				item.setData(PREV_BACKGROUND_KEY, backgrounds);
			}

			if (fontDatas == null) {
				for (int i=0; i<numColumns; i++){
					item.setFont(i,null);
				}
				item.setData(PREV_FONT_KEY, null);
			} else {
				Font[] fonts = new Font[fontDatas.length];
				for (int i = 0; i < fontDatas.length; i++) {
					fonts[i] = ((ITreeModelLabelProvider)getLabelProvider()).getFont(fontDatas[i]);
				}
				if (columnIds == null) {
					item.setFont(0,fonts[0]);
				} else {
					for (int i = 0; i < fonts.length; i++) {
						item.setFont(i, fonts[i]);
					}
				}
				item.setData(PREV_FONT_KEY, fonts);
			}
		}
	}

	@Override
	public ViewerLabel getElementLabel(TreePath path, String columnId) {
		if (path.getSegmentCount() == 0) {
			return null;
		}

		int columnIdx = -1;
		String[] visibleColumns = getVisibleColumns();
		if (columnId != null && visibleColumns != null) {
			int i = 0;
			for (i = 0; i < visibleColumns.length; i++) {
				if (columnId.equals(getVisibleColumns()[i])) {
					columnIdx = i;
					break;
				}
			}
			if (i == visibleColumns.length) {
				return null;
			}
		} else {
			columnIdx = 0;
		}
		TreeItem item = (TreeItem)findItem(path);

		if (item != null) {
			ViewerLabel label = new ViewerLabel(item.getText(columnIdx), item.getImage(columnIdx));
			label.setFont(item.getFont(columnIdx));
			label.setBackground(item.getBackground(columnIdx));
			label.setForeground(item.getForeground(columnIdx));
			return label;
		}
		return null;
	}

	@Override
	public void reveal(TreePath path, int index) {
		Widget item = findItem(path);
		TreeItem[] children = null;
		if (item instanceof TreeItem) {
			children = ((TreeItem)item).getItems();
		} else if (item instanceof Tree) {
			children = ((Tree)item).getItems();
		}
		if (children != null && index < children.length) {
			getTree().setTopItem(children[index]);
		}
	}

	@Override
	public int getChildCount(TreePath path) {
		if (path.getSegmentCount() == 0) {
			return ((Tree)getControl()).getItemCount();
		} else {
			Widget[] items = internalFindItems(path);
			if (items.length > 0) {
				if (items[0] instanceof TreeItem) {
					return ((TreeItem)items[0]).getItemCount();
				}
			}
		}
		return -1;
	}

	@Override
	public Object getChildElement(TreePath path, int index) {
		TreeItem childItem = null;
		if (path.getSegmentCount() == 0) {
			Tree tree = (Tree)getControl();
			try {
				childItem = tree.getItem(index);
			} catch (IllegalArgumentException e) {}
		} else {
			try {
				Widget[] items = internalFindItems(path);
				if (items.length > 0) {
					if (items[0] instanceof TreeItem) {
						childItem = ((TreeItem)items[0]).getItem(index);
					}
				}
			} catch (IllegalArgumentException e) {}
		}
		if (childItem != null) {
			return childItem.getData();
		}
		return null;
	}

	@Override
	public TreePath getTopElementPath() {
		TreeItem topItem = ((Tree)getControl()).getTopItem();
		if (topItem != null && !topItem.isDisposed() && topItem.getData() != null) {
			return getTreePathFromItem(topItem);
		}
		return null;
	}

	@Override
	public boolean saveElementState(TreePath path, ModelDelta delta, int flagsToSave) {
		Tree tree = (Tree) getControl();
		TreeItem[] selection = tree.getSelection();
		Set<TreeItem> set = new HashSet<>();
		Collections.addAll(set, selection);

		TreeItem[] items = null;
		Widget w = internalGetWidgetToSelect(path);
		if (w instanceof Tree) {
			delta.setChildCount(
				((ITreeModelContentProvider)getContentProvider()).viewToModelCount(path, tree.getItemCount()));
			if ((flagsToSave & IModelDelta.EXPAND) != 0) {
				delta.setFlags(delta.getFlags() | IModelDelta.EXPAND);
			}
			items = tree.getItems();
		} else if (w instanceof TreeItem) {
			TreeItem item = (TreeItem)w;
			if (item.getExpanded()) {
				int itemCount = item.getData() != null ? item.getItemCount() : -1;
				delta.setChildCount(((ITreeModelContentProvider)getContentProvider()).viewToModelCount(path, itemCount));
				if ((flagsToSave & IModelDelta.EXPAND) != 0) {
					delta.setFlags(delta.getFlags() | IModelDelta.EXPAND);
				}
			} else if ((flagsToSave & IModelDelta.COLLAPSE) != 0){
				delta.setFlags(delta.getFlags() | IModelDelta.COLLAPSE);
			}

			if (set.contains(item) && (flagsToSave & IModelDelta.SELECT) != 0) {
				delta.setFlags(delta.getFlags() | IModelDelta.SELECT);
			}
			items = ((TreeItem)w).getItems();
		}
		if (items != null && items.length != 0) {
			for (int i = 0; i < items.length; i++) {
				doSaveElementState(path, delta, items[i], set, i, flagsToSave);
			}
			return true;
		} else {
			return false;
		}
	}

	private void doSaveElementState(TreePath parentPath, ModelDelta delta, TreeItem item, Collection<TreeItem> set, int index, int flagsToSave) {
		Object element = item.getData();
		if (element != null) {
			boolean expanded = item.getExpanded();
			boolean selected = set.contains(item);
			int flags = IModelDelta.NO_CHANGE;
			if (expanded && (flagsToSave & IModelDelta.EXPAND) != 0) {
				flags = flags | IModelDelta.EXPAND;
			}
			if (!expanded && (flagsToSave & IModelDelta.COLLAPSE) != 0) {
				flags = flags | IModelDelta.COLLAPSE;
			}
			if (selected && (flagsToSave & IModelDelta.SELECT) != 0) {
				flags = flags | IModelDelta.SELECT;
			}
			if (expanded || flags != IModelDelta.NO_CHANGE) {
				int modelIndex = ((ITreeModelContentProvider)getContentProvider()).viewToModelIndex(parentPath, index);
				TreePath elementPath = parentPath.createChildPath(element);
				ModelDelta childDelta = delta.addNode(element, modelIndex, flags, -1);
				if (expanded) {
					// Only get the item count if the item is expanded.  Getting
					// item count triggers an update of the element (bug 335734).
					int itemCount = item.getItemCount();
					int numChildren = ((ITreeModelContentProvider)getContentProvider()).viewToModelCount(elementPath, itemCount);
					childDelta.setChildCount(numChildren);
					TreeItem[] items = item.getItems();
					for (int i = 0; i < items.length; i++) {
						doSaveElementState(elementPath, childDelta, items[i], set, i, flagsToSave);
					}
				}
			}
		}
	}

	@Override
	public void updateViewer(IModelDelta delta) {
		((ITreeModelContentProvider)getContentProvider()).updateModel(delta, ITreeModelContentProvider.ALL_MODEL_DELTA_FLAGS);
	}

	@Override
	public void setElementChecked(TreePath path, boolean checked, boolean grayed) {
		Widget widget = findItem(path);

		if (widget != null && widget instanceof TreeItem && !widget.isDisposed()) {
			TreeItem item = (TreeItem)widget;

			item.setChecked(checked);
			item.setGrayed(grayed);

			item.setData(PREV_CHECKED_KEY, checked ? Boolean.TRUE : Boolean.FALSE);
			item.setData(PREV_GRAYED_KEY, grayed ? Boolean.TRUE : Boolean.FALSE);
		}
	}

	@Override
	public boolean getElementChecked(TreePath path) {
		Widget widget = findItem(path);

		if (widget != null && widget instanceof TreeItem && !widget.isDisposed()) {
			TreeItem item = (TreeItem)widget;

			return item.getChecked();
		}
		return false;
	}

	/**
	 * Retrieves the element's check box grayed state.
	 *
	 * @param path the path of the element to set grayed
	 * @return grayed
	 */
	@Override
	public boolean getElementGrayed(TreePath path) {
		Widget widget = findItem(path);

		if (widget != null && widget instanceof TreeItem && !widget.isDisposed()) {
			TreeItem item = (TreeItem)widget;

			return item.getGrayed();
		}
		return false;
	}

	@Override
	public boolean getHasChildren(Object elementOrTreePath) {
		if (elementOrTreePath instanceof TreePath &&
			((TreePath)elementOrTreePath).getSegmentCount() == 0)
		{
			return getTree().getItemCount() > 0;
		}

		Widget[] items = internalFindItems(elementOrTreePath);
		if (items != null && items.length > 0) {
			if (items[0] instanceof TreeItem) {
				return ((TreeItem)items[0]).getItemCount() > 0;
			} else {
				return ((Tree)items[0]).getItemCount() > 0;
			}
		}

		return false;
	}

	@Override
	public TreePath[] getElementPaths(Object element) {
		Widget[] items = internalFindItems(element);
		TreePath[] paths = new TreePath[items.length];
		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof Tree) {
				paths[i] = TreePath.EMPTY;
			} else {
				paths[i] = getTreePathFromItem((Item)items[i]);
			}
		}
		return paths;
	}

	@Override
	protected void handleSelect(SelectionEvent event) {
		super.handleSelect(event);

		TreeItem item = (TreeItem) event.item;
		if (item != null) { // item can be null when de-selected (bug 296703)
			Object element = item.getData();
			IContentProvider contentProvider = getContentProvider();
			if (element != null && contentProvider instanceof TreeModelContentProvider) {
				TreePath path = getTreePathFromItem(item);

				if (event.detail == SWT.CHECK) {
					boolean checked = item.getChecked();
					boolean accepted = ((ITreeModelContentProvider) contentProvider).setChecked(path, checked);

					// if the listen rejects the change or there is not ICheckboxModelProxy, than revert the check state
					if (!accepted) {
						item.setChecked(!checked);
					} else {
						item.setData(PREV_CHECKED_KEY, Boolean.valueOf(checked));
					}
				} else {
					((TreeModelContentProvider) contentProvider).cancelRestore(path, IModelDelta.SELECT|IModelDelta.REVEAL);
				}
			}
		}
	}

	@Override
	protected void handleTreeExpand(TreeEvent event) {
		super.handleTreeExpand(event);
		IContentProvider contentProvider = getContentProvider();
		if (contentProvider instanceof TreeModelContentProvider && event.item.getData() != null) {
			TreePath path = getTreePathFromItem((TreeItem)event.item);
			((TreeModelContentProvider) contentProvider).cancelRestore(path, IModelDelta.COLLAPSE);
		}
	}

	@Override
	protected void handleTreeCollapse(TreeEvent event) {
		if (event.item.isDisposed()) {
			return;
		}
		super.handleTreeCollapse(event);
		IContentProvider contentProvider = getContentProvider();
		if (contentProvider instanceof TreeModelContentProvider && event.item.getData() != null) {
			TreePath path = getTreePathFromItem((TreeItem)event.item);
			((TreeModelContentProvider) contentProvider).cancelRestore(path, IModelDelta.EXPAND);
		}
	}

	@Override
	public void clearSelectionQuiet() {
		getTree().setSelection(new TreeItem[0]);
	}
}
