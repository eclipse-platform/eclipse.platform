/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.tags;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.wizards.CVSWizardPage;
import org.eclipse.team.internal.ui.PixelConverter;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.dialogs.DetailsDialog;

public class BranchPromptDialog extends DetailsDialog {

	private String branchTag = ""; //$NON-NLS-1$
	private String versionTag= ""; //$NON-NLS-1$
	private String versionName= "";		 //$NON-NLS-1$

	private boolean allStickyResources;	
	private boolean update;
		
	private Text versionText;
	private Text branchText;
	
	private static final int TAG_AREA_HEIGHT_HINT = 200;
	
	// widgets;
	private TagSource tagSource;
	private TagSelectionArea tagArea;
	private final IResource[] resources;
	
	public BranchPromptDialog(Shell parentShell, String title, IResource[] resources, boolean allResourcesSticky, String versionName) {
		super(parentShell, title);
		this.resources = resources;
		this.tagSource = TagSource.create(resources);
		this.allStickyResources = allResourcesSticky;
		this.versionName = versionName;
	}	

	@Override
	protected void createMainDialogArea(Composite composite) {
		
		applyDialogFont(composite);
		initializeDialogUnits(composite);
		
		final int areaWidth= convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		
		final Label description= SWTUtils.createLabel(composite, allStickyResources ? CVSUIMessages.BranchWizardPage_pageDescriptionVersion : CVSUIMessages.BranchWizardPage_pageDescription);  
		description.setLayoutData(SWTUtils.createGridData(areaWidth, SWT.DEFAULT, true, false));
		
		final Label name= SWTUtils.createLabel(composite, CVSUIMessages.BranchWizardPage_branchName); 
		name.setLayoutData(SWTUtils.createGridData(areaWidth, SWT.DEFAULT, true, false));
		
		branchText = CVSWizardPage.createTextField(composite);
		branchText.addListener(SWT.Modify, event -> {
			branchTag = branchText.getText();
			updateEnablements();
			updateVersionName(branchTag);
		});
		addBranchContentAssist();

		final Button check = SWTUtils.createCheckBox(composite, CVSUIMessages.BranchWizardPage_startWorking); 
		check.addListener(SWT.Selection, event -> update = check.getSelection());
		check.setSelection(true);		
		update = true;
		
		final Label versionLabel1= SWTUtils.createLabel(composite, CVSUIMessages.BranchWizardPage_specifyVersion); 
		versionLabel1.setLayoutData(SWTUtils.createGridData(areaWidth, SWT.DEFAULT, true, false));

		final Label versionLabel2= SWTUtils.createLabel(composite, CVSUIMessages.BranchWizardPage_versionName); 
		versionLabel2.setLayoutData(SWTUtils.createGridData(areaWidth, SWT.DEFAULT, true, false));
		
		versionText = CVSWizardPage.createTextField(composite);
		versionText.addListener(SWT.Modify, event -> {
			versionTag = versionText.getText();
			updateEnablements();
		});
		
		if(allStickyResources) {
			versionText.setEditable(false);
			versionText.setText(versionName);
		}

		applyDialogFont(composite);
		branchText.setFocus();
	}

	@Override
	protected String getHelpContextId() {
		return IHelpContextIds.BRANCH_DIALOG;
	}
	private void addBranchContentAssist() {
		TagSource projectTagSource = LocalProjectTagSource.create(getSeedProject());
		if (projectTagSource != null)
			TagContentAssistProcessor.createContentAssistant(branchText, projectTagSource, TagSelectionArea.INCLUDE_BRANCHES); 
	}

	private IProject getSeedProject() {
		return resources[0].getProject();
	}

	/**
	 * Updates version name
	 */
	protected void updateVersionName(String branchName) {
		if(versionText!=null && !allStickyResources) {
			versionText.setText(CVSUIMessages.BranchWizardPage_versionPrefix + branchName); 
		}
	}
	
	@Override
	protected Composite createDropDownDialogArea(Composite parent) {
		
		applyDialogFont(parent);
		final PixelConverter converter= new PixelConverter(parent);
		
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(SWTUtils.createGridLayout(1, converter, SWTUtils.MARGINS_DIALOG));
		final GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = TAG_AREA_HEIGHT_HINT;
		composite.setLayoutData(gridData);
		
		tagArea = new TagSelectionArea(getShell(), tagSource, TagSelectionArea.INCLUDE_VERSIONS | TagSelectionArea.INCLUDE_BRANCHES, null);
		tagArea.setTagAreaLabel(CVSUIMessages.BranchWizardPage_existingVersionsAndBranches); 
		tagArea.setIncludeFilterInputArea(false);
		tagArea.createArea(composite);

		return composite;
	}
	
	/**
	 * Validates branch and version names
	 */
	@Override
	protected void updateEnablements() {
		String message = null;
		
		if (branchTag.length() == 0) {
			message = ""; //$NON-NLS-1$
		} else {
			IStatus status = CVSTag.validateTagName(branchTag);
			if (!status.isOK()) {
				message = NLS.bind(CVSUIMessages.BranchWizard_branchNameWarning, new String[] { status.getMessage() }); 
			} else {
				if(versionText!=null) {
					status = CVSTag.validateTagName(versionText.getText());
					if (!status.isOK()) {
						message = NLS.bind(CVSUIMessages.BranchWizard_versionNameWarning, new String[] { status.getMessage() }); 
					} else {
						if(versionTag.length() != 0 && versionTag.equals(branchTag)) {
							message = CVSUIMessages.BranchWizard_branchAndVersionMustBeDifferent; 
						}
					}
				}
			}
		}
		setPageComplete(message == null);
		setErrorMessage(message);
	}
	
	/**
	 * Returns the branch tag name
	 */
	public String getBranchTagName() {
		return branchTag;
	}
	
	/**
	 * Returns the version tag name
	 */
	public String getVersionTagName() {
		return versionTag;
	}
	
	/**
	 * Returns the state of the update checkbox
	 */
	public boolean getUpdate() {
		return update;
	}
	
	@Override
	protected boolean isMainGrabVertical() {
		return false;
	}

}
