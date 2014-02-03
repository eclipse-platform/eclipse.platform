/*******************************************************************************
 *  Copyright (c) 2004, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.testplugin;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.ant.tests.ui.editor.performance.OpenAntEditorTest;
import org.eclipse.ant.tests.ui.performance.SeparateVMTests;

/**
 * Performance Test suite for the Ant UI.
 * All of the tests in this suite rely on the setup that occurs in the 
 * ProjectCreationDecorator suite. It must always run before any of the other
 * test suites.
 */
public class AntUIPerformanceTests extends TestSuite {

    public static Test suite() {

        TestSuite suite = new AntUIPerformanceTests();
        suite.setName("Ant UI Performance Unit Tests");
		suite.addTest(new TestSuite(OpenAntEditorTest.class));
		suite.addTest(new TestSuite(SeparateVMTests.class));
		//suite.addTest(new TestSuite(NonInitialTypingTest.class));
		//suite.addTest(new TestSuite(OpenLaunchConfigurationDialogTests.class));
        return suite;
    }
}
