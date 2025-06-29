/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui;

import org.eclipse.ant.internal.ui.editor.text.IAntEditorColorConstants;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.editors.text.EditorsUI;

public class AntUIPreferenceInitializer extends AbstractPreferenceInitializer {

	public AntUIPreferenceInitializer() {
		super();
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore prefs = AntUIPlugin.getDefault().getPreferenceStore();
		prefs.setDefault(IAntUIPreferenceConstants.ANT_FIND_BUILD_FILE_NAMES, "build.xml"); //$NON-NLS-1$

		prefs.setDefault(IAntUIPreferenceConstants.DOCUMENTATION_URL, "http://ant.apache.org/manual"); //$NON-NLS-1$
		prefs.setDefault(IAntUIPreferenceConstants.USE_WORKSPACE_JRE, false);

		EditorsUI.useAnnotationsPreferencePage(prefs);
		EditorsUI.useQuickDiffPreferencePage(prefs);
		if (AntUIPlugin.isMacOS()) {
			// the mac does not have a tools.jar Bug 40778
			prefs.setDefault(IAntUIPreferenceConstants.ANT_TOOLS_JAR_WARNING, false);
		} else {
			prefs.setDefault(IAntUIPreferenceConstants.ANT_TOOLS_JAR_WARNING, true);
		}

		prefs.setDefault(IAntUIPreferenceConstants.ANT_ERROR_DIALOG, true);

		prefs.setDefault(IAntUIPreferenceConstants.ANTEDITOR_FILTER_INTERNAL_TARGETS, false);
		prefs.setDefault(IAntUIPreferenceConstants.ANTEDITOR_FILTER_IMPORTED_ELEMENTS, false);
		prefs.setDefault(IAntUIPreferenceConstants.ANTEDITOR_FILTER_PROPERTIES, false);
		prefs.setDefault(IAntUIPreferenceConstants.ANTEDITOR_FILTER_TOP_LEVEL, false);

		// Ant Editor color preferences
		PreferenceConverter.setDefault(prefs, IAntEditorColorConstants.TEXT_COLOR, IAntEditorColorConstants.DEFAULT);
		PreferenceConverter.setDefault(prefs, IAntEditorColorConstants.PROCESSING_INSTRUCTIONS_COLOR, IAntEditorColorConstants.PROC_INSTR);
		PreferenceConverter.setDefault(prefs, IAntEditorColorConstants.STRING_COLOR, IAntEditorColorConstants.STRING);
		PreferenceConverter.setDefault(prefs, IAntEditorColorConstants.TAG_COLOR, IAntEditorColorConstants.TAG);
		PreferenceConverter.setDefault(prefs, IAntEditorColorConstants.XML_COMMENT_COLOR, IAntEditorColorConstants.XML_COMMENT);
		PreferenceConverter.setDefault(prefs, IAntEditorColorConstants.XML_DTD_COLOR, IAntEditorColorConstants.XML_DTD);

		PreferenceConverter.setDefault(prefs, IAntUIPreferenceConstants.CONSOLE_ERROR_COLOR, new RGB(255, 0, 0)); // red - exactly the same as debug
																													// Console
		PreferenceConverter.setDefault(prefs, IAntUIPreferenceConstants.CONSOLE_WARNING_COLOR, new RGB(250, 100, 0)); // orange
		PreferenceConverter.setDefault(prefs, IAntUIPreferenceConstants.CONSOLE_INFO_COLOR, new RGB(0, 0, 255)); // blue
		PreferenceConverter.setDefault(prefs, IAntUIPreferenceConstants.CONSOLE_VERBOSE_COLOR, new RGB(0, 200, 125)); // green
		PreferenceConverter.setDefault(prefs, IAntUIPreferenceConstants.CONSOLE_DEBUG_COLOR, new RGB(0, 0, 0)); // black

		AntEditorPreferenceConstants.initializeDefaultValues(prefs);
	}
}
