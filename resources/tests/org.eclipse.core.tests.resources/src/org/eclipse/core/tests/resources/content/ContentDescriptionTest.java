/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.content;

import static org.eclipse.core.tests.resources.AutomatedResourceTests.PI_RESOURCES_TESTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.internal.content.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.junit.Test;

public class ContentDescriptionTest extends ContentTypeTest {
	private static final String CT_VOID = PI_RESOURCES_TESTS + '.' + "void";
	private static final QualifiedName ZOO_PROPERTY = new QualifiedName(PI_RESOURCES_TESTS, "zoo");
	private static final QualifiedName BAR_PROPERTY = new QualifiedName(PI_RESOURCES_TESTS, "bar");
	private static final QualifiedName FOO_PROPERTY = new QualifiedName(PI_RESOURCES_TESTS, "foo");
	private static final QualifiedName FRED_PROPERTY = new QualifiedName(PI_RESOURCES_TESTS, "fred");

	private ContentType getContentType() {
		return ((ContentTypeHandler) Platform.getContentTypeManager().getContentType(CT_VOID)).getTarget();
	}

	@Test
	public void testAllProperties() {
		ContentDescription description = new ContentDescription(IContentDescription.ALL, getContentType());
		assertTrue("1.0", description.isRequested(FOO_PROPERTY));
		assertNull("1.1", description.getProperty(FOO_PROPERTY));
		description.setProperty(FOO_PROPERTY, "value1");
		assertEquals("1.2", "value1", description.getProperty(FOO_PROPERTY));
		description.setProperty(FOO_PROPERTY, "value1b");
		assertEquals("1.3", "value1b", description.getProperty(FOO_PROPERTY));
		assertTrue("2.0", description.isRequested(BAR_PROPERTY));
		description.setProperty(BAR_PROPERTY, "value2");
		assertEquals("2.1", "value2", description.getProperty(BAR_PROPERTY));
		description.setProperty(ZOO_PROPERTY, "value3");
		assertEquals("2.2", "value3", description.getProperty(ZOO_PROPERTY));
		description.markImmutable();
		try {
			description.setProperty(FOO_PROPERTY, "value1c");
			fail("3.0 - should have failed");
		} catch (IllegalStateException e) {
			// success - the object was marked as immutable
		}
	}

	@Test
	public void testOneProperty() {
		ContentDescription description = new ContentDescription(new QualifiedName[] {FOO_PROPERTY}, getContentType());
		assertTrue("1.0", description.isRequested(FOO_PROPERTY));
		assertNull("1.1", description.getProperty(FOO_PROPERTY));
		description.setProperty(FOO_PROPERTY, "value1");
		assertEquals("1.2", "value1", description.getProperty(FOO_PROPERTY));
		description.setProperty(FOO_PROPERTY, "value1b");
		assertEquals("1.3", "value1b", description.getProperty(FOO_PROPERTY));
		description.setProperty(BAR_PROPERTY, "value2");
		assertFalse("2.0", description.isRequested(BAR_PROPERTY));
		description.setProperty(BAR_PROPERTY, "value2");
		assertNull("2.1", description.getProperty(BAR_PROPERTY));
		description.markImmutable();
		try {
			description.setProperty(FOO_PROPERTY, "value1c");
			fail("3.0 - should have failed");
		} catch (IllegalStateException e) {
			// success - the object was marked as immutable
		}
	}

	@Test
	public void testZeroProperties() {
		ContentDescription description = new ContentDescription(new QualifiedName[0], getContentType());
		assertFalse("1.0", description.isRequested(FOO_PROPERTY));
		assertNull("1.1", description.getProperty(FOO_PROPERTY));
		description.setProperty(FOO_PROPERTY, "value1");
		assertNull("1.2", description.getProperty(FOO_PROPERTY));
		description.markImmutable();
		try {
			description.setProperty(FOO_PROPERTY, "value1b");
			fail("2.0 - should have failed");
		} catch (IllegalStateException e) {
			// success - the object was marked as immutable
		}
	}

	@Test
	public void testMultipleProperties() {
		ContentDescription description = new ContentDescription(new QualifiedName[] {FOO_PROPERTY, BAR_PROPERTY, ZOO_PROPERTY}, getContentType());
		assertTrue("1.0", description.isRequested(FOO_PROPERTY));
		assertNull("1.1", description.getProperty(FOO_PROPERTY));
		description.setProperty(FOO_PROPERTY, "value1");
		assertEquals("1.2", "value1", description.getProperty(FOO_PROPERTY));
		description.setProperty(FOO_PROPERTY, "value1b");
		assertEquals("1.3", "value1b", description.getProperty(FOO_PROPERTY));
		description.setProperty(BAR_PROPERTY, "value2");
		assertTrue("2.0", description.isRequested(BAR_PROPERTY));
		description.setProperty(BAR_PROPERTY, "value2");
		assertEquals("2.1", "value2", description.getProperty(BAR_PROPERTY));
		assertTrue("2.2", description.isRequested(ZOO_PROPERTY));
		description.setProperty(ZOO_PROPERTY, "value3");
		assertEquals("2.3", "value3", description.getProperty(ZOO_PROPERTY));
		assertFalse("3.0", description.isRequested(FRED_PROPERTY));
		description.setProperty(FRED_PROPERTY, "value3");
		assertNull("3.1", description.getProperty(FRED_PROPERTY));
		description.markImmutable();
		try {
			description.setProperty(FOO_PROPERTY, "value1c");
			fail("4.0 - should have failed");
		} catch (IllegalStateException e) {
			// success - the object was marked as immutable
		}
	}
}
