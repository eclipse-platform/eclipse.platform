/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.launching.launchConfigurations;

import org.eclipse.ant.internal.launching.AntLaunching;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;

/**
 *
 */
public class AntStreamsProxy implements IStreamsProxy {

	private final AntStreamMonitor fErrorMonitor = new AntStreamMonitor();
	private final AntStreamMonitor fOutputMonitor = new AntStreamMonitor();

	public static final String ANT_DEBUG_STREAM = AntLaunching.PLUGIN_ID + ".ANT_DEBUG_STREAM"; //$NON-NLS-1$
	public static final String ANT_VERBOSE_STREAM = AntLaunching.PLUGIN_ID + ".ANT_VERBOSE_STREAM"; //$NON-NLS-1$
	public static final String ANT_WARNING_STREAM = AntLaunching.PLUGIN_ID + ".ANT_WARNING_STREAM"; //$NON-NLS-1$

	private final AntStreamMonitor fDebugMonitor = new AntStreamMonitor();
	private final AntStreamMonitor fVerboseMonitor = new AntStreamMonitor();
	private final AntStreamMonitor fWarningMonitor = new AntStreamMonitor();

	@Override
	public IStreamMonitor getErrorStreamMonitor() {
		return fErrorMonitor;
	}

	@Override
	public IStreamMonitor getOutputStreamMonitor() {
		return fOutputMonitor;
	}

	@Override
	public void write(String input) {
		// do nothing
	}

	public IStreamMonitor getWarningStreamMonitor() {
		return fWarningMonitor;
	}

	public IStreamMonitor getDebugStreamMonitor() {
		return fDebugMonitor;
	}

	public IStreamMonitor getVerboseStreamMonitor() {
		return fVerboseMonitor;
	}
}
