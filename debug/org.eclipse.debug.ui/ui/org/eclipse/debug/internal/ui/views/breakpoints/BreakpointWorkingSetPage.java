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
package org.eclipse.debug.internal.ui.views.breakpoints;

import java.util.ArrayList;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.importexport.breakpoints.EmbeddedBreakpointsViewer;
import org.eclipse.debug.internal.ui.views.DebugUIViewsMessages;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetPage;

/**
 * The Breakpoint working set page allows the user to create
 * and edit a Breakpoint working set.
 *
 * @since 3.1
 */
public class BreakpointWorkingSetPage extends WizardPage implements IWorkingSetPage {

	final private static String PAGE_TITLE= DebugUIViewsMessages.BreakpointWorkingSetPage_0;
	final private static String PAGE_ID= "breakpointWorkingSetPage"; //$NON-NLS-1$

	private Text fWorkingSetName;
	private EmbeddedBreakpointsViewer fTViewer;
	private boolean fFirstCheck;
	private IWorkingSet fWorkingSet;

	/**
	 * Default constructor.
	 */
	public BreakpointWorkingSetPage() {
		super(PAGE_ID, PAGE_TITLE, DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_WIZBAN_DEBUG));
		setDescription(DebugUIViewsMessages.BreakpointWorkingSetPage_1);
		fFirstCheck= true;
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		setControl(composite);
		Label label= new Label(composite, SWT.WRAP);
		label.setText(DebugUIViewsMessages.BreakpointWorkingSetPage_2);
		GridData gd= new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(gd);
		fWorkingSetName= new Text(composite, SWT.SINGLE | SWT.BORDER);
		fWorkingSetName.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		fWorkingSetName.addModifyListener(e -> validateInput());
		fWorkingSetName.setFocus();
		label= new Label(composite, SWT.WRAP);
		label.setText(DebugUIViewsMessages.BreakpointWorkingSetPage_3);
		gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(gd);
		IViewPart viewpart = DebugUIPlugin.getActiveWorkbenchWindow().getActivePage().findView(IDebugUIConstants.ID_BREAKPOINT_VIEW);
		IStructuredSelection selection;
		if (viewpart == null) {
			selection = new StructuredSelection();
		} else {
			selection = (IStructuredSelection)viewpart.getViewSite().getSelectionProvider().getSelection();
		}
		fTViewer = new EmbeddedBreakpointsViewer(composite, DebugPlugin.getDefault().getBreakpointManager(), selection);
		// Add select / deselect all buttons for bug 46669
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(2, false));
		buttonComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		Button selectAllButton = SWTFactory.createPushButton(buttonComposite, DebugUIViewsMessages.BreakpointWorkingSetPage_selectAll_label, null);
		selectAllButton.setToolTipText(DebugUIViewsMessages.BreakpointWorkingSetPage_selectAll_toolTip);
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent selectionEvent) {
				BreakpointsViewer viewer = fTViewer.getViewer();
				viewer.getTree().selectAll();
				viewer.setCheckedElements(viewer.getStructuredSelection().toArray());
				viewer.setGrayedElements(new Object[] {});
				viewer.getTree().deselectAll();
				validateInput();
			}
		});
		Button deselectAllButton = SWTFactory.createPushButton(buttonComposite, DebugUIViewsMessages.BreakpointWorkingSetPage_deselectAll_label, null);
		deselectAllButton.setToolTipText(DebugUIViewsMessages.BreakpointWorkingSetPage_deselectAll_toolTip);
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent selectionEvent) {
				BreakpointsViewer viewer = fTViewer.getViewer();
				viewer.setCheckedElements(new Object[] {});
				validateInput();
			}
		});
		if (fWorkingSet != null) {
			fWorkingSetName.setText(fWorkingSet.getName());
		}
		validateInput();
		Dialog.applyDialogFont(composite);
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	@Override
	public IWorkingSet getSelection() {
		return fWorkingSet;
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	@Override
	public void setSelection(IWorkingSet workingSet) {
		Assert.isNotNull(workingSet, "Working set must not be null"); //$NON-NLS-1$
		fWorkingSet= workingSet;
		if (getContainer() != null && getShell() != null && fWorkingSetName != null) {
			fFirstCheck= false;
			fWorkingSetName.setText(fWorkingSet.getName());
			validateInput();
		}
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	@Override
	public void finish() {
		String workingSetName = fWorkingSetName.getText();
		ArrayList<IBreakpoint> elements = new ArrayList<>();
		//weed out non-breakpoint elements since 3.2
		for (Object adaptableElement : fTViewer.getCheckedElements()) {
			IBreakpoint breakpoint = (IBreakpoint)DebugPlugin.getAdapter(adaptableElement, IBreakpoint.class);
			if(breakpoint != null) {
				elements.add(breakpoint);
			}//end if
		}//end for
		if (fWorkingSet == null) {
			IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
			fWorkingSet = workingSetManager.createWorkingSet(workingSetName, elements.toArray(new IAdaptable[elements.size()]));
		} else {
			fWorkingSet.setName(workingSetName);
			fWorkingSet.setElements(elements.toArray(new IAdaptable[elements.size()]));
		}
	}

	/**
	 * validates the current input of the page to determine if the finish button can be enabled
	 */
	private void validateInput() {
		String errorMessage= null;
		String newText= fWorkingSetName.getText();

		if (!newText.equals(newText.trim())) {
			errorMessage = DebugUIViewsMessages.BreakpointWorkingSetPage_4;
		}
		if (newText.equals(IInternalDebugCoreConstants.EMPTY_STRING)) {
			if (fFirstCheck) {
				setPageComplete(false);
				fFirstCheck= false;
				return;
			}
			errorMessage= DebugUIViewsMessages.BreakpointWorkingSetPage_5;
		}
		fFirstCheck= false;
		if (errorMessage == null && (fWorkingSet == null || !newText.equals(fWorkingSet.getName()))) {
			for (IWorkingSet workingSet : PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets()) {
				if (newText.equals(workingSet.getName())) {
					errorMessage= DebugUIViewsMessages.BreakpointWorkingSetPage_6;
				}
			}
		}
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}

}
