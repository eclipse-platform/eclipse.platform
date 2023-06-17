/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.preferences;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.ant.core.Property;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.ColumnSorter;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

public class AntPropertiesBlock {

	/**
	 * Constant representing the id of the settings for the property table column widths
	 * 
	 * @since 3.5
	 */
	private static final String PROPERTY_COLUMN_WIDTH = "ant.properties.block.property.columnWidth"; //$NON-NLS-1$

	/**
	 * Constant representing the id of the settings for the property table sort column
	 * 
	 * @since 3.5
	 */
	private static final String PROPERTY_SORT_COLUMN = "ant.properties.block.property.sortColumn"; //$NON-NLS-1$

	/**
	 * Constant representing the id of the settings for the property table sort direction
	 * 
	 * @since 3.5
	 */
	private static final String PROPERTY_SORT_DIRECTION = "ant.properties.block.property.sortDirection"; //$NON-NLS-1$

	private IAntBlockContainer container;

	private Button editButton;
	private Button removeButton;
	private Button addButton;

	private Button addFileButton;
	private Button addExternalFileButton;
	private Button removeFileButton;

	private TableViewer propertyTableViewer;
	private TableViewer fileTableViewer;

	private final AntObjectLabelProvider labelProvider = new AntObjectLabelProvider();

	private IDialogSettings dialogSettings;

	private boolean tablesEnabled = true;

	private final String[] fTableColumnHeaders = { AntPreferencesMessages.AntPropertiesBlock_0, AntPreferencesMessages.AntPropertiesBlock_5,
			AntPreferencesMessages.AntPropertiesBlock_6 };

	private final ColumnLayoutData[] fTableColumnLayouts = { new ColumnWeightData(30), // , 190, true),
			new ColumnWeightData(40), // , 190, true),
			new ColumnWeightData(30) // , 190, true)
	};

