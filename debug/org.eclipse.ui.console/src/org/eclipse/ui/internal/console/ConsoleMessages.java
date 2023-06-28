/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
 * vogella GmbH - Bug 287303 - [patch] Add Word Wrap action to Console View
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import org.eclipse.osgi.util.NLS;

public class ConsoleMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ui.internal.console.ConsoleMessages";//$NON-NLS-1$

	public static String AbstractConsole_0;

	public static String ConsoleDropDownAction_0;
	public static String ConsoleDropDownAction_1;

	public static String ConsoleManager_0;

	public static String ConsoleManager_consoleContentChangeJob;

	public static String ConsoleView_0;

	public static String PinConsoleAction_0;
	public static String PinConsoleAction_1;

	public static String ClearOutputAction_title;
	public static String ClearOutputAction_toolTipText;

	public static String TextViewerGotoLineAction_Enter_line_number__8;
	public static String TextViewerGotoLineAction_Exceptions_occurred_attempt_to_go_to_line_2;
	public static String TextViewerGotoLineAction_Go_to__Line____Ctrl_L_4;
	public static String TextViewerGotoLineAction_Go_To_Line_1;
	public static String TextViewerGotoLineAction_Line_number_out_of_range_1;
	public static String TextViewerGotoLineAction_Not_a_number_2;

	public static String ScrollLockAction_0;
	public static String ScrollLockAction_1;
	public static String WordWrapAction_0;
	public static String WordWrapAction_1;
	public static String FollowHyperlinkAction_0;
	public static String FollowHyperlinkAction_1;
	public static String OpenConsoleAction_0;
	public static String OpenConsoleAction_1;
	public static String CloseConsoleAction_0;
	public static String CloseConsoleAction_1;

	public static String TextConsolePage_SelectAllDescrip;
	public static String TextConsolePage_SelectAllText;
	public static String TextConsolePage_CutText;
	public static String TextConsolePage_CutDescrip;
	public static String TextConsolePage_CopyText;
	public static String TextConsolePage_CopyDescrip;
	public static String TextConsolePage_PasteText;
	public static String TextConsolePage_PasteDescrip;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, ConsoleMessages.class);
	}

	public static String PatternMatchListenerExtension_3;

	public static String PatternMatchListenerExtension_4;

	public static String PatternMatchListenerExtension_5;

	public static String UpdatingConsoleState;
}
