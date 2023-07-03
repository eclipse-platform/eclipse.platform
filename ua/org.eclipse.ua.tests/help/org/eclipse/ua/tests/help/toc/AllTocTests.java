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
package org.eclipse.ua.tests.help.toc;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
 * Tests help table of contents functionality.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	TocAssemblerTest.class,
	EnabledTopicTest.class,
	TopicFinderTest.class,
	TocSortingTest.class,
	TopicSortingTest.class,
	TocIconTest.class,
	TocIconPathTest.class,
	TocProviderTest.class,
	HelpData.class
})
public class AllTocTests {
}
