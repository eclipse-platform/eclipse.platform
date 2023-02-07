/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.webapp;

import static org.junit.Assert.assertEquals;

import org.eclipse.help.internal.webapp.servlet.TocFragmentServlet;
import org.junit.Test;

/**
 * Tests for the class TocFragmentServlet
 */
public class FragmentServletTest {

	private static final String PATH = "1_2";
	private static final String DOC_HTML = "org.eclipse.ua.tests/doc.html";

	@Test
	public void testFixHrefNormal() {
		String href = TocFragmentServlet.fixupHref(DOC_HTML, PATH);
		assertEquals(DOC_HTML + "?cp=1_2", href);
	}

	@Test
	public void testFixHrefWithParameter() {
		String href = TocFragmentServlet.fixupHref(DOC_HTML + "?a=b", PATH);
		assertEquals(DOC_HTML + "?a=b&cp=1_2", href);
	}

	@Test
	public void testFixHrefNull() {
		String href = TocFragmentServlet.fixupHref(null, PATH);
		assertEquals("/../nav/1_2",  href);
	}

	@Test
	public void testFixHrefWithAnchor() {
		String href = TocFragmentServlet.fixupHref(DOC_HTML + "#A", PATH);
		assertEquals(DOC_HTML + "?cp=1_2#A" , href);
	}

	@Test
	public void testFixHrefWithAnchorAndParams() {
		String href = TocFragmentServlet.fixupHref(DOC_HTML + "?a=b#A", PATH);
		assertEquals(DOC_HTML + "?a=b&cp=1_2#A" , href);
	}

}
