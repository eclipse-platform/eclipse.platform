/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 559885
 *******************************************************************************/
package org.eclipse.ua.tests.cheatsheet.parser;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ua.tests.cheatsheet.util.CheatSheetModelSerializer;
import org.eclipse.ua.tests.util.FileUtil;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheet;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheetParser;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/*
 * Tests the cheat sheets parser on valid cheat sheets.
 */
public class ValidTest {

	private void parseCheatsheet(String file) throws IOException {
		IPath path = IPath.fromOSString("data/cheatsheet/valid/" + file);
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		URL url = FileLocator.find(bundle, path, null);
		CheatSheetParser parser = new CheatSheetParser();
		CheatSheet sheet = (CheatSheet) parser.parse(url, bundle.getSymbolicName(), CheatSheetParser.ANY);
		Assert.assertNotNull("Tried parsing a valid cheat sheet but parser returned null: " + url, sheet);
		String expectedPath = "data/cheatsheet/valid/" + getExpected(file);
		String expected = FileUtil.getContents(bundle, expectedPath);
		String actual = CheatSheetModelSerializer.serialize(sheet);
		Assert.assertEquals("The model serialization generated for the cheatsheet did not match the expected result for: " + path, expected, actual);
	}

	private String getExpected(String file) {
		int suffix = file.lastIndexOf(".xml");
		return file.substring(0, suffix) + "_expected.txt";
	}

	@Test
	public void testSubItems() throws IOException {
		parseCheatsheet("TestSubItems.xml");
	}

	@Test
	public void testParameters() throws IOException {
		parseCheatsheet("TestParameters.xml");
	}

	@Test
	public void testOpeningURL() throws IOException {
		parseCheatsheet("TestOpeningURL.xml");
	}

	@Test
	public void testDynamicSubitems() throws IOException {
		parseCheatsheet("TestDynamicSubItems.xml");
	}

	@Test
	public void testDescriptionFormatting() throws IOException {
		parseCheatsheet("TestDescriptionFormatting.xml");
	}

	@Test
	public void testCSActions() throws IOException {
		parseCheatsheet("TestCSActions.xml");
	}

	@Test
	public void testContextHelp() throws IOException {
		parseCheatsheet("TestContext_Help.xml");
	}

	@Test
	public void testActions() throws IOException {
		parseCheatsheet("TestActions.xml");
	}

	@Test
	public void testHelloWorldWithSubitems() throws IOException {
		parseCheatsheet("HelloWorldWithSubitems.xml");
	}

	@Test
	public void testHelloWorldWithExtensions() throws IOException {
		parseCheatsheet("HelloWorldWithExtensions.xml");
	}

	@Test
	public void testHelloWorld() throws IOException {
		parseCheatsheet("HelloWorld.xml");
	}

	@Test
	public void testHyperlinks() throws IOException {
		parseCheatsheet("TestCSHyperlinks.xml");
	}

}
