/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.memory;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * A dialog that presents a tree of memory tree elements
 * for the user to select from.
 */
public class MemoryTreeSelectionDialog extends SelectionDialog {

	/**
	 * Button id for the button that creates a new file
	 */
	private static final int CREATE_FILE_ID = 16;
	/**
	 * Button id for the button that creates a new folder
	 */
	private static final int CREATE_FOLDER_ID = 17;

	private Text nameField;
	TreeViewer tree;

	protected MemoryTreeSelectionDialog(Shell parentShell) {
		super(parentShell);
	}

	protected void buttonPressed(int buttonId) {
		if (buttonId == CREATE_FILE_ID || buttonId == CREATE_FOLDER_ID) {
			try {
				IFileStore parent = getSelectedFileStore();
				IFileStore toCreate;
				if (parent != null)
					toCreate = parent.getChild(nameField.getText());
				else
					toCreate = new MemoryFileStore(Path.ROOT.append(nameField.getText()));
				if (buttonId == CREATE_FILE_ID) {
					toCreate.openOutputStream(EFS.NONE, null).close();
				} else {
					toCreate.mkdir(EFS.NONE, null);
				}
				tree.refresh();
			} catch (Exception e) {
				IStatus status = Policy.createStatus(e);
				ErrorDialog.openError(getShell(), null, null, status);
				Policy.log(status);
			}
		} else {
			super.buttonPressed(buttonId);
		}
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Select an element from the in-memory file system");
	}

	protected Control createDialogArea(Composite parent) {
		Composite top = (Composite) super.createDialogArea(parent);

		tree = new TreeViewer(top, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 300;
		tree.getControl().setLayoutData(data);
		tree.setContentProvider(new WorkbenchContentProvider());
		tree.setLabelProvider(new WorkbenchLabelProvider());
		tree.setInput(new MemoryFileStore(Path.ROOT));

		createNewElementArea(parent);

		return top;
	}

	/**
	 * Creates the dialog region that allows the user to specify a new element name
	 * @param parent
	 */
	private void createNewElementArea(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 4;
		area.setLayout(layout);
		area.setLayoutData(new GridData(GridData.FILL_BOTH));

		new Label(area, SWT.NONE).setText("Name: ");
		nameField = new Text(area, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData();
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		nameField.setLayoutData(data);

		createButton(area, CREATE_FILE_ID, "New File", false);
		createButton(area, CREATE_FOLDER_ID, "New Folder", false);
	}

	/**
	 * Returns the currently selected file store, or null if there is no select.
	 * @return
	 */
	private IFileStore getSelectedFileStore() {
		ISelection selection = tree.getSelection();
		if (selection instanceof IStructuredSelection) {
			IFileStore selected = (IFileStore) ((IStructuredSelection) selection).getFirstElement();
			if (selected != null)
				return selected;
		}
		return null;
	}

	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	protected void okPressed() {
		IStructuredSelection selection = (IStructuredSelection) tree.getSelection();
		setResult(selection.toList());
		super.okPressed();
	}
}
