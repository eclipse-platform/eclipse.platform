/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.toc;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.help.internal.util.ProductPreferences;
import org.junit.Test;

public class HelpData {
	@Test
	public void testHelpDataInPlugin() {
		List<String> result = ProductPreferences.getTocOrdering("org.eclipse.ua.tests", "data/help/toc/helpData.xml", "");
		assertEquals(2, result.size());
		assertEquals("/org.eclipse.platform.doc.user/toc.xml", result.get(0));
		assertEquals("/org.eclipse.platform.doc.isv/toc.xml", result.get(1));
	}

	@Test
	public void testPluginsRoot() {
		List<String> result = ProductPreferences.getTocOrdering("org.eclipse.sdk", "PLUGINS_ROOT/org.eclipse.ua.tests/data/help/toc/helpData.xml", "");
		assertEquals(2, result.size());
		assertEquals("/org.eclipse.platform.doc.user/toc.xml", result.get(0));
		assertEquals("/org.eclipse.platform.doc.isv/toc.xml", result.get(1));
	}

	@Test
	public void testHelpDataOverridesBaseTocs() {
		List<String> result = ProductPreferences.getTocOrdering("org.eclipse.ua.tests", "data/help/toc/helpData.xml", "org.eclipse.help");
		assertEquals(2, result.size());
		assertEquals("/org.eclipse.platform.doc.user/toc.xml", result.get(0));
		assertEquals("/org.eclipse.platform.doc.isv/toc.xml", result.get(1));
	}

	@Test
	public void testBaseTocs() {
		List<String> result = ProductPreferences.getTocOrdering("", "", "/org.eclipse.help/toc.xml,/org.eclipse.test/toc.xml");
		assertEquals(2, result.size());
		assertEquals("/org.eclipse.help/toc.xml", result.get(0));
		assertEquals("/org.eclipse.test/toc.xml", result.get(1));
	}

}
