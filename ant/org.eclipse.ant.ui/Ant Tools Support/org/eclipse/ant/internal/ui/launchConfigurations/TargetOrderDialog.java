/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.launchConfigurations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.model.AntModelContentProvider;
import org.eclipse.ant.internal.ui.model.AntModelLabelProvider;
import org.eclipse.ant.internal.ui.model.AntTargetNode;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog to specify target execution order
 */
public class TargetOrderDialog extends Dialog implements ISelectionChangedListener {

	private Button fUp;
	private Button fDown;
	private TableViewer fViewer;
	private AntTargetNode[] fTargets;

	/**
	 * Constructs the dialog.
	 */
	public TargetOrderDialog(Shell parentShell, AntTargetNode[] targets) {
		super(parentShell);
		fTargets = targets;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(AntLaunchConfigurationMessages.TargetOrderDialog_Order_Targets_1);

		Composite comp = (Composite) super.createDialogArea(parent);
		((GridLayout) comp.getLayout()).numColumns = 2;
		Label label = new Label(comp, SWT.NONE);
		label.setText(AntLaunchConfigurationMessages.TargetOrderDialog__Specify_target_execution_order__2);
		label.setFont(comp.getFont());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		createTargetList(comp);

		createButtons(comp);

		updateButtons();

		return comp;
	}

	/**
	 * Create button area &amp; buttons
	 */
	private void createButtons(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.verticalAlignment = GridData.BEGINNING;
		comp.setLayout(layout);
		comp.setLayoutData(gd);

		fUp = new Button(comp, SWT.PUSH);
		fUp.setFont(parent.getFont());
		fUp.setText(AntLaunchConfigurationMessages.TargetOrderDialog__Up_3);
		setButtonLayoutData(fUp);
		fUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleUpPressed();
			}
		});

		fDown = new Button(comp, SWT.PUSH);
		fDown.setFont(parent.getFont());
		fDown.setText(AntLaunchConfigurationMessages.TargetOrderDialog__Down_4);
		setButtonLayoutData(fDown);
		fDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleDownPressed();
			}
		});
	}

	private void handleDownPressed() {
		List<AntTargetNode> targets = getOrderedSelection();
		if (targets.isEmpty()) {
			return;
		}
		List<AntTargetNode> list = new ArrayList<>(Arrays.asList(fTargets));
		int bottom = list.size() - 1;
		int index = 0;
		for (int i = targets.size() - 1; i >= 0; i--) {
			AntTargetNode target = targets.get(i);
			index = list.indexOf(target);
			if (index < bottom) {
				bottom = index + 1;
				AntTargetNode temp = list.get(bottom);
				list.set(bottom, target);
				list.set(index, temp);
			}
			bottom = index;
		}
		setEntries(list);
	}

	private void handleUpPressed() {
		List<AntTargetNode> targets = getOrderedSelection();
		if (targets.isEmpty()) {
			return;
		}
		int top = 0;
		int index = 0;
		List<AntTargetNode> list = new ArrayList<>(Arrays.asList(fTargets));
		Iterator<AntTargetNode> entries = targets.iterator();
		while (entries.hasNext()) {
			AntTargetNode target = entries.next();
			index = list.indexOf(target);
			if (index > top) {
				top = index - 1;
				AntTargetNode temp = list.get(top);
				list.set(top, target);
				list.set(index, temp);
			}
			top = index;
		}
		setEntries(list);
	}

	/**
	 * Updates the entries to the entries in the given list
	 */
	private void setEntries(List<AntTargetNode> list) {
		fTargets = list.toArray(new AntTargetNode[list.size()]);
		fViewer.setInput(fTargets);
		// update all selection listeners
		fViewer.setSelection(fViewer.getSelection());
	}

	/**
	 * Returns the selected items in the list, in the order they are displayed (not in the order they were selected).
	 * 
	 * @return targets for an action
	 */
	private List<AntTargetNode> getOrderedSelection() {
		List<AntTargetNode> targets = new ArrayList<>();
		List<?> selection = ((IStructuredSelection) fViewer.getSelection()).toList();
		for (AntTargetNode target : fTargets) {
			if (selection.contains(target)) {
				targets.add(target);
			}
		}
		return targets;
	}

	/**
	 * Creates a list viewer for the targets
	 */
	private void createTargetList(Composite comp) {
		fViewer = new TableViewer(comp, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
		fViewer.setLabelProvider(new AntModelLabelProvider());

		fViewer.setContentProvider(new AntModelContentProvider());
		fViewer.setInput(fTargets);
		fViewer.addSelectionChangedListener(this);
		Table table = fViewer.getTable();
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 200;
		gd.widthHint = 250;
		table.setLayoutData(gd);
		table.setFont(comp.getFont());
	}

	/**
	 * Returns the ordered targets
	 */
	public Object[] getTargets() {
		return fTargets;
	}

	/**
	 * Update button enablement
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		updateButtons();
	}

	private void updateButtons() {
		int[] selections = fViewer.getTable().getSelectionIndices();
		int last = fTargets.length - 1;
		boolean up = true && selections.length > 0;
		boolean down = true && selections.length > 0;
		for (int selection : selections) {
			if (selection == 0) {
				up = false;
			}
			if (selection == last) {
				down = false;
			}
		}
		fUp.setEnabled(up);
		fDown.setEnabled(down);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IAntUIHelpContextIds.TARGET_ORDER_DIALOG);
	}
}
