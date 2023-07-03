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

package org.eclipse.ua.tests.cheatsheet.execution;

import static org.junit.Assert.assertEquals;

/**
 * Test variable substitution in cheatsheets. This functionality is used by
 * command execution
 */

import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;
import org.junit.Before;
import org.junit.Test;

public class TestVariableSubstitution {
	private CheatSheetManager manager;

	@Before
	public void setUp() throws Exception {
		manager = new CheatSheetManager(new CheatSheetElement("name"));
		manager.setData("p1", "one");
		manager.setData("p2", "two");
	}

	private String substitute(String input) {
		return manager.performVariableSubstitution(input);
	}

	@Test
	public void testNoSubstitution() {
		assertEquals("abcdefg", substitute("abcdefg"));
	}

	@Test
	public void testFullString() {
		assertEquals("one", substitute("${p1}"));
	}

	@Test
	public void testEmbeddedString() {
		assertEquals("AoneB", substitute("A${p1}B"));
	}

	@Test
	public void testRepeatedSubstitution() {
		assertEquals("oneXone", substitute("${p1}X${p1}"));
	}

	@Test
	public void testMultipleSubstitution() {
		assertEquals("onetwo", substitute("${p1}${p2}"));
	}

	@Test
	public void testNonexistentParameter() {
		assertEquals("one", substitute("${p1}${p3}"));
	}

	@Test
	public void testUnterminatedParameter() {
		assertEquals("${p1", "${p1");
	}


}
