/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.scope;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
 * Tests help dynamic content functionality (automated).
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	IntersectionTest.class,
	ScopeHierarchy.class,
	EmptyLeafRemoval.class,
	ScopeSetManagerTest.class
})
public class AllScopeTests {
}
