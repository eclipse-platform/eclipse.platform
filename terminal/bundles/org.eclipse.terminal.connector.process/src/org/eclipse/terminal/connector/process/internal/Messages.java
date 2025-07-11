/*******************************************************************************
 * Copyright (c) 2011 - 2018 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.terminal.connector.process.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Process terminal connector plug-in externalized strings management.
 */
public class Messages extends NLS {

	static {
		// Load message values from bundle file
		NLS.initializeMessages(Messages.class.getName(), Messages.class);
	}

	// **** Declare externalized string id's down here *****

	public static String ProcessConnector_error_title;
	public static String ProcessConnector_error_creatingProcess;
	public static String ProcessSettingsPage_dialogTitle;
	public static String ProcessSettingsPage_processImagePathSelectorControl_label;
	public static String ProcessSettingsPage_processImagePathSelectorControl_button;
	public static String ProcessSettingsPage_processArgumentsControl_label;
	public static String ProcessSettingsPage_processWorkingDirControl_label;
	public static String ProcessSettingsPage_localEchoSelectorControl_label;
}
