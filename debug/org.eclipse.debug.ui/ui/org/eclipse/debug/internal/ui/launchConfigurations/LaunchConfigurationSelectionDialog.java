/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

import org.eclipse.debug.internal.ui.AbstractDebugListSelectionDialog;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.swt.widgets.Shell;

/**
 * This class provides a dialog for selecting a given launch configuration from a listing
 *
 * @since 3.3.0
 * CONTEXTLAUNCHING
 */
public class LaunchConfigurationSelectionDialog extends AbstractDebugListSelectionDialog {

	private static final String DIALOG_SETTINGS = IDebugUIConstants.PLUGIN_ID + ".SELECT_LAUNCH_CONFIGURATION_DIALOG"; //$NON-NLS-1$;
	private Object fInput;

	/**
	 * Constructor
	 * @param parent
	 */
	public LaunchConfigurationSelectionDialog(Shell parent, Object input) {
		super(parent);
		fInput = input;
		setTitle(LaunchConfigurationsMessages.LaunchConfigurationSelectionDialog_0);
	}

	@Override
	protected String getDialogSettingsId() {
		return DIALOG_SETTINGS;
	}

	@Override
	protected String getHelpContextId() {
		return IDebugHelpContextIds.SELECT_LAUNCH_CONFIGURATION_DIALOG;
	}

	@Override
	protected Object getViewerInput() {
		return fInput;
	}

	@Override
	protected String getViewerLabel() {
		return LaunchConfigurationsMessages.LaunchConfigurationSelectionDialog_1;
	}

}
