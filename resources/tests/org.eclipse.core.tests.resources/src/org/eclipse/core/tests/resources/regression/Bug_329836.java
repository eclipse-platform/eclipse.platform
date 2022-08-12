/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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
 *     Sergey Prigogin (Google) - Bug 458006 - Fix tests that fail on Mac when filesystem.java7 is used
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import org.eclipse.core.filesystem.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Test for bug 329836
 */
public class Bug_329836 extends ResourceTest {
	public void testBug() {
		if (!Platform.getOS().equals(Platform.OS_MACOSX)) {
			return;
		}

		IFileStore fileStore = getTempStore().getChild(getUniqueString());
		createFileInFileSystem(fileStore);

		// set EFS.ATTRIBUTE_READ_ONLY which also sets EFS.IMMUTABLE on Mac
		IFileInfo info = fileStore.fetchInfo();
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
		try {
			fileStore.putInfo(info, EFS.SET_ATTRIBUTES, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// read the info again
		info = fileStore.fetchInfo();

		// check that attributes are really set
		assertTrue("2.0", info.getAttribute(EFS.ATTRIBUTE_READ_ONLY));
		if (isAttributeSupported(EFS.ATTRIBUTE_IMMUTABLE)) {
			assertTrue("3.0", info.getAttribute(EFS.ATTRIBUTE_IMMUTABLE));
		}

		// unset EFS.ATTRIBUTE_READ_ONLY which also unsets EFS.IMMUTABLE on Mac

		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, false);
		try {
			fileStore.putInfo(info, EFS.SET_ATTRIBUTES, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}

		// read the info again
		info = fileStore.fetchInfo();

		// check that attributes are really unset
		assertFalse("5.0", info.getAttribute(EFS.ATTRIBUTE_READ_ONLY));
		if (isAttributeSupported(EFS.ATTRIBUTE_IMMUTABLE)) {
			assertFalse("6.0", info.getAttribute(EFS.ATTRIBUTE_IMMUTABLE));
		}
	}
}
