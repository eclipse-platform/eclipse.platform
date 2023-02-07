/*******************************************************************************
 *  Copyright (c) 2006, 2016 IBM Corporation and others.
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


package org.eclipse.ua.tests.cheatsheet.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.eclipse.core.runtime.Status;
import org.eclipse.ua.tests.util.ResourceFinder;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheetParser;
import org.eclipse.ui.internal.cheatsheets.data.ICheatSheet;
import org.eclipse.ui.internal.cheatsheets.data.ParserInput;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class ParseFromString {

	private static final String VALID_CONTENT =
		"<?xml version=\"1.0\" encoding=\"UTF-8\" ?> "
		+ "<cheatsheet title=\"Title\">"
		+ "<intro><description>Simple test</description></intro>"
		+ "<item title=\"Item\">"
		+ "<description>description</description>"
		+ "</item></cheatsheet>";

	// INVALID_CONTENT has no items
	private static final String INVALID_CONTENT =
		"<?xml version=\"1.0\" encoding=\"UTF-8\" ?> "
		+ "<cheatsheet title=\"Title\">"
		+ "<intro><description>Simple test</description></intro>"
		+ "</cheatsheet>";

	// Test that the default value for getContentXml is null
	@Test
	public void testElementXml() {
		CheatSheetElement element = new CheatSheetElement("name");
		assertNull(element.getContentXml());
		element.setContentXml(VALID_CONTENT);
	}

	@Test
	public void testDefaultParserInput() {
		ParserInput input = new ParserInput();
		assertNull(input.getUrl());
		assertNull(input.getXml());
	}

	@Test
	public void testXmlParserInput() {
		ParserInput input = new ParserInput(VALID_CONTENT, null);
		assertNull(input.getUrl());
		assertEquals(VALID_CONTENT, input.getXml());
	}

	@Test
	public void testUrlParserInput() {
		URL testURL = ResourceFinder.findFile(FrameworkUtil.getBundle(ParseFromString.class),
					"data/cheatsheet/valid/HelloWorld.xml");
		ParserInput input = new ParserInput(testURL, FrameworkUtil.getBundle(getClass()).getSymbolicName(), null);
		assertNull(input.getXml());
		assertTrue(testURL.equals(input.getUrl()));
	}

	@Test
	public void testValidCheatsheet() {
		ParserInput input = new ParserInput(VALID_CONTENT, null);
		CheatSheetParser parser = new CheatSheetParser();
		ICheatSheet cheatSheet = parser.parse(input, CheatSheetParser.SIMPLE_ONLY);
		assertNotNull(cheatSheet);
		assertEquals(Status.OK, parser.getStatus().getSeverity());
	}

	@Test
	public void testInvalidCheatsheet() {
		ParserInput input = new ParserInput(INVALID_CONTENT, null);
		CheatSheetParser parser = new CheatSheetParser();
		ICheatSheet cheatSheet = parser.parse(input, CheatSheetParser.SIMPLE_ONLY);
		assertNull(cheatSheet);
		assertEquals(Status.ERROR, parser.getStatus().getSeverity());
		assertTrue(parser.getStatus().getMessage().contains("must contain at least one <item>"));
	}
}
