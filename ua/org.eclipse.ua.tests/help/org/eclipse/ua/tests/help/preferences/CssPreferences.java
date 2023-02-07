/*******************************************************************************
 * Copyright (c) 2007, 2016  IBM Corporation and others.
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

package org.eclipse.ua.tests.help.preferences;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.webapp.data.CssUtil;
import org.junit.Test;

/*
 * Test the parsing of CSS preferences
 */
public class CssPreferences {

	private static final String ORG_ECLIPSE_TEST = "org.eclipse.test/";
	private static final String FILE_CSS = "file.css";
	private static final String ORG_ECLIPSE_TEST_FILE_CSS = ORG_ECLIPSE_TEST + FILE_CSS;
	private static final String FILENAME_WITH_PARAM = ORG_ECLIPSE_TEST + "${os}" + FILE_CSS;
	private static final String FILENAME_WITH_OS = ORG_ECLIPSE_TEST + Platform.getOS() + FILE_CSS;

	@Test
	public void testNull() {
		String[] options = CssUtil.getCssFilenames(null);
		assertEquals(0, options.length);
	}

	@Test
	public void testEmptyString() {
		String[] options = CssUtil.getCssFilenames("");
		assertEquals(0, options.length);
	}

	@Test
	public void testSingleString() {
		String[] options = CssUtil.getCssFilenames(ORG_ECLIPSE_TEST_FILE_CSS);
		assertEquals(1, options.length);
		assertEquals(ORG_ECLIPSE_TEST_FILE_CSS, options[0]);
	}

	@Test
	public void testTwoStrings() {
		String[] options = CssUtil.getCssFilenames(ORG_ECLIPSE_TEST_FILE_CSS + " , " + FILENAME_WITH_PARAM);
		assertEquals(2, options.length);
		assertEquals(ORG_ECLIPSE_TEST_FILE_CSS, options[0]);
		assertEquals(FILENAME_WITH_OS, options[1]);
	}


}
