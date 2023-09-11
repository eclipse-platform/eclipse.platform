/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
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

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

public class SetTargetsDialog extends Dialog {

	private static String DIALOG_SETTINGS_SECTION = "SetTargetsDialogSettings"; //$NON-NLS-1$

	private final ILaunchConfigurationWorkingCopy fConfiguration;
	private AntTargetsTab fTargetsTab;

	public SetTargetsDialog(Shell parentShell, ILaunchConfigurationWorkingCopy config) {
		super(parentShell);
		setShellStyle(SWT.RESIZE | getShellStyle());
		fConfiguration = config;
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		getShell().setText(AntLaunchConfigurationMessages.SetTargetsDialog_0);
		Composite composite = (Composite) super.createDialogArea(parent);

		fTargetsTab = new AntTargetsTab();
		fTargetsTab.createControl(composite);
		fTargetsTab.initializeFrom(fConfiguration);
		applyDialogFont(composite);
		return composite;
	}

	@Override
	protected void okPressed() {
		fTargetsTab.performApply(fConfiguration);

		super.okPressed();
	}

	protected String getTargetsSelected() {
		String defaultValue = null;
		if (!fTargetsTab.isTargetSelected()) {
			defaultValue = IAntCoreConstants.EMPTY_STRING;
		}
		try {
			return fConfiguration.getAttribute(IAntLaunchConstants.ATTR_ANT_TARGETS, defaultValue);
		}
		catch (CoreException e) {
			return defaultValue;
		}
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(SetTargetsDialog.class)).getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
		if (section == null) {
			section = settings.addNewSection(DIALOG_SETTINGS_SECTION);
		}
		return section;
	}
}
