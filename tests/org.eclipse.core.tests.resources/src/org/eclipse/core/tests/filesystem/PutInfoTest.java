/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.filesystem;

import org.eclipse.core.filesystem.*;
import org.eclipse.core.runtime.CoreException;

/**
 * Black box tests for {@link IFileStore#putInfo(IFileInfo, int, IProgressMonitor)}
 */
public class PutInfoTest extends FileSystemTest {
	public void testSetFileLastModified() {
		IFileStore file = baseStore.getChild("file");
		ensureExists(file, false);
		IFileInfo info = file.fetchInfo();
		long oldLastModified = info.getLastModified();
		long newLastModified = oldLastModified + 100;
		info = EFS.createFileInfo();
		info.setLastModified(newLastModified);
		try {
			file.putInfo(info, EFS.SET_LAST_MODIFIED, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
		info = file.fetchInfo();
		assertEquals("1.0", newLastModified, info.getLastModified());
		assertEquals("1.1", file.getName(), info.getName());
	}

	public void testSetReadOnly() {
		IFileStore file = baseStore.getChild("file");
		ensureExists(file, false);
		IFileInfo info = EFS.createFileInfo();
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
		try {
			file.putInfo(info, EFS.SET_ATTRIBUTES, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
		info = file.fetchInfo();
		assertEquals("1.0", true, info.getAttribute(EFS.ATTRIBUTE_READ_ONLY));
		assertEquals("1.1", file.getName(), info.getName());
	}
}
