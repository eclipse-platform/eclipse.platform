/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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

package org.eclipse.ua.tests.cheatsheet.other;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Unit tests for cheatsheets which don't fill into any other category
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	TestStatePersistence.class,
	TestEscape.class,
	TestCheatSheetManager.class,
	TestCheatSheetCollection.class,
	TestCheatSheetCategories.class
})
public class AllOtherCheatSheetTests {
}
