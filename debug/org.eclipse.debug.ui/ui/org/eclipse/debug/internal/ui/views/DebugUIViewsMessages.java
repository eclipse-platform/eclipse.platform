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
 * IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views;

import org.eclipse.osgi.util.NLS;

public class DebugUIViewsMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.debug.internal.ui.views.DebugUIViewsMessages";//$NON-NLS-1$

	public static String LaunchView_Error_1;
	public static String LaunchView_Exception_occurred_opening_editor_for_debugger__2;
	public static String LaunchView_Terminate_and_Remove_1;
	public static String LaunchView_Terminate_and_remove_selected__2;

	public static String SourceNotFoundEditorInput_Source_Not_Found_1;
	public static String SourceNotFoundEditorInput_Source_not_found_for__0__2;

	public static String BreakpointsView_12;
	public static String BreakpointWorkingSetPage_0;
	public static String BreakpointWorkingSetPage_1;
	public static String BreakpointWorkingSetPage_2;
	public static String BreakpointWorkingSetPage_3;
	public static String BreakpointWorkingSetPage_4;
	public static String BreakpointWorkingSetPage_5;
	public static String BreakpointWorkingSetPage_6;
	public static String BreakpointWorkingSetPage_selectAll_label;
	public static String BreakpointWorkingSetPage_selectAll_toolTip;
	public static String BreakpointWorkingSetPage_deselectAll_label;
	public static String BreakpointWorkingSetPage_deselectAll_toolTip;
	public static String OtherBreakpointOrganizer_0;
	public static String WorkingSetCategory_0;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, DebugUIViewsMessages.class);
	}

	public static String InspectPopupDialog_0;

	public static String InspectPopupDialog_1;
}
