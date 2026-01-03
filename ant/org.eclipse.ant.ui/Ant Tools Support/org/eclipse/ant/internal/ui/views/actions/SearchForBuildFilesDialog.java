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
package org.eclipse.ant.internal.ui.views.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.osgi.framework.FrameworkUtil;

/**
 * This dialog allows the user to search for Ant build files whose names match a given pattern. The search may be performed on the entire workspace or
 * it can be limited to a particular working set.
 */
public class SearchForBuildFilesDialog extends InputDialog {

	/**
	 * List of <code>IFile</code> objects that were found
	 */
	private List<IResource> results = new ArrayList<>();
	/**
	 * List of <code>IResource</code> objects in which to search.
	 *
	 * If the searchScopes are <code>null</code>, the user has asked to search the workspace. If the searchScopes are empty, the user has asked to
	 * search a working set that has no resources.
	 */
	private List<IResource> searchScopes = null;
	/**
	 * The working set scope radio button.
	 */
	private Button workingSetScopeButton;
	/**
	 * The workspace scope radio button.
	 */
	private Button workspaceScopeButton;
	/**
	 * The text field that displays the current working set name
	 */
	private Text workingSetText;
	/**
	 * The button that allows the user to decide if error results should be parsed
	 */
	private Button includeErrorResultButton;
	/**
	 * The dialog settings used to persist this dialog's settings.
	 */
	private static IDialogSettings settings = PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(SearchForBuildFilesDialog.class)).getDialogSettings();

	/**
	 * Initialize any dialog settings that haven't been set.
	 */
	static {
		if (settings.get(IAntUIPreferenceConstants.ANTVIEW_LAST_SEARCH_STRING) == null) {
			settings.put(IAntUIPreferenceConstants.ANTVIEW_LAST_SEARCH_STRING, "build.xml"); //$NON-NLS-1$
		}
		if (settings.get(IAntUIPreferenceConstants.ANTVIEW_LAST_WORKINGSET_SEARCH_SCOPE) == null) {
			settings.put(IAntUIPreferenceConstants.ANTVIEW_LAST_WORKINGSET_SEARCH_SCOPE, IAntCoreConstants.EMPTY_STRING);
		}
	}

	/**
	 * Creates a new dialog to search for build files.
	 */
	public SearchForBuildFilesDialog() {
		super(Display.getCurrent().getActiveShell(), AntViewActionMessages.SearchForBuildFilesDialog_Search_for_Build_Files_1, AntViewActionMessages.SearchForBuildFilesDialog__Input, settings.get(IAntUIPreferenceConstants.ANTVIEW_LAST_SEARCH_STRING), newText -> {
			String trimmedText = newText.trim();
			if (trimmedText.length() == 0) {
				return AntViewActionMessages.SearchForBuildFilesDialog_Build_name_cannot_be_empty_3;
			}
			return null;
		});
	}

	/**
	 * Change the label on the "Ok" button and initialize the enabled state
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getOkButton().setText(AntViewActionMessages.SearchForBuildFilesDialog__Search_4);

		String workingSetName = settings.get(IAntUIPreferenceConstants.ANTVIEW_LAST_WORKINGSET_SEARCH_SCOPE);
		if (workingSetName.length() > 0) {
			setWorkingSet(PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSetName));
		}
		if (!settings.getBoolean(IAntUIPreferenceConstants.ANTVIEW_USE_WORKINGSET_SEARCH_SCOPE)) {
			selectRadioButton(workspaceScopeButton);
			handleRadioButtonPressed();
		}
	}

	/**
	 * Add the scope selection widgets to the dialog area
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Font font = parent.getFont();

		Composite composite = (Composite) super.createDialogArea(parent);
		createIncludeErrorResultButton(composite, font);
		createScopeGroup(composite, font);
		return composite;
	}

	private void createScopeGroup(Composite composite, Font font) {
		Group scope = new Group(composite, SWT.NONE);
		scope.setText(AntViewActionMessages.SearchForBuildFilesDialog_Scope_5);
		GridData data = new GridData(GridData.FILL_BOTH);
		scope.setLayoutData(data);
		GridLayout layout = new GridLayout(3, false);
		scope.setLayout(layout);
		scope.setFont(font);

		// Create a composite for the radio buttons
		Composite radioComposite = new Composite(scope, SWT.NONE);
		GridLayout radioLayout = new GridLayout();
		radioLayout.marginHeight = 0;
		radioComposite.setLayout(radioLayout);

		SelectionAdapter selectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleRadioButtonPressed();
			}
		};

		workspaceScopeButton = new Button(radioComposite, SWT.RADIO);
		workspaceScopeButton.setFont(font);
		workspaceScopeButton.setText(AntViewActionMessages.SearchForBuildFilesDialog__Workspace_6);
		workspaceScopeButton.addSelectionListener(selectionListener);

		workingSetScopeButton = new Button(radioComposite, SWT.RADIO);
		workingSetScopeButton.setFont(font);
		workingSetScopeButton.setText(AntViewActionMessages.SearchForBuildFilesDialog_Wor_king_Set__7);
		workingSetScopeButton.addSelectionListener(selectionListener);

		selectRadioButton(workspaceScopeButton);

		workingSetText = new Text(scope, SWT.BORDER);
		workingSetText.setEditable(false);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_END);
		workingSetText.setLayoutData(data);
		workingSetText.setFont(font);

		Button chooseButton = new Button(scope, SWT.PUSH);
		data = new GridData(GridData.VERTICAL_ALIGN_END);
		chooseButton.setLayoutData(data);
		chooseButton.setFont(font);
		chooseButton.setText(AntViewActionMessages.SearchForBuildFilesDialog__Choose____8);
		chooseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				handleChooseButtonPressed();
			}
		});
	}

	/**
	 * Programatically selects the given radio button, deselecting the other radio button.
	 *
	 * @param button
	 *            the radio button to select. This parameter must be one of either the <code>workingSetScopeButton</code> or the
	 *            <code>workspaceScopeButton</code> or this method will have no effect.
	 */
	private void selectRadioButton(Button button) {
		if (button == workingSetScopeButton) {
			workingSetScopeButton.setSelection(true);
			workspaceScopeButton.setSelection(false);
		} else if (button == workspaceScopeButton) {
			workspaceScopeButton.setSelection(true);
			workingSetScopeButton.setSelection(false);
		}
	}

	/**
	 * One of the search scope radio buttons has been pressed. Update the dialog accordingly.
	 */
	private void handleRadioButtonPressed() {
		if (workingSetScopeButton.getSelection()) {
			IWorkingSet set = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(getWorkingSetName());
			if (set != null) {
				setWorkingSet(set);
				return;
			}
		}
		setWorkingSet(null);
	}

	/**
	 * Returns the working set name currently displayed.
	 */
	private String getWorkingSetName() {
		return workingSetText.getText().trim();
	}

	/**
	 * Creates the button that allows the user to specify whether or not build files should that cannot be parsed should be included in the results.
	 */
	private void createIncludeErrorResultButton(Composite composite, Font font) {
		includeErrorResultButton = new Button(composite, SWT.CHECK);
		includeErrorResultButton.setFont(font);
		includeErrorResultButton.setText(AntViewActionMessages.SearchForBuildFilesDialog_Include_errors);
		includeErrorResultButton.setSelection(settings.getBoolean(IAntUIPreferenceConstants.ANTVIEW_INCLUDE_ERROR_SEARCH_RESULTS));
	}

	/**
	 * Updates the dialog based on the state of the working set settings
	 * <ul>
	 * <li>Sets the enablement of the "Search" button based on the validity of the settings</li>
	 * <li>Sets any or clears the error message</li>
	 * </ul>
	 */
	private void updateForWorkingSetSettings() {
		if (workingSetScopeButton.getSelection()) {
			String error = null;
			if (searchScopes == null) {
				error = AntViewActionMessages.SearchForBuildFilesDialog_Must_select_a_working_set_10;
			} else if (searchScopes.isEmpty()) {
				error = AntViewActionMessages.SearchForBuildFilesDialog_No_searchable;
			}
			if (error != null) {
				setErrorMessage(error);
				getOkButton().setEnabled(false);
				return;
			}
		}
		getOkButton().setEnabled(true);
		setErrorMessage(null);
	}

	/**
	 * Handles the working set choose button pressed. Returns the name of the chosen working set or <code>null</code> if none.
	 */
	private void handleChooseButtonPressed() {
		IWorkingSetSelectionDialog dialog = PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSetSelectionDialog(getShell(), false);
		if (dialog.open() == Window.CANCEL) {
			return;
		}
		IWorkingSet[] sets = dialog.getSelection();
		if (sets == null) {
			return;
		}
		if (sets.length == 0) {
			setWorkingSet(null); // ok pressed with no working set selected
		} else {
			setWorkingSet(sets[0]); // We disallowed multi-select
		}
	}

	/**
	 * Sets the current working set search scope. This populates the search scope with resources found in the given working set and updates the
	 * enabled state of the dialog based on the sets contents.
	 *
	 * @param set
	 *            the working set scope for the search
	 */
	private void setWorkingSet(IWorkingSet set) {
		if (set == null) {
			searchScopes = null;
			workingSetText.setText(IAntCoreConstants.EMPTY_STRING);
			validateInput();
			return;
		}
		searchScopes = new ArrayList<>();
		for (IAdaptable adaptable : set.getElements()) {
			// Try to get an IResource object from each element
			IResource resource = null;
			if (adaptable instanceof IResource) {
				resource = (IResource) adaptable;
			} else {
				resource = adaptable.getAdapter(IResource.class);
			}
			if (resource != null) {
				searchScopes.add(resource);
			}
		}
		workingSetText.setText(set.getName());
		selectRadioButton(workingSetScopeButton);

		validateInput();
	}

	/**
	 * Returns the trimmed user input
	 */
	private String getInput() {
		return getText().getText().trim();
	}

	/**
	 * Returns the search results
	 */
	public IFile[] getResults() {
		return results.toArray(new IFile[results.size()]);
	}

	/**
	 * Returns whether the user wishes to include results which cannot be parsed.
	 */
	protected boolean getIncludeErrorResults() {
		return settings.getBoolean(IAntUIPreferenceConstants.ANTVIEW_INCLUDE_ERROR_SEARCH_RESULTS);
	}

	/**
	 * When the user presses the search button (tied to the OK id), search the workspace for files matching the regular expression in the input field.
	 */
	@Override
	protected void okPressed() {
		String input = getInput();
		settings.put(IAntUIPreferenceConstants.ANTVIEW_LAST_SEARCH_STRING, input);
		settings.put(IAntUIPreferenceConstants.ANTVIEW_INCLUDE_ERROR_SEARCH_RESULTS, includeErrorResultButton.getSelection());
		settings.put(IAntUIPreferenceConstants.ANTVIEW_LAST_WORKINGSET_SEARCH_SCOPE, getWorkingSetName());
		settings.put(IAntUIPreferenceConstants.ANTVIEW_USE_WORKINGSET_SEARCH_SCOPE, workingSetScopeButton.getSelection());
		results = new ArrayList<>(); // Clear previous results
		ResourceProxyVisitor visitor = new ResourceProxyVisitor();
		if (searchScopes == null || searchScopes.isEmpty()) {
			try {
				ResourcesPlugin.getWorkspace().getRoot().accept(visitor, IResource.NONE);
			}
			catch (CoreException ce) {
				// Closed project...don't want build files from there
			}
		} else {
			Iterator<IResource> iter = searchScopes.iterator();
			while (iter.hasNext()) {
				try {
					iter.next().accept(visitor, IResource.NONE);
				}
				catch (CoreException ce) {
					// Closed project...don't want build files from there
				}
			}
		}
		super.okPressed();
	}

	/**
	 * Searches for files whose name matches the given regular expression.
	 */
	class ResourceProxyVisitor implements IResourceProxyVisitor {
		Pattern pattern;

		ResourceProxyVisitor() {
			// Users use "*" and "?" where regex uses ".*" and ".?"
			// The character "." must be escaped in regex
			String input = getInput();
			// replace "." with "\\."
			input = input.replace(".", "\\."); //$NON-NLS-1$ //$NON-NLS-2$
			// replace "*" with ".*"
			input = input.replace("*", ".*"); //$NON-NLS-1$ //$NON-NLS-2$
			// replace "?" with ".?"
			input = input.replace("?", ".?"); //$NON-NLS-1$ //$NON-NLS-2$
			pattern = Pattern.compile(input);
		}

		@Override
		public boolean visit(IResourceProxy proxy) {
			if (proxy.getType() == IResource.FILE) {
				Matcher matcher = pattern.matcher(proxy.getName());
				if (matcher.find()) {
					results.add(proxy.requestResource());
				}
				return false;
			}
			return true;
		}
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IAntUIHelpContextIds.SEARCH_FOR_BUILDFILES_DIALOG);
	}

	@Override
	protected void validateInput() {
		String errorMessage = null;
		if (getValidator() != null) {
			errorMessage = getValidator().isValid(getText().getText());
		}

		setErrorMessage(errorMessage);
		if (errorMessage == null) {
			updateForWorkingSetSettings();
		}
	}
}
