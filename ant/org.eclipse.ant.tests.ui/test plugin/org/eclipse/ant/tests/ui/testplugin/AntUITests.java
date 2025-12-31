/*******************************************************************************
 * Copyright (c) 2002, 2015 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH,
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial implementation
 * 	   IBM Corporation - additional tests
 *******************************************************************************/

package org.eclipse.ant.tests.ui.testplugin;

import org.eclipse.ant.tests.ui.APITests;
import org.eclipse.ant.tests.ui.AntUtilTests;
import org.eclipse.ant.tests.ui.AntViewTests;
import org.eclipse.ant.tests.ui.BuildTests;
import org.eclipse.ant.tests.ui.ModelProjectTests;
import org.eclipse.ant.tests.ui.debug.BreakpointTests;
import org.eclipse.ant.tests.ui.debug.PropertyTests;
import org.eclipse.ant.tests.ui.debug.RunToLineTests;
import org.eclipse.ant.tests.ui.debug.StackTests;
import org.eclipse.ant.tests.ui.debug.SteppingTests;
import org.eclipse.ant.tests.ui.editor.AntEditorContentOutlineTests;
import org.eclipse.ant.tests.ui.editor.AntEditorTests;
import org.eclipse.ant.tests.ui.editor.CodeCompletionTest;
import org.eclipse.ant.tests.ui.editor.OccurrencesFinderTests;
import org.eclipse.ant.tests.ui.editor.TaskDescriptionProviderTest;
import org.eclipse.ant.tests.ui.editor.formatter.FormattingPreferencesTest;
import org.eclipse.ant.tests.ui.editor.formatter.XmlDocumentFormatterTest;
import org.eclipse.ant.tests.ui.editor.formatter.XmlFormatterTest;
import org.eclipse.ant.tests.ui.editor.formatter.XmlTagFormatterTest;
import org.eclipse.ant.tests.ui.externaltools.BuilderCoreUtilsTests;
import org.eclipse.ant.tests.ui.externaltools.MigrationTests;
import org.eclipse.ant.tests.ui.separateVM.SeparateVMTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for the Ant UI
 */
@Suite
@SelectClasses({ //
		BuildTests.class, //
		SeparateVMTests.class, //
		AntEditorTests.class, //
		CodeCompletionTest.class, //
		TaskDescriptionProviderTest.class, //
		AntEditorContentOutlineTests.class, //
		MigrationTests.class, //
		BuilderCoreUtilsTests.class, //
		ModelProjectTests.class, //
		FormattingPreferencesTest.class, //
		XmlDocumentFormatterTest.class, //
		XmlTagFormatterTest.class, //
		XmlFormatterTest.class, //
		AntUtilTests.class, //
		AntViewTests.class, //
		BreakpointTests.class, //
		RunToLineTests.class, //
		SteppingTests.class, //
		PropertyTests.class, //
		OccurrencesFinderTests.class, //
		StackTests.class, //
		APITests.class, //
})
public class AntUITests {
	//
}
