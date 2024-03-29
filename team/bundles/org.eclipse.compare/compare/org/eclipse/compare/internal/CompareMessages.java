/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Matt McCutchen (hashproduct+eclipse@gmail.com) - Bug 35390 Three-way compare cannot select (mis-selects) )ancestor resource
 *     Aleksandra Wozniak (aleksandra.k.wozniak@gmail.com) - Bug 239959, Bug 73923
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.osgi.util.NLS;

public final class CompareMessages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.compare.internal.CompareMessages";//$NON-NLS-1$

	private CompareMessages() {
		// Do not instantiate
	}

	public static String CompareContainer_0;
	public static String CompareDialog_commit_button;
	public static String CompareDialog_error_message;
	public static String CompareDialog_error_title;
	public static String CompareEditor_0;
	public static String CompareEditor_1;
	public static String CompareEditor_2;
	public static String DocumentMerger_0;
	public static String DocumentMerger_1;
	public static String DocumentMerger_2;
	public static String DocumentMerger_3;
	public static String CompareEditorInput_0;
	public static String ComparePlugin_internal_error;
	public static String ComparePreferencePage_0;
	public static String ComparePreferencePage_1;
	public static String ComparePreferencePage_2;
	public static String ComparePreferencePage_3;
	public static String ComparePreferencePage_4;
	public static String CompareUIPlugin_0;
	public static String CompareUIPlugin_1;
	public static String ContentMergeViewer_resource_changed_description;
	public static String ContentMergeViewer_resource_changed_title;
	public static String ExceptionDialog_seeErrorLogMessage;
	public static String CompareViewerSwitchingPane_Titleformat;
	public static String NavigationEndDialog_0;
	public static String NavigationEndDialog_1;
	public static String ShowWhitespaceAction_0;
	public static String StructureDiffViewer_0;
	public static String StructureDiffViewer_1;
	public static String StructureDiffViewer_2;
	public static String StructureDiffViewer_3;
	public static String StructureDiffViewer_NoStructuralDifferences;
	public static String StructureDiffViewer_StructureError;
	public static String TextMergeViewer_0;
	public static String TextMergeViewer_1;
	public static String TextMergeViewer_10;
	public static String TextMergeViewer_11;
	public static String TextMergeViewer_12;
	public static String TextMergeViewer_13;
	public static String TextMergeViewer_14;
	public static String TextMergeViewer_15;
	public static String TextMergeViewer_16;
	public static String TextMergeViewer_17;
	public static String TextMergeViewer_2;
	public static String TextMergeViewer_3;
	public static String TextMergeViewer_4;
	public static String TextMergeViewer_5;
	public static String TextMergeViewer_6;
	public static String TextMergeViewer_7;
	public static String TextMergeViewer_8;
	public static String TextMergeViewer_9;
	public static String TextMergeViewer_accessible_ancestor;
	public static String TextMergeViewer_accessible_left;
	public static String TextMergeViewer_accessible_right;
	public static String TextMergeViewer_cursorPosition_format;
	public static String TextMergeViewer_beforeLine_format;
	public static String TextMergeViewer_range_format;
	public static String TextMergeViewer_changeType_addition;
	public static String TextMergeViewer_changeType_deletion;
	public static String TextMergeViewer_changeType_change;
	public static String TextMergeViewer_direction_outgoing;
	public static String TextMergeViewer_direction_incoming;
	public static String TextMergeViewer_direction_conflicting;
	public static String TextMergeViewer_diffType_format;
	public static String TextMergeViewer_diffDescription_noDiff_format;
	public static String TextMergeViewer_diffDescription_diff_format;
	public static String TextMergeViewer_statusLine_format;
	public static String TextMergeViewer_atEnd_title;
	public static String TextMergeViewer_atEnd_message;
	public static String TextMergeViewer_atBeginning_title;
	public static String TextMergeViewer_atBeginning_message;
	public static String TextMergeViewer_differences;
	public static String CompareNavigator_atEnd_title;
	public static String CompareNavigator_atEnd_message;
	public static String CompareNavigator_atBeginning_title;
	public static String CompareNavigator_atBeginning_message;
	public static String WorkerJob_0;
	public static String SelectAncestorDialog_title;
	public static String SelectAncestorDialog_message;
	public static String SelectAncestorDialog_option;
	public static String CompareWithOtherResourceDialog_ancestor;
	public static String CompareWithOtherResourceDialog_rightPanel;
	public static String CompareWithOtherResourceDialog_leftPanel;
	public static String CompareWithOtherResourceDialog_dialogTitle;
	public static String CompareWithOtherResourceDialog_dialogMessage;
	public static String CompareWithOtherResourceDialog_error_not_comparable;
	public static String CompareWithOtherResourceDialog_error_empty;
	public static String CompareWithOtherResourceDialog_clear;
	public static String CompareWithOtherResourceDialog_info;
	public static String CompareWithOtherResourceDialog_externalFile_errorTitle;
	public static String CompareWithOtherResourceDialog_externalFile_errorMessage;
	public static String CompareWithOtherResourceDialog_externalFileMainButton;
	public static String CompareWithOtherResourceDialog_externalFileRadioButton;
	public static String CompareWithOtherResourceDialog_externalFolderMainButton;
	public static String CompareWithOtherResourceDialog_externalFolderRadioButton;
	public static String CompareWithOtherResourceDialog_workspaceMainButton;
	public static String CompareWithOtherResourceDialog_workspaceRadioButton;
	public static String CompareContentViewerSwitchingPane_defaultViewer;
	public static String CompareContentViewerSwitchingPane_switchButtonTooltip;
	public static String CompareContentViewerSwitchingPane_discoveredLabel;
	public static String CompareContentViewerSwitchingPane_optimizedLinkLabel;
	public static String CompareContentViewerSwitchingPane_optimizedTooltip;
	public static String CompareStructureViewerSwitchingPane_defaultViewer;
	public static String CompareStructureViewerSwitchingPane_switchButtonTooltip;
	public static String CompareStructureViewerSwitchingPane_discoveredLabel;

	public static String ReaderCreator_fileIsNotAccessible;

	static {
		NLS.initializeMessages(BUNDLE_NAME, CompareMessages.class);
	}
}
