/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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

import java.text.MessageFormat;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.debug.internal.core.DebugCoreMessages;
import org.eclipse.debug.internal.core.LaunchConfiguration;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * Allows the user to specify to see and copy the command line to be executed
 * for the launch.
 *
 * @since 3.13
 */
public class ShowCommandLineDialog extends Dialog {
	Text fModuleArgumentsText;
	ILaunchConfiguration flaunchConfiguration;
	String fMode;


	public ShowCommandLineDialog(Shell parentShell, String mode, ILaunchConfiguration config) {
		super(parentShell);
		fMode = mode;
		setShellStyle(SWT.RESIZE | getShellStyle());
		flaunchConfiguration = config;
	}


	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(LaunchConfigurationsMessages.LaunchConfigurationDialog_ShowCommandLine_Title);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				LaunchConfigurationsMessages.LaunchConfigurationDialog_ShowCommandLine_Copy, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				LaunchConfigurationsMessages.LaunchConfigurationDialog_ShowCommandLine_Close, false);
	}

	private LaunchManager getLaunchManager() {
		return (LaunchManager) DebugPlugin.getDefault().getLaunchManager();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		Font font = parent.getFont();

		Group group = new Group(comp, SWT.NONE);
		GridLayout topLayout = new GridLayout();
		group.setLayout(topLayout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = convertHeightInCharsToPixels(20);
		gd.widthHint = convertWidthInCharsToPixels(90);
		group.setLayoutData(gd);
		group.setFont(font);

		fModuleArgumentsText = new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = convertHeightInCharsToPixels(10);
		gd.widthHint = convertWidthInCharsToPixels(60);
		fModuleArgumentsText.setLayoutData(gd);

		String command = ""; //$NON-NLS-1$
		try {
			Set<String> modes = flaunchConfiguration.getModes();
			modes.add(fMode);
			ILaunchDelegate[] delegates = flaunchConfiguration.getType().getDelegates(modes);
			ILaunchConfigurationDelegate delegate = null;
			if (delegates.length == 1) {
				delegate = delegates[0].getDelegate();
			} else {
				if (flaunchConfiguration instanceof LaunchConfiguration) {
					delegate = ((LaunchConfiguration) flaunchConfiguration).getPreferredLaunchDelegate(fMode);
				}
			}
			if (delegate != null) {
				ILaunchConfigurationDelegate2 delegate2;
				ILaunch launch = null;
				if (delegate instanceof ILaunchConfigurationDelegate2) {
					delegate2 = (ILaunchConfigurationDelegate2) delegate;
					if (delegate2 != null) {
						launch = delegate2.getLaunch(flaunchConfiguration, fMode);
					}
					if (launch == null) {
						launch = new Launch(flaunchConfiguration, fMode, null);
					} else {
						// ensure the launch mode is valid
						if (!fMode.equals(launch.getLaunchMode())) {
							IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
									DebugPlugin.ERROR, MessageFormat.format(DebugCoreMessages.LaunchConfiguration_14,
											fMode, launch.getLaunchMode()),
									null);
							throw new CoreException(status);
						}
					}
					if (!flaunchConfiguration.getAttribute(DebugPlugin.ATTR_FORCE_SYSTEM_CONSOLE_ENCODING, false)) {
						launch.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING,
								getLaunchManager().getEncoding(flaunchConfiguration));
					}
				}
				command = delegate.showCommandLine(flaunchConfiguration, fMode, launch,
						null);

			}
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}
		if (command == null || (command != null && command.length() == 0)) {
			command = LaunchConfigurationsMessages.LaunchConfigurationDialog_ShowCommandLine_Default;
		}
		fModuleArgumentsText.setText(command);
		fModuleArgumentsText.setEditable(false);

		return comp;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK) {
			Clipboard clipboard = new Clipboard(null);
			try {
				TextTransfer textTransfer = TextTransfer.getInstance();
				Transfer[] transfers = new Transfer[] { textTransfer };
				Object[] data = new Object[] { fModuleArgumentsText.getText() };
				clipboard.setContents(data, transfers);
			} finally {
				clipboard.dispose();
			}
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(ShowCommandLineDialog.class)).getDialogSettings();
		IDialogSettings section = settings.getSection(getDialogSettingsSectionName());
		if (section == null) {
			section = settings.addNewSection(getDialogSettingsSectionName());
		}
		return section;
	}

	/**
	 * @return the name to use to save the dialog settings
	 */
	protected String getDialogSettingsSectionName() {
		return "SHOW_COMMAND_LINE_DIALOG"; //$NON-NLS-1$
	}

}
