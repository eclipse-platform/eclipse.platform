/*******************************************************************************
 * Copyright (c) 2004, 2012 John-Mason P. Shackelford and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     John-Mason P. Shackelford - initial API and implementation
 *     IBM Corporation - bug 84342
 *******************************************************************************/
package org.eclipse.ant.tests.ui.editor.formatter;

import junit.framework.TestCase;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.editor.formatter.FormattingPreferences;
import org.eclipse.ant.internal.ui.editor.formatter.XmlFormatter;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.core.runtime.Preferences;

public class XmlFormatterTest extends TestCase {

    public final void testFormatUsingPreferenceStore() {
        Preferences prefs = AntUIPlugin.getDefault().getPluginPreferences();
        prefs.setValue(AntEditorPreferenceConstants.FORMATTER_WRAP_LONG, true);
        prefs.setValue(AntEditorPreferenceConstants.FORMATTER_MAX_LINE_LENGTH, 40);
        prefs.setValue(AntEditorPreferenceConstants.FORMATTER_ALIGN, false);
        prefs.setValue(AntEditorPreferenceConstants.FORMATTER_TAB_CHAR, true);
        prefs.setValue(AntEditorPreferenceConstants.FORMATTER_TAB_SIZE, 4);
        String lineSep= System.getProperty("line.separator");
        String xmlDoc = "<project default=\"go\"><target name=\"go\" description=\"Demonstrate the wrapping of long tags.\"><echo>hi</echo></target></project>";
        String formattedDoc = XmlFormatter.format(xmlDoc);
        String expected = "<project default=\"go\">" + lineSep + "\t<target name=\"go\"" + lineSep + "\t        description=\"Demonstrate the wrapping of long tags.\">" + lineSep + "\t\t<echo>hi</echo>" + lineSep + "\t</target>" + lineSep + "</project>";
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
        String lineSep= System.getProperty("line.separator");
        String xmlDoc = "<project default=\"go\"><target name=\"go\" description=\"Demonstrate the wrapping of long tags.\"><echo>hi</echo></target></project>";
        String formattedDoc = XmlFormatter.format(xmlDoc, prefs);
        String expected = "<project default=\"go\">" + lineSep + "      <target name=\"go\"" + lineSep + "              description=\"Demonstrate the wrapping of long tags.\">" + lineSep + "            <echo>hi</echo>" + lineSep + "      </target>" + lineSep + "</project>";
        assertEquals(expected, formattedDoc);
    }
    
    /**
     * Bug 84342
     */
    public final void testFormatMaintainingLineSeparators() {
        FormattingPreferences prefs = new FormattingPreferences() {
            public boolean wrapLongTags() { return true;}
            public int getMaximumLineWidth() { return 40;}
            public boolean alignElementCloseChar() { return false;}
            public boolean useSpacesInsteadOfTabs() { return true;}
            public int getTabWidth() { return 6;}
        };
        String lineSep= System.getProperty("line.separator");
        String xmlDoc = "<project default=\"go\"><target name=\"go\" description=\"Demonstrate the wrapping of long tags.\"><echo>hi</echo></target>" + lineSep + lineSep + "</project>";
        String formattedDoc = XmlFormatter.format(xmlDoc, prefs);
        String expected = "<project default=\"go\">" + lineSep + "      <target name=\"go\"" + lineSep + "              description=\"Demonstrate the wrapping of long tags.\">" + lineSep + "            <echo>hi</echo>" + lineSep + "      </target>" + lineSep + lineSep + "</project>";
        assertEquals(expected, formattedDoc);
    }

}
