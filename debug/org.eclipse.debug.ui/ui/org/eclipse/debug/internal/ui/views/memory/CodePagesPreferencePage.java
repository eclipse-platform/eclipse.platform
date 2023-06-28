/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.views.memory;

import java.io.UnsupportedEncodingException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class CodePagesPreferencePage extends FieldEditorPreferencePage {

	private StringFieldEditor fEbcdicCodePage;
	private StringFieldEditor fAsciiCodePage;

	protected CodePagesPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		setPreferenceStore(DebugUITools.getPreferenceStore());
		setTitle(DebugUIMessages.CodePagesPrefDialog_1);
	}

	@Override
	protected void createFieldEditors() {
		fAsciiCodePage = new StringFieldEditor(IDebugUIConstants.PREF_DEFAULT_ASCII_CODE_PAGE, DebugUIMessages.CodePagesPrefDialog_2, getFieldEditorParent());
		fAsciiCodePage.setEmptyStringAllowed(false);
		addField(fAsciiCodePage);

		fEbcdicCodePage = new StringFieldEditor(IDebugUIConstants.PREF_DEFAULT_EBCDIC_CODE_PAGE, DebugUIMessages.CodePagesPrefDialog_4, getFieldEditorParent());
		fEbcdicCodePage.setEmptyStringAllowed(false);
		addField(fEbcdicCodePage);
	}

	@Override
	public boolean performOk() {

		if (fAsciiCodePage == null || fEbcdicCodePage == null)
			return super.performOk();

		// check that the codepages are supported
		String asciiCodePage = fAsciiCodePage.getStringValue();
		asciiCodePage = asciiCodePage.trim();
		try {
			new String(new byte[] { 1 }, asciiCodePage);
		} catch (UnsupportedEncodingException e) {
			Shell shell = DebugUIPlugin.getShell();
			if (shell != null) {
				IStatus status = DebugUIPlugin.newErrorStatus(DebugUIMessages.CodePagesPrefDialog_0, e);
				ErrorDialog.openError(shell, DebugUIMessages.CodePagesPrefDialog_6, DebugUIMessages.CodePagesPrefDialog_7, status); //
			}
			return false;
		}

		String ebcdicCodePage = fEbcdicCodePage.getStringValue();
		ebcdicCodePage = ebcdicCodePage.trim();
		try {
			new String(new byte[] { 1 }, ebcdicCodePage);
		} catch (UnsupportedEncodingException e) {
			Shell shell = DebugUIPlugin.getShell();
			if (shell != null) {
				IStatus status = DebugUIPlugin.newErrorStatus(DebugUIMessages.CodePagesPrefDialog_0, e);
				ErrorDialog.openError(shell, DebugUIMessages.CodePagesPrefDialog_8, DebugUIMessages.CodePagesPrefDialog_9, status); //
			}
			return false;
		}
		return super.performOk();
	}

	@Override
	protected Control createContents(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, DebugUIPlugin.getUniqueIdentifier() + ".CodePagesPrefDialog_context"); //$NON-NLS-1$
		return super.createContents(parent);
	}

}
