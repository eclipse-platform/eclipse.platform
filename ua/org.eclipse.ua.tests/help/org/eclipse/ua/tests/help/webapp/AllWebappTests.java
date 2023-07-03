/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.webapp;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
 * Tests utility classes and servlets used in Web Application
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	BrowserIdentificationTest.class,
	TopicPathTest.class,
	FilterTest.class,
	PluginsRootReplacement.class,
	UrlCoderTest.class,
	UrlUtilsTests.class,
	LocaleTest.class,
	PrintSubtopics.class,
	RestrictedTopicParameter.class,
	FilterExtensionTest.class,
	FragmentServletTest.class,
	HelpServerInterrupt.class,
	HelpServerBinding.class,
	HtmlCoderTest.class,
	TocZipTest.class,
	JsonHelperTests.class,
	EclipseConnectorTests.class
})
public class AllWebappTests {
}
