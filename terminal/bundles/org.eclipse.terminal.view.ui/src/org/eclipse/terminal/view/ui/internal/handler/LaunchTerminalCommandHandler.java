/*******************************************************************************
 * Copyright (c) 2011, 2025 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Alexander Fedorov (ArSysOp) - further evolution
 *******************************************************************************/
package org.eclipse.terminal.view.ui.internal.handler;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Named;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.terminal.view.core.IContextPropertiesConstants;
import org.eclipse.terminal.view.core.ITerminalContextPropertiesProvider;
import org.eclipse.terminal.view.core.ITerminalsConnectorConstants;
import org.eclipse.terminal.view.core.TerminalContextPropertiesProviderFactory;
import org.eclipse.terminal.view.ui.internal.ITraceIds;
import org.eclipse.terminal.view.ui.internal.UIPlugin;
import org.eclipse.terminal.view.ui.internal.dialogs.LaunchTerminalSettingsDialog;
import org.eclipse.terminal.view.ui.launcher.ILauncherDelegate;

/**
 * Launch terminal command handler implementation.
 */
public class LaunchTerminalCommandHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
			@Named(IServiceConstants.ACTIVE_SELECTION) ISelection selection,
			@Named(IServiceConstants.ACTIVE_PART) MPart activePart) {
		// Determine which command variant is being executed based on the active part context
		String commandId = determineCommandId(activePart);
		long start = System.currentTimeMillis();

		if (UIPlugin.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_LAUNCH_TERMINAL_COMMAND_HANDLER)) {
			DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
			String date = format.format(new Date(start));

			UIPlugin.getTraceHandler().trace("Started at " + date + " (" + start + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					ITraceIds.TRACE_LAUNCH_TERMINAL_COMMAND_HANDLER, this);
		}

		if (commandId.equals("org.eclipse.terminal.view.ui.command.launchConsole")) { //$NON-NLS-1$
			LaunchTerminalSettingsDialog dialog = new LaunchTerminalSettingsDialog(shell, start);
			if (dialog.open() == Window.OK) {
				Optional<ILauncherDelegate> delegate = findDelegate(dialog);
				if (delegate.isEmpty()) {
					return;
				}
				try {
					createConnector(delegate.get(), dialog.getSettings());
				} catch (Exception e) {
					ILog.get().error("Error creating terminal connector", e); //$NON-NLS-1$
				}
			}
			return;
		}
		if (commandId.equals("org.eclipse.terminal.view.ui.command.launchToolbar")) { //$NON-NLS-1$
			if (UIPlugin.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_LAUNCH_TERMINAL_COMMAND_HANDLER)) {
				UIPlugin.getTraceHandler().trace("(a) Attempt to open launch terminal settings dialog after " //$NON-NLS-1$
						+ (System.currentTimeMillis() - start) + " ms.", //$NON-NLS-1$
						ITraceIds.TRACE_LAUNCH_TERMINAL_COMMAND_HANDLER, this);
			}

			LaunchTerminalSettingsDialog dialog = new LaunchTerminalSettingsDialog(shell, start);

			if (isValidSelection(selection)) {
				dialog.setSelection(selection);
			}
			if (dialog.open() == Window.OK) {
				Optional<ILauncherDelegate> delegate = findDelegate(dialog);
				if (delegate.isPresent()) {
					executeDelegate(dialog.getSettings(), delegate.get());
				}
			}
		} else {
			if (UIPlugin.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_LAUNCH_TERMINAL_COMMAND_HANDLER)) {
				UIPlugin.getTraceHandler().trace(
						"Getting applicable launcher delegates after " + (System.currentTimeMillis() - start) + " ms.", //$NON-NLS-1$ //$NON-NLS-2$
						ITraceIds.TRACE_LAUNCH_TERMINAL_COMMAND_HANDLER, this);
			}

			// Check if the dialog needs to be shown at all
			List<ILauncherDelegate> delegates = UIPlugin.getLaunchDelegateManager()
					.getApplicableLauncherDelegates(selection).toList();

			if (UIPlugin.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_LAUNCH_TERMINAL_COMMAND_HANDLER)) {
				UIPlugin.getTraceHandler().trace(
						"Got applicable launcher delegates after " + (System.currentTimeMillis() - start) + " ms.", //$NON-NLS-1$ //$NON-NLS-2$
						ITraceIds.TRACE_LAUNCH_TERMINAL_COMMAND_HANDLER, this);
			}

			if (delegates.size() > 1 || (delegates.size() == 1 && delegates.get(0).needsUserConfiguration())) {
				if (UIPlugin.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_LAUNCH_TERMINAL_COMMAND_HANDLER)) {
					UIPlugin.getTraceHandler().trace("(b) Attempt to open launch terminal settings dialog after " //$NON-NLS-1$
							+ (System.currentTimeMillis() - start) + " ms.", //$NON-NLS-1$
							ITraceIds.TRACE_LAUNCH_TERMINAL_COMMAND_HANDLER, this);
				}

				// Create the launch terminal settings dialog
				LaunchTerminalSettingsDialog dialog = new LaunchTerminalSettingsDialog(shell, start);
				if (isValidSelection(selection)) {
					dialog.setSelection(selection);
				}
				if (dialog.open() == Window.OK) {
					Optional<ILauncherDelegate> delegate = findDelegate(dialog);
					if (delegate.isPresent()) {
						executeDelegate(dialog.getSettings(), delegate.get());
					}
				}
			} else if (delegates.size() == 1) {
				ILauncherDelegate delegate = delegates.get(0);
				executeDelegate(selection, delegate);
			}
		}
	}

	/**
	 * Determines the command ID based on the active part context.
	 * In E4, we can't directly access the command ID from the event, so we use
	 * a default behavior.
	 * 
	 * @param activePart the active part
	 * @return the command ID
	 */
	private String determineCommandId(MPart activePart) {
		// Default to launch command - this will be differentiated by the E4 model
		// which can bind different instances of this handler to different commands
		return "org.eclipse.terminal.view.ui.command.launch"; //$NON-NLS-1$
	}

	private Optional<ILauncherDelegate> findDelegate(LaunchTerminalSettingsDialog dialog) {
		return Optional.ofNullable(dialog.getSettings())
				.map(map -> map.get(ITerminalsConnectorConstants.PROP_DELEGATE_ID)).filter(String.class::isInstance)
				.map(String.class::cast)
				.flatMap(id -> UIPlugin.getLaunchDelegateManager().findLauncherDelegate(id, false));
	}

	private ITerminalConnector createConnector(ILauncherDelegate delegate, Map<String, Object> settings)
			throws CoreException {
		return delegate.createTerminalConnector(settings);
	}

	private boolean isValidSelection(ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			ITerminalContextPropertiesProvider provider = TerminalContextPropertiesProviderFactory.getProvider(element);
			if (provider != null) {
				Map<String, String> props = provider.getTargetAddress(element);
				if (props != null && props.containsKey(IContextPropertiesConstants.PROP_ADDRESS)) {
					return true;
				}
			}
		}

		return false;
	}

	private void executeDelegate(ISelection selection, ILauncherDelegate delegate) {
		Map<String, Object> properties = new HashMap<>();
		properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID, delegate.getId());
		properties.put(ITerminalsConnectorConstants.PROP_SELECTION, selection);
		executeDelegate(properties, delegate);
	}

	private void executeDelegate(Map<String, Object> properties, ILauncherDelegate delegate) {
		delegate.execute(properties).whenComplete((r, e) -> {
			if (e != null) {
				ILog.get().error("Error occurred while running delegate to open console", e); //$NON-NLS-1$
			}
		});
	}

}
