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

import java.text.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.ui.*;



public class ShowActivitiesDialog extends Dialog {
	private TableViewer activitiesViewer;

	// location configuration
	private IDialogSettings dialogSettings;
	private Point dialogLocation;
	private Point dialogSize;

	/**
	 * @param parentShell
	 */
	public ShowActivitiesDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.RESIZE | SWT.MIN | SWT.MAX | SWT.APPLICATION_MODAL);
		readConfiguration();
	}
	
	public void create() {
		super.create();
		// dialog location 
		if (dialogLocation != null)
			getShell().setLocation(dialogLocation);
		
		// dialog size
		if (dialogSize != null)
			getShell().setSize(dialogSize);
		else
			getShell().setSize(500,500);
		
		
		applyDialogFont(buttonBar);
		getButton(IDialogConstants.OK_ID).setFocus();
	}

	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.makeColumnsEqualWidth = false;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		container.setLayoutData(gd);
		createDescriptionSection(container);
		createActivitiesViewer(container);
		Dialog.applyDialogFont(container);
		return container;
	}

	protected Control createDescriptionSection(Composite parent){
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		container.setLayoutData(gd);
		try {
			Label targetLabel = new Label(container, SWT.NONE);
			targetLabel.setText(UpdateUI.getString("ShowActivitiesDialog.date")); //$NON-NLS-1$
			Label target = new Label(container, SWT.NONE);
			DateFormat df = DateFormat.getDateTimeInstance();
			String localizedDate = df.format(SiteManager.getLocalSite().getCurrentConfiguration().getCreationDate());
			target.setText(localizedDate);
			
			Label urlLabel = new Label(container, SWT.NONE);
			urlLabel.setText(UpdateUI.getString("ShowActivitiesDialog.loc")); //$NON-NLS-1$
			Label url = new Label(container, SWT.NONE);
			url.setText(((InstallConfiguration)SiteManager.getLocalSite().getCurrentConfiguration()).getURL().getFile());
			
			
		} catch (CoreException e) {
			UpdateUI.logException(e);
		}
		return container;
	}
	protected Control createActivitiesViewer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = gridLayout.marginWidth = 4;
		composite.setLayout(gridLayout);

		GridData gd = new GridData(GridData.FILL_BOTH);
//		gd.grabExcessHorizontalSpace = true;
//		gd.grabExcessVerticalSpace = true;

		composite.setLayoutData(gd);

		
		Label label = new Label(composite, SWT.NONE);
		label.setText(UpdateUI.getString("ShowActivitiesDialog.label")); //$NON-NLS-1$
		activitiesViewer = ActivitiesTableViewer.createViewer(composite, true);

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(8, 20, false));
		layout.addColumnData(new ColumnWeightData(50, 160, true));
		layout.addColumnData(new ColumnWeightData(50, 183, true));
		layout.addColumnData(new ColumnWeightData(50, 100, true));

		activitiesViewer.getTable().setLayout(layout);
		try {
			activitiesViewer.setInput(SiteManager.getLocalSite().getCurrentConfiguration());
		} catch (CoreException e) {
		}
		Dialog.applyDialogFont(composite);
		return composite;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		// create OK button only by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	public boolean close() {
		storeSettings();
		return super.close();
	}
	
	/**
	 * Stores the current state in the dialog settings.
	 * @since 2.0
	 */
	private void storeSettings() {
		writeConfiguration();
	}
	/**
	 * Returns the dialog settings object used to share state
	 * between several event detail dialogs.
	 * 
	 * @return the dialog settings to be used
	 */
	private IDialogSettings getDialogSettings() {
		IDialogSettings settings = UpdateUI.getDefault().getDialogSettings();
		dialogSettings = settings.getSection(getClass().getName());
		if (dialogSettings == null)
			dialogSettings= settings.addNewSection(getClass().getName());
		return dialogSettings;
	}

	/**
	 * Initializes itself from the dialog settings with the same state
	 * as at the previous invocation.
	 */
	private void readConfiguration() {
		IDialogSettings s= getDialogSettings();
		try {
			int x= s.getInt("x"); //$NON-NLS-1$
			int y= s.getInt("y"); //$NON-NLS-1$
			dialogLocation= new Point(x, y);
			
			x = s.getInt("width"); //$NON-NLS-1$
			y = s.getInt("height"); //$NON-NLS-1$
			dialogSize = new Point(x,y);
		} catch (NumberFormatException e) {
			dialogLocation= null;
			dialogSize = null;
		}
	}
	
	private void writeConfiguration(){
		IDialogSettings s = getDialogSettings();
		Point location = getShell().getLocation();
		s.put("x", location.x); //$NON-NLS-1$
		s.put("y", location.y); //$NON-NLS-1$
		
		Point size = getShell().getSize();
		s.put("width", size.x); //$NON-NLS-1$
		s.put("height", size.y); //$NON-NLS-1$
	}
}
