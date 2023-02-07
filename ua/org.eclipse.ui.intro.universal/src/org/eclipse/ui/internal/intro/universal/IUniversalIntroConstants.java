/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.universal;


public interface IUniversalIntroConstants {

	// all attributes here are by default public static final.

	// General consts.
	// ---------------
	String PLUGIN_ID = "org.eclipse.ui.intro.universal"; //$NON-NLS-1$
	String PREFIX = PLUGIN_ID + "."; //$NON-NLS-1$

	// Empty Standby Content part. This is registered in this plugin,
	// through markup.
	String EMPTY_STANDBY_CONTENT_PART = "org.eclipse.ui.intro.config.emptyStandby"; //$NON-NLS-1$

	// Memento constants
	// ------------------
	String MEMENTO_PRESENTATION_TAG = "presentation"; //$NON-NLS-1$
	String MEMENTO_CURRENT_PAGE_ATT = "currentPage"; //$NON-NLS-1$
	String MEMENTO_STANDBY_PART_TAG = "standbyPart"; //$NON-NLS-1$
	String MEMENTO_STANDBY_CONTENT_PART_ID_ATT = "contentPartID"; //$NON-NLS-1$
	String MEMENTO_STANDBY_CONTENT_PART_TAG = "standbyContentPart"; //$NON-NLS-1$
	String MEMENTO_RESTORE_ATT = "restore"; //$NON-NLS-1$

	// CustomizableIntroPart consts:
	// -----------------------------
	// key to retrieve if a standby part is needed.
	String SHOW_STANDBY_PART = "showStandbyPart"; //$NON-NLS-1$

	// Form implementation consts:
	// ---------------------------
	// key to retrieve the into link model object from imageHyperlink widget.
	// convention: actual string value is class name.
	String INTRO_LINK = "IntroLink"; //$NON-NLS-1$

	// key to retrive page sub-title from PageContentForm
	String PAGE_SUBTITLE = "PageSubtitle"; //$NON-NLS-1$


	// Performance keys
	// -----------------------
	String INTRO = "intro"; //$NON-NLS-1$
	String PERF_VIEW_CREATION_TIME = PLUGIN_ID + "/perf/createView"; //$NON-NLS-1$
	String PERF_SET_STANDBY_STATE = PLUGIN_ID + "/perf/setStandbyState"; //$NON-NLS-1$
	// not exposed in .option. Used because framework is convenient.
	String PERF_UI_ZOOM = PLUGIN_ID + "/perf/uiZoom"; //$NON-NLS-1$

	// Universal Welcome

	String LOW = "low"; //$NON-NLS-1$
	String MEDIUM = "medium"; //$NON-NLS-1$
	String HIGH = "high"; //$NON-NLS-1$
	String CALLOUT = "callout"; //$NON-NLS-1$
	String HIDDEN = "hidden"; //$NON-NLS-1$
	String NEW = "new"; //$NON-NLS-1$

	String STYLE_LOW = "importance-low"; //$NON-NLS-1$
	String STYLE_MEDIUM = "importance-medium"; //$NON-NLS-1$
	String STYLE_HIGH = "importance-high"; //$NON-NLS-1$
	String STYLE_CALLOUT = "importance-callout"; //$NON-NLS-1$
	String STYLE_NEW = "importance-new"; //$NON-NLS-1$
	String DEFAULT_ANCHOR = "defaultAnchor"; //$NON-NLS-1$
	String NEW_CONTENT_ANCHOR = "newContentAnchor"; //$NON-NLS-1$
	String DEFAULT_CONTENT_PATH = "/page-content/bottom-left/"+DEFAULT_ANCHOR; //$NON-NLS-1$
	String NEW_CONTENT_PATH = "/page-content/top-left/"+NEW_CONTENT_ANCHOR; //$NON-NLS-1$
	// Page ids
	String ID_ROOT = "root"; //$NON-NLS-1$
	String ID_STANDBY = "standby"; //$NON-NLS-1$
	String ID_OVERVIEW = "overview";//$NON-NLS-1$
	String ID_TUTORIALS = "tutorials";//$NON-NLS-1$
	String ID_SAMPLES = "samples";//$NON-NLS-1$
	String ID_FIRSTSTEPS = "firststeps";//$NON-NLS-1$
	String ID_WHATSNEW = "whatsnew";//$NON-NLS-1$
	String ID_MIGRATE = "migrate";//$NON-NLS-1$
	String ID_WEBRESOURCES = "webresources";//$NON-NLS-1$
	String ID_WORKBENCH = "workbench"; //$NON-NLS-1$

	// Page DIV ids
	String DIV_PAGE_LINKS = "page-links"; //$NON-NLS-1$
	String DIV_ACTION_LINKS = "action-links"; //$NON-NLS-1$
	String DIV_LAYOUT_TOP_LEFT = "top-left";  //$NON-NLS-1$
	String DIV_LAYOUT_TOP_RIGHT = "top-right"; //$NON-NLS-1$
	String DIV_LAYOUT_BOTTOM_LEFT = "bottom-left"; //$NON-NLS-1$
	String DIV_LAYOUT_BOTTOM_RIGHT = "bottom-right"; //$NON-NLS-1$

	// Product intro variables
	String VAR_INTRO_BACKGROUND_IMAGE = "INTRO_BACKGROUND_IMAGE"; //$NON-NLS-1$
	String VAR_INTRO_ROOT_PAGES = "INTRO_ROOT_PAGES"; //$NON-NLS-1$
	String VAR_INTRO_DATA = "INTRO_DATA";  //$NON-NLS-1$
	String VAR_WORKBENCH_AS_ROOT_LINK="workbenchAsRootLink"; //$NON-NLS-1$
	String VAR_INTRO_DESCRIPTION_PREFIX = "introDescription"; //$NON-NLS-1$

	//Page table properties
	String P_IMPORTANCE = "importance"; //$NON-NLS-1$
	String P_NAME = "name"; //$NON-NLS-1$

	//Theme property constants
	String LAUNCHBAR_OVERVIEW_ICON = "launchbarOverviewIcon"; //$NON-NLS-1$
	String LAUNCHBAR_FIRSTSTEPS_ICON = "launchbarFirststepsIcon"; //$NON-NLS-1$
	String LAUNCHBAR_TUTORIALS_ICON = "launchbarTutorialsIcon"; //$NON-NLS-1$
	String LAUNCHBAR_SAMPLES_ICON = "launchbarSamplesIcon"; //$NON-NLS-1$
	String LAUNCHBAR_WHATSNEW_ICON = "launchbarWhatsnewIcon"; //$NON-NLS-1$
	String LAUNCHBAR_MIGRATE_ICON = "launchbarMigrateIcon"; //$NON-NLS-1$
	String LAUNCHBAR_WEBRESOURCES_ICON = "launchbarWebresourcesIcon"; //$NON-NLS-1$
	String HIGH_CONTRAST_PREFIX = "highContrast-"; //$NON-NLS-1$
	String HIGH_CONTRAST_NAV_PREFIX = "highContrastNav-"; //$NON-NLS-1$
	String HIGH_CONTRAST = "high-contrast"; //$NON-NLS-1$
}
