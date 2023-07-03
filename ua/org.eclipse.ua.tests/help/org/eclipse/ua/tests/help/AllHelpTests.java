/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
package org.eclipse.ua.tests.help;

import org.eclipse.ua.tests.help.criteria.AllCriteriaTests;
import org.eclipse.ua.tests.help.dynamic.AllDynamicTests;
import org.eclipse.ua.tests.help.index.AllIndexTests;
import org.eclipse.ua.tests.help.other.AllOtherHelpTests;
import org.eclipse.ua.tests.help.preferences.AllPreferencesTests;
import org.eclipse.ua.tests.help.remote.AllRemoteTests;
import org.eclipse.ua.tests.help.scope.AllScopeTests;
import org.eclipse.ua.tests.help.search.AllSearchTests;
import org.eclipse.ua.tests.help.toc.AllTocTests;
import org.eclipse.ua.tests.help.webapp.AllWebappTests;
import org.eclipse.ua.tests.help.webapp.service.AllWebappServiceTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
 * Tests help functionality (automated).
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	AllPreferencesTests.class,
	AllCriteriaTests.class,
	AllDynamicTests.class,
	AllSearchTests.class,
	AllTocTests.class,
	AllIndexTests.class,
	AllWebappTests.class,
	AllOtherHelpTests.class,
	AllRemoteTests.class,
	AllScopeTests.class,
	AllWebappServiceTests.class,
})
public class AllHelpTests {
}
