/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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

package org.eclipse.help.ui.internal.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class ICTable {

	private Table table;
	private TableViewer viewer;

	private final String NAME_COLUMN = Messages.RemoteICViewer_Name;
	private final String LOCATION_COLUMN = Messages.RemoteICViewer_URL;
	private final String STATUS_COLUMN = Messages.RemoteICViewer_Enabled;

	// Set column names
	private String[] columnNames = new String[] {NAME_COLUMN,
			LOCATION_COLUMN, STATUS_COLUMN};


	public ICTable(Composite parent) {

		// Create the table
		table = createTable(parent);
		// Create and setup the TableViewer
		viewer = createTableViewer();

		loadPreferences();
	}

	/**
	 * Release resources
	 */
	public void dispose() {
		// Tell the label provider to release its resources
		viewer.getLabelProvider().dispose();
	}

	/**
	 * Create the Table
	 */
	private Table createTable(Composite parent) {
		int style = SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION;

		TableLayout tableLayout = new TableLayout();
		Table table = new Table(parent, style);
		table.setLayout(tableLayout);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setFont(parent.getFont());

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		gridData.heightHint =  table.getItemHeight();
		gridData.horizontalSpan = 1;
		table.setLayoutData(gridData);



		ColumnLayoutData[] fTableColumnLayouts= {
				new ColumnWeightData(85),
				new ColumnWeightData(165),
				new ColumnWeightData(60)
		};

		TableColumn column;

		tableLayout.addColumnData(fTableColumnLayouts[0]);
		column = new TableColumn(table, SWT.NONE, 0);
		column.setResizable(fTableColumnLayouts[0].resizable);
		column.setText(NAME_COLUMN);

		tableLayout.addColumnData(fTableColumnLayouts[1]);
		column = new TableColumn(table, SWT.NONE, 1);
		column.setResizable(fTableColumnLayouts[1].resizable);
		column.setText(LOCATION_COLUMN);

		tableLayout.addColumnData(fTableColumnLayouts[2]);
		column = new TableColumn(table, SWT.NONE, 2);
		column.setResizable(fTableColumnLayouts[2].resizable);
		column.setText(STATUS_COLUMN);

		return table;
	}

	/**
	 * Create the TableViewer
	 */
	private TableViewer createTableViewer() {

		TableViewer viewer = new TableViewer(table);
		viewer.setUseHashlookup(true);
		viewer.setColumnProperties(columnNames);
		viewer.setContentProvider(new ICContentProvider());
		viewer.setLabelProvider(new ICLabelProvider());
		return viewer;
	}

	/**
	 * Proxy for the the RemoteICList which provides content
	 * for the Table. This class implements IRemoteHelpListViewer interface an
	 * registers itself with RemoteICList
	 */
	static class ICContentProvider implements IStructuredContentProvider
	{
		private List<IC> content = new ArrayList<>();

		@Override
		public void dispose() {
			content = null;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			content = (List<IC>)newInput;
		}

		@Override
		public IC[] getElements(Object inputElement) {
			return content.toArray(new IC[content.size()]);
		}

	}

	public static class ICLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return ((IC)element).getName();
			case 1:
				return ((IC)element).getHref();
			case 2:
				return ((IC)element).isEnabled() ? Messages.RemoteICLabelProvider_4 : Messages.RemoteICLabelProvider_5;
			default:
				return null;
			}
		}

	}




	/**
	 * @param ics the ordered remote InfoCenters
	 */
	public void update(List<IC> ics) {
		viewer.getContentProvider().inputChanged(viewer, null, ics);
		refresh();
	}

	/**
	 * Make sure the table viewer shows the latest copy of the ordered InfoCenters
	 */
	public void refresh() {
		viewer.refresh(getICs());
	}

	/**
	 * Return the column names in a collection
	 *
	 * @return List containing column names
	 */
	public List<String> getColumnNames() {
		return Arrays.asList(columnNames);
	}

	/**
	 * @return currently selected item
	 */
	public ISelection getSelection() {
		return viewer.getSelection();
	}

	/**
	 * Return the RemoteICList
	 */
	public List<IC> getICs() {
		ICContentProvider p = (ICContentProvider)viewer.getContentProvider();
		IC objs[] = p.getElements(null);
		List<IC> content = new ArrayList<>();
		Collections.addAll(content, objs);
		return content;
	}

	public void setICs(List<IC> ics)
	{
		List<IC> oldICs = getICs();
		for (int o=0;o<oldICs.size();o++)
			removeIC(oldICs.get(o));

		for (int i=0;i<ics.size();i++)
			addIC(ics.get(i));
	}

	public TableViewer getTableViewer()
	{
		return viewer;
	}
	/**
	 * Return the parent composite
	 */
	public Control getControl() {
		return table.getParent();
	}

	public Table getTable() {
		return table;
	}

	public void addIC(IC ic)
	{
		List<IC> content = getICs();
		content.add(ic);
		getTableViewer().getContentProvider().inputChanged(
				getTableViewer(), null, content);
		getTableViewer().add(ic);
		refresh();
	}

	public void editIC(IC ic)
	{
		List<IC> content = getICs();
		content.set(getTable().getSelectionIndex(), ic);
		getTableViewer().replace(ic,getTable().getSelectionIndex());
		getTableViewer().getContentProvider().inputChanged(
				getTableViewer(), null, content);
		refresh();
	}

	public void removeIC(IC ic)
	{
		List<IC> content = getICs();
		content.remove(ic);
		getTableViewer().getContentProvider().inputChanged(getTableViewer(), null, content);
		getTableViewer().remove(ic);
		refresh();
	}

	private void loadPreferences()
	{
		List<IC> ics = ICPreferences.getICs();
		for (int i=0;i<ics.size();i++)
			addIC(ics.get(i));
	}

}
