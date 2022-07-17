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
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;

@SuppressWarnings("restriction")
public class FormattingPreferencesTest extends AbstractAntUITest {

	public FormattingPreferencesTest(String name) {
		super(name);
	}

	public final void testGetCanonicalIndent() {

		FormattingPreferences prefs;

		// test spaces
		prefs = new FormattingPreferences() {
			@Override
			public int getTabWidth() {
				return 3;
			}

			@Override
			public boolean useSpacesInsteadOfTabs() {
				return true;
			}
		};
		assertEquals("   ", prefs.getCanonicalIndent()); //$NON-NLS-1$

		// ensure the value is not hard coded
		prefs = new FormattingPreferences() {
			@Override
			public int getTabWidth() {
				return 7;
			}

			@Override
			public boolean useSpacesInsteadOfTabs() {
				return true;
			}
		};
		assertEquals("       ", prefs.getCanonicalIndent()); //$NON-NLS-1$

		// use tab character
		prefs = new FormattingPreferences() {
			@Override
			public int getTabWidth() {
				return 7;
			}

			@Override
			public boolean useSpacesInsteadOfTabs() {
				return false;
			}
		};
		assertEquals("\t", prefs.getCanonicalIndent()); //$NON-NLS-1$
	}
}
