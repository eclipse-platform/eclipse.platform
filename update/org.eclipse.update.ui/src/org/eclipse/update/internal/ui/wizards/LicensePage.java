/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.wizards;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.*;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.UpdateUIImages;
import org.eclipse.update.internal.ui.UpdateUIMessages;
import org.eclipse.update.operations.IInstallFeatureOperation;

public class LicensePage extends WizardPage implements IDynamicPage {
	private boolean multiLicenseMode = false;
	private IInstallFeatureOperation[] jobs;
	private IInstallFeatureOperation[] oldjJobs;
	private Text text;
	private Table table;
	private Button acceptButton;
	private Button declineButton;

	/**
	 * Constructor for LicensePage2
	 */
	public LicensePage(boolean multiLicenseMode) {
		super("License"); //$NON-NLS-1$
		setTitle(UpdateUIMessages.InstallWizard_LicensePage_title);
		setPageComplete(false);
		this.multiLicenseMode = multiLicenseMode;
		UpdateUI.getDefault().getLabelProvider().connect(this);
		setDescription(multiLicenseMode ?UpdateUIMessages.InstallWizard_LicensePage_desc2 : 
            UpdateUIMessages.InstallWizard_LicensePage_desc);
	}
	public void dispose() {
		UpdateUI.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public LicensePage(IInstallFeatureOperation job) {
		this(false);
		setJobs(new IInstallFeatureOperation[] { job });
	}

	public void setJobs(IInstallFeatureOperation[] jobs) {		
		this.jobs = jobs;		
	}

	public void createControl(Composite parent) {
		Composite client = new Composite(parent, SWT.NULL);
		client.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		client.setLayout(layout);
		
        PlatformUI.getWorkbench().getHelpSystem().setHelp(client, "org.eclipse.update.ui.LicensePage2"); //$NON-NLS-1$

		if (multiLicenseMode) {
			layout.numColumns = 3;
			layout.makeColumnsEqualWidth = true;

			table = new Table(client, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

			table.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (e.item != null) {
						Object data = e.item.getData();
						text.setText((data == null) ? "" : (String) data); //$NON-NLS-1$
					}
				}
			});
			table.setLayoutData(new GridData(GridData.FILL_BOTH));
		}
		text =
			new Text(
				client,
				SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP | SWT.READ_ONLY);

		GridData gd = new GridData(GridData.FILL_BOTH);
		if (multiLicenseMode)
			gd.horizontalSpan = 2;
		text.setLayoutData(gd);
		text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		Composite buttonContainer = new Composite(client, SWT.NULL);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		if (multiLicenseMode)
			gd.horizontalSpan = 3;
		buttonContainer.setLayout(new GridLayout());
		buttonContainer.setLayoutData(gd);

		acceptButton = new Button(buttonContainer, SWT.RADIO);
		acceptButton.setText(multiLicenseMode?UpdateUIMessages.InstallWizard_LicensePage_accept2
                : UpdateUIMessages.InstallWizard_LicensePage_accept);
		acceptButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setPageComplete(acceptButton.getSelection());
			}
		});
		declineButton = new Button(buttonContainer, SWT.RADIO);
		declineButton.setText(multiLicenseMode?UpdateUIMessages.InstallWizard_LicensePage_decline2
                : UpdateUIMessages.InstallWizard_LicensePage_decline);
		declineButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setPageComplete(acceptButton.getSelection());
			}
		});
		setControl(client);

		Dialog.applyDialogFont(parent);
	}

	public void setVisible(boolean visible) { // TO DO: Optimize out the case where a feature does not have a license?

		boolean jobsChanged = didJobsChange(jobs);
		declineButton.setSelection(!jobsChanged && declineButton.getSelection());
		acceptButton.setSelection(!jobsChanged && acceptButton.getSelection());
		
		if (jobs.length == 1) {
			acceptButton.setText(UpdateUIMessages.InstallWizard_LicensePage_accept);
			declineButton.setText(UpdateUIMessages.InstallWizard_LicensePage_decline);
		} else if (jobs.length > 1) {
			acceptButton.setText(UpdateUIMessages.InstallWizard_LicensePage_accept2);
			declineButton.setText(UpdateUIMessages.InstallWizard_LicensePage_decline2);
		}
		
		if (visible) {
			if (multiLicenseMode) {
				TableItem item;
				for (int i = 0; i < jobs.length; i++) {
					IFeature feature = jobs[i].getFeature();
					item = new TableItem(table, SWT.NONE);
					String label =
						feature.getLabel()
							+ " " //$NON-NLS-1$
							+ feature.getVersionedIdentifier().getVersion().toString();
					item.setText(label);
					item.setImage(
						UpdateUI.getDefault().getLabelProvider().get(
							feature.isPatch()
								? UpdateUIImages.DESC_EFIX_OBJ
								: UpdateUIImages.DESC_FEATURE_OBJ));
					String license = feature.getLicense().getAnnotation();
					// Question: Can this ever be null? What is the runtime cost?
					item.setData(license);
				}

				table.setSelection(0);
			}
			showLicenseText();
		} else {
			if (multiLicenseMode) {
				TableItem items[] = table.getItems();
				for (int i = items.length - 1; i >= 0; i--) {
					table.getItem(i).dispose();
				}
			}
		}
		super.setVisible(visible);
		oldjJobs = jobs;
		
	}

	private void showLicenseText() {
		if (!multiLicenseMode) {
			text.setText(jobs[0].getFeature().getLicense().getAnnotation());
			return;
		}
		TableItem[] selectedItems = table.getSelection();
		if (selectedItems.length == 0) {
			text.setText(""); //$NON-NLS-1$
		} else {
			Object data = selectedItems[0].getData();
			text.setText((data == null) ? "" : (String) data); //$NON-NLS-1$
		}
	}
	
	private boolean didJobsChange(IInstallFeatureOperation[] jobs){
		
		if ( (jobs == null) || (oldjJobs == null) || (jobs.length == 0) || (oldjJobs.length == 0) )
			return true;
				
		boolean foundIt = false;
		
		for ( int i = 0; i < jobs.length; i++) {
			foundIt = false;
			for ( int j = 0; j < oldjJobs.length; j++) {
				if (jobs[i].getFeature().getVersionedIdentifier().equals(oldjJobs[j].getFeature().getVersionedIdentifier()) ) {
					foundIt = true;
					break;
				}
			}
			if (!foundIt) {
				return true;
			}
		}
		return false;
	}
}
