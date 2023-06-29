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
 *     IBM - Initial API and implementation
 *     Pawel Piech - Wind River - adapted to use in Debug view
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.launch;

import org.eclipse.osgi.util.NLS;

public class LaunchViewMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.debug.internal.ui.views.launch.LaunchViewMessages";//$NON-NLS-1$

	public static String Breadcrumb_NoActiveContext;
	public static String LaunchView_ViewModeMenu_label;
	public static String DebugViewModeAction_Auto_label;
	public static String DebugViewModeAction_Auto_tooltip;
	public static String DebugViewModeAction_Auto_description;
	public static String DebugViewModeAction_Full_label;
	public static String DebugViewModeAction_Full_tooltip;
	public static String DebugViewModeAction_Full_description;
	public static String DebugViewModeAction_Compact_label;
	public static String DebugViewModeAction_Compact_tooltip;
	public static String DebugViewModeAction_Compact_description;

	public static String BreadcrumbDropDownAutoExpandAction_label;
	public static String BreadcrumbDropDownAutoExpandAction_tooltip;
	public static String BreadcrumbDropDownAutoExpandAction_description;

	public static String DebugToolBarAction_View_label;
	public static String DebugToolBarAction_View_tooltip;
	public static String DebugToolBarAction_View_description;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, LaunchViewMessages.class);
	}
}
