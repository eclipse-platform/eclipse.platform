/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.internal.ui.preferences.StringVariablePreferencePage.VariableWrapper;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

class StringVariablePreviewDialog extends Dialog {

	private final VariableWrapper fVariable;

	StringVariablePreviewDialog(Shell parentShell, VariableWrapper variable) {
		super(parentShell);
		fVariable = variable;
		setShellStyle(SWT.RESIZE | getShellStyle());
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(DebugPreferencesMessages.StringVariablePreviewDialog_Title);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, DebugPreferencesMessages.StringVariablePreviewDialog_Close,
				false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);

		StringBuilder text = new StringBuilder();
		List<StyleRange> styles = new ArrayList<>();
		appendDetail(text, styles, DebugPreferencesMessages.StringVariablePreviewDialog_Name, fVariable.getName());
		appendDetail(text, styles, DebugPreferencesMessages.StringVariablePreviewDialog_Value, fVariable.getValue());
		appendDetail(text, styles, DebugPreferencesMessages.StringVariablePreviewDialog_Description,
				fVariable.getDescription());
		appendDetail(text, styles, DebugPreferencesMessages.StringVariablePreviewDialog_ReadOnly,
				yesNo(fVariable.isReadOnly()));
		appendDetail(text, styles, DebugPreferencesMessages.StringVariablePreviewDialog_Contributed,
				yesNo(fVariable.isContributed()));
		if (fVariable.isContributed()) {
			appendDetail(text, styles, DebugPreferencesMessages.StringVariablePreviewDialog_ContributedBy,
					VariablesPlugin.getDefault().getStringVariableManager()
							.getContributingPluginId(fVariable.getUnderlyingVariable()));
		}

		StyledText styledText = new StyledText(comp, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL | SWT.READ_ONLY);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = convertHeightInCharsToPixels(22);
		gd.widthHint = convertWidthInCharsToPixels(120);
		styledText.setLayoutData(gd);
		styledText.setText(text.toString());
		styledText.setStyleRanges(styles.toArray(new StyleRange[0]));
		return comp;
	}

	private static String yesNo(boolean flag) {
		return flag ? DebugPreferencesMessages.StringVariablePreviewDialog_Yes
				: DebugPreferencesMessages.StringVariablePreviewDialog_No;
	}

	/**
	 * Appends a bold title followed by its value on the next line; skipped if the
	 * value is null.
	 */
	private static void appendDetail(StringBuilder text, List<StyleRange> styles, String title, String value) {
		if (value == null) {
			return;
		}
		if (text.length() > 0) {
			text.append("\n\n"); //$NON-NLS-1$
		}
		styles.add(new StyleRange(text.length(), title.length(), null, null, SWT.BOLD));
		text.append(title).append('\n').append(value);
	}
}
