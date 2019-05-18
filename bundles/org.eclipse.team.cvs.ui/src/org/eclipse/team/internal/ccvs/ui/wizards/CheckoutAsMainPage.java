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
 *     Philippe Ombredanne - bug 84808
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WorkingSetGroup;

/**
 * This is the main page of the Check Out As wizard. It allows the user to specify
 * whether they want to check out the remote folder(s) as project(s) or into an 
 * existing project. For single project checkout, the page will also allow the user to
 * choose whether to configure the new project (if it is missing a .project file).
 */
public class CheckoutAsMainPage extends CVSWizardPage {

	private String newProjectName;
	private boolean allowProjectConfiguration;
	private Button intoProjectButton;
	private Button simpleProjectButton;
	private Button configuredProjectButton;
	private Text projectNameField;
	private ICVSRemoteFolder[] folders;
	
	private Button recurseCheck;
	private boolean recurse = true;
	
	private WorkingSetGroup workingSetGroup;
	
	public static final String NAME = "CheckoutAsMainPage"; //$NON-NLS-1$
	
	/**
	 * @param pageName
	 * @param title
	 * @param titleImage
	 * @param description
	 */
	public CheckoutAsMainPage(ImageDescriptor titleImage, ICVSRemoteFolder[] folders, boolean allowProjectConfiguration) {
		super(NAME, CVSUIMessages.CheckoutAsMainPage_title, titleImage, CVSUIMessages.CheckoutAsMainPage_description); // 
		this.folders = folders;
		this.allowProjectConfiguration = allowProjectConfiguration;
	}
	
	@Override
	public void createControl(Composite parent) {

		Composite composite = createComposite(parent, 1, false);
		setControl(composite);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.CHECKOUT_CONFIGURATION_PAGE);
		
		if (isSingleFolder()) {
			createSingleFolderArea(composite);
		} else {
			createMultipleFoldersArea(composite);
		}

