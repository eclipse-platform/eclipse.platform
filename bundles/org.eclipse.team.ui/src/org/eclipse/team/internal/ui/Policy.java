/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui;

import java.util.ResourceBundle;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;

/**
 * Policy implements NLS convenience methods for the plugin and
 * makes progress monitor policy decisions
 */
public class Policy {
	//debug constants
	public static boolean DEBUG_SYNC_MODELS = false;
	public static boolean DEBUG_HISTORY = false;
	public static boolean DEBUG_DND = false;
    
    private static String ACTION_BUNDLE = "org.eclipse.team.internal.ui.actions.actions"; //$NON-NLS-1$
    private static ResourceBundle actionBundle = null;

    /*
     * Returns a resource bundle, creating one if it none is available.
     */
    public static ResourceBundle getActionBundle() {
        // thread safety
        ResourceBundle tmpBundle = actionBundle;
        if (tmpBundle != null)
            return tmpBundle;
        return actionBundle = ResourceBundle.getBundle(ACTION_BUNDLE);
    }

	static final DebugOptionsListener DEBUG_OPTIONS_LISTENER = new DebugOptionsListener() {
		public void optionsChanged(DebugOptions options) {
			boolean DEBUG = options.getBooleanOption(TeamUIPlugin.ID + "/debug", false); //$NON-NLS-1$
			DEBUG_SYNC_MODELS = DEBUG && options.getBooleanOption(TeamUIPlugin.ID + "/syncmodels", false); //$NON-NLS-1$
			DEBUG_HISTORY = DEBUG && options.getBooleanOption(TeamUIPlugin.ID + "/history", false); //$NON-NLS-1$
			DEBUG_DND = DEBUG && options.getBooleanOption(TeamUIPlugin.ID + "/dnd", false); //$NON-NLS-1$
		}
	};

	/**
	 * Checks if the progress monitor is canceled.
	 * 
	 * @param monitor  the onitor to check for cancellation
	 * @throws OperationCanceledException if the monitor is canceled
	 */
	public static void checkCanceled(IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}
	
	public static IProgressMonitor subMonitorFor(IProgressMonitor monitor, int ticks) {
		if (monitor == null)
			return new NullProgressMonitor();
		if (monitor instanceof NullProgressMonitor)
			return monitor;
		return new SubProgressMonitor(monitor, ticks);
	}

	public static IProgressMonitor monitorFor(IProgressMonitor monitor) {
		if (monitor == null)
			return new NullProgressMonitor();
		return monitor;
	}
}
