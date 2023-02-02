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

package org.eclipse.ua.tests.cheatsheet.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ua.tests.cheatsheet.util.StatusCheck;
import org.eclipse.ua.tests.util.ResourceFinder;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheetParser;
import org.eclipse.ui.internal.cheatsheets.data.ICheatSheet;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class InvalidCheatsheet {
	private static final String INVALID_CHEATSHEET_FOLDER = "data/cheatsheet/invalid/";
	private CheatSheetParser parser;
	private String bundleSymbolicName;

	@Before
	public void setUp() throws Exception {
		parser = new CheatSheetParser();
		bundleSymbolicName = FrameworkUtil.getBundle(getClass()).getSymbolicName();
	}

	private ICheatSheet parseTestFile(String path) {
		URL testURL = ResourceFinder.findFile(FrameworkUtil.getBundle(InvalidCheatsheet.class),
							INVALID_CHEATSHEET_FOLDER + path);
		return parser.parse(testURL, bundleSymbolicName, CheatSheetParser.SIMPLE_ONLY);
	}

	@Test
	public void testBadURL() {
		try {
			assertNull(parser.parse(new URL("file:/nonexistent"), bundleSymbolicName, CheatSheetParser.SIMPLE_ONLY));
		} catch (MalformedURLException e) {
			fail("Exception thrown");
		}
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Could not open");
	}

	@Test
	public void testActionMissingClass() {
		ICheatSheet model = parseTestFile("ActionElement_MissingClass.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "must specify a class");
	}

	@Test
	public void testActionMissingPluginId() {
		ICheatSheet model = parseTestFile("ActionElement_MissingPluginId.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "must specify a pluginId");
	}

	@Test
	public void testCommandMissingSerialization() {
		ICheatSheet model = parseTestFile("Command_MissingSerialization.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "must specify a serialization");
	}

	@Test
	public void testInvalidParamNumber() {
		ICheatSheet model = parseTestFile("ActionElement_ParamInvalidNumber.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "invalid parameter number");
	}

	@Test
	public void testInvalidParamRange() {
		ICheatSheet model = parseTestFile("ActionElement_ParamInvalidRange.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "invalid range");
	}

	@Test
	public void testMissingTitle() {
		ICheatSheet model = parseTestFile("CheatSheetElement_MissingTitle.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "must specify a title");
	}

	@Test
	public void testNotDefined() {
		ICheatSheet model = parseTestFile("CheatSheetElement_NotDefined.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "The <cheatsheet> element must be the root");
	}

	@Test
	public void testConditionalSubitemMissingCondition() {
		ICheatSheet model = parseTestFile("CondSubItem_MissingCondition.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "must specify a condition");
	}

	@Test
	public void testConditionalSubitemMissingSubitem() {
		ICheatSheet model = parseTestFile("CondSubItem_MissingSubItem.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "must specify a subitem");
	}

	@Test
	public void testIntroElementManyDefined() {
		ICheatSheet model = parseTestFile("IntroElement_ManyDefined.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "can only contain one <intro> element");
	}

	@Test
	public void testIntroElementMissingDescription() {
		ICheatSheet model = parseTestFile("IntroElement_MissingDescription.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "The description for element 'intro' was not defined");
	}

	@Test
	public void testIntroElementManyDescriptions() {
		ICheatSheet model = parseTestFile("IntroElement_ManyDescriptions.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "more than one description");
	}

	@Test
	public void testIntroElementNotDefined() {
		ICheatSheet model = parseTestFile("IntroElement_NotDefined.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "must contain an <intro>");
	}

	@Test
	public void testItemElementMissingTitle() {
		ICheatSheet model = parseTestFile("ItemElement_MissingTitle.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "must specify a title");
	}

	@Test
	public void testItemElementMissingDescription() {
		ICheatSheet model = parseTestFile("ItemElement_MissingDescription.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "The description for element 'item' was not defined");
	}

	@Test
	public void testItemElementManyDescriptions() {
		ICheatSheet model = parseTestFile("ItemElement_ManyDescriptions.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "more than one description");
	}

	@Test
	public void testItemElementNotDefined() {
		ICheatSheet model = parseTestFile("ItemElement_NotDefined.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "at least one <item>");
	}

	@Test
	public void testPerformWhenMissingAction() {
		ICheatSheet model = parseTestFile("PerformWhen_MissingAction.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "must specify an action");
	}

	@Test
	public void testPerformWhenMissingCondition() {
		ICheatSheet model = parseTestFile("PerformWhen_MissingCondition.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "must specify a condition");
	}

	@Test
	public void testSubitemElementMissingLabel() {
		ICheatSheet model = parseTestFile("SubItem_MissingLabel.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "must specify a label");
	}

	@Test
	public void testRepeatedSubitemMissingSubitem() {
		ICheatSheet model = parseTestFile("RepSubItem_MissingSubItem.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "must specify a subitem");
	}

	@Test
	public void testRepeatedSubitemMissingValues() {
		ICheatSheet model = parseTestFile("RepSubItem_MissingValues.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "must specify a values");
	}

	@Test
	public void testActionAndPerformWhen() {
		ICheatSheet model = parseTestFile("ActionAndPerformWhen.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "incompatible");
	}

	@Test
	public void testCommandAndAction() {
		ICheatSheet model = parseTestFile("CommandAndAction.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "incompatible");
	}

	@Test
	public void testCommandAndSubitem() {
		ICheatSheet model = parseTestFile("CommandAndSubitem.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "incompatible");
	}

	@Test
	public void testSubitemAndPerformWhen() {
		ICheatSheet model = parseTestFile("SubitemAndPerformWhen.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "incompatible");
	}

	@Test
	public void testTwoActions() {
		ICheatSheet model = parseTestFile("TwoActions.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "more than one");
	}

	@Test
	public void testTwoCommands() {
		ICheatSheet model = parseTestFile("TwoCommands.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "more than one");
	}

	@Test
	public void testTwoPerformWhen() {
		ICheatSheet model = parseTestFile("TwoPerformWhen.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "more than one");
	}

	@Test
	public void testConfirmTrueRequiredFalse() {
		ICheatSheet model = parseTestFile("ConfirmTrueRequiredFalse.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "required = false and confirm = true");
	}

}
