/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.other;

import static org.junit.Assert.assertEquals;

import org.eclipse.help.internal.base.util.LinkUtil;
import org.eclipse.ui.internal.cheatsheets.views.ViewUtilities;
import org.junit.Test;

public class LinkUtilTest {
	@Test
	public void testStripParamNull() {
		assertEquals(null, LinkUtil.stripParams(null));
	}

	@Test
	public void testStripParamEmpty() {
		assertEquals("", ViewUtilities.escapeForLabel(""));
	}

	@Test
	public void testStripParamWithoutParam() {
		assertEquals("http://www.eclipse.org", LinkUtil.stripParams("http://www.eclipse.org"));
	}

	@Test
	public void testStripParamWithParam() {
		assertEquals("http://www.mysite.com", LinkUtil.stripParams("http://www.mysite.com?param1=foo&param2=bar"));
	}

	@Test
	public void testStripParamWithAnchor() {
		assertEquals("http://www.mysite.com#anchor1", LinkUtil.stripParams("http://www.mysite.com#anchor1"));
	}

	@Test
	public void testStripParamWithAnchorBeforeParam() {
		assertEquals("http://www.mysite.com#anchor1", LinkUtil.stripParams("http://www.mysite.com#anchor1?param=foobar"));
	}

	@Test
	public void testStripParamWithAnchorAfterParam() {
		assertEquals("http://www.mysite.com#anchor1", LinkUtil.stripParams("http://www.mysite.com?param=foobar#anchor1"));
	}
}
