/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.ui.externaltools.internal.model;


/**
 * Help context ids for the external tools.
 * <p>
 * This interface contains constants only; it is not intended to be implemented
 * or extended.
 * </p>
 */
public interface IExternalToolsHelpContextIds {
	String PREFIX = "org.eclipse.ui.externaltools."; //$NON-NLS-1$

	// Preference Pages
	String EXTERNAL_TOOLS_PREFERENCE_PAGE = PREFIX + "preference_page_context";  //$NON-NLS-1$

	// Property Pages
	String EXTERNAL_TOOLS_BUILDER_PROPERTY_PAGE = PREFIX + "builder_property_page_context"; //$NON-NLS-1$

	//Dialogs
	String MESSAGE_WITH_TOGGLE_DIALOG = PREFIX + "message_with_toggle_dialog_context"; //$NON-NLS-1$
	String FILE_SELECTION_DIALOG = PREFIX + "file_selection_dialog_context"; //$NON-NLS-1$

	//Launch configuration dialog tabs
	String EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_BUILDER_TAB = PREFIX + "builders_tab_context"; //$NON-NLS-1$
	String EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_PROGRAM_MAIN_TAB = PREFIX + "program_main_tab_context"; //$NON-NLS-1$
	String EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_BUILD_TAB = PREFIX + "build_tab_context"; //$NON-NLS-1$
	String EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_REFRESH_TAB = PREFIX + "refresh_tab_context"; //$NON-NLS-1$
	String EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_ENVIRONMENT_TAB = PREFIX + "environment_tab_context"; //$NON-NLS-1$
	String EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_COMMON_TAB = PREFIX + "common_tab_context"; //$NON-NLS-1$
}
