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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.localstore.UnifiedTree;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(WorkspaceResetExtension.class)
public class SymlinkResourceTest {

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
	 * Test a case of both recursive and non recursive symbolic links that uncovered
	 * issue described in <a href=
	 * "https://github.com/eclipse-platform/eclipse.platform/issues/2220">GitHub bug
	 * 2220</a>.
	 *
	 * <pre>{@code
	 *      /A/B -> /X/Y/Z  	(B is symbolic link as precondition)
	 *      /A/B/C/D -> ../../D (non-recursive)
	 *      /A/B/C/E -> ../../Z (recursive)
	 * }</pre>
	 *
	 * The starting path /A/B/C is already based on link. The real path of start is
	 * /X/Y/Z/C so ../../Z points (recursive) to /X/Y/Z. /X/Y/D and /X/Y/Z is what
	 * we expect to resolve but we fail in both cases with NoSuchFileException.
	 */
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void testGithubBug2220(boolean useAdvancedLinkCheck) throws Exception {
		assumeTrue(canCreateSymLinks(), "only relevant for platforms supporting symbolic links");
		assumeTrue(!OS.isWindows(), "Windows file system handles recursive links differently");
		final boolean originalValue = UnifiedTree.isAdvancedRecursiveLinkChecksEnabled();
		try {
			UnifiedTree.enableAdvancedRecursiveLinkChecks(useAdvancedLinkCheck);
			IProject project = getWorkspace().getRoot().getProject("testGithubBug2220");
			createInWorkspace(project);

			/* Re-use projects which are cleaned up automatically */
			getWorkspace().run((IWorkspaceRunnable) monitor -> {
				/* delete open project because we must re-open with BACKGROUND_REFRESH */
				project.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, createTestMonitor());
				project.create(null);
				try {
					createGithubBug2220Structure(EFS.getStore(project.getLocationURI()));
				} catch (IOException e) {
					throw new IllegalStateException("unexpected IOException occurred", e);
				}
				// Bug only happens with BACKGROUND_REFRESH.
				project.open(IResource.BACKGROUND_REFRESH, createTestMonitor());
			}, null);

			// wait for BACKGROUND_REFRESH to complete.
			waitForRefresh();
			project.accept(new IResourceVisitor() {
				int resourceCount = 0;

				@Override
				public boolean visit(IResource resource) {
					resourceCount++;
					// We have 1 root + .settings + prefs + + .project + 10 elements --> 14 elements
					// to visit at most
					System.out.println(resourceCount + " visited: " + resource.getFullPath());
					assertTrue(resourceCount <= 15, "Expected max 15 elements to visit, got: " + resourceCount);
					return true;
				}
			});
		} finally {
			UnifiedTree.enableAdvancedRecursiveLinkChecks(originalValue);
		}
	}

	/**
	 * <pre>{@code
	 *      /A/B -> /X/Y/Z  	(B is symbolic link as precondition)
	 *      /A/B/C/D -> ../../D (non-recursive)
	 *      /A/B/C/E -> ../../Z (recursive)
	 * }</pre>
	 *
	 * The starting path /A/B/C is already based on link. The real path of start is
	 * /X/Y/Z/C so ../../Z points (recursive) to /X/Y/Z.
	 */
	protected void createGithubBug2220Structure(IFileStore rootDir) throws CoreException, IOException {
		Path root = rootDir.toLocalFile(EFS.NONE, createTestMonitor()).toPath();
		Files.createDirectories(root.resolve("A"));
		Files.createDirectories(root.resolve("X/Y/Z"));
		Files.createDirectories(root.resolve("X/Y/D"));
		Files.createSymbolicLink(root.resolve("A/B"), root.resolve("X/Y/Z"));
		Files.createDirectories(root.resolve("A/B/C"));
		Files.createSymbolicLink(root.resolve("A/B/C/D"), Paths.get("../../D"));
		Files.createSymbolicLink(root.resolve("A/B/C/E"), Paths.get("../../Z"));
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
		assumeTrue(canCreateSymLinks(), "only relevant for platforms supporting symbolic links");

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

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void testBug358830(boolean useAdvancedLinkCheck) throws Exception {
		assumeTrue(canCreateSymLinks(), "only relevant for platforms supporting symbolic links");
		final boolean originalValue = UnifiedTree.isAdvancedRecursiveLinkChecksEnabled();
		try {
			UnifiedTree.enableAdvancedRecursiveLinkChecks(useAdvancedLinkCheck);
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

			// wait for BACKGROUND_REFRESH to complete.
			waitForRefresh();
			final int resourceCount[] = new int[] { 0 };
			project.accept(resource -> {
				resourceCount[0]++;
				return true;
			});
			// We have 1 root + 1 folder + 1 file (.project)
			// + .settings / resources prefs
			// --> 5 elements to visit
			assertEquals(5, resourceCount[0]);
		} finally {
			UnifiedTree.enableAdvancedRecursiveLinkChecks(originalValue);
		}
	}

}
