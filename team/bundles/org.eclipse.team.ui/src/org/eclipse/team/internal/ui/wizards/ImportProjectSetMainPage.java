/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.team.internal.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.ProjectSetImporter;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WorkingSetGroup;

public class ImportProjectSetMainPage extends TeamWizardPage {
	Combo fileCombo;
	String file = ""; //$NON-NLS-1$
	Button browseButton;

	String urlString = ""; //$NON-NLS-1$
	Combo urlCombo;

	// input type radios
	private Button fileInputButton;
	private Button urlInputButton;

	// input type
	public static final int InputType_file = 0;
	public static final int InputType_URL = 1;
	private int inputType = InputType_file;

	private boolean runInBackground = isRunInBackgroundPreferenceOn();
	// a wizard shouldn't be in an error state until the state has been modified by the user
	private int messageType = NONE;
	private WorkingSetGroup workingSetGroup;

	private PsfFilenameStore psfFilenameStore = PsfFilenameStore.getInstance();
	private PsfUrlStore psfUrlStore = PsfUrlStore.getInstance();

	public ImportProjectSetMainPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setDescription(TeamUIMessages.ImportProjectSetMainPage_description);
	}

	private void setInputType(int inputTypeSelected) {
		this.inputType = inputTypeSelected;
		// reset the message type and give the user fresh chance to input
		// correct data
		messageType = NONE;
		// update controls
		fileInputButton.setSelection(inputType == InputType_file);
		fileCombo.setEnabled(inputType == InputType_file);
		browseButton.setEnabled(inputType == InputType_file);
		urlInputButton.setSelection(inputType == InputType_URL);
		urlCombo.setEnabled(inputType == InputType_URL);
		// validate field
		if (inputType == InputType_file)
			updateFileEnablement();
		if (inputType == InputType_URL)
			updateUrlEnablement();

	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 1);
		initializeDialogUnits(composite);

		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.IMPORT_PROJECT_SET_PAGE);

		Composite inner = new Composite(composite, SWT.NULL);
		inner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		inner.setLayout(layout);

		fileInputButton = new Button(inner, SWT.RADIO);
		fileInputButton
				.setText(TeamUIMessages.ImportProjectSetMainPage_Project_Set_File);
		fileInputButton.setEnabled(true);
		fileInputButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setInputType(InputType_file);
			}
		});

		fileCombo = createDropDownCombo(inner);
		file = psfFilenameStore.getSuggestedDefault();
		fileCombo.setItems(psfFilenameStore.getHistory());
		fileCombo.setText(file);
		fileCombo.addListener(SWT.Modify, event -> {
			file = fileCombo.getText();
			updateFileEnablement();
		});

		browseButton = new Button(inner, SWT.PUSH);
		browseButton.setText(TeamUIMessages.ImportProjectSetMainPage_Browse_3);

		urlInputButton = new Button(inner, SWT.RADIO);
		urlInputButton
				.setText(TeamUIMessages.ImportProjectSetMainPage_Project_Set_Url);
		urlInputButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setInputType(InputType_URL);
			}
		});
		urlCombo = createDropDownCombo(inner);
		urlString = psfUrlStore.getSuggestedDefault();
		urlCombo.setItems(psfUrlStore.getHistory());
		urlCombo.setText(urlString);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		urlCombo.setLayoutData(gd);
		urlCombo.addListener(SWT.Modify, event -> {
			urlString = urlCombo.getText();
			updateUrlEnablement();
		});

		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, browseButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		browseButton.setLayoutData(data);
		browseButton.addListener(SWT.Selection, event -> {
			FileDialog d = new FileDialog(getShell());
			d.setFilterExtensions(new String[] {"*.psf", "*"}); //$NON-NLS-1$ //$NON-NLS-2$
			d.setFilterNames(new String[] {TeamUIMessages.ImportProjectSetMainPage_Project_Set_Files_2, TeamUIMessages.ImportProjectSetMainPage_allFiles}); //
			String fileName= getFileName();
			if (fileName != null && fileName.length() > 0) {
				int separator= fileName.lastIndexOf(File.separatorChar);
				if (separator != -1) {
					fileName= fileName.substring(0, separator);
				}
			} else {
				fileName= ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
			}
			d.setFilterPath(fileName);
			String f = d.open();
			if (f != null) {
				fileCombo.setText(f);
				file = f;
			}
		});

		addWorkingSetSection(composite);

		Button runInBackgroundCheckbox = SWTUtils.createCheckBox(composite, TeamUIMessages.ImportProjectSetMainPage_runInBackground, 3);

		runInBackgroundCheckbox.setSelection(isRunInBackgroundPreferenceOn());
		runInBackgroundCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				runInBackground = !runInBackground;
			}
		});

		setControl(composite);
		setDefaultInputType();
		Dialog.applyDialogFont(parent);
	}

	private void setDefaultInputType() {
		// check for clipboard contents
		Control c = getControl();
		if (c != null) {
			Clipboard clipboard = new Clipboard(c.getDisplay());
			Object o = clipboard.getContents(TextTransfer.getInstance());
			clipboard.dispose();
			if (o instanceof String) {
				try {
					URL url = new URL((String) o);
					if (url != null) {
						setInputType(InputType_URL);
						urlCombo.setText((String) o);
						return;
					}
				} catch (MalformedURLException e) {
					// ignore, it's not and URL
				}
			}
		}
		setInputType(InputType_file);
	}

	private void addWorkingSetSection(Composite composite) {
		workingSetGroup = new WorkingSetGroup(
				composite,
				null,
				new String[] { "org.eclipse.ui.resourceWorkingSetPage", //$NON-NLS-1$
						"org.eclipse.jdt.ui.JavaWorkingSetPage" /* JavaWorkingSetUpdater.ID */}); //$NON-NLS-1$
	}

	private void updateUrlEnablement() {
		boolean complete = false;
		setMessage(null);
		setErrorMessage(null);

		if (urlString.length() == 0) {
			setMessage(TeamUIMessages.ImportProjectSetMainPage_specifyURL,
					messageType);
			complete = false;
		} else {

			try {
				new URL(urlString);
				// the URL is correct, we can clear the error message
				complete = true;
			} catch (MalformedURLException e) {
				messageType = ERROR;
				setMessage(TeamUIMessages.ImportProjectSetDialog_malformed_url,
						messageType);
				complete = false;
			}
		}

		if (complete) {
			setErrorMessage(null);
			setDescription(TeamUIMessages.ImportProjectSetMainPage_description);
		}

		setPageComplete(complete);
	}

	private void updateFileEnablement() {
		boolean complete = false;
		setMessage(null);
		setErrorMessage(null);

		if (file.length() == 0) {
			setMessage(TeamUIMessages.ImportProjectSetMainPage_specifyFile, messageType);
			setPageComplete(false);
			return;
		} else {
			// See if the file exists
			File f = new File(file);
			if (!f.exists()) {
				messageType = ERROR;
				setMessage(TeamUIMessages.ImportProjectSetMainPage_The_specified_file_does_not_exist_4, messageType);
				setPageComplete(false);
				return;
			} else if (f.isDirectory()) {
				messageType = ERROR;
				setMessage(TeamUIMessages.ImportProjectSetMainPage_You_have_specified_a_folder_5, messageType);
				setPageComplete(false);
				return;
			} else if (!ProjectSetImporter.isValidProjectSetFile(file)) {
				messageType = ERROR;
				setMessage(TeamUIMessages.ImportProjectSetMainPage_projectSetFileInvalid, messageType);
				setPageComplete(false);
				return;
			}
			complete = true;
		}

		if (complete) {
			setErrorMessage(null);
			setDescription(TeamUIMessages.ImportProjectSetMainPage_description);
		}

		setPageComplete(complete);
	}

	public String getFileName() {
		return file;
	}

	public String getUrl() {
		return urlString;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			fileCombo.setFocus();
		}
	}

	/**
	 * Return the working sets selected on the page or an empty array if none
	 * were selected.
	 *
	 * @return the selected working sets or an empty array
	 */
	public IWorkingSet[] getWorkingSets() {
		return workingSetGroup.getSelectedWorkingSets();
	}

	private static boolean isRunInBackgroundPreferenceOn() {
		return TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(
				IPreferenceIds.RUN_IMPORT_IN_BACKGROUND);
	}

	public boolean isRunInBackgroundOn() {
		return runInBackground;
	}

	public int getInputType() {
		return inputType;
	}

	public String getURLContents() {
		try {
			PsfUrlStore.getInstance().remember(urlString);
			String urlContent = Utilities.getURLContents(new URL(urlString),
					getContainer());
			if (ProjectSetImporter.isValidProjectSetString(urlContent)) {
				return urlContent;
			} else {
				messageType = ERROR;
				setMessage(
						TeamUIMessages.ImportProjectSetMainPage_projectSetFileInvalid,
						messageType);
				setPageComplete(false);
				return null;
			}
		} catch (OperationCanceledException e) { // ignore
		} catch (InterruptedException e) { // ignore
		} catch (InvocationTargetException e) {
			messageType = ERROR;
			setMessage(
					TeamUIMessages.ImportProjectSetMainPage_The_given_URL_cannot_be_loaded,
					messageType);
			setPageComplete(false);
		} catch (MalformedURLException e) {
			// ignore as we tested it with modify listener on combo
		}
		return null;
	}
}
