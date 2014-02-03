/*******************************************************************************
 * Copyright (c) 2004, 2012 John-Mason P. Shackelford and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     John-Mason P. Shackelford - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.editor.formatter;

import org.eclipse.ant.internal.ui.editor.formatter.FormattingPreferences;
import org.eclipse.ant.internal.ui.editor.formatter.XmlDocumentFormatter;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;

public class XmlDocumentFormatterTest extends AbstractAntUITest {

    public XmlDocumentFormatterTest(String name) {
        super(name);
    }

    /**
     * General Test 
     */
    public final void testGeneralFormat() throws Exception {
        FormattingPreferences prefs = new FormattingPreferences(){
            public int getTabWidth() {                
                return 3;
            }
            public boolean useSpacesInsteadOfTabs() {
                return true;
            }
        };
        simpleTest("formatTest_source01.xml","formatTest_target01.xml",prefs);        
    }

    /**
     * Insure that tab width is not hard coded
     */
    public final void testTabWidth() throws Exception {
        FormattingPreferences prefs = new FormattingPreferences(){
            public int getTabWidth() {                
                return 7;
            }
            public boolean useSpacesInsteadOfTabs() {
                return true;
            }
        };
        simpleTest("formatTest_source01.xml","formatTest_target02.xml",prefs);        
    }

    
    /**
     * Test with tab characters instead of spaces.
     */
    public final void testTabsInsteadOfSpaces() throws Exception {
        FormattingPreferences prefs = new FormattingPreferences(){
            public int getTabWidth() {                
                return 3;
            }
            public boolean useSpacesInsteadOfTabs() {
                return false;
            }
        };
        simpleTest("formatTest_source01.xml","formatTest_target03.xml",prefs);        
    }
    
    /**
     * @param sourceFileName - file to format
     * @param targetFileName - the source file after a properly executed format
     * @param prefs - given the included preference instructions
     * @throws Exception
     */
    private void simpleTest(String sourceFileName, String targetFileName, FormattingPreferences prefs) throws Exception {
        
        XmlDocumentFormatter xmlFormatter = new XmlDocumentFormatter();
        xmlFormatter.setDefaultLineDelimiter(System.getProperty("line.separator"));
        String result = xmlFormatter.format(getFileContentAsString(getBuildFile(sourceFileName)),prefs);
        String expectedResult = getFileContentAsString(getBuildFile(targetFileName));
        
        assertEquals(expectedResult, result);
    }
}