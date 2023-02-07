/*******************************************************************************
 *  Copyright (c) 2005, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.other;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	LinkUtilTest.class,
	TestEscapeUtils.class,
	ContextLinkSorter.class,
	UAElementTest.class,
	ContextTest.class,
	TopicTest.class,
	TocObjectTest.class,
	DocumentReaderTest.class,
	EntityResolutionTest.class,
	ResourceTest.class,
	ConcurrentTocAccess.class,
	XHTMLEntityTest.class,
	PathResolutionTest.class,
	IndexEntryTest.class,
	IndexSeeTest.class
})
public class AllOtherHelpTests {
}
