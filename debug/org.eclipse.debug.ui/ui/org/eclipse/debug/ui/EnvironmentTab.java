/*******************************************************************************
 * Copyright (c) 2000, 2019 Keith Seitz and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Keith Seitz (keiths@redhat.com) - initial implementation
 *     IBM Corporation - integration and code cleanup
 *     Jan Opacki (jan.opacki@gmail.com) bug 307139
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *     Jens Reimann (jreimann@redhat.com) - add copy & paste support
 *     Andrew Obuchowicz (aobuchow@redhat.com) - Bug 548344
 *******************************************************************************/
package org.eclipse.debug.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.AbstractDebugCheckboxSelectionDialog;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.MultipleInputDialog;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.TextGetSetEditingSupport;
import org.eclipse.debug.internal.ui.launchConfigurations.EnvironmentVariable;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsMessages;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;

/**
 * Launch configuration tab for configuring the environment passed into
 * Runtime.exec(...) when a config is launched.
 * <p>
 * Clients may call {@link #setHelpContextId(String)} on this tab prior to
 * control creation to alter the default context help associated with this tab.
 * </p>
 * <p>
 * This class may be instantiated.
 * </p>
 *
 * @since 3.0
 * @noextend This class is not intended to be sub-classed by clients.
 */
public class EnvironmentTab extends AbstractLaunchConfigurationTab {

	protected TableViewer environmentTable;
	protected String[] envTableColumnHeaders = { LaunchConfigurationsMessages.EnvironmentTab_Variable_1,
			LaunchConfigurationsMessages.EnvironmentTab_Value_2, };
	private static final String NAME_LABEL = LaunchConfigurationsMessages.EnvironmentTab_8;
	private static final String VALUE_LABEL = LaunchConfigurationsMessages.EnvironmentTab_9;
	protected static final String P_VARIABLE = "variable"; //$NON-NLS-1$
	protected static final String P_VALUE = "value"; //$NON-NLS-1$
	protected Button envAddButton;
	protected Button envEditButton;
	protected Button envRemoveButton;
	/**
	 * @since 3.14
	 */
	protected Button envCopyButton;
	/**
	 * @since 3.14
	 */
	protected Button envPasteButton;
	protected Button appendEnvironment;
	protected Button replaceEnvironment;
	protected Button envSelectButton;

	private KeyStroke copyKeyStroke;
	private KeyStroke pasteKeyStroke;

