/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets;

public interface ICheatSheetResource {

	// Empty string used through out the plugin
	public static final String EMPTY_STRING = ""; //$NON-NLS-1$

	//
	// ID used through out the cheatsheets:
	//		plugin, menu, view
	//
	public static final String CHEAT_SHEET_PLUGIN_ID = "org.eclipse.ui.cheatsheets"; //$NON-NLS-1$
	public static final String CHEAT_SHEET_VIEW_ID = "org.eclipse.ui.cheatsheets.views.CheatSheetView"; //$NON-NLS-1$
	public static final String CHEAT_SHEET_RESOURCE_ID = "org.eclipse.ui.internal.cheatsheets.CheatsheetPluginResources"; //$NON-NLS-1$

	// Memento info
	public static final String MEMENTO = "cheatSheetMemento"; //$NON-NLS-1$
	public static final String MEMENTO_ID = "id"; //$NON-NLS-1$
	public static final String MEMENTO_NAME = "name"; //$NON-NLS-1$
	public static final String MEMENTO_URL = "url"; //$NON-NLS-1$

	public static final String TRIGGER_POINT_ID = "org.eclipse.ui.cheatsheetSelectionDialog"; //$NON-NLS-1$


	//
	// Constants used to retrieve images from the cheatsheet image registry.
	//
	public static final String CHEATSHEET_OBJ = "CHEATSHEET_OBJ";//$NON-NLS-1$
	public static final String COMPOSITE_OBJ = "COMPOSITE_OBJ";//$NON-NLS-1$
	public static final String CHEATSHEET_ITEM_SKIP = "CHEATSHEET_ITEM_SKIP"; //$NON-NLS-1$
	public static final String CHEATSHEET_ITEM_COMPLETE = "CHEATSHEET_ITEM_COMPLETE"; //$NON-NLS-1$
	public static final String CHEATSHEET_ITEM_HELP = "CHEATSHEET_ITEM_HELP"; //$NON-NLS-1$
	public static final String CHEATSHEET_START = "CHEATSHEET_START"; //$NON-NLS-1$
	public static final String CHEATSHEET_RESTART = "CHEATSHEET_RESTART"; //$NON-NLS-1$
	public static final String CHEATSHEET_ITEM_BUTTON_START = "CHEATSHEET_ITEM_BUTTON_START"; //$NON-NLS-1$
	public static final String CHEATSHEET_ITEM_BUTTON_SKIP = "CHEATSHEET_ITEM_BUTTON_SKIP"; //$NON-NLS-1$
	public static final String CHEATSHEET_ITEM_BUTTON_COMPLETE = "CHEATSHEET_ITEM_BUTTON_COMPLETE"; //$NON-NLS-1$
	public static final String CHEATSHEET_ITEM_BUTTON_RESTART = "CHEATSHEET_ITEM_BUTTON_RESTART"; //$NON-NLS-1$
	public static final String COMPOSITE_TASK_START = "COMPOSITE_TASK_START"; //$NON-NLS-1$
	public static final String COMPOSITE_TASK_SKIP = "COMPOSITE_TASK_SKIP"; //$NON-NLS-1$
	public static final String COMPOSITE_TASK_REVIEW = "COMPOSITE_TASK_REVIEW"; //$NON-NLS-1$
	public static final String COMPOSITE_GOTO_TASK = "COMPOSITE_GOTO_TASK"; //$NON-NLS-1$
	public static final String COMPOSITE_RESTART_ALL = "COMPOSITE_RESTART_ALL"; //$NON-NLS-1$
	public static final String CHEATSHEET_RETURN = "CHEATSHEET_RETURN"; //$NON-NLS-1$
	public static final String CHEATSHEET_VIEW = "CHEATSHEET_VIEW"; //$NON-NLS-1$
	public static final String WARNING ="WARNING"; //$NON-NLS-1$
	public static final String ERROR ="ERROR"; //$NON-NLS-1$
	public static final String INFORMATION = "INFORMATION"; //$NON-NLS-1$
}

