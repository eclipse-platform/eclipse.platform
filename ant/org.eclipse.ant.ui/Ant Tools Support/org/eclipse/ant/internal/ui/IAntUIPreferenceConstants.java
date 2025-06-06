/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 *     John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 53547
 *******************************************************************************/
package org.eclipse.ant.internal.ui;

/**
 * Constants used to identify user preferences.
 */
public interface IAntUIPreferenceConstants {

	public static final String ANTVIEW_INCLUDE_ERROR_SEARCH_RESULTS = "antview.includeErrorSearchResults"; //$NON-NLS-1$
	public static final String ANTVIEW_LAST_SEARCH_STRING = "antview.lastSearchString"; //$NON-NLS-1$
	public static final String ANTVIEW_LAST_WORKINGSET_SEARCH_SCOPE = "antview.lastSearchScope"; //$NON-NLS-1$
	public static final String ANTVIEW_USE_WORKINGSET_SEARCH_SCOPE = "antview.useWorkingSetSearchScope"; //$NON-NLS-1$

	public static final String ANT_FIND_BUILD_FILE_NAMES = "ant.findBuildFileNames"; //$NON-NLS-1$

	/**
	 * The symbolic names for colors for displaying the content in the Console
	 *
	 * @see org.eclipse.jface.resource.ColorRegistry
	 */
	public static final String CONSOLE_ERROR_COLOR = "org.eclipse.ant.ui.errorColor"; //$NON-NLS-1$
	public static final String CONSOLE_WARNING_COLOR = "org.eclipse.ant.ui.warningColor"; //$NON-NLS-1$
	public static final String CONSOLE_INFO_COLOR = "org.eclipse.ant.ui.informationColor"; //$NON-NLS-1$
	public static final String CONSOLE_VERBOSE_COLOR = "org.eclipse.ant.ui.verboseColor"; //$NON-NLS-1$
	public static final String CONSOLE_DEBUG_COLOR = "org.eclipse.ant.ui.debugColor"; //$NON-NLS-1$

	public static final String ANT_TOOLS_JAR_WARNING = "toolsJAR"; //$NON-NLS-1$

	public static final String ANT_ERROR_DIALOG = "errorDialog"; //$NON-NLS-1$

	/**
	 * Boolean preference identifier constant which specifies whether to create Java problem markers from the javac output of Ant builds.
	 */
	public static final String ANT_CREATE_MARKERS = "createMarkers"; //$NON-NLS-1$

	/**
	 * Boolean preference identifier constant which specifies whether the Ant editor should show internal targets in the Outline.
	 */
	public static final String ANTEDITOR_FILTER_INTERNAL_TARGETS = "anteditor.filterInternalTargets"; //$NON-NLS-1$

	/**
	 * Boolean preference identifier constant which specifies whether the Ant editor should show imported elements in the Outline.
	 */
	public static final String ANTEDITOR_FILTER_IMPORTED_ELEMENTS = "anteditor.filterImportedElements"; //$NON-NLS-1$

	/**
	 * Boolean preference identifier constant which specifies whether the Ant editor should show properties in the Outline.
	 */
	public static final String ANTEDITOR_FILTER_PROPERTIES = "anteditor.filterProperties"; //$NON-NLS-1$

	/**
	 * Boolean preference identifier constant which specifies whether the Ant editor should show top level tasks/types in the Outline.
	 */
	public static final String ANTEDITOR_FILTER_TOP_LEVEL = "anteditor.filterTopLevel"; //$NON-NLS-1$

	/**
	 * Boolean preference identifier constant which specifies whether the Ant editor should sort elements in the Outline.
	 */
	public static final String ANTEDITOR_SORT = "anteditor.sort"; //$NON-NLS-1$

	/**
	 * Boolean preference identifier constant which specifies whether the Ant Outline page links its selection to the active Ant editor.
	 */
	public static final String OUTLINE_LINK_WITH_EDITOR = "outline.linkWithEditor"; //$NON-NLS-1$

	/**
	 * String preference identifier constant which specifies the URL for the location of the Ant documentation.
	 */
	public static final String DOCUMENTATION_URL = "documentation.url"; //$NON-NLS-1$

	/**
	 * Boolean preference that allows Ant to always be run in the same JRE as the workspace
	 *
	 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=135807"
	 * @since 3.7
	 */
	public static final String USE_WORKSPACE_JRE = "workspacejre"; //$NON-NLS-1$
}
