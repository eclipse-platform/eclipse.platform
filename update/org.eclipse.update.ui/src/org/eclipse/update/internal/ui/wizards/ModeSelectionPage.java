/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.wizards;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;
import org.eclipse.update.internal.operations.UpdateUtils;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.search.*;

/**
 */
public class ModeSelectionPage extends BannerPage implements ISearchProvider {
	private boolean updateMode=true;
	private Button updatesButton;
	private Button newFeaturesButton;
	private UpdateSearchRequest searchRequest;
	private SearchRunner searchRunner;
	private static final String SECTION_ID = "ModeSelectionPage"; //$NON-NLS-1$
	private static final String P_NEW_FEATURES_MODE = "new-features-mode"; //$NON-NLS-1$
	
	public ModeSelectionPage(SearchRunner searchRunner) {
		super("modeSelection"); //$NON-NLS-1$
		setTitle(UpdateUI.getString("ModeSelectionPage.title")); //$NON-NLS-1$
		setDescription(UpdateUI.getString("ModeSelectionPage.desc")); //$NON-NLS-1$
		this.searchRunner = searchRunner;
	}
	
	public UpdateSearchRequest getSearchRequest() {
		initializeSearch();
		return searchRequest;
	}
	
	private IDialogSettings getSettings() {
		IDialogSettings master = UpdateUI.getDefault().getDialogSettings();
		IDialogSettings section = master.getSection(SECTION_ID);
		if (section==null)
			section = master.addNewSection(SECTION_ID);
		return section;
	}

	private void initializeSearch() {
		if (searchRequest!=null) return;
		searchRequest = UpdateUtils.createNewUpdatesRequest(null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.update.internal.ui.wizards.BannerPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		updatesButton = new Button(composite, SWT.RADIO);
		updatesButton.setText(UpdateUI.getString("ModeSelectionPage.updates")); //$NON-NLS-1$
		boolean newFeaturesMode = getSettings().getBoolean(P_NEW_FEATURES_MODE);
		updatesButton.setSelection(!newFeaturesMode);
		updatesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				switchMode();
			}
		});
		newFeaturesButton = new Button(composite, SWT.RADIO);
		newFeaturesButton.setSelection(newFeaturesMode);
		newFeaturesButton.setText(UpdateUI.getString("ModeSelectionPage.newFeatures")); //$NON-NLS-1$
		newFeaturesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				switchMode();
			}
		});		
		switchMode();
		
		Dialog.applyDialogFont(parent);
		
		WorkbenchHelp.setHelp(composite, "org.eclipse.update.ui.ModeSelectionPage"); //$NON-NLS-1$

		return composite;
	}
	
	public void saveSettings() {
		boolean updateMode = updatesButton.getSelection();
		getSettings().put(P_NEW_FEATURES_MODE, !updateMode);
	}
	
	private void switchMode() {
		updateMode = updatesButton.getSelection();
		if (updateMode)
			searchRunner.setSearchProvider(this);
	}
	
	public boolean isUpdateMode() {
		return updateMode;
	}
}
