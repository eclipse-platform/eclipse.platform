/*******************************************************************************
 * Copyright (c) 2004, 2005 John-Mason P. Shackelford and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     John-Mason P. Shackelford - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.editor.formatter;

import org.eclipse.ant.internal.ui.editor.formatter.FormattingPreferences;
import org.eclipse.ant.internal.ui.editor.formatter.XmlDocumentFormatter;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;

@SuppressWarnings("restriction")
public class XmlDocumentFormatterTest extends AbstractAntUITest {

	public XmlDocumentFormatterTest(String name) {
		super(name);
	}

	/**
	 * General Test
	 */
	public final void testGeneralFormat() throws Exception {
		FormattingPreferences prefs = new FormattingPreferences() {
			@Override
			public int getTabWidth() {
				return 3;
			}

			@Override
			public boolean useSpacesInsteadOfTabs() {
				return true;
			}
		};
		simpleTest("formatTest_source01.xml", "formatTest_target01.xml", prefs); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Insure that tab width is not hard coded
	 */
	public final void testTabWidth() throws Exception {
		FormattingPreferences prefs = new FormattingPreferences() {
			@Override
			public int getTabWidth() {
				return 7;
			}

			@Override
			public boolean useSpacesInsteadOfTabs() {
				return true;
			}
		};
		simpleTest("formatTest_source01.xml", "formatTest_target02.xml", prefs); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Test with tab characters instead of spaces.
	 */
	public final void testTabsInsteadOfSpaces() throws Exception {
		FormattingPreferences prefs = new FormattingPreferences() {
			@Override
			public int getTabWidth() {
				return 3;
			}

			@Override
			public boolean useSpacesInsteadOfTabs() {
				return false;
			}
		};
		simpleTest("formatTest_source01.xml", "formatTest_target03.xml", prefs); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @param sourceFileName
	 *            - file to format
	 * @param targetFileName
	 *            - the source file after a properly executed format
	 * @param prefs
	 *            - given the included preference instructions
	 * @throws Exception
	 */
	private void simpleTest(String sourceFileName, String targetFileName, FormattingPreferences prefs) throws Exception {

		XmlDocumentFormatter xmlFormatter = new XmlDocumentFormatter();
		xmlFormatter.setDefaultLineDelimiter(System.getProperty("line.separator")); //$NON-NLS-1$
		String result = xmlFormatter.format(getFileContentAsString(getBuildFile(sourceFileName)), prefs);
		String expectedResult = getFileContentAsString(getBuildFile(targetFileName));

		assertEquals(expectedResult, result);
	}
}