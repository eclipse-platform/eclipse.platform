/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.ant.tests.core.tests;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.eclipse.ant.core.Property;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link Property} class
 *
 * @since 3.8
 */
public class PropertyTests extends AbstractAntTest {

	@Test
	public void testPropertyEqual() throws Exception {
		Property p1 = new Property("one", "ONE"); //$NON-NLS-1$ //$NON-NLS-2$
		Property p2 = new Property("one", "ONE"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(p1, p2);
	}

	@Test
	public void testPropertyEqualNameOnly() throws Exception {
		Property p1 = new Property("two", "TWO"); //$NON-NLS-1$ //$NON-NLS-2$
		Property p2 = new Property("two", "FOUR"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(p1, p2);
	}

	@Test
	public void testPropertyNotEqual() throws Exception {
		Property p1 = new Property("three", "THREE"); //$NON-NLS-1$ //$NON-NLS-2$
		Property p2 = new Property("four", "FOUR"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotEquals(p1, p2);
	}

	@Test
	public void testPropertyNotEqual2() throws Exception {
		Property p1 = new Property("five", "FIVE"); //$NON-NLS-1$ //$NON-NLS-2$
		Property p2 = new Property("six", "FIVE"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotEquals(p1, p2);
	}

	@Test
	public void testPropertyNotEqualNull() throws Exception {
		Property p1 = new Property("seven", "SEVEN"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotEquals(p1, null);
	}
}
