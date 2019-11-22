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
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.dialogs.DialogArea;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

/**
 * @author Administrator
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window&gt;Preferences&gt;Java&gt;Templates.
 * To enable and disable the creation of type comments go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation.
 */
public class WorkingSetSelectionArea extends DialogArea {

	private Button noWorkingSetButton;
	private Button workingSetButton;
	private Combo mruList;
	private Button selectButton;
	private IWorkingSet workingSet, oldWorkingSet;
	
	private String noWorkingSetText;
	private String workingSetText;
	
	private static final String USE_WORKING_SET = "UseWorkingSet"; //$NON-NLS-1$
	public static final String SELECTED_WORKING_SET = "SelectedWorkingSet"; //$NON-NLS-1$
	
	/*
	 * Used to update the mru list box when working sets are
	 * renamed in the working set selection dialog.
	 */
	private IPropertyChangeListener workingSetChangeListener = new IPropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			String property = event.getProperty();
			Object newValue = event.getNewValue();

			if (IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE.equals(property) &&
				newValue instanceof IWorkingSet) {
				String newName = ((IWorkingSet) newValue).getName();
				int count = mruList.getItemCount();
				for (int i = 0; i < count; i++) {
					String item = mruList.getItem(i);
					IWorkingSet workingSet = (IWorkingSet) mruList.getData(item);
					if (workingSet == newValue) {
						boolean isTopItem = (mruList.getData(mruList.getText()) == workingSet);
						mruList.remove(i);
						mruList.add(newName, i);
						mruList.setData(newName, workingSet);
						if (isTopItem) {
							mruList.setText(newName);
						}
						break;
					}
				}
			}
		}
	};
	private final IDialogSettings settings;
	private final Shell shell;
		
	public WorkingSetSelectionArea(Shell shell, String noWorkingSetText, String workingSetText, IDialogSettings settings) {
		this.shell = shell;
		this.noWorkingSetText = noWorkingSetText;
		this.workingSetText = workingSetText;
		this.settings = settings;
	}
	
	@Override
	public void createArea(Composite parent) {
		Dialog.applyDialogFont(parent);
		final Composite composite = createComposite(parent, 2, false);
		initializeDialogUnits(composite);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.numColumns = 2;
		composite.setLayout(layout);

		// Create the checkbox to enable/disable working set use
		noWorkingSetButton = createRadioButton(composite, noWorkingSetText, 2);
		workingSetButton = createRadioButton(composite, workingSetText, 2);
		workingSetButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleWorkingSetButtonSelection();
			}
		});
		
		boolean useWorkingSet = false;
		if (settings != null) {
			useWorkingSet = settings.getBoolean(USE_WORKING_SET);
		}
		noWorkingSetButton.setSelection(!useWorkingSet);
		workingSetButton.setSelection(useWorkingSet);

		// Create the combo/button which allows working set selection
		mruList = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, true);
		data.horizontalIndent= 15;
		mruList.setLayoutData(data);

		selectButton = createButton(composite, CVSUIMessages.WorkingSetSelectionArea_workingSetOther, GridData.HORIZONTAL_ALIGN_FILL); 
		selectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleWorkingSetSelection();
			}
		});

		initializeMru();
		initializeWorkingSet();
		
		mruList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleMruSelection();
			}
		});
	}

	/**
	 * Method handleMruSelection.
	 */
	private void handleMruSelection() {
		String selectedWorkingSet = mruList.getText();
		oldWorkingSet = workingSet;
		workingSet = (IWorkingSet) mruList.getData(selectedWorkingSet);
		if (settings != null)
			settings.put(SELECTED_WORKING_SET, selectedWorkingSet);
		handleWorkingSetChange();
	}
	
	/**
	 * Opens the working set selection dialog if the "Other..." item
	 * is selected in the most recently used working set list.
	 */
	private void handleWorkingSetSelection() {
		IWorkingSetSelectionDialog dialog = PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSetSelectionDialog(shell, false);
		IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSet workingSet = workingSetManager.getWorkingSet(mruList.getText());

		if (workingSet != null) {
			dialog.setSelection(new IWorkingSet[]{workingSet});
		}
		// add a change listener to detect a working set name change
		workingSetManager.addPropertyChangeListener(workingSetChangeListener);
		if (dialog.open() == Window.OK) {
			IWorkingSet[] result = dialog.getSelection();
			if (result != null && result.length > 0) {
				workingSet = result[0];
				String workingSetName = workingSet.getName();
				if (mruList.indexOf(workingSetName) != -1) {
					mruList.remove(workingSetName);
				}
				mruList.add(workingSetName, 0);
				mruList.setText(workingSetName);
				mruList.setData(workingSetName, workingSet);
				handleMruSelection();
			}
			else {
				workingSet = null;
			}
			// remove deleted working sets from the mru list box
			String[] mruNames = mruList.getItems();
			for (String mruName : mruNames) {
				if (workingSetManager.getWorkingSet(mruName) == null) {
					mruList.remove(mruName);
				}
			}
		}
		workingSetManager.removePropertyChangeListener(workingSetChangeListener);
	}
	
	/**
	 * Sets the enabled state of the most recently used working set list
	 * based on the checked state of the working set check box.
	 */
	private void handleWorkingSetButtonSelection() {
		boolean useWorkingSet = workingSetButton.getSelection();
		if (settings != null)
			settings.put(USE_WORKING_SET, useWorkingSet);
		mruList.setEnabled(useWorkingSet);
		selectButton.setEnabled(useWorkingSet);
		if (useWorkingSet && mruList.getSelectionIndex() >= 0) {
			handleMruSelection();
		} else if (!useWorkingSet) {
			handleDeselection();
		}
	}
	
	private void handleDeselection() {
		oldWorkingSet = workingSet;
		workingSet = null;
		handleWorkingSetChange();	
	}

	private void handleWorkingSetChange() {
		firePropertyChangeChange(SELECTED_WORKING_SET, oldWorkingSet, workingSet);
	}
	
	/**
	 * Populates the most recently used working set list with MRU items from
	 * the working set manager as well as adds an item to enable selection of
	 * a working set not in the MRU list.
	 */
	private void initializeMru() {
		IWorkingSet[] workingSets = PlatformUI.getWorkbench().getWorkingSetManager().getRecentWorkingSets();

		for (IWorkingSet w : workingSets) {
			String workingSetName = w.getName();
			mruList.add(workingSetName);
			mruList.setData(workingSetName, w);
		}
		if (workingSets.length > 0) {
			mruList.setText(workingSets[0].getName());
		}
	}
	
	/**
	 * Initializes the state of the working set part of the dialog.
	 */
	private void initializeWorkingSet() {
		if (workingSet == null && settings != null && settings.getBoolean(USE_WORKING_SET)) {
			IWorkingSet mruSet = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(settings.get(SELECTED_WORKING_SET));
			if (mruSet != null) {
				// the call to setWorkingSet will re-invoke the initializeWorkingSet method
				setWorkingSet(mruSet);
				return;
			}
		}
		workingSetButton.setSelection(workingSet != null);
		handleWorkingSetButtonSelection();
		if (workingSet != null && mruList.indexOf(workingSet.getName()) != -1) {
			mruList.setText(workingSet.getName());
		}
		handleWorkingSetChange();
	}
	
	/**
	 * Sets the working set that should be selected in the most recently
	 * used working set list.
	 *
	 * @param workingSet the working set that should be selected.
	 * 	has to exist in the list returned by
	 * 	org.eclipse.ui.IWorkingSetManager#getRecentWorkingSets().
	 * 	Must not be null.
	 */
	public void setWorkingSet(IWorkingSet workingSet) {
		oldWorkingSet = this.workingSet;
		this.workingSet = workingSet;

		if (workingSetButton != null && mruList != null) {
			initializeWorkingSet();
		}
	}
	
}
