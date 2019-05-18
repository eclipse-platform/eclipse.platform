/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.wizards;


import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryComparator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.*;

/**
 * First wizard page for importing a project into a CVS repository.
 * This page prompts the user to select an existing repo or create a new one.
 * If the user selected an existing repo, then getLocation() will return it.
 */
public class RepositorySelectionPage extends CVSWizardPage {
	
	private class DecoratingRepoLabelProvider extends WorkbenchLabelProvider {
		@Override
		protected String decorateText(String input, Object element) {
			//Used to process RTL locales only
			return TextProcessor.process(input, ":@/"); //$NON-NLS-1$
		}
	}
	
	
	private TableViewer table;
	private Button useExistingRepo;
	private Button useNewRepo;
	
	private ICVSRepositoryLocation result;
	
	String extendedDescription;
	
	/**
	 * RepositorySelectionPage constructor.
	 * 
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param titleImage  the image for the page
	 */
	public RepositorySelectionPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}
	protected TableViewer createTable(Composite parent, int span) {
		Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		GridData data = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
		data.horizontalSpan = span;
		data.widthHint = 200;
		table.setLayoutData(data);
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(100, true));
		table.setLayout(layout);
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
	
		return new TableViewer(table);
	}
	/**
	 * Creates the UI part of the page.
	 * 
	 * @param parent  the parent of the created widgets
	 */
	@Override
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 1, false);
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.SHARING_SELECT_REPOSITORY_PAGE);
		if (extendedDescription == null) {
			extendedDescription = CVSUIMessages.RepositorySelectionPage_description; 
		}
		createWrappingLabel(composite, extendedDescription, 0 /* indent */, 1 /* columns */); 
		
		useNewRepo = createRadioButton(composite, CVSUIMessages.RepositorySelectionPage_useNew, 1); 
		
		useExistingRepo = createRadioButton(composite, CVSUIMessages.RepositorySelectionPage_useExisting, 1); 
		useExistingRepo.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (table.getTable().getItemCount() > 0) {
					table.getTable().setFocus();
					traverseRepositories(e.character);
				}
			}
		});
		
		table = createTable(composite, 1);
		table.setContentProvider(new WorkbenchContentProvider());
		table.setLabelProvider(new DecoratingRepoLabelProvider()/*WorkbenchLabelProvider()*/);
		table.setComparator(new RepositoryComparator());
		table.addDoubleClickListener(event -> getContainer().showPage(getNextPage()));
		table.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				traverseRepositories(e.character);
			}
		});

		setControl(composite);

		initializeValues();
		Dialog.applyDialogFont(parent);
		
		table.addSelectionChangedListener(event -> {
			result = (ICVSRepositoryLocation)table.getStructuredSelection().getFirstElement();
			setPageComplete(true);
		});
		
		useExistingRepo.addListener(SWT.Selection, event -> {
			if (useNewRepo.getSelection()) {
				table.getTable().setEnabled(false);
				result = null;
			} else {
				table.getTable().setEnabled(true);
				result = (ICVSRepositoryLocation)table.getStructuredSelection().getFirstElement();
			}
			setPageComplete(true);
		});
	}
	/**
	 * Initializes states of the controls.
	 */
	private void initializeValues() {
		ICVSRepositoryLocation[] locations = CVSUIPlugin.getPlugin().getRepositoryManager().getKnownRepositoryLocations();
		AdaptableList input = new AdaptableList(locations);
		table.setInput(input);
		if (locations.length == 0) {
			useNewRepo.setSelection(true);
			useExistingRepo.setSelection(false);
			table.getTable().setEnabled(false);
		} else {
			useNewRepo.setSelection(false);
			useExistingRepo.setSelection(true);
			table.getTable().setEnabled(true);
			result = locations[0];
			table.setSelection(new StructuredSelection(result));
		}
		setPageComplete(true);
	}
	
	public ICVSRepositoryLocation getLocation() {
		return result;
	}
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			useExistingRepo.setFocus();
		}
	}
	
	public void setExtendedDescription(String extendedDescription) {
		this.extendedDescription = extendedDescription;
	}
	
	private void traverseRepositories(char c) {
		TableItem[] items = table.getTable().getItems();
		TableItem currentSelection = table.getTable().getSelection()[0];
		int currentSelectionIndex = 0;
		for (int i = 0; i < items.length; i++) {
			if (items[i].equals(currentSelection)) {
				currentSelectionIndex = i;
				break;
			}
		}
		for (int i = currentSelectionIndex + 1; i < items.length; i++) {
			if (c == items[i].getText().charAt(1)) {
				table.getTable().setSelection(i);
				return;
			}
		}
		for (int i = 0; i < currentSelectionIndex; i++) {
			if (c == items[i].getText().charAt(1)) {
				table.getTable().setSelection(i);
				return;
			}
		}
	}
}
