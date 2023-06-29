/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.launchConfigurations;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * This class is used to construct a launch configuration dialog used to edit a launch configuration and continue or cancel (optional),
 * not allowing a launch to occur.
 *
 * @since 3.3
 */
public class LaunchConfigurationEditDialog extends LaunchConfigurationDialog {

	private boolean fShowCancel = false;

	/**
	 * Constructor
	 * @param shell the shell to create this dialog on
	 * @param launchConfiguration the launch config that this dialog is allowing you to edit
	 * @param group the launch group associated with the showing tab group
	 * @param showcancel if the cancel button should be shown or not
	 */
	public LaunchConfigurationEditDialog(Shell shell, ILaunchConfiguration launchConfiguration, LaunchGroupExtension group, boolean showcancel) {
		super(shell, launchConfiguration, group);
		fShowCancel = showcancel;
	}

	@Override
	protected String getTitleAreaTitle() {
		return LaunchConfigurationsMessages.LaunchConfigurationEditDialog_0;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, ID_LAUNCH_BUTTON, LaunchConfigurationsMessages.LaunchConfigurationEditDialog_1, true);
		if(fShowCancel) {
			createButton(parent, ID_CANCEL_BUTTON, IDialogConstants.CANCEL_LABEL, false);
		}
	}

	@Override
	public void updateButtons() {
		getTabViewer().refresh();
		getButton(ID_LAUNCH_BUTTON).setEnabled(getTabViewer().canLaunch() && getTabViewer().canLaunchWithModes()
				&& !getTabViewer().hasDuplicateDelegates());
	}

	@Override
	protected String getDialogSettingsSectionName() {
		return IDebugUIConstants.PLUGIN_ID + ".LAUNCH_CONFIGURATION_EDIT_DIALOG_SECTION"; //$NON-NLS-1$
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
			case ID_LAUNCH_BUTTON: {
				int status = shouldSaveCurrentConfig();
				if (status == IDialogConstants.YES_ID) {
					okPressed();
				}
				setReturnCode(OK);
				if (status != IDialogConstants.CANCEL_ID) {
					close();
				}
				break;
			}
			case ID_CANCEL_BUTTON: {
				cancelPressed();
				break;
			}
			default:
				break;
		}
	}
}
