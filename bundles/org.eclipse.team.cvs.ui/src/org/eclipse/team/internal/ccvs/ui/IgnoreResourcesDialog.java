/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.fieldassist.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.util.FileNameMatcher;
import org.eclipse.ui.PlatformUI;

public class IgnoreResourcesDialog extends TitleAreaDialog {
	// resources that should be ignored
	private IResource[] resources;

	// preference keys
	private final String ACTION_KEY = "Action"; //$NON-NLS-1$
	private static final int ADD_NAME_ENTRY = 0;
	private static final int ADD_EXTENSION_ENTRY = 1;
	private static final int ADD_CUSTOM_ENTRY = 2;

	// dialogs settings that are persistent between workbench sessions
	private IDialogSettings settings;

	// buttons
	private Button addNameEntryButton;
	private Button addExtensionEntryButton;
	private Button addCustomEntryButton;
	private Text customEntryText;
	
	private int selectedAction;
	private String customPattern;
	
	// layout controls
	private static final int LABEL_INDENT_WIDTH = 32;
	
	/**
	 * Image for title area
	 */
	private Image dlgTitleImage = null;
	
	// to avoid an error/warning message at startup default values are as below
	private boolean resourceWithSpaces = false;
	private boolean allResourecesHaveExtensions = true;
	private boolean allResourcesWithSpacesHaveExtensions = true;

	/**
	 * Creates a new dialog for ignoring resources.
	 * @param shell the parent shell
	 * @param resources the array of resources to be ignored
	 */
	public IgnoreResourcesDialog(Shell shell, IResource[] resources) {
		super(shell);
		this.resources = resources;

		IDialogSettings workbenchSettings = CVSUIPlugin.getPlugin().getDialogSettings();
		this.settings = workbenchSettings.getSection("IgnoreResourcesDialog"); //$NON-NLS-1$
		if (settings == null) {
			this.settings = workbenchSettings.addNewSection("IgnoreResourcesDialog"); //$NON-NLS-1$
		}
		
		try {
			selectedAction = settings.getInt(ACTION_KEY);
		} catch (NumberFormatException e) {
			selectedAction = ADD_NAME_ENTRY;
		}
		
		resourceWithSpaces = checkForResourcesWithSpaces();
		allResourecesHaveExtensions = checkIfAllResourcesHaveExtensions();
		allResourcesWithSpacesHaveExtensions  = checkIfAllResourcesWithSpacesHaveExtensions();
	}
	
