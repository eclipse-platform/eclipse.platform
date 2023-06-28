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
package org.eclipse.debug.internal.ui.actions.breakpoints;

import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * Allows the user to specify if they want to delete the working set, all the breakpoints in the working
 * set or both
 * @since 3.2
 */
public class DeleteWorkingsetsMessageDialog extends MessageDialog {

	/**
	 * to determine if we should delete the working set as well
	 */
	private boolean fDeleteWorkingsets = false;

	/**
	 * to determine if we should delete all the breakpoints in the set.
	 * to maintain backward compatibility this is by default true
	 */
	private boolean fDeleteBreakpoints = true;

	//widgets
	private Button fDeleteWS;
	private Button fDeleteBPS;

	// dialog settings
	private final static String DIALOG_SETTINGS = "DeleteBreakpointsDialogSettings"; //$NON-NLS-1$
	private static final String DELETE_BREAKPOINTS = "DeleteBreakpoints";	 //$NON-NLS-1$
	private static final String DELETE_WORKINGSETS = "DeleteWorkingSets";	 //$NON-NLS-1$

	public DeleteWorkingsetsMessageDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels, int defaultIndex) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
		IDialogSettings section = getDialogSettings();
		fDeleteBreakpoints = section.getBoolean(DELETE_BREAKPOINTS);
		fDeleteWorkingsets = section.getBoolean(DELETE_WORKINGSETS);
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout());
		Font font = parent.getFont();
		fDeleteWS = new Button(comp, SWT.CHECK);
		fDeleteWS.setText(ActionMessages.DeleteWorkingsetsMessageDialog_0);
		fDeleteWS.setFont(font);
		fDeleteWS.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
			@Override
			public void widgetSelected(SelectionEvent e) {
				getButton(0).setEnabled(fDeleteWS.getSelection() || fDeleteBPS.getSelection());
			}
		});

		fDeleteBPS = new Button(comp, SWT.CHECK);
		fDeleteBPS.setText(ActionMessages.DeleteWorkingsetsMessageDialog_1);
		fDeleteBPS.setFont(font);
		fDeleteBPS.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
			@Override
			public void widgetSelected(SelectionEvent e) {
				getButton(0).setEnabled(fDeleteWS.getSelection() || fDeleteBPS.getSelection());
			}
		});

		fDeleteWS.setSelection(fDeleteWorkingsets);
		fDeleteBPS.setSelection(fDeleteBreakpoints);
		return comp;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if(buttonId == OK) {
			fDeleteBreakpoints = fDeleteBPS.getSelection();
			fDeleteWorkingsets = fDeleteWS.getSelection();
			IDialogSettings dialogSettings = getDialogSettings();
			dialogSettings.put(DELETE_BREAKPOINTS, fDeleteBreakpoints);
			dialogSettings.put(DELETE_WORKINGSETS, fDeleteWorkingsets);
		}
		super.buttonPressed(buttonId);
	}

	/**
	 * return the checked value of the delete working set check box
	 * @return the checked state of the delete working set check box
	 */
	public boolean deleteWorkingset() {
		return fDeleteWorkingsets;
	}

	/**
	 * returns the checked state of the delete all breakpoints in working set check box
	 * @return the checked state of the delete all breakpoints... check box
	 */
	public boolean deleteAllBreakpoints() {
		return fDeleteBreakpoints;
	}

	/**
	 * Returns the dialog settings for this dialog.
	 *
	 * @return dialog settings
	 */
	protected IDialogSettings getDialogSettings() {
		IDialogSettings workbenchSettings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(DeleteWorkingsetsMessageDialog.class))
				.getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection(DIALOG_SETTINGS);
		if (section == null) {
			section = workbenchSettings.addNewSection(DIALOG_SETTINGS);
			section.put(DELETE_BREAKPOINTS, true);
			section.put(DELETE_WORKINGSETS, false);
		}
		return section;
	}



}