	/**
	 * Button listener that delegates for widget selection events.
	 */
	private SelectionAdapter buttonListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			if (event.widget == addButton) {
				addProperty();
			} else if (event.widget == editButton) {
				edit();
			} else if (event.widget == removeButton) {
				remove(propertyTableViewer);
			} else if (event.widget == addFileButton) {
				addPropertyFile();
			} else if (event.widget == addExternalFileButton) {
				addExternalPropertyFile();
			} else if (event.widget == removeFileButton) {
				remove(fileTableViewer);
			}
		}
	};

	/**
	 * Key listener that delegates for key pressed events.
	 */
	private KeyAdapter keyListener = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent event) {
			if (event.getSource() == propertyTableViewer) {
				if (removeButton.isEnabled() && event.character == SWT.DEL && event.stateMask == 0) {
					remove(propertyTableViewer);
				}
			} else if (event.getSource() == fileTableViewer) {
				if (removeFileButton.isEnabled() && event.character == SWT.DEL && event.stateMask == 0) {
					remove(fileTableViewer);
				}
			}
		}
	};

	/**
	 * Selection changed listener that delegates selection events.
	 */
	private ISelectionChangedListener tableListener = event -> {
		if (tablesEnabled) {
			if (event.getSource() == propertyTableViewer) {
				propertyTableSelectionChanged((IStructuredSelection) event.getSelection());
			} else if (event.getSource() == fileTableViewer) {
				fileTableSelectionChanged((IStructuredSelection) event.getSelection());
			}
		}
	};

	public AntPropertiesBlock(IAntBlockContainer container) {
		this.container = container;
	}

	private void addPropertyFile() {
		String title = AntPreferencesMessages.AntPropertiesFileSelectionDialog_12;
		String message = AntPreferencesMessages.AntPropertiesFileSelectionDialog_13;
		String filterExtension = "properties"; //$NON-NLS-1$
		String filterMessage = AntPreferencesMessages.AntPropertiesFileSelectionDialog_14;

		Object[] existingFiles = getPropertyFiles();
		List<IFile> propFiles = new ArrayList<>(existingFiles.length);
		for (Object existingFile : existingFiles) {
			String file = (String) existingFile;
			try {
				propFiles.add(AntUtil.getFileForLocation(VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(file), null));
			}
			catch (CoreException e) {
				AntUIPlugin.log(e.getStatus());
			}
		}

		FileSelectionDialog dialog = new FileSelectionDialog(propertyTableViewer.getControl().getShell(), propFiles, title, message, filterExtension, filterMessage);
		if (dialog.open() == Window.OK) {
			for (Object element : dialog.getResult()) {
				IFile file = (IFile) element;
				String varExpression = VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression("workspace_loc", file.getFullPath().toString()); //$NON-NLS-1$
				((AntContentProvider) fileTableViewer.getContentProvider()).add(varExpression);
			}
			container.update();
		}
	}

	public void createControl(Composite top, String propertyLabel, String propertyFileLabel) {
		Font font = top.getFont();
		dialogSettings = PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(AntPropertiesBlock.class)).getDialogSettings();

		Label label = new Label(top, SWT.NONE);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		label.setFont(font);
		label.setText(propertyLabel);

		int idx = 0;
		int direction = SWT.DOWN;
		try {
			idx = dialogSettings.getInt(PROPERTY_SORT_COLUMN);
			direction = dialogSettings.getInt(PROPERTY_SORT_DIRECTION);
		}
		catch (NumberFormatException e) {
			// do nothing
		}
		propertyTableViewer = createTableViewer(top, true, false, idx, direction);
		propertyTableViewer.addDoubleClickListener(event -> {
			if (!event.getSelection().isEmpty() && editButton.isEnabled()) {
				edit();
			}
		});

		propertyTableViewer.getTable().addKeyListener(keyListener);

		createButtonGroup(top);

		label = new Label(top, SWT.NONE);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		label.setFont(font);
		label.setText(propertyFileLabel);

		fileTableViewer = createTableViewer(top, false, true, 0, SWT.DOWN);
		fileTableViewer.getTable().addKeyListener(keyListener);

		createButtonGroup(top);
	}

	/**
	 * Creates the group which will contain the buttons.
	 */
	private void createButtonGroup(Composite top) {
		Composite buttonGroup = new Composite(top, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonGroup.setLayout(layout);
		buttonGroup.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL));
		buttonGroup.setFont(top.getFont());

		addButtonsToButtonGroup(buttonGroup);
	}

	/**
	 * Creates and returns a configured table viewer in the given parent
	 */
	private TableViewer createTableViewer(Composite parent, boolean setColumns, boolean defaultsorting, int sortcolumnidx, int sortdirection) {
		Table table = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		int availableRows = availableRows(parent);
		if (setColumns) {
			data.heightHint = table.getItemHeight() * (availableRows / 10);
		}
		data.widthHint = 425;
		table.setLayoutData(data);
		table.setFont(parent.getFont());

		TableViewer tableViewer = new TableViewer(table);
		tableViewer.setContentProvider(new AntContentProvider(defaultsorting));
		tableViewer.setLabelProvider(labelProvider);
		tableViewer.addSelectionChangedListener(tableListener);

		if (setColumns) {
			TableLayout tableLayout = new TableLayout();
			table.setLayout(tableLayout);
			table.setHeaderVisible(true);
			table.setLinesVisible(true);
			ColumnSorter sorter = null;
			for (int i = 0; i < fTableColumnHeaders.length; i++) {
				tableLayout.addColumnData(fTableColumnLayouts[i]);
				TableColumn column = new TableColumn(table, SWT.NONE, i);
				column.setResizable(fTableColumnLayouts[i].resizable);
				column.setText(fTableColumnHeaders[i]);
				sorter = new ColumnSorter(tableViewer, column) {
					@Override
					public String getCompareText(Object obj, int columnindex) {
						return AntPropertiesBlock.this.labelProvider.getColumnText(obj, columnindex);
					}
				};
				if (i == sortcolumnidx) {
					sorter.setDirection(sortdirection);
				}
			}
		}
		return tableViewer;
	}

	/**
	 * Used to persist any settings for the block that the user has set
	 * 
	 * @since 3.5
	 */
	public void saveSettings() {
		if (propertyTableViewer != null) {
			saveColumnSettings();
		}
	}

	/**
	 * Persist table settings into the give dialog store.
	 * 
	 * @since 3.5
	 */
	private void saveColumnSettings() {
		Table table = this.propertyTableViewer.getTable();
		int columnCount = table.getColumnCount();
		for (int i = 0; i < columnCount; i++) {
			dialogSettings.put(PROPERTY_COLUMN_WIDTH + i, table.getColumn(i).getWidth());
		}
		TableColumn column = table.getSortColumn();
		if (column != null) {
			dialogSettings.put(PROPERTY_SORT_COLUMN, table.indexOf(column));
			dialogSettings.put(PROPERTY_SORT_DIRECTION, table.getSortDirection());
		}
	}

	/**
	 * Restore table settings from the given dialog store.
	 * 
	 * @since 3.5
	 */
	private void restoreColumnSettings() {
		if (this.propertyTableViewer == null) {
			return;
		}
		restoreColumnWidths();
	}

	/**
	 * Restores the column widths from dialog settings
	 * 
	 * @since 3.5
	 */
	private void restoreColumnWidths() {
		Table table = this.propertyTableViewer.getTable();
		int columnCount = table.getColumnCount();
		for (int i = 0; i < columnCount; i++) {
			int width = -1;
			try {
				width = dialogSettings.getInt(PROPERTY_COLUMN_WIDTH + i);
			}
			catch (NumberFormatException e) {
				// do nothing
			}

			if ((width <= 0) || (i == table.getColumnCount() - 1)) {
				table.getColumn(i).pack();
			} else {
				table.getColumn(i).setWidth(width);
			}
		}
	}

	/**
	 * Return the number of rows available in the current display using the current font.
	 * 
	 * @param parent
	 *            The Composite whose Font will be queried.
	 * @return The result of the display size divided by the font size.
	 */
	private int availableRows(Composite parent) {
		int fontHeight = (parent.getFont().getFontData())[0].getHeight();
		int displayHeight = parent.getDisplay().getClientArea().height;
		return displayHeight / fontHeight;
	}

	/*
	 * (non-Javadoc) Method declared on AntPage.
	 */
	protected void addButtonsToButtonGroup(Composite parent) {
		if (editButton == null) {
			addButton = createPushButton(parent, AntPreferencesMessages.AntPropertiesBlock_1);
			editButton = createPushButton(parent, AntPreferencesMessages.AntPropertiesBlock_2);
			removeButton = createPushButton(parent, AntPreferencesMessages.AntPropertiesBlock_3);
		} else {
			addFileButton = createPushButton(parent, AntPreferencesMessages.AntPropertiesBlock_4);
			addExternalFileButton = createPushButton(parent, AntPreferencesMessages.AntPropertiesBlock_14);
			removeFileButton = createPushButton(parent, AntPreferencesMessages.AntPropertiesBlock_removeFileButton);
		}
	}

	/**
	 * Creates and returns a configured button in the given composite with the given label. Widget selection call-backs for the returned button will
	 * be processed by the <code>buttonListener</code>
	 */
	private Button createPushButton(Composite parent, String label) {
		Button button = container.createPushButton(parent, label);
		button.addSelectionListener(buttonListener);
		GridData gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		button.setLayoutData(gridData);
		return button;
	}

	/**
	 * Allows the user to enter external property files
	 */
	private void addExternalPropertyFile() {
		String lastUsedPath;
		lastUsedPath = dialogSettings.get(IAntUIConstants.DIALOGSTORE_LASTEXTFILE);
		if (lastUsedPath == null) {
			lastUsedPath = IAntCoreConstants.EMPTY_STRING;
		}
		FileDialog dialog = new FileDialog(fileTableViewer.getControl().getShell(), SWT.MULTI | SWT.SHEET);
		dialog.setFilterExtensions(new String[] { "*.properties", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$ ;
		dialog.setFilterPath(lastUsedPath);

		String result = dialog.open();
		if (result == null) {
			return;
		}
		IPath filterPath = IPath.fromOSString(dialog.getFilterPath());
		for (String fileName : dialog.getFileNames()) {
			IPath path = filterPath.append(fileName).makeAbsolute();
			((AntContentProvider) fileTableViewer.getContentProvider()).add(path.toOSString());
		}

		dialogSettings.put(IAntUIConstants.DIALOGSTORE_LASTEXTFILE, filterPath.toOSString());
		container.update();
	}

	private void remove(TableViewer viewer) {
		AntContentProvider antContentProvider = (AntContentProvider) viewer.getContentProvider();
		IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
		antContentProvider.remove(sel);
		container.update();
	}

	/**
	 * Allows the user to enter a user property
	 */
	private void addProperty() {
		String title = AntPreferencesMessages.AntPropertiesBlock_Add_Property_2;
		AddPropertyDialog dialog = new AddPropertyDialog(propertyTableViewer.getControl().getShell(), title, new String[] {
				IAntCoreConstants.EMPTY_STRING, IAntCoreConstants.EMPTY_STRING });
		if (dialog.open() == Window.CANCEL) {
			return;
		}

		String[] pair = dialog.getNameValuePair();
		String name = pair[0];
		if (!overwrite(name)) {
			return;
		}
		Property prop = new Property();
		prop.setName(name);
		prop.setValue(pair[1]);
		((AntContentProvider) propertyTableViewer.getContentProvider()).add(prop);
		container.update();
	}

	private void edit() {
		IStructuredSelection selection = (IStructuredSelection) propertyTableViewer.getSelection();
		Property prop = (Property) selection.getFirstElement();

		String originalName = prop.getName();
		String title = AntPreferencesMessages.AntPropertiesBlock_Edit_User_Property_5;
		AddPropertyDialog dialog = new AddPropertyDialog(propertyTableViewer.getControl().getShell(), title, new String[] { prop.getName(),
				prop.getValue(false) });

		if (dialog.open() == Window.CANCEL) {
			return;
		}

		String[] pair = dialog.getNameValuePair();
		String name = pair[0];
		if (!name.equals(originalName)) {
			if (!overwrite(name)) {
				return;
			}
		}
		prop.setName(name);
		prop.setValue(pair[1]);
		// trigger a resort
		propertyTableViewer.refresh();
		container.update();
	}

	private boolean overwrite(String name) {
		for (Object prop : getProperties()) {
			Property property = (Property) prop;
			String propertyName = property.getName();
			if (propertyName.equals(name)) {
				if (property.isDefault()) {
					MessageDialog.openError(propertyTableViewer.getControl().getShell(), AntPreferencesMessages.AntPropertiesBlock_17, MessageFormat.format(AntPreferencesMessages.AntPropertiesBlock_18, new Object[] {
							propertyName, property.getPluginLabel() }));
					return false;
				}
				boolean overWrite = MessageDialog.openQuestion(propertyTableViewer.getControl().getShell(), AntPreferencesMessages.AntPropertiesBlock_15, MessageFormat.format(AntPreferencesMessages.AntPropertiesBlock_16, new Object[] {
						name }));
				if (!overWrite) {
					return false;
				}
				((AntContentProvider) propertyTableViewer.getContentProvider()).remove(property);
				break;
			}
		}
		return true;
	}

	/**
	 * Handles selection changes in the Property file table viewer.
	 */
	private void fileTableSelectionChanged(IStructuredSelection newSelection) {
		removeFileButton.setEnabled(newSelection.size() > 0);
	}

	/**
	 * Handles selection changes in the Property table viewer.
	 */
	private void propertyTableSelectionChanged(IStructuredSelection newSelection) {
		int size = newSelection.size();
		boolean enabled = true;

		Iterator<Object> itr = newSelection.iterator();
		while (itr.hasNext()) {
			Object element = itr.next();
			if (element instanceof Property) {
				Property property = (Property) element;
				if (property.isDefault()) {
					enabled = false;
					break;
				}
			}
		}
		editButton.setEnabled(enabled && size == 1);
		removeButton.setEnabled(enabled && size > 0);

	}

	public void populatePropertyViewer(Map<String, String> properties) {
		if (properties == null) {
			propertyTableViewer.setInput(new Property[0]);
			return;
		}
		Property[] result = new Property[properties.size()];
		Iterator<Map.Entry<String, String>> entries = properties.entrySet().iterator();
		int i = 0;
		while (entries.hasNext()) {
			Entry<String, String> element = entries.next();
			Property property = new Property();
			property.setName(element.getKey());
			property.setValue(element.getValue());
			result[i] = property;
			i++;
		}
		propertyTableViewer.setInput(result);
		restoreColumnSettings();
	}

	public void setPropertiesInput(Property[] properties) {
		propertyTableViewer.setInput(properties);
	}

	public void setPropertyFilesInput(String[] files) {
		fileTableViewer.setInput(files);
	}

	public void update() {
		propertyTableSelectionChanged((IStructuredSelection) propertyTableViewer.getSelection());
		fileTableSelectionChanged((IStructuredSelection) fileTableViewer.getSelection());
	}

	public Object[] getProperties() {
		return ((AntContentProvider) propertyTableViewer.getContentProvider()).getElements(null);
	}

	public Object[] getPropertyFiles() {
		return ((AntContentProvider) fileTableViewer.getContentProvider()).getElements(null);
	}

	public void setEnabled(boolean enable) {
		setTablesEnabled(enable);
		addButton.setEnabled(enable);
		addExternalFileButton.setEnabled(enable);
		addFileButton.setEnabled(enable);
		editButton.setEnabled(enable);
		removeButton.setEnabled(enable);
		removeFileButton.setEnabled(enable);

		if (enable) {
			propertyTableViewer.setSelection(propertyTableViewer.getSelection());
			fileTableViewer.setSelection(fileTableViewer.getSelection());
		}
	}

	public void setTablesEnabled(boolean tablesEnabled) {
		this.tablesEnabled = tablesEnabled;
	}
}
