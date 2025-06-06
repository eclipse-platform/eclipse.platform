/*******************************************************************************
 * Copyright (c) 2004, 2011 John-Mason P. Shackelford and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     John-Mason P. Shackelford - initial API and implementation
 * 	   IBM Corporation - bug 52076
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor.formatter;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;

public class FormattingPreferences {

	IPreferenceStore fPrefs = AntUIPlugin.getDefault().getPreferenceStore();

	public String getCanonicalIndent() {
		String canonicalIndent;
		if (!useSpacesInsteadOfTabs()) {
			canonicalIndent = "\t"; //$NON-NLS-1$
		} else {
			String tab = IAntCoreConstants.EMPTY_STRING;
			for (int i = 0; i < getTabWidth(); i++) {
				tab = tab.concat(" "); //$NON-NLS-1$
			}
			canonicalIndent = tab;
		}

		return canonicalIndent;
	}

	public int getMaximumLineWidth() {
		return fPrefs.getInt(AntEditorPreferenceConstants.FORMATTER_MAX_LINE_LENGTH);
	}

	public boolean wrapLongTags() {
		return fPrefs.getBoolean(AntEditorPreferenceConstants.FORMATTER_WRAP_LONG);
	}

	public boolean alignElementCloseChar() {
		return fPrefs.getBoolean(AntEditorPreferenceConstants.FORMATTER_ALIGN);
	}

	public int getTabWidth() {
		return fPrefs.getInt(AntEditorPreferenceConstants.FORMATTER_TAB_SIZE);
	}

	public boolean useSpacesInsteadOfTabs() {
		return !fPrefs.getBoolean(AntEditorPreferenceConstants.FORMATTER_TAB_CHAR);
	}

	public static boolean affectsFormatting(PropertyChangeEvent event) {
		String property = event.getProperty();
		return property.startsWith(AntEditorPreferenceConstants.FORMATTER_ALIGN)
				|| property.startsWith(AntEditorPreferenceConstants.FORMATTER_MAX_LINE_LENGTH)
				|| property.startsWith(AntEditorPreferenceConstants.FORMATTER_TAB_CHAR)
				|| property.startsWith(AntEditorPreferenceConstants.FORMATTER_TAB_SIZE)
				|| property.startsWith(AntEditorPreferenceConstants.FORMATTER_WRAP_LONG);
	}

	/**
	 * Sets the preference store for these formatting preferences.
	 *
	 * @param prefs
	 *            the preference store to use as a reference for the formatting preferences
	 */
	public void setPreferenceStore(IPreferenceStore prefs) {
		fPrefs = prefs;
	}
}