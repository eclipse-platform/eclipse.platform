/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.perf;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.OldCorePerformanceTest;

/**
 *
 */
public class BenchCopyFile extends OldCorePerformanceTest {
	private static final int COUNT = 5000;

	public void testCopyFile() {
		IFileStore input = getTempStore();
		createFileInFileSystem(input, getRandomContents());
		IFileStore[] output = new IFileStore[COUNT];
		for (int i = 0; i < output.length; i++) {
			output[i] = getTempStore();
		}
		startBench();
		for (IFileStore element : output) {
			try {
				input.copy(element, EFS.NONE, null);
			} catch (CoreException e) {
				fail("4.99", e);
			}
		}
		stopBench("copyFile", COUNT);

	}

	/**
	 * Override to get a bigger string
	 */
	@Override
	public String getRandomString() {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			buf.append("This is a line of text\n");
		}
		return buf.toString();
	}
}
