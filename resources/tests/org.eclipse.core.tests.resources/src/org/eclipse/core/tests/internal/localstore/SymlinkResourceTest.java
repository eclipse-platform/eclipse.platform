/*******************************************************************************
 * Copyright (c) 2008, 2017 Wind River Systems, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Martin Oberhuber (Wind River) - initial API and implementation for [232426]
 *     Szymon Ptaszkiewicz (IBM) - Symlink test failures on Windows 7 [331716]
 *     Sergey Prigogin (Google) -  ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.internal.localstore;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.harness.FileSystemHelper.canCreateSymLinks;
import static org.eclipse.core.tests.harness.FileSystemHelper.createSymLink;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForRefresh;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

public class SymlinkResourceTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private void mkLink(IFileStore dir, String src, String tgt, boolean isDir) throws CoreException, IOException {
		createSymLink(dir.toLocalFile(EFS.NONE, createTestMonitor()), src, tgt, isDir);
	}

	protected void createBug232426Structure(IFileStore rootDir) throws CoreException, IOException {
		IFileStore folderA = rootDir.getChild("a");
		IFileStore folderB = rootDir.getChild("b");
		IFileStore folderC = rootDir.getChild("c");
		folderA.mkdir(EFS.NONE, createTestMonitor());
		folderB.mkdir(EFS.NONE, createTestMonitor());
		folderC.mkdir(EFS.NONE, createTestMonitor());

		/* create symbolic links */
		mkLink(folderA, "link", IPath.fromOSString("../b").toOSString(), true);
		mkLink(folderB, "linkA", IPath.fromOSString("../a").toOSString(), true);
		mkLink(folderB, "linkC", IPath.fromOSString("../c").toOSString(), true);
		mkLink(folderC, "link", IPath.fromOSString("../b").toOSString(), true);
	}

	protected void createBug358830Structure(IFileStore rootDir) throws CoreException, IOException {
		IFileStore folderA = rootDir.getChild("a");
		folderA.mkdir(EFS.NONE, createTestMonitor());

		/* create trivial recursive symbolic link */
		mkLink(folderA, "link", IPath.fromOSString("../").toOSString(), true);
	}

	/**
	 * Test a very specific case of mutually recursive symbolic links:
	 * <pre> {@code
	 *   a/link  -> ../b
	 *   b/link1 -> ../a, b/link2 -> ../c
	 *   c/link  -> ../b
	 * }</pre>
	 * In the specific bug, the two links in b were followed in an alternated
	 * fashion while walking down the tree. A correct implementation should
	 * stop following symbolic links as soon as a node is reached that has
	 * been visited before.
	 * See <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=232426">bug 232426</a>
	 */
	@Test
	public void testBug232426() throws Exception {
		assumeTrue("only relevant for platforms supporting symbolic links", canCreateSymLinks());

		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);
		/* Re-use projects which are cleaned up automatically */
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			/* delete open project because we must re-open with BACKGROUND_REFRESH */
			project.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, createTestMonitor());
			project.create(null);
			try {
				createBug232426Structure(EFS.getStore(project.getLocationURI()));
			} catch (IOException e) {
				throw new IllegalStateException("unexpected IOException occurred", e);
			}
			//Bug only happens with BACKGROUND_REFRESH.
			project.open(IResource.BACKGROUND_REFRESH, createTestMonitor());
		}, null);

		//wait for BACKGROUND_REFRESH to complete.
		waitForRefresh();
		project.accept(new IResourceVisitor() {
			int resourceCount = 0;

			@Override
			public boolean visit(IResource resource) {
				resourceCount++;
				// We have 1 root + 4 folders + 5 elements --> 10 elements to visit at most
				assertTrue(resourceCount <= 10);
				return true;
			}
		});
	}

	@Test
	public void testBug358830() throws Exception {
		assumeTrue("only relevant for platforms supporting symbolic links", canCreateSymLinks());

		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);
		/* Re-use projects which are cleaned up automatically */
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			/* delete open project because we must re-open with BACKGROUND_REFRESH */
			project.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, createTestMonitor());
			project.create(null);
			try {
				createBug358830Structure(EFS.getStore(project.getLocationURI()));
			} catch (IOException e) {
				throw new IllegalStateException("unexpected IOException occurred", e);
			}
			project.open(IResource.BACKGROUND_REFRESH, createTestMonitor());
		}, null);

		//wait for BACKGROUND_REFRESH to complete.
		waitForRefresh();
		final int resourceCount[] = new int[] {0};
		project.accept(resource -> {
			resourceCount[0]++;
			return true;
		});
		// We have 1 root + 1 folder + 1 file (.project)
		// + .settings / resources prefs
		// --> 5 elements to visit
		assertEquals(5, resourceCount[0]);
	}

}
