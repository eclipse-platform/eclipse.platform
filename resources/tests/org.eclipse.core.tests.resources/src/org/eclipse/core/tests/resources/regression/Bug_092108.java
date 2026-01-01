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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that obtaining file info works on the root directory on windows.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class Bug_092108 {

	@Test
	public void testBug() throws CoreException {
		assumeTrue("only relevant on Windows", OS.isWindows());

		IFileStore root = EFS.getStore(new java.io.File("c:\\").toURI());
		IFileInfo info = root.fetchInfo();
		assertThat(info).matches(IFileInfo::exists, "exists");
		assertThat(info).matches(IFileInfo::isDirectory, "is directory");
	}

}
