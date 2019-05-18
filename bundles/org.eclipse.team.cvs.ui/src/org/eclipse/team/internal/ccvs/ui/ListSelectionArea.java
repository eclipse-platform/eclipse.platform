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

import java.util.*;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ui.dialogs.DialogArea;

/**
 * Reusable area that provides a list to select from and a select all and
 * deselect all button.
 */
public class ListSelectionArea extends DialogArea {
	private Object inputElement;
	private IStructuredContentProvider contentProvider;
	private ILabelProvider labelProvider;
	private String message;
	private List initialSelections;

	// the visual selection widget group
	private CheckboxTableViewer listViewer;
	
	private Object[] previousCheckedElements;

	public static final String LIST_SELECTION = "ListSelection"; //$NON-NLS-1$
	
	/**
	 * Constructor for ListSelectionArea.
	 * @param parentDialog
	 * @param settings
	 */
	public ListSelectionArea(
			Object input,
			IStructuredContentProvider contentProvider,
			ILabelProvider labelProvider,
			String message) {
		this.inputElement = input;
		this.contentProvider = contentProvider;
		this.labelProvider = labelProvider;
		this.message = message;
		this.initialSelections = new ArrayList();
	}

	@Override
	public void createArea(Composite parent) {

		Dialog.applyDialogFont(parent);

		final Composite composite = createComposite(parent, 1, true);
		
		initializeDialogUnits(composite);

		if (message != null)
			createWrappingLabel(composite, message, 1);

		listViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 0; // It will expand to the size of the wizard page!
		data.widthHint = 0;
		listViewer.getTable().setLayoutData(data);

		listViewer.setLabelProvider(labelProvider);
		listViewer.setContentProvider(contentProvider);
		
		listViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object[] checkedElements = getViewer().getCheckedElements();
				firePropertyChangeChange(LIST_SELECTION, previousCheckedElements, checkedElements);
				previousCheckedElements = checkedElements;
			}
		});

		addSelectionButtons(composite);

		initializeViewer();

		// initialize page
		if (!getInitialElementSelections().isEmpty())
			checkInitialSelections();
	}
	
	/**
	 * Initializes this dialog's viewer after it has been laid out.
	 */
	private void initializeViewer() {
		listViewer.setInput(inputElement);
	}
	
	/**
	 * Visually checks the previously-specified elements in this dialog's list
	 * viewer.
	 */
	private void checkInitialSelections() {
		Iterator itemsToCheck = getInitialElementSelections().iterator();

		while (itemsToCheck.hasNext())
			listViewer.setChecked(itemsToCheck.next(),true);
	}
	
	/**
	 * Add the selection and deselection buttons to the dialog.
	 * @param composite org.eclipse.swt.widgets.Composite
	 */
	private void addSelectionButtons(Composite composite) {
		Composite buttonComposite = new Composite(composite, SWT.RIGHT);
		buttonComposite.setLayout(new GridLayout(2, false));
		buttonComposite.setData(new GridData(SWT.END, SWT.BEGINNING, true, false));

		Button selectButton = createButton(buttonComposite, CVSUIMessages.ListSelectionArea_selectAll, GridData.HORIZONTAL_ALIGN_FILL); 

		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(true);
			}
		};
		selectButton.addSelectionListener(listener);


		Button deselectButton = createButton(buttonComposite, CVSUIMessages.ListSelectionArea_deselectAll, GridData.HORIZONTAL_ALIGN_FILL); 

		listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(false);

			}
		};
		deselectButton.addSelectionListener(listener);
	}
	
	/**
	 * Returns the list of initial element selections.
	 * @return List
	 */
	protected List getInitialElementSelections(){
		return initialSelections;
	}
	
	/**
	 * Returns the listViewer.
	 * @return CheckboxTableViewer
	 */
	public CheckboxTableViewer getViewer() {
		return listViewer;
	}

}
