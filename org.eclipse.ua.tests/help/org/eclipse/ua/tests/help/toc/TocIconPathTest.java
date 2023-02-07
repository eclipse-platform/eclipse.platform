/*******************************************************************************
 *  Copyright (c) 2008, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.toc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.help.internal.webapp.data.IconFinder;
import org.junit.Test;

public class TocIconPathTest {
	@Test
	public void testNullId() {
		assertNull(IconFinder.getIconAltFromId(null));
		assertNull(IconFinder.getImagePathFromId(null, IconFinder.TYPEICON_CLOSED));
		assertNull(IconFinder.getImagePathFromId(null, IconFinder.TYPEICON_OPEN));
		assertNull(IconFinder.getImagePathFromId(null, IconFinder.TYPEICON_LEAF));
	}

	@Test
	public void testBadId() {
		assertNull(IconFinder.getIconAltFromId("nosuchid"));
		assertNull(IconFinder.getImagePathFromId("nosuchid", IconFinder.TYPEICON_CLOSED));
		assertNull(IconFinder.getImagePathFromId("nosuchid", IconFinder.TYPEICON_OPEN));
		assertNull(IconFinder.getImagePathFromId("nosuchid", IconFinder.TYPEICON_LEAF));
	}

	@Test
	public void testIconSet() {
		String closedPath = IconFinder.getImagePathFromId("org.eclipse.ua.tests.iconSet", IconFinder.TYPEICON_CLOSED);
		String openPath = IconFinder.getImagePathFromId("org.eclipse.ua.tests.iconSet", IconFinder.TYPEICON_OPEN);
		String leafPath = IconFinder.getImagePathFromId("org.eclipse.ua.tests.iconSet", IconFinder.TYPEICON_LEAF);
		String altId = IconFinder.getIconAltFromId("org.eclipse.ua.tests.iconSet");
		assertEquals("altSample", altId);
		assertEquals("org.eclipse.ua.tests/icons/sample.gif", openPath);
		assertEquals("org.eclipse.ua.tests/icons/sample2.gif", closedPath);
		assertEquals("org.eclipse.ua.tests/icons/sample3.gif", leafPath);
	}

	@Test
	public void testSingleIcon() {
		String closedPath = IconFinder.getImagePathFromId("org.eclipse.ua.tests.openOnly", IconFinder.TYPEICON_CLOSED);
		String openPath = IconFinder.getImagePathFromId("org.eclipse.ua.tests.openOnly", IconFinder.TYPEICON_OPEN);
		String leafPath = IconFinder.getImagePathFromId("org.eclipse.ua.tests.openOnly", IconFinder.TYPEICON_LEAF);
		String altId = IconFinder.getIconAltFromId("org.eclipse.ua.tests.openOnly");
		assertNull(altId);
		assertEquals("org.eclipse.ua.tests/icons/sample.gif", openPath);
		assertEquals("org.eclipse.ua.tests/icons/sample.gif", closedPath);
		assertEquals("org.eclipse.ua.tests/icons/sample.gif", leafPath);
	}

	@Test
	public void testNullIconNotDefined() {
		assertFalse(IconFinder.isIconDefined(null));
	}

	@Test
	public void testEmptyIconNotDefined() {
		assertFalse(IconFinder.isIconDefined(null));
	}

	@Test
	public void testUnknownIconNotDefined() {
		assertFalse(IconFinder.isIconDefined("nosuchid"));
	}

	@Test
	public void testKnownIconDefined() {
		assertTrue(IconFinder.isIconDefined("org.eclipse.ua.tests.iconSet"));
	}

	@Test
	public void testOpenOnlyDefined() {
		assertTrue(IconFinder.isIconDefined("org.eclipse.ua.tests.openOnly"));
	}

}
