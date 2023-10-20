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

import static org.junit.Assert.assertEquals;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.junit.Test;

/**
 * Black box tests for {@link IFileStore#putInfo(IFileInfo, int, IProgressMonitor)}
 */
public class PutInfoTest extends FileSystemTest {
	@Test
	public void testSetFileLastModified() throws Exception {
		IFileStore file = baseStore.getChild("file");
		ensureExists(file, false);
		IFileInfo info = file.fetchInfo();
		long oldLastModified = info.getLastModified();
		long newLastModified = oldLastModified + 100;
		info = EFS.createFileInfo();
		info.setLastModified(newLastModified);
		file.putInfo(info, EFS.SET_LAST_MODIFIED, getMonitor());
		info = file.fetchInfo();
		assertEquals("1.0", newLastModified, info.getLastModified());
		assertEquals("1.1", file.getName(), info.getName());
	}

	@Test
	public void testSetReadOnly() throws Exception {
		IFileStore file = baseStore.getChild("file");
		ensureExists(file, false);
		IFileInfo info = EFS.createFileInfo();
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
		file.putInfo(info, EFS.SET_ATTRIBUTES, getMonitor());
		info = file.fetchInfo();
		assertEquals("1.0", true, info.getAttribute(EFS.ATTRIBUTE_READ_ONLY));
		assertEquals("1.1", file.getName(), info.getName());
	}
}
