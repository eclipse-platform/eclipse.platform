/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheetParser;
import org.eclipse.ui.internal.cheatsheets.data.ICheatSheet;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

/*
 * Tests the cheat sheets parser on tolerable cheat sheets. This means they're not strictly correct,
 * but the parser will tolerate them.
 */
public class TolerateTest {

	private void parseCheatsheet(String file) {
		IPath path = IPath.fromOSString("data/cheatsheet/valid/tolerate/" + file);
		URL url = FileLocator.find(FrameworkUtil.getBundle(TolerateTest.class), path, null);
		CheatSheetParser parser = new CheatSheetParser();
		ICheatSheet sheet = parser.parse(url, FrameworkUtil.getBundle(getClass()).getSymbolicName(),
				CheatSheetParser.SIMPLE_ONLY);
		Assert.assertEquals("Warning not generated: " + url, IStatus.WARNING, parser.getStatus().getSeverity());
		Assert.assertNotNull("Tried parsing a tolerable cheat sheet but parser returned null: " + url, sheet);
	}

	@Test
	public void testItemExtraAttr() {
		parseCheatsheet("ItemElement_ExtraAttr.xml");
	}

	@Test
	public void testIntroExtraElement() {
		parseCheatsheet("IntroElement_ExtraElement.xml");
	}

	@Test
	public void testIntroExtraAttr() {
		parseCheatsheet("IntroElement_ExtraAttr.xml");
	}

	@Test
	public void testDescExtraElement() {
		parseCheatsheet("DescriptionElement_ExtraElements.xml");
	}

	@Test
	public void testConditionalExtraElement() {
		parseCheatsheet("ConditionalSubItem_ExtraElement.xml");
	}

	@Test
	public void testConditionalExtraAttr() {
		parseCheatsheet("ConditionalSubItem_ExtraAttr.xml");
	}

	@Test
	public void testElementExtraElement() {
		parseCheatsheet("CheatSheetElement_ExtraElement.xml");
	}

	@Test
	public void testElementExtraAttr() {
		parseCheatsheet("CheatSheetElement_ExtraAttr.xml");
	}

	@Test
	public void testExtraElement() {
		parseCheatsheet("ActionElement_ExtraElement.xml");
	}

	@Test
	public void testExtraAttr() {
		parseCheatsheet("ActionElement_ExtraAttr.xml");
	}

}
