/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	
	private AntStreamMonitor fErrorMonitor = new AntStreamMonitor();
	private AntStreamMonitor fOutputMonitor = new AntStreamMonitor();
	
	public static final String ANT_DEBUG_STREAM = AntLaunching.PLUGIN_ID + ".ANT_DEBUG_STREAM"; //$NON-NLS-1$
	public static final String ANT_VERBOSE_STREAM = AntLaunching.PLUGIN_ID + ".ANT_VERBOSE_STREAM"; //$NON-NLS-1$
	public static final String ANT_WARNING_STREAM = AntLaunching.PLUGIN_ID + ".ANT_WARNING_STREAM"; //$NON-NLS-1$
	
	private AntStreamMonitor fDebugMonitor = new AntStreamMonitor();
	private AntStreamMonitor fVerboseMonitor = new AntStreamMonitor();
	private AntStreamMonitor fWarningMonitor = new AntStreamMonitor();

	/**
	 * @see org.eclipse.debug.core.model.IStreamsProxy#getErrorStreamMonitor()
	 */
	public IStreamMonitor getErrorStreamMonitor() {
		return fErrorMonitor;
	}

	/**
	 * @see org.eclipse.debug.core.model.IStreamsProxy#getOutputStreamMonitor()
	 */
	public IStreamMonitor getOutputStreamMonitor() {
		return fOutputMonitor;
	}

	/**
	 * @see org.eclipse.debug.core.model.IStreamsProxy#write(java.lang.String)
	 */
	public void write(String input) {
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
