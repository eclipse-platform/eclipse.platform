/*******************************************************************************
 * Copyright (c) 2002, 2004 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial implementation
 * 	   IBM Corporation - additional tests
 *******************************************************************************/

package org.eclipse.ant.tests.ui.testplugin;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.ant.tests.ui.BuildTests;
import org.eclipse.ant.tests.ui.editor.AntEditorContentOutlineTests;
import org.eclipse.ant.tests.ui.editor.CodeCompletionTest;
import org.eclipse.ant.tests.ui.editor.TaskDescriptionProviderTest;
import org.eclipse.ant.tests.ui.editor.formatter.XmlFormatterTest;
import org.eclipse.ant.tests.ui.editor.formatter.XmlTagFormatterTest;
import org.eclipse.ant.tests.ui.editor.formatter.FormattingPreferencesTest;
import org.eclipse.ant.tests.ui.editor.formatter.XmlDocumentFormatterTest;
import org.eclipse.ant.tests.ui.externaltools.MigrationTests;
import org.eclipse.ant.tests.ui.separateVM.SeparateVMTests;

/**
 * Test suite for the Ant UI
 */
public class AntUITests extends TestSuite {

    public static Test suite() {

        TestSuite suite= new AntUITests();
        suite.setName("Ant UI Unit Tests");
		suite.addTest(new TestSuite(ProjectCreationDecorator.class));
		suite.addTest(new TestSuite(BuildTests.class));
		suite.addTest(new TestSuite(SeparateVMTests.class));
        suite.addTest(new TestSuite(CodeCompletionTest.class));
        suite.addTest(new TestSuite(TaskDescriptionProviderTest.class));
        suite.addTest(new TestSuite(AntEditorContentOutlineTests.class));
        suite.addTest(new TestSuite(MigrationTests.class));
        suite.addTest(new TestSuite(FormattingPreferencesTest.class));
        suite.addTest(new TestSuite(XmlDocumentFormatterTest.class));
        suite.addTest(new TestSuite(XmlTagFormatterTest.class));
        suite.addTest(new TestSuite(XmlFormatterTest.class));
        return suite;
    }
}
