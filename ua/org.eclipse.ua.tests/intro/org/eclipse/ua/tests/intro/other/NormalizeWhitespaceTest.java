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
package org.eclipse.ua.tests.intro.other;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eclipse.ui.internal.intro.impl.util.StringUtil;
import org.junit.Test;

/*
 * Tests whitespace normalization used by SWT presentation.
 */
public class NormalizeWhitespaceTest {
	@Test
	public void testNullString() {
		assertNull(StringUtil.normalizeWhiteSpace(null));
	}

	@Test
	public void testEmptyString() {
		String result = StringUtil.normalizeWhiteSpace("");
		assertEquals("", result);
	}

	@Test
	public void testSimpleString() {
		String result = StringUtil.normalizeWhiteSpace("Hello World");
		assertEquals("Hello World", result);
	}

	@Test
	public void testRepeatedSpace() {
		String result = StringUtil.normalizeWhiteSpace("Hello   World");
		assertEquals("Hello World", result);
	}

	@Test
	public void testOtherWhitespace() {
		String result = StringUtil.normalizeWhiteSpace("Hello\n\r\t World");
		assertEquals("Hello World", result);
	}

	@Test
	public void testLeadingSpace() {
		String result = StringUtil.normalizeWhiteSpace(" Hello World");
		assertEquals("Hello World", result);
	}

	@Test
	public void testTrailingSpace() {
		String result = StringUtil.normalizeWhiteSpace("Hello World ");
		assertEquals("Hello World", result);
	}

}
