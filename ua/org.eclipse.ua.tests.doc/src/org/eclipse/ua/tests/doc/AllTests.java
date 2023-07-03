/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
package org.eclipse.ua.tests.doc;

import org.eclipse.ua.tests.doc.internal.linkchecker.ApiDocTest;
import org.eclipse.ua.tests.doc.internal.linkchecker.LinkTest;
import org.eclipse.ua.tests.doc.internal.linkchecker.TocLinkChecker;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
 * Tests all user assistance functionality (automated).
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ TocLinkChecker.class, ApiDocTest.class, LinkTest.class })
public class AllTests {
}
