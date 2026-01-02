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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.getFileStore;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isAttributeSupported;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for bug 329836
 */
@ExtendWith(WorkspaceResetExtension.class)
public class Bug_329836 {

	@Test
	public void testBug(@TempDir Path tempDirectory) throws Exception {
		assumeTrue(OS.isMac(), "only relevant on Mac");

		IFileStore fileStore = getFileStore(tempDirectory).getChild(createUniqueString());
		createInFileSystem(fileStore);

		// set EFS.ATTRIBUTE_READ_ONLY which also sets EFS.IMMUTABLE on Mac
		IFileInfo info = fileStore.fetchInfo();
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
		fileStore.putInfo(info, EFS.SET_ATTRIBUTES, createTestMonitor());

		// read the info again
		info = fileStore.fetchInfo();

		// check that attributes are really set
		assertThat(info).matches(it -> it.getAttribute(EFS.ATTRIBUTE_READ_ONLY), "is read only");
		if (isAttributeSupported(EFS.ATTRIBUTE_IMMUTABLE)) {
			assertThat(info).matches(it -> it.getAttribute(EFS.ATTRIBUTE_IMMUTABLE), "is immutable");
		}

		// unset EFS.ATTRIBUTE_READ_ONLY which also unsets EFS.IMMUTABLE on Mac

		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, false);
		fileStore.putInfo(info, EFS.SET_ATTRIBUTES, createTestMonitor());

		// read the info again
		info = fileStore.fetchInfo();

		// check that attributes are really unset
		assertThat(info).matches(it -> !it.getAttribute(EFS.ATTRIBUTE_READ_ONLY), "is not read only");
		if (isAttributeSupported(EFS.ATTRIBUTE_IMMUTABLE)) {
			assertThat(info).matches(it -> !it.getAttribute(EFS.ATTRIBUTE_IMMUTABLE), "is not immutable");
		}
	}

}
