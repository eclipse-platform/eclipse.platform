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
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;

public class FormattingPreferencesTest extends AbstractAntUITest {

    public FormattingPreferencesTest(String name) {
        super(name);
    }

    public final void testGetCanonicalIndent() {
     
        FormattingPreferences prefs;
        
        // test spaces 
        prefs = new FormattingPreferences(){
            public int getTabWidth() {                
                return 3;
            }
            public boolean useSpacesInsteadOfTabs() {
                return true;
            }
        };        
        assertEquals("   ",prefs.getCanonicalIndent());
        
        // ensure the value is not hard coded
        prefs = new FormattingPreferences(){
            public int getTabWidth() {                
                return 7;
            }
            public boolean useSpacesInsteadOfTabs() {
                return true;
            }
        };        
        assertEquals("       ",prefs.getCanonicalIndent());
        
        // use tab character
        prefs = new FormattingPreferences(){
            public int getTabWidth() {                
                return 7;
            }
            public boolean useSpacesInsteadOfTabs() {
                return false;
            }
        };        
        assertEquals("\t",prefs.getCanonicalIndent());
    }
}
