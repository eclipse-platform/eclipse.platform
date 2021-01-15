/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.internal.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.osgi.framework.FrameworkUtil;

public abstract class GlobalRefreshElementSelectionPage extends WizardPage {

	private boolean scopeCheckingElement = false;

	// Set of scope hint to determine the initial selection
	private Button participantScope;
	private Button selectedResourcesScope;
	private Button workingSetScope;

	// The checked tree viewer
	private ContainerCheckedTreeViewer fViewer;

	// Working set label and holder
	private Text workingSetLabel;
	private IWorkingSet[] workingSets;
	private IDialogSettings settings;

	// dialog settings
	/**
	 * Settings constant for section name (value <code>SynchronizeResourceSelectionDialog</code>).
	 */
	private static final String STORE_SECTION = "SynchronizeResourceSelectionDialog"; //$NON-NLS-1$
	/**
	 * Settings constant for working sets (value <code>SynchronizeResourceSelectionDialog.STORE_WORKING_SET</code>).
	 */
	private static final String STORE_WORKING_SETS = "SynchronizeResourceSelectionDialog.STORE_WORKING_SETS"; //$NON-NLS-1$

	protected GlobalRefreshElementSelectionPage(String pageName) {
		super(pageName);
		IDialogSettings s = PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(GlobalRefreshElementSelectionPage.class)).getDialogSettings();
		this.settings = s.getSection(STORE_SECTION);
		if(settings == null) {
			settings = s.addNewSection(STORE_SECTION);
		}
	}

	@Override
	public void createControl(Composite parent2) {
		Composite top = new Composite(parent2, SWT.NULL);
		top.setLayout(new GridLayout());
		initializeDialogUnits(top);

		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = 50;
		top.setLayoutData(data);
		setControl(top);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.SYNC_RESOURCE_SELECTION_PAGE);

		Label l = new Label(top, SWT.NULL);
		l.setText(TeamUIMessages.GlobalRefreshResourceSelectionPage_5);

		// The viewer
		fViewer = createViewer(top);

		Composite selectGroup = new Composite(top, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		//layout.makeColumnsEqualWidth = false;
		selectGroup.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		selectGroup.setLayoutData(data);

		Button selectAll = new Button(selectGroup, SWT.NULL);
		selectAll.setText(TeamUIMessages.GlobalRefreshResourceSelectionPage_12);
		selectAll.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				participantScope.setSelection(true);
				selectedResourcesScope.setSelection(false);
				workingSetScope.setSelection(false);
				updateParticipantScope();
				scopeCheckingElement = true;
				updateOKStatus();
				scopeCheckingElement = false;
			}
		});
		setButtonLayoutData(selectAll);

		Button deSelectAll = new Button(selectGroup, SWT.NULL);
		deSelectAll.setText(TeamUIMessages.GlobalRefreshResourceSelectionPage_13);
		deSelectAll.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				fViewer.setCheckedElements(new Object[0]);
				updateOKStatus();
			}
		});
		setButtonLayoutData(deSelectAll);

		// Scopes
		Group scopeGroup = new Group(top, SWT.NULL);
		scopeGroup.setText(TeamUIMessages.GlobalRefreshResourceSelectionPage_6);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		scopeGroup.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 50;
		scopeGroup.setLayoutData(data);

		participantScope = new Button(scopeGroup, SWT.RADIO);
		participantScope.setText(TeamUIMessages.GlobalRefreshResourceSelectionPage_7);
		participantScope.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				updateParticipantScope();
			}
		});

		selectedResourcesScope = new Button(scopeGroup, SWT.RADIO);
		selectedResourcesScope.setText(TeamUIMessages.GlobalRefreshResourceSelectionPage_8);
		selectedResourcesScope.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSelectedResourcesScope();
			}
		});
		data = new GridData();
		data.horizontalSpan = 2;
		selectedResourcesScope.setLayoutData(data);

		workingSetScope = new Button(scopeGroup, SWT.RADIO);
		workingSetScope.setText(TeamUIMessages.GlobalRefreshResourceSelectionPage_10);
		workingSetScope.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (isWorkingSetSelected()) {
					updateWorkingSetScope();
				}
			}
		});

		workingSetLabel = new Text(scopeGroup, SWT.BORDER);
		workingSetLabel.setEditable(false);
		data = new GridData(GridData.FILL_HORIZONTAL);
		workingSetLabel.setLayoutData(data);

		Button selectWorkingSetButton = new Button(scopeGroup, SWT.NULL);
		selectWorkingSetButton.setText(TeamUIMessages.GlobalRefreshResourceSelectionPage_11);
		selectWorkingSetButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				selectWorkingSetAction();
			}
		});
		data = new GridData(GridData.HORIZONTAL_ALIGN_END);
		selectWorkingSetButton.setLayoutData(data);
		Dialog.applyDialogFont(selectWorkingSetButton);

		initializeScopingHint();
		Dialog.applyDialogFont(top);
	}

	protected abstract ContainerCheckedTreeViewer createViewer(Composite top);

	/**
	 * Allow the finish button to be pressed if there are checked resources.
	 *
	 */
	protected void updateOKStatus() {
		if(fViewer != null) {
			if(! scopeCheckingElement) {
				if(! selectedResourcesScope.getSelection()) {
					selectedResourcesScope.setSelection(true);
					participantScope.setSelection(false);
					workingSetScope.setSelection(false);
					updateSelectedResourcesScope();
				}
			}
			setPageComplete(areAnyElementsChecked());
		} else {
			setPageComplete(false);
		}
	}

	/**
	 * Returns <code>true</code> if any of the root resources are grayed.
	 */
	private boolean areAnyElementsChecked() {
		TreeItem[] item = fViewer.getTree().getItems();
		for (TreeItem child : item) {
			if(child.getChecked() || child.getGrayed()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the list of top-most resources that have been checked.
	 *
	 * @return  the list of top-most resources that have been checked or an
	 * empty list if nothing is selected.
	 */
	public Object[] getRootElement() {
		TreeItem[] item = fViewer.getTree().getItems();
		List checked = new ArrayList();
		for (TreeItem child : item) {
			collectCheckedItems(child, checked);
		}
		return checked.toArray(new Object[checked.size()]);
	}

	protected void initializeScopingHint() {
		String working_sets = settings.get(STORE_WORKING_SETS);
		if (working_sets == null || working_sets.isEmpty()) {
			participantScope.setSelection(true);
			updateParticipantScope();
		} else {
			StringTokenizer st = new StringTokenizer(working_sets, " ,"); //$NON-NLS-1$
			ArrayList<IWorkingSet> ws = new ArrayList<>();
			while (st.hasMoreTokens()) {
				String workingSetName = st.nextToken();
				if (workingSetName != null && !workingSetName.isEmpty()) {
					IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
					IWorkingSet workingSet = workingSetManager.getWorkingSet(workingSetName);
					if (workingSet != null) {
						ws.add(workingSet);
					}
				}
			}
			if(! ws.isEmpty()) {
				this.workingSets = ws.toArray(new IWorkingSet[ws.size()]);
				updateWorkingSetScope();
				updateWorkingSetLabel();
				participantScope.setSelection(false);
				selectedResourcesScope.setSelection(false);
				workingSetScope.setSelection(true);
			}
		}
	}

	@Override
	public void dispose() {
		if(workingSets != null && isWorkingSetSelected()) {
			String concatsWorkingSets = makeWorkingSetLabel();
			settings.put(STORE_WORKING_SETS, concatsWorkingSets);
		} else {
			settings.put(STORE_WORKING_SETS, (String)null);
		}
	}

	private void updateParticipantScope() {
		if(isWorkspaceSelected()) {
			scopeCheckingElement = true;
			checkAll();
			setPageComplete(getRootElement().length > 0);
			scopeCheckingElement = false;
		}
	}

	protected abstract void checkAll();

	private void updateSelectedResourcesScope() {
		setPageComplete(getRootElement().length > 0);
	}

	private void selectWorkingSetAction() {
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = manager.createWorkingSetSelectionDialog(getShell(), true);
		dialog.open();
		IWorkingSet[] sets = dialog.getSelection();
		if(sets != null) {
			workingSets = sets;
		} else {
			// dialog cancelled
			return;
		}
		updateWorkingSetScope();
		updateWorkingSetLabel();

		participantScope.setSelection(false);
		selectedResourcesScope.setSelection(false);
		workingSetScope.setSelection(true);
	}

	private void updateWorkingSetScope() {
		if(workingSets != null) {
			scopeCheckingElement = true;
			boolean hasElements = checkWorkingSetElements();
			scopeCheckingElement = false;
			setPageComplete(hasElements);
		} else {
			scopeCheckingElement = true;
			fViewer.setCheckedElements(new Object[0]);
			scopeCheckingElement = false;
			setPageComplete(false);
		}
	}

	protected abstract boolean checkWorkingSetElements();

	private void collectCheckedItems(TreeItem item, List checked) {
		if(item.getChecked() && !item.getGrayed()) {
			checked.add(item.getData());
		} else if(item.getGrayed()) {
			TreeItem[] children = item.getItems();
			for (TreeItem child : children) {
				collectCheckedItems(child, checked);
			}
		}
	}

	private void updateWorkingSetLabel() {
		if (workingSets == null || workingSets.length == 0) {
			workingSetLabel.setText(TeamUIMessages.StatisticsPanel_noWorkingSet);
		} else {
			workingSetLabel.setText(makeWorkingSetLabel());
		}
	}

	private String makeWorkingSetLabel() {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < workingSets.length; i++) {
			IWorkingSet set = workingSets[i];
			if (i != 0)
				buffer.append(", "); //$NON-NLS-1$
			buffer.append(set.getLabel());
		}
		return buffer.toString();
	}

	protected boolean isWorkspaceSelected() {
		return participantScope.getSelection();
	}

	protected void setWorkspaceSelected(boolean selected) {
		workingSetScope.setSelection(!selected);
		selectedResourcesScope.setSelection(!selected);
		participantScope.setSelection(selected);
	}

	protected boolean isWorkingSetSelected() {
		return workingSetScope.getSelection();
	}

	public IWorkingSet[] getWorkingSets() {
		return workingSets;
	}

	public ContainerCheckedTreeViewer getViewer() {
		return fViewer;
	}

	protected boolean isSelectedResourcesSelected() {
		return selectedResourcesScope.getSelection();
	}
}
