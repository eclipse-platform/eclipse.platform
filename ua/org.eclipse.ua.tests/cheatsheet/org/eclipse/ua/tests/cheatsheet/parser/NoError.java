/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
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

/**
 * Tests which should generate no error or warning when parsing
 */

package org.eclipse.ua.tests.cheatsheet.parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.eclipse.ua.tests.util.ResourceFinder;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheet;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheetParser;
import org.eclipse.ui.internal.cheatsheets.data.ICheatSheet;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class NoError {
	private static final String NO_ERROR_FOLDER = "data/cheatsheet/no_error/";
	private static final String OTHER_FOLDER = "data/cheatsheet/other/";

	private CheatSheetParser parser;

	@Before
	public void setUp() throws Exception {
		parser = new CheatSheetParser();
	}

	private ICheatSheet parseTestFile(String path) {
		URL testURL = ResourceFinder.findFile(FrameworkUtil.getBundle(NoError.class), path);
		return parser.parse(testURL, FrameworkUtil.getBundle(getClass()).getSymbolicName(),
				CheatSheetParser.SIMPLE_ONLY);
	}

	@Test
	public void testConfirmRequiredCombinations() {
		ICheatSheet model = parseTestFile(NO_ERROR_FOLDER + "ConfirmRequired.xml");
		assertNotNull(model);
		assertTrue(parser.getStatus().isOK());
	}

	@Test
	public void testRestrictedAction() {
		ICheatSheet model = parseTestFile(OTHER_FOLDER + "TestActions.xml");
		assertNotNull(model);
		assertTrue(parser.getStatus().isOK());
		assertTrue(model instanceof CheatSheet);
		assertTrue(((CheatSheet)model).isContainsCommandOrAction());
	}

	@Test
	public void testRestrictedCommand() {
		ICheatSheet model = parseTestFile(OTHER_FOLDER + "TestCommand.xml");
		assertNotNull(model);
		assertTrue(parser.getStatus().isOK());
		assertTrue(model instanceof CheatSheet);
		assertTrue(((CheatSheet)model).isContainsCommandOrAction());
	}

	@Test
	public void testNoRestriction() {
		ICheatSheet model = parseTestFile(OTHER_FOLDER + "NoActions.xml");
		assertNotNull(model);
		assertTrue(parser.getStatus().isOK());
		assertTrue(model instanceof CheatSheet);
		assertFalse(((CheatSheet)model).isContainsCommandOrAction());
	}


}
