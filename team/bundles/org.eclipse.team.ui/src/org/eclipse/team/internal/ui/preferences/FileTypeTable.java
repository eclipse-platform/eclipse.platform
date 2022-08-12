/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

package org.eclipse.team.internal.ui.preferences;

import java.util.Collection;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.core.Team;
import org.eclipse.team.internal.ui.PixelConverter;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.TeamUIMessages;


public class FileTypeTable implements ICellModifier, IStructuredContentProvider, ITableLabelProvider {

	private final static int COMBO_INDEX_BINARY= 0;
	private final static int COMBO_INDEX_TEXT= 1;
	private final static String [] MODES_TEXT= { TeamUIMessages.FileTypeTable_0, TeamUIMessages.FileTypeTable_1 }; //

	private final static int COMBO_INDEX_SAVE= 0;
	private final static int COMBO_INDEX_DONT_SAVE= 1;
	private static final String [] SAVE_TEXT= { TeamUIMessages.FileTypeTable_2, TeamUIMessages.FileTypeTable_3 }; //

	private static final class FileTypeComparator extends ViewerComparator {

		public FileTypeComparator() {
		}

		private int getCategory(Object element) {
			if (element instanceof Extension)
				return 0;
			if (element instanceof Name) {
				return 1;
			}
			return 2;
		}

		@Override
		public int compare(Viewer viewer,Object e1,Object e2) {
			final int compare= getCategory(e1) - getCategory(e2);
			if (compare != 0)
				return compare;
			return super.compare(viewer, ((Item)e1).name,  ((Item)e2).name);
		}
	}

	public abstract static class Item implements Comparable {
		public final String name;
		public boolean save;
		public int mode;
		public boolean contributed;

		public Item(String name, boolean contributed) { this.name= name; this.contributed = contributed; save= true; mode= Team.BINARY; }

		@Override
		public int compareTo(Object o) {
			return name.compareTo(((Item)o).name);
		}
	}

	public static class Extension extends Item {
		public Extension(String name, boolean contributed) { super(name, contributed); }
	}

	public static class Name extends Item {
		public Name(String name, boolean contributed) { super(name, contributed); }
	}

	private final static int COLUMN_PADDING = 5;

	protected static final String ITEM = "item"; //$NON-NLS-1$
	protected static final String PROPERTY_MODE= "mode"; //$NON-NLS-1$
	protected static final String PROPERTY_SAVE= "save"; //$NON-NLS-1$

	private final TableViewer fTableViewer;
	private final List fItems;
	private final boolean fShowSaveColumn;

