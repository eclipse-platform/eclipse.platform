/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.cheatsheet.parser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
 * Tests all cheat sheet parser functionality (automated).
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	ValidTest.class, TolerateTest.class, InvalidCheatsheet.class, ParseFromString.class, NoError.class
})
public class AllParserTests {
}
