/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.resources;

import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.eclipse.core.internal.resources.LocalMetaArea;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class RemoveUnusedTreeFilesTest {

	/**
	 * A project whose current tree file is missing must not keep a stale tree file
	 * of the same name in another project from being deleted on a full save.
	 */
	@Test
	public void testMissingTreeFileDoesNotProtectOtherProject() throws Exception {
		Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
		LocalMetaArea metaArea = workspace.getMetaArea();
		// projects are visited in name order, so "a" comes before "b"
		IProject a = workspace.getRoot().getProject("a");
		IProject b = workspace.getRoot().getProject("b");
		createInWorkspace(new IProject[] { a, b });

		// leave "b" closed with a current tree file 2.tree and a stale 1.tree
		b.close(createTestMonitor());
		b.open(createTestMonitor());
		b.close(createTestMonitor());
		File bTree = metaArea.getTreeLocationFor(b, false).toFile();
		File bStale = new File(bTree.getParentFile(), "1.tree");
		assertTrue(bStale.exists());

		// leave "a" closed with its current tree file 1.tree missing
		a.close(createTestMonitor());
		File aTree = metaArea.getTreeLocationFor(a, false).toFile();
		assertTrue(aTree.delete());

		workspace.save(true, createTestMonitor());

		assertArrayEquals(new String[] { bTree.getName() },
				bTree.getParentFile().list((dir, name) -> name.endsWith(".tree")));
	}
}
