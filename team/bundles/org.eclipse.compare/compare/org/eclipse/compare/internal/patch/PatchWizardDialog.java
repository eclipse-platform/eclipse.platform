/*******************************************************************************
 * Copyright (c) 2005, 2021 IBM Corporation and others.
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
package org.eclipse.compare.internal.patch;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

public class PatchWizardDialog extends WizardDialog {
	private static final String PATCH_WIZARD_SETTINGS_SECTION = "PatchWizard"; //$NON-NLS-1$

	public PatchWizardDialog(Shell parent, IWizard wizard) {
		super(parent, wizard);

		setShellStyle(getShellStyle() | SWT.RESIZE);
		setMinimumPageSize(700, 500);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(PatchWizardDialog.class)).getDialogSettings();
		IDialogSettings section = settings.getSection(PATCH_WIZARD_SETTINGS_SECTION);
		if (section == null) {
			section = settings.addNewSection(PATCH_WIZARD_SETTINGS_SECTION);
		}
		return section;
	}
}
