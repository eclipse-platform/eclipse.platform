/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.examples.filesystem.Policy;

/**
 * This class provides the main page of the file system repository configuration wizard.
 * It allows the user to select a location on disk. Once the page is finished, the
 * location can be accessed using the <code>getLocation()</code> method.
 */
public class FileSystemMainPage extends WizardPage {

	private static final int COMBO_HISTORY_LENGTH = 5;

	String location;
	Combo locationCombo;

	/*
	 * WizardPage constructor comment.
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param description  the description of the page
	 * @param titleImage  the image for the page
	 */

	public FileSystemMainPage(String pageName, String title, String description, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setDescription(description);
		setTitle(title);
	}

	/*
	 * Creates a new checkbox instance and sets the default layout data.
	 *
	 * @param group  the composite in which to create the checkbox
	 * @param label  the string to set into the checkbox
	 * @return the new checkbox
	 */
	protected Button createCheckBox(Composite group, String label) {
		Button button = new Button(group, SWT.CHECK | SWT.LEFT);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		button.setLayoutData(data);
		return button;
	}

	/*
	 * Utility method that creates a combo box
	 *
	 * @param parent  the parent for the new label
	 * @return the new widget
	 */
	protected Combo createCombo(Composite parent) {
		Combo combo = new Combo(parent, SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		combo.setLayoutData(data);
		return combo;
	}

	/*
	 * Creates composite control and sets the default layout data.
	 *
	 * @param parent  the parent of the new composite
	 * @param numColumns  the number of columns for the new composite
	 * @return the newly-created coposite
	 */
	protected Composite createComposite(Composite parent, int numColumns) {
		Composite composite = new Composite(parent, SWT.NULL);

		// GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		composite.setLayout(layout);

		// GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);
		return composite;
	}

	/*
	 * Utility method that creates a label instance
	 * and sets the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @return the new label
	 */
	protected Label createLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}

	/*
	 * Create a text field specific for this application
	 *
	 * @param parent  the parent of the new text field
	 * @return the new text field
	 */
	protected Text createTextField(Composite parent) {
		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = GridData.CENTER;
		data.grabExcessVerticalSpace = false;
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		text.setLayoutData(data);
		return text;
	}

	/*
	 * Adds an entry to a history, while taking care of duplicate history items
	 * and excessively long histories.  The assumption is made that all histories
	 * should be of length <code>ConfigurationWizardMainPage.COMBO_HISTORY_LENGTH</code>.
	 *
	 * @param history the current history
	 * @param newEntry the entry to add to the history
	 * @return the history with the new entry appended
	 */
	protected String[] addToHistory(String[] history, String newEntry) {
		ArrayList<String> l = new ArrayList<>(Arrays.asList(history));
		addToHistory(l, newEntry);
		String[] r = new String[l.size()];
		l.toArray(r);
		return r;
	}

	/*
	 * Adds an entry to a history, while taking care of duplicate history items
	 * and excessively long histories.  The assumption is made that all histories
	 * should be of length <code>ConfigurationWizardMainPage.COMBO_HISTORY_LENGTH</code>.
	 *
	 * @param history the current history
	 * @param newEntry the entry to add to the history
	 */
	protected void addToHistory(List<String> history, String newEntry) {
		history.remove(newEntry);
		history.add(0,newEntry);

		// since only one new item was added, we can be over the limit
		// by at most one item
		if (history.size() > COMBO_HISTORY_LENGTH)
			history.remove(COMBO_HISTORY_LENGTH);
	}

	/*
	 * Utility method to create an editable combo box
	 *
	 * @param parent  the parent of the combo box
	 * @return the created combo
	 */
	protected Combo createEditableCombo(Composite parent) {
		Combo combo = new Combo(parent, SWT.NULL);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		combo.setLayoutData(data);
		return combo;
	}

	// Dialog store id constants
	private static final String STORE_LOCATION =
			"ExamplesFSWizardMainPage.STORE_LOCATION";//$NON-NLS-1$

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		setControl(composite);

		Label label = new Label(composite, SWT.NULL);
		label.setText(Policy.bind("FileSystemMainPage.location")); //$NON-NLS-1$
		label.setLayoutData(new GridData());

		locationCombo = createEditableCombo(composite);
		locationCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		locationCombo.addListener(SWT.Modify, e -> {
			location = ((Combo)e.widget).getText();
			FileSystemMainPage.this.validateFields();
		});

		locationCombo.setFocus();

		new Label(composite, SWT.NULL);
		Button browse = new Button(composite, SWT.NULL);
		browse.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		browse.setText(Policy.bind("FileSystemMainPage.browseDir")); //$NON-NLS-1$
		browse.addListener(SWT.Selection, event -> {
			DirectoryDialog d = new DirectoryDialog(getShell());
			String directory = d.open();
			if(directory!=null) {
				locationCombo.setText(directory);
			}
		});

		initializeValues();
		validateFields();
	}

	public String getLocation() {
		return location;
	}

	public boolean finish(IProgressMonitor monitor) {
		saveWidgetValues();
		return true;
	}
	/**
	 * Initializes states of the controls.
	 */
	private void initializeValues() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			String[] locations = settings.getArray(STORE_LOCATION);
			if (locations != null) {
				for (String location2 : locations) {
					locationCombo.add(location2);
				}
				locationCombo.select(0);
			}
		}
	}
	/**
	 * Saves the widget values
	 */
	private void saveWidgetValues() {
		// Update history
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			String[] locations = settings.getArray(STORE_LOCATION);
			if (locations == null) locations = new String[0];
			locations = addToHistory(locations, locationCombo.getText());
			settings.put(STORE_LOCATION, locations);
		}
	}

	/*
	 * Validates the contents of the editable fields and set page completion
	 * and error messages appropriately.
	 */
	void validateFields() {
		String location = locationCombo.getText();
		if (location.length() == 0) {
			setErrorMessage(null);
			setPageComplete(false);
			return;
		}
		File file = new File(location);
		if(!file.exists() || !file.isDirectory()) {
			setErrorMessage(Policy.bind("FileSystemMainPage.notValidLocation")); //$NON-NLS-1$
			setPageComplete(false);
			return;
		}
		setErrorMessage(null);
		setPageComplete(true);
	}
}
