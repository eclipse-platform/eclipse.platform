/*******************************************************************************
 * Copyright (c) 2002, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.cheatsheet.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import org.eclipse.ua.tests.util.FileUtil;
import org.eclipse.ua.tests.util.ResourceFinder;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheet;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheetParser;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

/*
 * A utility for regenerating the _expected.txt files that contain the expected
 * result for the cheat sheet model when serialized. This reads all the cheat
 * sheet content in the /data/cheatsheet/valid folder, constructs the cheat sheet
 * model, then serializes the model to a text file, which is stored in the same
 * directory as the xml file, as <original_name>_expected.txt.
 *
 * These files are used by the JUnit tests to compare the result with the expected
 * result.
 *
 * Usage:
 *
 * 1. Run the "org.eclipse.ua.tests.cheatsheet.util.CheatSheetModelSerializerTest" eclipse application.
 * 2. Right-click in "Package Explorer -> Refresh".
 *
 * The new files should appear.
 */
public class CheatSheetModelSerializerTest {
	@Test
	public void testRunSerializer() throws IOException {
		URL[] urls = ResourceFinder.findFiles(FrameworkUtil.getBundle(getClass()), "data/cheatsheet/valid", ".xml",
				true);
		Assert.assertTrue("Unable to find sample cheat sheets to test parser", urls.length > 0);
		for (URL url : urls) {
			CheatSheetParser parser = new CheatSheetParser();
			CheatSheet sheet = (CheatSheet) parser.parse(url, FrameworkUtil.getBundle(getClass()).getSymbolicName(),
					CheatSheetParser.ANY);
			Assert.assertNotNull("Tried parsing a valid cheat sheet but parser returned null: " + url, sheet);

			try (PrintWriter out = new PrintWriter(
					new FileOutputStream(FileUtil.getResultFile(url.toString().substring("file:/".length()))))) {
				out.print(CheatSheetModelSerializer.serialize(sheet));
			}
		}
	}
}
