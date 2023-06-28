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
package org.eclipse.debug.internal.ui.preferences;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.MultipleInputDialog;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

/**
 * Preference page for creating and configuring simple
 * launch variables.
 *
 * @see org.eclipse.debug.core.variables.IValueVariable
 * @see org.eclipse.debug.core.variables.ISimpleVariableRegistry
 */
public class StringVariablePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private TableViewer variableTable;
	protected Button envAddButton;
	protected Button envEditButton;
	protected Button envRemoveButton;

	protected SimpleVariableContentProvider variableContentProvider= new SimpleVariableContentProvider();

	protected static final String NAME_LABEL= DebugPreferencesMessages.SimpleVariablePreferencePage_10;
	protected static final String VALUE_LABEL = DebugPreferencesMessages.SimpleVariablePreferencePage_11;
	protected static final String DESCRIPTION_LABEL = DebugPreferencesMessages.SimpleVariablePreferencePage_12;

	protected static final String STRING_VARIABLE_PREFERENCE_KEY = "StringVariablePreferencePage"; //$NON-NLS-1$

	protected static String[] variableTableColumnProperties= {
		"variable", //$NON-NLS-1$
		"value", //$NON-NLS-1$
		"description" //$NON-NLS-1$
	};
	protected String[] variableTableColumnHeaders= {
		DebugPreferencesMessages.SimpleVariablePreferencePage_3,
		DebugPreferencesMessages.SimpleVariablePreferencePage_4,
		DebugPreferencesMessages.SimpleVariablePreferencePage_5,
		DebugPreferencesMessages.StringVariablePreferencePage_27
	};
	protected ColumnLayoutData[] variableTableColumnLayouts= {
		new ColumnWeightData(30),
		new ColumnWeightData(25),
		new ColumnWeightData(25),
		new ColumnWeightData(20)
	};

	public StringVariablePreferencePage() {
		setDescription(DebugPreferencesMessages.SimpleVariablePreferencePage_6);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IDebugHelpContextIds.SIMPLE_VARIABLE_PREFERENCE_PAGE);
	}

	@Override
	protected Control createContents(Composite parent) {
		noDefaultAndApplyButton();
		Font font= parent.getFont();
		//The main composite
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight=0;
		layout.marginWidth=0;
		layout.numColumns= 2;
		composite.setLayout(layout);
		composite.setFont(font);

		createTable(composite);
		createButtons(composite);

		return composite;
	}

	/**
	 * Creates and configures the table containing launch configuration variables
	 * and their associated value.
	 */
	private void createTable(Composite parent) {
		Font font= parent.getFont();
		// Create table composite
		Composite tableComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 1;
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 150;
		gridData.widthHint = 400;
		tableComposite.setLayout(layout);
		tableComposite.setLayoutData(gridData);
		tableComposite.setFont(font);
		// Create table
		variableTable = new TableViewer(tableComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		Table table = variableTable.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setFont(font);
		gridData = new GridData(GridData.FILL_BOTH);
		variableTable.getControl().setLayoutData(gridData);
		variableTable.setContentProvider(variableContentProvider);
		variableTable.setColumnProperties(variableTableColumnProperties);
		variableTable.addFilter(new VariableFilter());
		variableTable.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer iViewer, Object e1, Object e2) {
				if (e1 == null) {
					return -1;
				} else if (e2 == null) {
					return 1;
				} else {
					return ((VariableWrapper)e1).getName().compareToIgnoreCase(((VariableWrapper)e2).getName());
				}
			}
		});

		variableTable.addSelectionChangedListener(this::handleTableSelectionChanged);

		variableTable.addDoubleClickListener(event -> {
			if (!variableTable.getSelection().isEmpty()) {
				handleEditButtonPressed();
			}
		});
		variableTable.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.character == SWT.DEL && event.stateMask == 0) {
					handleRemoveButtonPressed();
				}
			}
		});

		for (int i = 0; i < variableTableColumnHeaders.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.NONE, i);
			tc.setResizable(variableTableColumnLayouts[i].resizable);
			tc.setText(variableTableColumnHeaders[i]);
		}

		// Try restoring column widths from preferences, if widths aren't stored, init columns to default
		if (!restoreColumnWidths()){
			restoreDefaultColumnWidths();
		}

		variableTable.setInput(getVariableManager());
		variableTable.setLabelProvider(new SimpleVariableLabelProvider());
	}

	/**
	 * Creates the new/edit/remove buttons for the variable table
	 * @param parent the composite in which the buttons should be created
	 */
	private void createButtons(Composite parent) {
		// Create button composite
		Composite buttonComposite = new Composite(parent, SWT.NONE);
		GridLayout glayout = new GridLayout();
		glayout.marginHeight = 0;
		glayout.marginWidth = 0;
		glayout.numColumns = 1;
		GridData gdata = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		buttonComposite.setLayout(glayout);
		buttonComposite.setLayoutData(gdata);
		buttonComposite.setFont(parent.getFont());

		// Create buttons
		envAddButton = SWTFactory.createPushButton(buttonComposite, DebugPreferencesMessages.SimpleVariablePreferencePage_7, null);
		envAddButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleAddButtonPressed();
			}
		});
		envEditButton = SWTFactory.createPushButton(buttonComposite, DebugPreferencesMessages.SimpleVariablePreferencePage_8, null);
		envEditButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleEditButtonPressed();
			}
		});
		envEditButton.setEnabled(false);
		envRemoveButton = SWTFactory.createPushButton(buttonComposite, DebugPreferencesMessages.SimpleVariablePreferencePage_9, null);
		envRemoveButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleRemoveButtonPressed();
			}
		});
		envRemoveButton.setEnabled(false);
	}

	private void handleAddButtonPressed() {
		boolean done = false;
		String name = null;
		String description = null;
		String value = null;
		while (!done){

			MultipleInputDialog dialog= new MultipleInputDialog(getShell(), DebugPreferencesMessages.SimpleVariablePreferencePage_13);
			dialog.addTextField(NAME_LABEL, name, false);
			dialog.addBrowseField(VALUE_LABEL, value, true);
			dialog.addTextField(DESCRIPTION_LABEL, description, true);

			if (dialog.open() != Window.OK) {
				done = true;
			}
			else {
				name= dialog.getStringValue(NAME_LABEL).trim();
				value = dialog.getStringValue(VALUE_LABEL);
				description= dialog.getStringValue(DESCRIPTION_LABEL);
				done = addVariable(name, description, value);
			}
		}
	}

	/**
	 * Attempts to create and add a new variable with the given properties.  Returns
	 * whether the operation completed successfully (either the variable was added
	 * successfully, or the user cancelled the operation).  Returns false if the name
	 * is null or the user chooses not to overwrite an existing variable.
	 *
	 * @param name name of the variable, cannot be <code>null</code> or empty.
	 * @param description description of the variable or <code>null</code>
	 * @param value value of the variable or <code>null</code>
	 * @return whether the operation completed successfully
	 */
	private boolean addVariable(String name, String description, String value) {
		if (name == null || name.length() == 0){
			MessageDialog.openError(getShell(),DebugPreferencesMessages.StringVariablePreferencePage_21, DebugPreferencesMessages.StringVariablePreferencePage_20);
			return false;
		}
		List<VariableWrapper> editedVariables = variableContentProvider.getWorkingSetVariables();
		Iterator<VariableWrapper> iter = editedVariables.iterator();
		while (iter.hasNext()) {
			VariableWrapper currentVariable = iter.next();
			if (!currentVariable.isRemoved()) {
				String currentName = currentVariable.getName();
				if (currentName.equals(name)) {
					if (currentVariable.isReadOnly()){
						MessageDialog.openError(getShell(), DebugPreferencesMessages.StringVariablePreferencePage_23, MessageFormat.format(DebugPreferencesMessages.StringVariablePreferencePage_22, new Object[] { name }));
						return false;
					}
					else {
						MessageDialog dialog = new MessageDialog(getShell(), DebugPreferencesMessages.SimpleVariablePreferencePage_15, null, MessageFormat.format(DebugPreferencesMessages.SimpleVariablePreferencePage_16, new Object[] { name }), MessageDialog.QUESTION, new String[] {
								IDialogConstants.YES_LABEL,
								IDialogConstants.NO_LABEL,
								IDialogConstants.CANCEL_LABEL }, 0);
						int overWrite= dialog.open();
						switch (overWrite) {
						case 0:
							currentVariable.setValue(value);
							currentVariable.setDescription(description);
							variableTable.update(currentVariable, null);
							return true;
						case 1:
							return false;
						default:
							return true;  // Cancel was pressed, return true so operation is ended
						}
					}
				}
			}
		}
		VariableWrapper newVariable = new VariableWrapper(name, description, value);
		variableContentProvider.addVariable(newVariable);
		variableTable.refresh();
		return true;
	}

	private void handleEditButtonPressed() {
		IStructuredSelection selection = variableTable.getStructuredSelection();
		VariableWrapper variable= (VariableWrapper) selection.getFirstElement();
		if (variable == null || variable.isReadOnly()) {
			return;
		}
		String value= variable.getValue();
		String description= variable.getDescription();
		String name= variable.getName();
		MultipleInputDialog dialog = new MultipleInputDialog(getShell(), MessageFormat.format(DebugPreferencesMessages.SimpleVariablePreferencePage_14, new Object[] { name }));
		dialog.addBrowseField(VALUE_LABEL, value, true);
		dialog.addTextField(DESCRIPTION_LABEL, description, true);

		if (dialog.open() == Window.OK) {
			value= dialog.getStringValue(VALUE_LABEL);
			description= dialog.getStringValue(DESCRIPTION_LABEL);
			if (value != null) {
				variable.setValue(value);
			}
			if (description != null) {
				variable.setDescription(description);
			}
			variableTable.update(variable, null);
		}
	}

	/**
	 * Remove the selection variables.
	 */
	private void handleRemoveButtonPressed() {
		IStructuredSelection selection = variableTable.getStructuredSelection();
		List<VariableWrapper> variablesToRemove = selection.toList();
		StringBuilder contributedVariablesToRemove= new StringBuilder();
		Iterator<VariableWrapper> iter = variablesToRemove.iterator();
		while (iter.hasNext()) {
			VariableWrapper variable = iter.next();
			if (variable.isContributed()) {
				contributedVariablesToRemove.append('\t').append(variable.getName()).append('\n');
			}
		}
		if (contributedVariablesToRemove.length() > 0) {
			boolean remove = MessageDialog.openQuestion(getShell(), DebugPreferencesMessages.SimpleLaunchVariablePreferencePage_21, MessageFormat.format(DebugPreferencesMessages.SimpleLaunchVariablePreferencePage_22, new Object[] { contributedVariablesToRemove.toString() })); //
			if (!remove) {
				return;
			}
		}
		VariableWrapper[] variables= variablesToRemove.toArray(new VariableWrapper[0]);
		for (VariableWrapper variable : variables) {
			variable.setRemoved(true);
		}
		variableTable.refresh();
	}

	/**
	 * Responds to a selection changed event in the variable table
	 * @param event the selection change event
	 */
	protected void handleTableSelectionChanged(SelectionChangedEvent event) {
		IStructuredSelection selection = (event.getStructuredSelection());
		VariableWrapper variable= (VariableWrapper) selection.getFirstElement();
		if (variable == null || variable.isReadOnly()) {
			envEditButton.setEnabled(false);
			envRemoveButton.setEnabled(false);
		}
		else {
			envEditButton.setEnabled(selection.size() == 1);
			envRemoveButton.setEnabled(selection.size() > 0);
		}
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	/**
	 * Clear the variables.
	 */
	@Override
	protected void performDefaults() {
		variableContentProvider.init();
		variableTable.refresh();
		super.performDefaults();
	}

	/**
	 * Sets the saved state for reversion.
	 */
	@Override
	public boolean performOk() {
		variableContentProvider.saveChanges();
		saveColumnWidths();
		return super.performOk();
	}

	/**
	 * Returns the DebugPlugin's singleton instance of the
	 * launch variable manager
	 * @return the singleton instance of the simple variable registry.
	 */
	private IStringVariableManager getVariableManager() {
		return VariablesPlugin.getDefault().getStringVariableManager();
	}

	public void saveColumnWidths() {
		StringBuilder widthPreference = new StringBuilder();
		for (int i = 0; i < variableTable.getTable().getColumnCount(); i++) {
			widthPreference.append(variableTable.getTable().getColumn(i).getWidth());
			widthPreference.append(',');
		}
		if (widthPreference.length() > 0){
			DebugUIPlugin.getDefault().getPreferenceStore().setValue(STRING_VARIABLE_PREFERENCE_KEY, widthPreference.toString());
		}
	}

	private boolean restoreColumnWidths() {
		String[] columnWidthStrings = DebugUIPlugin.getDefault().getPreferenceStore().getString(STRING_VARIABLE_PREFERENCE_KEY).split(","); //$NON-NLS-1$
		int columnCount = variableTable.getTable().getColumnCount();
		if (columnWidthStrings.length != columnCount){
			return false; // Preferred column sizes not stored correctly.
		}
		for (int i = 0; i < columnCount; i++) {
			try{
				int columnWidth = Integer.parseInt(columnWidthStrings[i]);
				variableTable.getTable().getColumn(i).setWidth(columnWidth);
			} catch (NumberFormatException e){
				DebugUIPlugin.log(new Throwable("Problem loading persisted column sizes for StringVariablePreferencesPage",e)); //$NON-NLS-1$
			}
		}
		return true;
	}

	private void restoreDefaultColumnWidths(){
		TableLayout layout = new TableLayout();
		for (ColumnLayoutData variableTableColumnLayout : variableTableColumnLayouts) {
			layout.addColumnData(variableTableColumnLayout);
		}
		variableTable.getTable().setLayout(layout);
	}

	private class SimpleVariableContentProvider implements IStructuredContentProvider {
		/**
		 * The content provider stores variable wrappers for use during editing.
		 */
		private List<VariableWrapper> fWorkingSet = new ArrayList<>();

		@Override
		public Object[] getElements(Object inputElement) {
			return fWorkingSet.toArray();
		}

		/**
		 * Adds the given variable to the 'wrappers'
		 *
		 * @param variable variable to add
		 */
		public void addVariable(VariableWrapper variable) {
			fWorkingSet.add(variable);
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput == null || !(newInput instanceof IStringVariableManager)){
				return;
			}
			init();
		}

		/**
		 * Saves the edited variable state to the variable manager.
		 */
		public void saveChanges() {
			IStringVariableManager manager = getVariableManager();
			Iterator<VariableWrapper> iterator = fWorkingSet.iterator();
			List<IValueVariable> remove = new ArrayList<>();
			List<IValueVariable> add = new ArrayList<>();
			while (iterator.hasNext()) {
				VariableWrapper variable = iterator.next();
				if (!variable.isReadOnly()) {
					IValueVariable underlyingVariable = variable.getUnderlyingVariable();
					if (variable.isRemoved()) {
						if (underlyingVariable != null) {
							// if added and removed there is no underlying variable
							remove.add(underlyingVariable);
						}
					} else if (variable.isAdded()) {
						IValueVariable vv = manager.newValueVariable(variable.getName(), variable.getDescription());
						vv.setValue(variable.getValue());
						add.add(vv);
					} else if (variable.isChanged()) {
						underlyingVariable.setValue(variable.getValue());
						underlyingVariable.setDescription(variable.getDescription());
					}
				}
			}
			// remove
			if (!remove.isEmpty()) {
				manager.removeVariables(remove.toArray(new IValueVariable[remove.size()]));
			}
			// add
			if (!add.isEmpty()) {
				try {
					manager.addVariables(add.toArray(new IValueVariable[add.size()]));
				} catch (CoreException e) {
				DebugUIPlugin.errorDialog(getShell(), DebugPreferencesMessages.StringVariablePreferencePage_24, DebugPreferencesMessages.StringVariablePreferencePage_25, e.getStatus()); //
				}
			}
		}

		/**
		 * Re-initializes to the variables currently stored in the manager.
		 */
		public void init() {
			fWorkingSet.clear();
			IStringVariableManager manager = getVariableManager();
			IValueVariable[] variables = manager.getValueVariables();
			for (IValueVariable variable : variables) {
				fWorkingSet.add(new VariableWrapper(variable));
			}
		}

		/**
		 * Returns the 'working set' of variables
		 *
		 * @return the working set of variables (not yet saved)
		 */
		public List<VariableWrapper> getWorkingSetVariables() {
			return fWorkingSet;
		}

	}

	static class VariableWrapper {

		protected IValueVariable fVariable;
		protected String fNewName = null;
		protected String fNewDesc = null;
		protected String fNewValue = null;
		boolean fRemoved = false;
		boolean fAdded = false;

		public VariableWrapper(IValueVariable variable) {
			fVariable = variable;
		}

		public VariableWrapper(String name, String desc, String value) {
			fNewName = name;
			fNewDesc = desc;
			fNewValue = value;
			fAdded = true;
		}

		public boolean isAdded() {
			return fAdded;
		}

		public String getName() {
			if (fNewName == null) {
				return fVariable.getName();
			}
			return fNewName;
		}

		public void setName(String name) {
			fNewName = name;
		}

		public String getDescription() {
			if (fNewDesc == null) {
				return fVariable.getDescription();
			}
			return fNewDesc;
		}

		public String getValue() {
			if (fNewValue == null) {
				return fVariable.getValue();
			}
			return fNewValue;
		}

		public void setValue(String value) {
			fNewValue = value;
		}

		public void setDescription(String desc) {
			fNewDesc = desc;
		}

		public boolean isChanged() {
			return !fAdded && !fRemoved && (fNewValue != null || fNewDesc != null);
		}

		public boolean isReadOnly() {
			if (fVariable == null) {
				return false;
			}
			return fVariable.isReadOnly();
		}

		public boolean isContributed() {
			if (fVariable == null) {
				return false;
			}
			return fVariable.isContributed();
		}

		public IValueVariable getUnderlyingVariable() {
			return fVariable;
		}

		public boolean isRemoved() {
			return fRemoved;
		}

		public void setRemoved(boolean removed) {
			fRemoved = removed;
		}
	}

	private class SimpleVariableLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider {
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof VariableWrapper) {
				VariableWrapper variable= (VariableWrapper) element;
				switch (columnIndex) {
					case 0 :
						StringBuilder name = new StringBuilder();
						name.append(variable.getName());
						if (variable.isReadOnly()){
							name.append(DebugPreferencesMessages.StringVariablePreferencePage_26);
						}
						return name.toString();

					case 1:
						String value= variable.getValue();
						if (value == null) {
							value= IInternalDebugCoreConstants.EMPTY_STRING;
						}
						return value;
					case 2:
						String description= variable.getDescription();
						if (description == null) {
							description= IInternalDebugCoreConstants.EMPTY_STRING;
						}
						return description;
					case 3:
						String contribution = IInternalDebugCoreConstants.EMPTY_STRING;
						if (variable.isContributed()) {
							String pluginId = getVariableManager().getContributingPluginId(variable.getUnderlyingVariable());
							if (pluginId != null) {
								contribution = pluginId;
							} else {
								contribution = DebugPreferencesMessages.SimpleLaunchVariablePreferencePage_23;
							}
						}
						return contribution;
					default:
						break;

				}
			}
			return null;
		}
		@Override
		public Color getForeground(Object element) {
			if (element instanceof VariableWrapper) {
				if (((VariableWrapper) element).isReadOnly()) {
					Display display= Display.getCurrent();
					return display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
				}
			}
			return null;
		}
		@Override
		public Color getBackground(Object element) {
			if (element instanceof VariableWrapper) {
				if (((VariableWrapper) element).isReadOnly()) {
					Display display= Display.getCurrent();
					return display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
				}
			}
			return null;
		}
	}

	static class VariableFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return !((VariableWrapper)element).isRemoved();
		}

	}
}
