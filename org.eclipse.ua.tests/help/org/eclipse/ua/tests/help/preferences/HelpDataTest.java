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
package org.eclipse.ua.tests.help.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.internal.HelpData;
import org.eclipse.help.internal.HelpPlugin;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

/*
 * Tests the help data ordering of tocs and hiding tocs, indexes, etc.
 */
public class HelpDataTest {

	private static final String[][][] TEST_DATA = new String[][][] {
		{ { "data/help/preferences/helpData1.xml" }, { "toc1", "category1", "toc2", "toc3" }, { "toc4", "category2" }, { "index1", "index2" } },
		{ { "data/help/preferences/helpData2.xml" }, { }, { }, { } },
		{ { "data/help/preferences/helpData3.xml" }, { }, { }, { } },
	};

	private String baseTocsPreference;
	private String ignoredTocsPreference;
	private String ignoredIndexesPreference;


	@Before
	public void setUp() throws Exception {
		baseTocsPreference = Platform.getPreferencesService().getString
			(HelpPlugin.PLUGIN_ID, HelpPlugin.BASE_TOCS_KEY, "", null);
		ignoredTocsPreference = Platform.getPreferencesService().getString
			(HelpPlugin.PLUGIN_ID, HelpPlugin.IGNORED_TOCS_KEY, "", null);
		ignoredIndexesPreference = Platform.getPreferencesService().getString
			(HelpPlugin.PLUGIN_ID, HelpPlugin.IGNORED_INDEXES_KEY, "", null);
		setBaseTocs("");
		setIgnoredTocs("");
		setIgnoredIndexes("");
	}

	@After
	public void tearDown() throws Exception {
		setBaseTocs(baseTocsPreference);
		setIgnoredTocs(ignoredTocsPreference);
		setIgnoredIndexes(ignoredIndexesPreference);
	}

	private void setBaseTocs(String value) {
		IEclipsePreferences pref = InstanceScope.INSTANCE.getNode(HelpPlugin.PLUGIN_ID);
		pref.put(HelpPlugin.BASE_TOCS_KEY, value);
	}

	private void setIgnoredTocs(String value) {
		IEclipsePreferences pref = InstanceScope.INSTANCE.getNode(HelpPlugin.PLUGIN_ID);
		pref.put(HelpPlugin.IGNORED_TOCS_KEY, value);
	}

	private void setIgnoredIndexes(String value) {
		IEclipsePreferences pref = InstanceScope.INSTANCE.getNode(HelpPlugin.PLUGIN_ID);
		pref.put(HelpPlugin.IGNORED_INDEXES_KEY, value);
	}

	@Test
	public void testHelpData() {
		for (String[][] entry : TEST_DATA) {
			String file = entry[0][0];
			List<String> expectedTocOrder = Arrays.asList(entry[1]);
			Set<String> expectedHiddenTocs = new HashSet<>(Arrays.asList(entry[2]));
			Set<String> expectedHiddenIndexes = new HashSet<>(Arrays.asList(entry[3]));
			URL url = FrameworkUtil.getBundle(HelpDataTest.class).getEntry(file);
			HelpData data = new HelpData(url);
			Assert.assertEquals("Did not get the expected toc order from help data file " + file, expectedTocOrder, data.getTocOrder());
			Assert.assertEquals("Did not get the expected hidden tocs from help data file " + file, expectedHiddenTocs, data.getHiddenTocs());
			Assert.assertEquals("Did not get the expected hidden indexes from help data file " + file, expectedHiddenIndexes, data.getHiddenIndexes());
		}
	}

	@Test
	public void testNullUrl() {
		HelpData data = new HelpData(null);
		assertEquals(0, data.getTocOrder().size());
		assertEquals(0, data.getHiddenTocs().size());
		assertEquals(0, data.getHiddenIndexes().size());
		assertTrue(data.isSortOthers());
	}

	@Test
	public void testNullUrlWithBaseTocs() {
		HelpData data = new HelpData(null);
		setBaseTocs("/a/b.xml,/c/d.xml");
		List<?> tocOrder = data.getTocOrder();
		assertEquals(2, tocOrder.size());
		assertEquals("/a/b.xml", tocOrder.get(0));
		assertEquals("/c/d.xml", tocOrder.get(1));
		assertEquals(0, data.getHiddenTocs().size());
		assertEquals(0, data.getHiddenIndexes().size());
		assertTrue(data.isSortOthers());
	}

	@Test
	public void testNullUrlWithHiddenTocs() {
		HelpData data = new HelpData(null);
		setIgnoredTocs("/a/b.xml,/c/d.xml");
		assertEquals(0, data.getTocOrder().size());
		Set<?> hiddenTocs = data.getHiddenTocs();
		assertEquals(2, hiddenTocs.size());
		assertTrue(hiddenTocs.contains("/a/b.xml"));
		assertTrue(hiddenTocs.contains("/c/d.xml"));
		assertEquals(0, data.getHiddenIndexes().size());
		assertTrue(data.isSortOthers());
	}

	@Test
	public void testNullUrlWithHiddenIndexes() {
		HelpData data = new HelpData(null);
		setIgnoredIndexes("/a/b.xml,/c/d.xml");
		assertEquals(0, data.getTocOrder().size());
		assertEquals(0, data.getHiddenTocs().size());
		Set<?> hiddenIndexes = data.getHiddenIndexes();
		assertEquals(2, hiddenIndexes.size());
		assertTrue(hiddenIndexes.contains("/a/b.xml"));
		assertTrue(hiddenIndexes.contains("/c/d.xml"));
		assertTrue(data.isSortOthers());
	}
}
