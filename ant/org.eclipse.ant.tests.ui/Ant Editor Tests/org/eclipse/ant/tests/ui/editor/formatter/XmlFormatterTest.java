/*******************************************************************************
 * Copyright (c) 2004 John-Mason P. Shackelford and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     John-Mason P. Shackelford - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.editor.formatter;

import junit.framework.TestCase;

import org.eclipse.ant.internal.ui.editor.formatter.FormattingPreferences;
import org.eclipse.ant.internal.ui.editor.formatter.XmlFormatter;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.core.runtime.Preferences;

/**
 *  
 */
public class XmlFormatterTest extends TestCase {

    public final void testFormatUsingPreferenceStore() {
        Preferences prefs = AntUIPlugin.getDefault().getPluginPreferences();
        prefs.setValue(AntEditorPreferenceConstants.FORMATTER_WRAP_LONG, true);
        prefs.setValue(AntEditorPreferenceConstants.FORMATTER_MAX_LINE_LENGTH, 40);
        prefs.setValue(AntEditorPreferenceConstants.FORMATTER_ALIGN, false);
        prefs.setValue(AntEditorPreferenceConstants.FORMATTER_TAB_CHAR, true);
        prefs.setValue(AntEditorPreferenceConstants.FORMATTER_TAB_SIZE, 4);
        String xmlDoc = "<project default=\"go\"><target name=\"go\" description=\"Demonstrate the wrapping of long tags.\"><echo>hi</echo></target></project>";
        String formattedDoc = XmlFormatter.format(xmlDoc);
        String expected = "<project default=\"go\">\n\t<target name=\"go\"\n\t        description=\"Demonstrate the wrapping of long tags.\">\n\t\t<echo>hi</echo>\n\t</target>\n</project>";
        assertEquals(expected, formattedDoc);
    }

    public final void testFormatWithPreferenceParameter() {
        FormattingPreferences prefs = new FormattingPreferences() {
            public boolean wrapLongTags() { return true;}
            public int getMaximumLineWidth() { return 40;}
            public boolean alignElementCloseChar() { return false;}
            public boolean useSpacesInsteadOfTabs() { return true;}
            public int getTabWidth() { return 6;}
        };
        String xmlDoc = "<project default=\"go\"><target name=\"go\" description=\"Demonstrate the wrapping of long tags.\"><echo>hi</echo></target></project>";
        String formattedDoc = XmlFormatter.format(xmlDoc, prefs);
        String expected = "<project default=\"go\">\n      <target name=\"go\"\n              description=\"Demonstrate the wrapping of long tags.\">\n            <echo>hi</echo>\n      </target>\n</project>";
        assertEquals(expected, formattedDoc);
    }

}
