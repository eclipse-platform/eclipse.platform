/*******************************************************************************
 * Copyright (c) 2004, 2013 John-Mason P. Shackelford and others.
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
 *     IBM Corporation - bug 84342
 *******************************************************************************/
package org.eclipse.ant.tests.ui.editor.formatter;

import static org.junit.Assert.assertEquals;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.editor.formatter.FormattingPreferences;
import org.eclipse.ant.internal.ui.editor.formatter.XmlFormatter;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.Test;

@SuppressWarnings("restriction")
public class XmlFormatterTest {
	@Test
	public final void testFormatUsingPreferenceStore() throws Exception {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(AntUIPlugin.getUniqueIdentifier());
		if (node != null) {
			node.putBoolean(AntEditorPreferenceConstants.FORMATTER_WRAP_LONG, true);
			node.putInt(AntEditorPreferenceConstants.FORMATTER_MAX_LINE_LENGTH, 40);
			node.putBoolean(AntEditorPreferenceConstants.FORMATTER_ALIGN, false);
			node.putBoolean(AntEditorPreferenceConstants.FORMATTER_TAB_CHAR, true);
			node.putInt(AntEditorPreferenceConstants.FORMATTER_TAB_SIZE, 4);
			node.flush();
		}
		String lineSep = System.getProperty("line.separator"); //$NON-NLS-1$
		String xmlDoc = "<project default=\"go\"><target name=\"go\" description=\"Demonstrate the wrapping of long tags.\"><echo>hi</echo></target></project>"; //$NON-NLS-1$
		String formattedDoc = XmlFormatter.format(xmlDoc);
		String expected = "<project default=\"go\">" + lineSep + "\t<target name=\"go\"" + lineSep //$NON-NLS-1$ //$NON-NLS-2$
				+ "\t        description=\"Demonstrate the wrapping of long tags.\">" + lineSep + "\t\t<echo>hi</echo>" //$NON-NLS-1$ //$NON-NLS-2$
				+ lineSep + "\t</target>" + lineSep + "</project>"; //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(expected, formattedDoc);
	}

	@Test
	public final void testFormatWithPreferenceParameter() {
		FormattingPreferences prefs = new FormattingPreferences() {
			@Override
			public boolean wrapLongTags() {
				return true;
			}

			@Override
			public int getMaximumLineWidth() {
				return 40;
			}

			@Override
			public boolean alignElementCloseChar() {
				return false;
			}

			@Override
			public boolean useSpacesInsteadOfTabs() {
				return true;
			}

			@Override
			public int getTabWidth() {
				return 6;
			}
		};
		String lineSep = System.getProperty("line.separator"); //$NON-NLS-1$
		String xmlDoc = "<project default=\"go\"><target name=\"go\" description=\"Demonstrate the wrapping of long tags.\"><echo>hi</echo></target></project>"; //$NON-NLS-1$
		String formattedDoc = XmlFormatter.format(xmlDoc, prefs);
		String expected = "<project default=\"go\">" + lineSep + "      <target name=\"go\"" + lineSep //$NON-NLS-1$ //$NON-NLS-2$
				+ "              description=\"Demonstrate the wrapping of long tags.\">" + lineSep //$NON-NLS-1$
				+ "            <echo>hi</echo>" + lineSep + "      </target>" + lineSep + "</project>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals(expected, formattedDoc);
	}

	/**
	 * Bug 84342
	 */
	@Test
	public final void testFormatMaintainingLineSeparators() {
		FormattingPreferences prefs = new FormattingPreferences() {
			@Override
			public boolean wrapLongTags() {
				return true;
			}

			@Override
			public int getMaximumLineWidth() {
				return 40;
			}

			@Override
			public boolean alignElementCloseChar() {
				return false;
			}

			@Override
			public boolean useSpacesInsteadOfTabs() {
				return true;
			}

			@Override
			public int getTabWidth() {
				return 6;
			}
		};
		String lineSep = System.getProperty("line.separator"); //$NON-NLS-1$
		String xmlDoc = "<project default=\"go\"><target name=\"go\" description=\"Demonstrate the wrapping of long tags.\"><echo>hi</echo></target>" //$NON-NLS-1$
				+ lineSep + lineSep + "</project>"; //$NON-NLS-1$
		String formattedDoc = XmlFormatter.format(xmlDoc, prefs);
		String expected = "<project default=\"go\">" + lineSep + "      <target name=\"go\"" + lineSep //$NON-NLS-1$ //$NON-NLS-2$
				+ "              description=\"Demonstrate the wrapping of long tags.\">" + lineSep //$NON-NLS-1$
				+ "            <echo>hi</echo>" + lineSep + "      </target>" + lineSep + lineSep + "</project>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals(expected, formattedDoc);
	}

}
