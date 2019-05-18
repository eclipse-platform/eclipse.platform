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
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.dialogs.DetailsDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * This dialog allows the user to add a set of resources to version control.
 * They can either all be added or the user can choose which to add from a
 * details list.
 */
public class AddToVersionControlDialog extends DetailsDialog {

	private static final int WIDTH_HINT = 350;
	private final static int SELECTION_HEIGHT_HINT = 100;
	
	private IResource[] unaddedResources;
	private Object[] resourcesToAdd;
	
	private CheckboxTableViewer listViewer;
	/**
	 * Constructor for AddToVersionControlDialog.
	 * @param parentShell
	 */
	public AddToVersionControlDialog(Shell parentShell, IResource[] unaddedResources) {
		super(parentShell, CVSUIMessages.AddToVersionControlDialog_title); 
		this.unaddedResources = unaddedResources;
	}

	@Override
	protected void createMainDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
			
		// add a description label
		if (unaddedResources.length==1) {
			createWrappingLabel(composite, NLS.bind(CVSUIMessages.AddToVersionControlDialog_thereIsAnUnaddedResource, new String[] { Integer.valueOf(unaddedResources.length).toString() }));  
		} else {
			createWrappingLabel(composite, NLS.bind(CVSUIMessages.AddToVersionControlDialog_thereAreUnaddedResources, new String[] { Integer.valueOf(unaddedResources.length).toString() }));  
		}
	}

	@Override
	protected String getHelpContextId() {
		return IHelpContextIds.ADD_TO_VERSION_CONTROL_DIALOG;
	}

	@Override
	protected Composite createDropDownDialogArea(Composite parent) {
		// create a composite with standard margins and spacing
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		addUnaddedResourcesArea(composite);
		
		return composite;
	}

	private void addUnaddedResourcesArea(Composite composite) {
		
		// add a description label
		createWrappingLabel(composite, CVSUIMessages.ReleaseCommentDialog_unaddedResources); 
	
		// add the selectable checkbox list
		listViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = SELECTION_HEIGHT_HINT;
		data.widthHint = WIDTH_HINT;
		listViewer.getTable().setLayoutData(data);

		// set the contents of the list
		listViewer.setLabelProvider(new WorkbenchLabelProvider() {
			@Override
			protected String decorateText(String input, Object element) {
				if (element instanceof IResource)
					return ((IResource)element).getFullPath().toString();
				else
					return input;
			}
		});
		listViewer.setContentProvider(new WorkbenchContentProvider());
		listViewer.setInput(new AdaptableResourceList(unaddedResources));
		if (resourcesToAdd == null) {
			listViewer.setAllChecked(true);
		} else {
			listViewer.setCheckedElements(resourcesToAdd);
		}
		listViewer.addSelectionChangedListener(event -> resourcesToAdd = listViewer.getCheckedElements());
		
		addSelectionButtons(composite);
	}
	
	/**
	 * Add the selection and deselection buttons to the dialog.
	 * @param composite org.eclipse.swt.widgets.Composite
	 */
	private void addSelectionButtons(Composite composite) {
	
		Composite buttonComposite = new Composite(composite, SWT.RIGHT);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		buttonComposite.setLayout(layout);
		GridData data =
			new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		composite.setData(data);
	
		Button selectButton = createButton(buttonComposite, IDialogConstants.SELECT_ALL_ID, CVSUIMessages.ReleaseCommentDialog_selectAll, false); 
		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(true);
				resourcesToAdd = null;
			}
		};
		selectButton.addSelectionListener(listener);
	
		Button deselectButton = createButton(buttonComposite, IDialogConstants.DESELECT_ALL_ID, CVSUIMessages.ReleaseCommentDialog_deselectAll, false); 
		listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(false);
				resourcesToAdd = new Object[0];
	
			}
		};
		deselectButton.addSelectionListener(listener);
	}
	
	@Override
	protected void updateEnablements() {
	}
	
	/**
	 * Returns the resourcesToAdd.
	 * @return IResource[]
	 */
	public IResource[] getResourcesToAdd() {
		if (resourcesToAdd == null) {
			return unaddedResources;
		} else {
			List result = Arrays.asList(resourcesToAdd);
			return (IResource[]) result.toArray(new IResource[result.size()]);
		}
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.YES_ID, IDialogConstants.YES_LABEL, true);
		createButton(parent, IDialogConstants.NO_ID, IDialogConstants.NO_LABEL, true);
		super.createButtonsForButtonBar(parent);
	}
	
	@Override
	protected boolean includeOkButton() {
		return false;
	}

	@Override
	protected void buttonPressed(int id) {
		// hijack yes and no buttons to set the correct return
		// codes.
		if(id == IDialogConstants.YES_ID || id == IDialogConstants.NO_ID) {
			setReturnCode(id);
			close();
		} else {
			super.buttonPressed(id);
		}
	}
}
