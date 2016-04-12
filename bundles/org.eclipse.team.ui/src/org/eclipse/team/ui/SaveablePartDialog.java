/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.PlatformUI;

/**
 * A dialog that displays a {@link org.eclipse.team.ui.ISaveableWorkbenchPart} and
 * ensures that changes made to the input are saved when the dialog is closed.
 *
 * @see ISaveableWorkbenchPart
 * @see SaveablePartAdapter
 * @since 3.0
 * @deprecated Clients should use a subclass of {@link CompareEditorInput}
 *      and {@link CompareUI#openCompareDialog(org.eclipse.compare.CompareEditorInput)}
 */
@Deprecated
public class SaveablePartDialog extends TrayDialog {

	private ISaveableWorkbenchPart input;
	private String fContextId;
	private boolean hasSettings = true;

	/**
	 * Creates a dialog with the given title and input. The input is not created until the dialog
	 * is opened.
	 *
	 * @param shell the parent shell or <code>null</code> to create a top level shell.
	 * @param input the part to show in the dialog.
	 */
	public SaveablePartDialog(Shell shell, ISaveableWorkbenchPart input) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		this.input = input;
	}

	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	@Override
	protected Control createDialogArea(Composite parent2) {
		Composite parent = (Composite) super.createDialogArea(parent2);
		input.createPartControl(parent);
		Shell shell = getShell();
		shell.setText(input.getTitle());
		shell.setImage(input.getTitleImage());
		Dialog.applyDialogFont(parent2);
		return parent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.internal.ResizableDialog#close()
	 */
	@Override
	public boolean close() {
		saveChanges();
		return super.close();
	}

	/**
	 * Save any changes to the compare editor.
	 */
	private void saveChanges() {
		MessageDialog dialog = new MessageDialog(
				getShell(), TeamUIMessages.ParticipantCompareDialog_2, null,
				TeamUIMessages.ParticipantCompareDialog_3, MessageDialog.QUESTION, new String[]{IDialogConstants.YES_LABEL,
				IDialogConstants.NO_LABEL}, 0); // YES is the default

		if (input.isDirty() && dialog.open() == IDialogConstants.OK_ID) {
			BusyIndicator.showWhile(null, new Runnable() {
				@Override
				public void run() {
					input.doSave(new NullProgressMonitor());
				}
			});
		}
	}

	/**
	 * Return the input to the dialog.
	 * @return the input to the dialog
	 * @since 3.2
	 */
	protected ISaveableWorkbenchPart getInput() {
		return input;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
	 */
	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings compareSettings = TeamUIPlugin.getPlugin().getDialogSettings();
		String sectionName = this.getClass().getName();
		IDialogSettings dialogSettings = compareSettings.getSection(sectionName);
		if (dialogSettings == null) {
			hasSettings = false;
			dialogSettings = compareSettings.addNewSection(sectionName);
		}
		return dialogSettings;
	}

	/**
	 * Set the help content id of this dialog.
	 * @param contextId the help context id
	 */
	public void setHelpContextId(String contextId) {
		fContextId= contextId;
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		if (fContextId != null)
			PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, fContextId);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#getInitialSize()
	 */
	@Override
	protected Point getInitialSize() {
		Point initialSize = super.getInitialSize();
		if (hasSettings) {
			return initialSize;
		}
		// If we don't have settings we need to come up with a reasonable default
		// since we can't depend on the compare editor input layout returning a good default size
		int width= 0;
		int height= 0;
		Shell shell= getParentShell();
		if (shell != null) {
			Point parentSize= shell.getSize();
			width= parentSize.x-100;
			height= parentSize.y-100;
		}
		if (width < 700)
			width= 700;
		if (height < 500)
			height= 500;
		return new Point(width, height);
	}
}
