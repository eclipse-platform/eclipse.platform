/*******************************************************************************
 *  Copyright (c) 2004, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.testplugin;

import org.eclipse.ant.tests.ui.editor.performance.OpenAntEditorTest;
import org.eclipse.ant.tests.ui.performance.OpenLaunchConfigurationDialogTests;
import org.eclipse.ant.tests.ui.performance.SeparateVMTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Performance Test suite for the Ant UI. All of the tests in this suite rely on
 * the setup that occurs in the ProjectCreationDecorator suite. It must always
 * run before any of the other test suites.
 */
@Suite
@SelectClasses({ OpenAntEditorTest.class, OpenLaunchConfigurationDialogTests.class, SeparateVMTests.class })
public class AntUIPerformanceTests {
	//
}
