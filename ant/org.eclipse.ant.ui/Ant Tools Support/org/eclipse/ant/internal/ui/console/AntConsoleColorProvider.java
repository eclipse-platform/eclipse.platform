/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.console;

import org.eclipse.ant.internal.launching.launchConfigurations.AntStreamsProxy;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.console.ConsoleColorProvider;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.IOConsoleOutputStream;

public class AntConsoleColorProvider extends ConsoleColorProvider implements IPropertyChangeListener {

	@Override
	public Color getColor(String streamIdentifer) {
		if (streamIdentifer.equals(IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM)) {
			return AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_INFO_COLOR);
		}
		if (streamIdentifer.equals(IDebugUIConstants.ID_STANDARD_ERROR_STREAM)) {
			return AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_ERROR_COLOR);
		}
		if (streamIdentifer.equals(AntStreamsProxy.ANT_DEBUG_STREAM)) {
			return AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_DEBUG_COLOR);
		}
		if (streamIdentifer.equals(AntStreamsProxy.ANT_VERBOSE_STREAM)) {
			return AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_VERBOSE_COLOR);
		}
		if (streamIdentifer.equals(AntStreamsProxy.ANT_WARNING_STREAM)) {
			return AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_WARNING_COLOR);
		}
		return super.getColor(streamIdentifer);
	}

	@Override
	public void connect(IProcess process, IConsole console) {
		// Both remote and local Ant builds are guaranteed to have
		// an AntStreamsProxy. The remote Ant builds make use of the
		// org.eclipse.debug.core.processFactories extension point
		AntStreamsProxy proxy = (AntStreamsProxy) process.getStreamsProxy();
		if (proxy != null) {
			console.connect(proxy.getDebugStreamMonitor(), AntStreamsProxy.ANT_DEBUG_STREAM);
			console.connect(proxy.getWarningStreamMonitor(), AntStreamsProxy.ANT_WARNING_STREAM);
			console.connect(proxy.getVerboseStreamMonitor(), AntStreamsProxy.ANT_VERBOSE_STREAM);
		}

		AntUIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
		super.connect(process, console);
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		final String streamId = getStreamId(event.getProperty());
		if (streamId != null) {
			AntUIPlugin.getStandardDisplay().asyncExec(() -> {
				IOConsoleOutputStream stream = getConsole().getStream(streamId);
				if (stream != null) {
					stream.setColor(getColor(streamId));
				}
			});
		}
	}

	private String getStreamId(String colorId) {
		if (IAntUIPreferenceConstants.CONSOLE_DEBUG_COLOR.equals(colorId)) {
			return AntStreamsProxy.ANT_DEBUG_STREAM;
		} else if (IAntUIPreferenceConstants.CONSOLE_ERROR_COLOR.equals(colorId)) {
			return IDebugUIConstants.ID_STANDARD_ERROR_STREAM;
		} else if (IAntUIPreferenceConstants.CONSOLE_INFO_COLOR.equals(colorId)) {
			return IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM;
		} else if (IAntUIPreferenceConstants.CONSOLE_VERBOSE_COLOR.equals(colorId)) {
			return AntStreamsProxy.ANT_VERBOSE_STREAM;
		} else if (IAntUIPreferenceConstants.CONSOLE_WARNING_COLOR.equals(colorId)) {
			return AntStreamsProxy.ANT_WARNING_STREAM;
		}
		return null;
	}

	@Override
	public void disconnect() {
		AntUIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		super.disconnect();
	}
}
