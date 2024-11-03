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

import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.internal.content.ContentDescription;
import org.eclipse.core.internal.content.ContentType;
import org.eclipse.core.internal.content.ContentTypeHandler;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.junit.jupiter.api.Test;

public class ContentDescriptionTest {
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
		assertTrue(description.isRequested(FOO_PROPERTY));
		assertNull(description.getProperty(FOO_PROPERTY));
		description.setProperty(FOO_PROPERTY, "value1");
		assertEquals("value1", description.getProperty(FOO_PROPERTY));
		description.setProperty(FOO_PROPERTY, "value1b");
		assertEquals("value1b", description.getProperty(FOO_PROPERTY));
		assertTrue(description.isRequested(BAR_PROPERTY));
		description.setProperty(BAR_PROPERTY, "value2");
		assertEquals("value2", description.getProperty(BAR_PROPERTY));
		description.setProperty(ZOO_PROPERTY, "value3");
		assertEquals("value3", description.getProperty(ZOO_PROPERTY));
		description.markImmutable();
		assertThrows(IllegalStateException.class, () -> description.setProperty(FOO_PROPERTY, "value1c"));
	}

	@Test
	public void testOneProperty() {
		ContentDescription description = new ContentDescription(new QualifiedName[] {FOO_PROPERTY}, getContentType());
		assertTrue(description.isRequested(FOO_PROPERTY));
		assertNull(description.getProperty(FOO_PROPERTY));
		description.setProperty(FOO_PROPERTY, "value1");
		assertEquals("value1", description.getProperty(FOO_PROPERTY));
		description.setProperty(FOO_PROPERTY, "value1b");
		assertEquals("value1b", description.getProperty(FOO_PROPERTY));
		description.setProperty(BAR_PROPERTY, "value2");
		assertFalse(description.isRequested(BAR_PROPERTY));
		description.setProperty(BAR_PROPERTY, "value2");
		assertNull(description.getProperty(BAR_PROPERTY));
		description.markImmutable();
		assertThrows(IllegalStateException.class, () -> description.setProperty(FOO_PROPERTY, "value1c"));
	}

	@Test
	public void testZeroProperties() {
		ContentDescription description = new ContentDescription(new QualifiedName[0], getContentType());
		assertFalse(description.isRequested(FOO_PROPERTY));
		assertNull(description.getProperty(FOO_PROPERTY));
		description.setProperty(FOO_PROPERTY, "value1");
		assertNull(description.getProperty(FOO_PROPERTY));
		description.markImmutable();
		assertThrows(IllegalStateException.class, () -> description.setProperty(FOO_PROPERTY, "value1b"));
	}

	@Test
	public void testMultipleProperties() {
		ContentDescription description = new ContentDescription(new QualifiedName[] {FOO_PROPERTY, BAR_PROPERTY, ZOO_PROPERTY}, getContentType());
		assertTrue(description.isRequested(FOO_PROPERTY));
		assertNull(description.getProperty(FOO_PROPERTY));
		description.setProperty(FOO_PROPERTY, "value1");
		assertEquals("value1", description.getProperty(FOO_PROPERTY));
		description.setProperty(FOO_PROPERTY, "value1b");
		assertEquals("value1b", description.getProperty(FOO_PROPERTY));
		description.setProperty(BAR_PROPERTY, "value2");
		assertTrue(description.isRequested(BAR_PROPERTY));
		description.setProperty(BAR_PROPERTY, "value2");
		assertEquals("value2", description.getProperty(BAR_PROPERTY));
		assertTrue(description.isRequested(ZOO_PROPERTY));
		description.setProperty(ZOO_PROPERTY, "value3");
		assertEquals("value3", description.getProperty(ZOO_PROPERTY));
		assertFalse(description.isRequested(FRED_PROPERTY));
		description.setProperty(FRED_PROPERTY, "value3");
		assertNull(description.getProperty(FRED_PROPERTY));
		description.markImmutable();
		assertThrows(IllegalStateException.class, () -> description.setProperty(FOO_PROPERTY, "value1c"));
	}
}
