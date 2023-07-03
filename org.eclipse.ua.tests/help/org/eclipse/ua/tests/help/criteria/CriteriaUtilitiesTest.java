/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.criteria;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.List;

import org.eclipse.help.internal.base.util.CriteriaUtilities;
import org.junit.Test;

public class CriteriaUtilitiesTest {

	@Test
	public void testNullValues() {
		List<String> values = CriteriaUtilities.getCriteriaValues(null);
		assertEquals(0, values.size());
	}

	@Test
	public void testSingleValue() {
		List<String> values = CriteriaUtilities.getCriteriaValues("1.0");
		assertEquals(1, values.size());
		assertEquals("1.0", values.get(0));
	}

	@Test
	public void testSingleValueWithWhitespace() {
		List<String> values = CriteriaUtilities.getCriteriaValues(" 1.0 ");
		assertEquals(1, values.size());
		assertEquals("1.0", values.get(0));
	}

	@Test
	public void testMultipleValues() {
		List<String> values = CriteriaUtilities.getCriteriaValues(" 1.0, 2.0 ");
		assertEquals(2, values.size());
		assertEquals("1.0", values.get(0));
		assertEquals("2.0", values.get(1));
	}

	@Test
	public void testUppercaseValue() {
		List<String> values = CriteriaUtilities.getCriteriaValues("LINUX");
		assertEquals(1, values.size());
		assertNotSame("linux", values.get(0));
	}

}
