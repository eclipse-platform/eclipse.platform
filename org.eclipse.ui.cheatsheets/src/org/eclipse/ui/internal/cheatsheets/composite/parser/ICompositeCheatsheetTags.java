/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.composite.parser;

/**
 * Interface containing the constants used by the cheatsheet parser
 * to identify the tags used in a composite cheatsheet file.
 */

public interface ICompositeCheatsheetTags {

	// Elements and attributes
	public static final String COMPOSITE_CHEATSHEET= "compositeCheatsheet"; //$NON-NLS-1$
	public static final String COMPOSITE_CHEATSHEET_STATE= "compositeCheatSheetState"; //$NON-NLS-1$
	public static final String TASK = "task"; //$NON-NLS-1$
	public static final String TASK_GROUP = "taskGroup"; //$NON-NLS-1$
	public static final String EXPLORER = "explorer"; //$NON-NLS-1$
	public static final String NAME = "name"; //$NON-NLS-1$
	public static final String VALUE = "value"; //$NON-NLS-1$
	public static final String KIND = "kind"; //$NON-NLS-1$
	public static final String ON_COMPLETION = "onCompletion"; //$NON-NLS-1$
	public static final String DEPENDS_ON = "dependsOn"; //$NON-NLS-1$
	public static final String STATE = "state"; //$NON-NLS-1$

	// Attribute values
	public static final String TREE = "tree"; //$NON-NLS-1$

	// Cheatsheet task parameters
	public static final String CHEATSHEET_TASK_KIND = "cheatsheet"; //$NON-NLS-1$
	public static final String CHEATSHEET_TASK_ID = "id"; //$NON-NLS-1$
	public static final String CHEATSHEET_TASK_PATH = "path"; //$NON-NLS-1$
	public static final String CHEATSHEET_TASK_SHOW_INTRO = "showIntro"; //$NON-NLS-1$

	// Tags used in Memento
	public static final String TASK_DATA = "taskData"; //$NON-NLS-1$
	public static final String CHEAT_SHEET_MANAGER = "cheatSheetManager"; //$NON-NLS-1$
	public static final String KEY = "key"; //$NON-NLS-1$
	public static final String TASK_ID = "id"; //$NON-NLS-1$
	public static final String SELECTED_TASK = "selectedTask"; //$NON-NLS-1$
	public static final String LAYOUT_DATA = "layout"; //$NON-NLS-1$

}
