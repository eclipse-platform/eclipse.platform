/*******************************************************************************
 * Copyright (c) 2017, 2019 SSI Schaefer IT Solutions GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.internal;

import org.eclipse.osgi.util.NLS;

public class LaunchViewMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.debug.ui.launchview.internal.messages"; //$NON-NLS-1$

	public static String DebugCoreLaunchObject_CannotGetType;
	public static String DebugCoreLaunchObject_CannotRelaunch;
	public static String DebugCoreLaunchObject_CannotTerminate;
	public static String DebugCoreLaunchObject_Terminate;
	public static String DebugCoreProvider_cannotFetchError;
	public static String DebugCoreProvider_delete;
	public static String DebugCoreProvider_deleteHint;
	public static String DebugCoreProvider_FailedLookup;
	public static String EditAction_Edit;
	public static String FileLogger_FailedAppend;
	public static String LaunchAction_FailedFetchLaunchDelegates;
	public static String LaunchObject_ErrorNoId;
	public static String LaunchObjectFavoriteContainerModel_Favorites;
	public static String LaunchView_Refresh;
	public static String LaunchView_Reset;
	public static String LaunchView_TerminateAll;
	public static String RelaunchAction_TerminateRelaunch;
	public static String StandaloneLaunchConfigExecutor_FailedLaunching;
	public static String StandaloneLaunchConfigExecutor_Launch;
	public static String StandaloneLaunchConfigExecutor_Launching;
	public static String StandaloneLaunchConfigExecutor_Timeout;
	public static String StandaloneLaunchConfigExecutor_Waiting;
	public static String TerminateAction_Terminate;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, LaunchViewMessages.class);
	}

	private LaunchViewMessages() {
	}
}
