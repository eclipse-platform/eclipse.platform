/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;

/**
 * This class is copied from package org.eclipse.jface.viewers.TableEditorImpl
 * because the original has package access only.
 *
 * TODO: complain to UI about package access to API class.
 *
 */
public abstract class TableEditorImpl {
	private CellEditor fCellEditor;
	private CellEditor[] fCellEditors;
	private ICellModifier fCellModifier;
	private String[] fColumnProperties;
	private Item fTableItem;
	private int fColumnNumber;
	private ICellEditorListener fCellEditorListener;
	private FocusListener fFocusListener;
	private MouseListener fMouseListener;
	private int fDoubleClickExpirationTime;
	private StructuredViewer fViewer;

	TableEditorImpl(StructuredViewer viewer) {
		fViewer = viewer;
		initCellEditorListener();
	}

	/**
	 * Returns this <code>TableViewerImpl</code> viewer
	 *
	 * @return the viewer
	 */
	public StructuredViewer getViewer() {
		return fViewer;
	}

	private void activateCellEditor() {
		if (fCellEditors != null) {
			if (fCellEditors[fColumnNumber] != null && fCellModifier != null) {
				Object element = fTableItem.getData();
				String property = fColumnProperties[fColumnNumber];
				if (fCellModifier.canModify(element, property)) {
					fCellEditor = fCellEditors[fColumnNumber];
					// table.showSelection();
					fCellEditor.addListener(fCellEditorListener);
					Object value = fCellModifier.getValue(element, property);
					fCellEditor.setValue(value);
					// Tricky flow of control here:
					// activate() can trigger callback to cellEditorListener
					// which will clear cellEditor
					// so must get control first, but must still call activate()
					// even if there is no control.
					final Control control = fCellEditor.getControl();
					fCellEditor.activate();
					if (control == null)
						return;
					setLayoutData(fCellEditor.getLayoutData());
					setEditor(control, fTableItem, fColumnNumber);
					fCellEditor.setFocus();
					if (fFocusListener == null) {
						fFocusListener = new FocusAdapter() {
							@Override
							public void focusLost(FocusEvent e) {
								applyEditorValue();
							}
						};
					}
					control.addFocusListener(fFocusListener);
					fMouseListener = new MouseAdapter() {
						@Override
						public void mouseDown(MouseEvent e) {
							// time wrap?
							// check for expiration of doubleClickTime
							if (e.time <= fDoubleClickExpirationTime) {
								control.removeMouseListener(fMouseListener);
								cancelEditing();
								handleDoubleClickEvent();
							} else if (fMouseListener != null) {
								control.removeMouseListener(fMouseListener);
							}
						}
					};
					control.addMouseListener(fMouseListener);
				}
			}
		}
	}

	/**
	 * Activate a cell editor for the given mouse position.
	 */
	private void activateCellEditor(MouseEvent event) {
		if (fTableItem == null || fTableItem.isDisposed()) {
			// item no longer exists
			return;
		}
		int columnToEdit;
		int columns = getColumnCount();
		if (columns == 0) {
			// If no TableColumn, Table acts as if it has a single column
			// which takes the whole width.
			columnToEdit = 0;
		} else {
			columnToEdit = -1;
			for (int i = 0; i < columns; i++) {
				Rectangle bounds = getBounds(fTableItem, i);
				if (bounds.contains(event.x, event.y)) {
					columnToEdit = i;
					break;
				}
			}
			if (columnToEdit == -1) {
				return;
			}
		}

		fColumnNumber = columnToEdit;
		activateCellEditor();
	}

	/**
	 * Deactivates the currently active cell editor.
	 */
	public void applyEditorValue() {
		CellEditor c = fCellEditor;
		if (c != null) {
			// null out cell editor before calling save
			// in case save results in applyEditorValue being re-entered
			// see 1GAHI8Z: ITPUI:ALL - How to code event notification when
			// using cell editor ?
			fCellEditor = null;
			Item t = fTableItem;
			// don't null out table item -- same item is still selected
			if (t != null && !t.isDisposed()) {
				saveEditorValue(c, t);
			}
			setEditor(null, null, 0);
			c.removeListener(fCellEditorListener);
			Control control = c.getControl();
			if (control != null) {
				if (fMouseListener != null) {
					control.removeMouseListener(fMouseListener);
				}
				if (fFocusListener != null) {
					control.removeFocusListener(fFocusListener);
				}
			}
			c.deactivate();
		}
	}

