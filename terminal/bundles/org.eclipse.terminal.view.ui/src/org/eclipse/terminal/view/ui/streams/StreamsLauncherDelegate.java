/*******************************************************************************
 * Copyright (c) 2015, 2025 Wind River Systems, Inc. and others. All rights reserved.
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
package org.eclipse.terminal.view.ui.streams;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.terminal.connector.ISettingsStore;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.terminal.connector.InMemorySettingsStore;
import org.eclipse.terminal.connector.TerminalConnectorExtension;
import org.eclipse.terminal.view.core.ITerminalServiceOutputStreamMonitorListener;
import org.eclipse.terminal.view.core.ITerminalsConnectorConstants;
import org.eclipse.terminal.view.ui.launcher.AbstractLauncherDelegate;
import org.eclipse.terminal.view.ui.launcher.IConfigurationPanel;
import org.eclipse.terminal.view.ui.launcher.IConfigurationPanelContainer;

/**
 * Streams launcher delegate implementation.
 */
public class StreamsLauncherDelegate extends AbstractLauncherDelegate {

	@Override
	public boolean needsUserConfiguration() {
		return false;
	}

	@Override
	public IConfigurationPanel getPanel(IConfigurationPanelContainer container) {
		return null;
	}

	@Override
	public CompletableFuture<?> execute(Map<String, Object> properties) {
		Assert.isNotNull(properties);
		try {
			return getTerminalService().openConsole(properties);
		} catch (RuntimeException e) {
			return CompletableFuture.failedFuture(e);
		}
	}

	@Override
	public ITerminalConnector createTerminalConnector(Map<String, Object> properties) throws CoreException {
		Assert.isNotNull(properties);

		// Check for the terminal connector id
		String connectorId = (String) properties.get(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID);
		if (connectorId == null) {
			connectorId = "org.eclipse.terminal.connector.streams.StreamsConnector"; //$NON-NLS-1$
		}

		// Extract the streams properties
		OutputStream stdin = (OutputStream) properties.get(ITerminalsConnectorConstants.PROP_STREAMS_STDIN);
		InputStream stdout = (InputStream) properties.get(ITerminalsConnectorConstants.PROP_STREAMS_STDOUT);
		InputStream stderr = (InputStream) properties.get(ITerminalsConnectorConstants.PROP_STREAMS_STDERR);
		Object value = properties.get(ITerminalsConnectorConstants.PROP_LOCAL_ECHO);
		boolean localEcho = value instanceof Boolean b ? b.booleanValue() : false;
		String lineSeparator = (String) properties.get(ITerminalsConnectorConstants.PROP_LINE_SEPARATOR);
		ITerminalServiceOutputStreamMonitorListener[] stdoutListeners = (ITerminalServiceOutputStreamMonitorListener[]) properties
				.get(ITerminalsConnectorConstants.PROP_STDOUT_LISTENERS);
		ITerminalServiceOutputStreamMonitorListener[] stderrListeners = (ITerminalServiceOutputStreamMonitorListener[]) properties
				.get(ITerminalsConnectorConstants.PROP_STDERR_LISTENERS);

		// Construct the terminal settings store
		ISettingsStore store = new InMemorySettingsStore();

		// Construct the streams settings
		StreamsSettings streamsSettings = new StreamsSettings();
		streamsSettings.setStdinStream(stdin);
		streamsSettings.setStdoutStream(stdout);
		streamsSettings.setStderrStream(stderr);
		streamsSettings.setLocalEcho(localEcho);
		streamsSettings.setLineSeparator(lineSeparator);
		streamsSettings.setStdOutListeners(stdoutListeners);
		streamsSettings.setStdErrListeners(stderrListeners);
		// And save the settings to the store
		streamsSettings.save(store);

		// Construct the terminal connector instance
		ITerminalConnector connector = TerminalConnectorExtension.makeTerminalConnector(connectorId);
		// Apply default settings
		connector.setDefaultSettings();
		// And load the real settings
		connector.load(store);
		return connector;
	}

}