		updateEnablements();
		Dialog.applyDialogFont(parent);
	}

	/*
	 * Is the input to the wizard a single folder or multiple folders
	 */
	private boolean isSingleFolder() {
		return folders.length == 1;
	}

	/*
	 * For the single folder case, return the name of the folder
	 */
	private String getFolderName() {
		String name = getPreferredFolderName(folders[0]);
		if (name .equals(".")) { //$NON-NLS-1$
			name = new Path(null, folders[0].getRepository().getRootDirectory()).lastSegment();
		}
		return name;
	}
	
	/*
	 * Create the page contents for a single folder checkout
	 */
	private void createSingleFolderArea(Composite composite) {
		createLabel(composite, NLS.bind(CVSUIMessages.CheckoutAsMainPage_singleFolder, new String[] { getFolderName() })); 
		configuredProjectButton = createRadioButton(composite, CVSUIMessages.CheckoutAsMainPage_asConfiguredProject, 1); 
		if (!allowProjectConfiguration) {
			configuredProjectButton.setEnabled(false);
			Label configuredLabel = createWrappingLabel(composite, CVSUIMessages.CheckoutAsMainPage_10, 5); 
			configuredLabel.setEnabled(false);
		}
		createCheckoutAsProjectRadioArea(composite);
		createCheckoutIntoRadioArea(composite);
		if (allowProjectConfiguration) {
			configuredProjectButton.setSelection(true);
		} else {
			simpleProjectButton.setSelection(true);
		}
		
		new Label(composite, SWT.NONE);
		
		// Should sub-folders of the folder be checked out?
		recurseCheck = createCheckBox(composite, CVSUIMessages.CheckoutAsProjectSelectionPage_recurse); 
		recurseCheck.addListener(SWT.Selection, event -> recurse = recurseCheck.getSelection());
		recurseCheck.setSelection(recurse);
		
		addWorkingSetSection(composite, CVSUIMessages.CheckoutAsMainPage_WorkingSetSingle);
	}

	/*
	 * Create the page contents for a multiple folder checkout
	 */
	private void createMultipleFoldersArea(Composite composite) {
		createLabel(composite, NLS.bind(CVSUIMessages.CheckoutAsMainPage_multipleFolders, new String[] { Integer.valueOf(folders.length).toString() })); 
		createCheckoutAsProjectRadioArea(composite);
		createCheckoutIntoRadioArea(composite);
		simpleProjectButton.setSelection(true);
		addWorkingSetSection(composite, CVSUIMessages.CheckoutAsMainPage_WorkingSetMultiple);
	}

	/**
	 * @param composite
	 */
	private void createCheckoutAsProjectRadioArea(Composite composite) {
		if (isSingleFolder()) {
			simpleProjectButton = createRadioButton(composite, CVSUIMessages.CheckoutAsMainPage_asSimpleProject, 1); 
			createProjectNameGroup(composite);
		} else {
			simpleProjectButton = createRadioButton(composite, CVSUIMessages.CheckoutAsMainPage_asProjects, 1); 
		}
	}
	
	/**
	 * @param composite
	 */
	private void createCheckoutIntoRadioArea(Composite composite) {
		intoProjectButton = createRadioButton(composite, CVSUIMessages.CheckoutAsMainPage_intoProject, 1); 
	}

	/**
	 * Creates the project name specification controls.
	 *
	 * @param parent the parent composite
	 */
	private void createProjectNameGroup(Composite parent) {
		// project specification group
		Composite projectGroup = new Composite(parent,SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		projectGroup.setLayout(layout);
		projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// new project label
		Label projectLabel = new Label(projectGroup,SWT.NONE);
		projectLabel.setText(CVSUIMessages.CheckoutAsMainPage_projectNameLabel); 

		// new project name entry field
		projectNameField = new Text(projectGroup, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		projectNameField.setLayoutData(data);
	
		// Set the initial value first before listener
		// to avoid handling an event during the creation.
		newProjectName = getFolderName();
		projectNameField.setText(newProjectName);
		projectNameField.selectAll();
	
		// Set the listener to capture modify events
		projectNameField.addModifyListener(e -> updateEnablements());
	}
	
	/**
	 * Check if the entries in the widget are valid. If they are return null otherwise
	 * return a string that indicates the problem.
	 */
	void updateEnablements() {
		if (projectNameField != null) {
			projectNameField.setEnabled(simpleProjectButton.getSelection());
			if (projectNameField.isEnabled()) {
				newProjectName = this.projectNameField.getText();
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IStatus nameStatus = workspace.validateName(newProjectName, IResource.PROJECT);
				setErrorMessage(nameStatus.isOK() ? null : nameStatus.getMessage());
				setPageComplete(nameStatus.isOK());
			}
		}
		if (intoProjectButton.getSelection()) {
			setErrorMessage(null);
			setPageComplete(true);
		}
	}
	
	public String getProjectName() {
		if (isSingleFolder() && simpleProjectButton.getSelection()) return newProjectName;
		return null;
	}

	public boolean isPerformConfigure() {
		if (configuredProjectButton == null) return false;
		return configuredProjectButton.getSelection();
	}

	public boolean isPerformCheckoutInto() {
		return intoProjectButton.getSelection();
	}

	public boolean isPerformCheckoutAs() {
		return simpleProjectButton.getSelection();
	}

	@Override
	protected Button createRadioButton(Composite parent, String label, int span) {
		Button radio = super.createRadioButton(parent, label, span);
		radio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateEnablements();
			}
		});
		return radio;
	}

	/**
	 * Returns the recurse.
	 * @return boolean
	 */
	public boolean isRecurse() {
		return recurse;
	}
	
	/**
	 * Returns the chosen working sets
	 * 
	 * @return an array containing the selected working sets; can be empty if no
	 *         working set has been selected
	 */
	public IWorkingSet[] getWorkingSets(){
		return workingSetGroup.getSelectedWorkingSets();
	}
	
	private void addWorkingSetSection(Composite composite, String label) {

		Composite inner = new Composite(composite, SWT.NULL);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		inner.setLayout(layout);

		setWorkingSetGroup(new WorkingSetGroup(inner, null, new String[] {
				"org.eclipse.ui.resourceWorkingSetPage", //$NON-NLS-1$
				"org.eclipse.jdt.ui.JavaWorkingSetPage" })); //$NON-NLS-1$
		updateEnablements();
	}
	
	public void setWorkingSetGroup(WorkingSetGroup workingSetGroup) {
		this.workingSetGroup = workingSetGroup;
	}
}
