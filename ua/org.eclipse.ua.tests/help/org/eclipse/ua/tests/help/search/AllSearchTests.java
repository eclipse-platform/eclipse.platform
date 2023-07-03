/*******************************************************************************
 *  Copyright (c) 2006, 2012 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.search;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
 * Tests help functionality (automated).
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	ExtraDirTest.class,
	BasicTest.class,
	WildcardTest.class,
	LocaleTest.class,
	AnalyzerTest.class,
	SearchCheatsheet.class,
	SearchIntro.class,
	EncodedCharacterSearch.class,
	MetaKeywords.class,
	SearchParticipantTest.class,
	SearchParticipantXMLTest.class,
	SearchRanking.class,
	WorkingSetManagerTest.class,
	InfocenterWorkingSetManagerTest.class,
	PrebuiltIndexCompatibility.class,
	LockTest.class
})
public class AllSearchTests {
}