	public FileTypeTable(Composite composite, List items, boolean showSaveColumn) {

		fShowSaveColumn= showSaveColumn;
		fItems= items;


		/**
		 * Create a table.
		 */
		final Table table = new Table(composite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		var gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 300;
		table.setLayoutData(gd);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		final PixelConverter converter= SWTUtils.createDialogPixelConverter(composite);

		/**
		 * The 'Extension' column
		 */
		final TableColumn fileColumn = new TableColumn(table, SWT.NONE, 0);
		fileColumn.setWidth(converter.convertWidthInCharsToPixels(TeamUIMessages.FileTypeTable_4.length() + COLUMN_PADDING));
		fileColumn.setText(TeamUIMessages.FileTypeTable_4);

		/**
		 * The 'Mode' column
		 */
		final TableColumn modeColumn = new TableColumn(table, SWT.NONE, 1);
		int length;
		try {
			length = TeamUIMessages.FileTypeTable_5.length();
			length = Math.max(length, TeamUIMessages.FileTypeTable_0.length());
			length = Math.max(length, TeamUIMessages.FileTypeTable_1.length());
		} catch (RuntimeException e) {
			// There may be an unbound message so just pick a reasonable length
			length = 15;
		}
		modeColumn.setWidth(converter.convertWidthInCharsToPixels(length + COLUMN_PADDING));
		modeColumn.setText(TeamUIMessages.FileTypeTable_5);

		/**
		 * The 'Save' column
		 */
		if (fShowSaveColumn) {
			final TableColumn saveColumn = new TableColumn(table, SWT.NONE, 2);
			saveColumn.setWidth(converter.convertWidthInCharsToPixels(TeamUIMessages.FileTypeTable_6.length() + COLUMN_PADDING));
			saveColumn.setText(TeamUIMessages.FileTypeTable_6);
		}

		/**
		 * Create a viewer for the table.
		 */
		fTableViewer = new TableViewer(table);
		fTableViewer.setContentProvider(this);
		fTableViewer.setLabelProvider(this);
		fTableViewer.setComparator(new FileTypeComparator());

		/**
		 * Add a cell editor in the Keyword Substitution Mode column
		 */
		new TableEditor(table);

		final CellEditor modeEditor = new ComboBoxCellEditor(table, MODES_TEXT, SWT.READ_ONLY);
		final CellEditor saveEditor= new ComboBoxCellEditor(table, SAVE_TEXT, SWT.READ_ONLY);

		if (fShowSaveColumn) {
			fTableViewer.setCellEditors(new CellEditor[] { null, modeEditor, saveEditor });
			fTableViewer.setColumnProperties(new String [] { ITEM, PROPERTY_MODE, PROPERTY_SAVE });
		} else {
			fTableViewer.setCellEditors(new CellEditor [] { null, modeEditor });
			fTableViewer.setColumnProperties(new String [] { ITEM, PROPERTY_MODE });
		}

		fTableViewer.setCellModifier(this);

		fTableViewer.setInput(fItems);
	}


	@Override
	public Object getValue(Object element, String property) {

		final Item item= (Item)element;

		if (PROPERTY_MODE.equals(property)) {
			if (item.mode == Team.BINARY)
				return Integer.valueOf(COMBO_INDEX_BINARY);
			if (item.mode == Team.TEXT)
				return Integer.valueOf(COMBO_INDEX_TEXT);
		}

		if (fShowSaveColumn && PROPERTY_SAVE.equals(property)) {
			return Integer.valueOf(item.save ? COMBO_INDEX_SAVE : COMBO_INDEX_DONT_SAVE);
		}
		return null;
	}

	@Override
	public boolean canModify(Object element, String property) {
		return PROPERTY_MODE.equals(property) || (fShowSaveColumn && PROPERTY_SAVE.equals(property));
	}

	@Override
	public void modify(Object element, String property, Object value) {

		final IStructuredSelection selection = fTableViewer.getStructuredSelection();
		final Item item= (Item)selection.getFirstElement();
		if (item == null)
			return;

		final int comboIndex = ((Integer)value).intValue();

		if (PROPERTY_MODE.equals(property)) {
			if (comboIndex == COMBO_INDEX_BINARY)
				item.mode= Team.BINARY;
			if (comboIndex == COMBO_INDEX_TEXT)
				item.mode= Team.TEXT;
		}

		if (fShowSaveColumn && PROPERTY_SAVE.equals(property)) {
			item.save= COMBO_INDEX_SAVE == comboIndex;
		}
		fTableViewer.refresh(item);
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {

		final Item item= (Item) element;

		if (columnIndex == 0) {
			String label = (item instanceof Extension ? "*." : "") + item.name; //$NON-NLS-1$ //$NON-NLS-2$
			label = TextProcessor.process(label, ".*"); //$NON-NLS-1$
			return label;
		}

		if (columnIndex == 1) {
			if (item.mode == Team.BINARY) {
				return MODES_TEXT[COMBO_INDEX_BINARY];
			} else if (item.mode == Team.TEXT) {
				return MODES_TEXT[COMBO_INDEX_TEXT];
			}
		}

		if (columnIndex == 2) {
			if (fShowSaveColumn) return SAVE_TEXT[item.save ? COMBO_INDEX_SAVE : COMBO_INDEX_DONT_SAVE];
		}

		return null;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {}

	@Override
	public void dispose() {}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {}

	@Override
	public Object[] getElements(Object inputElement) {
		return ((Collection)inputElement).toArray();
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	public IStructuredSelection getSelection() {
		return fTableViewer.getStructuredSelection();
	}

	public void setInput(List items) {
		fItems.clear();
		fItems.addAll(items);
		fTableViewer.refresh();
	}

	public TableViewer getViewer() {
		return fTableViewer;
	}
}
