/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * @since 3.2
 */
public class AsynchronousTableViewer extends AsynchronousViewer implements Listener {

	private Table fTable;

	private TableEditor fTableEditor;

	private TableEditorImpl fTableEditorImpl;

	public AsynchronousTableViewer(Composite parent) {
		this(parent, SWT.VIRTUAL);
	}

	public AsynchronousTableViewer(Composite parent, int style) {
		this(new Table(parent, style));
	}

	/**
	 * Table must be SWT.VIRTUAL. This is intentional. Labels will never be
	 * retrieved for non-visible items.
	 *
	 * @see SWT#VIRTUAL
	 * @param table the backing table widget
	 */
	public AsynchronousTableViewer(Table table) {
		Assert.isTrue((table.getStyle() & SWT.VIRTUAL) != 0);
		fTable = table;
		hookControl(fTable);
		fTableEditor = new TableEditor(fTable);
		fTableEditorImpl = createTableEditorImpl();
	}

	@Override
	protected void hookControl(Control control) {
		super.hookControl(control);
		control.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				fTableEditorImpl.handleMouseDown(e);
			}
		});
	}

	@Override
	public synchronized void dispose() {
		fTableEditor.dispose();
		fTable.dispose();
		super.dispose();
	}

	@Override
	protected ISelection doAttemptSelectionToWidget(ISelection selection, boolean reveal) {
		if (acceptsSelection(selection)) {
			List<?> list = ((IStructuredSelection) selection).toList();
			if (list == null) {
				fTable.deselectAll();
				return StructuredSelection.EMPTY;
			}

			int[] indices = new int[list.size()];
			ModelNode[] nodes = getModel().getRootNode().getChildrenNodes();
			if (nodes != null) {
				int index = 0;

				// I'm not sure if it would be faster to check TableItems first...
				for (int i = 0; i < nodes.length; i++) {
					Object element = nodes[i].getElement();
					if (list.contains(element)) {
						indices[index] = i;
						index++;
					}
				}

				fTable.setSelection(indices);
				if (reveal && indices.length > 0) {
					TableItem item = fTable.getItem(indices[0]);
					fTable.showItem(item);
				}
			}
		}
		return StructuredSelection.EMPTY;
	}

	@Override
	protected boolean acceptsSelection(ISelection selection) {
		return selection instanceof IStructuredSelection;
	}

	@Override
	protected ISelection getEmptySelection() {
		return StructuredSelection.EMPTY;
	}

	protected Widget getParent(Widget widget) {
		if (widget instanceof TableItem) {
			return fTable;
		}
		return null;
	}

	@Override
	protected List getSelectionFromWidget() {
		TableItem[] selection = fTable.getSelection();
		List<Object> datas = new ArrayList<>(selection.length);
		for (TableItem element : selection) {
			datas.add(element.getData());
		}
		return datas;
	}

	@Override
	public Control getControl() {
		return fTable;
	}

	public Table getTable() {
		return (Table) getControl();
	}

	@Override
	protected void internalRefresh(ModelNode node) {
		super.internalRefresh(node);
		if (node.getElement().equals(getInput())) {
			updateChildren(node);
		}
	}

	@Override
	protected void restoreLabels(Item item) {
		TableItem tableItem = (TableItem) item;
		String[] values = (String[]) tableItem.getData(OLD_LABEL);
		Image[] images = (Image[]) tableItem.getData(OLD_IMAGE);
		if (values != null) {
			tableItem.setText(values);
			tableItem.setImage(images);
		}
	}

	@Override
	public void setLabels(Widget widget, String[] labels, ImageDescriptor[] imageDescriptors) {
		TableItem item = (TableItem) widget;
		item.setText(labels);
		item.setData(OLD_LABEL, labels);
		Image[] images = new Image[labels.length];
		item.setData(OLD_IMAGE, images);
		if (imageDescriptors != null) {
			for (int i = 0; i < images.length; i++) {
				if (i < imageDescriptors.length) {
					images[i] = getImage(imageDescriptors[i]);
				}
			}
		}
		item.setImage(images);
	}

	@Override
	public void setColors(Widget widget, RGB[] foregrounds, RGB[] backgrounds) {
		TableItem item = (TableItem) widget;
		if (foregrounds == null) {
			foregrounds = new RGB[fTable.getColumnCount()];
		}
		if (backgrounds == null) {
			backgrounds = new RGB[fTable.getColumnCount()];
		}

		for (int i = 0; i < foregrounds.length; i++) {
			Color fg = getColor(foregrounds[i]);
			item.setForeground(i, fg);
		}
		for (int i = 0; i < backgrounds.length; i++) {
			Color bg = getColor(backgrounds[i]);
			item.setBackground(i, bg);
		}
	}

	@Override
	public void setFonts(Widget widget, FontData[] fontDatas) {
		TableItem item = (TableItem) widget;
		if (fontDatas != null) {
			for (int i = 0; i < fontDatas.length; i++) {
				Font font = getFont(fontDatas[i]);
				item.setFont(i, font);
			}
		}
	}

	public void setColumnHeaders(final String[] headers) {
		fTableEditorImpl.setColumnProperties(headers);
	}

	public Object[] getColumnProperties() {
		return fTableEditorImpl.getColumnProperties();
	}

	public void showColumnHeader(final boolean showHeaders) {
		WorkbenchJob job = new WorkbenchJob("Set Header Visibility") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (!fTable.isDisposed())
				{
					fTable.setHeaderVisible(showHeaders);
				}
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.INTERACTIVE);
		job.setSystem(true);
		job.schedule();
	}

	@Override
	public void reveal(Object element) {
		Assert.isNotNull(element);
		Widget w = findItem(element);
		if (w instanceof TableItem) {
			getTable().showItem((TableItem) w);
		}
	}

	/**
	 * Sets the cell editors of this table viewer.
	 *
	 * @param editors
	 *            the list of cell editors
	 */
	public void setCellEditors(CellEditor[] editors) {
		fTableEditorImpl.setCellEditors(editors);
	}

	/**
	 * Sets the cell modifier of this table viewer.
	 *
	 * @param modifier
	 *            the cell modifier
	 */
	public void setCellModifier(ICellModifier modifier) {
		fTableEditorImpl.setCellModifier(modifier);
	}

	protected TableEditorImpl createTableEditorImpl() {
		return new TableEditorImpl(this) {
			@Override
			Rectangle getBounds(Item item, int columnNumber) {
				return ((TableItem) item).getBounds(columnNumber);
			}

			@Override
			int getColumnCount() {
				return getTable().getColumnCount();
			}

			@Override
			Item[] getSelection() {
				return getTable().getSelection();
			}

			@Override
			void setEditor(Control w, Item item, int columnNumber) {
				fTableEditor.setEditor(w, (TableItem) item, columnNumber);
			}

			@Override
			void setSelection(StructuredSelection selection, boolean b) {
				AsynchronousTableViewer.this.setSelection(selection, b);
			}

			@Override
			void showSelection() {
				getTable().showSelection();
			}

			@Override
			void setLayoutData(CellEditor.LayoutData layoutData) {
				fTableEditor.grabHorizontal = layoutData.grabHorizontal;
				fTableEditor.horizontalAlignment = layoutData.horizontalAlignment;
				fTableEditor.minimumWidth = layoutData.minimumWidth;
			}

			@Override
			void handleDoubleClickEvent() {
				Viewer viewer = getViewer();
				fireDoubleClick(new DoubleClickEvent(viewer, viewer.getSelection()));
				fireOpen(new OpenEvent(viewer, viewer.getSelection()));
			}
		};
	}

	@Override
	protected ISelection newSelectionFromWidget() {
		Control control = getControl();
		if (control == null || control.isDisposed()) {
			return StructuredSelection.EMPTY;
		}
		List<?> list = getSelectionFromWidget();
		return new StructuredSelection(list);
	}

	public CellEditor[] getCellEditors() {
		return fTableEditorImpl.getCellEditors();
	}

	public ICellModifier getCellModifier() {
		return fTableEditorImpl.getCellModifier();
	}

	public boolean isCellEditorActive() {
		return fTableEditorImpl.isCellEditorActive();
	}

	/**
	 * This is not asynchronous. This method must be called in the UI Thread.
	 */
	public void cancelEditing() {
		fTableEditorImpl.cancelEditing();
	}

	/**
	 * This is not asynchronous. This method must be called in the UI Thread.
	 *
	 * @param element
	 *            The element to edit. Each element maps to a row in the Table.
	 * @param column
	 *            The column to edit
	 */
	public void editElement(Object element, int column) {
		fTableEditorImpl.editElement(element, column);
	}

	protected int indexForElement(Object element) {
		ViewerComparator comparator = getComparator();
		if (comparator == null) {
			return fTable.getItemCount();
		}
		int count = fTable.getItemCount();
		int min = 0, max = count - 1;
		while (min <= max) {
			int mid = (min + max) / 2;
			Object data = fTable.getItem(mid).getData();
			int compare = comparator.compare(this, data, element);
			if (compare == 0) {
				// find first item > element
				while (compare == 0) {
					++mid;
					if (mid >= count) {
						break;
					}
					data = fTable.getItem(mid).getData();
					compare = comparator.compare(this, data, element);
				}
				return mid;
			}
			if (compare < 0) {
				min = mid + 1;
			} else {
				max = mid - 1;
			}
		}
		return min;
	}

	public void add(Object element) {
		if (element != null) {
			add(new Object[] { element });
		}
	}

	public void add(Object[] elements) {
		if (elements == null || elements.length == 0)
		 {
			return; // done
		}
		((AsynchronousTableModel)getModel()).add(elements);
	}

	public void remove(Object element) {
		if (element != null) {
			remove(new Object[] { element });
		}
	}

	public void remove(final Object[] elements) {
		if (elements == null || elements.length == 0)
		 {
			return; // done
		}
		((AsynchronousTableModel)getModel()).remove(elements);
	}

	public void insert(Object element, int position) {
		if (element != null) {
			insert(new Object[] { element }, position);
		}
	}

	public void insert(Object[] elements, int position) {
		if (elements == null || elements.length == 0) {
			return;
		}
		((AsynchronousTableModel)getModel()).insert(elements, position);
	}

	public void replace(Object element, Object replacement) {
		if (element == null || replacement == null)
		 {
			throw new IllegalArgumentException("unexpected null parameter"); //$NON-NLS-1$
		}
		((AsynchronousTableModel)getModel()).replace(element, replacement);
	}

	@Override
	protected AsynchronousModel createModel() {
		return new AsynchronousTableModel(this);
	}

	@Override
	protected void setItemCount(Widget parent, int itemCount) {
		fTable.setItemCount(itemCount);
	}

	protected int getVisibleItemCount(int top) {
		int itemCount = fTable.getItemCount();
		return Math.min((fTable.getBounds().height / fTable.getItemHeight()) + 2, itemCount - top);
	}

	@Override
	public AbstractUpdatePolicy createUpdatePolicy() {
		return new TableUpdatePolicy();
	}

	@Override
	protected Widget getParentWidget(Widget widget) {
		if (widget instanceof TableItem) {
			return ((TableItem)widget).getParent();
		}
		return null;
	}

	@Override
	protected Widget getChildWidget(Widget parent, int index) {
		if (index < fTable.getItemCount()) {
			return fTable.getItem(index);
		}
		return null;
	}

	@Override
	protected void clear(Widget item) {
		if (item instanceof TableItem) {
			int i = fTable.indexOf((TableItem)item);
			if (i >= 0) {
				fTable.clear(i);
			}
		}
	}

	@Override
	protected void clearChild(Widget parent, int childIndex) {
		if (parent instanceof Table) {
			fTable.clear(childIndex);
		}
	}

	@Override
	protected void clearChildren(Widget item) {
		if (item instanceof Table) {
			fTable.clearAll();
		}
	}

	@Override
	protected int indexOf(Widget parent, Widget child) {
		if (parent instanceof Table) {
			return ((Table)parent).indexOf((TableItem)child);
		}
		return -1;
	}

}