	/**
	 * Cancels the active cell editor, without saving the value back to the
	 * domain model.
	 */
	public void cancelEditing() {
		if (fCellEditor != null) {
			setEditor(null, null, 0);
			fCellEditor.removeListener(fCellEditorListener);
			CellEditor oldEditor = fCellEditor;
			fCellEditor = null;
			oldEditor.deactivate();
		}
	}

	/**
	 * Start editing the given element.
	 *
	 * @param element
	 * @param column
	 */
	public void editElement(Object element, int column) {
		if (fCellEditor != null)
			applyEditorValue();

		setSelection(new StructuredSelection(element), true);
		Item[] selection = getSelection();
		if (selection.length != 1)
			return;

		fTableItem = selection[0];

		// Make sure selection is visible
		showSelection();
		fColumnNumber = column;
		activateCellEditor();

	}

	abstract Rectangle getBounds(Item item, int columnNumber);

	/**
	 * Return the array of CellEditors used in the viewer
	 *
	 * @return the cell editors
	 */
	public CellEditor[] getCellEditors() {
		return fCellEditors;
	}

	/**
	 * Get the cell modifier
	 *
	 * @return the cell modifier
	 */
	public ICellModifier getCellModifier() {
		return fCellModifier;
	}

	abstract int getColumnCount();

	/**
	 * Return the properties for the column
	 *
	 * @return the array of column properties
	 */
	public Object[] getColumnProperties() {
		return fColumnProperties;
	}

	abstract Item[] getSelection();

	/**
	 * Handles the mouse down event; activates the cell editor.
	 *
	 * @param event
	 *            the mouse event that should be handled
	 */
	public void handleMouseDown(MouseEvent event) {
		if (event.button != 1)
			return;

		if (fCellEditor != null)
			applyEditorValue();

		// activate the cell editor immediately. If a second mouseDown
		// is received prior to the expiration of the doubleClick time then
		// the cell editor will be deactivated and a doubleClick event will
		// be processed.
		//
		fDoubleClickExpirationTime = event.time + Display.getCurrent().getDoubleClickTime();

		Item[] items = getSelection();
		// Do not edit if more than one row is selected.
		if (items.length != 1) {
			fTableItem = null;
			return;
		}
		fTableItem = items[0];
		activateCellEditor(event);
	}

	private void initCellEditorListener() {
		fCellEditorListener = new ICellEditorListener() {
			@Override
			public void editorValueChanged(boolean oldValidState, boolean newValidState) {
				// Ignore.
			}

			@Override
			public void cancelEditor() {
				TableEditorImpl.this.cancelEditing();
			}

			@Override
			public void applyEditorValue() {
				TableEditorImpl.this.applyEditorValue();
			}
		};
	}

	/**
	 * Return whether there is an active cell editor.
	 *
	 * @return <code>true</code> if there is an active cell editor; otherwise
	 *         <code>false</code> is returned.
	 */
	public boolean isCellEditorActive() {
		return fCellEditor != null;
	}

	/**
	 * Saves the value of the currently active cell editor, by delegating to the
	 * cell modifier.
	 */
	private void saveEditorValue(CellEditor cellEditor, Item tableItem) {
		if (fCellModifier != null) {
			if (!cellEditor.isValueValid()) {
				// /Do what ???
			}
			String property = null;
			if (fColumnProperties != null && fColumnNumber < fColumnProperties.length)
				property = fColumnProperties[fColumnNumber];
			fCellModifier.modify(tableItem, property, cellEditor.getValue());
		}
	}

	/**
	 * Set the cell editors
	 *
	 * @param editors
	 */
	public void setCellEditors(CellEditor[] editors) {
		fCellEditors = editors;
	}

	/**
	 * Set the cell modifier
	 *
	 * @param modifier
	 */
	public void setCellModifier(ICellModifier modifier) {
		fCellModifier = modifier;
	}

	/**
	 * Set the column properties
	 *
	 * @param columnProperties
	 */
	public void setColumnProperties(String[] columnProperties) {
		fColumnProperties = columnProperties;
	}

	abstract void setEditor(Control w, Item item, int fColumnNumber);

	abstract void setLayoutData(CellEditor.LayoutData layoutData);

	abstract void setSelection(StructuredSelection selection, boolean b);

	abstract void showSelection();

	abstract void handleDoubleClickEvent();
}