	/**
	 * Content provider for the environment table
	 */
	protected class EnvironmentVariableContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			EnvironmentVariable[] elements = new EnvironmentVariable[0];
			ILaunchConfiguration config = (ILaunchConfiguration) inputElement;
			Map<String, String> m;
			try {
				m = config.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, (Map<String, String>) null);
			} catch (CoreException e) {
				DebugUIPlugin.log(new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), IStatus.ERROR,
						"Error reading configuration", e)); //$NON-NLS-1$
				return elements;
			}
			if (m != null && !m.isEmpty()) {
				elements = new EnvironmentVariable[m.size()];
				String[] varNames = new String[m.size()];
				m.keySet().toArray(varNames);
				for (int i = 0; i < m.size(); i++) {
					elements[i] = new EnvironmentVariable(varNames[i], m.get(varNames[i]));
				}
			}
			return elements;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput == null) {
				return;
			}
			if (viewer instanceof TableViewer) {
				TableViewer tableViewer = (TableViewer) viewer;
				if (tableViewer.getTable().isDisposed()) {
					return;
				}
				tableViewer.setComparator(new ViewerComparator() {
					@Override
					public int compare(Viewer iviewer, Object e1, Object e2) {
						if (e1 == null) {
							return -1;
						} else if (e2 == null) {
							return 1;
						} else {
							return ((EnvironmentVariable) e1).getName()
									.compareToIgnoreCase(((EnvironmentVariable) e2).getName());
						}
					}
				});
			}
		}
	}

	/**
	 * Label provider for the environment table
	 */
	public class EnvironmentVariableLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public String getColumnText(Object element, int columnIndex) {
			String result = null;
			if (element != null) {
				EnvironmentVariable var = (EnvironmentVariable) element;
				switch (columnIndex) {
				case 0: // variable
					result = var.getName();
					break;
				case 1: // value
					result = var.getValue();
					break;
				default:
					break;
				}
			}
			return result;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0) {
				return DebugPluginImages.getImage(IDebugUIConstants.IMG_OBJS_ENV_VAR);
			}
			return null;
		}
	}

	/**
	 * Constructs a new tab with default context help.
	 */
	public EnvironmentTab() {
		super();
		setHelpContextId(IDebugHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_ENVIRONMENT_TAB);
		try {
			this.copyKeyStroke = KeyStroke.getInstance("M1+C"); //$NON-NLS-1$
		} catch (ParseException e) {
		}
		try {
			this.pasteKeyStroke = KeyStroke.getInstance("M1+V"); //$NON-NLS-1$
		} catch (ParseException e) {
		}
	}

	@Override
	public void createControl(Composite parent) {
		// Create main composite
		Composite mainComposite = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);
		setControl(mainComposite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpContextId());

		createEnvironmentTable(mainComposite);
		createTableButtons(mainComposite);
		createAppendReplace(mainComposite);

		Dialog.applyDialogFont(mainComposite);
	}

	/**
	 * Creates and configures the widgets which allow the user to choose whether the
	 * specified environment should be appended to the native environment or if it
	 * should completely replace it.
	 *
	 * @param parent the composite in which the widgets should be created
	 */
	protected void createAppendReplace(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 2, GridData.FILL_HORIZONTAL);
		appendEnvironment = createRadioButton(comp, LaunchConfigurationsMessages.EnvironmentTab_16);
		appendEnvironment.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
		replaceEnvironment = createRadioButton(comp, LaunchConfigurationsMessages.EnvironmentTab_17);
	}

	/**
	 * Updates the enablement of the append/replace widgets. The widgets should
	 * disable when there are no environment variables specified.
	 */
	protected void updateAppendReplace() {
		boolean enable = environmentTable.getTable().getItemCount() > 0;
		appendEnvironment.setEnabled(enable);
		replaceEnvironment.setEnabled(enable);
	}

	/**
	 * Creates and configures the table that displayed the key/value pairs that
	 * comprise the environment.
	 *
	 * @param parent the composite in which the table should be created
	 */
	protected void createEnvironmentTable(Composite parent) {
		Font font = parent.getFont();
		// Create label, add it to the parent to align the right side buttons with the
		// top of the table
		SWTFactory.createLabel(parent, LaunchConfigurationsMessages.EnvironmentTab_Environment_variables_to_set__3, 2);
		// Create table composite
		Composite tableComposite = SWTFactory.createComposite(parent, font, 1, 1, GridData.FILL_BOTH, 0, 0);
		// Create table
		environmentTable = new TableViewer(tableComposite,
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		Table table = environmentTable.getTable();
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setFont(font);
		environmentTable.setContentProvider(new EnvironmentVariableContentProvider());
		environmentTable.setLabelProvider(new EnvironmentVariableLabelProvider());
		environmentTable.setColumnProperties(new String[] { P_VARIABLE, P_VALUE });
		environmentTable.addSelectionChangedListener(this::handleTableSelectionChanged);

		// Setup right-click context menu
		Menu menuTable = new Menu(table);
		table.setMenu(menuTable);

		// Create add environment variable menu item
		MenuItem miAdd = new MenuItem(menuTable, SWT.NONE);
		miAdd.setText(LaunchConfigurationsMessages.EnvironmentTab_Add_4);
		miAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleEnvAddButtonSelected();
			}
		});

		// Create copy environment variable menu item
		MenuItem miCopy = new MenuItem(menuTable, SWT.NONE);
		miCopy.setText(LaunchConfigurationsMessages.EnvironmentTab_Copy);

		miCopy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleEnvCopyButtonSelected();
			}
		});

		// Create paste environment variable menu item
		MenuItem miPaste = new MenuItem(menuTable, SWT.NONE);
		miPaste.setText(LaunchConfigurationsMessages.EnvironmentTab_Paste);

		miPaste.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleEnvPasteButtonSelected();
			}
		});

		// Create remove environment variable menu item
		MenuItem miRemove = new MenuItem(menuTable, SWT.NONE);
		miRemove.setText(LaunchConfigurationsMessages.EnvironmentTab_Remove_6);
		miRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleEnvRemoveButtonSelected();
			}
		});

		environmentTable.addSelectionChangedListener(event -> {
			IStructuredSelection selection = environmentTable.getStructuredSelection();
			if (selection.size() == 1) {
				miRemove.setText(LaunchConfigurationsMessages.EnvironmentTab_Remove_6);
			} else if (selection.size() > 1) {
				miRemove.setText(LaunchConfigurationsMessages.EnvironmentTab_Remove_All);
			}
		});

		// Disable certain context menu item's if no table item is selected
		table.addListener(SWT.MenuDetect, event -> {
			if (table.getSelectionCount() <= 0) {
				miRemove.setEnabled(false);
				miCopy.setEnabled(false);
			} else {
				miRemove.setEnabled(true);
				miCopy.setEnabled(true);
			}
		});

		// Setup and create Columns
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(environmentTable) {
			@Override
			protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}

		};

		int feature = ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | ColumnViewerEditor.TABBING_HORIZONTAL
				| ColumnViewerEditor.KEYBOARD_ACTIVATION
				| ColumnViewerEditor.TABBING_CYCLE_IN_VIEWER;

		TableViewerEditor.create(environmentTable, actSupport, feature);

		// Setup environment variable name column
		final TableViewerColumn tcv1 = new TableViewerColumn(environmentTable, SWT.NONE, 0);
		tcv1.setLabelProvider(
				ColumnLabelProvider.createTextProvider(element -> ((EnvironmentVariable) element).getName()));

		TableColumn tc1 = tcv1.getColumn();
		tc1.setText(envTableColumnHeaders[0]);
		tcv1.setEditingSupport(new TextGetSetEditingSupport<>(tcv1.getViewer(), EnvironmentVariable::getName,
				(EnvironmentVariable envVar, String value) -> {
					// Trim environment variable names
					String newName = value.trim();
					if (newName != null && !newName.isEmpty()) {
						if (!newName.equals(envVar.getName())) {
							if (canRenameVariable(newName)) {
								envVar.setName(newName);
								updateAppendReplace();
								updateLaunchConfigurationDialog();
							}
						}
					}
				}));

		// Setup environment variable value column
		final TableViewerColumn tcv2 = new TableViewerColumn(environmentTable, SWT.NONE, 1);
		tcv2.setLabelProvider(
				ColumnLabelProvider.createTextProvider(element -> ((EnvironmentVariable) element).getValue()));

		TableColumn tc2 = tcv2.getColumn();
		tc2.setText(envTableColumnHeaders[1]);
		tcv2.setEditingSupport(
				new TextGetSetEditingSupport<>(tcv2.getViewer(), EnvironmentVariable::getValue, (envVar, value) -> {
					// Don't trim environment variable values
					envVar.setValue(value);
					updateAppendReplace();
					updateLaunchConfigurationDialog();
				}));

		// Create table column layout
		TableColumnLayout tableColumnLayout = new TableColumnLayout(true);
		PixelConverter pixelConverter = new PixelConverter(font);
		tableColumnLayout.setColumnData(tc1, new ColumnWeightData(1, pixelConverter.convertWidthInCharsToPixels(20)));
		tableColumnLayout.setColumnData(tc2, new ColumnWeightData(2, pixelConverter.convertWidthInCharsToPixels(20)));
		tableComposite.setLayout(tableColumnLayout);

		environmentTable.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				KeyStroke current = computeKeyStroke(e);
				if (current.equals(copyKeyStroke)) {
					handleEnvCopyButtonSelected();
				} else if (current.equals(pasteKeyStroke)) {
					handleEnvPasteButtonSelected();
				}
			}

		});
	}

	/**
	 * Responds to a selection changed event in the environment table
	 *
	 * @param event the selection change event
	 */
	protected void handleTableSelectionChanged(SelectionChangedEvent event) {
		int size = event.getStructuredSelection().size();
		envEditButton.setEnabled(size == 1);
		envRemoveButton.setEnabled(size > 0);
		envCopyButton.setEnabled(size > 0);
	}

	/**
	 * Creates the add/edit/remove buttons for the environment table
	 *
	 * @param parent the composite in which the buttons should be created
	 */
	protected void createTableButtons(Composite parent) {
		// Create button composite
		Composite buttonComposite = SWTFactory.createComposite(parent, parent.getFont(), 1, 1,
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END, 0, 0);

		// Create buttons
		envAddButton = createPushButton(buttonComposite, LaunchConfigurationsMessages.EnvironmentTab_Add_4, null);
		envAddButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleEnvAddButtonSelected();
			}
		});
		envSelectButton = createPushButton(buttonComposite, LaunchConfigurationsMessages.EnvironmentTab_18, null);
		envSelectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleEnvSelectButtonSelected();
			}
		});
		envEditButton = createPushButton(buttonComposite, LaunchConfigurationsMessages.EnvironmentTab_Edit_5, null);
		envEditButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleEnvEditButtonSelected();
			}
		});
		envEditButton.setEnabled(false);
		envRemoveButton = createPushButton(buttonComposite, LaunchConfigurationsMessages.EnvironmentTab_Remove_6, null);
		envRemoveButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleEnvRemoveButtonSelected();
			}
		});
		envRemoveButton.setEnabled(false);
		envCopyButton = createPushButton(buttonComposite, LaunchConfigurationsMessages.EnvironmentTab_Copy, null);
		envCopyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleEnvCopyButtonSelected();
			}
		});
		envCopyButton.setEnabled(false);
		envPasteButton = createPushButton(buttonComposite, LaunchConfigurationsMessages.EnvironmentTab_Paste, null);
		envPasteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleEnvPasteButtonSelected();
			}
		});
		envPasteButton.setEnabled(true);
	}

	/**
	 * Adds a new environment variable to the table.
	 */
	protected void handleEnvAddButtonSelected() {
		MultipleInputDialog dialog = new MultipleInputDialog(getShell(),
				LaunchConfigurationsMessages.EnvironmentTab_22);
		dialog.addTextField(NAME_LABEL, null, false);
		dialog.addVariablesField(VALUE_LABEL, null, true);

		if (dialog.open() != Window.OK) {
			return;
		}

		String name = dialog.getStringValue(NAME_LABEL);
		String value = dialog.getStringValue(VALUE_LABEL);

		if (name != null && value != null && name.length() > 0 && value.length() > 0) {
			// Trim the environment variable name but *NOT* the value
			addVariable(new EnvironmentVariable(name.trim(), value));
			updateAppendReplace();
		}
	}

	/**
	 * Returns whether the environment variable can be renamed to the given variable
	 * name. If the name is already used for another variable, the user decides with
	 * a dialog whether to overwrite the existing variable
	 *
	 * @param newVariableName the chosen name to give to the variable
	 * @return whether the new name should be used or not
	 */
	private boolean canRenameVariable(String newVariableName) {
		for (TableItem item : environmentTable.getTable().getItems()) {
			EnvironmentVariable existingVariable = (EnvironmentVariable) item.getData();
			if (existingVariable.getName().equals(newVariableName)) {

				boolean overWrite = MessageDialog.openQuestion(getShell(),
						LaunchConfigurationsMessages.EnvironmentTab_12,
						MessageFormat.format(LaunchConfigurationsMessages.EnvironmentTab_13,
								new Object[] { newVariableName }));
				if (!overWrite) {
					return false;
				}
				environmentTable.remove(existingVariable);
				return true;
			}
		}
		return true;
	}

	/**
	 * Attempts to add the given variable. Returns whether the variable
	 * was added or not (as when the user answers not to overwrite an
	 * existing variable).
	 * @param variable the variable to add
	 * @return whether the variable was added
	 */
	protected boolean addVariable(EnvironmentVariable variable) {
		String name = variable.getName();
		TableItem[] items = environmentTable.getTable().getItems();
		for (TableItem item : items) {
			EnvironmentVariable existingVariable = (EnvironmentVariable) item.getData();
			if (existingVariable.getName().equals(name)) {

				boolean overWrite = MessageDialog.openQuestion(getShell(),
						LaunchConfigurationsMessages.EnvironmentTab_12,
						MessageFormat.format(LaunchConfigurationsMessages.EnvironmentTab_13, new Object[] { name })); //
				if (!overWrite) {
					return false;
				}
				environmentTable.remove(existingVariable);
				break;
			}
		}
		environmentTable.add(variable);
		updateLaunchConfigurationDialog();
		return true;
	}

	/**
	 * Attempts to add the given variables. Returns the number of variables added
	 * (as when the user answers not to overwrite an existing variable).
	 *
	 * @param variables the variables to add
	 * @return the number of variables added
	 * @since 3.14
	 */
	protected int addVariables(List<EnvironmentVariable> variables) {
		if (variables.isEmpty()) {
			return 0;
		}

		List<EnvironmentVariable> remove = new LinkedList<>();
		List<EnvironmentVariable> conflicting = new LinkedList<>();
		Map<String, String> requested = variables.stream()
				.collect(Collectors.toMap(EnvironmentVariable::getName, EnvironmentVariable::getValue));

		for (TableItem item : environmentTable.getTable().getItems()) {
			EnvironmentVariable existingVariable = (EnvironmentVariable) item.getData();
			String name = existingVariable.getName();
			String currentValue = requested.get(name);
			if (currentValue != null) {
				remove.add(existingVariable);
				if (!currentValue.equals(existingVariable.getValue())) {
					conflicting.add(existingVariable);
				}
			}
		}

		if (!conflicting.isEmpty()) {
			String names = conflicting.stream().map(EnvironmentVariable::getName).collect(Collectors.joining(", ")); //$NON-NLS-1$
			boolean overWrite = MessageDialog.openQuestion(getShell(),
					LaunchConfigurationsMessages.EnvironmentTab_Paste_Overwrite_Title,
					MessageFormat.format(LaunchConfigurationsMessages.EnvironmentTab_Paste_Overwrite_Message,
							new Object[] { names })); //
			if (!overWrite) {
				return 0;
			}
		}

		remove.forEach(environmentTable::remove);
		variables.forEach(environmentTable::add);
		updateLaunchConfigurationDialog();

		return variables.size();
	}

	/**
	 * Displays a dialog that allows user to select native environment variables to
	 * add to the table.
	 */
	private void handleEnvSelectButtonSelected() {
		// get Environment Variables from the OS
		Map<String, EnvironmentVariable> envVariables = getNativeEnvironment();

		// get Environment Variables from the table
		TableItem[] items = environmentTable.getTable().getItems();
		for (TableItem item : items) {
			EnvironmentVariable var = (EnvironmentVariable) item.getData();
			envVariables.remove(var.getName());
		}

		NativeEnvironmentSelectionDialog dialog = new NativeEnvironmentSelectionDialog(getShell(), envVariables);
		dialog.setTitle(LaunchConfigurationsMessages.EnvironmentTab_20);

		int button = dialog.open();
		if (button == Window.OK) {
			Object[] selected = dialog.getResult();
			for (Object o : selected) {
				environmentTable.add(o);
			}
		}

		updateAppendReplace();
		updateLaunchConfigurationDialog();
	}

	/**
	 * Gets native environment variable from the LaunchManager. Creates
	 * EnvironmentVariable objects.
	 *
	 * @return Map of name - EnvironmentVariable pairs based on native environment.
	 */
	private Map<String, EnvironmentVariable> getNativeEnvironment() {
		Map<String, String> stringVars = DebugPlugin.getDefault().getLaunchManager()
				.getNativeEnvironmentCasePreserved();
		HashMap<String, EnvironmentVariable> vars = new HashMap<>();
		for (Entry<String, String> entry : stringVars.entrySet()) {
			vars.put(entry.getKey(), new EnvironmentVariable(entry.getKey(), entry.getValue()));
		}
		return vars;
	}

	/**
	 * Creates an editor for the value of the selected environment variable.
	 */
	private void handleEnvEditButtonSelected() {
		IStructuredSelection sel = environmentTable.getStructuredSelection();
		EnvironmentVariable var = (EnvironmentVariable) sel.getFirstElement();
		if (var == null) {
			return;
		}
		String originalName = var.getName();
		String value = var.getValue();
		MultipleInputDialog dialog = new MultipleInputDialog(getShell(),
				LaunchConfigurationsMessages.EnvironmentTab_11);
		dialog.addTextField(NAME_LABEL, originalName, false);
		if (value != null && value.contains(System.lineSeparator())) {
			dialog.addMultilinedVariablesField(VALUE_LABEL, value, true);
		} else {
			dialog.addVariablesField(VALUE_LABEL, value, true);
		}

		if (dialog.open() != Window.OK) {
			return;
		}

		String name = dialog.getStringValue(NAME_LABEL);
		value = dialog.getStringValue(VALUE_LABEL);
		if (!originalName.equals(name)) {
			// Trim the environment variable name but *NOT* the value
			if (addVariable(new EnvironmentVariable(name.trim(), value))) {
				environmentTable.remove(var);
			}
		} else {
			var.setValue(value);
			environmentTable.update(var, null);
			updateLaunchConfigurationDialog();
		}
	}

	/**
	 * Removes the selected environment variable from the table.
	 */
	private void handleEnvRemoveButtonSelected() {
		IStructuredSelection sel = environmentTable.getStructuredSelection();
		try {
			environmentTable.getControl().setRedraw(false);
			for (Iterator<?> i = sel.iterator(); i.hasNext();) {
				EnvironmentVariable var = (EnvironmentVariable) i.next();
				environmentTable.remove(var);
			}
		} finally {
			environmentTable.getControl().setRedraw(true);
		}
		updateAppendReplace();
		updateLaunchConfigurationDialog();
	}

	/**
	 * Copy the currently selected table entries to the clipboard.
	 */
	private void handleEnvCopyButtonSelected() {
		Iterable<?> iterable = () -> environmentTable.getStructuredSelection().iterator();
		String data = StreamSupport.stream(iterable.spliterator(), false).filter(o -> o instanceof EnvironmentVariable)
				.map(EnvironmentVariable.class::cast).map(var -> String.format("%s=%s", var.getName(), var.getValue())) //$NON-NLS-1$
				.collect(Collectors.joining(System.lineSeparator()));

		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		try {
			clipboard.setContents(new Object[] { data }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
	}

	/**
	 * Extract the content from the clipboard and add the new content.
	 */
	private void handleEnvPasteButtonSelected() {
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		try {
			List<EnvironmentVariable> variables = convertEnvironmentVariablesFromData(
					clipboard.getContents(TextTransfer.getInstance()));
			addVariables(variables);
			updateAppendReplace();
		} finally {
			clipboard.dispose();
		}
	}

	/**
	 * Convert the clipboard data to a list of {@link EnvironmentVariable}s. <br>
	 * Only entries containing an equals sign ({@code =} will be considered.
	 *
	 * @param data The clipboard data. May be {@code null}, which will result in an
	 *             empty list.
	 * @return The resulting and valid {@link EnvironmentVariable}s in an
	 *         unmodifiable list.
	 */
	private static List<EnvironmentVariable> convertEnvironmentVariablesFromData(Object data) {
		if (!(data instanceof String)) {
			return Collections.emptyList();
		}

		String entries[] = ((String) data).split("\\R"); //$NON-NLS-1$
		List<EnvironmentVariable> result = new ArrayList<>(entries.length);
		for (String entry : entries) {
			int idx = entry.indexOf('=');
			if (idx < 1) {
				continue;
			}
			// the name is trimmed ...
			String name = entry.substring(0, idx).trim();
			// .. but the value is *not* trimmed
			String value = entry.substring(idx + 1);
			result.add(new EnvironmentVariable(name, value));
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * Updates the environment table for the given launch configuration
	 *
	 * @param configuration the configuration to use as input for the backing table
	 */
	protected void updateEnvironment(ILaunchConfiguration configuration) {
		environmentTable.setInput(configuration);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.removeAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		boolean append = true;
		try {
			append = configuration.getAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
		} catch (CoreException e) {
			DebugUIPlugin.log(e.getStatus());
		}
		if (append) {
			appendEnvironment.setSelection(true);
			replaceEnvironment.setSelection(false);
		} else {
			replaceEnvironment.setSelection(true);
			appendEnvironment.setSelection(false);
		}
		updateEnvironment(configuration);
		updateAppendReplace();
	}

	/**
	 * Stores the environment in the given configuration
	 *
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		// Convert the table's items into a Map so that this can be saved in the
		// configuration's attributes.
		TableItem[] items = environmentTable.getTable().getItems();
		Map<String, String> map = new HashMap<>(items.length);
		for (TableItem item : items) {
			EnvironmentVariable var = (EnvironmentVariable) item.getData();
			map.put(var.getName(), var.getValue());
		}
		if (map.isEmpty()) {
			configuration.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, (Map<String, String>) null);
		} else {
			configuration.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, map);
		}

		if (appendEnvironment.getSelection()) {
			ILaunchConfiguration orig = configuration.getOriginal();
			boolean hasTrueValue = false;
			if (orig != null) {
				try {
					hasTrueValue = orig.hasAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES)
							&& orig.getAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
				} catch (CoreException e) {
					DebugUIPlugin.log(e.getStatus());
				}
			}
			if (hasTrueValue) {
				configuration.setAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
			} else {
				configuration.removeAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES);
			}
		} else {
			configuration.setAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, false);
		}
	}

	@Override
	public String getName() {
		return LaunchConfigurationsMessages.EnvironmentTab_Environment_7;
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
	 *
	 * @since 3.3
	 */
	@Override
	public String getId() {
		return "org.eclipse.debug.ui.environmentTab"; //$NON-NLS-1$
	}

	@Override
	public Image getImage() {
		return DebugPluginImages.getImage(IDebugUIConstants.IMG_OBJS_ENVIRONMENT);
	}

	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		// do nothing when activated
	}

	@Override
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
		// do nothing when deactivated
	}

	/**
	 * @since 3.13
	 */
	@Override
	protected void initializeAttributes() {
		super.initializeAttributes();
		getAttributesLabelsForPrototype().put(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES,
				LaunchConfigurationsMessages.EnvironmentTab_AttributeLabel_AppendEnvironmentVariables);
		getAttributesLabelsForPrototype().put(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES,
				LaunchConfigurationsMessages.EnvironmentTab_AttributeLabel_EnvironmentVariables);
	}

	private KeyStroke computeKeyStroke(KeyEvent e) {
		int accelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
		return SWTKeySupport.convertAcceleratorToKeyStroke(accelerator);
	}

	/**
	 * This dialog allows users to select one or more known native environment
	 * variables from a list.
	 */
	private class NativeEnvironmentSelectionDialog extends AbstractDebugCheckboxSelectionDialog {

		private Object fInput;

		public NativeEnvironmentSelectionDialog(Shell parentShell, Object input) {
			super(parentShell);
			fInput = input;
			setShellStyle(getShellStyle() | SWT.RESIZE);
			setShowSelectAllButtons(true);
		}

		@Override
		protected String getDialogSettingsId() {
			return IDebugUIConstants.PLUGIN_ID + ".ENVIRONMENT_TAB.NATIVE_ENVIROMENT_DIALOG"; //$NON-NLS-1$
		}

		@Override
		protected String getHelpContextId() {
			return IDebugHelpContextIds.SELECT_NATIVE_ENVIRONMENT_DIALOG;
		}

		@Override
		protected Object getViewerInput() {
			return fInput;
		}

		@Override
		protected String getViewerLabel() {
			return LaunchConfigurationsMessages.EnvironmentTab_19;
		}

		@Override
		protected IBaseLabelProvider getLabelProvider() {
			return new ILabelProvider() {
				@Override
				public Image getImage(Object element) {
					return DebugPluginImages.getImage(IDebugUIConstants.IMG_OBJS_ENVIRONMENT);
				}

				@Override
				public String getText(Object element) {
					EnvironmentVariable var = (EnvironmentVariable) element;
					return MessageFormat.format(LaunchConfigurationsMessages.EnvironmentTab_7,
							new Object[] { var.getName(), var.getValue() });
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
			};
		}

		@Override
		protected IContentProvider getContentProvider() {
			return new IStructuredContentProvider() {
				@Override
				public Object[] getElements(Object inputElement) {
					EnvironmentVariable[] elements = null;
					if (inputElement instanceof HashMap) {
						Comparator<Object> comparator = (o1, o2) -> {
							String s1 = (String) o1;
							String s2 = (String) o2;
							return s1.compareTo(s2);
						};
						TreeMap<Object, Object> envVars = new TreeMap<>(comparator);
						envVars.putAll((Map<?, ?>) inputElement);
						elements = new EnvironmentVariable[envVars.size()];
						int index = 0;
						for (Iterator<Object> iterator = envVars.keySet().iterator(); iterator.hasNext(); index++) {
							Object key = iterator.next();
							elements[index] = (EnvironmentVariable) envVars.get(key);
						}
					}
					return elements;
				}

				@Override
				public void dispose() {
				}

				@Override
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				}
			};
		}
	}
}
