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

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SymlinkResourceTest extends LocalStoreTest {

	protected void mkLink(IFileStore dir, String src, String tgt, boolean isDir) {
		try {
			createSymLink(dir.toLocalFile(EFS.NONE, getMonitor()), src, tgt, isDir);
		} catch (CoreException e) {
			fail("mkLink", e);
		}
	}

	protected void createBug232426Structure(IFileStore rootDir) throws CoreException {
		IFileStore folderA = rootDir.getChild("a");
		IFileStore folderB = rootDir.getChild("b");
		IFileStore folderC = rootDir.getChild("c");
		folderA.mkdir(EFS.NONE, getMonitor());
		folderB.mkdir(EFS.NONE, getMonitor());
		folderC.mkdir(EFS.NONE, getMonitor());

		/* create symbolic links */
		mkLink(folderA, "link", new Path("../b").toOSString(), true);
		mkLink(folderB, "linkA", new Path("../a").toOSString(), true);
		mkLink(folderB, "linkC", new Path("../c").toOSString(), true);
		mkLink(folderC, "link", new Path("../b").toOSString(), true);
	}

	protected void createBug358830Structure(IFileStore rootDir) throws CoreException {
		IFileStore folderA = rootDir.getChild("a");
		folderA.mkdir(EFS.NONE, getMonitor());

		/* create trivial recursive symbolic link */
		mkLink(folderA, "link", new Path("../").toOSString(), true);
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
		// Only activate this test if testing of symbolic links is possible.
		assumeCanCreateSymLinks();

		/* Re-use projects which are cleaned up automatically */
		final IProject project = projects[0];
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			/* delete open project because we must re-open with BACKGROUND_REFRESH */
			project.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, getMonitor());
			project.create(null);
			createBug232426Structure(EFS.getStore(project.getLocationURI()));
			//Bug only happens with BACKGROUND_REFRESH.
			project.open(IResource.BACKGROUND_REFRESH, getMonitor());
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
		// Only activate this test if testing of symbolic links is possible.
		assumeCanCreateSymLinks();

		/* Re-use projects which are cleaned up automatically */
		final IProject project = projects[0];
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			/* delete open project because we must re-open with BACKGROUND_REFRESH */
			project.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, getMonitor());
			project.create(null);
			createBug358830Structure(EFS.getStore(project.getLocationURI()));
			project.open(IResource.BACKGROUND_REFRESH, getMonitor());
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
