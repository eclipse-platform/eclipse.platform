/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * This is a dialog that can host a {@link CompareEditorInput}.
 * <p>
 * This class can be used as is or can be subclassed.
 * 
 * @since 3.3
 */
public class CompareDialog extends TrayDialog implements IPropertyChangeListener {
	
	private final CompareEditorInput fCompareEditorInput;
	private Button fCommitButton;
	private Label statusLabel;
	boolean hasSettings = true;
	private final DialogCompareContainer fContainer = new DialogCompareContainer();
	
	private class DialogCompareContainer extends CompareContainer {
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.operation.IRunnableContext#run(boolean, boolean, org.eclipse.jface.operation.IRunnableWithProgress)
		 */
		public void run(boolean fork, boolean cancelable,
				IRunnableWithProgress runnable) throws InvocationTargetException,
				InterruptedException {
			ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
			dialog.run(fork, cancelable, runnable);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.compare.ICompareContainer#setStatusMessage(java.lang.String)
		 */
		public void setStatusMessage(String message) {
			if (statusLabel != null && !statusLabel.isDisposed()) {
				if (message == null) {
					statusLabel.setText(""); //$NON-NLS-1$
				} else {
					statusLabel.setText(message);
				}
			}
		}
	}
	
	/**
	 * Create a dialog to host the given input.
	 * @param shell a shell
	 * @param input the dialog input
	 */
	public CompareDialog(Shell shell, CompareEditorInput input) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		Assert.isNotNull(input);
		fCompareEditorInput= input;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.internal.ResizableDialog#close()
	 */
	public boolean close() {
		if (super.close()) {
			if (fCompareEditorInput != null)
				fCompareEditorInput.removePropertyChangeListener(this);
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		fCommitButton= createButton(parent, IDialogConstants.OK_ID, getOKButtonLabel(), true);
		fCommitButton.setEnabled(isOKEnabled());
		if (isCancelable()) {
			createButton(parent, IDialogConstants.CANCEL_ID, getCancelButtonLabel(), false);
		}
	}

	private String getCancelButtonLabel() {
		return fCompareEditorInput.getCancelButtonLabel();
	}

	private boolean isCancelable() {
		return isInputEditable() || isElementSelectionDialog();
	}

	private String getOKButtonLabel() {
		return fCompareEditorInput.getOKButtonLabel();
	}

	private boolean isElementSelectionDialog() {
		return fCompareEditorInput.isEditionSelectionDialog();
	}

	/**
	 * Return whether the compare editor input of this dialog is editable.
	 * By default, the input is editable if the compare configuration
	 * indicates that either the left or right sides are editable.
	 * Subclasses may override.
	 * @return whether the compare editor input of this dialog is editable
	 * @see CompareConfiguration#isLeftEditable()
	 * @see CompareConfiguration#isRightEditable()
	 */
	protected boolean isInputEditable() {
		return fCompareEditorInput.getCompareConfiguration().isLeftEditable() 
			|| fCompareEditorInput.getCompareConfiguration().isRightEditable();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(CompareEditorInput.DIRTY_STATE)
				|| event.getProperty().equals(CompareEditorInput.PROP_SELECTED_EDITION)) {
			if (fCommitButton != null && fCompareEditorInput != null)
				fCommitButton.setEnabled(isOKEnabled());
		} else if (event.getProperty().equals(CompareEditorInput.PROP_TITLE)) {
			getShell().setText(fCompareEditorInput.getTitle());
		} else if (event.getProperty().equals(CompareEditorInput.PROP_TITLE_IMAGE)) {
			getShell().setImage(fCompareEditorInput.getTitleImage());
		}
	}

	private boolean isOKEnabled() {
		if (isInputEditable())
			return fCompareEditorInput.isDirty();
		if (isElementSelectionDialog())
			return getSelectedElement() != null;
		return true;
	}
	
	private Object getSelectedElement() {
		return fCompareEditorInput.getSelectedEdition();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent2) {
						
		Composite parent= (Composite) super.createDialogArea(parent2);

		Control c= fCompareEditorInput.createContents(parent);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		statusLabel = new Label(parent, SWT.NONE);
		statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Shell shell= c.getShell();
		shell.setText(fCompareEditorInput.getTitle());
		shell.setImage(fCompareEditorInput.getTitleImage());
		applyDialogFont(parent);
		return parent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#open()
	 */
	public int open() {
		// Before opening, set the container of the input and listen
		// for changes to the input
		fCompareEditorInput.addPropertyChangeListener(this);
		fCompareEditorInput.setContainer(fContainer);
		return super.open();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK) {
			if (!fCompareEditorInput.okPressed())
				return;
		} else if (buttonId == CANCEL) {
			fCompareEditorInput.cancelPressed();
		}
		super.buttonPressed(buttonId);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
	 */
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings compareSettings = CompareUIPlugin.getDefault().getDialogSettings();
		String sectionName = this.getClass().getName();
		IDialogSettings dialogSettings = compareSettings.getSection(sectionName);
		if (dialogSettings == null) {
			hasSettings = false;
			dialogSettings = compareSettings.addNewSection(sectionName);
		}
		return dialogSettings;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.internal.ResizableDialog#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		if (getHelpContextId() != null)
			PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, getHelpContextId());
	}

	/**
	 * Return the help content id for this dialog or <code>null</code>.
	 * By default, a generic help content id is returned. Subclasses may
	 * override.
	 * @return the help content id for this dialog or <code>null</code>
	 */
	public String getHelpContextId() {
		return ICompareContextIds.COMPARE_DIALOG;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#getInitialSize()
	 */
	protected Point getInitialSize() {
		Point initialSize = super.getInitialSize();
		if (hasSettings) {
			return initialSize;
		}
		return getDefaultSize();
	}

	/**
	 * If we don't have settings we need to come up with a reasonable default
	 * since we can't depend on the compare editor input layout returning a 
	 * good default size.
	 * @return the default size of the dialog
	 */
	protected Point getDefaultSize() {
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

	/**
	 * Return the compare editor input for this dialog.
	 * @return the compare editor input for this dialog
	 */
	protected final CompareEditorInput getInput() {
		return fCompareEditorInput;
	}

}
