/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.views;

import java.util.ArrayList;

import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class ScopePreferenceDialog extends PreferenceDialog {
	private final EngineDescriptorManager descManager;
	private ArrayList<PendingOperation> pendingOperations;

	static class PendingOperation {
		int action;
		EngineDescriptor desc;
		public PendingOperation(int action, EngineDescriptor desc) {
			this.action = action;
			this.desc = desc;
		}
	}
	/**
	 * The Add button id.
	 */
	private final static int NEW_ID = IDialogConstants.CLIENT_ID + 1;

	/**
	 * The Remove button id.
	 */
	private final static int DELETE_ID = IDialogConstants.CLIENT_ID + 2;

	public ScopePreferenceDialog(Shell parentShell, PreferenceManager manager, EngineDescriptorManager descManager, boolean editable) {
		super(parentShell, manager);
		this.descManager = descManager;
	}

	@Override
	protected Control createTreeAreaContents(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(layout);
		Control treeControl = super.createTreeAreaContents(container);
		GridData treeGd = (GridData)treeControl.getLayoutData();
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		treeControl.setLayoutData(gd);

		Button lbutton = createButton(container, NEW_ID, Messages.ScopePreferenceDialog_new, false);
		gd = (GridData)lbutton.getLayoutData();
		gd.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
		Button rbutton = createButton(container, DELETE_ID, Messages.ScopePreferenceDialog_delete, false);
		rbutton.setEnabled(false);
		gd = (GridData)rbutton.getLayoutData();
		gd.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
		layout.numColumns = 2;
		container.setLayoutData(treeGd);
		Point size = container.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		treeGd.widthHint = Math.max(treeGd.widthHint, size.x);
		return container;
	}

	@Override
	protected TreeViewer createTreeViewer(Composite parent) {
		TreeViewer viewer = super.createTreeViewer(parent);
		viewer.addSelectionChangedListener(event -> {
			IStructuredSelection ssel = event.getStructuredSelection();
			Object obj = ssel.getFirstElement();
			treeSelectionChanged(obj);
		});
		return viewer;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case NEW_ID:
			doNew();
			break;
		case DELETE_ID:
			doDelete();
			break;
		default:
			super.buttonPressed(buttonId);
		}
	}

	private void treeSelectionChanged(Object obj) {
		boolean removable = false;
		if (obj instanceof ScopePreferenceManager.EnginePreferenceNode) {
			ScopePreferenceManager.EnginePreferenceNode node = (ScopePreferenceManager.EnginePreferenceNode)obj;
			EngineDescriptor desc = node.getDescriptor();
			removable = desc.isUserDefined();
		}
		getButton(DELETE_ID).setEnabled(removable);
	}

	private void doNew() {
		NewEngineWizard wizard = new NewEngineWizard(descManager.getEngineTypes());
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.create();
		dialog.getShell().setSize(400, 500);
		if (dialog.open()==Window.OK) {
			EngineTypeDescriptor etdesc = wizard.getSelectedEngineType();
			EngineDescriptor desc = new EngineDescriptor(descManager);
			desc.setEngineType(etdesc);
			desc.setUserDefined(true);
			desc.setId(descManager.computeNewId(etdesc.getId()));
			ScopePreferenceManager mng = (ScopePreferenceManager)getPreferenceManager();
			IPreferenceNode node = mng.addNode(desc);
			getTreeViewer().refresh();
			getTreeViewer().setSelection(new StructuredSelection(node));
			scheduleOperation(NEW_ID, desc);
		}
	}

	private void doDelete() {
		Object obj = getTreeViewer().getStructuredSelection().getFirstElement();
		if (obj instanceof ScopePreferenceManager.EnginePreferenceNode) {
			ScopePreferenceManager.EnginePreferenceNode node = (ScopePreferenceManager.EnginePreferenceNode)obj;
			EngineDescriptor desc = node.getDescriptor();
			//ScopePreferenceManager mng = (ScopePreferenceManager)getPreferenceManager();
			getTreeViewer().remove(node);
			scheduleOperation(DELETE_ID, desc);
		}
	}

	private void scheduleOperation(int action, EngineDescriptor desc) {
		if (pendingOperations==null)
			pendingOperations = new ArrayList<>();
		pendingOperations.add(new PendingOperation(action, desc));
	}

	@Override
	protected void okPressed() {
		super.okPressed();
		if (pendingOperations!=null) {
			// process pending operations
			for (int i=0; i<pendingOperations.size(); i++) {
				PendingOperation op = pendingOperations.get(i);
				if (op.action==NEW_ID)
					descManager.add(op.desc);
				else
					descManager.remove(op.desc);
			}
			pendingOperations = null;
		}
		descManager.save();
	}
}