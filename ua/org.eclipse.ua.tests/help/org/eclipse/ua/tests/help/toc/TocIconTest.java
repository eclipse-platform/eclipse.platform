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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.swt.graphics.Image;
import org.junit.Test;

public class TocIconTest {
	@Test
	public void testNullId() {
		assertNull(HelpUIResources.getImageFromId(null, false, false));
		assertNull(HelpUIResources.getImageFromId(null, true, false));
		assertNull(HelpUIResources.getImageFromId(null, false, true));
	}

	@Test
	public void testBadId() {
		assertNull(HelpUIResources.getImageFromId("nosuchid", false, false));
		assertNull(HelpUIResources.getImageFromId("nosuchid", true, false));
		assertNull(HelpUIResources.getImageFromId("nosuchid", false, true));
	}

	@Test
	public void testIconSet() {
		Image closedImage = HelpUIResources.getImageFromId("org.eclipse.ua.tests.iconSet", false, false);
		Image openImage = HelpUIResources.getImageFromId("org.eclipse.ua.tests.iconSet", true, false);
		Image leafImage = HelpUIResources.getImageFromId("org.eclipse.ua.tests.iconSet", false, true);
		assertNotNull(openImage);
		assertNotNull(closedImage);
		assertNotNull(leafImage);
		assertFalse(openImage.equals(closedImage));
		assertFalse(openImage.equals(leafImage));
		assertFalse(closedImage.equals(leafImage));
	}

	@Test
	public void testSingleIcon() {
		Image closedImage = HelpUIResources.getImageFromId("org.eclipse.ua.tests.openOnly", false, false);
		Image openImage = HelpUIResources.getImageFromId("org.eclipse.ua.tests.openOnly", true, false);
		Image leafImage = HelpUIResources.getImageFromId("org.eclipse.ua.tests.openOnly", false, true);
		assertNotNull(openImage);
		assertNotNull(closedImage);
		assertNotNull(leafImage);
		assertTrue(openImage.equals(closedImage));
		assertTrue(openImage.equals(leafImage));
		assertTrue(closedImage.equals(leafImage));
	}

}