	/**
	 * Determines the ignore pattern to use for a resource given the selected action.
	 * 
	 * @param resource the resource
	 * @return the ignore pattern for the specified resource
	 */
	public String getIgnorePatternFor(IResource resource) {
		switch (selectedAction) {
			case ADD_NAME_ENTRY:
				return resource.getName();
			case ADD_EXTENSION_ENTRY: {
				String extension = resource.getFileExtension();
				return (extension == null) ? resource.getName() : "*." + extension; //$NON-NLS-1$
			}
			case ADD_CUSTOM_ENTRY:
				return customPattern;
		}
		throw new IllegalStateException();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(CVSUIMessages.IgnoreResourcesDialog_dialogTitle);
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		dlgTitleImage = CVSUIPlugin.getPlugin().getImageDescriptor(
				ICVSUIConstants.IMG_WIZBAN_NEW_LOCATION).createImage();
		setTitleImage(dlgTitleImage);
		updateEnablements();
		setTitle(CVSUIMessages.IgnoreResourcesDialog_title);
		setErrorMessage(null);
		setDefaultMessage();
		return control;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite top = new Composite((Composite) super
				.createDialogArea(parent), SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		top.setLayout(layout);
		top.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(top, IHelpContextIds.ADD_TO_CVSIGNORE);
		
		Listener selectionListener = event -> updateEnablements();
		Listener modifyListener = event -> validate();
		
		addNameEntryButton = createRadioButton(top, CVSUIMessages.IgnoreResourcesDialog_addNameEntryButton); 
		addNameEntryButton.addListener(SWT.Selection, selectionListener);
		addNameEntryButton.setSelection(selectedAction == ADD_NAME_ENTRY);
		Label addNameEntryLabel = createIndentedLabel(top, CVSUIMessages.IgnoreResourcesDialog_addNameEntryExample, LABEL_INDENT_WIDTH); 
		
		addExtensionEntryButton = createRadioButton(top, CVSUIMessages.IgnoreResourcesDialog_addExtensionEntryButton); 
		addExtensionEntryButton.addListener(SWT.Selection, selectionListener);
		addExtensionEntryButton.setSelection(selectedAction == ADD_EXTENSION_ENTRY);
		Label addExtensionEntryLabel = createIndentedLabel(top, CVSUIMessages.IgnoreResourcesDialog_addExtensionEntryExample, LABEL_INDENT_WIDTH); 

		addCustomEntryButton = createRadioButton(top, CVSUIMessages.IgnoreResourcesDialog_addCustomEntryButton); 
		addCustomEntryButton.addListener(SWT.Selection, selectionListener);
		addCustomEntryButton.setSelection(selectedAction == ADD_CUSTOM_ENTRY);
		createIndentedLabel(top, CVSUIMessages.IgnoreResourcesDialog_addCustomEntryExample, LABEL_INDENT_WIDTH); 
		
		if (resourceWithSpaces) {
			customEntryText = createIndentedText(top, getResourceWithSpace()
					.getName().replaceAll(" ", "?"), LABEL_INDENT_WIDTH); //$NON-NLS-1$ //$NON-NLS-2$
			ControlDecoration customEntryTextDecoration = new ControlDecoration(
					customEntryText, SWT.TOP | SWT.LEAD);
			FieldDecorationRegistry registry = FieldDecorationRegistry
					.getDefault();
			FieldDecoration infoDecoration = registry
					.getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
			customEntryTextDecoration.setImage(infoDecoration.getImage());
			customEntryTextDecoration
					.setDescriptionText(CVSUIMessages.IgnoreResourcesDialog_filesWithSpaceWarning);
			customEntryTextDecoration.setShowOnlyOnFocus(false);
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalIndent = LABEL_INDENT_WIDTH
					+ FieldDecorationRegistry.getDefault()
							.getMaximumDecorationWidth();
			customEntryText.setLayoutData(gridData);
			customEntryText.setFocus();
			
			addNameEntryButton.setEnabled(false);
			addNameEntryLabel.setEnabled(false);
			// switch selection to "Custom pattern"
			if (!addCustomEntryButton.getSelection()) {
				addNameEntryButton.setSelection(false);
				addCustomEntryButton.setSelection(true);
				selectedAction = ADD_CUSTOM_ENTRY;
			}

			// when there is at least one resource without extension with spaces 
			// we should disable the "Wildcard extension" option
			if (!allResourcesWithSpacesHaveExtensions) {
				addExtensionEntryButton.setEnabled(false);
				addExtensionEntryLabel.setEnabled(false);
				// switch selection to "Custom pattern" which is always enabled
				addExtensionEntryButton.setSelection(false);
				addCustomEntryButton.setSelection(true);
				selectedAction = ADD_CUSTOM_ENTRY;
			} 
	
		} else {
			customEntryText = createIndentedText(top, resources[0].getName(),
					LABEL_INDENT_WIDTH);
		}
		
		customEntryText.addListener(SWT.Modify, modifyListener);

		applyDialogFont(top);
		
		return top;
	}

	@Override
	protected void okPressed() {
		settings.put(ACTION_KEY, selectedAction);
		super.okPressed();
	}

	private Label createIndentedLabel(Composite parent, String text, int indent) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL);
		data.horizontalIndent = indent;
		label.setLayoutData(data);
		return label;
	}

	private Text createIndentedText(Composite parent, String text, int indent) {
		Text textbox = new Text(parent, SWT.BORDER);
		textbox.setText(text);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalIndent = indent;
		textbox.setLayoutData(data);
		return textbox;
	}
	
	private Button createRadioButton(Composite parent, String text) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(text);
		button.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL));
		return button;
	}

	private void updateEnablements() {
		if (addNameEntryButton.getSelection()) {
			selectedAction = ADD_NAME_ENTRY;
		} else if (addExtensionEntryButton.getSelection()) {
			selectedAction = ADD_EXTENSION_ENTRY;
		} else if (addCustomEntryButton.getSelection()) {
			selectedAction = ADD_CUSTOM_ENTRY;
		}
		customEntryText.setEnabled(selectedAction == ADD_CUSTOM_ENTRY);
		validate();
	}
	
	private void validate() {
		if (selectedAction == ADD_CUSTOM_ENTRY) {
			customPattern = customEntryText.getText();
			if (customPattern.length() == 0) {
				setError(CVSUIMessages.IgnoreResourcesDialog_patternMustNotBeEmpty); 
				return;
			}
			FileNameMatcher matcher = new FileNameMatcher(new String[] { customPattern });
			for (IResource resource : resources) {
				String name = resource.getName();
				if (! matcher.match(name)) {
					setError(NLS.bind(CVSUIMessages.IgnoreResourcesDialog_patternDoesNotMatchFile, new String[] { name })); 
					return;
				}
			}
			if (resourceWithSpaces) {
				setWarning(CVSUIMessages.IgnoreResourcesDialog_filesWithSpaceWarningMessage);
				return;
			}
		} else if (selectedAction == ADD_EXTENSION_ENTRY && !allResourecesHaveExtensions) {
			setWarning(CVSUIMessages.IgnoreResourcesDialog_filesWithNoExtensionWarningMessage);
			return;
		} 
		setError(null);
	}
	
	/**
	 * Sets an error message on the dialog.
	 * 
	 * @param text
	 *            A message to set. If <code>null</code> the error message
	 *            will be cleared.
	 */
	private void setError(String text) {
		setErrorMessage(text);
		// set a non-error message
		if (text == null)
			setDefaultMessage();
		getButton(IDialogConstants.OK_ID).setEnabled(text == null);
	}
	
	private void setWarning(String text) {
		setError(null);
		setMessage(text, IMessageProvider.WARNING);
	}
	
	private void setDefaultMessage() {
		if (resources.length == 1) {
			setMessage(NLS.bind(
					CVSUIMessages.IgnoreResourcesDialog_messageSingle,
					new String[] { resources[0].getName() }));
		} else {
			setMessage(NLS.bind(
					CVSUIMessages.IgnoreResourcesDialog_messageMany,
					new String[] { Integer.toString(resources.length) }));
		}
	}
	
	/**
	 * Check whether at least one of the given resources names contain a space.
	 * 
	 * @param resources
	 *            Array of resources to check.
	 * @return <code>true</code> if a resource filename containing space has
	 *         been found, <code>false</code> otherwise.
	 */
	private boolean checkForResourcesWithSpaces() {
		return getResourceWithSpace() != null;
	}

	private IResource getResourceWithSpace() {
		for (IResource resource : resources) {
			if (resource.getName().contains(" ")) { //$NON-NLS-1$
				return resource;
			}
		}
		return null;
	}

	private boolean checkIfAllResourcesWithSpacesHaveExtensions() {
		for (IResource resource : resources) {
			if (resource.getName().contains(" ") && resource.getFileExtension() == null) { //$NON-NLS-1$
				return false;
			}
		}
		return true;
	}

	private boolean checkIfAllResourcesHaveExtensions() {
		for (IResource resource : resources) {
			if (resource.getFileExtension() != null) {
				return true;
			}
		}
		// couldn't find a resource with an extension
		return false;
	}
	
	@Override
	public boolean close() {
		if (dlgTitleImage != null) {
			dlgTitleImage.dispose();
		}
		return super.close();
	}
}
