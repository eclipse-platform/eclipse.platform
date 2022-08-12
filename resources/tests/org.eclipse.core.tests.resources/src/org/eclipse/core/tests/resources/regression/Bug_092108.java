/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import org.eclipse.core.filesystem.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests that obtaining file info works on the root directory on windows.
 */
public class Bug_092108 extends ResourceTest {
	public void testBug() {
		if (!isWindows()) {
			return;
		}
		IFileStore root;
		try {
			root = EFS.getStore(new java.io.File("c:\\").toURI());
			IFileInfo info = root.fetchInfo();
			assertTrue("1.0", info.exists());
			assertTrue("1.1", info.isDirectory());
		} catch (CoreException e) {
			fail("0.99", e);
		}
	}

}
